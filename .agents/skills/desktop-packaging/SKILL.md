---
name: desktop-packaging
description: Runbook for packaging GeckoCIRCUITS into native desktop installers (MSI, NSIS EXE, DMG, DEB, RPM, AppImage) using the modern Tauri 2 desktop shell pipeline and publishing CI releases.
---

# GeckoCIRCUITS Desktop Packaging Guide

## Tauri Desktop App (React UI + Embedded Engine — Primary Application)

The desktop app packages the React schematic editor with an embedded Java simulation engine sidecar and MCP server into native platform installers (no Java pre-installation required for end users).

### Building the Desktop Package

```sh
# 1. Build the engine bundle (JRE + REST API + MCP jars + smoke test)
python3 scripts/desktop/build-engine.py

# 2. Build full native installers for the current platform (requires Tauri CLI)
scripts/desktop/build-all.bat        # Windows: NSIS .exe, .msi in desktop/target/release/bundle
./scripts/desktop/build-all.sh       # Linux/macOS: .deb, .rpm, .AppImage, .dmg

# Helper scripts
python3 scripts/desktop/set-version.py X.Y.Z             # Sync version across tauri.conf.json & pom.xml
python3 scripts/desktop/write-mcp-launchers.py --dest <dir>  # Emit standalone MCP launchers
```

### Packaging Output Directory
Installers and bundles are generated in:
`desktop/target/release/bundle/`

### Supported Package Types
- **Windows**: `nsis` (`.exe` installer with WebView2 bootstrapper) and `msi`
- **macOS**: `dmg` (Intel x86_64 and Apple Silicon aarch64)
- **Linux**: `deb`, `rpm`, `appimage`

### CI Matrix Workflow
The active matrix packaging workflow is `.github/workflows/desktop.yml`.
Triggers:
- Tag push matching `v*` (automatically builds 3-OS installer matrix and publishes a GitHub Release with SHA256SUMS).
- Manual trigger via GitHub Actions `workflow_dispatch`.
- PR validation gates on all pull requests.

See `docs/desktop-app.md` for the full architecture guide, release QA checklist, and troubleshooting.
