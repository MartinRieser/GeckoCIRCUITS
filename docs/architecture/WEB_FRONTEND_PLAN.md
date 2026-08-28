# GeckoCIRCUITS Web Frontend — Implementation Plan

Status: APPROVED, ready for implementation
Branch: `feature/web-frontend`
Decisions locked in by the product owner:

1. New **React + TypeScript** web frontend; backend = extended `gecko-rest-api`.
2. The old Swing UI stays fully functional and unchanged. Both frontends are compared
   side-by-side during development.
3. **Result verification gate**: simulation results must be numerically identical between
   the old Swing engine and the headless engine. Scope for now: **electrical (LK) circuits
   only**. Control-domain headless parity is a separate future workstream
   (see `docs/architecture/SOLVER_GAP_ANALYSIS.md`).
4. Implementation order starts with **P0 (backend foundations)**.
5. Keyboard-first operation is a first-class requirement; mouse usage always remains
   possible as an alternative.

---

## 0. Engineering principles (BINDING for all phases)

The code base must NOT explode. These rules override any instruction an implementing
agent considers "nicer".

1. **Economy of code.** Every new class must earn its existence. Prefer adding methods to
   existing well-fitting classes over creating new files. Target: the whole program
   (phases P0–P5) adds roughly < 6.000 lines of Java and < 8.000 lines of TypeScript.
   If a phase is about to exceed its budget, stop and simplify instead.
2. **No speculative abstraction.** No interface with a single implementation, no
   `Abstract*Factory*Provider` chains, no DTO-mapper-DTO sandwiches, no `Optional`
   wrapping for internal calls, no builder pattern for objects with <= 3 fields.
3. **Follow the existing design.** New REST code goes into the existing
   controller/service pattern of `gecko-rest-api`. New core code goes into
   `gecko-simulation-core` next to the classes it belongs to. Do not invent parallel
   package structures.
4. **Improve what you touch.** When editing legacy code, leave it better than you found
   it (extract a method, remove dead code), but do NOT refactor large areas that are
   unrelated to the task.
5. **Unit tests whenever possible.** JUnit 5 + Mockito already configured. Every new
   service/writer/parser gets tests. GUI-only code that cannot be tested cheaply gets no
   fake tests. Keep the JaCoCo gate (60% on core packages) green.
6. **No AI slop.** Forbidden:
   - comments that restate the code (`// set the value` before `setValue(...)`)
   - javadoc on trivial private members
   - defensive try/catch that only logs and swallows
   - `throws Exception` signatures
   - generated boilerplate headers/footers, emoji, "This class is responsible for..."
     essays on obvious classes
   - unused parameters "for future extension"
   - comments must be in English and explain *why*, not *what* (existing German
     identifiers are legacy; new identifiers are English)
7. **Build must stay green.** After every work package:
   `mvn verify` (parent) must pass, including Checkstyle/PMD/SpotBugs as configured.
8. **Commits**: conventional-commit style as used in this repo
   (`feat(rest): ...`, `fix(core): ...`, `test(rest): ...`). One logical change per
   commit. Never commit secrets.
9. **Frontend minimalism.** Dependencies allowed: `react`, `react-dom`, `typescript`,
   `vite`, and (from P4 only) one lightweight chart library. Nothing else without
   explicit approval — no UI framework, no CSS-in-JS, no state-management library.
   Plain SVG + CSS. If state management becomes necessary, `useReducer` + context first.
10. **No reformatting of untouched code.** No whitespace-only diffs, no import
    re-ordering orgies outside the files you actually change.

---

## 1. Current-state facts (verified in code — trust these, do not re-explore)

Module layout (Maven, parent `pom.xml`):
- `src/modules/gecko-simulation-core` — GUI-free engine, ~216 files / ~29.5k LOC
- `src/modules/gecko-gui` — Swing app, ~802 files / ~135k LOC
- `src/modules/gecko-rest-api` — Spring Boot, headless-only (enforcer plugin BANS
  Swing/AWT deps in this module, `src/modules/gecko-rest-api/pom.xml:101-133`)

