# Phase 8: Javadoc and Code Documentation

This file lists all the target files and specific tasks for Phase 8: Javadoc and Code Documentation parsed from the review files.

Total target files: 772

## File and Task List

### [ABCDQCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ABCDQCalculator.java)
- Add class-level Javadoc explaining ABC-to-DQ (Park) transformation
- Document constants and input indices: [0]=a, [1]=b, [2]=c, [3]=theta

### [ACosCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ACosCalculator.java)
- Add class-level Javadoc: "Calculates arc cosine"

### [ASinCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ASinCalculator.java)
- Add class-level Javadoc: "Calculates arc sine"

### [AStampable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AStampable.java)
- Add Javadoc on interface explaining A-matrix stamping contract
- Add Javadoc on `stampMatrixA()` and `isBasisStampable()`

### [ATanCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ATanCalculator.java)
- Add class-level Javadoc: "Calculates arc tangent"

### [AbsCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbsCalculator.java)
- Add class-level Javadoc: "Calculates the absolute value"

### [AbstractBlockInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractBlockInterface.java)
- Add class-level Javadoc explaining the base abstract class for all circuit block components
- Add Javadoc to `getShortConnectors()`, `clickedTerminal(Point)`, `paintShortCircuitConnections()`
- Add Javadoc to `copyLKBlockPars(AbstractBlockInterface)` explaining what "LK Block Pars" means

### [AbstractCachedMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractCachedMatrix.java)
- Add class-level Javadoc explaining the base class for cached LU-decomposed matrices
- Add Javadoc to `equals()`, `setAccess()`, `getAccessCounter()`, `getLatestAccessTime()`
- Add Javadoc to abstract methods `initLUDecomp()`, `deleteCache()`, `solve()`, `calculateMemoryRequirement()`

### [AbstractCapacitor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCapacitor.java)
- Add Javadoc on class explaining nonlinear capacitance support and parameter[] array layout
- Document `returnValue` always being `false` in `updateNonlinearCapacitances()` or remove

### [AbstractCircuitBlockInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitBlockInterface.java)
- Add Javadoc on class explaining base class for all schematic circuit elements
- Add Javadoc on abstract methods `drawForeground()` and `drawConnectorLines()`

### [AbstractCircuitGlobalTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitGlobalTerminal.java)
- Add Javadoc on class explaining "global terminal" concept
- Add Javadoc on `fabric()` explaining reflective instantiation

### [AbstractCircuitSheetComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractCircuitSheetComponent.java)
- Add class-level Javadoc explaining the base class for circuit sheet components
- Add Javadoc to `deleteComponent()`, `importASCII()`, `exportASCII()`, `getParentCircuitSheet()`, `setParentCircuitSheet()`
- Add Javadoc to `shiftAllIdentifiers(long)` and `allParentSubcircuitsEnabled()`

### [AbstractCircuitSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitSource.java)
- Add Javadoc on class explaining source-type pattern
- Document parameter[] array layout (indices 0-20)
- Add Javadoc on abstract methods

### [AbstractCircuitSourceDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitSourceDialog.java)
- Add Javadoc on class explaining tabbed dialog structure
- Add Javadoc on `baueGUIIndividual()` and `processInputIndividual()`

### [AbstractCircuitTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitTerminal.java)
- Add Javadoc on class and `fabric()` method

### [AbstractCircuitTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitTypeInfo.java)
- Add Javadoc on class and `fabric()` method

### [AbstractCompileObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/AbstractCompileObject.java)
- Add class-level Javadoc explaining compile object base class
- Add Javadoc to all methods

### [AbstractComponentType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/AbstractComponentType.java)
- Add class-level Javadoc explaining the component type enumeration

### [AbstractControlCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractControlCalculatable.java)
- Add Javadoc on `SIGNAL_THRESHOLD` (what is 0.5 threshold)
- Add Javadoc on `_time` static field (global simulation time, thread-safety concern)
- Add Javadoc on `_inputSignal` and `_outputSignal` explaining `double[][]` structure
- Add `@param` Javadoc on constructor
- Add Javadoc on `calculateYOUT(double deltaT)`, `setInputSignal()`, `createOutputSignal(int)`
- Document implications of `setTime` mutating a public static field

### [AbstractControlOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractControlOrderer.java)
- Add Javadoc on class explaining topological sorting of control blocks
- Document `MAX_ITERATION_COUNT` constant (why 10000)
- Add Javadoc on abstract methods

### [AbstractControlPT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractControlPT.java)
- Add class-level Javadoc explaining base class for PT1/PT2 transfer functions
- Add Javadoc on `_TVal` and `_a1Val` UserParameters

### [AbstractControlSingleInputSingleOutput.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractControlSingleInputSingleOutput.java)
- Add class-level Javadoc explaining 1-input/1-output base class

### [AbstractControlVariableInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractControlVariableInputs.java)
- Add Javadoc on `setInputTerminalNumber()`, `setOutputTerminalNumber()`
- Document `_inputTerminalNumber` UserParameter field

### [AbstractCurrentMeasurement.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractCurrentMeasurement.java)
- Add class-level Javadoc explaining current measurement from coupled component
- Add Javadoc on inner classes `CurrentCalculation`, `MOSFETCurrentCalculation`

### [AbstractCurrentSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractCurrentSource.java)
- Add Javadoc on class, document drawing constants
- Add Javadoc on `getCircuitCalculatorsForSimulationStart()`

### [AbstractCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractCurve.java)
- Add class-level Javadoc describing the curve hierarchy base class
- Document thread-safety hazard: `static final GeneralPath GPATH` is shared mutable

### [AbstractCurvePainter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractCurvePainter.java)
- Add class-level Javadoc explaining the painter abstraction
- Add method-level Javadoc for all public/protected methods

### [AbstractCurvePixelPainter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractCurvePixelPainter.java)
- Add class-level Javadoc describing pixel-level painting strategy

### [AbstractDataContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/AbstractDataContainer.java)
- Add class-level Javadoc explaining base data container
- Add Javadoc to all abstract methods

### [AbstractDataContainerSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/AbstractDataContainerSignal.java)
- Add class-level Javadoc explaining signal data container base

### [AbstractDiagram.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractDiagram.java)
- Add class-level Javadoc (~500 lines, complex) describing diagram lifecycle, axis management, mouse interaction
- Document role of `_xAxis`, `_yAxis1`, `_yAxis2` (why exactly two Y axes)
- Add Javadoc to mouse listener methods explaining zoom/pan behavior

### [AbstractDialogPowerSwitch.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractDialogPowerSwitch.java)
- Add Javadoc on class, `createParameterPanel()`, `baueGUIIndividual()`

### [AbstractDialogWithExternalOption.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractDialogWithExternalOption.java)
- Add class-level Javadoc explaining "Use external parameters" checkbox dialog
- Add Javadoc on `getComponentsDisabledExternal()`

### [AbstractExpression.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/AbstractExpression.java)
- Add class-level Javadoc explaining the abstract base class role

### [AbstractGeckoCustom.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/AbstractGeckoCustom.java)
- Add class-level Javadoc explaining abstract base for custom Gecko integrations
- Add `@param`/`@return` tags throughout

### [AbstractInductor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractInductor.java)
- Add Javadoc on class explaining nonlinear inductance
- Document parameter[] array layout
- Add Javadoc on `getStartInductance()`, `doCalculation()`

### [AbstractInversTrigFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractInversTrigFunction.java)
- Add class-level Javadoc explaining base for inverse trig functions

### [AbstractJavaBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/AbstractJavaBlock.java)
- Add class-level Javadoc explaining base class for Java-based control blocks
- Add Javadoc to all public/protected methods

### [AbstractLossCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/AbstractLossCalculator.java)
- Fix stale `@param state` in `calcLosses()` Javadoc (no such parameter exists)
- Remove erroneous `@return` tag on `void calcLosses()` method
- Add Javadoc to the interface explaining the two-method contract
- Add `@param deltaT` unit documentation (seconds)

### [AbstractLossCalculatorFabric.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/AbstractLossCalculatorFabric.java)
- Add Javadoc to the interface explaining the factory contract
- Add Javadoc to `lossCalculatorFabric()` documenting return value

### [AbstractLossCalculatorSwitch.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/AbstractLossCalculatorSwitch.java)
- Add Javadoc to abstract class explaining switch/semiconductor loss calculation
- Add Javadoc to `calcLosses()`, `detectTurnOn()`, `detectTurnOff()`
- Add Javadoc to abstract methods `calcConductionLoss()`, `calcTurnOnSwitchingLoss()`, `calcTurnOffSwitchingLoss()`
- Document `EPS = 1e-2` constant (practically zero current threshold)

### [AbstractMotor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractMotor.java)
- Add Javadoc on class explaining motor simulation pattern (hidden subcircuit + mechanical equations)
- Document parameter[] array layout shared across subclasses
- Add Javadoc on `doCalculation()`, `calculateMechanicalParameters()`, `fabricHiddenSub()`

### [AbstractMotorDC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractMotorDC.java)
- Add Javadoc on class explaining DC motor subcircuit topology
- Add inline comment on `_anchorCurrent` ("Ankerstrom" = armature current)
- Add Javadoc on `calculateMotorEquations()`, `setSubCircuit()`, `updateSourceParameters()`

### [AbstractMotorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractMotorDialog.java)
- Add Javadoc on class, `baueGUIIndividual()`, `createLossButton()`

### [AbstractMotorIM.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractMotorIM.java)
- Add Javadoc on class explaining induction machine model (dq-axis flux model)

### [AbstractMotorIMCommon.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractMotorIMCommon.java)
- Add Javadoc on class explaining shared IM parameters
- Add Javadoc on abstract methods

### [AbstractMotorSM.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractMotorSM.java)
- Add Javadoc on class explaining synchronous machine model

### [AbstractNonLinearCircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractNonLinearCircuitComponent.java)
- Add Javadoc on interpolation methods `getActualValueLINFromLinearizedCharacteristic()`, `getActualValueLOGFromLinearizedCharacteristic()`

### [AbstractPTCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractPTCalculator.java)
- Add class-level Javadoc explaining "PT" = proportional-integral time-delay element
- Add Javadoc on `_TVal` and `_a1Val` fields (T = time constant, a1 = gain)
- Remove stale IDE template comment

### [AbstractPotentialMeasurement.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractPotentialMeasurement.java)
- Add class-level Javadoc explaining potential (voltage) measurement
- Add Javadoc on `checkComponentCompatibility()`, `addTextInfoParameters()`

### [AbstractResistor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractResistor.java)
- Add Javadoc on class explaining multi-domain resistance pattern
- Add Javadoc on `getLossCalculation()`

### [AbstractScopeSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractScopeSignal.java)
- Add class-level Javadoc explaining base signal abstraction
- Add Javadoc to all abstract methods

### [AbstractSemiconductor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractSemiconductor.java)
- Add Javadoc on class explaining semiconductor base
- Document parameter[] indices (2,3 = R_ON/R_OFF, etc.) as named constants
- Add Javadoc on `getFiles()`, `addFiles()`, `getOperationEnumInterfaces()`

### [AbstractSignalCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractSignalCalculator.java)
- Add class-level Javadoc explaining signal-source calculator base
- Add Javadoc on `TWO_PI` constant

### [AbstractSignalCalculatorPeriodic.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractSignalCalculatorPeriodic.java)
- Add class-level Javadoc explaining periodic signal generation
- Add Javadoc on `initializeAtSimulationStart` explaining THOUSAND-step discretization
- Add Javadoc on `calculatePhaseX()` explaining phase normalization

### [AbstractSingleInputSingleOutputCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractSingleInputSingleOutputCalculator.java)
- Add class-level Javadoc explaining 1-input/1-output calculator base

### [AbstractSinkControlOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractSinkControlOrderer.java)
- Add class-level Javadoc explaining orderer starting from sink blocks

### [AbstractSourceControlOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractSourceControlOrderer.java)
- Add class-level Javadoc explaining orderer starting from source blocks

### [AbstractSwitch.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractSwitch.java)
- Add Javadoc on class, `_connectedGateBlock` field
- Add Javadoc on `doReferenceAddAction()`, `doReferenceRemoveAction()`, `addGateTextInfo()`

### [AbstractSwitchCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractSwitchCalculator.java)
- Add Javadoc on class explaining variable-resistance switch stamping
- Document static fields `switchAction` and `switchActionOccurred` (global error-correction)

### [AbstractTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractTerminal.java)
- Add class-level Javadoc explaining the base abstract terminal
- Add Javadoc to `paintComponent()`, `paintLabelString()`, `createCopy()`, `getCategory()`

### [AbstractThreePhaseMotor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractThreePhaseMotor.java)
- Add Javadoc on class explaining three-phase terminal layout

### [AbstractTimeSerie.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AbstractTimeSerie.java)
- Add Javadoc to all methods

### [AbstractTrigonometricFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractTrigonometricFunction.java)
- Add class-level Javadoc explaining base for trig functions

### [AbstractTwoInputsOneOutputCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractTwoInputsOneOutputCalculator.java)
- Add class-level Javadoc explaining 2-input/1-output calculator base

### [AbstractTwoPortPowerCircuitBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractTwoPortPowerCircuitBlock.java)
- Add Javadoc on class explaining standard two-port topology
- Add Javadoc on `createTwoPortTerminals()`

