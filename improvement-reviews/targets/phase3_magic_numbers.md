# Phase 3: Magic Number and Sentinel Value Extraction

This file lists all the target files and specific tasks for Phase 3: Magic Number and Sentinel Value Extraction parsed from the review files.

Total target files: 70

## File and Task List

### [AbstractBlockInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractBlockInterface.java)
- Add Javadoc to `setAccessibleParameter(String, double)` explaining the forward-compatibility "enabled" hack and magic numbers 0/1/2
- Document the `parameter` array (size 40) and `nameOpt` array (size 40) magic numbers

### [AbstractCachedMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractCachedMatrix.java)
- Add Javadoc to `secondHashCode()` and `hashCode()` explaining the hash algorithms and magic numbers
- Document the lazy-init sentinel value of -1 for `_hashCode` and `_secondHashCode`

### [AbstractDiagram.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractDiagram.java)
- Extract magic numbers for pixel offsets/margins to named constants

### [AbstractLossCalculatorSwitch.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/AbstractLossCalculatorSwitch.java)
- Document `_oldCurrent`/`_oldVoltage` fields and sentinel value `-1`

### [AbstractTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractTerminal.java)
- Document magic numbers `DX_IN=3`, `DX_OUT=3`, `DY_TEXT=-3`, `POINT_DIAMETER=5`, `_pFa=11`, `_pFb=3`

### [AbstractUndoGenericModel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/AbstractUndoGenericModel.java)
- Extract magic number `1000` (undo limit) to a named constant like `UNDO_LIMIT` with documentation

### [Axis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Axis.java)
- Document all magic numbers in pixel calculations

### [AxisGridSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisGridSettings.java)
- Document magic constants `PX1 = 230`, `PX2 = 100`, `PXR = 2.5`

### [AxisLinLog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLinLog.java)
- Document the magic negative sentinel codes `-111111114`/`-111111115` (used for serialization compatibility)

### [BigLUDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigLUDecomposition.java)
- Extract magic number `1e-30` into a named constant such as `PIVOT_THRESHOLD`

### [CachedMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CachedMatrix.java)
- Document threshold magic number `50` in `initLUDecomp()` (when to use sparse vs dense)

### [CallbackClientImpl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/CallbackClientImpl.java)
- Replace magic number `0x64` (100) with named constant

### [ControlSPARSEMATRIX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSPARSEMATRIX.java)
- Extract magic numbers: `3 * dpix` (line 66) and `0.5` (line 71) into named constants; add Javadoc to all public/protected methods

### [ControlSampleHold.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSampleHold.java)
- Extract the magic number `0.5` from the HTML dialog message string into a named constant (e.g., `THRESHOLD = 0.5`) and reference it so the displayed condition stays consistent with the calculator logic

### [ControlSaveData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSaveData.java)
- Extract the magic number `100` (line 177: `while (testCounter < 100)`) into a named constant like `MAX_FILE_COUNTER = 100`

### [ControlSignalSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignalSource.java)
- Extract magic numbers in `drawBlockRectangle`: `1/2.0` (line 234), `0.25` (line 236), `Y_SIZE` position multiplier, and the five hardcoded `drawString` label positions -- refactor the repetitive block (lines 238-247) into a loop over a label array

### [ControlSignalSourceDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignalSourceDialog.java)
- Extract the magic number `7` (line 75: `element.getParameter()[7]`) into a named constant (e.g., `DISPLAY_DETAILS_PARAM_INDEX = 7`) or, preferably, use `element._displayDetails.getValue()` directly for type safety

### [ControlSlidingDFT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSlidingDFT.java)
- Add Javadoc to `setFrequencyDataWithUndoCheck`, `isUndoRequired`, `addDataPoint`, `removeLastFrequencyData`, and `OutputData.getFromIntCode`; also extract magic number `2` in `addDataPoint` (`newFrequencyValue * 2`) into a named constant documenting the doubling convention

