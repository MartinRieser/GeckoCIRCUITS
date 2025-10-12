# PowerShell script to test golden reference infrastructure with single circuit
# Usage: .\test-single-circuit.ps1

Write-Host "Testing golden reference infrastructure with single circuit..." -ForegroundColor Green

# Test with Education_ETHZ/ex_1.ipes (simple educational circuit)
$CIRCUIT = "Education_ETHZ/ex_1.ipes"
Write-Host "Testing circuit: $CIRCUIT" -ForegroundColor Yellow

# Create log file
$LOG_FILE = "test-single-result.log"
Write-Host "Log file: $LOG_FILE"

# Run baseline capture for just this circuit
Write-Host "Running baseline capture..." -ForegroundColor Blue

# Execute Maven command directly (avoiding complex escaping)
& mvn test-compile exec:java `
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" `
    -Dexec.classpathScope=test `
    -Dcircuits.filter="Education_ETHZ" `
    -Dbaseline.overwrite=true | Out-File -FilePath $LOG_FILE -Encoding UTF8

# Check if baseline was created
$BASELINE_DIR = "src/test/resources/golden/baselines/Education_ETHZ"
if (Test-Path $BASELINE_DIR) {
    Write-Host "✅ SUCCESS: Baseline directory created!" -ForegroundColor Green

    # List files created
    Write-Host "Files created:" -ForegroundColor Yellow
    Get-ChildItem -Path $BASELINE_DIR -Recurse -File | ForEach-Object {
        Write-Host "  $($_.FullName)" -ForegroundColor White
        Write-Host "    Size: $($_.Length) bytes" -ForegroundColor Gray
    }

    # Test loading the baseline
    Write-Host ""
    Write-Host "Testing baseline loading..." -ForegroundColor Blue

    Write-Host "Running: mvn test -Dtest=GoldenReferenceTest -Dcircuit=`"$CIRCUIT`""
    & mvn test -Dtest=GoldenReferenceTest -Dcircuit="$CIRCUIT" | Out-File -Append -FilePath $LOG_FILE -Encoding UTF8

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ All tests PASSED!" -ForegroundColor Green
        Write-Host "Golden reference infrastructure is working correctly!" -ForegroundColor Green
    } else {
        Write-Host "❌ Tests FAILED!" -ForegroundColor Red
        Write-Host "Check $LOG_FILE for details" -ForegroundColor Red
    }
} else {
    Write-Host "❌ FAILED: No baseline directory created" -ForegroundColor Red
    Write-Host "Check $LOG_FILE for errors" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test complete. Check $LOG_FILE for full output." -ForegroundColor Cyan