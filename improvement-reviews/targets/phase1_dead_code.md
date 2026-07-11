# Phase 1: Dead Code, Debug Prints, and Redundant Code Removal

This file lists all the target files and specific tasks for Phase 1: Dead Code, Debug Prints, and Redundant Code Removal parsed from the review files.

Total target files: 143

## File and Task List

### [ABCDQCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ABCDQCalculator.java)
- Fix `qVAl` -> `qVal`

### [AbsCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbsCalculator.java)
- Remove trailing semicolon after class closing brace

### [AbstractBlockInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractBlockInterface.java)
- Remove dead commented-out code in `paintComponentForeGround`

### [AbstractCapacitor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCapacitor.java)
- Extract magic indices (2,3,4,5,7,8,9,10) into named constants

### [AbstractCircuitBlockInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitBlockInterface.java)
- Fix `return 00;` in `istAngeklickt()` -> `return 0;`

### [AbstractCircuitSheetComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractCircuitSheetComponent.java)
- `findAndSetReferenceToParentSheet` and `findAndSetReferenceToParentSheet2` are **identical duplicates** -- remove one

### [AbstractControlOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractControlOrderer.java)
- Remove commented-out block in `addNodesToNextList()`

### [AbstractExpression.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/AbstractExpression.java)
- The entire class body is commented out (dead code). Consider deleting the file or adding a comment

### [AbstractGeckoCustom.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/AbstractGeckoCustom.java)
- Remove any dead/commented-out code blocks
- ---

### [AbstractNonLinearCircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractNonLinearCircuitComponent.java)
- Expand the TODO at line 43 with specifics

### [AbstractSingleInputSingleOutputCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractSingleInputSingleOutputCalculator.java)
- Document or remove unused `_inputSignalValue` and `_outputSignalValue` fields

### [AbstractSwitchCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractSwitchCalculator.java)
- Remove commented-out `System.out.println`

### [AbstractTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractTerminal.java)
- Remove unused `orientierung` parameter in `paintFlowSymbol` (dead parameter)

### [AxisLimits.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLimits.java)
- Remove commented-out code in `importASCII`
- Replace legacy `java.util.Stack` with `ArrayDeque`

### [AxisLinLog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLinLog.java)
- Remove unnecessary trailing semicolon after enum closing brace (line 48)

### [BVector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/BVector.java)
- Replace `@author andy` with real class-level description

