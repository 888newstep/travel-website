[CmdletBinding()]
param(
    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$Username = 'zhangsan',

    [Parameter()]
    [string]$Password = $env:TEST_DATA_USER_PASSWORD,

    [Parameter()]
    [ValidateRange(1, [int]::MaxValue)]
    [int]$RouteId = 1,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$RedisPort = 16379,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$UserServicePort = 18091,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$CollectionServicePort = 18094,

    [Parameter()]
    [string]$RedisServerPath,

    [Parameter()]
    [string]$MySqlPath = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',

    [Parameter()]
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { '127.0.0.1' }),

    [Parameter()]
    [ValidateRange(1, 65535)]
    [int]$DbPort = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),

    [Parameter()]
    [string]$DbName = $(if ($env:DB_NAME) { $env:DB_NAME } else { 'travel_website' }),

    [Parameter()]
    [string]$DbUsername = $env:DB_USERNAME,

    [Parameter()]
    [string]$DbPassword = $env:DB_PASSWORD,

    [Parameter()]
    [ValidateRange(30, 300)]
    [int]$StartupTimeoutSeconds = 150
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

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

function Resolve-RedisServer {
    param([string]$ExplicitPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath -PathType Leaf) {
            return (Resolve-Path -LiteralPath $ExplicitPath).Path
        }
        throw "Redis server does not exist: $ExplicitPath"
    }

    $service = Get-CimInstance Win32_Service -Filter "Name='redis6379'" -ErrorAction SilentlyContinue
    if ($null -ne $service) {
        $match = [regex]::Match($service.PathName, '^"([^"]+)"|^([^\s]+)')
        $servicePath = if ($match.Groups[1].Success) {
            $match.Groups[1].Value
        } else {
            $match.Groups[2].Value
        }
        if (Test-Path -LiteralPath $servicePath -PathType Leaf) {
            return (Resolve-Path -LiteralPath $servicePath).Path
        }
    }

    $command = Get-Command 'redis-server.exe' -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }
    throw 'redis-server.exe was not found. Pass -RedisServerPath.'
}

function Resolve-JavaPath {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    $command = Get-Command 'java.exe' -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw 'java.exe was not found.'
    }
    return $command.Source
}

function Test-TcpPort {
    param([int]$Port)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $result = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne(250)) {
            return $false
        }
        $client.EndConnect($result)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Wait-TcpPort {
    param(
        [int]$Port,
        [bool]$ExpectedOpen,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$Process,
        [string]$Name,
        [string]$LogPath
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($null -ne $Process) {
            $Process.Refresh()
            if ($Process.HasExited -and $ExpectedOpen) {
                $tail = if (Test-Path -LiteralPath $LogPath) {
                    (Get-Content -LiteralPath $LogPath -Tail 100) -join [Environment]::NewLine
                } else {
                    'No process log was created.'
                }
                throw "$Name exited with code $($Process.ExitCode). $tail"
            }
        }
        if ((Test-TcpPort -Port $Port) -eq $ExpectedOpen) {
            return
        }
        Start-Sleep -Milliseconds 300
    }
    throw "$Name did not reach expected port state on $Port."
}

function Get-PortOwner {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($null -eq $connection) {
        return $null
    }
    return [int]$connection.OwningProcess
}

