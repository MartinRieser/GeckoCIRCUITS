@echo off
rem One-shot local desktop build: engine bundle + Tauri installers.
rem Prereqs: JDK 25, Node, Rust (MSVC), tauri CLI (npm i -g @tauri-apps/cli)
setlocal
cd /d "%~dp0..\.."

rem Windows builds must use the MSVC toolchain (CI parity): GNU builds link
rem WebView2Loader.dll dynamically, which no installer ships.
rustup toolchain list 2>nul | findstr /c:"stable-x86_64-pc-windows-msvc" >nul
if %errorlevel% equ 0 (
    set RUSTUP_TOOLCHAIN=stable-x86_64-pc-windows-msvc
) else (
    echo ERROR: Rust MSVC toolchain missing. Run: rustup toolchain install stable-x86_64-pc-windows-msvc
    exit /b 1
)

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
