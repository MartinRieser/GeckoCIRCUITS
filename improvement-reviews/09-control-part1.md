# Improvement Tasks: ch/technokrat/gecko/geckocircuits/control/ (101 files, part 1: A-ControlSubtraction)

## AbstractControlOrderer.java
- Add Javadoc on class explaining topological sorting of control blocks
- Document `MAX_ITERATION_COUNT` constant (why 10000)
- Remove debug print statements checking for "OR.1" / "SPARSEMATRIX.2"
- Remove commented-out block in `addNodesToNextList()`
- Add Javadoc on abstract methods

## AbstractControlPT.java
- Add class-level Javadoc explaining base class for PT1/PT2 transfer functions
- Add Javadoc on `_TVal` and `_a1Val` UserParameters

## AbstractControlSingleInputSingleOutput.java
- Add class-level Javadoc explaining 1-input/1-output base class

## AbstractControlVariableInputs.java
- Add Javadoc on `setInputTerminalNumber()`, `setOutputTerminalNumber()`
- Document `_inputTerminalNumber` UserParameter field

## AbstractCurrentMeasurement.java
- Add class-level Javadoc explaining current measurement from coupled component
- Add Javadoc on inner classes `CurrentCalculation`, `MOSFETCurrentCalculation`

## AbstractDialogWithExternalOption.java
- Add class-level Javadoc explaining "Use external parameters" checkbox dialog
- Add Javadoc on `getComponentsDisabledExternal()`

## AbstractInversTrigFunction.java
- Add class-level Javadoc explaining base for inverse trig functions

## AbstractPotentialMeasurement.java
- Add class-level Javadoc explaining potential (voltage) measurement
- Add Javadoc on `checkComponentCompatibility()`, `addTextInfoParameters()`

## AbstractSinkControlOrderer.java
- Add class-level Javadoc explaining orderer starting from sink blocks

## AbstractSourceControlOrderer.java
- Add class-level Javadoc explaining orderer starting from source blocks

## AbstractTrigonometricFunction.java
- Add class-level Javadoc explaining base for trig functions

## BlockOrderOptimizer3.java
- Add class-level Javadoc explaining priority-based block ordering algorithm
- Remove commented-out `System.out.println` debug statements
- Remove redundant duplicate `import java.util.*`

## CheckBoxList.java
- Add class-level Javadoc explaining JList with checkbox-style toggle selection
- Remove debug `System.out.println` statements
- Remove `main()` method (test/demo code in library class)
- Document inner class `CheckBoxListCellRenderer`

## Cispr16Settings.java
- Add class-level Javadoc explaining CISPR-16 EMI test receiver configuration
- Document `_qpInteval` field name typo ("Inteval" should be "Interval")

## CisprBlockSettings.java
- Add class-level Javadoc explaining CISPR-16 component settings dialog

## CisprDataExport.java
- Add class-level Javadoc explaining CISPR-16 data export dialog
- Add Javadoc on `calculateInverseDbMu()`, `doSave()`, `saveData()`
- Consider using try-with-resources for `BufferedWriter`

## ComplexPrinter.java
- Add Javadoc on class explaining complex number formatting for UI display
- Fix indentation of class body

## ControlABCDQ.java
- Add class-level Javadoc explaining ABC to DQ (Park/Clarke) transformation

## ControlAbsolutValue.java
- Add class-level Javadoc explaining absolute value computation
- Fix class name typo: "AbsolutValue" should be "AbsoluteValue"

## ControlAdd.java
- Add class-level Javadoc explaining summation of all input signals

## ControlAmperemeter.java
- Add class-level Javadoc explaining electrical current measurement
- Add Javadoc on `checkComponentCompatibility()`

## ControlAmpereMeterDialog.java
- Add class-level Javadoc explaining amperemeter/flowmeter dialog

## ControlAnd.java
- Add class-level Javadoc explaining logical AND over all inputs

## ControlAreaCosine.java
- Add class-level Javadoc explaining arc cosine computation

