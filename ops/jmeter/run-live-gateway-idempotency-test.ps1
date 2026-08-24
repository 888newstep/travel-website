[CmdletBinding()]
param(
    [Parameter()]
    [string]$Username = 'zhangsan',

    [Parameter()]
    [string]$Password = $env:TEST_DATA_USER_PASSWORD,

    [Parameter()]
    [ValidateRange(1, [int]::MaxValue)]
    [int]$RouteId = 1,

    [Parameter()]
    [ValidateRange(1, 2000)]
    [int]$Threads = 100,

    [Parameter()]
    [ValidateRange(1, 10)]
    [int]$SteadyStateRuns = 5,

    [Parameter()]
    [ValidateRange(0, 10)]
    [int]$WarmupRuns = 5,

    [Parameter()]
    [ValidateRange(0, 30)]
    [int]$InterRoundDelaySeconds = 2,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$RedisPort = 16380,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$UserServicePort = 18091,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$CollectionServicePort = 18094,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$RouteServicePort = 18093,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$GatewayPort = 8090,

    [Parameter()]
    [string]$RedisServerPath = 'E:\Redis\redis-7.2.4\redis-server.exe',

    [Parameter()]
    [string]$JMeterPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Import-DotEnv {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $values
    }
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $name = $matches[1]
            $value = $matches[2].Trim()
            if ($value.Length -ge 2 -and
                    (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                     ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $values[$name] = $value
        }
    }
    return $values
}

function ConvertTo-NativeArgument {
    param([AllowEmptyString()][string]$Value)

    if ($Value.Length -eq 0) {
        return '""'
    }
    if ($Value -notmatch '[\s"]') {
        return $Value
    }
    return '"' + $Value.Replace('"', '\"') + '"'
}

function Join-NativeArguments {
    param([string[]]$Values)

    return (($Values | ForEach-Object { ConvertTo-NativeArgument $_ }) -join ' ')
}

function Start-NativeProcess {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [hashtable]$Environment
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.Arguments = Join-NativeArguments $Arguments
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    foreach ($name in $Environment.Keys) {
        $startInfo.EnvironmentVariables[$name] = [string]$Environment[$name]
    }
    return [System.Diagnostics.Process]::Start($startInfo)
}

function Wait-TcpPort {
    param(
        [int]$Port,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$Process,
        [string]$Name
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($null -ne $Process) {
            $Process.Refresh()
            if ($Process.HasExited) {
                throw "$Name exited with code $($Process.ExitCode)."
            }
        }
        if ($null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
                    Select-Object -First 1)) {
            return
        }
        Start-Sleep -Milliseconds 300
    }
    throw "$Name did not listen on port $Port."
}

function Stop-ProcessSafely {
    param([System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return
    }
    try {
        $processId = $Process.Id
        $Process.Refresh()
        if (-not $Process.HasExited) {
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
            Wait-Process -Id $processId -Timeout 10 -ErrorAction SilentlyContinue
        }
    }
    catch {
        Write-Warning "Failed to stop process $($Process.Id): $($_.Exception.Message)"
    }
}

function Stop-PortOwnerSafely {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($null -eq $connection) {
        return
    }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($connection.OwningProcess)" `
            -ErrorAction SilentlyContinue
    if ($null -eq $process -or $process.Name -notin @('java.exe', 'redis-server.exe')) {
        Write-Warning "Refusing to stop unexpected process on port ${Port}: $($process.Name)"
        return
    }
    if ($process.Name -eq 'java.exe' -and
            $process.CommandLine -notmatch 'IdeaProjects\\travel\\backend\\(gateway|user-service|route-service|collection-service)\\target') {
        Write-Warning "Refusing to stop unrelated Java process on port $Port."
        return
    }
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
}

function Invoke-MySql {
    param(
        [string]$MySqlPath,
        [string]$Sql,
        [hashtable]$Database
    )

    $arguments = @(
        "--host=$($Database.Host)",
        "--port=$($Database.Port)",
        "--user=$($Database.Username)",
        "--database=$($Database.Name)",
        '--batch',
        '--skip-column-names',
        "--execute=$Sql"
    )
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $MySqlPath
    $startInfo.Arguments = Join-NativeArguments $arguments
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.EnvironmentVariables['MYSQL_PWD'] = $Database.Password
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        Stdout = $stdout.Trim()
        Stderr = $stderr.Trim()
    }
}

