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