### [BigLUDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigLUDecomposition.java)
- Remove the dead commented-out "temporary, experimental code" block (lines 141-198) using invalid `\* ... *\` comment syntax

### [BigMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigMatrix.java)
- Remove the massive block of commented-out dead code (lines 278-491) containing disabled `plus`, `minus`, `times`, `arrayTimes`, `chol`, `qr`, `svd`, `eig`, and related methods

### [BlockOrderOptimizer3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/BlockOrderOptimizer3.java)
- Remove redundant duplicate `import java.util.*`

### [CBLAS.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/com/intel/mkl/CBLAS.java)
- Remove commented-out dead code on line 227

### [CapacitorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CapacitorCalculator.java)
- Remove large commented-out code blocks

### [CheckBoxList.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/CheckBoxList.java)
- Remove `main()` method (test/demo code in library class)

### [CholeskyDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/CholeskyDecomposition.java)
- Remove the commented-out "temporary, experimental code" block (lines 90-150) containing the dead right-triangular constructor and `getR()` method
- Replace bitwise `&` with logical `&&` on lines 79 and 82 (`isspd = isspd & ...`) to use proper short-circuit evaluation

### [CircuitLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitLabel.java)
- Remove redundant null check in `setLabel`

### [CircuitSheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitSheet.java)
- Remove redundant `if (comp instanceof SubcircuitBlock)` checks (three identical checks in a row)

### [Cispr16Fft.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Cispr16Fft.java)
- Make public mutable fields private with getters

### [CisprDataExport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/CisprDataExport.java)
- Consider using try-with-resources for `BufferedWriter`

### [ColorStrategySelected.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ColorStrategySelected.java)
- Remove redundant min/max clamping

### [CompileStatus.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompileStatus.java)
- **Remove unnecessary trailing semicolon** after the enum's closing brace on line 28

### [ComplexPrinter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ComplexPrinter.java)
- Fix indentation of class body

### [CompressedData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CompressedData.java)
- Remove unused imports `java.io.File` and `java.io.FileInputStream`

### [ConductionLossMeasurementCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/ConductionLossMeasurementCurve.java)
- Add null/empty guard in `copy()`

### [Connection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Connection.java)
- Document whether `initAnimationParts()` is dead code (throws UnsupportedOperationException)

### [ConstantExpression.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/ConstantExpression.java)
- The entire class body is commented out (dead code). Consider deleting the file or adding a comment explaining why it is disabled

### [ControlBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlBlock.java)
- Remove commented-out `System.out.println` in `getOutputs()`

### [ControlCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/ControlCalculatable.java)
- Remove commented-out `serialVersionUID` (dead code)

### [ControlGate.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGate.java)
- Remove trailing semicolon after method body on line 64

### [ControlImportDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlImportDialog.java)
- Remove large commented-out code block

### [ControlJavaTriangles.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/ControlJavaTriangles.java)
- **Make parameter modifiers consistent:** Add `final` to `isDecreaseClicked` parameters for consistency with `isIncreaseClicked`

### [ControlMAX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMAX.java)
- Add `serialVersionUID` field (missing)

### [ControlMIN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMIN.java)
- Add `serialVersionUID` field (missing)

### [ControlMMF.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMMF.java)
- Add `serialVersionUID` field (missing)

### [ControlMUX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMUX.java)
- Remove dead variables `d1`, `d2`, `dpfx`, `dpfy` in `drawBlockRectangle()`

### [ControlNE.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlNE.java)
- Add `serialVersionUID` field (missing)

### [ControlNOT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlNOT.java)
- Add `serialVersionUID` field (missing)

### [ControlNativeC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/ControlNativeC.java)
- **Remove dead/commented-out code blocks:** Lines 257-263 (commented `CompileStatus` color logic), lines 177-178 (commented `showMsg` call), lines 421-434 (commented dialog initialization logic)

### [ControlOr.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlOr.java)
- Add `serialVersionUID` field (missing)

### [ControlPMSMCONTROL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPMSMCONTROL.java)
- Add `serialVersionUID` field (missing)

### [ControlPOW.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPOW.java)
- Add `serialVersionUID` field (missing)

### [ControlPT1.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPT1.java)
- Add `serialVersionUID` field (missing)

### [ControlPT2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPT2.java)
- Add `serialVersionUID` field (missing)

### [ControlSIN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSIN.java)
- Fix the double space in the class declaration: `public final  class` should be `public final class` (line 20)

### [ControlSignalSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignalSource.java)
- Remove the stray empty semicolon statement on line 125 (bare `;` after the field declaration block)

### [ControlSlidingDFTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSlidingDFTDialog.java)
- Remove dead/commented-out code: line 89 `//addFormatJTextField(newFreq);` and line 177 `//addFormatJTextField(data._frequency.getValue());`

### [ControlSmallSignalAnalysis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSmallSignalAnalysis.java)
- Make `BLOCK_WIDTH` (line 36) `static final` since it is a constant currently declared as a plain instance field (`private final int BLOCK_WIDTH = 6`)

### [ControlSpaceVector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSpaceVector.java)
- **Remove dead code**: the local variable `y` (line 73) is declared and assigned but never used in `drawBlockRectangle`
- **Fix inefficient string comparison**: line 65 uses `header[knotenIndex + 1].equals(new String(""))` -- replace with `.isEmpty()` or `.equals("")` to avoid unnecessary object creation

### [ControlSubtraction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSubtraction.java)
- Change the visibility of `tinfo` (line 22) from package-private to `public static final` for consistency with all other control blocks in this set (e.g., `ControlRound.tinfo`, `ControlSIN.tinfo`)