Key classes (paths relative to repo root):

| Fact | Location |
|---|---|
| REST app entry | `src/modules/gecko-rest-api/src/main/java/gecko/rest/GeckoRestApiApplication.java` |
| REST config: port 8080, context `/gecko` | `src/modules/gecko-rest-api/src/main/resources/application.properties:2-3` |
| Simulation endpoints | `.../gecko/rest/controller/SimulationController.java` (`POST /api/v1/simulations`:54, results `/{id}/results`:98, SSE `/{id}/stream`:196, cancel DELETE:256, batch:372) |
| Circuit upload/parse (read-only) | `.../gecko/rest/controller/CircuitFileController.java` (`POST /parse`:39 + base64:72, `/{id}/components`:139, clone:284, `PUT /{id}/parameters`:318) |
| Circuit store (in-memory) | `.../gecko/rest/service/CircuitFileService.java` (ConcurrentHashMap:29, `getRawCircuit` returns `model.toString()`:187) |
| Simulation store | `.../gecko/rest/service/SimulationService.java` (`buildSimulationConfig`:420-432 requires server-local file path; results copied to Map<String,double[]>:482-507) |
| **Gap**: CircuitFileService and SimulationService are disconnected | no reference between them |
| **Gap**: docs promise base64/circuitId input for simulations | `docs/api/rest-api.md:350,790` — NOT implemented |
| **Gap**: docs promise pause/resume endpoints | `docs/api/rest-api.md:384-394` — NOT implemented; engine `pause()` doesn't halt the loop (`HeadlessSimulationEngine.java:345-357`, loop checks only cancel at :191) |
| WS/STOMP: `/ws` (SockJS), `/ws-raw`, broker `/topic`, prefix `/app` | `.../gecko/rest/config/WebSocketConfig.java:40-49` |
| WS progress broadcaster | `.../gecko/rest/service/WebSocketProgressService.java` (topic `/topic/simulations/{id}`) |
| Headless engine (real MNA solver, electrical only) | `src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/HeadlessSimulationEngine.java` (`executeSimulation`:132-269, parse:445-457, signals fallback "V_out/I_in/P_loss":459-465) |
| .ipes reader (gzip-tolerant) | `.../gecko/core/io/CircuitFileParser.java` (gzip sniffing:126-136, tolerant param parsing:451-459) |
| Netlist builder (label-based connectivity) | `.../gecko/core/circuit/netlist/NetlistBuilder.java:137-257` |
| **No .ipes writer anywhere in core** — the only serializer is Swing-side | `src/modules/gecko-gui/src/main/java/gecko/geckocircuits/general/ProjectData.java` (`exportASCII`:120-188), `gecko/geckocircuits/circuit/AbstractBlockInterface.java` (`exportASCII`:422-473), `Connection.java` (`exportASCII`:291-309), gzip write `MainWindow.rawSaveFile` (`general/MainWindow.java:870-904`) |
| Result data container | `.../core/datacontainer/DataContainerGlobal.java` (extends Observable:27) |
| Test circuit fixture | `src/modules/gecko-rest-api/src/test/resources/test-circuit.ipes` |

### .ipes format essentials (needed for P0)

File = **gzip-compressed line-oriented ASCII**. Both space- and `/`-separated string
arrays accepted by the reader. Empty string arrays are encoded `NIX_NIX_NIX`
(`ProjectData.java:81-83`). Component block example (tokens the reader consumes):

```
<ElementLK> (i)
labelAnfangsKnoten[] <labels...>
labelEndKnoten[] <labels...>
enabledShorted <bool>
parentSheetIdentifier <...>
typ <int>
uniqueObjectIdentifier <int>
x <int>
y <int>
parameter[] <space separated doubles>
parameterString[] </-separated>
nameOpt[] </-separated>
orientierung <int 0..3>
idStringDialog <name>
<\ElementLK>
```