### [AbstractTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractTypeInfo.java)
- Add class-level Javadoc explaining the abstract type info/metadata registry
- Document all static maps (`_classEnumMap`, `_classTypeMap`, `_stringTypeMap`, `_enumTypeMap`)
- Add Javadoc to `getTypeFromEnum()`, `getFromComponentName()`, `getTypeFromString()` (document difference: RuntimeException vs IllegalArgumentException)
- Add Javadoc to constructor explaining side effects (registers into static maps)
- Add Javadoc to `doConsistencyCheck()`, `addParentEnum()`, `fabric()`, factory methods

### [AbstractUndoGenericModel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/AbstractUndoGenericModel.java)
- Add Javadoc to the constructor `AbstractUndoGenericModel(T initValue)` which has an empty `/** */` comment
- Add Javadoc to `_initialized` field explaining it prevents undo edits during the first `setValue` call
- Add Javadoc to `setValueWithoutUndo()` explaining its purpose and when to use it
- Complete the empty `@param arg0` and `@return` tags in `addEdit()` Javadoc
- Complete the empty `@param arg0` tag in `replaceEdit()` Javadoc
- Complete the empty `@return` tags in `getUndoPresentationName()` and `getRedoPresentationName()`
- Add Javadoc to the `UndoableAction` inner class fields
- Document or remove the `@SuppressWarnings("PMD")` annotation on `die()`

### [AbstractVoltageDropSwitch.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractVoltageDropSwitch.java)
- Add Javadoc on class and `getForwardVoltageDropParameter()`

### [AbstractVoltageSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractVoltageSource.java)
- Add Javadoc on class
- Add Javadoc on `drawPlusSymbol()`, `drawMinusSymbol()`, `getDCValueShortNameFromDomain()`

### [AbstractVoltageSourceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractVoltageSourceCalculator.java)
- Add Javadoc on class explaining MNA voltage source stamping with `_z` auxiliary variable
- Add Javadoc on `stampMatrixA()`

### [AbstractVoltageSourceControlledCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractVoltageSourceControlledCalculator.java)
- Add Javadoc on class explaining controlled voltage sources
- Add Javadoc on `setGain()`, `setCurrentControlComponent()`
- Add missing license/file header comment

### [AndMultiInputCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AndMultiInputCalculator.java)
- Add `@param inputNumber` Javadoc on constructor
- Add Javadoc on `calculateYOUT` explaining multi-input AND logic

### [AndTwoPortCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AndTwoPortCalculator.java)
- Add Javadoc on `calculateYOUT` (note: "TwoPort" is really "TwoInput")

### [ArrowIcon.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ArrowIcon.java)
- Add class-level Javadoc explaining the arrow icon UI component

### [AverageValue.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/AverageValue.java)
- Add class-level Javadoc explaining running average calculation

### [Axis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Axis.java)
- Add class-level Javadoc (~700 lines) describing pixel calculation, tick/grid drawing
- Add Javadoc to pixel-to-data conversion methods

### [AxisConnection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisConnection.java)
- Add Javadoc to `iterateNext` and `getFromOrdinal`

### [AxisDesignSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisDesignSettings.java)
- Add class-level Javadoc explaining axis design settings

### [AxisGridSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisGridSettings.java)
- Add class-level Javadoc describing grid visibility logic

### [AxisLimits.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLimits.java)
- Add class-level Javadoc describing global vs local autoscale, zoom history
- Add Javadoc to `getLimits()` documenting full decision tree

### [AxisLinLog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLinLog.java)
- Add Javadoc to enum explaining linear vs logarithmic axis scaling selection
- Add Javadoc to `getFromCode()` explaining it defaults to `ACHSE_LIN` for unknown codes

### [AxisTickSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisTickSettings.java)
- Add class-level Javadoc describing tick mark configuration

### [BJT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/BJT.java)
- Add Javadoc on class explaining BJT model

### [BJTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/BJTDialog.java)
- Add Javadoc on class

### [BStampable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/BStampable.java)
- Add Javadoc on interface explaining B-vector stamping contract
- Add Javadoc on all methods

### [BVector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/BVector.java)
- Add Javadoc on class explaining optimization: caching basis-stampable components
- Add Javadoc on `stampBVector()`, `setUpdateAllFlag()`, `copy()`, `registerBVector()`

### [BigLUDecomposition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigLUDecomposition.java)
- Fix constructor Javadoc (lines 59-62): the `@return` tag is invalid for constructors

### [BigMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigMatrix.java)
- Remove dead `//package Jama;` and `//import Jama.util.*;` comments (lines 17, 27)

### [BlockOrderOptimizer3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/BlockOrderOptimizer3.java)
- Add class-level Javadoc explaining priority-based block ordering algorithm

### [BodePlot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/BodePlot.java)
- Add class-level Javadoc explaining Bode plot diagram type
- Add `@Deprecated` Javadoc explaining why deprecated and replacement

### [BodePlot2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/BodePlot2.java)
- Add class-level Javadoc explaining Bode plot and differences from `BodePlot`
- Document `@SuppressWarnings("deprecation")` rationale

### [CBLAS.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/com/intel/mkl/CBLAS.java)
- Add class-level Javadoc noting this class uses native methods directly
- Add `@param` documentation to `sgemm()`, `dgemm()`, `cgemm()`, `zgemm()` which only have one-line descriptions
- Add `@param` documentation to `sdot()`, `ddot()`, `cdotc_sub()`, `zdotc_sub()`, `cdotu_sub()`, `zdotu_sub()` which only have one-line descriptions
- Add full Javadoc to `ddnscsr()` which has no documentation at all
- Add full Javadoc to `dcsrmm()`, `scsrmm()`, `zcsrmm()` which have no documentation
- Fix `ccsrmm()` Javadoc: all `@param` tags are empty (just parameter names, no descriptions)
- Add Javadoc to `dnrm2()` which has no documentation and uses unclear parameter names
- Add Javadoc to `dfgmresInit()` which has no documentation and unclear parameters
- Add Javadoc to `dcsrgemv2()` which has no documentation
- Add Javadoc to `dcsrgemv()` which has no documentation
- Add Javadoc to `dfgmres()` which has no documentation
- Add Javadoc to `dfgmresCheck()` which has no documentation
- Add Javadoc to `dfgmresGet()` which has no documentation

### [CachedMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CachedMatrix.java)
- Add class-level Javadoc explaining LU decomposition with sparse optimization
- Add Javadoc to `doLUDecomposition()`, `doLUDecompositionSparse()`, `solve()` explaining the algorithms
- Add Javadoc to `calculateLowerSparseLUDecompositionIndices()`, `calculateUpperSparseLUDecompositionIndices()`
- Add Javadoc to `calculateMemoryRequirement()` explaining formula `2 * n * n * Double.SIZE / 8`

### [CallBackTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/CallBackTest.java)
- Add class-level Javadoc explaining the purpose (a test/demo for JavaScript engine callback functionality)
- Add Javadoc to the `main()` method explaining what the test does
- Add Javadoc to the `invoke()` method documenting `@param test` and `@throws ScriptException`
- Add Javadoc to the `engine` field explaining it is a shared JavaScript script engine
- Add Javadoc to the `mgr` field explaining it is the script engine manager

### [CallbackClientImpl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/CallbackClientImpl.java)
- Add Javadoc to the constructor explaining client hostname, user ID, connection timestamp
- Add Javadoc to `printSystemMessage(String)` and `printErrorMessage(String)`
- Add Javadoc to `ping()` documenting returned info
- Add field-level Javadoc for `_clientHostname`, `_clientUserID`, `_connectionDate`

### [CallbackClientInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/CallbackClientInterface.java)
- Add class-level Javadoc explaining this is the RMI callback interface
- Convert non-Javadoc comments into proper `@param` Javadoc tags
- Add `@throws java.rmi.RemoteException` Javadoc tags to all methods

### [CapacitanceCharacteristic.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CapacitanceCharacteristic.java)
- Add Javadoc on class explaining nonlinear capacitance interpolation
- Add Javadoc on `getCapacitanceAtV()`

### [CapacitorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CapacitorCalculator.java)
- Add Javadoc on class explaining capacitor MNA stamping and three solver types
- Add Javadoc on `stampMatrixA()`, `stampVectorB()`
- Document static fields `initCapacitor` and `capError`
- Add Javadoc on `updateNonLinearCapacitance()` explaining 10% deviation threshold

### [CapacitorCircuit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CapacitorCircuit.java)
- Add Javadoc on class (leaf class -- TYPE_INFO registration)

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

### [ControlAreaSine.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAreaSine.java)
- Add class-level Javadoc explaining arc sine computation

### [ControlAreaTangens.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlAreaTangens.java)
- Add class-level Javadoc explaining arc tangent computation

### [ControlBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlBlock.java)
- Add class-level Javadoc explaining abstract base class for all control blocks
- Document all visual layout fields (`pFa`, `pFb`, `xFl`, `yFl`, etc.)
- Add Javadoc on constants `EMPTY_OUTPUT`, `SIGNAL_THRESHOLD`, `DISP_DIGITS`, `WIDTH`

### [ControlBlockSimulink.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlBlockSimulink.java)
- Add class-level Javadoc explaining Simulink-interface control blocks base

### [ControlCISPR16.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlCISPR16.java)
- Add class-level Javadoc explaining CISPR-16 EMI test receiver implementation
- Document `DA_OFFSET` and `DI_OFFSET` constants
- Add Javadoc on `CisprCalculator` inner class

### [ControlCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/ControlCalculatable.java)
- Add Javadoc to `calculateYOUT(double[], double, double)` explaining parameters `xIN`, `time`, `deltaT`
- Add Javadoc to `init()` explaining when it is called in the simulation lifecycle

### [ControlCalculatableMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/ControlCalculatableMatrix.java)
- Add Javadoc to `calculateYOUT(double[][], double, double)` explaining the matrix-based calculation
- Add Javadoc to `init()` and `getOutputSignal()`

### [ControlComponentType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlComponentType.java)
- Add class-level Javadoc explaining integer type ID to component type mapping
- Document non-sequential integer IDs and gaps

### [ControlConstant.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlConstant.java)
- Add class-level Javadoc explaining constant value output

### [ControlConstantDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlConstantDialog.java)
- Add class-level Javadoc explaining constant value parameter dialog

### [ControlCosine.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlCosine.java)
- Add class-level Javadoc explaining cosine computation

### [ControlCounter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlCounter.java)
- Add class-level Javadoc explaining rising-edge counter

### [ControlDQABC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlDQABC.java)
- Add class-level Javadoc explaining DQ to ABC (inverse Park/Clarke) transformation

### [ControlDebugWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlDebugWindow.java)
- Document purpose of each jButton

### [ControlDelay.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlDelay.java)
- Add class-level Javadoc explaining signal delay block
- Add Javadoc on `_tDelay` UserParameter and `DEFAULT_DELAY` constant

### [ControlDelayDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlDelayDialog.java)
- Add class-level Javadoc explaining delay time parameter dialog

### [ControlDemux.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlDemux.java)
- Add class-level Javadoc explaining demultiplexer
- Remove auto-generated comment "To change body of generated methods"

### [ControlDivision.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlDivision.java)
- Add class-level Javadoc explaining division with division-by-zero warning

### [ControlEqual.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlEqual.java)
- Add class-level Javadoc explaining equality check

### [ControlExclusiveOr.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlExclusiveOr.java)
- Add class-level Javadoc explaining logical XOR

### [ControlExponential.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlExponential.java)
- Add class-level Javadoc explaining e^x computation

### [ControlFlowMeter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlFlowMeter.java)
- Add class-level Javadoc explaining heat flow/power measurement from thermal components
- Add Javadoc on `_measurementType` field and `LossComponent` enum

### [ControlFluxMeter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlFluxMeter.java)
- Add class-level Javadoc explaining magnetic flux measurement

### [ControlFromEXTERNAL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlFromEXTERNAL.java)
- Add class-level Javadoc explaining signal import from external (Simulink) interface
- Document `fromExternals` static list and lifecycle management

### [ControlGain.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGain.java)
- Add class-level Javadoc explaining gain multiplication
- Add Javadoc on `_gain` UserParameter

### [ControlGainDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGainDialog.java)
- Add class-level Javadoc explaining gain parameter dialog

### [ControlGate.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGate.java)
- Add class-level Javadoc explaining gate signal control for switch components

### [ControlGateDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGateDialog.java)
- Add class-level Javadoc explaining switch selection dialog

### [ControlGlobalTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGlobalTerminal.java)
- Add class-level Javadoc explaining cross-subcircuit control terminal
- Document `ALL_GLOBALS` static set

### [ControlGreaterEqual.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGreaterEqual.java)
- Add class-level Javadoc explaining >= comparison

### [ControlGreaterThan.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGreaterThan.java)
- Add class-level Javadoc explaining > comparison

### [ControlHysteresis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlHysteresis.java)
- Add class-level Javadoc explaining hysteresis comparator
- Document `DEF_HYS_THRES`, `_stashedTerminal` field

### [ControlHysteresisDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlHysteresisDialog.java)
- Add class-level Javadoc explaining hysteresis configuration dialog
- Document `IMAGE_COMPONENT_WIDTH` and `IMAGE_COMPONENT_HEIGHT` constants

### [ControlImportDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlImportDialog.java)
- Add class-level Javadoc explaining data import dialog

### [ControlImportFromFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlImportFromFile.java)
- Add class-level Javadoc explaining file-based signal source

### [ControlInputTwoTerminalStateable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlInputTwoTerminalStateable.java)
- Add class-level Javadoc explaining folded/expanded terminal state interface

### [ControlIntegrator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlIntegrator.java)
- Add class-level Javadoc explaining integrator with limits
- Add Javadoc on `_a1Val`, `_y0Val`, `_minLimit`, `_maxLimit` UserParameters

### [ControlIntegratorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlIntegratorDialog.java)
- Add class-level Javadoc explaining integrator configuration dialog

