# Headless CONTROL-Domain Parity — Investigation & Implementation Plan

Status: HANDOFF (2026-08-30) — read §0 "State for handoff" first; task **T1**
is the active open item with all findings collected.
Owner workstream referenced by `WEB_FRONTEND_PLAN.md` decision 3
("Control-domain headless parity is a separate future workstream").

## 0. State for handoff

### 0.1 What is done and committed

Commits on `feature/web-frontend` (latest first):
- `3b0667a3` classic-faithful switching simulations + extended parity harness
- `e17c622c` this plan + tutorial sweep evidence
- `ee81cc5e` MNA ground-pin fix (restores rc/rl/rlc parity)

Green: `mvn verify` (1837 core + 296 REST tests), parity harness
`tools/parity/run-parity.ps1` → 4/10 PASS: `rc-lowpass`, `rl-transient`,
`rlc-series`, `buck_simple` (the latter = gate-driven switching works).

Implemented in the headless engine (full change list in commit 3b0667a3):
classic wire connectivity
(endpoint-junction rule), semiconductor state machine (LK_D/THYR/IGBT) with
bounded re-solve, legacy parameter slots, initial conditions (L current /
C voltage from `params[1]`), probes named after `labelEndKnoten`, signal taps,
writer format fixes (`<Verbindung>` tags, no `null` arrays).

Parity harness: `run-parity.ps1` runs 10 circuits through BOTH engines and
compares. `ReferenceRunner <guiJar> <ipes> <outCsv> <signals> [port] [labels]
[tEnd]` drives the classic GUI via RMI; `NewEngineRunner <baseUrl> <ipes>
<outCsv> [signals] [tEnd] [legacy]` drives the REST API (the `legacy` marker
selects the new backend below). `CompareCsv <ref> <new> relTol absTol
[skipFirstRow]` — the classic engine's row 0 comes from its own init
convention, hence `skipFirstRow=true` in the orchestrator.

### 0.2 The legacy RMI backend — implemented, ONE open blocker (task T1)