Wire block:

```
verbindungLK (i)
<Connection>
label <string>
x[] <space separated ints>
y[] <space separated ints>
connectorType <string>
<\Connection>
```

Globals written at project level: `optimizerName[]/optimizerValue[]`,
`<scripterCode>`, `DtStor`, `tDURATION`, `dt`, `tPAUSE`, `T_pre`, `dt_pre`,
`solverType`, `path`, display settings, `FileVersion`, `UniqueFileId`,
`dataContainerSignals[]` (`ProjectData.java:131-182`).

The authoritative token list for READING is `CircuitFileParser` + `TokenMap`
(`.../core/circuit/TokenMap.java:25-52`). The writer must emit exactly the tokens the
reader knows; anything else is a bug.

### Existing GUI interaction reference (behavioral spec for the web editor)

- Placement: palette click arms type, component follows cursor, left-click places,
  right-click rotates, Esc cancels (`SchematicEditor2.java`: placement :192/:520,
  rotate :543/:1094, Esc :1033).
- Wiring: `w` toggles wire mode, click start, orthogonal L-route follows mouse, click
  end (`SchematicEditor2.java:1068/:1249-1273`; L-router `Connection.java:140-201`).
- Connectivity semantics: wires and terminals connect through coincident grid
  coordinates OR identical node labels — labels are assigned by double-click on a
  terminal (`AbstractBlockInterface.java:985-991`), netlist via label matching
  (`NetlistBuilder.java:137-257`).
- Grid raster: `dpix` (display settings), positions are integer raster coordinates.
- Undo: Swing `UndoManager` in `.../gecko/modelviewcontrol/AbstractUndoGenericModel.java:46`.

---

## 2. Target architecture

```
[Swing UI (unchanged)] ──opens/saves──┐
                                      ├── .ipes file  (single source of truth)
[Web editor (new)] ◄──REST/WS── [extended gecko-rest-api] ──► [gecko-simulation-core]
```

- The `.ipes` file is the only interchange format. No live model sharing between the
  two frontends, no shared event bus between JVMs. Sync = save + reload (+ WS model
  version notifications inside the web editor session).
- The web frontend is a static SPA (Vite build output) served from Spring Boot's
  static resources in production; during development it runs on the Vite dev server
  with a proxy to `:8080/gecko`.

---

## 3. Phases

Each phase ends with: all tests green, `mvn verify` green, phase acceptance criteria
met, work committed. Work through phases strictly in order.

### P0 — Backend foundations (.ipes round-trip) — FIRST MILESTONE

**Goal:** upload `.ipes` → parse → write back → byte-semantic-equal file that the old
Swing UI opens correctly.

Tasks:
1. New class `gecko.core.io.CircuitFileWriter` in `gecko-simulation-core`
   (next to `CircuitFileParser`). API sketch:
   - `static byte[] write(CircuitModel model)` — returns gzip'd bytes
   - `static void write(CircuitModel model, Path file)`
   - Implement it as the *inverse* of `CircuitFileParser`: iterate the model's
     components/connections and emit exactly the token grammar documented in §1.
     Unit-test against the parser (round trip), NOT against GUI code (core must not
     depend on gui).
2. Extend `CircuitModel` only where it lacks data the writer needs (e.g. preservation of
   unknown/raw tokens). Keep additions minimal; if the parser currently drops tokens
   that the Swing writer emits (e.g. display settings, `UniqueFileId`), store the raw
   token map in `CircuitModel` once (`Map<String,String> extraTokens`) and re-emit it,
   rather than modeling every field.
