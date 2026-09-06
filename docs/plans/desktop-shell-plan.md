# GeckoCIRCUITS Desktop: Tauri 2 Shell + Java Sidecar — Implementation & Update Plan

Status: approved 2026-09-05.
Decisions: bundled **Java MCP server**, **GraalVM JS included**, new app takes the name **GeckoCIRCUITS** (Swing ships on as "GeckoCIRCUITS Classic", existing pipeline untouched).

## Architecture

Tauri 2 shell (Rust, budget ≤ ~700 LOC) bundles the React UI as static assets and spawns the existing Spring Boot jar (`gecko-rest-api-1.0.0.jar`, which already embeds the UI and serves `/gecko/api/...`, SSE, STOMP-WS) on a bundled jlink Java-25 runtime, bound to `127.0.0.1` with `--server.port=0`. The shell reads the actual port from a `GECKO_READY` line the Java side prints on startup, then injects `window.__GECKO_BACKEND__` into the webview so the frontend's relative URLs (`/gecko/api/v1`, `location.host` WS) become absolute. MCP is a second jar (`gecko-mcp`) run by the same bundled runtime as a stdio command for LLM clients. No engine, REST, or frontend-feature code is rewritten.

Branch: `feature/tauri-desktop` off `feature/web-frontend` (after its merge to main).

## Phase 0 — Decouple frontend from origin (no new deps)

**WP0.1 Base-URL injection** — `frontend/src/api/client.ts`: `const API = ((globalThis as any).__GECKO_BACKEND__ ?? '') + '/gecko/api/v1'`; WS/SSE URL construction goes through one `backendOrigin()` helper (http→ws). Tests: extend `frontend/test/client.test.ts` (relative default, injected absolute, ws derivation).
**WP0.2 Wait-for-backend bootstrap** — new `frontend/src/bootstrap.ts`: poll `GET /gecko/api/health` (already exists, returns `{status,version}`), backoff 0.5→5 s, 60 s timeout → render `EngineStartupError` screen with "open logs" hint. Wire into `main.tsx`. Vitest with mocked fetch/timers.
**WP0.3 CORS** — new `gecko/rest/config/CorsConfig.java` (`WebMvcConfigurer`): allow origins `tauri://localhost`, `https://tauri.localhost`, `http://localhost:5173`, list overridable via `gecko.api.allowed-origins`. MockMvc tests: preflight OPTIONS + GET with Origin.
**WP0.4 Script-block spike — DONE (2026-09-05):** no GraalVM needed. The headless engine executes typ-61 script blocks via the core's own interpreter (`ScriptBlockCalculator`); proven by `ScriptBlockSimulationTest` (differential assertion: blanking the script changes the waveforms). rest-api pom stays without polyglot deps; the sidecar JVM needs no `polyglot` flags for script circuits.

## Phase 1 — Tauri shell MVP (`desktop/` crate) — implemented 2026-09-05

Status: `desktop/` workspace committed. Two crates: `engine` (pure logic: readiness handshake, engine process handling, filename sanitizing — 16 unit tests, std-only, builds on any toolchain) and `app` (Tauri 2 shell wiring: sidecar spawn, GECKO_READY wait, window with backend injection, download save dialog, commands). `clippy` and `rustfmt` clean; `cargo check` passes on windows-gnu locally (build.rs skips the broken windres resource step on that toolchain only; MSVC/CI uses full tauri_build). Icons generated from `_build/resources/GeckoCIRCUITS.png`. Java side: `EngineReadyLogger` + `ParentWatchdog` (exit 71 on parent death, `gecko.parent-pid=0` disables). Release bundling/installers = Phase 2 (`scripts/desktop/build-engine.py` + CI).

Scaffold: `desktop/Cargo.toml` (tauri 2, `tauri-plugin-single-instance`, `tauri-plugin-dialog`, `tauri-plugin-opener`), `desktop/tauri.conf.json` (productName `GeckoCIRCUITS`, id `com.geckocircuits.desktop`, icons from `cargo tauri icon` reusing the existing app icon; CSP `connect-src 'self' http://127.0.0.1:* ws://127.0.0.1:*`).

