# Cleanup TODO — Unfinished Implementations Found During Dead-Code Sweep

This file lists unfinished / abandoned / speculative implementations discovered during
the dead-code cleanup pass (see `git log` for the cleanup commit). Items are grouped by
priority. Each entry includes a recommended action.

---

## HIGH priority — real shipping gaps or likely bugs

### 1. Orphan API interfaces — `IMainWindow`, `ICircuitSheet`, `ICircuitEditor`
**Location:** `src/main/java/gecko/geckocircuits/api/{IMainWindow,ICircuitSheet,ICircuitEditor}.java`
**Status:** Confirmed unfinished
**What's there:** Three public API interfaces (Sprint 15) defining facades for
`Fenster`, `CircuitSheet`, `SchematischeEingabe2`. No `implements` clauses and zero
usages anywhere. Their siblings `ISimulationEngine`, `IScopeData`, `ISimulatorAccess`
ARE wired up. The three orphans target GUI-heavy classes that were never migrated.
**Action:** Decide — either complete the facade refactor (have the GUI classes
implement these and refactor callers) or delete them. Half-born SPI that
mis-advertises a stable API.

### 2. `scope/` package migration stalled with cross-package leak
**Location:** `src/main/java/gecko/geckocircuits/scope/` (11 classes)
**Status:** Confirmed unfinished
**What's there:** 7 classes are `@Deprecated` (`DataContainer`, `DataContainerSimple`,
`DisplayFourierWorksheet`, `GraferImplementation`, `GraferV3`, `HiLoData`, `Scopable`)
and have **zero external callers** in `main/`. The other 4 (`FourierCurveReconstruction`,
`FourierDiagram`, `FourierPlotFrame`, `DialogFourierDiagram`) are NOT deprecated, and
`newscope/DialogFourier.java:16,23` still imports `DialogFourierDiagram` and
`FourierPlotFrame` from the legacy package — the migration was left half-done with the
new package depending on the old one.
**Action:** Complete — finish migrating `DialogFourier` off the two legacy classes,
then delete the legacy `scope/` package (or move the four survivors into `newscope/`).

### 3. Signal names written but never restored on file load (likely bug)
**Location:** `src/main/java/gecko/geckocircuits/general/ProjectData.java:357`
**Status:** Confirmed unfinished (likely a real bug)
**What's there:** `exportASCII` writes `dataContainerSignals[]` (the scope signal
names from `NetzlisteCONTROL.globalData`). On import there is **no**
`tokenMap.containsToken("dataContainerSignals[]")` branch — only a TODO comment. The
modular mirror `gecko-simulation-core/.../CircuitFileParser.java:340` does read it;
the legacy path does not. Effect: scope signal names are silently dropped when
reloading a saved `.ipes` file.
**Action:** Complete — add the read branch that calls
`NetzlisteCONTROL.globalData.setSignalName(row, sigNames[row])` as the comment
suggests, or confirm signal names are restored via a different path.

### 4. `StateSpaceCalculator` — incomplete differentiation on dt change
**Location:** `src/main/java/gecko/geckocircuits/control/StateSpaceCalculator.java:162`
**Status:** Confirmed unfinished
**What's there:** `initializeWithNewDt(deltaT)` recalculates matrix A but leaves a
TODO noting that differentiation using `_stateVariables._xOLD`/`_xNEW` does not
correctly handle a step-width change. Variable-step solvers may produce wrong
output when the integrator changes dt mid-simulation.
**Action:** Investigate — confirm against test data whether this is observable;
either implement the correction or document the solver-step restriction.

---

## MEDIUM priority — speculative; needs human decision

### 5. Disabled native-library unload reflection in `NativeCBlock`
**Location:** `src/main/java/gecko/geckocircuits/nativec/NativeCBlock.java:88-96`
and mirror `src/modules/gecko-simulation-core/src/main/java/gecko/core/nativec/NativeCBlock.java:93-101`
**Status:** Confirmed unfinished
**What's there:** 9-line reflection block that called
`ClassLoader.nativeLibraries[].finalize()` to forcibly unload `.dll`/`.so` is
commented out. The method is still called from `ControlNativeC.java` at 5 sites.
Replacement just nulls fields + `System.gc()`. The reflection relied on a private
JDK field that became inaccessible on Java 9+. Effect: native libraries are not
deterministically released between simulation runs (possible file-lock / handle
leak on Windows).
**Action:** Investigate — on modern JDK, use `MethodHandles.cleaner()` / closeable
native-loader, or accept the leak and document why.

