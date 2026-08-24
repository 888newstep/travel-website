[CmdletBinding()]
param(
    [Parameter()]
    [ValidateRange(1, 2000)]
    [int]$Threads = 100,

    [Parameter()]
    [ValidateRange(1, 65535)]
    [int]$ServicePort = 18093,

    [Parameter()]
    [string]$ServiceContextPath = '/api',

    [Parameter()]
    [ValidateRange(100, 120000)]
    [int]$ConnectTimeoutMs = 5000,

    [Parameter()]
    [ValidateRange(100, 300000)]
    [int]$ResponseTimeoutMs = 30000,

    [Parameter()]
    [ValidateRange(30, 300)]
    [int]$StartupTimeoutSeconds = 150,

    [Parameter()]
    [string]$DbHost,

    [Parameter()]
    [ValidateRange(0, 65535)]
    [int]$DbPort = 0,

    [Parameter()]
    [string]$DbName,

    [Parameter()]
    [string]$DbUsername,

    [Parameter()]
    [string]$DbPassword,

    [Parameter()]
    [string]$RedisHost,

    [Parameter()]
    [ValidateRange(0, 65535)]
    [int]$RedisPort = 0,

    [Parameter()]
    [string]$RedisPassword,

    [Parameter()]
    [ValidateRange(0, 15)]
    [int]$RedisDatabase = 5,

    [Parameter()]
    [switch]$UseExistingRedis,

    [Parameter()]
    [string]$RedisServerPath,

    [Parameter()]
    [ValidateRange(1024, 65535)]
    [int]$DisposableRedisPort = 16382,

    [Parameter()]
    [string]$EnvFile,

    [Parameter()]
    [string]$JMeterPath,

    [Parameter()]
    [string]$MySqlPath,

    [Parameter()]
    [string]$MavenWrapper
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

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

function Write-Utf8NoBom {
    param(
        [string]$Path,
        [AllowEmptyString()][string]$Value
    )

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Value, $encoding)
}

function Read-DotEnv {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $values
    }
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) {
            continue
        }
        $separator = $trimmed.IndexOf('=')
        if ($separator -le 0) {
            continue
        }
        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if ($value.Length -ge 2 -and
                (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        if (-not [string]::IsNullOrWhiteSpace($name)) {
            $values[$name] = $value
        }
    }
    return $values
}

function Resolve-TextSetting {
    param(
        [AllowNull()][string]$ExplicitValue,
        [string]$Name,
        [hashtable]$DotEnv,
        [AllowEmptyString()][string]$DefaultValue = ''
    )

    if (-not [string]::IsNullOrWhiteSpace($ExplicitValue)) {
        return $ExplicitValue
    }
    $environmentValue = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if (-not [string]::IsNullOrWhiteSpace($environmentValue)) {
        return $environmentValue
    }
    if ($DotEnv.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace($DotEnv[$Name])) {
        return [string]$DotEnv[$Name]
    }
    return $DefaultValue
}

function Resolve-IntegerSetting {
    param(
        [int]$ExplicitValue,
        [string]$Name,
        [hashtable]$DotEnv,
        [int]$DefaultValue
    )

    if ($ExplicitValue -gt 0) {
        return $ExplicitValue
    }
    $text = Resolve-TextSetting $null $Name $DotEnv
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $DefaultValue
    }
    $value = 0
    if (-not [int]::TryParse($text, [ref]$value) -or $value -lt 1 -or $value -gt 65535) {
        throw "$Name must be between 1 and 65535."
    }
    return $value
}

function Resolve-Executable {
    param(
        [AllowEmptyString()][string]$ExplicitPath,
        [string]$CommandName
    )

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (-not (Test-Path -LiteralPath $ExplicitPath -PathType Leaf)) {
            throw "Executable does not exist: $ExplicitPath"
        }
        return (Resolve-Path -LiteralPath $ExplicitPath).Path
    }
    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "$CommandName was not found. Pass an explicit path."
    }
    return $command.Source
}