## ControlAreaSine.java
- Add class-level Javadoc explaining arc sine computation

## ControlAreaTangens.java
- Add class-level Javadoc explaining arc tangent computation

## ControlBlock.java
- Add class-level Javadoc explaining abstract base class for all control blocks
- Document all visual layout fields (`pFa`, `pFb`, `xFl`, `yFl`, etc.)
- Add Javadoc on constants `EMPTY_OUTPUT`, `SIGNAL_THRESHOLD`, `DISP_DIGITS`, `WIDTH`
- Remove commented-out `System.out.println` in `getOutputs()`

## ControlBlockSimulink.java
- Add class-level Javadoc explaining Simulink-interface control blocks base

## ControlCISPR16.java
- Add class-level Javadoc explaining CISPR-16 EMI test receiver implementation
- Document `DA_OFFSET` and `DI_OFFSET` constants
- Add Javadoc on `CisprCalculator` inner class

## ControlComponentType.java
- Add class-level Javadoc explaining integer type ID to component type mapping
- Document non-sequential integer IDs and gaps

## ControlConstant.java
- Add class-level Javadoc explaining constant value output

## ControlConstantDialog.java
- Add class-level Javadoc explaining constant value parameter dialog

## ControlControlDebug.java
- Add class-level Javadoc explaining debugging/breakpoint control block

## ControlCosine.java
- Add class-level Javadoc explaining cosine computation

## ControlCounter.java
- Add class-level Javadoc explaining rising-edge counter

## ControlDebugWindow.java
- Add class-level Javadoc explaining debug stepping dialog window
- Add TODO implementation for jButton1ActionPerformed or document as placeholder
- Document purpose of each jButton

## ControlDelay.java
- Add class-level Javadoc explaining signal delay block
- Add Javadoc on `_tDelay` UserParameter and `DEFAULT_DELAY` constant

## ControlDelayDialog.java
- Add class-level Javadoc explaining delay time parameter dialog

## ControlDemux.java
- Add class-level Javadoc explaining demultiplexer
- Remove auto-generated comment "To change body of generated methods"

## ControlDivision.java
- Add class-level Javadoc explaining division with division-by-zero warning

## ControlDQABC.java
- Add class-level Javadoc explaining DQ to ABC (inverse Park/Clarke) transformation

## ControlEqual.java
- Add class-level Javadoc explaining equality check

## ControlExclusiveOr.java
- Add class-level Javadoc explaining logical XOR

## ControlExponential.java
- Add class-level Javadoc explaining e^x computation

## ControlFlowMeter.java
- Add class-level Javadoc explaining heat flow/power measurement from thermal components
- Add Javadoc on `_measurementType` field and `LossComponent` enum

## ControlFluxMeter.java
- Add class-level Javadoc explaining magnetic flux measurement

## ControlFromEXTERNAL.java
- Add class-level Javadoc explaining signal import from external (Simulink) interface
- Document `fromExternals` static list and lifecycle management
- Translate German comments to English

## ControlGain.java
- Add class-level Javadoc explaining gain multiplication
- Add Javadoc on `_gain` UserParameter

## ControlGainDialog.java
- Add class-level Javadoc explaining gain parameter dialog
- Translate German comments to English

## ControlGate.java
- Add class-level Javadoc explaining gate signal control for switch components
- Remove trailing semicolon after method body on line 64

## ControlGateDialog.java
- Add class-level Javadoc explaining switch selection dialog

## ControlGlobalTerminal.java
- Add class-level Javadoc explaining cross-subcircuit control terminal
- Document `ALL_GLOBALS` static set

## ControlGreaterEqual.java
- Add class-level Javadoc explaining >= comparison

## ControlGreaterThan.java
- Add class-level Javadoc explaining > comparison

## ControlHysteresis.java
- Add class-level Javadoc explaining hysteresis comparator
- Document `DEF_HYS_THRES`, `_stashedTerminal` field

