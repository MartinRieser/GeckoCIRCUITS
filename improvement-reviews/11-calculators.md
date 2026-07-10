# Improvement Tasks: ch/technokrat/gecko/geckocircuits/control/calculators/ (73 files)

## AbstractControlCalculatable.java
- Add Javadoc on `SIGNAL_THRESHOLD` (what is 0.5 threshold)
- Add Javadoc on `_time` static field (global simulation time, thread-safety concern)
- Add Javadoc on `_inputSignal` and `_outputSignal` explaining `double[][]` structure
- Add `@param` Javadoc on constructor
- Add Javadoc on `calculateYOUT(double deltaT)`, `setInputSignal()`, `createOutputSignal(int)`
- Document implications of `setTime` mutating a public static field

## AbstractSignalCalculator.java
- Add class-level Javadoc explaining signal-source calculator base
- Add Javadoc on `TWO_PI` constant

## AbstractSignalCalculatorPeriodic.java
- Add class-level Javadoc explaining periodic signal generation
- Add Javadoc on all protected fields (German names `_aufsteigend`, `_anteilDC` need English docs)
- Add Javadoc on `initializeAtSimulationStart` explaining THOUSAND-step discretization
- Add Javadoc on `calculatePhaseX()` explaining phase normalization

## AbstractSingleInputSingleOutputCalculator.java
- Add class-level Javadoc explaining 1-input/1-output calculator base
- Document or remove unused `_inputSignalValue` and `_outputSignalValue` fields

## AbstractTwoInputsOneOutputCalculator.java
- Add class-level Javadoc explaining 2-input/1-output calculator base

## AbstractPTCalculator.java
- Add class-level Javadoc explaining "PT" = proportional-integral time-delay element
- Add Javadoc on `_TVal` and `_a1Val` fields (T = time constant, a1 = gain)
- Remove stale IDE template comment

## InitializableAtSimulationStart.java
- Add Javadoc on interface explaining simulation-start initialization
- Add `@param deltaT` Javadoc on `initializeAtSimulationStart`

## GateCalculator.java
- Add class-level Javadoc explaining Gate terminal in circuit context

## AbsCalculator.java
- Add class-level Javadoc: "Calculates the absolute value"
- Remove trailing semicolon after class closing brace

## ACosCalculator.java
- Add class-level Javadoc: "Calculates arc cosine"
- Fix contradictory assert messages ("must be <= -PI/2" for upper bound check)

## AndMultiInputCalculator.java
- Add `@param inputNumber` Javadoc on constructor
- Add Javadoc on `calculateYOUT` explaining multi-input AND logic

## AndTwoPortCalculator.java
- Add Javadoc on `calculateYOUT` (note: "TwoPort" is really "TwoInput")

## ASinCalculator.java
- Add class-level Javadoc: "Calculates arc sine"
- Fix contradictory assert messages

## ATanCalculator.java
- Add class-level Javadoc: "Calculates arc tangent"

## ConstantCalculator.java
- Add class-level Javadoc explaining constant value output
- Add `@param constValue` Javadoc on constructor

## CosCalculator.java
- No improvements needed (already has good Javadoc)

## CounterCalculatable.java
- Translate German comments to English
- Add Javadoc on `_lastValue` field and rising-edge detection logic

## DelayCalculator.java
- Add class-level Javadoc explaining discrete time delay buffer
- Add Javadoc on all private fields (German names `_youtVerzoegert`, `_zeigerYOUT`, etc.)
- Add Javadoc on `initWithNewDt()` explaining time-step-change resampling

## DEMUXCalculator.java
- Add class-level Javadoc explaining demultiplexer (vector to scalar split)
- Add Javadoc on `initializeAtSimulationStart` explaining Java block validation

## DivCalculator.java
- Add Javadoc on `calculateYOUT` explaining NaN (0/0) and Infinity (x/0) handling
- Document `LARGE_NUMBER` constant

## DQABCDCalculator.java
- Add class-level Javadoc explaining DQ-to-ABC (Park inverse) transformation
- Document constants `TWO_THIRD` and `TWO_THIRD`
- Document input indices: [0]=d, [1]=q, [2]=theta
- Fix inconsistent capitalization: `qVAl` -> `qVal`

## EqualCalculatorMultiInput.java
- Add class-level Javadoc: "Checks equality across all N inputs"
- Fix parameter typo "intputSize" -> "inputSize"

