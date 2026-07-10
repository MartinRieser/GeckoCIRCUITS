# Improvement Tasks: control/javablock/ + datacontainer/ + scope/ + math/ + nativec/ + geckoscript/ + general/ + i18n/

---

# javablock/ (19 files)

## AbstractCompileObject.java
- Add class-level Javadoc explaining compile object base class
- Add Javadoc to all methods

## AbstractJavaBlock.java
- Add class-level Javadoc explaining base class for Java-based control blocks
- Add Javadoc to all public/protected methods

## CodeWindowModern.java
- Add class-level Javadoc explaining the Java code editor window
- Add Javadoc to event handler methods
- Document the lifecycle (create, compile, execute)

## CompiledClassContainer.java
- Add class-level Javadoc explaining compiled class storage
- Add Javadoc to all methods

## CompileObject.java
- Add class-level Javadoc explaining the compile object interface
- Add Javadoc to all interface methods

## CompileObjectNull.java
- Add class-level Javadoc explaining the null object pattern for compile objects

## CompileObjectSavedFile.java
- Add class-level Javadoc explaining saved-file compile object

## CompileStatus.java
- Add Javadoc to enum explaining compile status values

## ControlJavaFunction.java
- Add class-level Javadoc explaining the Java function control block

## ControlJavaTriangles.java
- Add class-level Javadoc explaining Java block terminal triangles

## ExtraFilesWindow.java
- Add class-level Javadoc explaining extra files management window

## GeckoForwardingFileManager.java
- Add class-level Javadoc explaining the forwarding file manager for compilation

## JavaBlockClassLoader.java
- Add class-level Javadoc explaining custom class loader for compiled Java blocks

## JavaBlockMatrix.java
- Add class-level Javadoc explaining matrix support for Java blocks

## JavaBlockSource.java
- Add class-level Javadoc explaining source code container for Java blocks

## JavaBlockVector.java
- Add class-level Javadoc explaining vector support for Java blocks

## RamJavaFileObject.java
- Add class-level Javadoc explaining in-memory Java file object

## SourceFileGenerator.java
- Add class-level Javadoc explaining Java source code generation from block definitions

## VariableBusWidth.java
- Add class-level Javadoc explaining variable bus width detection

---

# datacontainer/ (30 files)

## AbstractDataContainer.java
- Add class-level Javadoc explaining base data container
- Add Javadoc to all abstract methods

## AbstractDataContainerSignal.java
- Add class-level Javadoc explaining signal data container base

## ArrowIcon.java
- Add class-level Javadoc explaining the arrow icon UI component

## AverageValue.java
- Add class-level Javadoc explaining running average calculation

## CompressorIntMatrix.java
- Add class-level Javadoc explaining integer matrix compression

## ContainerStatus.java
- Add Javadoc to enum explaining container status values

## DataContainerCompressable.java
- Add class-level Javadoc explaining lossy compression for data containers

## DataContainerFourier.java
- Add class-level Javadoc explaining Fourier-transformed data container

## DataContainerGlobal.java
- Add class-level Javadoc explaining global simulation data container

## DataContainerIntegralCalculatable.java
- Add class-level Javadoc explaining integral calculation interface

## DataContainerMeanWrapper.java
- Add class-level Javadoc explaining mean/average data wrapper

## DataContainerNullData.java
- Add class-level Javadoc explaining null data container pattern

## DataContainerScopeWrapper.java
- Add class-level Javadoc explaining scope data wrapper

## DataContainerSimple.java
- Add class-level Javadoc explaining simple data container

## DataContainerTable.java
- Add class-level Javadoc explaining tabular data container

## DataContainerTableModel.java
- Add class-level Javadoc explaining table model for data container

## DataContainerValuesSettable.java
- Add class-level Javadoc explaining settable values interface

## DataIndexItem.java
- Add class-level Javadoc explaining data index entry

## DataJunk.java
- Add class-level Javadoc explaining data junk/chunk abstraction

## DataJunkCompressable.java
- Add class-level Javadoc explaining compressable data junk
- Add Javadoc to static methods including `setMemoryPrecision()`
- Document relationship between junk size and compression ratio

