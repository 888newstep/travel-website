[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [string]$EnvFile,
    [ValidateRange(30, 600)]
    [int]$StartupTimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

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
        $values[$name] = $value
    }
    return $values
}

function Resolve-RedisPassword {
    param([hashtable]$Settings)

    if ($Settings.ContainsKey('REDIS_PASSWORD') -and -not [string]::IsNullOrWhiteSpace($Settings.REDIS_PASSWORD)) {
        return [string]$Settings.REDIS_PASSWORD
    }
    $candidatePaths = @(
        'E:\Redis\redis-7.2.4\redis.windows.conf',
        (Join-Path $repoRoot 'deploy\redis.windows.conf')
    )
    foreach ($candidate in $candidatePaths) {
        if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            continue
        }
        $match = Get-Content -LiteralPath $candidate | Where-Object {
            $_ -match '^\s*requirepass\s+\S+'
        } | Select-Object -First 1
        if ($match -and $match -match '^\s*requirepass\s+(.+?)\s*$') {
            return $Matches[1].Trim('"').Trim("'")
        }
    }
    return ''
}

function Test-TcpPort {
    param([string]$HostName, [int]$Port, [int]$TimeoutMilliseconds = 3000)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMilliseconds, $false)) {
            return $false
        }
        $client.EndConnect($async)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Wait-HttpReady {
    param([string]$Name, [string]$Url, [int]$TimeoutSeconds)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)
    throw "$Name did not become ready within $TimeoutSeconds seconds. See its log file."
}

function Set-EnvironmentFromMap {
    param([hashtable]$Values)

    foreach ($entry in $Values.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, [string]$entry.Value, 'Process')
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $repoRoot 'deploy\.env'
}
elseif (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile = Join-Path $repoRoot $EnvFile
}

$settings = Read-DotEnv $EnvFile
if (-not $settings.ContainsKey('DB_USERNAME')) { $settings.DB_USERNAME = 'root' }
if (-not $settings.ContainsKey('DB_HOST')) { $settings.DB_HOST = '127.0.0.1' }
if (-not $settings.ContainsKey('DB_PORT')) { $settings.DB_PORT = '3306' }
if (-not $settings.ContainsKey('DB_NAME')) { $settings.DB_NAME = 'travel_website' }
if (-not $settings.ContainsKey('REDIS_HOST')) { $settings.REDIS_HOST = '127.0.0.1' }
if (-not $settings.ContainsKey('REDIS_PORT')) { $settings.REDIS_PORT = '6379' }
if (-not $settings.ContainsKey('REDIS_DB')) { $settings.REDIS_DB = '3' }
if (-not $settings.ContainsKey('RABBITMQ_PORT')) { $settings.RABBITMQ_PORT = '5672' }
$settings.REDIS_PASSWORD = Resolve-RedisPassword $settings

foreach ($requiredName in @('DB_PASSWORD', 'REDIS_PASSWORD', 'RABBITMQ_HOST', 'RABBITMQ_USERNAME', 'RABBITMQ_PASSWORD', 'JWT_SECRET', 'AMAP_API_KEY')) {
    if (-not $settings.ContainsKey($requiredName) -or [string]::IsNullOrWhiteSpace([string]$settings[$requiredName])) {
        throw "$requiredName is required in deploy/.env or the supported local configuration."
    }
}

foreach ($endpoint in @(
        @{ Name = 'MySQL'; Host = $settings.DB_HOST; Port = [int]$settings.DB_PORT },
        @{ Name = 'Redis'; Host = $settings.REDIS_HOST; Port = [int]$settings.REDIS_PORT },
        @{ Name = 'RabbitMQ'; Host = $settings.RABBITMQ_HOST; Port = [int]$settings.RABBITMQ_PORT })) {
    if (-not (Test-TcpPort $endpoint.Host $endpoint.Port)) {
        throw "$($endpoint.Name) TCP preflight failed."
    }
}

$runtimeDirectory = Join-Path $repoRoot 'run-logs\e2e\current'
$archiveDirectory = Join-Path $repoRoot ('run-logs\e2e\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
if (Test-Path -LiteralPath $runtimeDirectory) {
    New-Item -ItemType Directory -Path $archiveDirectory -Force | Out-Null
    Get-ChildItem -LiteralPath $runtimeDirectory -Force | Move-Item -Destination $archiveDirectory -Force
}
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null

$ports = 8090..8095
foreach ($port in $ports) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "Port $port is already in use. Stop the existing listener before starting the E2E stack."
    }
}

if (-not $SkipBuild) {
    & mvn -f (Join-Path $repoRoot 'backend\pom.xml') package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Backend package failed with exit code $LASTEXITCODE."
    }
}

