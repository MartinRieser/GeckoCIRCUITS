# Phase 2: Typo Fixes and Naming Standardizations

This file lists all the target files and specific tasks for Phase 2: Typo Fixes and Naming Standardizations parsed from the review files.

Total target files: 62

## File and Task List

### [AbstractBlockInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractBlockInterface.java)
- Fix typo "mehtod" -> "method" in `importIndividual` comment

### [AbstractCapacitor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCapacitor.java)
- Fix typo `getInitalNonlinValues` -> `getInitialNonlinValues`

### [AbstractCircuitSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitSource.java)
- Fix typo "non-accessibe" -> "non-accessible"

### [AbstractMotor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractMotor.java)
- Rename `_drehzahl` to `_rotationalSpeed` or add inline comment

### [AbstractNonLinearCircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractNonLinearCircuitComponent.java)
- Fix typo "Impromer" -> "Improper" in error message
- Fix typo "picewise" -> "piecewise" in comments

### [AbstractTimeSerie.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractTimeSerie.java)
- Fix class name typo: "Serie" should be "Series"
- Fix Javadoc typo: "bigest" -> "biggest"

### [AxisLimits.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLimits.java)
- Fix `_HistoryStack` naming convention (should be `_historyStack`)

### [BigMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigMatrix.java)
- Fix typo "colums" -> "columns" in the constructor Javadoc (line 96 and other occurrences)
- Add Javadoc to `ResetLUDecomp()` (line 555) and rename it to `resetLUDecomp` to follow Java naming conventions

### [CholeskyDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/CholeskyDecomposition.java)
- Rename constructor parameter `Arg` (line 61) to follow Java naming conventions (e.g., `matrix` or `source`)

### [CircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CircuitComponent.java)
- Fix typo `terminalNUmber` -> `terminalNumber`

### [CircuitLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitLabel.java)
- Add Javadoc to `RenameLabelUndoableEdit` inner class

### [Cispr16Settings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/Cispr16Settings.java)
- Document `_qpInteval` field name typo ("Inteval" should be "Interval")

### [ColorStragegyDisabledComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ColorStragegyDisabledComponent.java)
- Fix class name typo: "Stragegy" -> "Strategy"

### [CompileStatus.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompileStatus.java)
- **Fix typo: `COMPILED_SUCCESSFULL`** should be `COMPILED_SUCCESSFUL` (line 18). Note: this is a public enum constant, so callers must be updated

### [ControlAbsolutValue.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAbsolutValue.java)
- Fix class name typo: "AbsolutValue" should be "AbsoluteValue"

### [ControlJavaTriangles.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/ControlJavaTriangles.java)
- **Fix typo in class Javadoc:** "whe should split this" (line 18) should be "**we** should split this"

### [ControlMUX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMUX.java)
- Fix typo "carful" -> "careful"

### [ControlNativeC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/ControlNativeC.java)
- **Fix typo: `severeErrorOccured`** (lines 120, 146, 148, 161, 166, 180, 194, 203) should be `severeErrorOccurred` (double 'r' in "occurred")

### [ControlSPARSEMATRIX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSPARSEMATRIX.java)
- Rename the class from `ControlSPARSEMATRIX` to `ControlSparseMatrix` to comply with Java naming conventions (note: the `tinfo` string ID `"SPARSEMATRIX"` can remain unchanged for serialization)

### [ControlSaveData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSaveData.java)
- Fix typo/abbreviation in `"Cont. Saving"` (line 228) -- should be `"Continuous Saving"` or `"Cont. saving"` for consistency with other status strings

### [ControlSlidingDFT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSlidingDFT.java)
- **Fix typo**: the constant `DEFAULT_FREQENCY` (line 34) should be `DEFAULT_FREQUENCY` -- note this is `private static final` so the rename is contained to this file
- Fix the presentation name strings `"Frequency selection of SFFT"` (lines 312, 317, 322) -- "SFFT" is likely a typo for "SDFT" (Sliding DFT)

### [ControlSmallSignalAnalysis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSmallSignalAnalysis.java)
- Verify/fix the typo `SMALL_SIGNAL_ANALYIS` in the referenced `I18nKeys` constant (line 34) -- "ANALYIS" should be "ANALYSIS"; if the key itself is misspelled, this requires checking `I18nKeys` but the reference here should use the corrected name

### [DataContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DataContainer.java)
- **Clarify the `getHiLoValue` parameter naming.** The interface declares `getHiLoValue(int row, int column, int columnOld)` but the implementation uses `columnStart`/`columnStop`. Rename interface parameter `columnOld` to `columnStop` for consistency

### [DetailedLossLookupTable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailedLossLookupTable.java)
- Fix repeated misspelling `wheigt` -> `weight`

### [DetailedSwitchingLossesPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailedSwitchingLossesPanel.java)
- Fix double semicolon typo on line 25

### [DetailledLossPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailledLossPanel.java)
- Add Javadoc to abstract class (note: class name typo "Detailled" vs "Detailed")
- Fix misspelled field `_grafer` (should be "grapher")

### [DialogElementCONTROL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogElementCONTROL.java)
- Fix typo "git an XException" -> "get an XException"

### [DialogElementLK.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DialogElementLK.java)
- Fix typo "packagse" -> "package" in Javadoc

### [DialogLossesDetail.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DialogLossesDetail.java)
- Fix typo in `fabricCreateExisiting()` -> `fabricCreateExisting()`

