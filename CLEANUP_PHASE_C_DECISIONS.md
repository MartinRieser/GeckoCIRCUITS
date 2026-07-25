# Phase C — Decision Analysis (UNTRACKED — not committed)

Companion analysis for the six Phase C items in `CLEANUP_TODO.md`. Captures the
GUI / simulation impact and recommended action for each. This file is intentionally
gitignored — see `.gitignore` entry. Keep as a local working note.

Last updated: 2026-07-19

---

## Quick-reference impact matrix

| #  | Item                                  | GUI effect                         | Sim result                            | Recommendation                                  |
|----|---------------------------------------|------------------------------------|---------------------------------------|-------------------------------------------------|
| 5  | `NativeCBlock` unload reflection      | None                               | **Possible Windows .dll lock**        | **Done** — Option D (copy-on-load) implemented  |
| 7  | `SimulationStateListener` orphan SPI  | None                               | None                                  | **Done** — decoupled SimulationRunner from GUI  |
| 8  | `UserParameterGUIAdapter` orphan      | None                               | None                                  | **Done** (Deleted)                              |
| 9  | `DialogExternalStorageConverter`      | None (reflection fallback works)   | None                                  | **Done** (Deleted)                              |
| 10 | `ControlOSZI.setTerminalNodeLabel`    | None — **zero callers**            | None                                  | **Done** (Deleted)                              |
| 11 | `AxisLimits` user-scale on import     | **YES — saved manual range lost**  | None                                  | **Done** — restored + test                      |

---

## #5 — NativeCBlock unload reflection  [PRIORITY — cross-platform fix wanted]

**Location:**
- `src/main/java/gecko/geckocircuits/nativec/NativeCBlock.java:88-96`
- Mirror: `src/modules/gecko-simulation-core/src/main/java/gecko/core/nativec/NativeCBlock.java:93-101`

**Status:** 9-line reflection block commented out. Method called from
`ControlNativeC.java` at 5 sites (between simulation runs / cleanup) and from
`NativeCTest.java`.

**Original mechanism:** reflected into the private `ClassLoader.nativeLibraries`
vector, called `finalize()` on each loaded library to forcibly unload the .dll/.so.
Broke on Java 9+ because the field became inaccessible.

**Current behaviour:** nulls the wrapper fields + `System.gc()`. Native library
handles are released non-deterministically by GC.

**Impact:**
- GUI: none.
- Simulation results: none (numerical correctness unaffected).
- Cross-platform: **Windows** may show ".dll in use" if user recompiles a C block
  and reloads without restarting the JVM. Linux/macOS unaffected (mapped files
  can be replaced at any time).

**Requirement from user:** must work on Windows, macOS, Linux.

### Investigation results — `feature/LLM_development_experiments` (2026-07-19)

**Verdict: NO real solution was implemented in any branch.**

What the experiments branch did:
- Moved `NativeCBlock.java` (and the whole nativec package) to a new namespace
  `ch.technokrat.gecko.geckocircuits.nativec`. (This rename was later REVERTED
  by commit `3c5838e8` on `feature/fix-jni-and-spotbugs-for-gecko2026`.)
- **Deleted the 9-line commented reflection block** (cosmetic cleanup).
- `unloadLibraries()` body unchanged: nulls fields + `System.gc()`.