## DataJunkSimple.java
- Add class-level Javadoc explaining simple data junk

## DataTableFrame.java
- Add class-level Javadoc explaining data table frame/window

## HeaderSymbol.java
- Add class-level Javadoc explaining header symbol for data columns

## IntegerMatrixCache.java
- Add class-level Javadoc explaining integer matrix cache

## ScopeWrapperIndices.java
- Add class-level Javadoc explaining scope wrapper index mapping

## ShortArrayCache.java
- Add class-level Javadoc explaining short array cache

## ShortMatrixCache.java
- Add class-level Javadoc explaining short matrix cache

## SignalDataContainerMean.java
- Add class-level Javadoc explaining signal mean data container

## SignalDataContainerRegular.java
- Add class-level Javadoc explaining regular signal data container

## TextSeparator.java
- Add class-level Javadoc explaining text separator utility

---

# scope/ (11 files) -- OLD SCOPE PACKAGE (not newscope/)

## DataContainer.java
- **Add Javadoc to all interface methods.** Every method (`getValue`, `getHiLoValue`, `setValue`, `getRowLength`, `getColumnLength`, `setColumn`, `getColumn`, `getTimeIntervalResolution`, `getEstimatedTimeValue`, `getMaximumTimeIndex`) lacks any Javadoc
- **Add `@param` documentation to `insertValuesAtEnd`.** The existing block comment is not a proper Javadoc block (missing `/**`). Convert to standard Javadoc with `@param timeValue` and `@param values` tags
- **Clarify the `getHiLoValue` parameter naming.** The interface declares `getHiLoValue(int row, int column, int columnOld)` but the implementation uses `columnStart`/`columnStop`. Rename interface parameter `columnOld` to `columnStop` for consistency

## DataContainerSimple.java
- **Add Javadoc to the class and all public/overridden methods.** Constructor `DataContainerSimple(int rows, int columns)` and methods like `getValue`, `setValue`, `getTimeIntervalResolution`, `getHiLoValue`, `getEstimatedTimeValue`, `insertValuesAtEnd` all lack Javadoc
- **Fix magic number in `getTimeIntervalResolution()`** (line 71): `_data[0][2] - _data[0][1]` uses hardcoded indices. Add clarifying comment or named constant explaining this assumes the time row has at least 3 data points
- **Bug: redundant/ineffective bounds check in `setValue`** (lines 42-44 and 49-51). The subsequent `if (column < _data[row].length)` is always true (dead redundant check) and can be removed

## DialogFourierDiagramm.java
- **Add Javadoc to the class and constructor.** The constructor (lines 72-98) has no `@param` documentation for its 7 parameters
- **Translate German method and variable names to English.** `baueGUI()` -> `buildGUI()`, `_signalFourierAnalysiert` -> `_signalFourierAnalyzed`
- **Replace `StringBuffer` with `StringBuilder`** (line 142) -- `StringBuffer` is synchronized and unnecessarily slow in single-threaded context
- **Remove debug/error code string `qe90r8gn03g8q`** (line 167) and similar obfuscated error identifiers (lines 240, 306). Replace `System.out.println` with proper `Logger` calls

## DisplayFourierWorksheet.java
- **Add Javadoc to the class and constructor.** Document `@param cnSG`, `@param jnSG`, `@param nMin`
- **Translate German method names to English.** `baueGUI()` -> `buildGUI()`, `schreibeData()` -> `writeData()`
- **Remove unnecessary `new String(...)` wrapper calls** (lines 57, 59, 61) -- `formatT` already returns a `String`
- **Add Javadoc to the anonymous `JTable` override** (lines 65-71) explaining why `isCellEditable` returns `false`

## FourierDiagramm.java
- **Bug: duplicate condition in zoom rectangle drawing** (lines 250-258). First two `else if` conditions are identical; also line 256 has `(y1Zoom > y1Zoom)` which is always false. Fix all four rectangle-positioning branches
- **Bug: `getPixelFromValue` never computes `xPix`/`yPix`** (lines 563-587). The method always returns `{-1, -1}`. The logic needs to be inverted to solve for `xPix`/`yPix`
- **Extract magic numbers from constructor.** Values like `350`, `300`, `60`, `0.1` (bar width), `1e-6` should be named constants
- **Fix typo in field name `xNeuWert`** (line 102) -- consider renaming for readability

