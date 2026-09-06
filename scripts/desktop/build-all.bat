@echo off
rem One-shot local desktop build: engine bundle + Tauri installers.
rem Prereqs: JDK 25, Node, Rust (MSVC), tauri CLI (npm i -g @tauri-apps/cli)
setlocal
cd /d "%~dp0..\.."

python scripts\desktop\build-engine.py %*
if errorlevel 1 exit /b 1

cargo tauri build
exit /b %errorlevel%
