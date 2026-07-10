# Improvement Tasks: ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/ (138 files)

## AbstractCapacitor.java
- Add Javadoc on class explaining nonlinear capacitance support and parameter[] array layout
- Document `returnValue` always being `false` in `updateNonlinearCapacitances()` or remove
- Fix typo `getInitalNonlinValues` -> `getInitialNonlinValues`
- Extract magic indices (2,3,4,5,7,8,9,10) into named constants

## AbstractCircuitBlockInterface.java
- Add Javadoc on class explaining base class for all schematic circuit elements
- Fix `return 00;` in `istAngeklickt()` -> `return 0;`
- Add Javadoc on abstract methods `drawForeground()` and `drawConnectorLines()`

## AbstractCircuitGlobalTerminal.java
- Add Javadoc on class explaining "global terminal" concept
- Add Javadoc on `fabric()` explaining reflective instantiation

## AbstractCircuitSource.java
- Add Javadoc on class explaining source-type pattern
- Document parameter[] array layout (indices 0-20)
- Fix typo "non-accessibe" -> "non-accessible"
- Add Javadoc on abstract methods

## AbstractCircuitSourceDialog.java
- Add Javadoc on class explaining tabbed dialog structure
- Add Javadoc on `baueGUIIndividual()` and `processInputIndividual()`

## AbstractCircuitTerminal.java
- Add Javadoc on class and `fabric()` method

## AbstractCircuitTypeInfo.java
- Add Javadoc on class and `fabric()` method

## AbstractCurrentSource.java
- Add Javadoc on class, document drawing constants
- Add Javadoc on `getCircuitCalculatorsForSimulationStart()`

## AbstractDialogPowerSwitch.java
- Add Javadoc on class, `createParameterPanel()`, `baueGUIIndividual()`

## AbstractInductor.java
- Add Javadoc on class explaining nonlinear inductance
- Document parameter[] array layout
- Add Javadoc on `getStartInductance()`, `doCalculation()`

## AbstractMotor.java
- Add Javadoc on class explaining motor simulation pattern (hidden subcircuit + mechanical equations)
- Document parameter[] array layout shared across subclasses
- Rename `_drehzahl` to `_rotationalSpeed` or add inline comment
- Add Javadoc on `doCalculation()`, `calculateMechanicalParameters()`, `fabricHiddenSub()`

## AbstractMotorDC.java
- Add Javadoc on class explaining DC motor subcircuit topology
- Add inline comment on `_anchorCurrent` ("Ankerstrom" = armature current)
- Add Javadoc on `calculateMotorEquations()`, `setSubCircuit()`, `updateSourceParameters()`

## AbstractMotorDialog.java
- Add Javadoc on class, `baueGUIIndividual()`, `createLossButton()`

## AbstractMotorIM.java
- Add Javadoc on class explaining induction machine model (dq-axis flux model)

## AbstractMotorIMCommon.java
- Add Javadoc on class explaining shared IM parameters
- Add Javadoc on abstract methods

## AbstractMotorSM.java
- Add Javadoc on class explaining synchronous machine model

## AbstractNonLinearCircuitComponent.java
- Expand the TODO at line 43 with specifics
- Add Javadoc on interpolation methods `getActualValueLINFromLinearizedCharacteristic()`, `getActualValueLOGFromLinearizedCharacteristic()`
- Fix typo "Impromer" -> "Improper" in error message
- Fix typo "picewise" -> "piecewise" in comments
- Remove debug commented-out code block

## AbstractResistor.java
- Add Javadoc on class explaining multi-domain resistance pattern
- Add Javadoc on `getLossCalculation()`

## AbstractSemiconductor.java
- Add Javadoc on class explaining semiconductor base
- Document parameter[] indices (2,3 = R_ON/R_OFF, etc.) as named constants
- Add Javadoc on `getFiles()`, `addFiles()`, `getOperationEnumInterfaces()`

## AbstractSwitch.java
- Add Javadoc on class, `_connectedGateBlock` field
- Add Javadoc on `doReferenceAddAction()`, `doReferenceRemoveAction()`, `addGateTextInfo()`

## AbstractSwitchCalculator.java
- Add Javadoc on class explaining variable-resistance switch stamping
- Document static fields `switchAction` and `switchActionOccurred` (global error-correction)
- Remove commented-out `System.out.println`

## AbstractThreePhaseMotor.java
- Add Javadoc on class explaining three-phase terminal layout

## AbstractTwoPortPowerCircuitBlock.java
- Add Javadoc on class explaining standard two-port topology
- Add Javadoc on `createTwoPortTerminals()`

