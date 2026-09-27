# GeckoCIRCUITS Frontend Architecture: Multi-Tab Workspace & Unified Simulation Setup

## Document Information
* **File Location**: `frontend/MULTI_TAB_WORKSPACE_PLAN.md`
* **Status**: Approved Implementation Plan (Ready for Execution)
* **Target Version**: GeckoCIRCUITS Web EDA v1.1

---

## 1. Overview & Goals

This plan outlines the architecture, UX design, and step-by-step implementation for upgrading the GeckoCIRCUITS Web Editor from a 2-tab layout into a professional, multi-tab EDA workstation.

### Key Objectives:
1. **Dynamic Tab Bar**: Automatically creates and maintains dedicated tabs for:
   * Permanent `📐 Schematic` tab at index 0.
   * Each oscilloscope instrument block present in the schematic (e.g., `📺 SCOPE.1`, `📺 SCOPE.2`).
   * Each script/function block present in the schematic (e.g., `💻 SCRIPT`, `💻 Controller`).
   * **Zero clutter**: Tabs are strictly tied to circuit elements—no manual `[ ➕ ]` button. A tab appears when a scope or script block is added to the circuit and disappears if deleted.
2. **Top Bar Simulation Controls (Option B - Direct Run & Settings Modal)**:
   * Prominent **`▶ Run Simulation (F5)`** button always visible in the top toolbar across all tabs.
   * Clicking **`▶ Run`** executes immediately using current/saved parameters.
   * Adjacent **`⚙` (Setup)** button opens the **Simulation Configuration Modal** for parameter adjustments (`tEnd`, `dt`, solver, initial conditions, validation warnings).
   * Live execution state with progress pill, simulation time, `⏸ Pause`, and `⏹ Stop`.
3. **Full-Viewport Dedicated Workspaces**:
   * **Scope View Tab (`ScopeViewTab.tsx`)**: Fullscreen digital storage oscilloscope (DSO) dedicated to that specific scope's channels, cursors, FFT, and measurements.
   * **Script IDE Tab (`ScriptViewTab.tsx`)**: Fullscreen code editor replacing the cramped 178 px sidebar, featuring line numbers, breakpoint gutter, terminal pin configuration, syntax cheatsheet, and live watch table.

---

## 2. Global Simulation Architecture (Option B)

### 2.1 Top Toolbar Action Cluster
In `App.tsx`, the top navigation bar receives an always-accessible simulation cluster:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ GeckoCIRCUITS  New  Open  Save .ipes  Examples ▾ │ ↩  ↪  Wire │ [ ▶ Run (F5) ] [ ⚙ ] │ Palette  ...│
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### States:
* **Idle / Ready**:
  * `[ ▶ Run (F5) ]`: Starts simulation immediately with active settings.
    The effective settings come from the shared `resolveActiveSimSettings` resolver
    (user settings > engine defaults > built-in `20m` / `1u` / backward-euler), so
    quick run and the configuration modal always agree. The shortcut and button are
    ignored while a run is `RUNNING`/`PENDING` to prevent double-starts.
  * `[ ⚙ ]`: Opens the **Simulation Configuration Modal**.
* **Running / Simulating**:
  * Button morphs into: `[ ⏳ 45% (t=9.0ms) ]` + `[ ⏸ Pause ]` + `[ ⏹ Cancel ]`.
* **Completed / Finished**:
  * Status returns to `[ ▶ Run (F5) ]` with a brief green flash `✓ Done (20ms in 0.18s)`.
  * Open scope tabs immediately display updated waveforms.

---

### 2.2 Simulation Configuration Modal (`SimConfigModal.tsx`)

A modern dialog accessible via the `⚙` button (or if run is triggered when settings are invalid):