### [ControlJavaFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/ControlJavaFunction.java)
- Add class-level Javadoc explaining the Java function control block

### [ControlJavaTriangles.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/ControlJavaTriangles.java)
- **Add Javadoc to `isIncreaseClicked` and `isDecreaseClicked` methods**, including `@param` and `@return` tags

### [ControlLN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlLN.java)
- Add class-level Javadoc explaining natural logarithm computation

### [ControlLimit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlLimit.java)
- Add class-level Javadoc explaining signal limiter
- Document all layout constants

### [ControlLimitDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlLimitDialog.java)
- Add class-level Javadoc explaining limiter configuration dialog
- Fix duplicated `//CHECKSTYLE:OFF` comment (should be `//CHECKSTYLE:ON`)

### [ControlMAX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMAX.java)
- Add class-level Javadoc explaining maximum of all inputs

### [ControlMIN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMIN.java)
- Add class-level Javadoc explaining minimum of all inputs

### [ControlMMF.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMMF.java)
- Add class-level Javadoc explaining magnetomotive force measurement

### [ControlMUL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMUL.java)
- Add class-level Javadoc explaining multiplication of all inputs
- Add Javadoc on `TwoParameterMultiplication` and `MoreParameterMultiplication` inner classes

### [ControlMUX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlMUX.java)
- Add class-level Javadoc explaining multiplexer

### [ControlNE.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlNE.java)
- Add class-level Javadoc explaining not-equal check

### [ControlNOT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlNOT.java)
- Add class-level Javadoc explaining logical NOT

### [ControlNativeC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/ControlNativeC.java)
- **Add Javadoc to the `CCalculator` inner class** and to methods like `convertString2List`, `convertList2String`, `loadUserData`, `triggerUpdate`

### [ControlOSZI.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlOSZI.java)
- Add class-level Javadoc explaining oscilloscope component
- Document all waveform/Fourier fields
- Add Javadoc on `initScope()`, `istAngeklickt()`, `copyFabric()`

### [ControlOr.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlOr.java)
- Add class-level Javadoc explaining logical OR

### [ControlOrderNode.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlOrderNode.java)
- Add Javadoc on `calculateDirectInputs()`, `calculateDirectOutputs()`
- Add Javadoc on `setLoopCrackTrue()`, `getLoopCrack()` explaining "loop crack"
- Document `_loopCrack` field

### [ControlPD.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPD.java)
- Add class-level Javadoc explaining PD controller: G(s) = a1*s

### [ControlPDDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPDDialog.java)
- Add class-level Javadoc explaining PD controller parameter dialog

### [ControlPI.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPI.java)
- Add class-level Javadoc explaining PI controller
- Add Javadoc on `_r0`, `_a1`, `_TimeConstant` UserParameters and relationship (T = r0/a1)

### [ControlPIDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPIDialog.java)
- Add class-level Javadoc explaining PI parameter dialog with live cross-computation

### [ControlPMSMCONTROL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPMSMCONTROL.java)
- Add class-level Javadoc explaining PMSM field-oriented controller

### [ControlPMSM_Modulator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPMSM_Modulator.java)
- Add class-level Javadoc explaining PMSM modulator

### [ControlPOW.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPOW.java)
- Add class-level Javadoc explaining power computation

### [ControlPT1.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPT1.java)
- Add class-level Javadoc explaining PT1: G(s) = a1/(1+s*T)

### [ControlPT2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPT2.java)
- Add class-level Javadoc explaining PT2: G(s) = a1/(1+s*T)^2

### [ControlPTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPTDialog.java)
- Add class-level Javadoc explaining PT1/PT2 parameter dialog

### [ControlRandomDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlRandomDialog.java)
- Add Javadoc description to the class (currently only `@author andy` with no summary), documenting that this dialog provides a simple info label for the Random signal source block
- Add Javadoc to the constructor `ControlRandomDialog(ControlSignalSource element)` explaining the `element` parameter and that it delegates to the superclass

### [ControlRandomWalk.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlRandomWalk.java)
- Add a Javadoc class description explaining that this block produces a random-walk signal by delegating to `ControlSignalSource` with the `QUELLE_RANDOM` source type
- Add Javadoc to the constructor documenting that it sets the source type to `QUELLE_RANDOM` via `setValueWithoutUndo`
- Add Javadoc to the `tinfo` field documenting its role as the registration metadata for the control framework

### [ControlRound.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlRound.java)
- Add a Javadoc class description documenting that this control block rounds its input to the nearest integer, using `RoundCalculator`
- Add Javadoc to `getOutputNames()`, `getInternalControlCalculatableForSimulationStart()`, `getCenteredDrawString()`, `openDialogWindow()`, and `getOutputDescription()` with `@return` and `@inheritDoc` tags as appropriate
- Add Javadoc to the `tinfo` field documenting its purpose in the control type registry

### [ControlSIN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSIN.java)
- Add a Javadoc class description documenting that this block computes the sine of its input angle (trigonometric function block)
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` and the `tinfo` field
- Consider adding `@Override` `getOutputNames()`/`getOutputDescription()` if they exist in the parent (they are inherited from `AbstractTrigonometricFunction` -- verify and add Javadoc or `@inheritDoc`)

### [ControlSPARSEMATRIX.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSPARSEMATRIX.java)
- Add a Javadoc class description documenting this block implements Sparse Matrix converter control logic

### [ControlSQR.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSQR.java)
- Add a Javadoc class description documenting that this block computes the square of its input (y = x^2)
- Add Javadoc to `getOutputNames()`, `getInternalControlCalculatableForSimulationStart()`, `openDialogWindow()`, and `getOutputDescription()` with appropriate `@return` and `@param` tags
- Add Javadoc to the `TYPE_INFO` field documenting its role in the control type registry

### [ControlSQRT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSQRT.java)
- Add a Javadoc class description documenting that this block computes the square root of its input (y = sqrt(x))
- Add Javadoc to `getOutputNames()`, `getInternalControlCalculatableForSimulationStart()`, `openDialogWindow()`, and `getOutputDescription()`
- Add Javadoc to the `tinfo` field documenting its purpose in the control type registry

### [ControlSampleHold.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSampleHold.java)
- Add a Javadoc class description documenting the sample-and-hold behavior: when control input z > 0.5 it samples x1; otherwise it holds the last sampled value
- Add Javadoc to the constructor `super(2, 1)` clarifying the meaning of the two arguments (2 inputs, 1 output)
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` and `openDialogWindow()`

### [ControlSaveData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSaveData.java)
- Add a Javadoc class description documenting this block's role: it exports simulation data to a file (text or binary), with configurable formatting, signal selection, and save modes
- Add Javadoc to `setSelectedSignal(int j, int i)` whose parameter names are ambiguous (`j` is the signal index value, `i` is the list position) -- clarify via `@param` tags or better parameter names

### [ControlSignalSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignalSource.java)
- Add a Javadoc class description documenting this as the primary signal source block supporting sine, triangle, rectangle, random, and file-imported waveforms with optional external terminals
- Add Javadoc to `fabricSignalCalculator`, `readExternalDataFromFile`, `addImportParameters`, and the `getOperationEnumInterfaces` anonymous method

### [ControlSignalSourceDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignalSourceDialog.java)
- Add a Javadoc class description documenting this dialog for configuring signal shape (rectangle/sine/triangle), amplitude, frequency, offset, phase, duty ratio, and display options
- Add Javadoc to `getComponentsDisabledExternal()` and `processInputs()`

### [ControlSignum.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignum.java)
- Add a Javadoc class description documenting that this block computes the signum (sign) function: +1 for positive input, -1 for negative, 0 for zero
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` and `getDialogMessage()` with `@return` tags
- Add Javadoc to the `tinfo` field and constructor

### [ControlSlidingDFT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSlidingDFT.java)
- Add a Javadoc class description documenting this block performs a sliding Discrete Fourier Transform (SDFT) for real-time spectral analysis at user-defined frequencies
- Add Javadoc to the inner class `FrequencyData` and its `equals`/`hashCode` methods; note that `hashCode()` (line 189) casts a `long` to `int` which can produce collisions -- document this or use `Long.hashCode()`

### [ControlSlidingDFTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSlidingDFTDialog.java)
- Add a Javadoc class description documenting this dialog for configuring SDFT frequencies (add/remove) and selecting output data types (magnitude, real, imag, phase)
- Remove the IDE-generated boilerplate comment on line 184: `"//To change body of generated methods, choose Tools | Templates."`

### [ControlSmallSignalAnalysis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSmallSignalAnalysis.java)
- Add a Javadoc class description documenting this block performs small-signal analysis by injecting a swept-frequency perturbation and measuring the system response
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()`, `getBlockWidth()`, `getCenteredDrawString()`, `openDialogWindow()`, and `getOutputNames()`

### [ControlSourceType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ControlSourceType.java)
- Add class-level Javadoc explaining control signal source type mapping
- Add Javadoc to `getFromID()`, `getNewID()` (note: returns `double` not `int`)
- Add Javadoc to constructor

### [ControlSpaceVector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSpaceVector.java)
- Add a Javadoc class description documenting this block displays a space vector diagram for up to 9 input signals (3-phase systems)

