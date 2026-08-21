[CmdletBinding()]
param(
    [Parameter()]
    [string]$EnvFile,

    [Parameter()]
    [string]$RabbitHost,

    [Parameter()]
    [ValidateRange(1, 65535)]
    [int]$RabbitPort = 0,

    [Parameter()]
    [string]$RabbitUsername,

    [Parameter()]
    [string]$RabbitPassword,

    [Parameter()]
    [string]$RabbitVhost,

    [Parameter()]
    [Nullable[bool]]$RabbitSslEnabled,

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
    [int]$RedisDatabase = 0,

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
        [int]$DefaultValue,
        [int]$Minimum,
        [int]$Maximum
    )

    if ($ExplicitValue -gt 0) {
        return $ExplicitValue
    }
    $text = Resolve-TextSetting -ExplicitValue $null -Name $Name -DotEnv $DotEnv
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $DefaultValue
    }
    $value = 0
    if (-not [int]::TryParse($text, [ref]$value) -or $value -lt $Minimum -or $value -gt $Maximum) {
        throw "$Name must be between $Minimum and $Maximum."
    }
    return $value
}

function Resolve-BooleanSetting {
    param(
        [Nullable[bool]]$ExplicitValue,
        [string]$Name,
        [hashtable]$DotEnv,
        [bool]$DefaultValue
    )

    if ($null -ne $ExplicitValue) {
        return [bool]$ExplicitValue
    }
    $text = Resolve-TextSetting -ExplicitValue $null -Name $Name -DotEnv $DotEnv
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $DefaultValue
    }
    if ($text -match '^(?i:true|1|yes|on)$') {
        return $true
    }
    if ($text -match '^(?i:false|0|no|off)$') {
        return $false
    }
    throw "$Name must be true or false."
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

function Test-TcpEndpoint {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMilliseconds = 5000
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne($TimeoutMilliseconds, $false)) {
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

function Protect-Text {
    param([AllowEmptyString()][string]$Value)

    $protected = $Value
    foreach ($secret in @(
            $script:ResolvedRabbitPassword,
            $script:ResolvedRabbitUsername,
            $script:ResolvedRabbitHost,
            $script:ResolvedDbPassword,
            $script:ResolvedRedisPassword)) {
        if (-not [string]::IsNullOrWhiteSpace($secret)) {
            $replacement = if ($secret -eq $script:ResolvedRabbitHost) { '<rabbit-host>' } else { '***' }
            $protected = $protected.Replace($secret, $replacement)
        }
    }
    return $protected
}

function Invoke-MySql {
    param(
        [string]$Executable,
        [string]$Sql
    )

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
    $startInfo.FileName = $Executable
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
        throw "MySQL command failed: $(Protect-Text $stderr.Trim())"
    }
    return $stdout.Trim()
}

