# GeckoCIRCUITS Desktop Packaging Guide

This guide explains how GeckoCIRCUITS is packaged as native desktop applications for Windows, macOS, and Linux using JDK's `jpackage` tool and GitHub Actions.

---

## Overview

GeckoCIRCUITS provides native, self-contained packages:
- **No separate Java installation required** -- all packages bundle an optimized, minimal Java Runtime Environment (JRE).
- **Native OS integration** -- file associations (`.ipes`), Start Menu / Applications menu shortcuts, desktop icons, and uninstallers.
- **Portable zero-install options** -- extract-and-run archives for USB drives or systems without admin rights.

---

## Available Packages

| Platform | Installer Package | Portable / Zero-Install | Desktop Integration |
|---|---|---|---|
| **Windows** (x64) | `GeckoCIRCUITS-<version>.msi` | `GeckoCIRCUITS-<version>-windows-x64-portable.zip` | Start Menu, Desktop Shortcut, `.ipes` association |
| **macOS** (Apple Silicon & Intel) | `GeckoCIRCUITS-<version>.dmg` | `GeckoCIRCUITS-<version>-macos-app.zip` | Drag-and-Drop to Applications, Dock icon |
| **Linux** (Debian/Ubuntu) | `geckocircuits_<version>-1_amd64.deb` | `GeckoCIRCUITS-<version>-linux-x64-portable.tar.gz` | App Menu (`Science;Electronics;Engineering`), `.desktop` file |
| **Linux** (Fedora/RHEL) | `geckocircuits-<version>-1.x86_64.rpm` | `GeckoCIRCUITS-<version>-linux-x64-portable.tar.gz` | App Menu (`Science;Electronics;Engineering`), `.desktop` file |
| **Universal (All OS)** | — | Fat JAR: `gecko-1.0-jar-with-dependencies.jar` | Run via `java -jar` (requires Java 25+) |

---

## Tauri Desktop App (new UI)

The React-editor desktop app is packaged with Tauri 2 (separate from the
jpackage/Classic flow below):

```sh
# engine bundle (jlink runtime + REST jar + MCP jar + smoke test)
python3 scripts/desktop/build-engine.py

# engine + installers in one go (needs Tauri CLI: npm i -g @tauri-apps/cli)
scripts/desktop/build-all.bat|.sh

# release version across tauri.conf.json / application.properties / package.json
python3 scripts/desktop/set-version.py 1.2.3
```

Outputs land in `desktop/target/release/bundle/`. CI: `.github/workflows/desktop.yml`
(3-OS matrix on `v*` tags, SHA256SUMS, GitHub release). Details, including the
release QA checklist: [desktop-app.md](desktop-app.md).



You can package GeckoCIRCUITS locally on any OS.

### Prerequisites
- **JDK 25+** (Adoptium Temurin or Oracle JDK) with `jpackage` in `PATH`
- **Maven 3.6+**
- **Python 3.8+** with `Pillow` installed (`pip install Pillow`)
- Platform-specific tools:
  - **Windows**: [WiX Toolset v3.11](https://wixtoolset.org/) (required for `.msi` installers: `choco install wixtoolset -y`)
  - **Linux**: `rpm` (for RPM packages: `sudo apt install rpm fakeroot`)
  - **macOS**: Xcode Command Line Tools (preinstalled)

### Build Commands

#### Windows
```cmd
REM Build both MSI installer and Portable ZIP
scripts\package-desktop.bat

REM Build only the portable ZIP bundle
scripts\package-desktop.bat --type portable

REM Build only the MSI installer
scripts\package-desktop.bat --type msi

REM Rebuild Maven JAR before packaging
scripts\package-desktop.bat --rebuild
```

#### macOS / Linux
```bash
# Build all packages for current OS
./scripts/package-desktop.sh

# Build only portable archive
./scripts/package-desktop.sh --type portable

# Build specific installer type (deb, rpm, dmg)
./scripts/package-desktop.sh --type deb
./scripts/package-desktop.sh --type dmg

# Rebuild Maven JAR before packaging
./scripts/package-desktop.sh --rebuild
```

#### Python CLI (Cross-Platform)
```bash
python3 scripts/package-desktop.py --version 1.0.0 --type all --dest dist/
```

---

## Automated GitHub Actions Pipeline

The workflow `.github/workflows/package-desktop.yml` automates cross-platform builds:

1. **Matrix Build**:
   Spawns 3 parallel runners:
   - `windows-latest` -> Builds `.msi` and `-windows-x64-portable.zip`
   - `macos-latest` -> Builds `.dmg` and `-macos-app.zip`
   - `ubuntu-latest` -> Builds `.deb`, `.rpm`, and `-linux-x64-portable.tar.gz`

2. **Artifacts & Checksums**:
   - Computes `SHA256SUMS.txt` for integrity verification.
   - Uploads all binaries as workflow artifacts.

3. **Release Publishing**:
   - When a Git tag matching `v*` (e.g., `v1.0.0`) is pushed, a GitHub Release is automatically published with all binary packages attached.
   - Can also be manually dispatched from the GitHub Actions tab.