### 6. `forRemoval=true` deprecations blocked by live callers
**Location:** `src/main/java/gecko/geckocircuits/circuit/ConnectorType.java:86,116`
and `src/main/java/gecko/geckocircuits/circuit/SubCircuitTerminable.java:43`
**Status:** Confirmed unfinished (deprecation cannot be honoured yet)
**What's there:** Both methods marked `@Deprecated(since="Sprint 15", forRemoval=true)`,
but each has live callers: `AbstractBlockInterface.java:197,201` calls
`getSimulationDomain().getForeGroundColor()`/`getBackgroundColor()` (delegating to
`ConnectorType`), and `TerminalSubCircuitBlock.java:65,109` calls
`_lkTerminal.getForeGroundColor()` (delegating to `SubCircuitTerminable`). The RGB
variants exist but aren't used at these sites.
**Action:** Complete — migrate the ~3 call sites to the `*Rgb()` variants and wrap
in `new Color(...)` at the GUI boundary, then remove the deprecated methods.

### 7. `SimulationStateListener` — orphan listener SPI
**Location:** `src/main/java/gecko/geckocircuits/general/SimulationStateListener.java`
**Status:** Confirmed unfinished
**What's there:** Public 5-method interface (`onSimulationStarted/Paused/Finished/Aborted/StatusUpdate`).
Javadoc references `SimulationRunner` and `Fenster`, but neither class references it.
No `implements`, no `addSimulationStateListener` registration API anywhere.
**Action:** Decide — either wire it into `SimulationRunner` (decouples UI from
engine, stated intent is reasonable) or remove the file.

### 8. `UserParameterGUIAdapter` — orphan adapter
**Location:** `src/main/java/gecko/geckocircuits/general/UserParameterGUIAdapter.java`
**Status:** Confirmed unfinished
**What's there:** Final class implementing `UserParameterCore<T>` by delegating to
the GUI `UserParameter<T>`. Has full Javadoc + usage example, but no caller
instantiates it. Core module classes (`LossCurve`, `SwitchingLossCurve`) use
`UserParameterCoreImpl` directly instead.
**Action:** Decide — delete unless there is a near-term plan to pass GUI params
into core-module loss-calculation code.

### 9. `DialogExternalStorageConverter` — orphan SPI implementation
**Location:** `src/main/java/gecko/geckocircuits/general/DialogExternalStorageConverter.java`
**Status:** Confirmed unfinished
**What's there:** 30-line implementation of `ExternalStorageConverter` that
delegates to `DialogMakeExternal.dialogResultFabric`. Never instantiated. `GeckoFile`
instead uses its own inner `DefaultExternalStorageConverter` (reflection-based
fallback) at lines 730-744.
**Action:** Decide — either wire `DialogExternalStorageConverter` into `GeckoFile`
construction (replacing the reflection fallback, which would be cleaner), or
remove it.

### 10. `ControlOSZI` — terminal rename does not propagate to scope data
**Location:** `src/main/java/gecko/geckocircuits/control/ControlOSZI.java:42, 164-165, 185`
**Status:** Confirmed unfinished
**What's there:** Class-level "please clean up this mess" is cosmetic, but lines
164-165 and 185 are real: `setTerminalNodeLabel(newLabel, nodeIndex)` and
`initScope()` both leave `_zvDatenRAM.setSignalName(...)` as commented-out
`// TODO ???`. Renaming a scope terminal in the schematic does not rename the
trace inside the scope's data container.
**Action:** Complete — re-enable the `setSignalName` calls (verifying the data
container is non-null at the call point), or document why the rename is
intentionally ignored.

### 11. `AxisLimits` — disabled user-scale override on import
**Location:** `src/main/java/gecko/geckocircuits/newscope/AxisLimits.java:239-243`
**Status:** Speculative
**What's there:** Five commented lines in `importASCII`: when auto-scale is
disabled and a non-trivial `_userScale` was saved, the original behaviour
overrode `_valueScaleLocal = _userScale`. Disabling it means a saved "manual"
axis range is read into `_userScale` but not made visible until the user
explicitly switches scaling mode.
**Action:** Investigate — needs domain knowledge of the scope UX. Either restore
the override or document why the two-step switch is intentional.

### 12. `GeckoRemoteTest` — Javadoc says IGNORED, but tests actually run
**Location:** `src/test/java/gecko/GeckoRemoteTest.java:47, 85`
**Status:** Confirmed (misleading, not broken)
**What's there:** Both `@Test` methods carry `IGNORED:` Javadoc explaining they
require RMI infrastructure and have a `TODO: Re-enable` note. They are NOT
annotated `@Disabled`, so JUnit will execute them on every build. They actually
pass because the bodies only do reflection over `GeckoRemote`/`GeckoRemoteInterface`
method signatures, not real RMI calls.
**Action:** Document — either align the Javadoc with reality (these run; remove
the misleading IGNORED notes) or genuinely disable them with `@Disabled` if they
were meant to be skipped.

---

## LOW priority — safe to delete; minor TODOs / cosmetic

