---
name: desktop-packaging
description: Runbook for packaging GeckoCIRCUITS into native desktop installers (MSI, DMG, DEB, RPM, portable ZIP/tarball) using jpackage and publishing CI releases.
---

# GeckoCIRCUITS Desktop Packaging Guide

## Tauri Desktop App (React UI — current primary app)

```sh
python3 scripts/desktop/build-engine.py      # engine bundle + smoke test
scripts/desktop/build-all.bat|.sh            # engine + installers (needs tauri CLI)
python3 scripts/desktop/set-version.py X.Y.Z # sync version files
python3 scripts/desktop/write-mcp-launchers.py --dest <dir>  # MCP launchers
```

CI: `.github/workflows/desktop.yml` (PR gates + 3-OS installer matrix on `v*`).
QA checklist and troubleshooting: `docs/desktop-app.md`. The jpackage flow
below packages the **Classic Swing UI** and stays available in parallel.


## Quick Execution
- **Windows**: `scripts\package-desktop.bat` (or `python scripts/package-desktop.py --type msi,portable`)
- **Linux**: `./scripts/package-desktop.sh` (or `python3 scripts/package-desktop.py --type deb,rpm,portable`)
- **macOS**: `./scripts/package-desktop.sh` (or `python3 scripts/package-desktop.py --type dmg,portable`)

## Packaging Output Directory
All packages are output to `dist/` at the repository root.

## Supported Package Types
- `app-image`: Self-contained application directory with bundled JRE
- `portable`: `.zip` (Windows/macOS) or `.tar.gz` (Linux) containing the application image
- `msi`: Windows installer MSI package (requires WiX Toolset 3.14+)
- `exe`: Windows installer EXE package
- `dmg`: macOS disk image installer
- `pkg`: macOS PKG installer
- `deb`: Debian/Ubuntu package (requires `dpkg-deb`)
- `rpm`: Fedora/RHEL package (requires `rpmbuild`)
- `all`: Build both installer and portable package for current platform

## CI Matrix Workflow
The matrix packaging workflow is located at `.github/workflows/package-desktop.yml`.
Triggers:
- Tag push matching `v*` (automatically builds and publishes a GitHub Release).
- Manual trigger via GitHub Actions `workflow_dispatch` (optional checkbox to publish release).
