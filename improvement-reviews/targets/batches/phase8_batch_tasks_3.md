### [CapacitorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CapacitorDialog.java)
- Add Javadoc on class

### [CapacitorThermal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CapacitorThermal.java)
- Add Javadoc on class explaining thermal capacitance (heat capacity)

### [Category.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/Category.java)
- Add class-level Javadoc explaining this annotation assigns a `MethodCategory` to remote interface methods
- Add Javadoc to `value()`

### [CharacteristicsCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CharacteristicsCalculator.java)
- Add class-level Javadoc describing calculated oscilloscope characteristics (RMS, mean, etc.)
- Document static mutable cache fields (thread-safety warning)
- Add Javadoc to each characteristic getter with formulas

### [CheckBoxList.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/CheckBoxList.java)
- Add class-level Javadoc explaining JList with checkbox-style toggle selection
- Document inner class `CheckBoxListCellRenderer`

### [CholeskyDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/CholeskyDecomposition.java)
- Fix constructor Javadoc (lines 56-59): the `@return` tag is invalid for constructors
- Remove dead `//package Jama;` comment (line 17)

### [CircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CircuitComponent.java)
- Add Javadoc on class explaining simulation-time calculator base
- Document `var_history` array layout (indices 0-8)
- Document static `disturbanceValue` field

### [CircuitGlobalTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CircuitGlobalTerminal.java)
- Add Javadoc on class

### [CircuitLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitLabel.java)
- Add class-level Javadoc explaining node/terminal label with undo support
- Add Javadoc to `setLabel()` and `setLabelWithoutUndo()` documenting the undo behavior difference
- Add Javadoc to `getLabelPriority()`, `setLabelFromUserDialog()`, `clearPriority()`

### [CircuitSheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitSheet.java)
- Add class-level Javadoc explaining the JPanel representing a circuit schematic sheet
- Add Javadoc to complex methods: `paintComponent()`, `findString()`, `mouseConnectorTest()`, `getConnection()`
- Add Javadoc to `findSubCircuit()` explaining `#` path syntax
- Add Javadoc to `getLocalComponents()` explaining filtering logic
- Document the static `_findNodes` and `_showNodes` Sets

### [CircuitSourceType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitSourceType.java)
- Add class-level Javadoc explaining old/new source type ID mapping
- Add Javadoc to `getFromID()` explaining the two-pass old/new ID search
- Add Javadoc to constructor and enum constants

### [CircuitType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CircuitType.java)
- Add Javadoc on enum explaining each circuit type constant

### [Cispr16Fft.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Cispr16Fft.java)
- Add class-level Javadoc explaining CISPR 16 FFT algorithm
- Remove or document the `System.gc()` call

### [Cispr16Settings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/Cispr16Settings.java)
- Add class-level Javadoc explaining CISPR-16 EMI test receiver configuration

### [CisprBlockSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/CisprBlockSettings.java)
- Add class-level Javadoc explaining CISPR-16 component settings dialog

### [CisprDataExport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/CisprDataExport.java)
- Add class-level Javadoc explaining CISPR-16 data export dialog
- Add Javadoc on `calculateInverseDbMu()`, `doSave()`, `saveData()`

### [Clipping.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Clipping.java)
- Add class-level Javadoc explaining display modes enum

### [CodeWindowModern.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CodeWindowModern.java)
- Add class-level Javadoc explaining the Java code editor window
- Add Javadoc to event handler methods
- Document the lifecycle (create, compile, execute)

### [ColorSettable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ColorSettable.java)
- Add class-level Javadoc describing color setter interface

### [ColorStragegyDisabledComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ColorStragegyDisabledComponent.java)
- Add class-level Javadoc

### [ColorStrategySelected.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ColorStrategySelected.java)
- Add class-level Javadoc explaining selected element color strategy

### [CompileObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompileObject.java)
- Add class-level Javadoc explaining the compile object interface
- Add Javadoc to all interface methods

### [CompileObjectNull.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompileObjectNull.java)
- Add class-level Javadoc explaining the null object pattern for compile objects

### [CompileObjectSavedFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompileObjectSavedFile.java)
- Add class-level Javadoc explaining saved-file compile object

### [CompileScript.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/CompileScript.java)
- Add class-level Javadoc explaining GeckoSCRIPT compilation
- Document the compilation pipeline and error handling