function Get-Median {
    param([double[]]$Values)

    if ($Values.Count -eq 0) {
        return 0.0
    }
    $sorted = @($Values | Sort-Object)
    $middle = [int][Math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 1) {
        return [double]$sorted[$middle]
    }
    return ([double]$sorted[$middle - 1] + [double]$sorted[$middle]) / 2.0
}

function Get-CoefficientOfVariationPercent {
    param([double[]]$Values)

    if ($Values.Count -eq 0) {
        return 0.0
    }
    $mean = ($Values | Measure-Object -Average).Average
    if ([double]$mean -eq 0.0) {
        return 0.0
    }
    $squaredDifferences = @($Values | ForEach-Object { [Math]::Pow(([double]$_ - [double]$mean), 2) })
    $variance = ($squaredDifferences | Measure-Object -Average).Average
    return [Math]::Round(([Math]::Sqrt($variance) / [double]$mean) * 100.0, 2)
}

function Get-StageMetric {
    param(
        [string]$MetricsUri,
        [string]$Stage,
        [string]$Outcome
    )

    $filters = [System.Collections.Generic.List[string]]::new()
    $filters.Add('tag=' + [uri]::EscapeDataString("stage:$Stage"))
    if (-not [string]::IsNullOrWhiteSpace($Outcome)) {
        $filters.Add('tag=' + [uri]::EscapeDataString("outcome:$Outcome"))
    }
    $uri = $MetricsUri + '?' + ($filters -join '&')
    try {
        $metric = Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 10
    }
    catch {
        $response = $_.Exception.Response
        if ($null -ne $response -and [int]$response.StatusCode -eq 404) {
            return [pscustomobject][ordered]@{
                count = 0.0
                totalTimeSeconds = 0.0
                maxSeconds = 0.0
            }
        }
        throw
    }

    $count = $metric.measurements | Where-Object { $_.statistic -eq 'COUNT' } | Select-Object -First 1
    $totalTime = $metric.measurements | Where-Object { $_.statistic -eq 'TOTAL_TIME' } | Select-Object -First 1
    $maximum = $metric.measurements | Where-Object { $_.statistic -eq 'MAX' } | Select-Object -First 1
    return [pscustomobject][ordered]@{
        count = if ($null -ne $count) { [double]$count.value } else { 0.0 }
        totalTimeSeconds = if ($null -ne $totalTime) { [double]$totalTime.value } else { 0.0 }
        maxSeconds = if ($null -ne $maximum) { [double]$maximum.value } else { 0.0 }
    }
}