```
┌───────────────────────────────────────────────────────────────────────────────────────┐
│ ⚙ Simulation Parameters & Solver Configuration                                     ✕  │
├───────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                       │
│  Timing Parameters                                                                    │
│  ┌────────────────────────────────────────┐ ┌────────────────────────────────────────┐│
│  │ Total Duration (t_end)                 │ │ Maximum Time Step (dt)                 ││
│  │ [ 20m                                ] │ │ [ 1u                                 ] ││
│  │ e.g. 20m, 100m, 1.5s                   │ │ e.g. 1u, 100n, 10u                     ││
│  └────────────────────────────────────────┘ └────────────────────────────────────────┘│
│                                                                                       │
│  Solver & Algorithms                                                                  │
│  ┌────────────────────────────────────────┐ ┌────────────────────────────────────────┐│
│  │ Numerical Solver                       │ │ Initial Circuit States                 ││
│  │ [ Backward Euler (Stiff, Default)    ▾]│ │ [ Zero Initial States                ▾]││
│  └────────────────────────────────────────┘ └────────────────────────────────────────┘│
│                                                                                       │
│  Workload Estimation:                                                                 │
│  ℹ Calculated steps: 20,000 steps  •  Expected time: < 0.25s  •  Memory: ~2.4 MB      │
│                                                                                       │
│  Active Scopes & Output Signals:                                                      │
│  • SCOPE.1 (2 traces: VOLT_IN, VOLT_OUT)                                              │
│  • SCOPE.2 (3 traces: VOLT_R1, VOLT_L1, VOLT_C1)                                      │
│                                                                                       │
│  Validation Status:                                                                   │
│  ✓ 8 components, 14 wires connected. Circuit is ready.                                │
│                                                                                       │
├───────────────────────────────────────────────────────────────────────────────────────┤
│  [ Cancel (Esc) ]                                             [ ▶ Start Simulation ]  │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Dynamic Multi-Tab Workspace Architecture

### 3.1 Tab Bar Model (`.workspace-tabs-bar`)
The tab bar reflects the circuit components:

```
[ 📐 Schematic ]  [ 📺 SCOPE.1 ]  [ 📺 SCOPE.2 ]  [ 💻 SCRIPT ]
```

* **No arbitrary `[ ➕ ]` button**: Tabs represent tangible blocks in the circuit.
* **Component-Synchronized Lifecycle**:
  * Adding a Scope or Script block to the schematic automatically registers a tab.
  * Deleting the component from the canvas removes its tab. If the active tab was deleted, navigation safely falls back to `📐 Schematic`.
  * Renaming a component updates the corresponding tab title immediately, and an *active* tab follows its component through the rename (scope/script kind must be unchanged).
* **Double-Click Canvas Navigation**:
  * Double-clicking a Scope block on the schematic switches active tab to `[ 📺 SCOPE.X ]`.
  * Double-clicking a Script block on the schematic switches active tab to `[ 💻 SCRIPT.X ]`.
  * Single-clicking still selects the component and displays its quick properties in the right inspector.

---

### 3.2 Tab State Data Model
In `App.tsx` / `types.ts`:

```ts
export type ActiveTabId = 
  | 'schematic'
  | `scope:${string}`   // e.g. 'scope:SCOPE.1'
  | `script:${string}`; // e.g. 'script:SCRIPT'