function Resolve-JavaExecutable {
    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Process')
    if (-not [string]::IsNullOrWhiteSpace($javaHome)) {
        $candidate = Join-Path $javaHome 'bin\java.exe'
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    return Resolve-Executable $null 'java.exe'
}

function Resolve-JMeterHome {
    param([string]$ExplicitPath)

    $candidates = New-Object System.Collections.Generic.List[string]
    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath -PathType Container) {
            $candidates.Add($ExplicitPath)
        }
        elseif (Test-Path -LiteralPath $ExplicitPath -PathType Leaf) {
            $candidates.Add((Split-Path -Parent (Split-Path -Parent $ExplicitPath)))
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($env:JMETER_HOME)) {
        $candidates.Add($env:JMETER_HOME)
    }
    $command = Get-Command 'jmeter.bat' -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        $candidates.Add((Split-Path -Parent (Split-Path -Parent $command.Source)))
    }
    foreach ($candidate in $candidates) {
        $resolved = Resolve-Path -LiteralPath $candidate -ErrorAction SilentlyContinue
        if ($null -ne $resolved -and
                (Test-Path -LiteralPath (Join-Path $resolved.Path 'bin\ApacheJMeter.jar') -PathType Leaf)) {
            return $resolved.Path
        }
    }
    throw 'JMeter was not found. Set JMETER_HOME or pass -JMeterPath.'
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
    throw 'redis-server.exe was not found. Pass -RedisServerPath or use -UseExistingRedis.'
}

function Invoke-MySql {
    param([string]$Sql)

    $arguments = @(
        "--host=$script:ResolvedDbHost",
        "--port=$script:ResolvedDbPort",
        "--user=$script:ResolvedDbUsername",
        "--database=$script:ResolvedDbName",
        '--batch',
        '--raw',
        '--skip-column-names',
        '--default-character-set=utf8mb4',
        '--connect-timeout=5',
        "--execute=$Sql"
    )
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $script:ResolvedMySql
    $startInfo.Arguments = Join-NativeArguments $arguments
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.EnvironmentVariables['MYSQL_PWD'] = $script:ResolvedDbPassword
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "MySQL command failed: $($stderr.Trim())"
    }
    return $stdout.Trim()
}

function Invoke-MySqlScript {
    param([string]$Path)

    $normalizedPath = (Resolve-Path -LiteralPath $Path).Path.Replace('\', '/')
    Invoke-MySql "source $normalizedPath" | Out-Null
}

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    return [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function New-TestJwt {
    param(
        [string]$Secret,
        [int]$UserId
    )

    $issuedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $headerJson = '{"alg":"HS256","typ":"JWT"}'
    $payloadJson = [ordered]@{
        sub = 'TRAVEL-PLATFORM-USER'
        exp = $issuedAt + 1800
        iat = $issuedAt
        jti = [string]$UserId
        userId = $UserId
        userType = 1
    } | ConvertTo-Json -Compress
    $header = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($headerJson))
    $payload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payloadJson))
    $unsignedToken = "$header.$payload"
    $base64Secret = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Secret))
    $keyBytes = [Text.Encoding]::UTF8.GetBytes($base64Secret)
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    try {
        $hmac.Key = $keyBytes
        $signature = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsignedToken))
    }
    finally {
        $hmac.Dispose()
    }
    return "$unsignedToken.$(ConvertTo-Base64Url $signature)"
}

function Test-TcpPort {
    param([int]$Port)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne(500, $false)) {
            return $false
        }
        $client.EndConnect($asyncResult)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Wait-TcpPort {
    param(
        [System.Diagnostics.Process]$Process,
        [int]$Port,
        [int]$TimeoutSeconds,
        [string]$Name,
        [string]$LogPath
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($Process.HasExited) {
            $tail = if (Test-Path -LiteralPath $LogPath) {
                (Get-Content -LiteralPath $LogPath -Tail 80 -ErrorAction SilentlyContinue) -join [Environment]::NewLine
            } else { '' }
            throw "$Name exited during startup. $tail"
        }
        if (Test-TcpPort $Port) {
            return
        }
        Start-Sleep -Milliseconds 200
    }
    throw "$Name did not listen on port $Port within $TimeoutSeconds seconds."
}