## ControlHysteresisDialog.java
- Add class-level Javadoc explaining hysteresis configuration dialog
- Document `IMAGE_COMPONENT_WIDTH` and `IMAGE_COMPONENT_HEIGHT` constants

## ControlImportDialog.java
- Add class-level Javadoc explaining data import dialog
- Remove large commented-out code block

## ControlImportFromFile.java
- Add class-level Javadoc explaining file-based signal source

## ControlInputTwoTerminalStateable.java
- Add class-level Javadoc explaining folded/expanded terminal state interface

## ControlIntegrator.java
- Add class-level Javadoc explaining integrator with limits
- Add Javadoc on `_a1Val`, `_y0Val`, `_minLimit`, `_maxLimit` UserParameters

## ControlIntegratorDialog.java
- Add class-level Javadoc explaining integrator configuration dialog
- Translate German comments to English

## ControlLimit.java
- Add class-level Javadoc explaining signal limiter
- Document all layout constants

## ControlLimitDialog.java
- Add class-level Javadoc explaining limiter configuration dialog
- Fix duplicated `//CHECKSTYLE:OFF` comment (should be `//CHECKSTYLE:ON`)

## ControlLN.java
- Add class-level Javadoc explaining natural logarithm computation

## ControlMAX.java
- Add class-level Javadoc explaining maximum of all inputs
- Add `serialVersionUID` field (missing)

## ControlMIN.java
- Add class-level Javadoc explaining minimum of all inputs
- Add `serialVersionUID` field (missing)

## ControlMMF.java
- Add class-level Javadoc explaining magnetomotive force measurement
- Add `serialVersionUID` field (missing)

## ControlMUL.java
- Add class-level Javadoc explaining multiplication of all inputs
- Add Javadoc on `TwoParameterMultiplication` and `MoreParameterMultiplication` inner classes

## ControlMUX.java
- Add class-level Javadoc explaining multiplexer
- Remove dead variables `d1`, `d2`, `dpfx`, `dpfy` in `drawBlockRectangle()`
- Fix typo "carful" -> "careful"

## ControlNE.java
- Add class-level Javadoc explaining not-equal check
- Add `serialVersionUID` field (missing)

## ControlNOT.java
- Add class-level Javadoc explaining logical NOT
- Add `serialVersionUID` field (missing)

## ControlOr.java
- Add class-level Javadoc explaining logical OR
- Add `serialVersionUID` field (missing)

## ControlOrderNode.java
- Add Javadoc on `calculateDirectInputs()`, `calculateDirectOutputs()`
- Add Javadoc on `setLoopCrackTrue()`, `getLoopCrack()` explaining "loop crack"
- Document `_loopCrack` field

## ControlOSZI.java
- Add class-level Javadoc explaining oscilloscope component
- Document all waveform/Fourier fields
- Add Javadoc on `initScope()`, `istAngeklickt()`, `copyFabric()`
- Fix potential bug: line 398 checks `settingsMap != null` but should check `windowSettingsMap`
- Remove commented-out debug line

## ControlPD.java
- Add class-level Javadoc explaining PD controller: G(s) = a1*s

## ControlPDDialog.java
- Add class-level Javadoc explaining PD controller parameter dialog
- Translate German comments to English

## ControlPI.java
- Add class-level Javadoc explaining PI controller
- Add Javadoc on `_r0`, `_a1`, `_TimeConstant` UserParameters and relationship (T = r0/a1)

## ControlPIDialog.java
- Add class-level Javadoc explaining PI parameter dialog with live cross-computation
- Translate German comments to English

## ControlPMSM_Modulator.java
- Add class-level Javadoc explaining PMSM modulator

## ControlPMSMCONTROL.java
- Add class-level Javadoc explaining PMSM field-oriented controller
- Add `serialVersionUID` field (missing)

## ControlPOW.java
- Add class-level Javadoc explaining power computation
- Add `serialVersionUID` field (missing)
- Note: calculator created with `super(1, 1)` but block has 2 inputs -- possible bug

