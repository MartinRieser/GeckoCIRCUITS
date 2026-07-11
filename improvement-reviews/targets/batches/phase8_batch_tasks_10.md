### [InitDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/InitDialog.java)
- Add class-level Javadoc explaining i18n initialization dialog

### [InitParameters.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/InitParameters.java)
- Add class-level Javadoc explaining initialization parameter constants

### [InitializableAtSimulationStart.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/InitializableAtSimulationStart.java)
- Add Javadoc on interface explaining simulation-start initialization
- Add `@param deltaT` Javadoc on `initializeAtSimulationStart`

### [IntegerMatrixCache.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/IntegerMatrixCache.java)
- Add class-level Javadoc explaining integer matrix cache

### [IntegratorCalculation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/IntegratorCalculation.java)
- Add class-level Javadoc explaining numerical integrator with trapezoidal rule
- Add Javadoc on all fields and two modes (normal vs reset)

### [InterfaceNativeCWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/InterfaceNativeCWrapper.java)
- **Add Javadoc to all three interface methods.** `loadLibrary`, `initParameters`, and `calcOutputs` have no method-level Javadoc
- **Fix terminology in class Javadoc:** "are intended to be overwritten by native functions" -- in Java, interface methods are *implemented*, not *overwritten*
- **Add `@param xOUTVector` documentation** to `calcOutputs` (line 31). Also document `numberOfOuts`, `time`, and `deltaT`

### [InvisibleEdit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/InvisibleEdit.java)
- Add class-level Javadoc explaining undoable edits that don't appear in the undo menu
- Document the "invisible" concept

### [IpesFileable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/IpesFileable.java)
- Add class-level Javadoc explaining what "Ipes" is/was
- Add Javadoc to `exportAscii()`
- Document why this is package-private

### [IsDtChangeSensitive.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/IsDtChangeSensitive.java)
- Add Javadoc on `initWithNewDt()` explaining when it is called
- Remove stale template comment

### [JLabelRot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/JLabelRot.java)
- Add class-level Javadoc explaining rotated JLabel

### [JPanelAxisSettings2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/JPanelAxisSettings2.java)
- Add class-level Javadoc explaining axis configuration panel UI

### [JPanelDialogRange.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/JPanelDialogRange.java)
- Add class-level Javadoc explaining time range selection dialog

### [JPanelFourier.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/JPanelFourier.java)
- Add class-level Javadoc explaining Fourier analysis panel UI
- Add `@Deprecated` Javadoc if deprecated

### [JPanelGridSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/JPanelGridSettings.java)
- Add class-level Javadoc (~500+ lines) explaining grid configuration panel UI

### [JPanelLineProperties.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/JPanelLineProperties.java)
- Add class-level Javadoc explaining line properties panel

### [JPanelLossDataInterpolationSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/JPanelLossDataInterpolationSettings.java)
- Add class-level Javadoc explaining settings panel for test/interpolation curves
- Add Javadoc to constructor and builder methods
- Add Javadoc to `setVoltageSelectionVisible()`

### [JPanelSemiconductorDetailButtons.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/JPanelSemiconductorDetailButtons.java)
- Add Javadoc on class explaining loss detail button panel

### [JPanelSymbProps.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/JPanelSymbProps.java)
- Add class-level Javadoc explaining symbol properties panel

### [JTextAreaWriter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/JTextAreaWriter.java)
- Add class-level Javadoc explaining Writer implementation for JTextArea
- Document thread-safety

### [JavaBlockClassLoader.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/JavaBlockClassLoader.java)
- Add class-level Javadoc explaining custom class loader for compiled Java blocks

### [JavaBlockMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/JavaBlockMatrix.java)
- Add class-level Javadoc explaining matrix support for Java blocks

### [JavaBlockSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/JavaBlockSource.java)
- Add class-level Javadoc explaining source code container for Java blocks

### [JavaBlockVector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/JavaBlockVector.java)
- Add class-level Javadoc explaining vector support for Java blocks

