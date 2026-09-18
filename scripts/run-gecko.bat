@echo off
REM ============================================================================
REM GeckoCIRCUITS Launcher Bridge
REM
REM NOTE: The classic Swing GUI has been retired.
REM This script delegates to the modern GeckoCIRCUITS Web Editor / Desktop App.
REM ============================================================================
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

echo ============================================================================
echo   GeckoCIRCUITS
echo   [NOTE] The classic Swing GUI has been retired.
echo   Forwarding to the modern GeckoCIRCUITS Web Editor...
echo ============================================================================

call "%PROJECT_DIR%\run-web-editor.bat" %*
