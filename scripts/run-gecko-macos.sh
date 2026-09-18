#!/usr/bin/env bash
# ============================================================================
# GeckoCIRCUITS Launcher Bridge (macOS)
#
# NOTE: The classic Swing GUI has been retired.
# This script delegates to the modern GeckoCIRCUITS Web Editor / Desktop App.
# ============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================================================"
echo "  GeckoCIRCUITS"
echo "  [NOTE] The classic Swing GUI has been retired."
echo "  Forwarding to the modern GeckoCIRCUITS Web Editor..."
echo "============================================================================"

exec "$PROJECT_DIR/run-web-editor.sh" "$@"