## ControlPT1.java
- Add class-level Javadoc explaining PT1: G(s) = a1/(1+s*T)
- Add `serialVersionUID` field (missing)

## ControlPT2.java
- Add class-level Javadoc explaining PT2: G(s) = a1/(1+s*T)^2
- Add `serialVersionUID` field (missing)

## ControlPTDialog.java
- Add class-level Javadoc explaining PT1/PT2 parameter dialog
- Translate German comments to English

## ControlRandomDialog.java
- Add Javadoc description to the class (currently only `@author andy` with no summary), documenting that this dialog provides a simple info label for the Random signal source block
- Translate the German method name `baueGuiIndividual()` to English (e.g., `buildIndividualGUI()`); this is an override, so the rename must be coordinated with the superclass `DialogElementCONTROL`
- Add Javadoc to the constructor `ControlRandomDialog(ControlSignalSource element)` explaining the `element` parameter and that it delegates to the superclass

## ControlRandomWalk.java
- Add a Javadoc class description explaining that this block produces a random-walk signal by delegating to `ControlSignalSource` with the `QUELLE_RANDOM` source type
- Add Javadoc to the constructor documenting that it sets the source type to `QUELLE_RANDOM` via `setValueWithoutUndo`
- Add Javadoc to the `tinfo` field documenting its role as the registration metadata for the control framework
- Consider translating or documenting the German identifier `_typQuelle` (meaning "source type") and the enum constant `QUELLE_RANDOM` (QUELLE = "source" in German) if cross-file refactoring scope allows

## ControlRound.java
- Add a Javadoc class description documenting that this control block rounds its input to the nearest integer, using `RoundCalculator`
- Add Javadoc to `getOutputNames()`, `getInternalControlCalculatableForSimulationStart()`, `getCenteredDrawString()`, `openDialogWindow()`, and `getOutputDescription()` with `@return` and `@inheritDoc` tags as appropriate
- Add Javadoc to the `tinfo` field documenting its purpose in the control type registry

## ControlSampleHold.java
- Add a Javadoc class description documenting the sample-and-hold behavior: when control input z > 0.5 it samples x1; otherwise it holds the last sampled value
- Add Javadoc to the constructor `super(2, 1)` clarifying the meaning of the two arguments (2 inputs, 1 output)
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` and `openDialogWindow()`
- Extract the magic number `0.5` from the HTML dialog message string into a named constant (e.g., `THRESHOLD = 0.5`) and reference it so the displayed condition stays consistent with the calculator logic

## ControlSaveData.java
- Add a Javadoc class description documenting this block's role: it exports simulation data to a file (text or binary), with configurable formatting, signal selection, and save modes
- Fix typo/abbreviation in `"Cont. Saving"` (line 228) -- should be `"Continuous Saving"` or `"Cont. saving"` for consistency with other status strings
- Extract the magic number `100` (line 177: `while (testCounter < 100)`) into a named constant like `MAX_FILE_COUNTER = 100`
- Translate the German identifier `SaveModus` (Modus = "mode") to `SaveMode` -- note this is used in import/export serialization so field name changes are safe but ordinal-based enum storage must be preserved
- Fix potential bug in `setPercentageText`: `_dataSaver.getPercentage()` is called a second time in the else-branch string formatting (line 233) instead of reusing the already-passed `percentage` parameter
- Add Javadoc to `setSelectedSignal(int j, int i)` whose parameter names are ambiguous (`j` is the signal index value, `i` is the list position) -- clarify via `@param` tags or better parameter names

## ControlSignalSource.java
- Add a Javadoc class description documenting this as the primary signal source block supporting sine, triangle, rectangle, random, and file-imported waveforms with optional external terminals
- Translate German identifiers and string literals used in code: `typus` (line 262, 439 etc.), `fabrikSignalCalculator` (German "fabrizieren"), `aufsteigend`/`_dreieck` (line 119 comment), and German UserParameter keys `anteilDC`, `tastverhaeltnis`, `frequenz`
- Remove the stray empty semicolon statement on line 125 (bare `;` after the field declaration block)
- Extract magic numbers in `drawBlockRectangle`: `1/2.0` (line 234), `0.25` (line 236), `Y_SIZE` position multiplier, and the five hardcoded `drawString` label positions -- refactor the repetitive block (lines 238-247) into a loop over a label array
- Add Javadoc to `fabricSignalCalculator`, `readExternalDataFromFile`, `addImportParameters`, and the `getOperationEnumInterfaces` anonymous method
- Translate the German comment on line 119 (`"for TRI, RECHT-states we simple store variables 'aufsteigend' and '_dreieck'"`) to English

## ControlSignalSourceDialog.java
- Add a Javadoc class description documenting this dialog for configuring signal shape (rectangle/sine/triangle), amplitude, frequency, offset, phase, duty ratio, and display options
- Translate the German method name `baueGuiIndividual()` to English (e.g., `buildIndividualGUI()`), coordinated with superclass
- Extract the magic number `7` (line 75: `element.getParameter()[7]`) into a named constant (e.g., `DISPLAY_DETAILS_PARAM_INDEX = 7`) or, preferably, use `element._displayDetails.getValue()` directly for type safety
- Translate the German comment `// Abstand` on line 107 to English (means "spacing"/"gap")
- Add Javadoc to `getComponentsDisabledExternal()` and `processInputs()`

