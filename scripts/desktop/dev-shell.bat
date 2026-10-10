@echo off
REM ============================================================================
REM GeckoCIRCUITS - Desktop shell dev loop
REM   backend (8080)  <-  Vite dev server (5173, proxies /gecko)  <-  debug Tauri shell
REM Use this to test desktop-only features (native dialogs). run-gecko.bat stays
REM the browser-window launcher.
REM ============================================================================
setlocal enabledelayedexpansion
set "ROOT=%~dp0..\..\"
cd /d "%ROOT%"

set "REST_JAR=%CD%\backend\gecko-rest-api\target\gecko-rest-api.jar"
set "EXE=%CD%\desktop\target\debug\gecko-desktop.exe"

REM 1. Java 25 (JAVA_HOME, then ~/.jdks/jdk-25*)
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA for /f "delims=" %%D in ('dir /b /ad "%USERPROFILE%\.jdks\jdk-25*" 2^>nul') do (
    if not defined JAVA if exist "%USERPROFILE%\.jdks\%%D\bin\javaw.exe" set "JAVA=%USERPROFILE%\.jdks\%%D\bin\javaw.exe"
)
if not defined JAVA (
    echo [ERROR] Java 25 not found. Set JAVA_HOME.
    exit /b 1
)

REM 2. Backend on 8080
call :port_open 8080
if errorlevel 1 (
    if not exist "%REST_JAR%" (
        echo [ERROR] %REST_JAR% missing. Run: run-gecko.bat --rebuild
        exit /b 1
    )
    echo [INFO] Starting backend on 8080...
    start "" "!JAVA!" -Duser.language=en -Duser.country=US -Xmx2g -jar "%REST_JAR%"
    call :wait_port 8080 || (echo [ERROR] Backend did not start. & exit /b 1)
) else (
    echo [INFO] Backend already running on 8080.
)

REM 3. Vite dev server on 5173
call :port_open 5173
if errorlevel 1 (
    echo [INFO] Starting Vite dev server on 5173...
    start "GeckoCIRCUITS Vite" /min cmd /c "cd /d "%CD%\frontend" && npm run dev -- --port 5173 --strictPort"
    call :wait_port 5173 || (echo [ERROR] Vite did not start. & exit /b 1)
) else (
    echo [INFO] Vite already running on 5173.
)

REM 4. Build + launch the debug shell (single-instance: close old windows first)
taskkill /im gecko-desktop.exe /f >nul 2>&1
echo [INFO] Building desktop shell (debug)...
cargo build --manifest-path "%CD%\desktop\app\Cargo.toml" -q || (echo [ERROR] cargo build failed. & exit /b 1)
echo [INFO] Launching desktop shell...
start "" "%EXE%" %*
exit /b 0

:port_open
netstat -ano -p tcp | findstr /r /c:":%1 .*LISTENING" >nul 2>&1 && exit /b 0
netstat -ano -p tcpv6 | findstr /r /c:":%1 .*LISTENING" >nul 2>&1 && exit /b 0
exit /b 1

:wait_port
for /L %%i in (1,1,60) do (
    call :port_open %1
    if not errorlevel 1 exit /b 0
    ping 127.0.0.1 -n 2 >nul
)
exit /b 1
