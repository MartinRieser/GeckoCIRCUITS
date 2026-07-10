# Improvement Tasks: ch/technokrat/gecko/geckocircuits/circuit/ (92 files, part 1: A-M)

## AbstractBlockInterface.java
- Add class-level Javadoc explaining the base abstract class for all circuit block components
- Add Javadoc to `setAccessibleParameter(String, double)` explaining the forward-compatibility "enabled" hack and magic numbers 0/1/2
- Document the `parameter` array (size 40) and `nameOpt` array (size 40) magic numbers
- Add Javadoc to `getShortConnectors()`, `clickedTerminal(Point)`, `paintShortCircuitConnections()`
- Add Javadoc to `copyLKBlockPars(AbstractBlockInterface)` explaining what "LK Block Pars" means
- Remove dead commented-out code in `paintComponentForeGround`
- Fix typo "mehtod" -> "method" in `importIndividual` comment

## AbstractCachedMatrix.java
- Add class-level Javadoc explaining the base class for cached LU-decomposed matrices
- Add Javadoc to `secondHashCode()` and `hashCode()` explaining the hash algorithms and magic numbers
- Add Javadoc to `equals()`, `setAccess()`, `getAccessCounter()`, `getLatestAccessTime()`
- Add Javadoc to abstract methods `initLUDecomp()`, `deleteCache()`, `solve()`, `calculateMemoryRequirement()`
- Document the lazy-init sentinel value of -1 for `_hashCode` and `_secondHashCode`

## AbstractCircuitSheetComponent.java
- Add class-level Javadoc explaining the base class for circuit sheet components
- Document the `dpix` static field (comment is only in German)
- `findAndSetReferenceToParentSheet` and `findAndSetReferenceToParentSheet2` are **identical duplicates** -- remove one
- Add Javadoc to `deleteComponent()`, `importASCII()`, `exportASCII()`, `getParentCircuitSheet()`, `setParentCircuitSheet()`
- Add Javadoc to `shiftAllIdentifiers(long)` and `allParentSubcircuitsEnabled()`

## AbstractSpecialBlock.java
- No improvements needed.

## AbstractTerminal.java
- Add class-level Javadoc explaining the base abstract terminal
- Document magic numbers `DX_IN=3`, `DX_OUT=3`, `DY_TEXT=-3`, `POINT_DIAMETER=5`, `_pFa=11`, `_pFb=3`
- Add Javadoc to `paintComponent()`, `paintLabelString()`, `createCopy()`, `getCategory()`
- Remove unused `orientierung` parameter in `paintFlowSymbol` (dead parameter)

## AbstractTypeInfo.java
- Add class-level Javadoc explaining the abstract type info/metadata registry
- Document all static maps (`_classEnumMap`, `_classTypeMap`, `_stringTypeMap`, `_enumTypeMap`)
- Add Javadoc to `getTypeFromEnum()`, `getFromComponentName()`, `getTypeFromString()` (document difference: RuntimeException vs IllegalArgumentException)
- Add Javadoc to constructor explaining side effects (registers into static maps)
- Add Javadoc to `doConsistencyCheck()`, `addParentEnum()`, `fabric()`, factory methods

## CachedMatrix.java
- Add class-level Javadoc explaining LU decomposition with sparse optimization
- Document threshold magic number `50` in `initLUDecomp()` (when to use sparse vs dense)
- Add Javadoc to `doLUDecomposition()`, `doLUDecompositionSparse()`, `solve()` explaining the algorithms
- Add Javadoc to `calculateLowerSparseLUDecompositionIndices()`, `calculateUpperSparseLUDecompositionIndices()`
- Add Javadoc to `calculateMemoryRequirement()` explaining formula `2 * n * n * Double.SIZE / 8`
- Remove dead/commented-out debug code (lines 75-76, 198-208, 254)

## CircuitLabel.java
- Add class-level Javadoc explaining node/terminal label with undo support
- Add Javadoc to `setLabel()` and `setLabelWithoutUndo()` documenting the undo behavior difference
- Add Javadoc to `getLabelPriority()`, `setLabelFromUserDialog()`, `clearPriority()`
- Add Javadoc to `RenameLabelUndoableEdit` inner class
- Remove redundant null check in `setLabel`

## CircuitSheet.java
- Add class-level Javadoc explaining the JPanel representing a circuit schematic sheet
- Add Javadoc to complex methods: `paintComponent()`, `findString()`, `mouseConnectorTest()`, `getConnection()`
- Add Javadoc to `findSubCircuit()` explaining `#` path syntax
- Add Javadoc to `getLocalComponents()` explaining filtering logic
- Remove redundant `if (comp instanceof SubcircuitBlock)` checks (three identical checks in a row)
- Document the static `_findNodes` and `_showNodes` Sets

## CircuitSourceType.java
- Add class-level Javadoc explaining old/new source type ID mapping
- Add Javadoc to `getFromID()` explaining the two-pass old/new ID search
- Note: `getNewID()` returns `double` not `int` (possible bug)
- Add Javadoc to constructor and enum constants

