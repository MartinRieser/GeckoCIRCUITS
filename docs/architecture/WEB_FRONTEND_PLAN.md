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

### P3 — Keyboard-first layer

**Goal:** the user's core requirement — full schematic editing without touching the mouse.
All features are additive to the mouse workflows, never replacing them.

1. **Command palette** — `Ctrl+K` opens a search box (component catalog, fuzzy
   substring match is enough — no fuzzy library), arrow keys navigate, Enter arms the
   ghost. Reuse the palette's catalog endpoint.
2. **Ghost placement by keyboard**: while a ghost is armed, arrow keys move it by one
   raster step (Shift+arrow = 5 steps), `r`/`Shift+R` rotates, Enter places, Esc
   cancels. This is the same store state as mouse ghosting — one code path.
3. **Keyboard wiring**: `Tab`/`Shift+Tab` cycles terminals of the last-placed/hovered
   component (visual highlight), Enter starts a wire at the highlighted terminal,
   arrows extend the route raster step by raster step, Enter ends (auto-connect when
   the end lands on a terminal), Esc aborts.
4. **Selection nudge**: arrow keys move selected components by one raster step
   (Shift = 5), with undo grouping (one undo entry per nudge-gesture, i.e. debounce
   500 ms like the GUI's move undo).
5. **Central keybinding map**: one `keybindings.ts` module mapping
   `key → action name → handler`. No scattered `onKeyDown` handlers. Actions: place,
   cancel, rotate, wire-toggle, delete, undo, redo, save, open, nudge, cycle-terminal,
   command-palette.
6. Standard shortcuts: Ctrl+Z/Y undo/redo (client store undo stack, backed by the P1
   server undo), Ctrl+S save, Delete delete, Ctrl+D duplicate.

Tests: reducer-level tests for every keyboard action (arm ghost, move ghost, place,
wire cycle, nudge). Keybinding map is data — test that every action has a binding.

### P4 — Simulation & results in the web UI

1. Sim panel: pick `dt`, `tEnd`, solver (model defaults from `/info`), Run →
   `POST /api/v1/simulations` with `circuitId` → progress via existing SSE
   (`/stream`) or WS topic → results table + line chart (ONE lightweight chart lib,
   propose `uplot` for size; get approval before adding).
2. Signal selection comes from `dataContainerSignals[]` in the file (same semantics as
   headless engine).
3. Implement pause/resume properly OR remove it from docs — whichever is cheaper
   (likely: implement `pause()` flag check in the headless loop, it is a 2-line loop
   condition fix plus REST endpoints; do that).
4. Tests: REST tests for the P0 circuitId submission path + pause/resume if
   implemented; frontend: chart data mapping unit test.

### P5 — Verification harness (acceptance gate)

**Goal:** prove "identical simulation results old vs. new" for electrical circuits.

1. Reference run (old engine): the Swing app already runs batch-capable via
   `-p <port>` RMI (`gecko/GeckoSim.java:233-251`, `GeckoRemoteInterface` exposes
   `simulateSteps/simulateTime/getSignalData`) and via GeckoSCRIPT
   (`AbstractGeckoCustom`). Implement a small Java CLI runner
   `tools/parity/ReferenceRunner.java` (plain main class, no framework) that:
   starts a headless-AWT GeckoSim instance (RMI mode), loads a `.ipes`, runs the
   simulation with the file's dt/tEnd, and dumps all scope signals to CSV in the
   format of the existing `DataSaver.TxtLinePrinter` (`time<sep>sig1<sep>...`).
2. New run: `POST /api/v1/simulations` with the same file →
   `POST /{id}/export` CSV (already exists).
3. Comparator: `tools/parity/CompareCsv.java` — aligns on time column, computes max
   abs & rel error per signal, pass/fail with configurable tolerance
   (default rel 1e-6, abs 1e-9; float storage on the GUI side justifies this).
   Non-zero exit on failure. ~150 lines, no dependencies.
4. Orchestrator: `tools/parity/run-parity.(sh|ps1)` — loops over a curated list of
   electrical example `.ipes` files (create `tools/parity/circuits/` with 3–5
   circuits: buck converter, 3-phase inverter, RC low-pass at minimum), runs both
   engines, compares, prints a table. Wire it as an opt-in Maven profile
   (`-Pparity`) that execs the script, so CI can run it later; do NOT hook it into
   the default build (it needs the GUI jar).
5. Document the harness in `docs/architecture/` (one page, usage + how to add circuits).

Acceptance: green report over all curated circuits; report committed as
`tools/parity/results/<date>.txt`.

### P6 — Polish (only after P0–P5 are green)

- Canvas zoom (mouse wheel) and pan (middle-drag + space-drag), HiDPI-correct.
- Dark mode (CSS custom properties only).
- Wire drawing anti-aliasing toggle.
- Component coverage: extend beyond the top-20 symbols.
- Anything else from user feedback; each item needs explicit approval.

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
