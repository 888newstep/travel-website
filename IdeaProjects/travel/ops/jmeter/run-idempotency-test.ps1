[CmdletBinding()]
param(
    [Parameter()]
    [ValidateNotNull()]
    [uri]$Gateway = 'http://127.0.0.1:8090',

    [Parameter()]
    [ValidateNotNullOrEmpty()]
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
    [ValidateRange(0, 3600)]
    [int]$RampUpSeconds = 0,

    [Parameter()]
    [ValidateRange(100, 120000)]
    [int]$ConnectTimeoutMs = 5000,

    [Parameter()]
    [ValidateRange(100, 300000)]
    [int]$ResponseTimeoutMs = 15000,

    [Parameter()]
    [ValidateLength(0, 128)]
    [string]$IdempotencyKey,

    [Parameter()]
    [string]$JMeterPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-JMeterHome {
    param([string]$ExplicitPath)

    $candidates = [System.Collections.Generic.List[string]]::new()
    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        if (Test-Path -LiteralPath $ExplicitPath -PathType Container) {
            $candidates.Add($ExplicitPath)
        }
        elseif (Test-Path -LiteralPath $ExplicitPath -PathType Leaf) {
            $explicitFile = Get-Item -LiteralPath $ExplicitPath
            $candidates.Add((Split-Path -Parent $explicitFile.DirectoryName))
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
        if (-not [string]::IsNullOrWhiteSpace($candidate)) {
            $resolvedHome = Resolve-Path -LiteralPath $candidate -ErrorAction SilentlyContinue
            if ($null -ne $resolvedHome -and
                (Test-Path -LiteralPath (Join-Path $resolvedHome.Path 'bin\ApacheJMeter.jar') -PathType Leaf)) {
                return $resolvedHome.Path
            }
        }
    }

    throw 'JMeter was not found. Set JMETER_HOME or pass -JMeterPath.'
}

function Test-TcpEndpoint {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMs
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne($TimeoutMs)) {
            return $false
        }
        $client.EndConnect($asyncResult)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Get-Percentile {
    param(
        [long[]]$Values,
        [ValidateRange(0.0, 1.0)]
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

if ($Gateway.Scheme -notin @('http', 'https')) {
    throw 'Gateway must use http or https.'
}
if ($Gateway.AbsolutePath -ne '/' -or -not [string]::IsNullOrEmpty($Gateway.Query)) {
    throw 'Gateway must be a root URL without a path or query, for example http://127.0.0.1:8090.'
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    throw 'The test password is blank. Set TEST_DATA_USER_PASSWORD or pass -Password.'
}

$jmeterHome = Resolve-JMeterHome -ExplicitPath $JMeterPath
$jmeterJar = Join-Path $jmeterHome 'bin\ApacheJMeter.jar'
$javaCommand = Get-Command 'java.exe' -ErrorAction SilentlyContinue
if ($null -eq $javaCommand) {
    throw 'java.exe was not found on PATH.'
}
$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = (Resolve-Path (Join-Path $scriptDirectory '..\..')).Path
$testPlanPath = Join-Path $scriptDirectory 'route-collection-idempotency.jmx'
if (-not (Test-Path -LiteralPath $testPlanPath -PathType Leaf)) {
    throw "JMeter test plan does not exist: $testPlanPath"
}

$gatewayPort = if ($Gateway.IsDefaultPort) {
    if ($Gateway.Scheme -eq 'https') { 443 } else { 80 }
} else {
    $Gateway.Port
}

if (-not (Test-TcpEndpoint -HostName $Gateway.DnsSafeHost -Port $gatewayPort -TimeoutMs $ConnectTimeoutMs)) {
    throw "Gateway is unreachable: $($Gateway.Scheme)://$($Gateway.DnsSafeHost):$gatewayPort. Start the gateway and its dependencies first."
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$outputDirectory = Join-Path $repositoryRoot "run-logs\jmeter\$timestamp"
$resultPath = Join-Path $outputDirectory 'results.jtl'
$reportDirectory = Join-Path $outputDirectory 'html-report'
$jmeterLogPath = Join-Path $outputDirectory 'jmeter.log'
$summaryPath = Join-Path $outputDirectory 'run-summary.json'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

if ([string]::IsNullOrWhiteSpace($IdempotencyKey)) {
    $IdempotencyKey = "jmeter-route-collection-$([guid]::NewGuid().ToString('N'))"
}

$jmeterArguments = @(
    '-n',
    '-t', $testPlanPath,
    '-l', $resultPath,
    '-j', $jmeterLogPath,
    '-e',
    '-o', $reportDirectory,
    "-Jgateway_protocol=$($Gateway.Scheme)",
    "-Jgateway_host=$($Gateway.DnsSafeHost)",
    "-Jgateway_port=$gatewayPort",
    "-Jusername=$Username",
    "-Jroute_id=$RouteId",
    "-Jthreads=$Threads",
    "-Jramp_up_seconds=$RampUpSeconds",
    "-Jconnect_timeout_ms=$ConnectTimeoutMs",
    "-Jresponse_timeout_ms=$ResponseTimeoutMs",
    "-Jidempotency_key=$IdempotencyKey",
    '-Jjmeter.save.saveservice.output_format=csv',
    '-Jjmeter.save.saveservice.print_field_names=true',
    '-Jjmeter.save.saveservice.assertion_results_failure_message=true'
)

$javaArguments = @(
    '-Xms512m',
    '-Xmx1g',
    '-XX:MaxMetaspaceSize=256m',
    '-Duser.language=en',
    '-Duser.region=EN',
    '-jar',
    $jmeterJar
) + $jmeterArguments

$previousJMeterPassword = $env:TRAVEL_JMETER_PASSWORD
$previousJMeterHome = $env:JMETER_HOME
try {
    # Pass the password only through the child process environment.
    $env:TRAVEL_JMETER_PASSWORD = $Password
    $env:JMETER_HOME = $jmeterHome
    Write-Host "Starting idempotency test: gateway=$Gateway, user=$Username, routeId=$RouteId, threads=$Threads"
    Write-Host "Output directory: $outputDirectory"
    & $javaCommand.Source @javaArguments
    $jmeterExitCode = $LASTEXITCODE
}
finally {
    if ($null -eq $previousJMeterPassword) {
        Remove-Item Env:TRAVEL_JMETER_PASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:TRAVEL_JMETER_PASSWORD = $previousJMeterPassword
    }
    if ($null -eq $previousJMeterHome) {
        Remove-Item Env:JMETER_HOME -ErrorAction SilentlyContinue
    }
    else {
        $env:JMETER_HOME = $previousJMeterHome
    }
}

if (-not (Test-Path -LiteralPath $resultPath -PathType Leaf)) {
    throw "JMeter did not create a result file, exitCode=$jmeterExitCode. Check $jmeterLogPath"
}

$rows = @(Import-Csv -LiteralPath $resultPath)
$failedRows = @($rows | Where-Object { $_.success -ne 'true' })
$mainRows = @($rows | Where-Object { $_.label -eq 'main:toggle' })
if ($mainRows.Count -ne $Threads) {
    throw "Concurrent sample count is invalid: expected=$Threads, actual=$($mainRows.Count). Check $resultPath"
}
$mainFailedRows = @($mainRows | Where-Object { $_.success -ne 'true' })

$elapsedValues = [long[]]@($mainRows | ForEach-Object { [long]$_.elapsed })
$startTimestamp = ($mainRows | Measure-Object -Property timeStamp -Minimum).Minimum
$endTimestamp = ($mainRows | ForEach-Object { [long]$_.timeStamp + [long]$_.elapsed } | Measure-Object -Maximum).Maximum
$durationMs = [Math]::Max(1, [long]$endTimestamp - [long]$startTimestamp)
$http200 = @($mainRows | Where-Object { $_.responseCode -eq '200' }).Count
$http409 = @($mainRows | Where-Object { $_.responseCode -eq '409' }).Count
$otherHttp = $mainRows.Count - $http200 - $http409
$aggregateRow = $rows | Where-Object { $_.label -eq 'teardown:aggregate' } | Select-Object -Last 1

$summary = [ordered]@{
    generatedAt = (Get-Date).ToString('o')
    gateway = $Gateway.AbsoluteUri.TrimEnd('/')
    username = $Username
    routeId = $RouteId
    threads = $Threads
    idempotencyKey = $IdempotencyKey
    jmeterExitCode = $jmeterExitCode
    samples = $mainRows.Count
    acceptedHttp200 = $http200
    acceptedHttp409 = $http409
    unexpectedHttp = $otherHttp
    mainFailedSamples = $mainFailedRows.Count
    errorRatePercent = [Math]::Round(($mainFailedRows.Count * 100.0) / $mainRows.Count, 4)
    failedSamples = $failedRows.Count
    averageMs = [Math]::Round(($elapsedValues | Measure-Object -Average).Average, 2)
    p95Ms = Get-Percentile -Values $elapsedValues -Percentile 0.95
    p99Ms = Get-Percentile -Values $elapsedValues -Percentile 0.99
    throughputPerSecond = [Math]::Round(($mainRows.Count * 1000.0) / $durationMs, 2)
    aggregate = if ($null -ne $aggregateRow) { $aggregateRow.responseMessage } else { $null }
    resultFile = $resultPath
    htmlReport = $reportDirectory
    jmeterLog = $jmeterLogPath
}
$summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $summaryPath -Encoding UTF8

Write-Host "Main phase: 200=$http200, 409=$http409, other=$otherHttp, failed=$($failedRows.Count)"
Write-Host "Performance: avg=$($summary.averageMs)ms, P95=$($summary.p95Ms)ms, P99=$($summary.p99Ms)ms, throughput=$($summary.throughputPerSecond)/s"
Write-Host "Aggregate: $($summary.aggregate)"
Write-Host "HTML report: $reportDirectory"

if ($jmeterExitCode -ne 0 -or $failedRows.Count -gt 0 -or $otherHttp -gt 0) {
    throw "Idempotency test failed. Check $summaryPath and $jmeterLogPath"
}

Write-Host 'Idempotency test passed. Verify one database row with verify-route-collection-idempotency.sql.'
