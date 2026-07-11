# Phase 7: German-to-English Translations and Aliases

This file lists all the target files and specific tasks for Phase 7: German-to-English Translations and Aliases parsed from the review files.

Total target files: 68

## File and Task List

### [AbstractCircuitSheetComponent.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractCircuitSheetComponent.java)
- Document the `dpix` static field (comment is only in German)

### [AbstractSignalCalculatorPeriodic.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractSignalCalculatorPeriodic.java)
- Add Javadoc on all protected fields (German names `_aufsteigend`, `_anteilDC` need English docs)

### [AbstractVoltageSourceCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/AbstractVoltageSourceCalculator.java)
- Translate German comment on line 51 to English

### [Axis.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Axis.java)
- Add Javadoc to `blendeEventuellGridLinienAus` (German) with English

### [AxisConnection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisConnection.java)
- Add class-level Javadoc explaining axis assignment enum (German names `ZUORDNUNG_X`, etc.)

### [AxisDesignSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisDesignSettings.java)
- Add English Javadoc to German methods (`getAchseBeschriftung`/`setAchseBeschriftung`)

### [AxisGridSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisGridSettings.java)
- Add English to German field names (`_farbeGridNormal`, `_linStilGridNormal`)

### [AxisLinLog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLinLog.java)
- Translate German enum constants `ACHSE_LIN`/`ACHSE_LOG` to `LINEAR`/`LOGARITHMIC` (or add English aliases)

### [AxisTickSettings.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisTickSettings.java)
- Document `_anzTicksMinor` field (German: "number of minor ticks")

### [Connection.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Connection.java)
- Add Javadoc to German-named methods (`setzeStartKnoten`, `setzeEndKnoten`, `setzeAktuellenPunktAufConnection`)

### [ControlFromEXTERNAL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlFromEXTERNAL.java)
- Translate German comments to English

### [ControlGainDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlGainDialog.java)
- Translate German comments to English

### [ControlIntegratorDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlIntegratorDialog.java)
- Translate German comments to English

### [ControlJavaTriangles.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/ControlJavaTriangles.java)
- **Fix German-derived field name typo:** `_xKlickMinTerminal`, `_xKlickMaxTerminal` (line 22) use "Klick" (German spelling). Rename to `_xClickMinTerminal` etc.; update callers in `ControlNativeC` (lines 305-318)

### [ControlNativeC.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/nativec/ControlNativeC.java)
- **Translate German method name: `istAngeklickt`** (line 226) means "is clicked". Rename to `isClicked` and update `@Override` in parent class
- **Translate German token keys:** `anzXIN` and `anzYOUT` (lines 342, 349) derive from "Anzahl" (German for "count"). Add English keys or document why German keys must be retained for backward compatibility

### [ControlPDDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPDDialog.java)
- Translate German comments to English

### [ControlPIDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPIDialog.java)
- Translate German comments to English

### [ControlPTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlPTDialog.java)
- Translate German comments to English

### [ControlRandomDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlRandomDialog.java)
- Translate the German method name `baueGuiIndividual()` to English (e.g., `buildIndividualGUI()`); this is an override, so the rename must be coordinated with the superclass `DialogElementCONTROL`

### [ControlRandomWalk.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlRandomWalk.java)
- Consider translating or documenting the German identifier `_typQuelle` (meaning "source type") and the enum constant `QUELLE_RANDOM` (QUELLE = "source" in German) if cross-file refactoring scope allows

### [ControlSaveData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSaveData.java)
- Translate the German identifier `SaveModus` (Modus = "mode") to `SaveMode` -- note this is used in import/export serialization so field name changes are safe but ordinal-based enum storage must be preserved

### [ControlSignalSource.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignalSource.java)
- Translate German identifiers and string literals used in code: `typus` (line 262, 439 etc.), `fabrikSignalCalculator` (German "fabrizieren"), `aufsteigend`/`_dreieck` (line 119 comment), and German UserParameter keys `anteilDC`, `tastverhaeltnis`, `frequenz`
- Translate the German comment on line 119 (`"for TRI, RECHT-states we simple store variables 'aufsteigend' and '_dreieck'"`) to English

### [ControlSignalSourceDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSignalSourceDialog.java)
- Translate the German method name `baueGuiIndividual()` to English (e.g., `buildIndividualGUI()`), coordinated with superclass
- Translate the German comment `// Abstand` on line 107 to English (means "spacing"/"gap")

### [ControlSlidingDFTDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSlidingDFTDialog.java)
- Translate the German method name `baueGuiIndividual()` to English (e.g., `buildIndividualGUI()`), coordinated with superclass

### [ControlSpaceVector.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSpaceVector.java)
- Translate the German parameter name `knotenIndex` (line 58, meaning "node index") to `nodeIndex`, and translate the German comment on lines 59-60

### [CounterCalculatable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/CounterCalculatable.java)
- Translate German comments to English

### [DelayCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/DelayCalculator.java)
- Add Javadoc on all private fields (German names `_youtVerzoegert`, `_zeigerYOUT`, etc.)

### [DetailedConductionLossPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/DetailedConductionLossPanel.java)
- Translate German comment to English

### [DialogConnectSignalsGraphs.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/DialogConnectSignalsGraphs.java)
- Add English for German methods (`baueGUI`, `aktualisiereGrafer`)

### [DialogExternal.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/DialogExternal.java)
- Document `_regelBlock` field (German for "control block")

