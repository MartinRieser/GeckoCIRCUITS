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