**Goal:** the web GUI gets legacy-correct results by driving the REAL classic
engine (`gecko-gui`'s `gecko.GeckoSim`) headlessly via its RMI remote control —
the exact path `tools/parity/ReferenceRunner.java` proves works for all
tutorials — as an opt-in backend of `gecko-rest-api`.

**Already implemented (uncommitted working tree — commit as-is before
starting T1):**
- `gecko.rest.service.LegacySimulationBackend` — launches
  `java -cp <guiJar> gecko.GeckoSim <tempFile> -p <freePort>`, waits for the
  RMI registry, labels unnamed VOLT/AMP blocks via `setOutputNodeName`,
  `initSimulation(dt,tEnd)` → `runSimulation()`, exports
  `getTimeArray/getSignalData` into a `SimulationResult`
  (DataContainerGlobal), destroys the process in `finally`, `cancelActive()`.
  Reflective invocation (no compile-time GUI dependency — the REST enforcer
  bans Swing/AWT): the RMI stub is looked up with a `URLClassLoader` over the
  GUI jar as thread-context classloader (`lookupRemote`), because
  `registry.lookup` needs the `gecko.GeckoRemoteInterface` class loadable
  client-side. Methods used: `connect/disconnect/initSimulation/runSimulation/
  getSimulationTime/getTimeArray/getSignalData/setOutputNodeName/shutdown`.
- `SimulationService`: `backend:"legacy"` in `SimulationRequest` selects the
  backend; `resolveLegacyCircuitBytes` provides FAITHFUL original bytes
  (circuitId store keeps `originalContent` now — `CircuitFileService.
  getOriginalBytes`; base64 decodes; server-local path is read). The classic
  GUI cannot open `CircuitFileWriter` rewrites (scope `<detail>` blocks are
  not round-tripped) — never feed it rewritten bytes.
- `NewEngineRunner` accepts a `legacy` marker argument (sets
  `"backend":"legacy"` in the JSON).
- Config: `gecko.legacy.gui-jar` (empty = auto-locate the fat jar),
  `gecko.legacy.java-executable` (default `java`) in `application.properties`.
- Unit tests: `LegacySimulationBackendTest` (export-name derivation).

**Verified working:** GUI jar auto-located, GeckoSim process starts and
becomes "ready" (its log lands in `$TEMP/gecko-legacy.log`), RMI lookup
succeeds, `connect`/`initSimulation` return.

**T1 — THE open blocker:** the `runSimulation()` RMI call never returns when
driven from the REST server JVM. Timeline observed: process "ready" → sim
stays RUNNING forever → REST runner times out (180 s) → process killed by
backend cleanup. NOT the cause (all ruled out): progress poller (removed —
still hangs), measurement labeling (ex_1 labels nothing), init overload
resolution, port handling, file bytes (the identical file via
`ReferenceRunner` completes in seconds with the identical call sequence
connect → initSimulation → runSimulation).

Prime difference vs `ReferenceRunner`: the REST server JVM is a Spring Boot
fat jar (LaunchedURLClassLoader) and the RMI stub comes from the
`URLClassLoader` trick. RMI *dispatch* works (connect/init return) — only the
long-running `runSimulation` call hangs. Next diagnostic steps, in order:
1. Reproduce with BOTH JVMs kept alive, then `jstack` the GeckoSim process
   (is the simulation loop running? blocked on the AWT EventQueue?) and the
   REST server thread (blocked in socket read = server never sent the reply,
   or in unmarshal = reply lost/class resolution failure). Earlier attempts
   failed only because the processes had been killed before dumping — keep
   them alive this time (do NOT run the 180 s timeout runner; submit with
   `curl` and inspect while RUNNING).
2. Try invoking `runSimulation` asynchronously from the classic side: the
   interface has `simulateSteps(int)` / `simulateTime(double)` — drive the
   simulation in slices from the REST side (e.g. `simulateTime(tEnd/10)`
   in a loop, polling `getSimulationTime()` between slices). This avoids
   one long-blocking RMI call entirely and yields progress for free. If
   `runSimulation` specifically hangs but `simulateTime` returns, the
   slicing loop IS the fix — keep it.
3. If RMI-from-Spring-Boot remains cursed, fall back to the helper-JVM
   pattern: spawn a tiny driver JVM (GUI jar on ITS classpath — reuse
   `ReferenceRunner` minus the CSV export, plus a `signals` argument) that
   writes the exported CSV to a file the backend passes in; the backend
   reads the CSV into a `SimulationResult` on process exit. ~100 lines,
   reuses proven code, zero client-side RMI.

**Acceptance for T1:** `NewEngineRunner <url> resources/tutorials/1xx_
getting_started/101_first_simulation/ex_1.ipes <csv> "u_out,u_R,i_L,i_s,
i_d,i_C" legacy` completes; `CompareCsv` against a fresh `ReferenceRunner`
export of the same file reports PASS for all common signals (identical
engine ⇒ identical numbers). Then repeat with `buck_simple`.

**After T1:** update `docs/api/rest-api.md` with the `backend` request field,
wire the web GUI sim drawer with a backend selector (optional), commit.

### 0.3 Remaining headless-engine work (only if the legacy backend route is
rejected by the product owner)

1. **DC operating point init** (`ex_1`, `ex_3_pwm`, `singlePhase_PWM_converter`):
   the classic GUI restarts at the operating point saved in the file
   (L `params[1]` initial current — works; C `params[1]` initial voltage on
   non-grounded capacitors and full node-potential initialization — needs a
   proper DC operating point solve, not per-element seeding).
2. **Unsupported control blocks** (`boostPFC`, `thyristor_RL_3phBridge`,
   `three-phase_VSR_250kW`): PI/MUL/comparator chains are skipped by
   `ControlCalculatorBuilder`, so their gate drives never fire. W4 fail-fast
   (clear per-block error instead of silent zero) is NOT yet implemented.
3. W2 items (op-amp hidden subcircuit, LKOP2 mutual M-terms) as detailed in
   §3 W2 below.

### Engineering rules (binding, from `WEB_FRONTEND_PLAN.md` §0)

