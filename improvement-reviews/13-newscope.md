# Improvement Tasks: ch/technokrat/gecko/geckocircuits/newscope/ (99 files)

## AbstractCurve.java
- Add class-level Javadoc describing the curve hierarchy base class
- Document thread-safety hazard: `static final GeneralPath GPATH` is shared mutable

## AbstractCurvePainter.java
- Add class-level Javadoc explaining the painter abstraction
- Add method-level Javadoc for all public/protected methods

## AbstractCurvePixelPainter.java
- Add class-level Javadoc describing pixel-level painting strategy

## AbstractDiagram.java
- Add class-level Javadoc (~500 lines, complex) describing diagram lifecycle, axis management, mouse interaction
- Document role of `_xAxis`, `_yAxis1`, `_yAxis2` (why exactly two Y axes)
- Add Javadoc to mouse listener methods explaining zoom/pan behavior
- Extract magic numbers for pixel offsets/margins to named constants

## AbstractScopeSignal.java
- Add class-level Javadoc explaining base signal abstraction
- Add Javadoc to all abstract methods

## AbstractTimeSerie.java
- Fix class name typo: "Serie" should be "Series"
- Fix Javadoc typo: "bigest" -> "biggest"
- Add Javadoc to all methods

## Axis.java
- Add class-level Javadoc (~700 lines) describing pixel calculation, tick/grid drawing
- Document all magic numbers in pixel calculations
- Add Javadoc to `blendeEventuellGridLinienAus` (German) with English
- Add Javadoc to pixel-to-data conversion methods

## AxisConnection.java
- Add class-level Javadoc explaining axis assignment enum (German names `ZUORDNUNG_X`, etc.)
- Add Javadoc to `iterateNext` and `getFromOrdinal`

## AxisDesignSettings.java
- Add class-level Javadoc explaining axis design settings
- Add English Javadoc to German methods (`getAchseBeschriftung`/`setAchseBeschriftung`)

## AxisGridSettings.java
- Add class-level Javadoc describing grid visibility logic
- Document magic constants `PX1 = 230`, `PX2 = 100`, `PXR = 2.5`
- Add English to German field names (`_farbeGridNormal`, `_linStilGridNormal`)

## AxisLimits.java
- Add class-level Javadoc describing global vs local autoscale, zoom history
- Add Javadoc to `getLimits()` documenting full decision tree
- Replace legacy `java.util.Stack` with `ArrayDeque`
- Fix `_HistoryStack` naming convention (should be `_historyStack`)
- Remove commented-out code in `importASCII`

## AxisTickSettings.java
- Add class-level Javadoc describing tick mark configuration
- Document `_anzTicksMinor` field (German: "number of minor ticks")

## BodePlot.java
- Add class-level Javadoc explaining Bode plot diagram type
- Add `@Deprecated` Javadoc explaining why deprecated and replacement

## BodePlot2.java
- Add class-level Javadoc explaining Bode plot and differences from `BodePlot`
- Document `@SuppressWarnings("deprecation")` rationale

## CharacteristicsCalculator.java
- Add class-level Javadoc describing calculated oscilloscope characteristics (RMS, mean, etc.)
- Document static mutable cache fields (thread-safety warning)
- Add Javadoc to each characteristic getter with formulas

## Cispr16Fft.java
- Add class-level Javadoc explaining CISPR 16 FFT algorithm
- Make public mutable fields private with getters
- Remove or document the `System.gc()` call

## Clipping.java
- Add class-level Javadoc explaining display modes enum
- Fix `assert false; return null;` pattern (risks NPE)

## ColorSettable.java
- Add class-level Javadoc describing color setter interface

## ColorStragegyDisabledComponent.java
- Fix class name typo: "Stragegy" -> "Strategy"
- Add class-level Javadoc

## ColorStrategySelected.java
- Add class-level Javadoc explaining selected element color strategy
- Remove redundant min/max clamping

## CompressedData.java
- Add class-level Javadoc explaining data compression strategy
- Remove unused imports `java.io.File` and `java.io.FileInputStream`

## CurveLabel.java
- Add class-level Javadoc explaining label drawing hierarchy

## CurveLabelRegular.java
- Add class-level Javadoc explaining regular curve labels

## CurveLabelSignal.java
- Add class-level Javadoc explaining digital signal curve labels

## CurvePainterRegular.java
- Add class-level Javadoc describing regular curve painting
- Replace legacy `java.util.Stack` with `ArrayDeque`

## CurvePainterSignal.java
- Add class-level Javadoc describing digital signal curve painting
- Replace legacy `java.util.Stack` with `ArrayDeque`

## CurvePixelPainterHiLow.java
- Add class-level Javadoc explaining hi/low pixel painting
- Remove redundant local `final HiLoData hiLow = value;`

## CurvePixelPainterPointsLine.java
- Add class-level Javadoc describing point/line pixel painting

## CurveRegular.java
- Add class-level Javadoc explaining regular (analog) curve type

## CurveSignal.java
- Add class-level Javadoc explaining digital signal curve type