### [CompileStatus.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompileStatus.java)
- **Add Javadoc to the enum and `getFromOrdinal` method**

### [CompiledClassContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompiledClassContainer.java)
- **Add `@param` Javadoc tags** to the two-argument constructor and the `TokenMap` constructor

### [ComplexPrinter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ComplexPrinter.java)
- Add Javadoc on class explaining complex number formatting for UI display

### [ComponentCoupable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ComponentCoupable.java)
- Add Javadoc to interface methods with `@param`/`@return` tags

### [ComponentCoupling.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ComponentCoupling.java)
- Add class-level Javadoc explaining coupling references between components
- Document all fields: `_coupledElements`, `_coupledIdentifiers`, `_coupledIdentifiersBeforeCopy`, etc.
- Add Javadoc to constructor, `setNewCouplingElement()` variants, `refreshCoupledReferences()`, `trySetCopyReference()`
- Add Javadoc to `SetOperation` inner class

### [ComponentDirection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ComponentDirection.java)
- Add class-level Javadoc explaining the four cardinal orientations
- Add Javadoc to constructor explaining legacy `oldOrdinal` values (501-504)
- Add Javadoc to `code()`, `getFromCode()`, `nextOrientation()`, `getDirection()`, `isHorizontal()`

### [ComponentState.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ComponentState.java)
- Add Javadoc to each enum constant (SELECTED, FINISHED)
- Add class-level Javadoc

### [ComponentTerminable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ComponentTerminable.java)
- Add Javadoc to `getAllNodeLabels()` and `getAllTerminals()`
- Add class-level Javadoc

### [CompressedData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/CompressedData.java)
- Add class-level Javadoc explaining data compression strategy

### [CompressorIntMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/CompressorIntMatrix.java)
- Add class-level Javadoc explaining integer matrix compression

### [ConductionLossMeasurementCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/ConductionLossMeasurementCurve.java)
- Add class-level Javadoc (replace informal `// //` comment)
- Add Javadoc to constructor `@param tj`
- Add Javadoc to `copy()` documenting deep-copy behavior

### [Connection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Connection.java)
- Add class-level Javadoc explaining wire/connection between nodes
- Document all fields (`_movementWestEast`, `_isInitialized`, `_subPaths`, `_trimmedCoords`)
- Add Javadoc to `moveHorizontal`, `moveVertical`, `trimCoordinates()`, `paintGeckoComponent()`
- Add Javadoc to `MoveConnectionUndoAction` inner class

### [ConnectionShortConnector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ConnectionShortConnector.java)
- Add class-level Javadoc explaining what a "short connector" is
- Add Javadoc to `getParentCircuitSheet()` override explaining why it bypasses normal parent logic
- Document the `_parentSheet` field

### [ConnectorType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ConnectorType.java)
- Add class-level Javadoc explaining the simulation domain categories (LK, CONTROL, RELUCTANCE, THERMAL)
- Add Javadoc to `fromOrdinal()`, `getDisplayMode()`, `getBackgroundColor()`, `getForeGroundColor()`
- Document each enum constant with domain description

### [ConstantCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ConstantCalculator.java)
- Add class-level Javadoc explaining constant value output
- Add `@param constValue` Javadoc on constructor

### [ConstantExpression.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/ConstantExpression.java)
- Add class-level Javadoc explaining the purpose of `ConstantExpression`

### [ContainerStatus.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ContainerStatus.java)
- Add Javadoc to enum explaining container status values

### [ContainsMatcher.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/ContainsMatcher.java)
- Add class-level Javadoc explaining substring matching

### [ControlABCDQ.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlABCDQ.java)
- Add class-level Javadoc explaining ABC to DQ (Park/Clarke) transformation

### [ControlAbsolutValue.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAbsolutValue.java)
- Add class-level Javadoc explaining absolute value computation

### [ControlAdd.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAdd.java)
- Add class-level Javadoc explaining summation of all input signals

### [ControlAmpereMeterDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAmpereMeterDialog.java)
- Add class-level Javadoc explaining amperemeter/flowmeter dialog

### [ControlAmperemeter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAmperemeter.java)
- Add class-level Javadoc explaining electrical current measurement
- Add Javadoc on `checkComponentCompatibility()`

### [ControlAnd.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAnd.java)
- Add class-level Javadoc explaining logical AND over all inputs

### [ControlAreaCosine.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAreaCosine.java)
- Add class-level Javadoc explaining arc cosine computation


