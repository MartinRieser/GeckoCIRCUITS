# Improvement Tasks: ch/technokrat/gecko/geckocircuits/control/ (168 files, part 2: ControlTAN-VariableTerminalNumber)

## ControlTAN.java
- Add class-level Javadoc explaining tangent computation

## ControlTEMP.java
- Add class-level Javadoc explaining thermal potential (temperature) measurement

## ControlTERMINAL.java
- Add class-level Javadoc explaining control-domain subcircuit terminal
- Document `_wrapped` field
- Complete `clickedTerminal()` Javadoc `@param`/`@return` tags
- Document magic number `3.14` in `ControlTerminalCalculator.calculateYOUT()` (looks like placeholder)

## ControlTerminalDialog.java
- Add class-level Javadoc explaining terminal label dialog

## ControlThyristorControl.java
- Add class-level Javadoc explaining 6-pulse thyristor rectifier gate signal generation
- Document `TN_Y` constant (set to 6, for 6 gate outputs)
- Document `_onTime`, `_initFreq`, `_phaseShift` UserParameters with units

## ControlTIME.java
- Add class-level Javadoc explaining simulation time output

## ControlToEXTERNAL.java
- Add class-level Javadoc explaining control signal export to external program
- Document static `toExternals` ArrayList (thread-safety, lifecycle)
- Add Javadoc on `compareTo()` and `Comparable` implementation

## ControlTransferFunction.java
- Add class-level Javadoc explaining transfer function H(s) via state-space
- Document `_zeros`, `_poles` field conventions (real/imaginary interleaved)
- Fix confusingly named tokens: "nominatorPoles" exports `_poles` and "denominatorZeros" exports `_zeros` (appear swapped)
- Document potential bug in `clearPolesAndZeros()`

## ControlType.java
- Add Javadoc on enum explaining TRANSFER, SINK, SOURCE classification

## ControlTypeInfo.java
- Add class-level Javadoc explaining type metadata for control components
- Document that `fabric()` catches `Throwable` and returns null
- Investigate: constructor parameter `typeDescriptionVerbose` never passed to super()

## ControlU_ZI.java
- Add class-level Javadoc explaining U-Z*I trajectory plot
- Document magic numbers `br = 1.4` and `da = 0.4`
- Document `setTerminalNodeLabel()` parameters

## ControlVIEWMOT.java
- Add class-level Javadoc explaining machine internal quantity reading
- Document `getParameterString()` 3-element parameter string convention

## ControlViewMotDialog.java
- Add class-level Javadoc explaining machine/variable selection dialog

## ControlVOLT.java
- Add class-level Javadoc explaining voltage measurement between labeled nodes

## ControlVOLTDialog.java
- Add class-level Javadoc explaining voltage measurement configuration dialog
- Document the mutual-exclusion listener logic between combo boxes

## ControlWithSingleReference.java
- Add class-level Javadoc explaining base class for single-reference control blocks

## DataSaver.java
- Add Javadoc on `doManualSave()`, `doManualSaveBlocking()`, `abortSave()`
- Document `WAIT_COUNTER` static AtomicInteger
- Fix copy-paste bug: catches `IOException` but logs using `DialogDataExport.class.getName()` instead of `DataSaver.class`
- Add Javadoc on inner classes `AbstractLinePrinter`, `TxtLinePrinter`, `BinaryLinePrinter`

## DialogControlVariableInputs.java
- Add class-level Javadoc explaining input terminal count dialog

## DialogDataExport.java
- Add class-level Javadoc explaining data export dialog
- Document the `_inFillLists` flag and `Thread.sleep(100)` race-condition workaround
- Replace hardcoded developer path `/home/andreas/testFile.txt` with a sensible default

## DialogElementCONTROL.java
- Add class-level Javadoc explaining base dialog for all control component parameters
- Fix typo "git an XException" -> "get an XException"

## DialogExternal.java
- Add class-level Javadoc explaining Simulink external interface configuration
- Fix class comment filename "DialogExterna.java" -> "DialogExternal.java"
- Document `_regelBlock` field (German for "control block")