## FourierKurvenRekonstruktion.java
- **Bug: `getPixelFromValue` never computes pixel values** (lines 444-468). Identical bug to `FourierDiagramm`: always returns `{-1, -1}`
- **Add Javadoc to the class and all public/protected methods.** No Javadoc exists on the class, constructor, `resize()`, `setMouseMode()`, `zeichne()`, `setzeAchsen()`, or `setzeKurven()`
- **Translate German comments and variable names to English.** Comments like `"braucht es, damit kein NullPointer-Error"`, field names `mausModus`, `xSchieberAktiv`, `xSchieberPix`, `imDragModus`
- **Remove commented-out debug code** (lines 302-303)
- **Extract magic numbers.** Values `350`, `300`, `75`, `30`, `1e99`/`-1e99`, `0.5` should use named constants

## FourierPlotFrame.java
- **Complete the class-level and constructor Javadoc.** The class Javadoc (lines 24-27) is essentially empty. Add `@param` for `newScope`, `baseFrequency`, and `erg`
- **Remove unused imports.** `SaveViewFrame`, `ScopeSignalSimpleName`, `AbstractScopeSignal`, `DialogConnectSignalsGraphs`, and `BufferedWriter` (lines 17-22)
- **Add Javadoc to `initComponents()`** explaining it is Netbeans Form Editor-generated code
- **Document the generated fields.** `jPanelFourier1` and `jToolBar1` lack field-level Javadoc

## GraferImplementation.java
- **Add Javadoc to the class and most public methods.** This 2476-line class has almost no Javadoc. Key methods needing documentation: constructor, `setzeKurvenUndWorksheetDaten`, `akualisiereKurvenUndWorksheetDaten` (note typo "akualisiere" missing 'k'), `setzeAchsen`, `setzeKurven`, `setMouseMode`, `mouseMode_ZOOM_WINDOW`, `zoomRectangle`
- **Bug: self-assignment in `setzeAchsen()`** (lines 1326-1329). `xTickAutoSpacing[i1] = xTickAutoSpacing[i1];` and `yTickAutoSpacing[i1] = yTickAutoSpacing[i1];` are no-ops
- **Remove dead/empty stub methods.** `mouseMode_DRAW_LINE`, `mouseMode_DRAW_TEXT`, `mouseMode_FIBONACCI_LIN`, `mouseMode_FIBONACCI_LOG` are empty stubs
- **Replace `System.out.println("Fehler: ...")` debug calls with proper Logger calls.** Lines 549, 753, 1746, 1765 use obfuscated error codes
- **Extract magic numbers.** Constants `1000` (used in `/ 1000` and `% 1000` throughout), `10000`, `0.6f`, `230`/`100`/`2.5` should be named constants

## GraferV3.java
- **Add Javadoc to the class and public methods.** Public methods like `setzeAchsenAnzahl`, `setAxisWidthHeightX0Y0`, `setzeAchsenBegrenzungen`, `setzeAchsenTyp`, `selectColor`, `getSelectedColor` all need Javadoc
- **Fix typo: `LIGTHGRAY`** (line 76, and used at line 1411). Should be `LIGHTGRAY`. Also the `FARBEN` array entry `"ligthgray"` (line 69)
- **Remove large block of commented-out dead code** (lines 1027-1035)
- **Replace `System.out.println("Fehler: ...")` debug calls with Logger.** Lines 217, 225, 585, 639, 1068, 1100, 1149, 1181, 1426 should all use a proper logger
- **Document the meaning of negative sentinel constant values.** Constants like `AUTO = -111111111`, `ACHSE_LIN = -111111114` use arbitrary large negative numbers with no Javadoc

## HiLoData.java
- **Add Javadoc to the class and methods.** The class has only a brief `@author` tag. Document what "Hi-Lo" means (stores min/max values across a range for data compression)
- **Extract magic numbers into named constants.** `1E30f`, `-1E30f`, `1E30` are sentinel values representing "uninitialized"
- **Add visibility modifiers to methods.** `insertCompare(float y)` and `insertCompare(HiLoData data)` are package-private
- **Bug: mixed float/double comparison** in `insertCompare(HiLoData)` (line 35). Uses `double` literal compared against `float` field