## ControlSignum.java
- Add a Javadoc class description documenting that this block computes the signum (sign) function: +1 for positive input, -1 for negative, 0 for zero
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` and `getDialogMessage()` with `@return` tags
- Add Javadoc to the `tinfo` field and constructor

## ControlSIN.java
- Add a Javadoc class description documenting that this block computes the sine of its input angle (trigonometric function block)
- Fix the double space in the class declaration: `public final  class` should be `public final class` (line 20)
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` and the `tinfo` field
- Consider adding `@Override` `getOutputNames()`/`getOutputDescription()` if they exist in the parent (they are inherited from `AbstractTrigonometricFunction` -- verify and add Javadoc or `@inheritDoc`)

## ControlSlidingDFT.java
- **Fix typo**: the constant `DEFAULT_FREQENCY` (line 34) should be `DEFAULT_FREQUENCY` -- note this is `private static final` so the rename is contained to this file
- Add a Javadoc class description documenting this block performs a sliding Discrete Fourier Transform (SDFT) for real-time spectral analysis at user-defined frequencies
- Fix the presentation name strings `"Frequency selection of SFFT"` (lines 312, 317, 322) -- "SFFT" is likely a typo for "SDFT" (Sliding DFT)
- Add Javadoc to the inner class `FrequencyData` and its `equals`/`hashCode` methods; note that `hashCode()` (line 189) casts a `long` to `int` which can produce collisions -- document this or use `Long.hashCode()`
- Add Javadoc to `setFrequencyDataWithUndoCheck`, `isUndoRequired`, `addDataPoint`, `removeLastFrequencyData`, and `OutputData.getFromIntCode`; also extract magic number `2` in `addDataPoint` (`newFrequencyValue * 2`) into a named constant documenting the doubling convention

## ControlSlidingDFTDialog.java
- Add a Javadoc class description documenting this dialog for configuring SDFT frequencies (add/remove) and selecting output data types (magnitude, real, imag, phase)
- Remove dead/commented-out code: line 89 `//addFormatJTextField(newFreq);` and line 177 `//addFormatJTextField(data._frequency.getValue());`
- Remove the IDE-generated boilerplate comment on line 184: `"//To change body of generated methods, choose Tools | Templates."`
- Translate the German method name `baueGuiIndividual()` to English (e.g., `buildIndividualGUI()`), coordinated with superclass
- Add Javadoc to `addParameterPanel()`, `addComboBox()`, `addFreqData()`, and `processInputIndividual()`, and document/extract the magic numbers in `new Dimension(20, 3)` (line 143) and `_grid.setColumns(5)` (line 114)