## DialogLabelEingeben.java
- Add class-level Javadoc explaining terminal/connection label editing dialog

## DialogMuxDemux.java
- Add class-level Javadoc explaining MUX/DEMUX port count configuration
- Fix title hardcoded as "External interface" (copy-paste error -- this is for Mux/Demux)

## DialogPanelVoltageMeasurement.java
- Add class-level Javadoc explaining voltage measurement node selection panel

## DialogSimpleInfoMessage.java
- Add class-level Javadoc explaining simple info message dialog

## DialogSmallSignalAnalysis.java
- Add class-level Javadoc explaining small-signal analysis (Bode plot) configuration dialog
- Document `oldTend` field
- Remove `System.out.println` debug output on line 200

## DialogSSAPlot.java
- Add class-level Javadoc explaining Bode plot display dialog
- Document the icon-loading catch block (silently swallows exceptions)
- Remove commented-out code blocks

## DialogThyristorControl.java
- Add class-level Javadoc explaining thyristor gate control parameter dialog
- Document gate-on-time unit conversion (spinner shows ms, parameter stored in seconds)

## DialogTransferFunction.java
- Add class-level Javadoc explaining transfer function editing dialog
- Document `_inPolynomialMode` field
- Fix method name typo "Cofficients" -> "Coefficients"
- Document why imaginary values use `Math.abs()` (enforces conjugate complex pairs)

## DialogWindowWithoutInput.java
- Add class-level Javadoc explaining minimal dialog for parameterless control blocks

## DragTest.java
- Document as standalone test/demo class or mark for removal
- Remove all `System.out.println` debug output

## FractionPrinter.java
- Add class-level Javadoc explaining transfer function fraction rendering
- Add Javadoc on `hsSetText()`, `setNumeratorText()`, `setDenominatorText()`
- Document `JLabelLine` inner class

## IsDtChangeSensitive.java
- Add Javadoc on `initWithNewDt()` explaining when it is called
- Remove stale template comment
- Fix grammar: "This function should called" -> "This function should be called"

## ListDnD.java
- Document as drag-and-drop test utility or mark for removal
- Make `arrayListHandler` field private

## LoopDetectionException.java
- Add class-level Javadoc explaining when this is thrown (control algebraic loop detected)
- Implement or remove commented-out `printLoopMessage()`

## MemoryInitializable.java
- Add Javadoc on `doInit()` explaining simulation start initialization

## NetlistControl.java
- Add class-level Javadoc explaining control-domain netlist management
- Add Javadoc on all factory methods
- Document `IndexConnection` inner class
- Remove large commented-out code blocks in `calculateTimeStep()`
- Translate German method name `optimiereAbarbeitungsListe()` or provide English Javadoc

## NotCalculateableMarker.java
- Fix typos "computatoin" -> "computation" and "simualtion" -> "simulation"

## Operationable.java
- Add Javadoc on interface explaining scriptable operations
- Add Javadoc on `OperationInterface` abstract class, `fabricFromString()`, `doOperation()`

## Point.java
- Add Javadoc on class, constructor, `distance()`, `equals()`, `hashCode()`, `toString()`
- Document magic numbers `7` and `89` in `hashCode()`

## PolynomTools.java
- Add class-level Javadoc explaining polynomial manipulation utilities
- Document or remove `main()` test method
- Fix Javadoc error: `[1 2 0 4] == 1 + 2s + 4s^4` should be `1 + 2s + 4s^3`
- Document potential bug on line 172: `polynomImag[1 + k]` uses `tmpReal[k]` instead of `tmpImag[k]`
- Remove commented-out code block

## PreviewDialog.java
- Add class-level Javadoc explaining signal preview dialog base class

## PreviewDialogRectangular.java
- Add class-level Javadoc explaining rectangular waveform preview
- Add inline comments for magic numbers (b=160, h=110, p1=10, p2=3...)
- Translate German variable names (`dreieck`, `aufsteigend`, `tastverhaeltnis`, `frequenz`)