### [ControlSubtraction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSubtraction.java)
- Add a Javadoc class description documenting that this block subtracts inputs: with 2 inputs it computes x1 - x2; with more inputs it subtracts all subsequent inputs from the first
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` documenting the branching logic between `SubtractionTwoParameter` and `SubtractionMoreParameter`
- Add Javadoc to `getOutputNames()` and `getOutputDescription()`, and to the `tinfo` field

### [ControlTAN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTAN.java)
- Add class-level Javadoc explaining tangent computation

### [ControlTEMP.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTEMP.java)
- Add class-level Javadoc explaining thermal potential (temperature) measurement

### [ControlTERMINAL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTERMINAL.java)
- Add class-level Javadoc explaining control-domain subcircuit terminal
- Document `_wrapped` field
- Complete `clickedTerminal()` Javadoc `@param`/`@return` tags

### [ControlTIME.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTIME.java)
- Add class-level Javadoc explaining simulation time output

### [ControlTerminable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ControlTerminable.java)
- Add Javadoc to `getNodeNumber()`, `setNodeNumber()`, `clearNodeNumber()` explaining node numbering
- Add class-level Javadoc

### [ControlTerminalDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTerminalDialog.java)
- Add class-level Javadoc explaining terminal label dialog

### [ControlThyristorControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlThyristorControl.java)
- Add class-level Javadoc explaining 6-pulse thyristor rectifier gate signal generation
- Document `TN_Y` constant (set to 6, for 6 gate outputs)
- Document `_onTime`, `_initFreq`, `_phaseShift` UserParameters with units

### [ControlToEXTERNAL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlToEXTERNAL.java)
- Add class-level Javadoc explaining control signal export to external program
- Document static `toExternals` ArrayList (thread-safety, lifecycle)
- Add Javadoc on `compareTo()` and `Comparable` implementation

### [ControlTransferFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTransferFunction.java)
- Add class-level Javadoc explaining transfer function H(s) via state-space
- Document `_zeros`, `_poles` field conventions (real/imaginary interleaved)

### [ControlType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlType.java)
- Add Javadoc on enum explaining TRANSFER, SINK, SOURCE classification

### [ControlTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlTypeInfo.java)
- Add class-level Javadoc explaining type metadata for control components
- Document that `fabric()` catches `Throwable` and returns null

### [ControlU_ZI.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlU_ZI.java)
- Add class-level Javadoc explaining U-Z*I trajectory plot
- Document `setTerminalNodeLabel()` parameters

### [ControlVIEWMOT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlVIEWMOT.java)
- Add class-level Javadoc explaining machine internal quantity reading
- Document `getParameterString()` 3-element parameter string convention

### [ControlVOLT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlVOLT.java)
- Add class-level Javadoc explaining voltage measurement between labeled nodes

### [ControlVOLTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlVOLTDialog.java)
- Add class-level Javadoc explaining voltage measurement configuration dialog
- Document the mutual-exclusion listener logic between combo boxes

### [ControlViewMotDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlViewMotDialog.java)
- Add class-level Javadoc explaining machine/variable selection dialog

### [ControlWithSingleReference.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlWithSingleReference.java)
- Add class-level Javadoc explaining base class for single-reference control blocks

### [CounterCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/CounterCalculatable.java)
- Add Javadoc on `_lastValue` field and rising-edge detection logic

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

### [DiagramCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DiagramCurve.java)
- Add class-level Javadoc explaining standard analog curve diagram

### [DiagramCurveSignalManager.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DiagramCurveSignalManager.java)
- Add class-level Javadoc explaining diagram/curve/signal lifecycle manager
- Add Javadoc to all management methods

### [DiagramSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DiagramSettings.java)
- Add class-level Javadoc explaining per-diagram settings
- Document `setWeightDiagram` valid range [0, 1]

### [DiagramSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DiagramSignal.java)
- Add class-level Javadoc explaining digital signal diagram type

### [DialogAbout.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogAbout.java)
- Add class-level Javadoc explaining About dialog

### [DialogCircuitComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogCircuitComponent.java)
- Add class-level Javadoc explaining the abstract base dialog for circuit component parameters
- Document all UI component fields
- Add Javadoc to `okActionListener`, `processInputIndividual()`, `baueGUI()`, `setNewElementName()`
- Add Javadoc to `getRegisteredTextField`, `createParameterPanel()`, `processRegisteredParameters()`

### [DialogConnectSignalsGraphs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogConnectSignalsGraphs.java)
- Add class-level Javadoc explaining signal-to-diagram assignment dialog
- Document `recalculateWeights` normalization algorithm

### [DialogControlCheck.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogControlCheck.java)
- Add class-level Javadoc explaining control port connection check dialog

### [DialogControlOrderN.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogControlOrderN.java)
- Add class-level Javadoc explaining control block reordering dialog

### [DialogControlVariableInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogControlVariableInputs.java)
- Add class-level Javadoc explaining input terminal count dialog

### [DialogCurveProperties.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogCurveProperties.java)
- Add class-level Javadoc explaining per-curve properties dialog

### [DialogDataExport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogDataExport.java)
- Add class-level Javadoc explaining data export dialog
- Document the `_inFillLists` flag and `Thread.sleep(100)` race-condition workaround

### [DialogDefineAvg.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogDefineAvg.java)
- Add class-level Javadoc explaining averaging signal definition dialog

### [DialogDiagramProps.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogDiagramProps.java)
- Add class-level Javadoc explaining diagram properties dialog
- Document the `_initDone` obfuscator workaround

### [DialogElementCONTROL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogElementCONTROL.java)
- Add class-level Javadoc explaining base dialog for all control component parameters

### [DialogElementLK.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DialogElementLK.java)
- Add Javadoc on `baueGUIIndividual()`, `createControlLabelCombo()`

### [DialogExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogExternal.java)
- Add class-level Javadoc explaining Simulink external interface configuration
- Fix class comment filename "DialogExterna.java" -> "DialogExternal.java"

### [DialogFeedback.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogFeedback.java)
- Add class-level Javadoc explaining feedback submission dialog

### [DialogFindInModel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogFindInModel.java)
- Add class-level Javadoc explaining find-in-model search dialog
- Document the search algorithm and matcher types

### [DialogFourier.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogFourier.java)
- Add class-level Javadoc explaining Fourier analysis dialog

### [DialogFourierDiagramm.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DialogFourierDiagramm.java)
- **Add Javadoc to the class and constructor.** The constructor (lines 72-98) has no `@param` documentation for its 7 parameters

### [DialogGlobalTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogGlobalTerminal.java)
- Add class-level Javadoc explaining global terminal name configuration dialog
- Add Javadoc to constructor, `readAllGlobalsIntoComboBoxes()`, `setComponentName()`
- Document the `_initDone` flag

### [DialogJavaCompilerOptimizer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogJavaCompilerOptimizer.java)
- Add class-level Javadoc

### [DialogLabelEingeben.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogLabelEingeben.java)
- Add class-level Javadoc explaining terminal/connection label editing dialog

### [DialogLicense.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogLicense.java)
- Add class-level Javadoc explaining license validation

### [DialogLicensing.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogLicensing.java)
- Add class-level Javadoc explaining licensing terms dialog

### [DialogLossesDetail.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DialogLossesDetail.java)
- Add class-level Javadoc explaining the modal editor dialog for detailed loss curves
- Add Javadoc to `applyChanges()`, `doSaveAsNew()`, `getNewFileNameDialog()`, `createTestCurve()`

### [DialogMakeExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogMakeExternal.java)
- Add class-level Javadoc explaining file conversion process

### [DialogMemory.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogMemory.java)
- Add class-level Javadoc explaining JVM memory settings dialog
- Document default memory values

### [DialogModule.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogModule.java)
- Add class-level Javadoc
- Document `jTextFieldFileName` initialized with placeholder "jTextField1" (leftover IDE code)

### [DialogMuxDemux.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogMuxDemux.java)
- Add class-level Javadoc explaining MUX/DEMUX port count configuration

### [DialogNonLinearity.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DialogNonLinearity.java)
- Add class-level Javadoc explaining non-linear characteristic editing dialog
- Add Javadoc to constructor explaining `yAxisLog` parameter
- Document the empty catch block (should at least log the exception)

### [DialogOptimizerParameterSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogOptimizerParameterSettings.java)
- Add class-level Javadoc

### [DialogPanelVoltageMeasurement.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogPanelVoltageMeasurement.java)
- Add class-level Javadoc explaining voltage measurement node selection panel

### [DialogRemotePort.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogRemotePort.java)
- Add class-level Javadoc explaining remote access port configuration

### [DialogSSAPlot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogSSAPlot.java)
- Add class-level Javadoc explaining Bode plot display dialog
- Document the icon-loading catch block (silently swallows exceptions)

### [DialogScopeCharacteristics.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogScopeCharacteristics.java)
- Add class-level Javadoc explaining scope characteristics measurement dialog

### [DialogScopeSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogScopeSettings.java)
- Add class-level Javadoc explaining general scope settings

### [DialogSimParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogSimParameter.java)
- Add class-level Javadoc explaining simulation parameter configuration
- Document solver type selection and time step validation

### [DialogSimpleInfoMessage.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogSimpleInfoMessage.java)
- Add class-level Javadoc explaining simple info message dialog

### [DialogSmallSignalAnalysis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogSmallSignalAnalysis.java)
- Add class-level Javadoc explaining small-signal analysis (Bode plot) configuration dialog
- Document `oldTend` field

### [DialogSubCktSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DialogSubCktSettings.java)
- Add Javadoc on class

### [DialogThyristorControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogThyristorControl.java)
- Add class-level Javadoc explaining thyristor gate control parameter dialog
- Document gate-on-time unit conversion (spinner shows ms, parameter stored in seconds)

### [DialogTransferFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogTransferFunction.java)
- Add class-level Javadoc explaining transfer function editing dialog
- Document `_inPolynomialMode` field
- Document why imaginary values use `Math.abs()` (enforces conjugate complex pairs)

### [DialogUpdate.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogUpdate.java)
- Add class-level Javadoc explaining software update dialog

### [DialogUpdateSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogUpdateSettings.java)
- Add class-level Javadoc explaining update frequency settings

### [DialogViewPowerModule.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DialogViewPowerModule.java)
- Add Javadoc on class

### [DialogWarningNodeNumber.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/DialogWarningNodeNumber.java)
- Add class-level Javadoc explaining node number warning

### [DialogWindowWithoutInput.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogWindowWithoutInput.java)
- Add class-level Javadoc explaining minimal dialog for parameterless control blocks

### [Diode.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/Diode.java)
- Add Javadoc on class explaining diode as self-commutated voltage-drop switch

### [DiodeCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DiodeCalculator.java)
- Add Javadoc on class explaining iterative diode on/off resistance switching
- Document static fields `diodeSwitchError`, `inSwitchErrorMode`, `diodeErrorOccurred`
- Document the NaN check `assert aValue == aValue` in `stampMatrixA()`

### [DiodeCharacteristic.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DiodeCharacteristic.java)
- Add Javadoc on class

### [DiodeDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DiodeDialog.java)
- Add Javadoc on class

### [DiodeSegment.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DiodeSegment.java)
- Add Javadoc on class explaining segmented diode loss characteristic

### [DirectCurrentCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/DirectCurrentCalculatable.java)
- Add Javadoc on interface explaining `getZValue()`/`setZValue()` for MNA auxiliary variables

### [DirectVoltageMeasurable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/DirectVoltageMeasurable.java)
- Add Javadoc to `getDirectVoltageMeasurementComponents(ConnectorType)`
- Add class-level Javadoc

### [DisplayFourierWorksheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DisplayFourierWorksheet.java)
- **Add Javadoc to the class and constructor.** Document `@param cnSG`, `@param jnSG`, `@param nMin`
- **Add Javadoc to the anonymous `JTable` override** (lines 65-71) explaining why `isCellEditable` returns `false`

### [DisplayScale.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DisplayScale.java)
- Add class-level Javadoc explaining zoom step mechanism
- Document `MAXSWITCHINDEX = 5`

### [DivCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/DivCalculator.java)
- Add Javadoc on `calculateYOUT` explaining NaN (0/0) and Infinity (x/0) handling
- Document `LARGE_NUMBER` constant

### [Documentation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/Documentation.java)
- Add class-level Javadoc explaining this annotation links a remote interface method to an i18n key
- Add Javadoc to `value()`

### [DoubleMap.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/DoubleMap.java)
- Add class-level Javadoc explaining double-keyed map structure
- Add Javadoc to all methods

### [DragTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DragTest.java)
- Document as standalone test/demo class or mark for removal

### [ElementDisplayProperties.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ElementDisplayProperties.java)
- Add class-level Javadoc explaining display/visibility flags
- Add Javadoc to each field explaining what UI element it controls

### [Enabled.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Enabled.java)
- Add class-level Javadoc explaining three component enable states
- Add Javadoc to `getFromOrdinal()`, `toString()`, enum constants

### [EndsWithMatcher.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/EndsWithMatcher.java)
- Add class-level Javadoc explaining suffix matching

### [EnumTerminalLocation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/EnumTerminalLocation.java)
- Add class-level Javadoc explaining four terminal positions
- Add Javadoc to `getFromOrdinal()`, enum constants

### [EqualCalculatorMultiInput.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/EqualCalculatorMultiInput.java)
- Add class-level Javadoc: "Checks equality across all N inputs"

### [EqualCalculatorTwoInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/EqualCalculatorTwoInputs.java)
- Add class-level Javadoc: "Checks equality of two inputs"

### [ExpCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ExpCalculator.java)
- Add class-level Javadoc: "Calculates e^x"
- Document why limit is 100 (overflow prevention)

### [ExternalGeckoCustom.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/ExternalGeckoCustom.java)
- Replace empty class-level Javadoc with meaningful description
- Add Javadoc to the constructor documenting the `access` parameter
- Add Javadoc to `runScript()` explaining why it throws `UnsupportedOperationException`

### [ExternalSignal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ExternalSignal.java)
- Add class-level Javadoc explaining external user-defined signals
- Document `Double.POSITIVE_INFINITY`/`NEGATIVE_INFINITY` as default min/max

### [ExtraFilesWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/ExtraFilesWindow.java)
- Add class-level Javadoc explaining extra files management window

### [FFTLibrary.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/FFTLibrary.java)
- Add class-level Javadoc explaining FFT library wrapper
- Add Javadoc to all methods

### [FormatJTextField.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/FormatJTextField.java)
- Add class-level Javadoc explaining formatted text field
- Document formatting rules

### [ForwardVoltageDropable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ForwardVoltageDropable.java)
- Add Javadoc on interface

### [FourierGUIless.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/FourierGUIless.java)
- Add class-level Javadoc explaining headless Fourier calculation
- Document windowing function

### [FourierKurvenRekonstruktion.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierKurvenRekonstruktion.java)
- **Add Javadoc to the class and all public/protected methods.** No Javadoc exists on the class, constructor, `resize()`, `setMouseMode()`, `zeichne()`, `setzeAchsen()`, or `setzeKurven()`

### [FourierPlotFrame.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierPlotFrame.java)
- **Complete the class-level and constructor Javadoc.** The class Javadoc (lines 24-27) is essentially empty. Add `@param` for `newScope`, `baseFrequency`, and `erg`
- **Add Javadoc to `initComponents()`** explaining it is Netbeans Form Editor-generated code
- **Document the generated fields.** `jPanelFourier1` and `jToolBar1` lack field-level Javadoc

### [FractionPrinter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/FractionPrinter.java)
- Add class-level Javadoc explaining transfer function fraction rendering
- Add Javadoc on `hsSetText()`, `setNumeratorText()`, `setDenominatorText()`
- Document `JLabelLine` inner class

### [FunctionDescription.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/FunctionDescription.java)
- Add class-level Javadoc explaining script function description for autocomplete

### [GainCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/GainCalculator.java)
- Add class-level Javadoc: "Multiplies input by constant gain"
- Add `@param gain` Javadoc on constructor

### [GateCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/GateCalculator.java)
- Add class-level Javadoc explaining Gate terminal in circuit context

### [GeckoColor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoColor.java)
- Add class-level Javadoc explaining fixed color palette for curves
- Document thread-safety of static `counter` field (not thread-safe)
- Document cycling behavior when colors exhausted

### [GeckoCustomMMF.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoCustomMMF.java)
- Complete the `@param methodObject` tag in `callMethod()` Javadoc
- Fix `@param` name mismatch in `checkForPrimitiveType()` -- says `type` but parameter is `argType`
- Add Javadoc to `monitorMMF()` and `startMonitoring()`
- Add field-level Javadoc for `_mmf`, `_accessEnabled`, `_connectionID`

### [GeckoCustomRemote.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoCustomRemote.java)
- Add Javadoc to `connect()`, `disconnect()`, `acceptExtraConnections()`, `registerForCallback()` etc.
- Add field-level Javadoc for `_free`, `_lastSessionIDActive`, `clients`, `_acceptsExtraConnections`
- Document thread-safety implications of public static `clients` map

### [GeckoDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoDialog.java)
- Add class-level Javadoc explaining base dialog with ESC-to-close

### [GeckoExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoExternal.java)
- `getThyristors()` is instance while all others are `static` -- inconsistent, document or fix
- `createComponent()` and `createConnector()` are instance while all others are `static` -- document or fix
- Add Javadoc to most delegate methods
- Consolidate duplicate class-level Javadoc blocks into one

### [GeckoFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoFile.java)
- Add class-level Javadoc explaining managed file reference
- Document internal vs. external file handling

### [GeckoFileChooser.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoFileChooser.java)
- Add class-level Javadoc explaining custom file chooser
- Add Javadoc to factory methods

### [GeckoFileManager.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoFileManager.java)
- Add class-level Javadoc explaining model-attached file management
- Document relative path computation

### [GeckoFileManagerWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoFileManagerWindow.java)
- Add class-level Javadoc explaining file manager GUI

### [GeckoFileable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GeckoFileable.java)
- Add Javadoc to each method: `initExtraFiles()`, `addFiles()`, `getFiles()`, `removeLocalComponentFiles()`

### [GeckoForwardingFileManager.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/GeckoForwardingFileManager.java)
- Add class-level Javadoc explaining the forwarding file manager for compilation

### [GeckoGraphics2D.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoGraphics2D.java)
- Add class-level Javadoc explaining delegating Graphics2D wrapper (~600+ lines)
- Add Javadoc to each delegating method explaining additional behavior

### [GeckoInvalidArgumentException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/GeckoInvalidArgumentException.java)
- Add class-level Javadoc explaining invalid script argument exception

### [GeckoJavaCompiler.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoJavaCompiler.java)
- Add class-level Javadoc explaining in-memory Java compiler
- Document classpath and toolchain configuration

### [GeckoLineStyle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoLineStyle.java)
- Add class-level Javadoc explaining line style enum with BasicStroke definitions
- Add Javadoc to each enum constant explaining stroke width and dash pattern

### [GeckoLineType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoLineType.java)
- Add class-level Javadoc explaining line type enum

### [GeckoMemoryMappedFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoMemoryMappedFile.java)
- Document the hardcoded buffer position constants with a comment explaining the memory layout
- Add Javadoc to `checkConnectionID(long)`

### [GeckoRemote.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemote.java)
- Add Javadoc to delegate methods (getControlElements, getCircuitElements, etc.)
- Complete incomplete Javadoc on `portFree(int)` -- `@param port` and `@return` are empty
- Complete incomplete Javadoc on `startGui(int)` -- `@param port` has no description
- Add `@Deprecated` annotation to methods delegating to deprecated proxies
- Add Javadoc to `RemoteInvocationHandler` inner class and its `invoke` method

### [GeckoRemoteException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteException.java)
- Fix constructor Javadoc referencing wrong class name `GeckoRemoteObjectException`

### [GeckoRemoteIntWithoutExc.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteIntWithoutExc.java)
- Add `@param` and `@return` Javadoc tags to all interface methods
- Add `@deprecated` Javadoc tags to deprecated methods with replacement guidance
- Add class-level Javadoc explaining the relationship with `GeckoRemoteInterface`

### [GeckoRemoteInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteInterface.java)
- Add Javadoc to session management methods explaining the multi-client connection model
- Add `@deprecated` tags with replacement guidance to all `@Deprecated` methods
- Add `@Documentation` annotation to `getSignalFourier` (missing, all other signal methods have it)
- Add `@param`/`@return` tags to `simulateToSteadyState` and `initSteadyStateDetection`

### [GeckoRemoteMMFObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteMMFObject.java)
- Add Javadoc to the ~60+ delegate methods that are completely undocumented
- Add Javadoc to `checkRemote()` explaining the connection validation logic
- Add Javadoc to `forceDisconnectFromGecko()`
- Add field-level Javadoc for `NO_SESSION_ID`, `sessionID`, `_mmf`, `_pathToJava`

### [GeckoRemoteObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteObject.java)
- Add Javadoc to the ~40+ delegate methods
- Add Javadoc to `connectToExistingInstance()`, `connectToGecko()`, `startNewRemoteInstance()` overloads
- Add Javadoc to `RemoteInvocationHandler` inner class
- Complete incomplete Javadoc on `portFree(int)`
- Add Javadoc to `checkRemoteWithException()`, `checkRemote()`, `disconnectFromGecko()`
- Add field-level Javadoc for `portNumber`, `_wrapped`, `_proxy`, `sessionID`, `doProxyCheck`

### [GeckoRemoteObjectTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteObjectTest.java)
- Add Javadoc to `main` method explaining what this manual test demonstrates
- Add class-level note clarifying this is a manual integration test

### [GeckoRemotePipeObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemotePipeObject.java)
- Add Javadoc to the `GeckoRemotePipeObjectType` enum constants
- Add note explaining why `_methodArguments` and `_methodReturnValue` are `transient`
- Document the potential constructor ambiguity

### [GeckoRemoteRegistry.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteRegistry.java)
- Add class-level Javadoc explaining RMI registry management
- Add Javadoc to all public methods
- Document the difference between `_remote` and `remoteAccess` fields
- Add field-level Javadoc for `DEFAULT_ACCESSPORT`, `PROPERTIES_KEY`, `_ipQuerySite`, `_ipAddress`

### [GeckoRemoteTestingDummy.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoRemoteTestingDummy.java)
- Add note that `SESSION_ID = 12345` is a fixed test value
- Remove repetitive IDE-generated comments from all ~60 method stubs
- Add Javadoc comments to key methods explaining test behavior

### [GeckoRuntimeException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoRuntimeException.java)
- Add class-level Javadoc explaining base runtime exception

### [GeckoSim.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoSim.java)
- Add Javadoc to `stopTime()`, `testIfBrandedVersion()`, `initialisiere()`, `checkJavaVersion()`, etc.
- Add Javadoc to `findOrCreateAppDataDirectory()` explaining platform-specific directory resolution
- Add field-level Javadoc for public static fields

### [GeckoSimulink.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoSimulink.java)
- Add class-level Javadoc explaining the MATLAB Simulink co-simulation interface
- Add Javadoc to all `external_*` methods
- Document the return value of `external_openFile()` (always returns hardcoded `"returnValue"`)
- Document `tStartSimulink` and `tEndSimulink` fields

### [GeckoStatusBar.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoStatusBar.java)
- Add class-level Javadoc explaining status bar widget

### [GeckoSymbol.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeckoSymbol.java)
- Add class-level Javadoc explaining symbol enum for data point markers

### [GeneralPathWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GeneralPathWrapper.java)
- Add class-level Javadoc explaining GeneralPath drawing optimization

### [GetJarPath.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GetJarPath.java)
- Add class-level Javadoc explaining JAR installation path resolution
- Document fallback behavior (IDE vs. JAR)

### [GlobalColors.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GlobalColors.java)
- Add class-level Javadoc explaining global color constants

### [GlobalFilePathes.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GlobalFilePathes.java)
- Add class-level Javadoc explaining global file path constants
- Document recent circuits list management

### [GlobalFonts.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GlobalFonts.java)
- Add class-level Javadoc explaining global font settings

### [GlobalTerminable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/GlobalTerminable.java)
- Add Javadoc to each method signature
- Add class-level Javadoc explaining cross-subcircuit connections

### [GraferTest.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GraferTest.java)
- Add class-level Javadoc explaining this is a test/demo JFrame

### [GraferV3.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferV3.java)
- **Add Javadoc to the class and public methods.** Public methods like `setzeAchsenAnzahl`, `setAxisWidthHeightX0Y0`, `setzeAchsenBegrenzungen`, `setzeAchsenTyp`, `selectColor`, `getSelectedColor` all need Javadoc

### [GraferV4.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/GraferV4.java)
- Add class-level Javadoc (~600+ lines, core class) describing scope graphing component
- Document mouse interaction modes (zoom, pan, cursor)

### [GreaterEqualCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/GreaterEqualCalculator.java)
- Add class-level Javadoc: "Outputs 1 if input[0] >= input[1]"

### [GreaterThanCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/GreaterThanCalculator.java)
- Add class-level Javadoc: "Outputs 1 if input[0] > input[1]"

### [GroupableUndoManager.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/GroupableUndoManager.java)
- Add class-level Javadoc explaining the purpose (extends UndoManager to support grouping multiple edits)
- Add Javadoc to `undo()`, `redo()`, and `addEdit()` explaining the synchronization and delegation behavior
- Add Javadoc to `GroupUndoStart` and `GroupUndoStop` inner classes
- Add Javadoc to all inner class methods explaining the undo/redo iteration logic
- Add Javadoc to fields: `otherEditsAccepted`, `_mergedEdits`, `_matchingStart`, `_editList`, `_parentEdit`

### [GuiFabric.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/GuiFabric.java)
- Add class-level Javadoc explaining i18n-aware Swing component fabrication
- Document how i18n keys are resolved

### [HeaderSymbol.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/HeaderSymbol.java)
- Add class-level Javadoc explaining header symbol for data columns

### [HeatFlowCurrentSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/HeatFlowCurrentSource.java)
- Add Javadoc on class (leaf class -- thermal heat flow source)

### [HiLoData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/HiLoData.java)
- Add class-level Javadoc explaining min/max value pair
- Add Javadoc to `hiLoDataFabric` factory method
- Document `CHECKSTYLE:OFF` for public final fields

### [HiddenSubCircuitable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/HiddenSubCircuitable.java)
- Add Javadoc to `getHiddenSubCircuitElements()` and `includeParentInSimulation()`

### [HistoryUpdatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/HistoryUpdatable.java)
- Add Javadoc on interface explaining history/rollback mechanism

### [HorizontalLevel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/HorizontalLevel.java)
- Add class-level Javadoc explaining horizontal level marker

### [HysteresisCalculatorExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/HysteresisCalculatorExternal.java)
- Add class-level Javadoc explaining hysteresis with external band input
- Document three-state output (1, -1, hold) logic

### [HysteresisCalculatorInternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/HysteresisCalculatorInternal.java)
- Add class-level Javadoc explaining hysteresis with internal band
- Add `@param hValue` Javadoc on constructor

### [IDStringDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/IDStringDialog.java)
- Add class-level Javadoc explaining unique component name/ID management
- Document the static `_allIDStrings` map
- Add Javadoc to `fabricVariableName()`, `setNameUnChecked()`, `setNewNameChecked()`, `deleteIDString()`
- Add Javadoc to `findUnusedName()` explaining auto-increment name logic
- Add Javadoc to `getComponentByName()` explaining `#` subcircuit path resolution