Set-EnvironmentFromMap $settings
$javaExecutable = Join-Path $env:JAVA_HOME 'bin\java.exe'
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or -not (Test-Path -LiteralPath $javaExecutable -PathType Leaf)) {
    $javacCommand = Get-Command javac.exe -ErrorAction Stop
    $javaExecutable = Join-Path (Split-Path $javacCommand.Source -Parent) 'java.exe'
}
$sharedArguments = @(
    '--spring.profiles.active=e2e',
    '--spring.cloud.nacos.discovery.enabled=false',
    '--spring.cloud.nacos.config.enabled=false',
    '--spring.cloud.nacos.config.import-check.enabled=false',
    '--spring.cloud.sentinel.eager=false',
    '--spring.cloud.sentinel.enabled=false',
    '--xxl.job.executor.enabled=false',
    '--travel.http.idempotency.enabled=true',
    '--logging.level.org.springframework.web=INFO',
    '--logging.level.travel=INFO'
)
$serviceDefinitions = @(
    @{ Name = 'user-service'; Port = 8091; Jar = 'backend\user-service\target\user-service-0.0.1-SNAPSHOT.jar'; Health = 'http://127.0.0.1:8091/api/actuator/health' },
    @{ Name = 'attraction-service'; Port = 8092; Jar = 'backend\attraction-service\target\attraction-service-0.0.1-SNAPSHOT.jar'; Health = 'http://127.0.0.1:8092/api/actuator/health' },
    @{ Name = 'route-service'; Port = 8093; Jar = 'backend\route-service\target\route-service-0.0.1-SNAPSHOT.jar'; Health = 'http://127.0.0.1:8093/api/actuator/health' },
    @{ Name = 'collection-service'; Port = 8094; Jar = 'backend\collection-service\target\collection-service-0.0.1-SNAPSHOT.jar'; Health = 'http://127.0.0.1:8094/api/actuator/health' },
    @{ Name = 'file-service'; Port = 8095; Jar = 'backend\file-service\target\file-service-0.0.1-SNAPSHOT.jar'; Health = 'http://127.0.0.1:8095/api/actuator/health' },
    @{ Name = 'gateway'; Port = 8090; Jar = 'backend\gateway\target\gateway-0.0.1-SNAPSHOT.jar'; Health = 'http://127.0.0.1:8090/actuator/health' }
)

$started = @()
try {
    foreach ($service in $serviceDefinitions) {
        $jarPath = Join-Path $repoRoot $service.Jar
        if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
            throw "Missing packaged jar: $jarPath"
        }
        $stdout = Join-Path $runtimeDirectory ($service.Name + '.out.log')
        $stderr = Join-Path $runtimeDirectory ($service.Name + '.err.log')
        $process = Start-Process -FilePath $javaExecutable -ArgumentList (@('-jar', $jarPath) + $sharedArguments) `
            -WorkingDirectory $repoRoot -RedirectStandardOutput $stdout -RedirectStandardError $stderr `
            -WindowStyle Hidden -PassThru
        Wait-HttpReady -Name $service.Name -Url $service.Health -TimeoutSeconds $StartupTimeoutSeconds
        $listener = Get-NetTCPConnection -State Listen -LocalPort $service.Port -ErrorAction Stop |
            Select-Object -First 1
        $started += [pscustomobject]@{
            name = $service.Name
            port = $service.Port
            pid = $listener.OwningProcess
            launcherPid = $process.Id
            jar = $service.Jar.Replace('\', '/')
            health = $service.Health
            stdout = $stdout.Substring($repoRoot.Length + 1).Replace('\', '/')
            stderr = $stderr.Substring($repoRoot.Length + 1).Replace('\', '/')
        }
        Write-Host "$($service.Name) ready on port $($service.Port)."
    }
    $pidPath = Join-Path $runtimeDirectory 'processes.json'
    $started | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $pidPath -Encoding UTF8
    $summary = [ordered]@{
        generatedAt = (Get-Date).ToString('o')
        success = $true
        profile = 'e2e'
        dependencies = [ordered]@{
            mysql = 'reachable'
            redis = 'reachable-authenticated-configuration'
            rabbitmq = 'reachable'
            amapKey = 'configured'
        }
        services = $started
    }
    $summary | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $runtimeDirectory 'startup-summary.json') -Encoding UTF8
    Write-Host "E2E stack ready: http://localhost:8090"
}
catch {
    foreach ($service in $started) {
        Stop-Process -Id $service.pid -Force -ErrorAction SilentlyContinue
    }
    throw
}