`mvn verify` green after every task; conventional commits
(`feat(rest): ...`); no speculative abstraction; update
`docs/api/rest-api.md` in the same commit when REST behavior changes; never
commit secrets. When debugging long-running scenarios: kill order matters —
the Spring Boot repackage fails if the server jar file is locked by a running
server (stop the server BEFORE `mvn package`).

---

Baseline: LK parity is green (`tools/parity/results/20260828-165310.txt`,
fix ee81cc5e "pin MNA island reference rows after stamping").

---

## 1. Evidence: tutorial sweep through the new engine (2026-08-28, historical)

All 76 `.ipes` files under `resources/tutorials` were run through the
headless engine (`tools/parity/TutorialSweep.java`, duration capped,
result classified). Outcome:

| Class | Count | Files (representative) |
|---|---|---|
| CLEAN (load, run, all signals finite) | 62 | all thyristor tutorials, buck/boost/SEPIC/Ćuk, PFC, Vienna rectifier 250 kW, matrix converters, EMI filters, thermal |
| SIM FAILED — singular matrix | 9 | `204_analog_circuits/opamp_3rdOrderBessel`, `opamp_frequency`, `opamp_invertingDifferentiator`, `703_simulink_cosimulation/vr1_simulink`, `704_java_blocks/JavaBlockPMSM`, `802_motor_drives_pmsm/*` (2), `803_optimization/.../dmNoise`, `.../Swiss_Rect_2StageInputFilter` |
| SIM OK — non-finite signals | 5 | `101_first_simulation/ex_1`, `103_pwm_basics/ex_3_pwm`, `401_single_phase_inverter/singlePhase_PWM_converter`, `702_matlab_integration/GeckoSCRIPT_ example_matlab`, `801_matrix_converters/..._junction_temperature` |

Full list: `tools/parity/results/20260828-tutorial-sweep.txt`.

Verified: the 9 singular failures behave identically with the solver from
commit 51401230 (pre pin-fix) — they are pre-existing gaps, not regressions.

Secondary observation: `ex_1.ipes` also exposes a harness gap — the file
stores no `dataContainerSignals[]`, so the legacy RMI path records nothing
and `run-parity.ps1` cannot compare it even though the legacy GUI simulates
it correctly.

## 2. Root-cause hypotheses as of 2026-08-28 (C1/C3 resolved since; C2a/C2b/C2c still open)

| ID | Symptom | Suspected cause | Evidence |
|---|---|---|---|
| C1 | gate/PWM circuits blow up (ex_1: `AMP.4` = 3540 A at t=0, NaN by t=2e-5) | switch resistance before first gate evaluation / initial-condition handling in the `ControlCalculatorBuilder` gate path, or component-current extraction for probes | wrong from the very first step while node voltages at t=0 match legacy topology-wise; `HeadlessSimulationEngine.applyGateSignals` (ee81cc5e parent 5406190e) |
| C2a | opamp variants singular (3 files) | floating differential-input node of the op-amp model gets no stamp; legacy `LK_U` voltage-controlled stamp covers a case the core `StamperRegistry` lacks | `OpAmp.ipes` (CLEAN) works but Bessel/differentiator/frequency fail — they chain opamps with feedback to floating nodes |
| C2b | coupled-magnetics model singular (`Swiss_Rect_2StageInputFilter`, 66×66) | mutual inductance M-terms of `LK_LKOP2` are explicitly deferred in `MatrixSolver.buildMatrixA` | code comment "mutual coupling (M) terms are deferred"; legacy injects `zuLKOP2gehoerigeM_*` after stamping (`LKMatrices.java:198-210`) |
| C2c | script/extern blocks (JAVA_FUNCTION typ 61, ToEXT typ 22, CISPR16 typ 60, MUL typ 14...) | `ControlCalculatorBuilder` skips unsupported calculators with a WARN, downstream sources then see undefined signals → singular or wrong | WARN lines in sweep log for exactly the failing files |
| C3 | harness cannot export legacy signals for files without stored `dataContainerSignals[]` | legacy container columns only exist for names saved in the file | rc-lowpass (`dataContainerSignals[] /u_out`) exports, ex_1 (`[]`) does not |