### [IGBT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IGBT.java)
- Add Javadoc on class explaining IGBT as gate-controlled switch with forward voltage drop

### [IGBTCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IGBTCalculator.java)
- Add Javadoc on class explaining gate-gated on/off logic

### [IGBTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IGBTDialog.java)
- Add Javadoc on class

### [IGenericMVCView.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/IGenericMVCView.java)
- Complete class-level Javadoc -- describe that this is the view interface for the generic MVC framework
- Complete `registerModel()` Javadoc: `@param pointModel` and `@param undoRedoText` have no descriptions
- Add description to `unregisterModel()` Javadoc

### [IdealSwitch.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IdealSwitch.java)
- Add Javadoc on class explaining ideal switch (no voltage drop)

### [IdealSwitchCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IdealSwitchCalculator.java)
- Add Javadoc on class explaining gate-only response (no self-commutation)

### [IdealSwitchDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IdealSwitchDialog.java)
- Add Javadoc on class

### [IdealTransformer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IdealTransformer.java)
- Add Javadoc on class explaining ideal transformer model

### [IdealTransformerDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/IdealTransformerDialog.java)
- Add Javadoc on class

### [InductorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/InductorCalculator.java)
- Add Javadoc on class explaining inductor stamping (B-vector only)
- Add Javadoc on `stampVectorB()` -- solver-specific formulas
- Document `stampVectorBTRZ()` -- "UGLY, just a temporary solution" comment
- Document `FAST_NULL_L` floor in `setInductance()`

### [InductorCoupable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/InductorCoupable.java)
- Add Javadoc on interface

### [InductorCouplingCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/InductorCouplingCalculator.java)
- Add Javadoc on class explaining coupled inductor current update
- Add Javadoc on `addNewCurrent()`, `stampVectorBTRZ()`, `setGroup()`

### [InductorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/InductorDialog.java)
- Add Javadoc on class

### [InductorWOCoupling.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/InductorWOCoupling.java)
- Add Javadoc on class explaining this variant excludes itself from mutual coupling

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

### [LossCalculatorResistor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculatorResistor.java)
- Add class-level Javadoc explaining P = I*V loss calculation
- Add Javadoc to constructor and `calcLosses()` explaining temperature/deltaT are ignored

### [LossComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossComponent.java)
- Add Javadoc to `toString()`, `getSaveString()`, `getEnumFromSaveString()`
- Document the fallback to `TOTAL` on unmatched strings

### [LossContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossContainer.java)
- Consider adding a brief usage example in class Javadoc

### [LossCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCurve.java)
- Add Javadoc to abstract class explaining template method pattern for loss curves
- Add Javadoc to `importASCII()`, `exportASCII()`, abstract methods
- Document `data` public field's expected layout
- Document `tj` UserParameter (junction temperature, default 0.0 C)

### [LossCurvePlotPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/LossCurvePlotPanel.java)
- Add class-level Javadoc explaining loss curve plot panel
- Document why private constructor (utility class?)

### [LossCurveTemperaturePanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCurveTemperaturePanel.java)
- Add class-level Javadoc explaining radio-button selection panel
- Add Javadoc to all methods

### [LossProperties.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossProperties.java)
- Add class-level Javadoc explaining central loss-calculation configuration
- Add Javadoc to constructor, inner wrapper classes, `lossCalculatorFabric()`
- Add Javadoc to `exportASCII()`, `importASCII()`, `setLossType()`, `getLossType()`

### [MOSFET.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MOSFET.java)
- Add Javadoc on class explaining MOSFET as bidirectional gate-controlled switch

### [MOSFETDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MOSFETDialog.java)
- Add Javadoc on class

### [MUXControlCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/MUXControlCalculatable.java)
- Add class-level Javadoc explaining multiplexer (N scalar to 1 vector)
- Add `@param noInputs` Javadoc on constructor

### [MainWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/MainWindow.java)
- Add class-level Javadoc (currently only license header, no class doc)
- Add Javadoc to all menu builder methods
- Add Javadoc to `BackupTask` inner class

### [MapList.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/MapList.java)
- Add class-level Javadoc explaining ArrayList with secondary class-type index map
- Add Javadoc to `getClassFromContainer()`, `add()`, `remove()`, `addAll()`
- Document `registeredTypes` array

