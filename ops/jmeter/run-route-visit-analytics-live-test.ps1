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
    [ValidateRange(1024, 65535)]
    [int]$UserServicePort = 18091,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$RouteServicePort = 18093,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$RedisPort = 16381,

    [Parameter()]
    [string]$RedisServerPath = 'E:\Redis\redis-7.2.4\redis-server.exe'
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

function Start-NativeProcess {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [hashtable]$Environment
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.Arguments = (($Arguments | ForEach-Object { ConvertTo-NativeArgument $_ }) -join ' ')
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
        $Process.Refresh()
        if ($Process.HasExited) {
            throw "$Name exited with code $($Process.ExitCode)."
        }
        if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
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
        $Process.Refresh()
        if (-not $Process.HasExited) {
            Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
            Wait-Process -Id $Process.Id -Timeout 10 -ErrorAction SilentlyContinue
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
    if ($null -eq $process) {
        return
    }
    $isOwnedJava = $process.Name -eq 'java.exe' -and
            $process.CommandLine -match 'IdeaProjects\\travel\\backend\\(user-service|route-service)\\target'
    $isOwnedRedis = $process.Name -eq 'redis-server.exe' -and $Port -eq $RedisPort
    if ($isOwnedJava -or $isOwnedRedis) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
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
    $startInfo.Arguments = (($arguments | ForEach-Object { ConvertTo-NativeArgument $_ }) -join ' ')
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.EnvironmentVariables['MYSQL_PWD'] = $Database.Password
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "MySQL command failed: $($stderr.Trim())"
    }
    return $stdout.Trim()
}