## AbstractVoltageDropSwitch.java
- Add Javadoc on class and `getForwardVoltageDropParameter()`

## AbstractVoltageSource.java
- Add Javadoc on class
- Add Javadoc on `drawPlusSymbol()`, `drawMinusSymbol()`, `getDCValueShortNameFromDomain()`

## AbstractVoltageSourceCalculator.java
- Add Javadoc on class explaining MNA voltage source stamping with `_z` auxiliary variable
- Translate German comment on line 51 to English
- Add Javadoc on `stampMatrixA()`

## AbstractVoltageSourceControlledCalculator.java
- Add Javadoc on class explaining controlled voltage sources
- Add Javadoc on `setGain()`, `setCurrentControlComponent()`
- Add missing license/file header comment

## AStampable.java
- Add Javadoc on interface explaining A-matrix stamping contract
- Add Javadoc on `stampMatrixA()` and `isBasisStampable()`

## BJT.java
- Add Javadoc on class explaining BJT model

## BJTDialog.java
- Add Javadoc on class

## BStampable.java
- Add Javadoc on interface explaining B-vector stamping contract
- Add Javadoc on all methods

## BVector.java
- Add Javadoc on class explaining optimization: caching basis-stampable components
- Add Javadoc on `stampBVector()`, `setUpdateAllFlag()`, `copy()`, `registerBVector()`
- Replace `@author andy` with real class-level description

## CapacitanceCharacteristic.java
- Add Javadoc on class explaining nonlinear capacitance interpolation
- Add Javadoc on `getCapacitanceAtV()`

## CapacitorCalculator.java
- Add Javadoc on class explaining capacitor MNA stamping and three solver types
- Add Javadoc on `stampMatrixA()`, `stampVectorB()`
- Document static fields `initCapacitor` and `capError`
- Remove debug `System.out.println("setting z Value: + " + z)`
- Remove large commented-out code blocks
- Add Javadoc on `updateNonLinearCapacitance()` explaining 10% deviation threshold

## CapacitorCircuit.java
- Add Javadoc on class (leaf class -- TYPE_INFO registration)

## CapacitorDialog.java
- Add Javadoc on class

## CapacitorThermal.java
- Add Javadoc on class explaining thermal capacitance (heat capacity)

## CircuitComponent.java
- Add Javadoc on class explaining simulation-time calculator base
- Document `var_history` array layout (indices 0-8)
- Fix typo `terminalNUmber` -> `terminalNumber`
- Document static `disturbanceValue` field

## CircuitGlobalTerminal.java
- Add Javadoc on class

## CircuitType.java
- Add Javadoc on enum explaining each circuit type constant

## CoupledInductorsGroup.java
- Add Javadoc on class explaining coupled inductor matrix assembly and Cholesky inversion
- Add Javadoc on `stampMatrixA()`, `calculateCurrent()`, `choleskyInverse()`

## CurrentCalculatable.java
- Add Javadoc on interface explaining post-solve current calculation contract

## CurrentSourceCalculator.java
- Add Javadoc on class explaining current source stamping (B-vector only)
- Add Javadoc on `stampVectorB()` explaining source type switch

## CurrentSourceCircuit.java
- Add Javadoc on class (leaf class)

## DialogElementLK.java
- Fix typo "packagse" -> "package" in Javadoc
- Add Javadoc on `baueGUIIndividual()`, `createControlLabelCombo()`

## DialogSubCktSettings.java
- Add Javadoc on class

## DialogViewPowerModule.java
- Add Javadoc on class

## Diode.java
- Add Javadoc on class explaining diode as self-commutated voltage-drop switch

## DiodeCalculator.java
- Add Javadoc on class explaining iterative diode on/off resistance switching
- Document static fields `diodeSwitchError`, `inSwitchErrorMode`, `diodeErrorOccurred`
- Remove commented-out code blocks and `System.out.println` calls
- Document the NaN check `assert aValue == aValue` in `stampMatrixA()`

## DiodeCharacteristic.java
- Add Javadoc on class

## DiodeDialog.java
- Add Javadoc on class

## DiodeSegment.java
- Add Javadoc on class explaining segmented diode loss characteristic

## DirectCurrentCalculatable.java
- Add Javadoc on interface explaining `getZValue()`/`setZValue()` for MNA auxiliary variables

## ForwardVoltageDropable.java
- Add Javadoc on interface

## HeatFlowCurrentSource.java
- Add Javadoc on class (leaf class -- thermal heat flow source)

