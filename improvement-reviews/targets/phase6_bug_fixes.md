# Phase 6: Logic, Shadowing, and Bug Fixes

This file lists all the target files and specific tasks for Phase 6: Logic, Shadowing, and Bug Fixes parsed from the review files.

Total target files: 71

## File and Task List

### [AbstractControlOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractControlOrderer.java)
- Remove debug print statements checking for "OR.1" / "SPARSEMATRIX.2"

### [AbstractNonLinearCircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractNonLinearCircuitComponent.java)
- Remove debug commented-out code block

### [BigLUDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigLUDecomposition.java)
- Fix bug: bitwise `&` on line 132 (`if (j < m & LU[j][j].abs().doubleValue() > 1e-30)`) should be logical `&&` -- risks `ArrayIndexOutOfBoundsException`
- Remove debug artifacts: `System.err.println(" j: " + j)` in `isNonsingular()` (line 210) and `//test` comment (line 37)

### [BigMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigMatrix.java)
- Fix bug in `read()` method (line 639): it returns `Matrix` instead of `BigMatrix` -- also uses deprecated raw `java.util.Vector` types

### [BlockOrderOptimizer3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/BlockOrderOptimizer3.java)
- Remove commented-out `System.out.println` debug statements

### [CBLAS.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/com/intel/mkl/CBLAS.java)
- Fix incorrect Javadoc on `cspmv()`: describes full `y := alpha*A*x + beta*y` but actual method signature is only `cspmv(int n, float[] ap, float[] x, float[] y)` -- copy-paste error from `sspmv`

### [CachedMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CachedMatrix.java)
- Remove dead/commented-out debug code (lines 75-76, 198-208, 254)

### [CapacitorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CapacitorCalculator.java)
- Remove debug `System.out.println("setting z Value: + " + z)`

### [CheckBoxList.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/CheckBoxList.java)
- Remove debug `System.out.println` statements

### [CircuitSourceType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitSourceType.java)
- Note: `getNewID()` returns `double` not `int` (possible bug)

### [CompiledClassContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompiledClassContainer.java)
- **Add Javadoc to the class and all methods.** The class-level Javadoc (lines 19-22) is an empty placeholder. All three constructors, `getClassBytes()`, and `getSourceString()` have no Javadoc
- **Bug: `getClassBytes()` will throw NullPointerException** when `_classBytes` is null. Add null guard or return empty array

### [ControlControlDebug.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlControlDebug.java)
- Add class-level Javadoc explaining debugging/breakpoint control block

### [ControlDebugWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlDebugWindow.java)
- Add class-level Javadoc explaining debug stepping dialog window
- Add TODO implementation for jButton1ActionPerformed or document as placeholder

### [ControlNativeC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/ControlNativeC.java)
- **Potential bug: `_libFile.getFileName()` NPE risk** in the `UnsatisfiedLinkError` catch block (line 199). Add null check

### [ControlOSZI.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlOSZI.java)
- Fix potential bug: line 398 checks `settingsMap != null` but should check `windowSettingsMap`
- Remove commented-out debug line

### [ControlPOW.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPOW.java)
- Note: calculator created with `super(1, 1)` but block has 2 inputs -- possible bug

### [ControlSPARSEMATRIX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSPARSEMATRIX.java)
- **Bug -- array size mismatch**: `getOutputNames()` returns 9 elements but `getOutputDescription()` returns only 8 `I18nKeys` (line 60-61) -- add the missing 9th description element
- **Bug -- duplicate label**: line 34 creates a second terminal labeled `"uN2"`; given the sequence (uN1, uN2, ...), this should likely be `"uN3"`

### [ControlSaveData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSaveData.java)
- Fix potential bug in `setPercentageText`: `_dataSaver.getPercentage()` is called a second time in the else-branch string formatting (line 233) instead of reusing the already-passed `percentage` parameter

### [ControlTransferFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTransferFunction.java)
- Document potential bug in `clearPolesAndZeros()`

### [DataContainerSimple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerSimple.java)
- **Bug: redundant/ineffective bounds check in `setValue`** (lines 42-44 and 49-51). The subsequent `if (column < _data[row].length)` is always true (dead redundant check) and can be removed

### [DataSaver.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DataSaver.java)
- Fix copy-paste bug: catches `IOException` but logs using `DialogDataExport.class.getName()` instead of `DataSaver.class`

### [DataTablePanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DataTablePanel.java)
- Remove or document `counter` static field (debug/test code)

