@echo off
REM ============================================================================
REM GeckoCIRCUITS Web Editor - Native Desktop Launcher
REM Launches the backend server and opens the web editor in a native app window.
REM ============================================================================
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%.."

call "%SCRIPT_DIR%..\run-web-editor.bat" %*