### [ControlSlidingDFTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSlidingDFTDialog.java)
- Add Javadoc to `addParameterPanel()`, `addComboBox()`, `addFreqData()`, and `processInputIndividual()`, and document/extract the magic numbers in `new Dimension(20, 3)` (line 143) and `_grid.setColumns(5)` (line 114)

### [ControlSmallSignalAnalysis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSmallSignalAnalysis.java)
- Extract magic numbers in the constructor: `-4`, `3`, `YOUT.size()` terminal position literals into named constants

### [ControlSpaceVector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSpaceVector.java)
- Extract magic numbers in `drawBlockRectangle`: `0.4`, `1.4` (WIDTH), `0.5` (DA_VALUE), `3`, `6`, `2`, and `ds1`/`ds2` into named constants; add Javadoc to `setTerminalNodeLabel`, `drawBlockRectangle`, `exportAsciiIndividual`, and `importIndividual`

### [ControlTERMINAL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTERMINAL.java)
- Document magic number `3.14` in `ControlTerminalCalculator.calculateYOUT()` (looks like placeholder)

### [ControlU_ZI.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlU_ZI.java)
- Document magic numbers `br = 1.4` and `da = 0.4`

### [DataContainerSimple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerSimple.java)
- **Fix magic number in `getTimeIntervalResolution()`** (line 71): `_data[0][2] - _data[0][1]` uses hardcoded indices. Add clarifying comment or named constant explaining this assumes the time row has at least 3 data points

### [DataTablePanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DataTablePanel.java)
- Document `calculateTableHash()` magic numbers (7, 13, 9)

### [DetailedLossLookupTable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailedLossLookupTable.java)
- Document assertion tolerance magic numbers `1.01` and `0.99`

### [DetailledLossPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailledLossPanel.java)
- Document the duplicate-temperature check (0.1 tolerance magic number)

### [DiagramSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DiagramSignal.java)
- Document magic number `DEF_MIN_WIDTH = 30`

### [DialogCircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogCircuitComponent.java)
- Document magic constants `TEXT_FIELD_LENGTH=10`, `NO_TF_COLS=6`, `BUTTON_WIDTH=90`, `BUTTON_HEIGHT=25`

### [DialogLossesDetail.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DialogLossesDetail.java)
- Document magic tab indices `0` and `1`

### [FourierDiagramm.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierDiagramm.java)
- **Extract magic numbers from constructor.** Values like `350`, `300`, `60`, `0.1` (bar width), `1e-6` should be named constants

### [FourierKurvenRekonstruktion.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierKurvenRekonstruktion.java)
- **Extract magic numbers.** Values `350`, `300`, `75`, `30`, `1e99`/`-1e99`, `0.5` should use named constants

### [GeckoMemoryMappedFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoMemoryMappedFile.java)
- Document the magic number `1000` (wait time in `rejectConnection()`)

### [GeckoRemoteObjectTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteObjectTest.java)
- Replace magic number `43035` with named constant
- Replace magic numbers `100` and `2000` with named constants

### [GeckoSim.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoSim.java)
- Extract magic numbers in `performScreenSettings()` (640, 480, 1000, 0.90, 0.80) into named constants

### [GeckoSymbol.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoSymbol.java)
- Document magic numbers for symbol sizes

### [GraferImplementation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferImplementation.java)
- **Extract magic numbers.** Constants `1000` (used in `/ 1000` and `% 1000` throughout), `10000`, `0.6f`, `230`/`100`/`2.5` should be named constants

### [GraferV3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferV3.java)
- **Document the meaning of negative sentinel constant values.** Constants like `AUTO = -111111111`, `ACHSE_LIN = -111111114` use arbitrary large negative numbers with no Javadoc

### [GraferV4.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GraferV4.java)
- Extract magic numbers for pixel offsets, zoom factors into named constants

### [JPanelLossDataInterpolationSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/JPanelLossDataInterpolationSettings.java)
- Document magic defaults `100` (temperature) and `300` (voltage)

### [JavaMemoryRestart.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/JavaMemoryRestart.java)
- Document magic number `MEGA_BYTE = 1098300` (unusual value)