## EqualCalculatorTwoInputs.java
- Add class-level Javadoc: "Checks equality of two inputs"
- Note: floating-point exact `==` comparison may be unreliable

## ExpCalculator.java
- Add class-level Javadoc: "Calculates e^x"
- Document why limit is 100 (overflow prevention)

## GainCalculator.java
- Add class-level Javadoc: "Multiplies input by constant gain"
- Add `@param gain` Javadoc on constructor

## GreaterEqualCalculator.java
- Add class-level Javadoc: "Outputs 1 if input[0] >= input[1]"

## GreaterThanCalculator.java
- Add class-level Javadoc: "Outputs 1 if input[0] > input[1]"

## HysteresisCalculatorExternal.java
- Add class-level Javadoc explaining hysteresis with external band input
- Document three-state output (1, -1, hold) logic
- Document potential bug: only checks positive hValue boundary

## HysteresisCalculatorInternal.java
- Add class-level Javadoc explaining hysteresis with internal band
- Add `@param hValue` Javadoc on constructor
- Same potential bug: only checks positive boundary

## SignalCalculatorSinus.java
- Add class-level Javadoc: "Generates A*sin(2*pi*f*t - phase) + DC offset"

## LimitCalculatorExternal.java
- Add class-level Javadoc explaining external min/max limiting
- Document input indices: [0]=signal, [1]=min, [2]=max

## LimitCalculatorInternal.java
- Add class-level Javadoc explaining internal min/max limiting
- Add `@param` Javadoc on constructor for `minLimit` and `maxLimit`

## LnCalculator.java
- Add class-level Javadoc: "Calculates natural logarithm"
- Document input domain assertion (> 0)

## MaxCalculatorMultiInputs.java
- Add class-level Javadoc: "Outputs maximum across all N inputs"

## MaxCalculatorTwoInputs.java
- Add class-level Javadoc: "Outputs maximum of two inputs"

## MinCalculatorMultiInputs.java
- Add class-level Javadoc: "Outputs minimum across all N inputs"

## MinCalculatorTwoInputs.java
- Add class-level Javadoc: "Outputs minimum of two inputs"

## MUXControlCalculatable.java
- Add class-level Javadoc explaining multiplexer (N scalar to 1 vector)
- Add `@param noInputs` Javadoc on constructor

## NotCalculator.java
- Add class-level Javadoc: "Logical NOT -- inverts signal based on SIGNAL_THRESHOLD"
- Translate German comment "Logik-Schwellwert --> 0.5" to English

## NotEqualCalculator.java
- Add class-level Javadoc: "Outputs 1 if inputs are not equal"

## NothingToDoCalculator.java
- Add class-level Javadoc explaining pass-through/no-op calculator
- Remove stale IDE template comment

## OrCalculatorMultipleInputs.java
- Add class-level Javadoc: "Logical OR across all N inputs"

## OrCalculatorTwoInputs.java
- Add class-level Javadoc: "Logical OR of two inputs"

## PDCalculator.java
- Add class-level Javadoc explaining PD controller
- Add Javadoc on numerical differentiation formula

