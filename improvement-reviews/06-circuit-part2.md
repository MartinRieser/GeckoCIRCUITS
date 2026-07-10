# Improvement Tasks: ch/technokrat/gecko/geckocircuits/circuit/ (92 files, part 2: N-Z)

## NameAlreadyExistsException.java
- Add class-level Javadoc explaining when this exception is thrown
- Document the constructor `@param message`

## NetListContainer.java
- Add class-level Javadoc explaining the container aggregating three netlists
- Document each static fabric method (`fabricStartSimulation`, `fabricContinueSimulation`, `fabricGuiUpdate`)
- Document public fields `_nlControl`, `_nlLK`, `_nlTH`
- Remove unused `simKern` parameter in fabric methods

## NetListLK.java
- Add class-level Javadoc explaining LK (power circuit) netlist
- Remove debug string `System.out.println("Fehler qer^08gj03qhg4")` in `getNetlistnNummer`
- Document public fields with German names (`knotenMAX`, `spgQuelleMAX`, `parameter`, etc.)
- Add Javadoc to all major methods
- Remove large commented-out debug block with hardcoded test values

## NetlistGeneral.java
- Add class-level Javadoc explaining the general netlist builder
- Remove or document unused `static int counter` field
- Add Javadoc to private methods (`createPotentialSheetConnectedGeometric`, `mergePotentialAreasViaLabels`, etc.)
- Add Javadoc to `GraphEdge` inner class and `traverse` method
- Translate German comment fragment "gleichlautenden Label" to English

## NodeLabel.java
- Add class-level Javadoc explaining schematic node label with anchor and click area
- Document all public methods (German names: `setKoordTxt`, `setKoordAnker`, `istAngeklickt`, `zeichne`)
- Remove commented-out dead code in `exportASCII`
- `importASCII` has an empty body -- implement or document why
- Remove commented-out `System.out.println` debug line

## NonLinearDialogPanel.java
- Add class-level Javadoc explaining non-linear characteristic editing dialog
- Document constructor parameters (`parentDialog`, `elementLK`, `isYAxisLog`)
- Document magic-number return codes (-1, 0, 1) with named constants
- Remove debug `System.out.println("iii " + ...)` loop

## Paradiso.java
- Add class-level Javadoc explaining Intel PARDISO sparse direct solver wrapper
- Document `factorize` and `solve` parameters and `mtype` values
- Convert magic-number `iparm[]` index assignments into named constants
- Remove commented-out `System.out.println` debug lines

## PardisoCachedMatrix.java
- Add class-level Javadoc explaining cached matrix using Pardiso solver
- Document constructor `@param matrix`
- Replace stale IDE template comment in `calculateMemoryRequirement()`
- Document `initLUDecomp()` explaining symmetry check

## PostCalculatable.java
- Add `@param dt` and `@param t` documentation to `doCalculation`
- Add Javadoc to `doInitialization()`

## PotentialArea.java
- Remove unused `static long counter = 0` field
- Add Javadoc to all methods (`geometricOnSamePotential`, `mergePotential`, `hasComponentConnection`, etc.)
- `getTermConnectors`, `getTermComponents`, `addTermConnector` all throw `UnsupportedOperationException("Not yet implemented")` -- implement or remove
- Remove commented-out block in `isEmptyPotential`
- Fix typo "mergin" -> "merging"

## PotentialCoupable.java
- Fix typo "Voltge" -> "Voltage" in class Javadoc
- Add `@return` documentation to `getPotentialCoupling()`

## PotentialCoupling.java
- Add class-level Javadoc explaining label-based potential couplings
- Document constructor parameters
- Add Javadoc to all methods and inner classes

## SchematicComponentSelection2.java
- Add class-level Javadoc explaining the tabbed component palette
- Document type arrays (`_typLK`, `_typMotor`, `_typSubcircuit`, `_typCONTROL`, etc.)
- Document inner classes and magic numbers

## SchematicEditor2.java
- Add class-level Javadoc explaining the main mouse-driven schematic editor controller
- Document the `MouseMoveMode` enum and state machine transitions
- Replace magic numbers (350ms double-click, 50ms drag, 10ms sleep) with named constants
- Fix bug in `isRightMouseClickActionOrCtrlLeftClick`: both sides of `||` are identical
- Remove dead code: `isLabelRenameRequired`, commented-out clipboard block, redundant assignment

## SchematicTextInfo.java
- Add class-level Javadoc explaining parameter text display beside components
- **Bug**: `_yTxtKlickMin` assigned then `_yTxtKlickMax` assigned to same expression in `updateRanges`
- **Bug**: line 121 has duplicate condition `_dxTxt != _dxTxtBeforeMove` (second should be `_dyTxt`)
- Document fields and inner classes

## SimulationKernel.java
- Add class-level Javadoc explaining the core time-stepping simulation engine
- Document public static mutable fields (`tSTART`, `tEND`, `counter`)
- Document German-named fields and all major methods
- Replace magic numbers: `10000` (max iterations), `0.99`/`0.9999999` (perturbation), `0.5` (switch threshold)
- Remove commented-out `external_step` method block
- Document `SimulationStatus` enum

## SolverSettings.java
- Add class-level Javadoc explaining user-configurable solver parameters
- Document each field (`SOLVER_TYPE`, `_T_pre`, `_dt_pre`, `dt`, `_tDURATION`, `_tPAUSE`)
- Make `SOLVER_TYPE` final for consistency (or document why mutable)
- Fix typo "stepwidth" -> "step width"

