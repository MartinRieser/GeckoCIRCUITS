# Architecture Document

**Project:** GeckoCIRCUITS
**Runtime Stack:** Java 25, Spring Boot 4.1.1, React + TypeScript, Tauri 2 (Rust), MCP SDK, Python (`geckopy`)

---

## 1. System Overview

GeckoCIRCUITS is an open-source multi-domain power electronics simulator (electrical, thermal, magnetic, mechanical, and control domains). All simulation logic lives in a GUI-free Java 25 core library shared identically across the web/desktop editor, the HTTP REST/WebSocket API, the Model Context Protocol (MCP) server, Python automation (`geckopy`), and the headless CLI.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          GeckoCIRCUITS Platform                         │
│                                                                         │
│  ┌──────────────────────┐  ┌────────────────────┐  ┌─────────────────┐  │
│  │  React + TypeScript  │  │  REST / WebSocket  │  │  MCP Server &   │  │
│  │  Editor (`frontend/`)│  │  (`gecko-rest-api`)│  │  `geckopy` SDK  │  │
│  │  & Tauri (`desktop/`)│  │  Spring Boot 4.1.1 │  │  (`gecko-mcp`)  │  │
│  └──────────┬───────────┘  └─────────┬──────────┘  └────────┬────────┘  │
│             └────────────────────────┼──────────────────────┘           │
│                                      ▼                                  │
│                     ┌──────────────────────────────────┐                │
│                     │     `gecko-simulation-core`      │                │
│                     │  GUI-free Java 25 MNA Engine     │                │
│                     └──────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Repository & Module Structure

```
GeckoCIRCUITS/
├── pom.xml                          # Maven multi-module reactor
├── backend/
│   ├── gecko-simulation-core/       # 196 Java classes: MNA engine, .ipes I/O, DRC, NativeC, calculators
│   ├── gecko-rest-api/              # 59 Java classes: Spring Boot 4.1.1 REST, SSE, STOMP WebSocket, embedded UI
│   └── gecko-mcp/                   # 18 Java classes: stdio MCP JSON-RPC 2.0 server (16 tools) + SVG exporter
├── frontend/                        # 50 TS/TSX files: React 19 schematic editor, scope plots, FFT, loss view
├── desktop/                         # 10 Rust files: Tauri 2 desktop shell (`gecko-desktop`) + `gecko-engine` crate
├── geckopy/                         # Python SDK wrapping gecko-mcp (`Circuit`, `SimulationResult`, `Metrics`)
├── examples/                        # Bundled quick-open example circuits + manifest.json
├── resources/                       # Tutorials, application examples, reference projects, newsletters
├── tools/parity/                    # Parity verification scripts and reference fixtures
├── scripts/                         # Dev launchers (`run-gecko.*`), desktop bundlers, verification scripts
├── docker/                          # Multi-stage Alpine JRE 25 Dockerfile & compose config
└── docs/                            # MkDocs Material documentation source
```

---

## 3. Core Simulation Engine (`gecko-simulation-core`)

### 3.1 Simulation Flow

```
.ipes file ──► CircuitFileParser ──► CircuitModel ──► NetlistBuilder ──► MNA MatrixSolver ──► DataContainerGlobal
                                                             │
                                                       StamperRegistry
                                                 (R, L, C, Diode, IGBT, MOSFET, ...)
```

1. **Parser (`gecko.core.io.CircuitFileParser`)**: Reads plain UTF-8 `.ipes` circuit files (plus gzip-compressed `.ipes` archives).
2. **Design Rule Check (`CircuitValidator`)**: Verifies ground references, short circuits, floating terminals, and control-block syntax/libraries.
3. **Netlist & MNA Stampers (`gecko.core.circuit.netlist`, `gecko.core.circuit.matrix`)**: `IMatrixStamper` and `IStatefulStamper` implementations stamp conductances/sources into `MatrixAccumulator` and track switching transitions.
4. **Solvers (`gecko.core.simulation.solver`)**: Backward Euler (`SOLVER_BE`), Trapezoidal (`SOLVER_TRZ`), and Gear-Shichman (`SOLVER_GS`), plus `ZeroCrossingDetector` and multi-domain coupling (`DomainCoupler`).
5. **Control & Firmware Blocks**:
   - **74 calculator classes** under `gecko.core.control.calculators` implementing `AbstractControlCalculatable`.
   - **Script Block (`typ 61`)**: Pure-Java per-step script evaluator (`ScriptBlockCalculator`).
   - **NativeC Block (`typ 88`)**: Loads user-compiled C/C++ shared libraries via JDK 25 FFM (`CLibraryCalculator`, `gecko_c_block.h`).