function Get-StageMetricSnapshot {
    param([string]$MetricsUri)

    $specifications = @(
        [pscustomobject]@{ name = 'service.jwt.authenticated'; stage = 'service.jwt'; outcome = 'authenticated' },
        [pscustomobject]@{ name = 'idempotency.claim'; stage = 'idempotency.claim'; outcome = $null },
        [pscustomobject]@{ name = 'idempotency.complete'; stage = 'idempotency.complete'; outcome = $null },
        [pscustomobject]@{ name = 'collection.route-feign'; stage = 'collection.route-feign'; outcome = $null },
        [pscustomobject]@{ name = 'collection.user-feign'; stage = 'collection.user-feign'; outcome = $null },
        [pscustomobject]@{ name = 'collection.toggle-locked'; stage = 'collection.toggle-locked'; outcome = $null },
        [pscustomobject]@{ name = 'collection.lookup'; stage = 'collection.lookup'; outcome = $null },
        [pscustomobject]@{ name = 'collection.db-insert'; stage = 'collection.db-insert'; outcome = $null },
        [pscustomobject]@{ name = 'collection.cache-invalidation'; stage = 'collection.cache-invalidation'; outcome = $null }
    )
    $snapshot = [ordered]@{}
    foreach ($specification in $specifications) {
        $snapshot[$specification.name] = Get-StageMetric -MetricsUri $MetricsUri `
                -Stage $specification.stage -Outcome $specification.outcome
    }
    return $snapshot
}

function Get-StageMetricDelta {
    param(
        [System.Collections.IDictionary]$Before,
        [System.Collections.IDictionary]$After
    )

    $deltas = [System.Collections.Generic.List[object]]::new()
    foreach ($name in $After.Keys) {
        $count = [Math]::Max(0.0, [double]$After[$name].count - [double]$Before[$name].count)
        $totalTimeSeconds = [Math]::Max(
                0.0,
                [double]$After[$name].totalTimeSeconds - [double]$Before[$name].totalTimeSeconds)
        $deltas.Add([pscustomobject][ordered]@{
            stage = $name
            count = [long][Math]::Round($count)
            totalTimeMs = [Math]::Round($totalTimeSeconds * 1000.0, 3)
            averageMs = if ($count -gt 0.0) {
                [Math]::Round(($totalTimeSeconds * 1000.0) / $count, 3)
            } else { 0.0 }
            cumulativeMaxMs = [Math]::Round([double]$After[$name].maxSeconds * 1000.0, 3)
        })
    }
    return @($deltas)
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    throw 'The business test password is blank. Pass -Password or set TEST_DATA_USER_PASSWORD.'
}
if (-not (Test-Path -LiteralPath $RedisServerPath -PathType Leaf)) {
    throw "redis-server.exe does not exist: $RedisServerPath"
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$envValues = Import-DotEnv -Path (Join-Path $repositoryRoot 'deploy\.env')
$database = @{
    Host = if ($envValues['DB_HOST']) { $envValues['DB_HOST'] } else { '127.0.0.1' }
    Port = if ($envValues['DB_PORT']) { [int]$envValues['DB_PORT'] } else { 3306 }
    Name = if ($envValues['DB_NAME']) { $envValues['DB_NAME'] } else { 'travel_website' }
    Username = if ($envValues['DB_USERNAME']) { $envValues['DB_USERNAME'] } else { 'root' }
    Password = $envValues['DB_PASSWORD']
}
if ([string]::IsNullOrWhiteSpace($database.Password)) {
    throw 'DB_PASSWORD is missing from deploy/.env.'
}

$mysqlPath = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
if (-not (Test-Path -LiteralPath $mysqlPath -PathType Leaf)) {
    $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($null -eq $mysqlCommand) {
        throw 'mysql.exe was not found.'
    }
    $mysqlPath = $mysqlCommand.Source
}
$javaPath = if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    $null
}
if ([string]::IsNullOrWhiteSpace($javaPath) -or -not (Test-Path -LiteralPath $javaPath -PathType Leaf)) {
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($null -ne $javacCommand) {
        $javaPath = Join-Path (Split-Path -Parent $javacCommand.Source) 'java.exe'
    }
}
if ([string]::IsNullOrWhiteSpace($javaPath) -or -not (Test-Path -LiteralPath $javaPath -PathType Leaf)) {
    throw 'java.exe was not found.'
}

$userJar = (Get-ChildItem (Join-Path $repositoryRoot 'backend\user-service\target') -Filter 'user-service-*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1).FullName
$collectionJar = (Get-ChildItem (Join-Path $repositoryRoot 'backend\collection-service\target') -Filter 'collection-service-*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1).FullName
$routeJar = (Get-ChildItem (Join-Path $repositoryRoot 'backend\route-service\target') -Filter 'route-service-*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1).FullName
$gatewayJar = (Get-ChildItem (Join-Path $repositoryRoot 'backend\gateway\target') -Filter 'gateway-*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1).FullName
if ([string]::IsNullOrWhiteSpace($userJar) -or
        [string]::IsNullOrWhiteSpace($collectionJar) -or
        [string]::IsNullOrWhiteSpace($routeJar) -or
        [string]::IsNullOrWhiteSpace($gatewayJar)) {
    throw 'Required service jars were not found. Run backend compilation first.'
}

$jmeterScript = Join-Path $repositoryRoot 'ops\jmeter\run-idempotency-test.ps1'
$outputDirectory = Join-Path $repositoryRoot ('run-logs\jmeter-gateway-idempotency\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$redisPassword = 'isolated-' + [guid]::NewGuid().ToString('N')
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
$result = $null
$summaryRecord = $null
$baselinePrepared = $false
$baselineRestored = $false
$baselineRows = 0
$cleanupError = $null
$backupTable = 'e2e_user_collection_backup_' + [guid]::NewGuid().ToString('N')

$userLookup = Invoke-MySql -MySqlPath $mysqlPath -Database $database `
        -Sql ("SELECT id FROM user WHERE username='" + $Username.Replace("'", "''") + "' LIMIT 1;")
if ($userLookup.ExitCode -ne 0 -or $userLookup.Stdout -notmatch '^\d+$') {
    throw "Could not resolve test user: $($userLookup.Stderr)"
}
$userId = [int]$userLookup.Stdout
$targetPredicate = "user_id=$userId AND item_id=$RouteId AND item_type='route' AND collection_type='collect'"

try {
    $baselineResult = Invoke-MySql -MySqlPath $mysqlPath -Database $database -Sql (
            "CREATE TABLE $backupTable LIKE user_collection; " +
            "INSERT INTO $backupTable SELECT * FROM user_collection WHERE $targetPredicate; " +
            "DELETE FROM user_collection WHERE $targetPredicate; " +
            "SELECT COUNT(*) FROM $backupTable;")
    if ($baselineResult.ExitCode -ne 0 -or $baselineResult.Stdout -notmatch '^\d+$') {
        throw "Could not prepare collection baseline: $($baselineResult.Stderr)"
    }
    $baselineRows = [int]$baselineResult.Stdout
    $baselinePrepared = $true

    $redisArguments = @(
        '--port', [string]$RedisPort,
        '--bind', '127.0.0.1',
        '--protected-mode', 'yes',
        '--requirepass', $redisPassword,
        '--databases', '16',
        '--appendonly', 'no',
        '--save', '',
        '--dir', $outputDirectory,
        '--dbfilename', 'gateway-idempotency.rdb',
        '--logfile', 'redis.log'
    )
    $redisProcess = Start-NativeProcess -FilePath $RedisServerPath -Arguments $redisArguments `
            -WorkingDirectory $outputDirectory -Environment @{}
    $processes.Add($redisProcess)
    Wait-TcpPort -Port $RedisPort -TimeoutSeconds 20 -Process $redisProcess -Name 'isolated Redis'

    $serviceEnvironment = @{
        REDIS_HOST = '127.0.0.1'
        REDIS_PORT = $RedisPort
        REDIS_PASSWORD = $redisPassword
        JWT_SECRET = $envValues['JWT_SECRET']
        RABBITMQ_HOST = '127.0.0.1'
        RABBITMQ_PORT = 1
        DB_HOST = $database.Host
        DB_PORT = $database.Port
        DB_NAME = $database.Name
        DB_USERNAME = $database.Username
        DB_PASSWORD = $database.Password
    }
    $serviceFlags = @(
        '--spring.profiles.active=e2e',
        '--spring.cloud.nacos.discovery.enabled=false',
        '--spring.cloud.nacos.config.enabled=false',
        '--spring.cloud.nacos.config.import-check.enabled=false',
        '--spring.cloud.service-registry.auto-registration.enabled=false',
        '--spring.cloud.sentinel.eager=false',
        '--spring.cloud.sentinel.enabled=false',
        '--xxl.job.enabled=false',
        '--travel.http.idempotency.enabled=true',
        '--spring.rabbitmq.listener.simple.auto-startup=false',
        '--spring.rabbitmq.dynamic=false',
        '--spring.sql.init.mode=never',
        '--spring.main.banner-mode=off',
        '--spring.output.ansi.enabled=never',
        '--logging.level.root=WARN',
        '--logging.level.travel=INFO',
        '--logging.level.org.springframework.web=WARN',
        '--logging.level.org.mybatis=WARN',
        '--mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl',
        '--travel.performance.stage-timing-enabled=true',
        '--travel.performance.metrics-endpoints-enabled=true',
        '--management.endpoints.web.exposure.include=health,info,metrics,prometheus'
    )
    $javaBaseArguments = @('-Xms512m', '-Xmx512m', '-Dfile.encoding=UTF-8', '-jar')
    $userArguments = $javaBaseArguments + @($userJar, "--server.port=$UserServicePort") + $serviceFlags +
            @("--logging.file.name=$((Join-Path $outputDirectory 'user-service.log').Replace('\', '/'))")
    $collectionArguments = $javaBaseArguments + @($collectionJar, "--server.port=$CollectionServicePort") + $serviceFlags +
            @("--logging.file.name=$((Join-Path $outputDirectory 'collection-service.log').Replace('\', '/'))",
              "--spring.cloud.openfeign.client.config.route-service.url=http://127.0.0.1:$RouteServicePort",
              '--spring.cloud.openfeign.client.config.route-service.connectTimeout=3000',
              '--spring.cloud.openfeign.client.config.route-service.readTimeout=15000',
              "--spring.cloud.openfeign.client.config.user-service.url=http://127.0.0.1:$UserServicePort",
              '--spring.cloud.openfeign.client.config.user-service.connectTimeout=3000',
              '--spring.cloud.openfeign.client.config.user-service.readTimeout=10000')
    $routeArguments = $javaBaseArguments + @($routeJar, "--server.port=$RouteServicePort") + $serviceFlags +
            @("--logging.file.name=$((Join-Path $outputDirectory 'route-service.log').Replace('\', '/'))")
    $userProcess = Start-NativeProcess -FilePath $javaPath -Arguments $userArguments `
            -WorkingDirectory (Split-Path -Parent $userJar) -Environment $serviceEnvironment
    $routeProcess = Start-NativeProcess -FilePath $javaPath -Arguments $routeArguments `
            -WorkingDirectory (Split-Path -Parent $routeJar) -Environment $serviceEnvironment
    $collectionProcess = Start-NativeProcess -FilePath $javaPath -Arguments $collectionArguments `
            -WorkingDirectory (Split-Path -Parent $collectionJar) -Environment $serviceEnvironment
    $processes.Add($userProcess)
    $processes.Add($routeProcess)
    $processes.Add($collectionProcess)
    Wait-TcpPort -Port $UserServicePort -TimeoutSeconds 150 -Process $userProcess -Name 'user-service'
    Wait-TcpPort -Port $RouteServicePort -TimeoutSeconds 150 -Process $routeProcess -Name 'route-service'
    Wait-TcpPort -Port $CollectionServicePort -TimeoutSeconds 150 -Process $collectionProcess -Name 'collection-service'

    $gatewayArguments = $javaBaseArguments + @(
        $gatewayJar,
        "--server.port=$GatewayPort",
        '--spring.cloud.nacos.discovery.enabled=false',
        '--spring.cloud.nacos.config.enabled=false',
        '--spring.cloud.nacos.config.import-check.enabled=false',
        '--spring.cloud.service-registry.auto-registration.enabled=false',
        '--spring.cloud.sentinel.eager=false',
        '--spring.cloud.sentinel.enabled=false',
        '--travel.http.idempotency.enabled=true',
        '--spring.main.banner-mode=off',
        '--spring.output.ansi.enabled=never',
        '--logging.level.root=WARN',
        '--logging.level.travel=INFO',
        '--logging.level.org.springframework.web=WARN',
        "--logging.file.name=$((Join-Path $outputDirectory 'gateway.log').Replace('\', '/'))",
        '--spring.cloud.gateway.routes[0].id=user-service',
        "--spring.cloud.gateway.routes[0].uri=http://127.0.0.1:$UserServicePort",
        '--spring.cloud.gateway.routes[0].predicates[0]=Path=/api/users/**',
        '--spring.cloud.gateway.routes[1].id=collection-service',
        "--spring.cloud.gateway.routes[1].uri=http://127.0.0.1:$CollectionServicePort",
        '--spring.cloud.gateway.routes[1].predicates[0]=Path=/api/v1/route-collections/**'
    )
    $gatewayProcess = Start-NativeProcess -FilePath $javaPath -Arguments $gatewayArguments `
            -WorkingDirectory (Split-Path -Parent $gatewayJar) -Environment @{ JWT_SECRET = $envValues['JWT_SECRET'] }
    $processes.Add($gatewayProcess)
    Wait-TcpPort -Port $GatewayPort -TimeoutSeconds 150 -Process $gatewayProcess -Name 'gateway'

    $roundDefinitions = [System.Collections.Generic.List[object]]::new()
    $roundDefinitions.Add([pscustomobject]@{ name = 'cold'; mode = 'cold' })
    for ($roundNumber = 1; $roundNumber -le $WarmupRuns; $roundNumber++) {
        $roundDefinitions.Add([pscustomobject]@{
            name = 'warmup-{0:D2}' -f $roundNumber
            mode = 'warmup'
        })
    }
    for ($roundNumber = 1; $roundNumber -le $SteadyStateRuns; $roundNumber++) {
        $roundDefinitions.Add([pscustomobject]@{
            name = 'steady-{0:D2}' -f $roundNumber
            mode = 'steady'
        })
    }
    $roundResults = [System.Collections.Generic.List[object]]::new()
    $collectionMetricsUri = "http://127.0.0.1:$CollectionServicePort/api/actuator/metrics/travel.performance.stage"
    $previousPassword = $env:TEST_DATA_USER_PASSWORD
    try {
        $env:TEST_DATA_USER_PASSWORD = $Password
        foreach ($roundDefinition in $roundDefinitions) {
            $roundDirectory = Join-Path $outputDirectory (Join-Path 'rounds' $roundDefinition.name)
            $beforeMetrics = Get-StageMetricSnapshot -MetricsUri $collectionMetricsUri
            & $jmeterScript -Gateway "http://127.0.0.1:$GatewayPort" -Username $Username `
                    -RouteId $RouteId -Threads $Threads -JMeterPath $JMeterPath `
                    -OutputDirectory $roundDirectory -RunMode $roundDefinition.mode

            $roundSummaryPath = Join-Path $roundDirectory 'run-summary.json'
            if (-not (Test-Path -LiteralPath $roundSummaryPath -PathType Leaf)) {
                throw "Round summary was not created: $roundSummaryPath"
            }
            $roundSummary = Get-Content -LiteralPath $roundSummaryPath -Raw | ConvertFrom-Json
            $duplicateOutcomes = [int]$roundSummary.idempotencyOutcome.replayed +
                    [int]$roundSummary.idempotencyOutcome.conflict
            if ([int]$roundSummary.idempotencyOutcome.original -ne 1 -or
                    $duplicateOutcomes -ne ($Threads - 1) -or
                    [int]$roundSummary.idempotencyOutcome.unexpected -ne 0 -or
                    [int]$roundSummary.unexpectedHttp -ne 0 -or
                    [int]$roundSummary.mainFailedSamples -ne 0) {
                throw "Round $($roundDefinition.name) did not satisfy the one-original/no-duplicate-side-effect contract."
            }

            $afterMetrics = Get-StageMetricSnapshot -MetricsUri $collectionMetricsUri
            $stageMetrics = Get-StageMetricDelta -Before $beforeMetrics -After $afterMetrics
            $stageMetricsPath = Join-Path $roundDirectory 'stage-metrics.json'
            [ordered]@{
                generatedAt = (Get-Date).ToString('o')
                before = $beforeMetrics
                after = $afterMetrics
                deltas = $stageMetrics
            } | ConvertTo-Json -Depth 7 |
                    Set-Content -LiteralPath $stageMetricsPath -Encoding UTF8
            $roundSummary | Add-Member -NotePropertyName stageMetricsFile -NotePropertyValue $stageMetricsPath
            $roundResults.Add([pscustomobject][ordered]@{
                name = $roundDefinition.name
                mode = $roundDefinition.mode
                summary = $roundSummary
                stageMetrics = $stageMetrics
            })
            if ($roundDefinition.name -ne $roundDefinitions[$roundDefinitions.Count - 1].name -and
                    $InterRoundDelaySeconds -gt 0) {
                Start-Sleep -Seconds $InterRoundDelaySeconds
            }
        }
    }
    finally {
        if ($null -eq $previousPassword) {
            Remove-Item Env:TEST_DATA_USER_PASSWORD -ErrorAction SilentlyContinue
        }
        else {
            $env:TEST_DATA_USER_PASSWORD = $previousPassword
        }
    }

    $countResult = Invoke-MySql -MySqlPath $mysqlPath -Database $database -Sql (
            "SELECT COUNT(*) FROM user_collection WHERE user_id=$userId AND item_id=$RouteId " +
            "AND item_type='route' AND collection_type='collect';")
    if ($countResult.ExitCode -ne 0 -or [int]$countResult.Stdout -ne 1) {
        throw "Final collection row count is not 1: $($countResult.Stdout) $($countResult.Stderr)"
    }
    $coldRound = $roundResults | Where-Object { $_.mode -eq 'cold' } | Select-Object -First 1
    $warmupRounds = @($roundResults | Where-Object { $_.mode -eq 'warmup' })
    $steadyRounds = @($roundResults | Where-Object { $_.mode -eq 'steady' })
    $steadyAverageValues = [double[]]@($steadyRounds | ForEach-Object { [double]$_.summary.averageMs })
    $steadyP95Values = [double[]]@($steadyRounds | ForEach-Object { [double]$_.summary.p95Ms })
    $steadyP99Values = [double[]]@($steadyRounds | ForEach-Object { [double]$_.summary.p99Ms })
    $steadyThroughputValues = [double[]]@($steadyRounds | ForEach-Object { [double]$_.summary.throughputPerSecond })
    $p95CoefficientOfVariation = Get-CoefficientOfVariationPercent -Values $steadyP95Values
    $steadyAggregate = [ordered]@{
        rounds = $steadyRounds.Count
        medianAverageMs = [Math]::Round((Get-Median -Values $steadyAverageValues), 2)
        medianP95Ms = [Math]::Round((Get-Median -Values $steadyP95Values), 2)
        medianP99Ms = [Math]::Round((Get-Median -Values $steadyP99Values), 2)
        medianThroughputPerSecond = [Math]::Round((Get-Median -Values $steadyThroughputValues), 2)
        p95CoefficientOfVariationPercent = $p95CoefficientOfVariation
    }
    $acceptancePassed = $steadyRounds.Count -eq $SteadyStateRuns -and
            $p95CoefficientOfVariation -le 10.0
    $result = [ordered]@{
        generatedAt = (Get-Date).ToString('o')
        username = $Username
        routeId = $RouteId
        threads = $Threads
        gatewayPort = $GatewayPort
        result = if ($acceptancePassed) { 'PASS' } else { 'FAIL' }
        baselineCollectionRows = $baselineRows
        finalCollectionRows = [int]$countResult.Stdout
        coldRound = $coldRound
        warmupRounds = $warmupRounds
        steadyRounds = $steadyRounds
        steadyAggregate = $steadyAggregate
        acceptance = [ordered]@{
            requiredSteadyRounds = $SteadyStateRuns
            excludedWarmupRounds = $WarmupRuns
            p95CoefficientOfVariationLimitPercent = 10.0
            passed = $acceptancePassed
        }
        baselineRestored = $false
        cleanupError = $null
        logDirectory = $outputDirectory
    }
    $summaryRecord = $result
}
catch {
    $summaryRecord = [ordered]@{
        generatedAt = (Get-Date).ToString('o')
        username = $Username
        routeId = $RouteId
        result = 'FAIL'
        baselineCollectionRows = $baselineRows
        baselineRestored = $false
        cleanupError = $null
        errorType = $_.Exception.GetType().FullName
        errorMessage = $_.Exception.Message
        logDirectory = $outputDirectory
    }
    throw
}
finally {
    foreach ($process in $processes) {
        Stop-ProcessSafely -Process $process
    }
    foreach ($port in $GatewayPort, $CollectionServicePort, $RouteServicePort, $UserServicePort, $RedisPort) {
        Stop-PortOwnerSafely -Port $port
    }
    if ($baselinePrepared) {
        $restoreResult = Invoke-MySql -MySqlPath $mysqlPath -Database $database -Sql (
                "DELETE FROM user_collection WHERE $targetPredicate; " +
                "INSERT INTO user_collection SELECT * FROM $backupTable; " +
                "DROP TABLE $backupTable; " +
                "SELECT COUNT(*) FROM user_collection WHERE $targetPredicate;")
        if ($restoreResult.ExitCode -eq 0 -and
                $restoreResult.Stdout -match '^\d+$' -and
                [int]$restoreResult.Stdout -eq $baselineRows) {
            $baselineRestored = $true
        }
        else {
            $cleanupError = "Could not restore collection baseline: $($restoreResult.Stderr)"
            Write-Warning $cleanupError
        }
    }
    if ($null -ne $summaryRecord) {
        $summaryRecord.baselineRestored = $baselineRestored
        if ($null -ne $cleanupError) {
            $summaryRecord.result = 'FAIL'
            $summaryRecord.cleanupError = $cleanupError
        }
        $summaryRecord | ConvertTo-Json -Depth 4 |
                Set-Content -LiteralPath (Join-Path $outputDirectory 'run-summary.json') -Encoding UTF8
    }
}

if ($null -ne $cleanupError) {
    throw $cleanupError
}
if ($null -ne $result -and $result.result -ne 'PASS') {
    throw "Cold/steady latency acceptance failed. Check $(Join-Path $outputDirectory 'run-summary.json')."
}
[pscustomobject]$result | Format-List
