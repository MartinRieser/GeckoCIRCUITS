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


