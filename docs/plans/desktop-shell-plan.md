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

## Phase 1 — Tauri shell MVP (`desktop/` crate)

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

## Phase 2 — Bundling, packaging, CI

- `scripts/desktop/build-engine.py` (style of existing `package-desktop.py`): `mvn -pl src/modules/gecko-rest-api -am package -DskipTests` → `npm --prefix frontend run build:spring` → `jlink` (module list pinned in-script after one `jdeps` pass; `--strip-debug --no-man-pages --no-header-files --compress=zip-6`) → copy jar to `desktop/engine/gecko-rest-api.jar` + `VERSION`. Built-in smoke test: `runtime/bin/java -cp gecko-rest-api.jar gecko.core.GeckoHeadless --circuit tools/parity/circuits/rc-lowpass.ipes --output <tmp>` must exit 0 — catches jlink module gaps immediately.
- `tauri.conf.json` bundle: `resources: ["engine/*"]`, targets `nsis,msi,dmg,app,deb,rpm,appimage`. Local one-shot: `scripts/desktop/build-all.bat|.sh` (engine → `cargo tauri build`).
- CI `.github/workflows/desktop.yml`: on PR → `cargo fmt --check`, `cargo clippy -- -D warnings`, `cargo test`, frontend eslint+vitest, `mvn -pl gecko-rest-api,gecko-mcp test` + checkstyle/pmd on new code; on tag `v*`/manual(version) → 3-OS matrix (JDK 25, Node, rust, python) → build-engine → tauri build → upload `GeckoCIRCUITS-<v>-{win-setup.exe,.msi,.dmg,.AppImage,.deb,.rpm}` → release job with SHA256SUMS (mirrors existing `package-desktop.yml` pattern).
- Acceptance: installers on all 3 OS run with **no Java preinstalled**; quitting the app leaves no `java` process; uninstall is clean.

## Phase 3 — Bundled Java MCP module

New Maven module `src/modules/gecko-mcp`: plain Java 25, no Spring. Deps: official `io.modelcontextprotocol.sdk:mcp` (stdio), jackson, `gecko-simulation-core`. Fat jar via shade; main `gecko.mcp.GeckoMcpServer`.

Port the 10 Python tools 1:1 with identical names (`gecko_server_status`, `gecko_catalog`, `gecko_setup_pfc_project`, `gecko_setup_llc_project`, `gecko_inspect_circuit`, `gecko_patch_component`, `gecko_set_script_code`, `gecko_simulate`, `gecko_get_waveforms`, `gecko_tune_pfc`). Structure: `ipes/` package (gzip-aware text reader/writer, component patcher, script patcher — pure functions) + one class per tool; engine access via `HeadlessSimulationEngine`, factoring GeckoHeadless' CSV export into a shared util rather than duplicating it. Workspace/`GECKO_HOME` conventions preserved.

Tests: per-tool JUnit with fixture `.ipes` under `src/test/resources` (rc-lowpass from `tools/parity/circuits`, PFC/LLC from `resources/projects`); patch→re-parse assertions; hand-computed golden values for waveform metrics (rms/ripple/dc); stdio end-to-end via the SDK's sync client over in-process pipes (list tools → call `gecko_simulate` → assert payload). No network anywhere.

Distribution: `gecko-mcp.jar` ships in `engine/`; `scripts/desktop/write-mcp-launchers.py` emits `gecko-mcp.bat`/`gecko-mcp` into the install dir; app Help menu shows copy-paste client JSON (Claude Desktop/Cursor/ZCode) with the absolute detected path. Python server in `tools/mcp/gecko_mcp` stays as-is for repo development.

## Phase 4 — Desktop niceties

- Single-instance (focus existing window) + `.ipes` file association (tauri `fileAssociations`/macOS document types): second launch forwards the path; shell emits `open-file`; frontend listens → existing `uploadIpes` flow. Vitest for the frontend handler.
- Save-flow QA across WebView2/WKWebView/webkit2gtk; fallback Tauri command `save_ipes(circuitId)` streaming `GET /circuits/{id}/ipes` to a chosen path if `on_download` proves unreliable on any engine.
- Optional if time permits: draft autosave to app-data + restore prompt.

## Phase 5 — Docs, versioning, release

`docs/desktop-app.md` (architecture, build, 10-point per-OS QA checklist, troubleshooting via engine logs), `docs/mcp.md`; README quick start leads with desktop installers; PACKAGING.md gains the Tauri section; `.agents/skills/desktop-packaging/SKILL.md` updated. `scripts/desktop/set-version.py <v>` syncs `tauri.conf.json` + `app.version` + frontend package.json; CI feeds it the tag. CHANGELOG + release template.

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