## Scopable.java
- **Add meaningful Javadoc to the interface and methods.** The class Javadoc (lines 16-19) is empty
- **Translate German parameter and method names to English.** `mausmodus` -> `mouseMode`, `getZVDatenImRAM()` -> `getZVDataInRAM()`, `ladeWorkSheet()` -> `loadWorkSheet()`
- **Remove commented-out method declaration** (line 25): `//public ScopeSettings getScopeSettings();`
- **Add `@return` documentation** to `getZVDatenImRAM()` explaining it returns high-resolution simulation data stored in RAM

---

# math/ (7 files)

## BigLUDecomposition.java
- Fix bug: bitwise `&` on line 132 (`if (j < m & LU[j][j].abs().doubleValue() > 1e-30)`) should be logical `&&` -- risks `ArrayIndexOutOfBoundsException`
- Remove debug artifacts: `System.err.println(" j: " + j)` in `isNonsingular()` (line 210) and `//test` comment (line 37)
- Extract magic number `1e-30` into a named constant such as `PIVOT_THRESHOLD`
- Remove the dead commented-out "temporary, experimental code" block (lines 141-198) using invalid `\* ... *\` comment syntax
- Fix constructor Javadoc (lines 59-62): the `@return` tag is invalid for constructors

## BigMatrix.java
- Remove the massive block of commented-out dead code (lines 278-491) containing disabled `plus`, `minus`, `times`, `arrayTimes`, `chol`, `qr`, `svd`, `eig`, and related methods
- Fix bug in `read()` method (line 639): it returns `Matrix` instead of `BigMatrix` -- also uses deprecated raw `java.util.Vector` types
- Fix typo "colums" -> "columns" in the constructor Javadoc (line 96 and other occurrences)
- Add Javadoc to `ResetLUDecomp()` (line 555) and rename it to `resetLUDecomp` to follow Java naming conventions
- Remove dead `//package Jama;` and `//import Jama.util.*;` comments (lines 17, 27)

## CholeskyDecomposition.java
- Remove the commented-out "temporary, experimental code" block (lines 90-150) containing the dead right-triangular constructor and `getR()` method
- Fix constructor Javadoc (lines 56-59): the `@return` tag is invalid for constructors
- Replace bitwise `&` with logical `&&` on lines 79 and 82 (`isspd = isspd & ...`) to use proper short-circuit evaluation
- Rename constructor parameter `Arg` (line 61) to follow Java naming conventions (e.g., `matrix` or `source`)
- Remove dead `//package Jama;` comment (line 17)

## LUDecomposition.java
- Fix bug: bitwise `&` on line 129 (`if (j < m & LU[j][j] != 0.0)`) should be logical `&&` -- risks `ArrayIndexOutOfBoundsException`
- Remove the commented-out "temporary, experimental code" block (lines 137-194)
- Fix constructor Javadoc (lines 61-64): the `@return` tag is invalid for constructors
- Remove `//test` debug comment (line 35) and dead `//package Jama;` comment (line 17)
- Remove unnecessary `(double)` casts on line 271 (`vals[i] = (double) piv[i]`) and line 285 -- int auto-widens to double

## Matrix.java
- Fix typo "colums" -> "columns" in Javadoc across constructors (lines 92, 105, 138, 783, 799)
- Fix `solve()` method (line 740): replace `assert false` with an explicit exception throw -- asserts can be disabled at runtime, causing silent `null` returns
- Add Javadoc to `times(Matrix B)` (line 689) and `ResetLUDecomp()` (line 745); rename `ResetLUDecomp` to `resetLUDecomp`
- Remove dead `//package Jama;` and `//import Jama.util.*;` comments (lines 16, 24)
- Use covariant return type for `clone()` (line 203): change return type from `Object` to `Matrix`