function Start-DisposableRedis {
    param(
        [string]$Executable,
        [string]$Password,
        [string]$OutputDirectory,
        [string]$LogPath
    )

    $arguments = @(
        '--port', [string]$DisposableRedisPort,
        '--bind', '127.0.0.1',
        '--protected-mode', 'yes',
        '--requirepass', $Password,
        '--databases', '16',
        '--appendonly', 'no',
        '--save', '',
        '--dir', $OutputDirectory,
        '--dbfilename', 'route-optimization-test.rdb',
        '--logfile', $LogPath
    )
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $Executable
    $startInfo.Arguments = Join-NativeArguments $arguments
    $startInfo.WorkingDirectory = $OutputDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    return [System.Diagnostics.Process]::Start($startInfo)
}

function Start-RouteService {
    param(
        [string]$JarPath,
        [string]$JwtSecret,
        [string]$ProcessMarker,
        [string]$LogPath
    )

    $arguments = @(
        '-Xms128m',
        '-Xmx512m',
        '-Dfile.encoding=UTF-8',
        $ProcessMarker,
        '-jar',
        $JarPath,
        "--server.port=$ServicePort",
        "--server.servlet.context-path=$ServiceContextPath",
        '--spring.cloud.nacos.discovery.enabled=false',
        '--spring.cloud.nacos.config.enabled=false',
        '--spring.cloud.nacos.config.import-check.enabled=false',
        '--spring.cloud.service-registry.auto-registration.enabled=false',
        '--spring.cloud.sentinel.enabled=false',
        '--seata.enabled=false',
        '--spring.rabbitmq.dynamic=false',
        '--spring.rabbitmq.listener.simple.auto-startup=false',
        '--spring.rabbitmq.host=127.0.0.1',
        '--spring.rabbitmq.port=1',
        '--xxl.job.enabled=false',
        '--spring.sql.init.mode=never',
        '--spring.main.banner-mode=off',
        '--spring.output.ansi.enabled=never',
        '--travel.http.idempotency.processing-ttl-seconds=30',
        '--travel.http.idempotency.completed-ttl-seconds=60',
        '--redisson.lock.wait-time-seconds=30',
        '--logging.level.travel=INFO',
        "--logging.file.name=$($LogPath.Replace('\', '/'))"
    )
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $script:JavaPath
    $startInfo.Arguments = Join-NativeArguments $arguments
    $startInfo.WorkingDirectory = Split-Path -Parent $JarPath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.EnvironmentVariables['DB_HOST'] = $script:ResolvedDbHost
    $startInfo.EnvironmentVariables['DB_PORT'] = [string]$script:ResolvedDbPort
    $startInfo.EnvironmentVariables['DB_NAME'] = $script:ResolvedDbName
    $startInfo.EnvironmentVariables['DB_USERNAME'] = $script:ResolvedDbUsername
    $startInfo.EnvironmentVariables['DB_PASSWORD'] = $script:ResolvedDbPassword
    $startInfo.EnvironmentVariables['REDIS_HOST'] = $script:ResolvedRedisHost
    $startInfo.EnvironmentVariables['REDIS_PORT'] = [string]$script:ResolvedRedisPort
    $startInfo.EnvironmentVariables['REDIS_PASSWORD'] = $script:ResolvedRedisPassword
    $startInfo.EnvironmentVariables['REDIS_DB'] = [string]$RedisDatabase
    $startInfo.EnvironmentVariables['JWT_SECRET'] = $JwtSecret
    return [System.Diagnostics.Process]::Start($startInfo)
}

function Wait-ServiceReady {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$BaseUrl,
        [int]$RouteId,
        [string]$Token,
        [int]$TimeoutSeconds,
        [string]$LogPath
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($Process.HasExited) {
            $tail = if (Test-Path -LiteralPath $LogPath) {
                (Get-Content -LiteralPath $LogPath -Tail 80 -ErrorAction SilentlyContinue) -join [Environment]::NewLine
            } else { '' }
            throw "Route service exited during startup. $tail"
        }
        try {
            $response = Invoke-WebRequest `
                    -Uri "$BaseUrl/route-optimization/history/$RouteId" `
                    -Headers @{ Authorization = "Bearer $Token" } `
                    -UseBasicParsing `
                    -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                return
            }
        }
        catch {
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Route service did not become ready within $TimeoutSeconds seconds."
}

