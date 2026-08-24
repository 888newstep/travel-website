[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$pidPath = Join-Path $repoRoot 'run-logs\e2e\current\processes.json'
if (-not (Test-Path -LiteralPath $pidPath -PathType Leaf)) {
    Write-Host 'No E2E PID file found; nothing to stop.'
    exit 0
}

$parsedServices = Get-Content -LiteralPath $pidPath -Raw -Encoding UTF8 | ConvertFrom-Json
$services = @($parsedServices | ForEach-Object { $_ })
foreach ($service in $services) {
    $process = Get-Process -Id $service.pid -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        continue
    }
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($service.pid)" -ErrorAction SilentlyContinue
    $commandLine = if ($null -eq $processInfo) { $null } else { $processInfo.CommandLine }
    $expectedJar = if ($service.PSObject.Properties.Name -contains 'jar') {
        ([string]$service.jar).Replace('/', '\')
    } else {
        [string]$service.name
    }
    if ($commandLine -and ($commandLine.Contains($expectedJar) -or $commandLine.Contains([string]$service.name))) {
        Stop-Process -Id $service.pid -Force
        Write-Host "Stopped $($service.name) (PID $($service.pid))."
    }
    else {
        Write-Warning "PID $($service.pid) no longer matches $($service.name); skipped."
    }
    if ($service.PSObject.Properties.Name -contains 'launcherPid' -and $service.launcherPid -ne $service.pid) {
        Stop-Process -Id $service.launcherPid -Force -ErrorAction SilentlyContinue
    }
}
Remove-Item -LiteralPath $pidPath -Force