### [DialogFourierDiagramm.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DialogFourierDiagramm.java)
- **Translate German method and variable names to English.** `baueGUI()` -> `buildGUI()`, `_signalFourierAnalysiert` -> `_signalFourierAnalyzed`

### [DisplayFourierWorksheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/DisplayFourierWorksheet.java)
- **Translate German method names to English.** `baueGUI()` -> `buildGUI()`, `schreibeData()` -> `writeData()`

### [FourierKurvenRekonstruktion.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/FourierKurvenRekonstruktion.java)
- **Translate German comments and variable names to English.** Comments like `"braucht es, damit kein NullPointer-Error"`, field names `mausModus`, `xSchieberAktiv`, `xSchieberPix`, `imDragModus`

### [GeckoSimulink.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/GeckoSimulink.java)
- Translate German comments to English

### [GeckoStatusBar.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/GeckoStatusBar.java)
- Add Javadoc to `setzeStatusRechenzeit()` (German: compute time display)

### [IntegratorCalculation.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/IntegratorCalculation.java)
- Translate German comments to English

### [LangInit.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/LangInit.java)
- Document the translation map initialization

### [LossCalculationDetailed.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossCalculationDetailed.java)
- Rename or document German field `datnamGemesseneVerluste`

### [LossProperties.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/LossProperties.java)
- Translate German field comment `Eigenschaften des Halbleiters` to English

### [MainWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/MainWindow.java)
- Translate German method names (`aktualisiereDividerSplitPane`, `setzeSTATUS`)

### [MethodCategory.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/MethodCategory.java)
- Fix typo `_tranlsationKey` -> `_translationKey`

### [NetListLK.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListLK.java)
- Document public fields with German names (`knotenMAX`, `spgQuelleMAX`, `parameter`, etc.)

### [NetlistControl.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/NetlistControl.java)
- Translate German method name `optimiereAbarbeitungsListe()` or provide English Javadoc

### [NetlistGeneral.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetlistGeneral.java)
- Translate German comment fragment "gleichlautenden Label" to English

### [NodeLabel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NodeLabel.java)
- Document all public methods (German names: `setKoordTxt`, `setKoordAnker`, `istAngeklickt`, `zeichne`)

### [NotCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NotCalculator.java)
- Translate German comment "Logik-Schwellwert --> 0.5" to English

### [PICalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PICalculator.java)
- Add Javadoc on all fields (German names `y1alt`, `xalt`, `y11` need English docs)

### [PT1Calculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT1Calculator.java)
- Translate German comments to English
- Fix incorrect German comment "Speicherung des I-Anteils" (should be "storage of previous output")

### [PowerAnalysisPanel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/PowerAnalysisPanel.java)
- Translate German field names

### [PreviewDialogRectangular.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/PreviewDialogRectangular.java)
- Translate German variable names (`dreieck`, `aufsteigend`, `tastverhaeltnis`, `frequenz`)

### [Scopable.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/scope/Scopable.java)
- **Translate German parameter and method names to English.** `mausmodus` -> `mouseMode`, `getZVDatenImRAM()` -> `getZVDataInRAM()`, `ladeWorkSheet()` -> `loadWorkSheet()`

### [SignalCalculatorImport.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorImport.java)
- Translate German comments to English

### [SimulationKernel.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationKernel.java)
- Document German-named fields and all major methods

### [SparseMatrixCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SparseMatrixCalculator.java)
- Translate all German field names to English

### [SubCircuitSheet.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/SubCircuitSheet.java)
- Document button action listeners and `wireModeVersteckt` field (German)

### [SwitchingLossCurve.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/SwitchingLossCurve.java)
- Translate German/English mixed comments to English

### [ThermAmbient.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermAmbient.java)
- Clean up mixed German/English comments

### [ThermMODUL.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ThermMODUL.java)
- Translate German field names (`_xBef`, `_yBef`, `getChipAnzahl`, `setDateiname`)

### [Thyristor.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/Thyristor.java)
- Translate German class comment to English

### [VoltageSourceDCMachineCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/VoltageSourceDCMachineCalculator.java)
- Translate German variable names (`phi`, `emk`, `drehzahl`, `omegaALT`, `momentElektr`)

### [bot/DLbot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/bot/DLbot.java)
- Add class-level Javadoc explaining translation download bot

### [bot/UPbot.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/bot/UPbot.java)
- Add class-level Javadoc explaining translation upload bot
- Add Javadoc to `addTranslationSuggestion_single()` and `_multiple()`

### [resources/EnglishMapper.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/resources/EnglishMapper.java)
- Add class-level Javadoc explaining English key-value pair map initialization
- Remove ~330 lines of commented-out dead code in `initEnglishMap_single()`
- Remove ~14 lines of commented-out code in `initEnglishMap_multiple()`
- Investigate: `initEnglishMap_multiple()` returns empty DoubleMap (all entries commented out)

### [translationtoolbox/PopupListener.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/PopupListener.java)
- Document why popup is suppressed when language is ENGLISH

### [translationtoolbox/TranslationDialog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationDialog.java)
- Add class-level Javadoc explaining translation dialog

### [translationtoolbox/TranslationPopupMultiple.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationPopupMultiple.java)
- Add class-level Javadoc explaining multi-line translation popup
- Note: nearly identical to `TranslationPopupSingle` -- consider extracting common base

### [translationtoolbox/TranslationPopupSingle.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationPopupSingle.java)
- Add class-level Javadoc explaining single-line translation popup

### [translationtoolbox/TranslationTools.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/i18n/translationtoolbox/TranslationTools.java)
- Add class-level Javadoc explaining translation toolbox main window