Rust modules, one concern each, all unit-tested:
- `sidecar.rs` — resolve `resource_dir()/engine/runtime/bin/java` + `engine/gecko-rest-api.jar`; spawn with args `[-Xmx2g, -jar, ..., --server.port=0, --server.address=127.0.0.1, --gecko.parent-pid=<pid>]`; `CREATE_NO_WINDOW` on Windows; stdout/stderr → rotating log file in app-data `logs/engine-*.log`; kill-on-drop + explicit cleanup on window-close/exit-requested.
- `ready.rs` — parse stdout lines until `GECKO_READY http://127.0.0.1:<port>/gecko` (90 s timeout, clear error dialog incl. log path on failure). Deterministic, no port race.
- `window.rs` — after ready: build main window (1400×900, min 1024×700) with `initialization_script` setting `window.__GECKO_BACKEND__`; create window only after ready.
- `commands.rs` — `get_backend_url()` (fallback if init-script timing ever bites), `open_logs()`, `mcp_config_path()` (Help > MCP dialog text).
- `download.rs` — `on_download` handler: save dialog (dialog plugin) → set destination → accept, so the existing `.ipes` blob-download save flow works natively; unit-test filename sanitizer.

Java additions (small, tested): `EngineReadyLogger` (`ApplicationListener<WebServerInitializedEvent>` printing the GECKO_READY line; OutputCaptureExtension test) and `ParentWatchdog` (`@Scheduled` every 5 s: `ProcessHandle.of(parentPid)` empty → `System.exit(71)`; exit strategy injectable for the unit test) so a crashed shell never leaves an orphan JVM.

Dev mode: `beforeDevCommand: npm run dev`, devUrl `:5173`; in `#[cfg(debug_assertions)]` the shell does **not** spawn the sidecar — the existing Vite proxy + `run-web-editor` backend on 8080 keep working unchanged.

Cargo tests: GECKO_READY parser (incl. garbage/noise lines), arg/path builders, sanitizer, kill test (spawn `ping`/`sleep`, assert exited).

## Phase 2 — Bundling, packaging, CI — implemented 2026-09-05

