### [CoupledInductorsGroup.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CoupledInductorsGroup.java)
- Add Javadoc on class explaining coupled inductor matrix assembly and Cholesky inversion
- Add Javadoc on `stampMatrixA()`, `calculateCurrent()`, `choleskyInverse()`

### [CurrentCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CurrentCalculatable.java)
- Add Javadoc on interface explaining post-solve current calculation contract

### [CurrentMeasurable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CurrentMeasurable.java)
- Add Javadoc to `getCurrentMeasurementComponents(ConnectorType)` explaining return value

### [CurrentSourceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CurrentSourceCalculator.java)
- Add Javadoc on class explaining current source stamping (B-vector only)
- Add Javadoc on `stampVectorB()` explaining source type switch

### [CurrentSourceCircuit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CurrentSourceCircuit.java)
- Add Javadoc on class (leaf class)

### [CurveLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurveLabel.java)
- Add class-level Javadoc explaining label drawing hierarchy

### [CurveLabelRegular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurveLabelRegular.java)
- Add class-level Javadoc explaining regular curve labels

### [CurveLabelSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurveLabelSignal.java)
- Add class-level Javadoc explaining digital signal curve labels

### [CurvePainterRegular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurvePainterRegular.java)
- Add class-level Javadoc describing regular curve painting

### [CurvePainterSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurvePainterSignal.java)
- Add class-level Javadoc describing digital signal curve painting

### [CurvePixelPainterHiLow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurvePixelPainterHiLow.java)
- Add class-level Javadoc explaining hi/low pixel painting

### [CurvePixelPainterPointsLine.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurvePixelPainterPointsLine.java)
- Add class-level Javadoc describing point/line pixel painting

### [CurveRegular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurveRegular.java)
- Add class-level Javadoc explaining regular (analog) curve type

### [CurveSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurveSettings.java)
- Add class-level Javadoc explaining per-curve settings

### [CurveSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CurveSignal.java)
- Add class-level Javadoc explaining digital signal curve type

### [DEMUXCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/DEMUXCalculator.java)
- Add class-level Javadoc explaining demultiplexer (vector to scalar split)
- Add Javadoc on `initializeAtSimulationStart` explaining Java block validation

### [DQABCDCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/DQABCDCalculator.java)
- Add class-level Javadoc explaining DQ-to-ABC (Park inverse) transformation
- Document constants `TWO_THIRD` and `TWO_THIRD`
- Document input indices: [0]=d, [1]=q, [2]=theta

### [DataBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DataBlock.java)
- Add class-level Javadoc explaining block-based data storage
- Add Javadoc to `IndexLimit`/`TimeLimit` nested classes

### [DataContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DataContainer.java)
- **Add Javadoc to all interface methods.** Every method (`getValue`, `getHiLoValue`, `setValue`, `getRowLength`, `getColumnLength`, `setColumn`, `getColumn`, `getTimeIntervalResolution`, `getEstimatedTimeValue`, `getMaximumTimeIndex`) lacks any Javadoc
- **Add `@param` documentation to `insertValuesAtEnd`.** The existing block comment is not a proper Javadoc block (missing `/**`). Convert to standard Javadoc with `@param timeValue` and `@param values` tags

### [DataContainerCompressable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerCompressable.java)
- Add class-level Javadoc explaining lossy compression for data containers

### [DataContainerFourier.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerFourier.java)
- Add class-level Javadoc explaining Fourier-transformed data container

### [DataContainerGlobal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerGlobal.java)
- Add class-level Javadoc explaining global simulation data container

### [DataContainerIntegralCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerIntegralCalculatable.java)
- Add class-level Javadoc explaining integral calculation interface

### [DataContainerManyTimeSeries.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DataContainerManyTimeSeries.java)
- Add class-level Javadoc explaining container for multiple time series

### [DataContainerMeanWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerMeanWrapper.java)
- Add class-level Javadoc explaining mean/average data wrapper

### [DataContainerNullData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerNullData.java)
- Add class-level Javadoc explaining null data container pattern

### [DataContainerScopeWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerScopeWrapper.java)
- Add class-level Javadoc explaining scope data wrapper

### [DataContainerSimple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerSimple.java)
- **Add Javadoc to the class and all public/overridden methods.** Constructor `DataContainerSimple(int rows, int columns)` and methods like `getValue`, `setValue`, `getTimeIntervalResolution`, `getHiLoValue`, `getEstimatedTimeValue`, `insertValuesAtEnd` all lack Javadoc

### [DataContainerTable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerTable.java)
- Add class-level Javadoc explaining tabular data container

### [DataContainerTableModel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerTableModel.java)
- Add class-level Javadoc explaining table model for data container

### [DataContainerValuesSettable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerValuesSettable.java)
- Add class-level Javadoc explaining settable values interface