function Set-TestStage {
    param([string]$Name)

    $script:CurrentStage = $Name
    $line = '{0} [{1}]' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff'), $Name
    Write-Host $line
    if (-not [string]::IsNullOrWhiteSpace($script:OutputDirectory) -and
        (Test-Path -LiteralPath $script:OutputDirectory -PathType Container)) {
        Add-Content -LiteralPath (Join-Path $script:OutputDirectory 'execution.log') `
            -Value $line -Encoding UTF8
    }
}

function Stop-ProcessSafely {
    param([int]$ProcessId)

    if ($ProcessId -le 0) {
        return
    }
    try {
        $process = [System.Diagnostics.Process]::GetProcessById($ProcessId)
        $process.Kill()
        $process.WaitForExit(10000) | Out-Null
    }
    catch {
        # The process may already be stopped.
    }
}

function Invoke-MySql {
    param([string]$Sql)

    $arguments = @(
        "--host=$DbHost",
        "--port=$DbPort",
        "--user=$DbUsername",
        "--database=$DbName",
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
    $startInfo.EnvironmentVariables['MYSQL_PWD'] = $DbPassword
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

function Get-BusinessRowCount {
    $sql = "SELECT COUNT(*) FROM user_collection WHERE user_id=$script:UserId " +
            "AND item_id=$RouteId AND item_type='route' AND collection_type='collect';"
    $result = Invoke-MySql -Sql $sql
    if ($result.ExitCode -ne 0) {
        throw "MySQL count failed: $($result.Stderr)"
    }
    return [int]$result.Stdout
}

function Start-DisposableRedis {
    $arguments = @(
        '--port', [string]$RedisPort,
        '--bind', '127.0.0.1',
        '--protected-mode', 'yes',
        '--requirepass', $script:DisposableRedisPassword,
        '--databases', '16',
        '--appendonly', 'no',
        '--save', '',
        '--dir', $script:OutputDirectory,
        '--dbfilename', 'outage.rdb',
        '--logfile', 'redis.log'
    )
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $script:ResolvedRedisServer
    $startInfo.Arguments = Join-NativeArguments $arguments
    $startInfo.WorkingDirectory = $script:OutputDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $process = [System.Diagnostics.Process]::Start($startInfo)
    Wait-TcpPort -Port $RedisPort -ExpectedOpen $true -TimeoutSeconds 15 `
        -Process $process -Name 'Disposable Redis' -LogPath (Join-Path $script:OutputDirectory 'redis.log')
    return $process
}

function Start-ServiceProcess {
    param(
        [string]$Name,
        [int]$Port,
        [string]$JarPath,
        [string]$LogPath
    )

    $arguments = @(
        '-Xms128m',
        '-Xmx512m',
        '-Dfile.encoding=UTF-8',
        '-jar',
        $JarPath,
        "--server.port=$Port",
        '--spring.cloud.nacos.discovery.enabled=false',
        '--spring.cloud.nacos.config.enabled=false',
        '--spring.cloud.nacos.config.import-check.enabled=false',
        '--spring.cloud.service-registry.auto-registration.enabled=false',
        '--spring.rabbitmq.listener.simple.auto-startup=false',
        '--spring.rabbitmq.dynamic=false',
        '--spring.sql.init.mode=never',
        '--spring.main.banner-mode=off',
        '--spring.output.ansi.enabled=never',
        "--logging.file.name=$($LogPath.Replace('\', '/'))"
    )
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $script:JavaPath
    $startInfo.Arguments = Join-NativeArguments $arguments
    $startInfo.WorkingDirectory = Split-Path -Parent $JarPath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.EnvironmentVariables['REDIS_HOST'] = '127.0.0.1'
    $startInfo.EnvironmentVariables['REDIS_PORT'] = [string]$RedisPort
    $startInfo.EnvironmentVariables['REDIS_PASSWORD'] = $script:DisposableRedisPassword
    $startInfo.EnvironmentVariables['JWT_SECRET'] = $script:JwtSecret
    $startInfo.EnvironmentVariables['RABBITMQ_HOST'] = '127.0.0.1'
    $startInfo.EnvironmentVariables['RABBITMQ_PORT'] = '1'
    $startInfo.EnvironmentVariables['DB_HOST'] = $DbHost
    $startInfo.EnvironmentVariables['DB_PORT'] = [string]$DbPort
    $startInfo.EnvironmentVariables['DB_NAME'] = $DbName
    $startInfo.EnvironmentVariables['DB_USERNAME'] = $DbUsername
    $startInfo.EnvironmentVariables['DB_PASSWORD'] = $DbPassword
    return [System.Diagnostics.Process]::Start($startInfo)
}

function Send-JsonRequest {
    param(
        [System.Net.Http.HttpClient]$Client,
        [System.Net.Http.HttpMethod]$Method,
        [string]$Uri,
        [AllowNull()][object]$Body,
        [AllowNull()][string]$IdempotencyKey
    )

    $request = [System.Net.Http.HttpRequestMessage]::new($Method, $Uri)
    try {
        if (-not [string]::IsNullOrWhiteSpace($IdempotencyKey)) {
            $request.Headers.Add('Idempotency-Key', $IdempotencyKey)
        }
        if ($null -ne $Body) {
            $request.Content = [System.Net.Http.StringContent]::new(
                    [string]$Body,
                    [System.Text.Encoding]::UTF8,
                    'application/json')
        }
        $response = $Client.SendAsync($request).GetAwaiter().GetResult()
        try {
            return [pscustomobject]@{
                StatusCode = [int]$response.StatusCode
                Body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            }
        }
        finally {
            $response.Dispose()
        }
    }
    finally {
        $request.Dispose()
    }
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    throw 'The test password is blank. Set TEST_DATA_USER_PASSWORD or pass -Password.'
}
if ([string]::IsNullOrWhiteSpace($DbUsername) -or [string]::IsNullOrWhiteSpace($DbPassword)) {
    throw 'DB_USERNAME and DB_PASSWORD must be configured.'
}
if (-not (Test-Path -LiteralPath $MySqlPath -PathType Leaf)) {
    throw "mysql.exe does not exist: $MySqlPath"
}
$uniquePorts = @($RedisPort, $UserServicePort, $CollectionServicePort) | Select-Object -Unique
if (@($uniquePorts).Count -ne 3) {
    throw 'Redis, user-service, and collection-service ports must be different.'
}
foreach ($port in $RedisPort, $UserServicePort, $CollectionServicePort) {
    if (Test-TcpPort -Port $port) {
        throw "Port is already in use: $port"
    }
}

$script:ResolvedRedisServer = Resolve-RedisServer -ExplicitPath $RedisServerPath
$script:JavaPath = Resolve-JavaPath
$script:DisposableRedisPassword = 'outage-' + [guid]::NewGuid().ToString('N')
$script:JwtSecret = 'outage-jwt-' + [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$repositoryRoot = (Resolve-Path (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) '..\..')).Path
$script:OutputDirectory = Join-Path $repositoryRoot ('run-logs\redis-outage\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $script:OutputDirectory -Force | Out-Null

$userJar = (Get-ChildItem -LiteralPath (Join-Path $repositoryRoot 'backend\user-service\target') `
        -Filter 'user-service-*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } |
        Select-Object -First 1).FullName
$collectionJar = (Get-ChildItem -LiteralPath (Join-Path $repositoryRoot 'backend\collection-service\target') `
        -Filter 'collection-service-*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } |
        Select-Object -First 1).FullName
if ([string]::IsNullOrWhiteSpace($userJar) -or [string]::IsNullOrWhiteSpace($collectionJar)) {
    throw 'Service jars were not found. Package user-service and collection-service first.'
}

$trackedProcessIds = [System.Collections.Generic.HashSet[int]]::new()
$redisProcess = $null
$httpClient = $null
$scriptFailure = $null
$script:CurrentStage = 'initializing'
try {
    Set-TestStage -Name 'preparing database fixture'
    $userLookup = Invoke-MySql -Sql (
            "SELECT id FROM user WHERE username='" + $Username.Replace("'", "''") +
            "' OR phone='" + $Username.Replace("'", "''") + "' ORDER BY id LIMIT 1;")
    if ($userLookup.ExitCode -ne 0 -or $userLookup.Stdout -notmatch '^\d+$') {
        throw "Test user was not found: $Username"
    }
    $script:UserId = [int]$userLookup.Stdout

    $baselineRows = Get-BusinessRowCount
    if ($baselineRows -eq 0) {
        $fixture = Invoke-MySql -Sql (
                "INSERT INTO user_collection(user_id,item_id,item_type,collection_type) " +
                "VALUES($script:UserId,$RouteId,'route','collect');")
        if ($fixture.ExitCode -ne 0) {
            throw "Failed to create test fixture: $($fixture.Stderr)"
        }
        $baselineRows = Get-BusinessRowCount
    }
    if ($baselineRows -ne 1) {
        throw "Expected one baseline collection row, actual=$baselineRows"
    }

    Set-TestStage -Name 'starting disposable Redis'
    $redisProcess = Start-DisposableRedis
    $trackedProcessIds.Add($redisProcess.Id) | Out-Null
    $userLog = Join-Path $script:OutputDirectory 'user-service.log'
    $collectionLog = Join-Path $script:OutputDirectory 'collection-service.log'
    Set-TestStage -Name 'starting application services'
    $userProcess = Start-ServiceProcess -Name 'user-service' -Port $UserServicePort `
        -JarPath $userJar -LogPath $userLog
    $collectionProcess = Start-ServiceProcess -Name 'collection-service' -Port $CollectionServicePort `
        -JarPath $collectionJar -LogPath $collectionLog
    $trackedProcessIds.Add($userProcess.Id) | Out-Null
    $trackedProcessIds.Add($collectionProcess.Id) | Out-Null
    Set-TestStage -Name 'waiting for application services'
    Wait-TcpPort -Port $UserServicePort -ExpectedOpen $true -TimeoutSeconds $StartupTimeoutSeconds `
        -Process $userProcess -Name 'user-service' -LogPath $userLog
    Wait-TcpPort -Port $CollectionServicePort -ExpectedOpen $true -TimeoutSeconds $StartupTimeoutSeconds `
        -Process $collectionProcess -Name 'collection-service' -LogPath $collectionLog
    Set-TestStage -Name 'tracking service processes'
    foreach ($port in $UserServicePort, $CollectionServicePort) {
        $owner = Get-PortOwner -Port $port
        if ($null -ne $owner) {
            $trackedProcessIds.Add($owner) | Out-Null
        }
    }

    Set-TestStage -Name 'authenticating test user'
    $httpClient = [System.Net.Http.HttpClient]::new()
    $httpClient.Timeout = [TimeSpan]::FromSeconds(45)
    $loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
    $login = Send-JsonRequest -Client $httpClient -Method ([System.Net.Http.HttpMethod]::Post) `
        -Uri "http://127.0.0.1:$UserServicePort/api/users/login" `
        -Body $loginBody `
        -IdempotencyKey $null
    if ($login.StatusCode -ne 200) {
        throw "Login failed: status=$($login.StatusCode), body=$($login.Body)"
    }
    $token = ($login.Body | ConvertFrom-Json).data.token
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw 'Login token is blank.'
    }
    $httpClient.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $token)

    Set-TestStage -Name 'stopping disposable Redis'
    $redisProcess.Kill()
    $redisProcess.WaitForExit(10000) | Out-Null
    Wait-TcpPort -Port $RedisPort -ExpectedOpen $false -TimeoutSeconds 10 `
        -Process $null -Name 'Disposable Redis' -LogPath ''

    Set-TestStage -Name 'verifying fail-closed response'
    $outageResponse = Send-JsonRequest -Client $httpClient -Method ([System.Net.Http.HttpMethod]::Post) `
        -Uri "http://127.0.0.1:$CollectionServicePort/api/v1/route-collections/toggle" `
        -Body ("{`"routeId`":$RouteId}") `
        -IdempotencyKey ('redis-outage-' + [guid]::NewGuid().ToString('N'))
    $rowsDuringOutage = Get-BusinessRowCount
    $outagePayload = $outageResponse.Body | ConvertFrom-Json
    if ($outageResponse.StatusCode -ne 503 -or [int]$outagePayload.code -ne 503) {
        throw "Expected fail-closed HTTP 503, status=$($outageResponse.StatusCode), body=$($outageResponse.Body)"
    }
    if ($rowsDuringOutage -ne $baselineRows) {
        throw "Database changed during Redis outage: before=$baselineRows, during=$rowsDuringOutage"
    }

    Set-TestStage -Name 'restarting disposable Redis'
    $redisProcess = Start-DisposableRedis
    $trackedProcessIds.Add($redisProcess.Id) | Out-Null
    Set-TestStage -Name 'verifying Redis recovery'
    $recovered = $false
    $recoveryStatus = 0
    $lastRecoveryError = $null
    for ($attempt = 1; $attempt -le 20; $attempt++) {
        Start-Sleep -Seconds 1
        try {
            $check = Send-JsonRequest -Client $httpClient -Method ([System.Net.Http.HttpMethod]::Get) `
                -Uri "http://127.0.0.1:$CollectionServicePort/api/v1/route-collections/check?routeId=$RouteId" `
                -Body $null -IdempotencyKey $null
            $recoveryStatus = $check.StatusCode
            if ($check.StatusCode -eq 200 -and ($check.Body | ConvertFrom-Json).data -eq $true) {
                $recovered = $true
                break
            }
        }
        catch {
            $lastRecoveryError = '{0}: {1}' -f $_.Exception.GetType().FullName, $_.Exception.Message
        }
    }
    if (-not $recovered) {
        throw "Service did not recover after Redis restart, lastStatus=$recoveryStatus, lastError=$lastRecoveryError"
    }

    Set-TestStage -Name 'verifying database unique constraint'
    $duplicate = Invoke-MySql -Sql (
            "INSERT INTO user_collection(user_id,item_id,item_type,collection_type) " +
            "VALUES($script:UserId,$RouteId,'route','collect');")
    $rowsAfterDuplicate = Get-BusinessRowCount
    if ($duplicate.ExitCode -eq 0 -or $duplicate.Stderr -notmatch '1062|Duplicate entry' -or
        $rowsAfterDuplicate -ne 1) {
        throw "Unique index validation failed: exit=$($duplicate.ExitCode), rows=$rowsAfterDuplicate, error=$($duplicate.Stderr)"
    }

    Set-TestStage -Name 'writing successful summary'
    $summary = [ordered]@{
        generatedAt = (Get-Date).ToString('o')
        username = $Username
        userId = $script:UserId
        routeId = $RouteId
        redisPort = $RedisPort
        outageHttpStatus = $outageResponse.StatusCode
        outageBodyCode = [int]$outagePayload.code
        rowsBeforeOutage = $baselineRows
        rowsDuringOutage = $rowsDuringOutage
        recoveredAfterRestart = $recovered
        duplicateInsertExitCode = $duplicate.ExitCode
        duplicateErrorCode = 1062
        rowsAfterDuplicateAttempt = $rowsAfterDuplicate
        result = 'PASS'
        logDirectory = $script:OutputDirectory
    }
    $summary | ConvertTo-Json -Depth 4 |
            Set-Content -LiteralPath (Join-Path $script:OutputDirectory 'run-summary.json') -Encoding UTF8
    [pscustomobject]$summary | Format-List
}
catch {
    $scriptFailure = $_
    $failureSummary = [ordered]@{
        generatedAt = (Get-Date).ToString('o')
        username = $Username
        routeId = $RouteId
        redisPort = $RedisPort
        userServicePort = $UserServicePort
        collectionServicePort = $CollectionServicePort
        stage = $script:CurrentStage
        result = 'FAIL'
        errorType = $_.Exception.GetType().FullName
        errorMessage = $_.Exception.Message
        scriptStackTrace = $_.ScriptStackTrace
        logDirectory = $script:OutputDirectory
    }
    try {
        $failureSummary | ConvertTo-Json -Depth 4 |
                Set-Content -LiteralPath (Join-Path $script:OutputDirectory 'run-summary.json') -Encoding UTF8
    }
    catch {
        Write-Warning "Failed to write failure summary: $($_.Exception.Message)"
    }
}
finally {
    if ($null -ne $httpClient) {
        $httpClient.Dispose()
    }
    foreach ($port in $CollectionServicePort, $UserServicePort, $RedisPort) {
        try {
            $owner = Get-PortOwner -Port $port
            if ($null -ne $owner) {
                $trackedProcessIds.Add($owner) | Out-Null
            }
        }
        catch {
            Write-Warning "Failed to resolve owner for port ${port}: $($_.Exception.Message)"
        }
    }
    foreach ($processId in $trackedProcessIds) {
        Stop-ProcessSafely -ProcessId $processId
    }
}
if ($null -ne $scriptFailure) {
    throw $scriptFailure
}
