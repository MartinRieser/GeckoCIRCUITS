# GeckoCIRCUITS Desktop App

The desktop app is the modern face of GeckoCIRCUITS: a native window (Tauri 2)
hosting the React editor, with the full simulation engine running as a hidden
local sidecar. It installs from a single file per OS and requires **no Java
installation** — a trimmed Java runtime ships inside the app.

```
┌─────────────────────────────── installer ┬──────────────────────────────┐
│  Tauri shell (native window)             │  engine/                     │
│  - spawns engine on 127.0.0.1:0          │  - gecko-rest-api.jar        │
│  - waits for GECKO_READY line            │    (embeds the React UI)     │
│  - injects backend URL into the webview  │  - gecko-mcp.jar (LLM tools) │
│  - native save dialogs, file association │  - runtime/ (jlink Java 25)  │
└──────────────────────────────────────────┴──────────────────────────────┘
```

## Installing

Download the installer for your OS from the
[Releases](https://github.com/tinix84/GeckoCIRCUITS/releases) page (assets named
`GeckoCIRCUITS_<version>_*` come from the desktop pipeline):

| OS | File | Notes |
|----|------|-------|
| Windows | `GeckoCIRCUITS_<v>_x64-setup.exe` (NSIS) or `.msi` | Downloads WebView2 automatically if missing |
| macOS | `GeckoCIRCUITS_<v>_x64.dmg` / `_aarch64.dmg` | Unsigned: right-click → Open on first start |
| Linux | `gecko-circuits_<v>_amd64.deb`, `*.rpm`, or `*.AppImage` | deb/rpm register the `.ipes` file association |

Double-clicking any `.ipes` circuit file opens it in the app.

## Architecture & startup handshake

1. The shell resolves `engine/runtime/bin/java` and `engine/gecko-rest-api.jar`.
2. It spawns the engine with `--server.port=0 --server.address=127.0.0.1
   --gecko.parent-pid=<shell pid>` (ephemeral port, localhost only).
3. The engine prints `GECKO_READY http://127.0.0.1:<port>/gecko` on stdout once
   its web server is up; the shell waits up to 90 s for that line.
4. The shell opens the editor window and injects `window.__GECKO_BACKEND__`;
   the frontend derives all API/WebSocket URLs from it (the engine's CORS
   allowlist covers the cross-origin calls).
5. If the shell dies, the engine's parent watchdog exits the JVM within ~5 s
   (exit code 71). If the engine dies, the shell shows an error dialog and
   points at the engine log.

## Building

Prerequisites: JDK 25, Node 22, Rust (MSVC on Windows), Tauri CLI
(`npm install -g @tauri-apps/cli`), Python 3.

```sh
# engine bundle only (runtime + jars + smoke test)
python3 scripts/desktop/build-engine.py

# full local build: engine bundle + installers into desktop/target/release/bundle
scripts/desktop/build-all.bat        # Windows
./scripts/desktop/build-all.sh       # Linux/macOS
```

`build-engine.py` derives the jlink module set from the fat jar (jdeps plus a
pinned safety list) and finishes with a smoke test: a real RC-lowpass
simulation must run through the bundled runtime and the Boot jar
(`PropertiesLauncher -Dloader.main`), producing CSV output.

**Dev mode** never spawns the sidecar: `cargo tauri dev` (in `desktop/app`)
loads the Vite dev server, which proxies to an engine started separately via
`run-web-editor.bat|.sh`.

### Versioning

`scripts/desktop/set-version.py <version>` syncs the version across
`tauri.conf.json`, the REST API's `application.properties`, and the frontend
`package.json`. The CI pipeline calls it with the `v*` tag before building.

## CI

`.github/workflows/desktop.yml`:

- **Pull requests**: Rust gates (rustfmt, clippy `-D warnings`, engine unit
  tests, full compile check) on Windows-MSVC — the official Tauri toolchain —
  plus Java and frontend tests on Linux.
- **Tags `v*` / manual dispatch**: 3-OS installer matrix (build engine →
  `cargo tauri build`), artifacts uploaded, release published with
  SHA256SUMS.

## Release QA checklist

Run once per release, per OS, on a machine **without Java installed**
(the point is to catch bundling regressions):

| # | Check | All OS |
|---|-------|--------|
| 1 | Installer completes; app appears in start menu / Applications / menu | ✅ |
| 2 | Launch shows a *Starting simulation engine…* splash immediately; editor ready within ~20 s | ✅ |
| 3 | Blank workspace auto-loads; placing a component and a wire works | ✅ |
| 4 | Loading an example and pressing *Run Simulation* draws waveforms | ✅ |
| 5 | Double-clicking a `.ipes` file opens it in the app | ✅ |
| 6 | Launching a second instance focuses the first and opens its file | Win/Linux |
| 7 | *Save* shows a native dialog; the written file re-opens correctly | ✅ |
| 8 | Quitting the app leaves **no** `java` process behind | ✅ |
| 9 | Killing the app via task manager → engine exits within ~10 s | ✅ |
| 10 | `gecko-mcp` launcher registers 10 tools in an LLM client (see [MCP](mcp.md)) | ✅ |
| 11 | Uninstall removes files and the file association (Windows) | Win only |

Log failures against the engine log (below) — it contains the engine's full
stdout/stderr including the `GECKO_READY` handshake line.

## Troubleshooting

**Where are the engine logs?** `<app log dir>/logs/engine/engine.log`:

- Windows: `%APPDATA%\com.geckocircuits.desktop\logs\engine\`
- macOS: `~/Library/Logs/com.geckocircuits.desktop/logs/engine/`
- Linux: `~/.local/share/com.geckocircuits.desktop/logs/engine/`

The in-app *Open Logs Folder* command opens this directory directly.

**"Engine failed to start" dialog** — check the log for the failure reason.
Common causes: antivirus quarantining the bundled runtime, a port/permission
problem, or a corrupted install (re-run `build-engine.py` if local).

**Windows SmartScreen / macOS Gatekeeper warnings** — the binaries are not
code-signed yet; choose *More info → Run anyway* (Windows) or right-click →
*Open* (macOS). Signing is a planned follow-up.

**App starts but the editor shows a startup error screen** — the shell is
running but the engine never became healthy. The screen appears after the
60 s health-timeout; check the engine log.
