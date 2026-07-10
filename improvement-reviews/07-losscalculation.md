# Improvement Tasks: ch/technokrat/gecko/geckocircuits/circuit/losscalculation/ (22 files)

## AbstractLossCalculator.java
- Fix stale `@param state` in `calcLosses()` Javadoc (no such parameter exists)
- Remove erroneous `@return` tag on `void calcLosses()` method
- Add Javadoc to the interface explaining the two-method contract
- Add `@param deltaT` unit documentation (seconds)

## AbstractLossCalculatorFabric.java
- Add Javadoc to the interface explaining the factory contract
- Add Javadoc to `lossCalculatorFabric()` documenting return value

## AbstractLossCalculatorSwitch.java
- Add Javadoc to abstract class explaining switch/semiconductor loss calculation
- Add Javadoc to `calcLosses()`, `detectTurnOn()`, `detectTurnOff()`
- Add Javadoc to abstract methods `calcConductionLoss()`, `calcTurnOnSwitchingLoss()`, `calcTurnOffSwitchingLoss()`
- Document `_oldCurrent`/`_oldVoltage` fields and sentinel value `-1`
- Document `EPS = 1e-2` constant (practically zero current threshold)

## ConductionLossMeasurementCurve.java
- Add class-level Javadoc (replace informal `// //` comment)
- Add Javadoc to constructor `@param tj`
- Add Javadoc to `copy()` documenting deep-copy behavior
- Add null/empty guard in `copy()`

## DetailedConductionLossPanel.java
- Add class-level Javadoc explaining 2-column editor (U, I)
- Add Javadoc to `useNonlinearInElectric()`
- Remove or document dead fields (`uMaxCOND`, `iMaxCOND`, `b0COND`, `c0COND`, etc.)
- Translate German comment to English

## DetailedLossLookupTable.java
- Add class-level Javadoc explaining 2D interpolation table (temperature vs. current)
- Add Javadoc to `fabric()` documenting `dataIndex` (1=Eon, 2=Eoff) and normalization
- Add Javadoc to `getInterpolatedYValue()` and `getInterpolatedXValue()` explaining bilinear interpolation
- Fix repeated misspelling `wheigt` -> `weight`
- Document assertion tolerance magic numbers `1.01` and `0.99`

## DetailedSwitchingLossesPanel.java
- Add class-level Javadoc explaining 3-column editor (I, Eon, Eoff)
- Add Javadoc to all overridden methods
- Fix double semicolon typo on line 25
- Document magic string `"600"` default voltage

## DetailledLossPanel.java
- Add Javadoc to abstract class (note: class name typo "Detailled" vs "Detailed")
- Add Javadoc to all abstract methods
- Add Javadoc to `baueGUI()`
- Document the duplicate-temperature check (0.1 tolerance magic number)
- Fix misspelled field `_grafer` (should be "grapher")

## DialogLossesDetail.java
- Add class-level Javadoc explaining the modal editor dialog for detailed loss curves
- Fix typo in `fabricCreateExisiting()` -> `fabricCreateExisting()`
- Add Javadoc to `applyChanges()`, `doSaveAsNew()`, `getNewFileNameDialog()`, `createTestCurve()`
- Remove commented-out debug line
- Document magic tab indices `0` and `1`

## JPanelLossDataInterpolationSettings.java
- Add class-level Javadoc explaining settings panel for test/interpolation curves
- Add Javadoc to constructor and builder methods
- Add Javadoc to `setVoltageSelectionVisible()`
- Replace swallowed exception with proper error logging
- Document magic defaults `100` (temperature) and `300` (voltage)

## LossCalculatable.java
- Add Javadoc to the interface (capability marker for loss calculation)
- Add Javadoc to `getLossCalculation()`

## LossCalculationDetail.java
- Add Javadoc to enum explaining two detail levels (SIMPLE vs DETAILED)
- Add Javadoc to `getOldGeckoCIRCUITSOrdinal()` documenting magic numbers 1 and 2
- Add Javadoc to `getFromDeprecatedFileVersion()`
- Fix constructor parameter typo: `diplayString` -> `displayString`

## LossCalculationDetailed.java
- Add class-level Javadoc explaining detailed loss data from measurement files (.scl)
- Rename or document German field `datnamGemesseneVerluste`
- Add Javadoc to `readDetailedLossesFromFile()` explaining three nested fallback formats
- Add Javadoc to `writeDetailedLossesToFile()` -- unclear parameter names `fkaku`, `fyomu`
- Fix incorrect `@return String` on `checkLinkToSemiconductorFile()` (returns boolean)
- Investigate bug: `_temperature = DEFAULT_REFERENCE_VOLTAGE` (100) uses voltage constant as temperature
- Fix method name typo: `getCopyOfConductionLossMeasurementCurvenArray` -> `Curves`

## LossCalculationSimple.java
- Add class-level Javadoc explaining simplified loss formulas
- Document fields `_kON`, `_kOFF`, `_uSWnorm` (switching loss coefficients)
- Rename or document `UK_DEFAULT_VALUE` (400.0) -- "UK" is unclear
- Add Javadoc to inner class `LossCalculatorSwitchSimple`
- Remove debug print `System.out.println("xxxxxxxx " + _uSWnorm)`
- Document NaN check idiom `returnValue != returnValue`

## LossCalculationSplittable.java
- Add Javadoc to `getSwitchingLoss()` and `getConductionLoss()` documenting return unit (W)
- Fix class-level Javadoc wording

## LossCalculatorResistor.java
- Add class-level Javadoc explaining P = I*V loss calculation
- Add Javadoc to constructor and `calcLosses()` explaining temperature/deltaT are ignored

## LossComponent.java
- Add Javadoc to `toString()`, `getSaveString()`, `getEnumFromSaveString()`
- Document the fallback to `TOTAL` on unmatched strings

## LossContainer.java
- Consider adding a brief usage example in class Javadoc

## LossCurve.java
- Add Javadoc to abstract class explaining template method pattern for loss curves
- Add Javadoc to `importASCII()`, `exportASCII()`, abstract methods
- Document `data` public field's expected layout
- Document `tj` UserParameter (junction temperature, default 0.0 C)

## LossCurveTemperaturePanel.java
- Add class-level Javadoc explaining radio-button selection panel
- Add Javadoc to all methods
- Investigate `_gbc` variable (created but GridLayout is used -- possibly redundant)

## LossProperties.java
- Add class-level Javadoc explaining central loss-calculation configuration
- Add Javadoc to constructor, inner wrapper classes, `lossCalculatorFabric()`
- Add Javadoc to `exportASCII()`, `importASCII()`, `setLossType()`, `getLossType()`
- Translate German field comment `Eigenschaften des Halbleiters` to English

## SwitchingLossCurve.java
- Add class-level Javadoc (replace informal `// //` comment)
- Add Javadoc to constructor `@param tj` (junction temperature C), `@param uBlock` (blocking voltage V)
- Add Javadoc to `copy()` explaining deep-copy and sentinel values
- Add Javadoc to `importIndividual()` explaining legacy data-repair when `data.length == 4`
- Translate German/English mixed comments to English
