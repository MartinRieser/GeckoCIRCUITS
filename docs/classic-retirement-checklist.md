# Classic Swing UI — Retirement Checklist

Status: formal gate for step 3 of docs/plans/classic-parity-plan.md. The checklist
runs **per feature**; a feature row may only be deleted from the tree when its
"confirm usage/parity" column is signed off (user confirmation of the desktop app,
or a green parity run). Until then, classic ships frozen as "GeckoCIRCUITS Classic"
(`classic-v*` tags).

## P0 parity features (implemented)

| Feature | Parity evidence | Sign-off |
|---|---|---|
| Scope zoom/pan | desktop UI: wheel zoom, drag pan, toolbar, visible-range rescale (80c48e7c) | ☐ |
| FFT view | frontend FFT panel on /analysis/fourier: harmonics, THD, spectrum, table (fa3a98ff) | ☐ |
| NativeC C-blocks | typ-88 via FFM, load-only, per-run state reset (3cb96ec3); fixture-lib tests; MCP runs (never authors) C blocks | ☐ |

## P1 parity features (implemented)

| Feature | Parity evidence | Sign-off |
|---|---|---|
| Thermal domain editing | THERM create/patch via CircuitEditService (service test abf4a842-lineage); temperature signals render as regular scope channels | ☐ |
| Loss calculation | Losses panel on /loss endpoints: switching + conduction + total | ☐ |
| Simulation parameters | time/dt/solver/pause/resume exported. **Decided away:** classic's precalculation window (headless engine computes from t=0) and per-scope decimation (web charts decimate automatically). If a use case appears, port then. | ☐ |

## Deletion candidates (free after classic retirement)

| Item | Notes |
|---|---|
| `gecko.geckocircuits` Swing UI tree | ~1000 files; keep compiled as "Classic" until sign-offs complete |
| MATLAB RMI + memory-mapped export (`GeckoRemoteInterface`, `CallbackClientImpl`, `GeckoExternal`) | superseded by REST API + MCP |
| Intel MKL/Pardiso JNA (`com.intel.mkl`, `Paradiso`) | classic GUI solver only; new stack is pure-Java |
| GraalVM JS in classic packaging | headless typ-61 path is the pure-Java interpreter |
| Batik/FOP report+PDF export | classic-only; future exports are CSV/PNG from the web UI |
| Legacy `scope/` package | superseded by `newscope/` (verify during retirement) |
| `gecko2octave` | CSV + REST cover it |

## Keep (product features, not classic legacy)

| Item | Notes |
|---|---|
| Thermal engine domain (TH_* elements, THERM connections) | differentiator; editable via REST |
| Loss calculation engine (conduction/switching/detailed) | differentiator; exposed via REST/MCP |
| Motor models (PMSM/IM/SM/DC), LISN | real topologies need them; PMSM is MCP-first-class |
| NativeC v2 (FFM load-only C blocks) | firmware-in-the-loop use case |
| MATLAB interop via REST | MATLAB talks to the REST API/MCP instead of RMI |

## Retirement runbook (when sign-offs complete)

1. Freeze classic jar at the last `classic-v*` tag; keep the tag + release downloadable.
2. Delete the `gecko.geckocircuits` UI tree, `com.intel.mkl`, Batik/FOP deps, GraalVM
   packaging bits, MATLAB RMI files, legacy `scope/`, `gecko2octave/`.
3. Retain: engine core, io, nativec v2, loss calculators, REST API, MCP, desktop, frontend.
4. Update README feature table (drop EMI marketing, Classic references).
5. CI: retire `build-windows/macos/wsl.yml` assembly jobs and `package-desktop.yml`
   (jpackage Classic pipeline) once `classic-v*` releases stop.
