#!/usr/bin/env bash
# GeckoCIRCUITS Desktop Packaging Script for Linux and macOS
# Usage:
#   ./package-desktop.sh                  - Build default packages
#   ./package-desktop.sh --type portable  - Build only portable archive
#   ./package-desktop.sh --type deb       - Build Debian package (Linux)
#   ./package-desktop.sh --type dmg       - Build DMG package (macOS)
#   ./package-desktop.sh --rebuild        - Recompile Maven before packaging

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "${SCRIPT_DIR}/package-desktop.py" "$@"