function Stop-ProcessSafely {
    param([AllowNull()][System.Diagnostics.Process]$Process)

    if ($null -eq $Process) {
        return
    }
    try {
        if (-not $Process.HasExited) {
            $Process.Kill()
            $Process.WaitForExit(10000) | Out-Null
        }
    }
    catch {
    }
}

function Stop-MarkedJavaProcesses {
    param([string]$ProcessMarker)

    if ([string]::IsNullOrWhiteSpace($ProcessMarker)) {
        return
    }
    $processes = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
            Where-Object { $null -ne $_.CommandLine -and $_.CommandLine.Contains($ProcessMarker) }
    foreach ($process in $processes) {
        try {
            Stop-Process -Id $process.ProcessId -Force -ErrorAction Stop
        }
        catch {
        }
    }
}

function Get-Percentile {
    param(
        [long[]]$Values,
        [double]$Percentile
    )

    if ($Values.Count -eq 0) {
        return 0
    }
    $sorted = @($Values | Sort-Object)
    $index = [Math]::Ceiling($Percentile * $sorted.Count) - 1
    $index = [Math]::Max(0, [Math]::Min($index, $sorted.Count - 1))
    return [long]$sorted[$index]
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $repoRoot 'deploy\.env'
}
elseif (-not [IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile = Join-Path $repoRoot $EnvFile
}
$dotEnv = Read-DotEnv $EnvFile

$script:ResolvedDbHost = Resolve-TextSetting $DbHost 'DB_HOST' $dotEnv '127.0.0.1'
$script:ResolvedDbPort = Resolve-IntegerSetting $DbPort 'DB_PORT' $dotEnv 3306
$script:ResolvedDbName = Resolve-TextSetting $DbName 'DB_NAME' $dotEnv 'travel_website'
$script:ResolvedDbUsername = Resolve-TextSetting $DbUsername 'DB_USERNAME' $dotEnv
$script:ResolvedDbPassword = Resolve-TextSetting $DbPassword 'DB_PASSWORD' $dotEnv
$script:ResolvedRedisHost = Resolve-TextSetting $RedisHost 'REDIS_HOST' $dotEnv '127.0.0.1'
$script:ResolvedRedisPort = Resolve-IntegerSetting $RedisPort 'REDIS_PORT' $dotEnv 6379
$script:ResolvedRedisPassword = Resolve-TextSetting $RedisPassword 'REDIS_PASSWORD' $dotEnv

$ServiceContextPath = $ServiceContextPath.Trim()
if ($ServiceContextPath -eq '/') {
    $ServiceContextPath = ''
}
elseif ($ServiceContextPath.Length -gt 0) {
    if (-not $ServiceContextPath.StartsWith('/') -or $ServiceContextPath -match '[?#\s]') {
        throw 'ServiceContextPath must be empty or an absolute URL path without query, fragment, or whitespace.'
    }
    $ServiceContextPath = $ServiceContextPath.TrimEnd('/')
}
$serviceBaseUrl = "http://127.0.0.1:$ServicePort$ServiceContextPath"

if ([string]::IsNullOrWhiteSpace($script:ResolvedDbUsername) -or
        [string]::IsNullOrWhiteSpace($script:ResolvedDbPassword)) {
    throw 'DB_USERNAME and DB_PASSWORD must be configured.'
}
if (Test-TcpPort $ServicePort) {
    throw "Service port $ServicePort is already in use."
}

$script:ResolvedMySql = Resolve-Executable $MySqlPath 'mysql.exe'
$script:JavaPath = Resolve-JavaExecutable
$jmeterHome = Resolve-JMeterHome $JMeterPath
$jmeterJar = Join-Path $jmeterHome 'bin\ApacheJMeter.jar'
if ([string]::IsNullOrWhiteSpace($MavenWrapper)) {
    $MavenWrapper = Join-Path $repoRoot 'mvnw.cmd'
}
$resolvedMavenWrapper = Resolve-Executable $MavenWrapper 'mvnw.cmd'
$testPlanPath = Join-Path $PSScriptRoot 'route-optimization-concurrency.jmx'
if (-not (Test-Path -LiteralPath $testPlanPath -PathType Leaf)) {
    throw "JMeter test plan does not exist: $testPlanPath"
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$runId = [Guid]::NewGuid().ToString('N')
$processMarker = "-Dtravel.jmeter.run-id=$runId"
$testPrefix = "jmeter_route_opt_$($runId.Substring(0, 12))"
$outputDirectory = Join-Path $repoRoot "run-logs\jmeter-route-optimization\$timestamp"
$resultPath = Join-Path $outputDirectory 'results.jtl'
$reportDirectory = Join-Path $outputDirectory 'html-report'
$jmeterLogPath = Join-Path $outputDirectory 'jmeter.log'
$serviceLogPath = Join-Path $outputDirectory 'route-service.log'
$redisLogPath = Join-Path $outputDirectory 'redis.log'
$buildLogPath = Join-Path $outputDirectory 'build.log'
$summaryPath = Join-Path $outputDirectory 'run-summary.json'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$serviceProcess = $null
$redisProcess = $null
$userId = $null
$routeId = $null
$attractionIds = @()
$jmeterExitCode = $null
$success = $false
$failure = $null
$metrics = $null
$databaseResult = $null
$historyResult = $null
$cleanupResult = $null
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

try {
    if ($UseExistingRedis) {
        if (-not (Test-TcpPort $script:ResolvedRedisPort)) {
            throw "Existing Redis is unreachable: $($script:ResolvedRedisHost):$($script:ResolvedRedisPort)"
        }
    }
    else {
        if (Test-TcpPort $DisposableRedisPort) {
            throw "Disposable Redis port $DisposableRedisPort is already in use."
        }
        $resolvedRedisServer = Resolve-RedisServer $RedisServerPath
        $script:ResolvedRedisHost = '127.0.0.1'
        $script:ResolvedRedisPort = $DisposableRedisPort
        $script:ResolvedRedisPassword = [Guid]::NewGuid().ToString('N')
        $redisProcess = Start-DisposableRedis `
                $resolvedRedisServer `
                $script:ResolvedRedisPassword `
                $outputDirectory `
                $redisLogPath
        Wait-TcpPort $redisProcess $DisposableRedisPort 15 'Disposable Redis' $redisLogPath
    }

    Invoke-MySqlScript (Join-Path $repoRoot 'backend\docs\infrastructure\ROUTE_OPTIMIZATION_CONSISTENCY_MIGRATION.sql')

    $cityId = [int](Invoke-MySql 'SELECT MIN(id) FROM city;')
    if ($cityId -le 0) {
        throw 'No city exists for the temporary route.'
    }

    $username = "${testPrefix}_user"
    $userIdText = Invoke-MySql @"
INSERT INTO user (username, password, user_type)
VALUES ('$username', 'not-used-by-jmeter', 1);
SELECT LAST_INSERT_ID();
"@
    $userId = [int]($userIdText -split "`r?`n" | Select-Object -Last 1)

    $routeTitle = "${testPrefix}_route"
    $routeIdText = Invoke-MySql @"
INSERT INTO route (title, description, city_id, duration_days, user_id, is_public)
VALUES ('$routeTitle', 'temporary route optimization concurrency target', $cityId, 1, $userId, FALSE);
SELECT LAST_INSERT_ID();
"@
    $routeId = [int]($routeIdText -split "`r?`n" | Select-Object -Last 1)

    $attractionName1 = "${testPrefix}_attraction_1"
    $attractionName2 = "${testPrefix}_attraction_2"
    $attractionName3 = "${testPrefix}_attraction_3"
    Invoke-MySql @"
INSERT INTO attraction (name, city_id, latitude, longitude, ticket_price, rating)
VALUES
    ('$attractionName1', $cityId, 31.23040000, 121.47370000, 0, 4.50),
    ('$attractionName2', $cityId, 31.24040000, 121.48370000, 0, 4.50),
    ('$attractionName3', $cityId, 31.25040000, 121.49370000, 0, 4.50);
"@ | Out-Null

    $attractionRows = @((Invoke-MySql @"
SELECT id
FROM attraction
WHERE name IN ('$attractionName1', '$attractionName2', '$attractionName3')
ORDER BY name;
"@) -split "`r?`n" | Where-Object { $_ })
    if ($attractionRows.Count -ne 3) {
        throw "Expected 3 temporary attractions, found $($attractionRows.Count)."
    }
    $attractionIds = @($attractionRows | ForEach-Object { [int]$_ })

    Invoke-MySql @"
INSERT INTO route_attractions (route_id, attraction_id, day_number, visit_order, notes)
VALUES
    ($routeId, $($attractionIds[0]), 1, 1, 'initial-1'),
    ($routeId, $($attractionIds[1]), 1, 2, 'initial-2'),
    ($routeId, $($attractionIds[2]), 1, 3, 'initial-3');
"@ | Out-Null

    $expectedOrder = @($attractionIds[2], $attractionIds[0], $attractionIds[1])
    $expectedOrderText = $expectedOrder -join ','
    $jwtSecret = [Guid]::NewGuid().ToString('N')
    $token = New-TestJwt $jwtSecret $userId

    $buildArguments = @(
        '-q',
        '-f', (Join-Path $repoRoot 'backend\pom.xml'),
        '-pl', 'route-service',
        '-am',
        '-DskipTests',
        'package'
    )
    & $resolvedMavenWrapper @buildArguments *> $buildLogPath
    if ($LASTEXITCODE -ne 0) {
        throw "Route service build failed. See $buildLogPath"
    }
    $jar = Get-ChildItem (Join-Path $repoRoot 'backend\route-service\target') -Filter '*.jar' -File |
            Where-Object { $_.Name -notmatch '\.original$' } |
            Sort-Object Length -Descending |
            Select-Object -First 1
    if ($null -eq $jar) {
        throw 'Route service jar was not produced.'
    }

    $serviceProcess = Start-RouteService $jar.FullName $jwtSecret $processMarker $serviceLogPath
    Wait-ServiceReady $serviceProcess $serviceBaseUrl $routeId $token $StartupTimeoutSeconds $serviceLogPath

    $jmeterArguments = @(
        '-Xms512m',
        '-Xmx1g',
        '-XX:MaxMetaspaceSize=256m',
        '-Duser.language=en',
        '-Duser.region=EN',
        '-jar',
        $jmeterJar,
        '-n',
        '-t', $testPlanPath,
        '-l', $resultPath,
        '-j', $jmeterLogPath,
        '-e',
        '-o', $reportDirectory,
        '-Jservice_host=127.0.0.1',
        "-Jservice_port=$ServicePort",
        "-Jservice_context_path=$ServiceContextPath",
        "-Jroute_id=$routeId",
        "-Jattraction_1=$($expectedOrder[0])",
        "-Jattraction_2=$($expectedOrder[1])",
        "-Jattraction_3=$($expectedOrder[2])",
        "-Jthreads=$Threads",
        "-Jtoken=$token",
        "-Jconnect_timeout_ms=$ConnectTimeoutMs",
        "-Jresponse_timeout_ms=$ResponseTimeoutMs",
        '-Jjmeter.save.saveservice.output_format=csv',
        '-Jjmeter.save.saveservice.print_field_names=true',
        '-Jjmeter.save.saveservice.assertion_results_failure_message=true'
    )
    & $script:JavaPath @jmeterArguments
    $jmeterExitCode = $LASTEXITCODE

    if (-not (Test-Path -LiteralPath $resultPath -PathType Leaf)) {
        throw 'JMeter result file was not created.'
    }
    $samples = @(Import-Csv -LiteralPath $resultPath |
            Where-Object { $_.label -eq 'route-optimization:apply' })
    if ($samples.Count -ne $Threads) {
        throw "Expected $Threads route optimization samples, found $($samples.Count)."
    }
    $failedSamples = @($samples | Where-Object { $_.success -ne 'true' -or $_.responseCode -ne '200' })
    $elapsedValues = @($samples | ForEach-Object { [long]$_.elapsed })
    $startTimestamp = ($samples | ForEach-Object { [long]$_.timeStamp } | Measure-Object -Minimum).Minimum
    $endTimestamp = ($samples | ForEach-Object { [long]$_.timeStamp + [long]$_.elapsed } | Measure-Object -Maximum).Maximum
    $durationSeconds = [Math]::Max(0.001, ($endTimestamp - $startTimestamp) / 1000.0)
    $metrics = [ordered]@{
        samples = $samples.Count
        http200 = @($samples | Where-Object { $_.responseCode -eq '200' }).Count
        failures = $failedSamples.Count
        averageMs = [Math]::Round((($elapsedValues | Measure-Object -Average).Average), 2)
        p95Ms = Get-Percentile $elapsedValues 0.95
        p99Ms = Get-Percentile $elapsedValues 0.99
        maxMs = ($elapsedValues | Measure-Object -Maximum).Maximum
        throughputPerSecond = [Math]::Round($samples.Count / $durationSeconds, 2)
    }
    if ($jmeterExitCode -ne 0 -or $failedSamples.Count -ne 0) {
        throw "JMeter route optimization load failed: exitCode=$jmeterExitCode, failedSamples=$($failedSamples.Count)."
    }

    $databaseRow = Invoke-MySql @"
SELECT
    COUNT(*),
    COALESCE(SUM(day_number <= 0 OR visit_order <= 0), 0),
    COUNT(DISTINCT CONCAT(day_number, ':', visit_order)),
    MIN(day_number),
    MAX(day_number),
    MIN(visit_order),
    MAX(visit_order),
    GROUP_CONCAT(attraction_id ORDER BY day_number, visit_order SEPARATOR ',')
FROM route_attractions
WHERE route_id=$routeId;
"@
    $databaseParts = $databaseRow.Split("`t")
    if ($databaseParts.Count -ne 8) {
        throw 'Unexpected route schedule verification result.'
    }
    $indexColumns = Invoke-MySql @"
SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')
FROM information_schema.statistics
WHERE table_schema=DATABASE()
  AND table_name='route_attractions'
  AND index_name='uk_route_day_visit_order'
HAVING MIN(non_unique)=0;
"@
    $databaseResult = [ordered]@{
        relationRows = [int]$databaseParts[0]
        invalidPositions = [int]$databaseParts[1]
        distinctPositions = [int]$databaseParts[2]
        minDay = [int]$databaseParts[3]
        maxDay = [int]$databaseParts[4]
        minOrder = [int]$databaseParts[5]
        maxOrder = [int]$databaseParts[6]
        actualOrder = $databaseParts[7]
        expectedOrder = $expectedOrderText
        uniqueIndexColumns = $indexColumns
    }
    if ($databaseResult.relationRows -ne 3 -or
            $databaseResult.invalidPositions -ne 0 -or
            $databaseResult.distinctPositions -ne 3 -or
            $databaseResult.minDay -ne 1 -or $databaseResult.maxDay -ne 1 -or
            $databaseResult.minOrder -ne 1 -or $databaseResult.maxOrder -ne 3 -or
            $databaseResult.actualOrder -ne $expectedOrderText -or
            $databaseResult.uniqueIndexColumns -ne 'route_id,day_number,visit_order') {
        throw "Database route schedule verification failed: $($databaseResult | ConvertTo-Json -Compress)"
    }

    $historyResponse = Invoke-WebRequest `
            -Uri "$serviceBaseUrl/route-optimization/history/$routeId" `
            -Headers @{ Authorization = "Bearer $token" } `
            -UseBasicParsing `
            -TimeoutSec 10
    $historyPayload = $historyResponse.Content | ConvertFrom-Json
    $historyItems = @($historyPayload.data)
    $historyResult = [ordered]@{
        httpStatus = [int]$historyResponse.StatusCode
        success = [bool]$historyPayload.success
        count = $historyItems.Count
        routeId = if ($historyItems.Count -eq 1) { [int]$historyItems[0].routeId } else { $null }
        optimizationType = if ($historyItems.Count -eq 1) { [string]$historyItems[0].optimizationType } else { $null }
    }
    if ($historyResult.httpStatus -ne 200 -or -not $historyResult.success -or
            $historyResult.count -ne 1 -or $historyResult.routeId -ne $routeId -or
            $historyResult.optimizationType -ne 'distance') {
        throw "Route optimization history verification failed: $($historyResult | ConvertTo-Json -Compress)"
    }

    $success = $true
}
catch {
    $failure = $_.Exception.Message
}
finally {
    Stop-ProcessSafely $serviceProcess
    Stop-MarkedJavaProcesses $processMarker
    Stop-ProcessSafely $redisProcess
    Start-Sleep -Milliseconds 500

    try {
        Invoke-MySql @"
DELETE FROM route
WHERE LEFT(title, CHAR_LENGTH('$testPrefix')) = '$testPrefix';
DELETE FROM attraction
WHERE LEFT(name, CHAR_LENGTH('$testPrefix')) = '$testPrefix';
DELETE FROM user
WHERE LEFT(username, CHAR_LENGTH('$testPrefix')) = '$testPrefix';
"@ | Out-Null
        $residueCount = [int](Invoke-MySql @"
SELECT
    (SELECT COUNT(*) FROM user WHERE LEFT(username, CHAR_LENGTH('$testPrefix')) = '$testPrefix') +
    (SELECT COUNT(*) FROM route WHERE LEFT(title, CHAR_LENGTH('$testPrefix')) = '$testPrefix') +
    (SELECT COUNT(*) FROM attraction WHERE LEFT(name, CHAR_LENGTH('$testPrefix')) = '$testPrefix');
"@)
        $cleanupResult = [ordered]@{
            residueRows = $residueCount
            servicePortListening = Test-TcpPort $ServicePort
            redisPortListening = if ($UseExistingRedis) { $null } else { Test-TcpPort $DisposableRedisPort }
        }
        if ($cleanupResult.residueRows -ne 0 -or $cleanupResult.servicePortListening -or
                (!$UseExistingRedis -and $cleanupResult.redisPortListening)) {
            throw "Cleanup verification failed: $($cleanupResult | ConvertTo-Json -Compress)"
        }
    }
    catch {
        if ($null -eq $failure) {
            $failure = "Cleanup failed: $($_.Exception.Message)"
            $success = $false
        }
    }
    $stopwatch.Stop()
}

$summary = [ordered]@{
    generatedAt = (Get-Date).ToString('o')
    success = $success
    durationMs = $stopwatch.ElapsedMilliseconds
    error = $failure
    threads = $Threads
    temporaryUserId = $userId
    temporaryRouteId = $routeId
    temporaryAttractionIds = $attractionIds
    servicePort = $ServicePort
    serviceContextPath = $ServiceContextPath
    jmeterExitCode = $jmeterExitCode
    metrics = $metrics
    database = $databaseResult
    history = $historyResult
    cleanup = $cleanupResult
    artifacts = [ordered]@{
        results = $resultPath.Substring($repoRoot.Length + 1).Replace('\', '/')
        htmlReport = $reportDirectory.Substring($repoRoot.Length + 1).Replace('\', '/')
        jmeterLog = $jmeterLogPath.Substring($repoRoot.Length + 1).Replace('\', '/')
        serviceLog = $serviceLogPath.Substring($repoRoot.Length + 1).Replace('\', '/')
        redisLog = if ($UseExistingRedis) { $null } else { $redisLogPath.Substring($repoRoot.Length + 1).Replace('\', '/') }
        buildLog = $buildLogPath.Substring($repoRoot.Length + 1).Replace('\', '/')
    }
}
Write-Utf8NoBom $summaryPath ($summary | ConvertTo-Json -Depth 8)

Write-Host "Route optimization concurrency test success: $success"
Write-Host "Summary: $summaryPath"
if ($null -ne $metrics) {
    Write-Host "Samples=$($metrics.samples), Failures=$($metrics.failures), P95=$($metrics.p95Ms)ms, P99=$($metrics.p99Ms)ms"
}
if (-not $success) {
    Write-Error $failure
    exit 1
}