### [Matrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Matrix.java)
- Remove dead `//package Jama;` and `//import Jama.util.*;` comments (lines 16, 24)

### [MaxCalculatorMultiInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/MaxCalculatorMultiInputs.java)
- Add class-level Javadoc: "Outputs maximum across all N inputs"

### [MaxCalculatorTwoInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/MaxCalculatorTwoInputs.java)
- Add class-level Javadoc: "Outputs maximum of two inputs"

### [MemoryContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/MemoryContainer.java)
- Add class-level Javadoc explaining memory management container
- Document threading model (acknowledged thread-safety issues)

### [MemoryInitializable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/MemoryInitializable.java)
- Add Javadoc on `doInit()` explaining simulation start initialization

### [MemoryWarning.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/MemoryWarning.java)
- Add class-level Javadoc explaining memory warning dialog

### [MethodCategory.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/MethodCategory.java)
- Add class-level Javadoc explaining this enum categorizes remote API methods
- Add Javadoc to enum constants and `toString()`

### [MethodNameChecker.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/MethodNameChecker.java)
- Document parameters `checkMethods` and `containsMethodSignature`

### [MinCalculatorMultiInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/MinCalculatorMultiInputs.java)
- Add class-level Javadoc: "Outputs minimum across all N inputs"

### [MinCalculatorTwoInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/MinCalculatorTwoInputs.java)
- Add class-level Javadoc: "Outputs minimum of two inputs"

### [ModelMVC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/ModelMVC.java)
- Complete the class-level Javadoc to describe the concrete model class role
- Add Javadoc to the `ModelMVC(T initValue)` constructor
- Fix `ModelMVC(T initValue, Object descriptionObject)` Javadoc: `@param initValue` says "initial Float value" but T is generic
- Add Javadoc to `toString()` and `_descriptionObject` field

### [ModelMVCGeneric.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/ModelMVCGeneric.java)
- Add Javadoc to the `listeners` field explaining why it uses `WeakListModel`
- Document the NaN-replacement behavior in `setValue()` Javadoc

### [MotorDC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorDC.java)
- Add Javadoc on class (leaf class)

### [MotorDCDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorDCDialog.java)
- Add Javadoc on class

### [MotorImCage.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorImCage.java)
- Add Javadoc on class explaining squirrel-cage induction machine
- Add Javadoc on `calculateElectricTorque()`, `calculateMotorEquations()`

### [MotorImCageDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorImCageDialog.java)
- Add Javadoc on class

### [MotorImSat.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorImSat.java)
- Add Javadoc on class explaining saturated induction machine

### [MotorImSatDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorImSatDialog.java)
- Add Javadoc on class

### [MotorInductionMachine.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorInductionMachine.java)
- Add Javadoc on class (leaf class)

### [MotorInductionMachineDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorInductionMachineDialog.java)
- Add Javadoc on class

### [MotorPMSM.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorPMSM.java)
- Add Javadoc on class explaining PMSM

### [MotorPMSMDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorPMSMDialog.java)
- Add Javadoc on class

### [MotorPermanent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorPermanent.java)
- Add Javadoc on class explaining permanent magnet motor

### [MotorPermanentDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorPermanentDialog.java)
- Add Javadoc on class

### [MotorSmRound.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorSmRound.java)
- Add Javadoc on class explaining round-rotor synchronous machine

### [MotorSmRoundDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorSmRoundDialog.java)
- Add Javadoc on class

### [MotorSmSalient.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorSmSalient.java)
- Add Javadoc on class explaining salient-pole synchronous machine

### [MotorSmSalientDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MotorSmSalientDialog.java)
- Add Javadoc on class

### [MutualCouplingCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MutualCouplingCalculator.java)
- Add Javadoc on class explaining mutual inductance stamping
- Add Javadoc on `stampInductanceMatrix()`

### [MutualInductance.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MutualInductance.java)
- Add Javadoc on class explaining coupling component

### [MutualInductanceDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/MutualInductanceDialog.java)
- Add Javadoc on class

### [MyFFT.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/MyFFT.java)
- Add class-level Javadoc explaining custom FFT implementation
- Add Javadoc explaining FFT butterfly algorithm

### [MyProxy.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/MyProxy.java)
- If keeping, add `@Deprecated` annotation

### [MyTableCellEditor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/MyTableCellEditor.java)
- Add class-level Javadoc explaining table cell editor using FormatJTextField
- Add Javadoc to `getTableCellEditorComponent`, `getCellEditorValue()`

### [MyTableCellRenderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/MyTableCellRenderer.java)
- Add class-level Javadoc explaining numeric value rendering with engineering notation
- Add Javadoc to `setValue(Object)`

### [MyTableComparator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/MyTableComparator.java)
- Add class-level Javadoc explaining first-column ascending sort comparator
- Add Javadoc to `compare()` explaining null-handling

### [NComplex.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/NComplex.java)
- Add `@param` and `@return` Javadoc tags to all three constructors and all static utility methods (`add`, `sub`, `mul`, `div`, `conj`, `abs`, `sqrt`, `RCmul`)

### [NameAlreadyExistsException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NameAlreadyExistsException.java)
- Add class-level Javadoc explaining when this exception is thrown
- Document the constructor `@param message`

### [NativeCBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCBlock.java)
- **Add Javadoc to all methods:** `calculateYOUT`, `loadLibraries`, `unloadLibraries`, `checkOutputsForNANorINFValues`, and the constructor

### [NativeCClassLoader.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCClassLoader.java)
- **Add Javadoc to `findClass` method and its `name` parameter** (line 38)
- **Add Javadoc to the no-arg constructor** and `toString` override

### [NativeCDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCDialog.java)
- **Add Javadoc to methods:** `initFileChooser` (line 224) and `isFileNameAlreadyInList` (line 238)

### [NativeCLibraryFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCLibraryFile.java)
- **Complete the empty `@return` tag** in `getTimeStamp()` Javadoc (line 98)
- **Add Javadoc to all undocumented methods:** `getFileName`, `setFile(File)`, `setFile(String)`, `setFile()`, `getFile`, `updateTimeStamp`, and both parameterized constructors

### [NativeCWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCWrapper.java)
- **Add missing `@param xOUTVector` to `calcOutputs` Javadoc** (lines 32-39). Also add `@param` tags for `xINVector` and `deltaT`
- **Remove invalid `@return` tag** (line 38) from `calcOutputs` -- the method returns `void`. Move description text to `@param xOUTVector`
- **Improve `initParameters` Javadoc** (lines 43-45): Note that this is a `native` method implemented in the external C/C++ library

### [NetListContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListContainer.java)
- Add class-level Javadoc explaining the container aggregating three netlists
- Document each static fabric method (`fabricStartSimulation`, `fabricContinueSimulation`, `fabricGuiUpdate`)
- Document public fields `_nlControl`, `_nlLK`, `_nlTH`

### [NetListLK.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListLK.java)
- Add class-level Javadoc explaining LK (power circuit) netlist
- Add Javadoc to all major methods

### [NetlistControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/NetlistControl.java)
- Add class-level Javadoc explaining control-domain netlist management
- Add Javadoc on all factory methods
- Document `IndexConnection` inner class

### [NetlistGeneral.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetlistGeneral.java)
- Add class-level Javadoc explaining the general netlist builder
- Remove or document unused `static int counter` field
- Add Javadoc to private methods (`createPotentialSheetConnectedGeometric`, `mergePotentialAreasViaLabels`, etc.)
- Add Javadoc to `GraphEdge` inner class and `traverse` method

### [NewScope.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NewScope.java)
- Add class-level Javadoc (~900+ lines) describing scope panel, cursor logic, signal management
- Add Javadoc to cursor measurement methods
- Document keyboard shortcut bindings