### [DefinedMeanSignals.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DefinedMeanSignals.java)
- Fix potential bug in `defineNewMeanSignal` sorted insertion (may add twice)

### [DelegateCheckBox.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/DelegateCheckBox.java)
- Add Javadoc to `actionPerformed(ActionEvent arg0)` explaining it synchronizes the checkbox state
- Rename the unclear parameter `arg0` in `actionPerformed()` to `e` or `event`

### [DelegateIntSpinner.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/DelegateIntSpinner.java)
- Complete `actionPerformed()` Javadoc: `@param evt` has no description

### [DelegateNumericTextField.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/DelegateNumericTextField.java)
- Add Javadoc to `registerModel()`, `unregisterModel()`, `actionPerformed()`, `saveValue()`
- Add input validation around `Double.parseDouble(getText())` to handle `NumberFormatException`

### [DialogFourierDiagramm.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DialogFourierDiagramm.java)
- **Remove debug/error code string `qe90r8gn03g8q`** (line 167) and similar obfuscated error identifiers (lines 240, 306). Replace `System.out.println` with proper `Logger` calls

### [DialogLossesDetail.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DialogLossesDetail.java)
- Remove commented-out debug line

### [DialogMuxDemux.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogMuxDemux.java)
- Fix title hardcoded as "External interface" (copy-paste error -- this is for Mux/Demux)

### [DialogSmallSignalAnalysis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogSmallSignalAnalysis.java)
- Remove `System.out.println` debug output on line 200

### [DragTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DragTest.java)
- Remove all `System.out.println` debug output

### [FourierDiagramm.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierDiagramm.java)
- **Bug: duplicate condition in zoom rectangle drawing** (lines 250-258). First two `else if` conditions are identical; also line 256 has `(y1Zoom > y1Zoom)` which is always false. Fix all four rectangle-positioning branches
- **Bug: `getPixelFromValue` never computes `xPix`/`yPix`** (lines 563-587). The method always returns `{-1, -1}`. The logic needs to be inverted to solve for `xPix`/`yPix`

### [FourierKurvenRekonstruktion.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierKurvenRekonstruktion.java)
- **Bug: `getPixelFromValue` never computes pixel values** (lines 444-468). Identical bug to `FourierDiagramm`: always returns `{-1, -1}`
- **Remove commented-out debug code** (lines 302-303)

### [GeckoLineType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoLineType.java)
- **Bug**: `getFromOrdinal` iterates over `GeckoLineStyle` values instead of `GeckoLineType` values

### [GeckoRemoteMMFObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteMMFObject.java)
- **Bug found**: line 1814 the constructor uses `"getSignalData"` instead of `"simulateToSteadyState"` -- copy-paste error

### [GeckoSim.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoSim.java)
- Add field-level Javadoc for `public static double xx = 4.67` (appears to be debug/test value)

### [GraferImplementation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferImplementation.java)
- **Bug: self-assignment in `setzeAchsen()`** (lines 1326-1329). `xTickAutoSpacing[i1] = xTickAutoSpacing[i1];` and `yTickAutoSpacing[i1] = yTickAutoSpacing[i1];` are no-ops
- **Replace `System.out.println("Fehler: ...")` debug calls with proper Logger calls.** Lines 549, 753, 1746, 1765 use obfuscated error codes

### [GraferV3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferV3.java)
- **Replace `System.out.println("Fehler: ...")` debug calls with Logger.** Lines 217, 225, 585, 639, 1068, 1100, 1149, 1181, 1426 should all use a proper logger

### [GroupableUndoManager.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/GroupableUndoManager.java)
- Remove the commented-out debug line on line 32

### [HysteresisCalculatorExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/HysteresisCalculatorExternal.java)
- Document potential bug: only checks positive hValue boundary

### [HysteresisCalculatorInternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/HysteresisCalculatorInternal.java)
- Same potential bug: only checks positive boundary

### [IGBTCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IGBTCalculator.java)
- Remove commented-out debug lines

### [LKMatrices.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LKMatrices.java)
- Remove massive blocks of dead commented-out debug code
- Replace German variable names in debug prints with English

### [LUDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/LUDecomposition.java)
- Fix bug: bitwise `&` on line 129 (`if (j < m & LU[j][j] != 0.0)`) should be logical `&&` -- risks `ArrayIndexOutOfBoundsException`
- Remove `//test` debug comment (line 35) and dead `//package Jama;` comment (line 17)