### [JavaMemoryRestart.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/JavaMemoryRestart.java)
- Add Javadoc to `isMemoryRestartRequired(int)` documenting `userMemorySize` parameter
- Add Javadoc to `searchForReadyString()` and `createJVMCallCommands()`

### [JavaScriptTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/JavaScriptTest.java)
- Add class-level Javadoc explaining the test/demo purpose

### [LAPACK.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/com/intel/mkl/LAPACK.java)
- Remove the stale IDE template comment on lines 2-4 ("To change this template, choose Tools | Templates...")
- Fix incorrect Javadoc on `zgetrs()` that says "Wrapper for MKL function dgetrf()" instead of zgetrs
- Fix incorrect Javadoc on `zgetrs2()` that says "Wrapper for MKL function dgetrf()" instead of zgetrs2
- Fix incorrect Javadoc on `cgetrs()` that says "Wrapper for MKL function dgetrf()" instead of cgetrs
- Fix incorrect Javadoc on `sgetrs()` that says "Wrapper for MKL function dgetrf()" instead of sgetrs
- Fix incorrect Javadoc on `dgetrs()` that says "Wrapper for MKL function dgetrf()" instead of dgetrs
- Fix incorrect Javadoc on `zsytrf()` that says "Wrapper for MKL function dgetrf()" instead of zsytrf
- Fix incorrect Javadoc on `zsytrs()` that says "Wrapper for MKL function dgetrf()" instead of zsytrs
- Fix incorrect Javadoc on `zsptrf()` that says "Wrapper for MKL function dgetrf()" instead of zsptrf
- Fix incorrect Javadoc on `zsptrs()` that says "Wrapper for MKL function zgetrs()" instead of zsptrs
- Fix incorrect Javadoc on `csptrs()` that says "Wrapper for MKL function zgetrs()" instead of csptrs
- Add Javadoc with `@param`/`@return` documentation for `spotri()`, `spptrf()`, `spptri()`, `spptrf2()`, `spptri2()`, `cpptri()`, `cpptrf()`, `cgetrf()`, `csptrf()`, `csptri()` which have none
- Add Javadoc with `@param`/`@return` for `PARDISO()` which has many unclear parameters (maxfct, mnum, mtype, phase, idum, etc.)
- Add Javadoc for `sgecon()`, `dgecon()`, `zgecon()` which have none
- Add Javadoc for `zgeequ()`, `zgeequ2()`, `zlaqge()`, `claqge()`, `cgeequ()` which have none
- Fix mismatched `@param` tags in `csprfs()` Javadoc: documents `@param a` but parameter is `af`, and `afp` parameter has no `@param` tag
- Fix mismatched `@param` tags in `zsprfs()` Javadoc: same issue
- Complete the empty `@return` tags in `sgetri()`, `dgetri()`, and `zgetri()` Javadoc

### [LAPACKNative.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/com/intel/mkl/LAPACKNative.java)
- Remove the stale IDE template comment on lines 2-4
- Add Javadoc to the class explaining it is package-private and holds JNI native method declarations
- Add Javadoc to the static initializer block explaining the platform-specific library loading logic
- Add Javadoc to the `PARADISO()` native method documenting all parameters and return value
- Add Javadoc to all other native method declarations (~50 methods have no Javadoc)
- Remove or document the developer marker comment `// ----- andy ----` on line 54

### [LISN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/LISN.java)
- Add Javadoc on class explaining LISN for EMI analysis

### [LISNDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/LISNDialog.java)
- Add Javadoc on class

### [LKMatrices.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LKMatrices.java)
- **Largest/most complex file (1523 lines) with almost no Javadoc** -- critical documentation gap
- Add Javadoc to `initMatrizen` (all 3 overloads), `schreibeMatrix_A`, `schreibeMatrix_B`
- Add Javadoc to `calculateComponentCurrents` explaining non-linear convergence loop
- Add Javadoc to `aktualisiereKnotenpotentiale`, `setzeAnfangsbedingungen`, `getAWForInductance`
- Document solver-type-dependent coefficients (SOLVER_BE, SOLVER_TRZ, SOLVER_GS)
- Document all `parameter[i1][N]` magic column indices

