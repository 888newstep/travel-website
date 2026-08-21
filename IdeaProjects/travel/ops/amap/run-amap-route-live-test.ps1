[CmdletBinding()]
param(
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

function Protect-Secrets {
    param([AllowEmptyString()][string]$Value)

    $protected = $Value
    foreach ($secret in @($env:AMAP_API_KEY, $DbPassword)) {
        if (-not [string]::IsNullOrWhiteSpace($secret)) {
            $protected = $protected.Replace($secret, '***')
        }
    }
    return [regex]::Replace($protected, '(?i)([?&]key=)[^&\s"]+', '$1***')
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

function Invoke-MySql {
    param(
        [string]$Executable,
        [string]$Sql
    )

    $arguments = @(
        "--host=$DbHost",
        "--port=$DbPort",
        "--user=$DbUsername",
        "--database=$DbName",
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
    $startInfo.EnvironmentVariables['MYSQL_PWD'] = $DbPassword

    $process = [System.Diagnostics.Process]::Start($startInfo)
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw "MySQL query failed: $(Protect-Secrets $stderr.Trim())"
    }
    return $stdout.Trim()
}

function ConvertTo-Scenario {
    param(
        [string]$RawValue,
        [string]$Name,
        [int]$MinimumPoints,
        [int]$MaximumPoints
    )

    if ([string]::IsNullOrWhiteSpace($RawValue)) {
        throw "No $Name route is available. Run AMAP_ROUTE_DATA_MIGRATION.sql first."
    }
    $parts = $RawValue.Trim().Split("`t")
    if ($parts.Count -ne 3) {
        throw "Unexpected MySQL result for $Name route."
    }

    $routeId = 0L
    $pointCount = 0
    if (-not [long]::TryParse($parts[0], [ref]$routeId) -or
            -not [int]::TryParse($parts[1], [ref]$pointCount)) {
        throw "Invalid route metadata for $Name route."
    }
    $points = $parts[2].Split('|')
    if ($pointCount -ne $points.Count -or
            $pointCount -lt $MinimumPoints -or
            $pointCount -gt $MaximumPoints) {
        throw "Invalid point count for $Name route."
    }
    foreach ($point in $points) {
        if ($point -notmatch '^-?\d+(\.\d+)?,-?\d+(\.\d+)?$') {
            throw "Invalid coordinate returned for $Name route."
        }
    }

    return [pscustomobject]@{
        RouteId = $routeId
        PointCount = $pointCount
        Points = $parts[2]
    }
}

function Set-ScenarioEnvironment {
    param(
        [hashtable]$OriginalValues,
        [string]$Name,
        [string]$Value
    )

    $OriginalValues[$Name] = [Environment]::GetEnvironmentVariable($Name, 'Process')
    [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
}

function Restore-ScenarioEnvironment {
    param([hashtable]$OriginalValues)

    foreach ($name in $OriginalValues.Keys) {
        [Environment]::SetEnvironmentVariable($name, $OriginalValues[$name], 'Process')
    }
}

function ConvertTo-TwoPointResult {
    param([string]$Output)

    $pattern = 'AMAP_LIVE_RESULT scenario=two-point routeId=(\d+) pointCount=(\d+) ' +
            'distanceMeters=(\d+) durationSeconds=(\d+) trafficSegmentCount=(\d+)'
    $match = [regex]::Match($Output, $pattern)
    if (-not $match.Success) {
        return $null
    }
    return [ordered]@{
        routeId = [long]$match.Groups[1].Value
        pointCount = [int]$match.Groups[2].Value
        distanceMeters = [long]$match.Groups[3].Value
        durationSeconds = [long]$match.Groups[4].Value
        trafficSegmentCount = [long]$match.Groups[5].Value
    }
}

function ConvertTo-MultiPointResult {
    param([string]$Output)

    $pattern = 'AMAP_LIVE_RESULT scenario=multi-point routeId=(\d+) pointCount=(\d+) ' +
            'distanceKm=([0-9.]+) durationMinutes=([0-9.]+) tolls=([0-9.]+) stepCount=(\d+)'
    $match = [regex]::Match($Output, $pattern)
    if (-not $match.Success) {
        return $null
    }
    $culture = [System.Globalization.CultureInfo]::InvariantCulture
    return [ordered]@{
        routeId = [long]$match.Groups[1].Value
        pointCount = [int]$match.Groups[2].Value
        distanceKm = [double]::Parse($match.Groups[3].Value, $culture)
        durationMinutes = [double]::Parse($match.Groups[4].Value, $culture)
        tolls = [double]::Parse($match.Groups[5].Value, $culture)
        stepCount = [int]$match.Groups[6].Value
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$backendPom = Join-Path $repoRoot 'backend\pom.xml'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outputDirectory = Join-Path $repoRoot "run-logs\amap\$timestamp"
$logPath = Join-Path $outputDirectory 'maven-output.log'
$summaryPath = Join-Path $outputDirectory 'run-summary.json'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$success = $false
$failure = $null
$mavenExitCode = $null
$twoPointScenario = $null
$multiPointScenario = $null
$twoPointResult = $null
$multiPointResult = $null
$sanitizedOutput = ''
$originalEnvironment = @{}
$commandDisplay = $null

try {
    if ([string]::IsNullOrWhiteSpace($env:AMAP_API_KEY)) {
        throw 'AMAP_API_KEY must be configured in the process environment.'
    }
    if ([string]::IsNullOrWhiteSpace($DbUsername) -or [string]::IsNullOrWhiteSpace($DbPassword)) {
        throw 'DB_USERNAME and DB_PASSWORD must be configured.'
    }
    if (-not (Test-Path -LiteralPath $backendPom -PathType Leaf)) {
        throw "Backend pom does not exist: $backendPom"
    }

    $resolvedMySql = Resolve-Executable -ExplicitPath $MySqlPath -CommandName 'mysql.exe'
    if ([string]::IsNullOrWhiteSpace($MavenWrapper)) {
        $MavenWrapper = Join-Path $repoRoot 'mvnw.cmd'
    }
    $resolvedMavenWrapper = Resolve-Executable -ExplicitPath $MavenWrapper -CommandName 'mvnw.cmd'

    $twoPointSql = @'
SELECT route.id,
       COUNT(*) AS point_count,
       GROUP_CONCAT(CONCAT(attraction.longitude, ',', attraction.latitude)
                    ORDER BY route_attraction.day_number, route_attraction.visit_order, route_attraction.id
                    SEPARATOR '|') AS points
FROM route
JOIN route_attractions route_attraction ON route_attraction.route_id = route.id
JOIN attraction ON attraction.id = route_attraction.attraction_id
WHERE attraction.longitude IS NOT NULL
  AND attraction.latitude IS NOT NULL
GROUP BY route.id
HAVING COUNT(*) = 2
ORDER BY route.id
LIMIT 1;
'@
    $multiPointSql = @'
SELECT route.id,
       COUNT(*) AS point_count,
       GROUP_CONCAT(CONCAT(attraction.longitude, ',', attraction.latitude)
                    ORDER BY route_attraction.day_number, route_attraction.visit_order, route_attraction.id
                    SEPARATOR '|') AS points
FROM route
JOIN route_attractions route_attraction ON route_attraction.route_id = route.id
JOIN attraction ON attraction.id = route_attraction.attraction_id
WHERE attraction.longitude IS NOT NULL
  AND attraction.latitude IS NOT NULL
GROUP BY route.id
HAVING COUNT(*) >= 3
ORDER BY COUNT(*), route.id
LIMIT 1;
'@

    $twoPointScenario = ConvertTo-Scenario `
            -RawValue (Invoke-MySql -Executable $resolvedMySql -Sql $twoPointSql) `
            -Name 'two-point' -MinimumPoints 2 -MaximumPoints 2
    $multiPointScenario = ConvertTo-Scenario `
            -RawValue (Invoke-MySql -Executable $resolvedMySql -Sql $multiPointSql) `
            -Name 'multi-point' -MinimumPoints 3 -MaximumPoints ([int]::MaxValue)

    Set-ScenarioEnvironment $originalEnvironment 'AMAP_LIVE_TWO_ROUTE_ID' ([string]$twoPointScenario.RouteId)
    Set-ScenarioEnvironment $originalEnvironment 'AMAP_LIVE_TWO_POINTS' $twoPointScenario.Points
    Set-ScenarioEnvironment $originalEnvironment 'AMAP_LIVE_MULTI_ROUTE_ID' ([string]$multiPointScenario.RouteId)
    Set-ScenarioEnvironment $originalEnvironment 'AMAP_LIVE_MULTI_POINTS' $multiPointScenario.Points

    $mavenArguments = @(
        '-q',
        '-f', $backendPom,
        '-pl', 'common',
        '-am',
        '-Dtest=AMapRouteLiveIT',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        '-Dstyle.color=never',
        'test'
    )
    $commandDisplay = (Split-Path -Leaf $resolvedMavenWrapper) + ' ' +
            (Join-NativeArguments $mavenArguments)

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

    $sanitizedOutput = Protect-Secrets ($rawOutputLines -join [Environment]::NewLine)
    Write-Utf8NoBom -Path $logPath -Value $sanitizedOutput

    if ($mavenExitCode -ne 0) {
        throw "AMap live test failed with Maven exit code $mavenExitCode."
    }

    $twoPointResult = ConvertTo-TwoPointResult $sanitizedOutput
    $multiPointResult = ConvertTo-MultiPointResult $sanitizedOutput
    if ($null -eq $twoPointResult -or $null -eq $multiPointResult) {
        throw 'AMap live test passed without complete result markers.'
    }
    $success = $true
}
catch {
    $failure = Protect-Secrets $_.Exception.Message
    if ([string]::IsNullOrWhiteSpace($sanitizedOutput)) {
        $sanitizedOutput = "ERROR: $failure"
    }
    else {
        $sanitizedOutput += [Environment]::NewLine + "ERROR: $failure"
    }
    Write-Utf8NoBom -Path $logPath -Value $sanitizedOutput
}
finally {
    Restore-ScenarioEnvironment $originalEnvironment
    $stopwatch.Stop()
}

$apiUrl = if ($env:AMAP_API_URL) { $env:AMAP_API_URL } else { 'https://restapi.amap.com/v3' }
$relativeLogPath = $logPath.Substring($repoRoot.Length + 1).Replace('\', '/')
$summary = [ordered]@{
    generatedAt = (Get-Date).ToString('o')
    success = $success
    durationMs = $stopwatch.ElapsedMilliseconds
    exitCode = $mavenExitCode
    error = $failure
    database = [ordered]@{
        host = $DbHost
        port = $DbPort
        name = $DbName
    }
    amap = [ordered]@{
        apiUrl = $apiUrl
        apiKeySource = 'AMAP_API_KEY'
        apiKeyConfigured = -not [string]::IsNullOrWhiteSpace($env:AMAP_API_KEY)
    }
    scenarios = [ordered]@{
        twoPoint = if ($null -eq $twoPointScenario) { $null } else {
            [ordered]@{
                routeId = $twoPointScenario.RouteId
                pointCount = $twoPointScenario.PointCount
                points = $twoPointScenario.Points
                result = $twoPointResult
            }
        }
        multiPoint = if ($null -eq $multiPointScenario) { $null } else {
            [ordered]@{
                routeId = $multiPointScenario.RouteId
                pointCount = $multiPointScenario.PointCount
                points = $multiPointScenario.Points
                result = $multiPointResult
            }
        }
    }
    command = $commandDisplay
    logFile = $relativeLogPath
}
Write-Utf8NoBom -Path $summaryPath -Value ($summary | ConvertTo-Json -Depth 8)

Write-Host "AMap live test success: $success"
Write-Host "Summary: $summaryPath"
Write-Host "Log: $logPath"

if (-not $success) {
    Write-Error $failure
    exit 1
}