## NComplex.java
- Fix hashCode bug: line 184 uses `Double.hashCode()` on float fields `re` and `im` -- should use `Float.hashCode()`
- Add `@param` and `@return` Javadoc tags to all three constructors and all static utility methods (`add`, `sub`, `mul`, `div`, `conj`, `abs`, `sqrt`, `RCmul`)
- Add Javadoc to `nicePrint()` and rename `RCmul` to a descriptive name such as `scale` or `multiplyByScalar`
- Fix fragile float equality comparisons: `im == 1`, `im == -1`, `Math.abs(im) == 1` -- should use epsilon-based comparison
- Make `TechFormat tcf` field `static final` since `NComplex` is otherwise immutable

## Polynomials.java
- Add `@param` and `@return` Javadoc tags to `poldiv` -- currently only has a prose description, missing documentation for all six parameters
- Remove redundant `extends Object` (line 19)
- Fix Java array declaration style: change C-style `float u[]` to idiomatic `float[] u` for all parameters on line 28
- Add a class-level Javadoc with a brief usage example showing how to call `poldiv` for polynomial division

---

# nativec/ (10 files)

## CompiledClassContainer.java
- **Add Javadoc to the class and all methods.** The class-level Javadoc (lines 19-22) is an empty placeholder. All three constructors, `getClassBytes()`, and `getSourceString()` have no Javadoc
- **Bug: `getClassBytes()` will throw NullPointerException** when `_classBytes` is null. Add null guard or return empty array
- **Add `@param` Javadoc tags** to the two-argument constructor and the `TokenMap` constructor

## CompileStatus.java
- **Fix typo: `COMPILED_SUCCESSFULL`** should be `COMPILED_SUCCESSFUL` (line 18). Note: this is a public enum constant, so callers must be updated
- **Add Javadoc to the enum and `getFromOrdinal` method**
- **Bug: `assert false; return null;`** in `getFromOrdinal` (lines 26-27). If assertions are disabled, this silently returns `null`. Replace with `throw new IllegalArgumentException`
- **Remove unnecessary trailing semicolon** after the enum's closing brace on line 28

## ControlJavaTriangles.java
- **Fix typo in class Javadoc:** "whe should split this" (line 18) should be "**we** should split this"
- **Fix German-derived field name typo:** `_xKlickMinTerminal`, `_xKlickMaxTerminal` (line 22) use "Klick" (German spelling). Rename to `_xClickMinTerminal` etc.; update callers in `ControlNativeC` (lines 305-318)
- **Add Javadoc to `isIncreaseClicked` and `isDecreaseClicked` methods**, including `@param` and `@return` tags
- **Make parameter modifiers consistent:** Add `final` to `isDecreaseClicked` parameters for consistency with `isIncreaseClicked`

## ControlNativeC.java
- **Fix typo: `severeErrorOccured`** (lines 120, 146, 148, 161, 166, 180, 194, 203) should be `severeErrorOccurred` (double 'r' in "occurred")
- **Translate German method name: `istAngeklickt`** (line 226) means "is clicked". Rename to `isClicked` and update `@Override` in parent class
- **Translate German token keys:** `anzXIN` and `anzYOUT` (lines 342, 349) derive from "Anzahl" (German for "count"). Add English keys or document why German keys must be retained for backward compatibility
- **Remove dead/commented-out code blocks:** Lines 257-263 (commented `CompileStatus` color logic), lines 177-178 (commented `showMsg` call), lines 421-434 (commented dialog initialization logic)
- **Potential bug: `_libFile.getFileName()` NPE risk** in the `UnsatisfiedLinkError` catch block (line 199). Add null check
- **Add Javadoc to the `CCalculator` inner class** and to methods like `convertString2List`, `convertList2String`, `loadUserData`, `triggerUpdate`

## InterfaceNativeCWrapper.java
- **Add Javadoc to all three interface methods.** `loadLibrary`, `initParameters`, and `calcOutputs` have no method-level Javadoc
- **Fix terminology in class Javadoc:** "are intended to be overwritten by native functions" -- in Java, interface methods are *implemented*, not *overwritten*
- **Add `@param xOUTVector` documentation** to `calcOutputs` (line 31). Also document `numberOfOuts`, `time`, and `deltaT`

