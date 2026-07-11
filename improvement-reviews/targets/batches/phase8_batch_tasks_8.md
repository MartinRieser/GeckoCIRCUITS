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