### [NiceScale.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NiceScale.java)
- Add class-level Javadoc explaining "nice number" axis scaling (Heckbert's algorithm)

### [NoCurveSelectedException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NoCurveSelectedException.java)
- Add class-level Javadoc explaining when this exception is thrown

### [NodeLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NodeLabel.java)
- Add class-level Javadoc explaining schematic node label with anchor and click area
- `importASCII` has an empty body -- implement or document why

### [NonLinearDialogPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NonLinearDialogPanel.java)
- Add class-level Javadoc explaining non-linear characteristic editing dialog
- Document constructor parameters (`parentDialog`, `elementLK`, `isYAxisLog`)

### [NonLinearReluctance.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/NonLinearReluctance.java)
- Add Javadoc on class explaining nonlinear reluctance (magnetic saturation)

### [NonlinearReluctanceDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/NonlinearReluctanceDialog.java)
- Add Javadoc on class

### [Nonlinearable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/Nonlinearable.java)
- Add Javadoc on interface explaining nonlinear characteristic contract

### [NotCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NotCalculator.java)
- Add class-level Javadoc: "Logical NOT -- inverts signal based on SIGNAL_THRESHOLD"

### [NotEqualCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NotEqualCalculator.java)
- Add class-level Javadoc: "Outputs 1 if inputs are not equal"

### [NothingToDoCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NothingToDoCalculator.java)
- Add class-level Javadoc explaining pass-through/no-op calculator
- Remove stale IDE template comment

### [OperatingMode.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/OperatingMode.java)
- Add Javadoc to enum explaining operating modes
- Add Javadoc to each enum constant

### [Operationable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/Operationable.java)
- Add Javadoc on interface explaining scriptable operations
- Add Javadoc on `OperationInterface` abstract class, `fabricFromString()`, `doOperation()`

### [OperationalAmplifier.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/OperationalAmplifier.java)
- Add Javadoc on class explaining ideal op-amp model

### [OperationalAmplifierDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/OperationalAmplifierDialog.java)
- Add Javadoc on class

### [OptimizerParameterData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/OptimizerParameterData.java)
- Add class-level Javadoc explaining optimizer parameter storage

### [OrCalculatorMultipleInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/OrCalculatorMultipleInputs.java)
- Add class-level Javadoc: "Logical OR across all N inputs"

### [OrCalculatorTwoInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/OrCalculatorTwoInputs.java)
- Add class-level Javadoc: "Logical OR of two inputs"

### [OutputWarningStream.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/OutputWarningStream.java)
- Add class-level Javadoc explaining this monitors output volume and warns about excessive output
- Add Javadoc to all public methods
- Document why `byteCounter` and `warningBytesSize` are `static`
- Add field-level comments for `_verbosityWarnShown`, `_isOriginalOutput`, `_ignoreFutureMessages`

### [PDCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PDCalculator.java)
- Add class-level Javadoc explaining PD controller
- Add Javadoc on numerical differentiation formula

### [PICalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PICalculator.java)
- Add class-level Javadoc explaining PI controller using trapezoidal integration

### [PT1Calculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT1Calculator.java)
- Add class-level Javadoc explaining PT1: G(s) = K/(1+sT)

### [PT2Calculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT2Calculator.java)
- Add class-level Javadoc explaining PT2: G(s) = K/(1+(sT)^2)

### [PanelCharacteristicsResult.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PanelCharacteristicsResult.java)
- Add class-level Javadoc explaining results display panel

### [Paradiso.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Paradiso.java)
- Add class-level Javadoc explaining Intel PARDISO sparse direct solver wrapper
- Document `factorize` and `solve` parameters and `mtype` values

### [ParameterSupport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/ParameterSupport.java)
- Add class-level Javadoc explaining parameter management for scriptable components
- Document parameter name resolution mechanism

### [PardisoCachedMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PardisoCachedMatrix.java)
- Add class-level Javadoc explaining cached matrix using Pardiso solver
- Document constructor `@param matrix`
- Replace stale IDE template comment in `calculateMemoryRequirement()`
- Document `initLUDecomp()` explaining symmetry check

### [PmsmControlCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmControlCalculator.java)
- Add class-level Javadoc explaining PMSM field-oriented control
- Add Javadoc on all 14+ fields (none documented)
- Document all 12 input signals and 8 output signals

### [PmsmModulatorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmModulatorCalculator.java)
- Add class-level Javadoc explaining Space Vector PWM for 2-level 3-phase inverter
- Document all intermediate variables
- Document sector detection using if-statements instead of else-if

### [Point.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/Point.java)
- Add Javadoc on class, constructor, `distance()`, `equals()`, `hashCode()`, `toString()`

### [PolynomTools.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PolynomTools.java)
- Add class-level Javadoc explaining polynomial manipulation utilities
- Document or remove `main()` test method
- Fix Javadoc error: `[1 2 0 4] == 1 + 2s + 4s^4` should be `1 + 2s + 4s^3`

### [Polynomials.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Polynomials.java)
- Add `@param` and `@return` Javadoc tags to `poldiv` -- currently only has a prose description, missing documentation for all six parameters
- Add a class-level Javadoc with a brief usage example showing how to call `poldiv` for polynomial division

### [PostCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PostCalculatable.java)
- Add `@param dt` and `@param t` documentation to `doCalculation`
- Add Javadoc to `doInitialization()`

### [PostProcessable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/PostProcessable.java)
- Add Javadoc on interface explaining post-solve calculation contract

### [PotentialArea.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialArea.java)
- Add Javadoc to all methods (`geometricOnSamePotential`, `mergePotential`, `hasComponentConnection`, etc.)

### [PotentialCoupable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialCoupable.java)
- Add `@return` documentation to `getPotentialCoupling()`

### [PotentialCoupling.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialCoupling.java)
- Add class-level Javadoc explaining label-based potential couplings
- Document constructor parameters
- Add Javadoc to all methods and inner classes

### [PowerAnalysisPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PowerAnalysisPanel.java)
- Add class-level Javadoc explaining power analysis (P, Q, S, D, cos(phi))
- Add Javadoc to `calculate` method
- Fix comment "performance values A and B" (grid has 3 columns: A, B, C)

### [PowerAnalysisSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PowerAnalysisSettings.java)
- Add class-level Javadoc explaining voltage/current indices for phases A/B/C

### [PowerCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PowerCalculator.java)
- Add class-level Javadoc explaining power calculation algorithm
- Add Javadoc to each power quantity getter with formula

### [PowerModulePainter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/PowerModulePainter.java)
- Add Javadoc on class explaining thermal power module rendering

### [PreviewDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PreviewDialog.java)
- Add class-level Javadoc explaining signal preview dialog base class

### [PreviewDialogRectangular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PreviewDialogRectangular.java)
- Add class-level Javadoc explaining rectangular waveform preview

### [PreviewDialogSine.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PreviewDialogSine.java)
- Add class-level Javadoc explaining sinusoidal waveform preview

### [PreviewDialogTriangle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PreviewDialogTriangle.java)
- Add class-level Javadoc explaining triangular waveform preview

### [PriorityThreadFactory.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PriorityThreadFactory.java)
- Add class-level Javadoc explaining low-priority thread factory

### [ProjectData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/ProjectData.java)
- Add class-level Javadoc explaining project/model data holder
- Add Javadoc to `exportASCII()` and `shiftComponentReferences()`

### [QuasiPeakCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/QuasiPeakCalculator.java)
- Add class-level Javadoc explaining CISPR 16 quasi-peak detector
- Document CISPR band constants (`A_LOWER_LIMIT`, `B_LOWER_LIMIT`, etc.)
- Add Javadoc on `quasiPeakDetector()` explaining backward Euler discretization
- Document the `System.gc()` call (generally discouraged)

### [RamJavaFileObject.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/RamJavaFileObject.java)
- Add class-level Javadoc explaining in-memory Java file object

### [RelTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/RelTerminal.java)
- Add Javadoc on class (leaf class -- reluctance terminal)

### [ReluctanceAndCircuitTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ReluctanceAndCircuitTypeInfo.java)
- Add Javadoc on class explaining dual-domain type info

### [ReluctanceComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ReluctanceComponent.java)
- Add Javadoc on interface explaining reluctance-domain contract

### [ReluctanceGlobalTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ReluctanceGlobalTerminal.java)
- Add Javadoc on class (leaf class)

### [ReluctanceInductor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ReluctanceInductor.java)
- Add Javadoc on class explaining reluctance inductor (permeance analogy)

### [ReluctanceInductorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ReluctanceInductorDialog.java)
- Add Javadoc on class

### [ReluctanceTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ReluctanceTypeInfo.java)
- Add Javadoc on class explaining reluctance-domain type info

### [ReportingListTransferHandler.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ReportingListTransferHandler.java)
- Add class-level Javadoc explaining drag-and-drop reordering handler
- Add Javadoc on `importData()`, `exportDone()`, `createTransferable()`

### [ResistorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ResistorCalculator.java)
- Add Javadoc on class explaining simplest A-matrix stamping (1/R conductance)
- Add Javadoc on `setResistance()` -- note: check tests OLD value, should test parameter
- Add Javadoc on `updateHistory()`, `toString()`

### [ResistorCircuit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ResistorCircuit.java)
- Add Javadoc on class (leaf class)

### [ResistorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ResistorDialog.java)
- Add Javadoc on class

### [ResistorReluctance.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ResistorReluctance.java)
- Add Javadoc on class (leaf class -- magnetic reluctance)

### [ResistorThermal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ResistorThermal.java)
- Add Javadoc on class (leaf class -- thermal resistance)

### [RoundCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/RoundCalculator.java)
- Add class-level Javadoc: "Rounds to nearest integer using Math.round()"

### [SSAShape.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SSAShape.java)
- Add Javadoc on enum explaining excitation signal shapes for small-signal analysis
- Add Javadoc on each enum constant

### [SampleHoldCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SampleHoldCalculator.java)
- Add class-level Javadoc explaining sample-and-hold (samples on clock high)
- Document input indices: [0]=signal, [1]=clock

### [SaveViewFrame.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SaveViewFrame.java)
- Add class-level Javadoc explaining image export frame
- Document supported export formats (PNG, PDF, SVG, etc.)

### [SchematicComponentSelection2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicComponentSelection2.java)
- Add class-level Javadoc explaining the tabbed component palette
- Document type arrays (`_typLK`, `_typMotor`, `_typSubcircuit`, `_typCONTROL`, etc.)

### [SchematicEditor2.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicEditor2.java)
- Add class-level Javadoc explaining the main mouse-driven schematic editor controller
- Document the `MouseMoveMode` enum and state machine transitions

### [SchematicTextInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SchematicTextInfo.java)
- Add class-level Javadoc explaining parameter text display beside components
- Document fields and inner classes

### [Scopable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/Scopable.java)
- **Add meaningful Javadoc to the interface and methods.** The class Javadoc (lines 16-19) is empty
- **Add `@return` documentation** to `getZVDatenImRAM()` explaining it returns high-resolution simulation data stored in RAM

### [ScopeFrame.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ScopeFrame.java)
- Add class-level Javadoc (~700+ lines) describing scope JFrame window
- Add Javadoc to menu item handlers and key bindings
- Document save/load workflow

### [ScopeSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ScopeSettings.java)
- Add class-level Javadoc explaining scope-level settings
- Address "biggest bullshit... should be refactored" comment -- refactor or add proper Javadoc

### [ScopeSignalMean.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ScopeSignalMean.java)
- Add class-level Javadoc explaining averaged/mean signal
- Document cast safety in constructor (`AbstractScopeSignal` to `ScopeSignalRegular`)

### [ScopeSignalRegular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ScopeSignalRegular.java)
- Add class-level Javadoc explaining standard immutable scope signal
- Document immutability contract

### [ScopeSignalSimpleName.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ScopeSignalSimpleName.java)
- Add class-level Javadoc explaining simple scope signal wrapper

### [ScopeWrapperIndices.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ScopeWrapperIndices.java)
- Add class-level Javadoc explaining scope wrapper index mapping

### [ScriptWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/ScriptWindow.java)
- Add class-level Javadoc explaining the GeckoSCRIPT IDE window
- Document the script compilation and execution pipeline

### [SelectableLanguages.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/SelectableLanguages.java)
- Add Javadoc to enum explaining available GUI languages

### [SemiconductorLossCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SemiconductorLossCalculatable.java)
- Add Javadoc on interface

### [ShortArrayCache.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ShortArrayCache.java)
- Add class-level Javadoc explaining short array cache

### [ShortMatrixCache.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ShortMatrixCache.java)
- Add class-level Javadoc explaining short matrix cache

### [SignalCalculatorExternalWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorExternalWrapper.java)
- Add class-level Javadoc explaining wrapper exposing parameters as runtime inputs
- Add Javadoc on all index constants

### [SignalCalculatorImport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorImport.java)
- Add class-level Javadoc explaining imported data table interpolation
- Add `@param dataTable` Javadoc explaining format
- Add Javadoc on linear interpolation logic

### [SignalCalculatorRandom.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorRandom.java)
- Add class-level Javadoc: "Generates random walk signal"

### [SignalCalculatorRectangle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorRectangle.java)
- Add class-level Javadoc: "Generates rectangular wave with duty cycle"

### [SignalCalculatorSinus.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorSinus.java)
- Add class-level Javadoc: "Generates A*sin(2*pi*f*t - phase) + DC offset"

### [SignalCalculatorTriangle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorTriangle.java)
- Add class-level Javadoc: "Generates triangular wave with duty cycle"

### [SignalDataContainerMean.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/SignalDataContainerMean.java)
- Add class-level Javadoc explaining signal mean data container

### [SignalDataContainerRegular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/SignalDataContainerRegular.java)
- Add class-level Javadoc explaining regular signal data container

### [SignalStateDrawer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SignalStateDrawer.java)
- Add class-level Javadoc explaining factory pattern for signal state drawing

### [SignumCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignumCalculator.java)
- Add class-level Javadoc: "Calculates signum: -1, 0, or 1"

### [SimpleControlBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SimpleControlBlock.java)
- Add class-level Javadoc explaining base for info-only control blocks

### [SimpleGraferPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SimpleGraferPanel.java)
- Add class-level Javadoc explaining simple grafer panel wrapper

### [SimulationAccess.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/SimulationAccess.java)
- Add class-level Javadoc explaining scripting access to simulation engine
- Add `@param`/`@return` to all methods
- Document thread-safety: which methods must be called on EDT

### [SimulationKernel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationKernel.java)
- Add class-level Javadoc explaining the core time-stepping simulation engine
- Document public static mutable fields (`tSTART`, `tEND`, `counter`)
- Document `SimulationStatus` enum

### [SimulationRunner.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SimulationRunner.java)
- Add class-level Javadoc explaining simulation lifecycle management
- Add Javadoc to `startCalculation()`, `continueCalculation()`, `pauseSimulation()`

### [SinCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SinCalculator.java)
- Add class-level Javadoc: "Calculates sine"

### [Slider.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Slider.java)
- Add class-level Javadoc explaining slider measurement component

### [SliderContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SliderContainer.java)
- Add class-level Javadoc explaining slider container with measurement modes

### [SliderUtils.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SliderUtils.java)
- Add class-level Javadoc explaining slider calculation utilities

### [SliderValues.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SliderValues.java)
- Add class-level Javadoc explaining slider measurement value container

### [SlidingDFTCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SlidingDFTCalculator.java)
- Add class-level Javadoc explaining Sliding Discrete Fourier Transform
- Add Javadoc on all fields
- Add Javadoc on `doSlidingFourierStep()` explaining recursive DFT update formula

### [SmallSignalCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SmallSignalCalculator.java)
- Add class-level Javadoc explaining Small Signal Analysis via multi-sine excitation and FFT
- Add Javadoc on `tearDownOnPause()` explaining FFT and Bode calculation
- Document static `_bode` field (shared across instances)

### [SolverSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SolverSettings.java)
- Add class-level Javadoc explaining user-configurable solver parameters
- Document each field (`SOLVER_TYPE`, `_T_pre`, `_dt_pre`, `dt`, `_tDURATION`, `_tPAUSE`)
- Make `SOLVER_TYPE` final for consistency (or document why mutable)

### [SolverType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SolverType.java)
- Add Javadoc to enum explaining available solver types (TRAPEZOIDAL, BACKWARD_EULER, GEAR_SHICHMAN)

### [SourceFileGenerator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/SourceFileGenerator.java)
- Add class-level Javadoc explaining Java source code generation from block definitions

### [SourceType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SourceType.java)
- Add Javadoc on enum

### [SpaceVectorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SpaceVectorCalculator.java)
- Add class-level Javadoc explaining 9-input display feeding
- Document `NO_INPUTS = 9` (what each of the 9 inputs represents)

### [SpaceVectorDisplay.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SpaceVectorDisplay.java)
- Add class-level Javadoc explaining real-time space vector display
- Document thread-safety concern of `counter` static field
- Document `HISTORY_BUFFER_SIZE = 100000` memory implications

### [SparseMatrixCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SparseMatrixCalculator.java)
- Add class-level Javadoc explaining sparse matrix converter modulation algorithm
- Document massive code duplication in switch statements for sectors 1-12

### [SpecialNameVisible.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SpecialNameVisible.java)
- Add Javadoc on `isNameVisible()` and `setNameVisible()` methods

### [SpecialType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SpecialType.java)
- Add class-level Javadoc explaining "special" non-circuit component types

### [SpecialTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SpecialTypeInfo.java)
- Add Javadoc on class explaining special (non-circuit) type info

### [SqrtCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SqrtCalculator.java)
- Add class-level Javadoc: "Calculates square root"

### [SquareCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SquareCalculator.java)
- Add class-level Javadoc: "Calculates x^2"

### [StartFromBlocksWithoutPredecessorOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromBlocksWithoutPredecessorOrderer.java)
- Add class-level Javadoc explaining ordering starting from no-input blocks

### [StartFromBlocksWithoutSuccessorOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromBlocksWithoutSuccessorOrderer.java)
- Add class-level Javadoc explaining ordering starting from no-output blocks

### [StartFromSinkOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromSinkOrderer.java)
- Add class-level Javadoc explaining ordering starting from sink blocks

### [StartFromSourceOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromSourceOrderer.java)
- Add class-level Javadoc explaining ordering starting from source blocks

### [StartsWithMatcher.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/StartsWithMatcher.java)
- Add class-level Javadoc explaining prefix matching

### [StartupWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/StartupWindow.java)
- Add class-level Javadoc explaining startup splash window
- Document blocking vs. non-blocking startup

### [StateSpaceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StateSpaceCalculator.java)
- Add Javadoc on `calculateTimeStep()` explaining trapezoidal integration
- Document `MAX_DEGREE_DIFF = 3` and the intentional switch fallthrough

### [SubCircuitSheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SubCircuitSheet.java)
- Add class-level Javadoc explaining circuit sheet inside a subcircuit block

### [SubCircuitTerminable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SubCircuitTerminable.java)
- Add class-level Javadoc explaining terminals bridging subcircuit sheet and parent block
- Document each method

### [SubcircuitBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SubcircuitBlock.java)
- Add Javadoc on class explaining subcircuit container
- Fix `if(1>0) return true;` in `areTerminalPositionsOK()` -- implement properly or explain
- Add Javadoc on `copyFabric()`, `paintIndividualComponent()`, `getColorForTerminal()`

### [SubtractionMoreParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SubtractionMoreParameter.java)
- Add class-level Javadoc: "Subtracts all subsequent inputs from first"

### [SubtractionTwoParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SubtractionTwoParameter.java)
- Add class-level Javadoc: "Subtracts second input from first"

### [SuggestMatcher.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SuggestMatcher.java)
- Add Javadoc to interface explaining matching contract

### [SuggestionField.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SuggestionField.java)
- Add class-level Javadoc explaining auto-suggest text field

### [SwitchState.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SwitchState.java)
- Add Javadoc on class explaining switch state snapshot
- Add Javadoc on inner `State` enum

### [SwitchingLossCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/SwitchingLossCurve.java)
- Add class-level Javadoc (replace informal `// //` comment)
- Add Javadoc to constructor `@param tj` (junction temperature C), `@param uBlock` (blocking voltage V)
- Add Javadoc to `importIndividual()` explaining legacy data-repair when `data.length == 4`

### [SymbolSettable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SymbolSettable.java)
- Add class-level Javadoc describing symbol setter interface

### [SymmetricDoubleSparseMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SymmetricDoubleSparseMatrix.java)
- Add class-level Javadoc explaining sparse symmetric matrix using HashMaps
- Document all methods

### [SymmetricSparseMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SymmetricSparseMatrix.java)
- Add class-level Javadoc expanding on CSR format explanation
- Document `factorize` parameters
- Document magic `mtype` value `-2`

### [SystemOutputRedirect.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/SystemOutputRedirect.java)
- Replace empty class-level Javadoc with description of stdout/stderr redirection utility
- Add Javadoc to `init()`, `setAlternativeOutput()`, `setOriginalOutput()`, `setConsoleOutput()`, `reset()`

### [TanCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/TanCalculator.java)
- Add class-level Javadoc: "Calculates tangent"
- Document `SMALL_NUMBER` constant and PI/2 singularity assertions

### [TechFormat.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/TechFormat.java)
- Add class-level Javadoc explaining technical number formatting utility
- Document format patterns (scientific, engineering notation)

### [TerminalCircuit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/TerminalCircuit.java)
- Add Javadoc on class (leaf class)

### [TerminalCircuitDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/TerminalCircuitDialog.java)
- Add Javadoc on class

### [TerminalConnection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalConnection.java)
- Add class-level Javadoc explaining Connection's start/end point as terminal
- Document `Location` enum, constructor, all methods

### [TerminalControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControl.java)
- Add class-level Javadoc explaining control-circuit terminal with node numbering
- Document the fragile value-to-string formatting via `substring`

### [TerminalControlBidirectional.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControlBidirectional.java)
- Add class-level Javadoc explaining bidirectional control terminals
- Document constructor and `paintLabelString`

### [TerminalControlInput.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControlInput.java)
- Add class-level Javadoc explaining control input terminal
- Document constructor, `paintComponent`, `createCopy`, `paintLabelString`

### [TerminalControlInputWithLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TerminalControlInputWithLabel.java)
- Add class-level Javadoc explaining extended input terminal with label

### [TerminalControlOutput.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControlOutput.java)
- Add class-level Javadoc explaining control output terminal (triangle)
- Document constructor, `createCopy`, `paintLabelString`

### [TerminalControlOutputWithLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TerminalControlOutputWithLabel.java)
- Add class-level Javadoc explaining extended output terminal with label

### [TerminalFixedPosition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalFixedPosition.java)
- Add class-level Javadoc explaining fixed absolute position terminal
- Document constructor parameters

### [TerminalFixedPositionInvisible.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalFixedPositionInvisible.java)
- Promote inline comment to proper class-level Javadoc
- Document `paintLabelString(Graphics2D, int)` signature difference

### [TerminalHiddenSubcircuit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalHiddenSubcircuit.java)
- Add class-level Javadoc explaining invisible internal subcircuit terminal

### [TerminalInterface.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalInterface.java)
- Add class-level Javadoc explaining the base interface for all terminal types
- Document each method

### [TerminalRelativeFixedDirection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalRelativeFixedDirection.java)
- Add class-level Javadoc explaining fixed direction terminal

### [TerminalRelativePosition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalRelativePosition.java)
- Add class-level Javadoc explaining relative position terminal following parent rotation
- Document `getRelativeX`, `getRelativeY`, `getPointFromDirection`

### [TerminalRelativePositionReluctance.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalRelativePositionReluctance.java)
- Add class-level Javadoc explaining reluctance-domain colored terminal

### [TerminalSubCircuitBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalSubCircuitBlock.java)
- Add method-level Javadoc to all methods

### [TerminalToWrap.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalToWrap.java)
- Add method-level Javadoc to all methods
- Document suspicious condition in `moveComponent` (`moveToPoint.x == moveToPoint.y`)

### [TerminalTwoPortComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalTwoPortComponent.java)
- Add class-level Javadoc explaining offset terminal
- Document `_isFlowSymbolTerminal` field

### [TerminalTwoPortRelativeFixedDirection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalTwoPortRelativeFixedDirection.java)
- Add class-level Javadoc explaining two-port terminal with fixed direction
- Document constructor parameters

### [TestReceiverCalculation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TestReceiverCalculation.java)
- Add class-level Javadoc explaining CISPR 16 test receiver calculations

### [TestReceiverWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TestReceiverWindow.java)
- Add class-level Javadoc explaining EMI test receiver window
- Document filtering/elimination methods (automatic peak selection algorithm)
- Document `getClassAValue()` and `getClassBValue()` with CISPR limit dBuV values

### [TextAreaOutputStream.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/TextAreaOutputStream.java)
- Add class-level Javadoc explaining output redirection to JTextArea
- Document thread-safety for writing to Swing from non-EDT threads

### [TextFieldBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TextFieldBlock.java)
- Add class-level Javadoc explaining display-only text annotation block
- Document `importIndividual()` newline replacement logic

### [TextFieldDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/TextFieldDialog.java)
- Add class-level Javadoc explaining text field properties dialog
- Document height scaling: spinner value divided by 2.0

### [TextInfoType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/TextInfoType.java)
- Add Javadoc on enum explaining display modes

### [TextSeparator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/TextSeparator.java)
- Add class-level Javadoc explaining text separator utility

### [ThGlobalTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThGlobalTerminal.java)
- Add Javadoc on class (leaf class)

### [ThTerminal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThTerminal.java)
- Add Javadoc on class (leaf class)

### [ThermAmbient.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermAmbient.java)
- Add Javadoc on class explaining reference temperature element

### [ThermAmbientDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermAmbientDialog.java)
- Add Javadoc on class explaining read-only temperature dialog

### [ThermMODUL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermMODUL.java)
- Add Javadoc on class explaining thermal power module

### [ThermPvChip.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermPvChip.java)
- Add Javadoc on class explaining heat source coupled to semiconductor
- Add Javadoc on `doCalculation()`, `doInitialization()`, `setzeSubcircuit()`

### [ThermPvChipDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermPvChipDialog.java)
- Add Javadoc on class

### [ThermalTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermalTypeInfo.java)
- Add Javadoc on class explaining thermal-domain type info

### [Thyristor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/Thyristor.java)
- Add Javadoc on `drawGateSymbol()`, `drawBackground()`, `drawForeground()`

### [ThyristorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThyristorCalculator.java)
- Add Javadoc on class explaining thyristor behavior (gate-triggered on, current-zero turn-off)
- Add Javadoc on `calculateCurrent()`, `setGateSignal()` override, `setTRR()`
- Document `REVERSE_FACTOR = 3.0` constant

### [ThyristorControlCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ThyristorControlCalculator.java)
- Add class-level Javadoc explaining 6-pulse thyristor converter gate signal generation
- Add Javadoc on all fields and `GateEvent` inner class
- Document constants `TN_X`, `TN_Y`, `THREE`, `THREE_HALF`

### [ThyristorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThyristorDialog.java)
- Add Javadoc on class

### [TimeCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/TimeCalculator.java)
- Add class-level Javadoc: "Outputs current simulation time"

### [TimeFunction.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TimeFunction.java)
- Add class-level Javadoc explaining time-dependent source functions with history tracking
- Document all protected fields and methods for back-stepping algorithm

### [TimeFunctionConstant.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TimeFunctionConstant.java)
- Add class-level Javadoc explaining constant (DC) time function
- Document constructor, `setValue`, `calculate`, `stepBack`
- Add comment explaining empty `stepBack()` is intentional

### [TimeSeriesArray.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TimeSeriesArray.java)
- Add class-level Javadoc explaining basic array-backed time series

### [TimeSeriesConstantDt.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TimeSeriesConstantDt.java)
- Add class-level Javadoc explaining constant-delta-t optimization (O(1) lookups)

### [TimeSeriesVariableArray.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TimeSeriesVariableArray.java)
- Add class-level Javadoc explaining variable-array for non-uniform spacing
- Document memory trade-off of `ArrayList<Double>` (boxed doubles)

### [TimeSeriesVariableBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TimeSeriesVariableBlock.java)
- Add class-level Javadoc explaining block-based variable time series optimization
- Implement or document `getLastTimeInterval()` (throws UnsupportedOperationException)

### [TokenMap.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TokenMap.java)
- Add method-level Javadoc to the many overloaded `readDataLine(...)` variants
- Document `SpecialPair`, `BlockInfo` inner classes
- Document `findSubBlock`, `createSubBlock`, `leseASCIITextBlock`, etc.

### [ToolBar.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ToolBar.java)
- Remove stale IDE template comment

### [TriggerPosition.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/TriggerPosition.java)
- Add class-level Javadoc explaining trigger position marker

### [UZiDisplay.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/UZiDisplay.java)
- Add class-level Javadoc explaining U vs Z*I trajectory display
- Fix class comment "SpaceVectorDisplay.java" (wrong filename)
- Document `counter` static field

### [UndoRedoManager.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/UndoRedoManager.java)
- Add class-level Javadoc explaining undo/redo coordination

### [UniqueObjectIdentifer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/UniqueObjectIdentifer.java)
- Add class-level Javadoc explaining unique object ID generation
- Document all methods

### [UserParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/UserParameter.java)
- Add class-level Javadoc explaining user-editable parameter model
- Add Javadoc to `setValue()`, `getValue()`, `setValueWithoutUndo()`
- Document undo integration and validation

### [VariableBusWidth.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/VariableBusWidth.java)
- Add class-level Javadoc explaining variable bus width detection

### [VariableExpression.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/expressionscripting/VariableExpression.java)
- Add class-level Javadoc explaining the class represents an expression containing variable references

### [VariableTerminalNumber.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/VariableTerminalNumber.java)
- Add class-level Javadoc explaining runtime variable terminal count interface
- Add Javadoc on `setInputTerminalNumber()` and `setOutputTerminalNumber()` contract

### [ViewMotorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ViewMotorCalculator.java)
- Add class-level Javadoc explaining display-only calculator

### [VoltageSourceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceCalculator.java)
- Add Javadoc on class explaining function-driven voltage source
- Add Javadoc on `stampVectorB()`, `stepBack()` override

### [VoltageSourceCurrentControlledCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceCurrentControlledCalculator.java)
- Add Javadoc on class explaining CCVS stamping

### [VoltageSourceDCMachineCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceDCMachineCalculator.java)
- Add Javadoc on class explaining DC machine EMF source
- Add Javadoc on `doPostProcess()` -- mechanical equation solver

### [VoltageSourceDIDTControlledCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceDIDTControlledCalculator.java)
- Add Javadoc on class explaining di/dt-controlled voltage source
- Clean up comment "nothing todo???"

### [VoltageSourceElectric.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceElectric.java)
- Add Javadoc on class (leaf class)

### [VoltageSourceReluctanceMMF.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceReluctanceMMF.java)
- Add Javadoc on class (leaf class)
- Add inline comment explaining "MMF" = Magnetomotive Force

### [VoltageSourceThermalTemperature.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceThermalTemperature.java)
- Add Javadoc on class (leaf class)

### [WeakListModel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/modelviewcontrol/WeakListModel.java)
- Add class-level Javadoc explaining the purpose (weak-reference ListModel to prevent memory leaks)
- Add Javadoc to all public methods (~20 methods have none)
- Add Javadoc to the three `fire*` methods explaining the event notification pattern
- Add Javadoc to fields `_listenerList`, `_present`, `_delegate`
- Document the `@SuppressWarnings("PMD")` annotation

### [WindowCloseable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/WindowCloseable.java)
- Add method-level Javadoc to `closeWindow()`
- Expand class Javadoc with implementation guidance

### [WorksheetSize.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/WorksheetSize.java)
- Add class-level Javadoc explaining worksheet (canvas) dimensions management
- Document all methods
- Document inconsistency: `DEFAULT_SIZE = 40` but `getOldFormatWSSize` returns `30` as default

### [XORCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/XORCalculator.java)
- Add class-level Javadoc: "Logical XOR of two inputs"

### [XSliderValueDrawer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/XSliderValueDrawer.java)
- Add class-level Javadoc explaining slider value label drawing on X axis

### [ZoomWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/ZoomWindow.java)
- Add class-level Javadoc explaining zoom window state management
- Document the flag-based approach (consider refactoring to enum)

### [bot/DLbot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/bot/DLbot.java)
- Document Wiki API interaction

### [bot/UPbot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/bot/UPbot.java)
- Document thread-safety model

### [resources/I18nKeys.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/resources/I18nKeys.java)
- Add class-level Javadoc explaining this enum holds all internationalizable strings
- Add Javadoc to `fabricFromKeyString()` explaining lazy initialization

### [translationtoolbox/PopupListener.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/PopupListener.java)
- Add class-level Javadoc explaining Ctrl+Shift+click popup trigger mechanism

### [translationtoolbox/TranslationPopupSingle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationPopupSingle.java)
- Add Javadoc to inner classes

### [translationtoolbox/TranslationTools.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationTools.java)
- Add Javadoc to inner classes `Task` and `Progress`
- Add inline documentation for generated component names