## NativeCBlock.java
- **Remove unused imports** (lines 17-19): `java.lang.reflect.Field`, `java.lang.reflect.Method`, and `java.util.Vector` are only referenced in commented-out code
- **Remove dead/commented-out code** in `unloadLibraries` (lines 86-94): The entire reflection-based native library finalization block is commented out
- **Add Javadoc to all methods:** `calculateYOUT`, `loadLibraries`, `unloadLibraries`, `checkOutputsForNANorINFValues`, and the constructor
- **Bug: `checkOutputsForNANorINFValues` only checks for NaN, not Infinity.** `signal[i] != signal[i]` only detects NaN. Add `Double.isInfinite()` check or rename method

## NativeCClassLoader.java
- **Add Javadoc to `findClass` method and its `name` parameter** (line 38)
- **Resource leak risk in `findClass`:** No `finally` block or try-with-resources. If `inBuff.read()` throws, streams are never closed
- **Add Javadoc to the no-arg constructor** and `toString` override
- **Performance: byte-by-byte reading** in the `while` loop (line 51) is inefficient. Consider using `inBuff.read(byte[])` or `readAllBytes()` (Java 9+)

## NativeCDialog.java
- **Fix typo in comment:** "Paramter" (line 134) should be "Parameter"
- **Bug: `System.err.println(exc.getStackTrace())`** (line 191) prints a `StackTraceElement[]` array object, producing unhelpful output. Replace with `exc.printStackTrace()` or proper logger
- **Add Javadoc to methods:** `initFileChooser` (line 224) and `isFileNameAlreadyInList` (line 238)
- **Extract magic number `0.3`** (line 204) into a named constant with comment explaining the 30% sizing rationale
- **Fix raw types:** `_fileList` (line 58) is declared as raw `DefaultListModel` without generics. Update to `DefaultListModel<String>` for type safety

## NativeCLibraryFile.java
- **Fix typo in method names:** `savegetFile` (line 79) and `savegetFileName` (line 87) -- "saveget" should be `safeGetFile` and `safeGetFileName`. Update callers (e.g., `ControlNativeC.java` line 159)
- **Complete the empty `@return` tag** in `getTimeStamp()` Javadoc (line 98)
- **Add Javadoc to all undocumented methods:** `getFileName`, `setFile(File)`, `setFile(String)`, `setFile()`, `getFile`, `updateTimeStamp`, and both parameterized constructors
- **Reduce code duplication in `setFile` overloads:** `setFile(File)` and `setFile(String)` repeat existence check, path assignment, and timestamp logic. Consolidate

## NativeCWrapper.java
- **Add missing `@param xOUTVector` to `calcOutputs` Javadoc** (lines 32-39). Also add `@param` tags for `xINVector` and `deltaT`
- **Remove invalid `@return` tag** (line 38) from `calcOutputs` -- the method returns `void`. Move description text to `@param xOUTVector`
- **Improve `initParameters` Javadoc** (lines 43-45): Note that this is a `native` method implemented in the external C/C++ library

---

# geckoscript/ (9 files)

## TextAreaOutputStream.java
- Add class-level Javadoc explaining output redirection to JTextArea
- Document thread-safety for writing to Swing from non-EDT threads

## SimulationAccess.java
- Add class-level Javadoc explaining scripting access to simulation engine
- Add `@param`/`@return` to all methods
- Document thread-safety: which methods must be called on EDT

## ScriptWindow.java
- Add class-level Javadoc explaining the GeckoSCRIPT IDE window
- Document the script compilation and execution pipeline

## ParameterSupport.java
- Add class-level Javadoc explaining parameter management for scriptable components
- Document parameter name resolution mechanism

## JTextAreaWriter.java
- Add class-level Javadoc explaining Writer implementation for JTextArea
- Document thread-safety

## GeckoInvalidArgumentException.java
- Add class-level Javadoc explaining invalid script argument exception

## FunctionDescription.java
- Add class-level Javadoc explaining script function description for autocomplete

## CompileScript.java
- Add class-level Javadoc explaining GeckoSCRIPT compilation
- Document the compilation pipeline and error handling

