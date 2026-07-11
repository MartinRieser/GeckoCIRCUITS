/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.general.AbstractComponentType;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates all circuit component types across the electrical (LK), reluctance (REL),
 * thermal (TH), and motor domains. Each constant maps a legacy type number to its
 * {@link AbstractTypeInfo} descriptor.
 *
 * @author andy
 */
public enum CircuitType implements AbstractComponentType {

    /** Electrical resistor. */
    LK_R(1, ResistorCircuit.TYPE_INFO),
    /** Electrical inductor (without coupling). */
    LK_L(2, InductorWOCoupling.TYPE_INFO),
    /** Electrical capacitor. */
    LK_C(3, CapacitorCircuit.TYPE_INFO),
    /** Voltage source. */
    LK_U(4, VoltageSourceElectric.TYPE_INFO),
    /** Current source. */
    LK_I(5, CurrentSourceCircuit.TYPE_INFO),
    /** Diode. */
    LK_D(6, Diode.TYPE_INFO),
    /** Ideal switch. */
    LK_S(7, IdealSwitch.TYPE_INFO),
    /** Thyristor. */
    LK_THYR(8, Thyristor.TYPE_INFO),
    /** Mutual inductance coupling. */
    LK_M(9, MutualInductance.TYPE_INFO),
    /** Insulated-gate bipolar transistor (IGBT). */
    LK_IGBT(10, IGBT.TYPE_INFO),
    // // when using this inductance, the matrix equation becomes the inductance currents
    // erweitert --> bessere numerische Stabilitaet
    /** Couplable inductor for current-based matrix formulation. */
    LK_LKOP2(12, InductorCoupable.TYPE_INFO),
    /** LISN (Line Impedance Stabilization Network). */
    LK_LISN(13, LISN.TYPE_INFO),
    /** DC motor. */
    LK_MOTOR(14, MotorDC.TYPE_INFO),
    /** Permanent-magnet synchronous motor. */
    LK_MOTOR_PMSM(15, MotorPMSM.TYPE_INFO),
    /** Salient-pole synchronous motor. */
    LK_MOTOR_SMSALIENT(16, MotorSmSalient.TYPE_INFO),
    /** Round-rotor synchronous motor. */
    LK_MOTOR_SMROUND(17, MotorSmRound.TYPE_INFO),
    /** Induction machine with cage rotor (IMA). */
    LK_MOTOR_IMA(18, MotorImCage.TYPE_INFO),
    /** Induction machine (IMC). */
    LK_MOTOR_IMC(20, MotorInductionMachine.TYPE_INFO),
    /** Saturable induction machine. */
    LK_MOTOR_IMSAT(21, MotorImSat.TYPE_INFO),
    /** Operational amplifier. */
    LK_OPV1(22, OperationalAmplifier.TYPE_INFO ),
    /** Ideal transformer. */
    LK_TRANS(23, IdealTransformer.TYPE_INFO),
    /** Reluctance domain resistor. */
    REL_RELUCTANCE(24, ResistorReluctance.TYPE_INFO),
    /** Reluctance domain inductor. */
    REL_INDUCTOR(25, ReluctanceInductor.TYPE_INFO),
    /** Reluctance domain MMF source. */
    REL_MMF(26, VoltageSourceReluctanceMMF.TYPE_INFO),
    // this was moved to SpecialType!!! : SUBCIRCUIT(27),
    /** MOSFET. */
    LK_MOSFET(28, MOSFET.TYPE_INFO),
    /** Electrical terminal. */
    LK_TERMINAL(29, TerminalCircuit.TYPE_INFO ),
    /** Reluctance terminal. */
    REL_TERMINAL(30, RelTerminal.TYPE_INFO),
    /** Electrical global terminal. */
    LK_GLOBAL_TERMINAL(31, CircuitGlobalTerminal.TYPE_INFO),
    /** Reluctance global terminal. */
    REL_GLOBAL_TERMINAL(32, ReluctanceGlobalTerminal.TYPE_INFO),
    /** Bipolar junction transistor (BJT). */
    LK_BJT(33, BJT.TYPE_INFO),
    /** Thermal PV chip. */
    TH_PvCHIP(41, ThermPvChip.TYPE_INFO),
    /** Thermal module. */
    TH_MODUL(42, ThermMODUL.TYPE_INFO),
    //TH_KUEHLER(43, THERMAL),
    /** Heat-flow current source. */
    TH_FLOW(44, HeatFlowCurrentSource.TYPE_INFO),
    /** Thermal temperature source. */
    TH_TEMP(45, VoltageSourceThermalTemperature.TYPE_INFO),
    /** Thermal resistor. */
    TH_RTH(46, ResistorThermal.TYPE_INFO),
    /** Thermal capacitor. */
    TH_CTH(47, CapacitorThermal.TYPE_INFO),
    /** Thermal ambient. */
    TH_AMBIENT(48, ThermAmbient.TYPE_INFO),
    /** Thermal terminal. */
    TH_TERMINAL(49, ThTerminal.TYPE_INFO),
    /** Thermal global terminal. */
    TH_GLOBAL_TERMINAL(50, ThGlobalTerminal.TYPE_INFO),
    /** Permanent-magnet motor. */
    LK_MOTOR_PERM(51, MotorPermanent.TYPE_INFO),
    /** Non-linear reluctance. */
    NONLIN_REL(52, NonLinearReluctance.TYPE_INFO);
    
    private final int _intValue;
    private final AbstractTypeInfo _tInfo;

    CircuitType(final int initValue, final AbstractTypeInfo typeInfo) {
        _intValue = initValue;
        _tInfo = typeInfo;
        _tInfo.addParentEnum(this);
    }

    @Override
    public int getTypeNumber() {
        return _intValue;
    }
    private static Map<Integer, CircuitType> _backwardMap;

    public static CircuitType getFromIntNumber(final int intNumber) {

        if (_backwardMap == null) {
            _backwardMap = new HashMap<Integer, CircuitType>();
            for (CircuitType typ : values()) {
                _backwardMap.put(typ._intValue, typ);
            }
        }

        if (_backwardMap.containsKey(intNumber)) {
            return _backwardMap.get(intNumber);
        }
        throw new IllegalArgumentException("Type with identifier: " + intNumber + " is not known!");
    }

    @Override
    public AbstractTypeInfo getTypeInfo() {
        return _tInfo;
    }        
}
