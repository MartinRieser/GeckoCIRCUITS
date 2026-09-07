@echo off
rem One-shot local desktop build: engine bundle + Tauri installers.
rem Prereqs: JDK 25, Node, Rust (MSVC), tauri CLI (npm i -g @tauri-apps/cli)
setlocal
cd /d "%~dp0..\.."

python scripts\desktop\build-engine.py %*
if errorlevel 1 exit /b 1

where tauri >nul 2>nul
if %errorlevel% equ 0 (
    tauri build
    exit /b %errorlevel%
)
where cargo-tauri >nul 2>nul
if %errorlevel% equ 0 (
    cargo tauri build
    exit /b %errorlevel%
)
npx @tauri-apps/cli build
exit /b %errorlevel%
