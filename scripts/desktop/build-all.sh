#!/usr/bin/env bash
# One-shot local desktop build: engine bundle + Tauri installers.
# Prereqs: JDK 25, Node, Rust, tauri CLI (npm i -g @tauri-apps/cli)
set -euo pipefail
cd "$(dirname "$0")/../.."

python3 scripts/desktop/build-engine.py "$@"
cargo tauri build
