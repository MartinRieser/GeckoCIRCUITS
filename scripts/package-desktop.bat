@echo off
REM GeckoCIRCUITS Desktop Packaging Script for Windows
REM Usage:
REM   package-desktop.bat                  - Build all packages (MSI + Portable ZIP)
REM   package-desktop.bat --type portable - Build only portable ZIP
REM   package-desktop.bat --type msi      - Build only MSI installer
REM   package-desktop.bat --rebuild       - Recompile Maven before packaging

setlocal
set "SCRIPT_DIR=%~dp0"
python "%SCRIPT_DIR%package-desktop.py" %*