3. REST: `PUT /api/v1/circuits/{circuitId}` → saves edited model (P1), for P0 it
   re-serializes the parsed model and returns it as downloadable `.ipes`
   (`application/gzip`). Add `GET /api/v1/circuits/{circuitId}/ipes` returning the
   rewritten file. Implement in the existing `CircuitFileController`/`CircuitFileService`.
4. Fix the documented-but-missing simulation input: `POST /api/v1/simulations` accepts
   `circuitId` (resolved via `CircuitFileService`) and inline base64 content, in addition
   to the existing server-local path. Wire `CircuitFileService` → `SimulationService`
   (this closes the disconnected-stores gap). Update `docs/api/rest-api.md` where
   behavior differs from docs.
5. Tests:
   - `CircuitFileWriterTest`: for every `.ipes` fixture in the repo
     (`src/modules/gecko-rest-api/src/test/resources/*.ipes` + a couple of circuits from
     the examples/demos resources if reachable): parse → write → parse → model equality
     (implement `equals`/a small `CircuitModelDiff` test helper comparing components,
     positions, parameters, labels, wires).
   - REST integration test: `POST /parse` → `GET /{id}/ipes` → re-`POST /parse` → ids
     behave identically; `POST /simulations` with `circuitId` runs the simulation.

Acceptance:
- Round-trip test green for all fixtures.
- A file written by the new writer opens in the Swing UI (manual check, documented in
  the PR/commit message; part of the phase definition of done).

### P1 — Editing REST API

**Goal:** full CRUD circuit editing over REST, the foundation for both mouse and
keyboard workflows.

New endpoints (in the existing controllers; keep the existing URL style):

```
POST   /api/v1/circuits/{id}/components        create (type, name, x, y, orientation)
PATCH  /api/v1/circuits/{id}/components/{name}  move / rotate / rename / set parameters
DELETE /api/v1/circuits/{id}/components/{name}
POST   /api/v1/circuits/{id}/connections        create wire (point list + connectorType)
PATCH  /api/v1/circuits/{id}/connections/{i}    move points
DELETE /api/v1/circuits/{id}/connections/{i}
PUT    /api/v1/circuits/{id}/nodes/{terminalRef} set node label (name labels are the
                                                   electrical connectivity mechanism)
GET    /api/v1/circuits/{id}/catalog             component type catalog (from the type
                                                   registry in core) for the palette
```

Design constraints:
- One new service class `CircuitEditService` (in `gecko/rest/service/`) holding the
  mutation logic; controllers stay thin. No repository layer — the in-memory
  ConcurrentHashMap store is sufficient and already there.
- Every mutation bumps a monotonically increasing `modelVersion` on the stored circuit
  and produces the new `.ipes` bytes lazily on demand.
- Broadcast change events on the existing STOMP broker: topic
  `/topic/circuits/{id}`, payload `{modelVersion, operation, payload}` — reuse the
  pattern of `WebSocketProgressService`.
- Command log: keep a bounded in-memory list of the last N (e.g. 200) edit operations
  per circuit as the foundation for undo/redo; `POST .../undo`, `POST .../redo` apply
  inverse operations. Do NOT build a general framework — inverse-op map per operation
  type is enough.
- Validation: reject unknown types, off-grid coordinates (snap server-side to `dpix`
  raster), duplicate names (auto-suffix like the GUI does).
- Fix docs drift found in P0 (`pause`/`resume` either implemented later in P4 or
  removed from docs — decide when P4 starts; until then remove the wrong doc sections).

Tests: API-level CRUD tests for every endpoint (happy path + one error case each);
every mutation leaves the model serializable (assert via P0 writer round-trip call in
the test).

### P2 — Web editor MVP (mouse parity)

**Goal:** the editor can do what the Swing sheet can do with the mouse: place, move,
rotate, wire, save, open.

**Status: IMPLEMENTED (incl. reviewed fixes for classic behavior parity).**
Locked-in classic behaviors (verified against `SchematicEditor2.java` /
`Connection.java`):
- Wire pen stays armed after each wire (`w`/toolbar toggles it, Esc aborts only the
  draft; a second Esc leaves wire mode).
