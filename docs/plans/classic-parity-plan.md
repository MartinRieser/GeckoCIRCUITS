# Classic GUI Parity Plan — path to retiring the Swing UI

Status: draft, 2026-09-06. Supersedes the NativeC portion of the earlier feature-review
conversation: NativeC stays and is modernized as a **load-only** external-library block.

Goal: the React desktop/web editor + headless engine cover the features people actually use,
so the classic Swing UI can be frozen and eventually removed. Hard requirement from product
owner: **NativeC blocks must work in the new stack** — use case is firmware-in-the-loop
(test real microcontroller control code in simulation before flashing the device).

---

## 1. NativeC v2 — external C library block (typ 88, load-only)

### Design principle

GeckoCIRCUITS **never compiles C**. The user builds their MCU control code with their own
toolchain (the one that also targets the device) into a host shared library; the block loads
and binds it. No system-compiler detection, no compile cache, no CI compiler dependency for
the product.

### Interface contract — the shipped header

The repo ships `gecko_c_block.h`; users `#include` it in their firmware project. This header
*is* the interface description Gecko binds against:

```c
// C and C++ both work: the boundary is a C ABI (extern "C"), the
// implementation behind these hooks may be arbitrary C++.
#ifdef __cplusplus
extern "C" {
#endif

#if defined(_WIN32)
  #define GECKO_EXPORT __declspec(dllexport)
#else
  #define GECKO_EXPORT __attribute__((visibility("default")))
#endif

// All functions optional except gecko_step. Symbols are resolved by name;
// absent optional symbols are skipped.

// Called once at simulation start (state reset / power-on equivalent).
GECKO_EXPORT void gecko_init(void);

// Called once per simulation timestep with the block inputs/outputs.
GECKO_EXPORT void gecko_step(const double* xIN, int n_in,
                             double* yOUT, int n_out,
                             double t, double dt);

// Called at simulation end (cleanup).
GECKO_EXPORT void gecko_deinit(void);

#ifdef __cplusplus
}
#endif
```

**C++ rules of the boundary** (documented in the header comments):

- Hooks must keep C linkage (`extern "C"` guard above) — mangled names cannot
  be resolved; the implementation behind them is unrestricted C++ (classes,
  templates, STL, RAII).
- Exceptions must never escape `gecko_step` — wrap the body in `try/catch`
  and write fallback outputs; an exception crossing the FFM boundary is
  undefined behavior.
- C++ static/global objects construct at library load; with the per-run copy
  that is exactly the power-on-reset semantics of the target device.
- Windows builds need the exports (`GECKO_EXPORT` handles MSVC and MinGW;
  ELF/Mach-O default visibility is usually sufficient).

- Inputs/outputs map to the block terminals like script blocks (`anzXIN` / `anzYOUT`).
- The user's C statics hold state between steps; `gecko_init` gives deterministic
  re-runs (each simulation starts from a fresh library instance — see loading).

### Loading — FFM, per-run copies

- Bind via the **FFM API** (`java.lang.foreign`, final since JDK 22): `SymbolLookup.
  libraryLookup(path, arena)` + `Linker.downcallHandle` with a `FunctionDescriptor` of
  C `double*`/`int`/`double`. No JNI headers, no javah, no native glue compiled by us.
- **Per-simulation-run copy**: at simulation start the block copies the user's library to a
  unique temp file and loads the copy (the existing `gecko.core.nativec` temp-copy trick,
  promoted from per-load to per-run). Two wins: the user's original file is never locked
  (rebuild while the app is open), and every run gets a fresh library image — C statics
  restart from zero exactly like a power-on reset on the device.
- Library path resolution: relative paths resolve against the `.ipes` file's directory,
  then the workspace root; absolute paths allowed.

### Engine wiring

- New `CLibraryCalculator` in `gecko.core.control.calculators` (sibling of
  `ScriptBlockCalculator`), registered in `ControlCalculatorBuilder` for typ 88.
- Replaces the dormant `gecko.core.nativec` copy semantics; classic GUI keeps its own
  path untouched (works today via `System.load`).
- Failure contract mirrors script blocks: missing library/symbol → outputs hold at initial
  values, one logged error, surfaced through `gecko_validate_circuit` and the web editor.

### Editor + MCP surface

- Web editor properties panel: library path (native picker on desktop), entry symbol
  (default `gecko_step`), input/output counts. Validation shows load errors inline.
- MCP may **run and validate** circuits containing C blocks; it must **not author** them
  (no library-path writing through `gecko_set_script_code` — prompt-injection into native
  code is out of scope by design).
- P2 nicety: "import header" button that scans the user's `.h` for the `gecko_step`
  signature and prefills the pin counts (tiny regex scan, optional, never required).

### Compatibility + testing

- Implementation starts by auditing classic typ-88 `.ipes` serialization and reusing it
  (no typ-88 circuit ships in the repo examples, so breakage risk is low, but old user
  circuits should still load).
- Unit tests: a committed ~2 KB fixture shared library per OS in test resources
  (`gecko_step` returning `xIN[0]*2`, etc.); tests skip gracefully (`assumeTrue`) when a
  platform binary is absent. CI builds the fixture in one line per runner (cc/cl/clang) —
  test-only, not product.
- Parity: one typ-88 circuit run through classic and headless engine in the parity
  harness; outputs must match.

### Effort: M (calculator + builder wiring + FFM loader + properties UI + tests)

---

## 2. Parity checklist (blocks classic removal)

### P0 — must have

| Item | Evidence today | Effort |
|---|---|---|
| Scope zoom/pan | `ScopeViewTab` has cursors/toggles/metrics, no wheel/drag handlers | S |
| FFT view | REST `/analysis/fourier` exists (An/Bn/Cn/Jn, harmonics, window); frontend never calls it | S |
| NativeC v2 (above) | typ 88 classic-only; core `nativec` package dormant, unwired | M |

### P1 — should have

- Thermal domain editing: catalog serves THERM already; missing property dialogs + loss/
  temperature views (loss endpoints exist).
- Loss calculation UI (endpoints exist, no frontend).
- Simulation parameter audit: REST lacks classic's precalculation window / decimation
  settings — decide per item whether to port or document away.

### P2 — ship without, document

- Subcircuits editing (parser loads them; editing deferred until a real circuit forces it).
- External data import into scopes, report/PDF export (slated for removal anyway),
  interactive controls (sliders/buttons — verify they even work in classic before deciding).

Verified non-gaps: CSV export of results, undo/redo (server-side), file open/save +
association, examples, themes.

---

## 3. Sequence

1. **P0 batch**: scope zoom/pan → FFT panel → NativeC v2 (each separately tested and merged).
2. **P1 batch**: thermal dialogs + loss view + parameter audit.
3. **Retirement checklist**: per classic feature, confirm usage/parity, then delete
   (classic UI tree, MKL/Pardiso, Batik/FOP, GraalVM JS, MATLAB RMI, legacy `scope/`
   package, gecko2octave — most removals become free here).
4. Classic stays shippable ("GeckoCIRCUITS Classic", `classic-v*` tags) until 3 completes.

Acceptance for the P0 batch: the user can (a) zoom/pan scopes, (b) open an FFT view from a
finished simulation, (c) place a typ-88 block pointing at a self-built firmware library and
get correct closed-loop behavior in the desktop app and via MCP — with state resetting on
every re-run.