function Invoke-MySqlScript {
    param(
        [string]$Executable,
        [string]$Path
    )

    $normalizedPath = (Resolve-Path -LiteralPath $Path).Path.Replace('\', '/')
    Invoke-MySql -Executable $Executable -Sql "source $normalizedPath" | Out-Null
}

function Set-ProcessEnvironment {
    param(
        [hashtable]$OriginalValues,
        [string]$Name,
        [AllowEmptyString()][string]$Value
    )

    if (-not $OriginalValues.ContainsKey($Name)) {
        $OriginalValues[$Name] = [Environment]::GetEnvironmentVariable($Name, 'Process')
    }
    [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
}

function Restore-ProcessEnvironment {
    param([hashtable]$OriginalValues)

    foreach ($name in $OriginalValues.Keys) {
        [Environment]::SetEnvironmentVariable($name, $OriginalValues[$name], 'Process')
    }
}

function Parse-LiveResults {
    param([string]$Output)

    $results = @()
    foreach ($line in ($Output -split "`r?`n")) {
        if ($line -notmatch '^RABBIT_LIVE_RESULT\s+') {
            continue
        }
        $scenarioMatch = [regex]::Match($line, 'scenario=([^\s]+)')
        $messageIdMatch = [regex]::Match($line, 'messageId=([^\s]+)')
        $results += [ordered]@{
            scenario = if ($scenarioMatch.Success) { $scenarioMatch.Groups[1].Value } else { 'unknown' }
            messageId = if ($messageIdMatch.Success) { $messageIdMatch.Groups[1].Value } else { $null }
            evidence = $line
        }
    }
    return $results
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $repoRoot 'deploy\.env'
}
elseif (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile = Join-Path $repoRoot $EnvFile
}

$dotEnv = Read-DotEnv -Path $EnvFile
$script:ResolvedRabbitHost = Resolve-TextSetting $RabbitHost 'RABBITMQ_HOST' $dotEnv
$script:ResolvedRabbitPort = Resolve-IntegerSetting $RabbitPort 'RABBITMQ_PORT' $dotEnv 5672 1 65535
$script:ResolvedRabbitUsername = Resolve-TextSetting $RabbitUsername 'RABBITMQ_USERNAME' $dotEnv
$script:ResolvedRabbitPassword = Resolve-TextSetting $RabbitPassword 'RABBITMQ_PASSWORD' $dotEnv
$script:ResolvedRabbitVhost = Resolve-TextSetting $RabbitVhost 'RABBITMQ_VHOST' $dotEnv '/'
$script:ResolvedRabbitSsl = Resolve-BooleanSetting $RabbitSslEnabled 'RABBITMQ_SSL_ENABLED' $dotEnv $false
$script:ResolvedDbHost = Resolve-TextSetting $DbHost 'DB_HOST' $dotEnv '127.0.0.1'
$script:ResolvedDbPort = Resolve-IntegerSetting $DbPort 'DB_PORT' $dotEnv 3306 1 65535
$script:ResolvedDbName = Resolve-TextSetting $DbName 'DB_NAME' $dotEnv 'travel_website'
$script:ResolvedDbUsername = Resolve-TextSetting $DbUsername 'DB_USERNAME' $dotEnv
$script:ResolvedDbPassword = Resolve-TextSetting $DbPassword 'DB_PASSWORD' $dotEnv
$script:ResolvedRedisHost = Resolve-TextSetting $RedisHost 'REDIS_HOST' $dotEnv '127.0.0.1'
$script:ResolvedRedisPort = Resolve-IntegerSetting $RedisPort 'REDIS_PORT' $dotEnv 6379 1 65535
$script:ResolvedRedisPassword = Resolve-TextSetting $RedisPassword 'REDIS_PASSWORD' $dotEnv
$resolvedRedisDatabaseText = Resolve-TextSetting $null 'REDIS_DB' $dotEnv ([string]$RedisDatabase)
$script:ResolvedRedisDatabase = 0
if (-not [int]::TryParse($resolvedRedisDatabaseText, [ref]$script:ResolvedRedisDatabase) -or
        $script:ResolvedRedisDatabase -lt 0 -or $script:ResolvedRedisDatabase -gt 15) {
    throw 'REDIS_DB must be between 0 and 15.'
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outputDirectory = Join-Path $repoRoot "run-logs\rabbitmq\$timestamp"
$logPath = Join-Path $outputDirectory 'maven-output.log'
$summaryPath = Join-Path $outputDirectory 'run-summary.json'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$success = $false
$failure = $null
$mavenExitCode = $null
$sanitizedOutput = ''
$results = @()
$originalEnvironment = @{}
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$preflight = [ordered]@{
    mysqlTcp = $false
    redisTcp = $false
    rabbitTcp = $false
    notificationMigration = $false
    messageStatusMigration = $false
}

try {
    foreach ($required in @(
            @{ Name = 'RABBITMQ_HOST'; Value = $script:ResolvedRabbitHost },
            @{ Name = 'RABBITMQ_USERNAME'; Value = $script:ResolvedRabbitUsername },
            @{ Name = 'RABBITMQ_PASSWORD'; Value = $script:ResolvedRabbitPassword },
            @{ Name = 'DB_USERNAME'; Value = $script:ResolvedDbUsername },
            @{ Name = 'DB_PASSWORD'; Value = $script:ResolvedDbPassword })) {
        if ([string]::IsNullOrWhiteSpace($required.Value)) {
            throw "$($required.Name) must be configured in the process environment, the env file, or script parameters."
        }
    }

    $resolvedMySql = Resolve-Executable $MySqlPath 'mysql.exe'
    if ([string]::IsNullOrWhiteSpace($MavenWrapper)) {
        $MavenWrapper = Join-Path $repoRoot 'mvnw.cmd'
    }
    $resolvedMavenWrapper = Resolve-Executable $MavenWrapper 'mvnw.cmd'

    $preflight.mysqlTcp = Test-TcpEndpoint $script:ResolvedDbHost $script:ResolvedDbPort
    $preflight.redisTcp = Test-TcpEndpoint $script:ResolvedRedisHost $script:ResolvedRedisPort
    $preflight.rabbitTcp = Test-TcpEndpoint $script:ResolvedRabbitHost $script:ResolvedRabbitPort
    if (-not $preflight.mysqlTcp) {
        throw 'MySQL TCP preflight failed.'
    }
    if (-not $preflight.redisTcp) {
        throw 'Redis TCP preflight failed.'
    }
    if (-not $preflight.rabbitTcp) {
        throw 'RabbitMQ TCP preflight failed.'
    }

    Invoke-MySqlScript $resolvedMySql (Join-Path $repoRoot 'backend\docs\infrastructure\MQ_RELIABLE_NOTIFICATION_MIGRATION.sql')
    $preflight.notificationMigration = $true

    $messageStatusExists = Invoke-MySql $resolvedMySql @'
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'mq_message_status';
'@
    if ($messageStatusExists -ne '1') {
        Invoke-MySqlScript $resolvedMySql (Join-Path $repoRoot 'backend\docs\infrastructure\MQ_MESSAGE_STATUS_MIGRATION.sql')
    }
    $preflight.messageStatusMigration = $true

    Set-ProcessEnvironment $originalEnvironment 'DB_HOST' $script:ResolvedDbHost
    Set-ProcessEnvironment $originalEnvironment 'DB_PORT' ([string]$script:ResolvedDbPort)
    Set-ProcessEnvironment $originalEnvironment 'DB_NAME' $script:ResolvedDbName
    Set-ProcessEnvironment $originalEnvironment 'DB_USERNAME' $script:ResolvedDbUsername
    Set-ProcessEnvironment $originalEnvironment 'DB_PASSWORD' $script:ResolvedDbPassword
    Set-ProcessEnvironment $originalEnvironment 'REDIS_HOST' $script:ResolvedRedisHost
    Set-ProcessEnvironment $originalEnvironment 'REDIS_PORT' ([string]$script:ResolvedRedisPort)
    Set-ProcessEnvironment $originalEnvironment 'REDIS_PASSWORD' $script:ResolvedRedisPassword
    Set-ProcessEnvironment $originalEnvironment 'REDIS_DB' ([string]$script:ResolvedRedisDatabase)
    Set-ProcessEnvironment $originalEnvironment 'RABBITMQ_HOST' $script:ResolvedRabbitHost
    Set-ProcessEnvironment $originalEnvironment 'RABBITMQ_PORT' ([string]$script:ResolvedRabbitPort)
    Set-ProcessEnvironment $originalEnvironment 'RABBITMQ_USERNAME' $script:ResolvedRabbitUsername
    Set-ProcessEnvironment $originalEnvironment 'RABBITMQ_PASSWORD' $script:ResolvedRabbitPassword
    Set-ProcessEnvironment $originalEnvironment 'RABBITMQ_VHOST' $script:ResolvedRabbitVhost
    Set-ProcessEnvironment $originalEnvironment 'SPRING_RABBITMQ_SSL_ENABLED' ([string]$script:ResolvedRabbitSsl).ToLowerInvariant()

    $mavenArguments = @(
        '-q',
        '-f', (Join-Path $repoRoot 'backend\pom.xml'),
        '-pl', 'collection-service',
        '-am',
        '-Dtest=ReliableNotificationLiveIT',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        '-Dstyle.color=never',
        'test'
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        Push-Location $repoRoot
        try {
            $rawOutputLines = @(& $resolvedMavenWrapper @mavenArguments 2>&1 |
                    ForEach-Object { $_.ToString() })
            $mavenExitCode = $LASTEXITCODE
        }
        finally {
            Pop-Location
        }
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    $sanitizedOutput = Protect-Text ($rawOutputLines -join [Environment]::NewLine)
    Write-Utf8NoBom $logPath $sanitizedOutput
    $results = @(Parse-LiveResults $sanitizedOutput)

    if ($mavenExitCode -ne 0) {
        throw "Reliable notification live test failed with Maven exit code $mavenExitCode."
    }
    if ($results.Count -ne 4) {
        throw "Expected 4 live result markers, found $($results.Count)."
    }
    $success = $true
}
catch {
    $failure = Protect-Text $_.Exception.Message
    if ([string]::IsNullOrWhiteSpace($sanitizedOutput)) {
        $sanitizedOutput = "ERROR: $failure"
    }
    else {
        $sanitizedOutput += [Environment]::NewLine + "ERROR: $failure"
    }
    Write-Utf8NoBom $logPath $sanitizedOutput
}
finally {
    Restore-ProcessEnvironment $originalEnvironment
    $stopwatch.Stop()
}

$summary = [ordered]@{
    generatedAt = (Get-Date).ToString('o')
    success = $success
    durationMs = $stopwatch.ElapsedMilliseconds
    exitCode = $mavenExitCode
    error = $failure
    environment = [ordered]@{
        envFile = if (Test-Path -LiteralPath $EnvFile) {
            (Resolve-Path -LiteralPath $EnvFile).Path.Substring($repoRoot.Length + 1).Replace('\', '/')
        } else {
            $null
        }
        rabbitHost = if ([string]::IsNullOrWhiteSpace($script:ResolvedRabbitHost)) { $null } else { '<configured>' }
        rabbitPort = $script:ResolvedRabbitPort
        rabbitVhost = $script:ResolvedRabbitVhost
        rabbitSslEnabled = $script:ResolvedRabbitSsl
        database = "$($script:ResolvedDbHost):$($script:ResolvedDbPort)/$($script:ResolvedDbName)"
        redis = "$($script:ResolvedRedisHost):$($script:ResolvedRedisPort)/$($script:ResolvedRedisDatabase)"
    }
    preflight = $preflight
    results = $results
    logFile = $logPath.Substring($repoRoot.Length + 1).Replace('\', '/')
    note = 'Live DLQ messages remain in the canonical project DLQ for replay evidence.'
}
Write-Utf8NoBom $summaryPath ($summary | ConvertTo-Json -Depth 8)

Write-Host "RabbitMQ live test success: $success"
Write-Host "Summary: $summaryPath"
Write-Host "Log: $logPath"
if (-not $success) {
    Write-Error $failure
    exit 1
}
