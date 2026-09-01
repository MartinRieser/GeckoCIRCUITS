#!/usr/bin/env bash
# ============================================================================
# GeckoCIRCUITS Web Editor - Launcher for macOS & Linux (scripts/ folder)
# Launches the backend server and opens the web editor in your browser.
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

exec "$PROJECT_DIR/run-web-editor.sh" "$@"