- L-router keeps its horizontal/vertical preference once the draft leaves the start
  point (port of `_movementWestEast`), and committed wires are stored as dense
  per-raster-step point lists — the classic export format, which is also the
  connectivity contract (a terminal touching ANY listed raster point connects;
  `NetlistBuilder.buildFromWiresAndComponents` implements the same semantics).
- Moving works like the classic editor: click grabs, the group follows the cursor,
  the next click drops; a held-button drag commits on release; Esc restores the
  original positions.
- Component creation seeds the classic GUI default parameters server-side
  (`CircuitEditService.DEFAULT_PARAMETERS`), so fresh circuits are simulable.

Scope pulled forward during P2 (reviewed and kept):
- Ctrl+K command palette and the basic keymap (part of P3) — arrow-key ghost
  placement, keyboard wiring and nudging remain P3 work.
- A simulation slice of P4 (run via `circuitId`, REST polling, SVG waveform
  chart, CSV export). Signal selection from `dataContainerSignals[]`,
  SSE/WS progress and the pause/resume decision remain P4 work.
- Canvas zoom/pan (was P6). Dark mode and HiDPI polish remain P6.
- Built-in example circuits + blank-circuit template (not previously planned;
  kept because they make the editor instantly explorable).

Budget note (principle 1): the frontend is ~8.5k lines incl. tests and CSS,
above the 8k target for the whole program. Accepted deviation: ~2.9k of it are
the component metadata table, example circuits and stylesheet that carry the
approved GUI design; the interactive logic (store, sheet, router, client) is
~1.7k. Revisit before P3 grows it further.

Structure (new top-level directory `frontend/` — OUTSIDE the Maven modules, so the
Java build is untouched):

```
frontend/
  package.json, vite.config.ts, tsconfig.json
  src/
    main.tsx
    api/client.ts          tiny REST+WS client, hand-rolled fetch wrappers, no SDK generation
    model/types.ts         CircuitModel, Component, Connection types mirroring the REST JSON
    model/store.ts         useReducer-based editor store (documented state machine)
    canvas/Sheet.tsx       the SVG schematic sheet (grid, components, wires, ghost preview)
    canvas/symbols/*.tsx   one SVG symbol per component family (start with top ~20 types)
    canvas/WireRouter.ts   port of the L-router logic from Connection.java:140-201
    palette/Palette.tsx    component palette fed by /catalog, keyboard-searchable
    App.tsx                layout: palette left, sheet center, status bar
```

Behavior spec (= what the Swing editor does, see §1 "GUI interaction reference"):
- Palette click (or Enter on highlighted palette entry) arms a type; moving the mouse
  over the sheet shows a ghost snapped to the `dpix` raster; left-click places
  (optimistically in the store; PATCH/POST to server); right-click or `r` rotates the
  ghost by 90°; Esc cancels.
- Drag moves selection with raster snap; multi-select via rubber band (drag on empty
  space); Delete removes; double-click on a component opens the parameter panel
  (a React side panel, NOT a modal dialog).
- Wire mode: toggle with `w` or toolbar button; click start terminal, orthogonal
  preview follows mouse (L-shape, horizontal/vertical preference by drag direction —
  port `Connection.setCurrentPointOnConnection`), click end; connectivity by shared
  grid coordinates or equal node labels (offer a "label" action in the terminal
  context menu).
- Open/Save: toolbar buttons → REST endpoints from P0/P1; model version from WS keeps
  the tab in sync (warn on external version bump).
- Rendering: plain SVG; symbol components are small functions returning SVG path
  groups; reuse the visual language of the Swing painting code (resistor = rectangle,
  capacitor = two bars, etc. — see `AbstractResistor`, `AbstractCapacitor`, `MOSFET`
  painting code for reference shapes). Do not attempt pixel-identical clones;
  recognizable-correct is enough for MVP.
