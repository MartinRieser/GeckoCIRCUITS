# GeckoCIRCUITS Roadmap

Strategic roadmap for GeckoCIRCUITS — completed modern platform milestones and active development priorities.

---

## Completed Platform Milestones

1. **GUI-Free Core Extraction (`backend/gecko-simulation-core`)**
   - Java 25 MNA solver (`Backward Euler`, `Trapezoidal`, `Gear-Shichman`), zero-crossing detector, electro-thermal & magnetic domain coupling, `.ipes` parser/writer, DRC validator, pure-Java `SCRIPT_BLOCK` (`typ 61`), and JDK 25 FFM `NativeC` C/C++ library block (`typ 88`).
2. **Spring Boot REST & WebSocket API (`backend/gecko-rest-api`)**
   - Spring Boot 4.1.1 HTTP API under `/gecko/api/v1` with OpenAPI/Swagger UI, SSE and STOMP WebSocket progress streaming, batch sweeps, Fourier/signal analysis, semiconductor loss calculation, live schematic editing, and API key authentication.
3. **Modern React Editor & Tauri 2 Desktop App (`frontend/`, `desktop/`)**
   - Full React/TypeScript schematic editor with multi-graph oscilloscope registers, measurement cursors, 2D zoom/pan, FFT spectrum analysis, thermal/loss views, and native Tauri 2 desktop installers bundling a `jlink` JDK 25 runtime (Windows NSIS/MSI, macOS DMG, Linux DEB/RPM/AppImage).
4. **AI / Python Automation Platform (`backend/gecko-mcp`, `geckopy/`)**
   - Native stdio MCP server exposing 16 tools (including DRC pre-flight validation, programmatic planar circuit synthesis, multi-domain metrics, and headless IEC 60617 SVG schematic export) plus the `geckopy` Python SDK.
5. **Classic Swing UI Retirement**
   - Archived legacy Swing GUI (`classic-v1.0-final` git tag), dropped legacy desktop/RMI dependencies, and streamlined Maven reactor & CI workflows to build exclusively the modern stack.

---

## Active Focus (Modern UI & Workbench Polish)

See [Post-v1.0 Strategic Roadmap](plans/post-v1-roadmap.md) for active tracking items across:

- **Oscilloscope & Waveform UX**: Signal styling/persistence customization and streamlined waveform CSV/PNG export.
- **Canvas Ergonomics**: Orthogonal wire snapping polish, keyboard shortcuts, and command palette navigation.
- **Desktop Integration**: Recent files workflow, unsaved-change state polish, and platform release verification.

---

## Future Directions (Backlog)

- **SPICE / PLECS Interoperability**: Importing LTspice (`.asc`) and PLECS (`.plecs`) schematics into `.ipes` models and the React editor.
- **Advanced Simulation Capabilities**: Adaptive time-step control, non-linear saturable magnetic core models, and small-signal loop-gain / Bode extraction.
- **Multi-Objective Optimization & Compliance**: Automated Pareto sweeps over efficiency, thermal rise, and magnetic sizing plus EMI pre-compliance reporting.

---

## Release Process

- **Semantic & Calendar Releases**: Version tags (`v*`) trigger `.github/workflows/desktop.yml` to synchronize versions via `scripts/desktop/set-version.py`, build installers across Windows, macOS, and Linux, generate `SHA256SUMS.txt`, and publish GitHub Release assets.
- **Pre-Release Verification**:
  - `mvn clean verify` (backend engine, REST API, MCP server, >=30% core JaCoCo gate)
  - `mvn checkstyle:check pmd:check -D"checkstyle.config.location=../../checkstyle.xml" -D"pmd.rulesets=../../pmd-ruleset.xml" -pl backend/gecko-mcp,backend/gecko-rest-api`
  - `npm --prefix frontend run lint && npx --prefix frontend tsc -p frontend/tsconfig.json --noEmit && npm --prefix frontend test`
  - `cargo fmt --manifest-path desktop/Cargo.toml --all --check && cargo clippy --manifest-path desktop/Cargo.toml --workspace -- -D warnings && cargo test --manifest-path desktop/Cargo.toml -p gecko-engine`
  - `python -m unittest discover -s tests`
  - `mkdocs build --strict`

---

## Contributing

See [Contributing Guide](resources/contributing.md) or [GitHub Issues](https://github.com/MartinRieser/GeckoCIRCUITS/issues) to propose features or report bugs.
