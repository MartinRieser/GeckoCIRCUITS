@echo off
REM ============================================================================
REM GeckoCIRCUITS Web Editor - Native Desktop Launcher
REM Launches the backend server and opens the web editor in a native app window.
REM ============================================================================
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "REST_JAR=%SCRIPT_DIR%src\modules\gecko-rest-api\target\gecko-rest-api-1.0.0.jar"
set "PORT=8080"
set "URL=http://localhost:%PORT%/gecko/"

echo ============================================
echo   GeckoCIRCUITS Web Editor
echo ============================================

REM 1. Find Java 25 or later
set "JAVA_EXE="
set "JAVA_VER=0"

REM Check JAVA_HOME first
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    call :check_java "%JAVA_HOME%\bin\java.exe"
)

REM Search user .jdks directory
if not defined JAVA_EXE (
    for /f "delims=" %%D in ('dir /b /ad "%USERPROFILE%\.jdks\jdk-25*" 2^>nul') do (
        if not defined JAVA_EXE if exist "%USERPROFILE%\.jdks\%%D\bin\java.exe" (
            call :check_java "%USERPROFILE%\.jdks\%%D\bin\java.exe"
        )
    )
)

REM Search Program Files for Java 25
if not defined JAVA_EXE (
    for /f "delims=" %%D in ('dir /b /ad "%ProgramFiles%\Java\jdk-25*" 2^>nul') do (
        if not defined JAVA_EXE if exist "%ProgramFiles%\Java\%%D\bin\java.exe" (
            call :check_java "%ProgramFiles%\Java\%%D\bin\java.exe"
        )
    )
)
if not defined JAVA_EXE (
    for /f "delims=" %%D in ('dir /b /ad "%ProgramFiles%\Eclipse Adoptium\jdk-25*" 2^>nul') do (
        if not defined JAVA_EXE if exist "%ProgramFiles%\Eclipse Adoptium\%%D\bin\java.exe" (
            call :check_java "%ProgramFiles%\Eclipse Adoptium\%%D\bin\java.exe"
        )
    )
)

REM Check PATH
if not defined JAVA_EXE (
    where java >nul 2>&1
    if not errorlevel 1 (
        for /f "delims=" %%I in ('where java 2^>nul') do (
            if not defined JAVA_EXE call :check_java "%%I"
        )
    )
)

if not defined JAVA_EXE (
    echo [ERROR] Java 25 or later is required, but none was found.
    echo Please install JDK 25 or set JAVA_HOME to point to a JDK 25+ installation.
    pause
    exit /b 1
)

echo [INFO] Using Java !JAVA_VER! [!JAVA_EXE!]
set "JAVA_HOME=%JAVA_EXE:\bin\java.exe=%"
set "JAVAW_EXE=%JAVA_EXE:java.exe=javaw.exe%"
if not exist "%JAVAW_EXE%" set "JAVAW_EXE=%JAVA_EXE%"

REM 2. Check if JAR exists, build if missing
if not exist "%REST_JAR%" (
    echo [INFO] Building GeckoCIRCUITS Web Editor package...
    call mvn -pl src/modules/gecko-rest-api -am package -DskipTests -q
    if errorlevel 1 (
        echo [ERROR] Build failed. Please ensure Maven and JDK are installed.
        pause
        exit /b 1
    )
)

REM 3. Check if server is already running on port 8080
powershell -Command "try { $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:%PORT%/gecko/api/v1/circuits/catalog' -TimeoutSec 1; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Starting GeckoCIRCUITS Server...
    start /B "" "%JAVAW_EXE%" -Xmx2g -jar "%REST_JAR%" >nul 2>&1
    
    REM Wait for server to become ready
    set "READY=0"
    for /L %%i in (1,1,30) do (
        if !READY! equ 0 (
            ping 127.0.0.1 -n 2 >nul
            powershell -Command "try { $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:%PORT%/gecko/api/v1/circuits/catalog' -TimeoutSec 1; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
            if not errorlevel 1 (
                set "READY=1"
            )
        )
    )
    if !READY! equ 0 (
        echo [WARNING] Server startup timed out, attempting to open anyway...
    )
) else (
    echo [INFO] Server is already running.
)

REM 4. Launch in Native App Window mode (Edge, Chrome, or Default Browser)
echo [INFO] Launching GeckoCIRCUITS window...

REM Check Chrome in Program Files
if exist "%ProgramFiles%\Google\Chrome\Application\chrome.exe" (
    start "" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" --app="%URL%" --app-window-size=1400,900
    goto :done
)
if exist "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" (
    start "" "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" --app="%URL%" --app-window-size=1400,900
    goto :done
)
if exist "%LocalAppData%\Google\Chrome\Application\chrome.exe" (
    start "" "%LocalAppData%\Google\Chrome\Application\chrome.exe" --app="%URL%" --app-window-size=1400,900
    goto :done
)

REM Check Edge in Program Files
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --app="%URL%" --app-window-size=1400,900
    goto :done
)
if exist "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" --app="%URL%" --app-window-size=1400,900
    goto :done
)
if exist "%LocalAppData%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%LocalAppData%\Microsoft\Edge\Application\msedge.exe" --app="%URL%" --app-window-size=1400,900
    goto :done
)

REM Check PATH for Edge or Chrome
where msedge >nul 2>&1
if not errorlevel 1 (
    start "" msedge --app="%URL%" --app-window-size=1400,900
    goto :done
)
where chrome >nul 2>&1
if not errorlevel 1 (
    start "" chrome --app="%URL%" --app-window-size=1400,900
    goto :done
)

REM Fallback to default browser
start "" "%URL%"

:done
echo [INFO] GeckoCIRCUITS Web Editor started.
exit /b 0

:check_java
set "CAND_EXE=%~1"
set "CAND_VER=0"
set "TMP_VER=%TEMP%\gecko_java_ver_%RANDOM%.tmp"
"%CAND_EXE%" -version 2> "%TMP_VER%"
for /f "tokens=3" %%g in ('findstr /i "version" "%TMP_VER%" 2^>nul') do (
    for /f "delims=." %%v in ("%%~g") do set "CAND_VER=%%v"
)
if exist "%TMP_VER%" del "%TMP_VER%" >nul 2>&1
if !CAND_VER! geq 25 (
    set "JAVA_EXE=%CAND_EXE%"
    set "JAVA_VER=!CAND_VER!"
)
exit /b

