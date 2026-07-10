# Improvement Tasks: ch/technokrat/gecko/ (root package, 32 files)

## CallbackClientImpl.java
- Add Javadoc to the constructor explaining client hostname, user ID, connection timestamp
- Add Javadoc to `printSystemMessage(String)` and `printErrorMessage(String)`
- Add Javadoc to `ping()` documenting returned info
- Replace magic number `0x64` (100) with named constant
- Add field-level Javadoc for `_clientHostname`, `_clientUserID`, `_connectionDate`

## CallbackClientInterface.java
- Add class-level Javadoc explaining this is the RMI callback interface
- Convert non-Javadoc comments into proper `@param` Javadoc tags
- Add `@throws java.rmi.RemoteException` Javadoc tags to all methods

## CallbackServerInterface.java
- No improvements needed.

## Category.java
- Add class-level Javadoc explaining this annotation assigns a `MethodCategory` to remote interface methods
- Add Javadoc to `value()`

## ControlCalculatable.java
- Add Javadoc to `calculateYOUT(double[], double, double)` explaining parameters `xIN`, `time`, `deltaT`
- Add Javadoc to `init()` explaining when it is called in the simulation lifecycle
- Remove commented-out `serialVersionUID` (dead code)

## ControlCalculatableMatrix.java
- Add Javadoc to `calculateYOUT(double[][], double, double)` explaining the matrix-based calculation
- Add Javadoc to `init()` and `getOutputSignal()`

## Declaration.java
- Add class-level Javadoc explaining this annotation stores the method signature string
- Add Javadoc to `value()`

## Documentation.java
- Add class-level Javadoc explaining this annotation links a remote interface method to an i18n key
- Add Javadoc to `value()`

## ExternalGeckoCustom.java
- Replace empty class-level Javadoc with meaningful description
- Add Javadoc to the constructor documenting the `access` parameter
- Add Javadoc to `runScript()` explaining why it throws `UnsupportedOperationException`

## SystemOutputRedirect.java
- Replace empty class-level Javadoc with description of stdout/stderr redirection utility
- Add Javadoc to `init()`, `setAlternativeOutput()`, `setOriginalOutput()`, `setConsoleOutput()`, `reset()`

## OutputWarningStream.java
- Add class-level Javadoc explaining this monitors output volume and warns about excessive output
- Add Javadoc to all public methods
- Replace magic numbers (`50000000`, `100000`, `5`, `200`) with named constants
- Document why `byteCounter` and `warningBytesSize` are `static`
- Add field-level comments for `_verbosityWarnShown`, `_isOriginalOutput`, `_ignoreFutureMessages`

## MyProxy.java
- Add meaningful class-level Javadoc or remove if dead code (contains only `// TODO!!! asdf`)
- If keeping, add `@Deprecated` annotation

## MethodNameChecker.java
- Add Javadoc to `checkFabric()` explaining the assertion-based validation strategy
- Document parameters `checkMethods` and `containsMethodSignature`

## MethodCategory.java
- Add class-level Javadoc explaining this enum categorizes remote API methods
- Fix typo `_tranlsationKey` -> `_translationKey`
- Add Javadoc to enum constants and `toString()`

## JavaMemoryRestart.java
- Add Javadoc to `isMemoryRestartRequired(int)` documenting `userMemorySize` parameter
- Add Javadoc to `searchForReadyString()` and `createJVMCallCommands()`
- Document magic number `MEGA_BYTE = 1098300` (unusual value)
- Fix inconsistent indentation on lines 181-182
- Remove trailing semicolon after class closing brace

## GeckoRemoteMMFObject.java
- Add Javadoc to the ~60+ delegate methods that are completely undocumented
- Add Javadoc to `checkRemote()` explaining the connection validation logic
- **Bug found**: line 1814 the constructor uses `"getSignalData"` instead of `"simulateToSteadyState"` -- copy-paste error
- Add Javadoc to `forceDisconnectFromGecko()`
- Add field-level Javadoc for `NO_SESSION_ID`, `sessionID`, `_mmf`, `_pathToJava`
- Consider extracting the repeated try/catch/checkRemote pattern into a helper method

## GeckoRemoteIntWithoutExc.java
- Add `@param` and `@return` Javadoc tags to all interface methods
- Add `@deprecated` Javadoc tags to deprecated methods with replacement guidance
- Fix typo `supressMessages` -> `suppressMessages`
- Add class-level Javadoc explaining the relationship with `GeckoRemoteInterface`

## GeckoRemoteInterface.java
- Add Javadoc to session management methods explaining the multi-client connection model
- Add `@deprecated` tags with replacement guidance to all `@Deprecated` methods
- Add `@Documentation` annotation to `getSignalFourier` (missing, all other signal methods have it)
- Add `@param`/`@return` tags to `simulateToSteadyState` and `initSteadyStateDetection`