## SpecialType.java
- Add class-level Javadoc explaining "special" non-circuit component types
- Document magic numbers `27` and `70` in enum constants

## SubCircuitTerminable.java
- Add class-level Javadoc explaining terminals bridging subcircuit sheet and parent block
- Document each method

## SymmetricDoubleSparseMatrix.java
- Add class-level Javadoc explaining sparse symmetric matrix using HashMaps
- Document magic number `1e-70` (placeholder for Pardiso solver)
- Document all methods
- Fix bug risk in `removeZeroEntry`: removes by value instead of by key

## SymmetricSparseMatrix.java
- Add class-level Javadoc expanding on CSR format explanation
- Remove or document unused fields (`Acsr`, `AcsrComplex`, `AIT`, `AJT`, `columnRowIndices`)
- Document `factorize` parameters
- Document magic `mtype` value `-2`

## TerminalConnection.java
- Add class-level Javadoc explaining Connection's start/end point as terminal
- Document `Location` enum, constructor, all methods

## TerminalControl.java
- Add class-level Javadoc explaining control-circuit terminal with node numbering
- Document `paintControlState` and magic numbers (`CIRCLE_DIAMETER = 6`, font size `10`)
- Document the fragile value-to-string formatting via `substring`

## TerminalControlBidirectional.java
- Add class-level Javadoc explaining bidirectional control terminals
- Document possible bug: `createCopy` returns `TerminalControlInput` not `TerminalControlBidirectional`
- Document constructor and `paintLabelString`

## TerminalControlInput.java
- Add class-level Javadoc explaining control input terminal
- Document constructor, `paintComponent`, `createCopy`, `paintLabelString`

## TerminalControlOutput.java
- Add class-level Javadoc explaining control output terminal (triangle)
- Document magic numbers `0.7` and `0.3` (triangle sizing fractions)
- Document constructor, `createCopy`, `paintLabelString`

## TerminalFixedPosition.java
- Add class-level Javadoc explaining fixed absolute position terminal
- Document constructor parameters

## TerminalFixedPositionInvisible.java
- Promote inline comment to proper class-level Javadoc
- Fix typos "ther" -> "the" and "temperatre" -> "temperature"
- Document `paintLabelString(Graphics2D, int)` signature difference

## TerminalHiddenSubcircuit.java
- Add class-level Javadoc explaining invisible internal subcircuit terminal

## TerminalInterface.java
- Add class-level Javadoc explaining the base interface for all terminal types
- Document each method

## TerminalRelativeFixedDirection.java
- Add class-level Javadoc explaining fixed direction terminal
- Document `getPointFromDirection` and verify possible bug in `WEST_EAST` case

## TerminalRelativePosition.java
- Add class-level Javadoc explaining relative position terminal following parent rotation
- Document `getRelativeX`, `getRelativeY`, `getPointFromDirection`

## TerminalRelativePositionReluctance.java
- Add class-level Javadoc explaining reluctance-domain colored terminal
- Fix typo `poxX` -> `posX`

## TerminalSubCircuitBlock.java
- Add method-level Javadoc to all methods
- Fix typo "subcuircuit" -> "subcircuit" in class Javadoc

## TerminalToWrap.java
- Add method-level Javadoc to all methods
- Document magic numbers in `reCalculateLocation`
- Document suspicious condition in `moveComponent` (`moveToPoint.x == moveToPoint.y`)

## TerminalTwoPortComponent.java
- Add class-level Javadoc explaining offset terminal
- Document `_isFlowSymbolTerminal` field

## TerminalTwoPortRelativeFixedDirection.java
- Add class-level Javadoc explaining two-port terminal with fixed direction
- Document constructor parameters

## TimeFunction.java
- Add class-level Javadoc explaining time-dependent source functions with history tracking
- Document all protected fields and methods for back-stepping algorithm

## TimeFunctionConstant.java
- Add class-level Javadoc explaining constant (DC) time function
- Document constructor, `setValue`, `calculate`, `stepBack`
- Add comment explaining empty `stepBack()` is intentional

## TokenMap.java
- Add method-level Javadoc to the many overloaded `readDataLine(...)` variants
- Remove or implement empty/dead `makeBlockTokenMap` and `getBlockMap()` (returns null)
- Document `SpecialPair`, `BlockInfo` inner classes
- Document `findSubBlock`, `createSubBlock`, `leseASCIITextBlock`, etc.

## ToolBar.java
- Verify whether this is dead code (leftover demo/test with `main()` and hardcoded `.gif` files)
- Remove stale IDE template comment
- If kept, add null-checks for `ImageIcon` loads

## UniqueObjectIdentifer.java
- Fix class name typo: `Identifer` -> `Identifier`
- Add class-level Javadoc explaining unique object ID generation
- Document all methods
- Remove commented-out assert
- Make `generator` field `final`

## WindowCloseable.java
- Add method-level Javadoc to `closeWindow()`
- Expand class Javadoc with implementation guidance

## WorksheetSize.java
- Add class-level Javadoc explaining worksheet (canvas) dimensions management
- Document magic-number constants and the `getOldFormatWSSize` backwards-compat mapping
- Document all methods
- Document inconsistency: `DEFAULT_SIZE = 40` but `getOldFormatWSSize` returns `30` as default