### [ControlTransferFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTransferFunction.java)
- Fix confusingly named tokens: "nominatorPoles" exports `_poles` and "denominatorZeros" exports `_zeros` (appear swapped)

### [ControlTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTypeInfo.java)
- Investigate: constructor parameter `typeDescriptionVerbose` never passed to super()

### [CurvePainterRegular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurvePainterRegular.java)
- Replace legacy `java.util.Stack` with `ArrayDeque`

### [CurvePainterSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurvePainterSignal.java)
- Replace legacy `java.util.Stack` with `ArrayDeque`

### [CurvePixelPainterHiLow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurvePixelPainterHiLow.java)
- Remove redundant local `final HiLoData hiLow = value;`

### [DQABCDCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/DQABCDCalculator.java)
- Fix inconsistent capitalization: `qVAl` -> `qVal`

### [DataTablePanelParameters.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DataTablePanelParameters.java)
- **`getCheckedData()` always returns null** with commented-out body -- implement or document why

### [DefinedExternalSignals.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DefinedExternalSignals.java)
- Entire file is commented-out dead code -- remove or add explanation

### [DefinedMeanSignals.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DefinedMeanSignals.java)
- Replace legacy `java.util.Stack` with `ArrayDeque`

### [DelegateNumericTextField.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/DelegateNumericTextField.java)
- Remove extra blank lines at end of file

### [DialogConnectSignalsGraphs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogConnectSignalsGraphs.java)
- Remove stray semicolon on line 51

### [DialogDataExport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogDataExport.java)
- Replace hardcoded developer path `/home/andreas/testFile.txt` with a sensible default

### [DialogFourierDiagramm.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DialogFourierDiagramm.java)
- **Replace `StringBuffer` with `StringBuilder`** (line 142) -- `StringBuffer` is synchronized and unnecessarily slow in single-threaded context

### [DialogGlobalTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogGlobalTerminal.java)
- Remove empty `jTextFieldNameKeyTyped` and `jTextFieldNameFocusLost` if unused

### [DialogModule.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogModule.java)
- Remove `main(String[])` test/demo method if dead code

### [DialogSSAPlot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogSSAPlot.java)
- Remove commented-out code blocks

### [DiodeCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DiodeCalculator.java)
- Remove commented-out code blocks and `System.out.println` calls

### [DisplayFourierWorksheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DisplayFourierWorksheet.java)
- **Remove unnecessary `new String(...)` wrapper calls** (lines 57, 59, 61) -- `formatT` already returns a `String`

### [EqualCalculatorTwoInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/EqualCalculatorTwoInputs.java)
- Note: floating-point exact `==` comparison may be unreliable

### [FourierPlotFrame.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierPlotFrame.java)
- **Remove unused imports.** `SaveViewFrame`, `ScopeSignalSimpleName`, `AbstractScopeSignal`, `DialogConnectSignalsGraphs`, and `BufferedWriter` (lines 17-22)

### [GeckoExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoExternal.java)
- Remove or document empty `runGeckoSCRIPT()` method (dead code)

### [GeckoLineType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoLineType.java)
- Methods return `GeckoLineStyle` instead of `GeckoLineType` -- investigate type confusion

### [GeckoMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GeckoMatrix.java)
- **Entire file is dead/commented-out code** -- recommend removing entirely

### [GeckoRemoteMMFObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteMMFObject.java)
- Consider extracting the repeated try/catch/checkRemote pattern into a helper method

### [GeckoSim.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoSim.java)
- Document or remove empty catch block in `loadPropertyFile()`
- Fix duplicate text in `checkJavaVersion()` error message

### [GeckoSimulink.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoSimulink.java)
- Remove dead field `tmpRemove` (never referenced)

### [GlobalColors.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GlobalColors.java)
- Consider grouping colors by domain

### [GraferImplementation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferImplementation.java)
- **Remove dead/empty stub methods.** `mouseMode_DRAW_LINE`, `mouseMode_DRAW_TEXT`, `mouseMode_FIBONACCI_LIN`, `mouseMode_FIBONACCI_LOG` are empty stubs