### [LossCalculationDetailed.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationDetailed.java)
- Investigate bug: `_temperature = DEFAULT_REFERENCE_VOLTAGE` (100) uses voltage constant as temperature

### [LossCalculationSimple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationSimple.java)
- Remove debug print `System.out.println("xxxxxxxx " + _uSWnorm)`

### [LossCurveTemperaturePanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCurveTemperaturePanel.java)
- Investigate `_gbc` variable (created but GridLayout is used -- possibly redundant)

### [MainWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/MainWindow.java)
- Add Javadoc to `saveFile()`, `openFile()`, `actionPerformed()`, `processKeyEvents()`

### [MyTableComparator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/MyTableComparator.java)
- Fix logic bug: line 37 `o2.get(0) == 0` uses autoboxing identity comparison instead of `.equals(0.0)`

### [NComplex.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/NComplex.java)
- Fix hashCode bug: line 184 uses `Double.hashCode()` on float fields `re` and `im` -- should use `Float.hashCode()`

### [NativeCBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCBlock.java)
- **Bug: `checkOutputsForNANorINFValues` only checks for NaN, not Infinity.** `signal[i] != signal[i]` only detects NaN. Add `Double.isInfinite()` check or rename method

### [NativeCDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCDialog.java)
- **Bug: `System.err.println(exc.getStackTrace())`** (line 191) prints a `StackTraceElement[]` array object, producing unhelpful output. Replace with `exc.printStackTrace()` or proper logger

### [NativeCLibraryFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCLibraryFile.java)
- **Reduce code duplication in `setFile` overloads:** `setFile(File)` and `setFile(String)` repeat existence check, path assignment, and timestamp logic. Consolidate

### [NetListLK.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListLK.java)
- Remove debug string `System.out.println("Fehler qer^08gj03qhg4")` in `getNetlistnNummer`
- Remove large commented-out debug block with hardcoded test values

### [NodeLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NodeLabel.java)
- Remove commented-out `System.out.println` debug line

### [NonLinearDialogPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NonLinearDialogPanel.java)
- Remove debug `System.out.println("iii " + ...)` loop

### [Paradiso.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Paradiso.java)
- Remove commented-out `System.out.println` debug lines

### [PolynomTools.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PolynomTools.java)
- Document potential bug on line 172: `polynomImag[1 + k]` uses `tmpReal[k]` instead of `tmpImag[k]`

### [PriorityThreadFactory.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PriorityThreadFactory.java)
- **Bug**: `newThread()` creates a thread, sets priority, but then returns a *new* `Thread(r)` without the priority -- fix by returning the first thread

### [SchematicEditor2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicEditor2.java)
- Fix bug in `isRightMouseClickActionOrCtrlLeftClick`: both sides of `||` are identical

### [SchematicTextInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicTextInfo.java)
- **Bug**: `_yTxtKlickMin` assigned then `_yTxtKlickMax` assigned to same expression in `updateRanges`
- **Bug**: line 121 has duplicate condition `_dxTxt != _dxTxtBeforeMove` (second should be `_dyTxt`)

### [SignalCalculatorRectangle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorRectangle.java)
- Fix potential bug: hardcoded `0.5` on lines 63-64 instead of `_dutyRatio`

### [SlidingDFTCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SlidingDFTCalculator.java)
- Fix potential bug: line 75 uses `_outputSignal[0][0]` instead of `_outputSignal[i][0]`

### [SmallSignalCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SmallSignalCalculator.java)
- Remove dead/debug code: `System.out.println("xxx ...")`, `printResults()` methods

### [SymmetricDoubleSparseMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SymmetricDoubleSparseMatrix.java)
- Fix bug risk in `removeZeroEntry`: removes by value instead of by key

### [TerminalControlBidirectional.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControlBidirectional.java)
- Document possible bug: `createCopy` returns `TerminalControlInput` not `TerminalControlBidirectional`

### [TerminalRelativeFixedDirection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalRelativeFixedDirection.java)
- Document `getPointFromDirection` and verify possible bug in `WEST_EAST` case

### [TimeSeriesArray.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TimeSeriesArray.java)
- Remove or document `static int counter` debug variable

### [VoltageSourceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceCalculator.java)
- Remove commented-out debug lines

### [VoltageSourceDCMachineCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceDCMachineCalculator.java)
- Remove commented-out debug

### [WeakListModel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/WeakListModel.java)
- Change `serialVersionUID` from public to private
- Reduce code duplication in the three `fire*` methods