export interface CircuitTab {
  id: ActiveTabId;
  title: string;
  icon: string;
  type: 'schematic' | 'scope' | 'script';
  targetName?: string;
  hasData?: boolean;
}
```

---

## 4. Dedicated Workspace Views

### 4.1 Schematic Workspace (`activeTab === 'schematic'`)
* Main interactive canvas (`<Sheet />`).
* Left component palette (`<Palette />`).
* Bottom status bar (grid position, component count, selection count).
* Right sidebar (`<PropertiesPanel />`) for editing discrete components (R, L, C, diodes, voltage sources).

### 4.2 Dedicated Oscilloscope View (`activeTab === 'scope:...'`)
* Renders `<ScopeViewTab />` bound to the specific scope instrument (e.g. `selectedScope="SCOPE.1"`).
* Fullscreen DSO bezel with dark/light theme support.
* Direct access to:
  * Traces connected to this scope's inputs.
  * Cursors A & B with $\Delta t$, $\Delta V$, $1/\Delta t$ readout.
  * Horizontal zoom/pan, time/div knob.
  * FFT spectrum analysis and Loss breakdown panels.
  * One-click CSV export of this scope's recorded signals.

### 4.3 Dedicated Script IDE View (`activeTab === 'script:...'`)
* New component: `frontend/src/properties/ScriptViewTab.tsx`.
* Layout:
  * **Top Sub-Toolbar**: Script block name, syntax status pill, `[ ✓ Apply Script (Ctrl+S) ]`, `[ ℹ Cheat Sheet ]`.
  * **Main Code Editor**: Full-height `<ScriptCodeEditor />` with line numbers, syntax highlighting, breakpoint toggles in gutter, and step pause highlighting.
  * **Right Config & Watch Panel** (resizable):
    * Terminal Pin Counts: Input terminals ($x_{in}$), Output terminals ($y_{out}$).
    * Live Watch Table during simulation:
      * Current simulation time $t$, time step $dt$.
      * Inputs $u_1, u_2, \dots$
      * Outputs $y_1, y_2, \dots$
      * Internal persisted variables (`integral`, `state`, etc.).
    * Debugger Action Bar: `[ ▶ Continue ]`, `[ ↳ Step Over ]`.

---

## 5. Implementation Roadmap & Checklist

### Phase 1: Global Simulation Controls & Parameters Modal
- [x] Create `frontend/src/simulation/SimConfigModal.tsx`
  - Parameter inputs: $T_{end}$, $\Delta t$, Solver selection.
  - Step counter & workload estimator.
  - Signal overview and pre-run validation checks.
  - Apply & Run callback.
- [x] Update `frontend/src/App.tsx` top navbar:
  - Add `[ ▶ Run Simulation ]` button with keyboard shortcut `F5` / `Ctrl+Enter`.
  - Add `[ ⚙ ]` button to trigger `SimConfigModal`.
  - Add execution progress & cancel/pause buttons while running.
- [x] Add modal styles to `frontend/src/styles.css`.

### Phase 2: Dynamic Multi-Tab State Architecture
- [x] Helper functions in `frontend/src/simulation/scopes.ts` and `frontend/src/model/componentSchema.ts`:
  - `isScopeComponent(comp)`: checks for scope blocks.
  - `isScriptComponent(comp)`: checks for script/function blocks.
- [x] Update `App.tsx` tab state:
  - Derive tabs dynamically from `state.components`.
  - Track `activeTabId`: `'schematic' | 'scope:<name>' | 'script:<name>'`.
  - Fallback to `'schematic'` if the active component is deleted.
- [x] Render dynamic tabs in `.workspace-tabs-bar`:
  - Icon (`📐`, `📺`, `💻`), component name, and active indicator.
  - Waveform data dot indicator on scope tabs when results exist.

### Phase 3: Dedicated Full-Viewport Script IDE (`ScriptViewTab.tsx`)
- [x] Create `frontend/src/properties/ScriptViewTab.tsx`:
  - Full-height code editor view.
  - Live watch variable panel.
  - Terminal pins configurator (inputs/outputs).
  - Breakpoint controls and cheat sheet drawer.
- [x] Mount `<ScriptViewTab />` in `App.tsx` when a script tab is active.
- [x] Add IDE styles to `frontend/src/styles.css`.

### Phase 4: Individual Scope View Integration
- [x] Connect `ScopeViewTab.tsx` directly to the active tab's scope name.
- [x] Ensure channel filtering correctly isolates signals connected to the specific scope block.
- [x] Display scope name and trace count in the header.

### Phase 5: Canvas Double-Click Interaction
- [x] Update `Sheet.tsx`:
  - Double-clicking a Scope block activates its scope tab: `actions.openScopeTab(scopeName)`.
  - Double-clicking a Script block activates its script tab: `actions.openScriptTab(scriptName)`.
- [x] Ensure single-click still selects the component and maintains property inspection.

### Phase 6: Automated Testing & Verification
- [x] Unit tests for `SimConfigModal.tsx`: validation, input parsing, submit (`test/SimConfigModal.test.tsx`).
- [x] Unit tests for dynamic tab derivation and navigation logic (`test/MultiTabWorkflow.test.tsx`).
- [x] Unit tests for `ScriptViewTab.tsx`: script update, terminal count changes, breakpoint toggling (`test/ScriptViewTab.test.tsx`).
- [x] Canvas double-click interaction unit test (`test/Sheet.interaction.test.tsx`).
- [x] Full test suite verification: 39 test suites, 362 tests passing.

### Phase 7: Scope Settings Isolation & Project File Persistence (.ipes)
- [x] Dedicated `ScopePropertiesPanel.tsx`:
  - Strips all global simulation settings and solver timing inputs ($T_{end}$, $\Delta t$, method) when inspecting a scope.
  - Strips all outside signals from other scopes; strictly shows only channels wired to the active scope.
  - Controls: Display layout (`overlay` vs `stacked`), Timebase ($t/div$, Fit, Zoom, Pan), Scale (`Auto` vs `Fixed`), channel visibility toggles, cursors table, and CSV export for this scope's channels.
- [x] Per-Scope Project File Persistence:
  - Settings (`scopeLayout`, `yScaleMode`, `hiddenSignals`, `cursorsEnabled`) are saved per scope via `actions.setParameter`.
  - Serialized to/from `<scopeSettings>` sub-block in `.ipes` files via `CircuitFileWriter.java` and `CircuitFileParser.java`.
  - Whitelisted in `CircuitEditService.java` (`isNamedControlParameter`).
  - Unit tests for backend round-trip persistence (`CircuitFileWriterTest.roundTrip_scopeSettings_preservesSettings`) and UI behavior (`test/ScopePropertiesPanel.test.tsx`).
  - All 40 test suites (367 tests) passing.