- No zoom in MVP (P6) — fixed `dpix` scale, scrollable sheet.

Tests: Vitest (comes with Vite ecosystem) for `WireRouter`, store reducer, and API
client (mocked fetch). No component-snapshot tests.

### P3 — Keyboard-first layer [COMPLETE]

**Goal:** full schematic editing without touching the mouse. All features are additive to the mouse workflows.

**Completed in P3:**
1. **Central keybinding map** (`keybindings.ts`): declarative registry mapping shortcuts to semantic editor actions across all modes (`idle`, `placing`, `wiring`, `dragging`). Includes keyboard shortcuts cheatsheet modal (`?`).
2. **Ghost placement by keyboard**: armed ghost steered via arrow keys (1 step / Shift: 5 steps), `r`/`Shift+R` rotation, `Enter` / `Space` places, `Esc` cancels.
3. **Keyboard wiring**: `Tab`/`Shift+Tab` cycles component terminals with glowing visual focus halo; `Enter` starts wire draft; arrow keys steer route step-by-step; `Enter` commits wire; `Esc` aborts draft.
4. **Selection nudging & duplication**: arrow keys nudge selected components with 400ms server debounce; `Ctrl+D` duplicates selection with offset and opens inspector.
5. **Undo/Redo & Shortcuts**: `Ctrl+Z` / `Ctrl+Y` / `Ctrl+Shift+Z` undo/redo with server sync, `Ctrl+S` save, `Del` / `Backspace` delete.
6. **Tests**: full test coverage for keybinding registry, store reducer actions, and end-to-end keyboard editing flows (`Sheet.keyboard.test.tsx`, `keybindings.test.ts`, `store.test.ts`).

### P3.5 — Legacy GUI Parity & Signal/Control Domain [COMPLETE]

**Goal:** Visual, structural, and behavioral parity with legacy Swing GUI domains (LK, CONTROL, THERM) and robust simulation launch.

**Completed in P3.5:**
1. **CONTROL Domain Catalog & Symbols**:
   - Backend: Added 15 control/signal types (`CTRL_VOLT` 1001, `CTRL_AMP` 1002, `CTRL_SCOPE` 1003, `CTRL_SIGNAL` 1004, `CTRL_CONSTANT` 1005, `CTRL_GAIN` 1006, `CTRL_PI` 1007, `CTRL_PT1` 1008, `CTRL_INTEGRATOR` 1009, `CTRL_COMPARATOR` 1010, `CTRL_AND` 1011, `CTRL_OR` 1012, `CTRL_NOT` 1013, `CTRL_MUX` 1014, `CTRL_DELAY` 1015) to `CircuitTypCore.java` and `/catalog` endpoint with `isControl()` filtering.
   - Frontend: Registered 3 new categories in `componentSchema.ts` ("Measurement", "Control & Signal", "Logic Gates") and created matching green SVG symbols in `symbols.tsx`.
2. **Visual Differentiation by Domain**:
   - Canvas & palette color-coding: LK (Electrical) = Sky Blue (`#38bdf8`), CONTROL (Signal/Measurement) = Green (`#4ade80`), THERM (Thermal) = Orange (`#fb923c`).
3. **Wire Domain Typing**:
   - Automatic terminal snap family detection (drafting wires from CONTROL terminals emits `type: 'CONTROL'` connections styled in green).
4. **Simulation Pipeline Fixes**:
   - Replaced auto-run on navbar click with explicit parameter configuration in `SimulationDrawer.tsx`.
   - Surfaced server error messages directly to status bar and drawer.
   - Added descriptive empty states when simulations return without recorded signals.