### [DialogTransferFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogTransferFunction.java)
- Fix method name typo "Cofficients" -> "Coefficients"

### [EqualCalculatorMultiInput.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/EqualCalculatorMultiInput.java)
- Fix parameter typo "intputSize" -> "inputSize"

### [FourierDiagramm.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierDiagramm.java)
- **Fix typo in field name `xNeuWert`** (line 102) -- consider renaming for readability

### [GeckoMemoryMappedFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoMemoryMappedFile.java)
- Rename `_defaultBufferSize` to `DEFAULT_BUFFER_SIZE` (it is `public static final`)

### [GeckoRemoteException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteException.java)
- Fix typo `preecedingException` -> `precedingException`

### [GeckoRemoteIntWithoutExc.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteIntWithoutExc.java)
- Fix typo `supressMessages` -> `suppressMessages`

### [GeckoRemoteRegistry.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteRegistry.java)
- Rename `_ipQuerySite` to follow constant naming conventions

### [GraferImplementation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferImplementation.java)
- **Add Javadoc to the class and most public methods.** This 2476-line class has almost no Javadoc. Key methods needing documentation: constructor, `setzeKurvenUndWorksheetDaten`, `akualisiereKurvenUndWorksheetDaten` (note typo "akualisiere" missing 'k'), `setzeAchsen`, `setzeKurven`, `setMouseMode`, `mouseMode_ZOOM_WINDOW`, `zoomRectangle`

### [GraferV3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferV3.java)
- **Fix typo: `LIGTHGRAY`** (line 76, and used at line 1411). Should be `LIGHTGRAY`. Also the `FARBEN` array entry `"ligthgray"` (line 69)

### [LAPACKNative.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/com/intel/mkl/LAPACKNative.java)
- Add Javadoc to grouped method sections explaining the s/d/c/z naming convention (single/double/complex/double-complex)

### [LabelPriority.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LabelPriority.java)
- Fix typo in method name `getHighesPriority` -> `getHighestPriority`

### [LossCalculationDetail.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationDetail.java)
- Fix constructor parameter typo: `diplayString` -> `displayString`

### [LossCalculationDetailed.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationDetailed.java)
- Fix method name typo: `getCopyOfConductionLossMeasurementCurvenArray` -> `Curves`

### [LossCalculationSimple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationSimple.java)
- Rename or document `UK_DEFAULT_VALUE` (400.0) -- "UK" is unclear

### [Matrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Matrix.java)
- Fix typo "colums" -> "columns" in Javadoc across constructors (lines 92, 105, 138, 783, 799)
- Add Javadoc to `times(Matrix B)` (line 689) and `ResetLUDecomp()` (line 745); rename `ResetLUDecomp` to `resetLUDecomp`

### [ModelMVCGeneric.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/ModelMVCGeneric.java)
- Fix typo "usefull" -> "useful" in class-level Javadoc
- Fix typo "aquire" -> "acquire" in `addModelListener()` Javadoc

### [NComplex.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/NComplex.java)
- Add Javadoc to `nicePrint()` and rename `RCmul` to a descriptive name such as `scale` or `multiplyByScalar`

### [NativeCDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCDialog.java)
- **Fix typo in comment:** "Paramter" (line 134) should be "Parameter"

### [NativeCLibraryFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCLibraryFile.java)
- **Fix typo in method names:** `savegetFile` (line 79) and `savegetFileName` (line 87) -- "saveget" should be `safeGetFile` and `safeGetFileName`. Update callers (e.g., `ControlNativeC.java` line 159)

### [NiceScale.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NiceScale.java)
- Fix typo `ONE_PT_FIFE` -> `ONE_PT_FIVE`

### [NotCalculateableMarker.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/NotCalculateableMarker.java)
- Fix typos "computatoin" -> "computation" and "simualtion" -> "simulation"

### [PotentialArea.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialArea.java)
- Fix typo "mergin" -> "merging"

### [PotentialCoupable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialCoupable.java)
- Fix typo "Voltge" -> "Voltage" in class Javadoc

### [PowerModulePainter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/PowerModulePainter.java)
- Rename `zeichne()` or add comment ("zeichne" = "draw")

### [SolverSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SolverSettings.java)
- Fix typo "stepwidth" -> "step width"

### [SubcircuitBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SubcircuitBlock.java)
- Fix typo "enshure" -> "ensure"

### [TerminalFixedPositionInvisible.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalFixedPositionInvisible.java)
- Fix typos "ther" -> "the" and "temperatre" -> "temperature"

### [TerminalRelativePositionReluctance.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalRelativePositionReluctance.java)
- Fix typo `poxX` -> `posX`

### [TerminalSubCircuitBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalSubCircuitBlock.java)
- Fix typo "subcuircuit" -> "subcircuit" in class Javadoc

### [TestReceiverCalculation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TestReceiverCalculation.java)
- Rename misleading variable `thread` to `calculator`

### [ThermPvChip.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermPvChip.java)
- Fix typo `getSwitchngLosses()` -> `getSwitchingLosses()`

### [UniqueObjectIdentifer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/UniqueObjectIdentifer.java)
- Fix class name typo: `Identifer` -> `Identifier`

### [VoltageSourceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceCalculator.java)
- Rename `THREE` and `FOUR` constants to `HISTORY_CURRENT_INDEX` and `HISTORY_VOLTAGE_INDEX`