## PICalculator.java
- Add class-level Javadoc explaining PI controller using trapezoidal integration
- Add Javadoc on all fields (German names `y1alt`, `xalt`, `y11` need English docs)
- Fix field naming inconsistency (some use `_` prefix, some don't)

## PT1Calculator.java
- Add class-level Javadoc explaining PT1: G(s) = K/(1+sT)
- Translate German comments to English
- Fix incorrect German comment "Speicherung des I-Anteils" (should be "storage of previous output")

## PT2Calculator.java
- Add class-level Javadoc explaining PT2: G(s) = K/(1+(sT)^2)
- Remove commented-out dead code

## RoundCalculator.java
- Add class-level Javadoc: "Rounds to nearest integer using Math.round()"

## SampleHoldCalculator.java
- Add class-level Javadoc explaining sample-and-hold (samples on clock high)
- Document input indices: [0]=signal, [1]=clock

## SignalCalculatorExternalWrapper.java
- Add class-level Javadoc explaining wrapper exposing parameters as runtime inputs
- Add Javadoc on all index constants
- Fix inconsistency: line 46 uses `_inputSignal[1][0]` instead of `_inputSignal[FREQUENCY_INDEX][0]`

## SignalCalculatorImport.java
- Add class-level Javadoc explaining imported data table interpolation
- Add `@param dataTable` Javadoc explaining format
- Add Javadoc on linear interpolation logic
- Translate German comments to English

## SignalCalculatorRandom.java
- Add class-level Javadoc: "Generates random walk signal"

## SignalCalculatorRectangle.java
- Add class-level Javadoc: "Generates rectangular wave with duty cycle"
- Fix potential bug: hardcoded `0.5` on lines 63-64 instead of `_dutyRatio`

## SignalCalculatorTriangle.java
- Add class-level Javadoc: "Generates triangular wave with duty cycle"
- Remove commented-out dead code

## SignumCalculator.java
- Add class-level Javadoc: "Calculates signum: -1, 0, or 1"

## SinCalculator.java
- Add class-level Javadoc: "Calculates sine"

## SlidingDFTCalculator.java
- Add class-level Javadoc explaining Sliding Discrete Fourier Transform
- Add Javadoc on all fields
- Add Javadoc on `doSlidingFourierStep()` explaining recursive DFT update formula
- Fix potential bug: line 75 uses `_outputSignal[0][0]` instead of `_outputSignal[i][0]`

## SmallSignalCalculator.java
- Add class-level Javadoc explaining Small Signal Analysis via multi-sine excitation and FFT
- Add Javadoc on `tearDownOnPause()` explaining FFT and Bode calculation
- Remove dead/debug code: `System.out.println("xxx ...")`, `printResults()` methods
- Remove hardcoded file path `/home/andy/data.txt`
- Document static `_bode` field (shared across instances)

## SparseMatrixCalculator.java
- Add class-level Javadoc explaining sparse matrix converter modulation algorithm
- Translate all German field names to English
- Document magic numbers `LG = 1000` and `d = 12`
- Document massive code duplication in switch statements for sectors 1-12

## SpaceVectorCalculator.java
- Add class-level Javadoc explaining 9-input display feeding
- Document `NO_INPUTS = 9` (what each of the 9 inputs represents)

## SquareCalculator.java
- Add class-level Javadoc: "Calculates x^2"

## SqrtCalculator.java
- Add class-level Javadoc: "Calculates square root"

## SubtractionMoreParameter.java
- Add class-level Javadoc: "Subtracts all subsequent inputs from first"

## SubtractionTwoParameter.java
- Add class-level Javadoc: "Subtracts second input from first"

## TanCalculator.java
- Add class-level Javadoc: "Calculates tangent"
- Document `SMALL_NUMBER` constant and PI/2 singularity assertions

## ThyristorControlCalculator.java
- Add class-level Javadoc explaining 6-pulse thyristor converter gate signal generation
- Add Javadoc on all fields and `GateEvent` inner class
- Document constants `TN_X`, `TN_Y`, `THREE`, `THREE_HALF`
- Document dead code: condition `-1 > _lastFallingZero` is always false

## TimeCalculator.java
- Add class-level Javadoc: "Outputs current simulation time"

## XORCalculator.java
- Add class-level Javadoc: "Logical XOR of two inputs"

## ABCDQCalculator.java
- Add class-level Javadoc explaining ABC-to-DQ (Park) transformation
- Document constants and input indices: [0]=a, [1]=b, [2]=c, [3]=theta
- Fix `qVAl` -> `qVal`

## IntegratorCalculation.java
- Add class-level Javadoc explaining numerical integrator with trapezoidal rule
- Add Javadoc on all fields and two modes (normal vs reset)
- Translate German comments to English
- Document `_xoldInit` and `_yoldInit` (never assigned -- possible dead code)

## ViewMotorCalculator.java
- Add class-level Javadoc explaining display-only calculator

## PmsmControlCalculator.java
- Add class-level Javadoc explaining PMSM field-oriented control
- Add Javadoc on all 14+ fields (none documented)
- Document all 12 input signals and 8 output signals
- Document magic number `999e-3` and `60` (RPM to rad/s conversion)
- Remove possible dead fields (`psi_sa_last`, `psi_sb_last`, etc.)

## PmsmModulatorCalculator.java
- Add class-level Javadoc explaining Space Vector PWM for 2-level 3-phase inverter
- Document all intermediate variables
- Document sector detection using if-statements instead of else-if
