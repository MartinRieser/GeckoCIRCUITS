# GeckoCIRCUITS Post-v1.0 Strategic Roadmap

**Version:** 1.0.0  
**Date:** September 2026  
**Status:** Active  

This document defines the strategic sequence of work following the **v1.0.0** release:
1. **Phase 1: Retire the Classic GUI** (Current Focus — Eliminate technical debt & prune legacy tree)
2. **Phase 2: Modern GUI Polish & Usability Improvements** (Canvas UX, component properties, scopes, interaction ergonomics)
3. **Phase 3: Future Ideas & Advanced Features** (SPICE/PLECS import, adaptive solvers, saturable magnetics, AI optimization)

---

## Phase 1: Classic GUI Retirement & Codebase Streamlining (Completed)

With P0 and P1 parity achieved in the modern Tauri/React desktop application, the legacy Java Swing UI (`gecko.geckocircuits`) has been officially retired.

### Objectives Achieved
1. **Zero-Loss Test Migration**: Relocated all 26 benchmark circuits from `gecko-gui` test resources into `gecko-simulation-core`.
2. **Archival Preservation**: Cut permanent annotated git tag `classic-v1.0-final` preserving the Swing GUI for historical reference.
3. **Legacy Tree Pruning**:
   - Deleted `src/modules/gecko-gui` (~1,000 legacy Swing classes and ~135k LOC).
   - Dropped obsolete legacy dependencies (Intel MKL/Pardiso JNA, Apache Batik/FOP PDF reporting).
   - Retired legacy MATLAB RMI (`GeckoRemoteInterface`, `CallbackClientImpl`).
4. **Build Streamlining**:
   - Removed `src/modules/gecko-gui` from top-level `pom.xml`.
   - Build reactor compiles strictly `gecko-simulation-core`, `gecko-rest-api`, and `gecko-mcp`.
   - Verified end-to-end: Full Maven build (`mvn clean test` under JDK 25) passes 2,200+ unit tests across all remaining modules.

---

## Phase 2: Modern GUI Usability & UX Improvements (Active)

The primary focus is perfecting the user experience of the new React schematic editor (`frontend/`) and Tauri desktop shell (`desktop/`):

1. **Oscilloscope & Waveform Analysis Polish**:
   - [x] Scope Cursors A & B: Direct positioning via Left-Click (Cursor A) and Shift+Click or Right-Click (Cursor B).
   - [x] Draggable Cursor tabs: Grabbable `[A]` and `[B]` badges to slide measurement lines continuously across time.
   - [x] Zoom & Pan: Box-drag time zoom, scroll wheel zoom, middle-click pan, and Reset Zoom button (`Esc`).
   - [x] Feature parity across Overlay and Stacked chart modes.
   - [ ] Multi-channel persistence and signal color customization.
   - [ ] One-click waveform export (CSV, PNG, clipboard copy).
2. **Canvas Ergonomics & Usability**:
   - Improve wire routing, orthogonal wire snapping, and junction dot placement.
   - Smooth multi-selection, drag-and-drop alignment, and grid snap toggle.
   - Keyboard navigation (`Ctrl+A`, duplicate `Ctrl+D`, quick rotation `R`, flip `F`).
   - Quick component insertion palette (`Ctrl+K` / `Cmd+K` command palette).
3. **Component Property Editors**:
   - Clean up and polish property dialogs for complex elements (thermal networks, semiconductors, transformers).
   - In-place parameter editing on canvas labels.
4. **Desktop Shell Integration**:
   - Recent files list, unsaved changes dirty indicator, and native window title bar synchronization.

---

## Phase 3: Future Ideas & Advanced Features (Backlog)

*The following features are preserved as high-value candidate initiatives for future development sprints after Phase 2 is completed:*

### 3.1 SPICE & PLECS Interoperability Pipeline
- Ingest LTspice schematics (`.asc`) by modernizing the 2,500 LOC prototype in `tinix84/copilot/add-ltspice-to-geckocircuit-conversion`.
- Ingest PLECS models (`.plecs`) by porting the XML parser in `tinix84/copilot/add-plecs-to-geckocircuit-conversion`.
- Direct drag-and-drop import into the React canvas and REST/MCP import endpoints.

### 3.2 Advanced Simulation Solvers & Modeling
- **Adaptive Timestep Solver**: Variable $dt$ stepping for accelerated mixed-timescale simulations (switching frequency vs. line/thermal frequency).
- **Non-Linear Saturable Magnetics**: Saturable inductor and transformer models with non-linear $B\text{-}H$ core saturation curves.
- **Automated Loop-Gain Extraction**: AC perturbation injection for automated closed-loop Bode plot and phase margin ($\phi_m$) calculation.

### 3.3 Autonomous AI & Optimization
- **Multi-Objective Pareto Optimization (`gecko_optimize_circuit`)**: Automated sweep over efficiency $\eta$, heatsink temperature, and magnetic volume.
- **Compliance Reporting Tool**: Automated EMC/EMI pre-compliance checks (CISPR 16 / EN 55032) generating markdown engineering reports.