Status: `scripts/desktop/build-engine.py` builds frontend + jar, derives jlink modules (jdeps ∪ pinned list: 13 modules incl. java.rmi for the legacy backend), creates the runtime image and runs a smoke test that simulates rc-lowpass through the Boot jar via `PropertiesLauncher -Dloader.main` (plain -cp cannot see Boot's nested jars). Bundle ≈ 52 MB runtime + 35 MB jar. `scripts/desktop/build-all.bat|.sh` chain engine + `cargo tauri build`. `.github/workflows/desktop.yml`: PR gates (Rust fmt/clippy/tests/check on MSVC windows runner; Java + frontend lint/tests on Linux) and a 3-OS installer matrix on tags/dispatch with `set-version.py` version sync, SHA256SUMS and GitHub release. `build:spring` now cleans stale hashed assets before copying. Frontend gained ESLint 9 + typescript-eslint 8 (dev-only); `terminalPositions` narrowed-type gained the optional fields it was accessing via `as any`.

- `scripts/desktop/build-engine.py` (style of existing `package-desktop.py`): `mvn -pl src/modules/gecko-rest-api -am package -DskipTests` → `npm --prefix frontend run build:spring` → `jlink` (module list pinned in-script after one `jdeps` pass; `--strip-debug --no-man-pages --no-header-files --compress=zip-6`) → copy jar to `desktop/engine/gecko-rest-api.jar` + `VERSION`. Built-in smoke test: `runtime/bin/java -cp gecko-rest-api.jar gecko.core.GeckoHeadless --circuit tools/parity/circuits/rc-lowpass.ipes --output <tmp>` must exit 0 — catches jlink module gaps immediately.
- `tauri.conf.json` bundle: `resources: ["engine/*"]`, targets `nsis,msi,dmg,app,deb,rpm,appimage`. Local one-shot: `scripts/desktop/build-all.bat|.sh` (engine → `cargo tauri build`).
- CI `.github/workflows/desktop.yml`: on PR → `cargo fmt --check`, `cargo clippy -- -D warnings`, `cargo test`, frontend eslint+vitest, `mvn -pl gecko-rest-api,gecko-mcp test` + checkstyle/pmd on new code; on tag `v*`/manual(version) → 3-OS matrix (JDK 25, Node, rust, python) → build-engine → tauri build → upload `GeckoCIRCUITS-<v>-{win-setup.exe,.msi,.dmg,.AppImage,.deb,.rpm}` → release job with SHA256SUMS (mirrors existing `package-desktop.yml` pattern).
- Acceptance: installers on all 3 OS run with **no Java preinstalled**; quitting the app leaves no `java` process; uninstall is clean.

## Phase 3 — Bundled Java MCP module — implemented 2026-09-05

Status: `src/modules/gecko-mcp` (Java 25, no Spring) on MCP Java SDK 2.0.1 (`mcp-core` + `mcp-json-jackson3`), shaded jar `gecko-mcp-1.0.0-jar-with-dependencies.jar`. All 10 Python tools ported 1:1 (identical names/params/result shapes); simulations run **in-process** through `HeadlessSimulationEngine` instead of the Python subprocess (CSV export factored into core `SimulationCsv`, now shared with `GeckoHeadless`). The two .ipes generators were extracted mechanically from the Python AST into template resources with numbered holes (`scripts/desktop/extract-templates.py`) — **byte-exact golden equivalence tests** over 10 parameter sets guard the port. Deviation from bug-for-bug: the patcher's block regex is tempered so it cannot match across element blocks (the Python regex could patch the wrong component); CRLF handling added in CSV parsing (Windows). Tests: 11 in gecko-mcp (goldens, patch→re-read, metrics vs Python-generated golden JSON, tool registry, **real stdio E2E** spawning the server JVM via the SDK client). `build-engine.py` bundles `gecko-mcp.jar`; `write-mcp-launchers.py` emits `gecko-mcp.bat`/`.sh` + client config.

New Maven module `src/modules/gecko-mcp`: plain Java 25, no Spring. Deps: official `io.modelcontextprotocol.sdk:mcp` (stdio), jackson, `gecko-simulation-core`. Fat jar via shade; main `gecko.mcp.GeckoMcpServer`.

Port the 10 Python tools 1:1 with identical names (`gecko_server_status`, `gecko_catalog`, `gecko_setup_pfc_project`, `gecko_setup_llc_project`, `gecko_inspect_circuit`, `gecko_patch_component`, `gecko_set_script_code`, `gecko_simulate`, `gecko_get_waveforms`, `gecko_tune_pfc`). Structure: `ipes/` package (gzip-aware text reader/writer, component patcher, script patcher — pure functions) + one class per tool; engine access via `HeadlessSimulationEngine`, factoring GeckoHeadless' CSV export into a shared util rather than duplicating it. Workspace/`GECKO_HOME` conventions preserved.

Tests: per-tool JUnit with fixture `.ipes` under `src/test/resources` (rc-lowpass from `tools/parity/circuits`, PFC/LLC from `resources/projects`); patch→re-parse assertions; hand-computed golden values for waveform metrics (rms/ripple/dc); stdio end-to-end via the SDK's sync client over in-process pipes (list tools → call `gecko_simulate` → assert payload). No network anywhere.

Distribution: `gecko-mcp.jar` ships in `engine/`; `scripts/desktop/write-mcp-launchers.py` emits `gecko-mcp.bat`/`gecko-mcp` into the install dir; app Help menu shows copy-paste client JSON (Claude Desktop/Cursor/ZCode) with the absolute detected path. Python server in `tools/mcp/gecko_mcp` stays as-is for repo development.

## Phase 4 — Desktop niceties — implemented 2026-09-06

Status: `.ipes` file association via `bundle.fileAssociations` (NSIS/MSI/DMG/DEB/RPM generate the OS hooks); double-click/second-launch/macOS `RunEvent::Opened` paths all forward through `window::open_file_paths` → `read_ipes_file` command → `window.__geckoOpenFile` queue shim (shell injects it before page scripts; payloads arriving before the editor mounts are buffered). Frontend bridge `desktop.ts` (zero npm deps, uses `withGlobalTauri`) registers the handler → `useEditor.openBase64` → existing upload flow. Save flow: `downloadIpes` now routes to the native save dialog via the `save_file_dialog` command on desktop, browser blob download otherwise. Pure logic (path validation, base64, arg filtering) lives in `engine/src/circuit_files.rs` — 20 engine tests total; the app crate stays thin Tauri wrappers (its test binary cannot run on the local windows-gnu toolchain: STATUS_ENTRYPOINT_NOT_FOUND from WebView2 loader DLLs; CI/MSVC covers it). Draft autosave (optional) deferred. Pending manual QA: open/save flows on real installers (WebView2/WKWebView/webkit2gtk).


- Single-instance (focus existing window) + `.ipes` file association (tauri `fileAssociations`/macOS document types): second launch forwards the path; shell emits `open-file`; frontend listens → existing `uploadIpes` flow. Vitest for the frontend handler.
- Save-flow QA across WebView2/WKWebView/webkit2gtk; fallback Tauri command `save_ipes(circuitId)` streaming `GET /circuits/{id}/ipes` to a chosen path if `on_download` proves unreliable on any engine.
- Optional if time permits: draft autosave to app-data + restore prompt.

## Phase 5 — Docs, versioning, release — implemented 2026-09-06

Status: `docs/desktop-app.md` (architecture, build, install matrix, versioning, CI, **11-point per-OS release QA checklist**, troubleshooting incl. engine-log locations and unsigned-binary notes) and `docs/mcp.md` (bundled-Java vs Python-dev server, client config, tool table, security) added and wired into the mkdocs nav as a "Desktop" section. README Quick Start now leads with the desktop app (Classic Swing installers demoted to a secondary section). PACKAGING.md and the desktop-packaging skill gained the Tauri flow (build-engine / build-all / set-version / write-mcp-launchers). Release template: `.github/release.yml` categories for conventional-commit labels. Version sync (`set-version.py`) was already wired into desktop.yml in Phase 2. Remaining manual step (inherently human): run the QA checklist against the first CI-built installers.


`docs/desktop-app.md` (architecture, build, 10-point per-OS QA checklist, troubleshooting via engine logs), `docs/mcp.md`; README quick start leads with desktop installers; PACKAGING.md gains the Tauri section; `.agents/skills/desktop-packaging/SKILL.md` updated. `scripts/desktop/set-version.py <v>` syncs `tauri.conf.json` + `app.version` + frontend package.json; CI feeds it the tag. CHANGELOG + release template.

## Post-completion deep review (2026-09-06) — findings fixed

Dead code removed (`get_backend_url` command + `backend_url` state — the init-script injection made them unreachable; unused `Files` import in CircuitInspector); `client.ts` now reuses `desktop.ts::saveFileNative` instead of duplicating the invoke. Usability: release builds show a splash window during engine cold start; the startup-error screen's "open engine logs" is now a real button (`open_logs_folder` command); circuits arriving while the main window is still starting are parked in `pending_opens` and delivered once it exists (previously dropped when a second instance won the startup race). Ported fixed analysis windows/thresholds in `WaveformAnalysis` are now documented inline. Test coverage additions: `gecko_tune_pfc` handler test, `openLogsFolder` bridge tests (gecko-mcp 12, frontend 123, engine 20 — all green). Known acceptable gaps: Tauri-dialog wiring (`download.rs`, `save_file_dialog` dialog part) and window orchestration are compile-checked + CI-gated only — they need a display; `WaveformAnalysis` fixed windows stay magic-but-documented because they are bug-for-bug ports of the Python tool's reference-project constants.

## Test & static-analysis gates (summary)

| Target | Tests | Static analysis |
|---|---|---|
| Rust shell | cargo test (parsers, spawner/kill, sanitizer) | `rustfmt --check`, `clippy -D warnings` — CI hard gates |
| Frontend | vitest: base-URL, bootstrap retry/error, open-file, existing suites | new ESLint 9 flat config (typescript-eslint, `no-unused-vars` error), `npm run lint` gate; **zero new npm deps** |
| Java (new code) | MockMvc CORS, EngineReadyLogger, ParentWatchdog, ScriptBlockSimulationIT, full gecko-mcp suite; parity harness (`mvn -Pparity verify`) still gates engine changes | checkstyle+pmd **enforced (failOnViolation=true) only in gecko-mcp + new rest-api files**; legacy modules untouched — no mass reformat |
| Packaging | build-engine smoke (headless rc-lowpass), installer manual QA checklist | — |

## Anti-slop rules

Rust ≤ ~700 LOC, 3 plugins max, no trait forests/settings managers. Frontend: no new dependencies. gecko-mcp: no DI framework, tool classes < 300 LOC, faithful port (no "improved" .ipes semantics). Comments only for non-obvious constraints (e.g. why port=0 + stdout handshake). Every new file traces to a WP above; nothing speculative.

## Key risks & mitigations

Spring cold start (5–15 s) → loading-then-ready window UX, measure early. jlink module gaps → smoke test gates every build. GraalVM JS in Boot fat jar → WP0.4 spike before anything depends on it. Unsigned binaries → SmartScreen/Gatekeeper notes in docs; signing later. WebView2 absent on old Windows → documented bootstrap. Cross-origin SSE/WS quirks → CORS+`allowedOriginPatterns` already permissive on WS; frontend already falls back from SSE to REST polling.

## Sequencing & effort

P0 ≈ 2 d → P1 ≈ 4 d → P2 ≈ 4 d → P3 ≈ 5 d → P4 ≈ 3 d → P5 ≈ 1 d (~3.5 weeks focused work). Each phase merges independently and leaves the repo shippable (Swing + REST + Python MCP all keep working throughout).

## Pre-flight (step zero, done before Phase 0)

Commit untracked MCP work (`tools/mcp/`, `resources/projects/`, `.agents/mcp_config.json`) + launcher changes; merge `feature/web-frontend` to main; verify and gate rest-api/core tests in CI (remove `-Dmaven.test.failure.ignore=true` for those modules); fix `SecurityConfig` health whitelist (`/api/health`, not `/api/v1/health`); drop dead `gecko-api-dev` docker service.
