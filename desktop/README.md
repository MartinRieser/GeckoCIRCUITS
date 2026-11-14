# GeckoCIRCUITS Desktop Shell

Tauri 2 native desktop wrapper around the React schematic editor and bundled Java simulation engine.

## Workspace Crates

- `desktop/engine` (`gecko-engine`): Pure-logic helpers (`circuit_files`, `ready`, `sanitize`, `sidecar`) using `std`, `base64`, and `serde`, unit-tested without a display.
- `desktop/app` (`gecko-desktop`): Tauri 2 application binary managing window lifecycle, splash screen (`splash.html` in release builds), single-instance file handoff, and bundled engine teardown.

## Bundled Engine & Sidecar Runtime

`scripts/desktop/build-engine.py` stages the `engine/` resource directory bundled by `desktop/app/tauri.conf.json` (alongside `../../examples` and `.ipes` file associations):

- `runtime/`: `jlink` Java runtime (`bin/java`, or `bin/java.exe` on Windows).
- `gecko-rest-api.jar`: REST simulation sidecar launched at startup with `--enable-native-access=ALL-UNNAMED`.
- `gecko-mcp.jar`: Bundled MCP server JAR for external MCP clients (not spawned by `sidecar.rs`).
- `gecko_c_block.h` and `VERSION`: C-block header and staged engine version metadata.

At runtime, `desktop/app/src/main.rs` configures `GECKO_LOG_DIR` (writing under `app_log_dir()/engine`, alongside `rest-api.log`, `webview.log`, and `shell_error.log`) and `GECKO_EXAMPLES_DIR`, registers nine Tauri IPC commands (`open_logs_folder`, `open_examples_folder`, `open_file_dialog`, `save_file_dialog`, `save_file_direct`, `set_window_title`, `confirm_dialog`, `read_ipes_file`, `log_webview_message`), and queues single-instance `.ipes` launches into `__geckoOpenFile`. `open_file_dialog` and `circuit_files` accept `.ipes` and `.txt` circuit files.

## Verification

```bash
cargo fmt --manifest-path desktop/Cargo.toml --all --check
cargo clippy --manifest-path desktop/Cargo.toml --workspace -- -D warnings
cargo test --manifest-path desktop/Cargo.toml -p gecko-engine
```