### 3.2 GUI-Free Enforcer Boundary

`backend/gecko-simulation-core/pom.xml` configures `maven-enforcer-plugin` to ban GUI framework dependencies (`org.openjfx`, SWT) and enforce >=30% JaCoCo instruction coverage during `mvn verify`.

---

## 4. Application & Integration Surfaces

### 4.1 REST & WebSocket API (`gecko-rest-api`)

Served under servlet context path `/gecko` on port `8080` by default (see [REST API Reference](api/rest-api.md)):

- **Health & OpenAPI**: `/gecko/api/health`, `/gecko/api/info`, `/gecko/api/docs`, `/gecko/swagger-ui.html`, `/gecko/api-docs`
- **Circuit File & Editing (`/gecko/api/v1/circuits`)**: parse, info, components, validate, raw, ipes export, update, clone, parameter updates, component/wire CRUD, node labels, undo/redo, model snapshot, graph registers, catalog
- **Built-in Examples (`/gecko/api/v1/examples`)**: list, inspect, save, update, delete
- **Simulation & Batch Control (`/gecko/api/v1/simulations`)**: async run, status, results, per-signal results, progress, SSE stream, ws-info, CSV export, pause/resume/cancel, debug breakpoints/step/state, batch sweeps
- **Signal Analysis & Semiconductor Losses (`/gecko/api/v1/analysis`, `/gecko/api/v1/loss`)**: characteristics, Fourier/FFT, RMS, switching/conduction/detailed thermal losses
- **WebSockets (STOMP)**: `/gecko/ws` (SockJS) and `/gecko/ws-raw` broadcasting progress on `/topic/simulations/{id}` and circuit changes on `/topic/circuits/{circuitId}`
- **Security**: Optional `X-API-Key` header via `ApiKeyAuthFilter` (`gecko.api.auth-enabled=false` by default)

### 4.2 Web Frontend & Tauri Desktop (`frontend/`, `desktop/`)

- **Browser Mode**: Spring Boot serves the compiled React UI from static classpath resources; in local dev, Vite proxies API/WS calls to port `8080`.
- **Desktop Mode (`desktop/`)**: Tauri 2 Rust binary spawns `gecko-rest-api.jar` on `127.0.0.1` (`--server.port=0`) using a bundled JDK 25 `jlink` runtime, waits for the `GECKO_READY` stdout handshake, injects `window.__GECKO_BACKEND__`, handles `.ipes` file associations and native save dialogs, and ties engine lifetime via `ParentWatchdog`. See [Desktop Application](desktop-app.md).

### 4.3 MCP Server & Python SDK (`backend/gecko-mcp`, `geckopy/`)

`gecko-mcp.jar` runs headless in-process simulations over MCP JSON-RPC 2.0 stdio with 16 tools covering circuit creation (`pfc`, `llc`, generic netlists), DRC validation, component/script patching, simulation metrics/waveforms, PFC tuning, SVG schematic export, and engine reset. `geckopy/` provides a Python API (`Circuit`, `SimulationResult`, `Metrics`, `Waveforms`) over `gecko-mcp.jar`. See [MCP Interface](mcp.md).

---

## 5. Build, Verification & CI Pipelines

| Pipeline / Check | Command |
|------------------|---------|
| **Backend build & tests (JaCoCo >=30%)** | `mvn clean verify` |
| **Style & static checks (`gecko-mcp`, `gecko-rest-api`)** | `mvn checkstyle:check pmd:check -D"checkstyle.config.location=../../checkstyle.xml" -D"pmd.rulesets=../../pmd-ruleset.xml" -pl backend/gecko-mcp,backend/gecko-rest-api` |
| **Frontend lint, type-check & unit tests** | `npm --prefix frontend run lint && npx --prefix frontend tsc -p frontend/tsconfig.json --noEmit && npm --prefix frontend test` |
| **Desktop Rust checks** | `cargo fmt --manifest-path desktop/Cargo.toml --all --check && cargo clippy --manifest-path desktop/Cargo.toml --workspace -- -D warnings && cargo test --manifest-path desktop/Cargo.toml -p gecko-engine` |
| **Python tooling & `geckopy` tests** | `python -m unittest discover -s tests` |
| **Documentation build** | `mkdocs build --strict` |

GitHub Actions workflows automate verification and publishing in `.github/workflows/ci.yml`, `.github/workflows/desktop.yml`, and `.github/workflows/docs.yml`.

---

## 6. Historical Note

The legacy Swing desktop UI (`gecko.geckocircuits`), RMI remote control (`GeckoRemoteInterface`), memory-mapped interface, and GraalVM polyglot scripting were retired in v1.0.0 and remain preserved under git tag `classic-v1.0-final`.
