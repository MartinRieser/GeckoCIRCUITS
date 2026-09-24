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
- [ ] **Phase 2: Client & State Layer**
  - Refactor `src/api/client.ts` (dynamic origin, typed errors, abort handling)
  - Review `src/desktop.ts`
  - Refactor `src/model/store.ts` (bounded history, strict reducer contracts)
  - Add test suite `test/examples.test.ts` for all pre-defined circuits in `src/model/examples.ts`
- [ ] **Phase 3: Canvas Engine & Schematic Graphics**
  - Review `src/canvas/WireRouter.ts` (obstacle avoidance, corner simplification)
  - Deduplicate & document `src/canvas/symbols.tsx`; create `test/symbols.test.tsx`
  - Review `src/canvas/ContextMenu.tsx`; create `test/ContextMenu.test.tsx`
  - Modularize `src/canvas/Sheet.tsx` (CanvasGrid, CanvasWires, CanvasOverlays)
  - Decompose `src/hooks/useEditor.ts`; create `test/useEditor.test.ts`
- [ ] **Phase 4: Tooling & Properties Panels**
  - Review `src/palette/Palette.tsx`; create `test/Palette.test.tsx`
  - Review `src/palette/CommandPalette.tsx`; create `test/CommandPalette.test.tsx`
  - Modularize `src/properties/PropertiesPanel.tsx`; expand tests
  - Review `src/properties/SimulationPropertiesPanel.tsx`; create `test/SimulationPropertiesPanel.test.tsx`
- [ ] **Phase 5: Simulation & Waveforms**
  - Review `src/simulation/simSteps.ts`, `scopes.ts`, `chartData.ts`, `traceColors.ts`, `viewWindow.ts`, `useScopeController.ts`
  - Create missing test suites: `scopes.test.ts`, `traceColors.test.ts`, `useScopeController.test.ts`
  - Review `src/simulation/FftPanel.tsx` & `src/simulation/LossPanel.tsx`
  - Modularize `src/simulation/ScopeViewTab.tsx` (WaveformCanvas, ScopeCursors, ChannelConfigTable)
  - Review `src/simulation/SimulationDrawer.tsx`; create `test/SimulationDrawer.test.tsx`
- [ ] **Phase 6: Shell, Styles & Bootstrap**
  - Review `src/App.tsx`, `src/bootstrap.tsx`, `src/main.tsx`
  - Audit `src/styles.css` for semantic token consistency
  - Create `test/main.test.tsx`
- [ ] **Phase 7: Full Verification & Coverage Audit**
  - Run `npm test -- --coverage`
  - Run `npm run lint`
  - Ensure zero lint errors, 100% test pass rate, and coverage meets or exceeds 90% threshold
