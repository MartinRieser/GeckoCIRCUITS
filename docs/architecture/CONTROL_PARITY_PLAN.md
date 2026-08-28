# Headless CONTROL-Domain Parity — Investigation & Implementation Plan

Status: PROPOSED (evidence gathered 2026-08-28)
Owner workstream referenced by `WEB_FRONTEND_PLAN.md` decision 3
("Control-domain headless parity is a separate future workstream").

Baseline: LK parity is green (`tools/parity/results/20260828-165310.txt`,
fix ee81cc5e "pin MNA island reference rows after stamping").

---

## 1. Evidence: tutorial sweep through the new engine

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

## 2. Root-cause hypotheses (to confirm per workstream)

| ID | Symptom | Suspected cause | Evidence |
|---|---|---|---|
| C1 | gate/PWM circuits blow up (ex_1: `AMP.4` = 3540 A at t=0, NaN by t=2e-5) | switch resistance before first gate evaluation / initial-condition handling in the `ControlCalculatorBuilder` gate path, or component-current extraction for probes | wrong from the very first step while node voltages at t=0 match legacy topology-wise; `HeadlessSimulationEngine.applyGateSignals` (ee81cc5e parent 5406190e) |
| C2a | opamp variants singular (3 files) | floating differential-input node of the op-amp model gets no stamp; legacy `LK_U` voltage-controlled stamp covers a case the core `StamperRegistry` lacks | `OpAmp.ipes` (CLEAN) works but Bessel/differentiator/frequency fail — they chain opamps with feedback to floating nodes |
| C2b | coupled-magnetics model singular (`Swiss_Rect_2StageInputFilter`, 66×66) | mutual inductance M-terms of `LK_LKOP2` are explicitly deferred in `MatrixSolver.buildMatrixA` | code comment "mutual coupling (M) terms are deferred"; legacy injects `zuLKOP2gehoerigeM_*` after stamping (`LKMatrices.java:198-210`) |
| C2c | script/extern blocks (JAVA_FUNCTION typ 61, ToEXT typ 22, CISPR16 typ 60, MUL typ 14...) | `ControlCalculatorBuilder` skips unsupported calculators with a WARN, downstream sources then see undefined signals → singular or wrong | WARN lines in sweep log for exactly the failing files |
| C3 | harness cannot export legacy signals for files without stored `dataContainerSignals[]` | legacy container columns only exist for names saved in the file | rc-lowpass (`dataContainerSignals[] /u_out`) exports, ex_1 (`[]`) does not |

## 3. Workstreams (strict order, each ends green: `mvn verify`, parity harness, sweep)

### W1 — Gate-driven switching (C1) — FIRST (getting-started tutorial!)

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

### W3 — Legacy signal export for arbitrary files (C3)

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

### W5 — Make the sweep a permanent gate

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
