---
title: Installation
description: Install the GeckoCIRCUITS desktop app on Windows, Linux, or macOS
---

# Installation

GeckoCIRCUITS ships as a self-contained desktop application for Windows,
Linux, and macOS. **No Java installation is required** — the simulation
engine ships inside the app.

## Recommended: Desktop Installers

Download the installer for your operating system from the
[Releases](https://github.com/MartinRieser/GeckoCIRCUITS/releases) page
(assets named `GeckoCIRCUITS_<version>_*`):

| OS | File | Notes |
|----|------|-------|
| Windows | `GeckoCIRCUITS_<v>_x64-setup.exe` or `.msi` | WebView2 downloads automatically if missing; `.ipes` file association registered |
| macOS | `GeckoCIRCUITS_<v>_x64.dmg` / `_aarch64.dmg` | Unsigned builds: right-click → **Open** on first start |
| Linux | `gecko-circuits_<v>_amd64.deb`, `*.rpm`, or `*.AppImage` | deb/rpm register the `.ipes` file association |

After installing:

- Launch **GeckoCIRCUITS** from the start menu, Applications folder, or app
  launcher. A *Starting simulation engine…* splash appears for a few seconds
  while the bundled engine boots, then the editor opens.
- Double-clicking any `.ipes` circuit file opens it in the app.
- The **C library interface header** (`gecko_c_block.h`) ships in the
  installation's `engine/` folder for [NativeC blocks](../native-c-blocks.md).

### First-launch troubleshooting

| Symptom | Fix |
|---------|-----|
| Windows SmartScreen warning | *More info* → *Run anyway* (builds are not code-signed yet) |
| macOS "cannot be opened" | Right-click the app → *Open*, or allow it in *System Settings → Privacy & Security* |
| "Simulation engine failed to start" | Open the engine log via `Help ▸ Open Logs Folder`; details in the [Desktop App guide](../desktop-app.md) |

## Alternative: Run from Source (Web Editor)

Developers can run the editor + engine from a repository checkout. This needs
**JDK 25**, **Node.js 22**, and **Maven**:

```bash
# builds the engine jar if missing, starts it on localhost:8080,
# and opens the editor in your browser
run-web-editor.bat        # Windows
./run-web-editor.sh       # Linux / macOS
```

The web editor is the same React application the desktop app embeds, served
against the same simulation engine — circuits and workflows are identical.

## Classic Swing UI (Deprecated / Slated for Removal)

The original Swing-based desktop UI is legacy and being phased out. All active development, new UI features, and current `.ipes` format workflows are centered on the modern **GeckoCIRCUITS Desktop App** (Tauri 2 + React Canvas) and **Web Editor**. The legacy launcher scripts (`scripts/run-gecko.bat|.sh`) do not support newer editor workflows and will be retired. All users should use the modern desktop app or `run-web-editor.bat|.sh`.

## Docker

The headless REST API and simulation engine can be run as a containerized service using the provided Docker configuration:

```bash
# Build and start the REST API container via Compose
docker compose -f docker/docker-compose.yml up -d

# Or build and run the image directly
docker build -f docker/Dockerfile.api -t geckocircuits-api:latest .
docker run -p 8080:8080 geckocircuits-api:latest
```

Once running, the API is available at `http://localhost:8080/api/v1/`. See the [REST API Reference](../api/rest-api.md) for endpoint details.
