# GeckoCIRCUITS Desktop Shell

Tauri 2 wrapper that turns the existing React editor + Java engine into a
standalone desktop application. The shell spawns the bundled engine
(`gecko-rest-api.jar` on a bundled jlink Java 25 runtime) as a hidden
localhost-only sidecar, waits for its `GECKO_READY <url>` line, then opens
the editor window with the backend origin injected into the webview.

## Layout

| Path | Purpose |
|------|---------|
| `engine/` | `gecko-engine` crate: pure logic (readiness handshake, engine process handling, filename sanitizing). Std-only, fully unit-tested, no Tauri dependency — builds on any Rust toolchain. |
| `app/` | `gecko-desktop` crate: the Tauri shell (window, commands, download handling). Compiles with the official MSVC toolchain on Windows; CI gates it. |
| `app/icons/` | Generated from `_build/resources/GeckoCIRCUITS.png`. |
| `app/engine/` | Build output: `gecko-rest-api.jar` + `gecko-mcp.jar` + jlink runtime (via `scripts/desktop/build-engine.py`). |
| `src/modules/gecko-mcp/` | Bundled MCP server (10 tools, ported 1:1 from `tools/mcp/gecko_mcp`); Python original stays for repo development. |

## Build & test

```sh
# unit tests (engine crate: any toolchain, no display needed)
cargo test -p gecko-engine

# compile / run the shell in dev mode (needs tauri CLI: cargo install tauri-cli --version "^2")
cargo tauri dev    # debug: no sidecar; talks to Vite dev server + engine from run-web-editor
cargo tauri build  # release: full installer (engine bundling lands in Phase 2)
```

Debug builds never spawn the engine: the window loads `http://localhost:5173`
(Vite) and talks to an engine started separately (`run-web-editor.bat`).
Release builds spawn the bundled engine on `127.0.0.1:<ephemeral port>`.

## Startup handshake

1. Shell resolves `engine/runtime/bin/java` + `engine/gecko-rest-api.jar`.
2. Spawns: `java -Xmx2g -jar gecko-rest-api.jar --server.port=0
   --server.address=127.0.0.1 --gecko.parent-pid=<shell pid>`.
3. Java prints `GECKO_READY http://127.0.0.1:<port>/gecko` on stdout
   (`EngineReadyLogger`); the shell parses it (90 s budget).
4. Shell opens the main window with `window.__GECKO_BACKEND__` injected;
   the frontend derives all API/WebSocket URLs from it (CORS allowlist in
   `CorsConfig.java` covers the cross-origin calls).
5. If the shell dies, the engine's `ParentWatchdog` exits the JVM (code 71);
   if the engine dies, the shell shows an error dialog with the log path
   (`%APPDATA%/com.geckocircuits.desktop/logs/engine/engine.log` on Windows).

## Opening circuits from the OS

The installers register a file association for `.ipes`. Double-clicking a
circuit (or "Open with", or a second launch while the app runs) forwards it
to the running window: the shell validates + base64-encodes the file and
hands it to the editor, which loads it through the normal upload flow.
Saves route through a native save dialog (`save_file_dialog` command).

## LLM / MCP integration

The engine ships with a bundled MCP server (stdio) so Claude Desktop, Cursor,
ZCode & co. can drive simulations without any Python or Java installation:

```sh
# generate launchers + client config next to an installation
python scripts/desktop/write-mcp-launchers.py --dest <install-dir>
```

Point your LLM client at `gecko-mcp.bat` / `gecko-mcp.sh` (config template:
`mcp-client-config.json`). Tools: `gecko_server_status`, `gecko_catalog`,
`gecko_setup_pfc_project`, `gecko_setup_llc_project`, `gecko_inspect_circuit`,
`gecko_patch_component`, `gecko_set_script_code`, `gecko_simulate`,
`gecko_get_waveforms`, `gecko_tune_pfc` — identical names and result shapes to
the Python server in `tools/mcp/`, guarded by golden equivalence tests
(`ProjectGoldenTest`, `ToolsTest`, `StdioEndToEndTest`).