Known limitation: the 15 CONTROL types are authoring/palette symbols only — no
matrix stampers are registered for them (`StamperRegistry`), so the headless
solver skips them during simulation. Control-domain headless parity is tracked
in `docs/architecture/CONTROL_PARITY_PLAN.md` (evidence: tutorial sweep
`tools/parity/results/20260828-tutorial-sweep.txt`; solver capability gaps in
`docs/architecture/SOLVER_GAP_ANALYSIS.md`).

### P4 — Simulation & results in the web UI [COMPLETE]

Note: an initial slice landed with P2/P3 (sim drawer, run by circuitId, plain-SVG
chart, CSV export). Completed in P4:

1. Sim panel defaults pre-populated from `.ipes` metadata: the editor model
   snapshot (`GET /circuits/{id}/model`) now carries `simulationDefaults`
   (`timeStep`, `duration`, `solverType`, `signals`); the drawer re-seeds its
   inputs whenever another circuit is opened. Live progress runs over the
   existing SSE stream (`/simulations/{id}/stream`), with the REST polling kept
   only as a connection fallback. The drawer's chart stays plain SVG.
2. Signal selection comes from `dataContainerSignals[]` in the file (same
   semantics as the headless engine; positional placeholders and the `[]`
   empty-list marker are filtered). Files without stored signals fall back to
   the circuit's node labels; the sim panel lets the user toggle which signals
   to record, sent as `signals` on the simulation request and honored by
   `HeadlessSimulationEngine`.
3. Pause/resume implemented: `awaitResumeOrCancel()` in the headless loop,
   cancel works from the paused state, `PAUSED` status on the response, REST
   endpoints `POST /simulations/{id}/pause|resume`, and drawer
   Pause/Resume/Cancel buttons with a paused progress bar.
4. Tests: engine-level pause/freeze/resume and cancel-from-paused tests, signal
   override test, controller + service pause/resume tests, E2E
   `SimulationPauseResumeE2ETest` (pause freezes `currentTime`, resume
   continues, cancel from paused), and the frontend chart data mapping unit
   test (`chartData.test.ts`). The P0 circuitId submission path was already
   covered by `CircuitEditE2ETest.simulateEditedCircuitByCircuitId`.

### P5 — Verification harness (acceptance gate) [COMPLETE]

**Goal:** prove "identical simulation results old vs. new" for electrical circuits.

**Completed in P5:**
1. **Reference Runner** (`tools/parity/ReferenceRunner.java`): starts headless AWT `GeckoSim`, executes batch RMI simulation using the legacy engine, and dumps scope signals to CSV (`DataSaver.TxtLinePrinter` format).
2. **New Engine Runner** (`tools/parity/NewEngineRunner.java`): submits `.ipes` file to `gecko-rest-api`, polls completion, and exports simulation signal vectors to CSV.
3. **CSV Comparator** (`tools/parity/CompareCsv.java`): performs time-column alignment, computes max absolute and relative error per signal trace, and validates against configurable tolerances (default `rel = 1e-3`, `abs = 5e-3` accounting for legacy float32 scope storage and decimation).
4. **Orchestrator** (`tools/parity/run-parity.ps1`): runs all curated circuits (`rc-lowpass`, `rl-transient`, `rlc-series`) through both engines and comparator, generating `tools/parity/results/<date>.txt`.
5. **Maven Profile Integration**: configured opt-in `-Pparity` profile in root `pom.xml`.
6. **Engine / Solver Parity Fixes**:
   - `MatrixSolver.java`: pinned node 0 as ground reference to prevent singular KCL matrix; extended MNA formulation for `LK_LKOP2` coupled inductors; promoted current-step currents (`iCurrent`) to `iALT` on history shift.
   - `NetlistBuilder.java`: excluded CONTROL blocks from electrical netlist to prevent type-ID collision with LK components.
   - `GeckoCustomRemote.java`: synchronized map supporting null placeholders for RMI callback client registrations.
7. **Documentation**: written in `docs/architecture/PARITY_HARNESS.md`.

Acceptance: green report committed in `tools/parity/results/20260822-200129.txt`.