### 13. `NodeLabel` — dead class, no deserialization role
**Location:** `src/main/java/gecko/geckocircuits/circuit/NodeLabel.java`
**Status:** Confirmed dead
**What's there:** 109-line `Serializable` class for schematic text labels.
`importASCII` is a no-op and `exportASCII` body is commented out. The only non-self
reference is `CorePackageValidationTest.java:97` (a package-content whitelist).
**Action:** Delete — also remove the `"NodeLabel.java"` entry from
`CIRCUIT_GUI_CLASSES` in `CorePackageValidationTest.java:97`.

### 14. `GeneralPathWrapper.paintSymbols` — empty stub
**Location:** `src/main/java/gecko/geckocircuits/newscope/GeneralPathWrapper.java:39`
**Status:** Confirmed dead
**What's there:** `void paintSymbols(...)` body is `// TODO: implement symbol painting`
and **no caller** invokes it. Symbol painting actually happens in
`CurvePainterRegular.java:61` and `CurvePainterSignal.java:74`, which read
`_curve.getSymbol()` directly.
**Action:** Delete the method (and the TODO with it).

### 15. `ControlJavaFunction._doDebug` — commented field
**Location:** `src/main/java/gecko/geckocircuits/control/javablock/ControlJavaFunction.java:89-96`
**Status:** Confirmed dead
**What's there:** A `UserParameter<Boolean> _doDebug` builder chain wrapped in
`/* ... */`. Never referenced anywhere else in the codebase.
**Action:** Delete — dead debugging scaffold.

### 16. `SparseMatrixCalculator` "biggest mess" TODO
**Location:** `src/main/java/gecko/geckocircuits/control/calculators/SparseMatrixCalculator.java:19`
**Status:** Cosmetic (implementation is complete and was hardened)
**What's there:** 1065-line matrix-converter PWM calculator. The TODO is a
cleanliness complaint; the code works and has been safety-hardened (`safeDivide`,
`clampDutyRatio`, `sanitizeFinite`, `sanitizeDutyArray` helpers).
**Action:** Document — leave as tech-debt marker; only refactor if test coverage
exists.

### 17. `PmsmModulatorCalculator` "beautify this mess" TODO
**Location:** `src/main/java/gecko/geckocircuits/control/calculators/PmsmModulatorCalculator.java:17`
**Status:** Cosmetic
**What's there:** 142-line SVPWM modulator, complete and functional,
`@SuppressWarnings("PMD")` plus a style TODO.
**Action:** Document — same as above.

### 18. `AbstractNonLinearCircuitComponent` refactor TODO
**Location:** `src/main/java/gecko/geckocircuits/circuit/circuitcomponents/AbstractNonLinearCircuitComponent.java:46`
**Status:** Cosmetic
**What's there:** Single-line `// TODO: Future developer: This class needs
refactoring when you have time and understand the full context.` above an abstract
class with public primitive fields (justified by `@SuppressFBWarnings`). No
specific gap identified.
**Action:** Document — vague aspiration; not actionable without scope.

### 19. `DataTablePanelParameters.getCheckedData()` — stubbed to empty
**Location:** `src/main/java/gecko/geckocircuits/circuit/DataTablePanelParameters.java:137-155`
**Status:** Confirmed dead (method only)
**What's there:** Method body is fully commented out and returns
`new double[0][]`. The class itself IS used
(`DialogOptimizerParameterSettings:92` instantiates it), but `getCheckedData()`
is never called on it — callers use the sibling `DataTablePanel.getCheckedData()`
instead.
**Action:** Delete the method — leave the class.

### 20. `DialogSmallSignalAnalysis` — scattered dead comments
**Location:** `src/main/java/gecko/geckocircuits/control/DialogSmallSignalAnalysis.java:57, 119, 225-227, 232`
**Status:** Cosmetic
**What's there:** Four unrelated commented-out lines: a `//jPanelBode.add(...)`
in ctor, a `//_bodePlot._graferNew...` symbol-disabling line, an obsolete
data-loading loop superseded by the loop above it (lines 214-221), and a
`//dcs1.setContainerStatus(PAUSED)`. The dialog itself works.
**Action:** Delete the dead comments.

### 21. `ControlSlidingDFT.openDialogWindow` — duplicate commented return
**Location:** `src/main/java/gecko/geckocircuits/control/ControlSlidingDFT.java:254`
**Status:** Cosmetic
**What's there:** `//return new ControlSlidingDFTDialog(this);` immediately
followed by the live `return new ControlSlidingDFTDialog(this);` — identical,
not an alternate constructor.
**Action:** Delete the comment line.

---

## Pre-existing test failures (not caused by cleanup)

The following tests fail on the unmodified baseline and continue to fail after
cleanup. They appear unrelated to dead-code work:

- `CircuitIntegrationTest.testRCCharging_TimeConstant`
- `CircuitIntegrationTest.testRLCurrentRise_TimeConstant`
- `CircuitIntegrationTest.testResistorDivider_HalfVoltage`
- `CircuitIntegrationTest.testSolverTypes_DCConsistency`

Error: `Index 10 out of bounds for length 10` from the headless simulation
engine. Worth tracking as a separate bug.
