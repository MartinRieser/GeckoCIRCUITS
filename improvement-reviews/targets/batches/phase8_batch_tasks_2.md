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


