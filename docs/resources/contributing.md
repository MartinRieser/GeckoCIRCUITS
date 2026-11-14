---
title: Contributing
---

# Contributing to GeckoCIRCUITS

Thank you for your interest in contributing to GeckoCIRCUITS!

## Ways to Contribute

| Contribution | Description | Difficulty |
|--------------|-------------|------------|
| Bug reports | Report simulation, UI, or desktop issues | Easy |
| Circuit examples | Add verified `.ipes` circuits and tutorial notes | Easy–Medium |
| Documentation | Improve guides, API docs, and tutorials | Easy–Medium |
| Bug fixes | Fix backend, frontend, desktop, or tooling bugs | Medium |
| New features | Add editor tools, MCP tools, or REST endpoints | Medium–Hard |
| Core engine | Improve MNA stampers, solvers, or calculators | Hard |

## Development Setup

### Prerequisites

- **JDK 25** (Eclipse Temurin recommended)
- **Maven 3.8+**
- **Node.js 22+** and **npm**
- **Python 3** (for `geckopy` tests and example/docs scripts)
- **Rust stable** (only needed if modifying `desktop/`)

### Build & Test Everything

```bash
# Backend engine, REST API, and MCP server (enforces >=30% JaCoCo coverage on core)
mvn clean verify

# View core JaCoCo coverage report:
# backend/gecko-simulation-core/target/site/jacoco/index.html

# Style & static checks on gecko-mcp and gecko-rest-api
mvn checkstyle:check pmd:check \
  -D"checkstyle.config.location=../../checkstyle.xml" \
  -D"pmd.rulesets=../../pmd-ruleset.xml" \
  -pl backend/gecko-mcp,backend/gecko-rest-api

# Frontend lint, TypeScript check, and Vitest suite
npm --prefix frontend run lint
npx --prefix frontend tsc -p frontend/tsconfig.json --noEmit
npm --prefix frontend test

# Python SDK and tooling unit tests
python -m unittest discover -s tests

# Documentation strict link & build check
mkdocs build --strict
```

### Run the Application Locally

```bash
# Starts the REST API engine on localhost:8080/gecko and launches the editor
run-gecko.bat        # Windows
./run-gecko.sh       # Linux / macOS
```

## Code Architecture

```
GeckoCIRCUITS/
├── backend/
│   ├── gecko-simulation-core/   # GUI-free Java 25 MNA solver, stampers, calculators, .ipes I/O, DRC
│   ├── gecko-rest-api/          # Spring Boot 4.1.1 REST/SSE/WebSocket API + embedded frontend assets
│   └── gecko-mcp/               # Stdio MCP JSON-RPC 2.0 server (16 tools) + vector SVG exporter
├── frontend/                    # React + TypeScript schematic editor, scopes, FFT, loss panels
├── desktop/                     # Tauri 2 Rust desktop shell (`gecko-engine` & `gecko-desktop`)
├── geckopy/                     # Python SDK wrapping `gecko-mcp`
├── examples/                    # Curated quick-open circuits + manifest.json
├── resources/                   # Tutorials, application examples, reference projects, newsletters
└── docs/                        # MkDocs documentation site source
```

### Architectural Principles

- **Strict GUI-Free Simulation Core**: `backend/gecko-simulation-core` never imports Swing, AWT GUI frameworks, or UI libraries (guarded by `maven-enforcer-plugin`).
- **MNA Solver & Stampers**: Components register in `StamperRegistry` (`CircuitTypCore` -> `IMatrixStamper` / `IStatefulStamper`).
- **Pure-Java Control Calculators**: 74 calculator classes in `gecko.core.control.calculators` implement `AbstractControlCalculatable.calculateYOUT(double deltaT)`.

## Pull Request Process

1. **Fork** the repository and create a branch from `main` (`fix/...`, `feature/...`, `docs/...`, `refactor/...`, `test/...`).
2. Keep changes focused and add unit tests for new behavior.
3. Run the relevant backend, frontend, desktop, Python, and `mkdocs build --strict` checks.
4. Open a Pull Request against `main` with a clear summary and verification results.

## Adding Circuit Examples

1. Author and validate your `.ipes` file in GeckoCIRCUITS (or via `geckopy` / `gecko_validate_circuit`).
2. Verify it simulates cleanly with expected physics behavior.
3. Add or update documentation in `docs/` and run `mkdocs build --strict` (plus `python scripts/verify-examples.py` if editing `examples/`).

## License

By contributing, you agree that your contributions will be licensed under the same [dual license](https://github.com/MartinRieser/GeckoCIRCUITS/blob/main/LICENSE) as the project.
