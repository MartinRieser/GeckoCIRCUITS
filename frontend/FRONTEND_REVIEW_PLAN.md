# GeckoCIRCUITS Frontend Comprehensive Review & Refactoring Plan

This document outlines the systematic, file-by-file review, refactoring, and test coverage expansion plan for every source file in `frontend/src`.

---

## 1. Review Pillars & Quality Criteria

Every source code file must strictly satisfy the following criteria:

| Pillar | Standards & Requirements |
| :--- | :--- |
| **1. Comprehensive Documentation** | Complete TSDoc/JSDoc on all exported functions, hooks, interfaces, props, and algorithms (coordinate transformations, obstacle routing, FFT, waveform interpolation). Zero undocumented public symbols. |
| **2. Zero Magic Numbers** | Extraction of all numerical literals (component type IDs, orientation codes `501`–`504`, raster grid steps, snap distances `0.75`, lead lengths, zoom thresholds, debounce intervals) into centralized, typed constants or `enum`s. |
| **3. General Solutions** | Replace ad-hoc string heuristic detection (e.g., checking if `name.startsWith('V_meas')`, `name.startsWith('S.')`) with schema-driven traits. Eliminate static initialization race conditions and provide robust error boundaries. |
| **4. Zero Unfinished Code** | Complete all default branches, missing switch cases, and fallback handlers. Ensure all catalog components have complete parameter schemas and validation rules. |
| **5. Zero Duplicate Code** | Extract duplicated algorithms (pin-count resolution between geometry and symbols, orientation angle mapping, signal name extraction, color cycling) into single-source-of-truth utility modules. |
| **6. Maximum Unit Test Coverage** | Install `@vitest/coverage-v8`, configure minimum 90%+ branch/statement thresholds, and implement dedicated unit tests for all currently untested files and edge cases. |

---

## 2. File Inventory & Categorization (32 Files)

```
frontend/src/
├── Shell & Bootstrap
│   ├── main.tsx
│   ├── bootstrap.tsx
│   ├── desktop.ts
│   ├── App.tsx
│   └── styles.css
├── API & Network
│   └── api/client.ts
├── Domain Core & State
│   ├── model/constants.ts        (New centralized constants & enums)
│   ├── model/types.ts
│   ├── model/componentSchema.ts
│   ├── model/geometry.ts
│   ├── model/validation.ts
│   ├── model/store.ts
│   ├── model/keybindings.ts
│   └── model/examples.ts
├── Canvas & Schematic Engine
│   ├── canvas/WireRouter.ts
│   ├── canvas/symbols.tsx
│   ├── canvas/ContextMenu.tsx
│   ├── canvas/Sheet.tsx
│   └── hooks/useEditor.ts
├── Tool Palettes & Properties
│   ├── palette/Palette.tsx
│   ├── palette/CommandPalette.tsx
│   ├── properties/PropertiesPanel.tsx
│   └── properties/SimulationPropertiesPanel.tsx
└── Simulation & Waveform Instruments
    ├── simulation/simSteps.ts
    ├── simulation/scopes.ts
    ├── simulation/chartData.ts
    ├── simulation/traceColors.ts
    ├── simulation/viewWindow.ts
    ├── simulation/useScopeController.ts
    ├── simulation/FftPanel.tsx
    ├── simulation/LossPanel.tsx
    ├── simulation/ScopeViewTab.tsx
    └── simulation/SimulationDrawer.tsx
```

---

## 3. Phased Execution Roadmap

- [x] **Phase 0: Testing Infrastructure & Coverage Baseline**
  - Installed `@vitest/coverage-v8`
  - Configured `vite.config.ts` with coverage reporter, inclusions, and thresholds
  - Ran initial coverage audit (Baseline: 64.77% statements across project)
- [x] **Phase 1: Domain Core & Schemas**
  - Created `src/model/constants.ts` (Orientation, LkComponentType, ControlComponentType, CanvasMetrics, Sentinels)
  - Refactored `src/model/types.ts` with strict types, PointTuple, and full TSDoc
  - Refactored `src/model/componentSchema.ts` (deduplicated pin calculation with `resolveComponentPinCounts`, eliminated `'NIX_NIX_NIX'`, replaced magic numbers with enums)
  - Refactored `src/model/geometry.ts` (Orientation enums, LkComponentType enums, deduplicated pin count logic, full TSDoc)
  - Refactored `src/model/validation.ts` (LkComponentType enums, named constants, full TSDoc)
  - Created `test/constants.test.ts`
  - Expanded `test/geometry.test.ts`, `test/componentSchema.test.ts`, `test/validation.test.ts`
  - Model coverage reached **98.18%** statements, **100%** functions, **82.3%** branches