## CurveSettings.java
- Add class-level Javadoc explaining per-curve settings

## DataLoader.java
- Add class-level Javadoc explaining data loading, caching, change detection
- Document the caching strategy

## DataBlock.java
- Add class-level Javadoc explaining block-based data storage
- Add Javadoc to `IndexLimit`/`TimeLimit` nested classes

## DataContainerManyTimeSeries.java
- Add class-level Javadoc explaining container for multiple time series

## DefinedExternalSignals.java
- Entire file is commented-out dead code -- remove or add explanation

## DefinedMeanSignals.java
- Add class-level Javadoc explaining user-defined mean signals
- Fix potential bug in `defineNewMeanSignal` sorted insertion (may add twice)
- Replace legacy `java.util.Stack` with `ArrayDeque`

## DialogConnectSignalsGraphs.java
- Add class-level Javadoc explaining signal-to-diagram assignment dialog
- Add English for German methods (`baueGUI`, `aktualisiereGrafer`)
- Document `recalculateWeights` normalization algorithm
- Remove stray semicolon on line 51

## DialogCurveProperties.java
- Add class-level Javadoc explaining per-curve properties dialog

## DialogDefineAvg.java
- Add class-level Javadoc explaining averaging signal definition dialog

## DialogDiagramProps.java
- Add class-level Javadoc explaining diagram properties dialog
- Document the `_initDone` obfuscator workaround

## DialogFourier.java
- Add class-level Javadoc explaining Fourier analysis dialog

## DialogScopeCharacteristics.java
- Add class-level Javadoc explaining scope characteristics measurement dialog

## DialogScopeSettings.java
- Add class-level Javadoc explaining general scope settings

## DiagramCurve.java
- Add class-level Javadoc explaining standard analog curve diagram

## DiagramCurveSignalManager.java
- Add class-level Javadoc explaining diagram/curve/signal lifecycle manager
- Add Javadoc to all management methods

## DiagramSettings.java
- Add class-level Javadoc explaining per-diagram settings
- Document `setWeightDiagram` valid range [0, 1]

## DiagramSignal.java
- Add class-level Javadoc explaining digital signal diagram type
- Document magic number `DEF_MIN_WIDTH = 30`

## DisplayScale.java
- Add class-level Javadoc explaining zoom step mechanism
- Document `MAXSWITCHINDEX = 5`

## ExternalSignal.java
- Add class-level Javadoc explaining external user-defined signals
- Document `Double.POSITIVE_INFINITY`/`NEGATIVE_INFINITY` as default min/max

## FFTLibrary.java
- Add class-level Javadoc explaining FFT library wrapper
- Add Javadoc to all methods

## FourierGUIless.java
- Add class-level Javadoc explaining headless Fourier calculation
- Document windowing function

## GeckoColor.java
- Add class-level Javadoc explaining fixed color palette for curves
- Document thread-safety of static `counter` field (not thread-safe)
- Document cycling behavior when colors exhausted

## GeckoDialog.java
- Add class-level Javadoc explaining base dialog with ESC-to-close

## GeckoGraphics2D.java
- Add class-level Javadoc explaining delegating Graphics2D wrapper (~600+ lines)
- Add Javadoc to each delegating method explaining additional behavior

## GeckoLineType.java
- **Bug**: `getFromOrdinal` iterates over `GeckoLineStyle` values instead of `GeckoLineType` values
- Methods return `GeckoLineStyle` instead of `GeckoLineType` -- investigate type confusion
- Add class-level Javadoc explaining line type enum

## GeckoLineStyle.java
- Add class-level Javadoc explaining line style enum with BasicStroke definitions
- Add Javadoc to each enum constant explaining stroke width and dash pattern

## GeckoSymbol.java
- Add class-level Javadoc explaining symbol enum for data point markers
- Document magic numbers for symbol sizes

## GeneralPathWrapper.java
- Add class-level Javadoc explaining GeneralPath drawing optimization

## GraferTest.java
- Add class-level Javadoc explaining this is a test/demo JFrame
- Consider moving to test source directory

## GraferV4.java
- Add class-level Javadoc (~600+ lines, core class) describing scope graphing component
- Document mouse interaction modes (zoom, pan, cursor)
- Extract magic numbers for pixel offsets, zoom factors into named constants

## HiLoData.java
- Add class-level Javadoc explaining min/max value pair
- Add Javadoc to `hiLoDataFabric` factory method
- Document `CHECKSTYLE:OFF` for public final fields

## HorizontalLevel.java
- Add class-level Javadoc explaining horizontal level marker
- Note: nearly identical to `TriggerPosition.java` -- consider extracting common base

## JLabelRot.java
- Add class-level Javadoc explaining rotated JLabel

## JPanelAxisSettings2.java
- Add class-level Javadoc explaining axis configuration panel UI

## JPanelDialogRange.java
- Add class-level Javadoc explaining time range selection dialog

## JPanelFourier.java
- Add class-level Javadoc explaining Fourier analysis panel UI
- Add `@Deprecated` Javadoc if deprecated

