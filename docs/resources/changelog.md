---
title: Changelog
---

# Changelog

All notable changes to GeckoCIRCUITS.

## v44-2026 (2026-10)

### Added
- **Native Desktop File Workflow**: Implemented full file lifecycle management (New, Open, Save, Save As, and real-time dirty state tracking) across both the Tauri desktop shell and browser application modes.
- **Integrated Examples Library**: Integrated the circuit examples browser directly into the `File -> Examples` menu, supporting local/disk synchronization, custom example authoring, and collision-free overwriting.
- **2D Planar Topology Placement & Trunk-and-Stub Manhattan Router**: Intelligent collision-free component placement and orthogonal Steiner comb routing in `gecko_create_circuit` for electrical, control, and thermal domains.
- **Headless Vector SVG Schematic Generator (`gecko_export_schematic_svg`)**: Publication-quality vector export of `.ipes` schematics with IEC 60617 symbols, orthogonal wire polylines, 3-way junction dots, and light/dark themes.
- **SPWM Inverter with Dead-Time Example**: Bundled turnkey full-bridge SPWM inverter circuit configuration with active semiconductor freewheeling and dead-time control.

### Changed
- **IEC 60617 / IEEE 315 Semiconductor Symbols**: Modernized IGBT, MOSFET, and BJT symbols by removing surrounding circular enclosures and scaling channel bar dimensions to match passive and diode component proportions.
- **Unified Global Ground Terminals**: Synchronized `LK_GLOBAL_TERMINAL` (31), `REL_GLOBAL_TERMINAL` (32), and `TH_GLOBAL_TERMINAL` (50) across `ComponentTerminals`, `NetlistBuilder`, `CircuitValidator`, and the web canvas as single-terminal pins at `(x, y - 2)`.

### Fixed
- **DRC Dangling Wire Warnings**: Resolved ground terminal pin offset bug where wires erroneously routed past the terminal stem to the symbol center.
- **Thermal Domain Wire Routing**: Fixed diagonal thermal connection lines by routing thermal RC ladders through orthogonal unit-stepped paths with intermediate obstacle avoidance.
- **Code Review Cleanups**: Eliminated dead code, replaced hardcoded grid coordinates and orientation flags with named constants, and completed full Javadoc/TSDoc coverage.

## v43-2026 (2026-10)

### Added
- **Workspace-Level Graph Register Tabs**: Dynamic oscilloscope view registers managed directly from the workspace tab bar with `+ Add Osci` button, inline renaming, and close tabs.
- **1, 2, or 3 Stacked Subplots**: Configurable oscilloscope subplots with synchronized timebases and per-channel `G1`/`G2`/`G3` assignment.
- **Pre-Simulation Signal Discovery**: Automatic discovery and category filtering of electrical and control signals before running simulations.
- **Interactive Measurement Cursors**: Precision drag cursors with delta readouts, 2D box zoom, and collapsible statistical analysis drawer.
- **MCP Server Comprehensive Test Suite**: End-to-end automated tests for all 16 Model Context Protocol tools.

### Changed
- **Wire-Based DRC Net Mapping**: Linter and DRC validator resolve circuit nodes and ground references through continuous wiring networks.
- **Tutorial Schematics**: Upgraded documentation schematics to planar IEC 60617 vector SVGs.

## v1.1.0 - "Multi-Module" (2026-02)

### Added
- Multi-module Maven parent build (`pom.xml`) aggregating main app, gecko-simulation-core, and gecko-rest-api
- Zero-crossing detection for switch events (`ZeroCrossingDetector`, `ZeroCrossingEvent`) in simulation core
- 49 new tests for zero-crossing detection (bisection convergence, rising/falling edges, power electronics use cases)

### Changed
- Standardized groupIds to `gecko` across all modules
- CI workflow now builds all modules via reactor POM
- Updated CLAUDE.md with multi-module build instructions

### Fixed
- REST API timing-sensitive E2E tests (3 tests): accept race conditions in async simulation status transitions
- Duplicate maven-compiler-plugin warning in gecko-rest-api POM

## v1.0.0 - "Production" (2026-02)

### Changed
- Updated README with documentation links, proper version references, and CONTRIBUTING.md pointer
- Fixed download page URLs to point to correct GitHub repository (MartinRieser/GeckoCIRCUITS)
- Added version history table to download page
- Production-ready documentation, examples, and developer onboarding