## AbstractGeckoCustom.java
- Add class-level Javadoc explaining abstract base for custom Gecko integrations
- Add `@param`/`@return` tags throughout
- Remove any dead/commented-out code blocks

---

# general/ (49 files)

## AbstractComponentType.java
- Add class-level Javadoc explaining the component type enumeration

## ContainsMatcher.java
- Add class-level Javadoc explaining substring matching

## DialogAbout.java
- Add class-level Javadoc explaining About dialog

## DialogControlCheck.java
- Add class-level Javadoc explaining control port connection check dialog

## DialogControlOrderN.java
- Add class-level Javadoc explaining control block reordering dialog

## DialogFeedback.java
- Add class-level Javadoc explaining feedback submission dialog

## DialogFindInModel.java
- Add class-level Javadoc explaining find-in-model search dialog
- Document the search algorithm and matcher types

## DialogJavaCompilerOptimizer.java
- Add class-level Javadoc

## DialogLicense.java
- Add class-level Javadoc explaining license validation

## DialogLicensing.java
- Add class-level Javadoc explaining licensing terms dialog

## DialogMakeExternal.java
- Add class-level Javadoc explaining file conversion process

## DialogMemory.java
- Add class-level Javadoc explaining JVM memory settings dialog
- Document default memory values

## DialogOptimizerParameterSettings.java
- Add class-level Javadoc

## DialogRemotePort.java
- Add class-level Javadoc explaining remote access port configuration

## DialogSimParameter.java
- Add class-level Javadoc explaining simulation parameter configuration
- Document solver type selection and time step validation

## DialogUpdate.java
- Add class-level Javadoc explaining software update dialog

## DialogUpdateSettings.java
- Add class-level Javadoc explaining update frequency settings

## DialogWarningNodeNumber.java
- Add class-level Javadoc explaining node number warning

## EndsWithMatcher.java
- Add class-level Javadoc explaining suffix matching

## FormatJTextField.java
- Add class-level Javadoc explaining formatted text field
- Document formatting rules

## GeckoFile.java
- Add class-level Javadoc explaining managed file reference
- Document internal vs. external file handling

## GeckoFileChooser.java
- Add class-level Javadoc explaining custom file chooser
- Add Javadoc to factory methods

## GeckoFileManager.java
- Add class-level Javadoc explaining model-attached file management
- Document relative path computation

## GeckoFileManagerWindow.java
- Add class-level Javadoc explaining file manager GUI

## GeckoJavaCompiler.java
- Add class-level Javadoc explaining in-memory Java compiler
- Document classpath and toolchain configuration

## GeckoRuntimeException.java
- Add class-level Javadoc explaining base runtime exception

## GeckoStatusBar.java
- Add class-level Javadoc explaining status bar widget
- Add Javadoc to `setzeStatusRechenzeit()` (German: compute time display)

## GetJarPath.java
- Add class-level Javadoc explaining JAR installation path resolution
- Document fallback behavior (IDE vs. JAR)

## GlobalColors.java
- Add class-level Javadoc explaining global color constants
- Consider grouping colors by domain

## GlobalFilePathes.java
- Add class-level Javadoc explaining global file path constants
- Document recent circuits list management

## GlobalFonts.java
- Add class-level Javadoc explaining global font settings

## LastComponentButton.java
- Add class-level Javadoc explaining last-used component button

## LaunchBrowser.java
- Add class-level Javadoc explaining cross-platform browser launching
- Document platform-specific behavior

## LoginDialog.java
- Add class-level Javadoc explaining login dialog

## MainWindow.java
- Add class-level Javadoc (currently only license header, no class doc)
- Add Javadoc to `saveFile()`, `openFile()`, `actionPerformed()`, `processKeyEvents()`
- Document magic numbers in `processKeyEvents()` (4=CONTROL+D, 18=CONTROL+R, 23)
- Add Javadoc to all menu builder methods
- Remove commented-out steady-state analysis block
- Add Javadoc to `BackupTask` inner class
- Translate German method names (`aktualisiereDividerSplitPane`, `setzeSTATUS`)

## MemoryWarning.java
- Add class-level Javadoc explaining memory warning dialog

