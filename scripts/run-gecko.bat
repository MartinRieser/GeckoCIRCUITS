@echo off
REM GeckoCIRCUITS Launcher for Windows
REM
REM Usage:
REM   run-gecko.bat                    - Start GeckoCIRCUITS
REM   run-gecko.bat circuit.ipes       - Open a circuit file
REM   run-gecko.bat --hidpi            - Start with HiDPI scaling (4K displays)
REM   run-gecko.bat --hidpi circuit.ipes

setlocal enabledelayedexpansion

REM Script directory
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

REM JAR file location
set "JAR_FILE=%PROJECT_DIR%\target\gecko-1.0-jar-with-dependencies.jar"
if not exist "%JAR_FILE%" (
    set "JAR_FILE=%PROJECT_DIR%\src\modules\gecko-gui\target\gecko-1.0-jar-with-dependencies.jar"
)

REM Default JVM options
set "JVM_OPTS=-Xmx3G -Dpolyglot.js.nashorn-compat=true"

REM Parse arguments
set "CIRCUIT_FILE="
set "HIDPI="

:parse_args
if "%~1"=="" goto done_args
if /i "%~1"=="--hidpi" (
    set "HIDPI=1"
    shift
    goto parse_args
)
if /i "%~1"=="-h" (
    goto show_help
)
if /i "%~1"=="--help" (
    goto show_help
)
REM Assume it's a circuit file
set "CIRCUIT_FILE=%~1"
shift
goto parse_args

:done_args

REM Add HiDPI scaling if requested
if defined HIDPI (
    set "JVM_OPTS=%JVM_OPTS% -Dsun.java2d.uiScale=2"
)

REM Check if JAR exists
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found at %JAR_FILE%
    echo.
    echo Please build the project first:
    echo   cd %PROJECT_DIR%
    echo   mvn clean package assembly:single -DskipTests
    exit /b 1
)

REM Check Java installation (prefer JAVA_HOME if valid, otherwise PATH)
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
    where java >nul 2>&1
    if not errorlevel 1 (
        for /f "delims=" %%I in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%I"
    )
)

if not defined JAVA_EXE (
    echo Error: Java not found in PATH or JAVA_HOME
    echo Please install Java 25 or later
    exit /b 1
)

REM Verify Java version >= 25
set "JAVA_VER=0"
set "TMP_VER=%TEMP%\gecko_java_ver_%RANDOM%.tmp"
"%JAVA_EXE%" -version 2> "%TMP_VER%"
for /f "tokens=3" %%g in ('findstr /i "version" "%TMP_VER%" 2^>nul') do (
    for /f "delims=." %%v in ("%%~g") do set "JAVA_VER=%%v"
)
if exist "%TMP_VER%" del "%TMP_VER%" >nul 2>&1

if %JAVA_VER% lss 25 (
    echo Error: Java 25 or later is required. Found: Java %JAVA_VER% [%JAVA_EXE%]
    echo Please set JAVA_HOME or update PATH to point to JDK 25+.
    exit /b 1
)

REM Display startup info
echo ============================================
echo GeckoCIRCUITS Launcher
echo ============================================
echo JAR: %JAR_FILE%
if defined HIDPI echo HiDPI: enabled
if defined CIRCUIT_FILE echo Circuit: %CIRCUIT_FILE%
echo.

REM Run GeckoCIRCUITS
if defined CIRCUIT_FILE (
    "%JAVA_EXE%" %JVM_OPTS% -jar "%JAR_FILE%" "%CIRCUIT_FILE%"
) else (
    "%JAVA_EXE%" %JVM_OPTS% -jar "%JAR_FILE%"
)
goto :eof

:show_help
echo GeckoCIRCUITS Launcher for Windows
echo.
echo Usage:
echo   run-gecko.bat [options] [circuit.ipes]
echo.
echo Options:
echo   --hidpi     Enable HiDPI scaling for 4K displays
echo   -h, --help  Show this help message
echo.
echo Examples:
echo   run-gecko.bat                        Start GeckoCIRCUITS
echo   run-gecko.bat my_circuit.ipes        Open a circuit file
echo   run-gecko.bat --hidpi                Start with HiDPI scaling
echo   run-gecko.bat --hidpi circuit.ipes   HiDPI with circuit file
exit /b 0
