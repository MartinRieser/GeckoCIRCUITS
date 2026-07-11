### [NativeCBlock.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCBlock.java)
- **Add Javadoc to all methods:** `calculateYOUT`, `loadLibraries`, `unloadLibraries`, `checkOutputsForNANorINFValues`, and the constructor

### [NativeCClassLoader.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCClassLoader.java)
- **Add Javadoc to `findClass` method and its `name` parameter** (line 38)
- **Add Javadoc to the no-arg constructor** and `toString` override

### [NativeCDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCDialog.java)
- **Add Javadoc to methods:** `initFileChooser` (line 224) and `isFileNameAlreadyInList` (line 238)

### [NativeCLibraryFile.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCLibraryFile.java)
- **Complete the empty `@return` tag** in `getTimeStamp()` Javadoc (line 98)
- **Add Javadoc to all undocumented methods:** `getFileName`, `setFile(File)`, `setFile(String)`, `setFile()`, `getFile`, `updateTimeStamp`, and both parameterized constructors

### [NativeCWrapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/NativeCWrapper.java)
- **Add missing `@param xOUTVector` to `calcOutputs` Javadoc** (lines 32-39). Also add `@param` tags for `xINVector` and `deltaT`
- **Remove invalid `@return` tag** (line 38) from `calcOutputs` -- the method returns `void`. Move description text to `@param xOUTVector`
- **Improve `initParameters` Javadoc** (lines 43-45): Note that this is a `native` method implemented in the external C/C++ library

### [NetListContainer.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListContainer.java)
- Add class-level Javadoc explaining the container aggregating three netlists
- Document each static fabric method (`fabricStartSimulation`, `fabricContinueSimulation`, `fabricGuiUpdate`)
- Document public fields `_nlControl`, `_nlLK`, `_nlTH`

### [NetListLK.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListLK.java)
- Add class-level Javadoc explaining LK (power circuit) netlist
- Add Javadoc to all major methods

### [NetlistControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/NetlistControl.java)
- Add class-level Javadoc explaining control-domain netlist management
- Add Javadoc on all factory methods
- Document `IndexConnection` inner class

### [NetlistGeneral.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetlistGeneral.java)
- Add class-level Javadoc explaining the general netlist builder
- Remove or document unused `static int counter` field
- Add Javadoc to private methods (`createPotentialSheetConnectedGeometric`, `mergePotentialAreasViaLabels`, etc.)
- Add Javadoc to `GraphEdge` inner class and `traverse` method

### [NewScope.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NewScope.java)
- Add class-level Javadoc (~900+ lines) describing scope panel, cursor logic, signal management
- Add Javadoc to cursor measurement methods
- Document keyboard shortcut bindings

