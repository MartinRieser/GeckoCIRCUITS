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