## PreviewDialogSine.java
- Add class-level Javadoc explaining sinusoidal waveform preview

## PreviewDialogTriangle.java
- Add class-level Javadoc explaining triangular waveform preview

## PriorityThreadFactory.java
- Add class-level Javadoc explaining low-priority thread factory
- **Bug**: `newThread()` creates a thread, sets priority, but then returns a *new* `Thread(r)` without the priority -- fix by returning the first thread

## QuasiPeakCalculator.java
- Add class-level Javadoc explaining CISPR 16 quasi-peak detector
- Document CISPR band constants (`A_LOWER_LIMIT`, `B_LOWER_LIMIT`, etc.)
- Add Javadoc on `quasiPeakDetector()` explaining backward Euler discretization
- Document the `System.gc()` call (generally discouraged)

## ReportingListTransferHandler.java
- Add class-level Javadoc explaining drag-and-drop reordering handler
- Add Javadoc on `importData()`, `exportDone()`, `createTransferable()`

## ScopeSignalSimpleName.java
- Add class-level Javadoc explaining simple scope signal wrapper

## SimpleControlBlock.java
- Add class-level Javadoc explaining base for info-only control blocks

## SpaceVectorDisplay.java
- Add class-level Javadoc explaining real-time space vector display
- Document thread-safety concern of `counter` static field
- Document `HISTORY_BUFFER_SIZE = 100000` memory implications
- Remove commented-out `main()` method
- Document `_old_time` field (assigned but never read -- potential dead code)

## SpecialNameVisible.java
- Add Javadoc on `isNameVisible()` and `setNameVisible()` methods

## SSAShape.java
- Add Javadoc on enum explaining excitation signal shapes for small-signal analysis
- Add Javadoc on each enum constant

## StartFromBlocksWithoutPredecessorOrderer.java
- Add class-level Javadoc explaining ordering starting from no-input blocks

## StartFromBlocksWithoutSuccessorOrderer.java
- Add class-level Javadoc explaining ordering starting from no-output blocks

## StartFromSinkOrderer.java
- Add class-level Javadoc explaining ordering starting from sink blocks

## StartFromSourceOrderer.java
- Add class-level Javadoc explaining ordering starting from source blocks

## StateSpaceCalculator.java
- Add Javadoc on `calculateTimeStep()` explaining trapezoidal integration
- Document `MAX_DEGREE_DIFF = 3` and the intentional switch fallthrough

## SubCircuitSheet.java
- Add class-level Javadoc explaining circuit sheet inside a subcircuit block
- Document button action listeners and `wireModeVersteckt` field (German)

## TerminalControlInputWithLabel.java
- Add class-level Javadoc explaining extended input terminal with label
- Document magic numbers in position calculations (`1.75`, `1.2`, `3/4` font scaling)

## TerminalControlOutputWithLabel.java
- Add class-level Javadoc explaining extended output terminal with label

## TestReceiverCalculation.java
- Add class-level Javadoc explaining CISPR 16 test receiver calculations
- Rename misleading variable `thread` to `calculator`

## TestReceiverWindow.java
- Add class-level Javadoc explaining EMI test receiver window
- Document filtering/elimination methods (automatic peak selection algorithm)
- Document `getClassAValue()` and `getClassBValue()` with CISPR limit dBuV values
- Remove commented-out code

## TextFieldBlock.java
- Add class-level Javadoc explaining display-only text annotation block
- Document `importIndividual()` newline replacement logic
- Document magic numbers `1.333` and `3.6`

## TextFieldDialog.java
- Add class-level Javadoc explaining text field properties dialog
- Document height scaling: spinner value divided by 2.0

## UZiDisplay.java
- Add class-level Javadoc explaining U vs Z*I trajectory display
- Fix class comment "SpaceVectorDisplay.java" (wrong filename)
- Document `counter` static field

## VariableTerminalNumber.java
- Add class-level Javadoc explaining runtime variable terminal count interface
- Add Javadoc on `setInputTerminalNumber()` and `setOutputTerminalNumber()` contract
