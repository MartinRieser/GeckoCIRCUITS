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