### [DataIndexItem.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataIndexItem.java)
- Add class-level Javadoc explaining data index entry

### [DataJunk.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataJunk.java)
- Add class-level Javadoc explaining data junk/chunk abstraction

### [DataJunkCompressable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataJunkCompressable.java)
- Add class-level Javadoc explaining compressable data junk
- Add Javadoc to static methods including `setMemoryPrecision()`
- Document relationship between junk size and compression ratio

### [DataJunkSimple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataJunkSimple.java)
- Add class-level Javadoc explaining simple data junk

### [DataLoader.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DataLoader.java)
- Add class-level Javadoc explaining data loading, caching, change detection
- Document the caching strategy

### [DataSaver.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DataSaver.java)
- Add Javadoc on `doManualSave()`, `doManualSaveBlocking()`, `abortSave()`
- Document `WAIT_COUNTER` static AtomicInteger
- Add Javadoc on inner classes `AbstractLinePrinter`, `TxtLinePrinter`, `BinaryLinePrinter`

### [DataTableFrame.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataTableFrame.java)
- Add class-level Javadoc explaining data table frame/window

### [DataTablePanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DataTablePanel.java)
- Add class-level Javadoc explaining the sortable JTable for double-value data entry
- Add Javadoc to `setValues()`, `getCheckedData()`, `clear()`, `clearWithoutEvent()`
- Add Javadoc to `MyTableModel` inner class
- Add Javadoc to `sortWithFirstRow()`, `createNullRow()`

### [DataTablePanelParameters.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DataTablePanelParameters.java)
- Add class-level Javadoc explaining parameter name/value entry panel
- Add Javadoc to constructor explaining `usedParameterNames` map
- Add Javadoc to `getVariableNames()`, `getVariableValues()` explaining `$`-prefix convention
- Document magic column index constants (0=name, 1=value, 2=usage count)
- Document color coding: red = invalid, blue = valid but unused

### [Declaration.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/Declaration.java)
- Add class-level Javadoc explaining this annotation stores the method signature string
- Add Javadoc to `value()`

### [DefinedMeanSignals.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DefinedMeanSignals.java)
- Add class-level Javadoc explaining user-defined mean signals

### [DelayCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/DelayCalculator.java)
- Add class-level Javadoc explaining discrete time delay buffer
- Add Javadoc on `initWithNewDt()` explaining time-step-change resampling

### [DelegateCheckBox.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/DelegateCheckBox.java)
- Add class-level Javadoc describing the purpose (a JCheckBox that acts as an MVC view for a Boolean model)
- Complete `registerModel()` Javadoc: `@param model` has no description, `@param undoRedoText` is missing
- Add Javadoc to `unregisterModel()` explaining it detaches the view from the model
- Add Javadoc to the `_listener` and `_model` fields

### [DelegateIntSpinner.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/DelegateIntSpinner.java)
- Complete class-level Javadoc (currently empty except `@param <M>`) -- describe the JSpinner view for Integer MVC models
- Complete `registerModel()` Javadoc: `@param integer Model` is truncated, `@param undoRedoText` is missing
- Add Javadoc to `getIntegerValue()` explaining it retrieves the current spinner value as an Integer
- Add Javadoc to `unregisterModel()`
- Add Javadoc to `_changeListener` and `_model` fields

### [DelegateNumericTextField.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/DelegateNumericTextField.java)
- Add class-level Javadoc describing the purpose (a JTextField view for Double MVC models)
- Add Javadoc to the constructor explaining the initial "0.0" default and the `@SuppressWarnings("this-escape")`
- Add Javadoc to `_listener` and `_model` fields

### [DetailedConductionLossPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailedConductionLossPanel.java)
- Add class-level Javadoc explaining 2-column editor (U, I)
- Add Javadoc to `useNonlinearInElectric()`
- Remove or document dead fields (`uMaxCOND`, `iMaxCOND`, `b0COND`, `c0COND`, etc.)

### [DetailedLossLookupTable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailedLossLookupTable.java)
- Add class-level Javadoc explaining 2D interpolation table (temperature vs. current)
- Add Javadoc to `fabric()` documenting `dataIndex` (1=Eon, 2=Eoff) and normalization
- Add Javadoc to `getInterpolatedYValue()` and `getInterpolatedXValue()` explaining bilinear interpolation

### [DetailedSwitchingLossesPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailedSwitchingLossesPanel.java)
- Add class-level Javadoc explaining 3-column editor (I, Eon, Eoff)
- Add Javadoc to all overridden methods
- Document magic string `"600"` default voltage

### [DetailledLossPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailledLossPanel.java)
- Add Javadoc to all abstract methods
- Add Javadoc to `baueGUI()`


