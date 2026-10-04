---
name: example-verification
description: Runbook for creating, modifying, or debugging the bundled circuit examples in frontend/src/model/examples.ts. Use when adding a new example, fixing a reported example bug, or when any change touches engine semantics (sources, switches, scopes, control blocks) that examples rely on. Covers the mandatory headless simulation gate, physics plausibility assertions, and the GUI smoke test.
---

# Example-Circuit Verification

Bundled examples are the first thing users open. They break in ways unit tests
don't see: a scope channel name that no probe produces (flat-zero trace), an AC
example authored with a DC source code, a simulation window that ends before
the transient settles. This skill is the protocol that catches those before a
human does.

## The gate

```bash
# backend must be running (start it if not):
mvn -f pom.xml -pl backend/gecko-rest-api -am package -DskipTests
java -jar backend/gecko-rest-api/target/gecko-rest-api.jar

# all examples, structural + plausibility assertions:
python scripts/verify-examples.py

# single example while iterating:
python scripts/verify-examples.py --only pfc-boost
```

Exit 0 = gate green. The script extracts every example from
`frontend/src/model/examples.ts`, runs it headlessly through
`POST /gecko/api/v1/simulations`, and asserts:

1. status COMPLETED, every `dataContainerSignals[]` entry present in results,
2. time span reaches the file's `tDURATION`,
3. signals finite; constant **zero** is flagged (dangling tap), constant
   nonzero is legal (DC step input),
4. per-example physics windows from the `ASSERTIONS` table in the script.

## Authoring or fixing an example

The `.ipes` content lives in template literals in `examples.ts`. When editing
by hand, splice with a small Python script (regex the `export const X_IPES`
block, escape `\` → `\\`), never edit hundreds of escaped lines inline — then
round-trip-verify the literal reproduces the intended file byte-for-byte.

Checklist, in order:

1. **Power-stage physics**: compute the expected steady state BEFORE trusting
   the sim. Buck: V·D. Boost / buck-boost: V/(1−D) / −V·D/(1−D). Rectifier bus:
   Vpk − 2·Vf. RC/RLC: τ = RC / √(LC), ensure tDURATION ≥ 5τ. PFC: ripple
   2·P/(ω·C·V), PF = P/(Vrms·Irms).
2. **Signal-name coupling** (the #1 breakage): every scope input label,
   `dataContainerSignals[]` entry, and `savedSignalNames[]` entry must exactly
   match a producer's `labelEndKnoten[]` (probe or script output). Mismatch =
   constant-zero channel. Diagnose by requesting extra signals in a debug run:
   `signals: ["node_x", "pwm", ...]` — node voltages and all taps are always
   recordable.
3. **Geometry**: control wiring is resolved by exact grid points (union-find
   over wire geometry, engine) AND by centered multi-pin layout (frontend,
   step 2). Script with N inputs has pins at (x−2, y−(i−(N−1))); a scope with
   C channels likewise. Two wires may share at most ONE raster cell unless
   identical. Probe name labels need ≥4 grid rows from badges below them.
   `frontend/test/examples.connectivity.test.ts` encodes all of this — run it.
4. **tDURATION / dt**: simulate long enough for steady state (or state clearly
   it shows a transient). Boost-style converters overshoot at startup; a
   window ending mid-overshoot lies to the user.
5. **Add the assertion entry** to `ASSERTIONS` in `scripts/verify-examples.py`
   with theory-derived windows (±20% generous), not observed values.
6. **Run the gate** until green, then `npx vitest run` + `npx tsc --noEmit`
   in `frontend/`.
7. **GUI smoke test** (browser-use): open http://localhost:5173, Examples →
   the example, F5. Confirm the scope tab appears with the promised channels
   and plausible shapes. Prefer `dom_cua.get_visible_dom()` refs /
   `cua.click` coordinates for the palette/menu (role-based clicks on the
   canvas app time out); read measurement chips via `evaluate` on
   `.sim-drawer` for min/max/mean/Vpp cross-checks.

## Plausibility analysis patterns (for debugging a suspicious example)

Run the example's extracted `.ipes` directly:

```python
body = open("debug.ipes", "rb").read()
# POST /gecko/api/v1/simulations {"base64Circuit": ..., "signals": [...]}
```

Then compute over the steady-state tail (skip startup):
- mean/min/max/peak-to-peak per window of one grid cycle;
- power balance P_in = mean(|v_grid|·i) vs P_out = V²/R → efficiency (expect
  85–99 % with diode drops, flag anything > 100 % or < 50 %);
- power factor P/(Vrms·Irms) for PFC-style examples;
- correlation of current peaks with voltage peaks (sample at |v| > 0.95·Vpk).

## Known bug classes this gate exists for

| Symptom | Root cause found in the wild |
|---|---|
| Scope channel flat at 0 | probe outputs `v_meas`, scope records `v_out_scope` (closed-loop-buck, fixed 2026-10) |
| "AC" example shows DC | source parameter[0]=401 (DC) instead of 402 (SIN) (rectifier, fixed 2026-10) |
| Scope shows wrong voltage | tDURATION ends inside the startup overshoot (boost at 2 ms → 31–42 V, fixed by 12 ms) |
| Scope shows violent 100 Hz limit cycle | control script with proportional term on ripple-hungry feedback and no current loop (pfc-boost, rebuilt 2026-10) |

## Committing

Example fixes follow the repo style `fix(examples): ...` / `feat(examples): ...`,
one logical example per commit when practical, and list the verification
performed (gate output summary, test counts, GUI check) in the body.