### [LUDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/LUDecomposition.java)
- Fix constructor Javadoc (lines 61-64): the `@return` tag is invalid for constructors

### [LUDecompositionCache.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LUDecompositionCache.java)
- Add Javadoc to `getCachedLUDecomposition()` explaining hash-collision double-check logic
- Add Javadoc to `testForCacheShrink()`, `removeLeastAccessedMatrices()`, `calculateNewVarMaxCacheSize()`

### [Labable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Labable.java)
- Add class-level Javadoc explaining this marker interface
- Add Javadoc to `getLabelObject()`

### [Label.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Label.java)
- Add class-level Javadoc explaining the immutable value class
- Add Javadoc to `hashCode()` and `equals()`

### [LabelPriority.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LabelPriority.java)
- Add class-level Javadoc explaining label display priority
- Document numeric values (0, 1, 2, 4) and the gap (no 3)
- Add Javadoc to `isBiggerThan()`

### [LangInit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/LangInit.java)
- Add class-level Javadoc explaining language initialization system

### [LastComponentButton.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/LastComponentButton.java)
- Add class-level Javadoc explaining last-used component button

### [LaunchBrowser.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/LaunchBrowser.java)
- Add class-level Javadoc explaining cross-platform browser launching
- Document platform-specific behavior

### [LimitCalculatorExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/LimitCalculatorExternal.java)
- Add class-level Javadoc explaining external min/max limiting
- Document input indices: [0]=signal, [1]=min, [2]=max

### [LimitCalculatorInternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/LimitCalculatorInternal.java)
- Add class-level Javadoc explaining internal min/max limiting
- Add `@param` Javadoc on constructor for `minLimit` and `maxLimit`

### [LineSettable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/LineSettable.java)
- Add class-level Javadoc describing line setter interface

### [ListDnD.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ListDnD.java)
- Document as drag-and-drop test utility or mark for removal

### [LnCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/LnCalculator.java)
- Add class-level Javadoc: "Calculates natural logarithm"
- Document input domain assertion (> 0)

### [LoginDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/LoginDialog.java)
- Add class-level Javadoc explaining login dialog

### [LoopDetectionException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/LoopDetectionException.java)
- Add class-level Javadoc explaining when this is thrown (control algebraic loop detected)

### [LossCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculatable.java)
- Add Javadoc to the interface (capability marker for loss calculation)
- Add Javadoc to `getLossCalculation()`

### [LossCalculationDetail.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationDetail.java)
- Add Javadoc to enum explaining two detail levels (SIMPLE vs DETAILED)
- Add Javadoc to `getFromDeprecatedFileVersion()`

### [LossCalculationDetailed.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationDetailed.java)
- Add class-level Javadoc explaining detailed loss data from measurement files (.scl)
- Add Javadoc to `readDetailedLossesFromFile()` explaining three nested fallback formats
- Add Javadoc to `writeDetailedLossesToFile()` -- unclear parameter names `fkaku`, `fyomu`
- Fix incorrect `@return String` on `checkLinkToSemiconductorFile()` (returns boolean)

### [LossCalculationSimple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationSimple.java)
- Add class-level Javadoc explaining simplified loss formulas
- Document fields `_kON`, `_kOFF`, `_uSWnorm` (switching loss coefficients)
- Add Javadoc to inner class `LossCalculatorSwitchSimple`
- Document NaN check idiom `returnValue != returnValue`

### [LossCalculationSplittable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationSplittable.java)
- Add Javadoc to `getSwitchingLoss()` and `getConductionLoss()` documenting return unit (W)
- Fix class-level Javadoc wording