### [LKMatrices.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LKMatrices.java)
- Document `FAST_NULL_R` and `FAST_NULL_L` sentinel constants

### [LUDecompositionCache.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LUDecompositionCache.java)
- Document magic numbers: `MAX_CACHE_SIZE=1000`, `maxJVMMemory / 3`, `maxJVMMemory / 10`
- Document the `1e99` sentinel for oldestTime

### [LossCalculationDetail.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationDetail.java)
- Add Javadoc to `getOldGeckoCIRCUITSOrdinal()` documenting magic numbers 1 and 2

### [MainWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/MainWindow.java)
- Document magic numbers in `processKeyEvents()` (4=CONTROL+D, 18=CONTROL+R, 23)

### [ModelMVCGeneric.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/ModelMVCGeneric.java)
- Extract magic number `1.0` (NaN replacement) to a named constant

### [NativeCDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCDialog.java)
- **Extract magic number `0.3`** (line 204) into a named constant with comment explaining the 30% sizing rationale

### [NiceScale.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NiceScale.java)
- Document magic constants `SEVEN`, `FIVE`

### [NonLinearDialogPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NonLinearDialogPanel.java)
- Document magic-number return codes (-1, 0, 1) with named constants

### [OutputWarningStream.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/OutputWarningStream.java)
- Replace magic numbers (`50000000`, `100000`, `5`, `200`) with named constants

### [Paradiso.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Paradiso.java)
- Convert magic-number `iparm[]` index assignments into named constants

### [PmsmControlCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmControlCalculator.java)
- Document magic number `999e-3` and `60` (RPM to rad/s conversion)

### [Point.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/Point.java)
- Document magic numbers `7` and `89` in `hashCode()`

### [PreviewDialogRectangular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PreviewDialogRectangular.java)
- Add inline comments for magic numbers (b=160, h=110, p1=10, p2=3...)

### [SchematicComponentSelection2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicComponentSelection2.java)
- Document inner classes and magic numbers

### [SchematicEditor2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicEditor2.java)
- Replace magic numbers (350ms double-click, 50ms drag, 10ms sleep) with named constants

### [SimulationKernel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationKernel.java)
- Replace magic numbers: `10000` (max iterations), `0.99`/`0.9999999` (perturbation), `0.5` (switch threshold)

### [SparseMatrixCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SparseMatrixCalculator.java)
- Document magic numbers `LG = 1000` and `d = 12`

### [SpecialType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SpecialType.java)
- Document magic numbers `27` and `70` in enum constants

### [SwitchingLossCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/SwitchingLossCurve.java)
- Add Javadoc to `copy()` explaining deep-copy and sentinel values

### [SymmetricDoubleSparseMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SymmetricDoubleSparseMatrix.java)
- Document magic number `1e-70` (placeholder for Pardiso solver)

### [TerminalControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControl.java)
- Document `paintControlState` and magic numbers (`CIRCLE_DIAMETER = 6`, font size `10`)

### [TerminalControlInputWithLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TerminalControlInputWithLabel.java)
- Document magic numbers in position calculations (`1.75`, `1.2`, `3/4` font scaling)

### [TerminalControlOutput.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControlOutput.java)
- Document magic numbers `0.7` and `0.3` (triangle sizing fractions)

### [TerminalToWrap.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalToWrap.java)
- Document magic numbers in `reCalculateLocation`

### [TextFieldBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TextFieldBlock.java)
- Document magic numbers `1.333` and `3.6`

### [ThermAmbient.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermAmbient.java)
- Document `THERMAL_ZERO` sentinel value (-4711, -4711)

### [TimeSeriesConstantDt.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TimeSeriesConstantDt.java)
- Document magic constants `MAX_DT_CHECK = 1.05` and `ADAPT_THRESHOLD = 100`

### [WorksheetSize.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/WorksheetSize.java)
- Document magic-number constants and the `getOldFormatWSSize` backwards-compat mapping

### [translationtoolbox/TranslationPopupSingle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationPopupSingle.java)
- Document Thread.sleep(500) magic number

