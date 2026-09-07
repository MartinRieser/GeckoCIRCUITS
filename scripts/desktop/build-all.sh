#!/usr/bin/env bash
# One-shot local desktop build: engine bundle + Tauri installers.
# Prereqs: JDK 25, Node, Rust, tauri CLI (npm i -g @tauri-apps/cli)
set -euo pipefail
cd "$(dirname "$0")/../.."

python3 scripts/desktop/build-engine.py "$@"
if command -v tauri &>/dev/null; then
    tauri build
elif command -v cargo-tauri &>/dev/null; then
    cargo tauri build
else
    npx @tauri-apps/cli build
fi