## OperatingMode.java
- Add Javadoc to enum explaining operating modes
- Add Javadoc to each enum constant

## OptimizerParameterData.java
- Add class-level Javadoc explaining optimizer parameter storage

## ProjectData.java
- Add class-level Javadoc explaining project/model data holder
- Add Javadoc to `exportASCII()` and `shiftComponentReferences()`

## SaveViewFrame.java
- Add class-level Javadoc explaining image export frame
- Document supported export formats (PNG, PDF, SVG, etc.)

## SimulationRunner.java
- Add class-level Javadoc explaining simulation lifecycle management
- Add Javadoc to `startCalculation()`, `continueCalculation()`, `pauseSimulation()`

## SuggestionField.java
- Add class-level Javadoc explaining auto-suggest text field

## SuggestMatcher.java
- Add Javadoc to interface explaining matching contract

## SolverType.java
- Add Javadoc to enum explaining available solver types (TRAPEZOIDAL, BACKWARD_EULER, GEAR_SHICHMAN)

## StartupWindow.java
- Add class-level Javadoc explaining startup splash window
- Document blocking vs. non-blocking startup

## StartsWithMatcher.java
- Add class-level Javadoc explaining prefix matching

## TechFormat.java
- Add class-level Javadoc explaining technical number formatting utility
- Document format patterns (scientific, engineering notation)

## UndoRedoManager.java
- Add class-level Javadoc explaining undo/redo coordination

## UserParameter.java
- Add class-level Javadoc explaining user-editable parameter model
- Add Javadoc to `setValue()`, `getValue()`, `setValueWithoutUndo()`
- Document undo integration and validation

---

# i18n/ (15 files including subdirectories)

## DoubleMap.java
- Add class-level Javadoc explaining double-keyed map structure
- Add Javadoc to all methods

## GuiFabric.java
- Add class-level Javadoc explaining i18n-aware Swing component fabrication
- Document how i18n keys are resolved

## InitDialog.java
- Add class-level Javadoc explaining i18n initialization dialog

## InitParameters.java
- Add class-level Javadoc explaining initialization parameter constants

## LangInit.java
- Add class-level Javadoc explaining language initialization system
- Document the translation map initialization

## SelectableLanguages.java
- Add Javadoc to enum explaining available GUI languages

## bot/DLbot.java
- Add class-level Javadoc explaining translation download bot
- Document Wiki API interaction

## bot/UPbot.java
- Add class-level Javadoc explaining translation upload bot
- Add Javadoc to `addTranslationSuggestion_single()` and `_multiple()`
- Document thread-safety model

## translationtoolbox/TranslationTools.java
- Add class-level Javadoc explaining translation toolbox main window
- Add Javadoc to inner classes `Task` and `Progress`
- Extract duplicated progress monitor setup code (repeated 5x) into helper method
- Add inline documentation for generated component names

## translationtoolbox/TranslationPopupSingle.java
- Add class-level Javadoc explaining single-line translation popup
- Add Javadoc to inner classes
- Document Thread.sleep(500) magic number

## translationtoolbox/TranslationPopupMultiple.java
- Add class-level Javadoc explaining multi-line translation popup
- Note: nearly identical to `TranslationPopupSingle` -- consider extracting common base

## translationtoolbox/TranslationDialog.java
- Add class-level Javadoc explaining translation dialog
- Extract common initialization code from duplicate constructors

## translationtoolbox/PopupListener.java
- Add class-level Javadoc explaining Ctrl+Shift+click popup trigger mechanism
- Document why popup is suppressed when language is ENGLISH

## resources/I18nKeys.java
- Add class-level Javadoc explaining this enum holds all internationalizable strings
- Add Javadoc to `fabricFromKeyString()` explaining lazy initialization
- Remove commented-out two-argument constructor (dead code)
- Consider grouping enum constants by domain with section headers

## resources/EnglishMapper.java
- Add class-level Javadoc explaining English key-value pair map initialization
- Remove ~330 lines of commented-out dead code in `initEnglishMap_single()`
- Remove ~14 lines of commented-out code in `initEnglishMap_multiple()`
- Investigate: `initEnglishMap_multiple()` returns empty DoubleMap (all entries commented out)