## ControlSmallSignalAnalysis.java
- Add a Javadoc class description documenting this block performs small-signal analysis by injecting a swept-frequency perturbation and measuring the system response
- Make `BLOCK_WIDTH` (line 36) `static final` since it is a constant currently declared as a plain instance field (`private final int BLOCK_WIDTH = 6`)
- Verify/fix the typo `SMALL_SIGNAL_ANALYIS` in the referenced `I18nKeys` constant (line 34) -- "ANALYIS" should be "ANALYSIS"; if the key itself is misspelled, this requires checking `I18nKeys` but the reference here should use the corrected name
- Extract magic numbers in the constructor: `-4`, `3`, `YOUT.size()` terminal position literals into named constants
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()`, `getBlockWidth()`, `getCenteredDrawString()`, `openDialogWindow()`, and `getOutputNames()`

## ControlSpaceVector.java
- **Fix inefficient string comparison**: line 65 uses `header[knotenIndex + 1].equals(new String(""))` -- replace with `.isEmpty()` or `.equals("")` to avoid unnecessary object creation
- Add a Javadoc class description documenting this block displays a space vector diagram for up to 9 input signals (3-phase systems)
- **Remove dead code**: the local variable `y` (line 73) is declared and assigned but never used in `drawBlockRectangle`
- Translate the German parameter name `knotenIndex` (line 58, meaning "node index") to `nodeIndex`, and translate the German comment on lines 59-60
- Extract magic numbers in `drawBlockRectangle`: `0.4`, `1.4` (WIDTH), `0.5` (DA_VALUE), `3`, `6`, `2`, and `ds1`/`ds2` into named constants; add Javadoc to `setTerminalNodeLabel`, `drawBlockRectangle`, `exportAsciiIndividual`, and `importIndividual`

## ControlSPARSEMATRIX.java
- **Bug -- array size mismatch**: `getOutputNames()` returns 9 elements but `getOutputDescription()` returns only 8 `I18nKeys` (line 60-61) -- add the missing 9th description element
- **Bug -- duplicate label**: line 34 creates a second terminal labeled `"uN2"`; given the sequence (uN1, uN2, ...), this should likely be `"uN3"`
- Add a Javadoc class description documenting this block implements Sparse Matrix converter control logic
- Rename the class from `ControlSPARSEMATRIX` to `ControlSparseMatrix` to comply with Java naming conventions (note: the `tinfo` string ID `"SPARSEMATRIX"` can remain unchanged for serialization)
- Extract magic numbers: `3 * dpix` (line 66) and `0.5` (line 71) into named constants; add Javadoc to all public/protected methods

## ControlSQR.java
- Add a Javadoc class description documenting that this block computes the square of its input (y = x^2)
- Add Javadoc to `getOutputNames()`, `getInternalControlCalculatableForSimulationStart()`, `openDialogWindow()`, and `getOutputDescription()` with appropriate `@return` and `@param` tags
- Add Javadoc to the `TYPE_INFO` field documenting its role in the control type registry

## ControlSQRT.java
- Add a Javadoc class description documenting that this block computes the square root of its input (y = sqrt(x))
- Add Javadoc to `getOutputNames()`, `getInternalControlCalculatableForSimulationStart()`, `openDialogWindow()`, and `getOutputDescription()`
- Add Javadoc to the `tinfo` field documenting its purpose in the control type registry

## ControlSubtraction.java
- Add a Javadoc class description documenting that this block subtracts inputs: with 2 inputs it computes x1 - x2; with more inputs it subtracts all subsequent inputs from the first
- Change the visibility of `tinfo` (line 22) from package-private to `public static final` for consistency with all other control blocks in this set (e.g., `ControlRound.tinfo`, `ControlSIN.tinfo`)
- Add Javadoc to `getInternalControlCalculatableForSimulationStart()` documenting the branching logic between `SubtractionTwoParameter` and `SubtractionMoreParameter`
- Add Javadoc to `getOutputNames()` and `getOutputDescription()`, and to the `tinfo` field