## CircuitTypeInfo.java
- No improvements needed.

## ComponentCoupable.java
- Add Javadoc to interface methods with `@param`/`@return` tags

## ComponentCoupling.java
- Add class-level Javadoc explaining coupling references between components
- Document all fields: `_coupledElements`, `_coupledIdentifiers`, `_coupledIdentifiersBeforeCopy`, etc.
- Add Javadoc to constructor, `setNewCouplingElement()` variants, `refreshCoupledReferences()`, `trySetCopyReference()`
- Add Javadoc to `SetOperation` inner class

## ComponentDirection.java
- Add class-level Javadoc explaining the four cardinal orientations
- Add Javadoc to constructor explaining legacy `oldOrdinal` values (501-504)
- Add Javadoc to `code()`, `getFromCode()`, `nextOrientation()`, `getDirection()`, `isHorizontal()`

## ComponentState.java
- Add Javadoc to each enum constant (SELECTED, FINISHED)
- Add class-level Javadoc

## ComponentTerminable.java
- Add Javadoc to `getAllNodeLabels()` and `getAllTerminals()`
- Add class-level Javadoc

## Connection.java
- Add class-level Javadoc explaining wire/connection between nodes
- Document all fields (`_movementWestEast`, `_isInitialized`, `_subPaths`, `_trimmedCoords`)
- Add Javadoc to German-named methods (`setzeStartKnoten`, `setzeEndKnoten`, `setzeAktuellenPunktAufConnection`)
- Add Javadoc to `moveHorizontal`, `moveVertical`, `trimCoordinates()`, `paintGeckoComponent()`
- Add Javadoc to `MoveConnectionUndoAction` inner class
- Document whether `initAnimationParts()` is dead code (throws UnsupportedOperationException)

## ConnectionShortConnector.java
- Add class-level Javadoc explaining what a "short connector" is
- Add Javadoc to `getParentCircuitSheet()` override explaining why it bypasses normal parent logic
- Document the `_parentSheet` field

## ConnectorType.java
- Add class-level Javadoc explaining the simulation domain categories (LK, CONTROL, RELUCTANCE, THERMAL)
- Add Javadoc to `fromOrdinal()`, `getDisplayMode()`, `getBackgroundColor()`, `getForeGroundColor()`
- Document each enum constant with domain description

## ControlSourceType.java
- Add class-level Javadoc explaining control signal source type mapping
- Add Javadoc to `getFromID()`, `getNewID()` (note: returns `double` not `int`)
- Add Javadoc to constructor

## ControlTerminable.java
- Add Javadoc to `getNodeNumber()`, `setNodeNumber()`, `clearNodeNumber()` explaining node numbering
- Add class-level Javadoc

## CurrentMeasurable.java
- Add Javadoc to `getCurrentMeasurementComponents(ConnectorType)` explaining return value

## DataTablePanel.java
- Add class-level Javadoc explaining the sortable JTable for double-value data entry
- Add Javadoc to `setValues()`, `getCheckedData()`, `clear()`, `clearWithoutEvent()`
- Add Javadoc to `MyTableModel` inner class
- Remove or document `counter` static field (debug/test code)
- Document `calculateTableHash()` magic numbers (7, 13, 9)
- Add Javadoc to `sortWithFirstRow()`, `createNullRow()`

## DataTablePanelParameters.java
- Add class-level Javadoc explaining parameter name/value entry panel
- Add Javadoc to constructor explaining `usedParameterNames` map
- **`getCheckedData()` always returns null** with commented-out body -- implement or document why
- Add Javadoc to `getVariableNames()`, `getVariableValues()` explaining `$`-prefix convention
- Document magic column index constants (0=name, 1=value, 2=usage count)
- Document color coding: red = invalid, blue = valid but unused

## DialogCircuitComponent.java
- Add class-level Javadoc explaining the abstract base dialog for circuit component parameters
- Document all UI component fields
- Add Javadoc to `okActionListener`, `processInputIndividual()`, `baueGUI()`, `setNewElementName()`
- Add Javadoc to `getRegisteredTextField`, `createParameterPanel()`, `processRegisteredParameters()`
- Document magic constants `TEXT_FIELD_LENGTH=10`, `NO_TF_COLS=6`, `BUTTON_WIDTH=90`, `BUTTON_HEIGHT=25`

## DialogGlobalTerminal.java
- Add class-level Javadoc explaining global terminal name configuration dialog
- Add Javadoc to constructor, `readAllGlobalsIntoComboBoxes()`, `setComponentName()`
- Document the `_initDone` flag
- Remove empty `jTextFieldNameKeyTyped` and `jTextFieldNameFocusLost` if unused

## DialogModule.java
- Add class-level Javadoc
- Remove `main(String[])` test/demo method if dead code
- Document `jTextFieldFileName` initialized with placeholder "jTextField1" (leftover IDE code)

