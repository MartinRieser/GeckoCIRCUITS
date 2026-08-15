---
name: desktop-packaging
description: Runbook for packaging GeckoCIRCUITS into native desktop installers (MSI, DMG, DEB, RPM, portable ZIP/tarball) using jpackage and publishing CI releases.
---

# GeckoCIRCUITS Desktop Packaging Guide

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
