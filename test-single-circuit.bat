@echo off
echo Testing golden reference infrastructure with single circuit...
echo.

REM Test with Education_ETHZ/ex_1.ipes
echo Capturing baseline for Education_ETHZ/ex_1.ipes...
mvn test-compile exec:java ^
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" ^
    -Dexec.classpathScope=test ^
    -Dcircuits.filter="Education_ETHZ" ^
    -Dbaseline.overwrite=true

if exist "src\test\resources\golden\baselines\Education_ETHZ" (
    echo.
    echo SUCCESS: Baseline directory created!
    echo.
    echo Files created:
    dir "src\test\resources\golden\baselines\Education_ETHZ" /s /b
    echo.
    echo Testing baseline loading...
    mvn test -Dtest=GoldenReferenceTest -Dcircuit="Education_ETHZ/ex_1.ipes"

    if %ERRORLEVEL%==0 (
        echo.
        echo SUCCESS: All tests PASSED!
        echo Golden reference infrastructure is working correctly!
    ) else (
        echo.
        echo FAILED: Tests did not pass
        echo Check the output above for details
    )
) else (
    echo.
    echo FAILED: No baseline directory created
    echo Check the output above for errors
)

echo.
echo Test complete.