function Find-ServiceJar {
    param(
        [string]$Directory,
        [string]$Pattern
    )

    $jar = Get-ChildItem -LiteralPath $Directory -Filter $Pattern -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike '*.original' } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
    if ($null -eq $jar) {
        throw "Service jar was not found in $Directory."
    }
    return $jar.FullName
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    throw 'The business test password is blank. Pass -Password or set TEST_DATA_USER_PASSWORD.'
}
if (@($UserServicePort, $RouteServicePort, $RedisPort) | Group-Object | Where-Object Count -gt 1) {
    throw 'UserServicePort, RouteServicePort and RedisPort must be different.'
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
if ([string]::IsNullOrWhiteSpace($envValues['JWT_SECRET'])) {
    throw 'JWT_SECRET is missing from deploy/.env.'
}

$mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
if ($null -eq $mysqlCommand) {
    throw 'mysql.exe was not found.'
}
$javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
if ($null -eq $javaCommand) {
    throw 'java.exe was not found.'
}

$userJar = Find-ServiceJar -Directory (Join-Path $repositoryRoot 'backend\user-service\target') `
        -Pattern 'user-service-*.jar'
$routeJar = Find-ServiceJar -Directory (Join-Path $repositoryRoot 'backend\route-service\target') `
        -Pattern 'route-service-*.jar'
$outputDirectory = Join-Path $repositoryRoot `
        ('run-logs\route-visit\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$baselineSql = "SELECT COALESCE(r.view_count,0),COALESCE(MAX(rv.id),0),COUNT(rv.id) " +
        "FROM route r LEFT JOIN route_visit rv ON rv.route_id=r.id " +
        "WHERE r.id=$RouteId GROUP BY r.id,r.view_count;"
$baseline = (Invoke-MySql -MySqlPath $mysqlCommand.Source -Database $database -Sql $baselineSql).Split([char]9)
if ($baseline.Count -ne 3) {
    throw "Route $RouteId does not exist or its baseline could not be captured."
}
$baselineViewCount = [long]$baseline[0]
$baselineMaxVisitId = [long]$baseline[1]
$baselineVisitRows = [long]$baseline[2]
$redisPassword = 'isolated-' + [guid]::NewGuid().ToString('N')

$serviceEnvironment = @{
    DB_HOST = $database.Host
    DB_PORT = $database.Port
    DB_NAME = $database.Name
    DB_USERNAME = $database.Username
    DB_PASSWORD = $database.Password
    REDIS_HOST = '127.0.0.1'
    REDIS_PORT = $RedisPort
    REDIS_PASSWORD = $redisPassword
    JWT_SECRET = $envValues['JWT_SECRET']
    ROUTE_ANALYTICS_HASH_SALT = 'live-' + [guid]::NewGuid().ToString('N')
    RABBITMQ_HOST = '127.0.0.1'
    RABBITMQ_PORT = 1
}
$serviceFlags = @(
    '--spring.cloud.nacos.discovery.enabled=false',
    '--spring.cloud.nacos.config.enabled=false',
    '--spring.cloud.nacos.config.import-check.enabled=false',
    '--spring.cloud.service-registry.auto-registration.enabled=false',
    '--spring.rabbitmq.listener.simple.auto-startup=false',
    '--spring.rabbitmq.dynamic=false',
    '--spring.sql.init.mode=never',
    '--spring.main.banner-mode=off',
    '--spring.output.ansi.enabled=never'
)
$javaBaseArguments = @('-Xms128m', '-Xmx512m', '-Dfile.encoding=UTF-8', '-jar')
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
$summary = $null
$failure = $null

try {
    foreach ($port in $UserServicePort, $RouteServicePort, $RedisPort) {
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
            throw "Port $port is already in use."
        }
    }

    $redisArguments = @(
        '--port', [string]$RedisPort,
        '--bind', '127.0.0.1',
        '--protected-mode', 'yes',
        '--requirepass', $redisPassword,
        '--databases', '16',
        '--appendonly', 'no',
        '--save', '',
        '--dir', $outputDirectory,
        '--dbfilename', 'route-visit.rdb',
        '--logfile', 'redis.log'
    )
    $redisProcess = Start-NativeProcess -FilePath $RedisServerPath -Arguments $redisArguments `
            -WorkingDirectory $outputDirectory -Environment @{}
    $processes.Add($redisProcess)
    Wait-TcpPort -Port $RedisPort -TimeoutSeconds 20 -Process $redisProcess -Name 'isolated Redis'

    $userArguments = $javaBaseArguments + @($userJar, "--server.port=$UserServicePort") + $serviceFlags +
            @("--logging.file.name=$((Join-Path $outputDirectory 'user-service.log').Replace('\', '/'))")
    $routeArguments = $javaBaseArguments + @($routeJar, "--server.port=$RouteServicePort") + $serviceFlags +
            @("--logging.file.name=$((Join-Path $outputDirectory 'route-service.log').Replace('\', '/'))")
    $userProcess = Start-NativeProcess -FilePath $javaCommand.Source -Arguments $userArguments `
            -WorkingDirectory (Split-Path -Parent $userJar) -Environment $serviceEnvironment
    $routeProcess = Start-NativeProcess -FilePath $javaCommand.Source -Arguments $routeArguments `
            -WorkingDirectory (Split-Path -Parent $routeJar) -Environment $serviceEnvironment
    $processes.Add($userProcess)
    $processes.Add($routeProcess)
    Wait-TcpPort -Port $UserServicePort -TimeoutSeconds 150 -Process $userProcess -Name 'user-service'
    Wait-TcpPort -Port $RouteServicePort -TimeoutSeconds 150 -Process $routeProcess -Name 'route-service'

    $loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Method Post `
            -Uri "http://127.0.0.1:$UserServicePort/api/users/login" `
            -ContentType 'application/json' -Body $loginBody
    if (-not $login.success -or [string]::IsNullOrWhiteSpace($login.data.token)) {
        throw 'Login did not return a token.'
    }

    $authenticatedHeaders = @{
        Authorization = "Bearer $($login.data.token)"
        'User-Agent' = 'route-visit-live-authenticated'
    }
    foreach ($requestNumber in 1..3) {
        $detail = Invoke-RestMethod -Method Get `
                -Uri "http://127.0.0.1:$RouteServicePort/api/routes/$RouteId" `
                -Headers $authenticatedHeaders
        if (-not $detail.success) {
            throw "Authenticated route request $requestNumber failed."
        }
    }
    foreach ($userAgent in 'route-visit-live-anonymous-a', 'route-visit-live-anonymous-b') {
        $detail = Invoke-RestMethod -Method Get `
                -Uri "http://127.0.0.1:$RouteServicePort/api/routes/$RouteId" `
                -Headers @{ 'User-Agent' = $userAgent }
        if (-not $detail.success) {
            throw "Anonymous route request for $userAgent failed."
        }
    }

    $analytics = Invoke-RestMethod -Method Get `
            -Uri "http://127.0.0.1:$RouteServicePort/api/routes/$RouteId/analytics?days=1" `
            -Headers $authenticatedHeaders
    if (-not $analytics.success) {
        throw 'Analytics endpoint failed.'
    }

    $verificationSql = "SELECT r.view_count,COUNT(rv.id),COUNT(DISTINCT rv.visitor_hash)," +
            "MIN(CHAR_LENGTH(rv.visitor_hash)),MAX(CHAR_LENGTH(rv.visitor_hash))," +
            "SUM(rv.visitor_type='AUTHENTICATED'),SUM(rv.visitor_type='ANONYMOUS') " +
            "FROM route r JOIN route_visit rv " +
            "ON rv.route_id=r.id AND rv.id>$baselineMaxVisitId WHERE r.id=$RouteId GROUP BY r.view_count;"
    $databaseCheck = (Invoke-MySql -MySqlPath $mysqlCommand.Source -Database $database `
            -Sql $verificationSql).Split([char]9)
    if ($databaseCheck.Count -ne 7) {
        throw 'Database verification did not return the expected columns.'
    }
    $expectedViewCount = $baselineViewCount + 5
    if ([long]$databaseCheck[0] -ne $expectedViewCount -or
            [long]$databaseCheck[1] -ne 5 -or
            [long]$databaseCheck[2] -ne 3 -or
            [int]$databaseCheck[3] -ne 64 -or
            [int]$databaseCheck[4] -ne 64 -or
            [int]$databaseCheck[5] -ne 3 -or
            [int]$databaseCheck[6] -ne 2) {
        throw "Database assertions failed: $($databaseCheck -join ',')."
    }
    if ([long]$analytics.data.periodVisits -lt 5 -or [long]$analytics.data.uniqueVisitors -lt 3) {
        throw 'Analytics totals are below the visits created by this run.'
    }

    $sensitiveColumnSql = "SELECT COUNT(*) FROM information_schema.columns " +
            "WHERE table_schema=DATABASE() AND table_name='route_visit' " +
            "AND column_name IN ('ip','ip_address','user_agent','remote_addr');"
    $sensitiveColumns = [int](Invoke-MySql -MySqlPath $mysqlCommand.Source `
            -Database $database -Sql $sensitiveColumnSql)
    if ($sensitiveColumns -ne 0) {
        throw 'route_visit contains raw request identity columns.'
    }

    $summary = [ordered]@{
        generatedAt = (Get-Date).ToString('o')
        result = 'PASS'
        routeId = $RouteId
        requests = 5
        authenticatedRequests = 3
        anonymousRequests = 2
        insertedVisitRows = [int]$databaseCheck[1]
        uniqueVisitorHashes = [int]$databaseCheck[2]
        analyticsPeriodVisits = [long]$analytics.data.periodVisits
        analyticsUniqueVisitors = [long]$analytics.data.uniqueVisitors
        hashLength = [int]$databaseCheck[3]
        rawIdentityColumns = $sensitiveColumns
        baselineViewCount = $baselineViewCount
        observedViewCount = [long]$databaseCheck[0]
        cleanup = 'pending'
    }
}
catch {
    $failure = $_
    $summary = [ordered]@{
        generatedAt = (Get-Date).ToString('o')
        result = 'FAIL'
        routeId = $RouteId
        errorType = $_.Exception.GetType().FullName
        errorMessage = $_.Exception.Message
        cleanup = 'pending'
    }
}
finally {
    foreach ($process in $processes) {
        Stop-ProcessSafely -Process $process
    }
    foreach ($port in $UserServicePort, $RouteServicePort, $RedisPort) {
        Stop-PortOwnerSafely -Port $port
    }

    try {
        $cleanupSql = "DELETE FROM route_visit WHERE route_id=$RouteId AND id>$baselineMaxVisitId; " +
                "UPDATE route SET view_count=$baselineViewCount WHERE id=$RouteId;"
        Invoke-MySql -MySqlPath $mysqlCommand.Source -Database $database -Sql $cleanupSql | Out-Null
        $cleanupCheckSql = "SELECT view_count," +
                "(SELECT COUNT(*) FROM route_visit WHERE route_id=$RouteId) FROM route WHERE id=$RouteId;"
        $cleanupCheck = (Invoke-MySql -MySqlPath $mysqlCommand.Source -Database $database `
                -Sql $cleanupCheckSql).Split([char]9)
        $cleanupPassed = $cleanupCheck.Count -eq 2 -and
                [long]$cleanupCheck[0] -eq $baselineViewCount -and
                [long]$cleanupCheck[1] -eq $baselineVisitRows
        $summary.cleanup = if ($cleanupPassed) { 'PASS' } else { 'FAIL' }
    }
    catch {
        $summary.cleanup = 'FAIL'
    }

    $summary | ConvertTo-Json -Depth 5 |
            Set-Content -LiteralPath (Join-Path $outputDirectory 'run-summary.json') -Encoding UTF8
}

if ($null -ne $failure) {
    throw $failure
}
[pscustomobject]$summary | Format-List