## JPanelGridSettings.java
- Add class-level Javadoc (~500+ lines) explaining grid configuration panel UI

## JPanelLineProperties.java
- Add class-level Javadoc explaining line properties panel

## JPanelSymbProps.java
- Add class-level Javadoc explaining symbol properties panel

## LossCurvePlotPanel.java
- Add class-level Javadoc explaining loss curve plot panel
- Document why private constructor (utility class?)

## LineSettable.java
- Add class-level Javadoc describing line setter interface

## MemoryContainer.java
- Add class-level Javadoc explaining memory management container
- Document threading model (acknowledged thread-safety issues)

## MyFFT.java
- Add class-level Javadoc explaining custom FFT implementation
- Add Javadoc explaining FFT butterfly algorithm
- Remove commented-out code blocks

## NewScope.java
- Add class-level Javadoc (~900+ lines) describing scope panel, cursor logic, signal management
- Add Javadoc to cursor measurement methods
- Document keyboard shortcut bindings

## NiceScale.java
- Add class-level Javadoc explaining "nice number" axis scaling (Heckbert's algorithm)
- Fix typo `ONE_PT_FIFE` -> `ONE_PT_FIVE`
- Document magic constants `SEVEN`, `FIVE`

## NoCurveSelectedException.java
- Add class-level Javadoc explaining when this exception is thrown

## PanelCharacteristicsResult.java
- Add class-level Javadoc explaining results display panel

## PowerAnalysisPanel.java
- Add class-level Javadoc explaining power analysis (P, Q, S, D, cos(phi))
- Add Javadoc to `calculate` method
- Translate German field names
- Fix comment "performance values A and B" (grid has 3 columns: A, B, C)

## PowerAnalysisSettings.java
- Add class-level Javadoc explaining voltage/current indices for phases A/B/C

## PowerCalculator.java
- Add class-level Javadoc explaining power calculation algorithm
- Add Javadoc to each power quantity getter with formula

## ScopeFrame.java
- Add class-level Javadoc (~700+ lines) describing scope JFrame window
- Add Javadoc to menu item handlers and key bindings
- Document save/load workflow

## ScopeSignalMean.java
- Add class-level Javadoc explaining averaged/mean signal
- Document cast safety in constructor (`AbstractScopeSignal` to `ScopeSignalRegular`)

## ScopeSignalRegular.java
- Add class-level Javadoc explaining standard immutable scope signal
- Document immutability contract

## ScopeSettings.java
- Add class-level Javadoc explaining scope-level settings
- Address "biggest bullshit... should be refactored" comment -- refactor or add proper Javadoc

## SignalStateDrawer.java
- Add class-level Javadoc explaining factory pattern for signal state drawing

## SimpleGraferPanel.java
- Add class-level Javadoc explaining simple grafer panel wrapper

## Slider.java
- Add class-level Javadoc explaining slider measurement component

## SliderContainer.java
- Add class-level Javadoc explaining slider container with measurement modes

## SliderUtils.java
- Add class-level Javadoc explaining slider calculation utilities

## SliderValues.java
- Add class-level Javadoc explaining slider measurement value container

## SymbolSettable.java
- Add class-level Javadoc describing symbol setter interface

## TimeSeriesArray.java
- Add class-level Javadoc explaining basic array-backed time series
- Remove or document `static int counter` debug variable

## TimeSeriesConstantDt.java
- Add class-level Javadoc explaining constant-delta-t optimization (O(1) lookups)
- Document magic constants `MAX_DT_CHECK = 1.05` and `ADAPT_THRESHOLD = 100`

## TimeSeriesVariableArray.java
- Add class-level Javadoc explaining variable-array for non-uniform spacing
- Document memory trade-off of `ArrayList<Double>` (boxed doubles)

## TimeSeriesVariableBlock.java
- Add class-level Javadoc explaining block-based variable time series optimization
- Implement or document `getLastTimeInterval()` (throws UnsupportedOperationException)
- Remove large commented-out `main()` test method

## TriggerPosition.java
- Add class-level Javadoc explaining trigger position marker
- Note: nearly identical to `HorizontalLevel.java` -- consider extracting common base

## AxisLinLog.java
- Add Javadoc to enum explaining linear vs logarithmic axis scaling selection
- Translate German enum constants `ACHSE_LIN`/`ACHSE_LOG` to `LINEAR`/`LOGARITHMIC` (or add English aliases)
- Document the magic negative sentinel codes `-111111114`/`-111111115` (used for serialization compatibility)
- Replace `assert false; return null;` in `getFromOrdinal` (line 36) with `throw new IllegalArgumentException` -- asserts can be disabled at runtime
- Remove unnecessary trailing semicolon after enum closing brace (line 48)
- Add Javadoc to `getFromCode()` explaining it defaults to `ACHSE_LIN` for unknown codes

## XSliderValueDrawer.java
- Add class-level Javadoc explaining slider value label drawing on X axis

## ZoomWindow.java
- Add class-level Javadoc explaining zoom window state management
- Document the flag-based approach (consider refactoring to enum)
