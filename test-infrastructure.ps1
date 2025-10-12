# PowerShell script to test the golden reference infrastructure
# Usage: .\test-infrastructure.ps1

Write-Host "========================================" -ForegroundColor Green
Write-Host "Testing Golden Reference Infrastructure" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Step 1: Check that we have circuit files
Write-Host ""
Write-Host "Step 1: Checking for circuit files..." -ForegroundColor Blue
$circuitFiles = Get-ChildItem -Path "resources" -Recurse -Filter "*.ipes"
$circuitCount = $circuitFiles.Count
Write-Host "Found $circuitCount circuit files"

if ($circuitCount -eq 0) {
    Write-Host "ERROR: No circuit files found in resources/ directory" -ForegroundColor Red
    exit 1
}

# Step 2: Try to capture baseline for one circuit in Education_ETHZ
Write-Host ""
Write-Host "Step 2: Capturing baseline for sample circuit..." -ForegroundColor Blue
Write-Host "(This may take 1-2 minutes)" -ForegroundColor Yellow
Write-Host ""

$BASELINE_CMD = "mvn test-compile exec:java " +
                 "-Dexec.mainClass=`"ch.technokrat.regression.BaselineCapture`" " +
                 "-Dexec.classpathScope=test " +
                 "-Dcircuits.filter=`"Education_ETHZ`" " +
                 "-Dbaseline.overwrite=true"

Write-Host "Running: $BASELINE_CMD" -ForegroundColor Gray
Invoke-Expression $BASELINE_CMD | Out-File -FilePath "test-baseline-capture.log" -Encoding UTF8

# Check if baseline was created
$BASELINE_DIR = "src/test/resources/golden/baselines"
if (Test-Path $BASELINE_DIR) {
    $baselineCount = (Get-ChildItem -Path $BASELINE_DIR -Recurse -Filter "_metadata.txt").Count
    Write-Host ""
    Write-Host "Baseline files created: $baselineCount"

    if ($baselineCount -gt 0) {
        Write-Host "✓ Baseline capture successful!" -ForegroundColor Green

        # Show what was created
        Get-ChildItem -Path $BASELINE_DIR -Recurse -Filter "_metadata.txt" | ForEach-Object {
            $dir = $_.Directory.FullName
            $files = Get-ChildItem -Path $dir -File
            Write-Host "  Circuit: $($_.Directory.Name)" -ForegroundColor Cyan
            $files | ForEach-Object {
                Write-Host "    $($_.Name) ($($_.Length) bytes)" -ForegroundColor White
            }
        }
    } else {
        Write-Host "✗ No baselines were created" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✗ Baseline directory not created" -ForegroundColor Red
    exit 1
}

# Step 3: Run golden reference test
Write-Host ""
Write-Host "Step 3: Running golden reference test..." -ForegroundColor Blue
Write-Host ""

$TEST_CMD = "mvn test -Dtest=GoldenReferenceTest"
Write-Host "Running: $TEST_CMD" -ForegroundColor Gray
Invoke-Expression $TEST_CMD | Out-File -FilePath "test-golden-reference.log" -Encoding UTF8

# Check test results
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✓ All tests PASSED!" -ForegroundColor Green
    Write-Host "Infrastructure is working correctly" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    exit 0
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "✗ Tests FAILED" -ForegroundColor Red
    Write-Host "Check test-golden-reference.log for details" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}