## 3. Original workstreams (W1/W3/W5 substantially done — see §0.1; W2/W4 still open)

### W1 — Gate-driven switching (C1) — DONE, commit 3b0667a3

1. Minimal repros, one per phenomenon, added to core tests:
   U–S(gate)–R; U–S(gate)–L–C–R (ex_1 reduced); D+L freewheeling.
   Expected values computed analytically (switch on/off steady state).
2. Instrument `HeadlessSimulationEngine` first two steps: log switch
   resistance actually stamped, gate signal value, probe currents.
   Compare against legacy by instrumenting `LKMatrices` equivalently
   (temporary debug build, not committed).
3. Fix whatever diverges (likely: evaluate CONTROL calculators once before
   the first `buildMatrixA` so `applyGateSignals` never sees the initial
   NaN/default, and/or clamp switch resistance during step 0).
4. Acceptance: ex_1, ex_3_pwm, singlePhase_PWM_converter simulate with all
   signals finite AND numerically within parity tolerance vs legacy
   (requires W3 for export) — interim gate: finite + physically plausible
   (LC resonance envelope bounded).

### W2 — Singular matrices (C2a, C2b)

1. Port the missing voltage-controlled-source stamp case(s) from
   `LKMatrices.schreibeMatrix_A` (op-amp / VCVS variants incl.
   `params[12..14]` direct-control b-vector terms) into core stampers.
2. Implement `LK_LKOP2` mutual coupling M-terms in `MatrixSolver`
   (port `zuLKOP2gehoerigeM_spgQnr/_kWerte` injection, both A and B).
3. Acceptance: the 4 files (3 opamp + Swiss_Rect_2StageInputFilter)
   simulate finite; add them to `ClassicCompatibilityTest`; parity vs
   legacy on opamp_frequency + Swiss_Rect_2StageInputFilter.

### W3 — Legacy signal export for arbitrary files (C3) — DONE differently, see §0.2

1. Extend the harness: before feeding the legacy GUI, inject the desired
   signal names into `dataContainerSignals[]` using the existing core
   `CircuitFileWriter` (P0) — validate that the legacy GUI then records
   those columns (rc-lowpass semantics; check `ProjectData` read path).
2. Acceptance: `ReferenceRunner` exports ex_1 node labels (`in,out,ur`).

### W4 — Script/extern blocks (C2c) — decide per block

JAVA_FUNCTION, ToEXT/FROMEXT (Simulink), CISPR16, script blocks:
either implement the calculator in core (pure-Java ones first: MUL typ 14
is trivial) or make the engine fail FAST with a clear message naming the
unsupported block (surfaced through REST to the web GUI status bar) instead
of "matrix singular". No silent skipping.
Acceptance: every tutorial file either simulates finite or reports a
precise unsupported-block error; zero "Matrix ... is singular" surprises.

### W5 — Make the sweep a permanent gate — partially done (harness table; JUnit ratchet still open)

1. Commit `tools/parity/TutorialSweep.java` + a `-Psweep` orchestrator
   script; integrate the sweep as a JUnit-tagged test with an explicit
   expected-failure allowlist (auto-fails when an allowlisted file starts
   passing — shrinks the list ratchet-style).
2. Extend `run-parity.ps1` circuit table with the curated set:
   ex_1, ex_3_pwm, singlePhase_PWM_converter, opamp_frequency,
   Swiss_Rect_2StageInputFilter, buck_simple, boostPFC,
   three-phase_VSR_simpleControl_250kW, thyristor_RL_3phBridge
   (legacy comparison where W3 allows).

## 4. Definition of done

1. Sweep: 76/76 tutorials either CLEAN or precise unsupported-block error.
2. Parity harness green on the curated table above (LK + gate-driven).
3. `docs/api/rest-api.md` + web GUI list remaining known-unsupported
   blocks explicitly.
4. `mvn verify` green; no new code-size outliers (principle 1 of
   WEB_FRONTEND_PLAN).