## HistoryUpdatable.java
- Add Javadoc on interface explaining history/rollback mechanism

## IdealSwitch.java
- Add Javadoc on class explaining ideal switch (no voltage drop)

## IdealSwitchCalculator.java
- Add Javadoc on class explaining gate-only response (no self-commutation)

## IdealSwitchDialog.java
- Add Javadoc on class

## IdealTransformer.java
- Add Javadoc on class explaining ideal transformer model

## IdealTransformerDialog.java
- Add Javadoc on class

## IGBT.java
- Add Javadoc on class explaining IGBT as gate-controlled switch with forward voltage drop

## IGBTCalculator.java
- Add Javadoc on class explaining gate-gated on/off logic
- Remove commented-out debug lines

## IGBTDialog.java
- Add Javadoc on class

## InductorCalculator.java
- Add Javadoc on class explaining inductor stamping (B-vector only)
- Add Javadoc on `stampVectorB()` -- solver-specific formulas
- Document `stampVectorBTRZ()` -- "UGLY, just a temporary solution" comment
- Document `FAST_NULL_L` floor in `setInductance()`

## InductorCoupable.java
- Add Javadoc on interface

## InductorCouplingCalculator.java
- Add Javadoc on class explaining coupled inductor current update
- Add Javadoc on `addNewCurrent()`, `stampVectorBTRZ()`, `setGroup()`

## InductorDialog.java
- Add Javadoc on class

## InductorWOCoupling.java
- Add Javadoc on class explaining this variant excludes itself from mutual coupling

## JPanelSemiconductorDetailButtons.java
- Add Javadoc on class explaining loss detail button panel

## LISN.java
- Add Javadoc on class explaining LISN for EMI analysis

## LISNDialog.java
- Add Javadoc on class

## MOSFET.java
- Add Javadoc on class explaining MOSFET as bidirectional gate-controlled switch

## MOSFETDialog.java
- Add Javadoc on class

## MotorDC.java
- Add Javadoc on class (leaf class)

## MotorDCDialog.java
- Add Javadoc on class

## MotorImCage.java
- Add Javadoc on class explaining squirrel-cage induction machine
- Add Javadoc on `calculateElectricTorque()`, `calculateMotorEquations()`

## MotorImCageDialog.java
- Add Javadoc on class

## MotorImSat.java
- Add Javadoc on class explaining saturated induction machine

## MotorImSatDialog.java
- Add Javadoc on class

## MotorInductionMachine.java
- Add Javadoc on class (leaf class)

## MotorInductionMachineDialog.java
- Add Javadoc on class

## MotorPermanent.java
- Add Javadoc on class explaining permanent magnet motor

## MotorPermanentDialog.java
- Add Javadoc on class

## MotorPMSM.java
- Add Javadoc on class explaining PMSM

## MotorPMSMDialog.java
- Add Javadoc on class

## MotorSmRound.java
- Add Javadoc on class explaining round-rotor synchronous machine

## MotorSmRoundDialog.java
- Add Javadoc on class

## MotorSmSalient.java
- Add Javadoc on class explaining salient-pole synchronous machine

## MotorSmSalientDialog.java
- Add Javadoc on class

## MutualCouplingCalculator.java
- Add Javadoc on class explaining mutual inductance stamping
- Add Javadoc on `stampInductanceMatrix()`

## MutualInductance.java
- Add Javadoc on class explaining coupling component

## MutualInductanceDialog.java
- Add Javadoc on class

## Nonlinearable.java
- Add Javadoc on interface explaining nonlinear characteristic contract

## NonLinearReluctance.java
- Add Javadoc on class explaining nonlinear reluctance (magnetic saturation)

## NonlinearReluctanceDialog.java
- Add Javadoc on class

## OperationalAmplifier.java
- Add Javadoc on class explaining ideal op-amp model

## OperationalAmplifierDialog.java
- Add Javadoc on class

## PostProcessable.java
- Add Javadoc on interface explaining post-solve calculation contract

## PowerModulePainter.java
- Add Javadoc on class explaining thermal power module rendering
- Rename `zeichne()` or add comment ("zeichne" = "draw")

## RelTerminal.java
- Add Javadoc on class (leaf class -- reluctance terminal)

## ReluctanceAndCircuitTypeInfo.java
- Add Javadoc on class explaining dual-domain type info

## ReluctanceComponent.java
- Add Javadoc on interface explaining reluctance-domain contract

## ReluctanceGlobalTerminal.java
- Add Javadoc on class (leaf class)

## ReluctanceInductor.java
- Add Javadoc on class explaining reluctance inductor (permeance analogy)

