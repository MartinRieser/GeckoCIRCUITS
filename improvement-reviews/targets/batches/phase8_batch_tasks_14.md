### [SimpleControlBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SimpleControlBlock.java)
- Add class-level Javadoc explaining base for info-only control blocks

### [SimpleGraferPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SimpleGraferPanel.java)
- Add class-level Javadoc explaining simple grafer panel wrapper

### [SimulationAccess.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/SimulationAccess.java)
- Add class-level Javadoc explaining scripting access to simulation engine
- Add `@param`/`@return` to all methods
- Document thread-safety: which methods must be called on EDT

### [SimulationKernel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationKernel.java)
- Add class-level Javadoc explaining the core time-stepping simulation engine
- Document public static mutable fields (`tSTART`, `tEND`, `counter`)
- Document `SimulationStatus` enum

### [SimulationRunner.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SimulationRunner.java)
- Add class-level Javadoc explaining simulation lifecycle management
- Add Javadoc to `startCalculation()`, `continueCalculation()`, `pauseSimulation()`

### [SinCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SinCalculator.java)
- Add class-level Javadoc: "Calculates sine"

### [Slider.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Slider.java)
- Add class-level Javadoc explaining slider measurement component

### [SliderContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SliderContainer.java)
- Add class-level Javadoc explaining slider container with measurement modes

### [SliderUtils.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SliderUtils.java)
- Add class-level Javadoc explaining slider calculation utilities

### [SliderValues.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SliderValues.java)
- Add class-level Javadoc explaining slider measurement value container

### [SlidingDFTCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SlidingDFTCalculator.java)
- Add class-level Javadoc explaining Sliding Discrete Fourier Transform
- Add Javadoc on all fields
- Add Javadoc on `doSlidingFourierStep()` explaining recursive DFT update formula

### [SmallSignalCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SmallSignalCalculator.java)
- Add class-level Javadoc explaining Small Signal Analysis via multi-sine excitation and FFT
- Add Javadoc on `tearDownOnPause()` explaining FFT and Bode calculation
- Document static `_bode` field (shared across instances)

### [SolverSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SolverSettings.java)
- Add class-level Javadoc explaining user-configurable solver parameters
- Document each field (`SOLVER_TYPE`, `_T_pre`, `_dt_pre`, `dt`, `_tDURATION`, `_tPAUSE`)
- Make `SOLVER_TYPE` final for consistency (or document why mutable)

### [SolverType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SolverType.java)
- Add Javadoc to enum explaining available solver types (TRAPEZOIDAL, BACKWARD_EULER, GEAR_SHICHMAN)

### [SourceFileGenerator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/SourceFileGenerator.java)
- Add class-level Javadoc explaining Java source code generation from block definitions

### [SourceType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SourceType.java)
- Add Javadoc on enum

### [SpaceVectorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SpaceVectorCalculator.java)
- Add class-level Javadoc explaining 9-input display feeding
- Document `NO_INPUTS = 9` (what each of the 9 inputs represents)

### [SpaceVectorDisplay.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SpaceVectorDisplay.java)
- Add class-level Javadoc explaining real-time space vector display
- Document thread-safety concern of `counter` static field
- Document `HISTORY_BUFFER_SIZE = 100000` memory implications

### [SparseMatrixCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SparseMatrixCalculator.java)
- Add class-level Javadoc explaining sparse matrix converter modulation algorithm
- Document massive code duplication in switch statements for sectors 1-12

### [SpecialNameVisible.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SpecialNameVisible.java)
- Add Javadoc on `isNameVisible()` and `setNameVisible()` methods

### [SpecialType.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SpecialType.java)
- Add class-level Javadoc explaining "special" non-circuit component types

### [SpecialTypeInfo.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SpecialTypeInfo.java)
- Add Javadoc on class explaining special (non-circuit) type info

### [SqrtCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SqrtCalculator.java)
- Add class-level Javadoc: "Calculates square root"

### [SquareCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SquareCalculator.java)
- Add class-level Javadoc: "Calculates x^2"

### [StartFromBlocksWithoutPredecessorOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromBlocksWithoutPredecessorOrderer.java)
- Add class-level Javadoc explaining ordering starting from no-input blocks

### [StartFromBlocksWithoutSuccessorOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromBlocksWithoutSuccessorOrderer.java)
- Add class-level Javadoc explaining ordering starting from no-output blocks

### [StartFromSinkOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromSinkOrderer.java)
- Add class-level Javadoc explaining ordering starting from sink blocks

### [StartFromSourceOrderer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StartFromSourceOrderer.java)
- Add class-level Javadoc explaining ordering starting from source blocks

### [StartsWithMatcher.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/StartsWithMatcher.java)
- Add class-level Javadoc explaining prefix matching

### [StartupWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/StartupWindow.java)
- Add class-level Javadoc explaining startup splash window
- Document blocking vs. non-blocking startup

### [StateSpaceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/StateSpaceCalculator.java)
- Add Javadoc on `calculateTimeStep()` explaining trapezoidal integration
- Document `MAX_DEGREE_DIFF = 3` and the intentional switch fallthrough

### [SubCircuitSheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SubCircuitSheet.java)
- Add class-level Javadoc explaining circuit sheet inside a subcircuit block

### [SubCircuitTerminable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SubCircuitTerminable.java)
- Add class-level Javadoc explaining terminals bridging subcircuit sheet and parent block
- Document each method

### [SubcircuitBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SubcircuitBlock.java)
- Add Javadoc on class explaining subcircuit container
- Fix `if(1>0) return true;` in `areTerminalPositionsOK()` -- implement properly or explain
- Add Javadoc on `copyFabric()`, `paintIndividualComponent()`, `getColorForTerminal()`

### [SubtractionMoreParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SubtractionMoreParameter.java)
- Add class-level Javadoc: "Subtracts all subsequent inputs from first"

### [SubtractionTwoParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SubtractionTwoParameter.java)
- Add class-level Javadoc: "Subtracts second input from first"

### [SuggestMatcher.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SuggestMatcher.java)
- Add Javadoc to interface explaining matching contract

### [SuggestionField.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/SuggestionField.java)
- Add class-level Javadoc explaining auto-suggest text field

### [SwitchState.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/SwitchState.java)
- Add Javadoc on class explaining switch state snapshot
- Add Javadoc on inner `State` enum

### [SwitchingLossCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/SwitchingLossCurve.java)
- Add class-level Javadoc (replace informal `// //` comment)
- Add Javadoc to constructor `@param tj` (junction temperature C), `@param uBlock` (blocking voltage V)
- Add Javadoc to `importIndividual()` explaining legacy data-repair when `data.length == 4`

### [SymbolSettable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/SymbolSettable.java)
- Add class-level Javadoc describing symbol setter interface

### [SymmetricDoubleSparseMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SymmetricDoubleSparseMatrix.java)
- Add class-level Javadoc explaining sparse symmetric matrix using HashMaps
- Document all methods

### [SymmetricSparseMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SymmetricSparseMatrix.java)
- Add class-level Javadoc expanding on CSR format explanation
- Document `factorize` parameters
- Document magic `mtype` value `-2`

### [SystemOutputRedirect.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/SystemOutputRedirect.java)
- Replace empty class-level Javadoc with description of stdout/stderr redirection utility
- Add Javadoc to `init()`, `setAlternativeOutput()`, `setOriginalOutput()`, `setConsoleOutput()`, `reset()`

### [TanCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/TanCalculator.java)
- Add class-level Javadoc: "Calculates tangent"
- Document `SMALL_NUMBER` constant and PI/2 singularity assertions

### [TechFormat.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/TechFormat.java)
- Add class-level Javadoc explaining technical number formatting utility
- Document format patterns (scientific, engineering notation)

### [TerminalCircuit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/TerminalCircuit.java)
- Add Javadoc on class (leaf class)

### [TerminalCircuitDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/TerminalCircuitDialog.java)
- Add Javadoc on class

### [TerminalConnection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalConnection.java)
- Add class-level Javadoc explaining Connection's start/end point as terminal
- Document `Location` enum, constructor, all methods

### [TerminalControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/TerminalControl.java)
- Add class-level Javadoc explaining control-circuit terminal with node numbering
- Document the fragile value-to-string formatting via `substring`


