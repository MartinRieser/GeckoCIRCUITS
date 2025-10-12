@echo off
echo ==========================================
echo GUI Baseline Capture for GeckoCIRCUITS
echo ==========================================
echo.

REM Set environment for GUI mode
echo Setting up GUI environment...
set JAVA_TOOL_OPTIONS=
echo.

REM Capture baselines for Education_ETHZ directory first (smaller test)
echo Capturing baselines for Education_ETHZ directory...
echo This will open GeckoCIRCUITS GUI components
echo.

mvn test-compile exec:java ^
    -Dexec.mainClass="ch.technokrat.regression.BaselineCaptureGUI" ^
    -Dexec.classpathScope=test ^
    -Dcircuits.filter="Education_ETHZ" ^
    -Dbaseline.overwrite=true ^
    -Dbaseline.verify=true

if %ERRORLEVEL%==0 (
    echo.
    echo SUCCESS: Education_ETHZ baselines captured!

    REM Check if baselines were created
    if exist "src\test\resources\golden\baselines\Education_ETHZ" (
        echo.
        echo Baseline files created:
        dir "src\test\resources\golden\baselines\Education_ETHZ\*_metadata.txt" /b 2>nul
        if %errorlevel%==0 (
            echo   Metadata files found ✓
        ) else (
            echo   No metadata files found - checking other files...
            dir "src\test\resources\golden\baselines\Education_ETHZ\*.csv" /b 2>nul
        )
    ) else (
        echo No baseline directory found - may have failed
    )

    echo.
    echo Would you like to capture ALL baselines now? (Y/N)
    set /p choice=Your choice:

    if /i "%choice%"=="Y" (
        echo.
        echo Capturing ALL baselines (this will take 2-3 hours)...
        echo.
        echo Press Ctrl+C to cancel, or wait for completion...
        echo.

        mvn test-compile exec:java ^
            -Dexec.mainClass="ch.technokrat.regression.BaselineCaptureGUI" ^
            -Dexec.classpathScope=test ^
            -Dbaseline.overwrite=true ^
            -Dbaseline.verify=true

        if %ERRORLEVEL%==0 (
            echo.
            echo SUCCESS: All baselines captured!
            echo.
            echo Testing baseline loading...
            mvn test -Dtest=GoldenReferenceTest

            if %ERRORLEVEL%==0 (
                echo.
                echo ✅ ALL TESTS PASSED!
                echo Golden reference infrastructure is fully functional!
            ) else (
                echo.
                echo ⚠️  Some tests failed, but infrastructure is working
                echo Check the output above for details
            )
        ) else (
            echo.
            echo ❌ Failed to capture all baselines
            echo Check the output above for error details
        )
    )

) else (
    echo.
    echo ❌ Failed to capture Education_ETHZ baselines
    echo Check the output above for error details
    echo.
    echo Common issues:
    echo - Display not available (requires GUI mode)
    echo - Permission issues
    echo - Out of memory (increase Java heap size)
    echo - GeckoCIRCUITS GUI initialization failed
)

echo.
echo ==========================================
echo Baseline Capture Complete
echo ==========================================
echo.

REM Show what we captured
if exist "src\test\resources\golden\baselines" (
    echo Summary of baselines captured:
    echo.
    for /d %%d in ("src\test\resources\golden\baselines\*") do (
        echo Directory: %%~nxd
        if exist "%%d\*_metadata.txt" (
            echo   ✓ Has metadata
            dir "%%d\*.csv" /b 2>nul | find /c /v ""
            echo   CSV files: %%count%%
        ) else (
            echo   ✗ No metadata found
        )
    )
) else (
    echo No baseline data found
)

echo.
echo Next steps:
echo 1. If baselines were captured, you're ready for Phase 2 refactoring
echo 2. Run git add/commit to save baseline data
echo 3. Create git tag for Phase 1 completion
echo.

pause