### [NiceScale.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NiceScale.java)
- Add class-level Javadoc explaining "nice number" axis scaling (Heckbert's algorithm)

### [NoCurveSelectedException.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/NoCurveSelectedException.java)
- Add class-level Javadoc explaining when this exception is thrown

### [NodeLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NodeLabel.java)
- Add class-level Javadoc explaining schematic node label with anchor and click area
- `importASCII` has an empty body -- implement or document why

### [NonLinearDialogPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NonLinearDialogPanel.java)
- Add class-level Javadoc explaining non-linear characteristic editing dialog
- Document constructor parameters (`parentDialog`, `elementLK`, `isYAxisLog`)

### [NonLinearReluctance.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/NonLinearReluctance.java)
- Add Javadoc on class explaining nonlinear reluctance (magnetic saturation)

### [NonlinearReluctanceDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/NonlinearReluctanceDialog.java)
- Add Javadoc on class

### [Nonlinearable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/Nonlinearable.java)
- Add Javadoc on interface explaining nonlinear characteristic contract

### [NotCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NotCalculator.java)
- Add class-level Javadoc: "Logical NOT -- inverts signal based on SIGNAL_THRESHOLD"

### [NotEqualCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NotEqualCalculator.java)
- Add class-level Javadoc: "Outputs 1 if inputs are not equal"

### [NothingToDoCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NothingToDoCalculator.java)
- Add class-level Javadoc explaining pass-through/no-op calculator
- Remove stale IDE template comment

### [OperatingMode.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/OperatingMode.java)
- Add Javadoc to enum explaining operating modes
- Add Javadoc to each enum constant

### [Operationable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/Operationable.java)
- Add Javadoc on interface explaining scriptable operations
- Add Javadoc on `OperationInterface` abstract class, `fabricFromString()`, `doOperation()`

### [OperationalAmplifier.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/OperationalAmplifier.java)
- Add Javadoc on class explaining ideal op-amp model

### [OperationalAmplifierDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/OperationalAmplifierDialog.java)
- Add Javadoc on class

### [OptimizerParameterData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/OptimizerParameterData.java)
- Add class-level Javadoc explaining optimizer parameter storage

### [OrCalculatorMultipleInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/OrCalculatorMultipleInputs.java)
- Add class-level Javadoc: "Logical OR across all N inputs"

### [OrCalculatorTwoInputs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/OrCalculatorTwoInputs.java)
- Add class-level Javadoc: "Logical OR of two inputs"

### [OutputWarningStream.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/OutputWarningStream.java)
- Add class-level Javadoc explaining this monitors output volume and warns about excessive output
- Add Javadoc to all public methods
- Document why `byteCounter` and `warningBytesSize` are `static`
- Add field-level comments for `_verbosityWarnShown`, `_isOriginalOutput`, `_ignoreFutureMessages`

### [PDCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PDCalculator.java)
- Add class-level Javadoc explaining PD controller
- Add Javadoc on numerical differentiation formula

### [PICalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PICalculator.java)
- Add class-level Javadoc explaining PI controller using trapezoidal integration

### [PT1Calculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT1Calculator.java)
- Add class-level Javadoc explaining PT1: G(s) = K/(1+sT)

### [PT2Calculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT2Calculator.java)
- Add class-level Javadoc explaining PT2: G(s) = K/(1+(sT)^2)

### [PanelCharacteristicsResult.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PanelCharacteristicsResult.java)
- Add class-level Javadoc explaining results display panel

### [Paradiso.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Paradiso.java)
- Add class-level Javadoc explaining Intel PARDISO sparse direct solver wrapper
- Document `factorize` and `solve` parameters and `mtype` values

### [ParameterSupport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckoscript/ParameterSupport.java)
- Add class-level Javadoc explaining parameter management for scriptable components
- Document parameter name resolution mechanism

### [PardisoCachedMatrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PardisoCachedMatrix.java)
- Add class-level Javadoc explaining cached matrix using Pardiso solver
- Document constructor `@param matrix`
- Replace stale IDE template comment in `calculateMemoryRequirement()`
- Document `initLUDecomp()` explaining symmetry check

### [PmsmControlCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmControlCalculator.java)
- Add class-level Javadoc explaining PMSM field-oriented control
- Add Javadoc on all 14+ fields (none documented)
- Document all 12 input signals and 8 output signals

### [PmsmModulatorCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmModulatorCalculator.java)
- Add class-level Javadoc explaining Space Vector PWM for 2-level 3-phase inverter
- Document all intermediate variables
- Document sector detection using if-statements instead of else-if

### [Point.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/Point.java)
- Add Javadoc on class, constructor, `distance()`, `equals()`, `hashCode()`, `toString()`

### [PolynomTools.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PolynomTools.java)
- Add class-level Javadoc explaining polynomial manipulation utilities
- Document or remove `main()` test method
- Fix Javadoc error: `[1 2 0 4] == 1 + 2s + 4s^4` should be `1 + 2s + 4s^3`

### [Polynomials.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Polynomials.java)
- Add `@param` and `@return` Javadoc tags to `poldiv` -- currently only has a prose description, missing documentation for all six parameters
- Add a class-level Javadoc with a brief usage example showing how to call `poldiv` for polynomial division

### [PostCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PostCalculatable.java)
- Add `@param dt` and `@param t` documentation to `doCalculation`
- Add Javadoc to `doInitialization()`

### [PostProcessable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/PostProcessable.java)
- Add Javadoc on interface explaining post-solve calculation contract

### [PotentialArea.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialArea.java)
- Add Javadoc to all methods (`geometricOnSamePotential`, `mergePotential`, `hasComponentConnection`, etc.)

### [PotentialCoupable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialCoupable.java)
- Add `@return` documentation to `getPotentialCoupling()`

### [PotentialCoupling.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/PotentialCoupling.java)
- Add class-level Javadoc explaining label-based potential couplings
- Document constructor parameters
- Add Javadoc to all methods and inner classes

### [PowerAnalysisPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PowerAnalysisPanel.java)
- Add class-level Javadoc explaining power analysis (P, Q, S, D, cos(phi))
- Add Javadoc to `calculate` method
- Fix comment "performance values A and B" (grid has 3 columns: A, B, C)

### [PowerAnalysisSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PowerAnalysisSettings.java)
- Add class-level Javadoc explaining voltage/current indices for phases A/B/C

### [PowerCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PowerCalculator.java)
- Add class-level Javadoc explaining power calculation algorithm
- Add Javadoc to each power quantity getter with formula

### [PowerModulePainter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/PowerModulePainter.java)
- Add Javadoc on class explaining thermal power module rendering


