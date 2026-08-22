# P5 parity harness orchestrator.
# Runs every circuit in tools/parity/circuits through BOTH engines:
#   1. legacy Swing engine (RMI-driven, needs a display)
#   2. new headless engine (gecko-rest-api, started/stopped by this script)
# and compares the exported CSVs. Writes a report to tools/parity/results/.
#
# Usage: powershell -File run-parity.ps1 [-RelTol 1e-3] [-AbsTol 5e-3]
# Exit code 0 = all circuits PASS.
#
# Tolerances: the legacy reference path stores scope data as float32 and the
# legacy scope container decimates once its buffer fills; the defaults below
# reflect that data path, not solver accuracy (engines agree to ~1e-4 rel on
# raw regions).

param(
    [double]$RelTol = 1e-3,
    [double]$AbsTol = 5e-3,
    [string]$BaseUrl = "http://localhost:8080",
    [int]$RmiPort = 43099,
    [int]$RestPort = 8080
)

$ErrorActionPreference = 'Stop'
# $PSScriptRoot = <repo>\tools\parity
$script:RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$tools = Join-Path $script:RepoRoot 'tools\parity'
$classes = Join-Path $tools 'classes'
$work = Join-Path $env:TEMP ('gecko-parity-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
New-Item -ItemType Directory -Force -Path $classes, "$tools\results", $work | Out-Null

# name -> recorded signals (must match the file's dataContainerSignals)
$circuits = @(
    @{ Name = 'rc-lowpass';  Signals = 'u_out' },
    @{ Name = 'rl-transient'; Signals = 'u_l' },
    @{ Name = 'rlc-series';  Signals = 'u_c,u_l' }
)

function Stop-JavaChildren {
    param([int]$ProcId)
    try {
        # kill the launcher and whatever JVM it spawned (same process tree via command line match is overkill; destroy tree)
        $p = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcId" -ErrorAction SilentlyContinue
        if ($p) {
            Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcId" -ErrorAction SilentlyContinue |
                ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
            Stop-Process -Id $ProcId -Force -ErrorAction SilentlyContinue
        }
    } catch { }
}

try {
    # ---------- locate / build artifacts ----------
    $guiJar = Join-Path $script:RepoRoot 'src\modules\gecko-gui\target\gecko-1.0-jar-with-dependencies.jar'
    $restJar = Join-Path $script:RepoRoot 'src\modules\gecko-rest-api\target\gecko-rest-api-1.0.0.jar'
    if (-not (Test-Path $guiJar) -or -not (Test-Path $restJar)) {
        Write-Host 'building engine jars (skipping tests)...'
        & mvn -q -f (Join-Path $script:RepoRoot 'pom.xml') package '-pl' 'src/modules/gecko-gui,src/modules/gecko-rest-api' '-am' '-DskipTests' '-Djacoco.skip=true' '-Dcheckstyle.skip=true' '-Dpmd.skip=true' '-Dspotbugs.skip=true' '-o'
        if ($LASTEXITCODE -ne 0) { throw 'maven package failed' }
    }

    Write-Host 'compiling harness tools...'
    & javac '-proc:none' '-encoding' 'UTF-8' '-cp' $guiJar '-d' $classes `
        (Join-Path $tools 'ReferenceRunner.java') (Join-Path $tools 'NewEngineRunner.java') (Join-Path $tools 'CompareCsv.java')
    if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

    # ---------- start REST server ----------
    Write-Host 'starting gecko-rest-api...'
    $restProc = Start-Process -FilePath 'java' -ArgumentList '-Xmx1g', '-jar', $restJar `
        -WindowStyle Hidden -PassThru
    $up = $false
    foreach ($i in 1..60) {
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/gecko/api/v1/circuits/catalog" -TimeoutSec 2
            if ($r.StatusCode -eq 200) { $up = $true; break }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $up) { throw 'REST server did not come up' }

    # ---------- run all circuits ----------
    $results = @()
    foreach ($c in $circuits) {
        $name = $c.Name
        $ipes = Join-Path $tools "circuits\$name.ipes"
        $refCsv = Join-Path $work "$name-ref.csv"
        $newCsv = Join-Path $work "$name-new.csv"

        Write-Host "`n=== $name : legacy engine ==="
        & java -cp "$classes;$guiJar" ReferenceRunner $guiJar $ipes $refCsv $c.Signals $RmiPort
        $refOk = ($LASTEXITCODE -eq 0)

        Write-Host "=== $name : new engine ==="
        $newOk = $false
        if ($refOk) {
            & java -cp $classes NewEngineRunner $BaseUrl $ipes $newCsv
            $newOk = ($LASTEXITCODE -eq 0)
        }

        Write-Host "=== $name : comparison ==="
        if ($refOk -and $newOk) {
            $out = & java -cp $classes CompareCsv $refCsv $newCsv $RelTol $AbsTol
            $code = $LASTEXITCODE
            $out | Write-Host
            $results += [pscustomobject]@{ Circuit = $name; Result = if ($code -eq 0) { 'PASS' } else { 'FAIL' } }
        } else {
            Write-Host "SKIP (engine run failed: ref=$refOk new=$newOk)"
            $results += [pscustomobject]@{ Circuit = $name; Result = 'ERROR' }
        }
    }
} finally {
    if ($restProc) { Stop-JavaChildren $restProc.Id }
}

# ---------- report ----------
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$report = Join-Path $tools "results\$stamp.txt"
$lines = @()
$lines += "GeckoCIRCUITS parity report $stamp"
$lines += "tolerances: rel=$RelTol abs=$AbsTol (legacy scope path: float32 + decimation)"
$lines += ''
foreach ($r in $results) { $lines += ('{0,-16} {1}' -f $r.Circuit, $r.Result) }
$lines += ''
$allPass = (@($results | Where-Object { $_.Result -ne 'PASS' }).Count -eq 0)
$lines += if ($allPass) { 'PARITY: PASS' } else { 'PARITY: FAIL' }
$lines | Set-Content -Encoding utf8 $report
Write-Host "`nreport: $report"
exit ([int](-not $allPass))
