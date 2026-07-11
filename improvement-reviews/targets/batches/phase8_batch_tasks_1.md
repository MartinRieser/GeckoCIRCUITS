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