## v0.5.0 - "Adopt It" (2026-02)

### Added
- Developer Guide with step-by-step instructions for extending the simulator (control blocks, circuit components, MNA stampers)
- Root CONTRIBUTING.md as entry point for new contributors
- EV Charger application example documentation (7.4kW, PFC + isolated DC-DC)
- LLC Resonant Converter example documentation (1kW, 400V-to-48V)
- Grid-tied Solar Inverter example documentation (5kW, single-phase with MPPT)
- BLDC Motor Control example documentation (500W, six-step commutation)

### Changed
- Updated contributing guide with accurate single-module architecture (removed references to planned modules)
- Added Developer Guide to documentation site navigation

## v0.4.0 - "Test It" (2026-02)

### Added
- JaCoCo coverage enforcement with 60% minimum threshold for core packages
- Coverage summary reporting in CI pipeline (GitHub Step Summary)
- Coverage report artifact upload in CI
- GroupableUndoManager test suite (56 tests covering undo/redo grouping)
- ThyristorStamper edge case tests (31 tests covering firing angle, state transitions, recovery timing)
- MOSFETStamper edge case tests (38 tests covering body diode, gate control, switching cycles)

### Changed
- CI workflow now runs `jacoco:report` and displays per-package coverage table
- Aligned JaCoCo plugin version to 0.8.11 across pom.xml
- Updated CLAUDE.md with accurate module status and coverage numbers

### Fixed
- JaCoCo version mismatch between pluginManagement (0.8.13) and build (0.8.11)
- Inaccurate references to gecko-simulation-core and gecko-rest-api modules (marked as planned)

## v0.3.0 - "Script It" (2026-02)

### Added
- GeckoSCRIPT tutorial with script editor walkthrough, parameter sweeps, and batch automation
- MATLAB/Octave integration tutorial covering RMI setup, parameter control, and Simulink co-simulation
- Python integration tutorial with subprocess control, file-based modification, and optimization examples
- Java Blocks tutorial for custom control blocks and circuit components
- Python parameter sweep example script (`resources/tutorials/7xx_scripting_automation/706_python_integration/parameter_sweep.py`)
- Installation verification script (`scripts/run-examples.py`)
- Circuit validation step in CI pipeline

### Changed
- REST API documentation marked as "Planned Feature" to reflect actual implementation status
- API index page updated to distinguish implemented vs planned interfaces

## v0.2.0 - "Understand It" (2026-02)

### Added
- Comprehensive documentation site with MkDocs Material theme
- Getting Started guide with installation, quickstart, and first simulation tutorials
- Tutorial library covering DC-DC, AC-DC, DC-AC, thermal, and scripting topics
- FAQ, troubleshooting, and contributing guides
- GitHub issue templates for bugs and feature requests

### Changed
- Consolidated CI workflows into a single `ci.yml`
- Excluded integration tests (ModelResultsTest) that require GUI from CI

### Removed
- Redundant workflow files (maven.yml, build-test.yml, dev-ci.yml)
- Stale remote branches (experimental)

## v0.1.0 - "It Works" (2026-02)

### Added
- GitHub Actions CI pipeline with automated build and test
- MkDocs documentation deployment to GitHub Pages
- Platform-specific launcher scripts (Windows, Linux, macOS, WSL)
- Distribution packaging via Maven profiles
- New user-focused README

### Changed
- Reorganized `resources/` directory structure (tutorials, examples, articles)
- Updated `.gitignore` for modern Java/Python/IDE patterns
- Cleaned up article file naming with date prefixes

### Removed
- Archive directory (old DLLs, database files)
- Redundant resource files

## Pre-release History

GeckoCIRCUITS was originally developed at ETH Zurich's Power Electronic Systems Laboratory (PES). The open-source release builds on years of academic development and use in power electronics education and research.

### Key historical milestones
- **2009-2012** - Newsletter series documenting simulation techniques
- **2010** - MATLAB/Simulink integration via RMI
- **2011** - Thermal simulation capabilities
- **2024** - Migration to Java 21, GraalVM polyglot scripting
- **2025** - GUI-free core extraction (gecko-simulation-core)
- **2026** - Open-source release preparation