### [GraferTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GraferTest.java)
- Consider moving to test source directory

### [GraferV3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferV3.java)
- **Remove large block of commented-out dead code** (lines 1027-1035)

### [HorizontalLevel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/HorizontalLevel.java)
- Note: nearly identical to `TriggerPosition.java` -- consider extracting common base

### [IntegratorCalculation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/IntegratorCalculation.java)
- Document `_xoldInit` and `_yoldInit` (never assigned -- possible dead code)

### [IsDtChangeSensitive.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/IsDtChangeSensitive.java)
- Fix grammar: "This function should called" -> "This function should be called"

### [JPanelLossDataInterpolationSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/JPanelLossDataInterpolationSettings.java)
- Replace swallowed exception with proper error logging

### [JavaMemoryRestart.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/JavaMemoryRestart.java)
- Fix inconsistent indentation on lines 181-182
- Remove trailing semicolon after class closing brace

### [JavaScriptTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/JavaScriptTest.java)
- The entire class body is commented out (dead code). Consider deleting the file

### [LAPACKNative.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/com/intel/mkl/LAPACKNative.java)
- Remove commented-out dead code on line 27: `//System.loadLibrary( "mkl_java_stubs" );`

### [LUDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/LUDecomposition.java)
- Remove the commented-out "temporary, experimental code" block (lines 137-194)
- Remove unnecessary `(double)` casts on line 271 (`vals[i] = (double) piv[i]`) and line 285 -- int auto-widens to double

### [LUDecompositionCache.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LUDecompositionCache.java)
- Remove dead commented-out code

### [Label.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Label.java)
- Consider making the class `final`

### [ListDnD.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ListDnD.java)
- Make `arrayListHandler` field private

### [LoopDetectionException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/LoopDetectionException.java)
- Implement or remove commented-out `printLoopMessage()`

### [MainWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/MainWindow.java)
- Remove commented-out steady-state analysis block

### [Matrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Matrix.java)
- Use covariant return type for `clone()` (line 203): change return type from `Object` to `Matrix`

### [ModelMVCGeneric.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/ModelMVCGeneric.java)
- Remove commented-out dead code on line 31

### [MyFFT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/MyFFT.java)
- Remove commented-out code blocks

### [MyProxy.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/MyProxy.java)
- Add meaningful class-level Javadoc or remove if dead code (contains only `// TODO!!! asdf`)

### [NComplex.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/NComplex.java)
- Fix fragile float equality comparisons: `im == 1`, `im == -1`, `Math.abs(im) == 1` -- should use epsilon-based comparison
- Make `TechFormat tcf` field `static final` since `NComplex` is otherwise immutable

### [NativeCBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCBlock.java)
- **Remove unused imports** (lines 17-19): `java.lang.reflect.Field`, `java.lang.reflect.Method`, and `java.util.Vector` are only referenced in commented-out code
- **Remove dead/commented-out code** in `unloadLibraries` (lines 86-94): The entire reflection-based native library finalization block is commented out

### [NativeCClassLoader.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCClassLoader.java)
- **Resource leak risk in `findClass`:** No `finally` block or try-with-resources. If `inBuff.read()` throws, streams are never closed
- **Performance: byte-by-byte reading** in the `while` loop (line 51) is inefficient. Consider using `inBuff.read(byte[])` or `readAllBytes()` (Java 9+)

### [NativeCDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCDialog.java)
- **Fix raw types:** `_fileList` (line 58) is declared as raw `DefaultListModel` without generics. Update to `DefaultListModel<String>` for type safety

### [NativeCWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCWrapper.java)
- ---

### [NetListContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListContainer.java)
- Remove unused `simKern` parameter in fabric methods

### [NetlistControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/NetlistControl.java)
- Remove large commented-out code blocks in `calculateTimeStep()`

### [NodeLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NodeLabel.java)
- Remove commented-out dead code in `exportASCII`

