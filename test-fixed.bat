@echo off
echo Testing golden reference infrastructure (Windows fixed version)...
echo.

REM Set headless mode to avoid GUI issues
set JAVA_TOOL_OPTIONS=-Djava.awt.headless=true

echo Capturing baseline for Education_ETHZ/ex_1.ipes...
echo (This may take 1-3 minutes per circuit)
echo.

mvn test-compile exec:java ^
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" ^
    -Dexec.classpathScope=test ^
    -Dcircuits.filter="Education_ETHZ" ^
    -Dbaseline.overwrite=true ^
    -Djava.awt.headless=true

if exist "src\test\resources\golden\baselines\Education_ETHZ" (
    echo.
    echo SUCCESS: Baseline directory created!
    echo.
    echo Files created:
    dir "src\test\resources\golden\baselines\Education_ETHZ" /s /b 2>nul
    if %errorlevel% neq 0 (
        echo Directory exists but no files found - checking metadata...
        dir "src\test\resources\golden\baselines\Education_ETHZ\*_metadata.txt" /b 2>nul
    )
    echo.
    echo Testing baseline loading...
    mvn test -Dtest=GoldenReferenceTest -Dcircuit="Education_ETHZ/ex_1.ipes" -Djava.awt.headless=true

    if %ERRORLEVEL%==0 (
        echo.
        echo SUCCESS: All tests PASSED!
        echo Golden reference infrastructure is working correctly!
        echo.
        echo Infrastructure Status: READY FOR REFACTORING
    ) else (
        echo.
        echo WARNING: Tests did not pass, but infrastructure may be working
        echo Check the output above for details
        echo Some circuits may fail due to missing scopes or other issues
    )
) else (
    echo.
    echo FAILED: No baseline directory created
    echo Check the output above for errors
    echo.
    echo Common issues:
    echo - GraalVM JavaScript path issues (may show warnings)
    echo - GUI initialization warnings (normal in headless mode)
    echo - Circuit may not have scope elements
)

echo.
echo Test complete.