## GeckoRemoteException.java
- Fix typo `preecedingException` -> `precedingException`
- Fix constructor Javadoc referencing wrong class name `GeckoRemoteObjectException`

## GeckoRemote.java
- Add Javadoc to delegate methods (getControlElements, getCircuitElements, etc.)
- Complete incomplete Javadoc on `portFree(int)` -- `@param port` and `@return` are empty
- Complete incomplete Javadoc on `startGui(int)` -- `@param port` has no description
- Add `@Deprecated` annotation to methods delegating to deprecated proxies
- Add Javadoc to `RemoteInvocationHandler` inner class and its `invoke` method

## GeckoMemoryMappedFile.java
- Rename `_defaultBufferSize` to `DEFAULT_BUFFER_SIZE` (it is `public static final`)
- Document the hardcoded buffer position constants with a comment explaining the memory layout
- Document the magic number `1000` (wait time in `rejectConnection()`)
- Add Javadoc to `checkConnectionID(long)`

## GeckoExternal.java
- `getThyristors()` is instance while all others are `static` -- inconsistent, document or fix
- `createComponent()` and `createConnector()` are instance while all others are `static` -- document or fix
- Remove or document empty `runGeckoSCRIPT()` method (dead code)
- Add Javadoc to most delegate methods
- Consolidate duplicate class-level Javadoc blocks into one

## GeckoCustomRemote.java
- Add Javadoc to `connect()`, `disconnect()`, `acceptExtraConnections()`, `registerForCallback()` etc.
- Add field-level Javadoc for `_free`, `_lastSessionIDActive`, `clients`, `_acceptsExtraConnections`
- Document thread-safety implications of public static `clients` map

## GeckoCustomMMF.java
- Complete the `@param methodObject` tag in `callMethod()` Javadoc
- Fix `@param` name mismatch in `checkForPrimitiveType()` -- says `type` but parameter is `argType`
- Add Javadoc to `monitorMMF()` and `startMonitoring()`
- Add field-level Javadoc for `_mmf`, `_accessEnabled`, `_connectionID`

## GeckoRemoteTestingDummy.java
- Add note that `SESSION_ID = 12345` is a fixed test value
- Remove repetitive IDE-generated comments from all ~60 method stubs
- Add Javadoc comments to key methods explaining test behavior

## GeckoRemoteRegistry.java
- Add class-level Javadoc explaining RMI registry management
- Add Javadoc to all public methods
- Document the difference between `_remote` and `remoteAccess` fields
- Add field-level Javadoc for `DEFAULT_ACCESSPORT`, `PROPERTIES_KEY`, `_ipQuerySite`, `_ipAddress`
- Rename `_ipQuerySite` to follow constant naming conventions

## GeckoRemotePipeObject.java
- Add Javadoc to the `GeckoRemotePipeObjectType` enum constants
- Add note explaining why `_methodArguments` and `_methodReturnValue` are `transient`
- Document the potential constructor ambiguity

## GeckoRemoteObjectTest.java
- Add Javadoc to `main` method explaining what this manual test demonstrates
- Replace magic number `43035` with named constant
- Replace magic numbers `100` and `2000` with named constants
- Add class-level note clarifying this is a manual integration test

## GeckoRemoteObject.java
- Add Javadoc to the ~40+ delegate methods
- Add Javadoc to `connectToExistingInstance()`, `connectToGecko()`, `startNewRemoteInstance()` overloads
- Add Javadoc to `RemoteInvocationHandler` inner class
- Complete incomplete Javadoc on `portFree(int)`
- Add Javadoc to `checkRemoteWithException()`, `checkRemote()`, `disconnectFromGecko()`
- Add field-level Javadoc for `portNumber`, `_wrapped`, `_proxy`, `sessionID`, `doProxyCheck`

## GeckoRuntimeException.java
- Add meaningful class-level Javadoc describing when this exception is thrown
- Add Javadoc to the constructor

## GeckoSimulink.java
- Add class-level Javadoc explaining the MATLAB Simulink co-simulation interface
- Add Javadoc to all `external_*` methods
- Translate German comments to English
- Remove dead field `tmpRemove` (never referenced)
- Document the return value of `external_openFile()` (always returns hardcoded `"returnValue"`)
- Document `tStartSimulink` and `tEndSimulink` fields

## GeckoSim.java
- Add field-level Javadoc for `public static double xx = 4.67` (appears to be debug/test value)
- Add Javadoc to `stopTime()`, `testIfBrandedVersion()`, `initialisiere()`, `checkJavaVersion()`, etc.
- Fix duplicate text in `checkJavaVersion()` error message
- Document or remove empty catch block in `loadPropertyFile()`
- Add Javadoc to `findOrCreateAppDataDirectory()` explaining platform-specific directory resolution
- Extract magic numbers in `performScreenSettings()` (640, 480, 1000, 0.90, 0.80) into named constants
- Add field-level Javadoc for public static fields