### [PICalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PICalculator.java)
- Fix field naming inconsistency (some use `_` prefix, some don't)

### [PT2Calculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT2Calculator.java)
- Remove commented-out dead code

### [PmsmControlCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmControlCalculator.java)
- Remove possible dead fields (`psi_sa_last`, `psi_sb_last`, etc.)

### [PolynomTools.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PolynomTools.java)
- Remove commented-out code block

### [Polynomials.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Polynomials.java)
- Remove redundant `extends Object` (line 19)
- Fix Java array declaration style: change C-style `float u[]` to idiomatic `float[] u` for all parameters on line 28
- ---

### [PotentialArea.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialArea.java)
- Remove unused `static long counter = 0` field
- Remove commented-out block in `isEmptyPotential`
- `getTermConnectors`, `getTermComponents`, `addTermConnector` all throw `UnsupportedOperationException("Not yet implemented")` -- implement or remove

### [SchematicEditor2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicEditor2.java)
- Remove dead code: `isLabelRenameRequired`, commented-out clipboard block, redundant assignment

### [Scopable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/Scopable.java)
- **Remove commented-out method declaration** (line 25): `//public ScopeSettings getScopeSettings();`
- ---

### [SignalCalculatorExternalWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorExternalWrapper.java)
- Fix inconsistency: line 46 uses `_inputSignal[1][0]` instead of `_inputSignal[FREQUENCY_INDEX][0]`

### [SignalCalculatorTriangle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorTriangle.java)
- Remove commented-out dead code

### [SimulationKernel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationKernel.java)
- Remove commented-out `external_step` method block

### [SmallSignalCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SmallSignalCalculator.java)
- Remove hardcoded file path `/home/andy/data.txt`

### [SpaceVectorDisplay.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SpaceVectorDisplay.java)
- Remove commented-out `main()` method
- Document `_old_time` field (assigned but never read -- potential dead code)

### [SubcircuitBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SubcircuitBlock.java)
- Remove `System.err.println` or convert to proper logging

### [SymmetricSparseMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SymmetricSparseMatrix.java)
- Remove or document unused fields (`Acsr`, `AcsrComplex`, `AIT`, `AJT`, `columnRowIndices`)

### [TestReceiverWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TestReceiverWindow.java)
- Remove commented-out code

### [TextSeparator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/TextSeparator.java)
- ---

### [ThermMODUL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermMODUL.java)
- Remove commented-out test labels ("bli", "bla", "blub")

### [ThyristorControlCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ThyristorControlCalculator.java)
- Document dead code: condition `-1 > _lastFallingZero` is always false

### [TimeSeriesVariableBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TimeSeriesVariableBlock.java)
- Remove large commented-out `main()` test method

### [TokenMap.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TokenMap.java)
- Remove or implement empty/dead `makeBlockTokenMap` and `getBlockMap()` (returns null)

### [ToolBar.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ToolBar.java)
- Verify whether this is dead code (leftover demo/test with `main()` and hardcoded `.gif` files)
- If kept, add null-checks for `ImageIcon` loads

### [TriggerPosition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TriggerPosition.java)
- Note: nearly identical to `HorizontalLevel.java` -- consider extracting common base

### [UniqueObjectIdentifer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/UniqueObjectIdentifer.java)
- Remove commented-out assert
- Make `generator` field `final`

### [UserParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/UserParameter.java)
- ---

### [VariableBusWidth.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/VariableBusWidth.java)
- ---

### [VariableExpression.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/VariableExpression.java)
- The entire class body is commented out (dead code). Consider deleting the file

### [WeakListModel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/WeakListModel.java)
- Add null check in `firstElement()` and `lastElement()` for empty list

### [resources/I18nKeys.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/resources/I18nKeys.java)
- Remove commented-out two-argument constructor (dead code)
- Consider grouping enum constants by domain with section headers

### [translationtoolbox/TranslationDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationDialog.java)
- Extract common initialization code from duplicate constructors

### [translationtoolbox/TranslationTools.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationTools.java)
- Extract duplicated progress monitor setup code (repeated 5x) into helper method