### P6 — Polish (acceptance gate passed) [COMPLETE]

Completed in P6:
1. **Light & Dark Theme Switcher**:
   - CSS custom property design system (`:root[data-theme='light']` and `:root[data-theme='dark']`) with clean paper-white schematic background and slate palette in light mode.
   - Navbar Sun/Moon toggle with `localStorage` persistence and automated unit tests (`App.flow.test.tsx`).
2. **CAD-Style Canvas Coordinate HUD**:
   - Live grid coordinate readout (`X: 12 Y: 8`) dynamically rendered in the bottom-left floating view controls bar on mouse hover.
3. **Theme-Aware Simulation Waveforms**:
   - Simulation drawer SVG waveform chart plot area, background, and hover crosshair tooltips styled with CSS variables to adapt dynamically to light/dark themes.
4. **Symbol & Domain Visual Parity**:
   - Full palette coverage for LK, CONTROL (15 types), and THERM component domains with color-coded SVG symbols and connection typing.

---

## 4. What we deliberately do NOT build

- No live model sync between Swing UI and web editor (files are the interface).
- No general plugin/extension system for the editor.
- No SPICE netlist import/export (possible future phase; the label-based format makes
  it feasible, but it is out of scope).
- No rewriting/moving of existing Swing code. The Swing UI is frozen (bug fixes only).
- No database, no persistence beyond the in-memory circuit store (restart = re-upload;
  files are the persistence).

---

## 5. Risks & mitigations (re-check at every phase boundary)

| Risk | Mitigation |
|---|---|
| Writer emits tokens the reader/Swing UI misinterprets | P0 round-trip tests + manual Swing open check per fixture; raw-token passthrough for unknown tokens |
| Control-domain circuits silently verify wrong | Harness detects non-LK components in the file and SKIPS them with a loud warning; P5 scope is LK-only |
| Frontend grows a second framework / state lib | Principle 9; dependency additions need approval |
| REST endpoints drift from docs again | P0/P1 update `docs/api/rest-api.md` in the same commit as behavior changes |
| Editor state machine becomes spaghetti | Single `useReducer` store with documented actions; all interaction modes (idle, ghost-armed, wiring, rubber-band) are explicit states like `SchematicEditor2.MouseMoveMode` |
| Headless pause/resume dead code confuses users | Fixed or removed in P4 |

---

## 6. Definition of done (whole feature)

1. All phases' acceptance criteria met; `mvn verify` green; `cd frontend && npm test &&
   npm run build` green.
2. A user can build a purely electrical circuit from scratch in the browser using ONLY
   the keyboard (command palette, arrows, Enter, w, r, labels), simulate it, and see
   the same numbers as the Swing UI on the same file (parity report green).
3. The same user can do everything with the mouse as well.
4. The Swing UI remains bit-for-bit untouched in behavior (no changes to
   `gecko-gui` other than, if unavoidable, bug fixes clearly marked and justified).
5. Code-size budget from principle 1 respected; if exceeded, a documented decision
   why.

---

## 7. Suggested commit sequence

```
docs(architecture): add web frontend implementation plan          [this file]
feat(core): add CircuitFileWriter with .ipes round-trip support    [P0]
feat(rest): serve rewritten .ipes and accept circuitId for simulations [P0]
test(rest): round-trip and simulate-by-circuitId integration tests [P0]
feat(rest): circuit editing CRUD + change events + undo/redo       [P1]
feat(frontend): scaffold Vite React app, API client, model types   [P2]
feat(frontend): SVG sheet, palette, ghost placement, wiring        [P2]
feat(frontend): keyboard-first layer, command palette, bindings    [P3]
feat(rest): simulation run/progress/results wiring in editor       [P4]
feat(parity): reference runner, CSV comparator, orchestrator       [P5]
docs(architecture): parity harness usage + first green report      [P5]
```