## DialogNonLinearity.java
- Add class-level Javadoc explaining non-linear characteristic editing dialog
- Add Javadoc to constructor explaining `yAxisLog` parameter
- Document the empty catch block (should at least log the exception)

## DirectVoltageMeasurable.java
- Add Javadoc to `getDirectVoltageMeasurementComponents(ConnectorType)`
- Add class-level Javadoc

## ElementDisplayProperties.java
- Add class-level Javadoc explaining display/visibility flags
- Add Javadoc to each field explaining what UI element it controls

## Enabled.java
- Add class-level Javadoc explaining three component enable states
- Add Javadoc to `getFromOrdinal()`, `toString()`, enum constants

## EnumTerminalLocation.java
- Add class-level Javadoc explaining four terminal positions
- Add Javadoc to `getFromOrdinal()`, enum constants

## GeckoFileable.java
- Add Javadoc to each method: `initExtraFiles()`, `addFiles()`, `getFiles()`, `removeLocalComponentFiles()`

## GeckoMatrix.java
- **Entire file is dead/commented-out code** -- recommend removing entirely

## GlobalTerminable.java
- Add Javadoc to each method signature
- Add class-level Javadoc explaining cross-subcircuit connections

## HiddenSubCircuitable.java
- Add Javadoc to `getHiddenSubCircuitElements()` and `includeParentInSimulation()`

## IDStringDialog.java
- Add class-level Javadoc explaining unique component name/ID management
- Document the static `_allIDStrings` map
- Add Javadoc to `fabricVariableName()`, `setNameUnChecked()`, `setNewNameChecked()`, `deleteIDString()`
- Add Javadoc to `findUnusedName()` explaining auto-increment name logic
- Add Javadoc to `getComponentByName()` explaining `#` subcircuit path resolution

## InvisibleEdit.java
- Add class-level Javadoc explaining undoable edits that don't appear in the undo menu
- Document the "invisible" concept

## IpesFileable.java
- Add class-level Javadoc explaining what "Ipes" is/was
- Add Javadoc to `exportAscii()`
- Document why this is package-private

## Labable.java
- Add class-level Javadoc explaining this marker interface
- Add Javadoc to `getLabelObject()`

## Label.java
- Add class-level Javadoc explaining the immutable value class
- Add Javadoc to `hashCode()` and `equals()`
- Consider making the class `final`

## LabelPriority.java
- Add class-level Javadoc explaining label display priority
- Document numeric values (0, 1, 2, 4) and the gap (no 3)
- Fix typo in method name `getHighesPriority` -> `getHighestPriority`
- Add Javadoc to `isBiggerThan()`

## LKMatrices.java
- **Largest/most complex file (1523 lines) with almost no Javadoc** -- critical documentation gap
- Add Javadoc to `initMatrizen` (all 3 overloads), `schreibeMatrix_A`, `schreibeMatrix_B`
- Add Javadoc to `calculateComponentCurrents` explaining non-linear convergence loop
- Add Javadoc to `aktualisiereKnotenpotentiale`, `setzeAnfangsbedingungen`, `getAWForInductance`
- Document solver-type-dependent coefficients (SOLVER_BE, SOLVER_TRZ, SOLVER_GS)
- Document all `parameter[i1][N]` magic column indices
- Document `FAST_NULL_R` and `FAST_NULL_L` sentinel constants
- Remove massive blocks of dead commented-out debug code
- Replace German variable names in debug prints with English

## LUDecompositionCache.java
- Add Javadoc to `getCachedLUDecomposition()` explaining hash-collision double-check logic
- Add Javadoc to `testForCacheShrink()`, `removeLeastAccessedMatrices()`, `calculateNewVarMaxCacheSize()`
- Document magic numbers: `MAX_CACHE_SIZE=1000`, `maxJVMMemory / 3`, `maxJVMMemory / 10`
- Document the `1e99` sentinel for oldestTime
- Remove dead commented-out code

## MapList.java
- Add class-level Javadoc explaining ArrayList with secondary class-type index map
- Add Javadoc to `getClassFromContainer()`, `add()`, `remove()`, `addAll()`
- Document `registeredTypes` array
- Add Javadoc to `removeAll()` explaining `assert false` (intentionally unsupported)

## MyTableCellEditor.java
- Add class-level Javadoc explaining table cell editor using FormatJTextField
- Add Javadoc to `getTableCellEditorComponent`, `getCellEditorValue()`

## MyTableCellRenderer.java
- Add class-level Javadoc explaining numeric value rendering with engineering notation
- Add Javadoc to `setValue(Object)`

## MyTableComparator.java
- Add class-level Javadoc explaining first-column ascending sort comparator
- Add Javadoc to `compare()` explaining null-handling
- Fix logic bug: line 37 `o2.get(0) == 0` uses autoboxing identity comparison instead of `.equals(0.0)`