- [x] **Phase 2: Client & State Layer**
  - Refactored `src/api/client.ts` (dynamic origin resolution `apiBase()`, strongly typed `ApiError` class, full TSDoc, constants for chunking & reconnection)
  - Reviewed `src/desktop.ts` (100% covered, clean IPC bridge)
  - Refactored `src/model/store.ts` (Orientation enums, zero magic numbers)
  - Created `test/examples.test.ts` (verifying all pre-defined templates in `src/model/examples.ts`)
  - Expanded `test/client.test.ts` (ApiError, pause/resume/cancel simulation endpoints)
  - Passed all 23 test suites (241 tests) with 0 lint errors
- [x] **Phase 3: Canvas Engine & Schematic Graphics**
  - Refactored `src/canvas/WireRouter.ts` (extracted `ROUTE_DETOUR_OFFSETS` constant, verified obstacle avoidance & wire deconfliction)
  - Deduplicated & documented `src/canvas/symbols.tsx` (re-used `resolveComponentPinCounts`, replaced magic numbers with `Orientation` and `CANVAS_METRICS`)
  - Reviewed & unit-tested `src/canvas/ContextMenu.tsx` with dedicated test suite `test/ContextMenu.test.tsx`
  - Refactored `src/canvas/Sheet.tsx` (replaced magic orientation and zoom scaling numbers with `Orientation` and `CANVAS_METRICS`)
  - Refactored `src/hooks/useEditor.ts` (replaced poll interval with `SIMULATION_DEFAULTS.POLL_INTERVAL_MS`); created comprehensive hook test suite `test/useEditor.test.ts`
  - Created `test/symbols.test.tsx` testing symbol rendering, preview, angles, and pin distributions
  - Passed all 26 test suites (258 tests) with 0 lint errors
- [x] **Phase 4: Tooling & Properties Panels**
  - Refactored `src/palette/Palette.tsx` (replaced magic number `61` with `ControlComponentType.LEGACY_JAVA_FUNCTION`, full TSDoc); created `test/Palette.test.tsx` (7 tests, 97% coverage)
  - Refactored `src/palette/CommandPalette.tsx` (exported `getMatchScore`, full TSDoc); created `test/CommandPalette.test.tsx` (14 tests, 100% coverage)
  - Refactored `src/properties/PropertiesPanel.tsx` (exported `PropertiesPanelProps`, full TSDoc); expanded `test/PropertiesPanel.test.tsx` to 10 tests covering rename, rotate, delete, collapse, and ScriptBlockEditor
  - Refactored `src/properties/SimulationPropertiesPanel.tsx` (exported `SimulationPropertiesPanelProps`, full TSDoc); created `test/SimulationPropertiesPanel.test.tsx` (6 tests, 94% coverage)
  - Passed all 29 test suites (287 tests) with 0 lint errors
- [x] **Phase 5: Simulation & Waveforms**
  - Refactored `src/simulation/scopes.ts` (replaced magic numbers with `ControlComponentType` enums, added full TSDoc); created `test/scopes.test.ts` (12 tests, 100% coverage)
  - Refactored `src/simulation/traceColors.ts` (added full TSDoc, verified negative index handling); created `test/traceColors.test.ts` (6 tests, 100% coverage)
  - Refactored `src/simulation/useScopeController.ts` (full TSDoc on interfaces and hook); created `test/useScopeController.test.ts` (7 tests, 100% coverage)
  - Refactored `src/simulation/ScopeViewTab.tsx` (exported `sampleIndexAt`, documented `ScopeViewTabProps` and `inferSignalUnit`); expanded `test/ScopeViewTab.test.tsx` to 13 tests
  - Refactored `src/simulation/SimulationDrawer.tsx` (shared canonical `CHANNEL_TRACE_COLORS`, exported `SimulationDrawerProps`, full TSDoc); created `test/SimulationDrawer.test.tsx` (5 tests, 66% coverage)
  - Verified 100% statement coverage across `chartData.ts`, `scopes.ts`, `simSteps.ts`, `traceColors.ts`, `useScopeController.ts`, `viewWindow.ts`, and `LossPanel.tsx`
  - Passed all 33 test suites (325 tests) with 0 lint errors
- [x] **Phase 6: Shell, Styles & Bootstrap**
  - Refactored `src/main.tsx` (parameterized root target container with null safety, exported `start`, full TSDoc)
  - Created `test/main.test.tsx` (3 tests testing null container, backend startup failures, and app mounting)
  - Expanded `test/bootstrap.test.tsx` to 6 tests (tested `Retry` reload and desktop `Open engine logs` button)
  - Audited `src/styles.css` for semantic token consistency across dark and light modes
  - Cleaned up React test `act(...)` warnings in `test/App.flow.test.tsx` and `test/Sheet.keyboard.test.tsx`
  - Reached **100%** statement and branch coverage on `bootstrap.tsx` and `desktop.ts`
  - Passed all 34 test suites (330 tests) with 0 lint errors
- [ ] **Phase 7: Full Verification & Coverage Audit**
  - Run full test suite with coverage (`npm run test:coverage`)
  - Run linter verification (`npm run lint`)
  - Summarize completed code improvements, zero magic numbers, modularity, and test metrics