What `feature/fix-jni-and-spotbugs-for-gecko2026` did (commit `71ac8ddd`,
"Fix NativeCTest package path, restore System.gc() to unload native library,
and recompile Windows DLLs with updated JNI symbols"):
- Tried to remove `System.gc()` (probably for PMD compliance).
- **Put it back** because native libraries weren't being released on Windows
  without it. This confirms `System.gc()` is load-bearing in production.

No branch contains a modern replacement (`Cleaner`, `privateLookupIn`,
`dlclose()` via JNI, copy-on-load, etc.).

### Options (all cross-platform)

| Option | Effort | Determinism | Notes |
|---|---|---|---|
| **A. Document** | 5 min | Same as today | Delete dead comment block; add explanatory comment citing commit 71ac8ddd |
| **B. `java.lang.ref.Cleaner`** | ~2 h | Medium | Register a phantom cleanup action per NativeCBlock. Still relies on GC. |
| **C. Explicit `dlclose()` / `FreeLibrary()` via JNI** | ~1 day | High | Add a tiny JNI method to NativeCWrapper that calls the OS unload. Requires per-platform compilation. |
| **D. Copy-on-load** | ~30 min | High (for the source file) | On `loadLibraries(name)`: copy `foo.dll` to `foo-{ts}.dll` and load that. Original is never locked. Pure Java. |

**Recommendation:** Start with **A** (just delete the dead comment, document why
`System.gc()` is kept). The current code IS cross-platform — it just relies on
JVM GC timing. If you hit real Windows .dll-lock issues in production, escalate
to **D** (simplest reliable fix) or **C** (most correct, most work).

Do NOT pick B unless you specifically want phantom-reachability semantics; it
doesn't actually unload the lib any faster than the current approach.

### Decision

**Implemented Option D (copy-on-load)** in commit [7114bd53](file:///c:/Users/mhr/Documents/GeckoCIRCUITS) (branch `cleanup/05-nativecblock-copy-on-load`).
The JNI wrapper now copies the native library to a temporary location before loading it, which prevents the source file from being locked by the OS and allows users to rebuild/replace native C blocks dynamically.

---

## #7 — SimulationStateListener orphan SPI  → WIRE-UP (Decoupled GUI)

**File:** `src/main/java/gecko/geckocircuits/general/SimulationStateListener.java`
(58 lines, public 5-method interface)

**Verified:** zero `implements`, zero callers, zero registration API. Javadoc
references SimulationRunner and Fenster — neither references it back.

**Decision:** WIRE-UP. We implemented the listener pattern to completely decouple `SimulationRunner` from Swing GUI components, allowing headless execution of simulations. (Completed in commit [4e089753](file:///c:/Users/mhr/Documents/GeckoCIRCUITS) on branch `cleanup/07-simulationstatelistener-wire-up`).

---

## #8 — UserParameterGUIAdapter orphan  → DELETE

**File:** `src/main/java/gecko/geckocircuits/general/UserParameterGUIAdapter.java`
(110 lines, full adapter with javadoc + usage example)

**Verified:** zero instantiations. Core classes use `UserParameterCoreImpl`
directly.

**Decision:** DELETE. Adapter without a client. (Completed in commit [1709c237](file:///c:/Users/mhr/Documents/GeckoCIRCUITS) on branch `cleanup/08-userparameterguiadapter-orphan`).

---

## #9 — DialogExternalStorageConverter orphan  → DELETE

**File:** `src/main/java/gecko/geckocircuits/general/DialogExternalStorageConverter.java`
(30 lines)

**Verified:** never instantiated. The production code path is
`GeckoFile.DefaultExternalStorageConverter` (in gecko-simulation-core at
`GeckoFile.java:732-744`) which calls `DialogMakeExternal.dialogResultFabric`
**via reflection** at runtime. Same dialog runs, just via reflection.

**Decision:** DELETE the orphan class. Status quo (reflection) is unchanged. (Completed in commit [b8f6b0e5](file:///c:/Users/mhr/Documents/GeckoCIRCUITS) on branch `cleanup/09-dialogexternalstorageconverter-orphan`).
Wire-up is a separate refactor that doesn't change runtime behaviour.

---

## #10 — ControlOSZI terminal rename (claimed bug)  → DELETE

**What the TODO claimed:** "latent bug — renaming a scope terminal does not
propagate to scope data".

**What I found:** `setTerminalNodeLabel` is defined in 3 classes
(`ControlOSZI:184`, `ControlSpaceVector:58`, `ControlU_ZI:67`) but **never
called** from anywhere in main or test source. The "bug" doesn't manifest
because nothing triggers it. Duplicate `TODO ???` comments at ControlOSZI:164-165
are copy-paste noise.

**GUI effect:** none observable (no caller).
**Simulation effect:** none.

**Decision:** DELETE. We deleted the 3 orphan `setTerminalNodeLabel` methods and their unused fields / duplicate TODO comments. (Completed in commit [00caf948](file:///c:/Users/mhr/Documents/GeckoCIRCUITS) on branch `cleanup/10-controloszi-orphan-methods`).

If a future feature (e.g. "rename terminal" GUI action) needs this, redesign
properly then — store label, apply via `setSignalName` at `initScope` time when
`_zvDatenRAM` is guaranteed non-null.

---

## #11 — AxisLimits user-scale override on import  → RESTORE + TEST

**Location:** `src/main/java/gecko/geckocircuits/newscope/AxisLimits.java:239-243`

**Status:** 5 commented lines that used to copy `_userScale` into
`_valueScaleLocal` when reloading with `_isAutoEnabled=false`.

**Cross-check:** the interactive path `setAutoEnabled(false)` (lines 73-83)
does exactly this copy. The import path skips the same step — clearly an
oversight.

**GUI effect (real):** When user saves a scope with manual axis limits and
reloads the file, the scope initially shows default/cached limits, NOT the
saved manual range. User must toggle autoscale off→on→off to see their saved
range.

**Simulation effect:** none (pure display).

**Decision:** RESTORE. We restored the 5 lines to correctly copy `_userScale` to `_valueScaleLocal` on import when auto-scale is disabled. Added a regression test `testImportASCII_RestoresUserScaleWhenAutoDisabled`. (Completed in commits [3f1c089e](file:///c:/Users/mhr/Documents/GeckoCIRCUITS) (test) and [09213753](file:///c:/Users/mhr/Documents/GeckoCIRCUITS) (fix) on branch `cleanup/11-axislimits-user-scale-import`).

---

## Recommended execution order

1. **Safe-delete batch** (one branch each, Phase A workflow):
   - #7 SimulationStateListener
   - #8 UserParameterGUIAdapter
   - #9 DialogExternalStorageConverter
   - #10 ControlOSZI + ControlSpaceVector + ControlU_ZI orphan methods + TODOs
2. **Restore with test:**
   - #11 AxisLimits (5-line restore + regression test)
3. **Native-library fix (after reviewing experiments branch):**
   - #5 NativeCBlock — design cross-platform solution