## ReluctanceInductorDialog.java
- Add Javadoc on class

## ReluctanceTypeInfo.java
- Add Javadoc on class explaining reluctance-domain type info

## ResistorCalculator.java
- Add Javadoc on class explaining simplest A-matrix stamping (1/R conductance)
- Add Javadoc on `setResistance()` -- note: check tests OLD value, should test parameter
- Add Javadoc on `updateHistory()`, `toString()`

## ResistorCircuit.java
- Add Javadoc on class (leaf class)

## ResistorDialog.java
- Add Javadoc on class

## ResistorReluctance.java
- Add Javadoc on class (leaf class -- magnetic reluctance)

## ResistorThermal.java
- Add Javadoc on class (leaf class -- thermal resistance)

## SemiconductorLossCalculatable.java
- Add Javadoc on interface

## SourceType.java
- Add Javadoc on enum

## SpecialTypeInfo.java
- Add Javadoc on class explaining special (non-circuit) type info

## SubcircuitBlock.java
- Add Javadoc on class explaining subcircuit container
- Fix `if(1>0) return true;` in `areTerminalPositionsOK()` -- implement properly or explain
- Fix typo "enshure" -> "ensure"
- Add Javadoc on `copyFabric()`, `paintIndividualComponent()`, `getColorForTerminal()`
- Remove `System.err.println` or convert to proper logging

## SwitchState.java
- Add Javadoc on class explaining switch state snapshot
- Add Javadoc on inner `State` enum

## TerminalCircuit.java
- Add Javadoc on class (leaf class)

## TerminalCircuitDialog.java
- Add Javadoc on class

## TextInfoType.java
- Add Javadoc on enum explaining display modes

## ThermalTypeInfo.java
- Add Javadoc on class explaining thermal-domain type info

## ThermAmbient.java
- Add Javadoc on class explaining reference temperature element
- Clean up mixed German/English comments
- Document `THERMAL_ZERO` sentinel value (-4711, -4711)

## ThermAmbientDialog.java
- Add Javadoc on class explaining read-only temperature dialog

## ThermMODUL.java
- Add Javadoc on class explaining thermal power module
- Translate German field names (`_xBef`, `_yBef`, `getChipAnzahl`, `setDateiname`)
- Remove commented-out test labels ("bli", "bla", "blub")

## ThermPvChip.java
- Add Javadoc on class explaining heat source coupled to semiconductor
- Fix typo `getSwitchngLosses()` -> `getSwitchingLosses()`
- Add Javadoc on `doCalculation()`, `doInitialization()`, `setzeSubcircuit()`

## ThermPvChipDialog.java
- Add Javadoc on class

## ThGlobalTerminal.java
- Add Javadoc on class (leaf class)

## ThTerminal.java
- Add Javadoc on class (leaf class)

## Thyristor.java
- Translate German class comment to English
- Add Javadoc on `drawGateSymbol()`, `drawBackground()`, `drawForeground()`

## ThyristorCalculator.java
- Add Javadoc on class explaining thyristor behavior (gate-triggered on, current-zero turn-off)
- Add Javadoc on `calculateCurrent()`, `setGateSignal()` override, `setTRR()`
- Document `REVERSE_FACTOR = 3.0` constant

## ThyristorDialog.java
- Add Javadoc on class

## VoltageSourceCalculator.java
- Add Javadoc on class explaining function-driven voltage source
- Add Javadoc on `stampVectorB()`, `stepBack()` override
- Remove commented-out debug lines
- Rename `THREE` and `FOUR` constants to `HISTORY_CURRENT_INDEX` and `HISTORY_VOLTAGE_INDEX`

## VoltageSourceCurrentControlledCalculator.java
- Add Javadoc on class explaining CCVS stamping

## VoltageSourceDCMachineCalculator.java
- Translate German variable names (`phi`, `emk`, `drehzahl`, `omegaALT`, `momentElektr`)
- Add Javadoc on class explaining DC machine EMF source
- Add Javadoc on `doPostProcess()` -- mechanical equation solver
- Remove commented-out debug

## VoltageSourceDIDTControlledCalculator.java
- Add Javadoc on class explaining di/dt-controlled voltage source
- Clean up comment "nothing todo???"

## VoltageSourceElectric.java
- Add Javadoc on class (leaf class)

## VoltageSourceReluctanceMMF.java
- Add Javadoc on class (leaf class)
- Add inline comment explaining "MMF" = Magnetomotive Force

## VoltageSourceThermalTemperature.java
- Add Javadoc on class (leaf class)
