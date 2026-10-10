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
package gecko.core.control;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.AddCalculator;
import gecko.core.control.calculators.DeadTimeCalculator;
import gecko.core.control.calculators.DelayCalculator;
import gecko.core.control.calculators.GainCalculator;
import gecko.core.control.calculators.GreaterThanCalculator;
import gecko.core.control.calculators.IntegratorCalculation;
import gecko.core.control.calculators.MulCalculator;
import gecko.core.control.calculators.NotCalculator;
import gecko.core.control.calculators.SubtractionTwoParameter;
import gecko.core.control.calculators.PICalculator;
import gecko.core.control.calculators.PT1Calculator;
import gecko.core.control.calculators.SignalSelectorCalculator;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the web catalog control blocks (CircuitTypCore CTRL_* range,
 * typ 1000..1015): the headless engine must execute them exactly like their
 * classic 1..6 counterparts, with the parameter slot layouts of the web
 * editor's component schema.
 *
 * <p>Terminal geometry contract: inputs at rel (-2, -i), outputs at
 * (x + 2, -j) for NORTH_SOUTH orientation, so the CONTROL wires in the test
 * .ipes snippets connect the exact terminal points the engine computes.</p>
 */
class NativeControlBlockTest {

    private static final double DT = 1e-6;

    /** Enum sync: the builder switch uses literal case labels. */
    @Test
    void webCatalogTypeNumbers_matchCircuitTypCore() {
        assertEquals(1000, CircuitTypCore.CTRL_GATE.getTypeNumber());
        assertEquals(1001, CircuitTypCore.CTRL_VOLT.getTypeNumber());
        assertEquals(1002, CircuitTypCore.CTRL_AMP.getTypeNumber());
        assertEquals(1003, CircuitTypCore.CTRL_SCOPE.getTypeNumber());
        assertEquals(1004, CircuitTypCore.CTRL_SIGNAL.getTypeNumber());
        assertEquals(1005, CircuitTypCore.CTRL_CONSTANT.getTypeNumber());
        assertEquals(1006, CircuitTypCore.CTRL_GAIN.getTypeNumber());
        assertEquals(1007, CircuitTypCore.CTRL_PI.getTypeNumber());
        assertEquals(1008, CircuitTypCore.CTRL_PT1.getTypeNumber());
        assertEquals(1009, CircuitTypCore.CTRL_INTEGRATOR.getTypeNumber());
        assertEquals(1010, CircuitTypCore.CTRL_COMPARATOR.getTypeNumber());
        assertEquals(1011, CircuitTypCore.CTRL_AND.getTypeNumber());
        assertEquals(1012, CircuitTypCore.CTRL_OR.getTypeNumber());
        assertEquals(1013, CircuitTypCore.CTRL_NOT.getTypeNumber());
        assertEquals(1014, CircuitTypCore.CTRL_MUX.getTypeNumber());
        assertEquals(1015, CircuitTypCore.CTRL_DELAY.getTypeNumber());
        assertEquals(1017, CircuitTypCore.CTRL_SUB.getTypeNumber());
        assertEquals(1018, CircuitTypCore.CTRL_ADD.getTypeNumber());
        assertEquals(1019, CircuitTypCore.CTRL_MUL.getTypeNumber());
        assertEquals(1020, CircuitTypCore.CTRL_DIV.getTypeNumber());
        assertEquals(1021, CircuitTypCore.CTRL_LIMIT.getTypeNumber());
        assertEquals(1022, CircuitTypCore.CTRL_ABS.getTypeNumber());
        assertEquals(1023, CircuitTypCore.CTRL_SQRT.getTypeNumber());
        assertEquals(1024, CircuitTypCore.CTRL_EXP.getTypeNumber());
        assertEquals(1025, CircuitTypCore.CTRL_LN.getTypeNumber());
        assertEquals(1026, CircuitTypCore.CTRL_SIN.getTypeNumber());
        assertEquals(1027, CircuitTypCore.CTRL_COS.getTypeNumber());
        assertEquals(1028, CircuitTypCore.CTRL_MIN.getTypeNumber());
        assertEquals(1029, CircuitTypCore.CTRL_MAX.getTypeNumber());
        assertEquals(1030, CircuitTypCore.CTRL_HYS.getTypeNumber());
        assertEquals(1031, CircuitTypCore.CTRL_PT2.getTypeNumber());
        assertEquals(1032, CircuitTypCore.CTRL_PD.getTypeNumber());
        assertEquals(1033, CircuitTypCore.CTRL_SAMPLEHOLD.getTypeNumber());
        assertEquals(1034, CircuitTypCore.CTRL_TIME.getTypeNumber());
        assertEquals(1035, CircuitTypCore.CTRL_XOR.getTypeNumber());
        assertEquals(1036, CircuitTypCore.CTRL_GE.getTypeNumber());
        assertEquals(1037, CircuitTypCore.CTRL_DEADTIME.getTypeNumber());
    }

    @Test
    void addCalculator_computesSum() {
        AddCalculator add = new AddCalculator();
        add._inputSignal[0] = new double[]{3.5};
        add._inputSignal[1] = new double[]{2.5};
        add.calculateYOUT(1e-6);
        assertEquals(6.0, add._outputSignal[0][0], 1e-9);
    }

    @Test
    void mulCalculator_computesProduct() {
        MulCalculator mul = new MulCalculator();
        mul._inputSignal[0] = new double[]{3.0};
        mul._inputSignal[1] = new double[]{4.0};
        mul.calculateYOUT(1e-6);
        assertEquals(12.0, mul._outputSignal[0][0], 1e-9);
    }

    @Test
    void subCalculator_computesDifference() {
        SubtractionTwoParameter sub = new SubtractionTwoParameter();
        sub._inputSignal[0] = new double[]{10.0};
        sub._inputSignal[1] = new double[]{8.5};
        sub.calculateYOUT(1e-6);
        assertEquals(1.5, sub._outputSignal[0][0], 1e-9);
    }

    @Test
    void deadTimeCalculator_insertsDeadBandBetweenHighAndLowOutputs() {
        double tDead = 10e-6;
        double dt = 1e-6;
        DeadTimeCalculator dtCalc = new DeadTimeCalculator(tDead);
        double[] pwmSignal = new double[]{0.0};
        dtCalc._inputSignal[0] = pwmSignal;

        // Initially PWM = 0: G_hi = 0, G_lo = 1
        dtCalc.calculateYOUT(dt);
        assertEquals(0.0, dtCalc._outputSignal[0][0]);
        assertEquals(1.0, dtCalc._outputSignal[1][0]);

        // Step 1: PWM transitions from 0 to 1
        pwmSignal[0] = 1.0;
        dtCalc.calculateYOUT(dt);
        // Low side must immediately drop to 0
        assertEquals(0.0, dtCalc._outputSignal[1][0], "Low side must turn OFF immediately on PWM rising edge");
        // High side must stay 0 during the dead-time window
        assertEquals(0.0, dtCalc._outputSignal[0][0], "High side must remain OFF during dead time");

        // Advance through dead-time window (10 us total)
        for (int i = 0; i < 9; i++) {
            dtCalc.calculateYOUT(dt);
            assertEquals(0.0, dtCalc._outputSignal[0][0], "High side must not turn ON before dead time elapses");
            assertEquals(0.0, dtCalc._outputSignal[1][0], "Low side must remain OFF");
        }

        // At 10th step (10 us elapsed), High side turns ON
        dtCalc.calculateYOUT(dt);
        assertEquals(1.0, dtCalc._outputSignal[0][0], "High side turns ON after tDead");
        assertEquals(0.0, dtCalc._outputSignal[1][0]);

        // Transition back from 1 to 0
        pwmSignal[0] = 0.0;
        dtCalc.calculateYOUT(dt);
        // High side turns OFF immediately
        assertEquals(0.0, dtCalc._outputSignal[0][0], "High side turns OFF immediately on PWM falling edge");
        // Low side remains OFF during dead-time
        assertEquals(0.0, dtCalc._outputSignal[1][0], "Low side remains OFF during dead time");

        // Advance through falling dead-time window
        for (int i = 0; i < 9; i++) {
            dtCalc.calculateYOUT(dt);
            assertEquals(0.0, dtCalc._outputSignal[0][0]);
            assertEquals(0.0, dtCalc._outputSignal[1][0]);
        }

        // Low side turns ON after dead time
        dtCalc.calculateYOUT(dt);
        assertEquals(0.0, dtCalc._outputSignal[0][0]);
        assertEquals(1.0, dtCalc._outputSignal[1][0], "Low side turns ON after tDead");
    }

    // ------------------------------------------------------------------
    // single blocks
    // ------------------------------------------------------------------

    @Test
    void constant1005_outputsItsSlotValueAndALabeledTap() throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /vref
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] 6.0
            orientierung 503
            idStringDialog CONST.1
            <\\ElementCONTROL>
            """);

        assertTrue(coupling.calculators().isEmpty(),
                "constants are non-executing markers and never enter the calculator list");
        assertEquals(1, coupling.signalTaps().size());
        ControlCalculatorBuilder.SignalTap tap = coupling.signalTaps().get(0);
        assertEquals("vref", tap.name());
        assertEquals(6.0, tap.source()._outputSignal[0][0], 1e-12,
                "constant outputs are pre-set at construction");
    }

    @Test
    void gain1006_scalesTheIncomingSignal() throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] 4.0
            orientierung 503
            idStringDialog CONST.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1006
            uniqueObjectIdentifier 301
            x 20
            y 10
            parameter[] 2.5
            orientierung 503
            idStringDialog GAIN.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18
            y[] 10 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """);

        GainCalculator gain = assertInstanceOf(GainCalculator.class, coupling.calculators().get(0));
        step(coupling);
        assertEquals(10.0, gain._outputSignal[0][0], 1e-12);
    }

    // ------------------------------------------------------------------
    // CTRL_PI (1007): series form y = Kp * (x + (1/Ti) * integral of x)
    // ------------------------------------------------------------------

    @Test
    void pi1007_matchesTheSeriesFormTrapezoidalResponse() throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling(piChain(0.001));

        PICalculator pi = assertInstanceOf(PICalculator.class, coupling.calculators().get(0));
        coupling.initialize(DT);

        // Kp = 2, Ti = 1e-3 -> integral gain Kp/Ti = 2000.
        // Step 1: y = Kp*x + 0.5*a1*dt*(x + 0) = 2 + 2000*0.5e-6 = 2.001
        step(coupling);
        assertEquals(2.001, pi._outputSignal[0][0], 1e-12);
        // Step 2: integral accumulates the trapezoid over (x, x) -> +2e-3
        step(coupling);
        assertEquals(2.003, pi._outputSignal[0][0], 1e-12);
    }

    @Test
    void pi1007_nonPositiveIntegrationTime_degradesToProportionalOnly() throws Exception {
        // parameter[] = {Kp = 3.0, Ti = 0.0}
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling(piChain(0.0));

        PICalculator pi = assertInstanceOf(PICalculator.class, coupling.calculators().get(0));
        coupling.initialize(DT);
        step(coupling);
        step(coupling);
        assertEquals(2.0, pi._outputSignal[0][0], 1e-12);
    }

    /** Constant 1.0 feeding a PI block with Kp = 2, Ti = 1e-3 s (or Ti = 0 when disabled). */
    private static String piChain(double integrationTime) {
        return """
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] 1.0
            orientierung 503
            idStringDialog CONST.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1007
            uniqueObjectIdentifier 301
            x 20
            y 10
            parameter[] 2.0 %s
            orientierung 503
            idStringDialog PI.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18
            y[] 10 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """.formatted(Double.toString(integrationTime));
    }

    // ------------------------------------------------------------------
    // CTRL_PT1 (1008)
    // ------------------------------------------------------------------

    @Test
    void pt11008_firstOrderLag_reachesUnityDcGain() throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] 1.0
            orientierung 503
            idStringDialog CONST.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1008
            uniqueObjectIdentifier 301
            x 20
            y 10
            parameter[] 0.001
            orientierung 503
            idStringDialog PT1.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18
            y[] 10 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """);

        PT1Calculator pt1 = assertInstanceOf(PT1Calculator.class, coupling.calculators().get(0));
        coupling.initialize(DT);
        step(coupling);
        // one step of a 1 ms lag at dt = 1 us: y ~ dt/(2T) = 5e-4
        assertEquals(5.0e-4, pt1._outputSignal[0][0], 1.0e-6);
        for (int i = 0; i < 20_000; i++) {
            step(coupling);
        }
        // after 20 time constants the lag has settled to unity DC gain
        assertEquals(1.0, pt1._outputSignal[0][0], 1e-6);
    }

    // ------------------------------------------------------------------
    // CTRL_INTEGRATOR (1009)
    // ------------------------------------------------------------------

    @Test
    void integrator1009_accumulatesFromTheInitialValue() throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] 2.0
            orientierung 503
            idStringDialog CONST.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1009
            uniqueObjectIdentifier 301
            x 20
            y 10
            parameter[] 0.5
            orientierung 503
            idStringDialog INT.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18
            y[] 10 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """);

        IntegratorCalculation integrator = assertInstanceOf(IntegratorCalculation.class,
                coupling.calculators().get(0));
        coupling.initialize(DT);
        for (int i = 0; i < 1000; i++) {
            step(coupling);
        }
        // initial state 0.5 plus ~1 ms of constant 2 integrated (trapezoid edge
        // correction 2e-6 is below the tolerance)
        assertEquals(0.502, integrator._outputSignal[0][0], 1e-4);
    }

    // ------------------------------------------------------------------
    // CTRL_COMPARATOR (1010)
    // ------------------------------------------------------------------

    @Test
    void comparator1010_outputsThePlusMinusDecision() throws Exception {
        assertEquals(1.0, comparatorOutput(3.0, 1.0), 1e-12, "plus above minus -> 1");
        assertEquals(0.0, comparatorOutput(1.0, 3.0), 1e-12, "plus below minus -> 0");
    }

    /** Constant 'plus' into input 0, constant 'minus' into input 1 of a comparator. */
    private static double comparatorOutput(double plus, double minus) throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 8
            parameter[] %s
            orientierung 503
            idStringDialog PLUS.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 301
            x 10
            y 12
            parameter[] %s
            orientierung 503
            idStringDialog MINUS.1
            <\\ElementCONTROL>
            c (2)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1010
            uniqueObjectIdentifier 302
            x 24
            y 10
            parameter[] 0.0
            orientierung 503
            idStringDialog CMP.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 22 22
            y[] 8 8 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            verbindungCONTROL (1)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18 18 22
            y[] 12 12 11 11
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """.formatted(Double.toString(plus), Double.toString(minus)));

        GreaterThanCalculator comparator = assertInstanceOf(GreaterThanCalculator.class,
                coupling.calculators().get(0));
        step(coupling);
        return comparator._outputSignal[0][0];
    }

    // ------------------------------------------------------------------
    // logic (1011..1013)
    // ------------------------------------------------------------------

    @Test
    void and1011_and1013_not_followThresholdSemantics() throws Exception {
        assertEquals(1.0, twoInputLogic(1011, 1.0, 0.6), 1e-12, "AND of high signals");
        assertEquals(0.0, twoInputLogic(1011, 1.0, 0.4), 1e-12, "AND with one low signal");
        assertEquals(1.0, twoInputLogic(1012, 0.0, 0.7), 1e-12, "OR with one high signal");
        assertEquals(0.0, twoInputLogic(1012, 0.2, 0.1), 1e-12, "OR of low signals");
        assertEquals(1.0, notOutput(0.3), 1e-12, "NOT of a low signal");
        assertEquals(0.0, notOutput(0.8), 1e-12, "NOT of a high signal");
    }

    /** Two constants (a into input 0, b into input 1) feeding a logic block of the given typ. */
    private static double twoInputLogic(int typ, double a, double b) throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 8
            parameter[] %s
            orientierung 503
            idStringDialog A.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 301
            x 10
            y 12
            parameter[] %s
            orientierung 503
            idStringDialog B.1
            <\\ElementCONTROL>
            c (2)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ %d
            uniqueObjectIdentifier 302
            x 24
            y 10
            parameter[] 0.0
            orientierung 503
            idStringDialog LOGIC.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 22 22
            y[] 8 8 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            verbindungCONTROL (1)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18 18 22
            y[] 12 12 11 11
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """.formatted(Double.toString(a), Double.toString(b), typ));

        step(coupling);
        return coupling.calculators().get(0)._outputSignal[0][0];
    }

    /** Constant input feeding a NOT block. */
    private static double notOutput(double value) throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] %s
            orientierung 503
            idStringDialog A.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1013
            uniqueObjectIdentifier 301
            x 20
            y 10
            parameter[] 0.0
            orientierung 503
            idStringDialog NOT.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18
            y[] 10 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """.formatted(Double.toString(value)));

        assertInstanceOf(NotCalculator.class, coupling.calculators().get(0));
        step(coupling);
        return coupling.calculators().get(0)._outputSignal[0][0];
    }

    // ------------------------------------------------------------------
    // CTRL_MUX selector (1014)
    // ------------------------------------------------------------------

    @Test
    void selector1014_switchesBetweenTheDataInputs() throws Exception {
        assertEquals(7.0, selectorOutput(1.0), 1e-12, "selector high passes input 2");
        assertEquals(5.0, selectorOutput(0.0), 1e-12, "selector low passes input 1");
    }

    /**
     * Selector block with data inputs 5 and 7 and the given selector value;
     * returns the block output after one step.
     */
    private static double selectorOutput(double selector) throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 6
            parameter[] %s
            orientierung 503
            idStringDialog SEL.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 301
            x 10
            y 12
            parameter[] 5.0
            orientierung 503
            idStringDialog IN0.1
            <\\ElementCONTROL>
            c (2)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 302
            x 10
            y 16
            parameter[] 7.0
            orientierung 503
            idStringDialog IN1.1
            <\\ElementCONTROL>
            c (3)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1014
            uniqueObjectIdentifier 303
            x 24
            y 10
            parameter[] 0.0
            orientierung 503
            idStringDialog MUX.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 22 22
            y[] 6 6 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            verbindungCONTROL (1)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 16 16 22
            y[] 12 12 11 11
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            verbindungCONTROL (2)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 22 22
            y[] 16 16 12
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """.formatted(Double.toString(selector)));

        assertInstanceOf(SignalSelectorCalculator.class, coupling.calculators().get(0));
        step(coupling);
        return coupling.calculators().get(0)._outputSignal[0][0];
    }

    // ------------------------------------------------------------------
    // CTRL_DELAY (1015)
    // ------------------------------------------------------------------

    @Test
    void delay1015_passesTheSignalAfterTheDelayTime() throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1005
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] 1.0
            orientierung 503
            idStringDialog CONST.1
            <\\ElementCONTROL>
            c (1)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1015
            uniqueObjectIdentifier 301
            x 20
            y 10
            parameter[] 5.0e-6
            orientierung 503
            idStringDialog DELAY.1
            <\\ElementCONTROL>
            verbindungCONTROL (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 12 18
            y[] 10 10
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 1
            <\\Connection>
            """);

        assertInstanceOf(DelayCalculator.class, coupling.calculators().get(0));
        coupling.initialize(DT);
        double[] outputs = new double[12];
        for (int i = 0; i < outputs.length; i++) {
            step(coupling);
            outputs[i] = coupling.calculators().get(0)._outputSignal[0][0];
        }
        assertEquals(0.0, outputs[2], 1e-12, "still inside the delay");
        assertEquals(1.0, outputs[11], 1e-12, "delay elapsed -> signal passes");
    }

    // ------------------------------------------------------------------
    // web gate (1000) and web probes (1001/1002) behave like types 6/1/2
    // ------------------------------------------------------------------

    @Test
    void gate1000_drivesTheIgbtLikeTheClassicType() throws Exception {
        CircuitModel model = parseBuck("1000", "1001");
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        ControlCalculatorBuilder.ControlCoupling coupling =
                ControlCalculatorBuilder.build(model, netlist);

        assertEquals(1, coupling.gateDrives().size());
        assertEquals(1, coupling.probes().size());
        ControlCalculatorBuilder.GateDrive drive = coupling.gateDrives().get(0);
        assertEquals(1, drive.elementIndex(), "IGBT.1 is the second circuit component");
        assertEquals(CircuitTypCore.LK_IGBT, drive.switchType());

        // the only executable calculator is the rectangle source (duty 0.5,
        // first step high); the gate reads it live through the aliased input
        assertEquals(1, coupling.calculators().size());
        coupling.initialize(DT);
        coupling.calculators().get(0).calculateYOUT(DT);
        assertEquals(1.0, drive.gateSignal(), 1e-12);
        coupling.applyGateSignals(netlist);
        coupling.applyGateSignals(netlist);
        assertEquals(1.0, netlist.getParameter(drive.elementIndex())[8], 1e-12);
    }

    @Test
    void volt1001_probe_readsVoltageAcrossTheCoupledResistor() throws Exception {
        CircuitModel model = parseBuck("6", "1001");
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        ControlCalculatorBuilder.ControlCoupling coupling =
                ControlCalculatorBuilder.build(model, netlist);

        assertEquals(1, coupling.probes().size());
        ControlCalculatorBuilder.Probe probe = coupling.probes().get(0);
        assertFalse(probe.current());
        double[] nodeVoltages = new double[netlist.getNodeMax() + 1];
        nodeVoltages[2] = 50.0;
        coupling.updateProbes(netlist, nodeVoltages);
        assertEquals(50.0, probe.outputHolder()._outputSignal[0][0], 1e-12);
    }

    @Test
    void amp1002_probe_readsCurrentThroughTheCoupledResistor() throws Exception {
        CircuitModel model = parseBuck("6", "1002");
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        ControlCalculatorBuilder.ControlCoupling coupling =
                ControlCalculatorBuilder.build(model, netlist);

        assertEquals(1, coupling.probes().size());
        assertTrue(coupling.probes().get(0).current());
    }

    /** Parses the buck backdrop with the gate/probe control types substituted. */
    private static CircuitModel parseBuck(String gateType, String probeType) throws Exception {
        String text = BUCK_TEXT
                .replace("%GATE%", gateType)
                .replace("%PROBE%", probeType);
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "test.ipes");
    }

    @Test
    void scope1003_isSkippedSilently() throws Exception {
        ControlCalculatorBuilder.ControlCoupling coupling = buildCoupling("""
            c (0)
            <ElementCONTROL>
            labelAnfangsKnoten[] /NIX_NIX_NIX
            labelEndKnoten[] /NIX_NIX_NIX
            enabledShorted 1
            typ 1003
            uniqueObjectIdentifier 300
            x 10
            y 10
            parameter[] 0.0
            orientierung 503
            idStringDialog SCOPE.1
            <\\ElementCONTROL>
            """);

        assertTrue(coupling.calculators().isEmpty());
        assertTrue(coupling.gateDrives().isEmpty());
        assertTrue(coupling.probes().isEmpty());
    }

    // ------------------------------------------------------------------
    // closed-loop integration: bang-bang buck regulated by native blocks
    // ------------------------------------------------------------------

    /**
     * Full closed loop built only from web catalog blocks: a CTRL_CONSTANT
     * reference (6 V) and a CTRL_VOLT probe on the load resistor feed the two
     * inputs of a CTRL_COMPARATOR, whose output drives the buck switch through
     * a CTRL_GATE. The bang-bang (hysteretic) loop must regulate the output to
     * the reference.
     */
    @Test
    void closedLoopBuck_bangBangComparatorRegulatesToTheReference() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(
                new BufferedReader(new StringReader(CLOSED_LOOP_BUCK)), "buck.ipes");

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationResult result = engine.runSimulation(SimulationConfig.builder()
                .circuitModel(model)
                .solverType(SolverType.SOLVER_BE)
                .stepWidth(DT)
                .simulationDuration(0.005)
                .signals(List.of("vout", "vref", "pwm"))
                .build());
        assertTrue(result.isSuccess(), () -> "simulation failed: " + result.getErrorMessage());

        String[] names = result.getSignalNames();
        List<String> signalList = Arrays.asList(names);
        assertTrue(signalList.contains("vout"), "probe signal missing: " + Arrays.toString(names));
        assertTrue(signalList.contains("vref"), "constant tap missing: " + Arrays.toString(names));
        assertTrue(signalList.contains("pwm"), "comparator tap missing: " + Arrays.toString(names));

        float[] vout = result.getSignalData(signalList.indexOf("vout"));
        float[] pwm = result.getSignalData(signalList.indexOf("pwm"));
        assertTrue(vout.length >= 1000, "expected a substantial run");

        double tailMean = 0;
        for (int i = vout.length - 1000; i < vout.length; i++) {
            tailMean += vout[i];
        }
        tailMean /= 1000;
        assertEquals(6.0, tailMean, 0.06,
                "bang-bang loop must regulate the output voltage to the 6 V reference");
        assertEquals(6.0, vout[vout.length - 1], 0.12, "final sample must sit at the reference");

        boolean pwmSwitched = false;
        for (float value : pwm) {
            if (value == 0.0f) {
                pwmSwitched = true;
                break;
            }
        }
        assertTrue(pwmSwitched, "the comparator output must actually switch the gate");
    }

    /** Buck power stage (12 V input, 6 V reference, LC 100 uH / 100 uF, 6 ohm load). */
    private static final String CLOSED_LOOP_BUCK = """
        tDURATION 0.005
        dt 1e-06
        e (0)
        <ElementLK>
        labelAnfangsKnoten[] /vin
        labelEndKnoten[] /0
        enabledShorted 1
        typ 4
        uniqueObjectIdentifier 100
        x 6
        y 20
        parameter[] 401.0 12.0
        orientierung 503
        idStringDialog U.1
        <\\ElementLK>
        e (1)
        <ElementLK>
        labelAnfangsKnoten[] /vin
        labelEndKnoten[] /sw
        enabledShorted 1
        typ 7
        uniqueObjectIdentifier 101
        x 14
        y 18
        parameter[] 10000000.0 0.01 10000000.0
        orientierung 502
        idStringDialog S.1
        <\\ElementLK>
        e (2)
        <ElementLK>
        labelAnfangsKnoten[] /0
        labelEndKnoten[] /sw
        enabledShorted 1
        typ 6
        uniqueObjectIdentifier 102
        x 14
        y 26
        parameter[] 0.01 0.6 0.01 10000000.0
        orientierung 501
        idStringDialog D.1
        <\\ElementLK>
        e (3)
        <ElementLK>
        labelAnfangsKnoten[] /sw
        labelEndKnoten[] /vout
        enabledShorted 1
        typ 2
        uniqueObjectIdentifier 103
        x 22
        y 18
        parameter[] 1.0E-4 0.0
        orientierung 502
        idStringDialog L.1
        <\\ElementLK>
        e (4)
        <ElementLK>
        labelAnfangsKnoten[] /0
        labelEndKnoten[] /vout
        enabledShorted 1
        typ 3
        uniqueObjectIdentifier 104
        x 26
        y 26
        parameter[] 1.0E-4 0.0
        orientierung 501
        idStringDialog C.1
        <\\ElementLK>
        e (5)
        <ElementLK>
        labelAnfangsKnoten[] /vout
        labelEndKnoten[] /0
        enabledShorted 1
        typ 1
        uniqueObjectIdentifier 105
        x 32
        y 26
        parameter[] 6.0
        orientierung 503
        idStringDialog R.1
        <\\ElementLK>
        verbindungLK (0)
        <Connection>
        label NIX_NIX_NIX
        x[] 6 12
        y[] 18 18
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        verbindungLK (1)
        <Connection>
        label NIX_NIX_NIX
        x[] 16 16 14
        y[] 18 24 24
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        verbindungLK (2)
        <Connection>
        label NIX_NIX_NIX
        x[] 16 18 20
        y[] 18 18 18
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        verbindungLK (3)
        <Connection>
        label NIX_NIX_NIX
        x[] 24 24 26
        y[] 18 24 24
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        verbindungLK (4)
        <Connection>
        label NIX_NIX_NIX
        x[] 26 32
        y[] 24 24
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        verbindungLK (5)
        <Connection>
        label NIX_NIX_NIX
        x[] 6 6 14 26 32 32
        y[] 22 30 30 30 30 28
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        verbindungLK (6)
        <Connection>
        label NIX_NIX_NIX
        x[] 14 14
        y[] 28 30
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        verbindungLK (7)
        <Connection>
        label NIX_NIX_NIX
        x[] 26 26
        y[] 28 30
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 0
        <\\Connection>
        c (0)
        <ElementCONTROL>
        labelAnfangsKnoten[] /NIX_NIX_NIX
        labelEndKnoten[] /vref
        enabledShorted 1
        typ 1005
        uniqueObjectIdentifier 300
        x 14
        y 40
        parameter[] 6.0
        orientierung 503
        idStringDialog CONST.1
        <\\ElementCONTROL>
        c (1)
        <ElementCONTROL>
        labelAnfangsKnoten[] /NIX_NIX_NIX
        labelEndKnoten[] /vout
        enabledShorted 1
        typ 1001
        uniqueObjectIdentifier 301
        x 14
        y 34
        parameter[] 0.0
        coupledReferenceID[] 105
        orientierung 503
        idStringDialog VOLT.1
        <\\ElementCONTROL>
        c (2)
        <ElementCONTROL>
        labelAnfangsKnoten[] /NIX_NIX_NIX
        labelEndKnoten[] /pwm
        enabledShorted 1
        typ 1010
        uniqueObjectIdentifier 302
        x 24
        y 36
        parameter[] 0.0
        orientierung 503
        idStringDialog CMP.1
        <\\ElementCONTROL>
        c (3)
        <ElementCONTROL>
        labelAnfangsKnoten[] /NIX_NIX_NIX
        labelEndKnoten[] /NIX_NIX_NIX
        enabledShorted 1
        typ 1000
        uniqueObjectIdentifier 303
        x 34
        y 36
        parameter[] 0.0
        coupledReferenceID[] 101
        orientierung 503
        idStringDialog GATE.1
        <\\ElementCONTROL>
        verbindungCONTROL (0)
        <Connection>
        label NIX_NIX_NIX
        x[] 16 18 18 22
        y[] 34 34 37 37
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 1
        <\\Connection>
        verbindungCONTROL (1)
        <Connection>
        label NIX_NIX_NIX
        x[] 16 20 20 22
        y[] 40 40 36 36
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 1
        <\\Connection>
        verbindungCONTROL (2)
        <Connection>
        label NIX_NIX_NIX
        x[] 26 32
        y[] 36 36
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 1
        <\\Connection>
        """;

    /** The classic buck-with-control text used as the backdrop for web-type substitution. */
    private static final String BUCK_TEXT = """
        tDURATION 0.005
        dt 5e-07
        e (0)
        <ElementLK>
        labelAnfangsKnoten[] /1
        labelEndKnoten[] /0
        typ 4
        uniqueObjectIdentifier 100
        x 10
        y 18
        parameter[] 401.0 100.0
        orientierung 503
        idStringDialog U.1
        <\\ElementLK>
        e (1)
        <ElementLK>
        labelAnfangsKnoten[] /1
        labelEndKnoten[] /2
        typ 10
        uniqueObjectIdentifier 101
        x 15
        y 13
        parameter[] 10000000.0 0.6 0.01 10000000.0
        orientierung 502
        idStringDialog IGBT.1
        <\\ElementLK>
        e (2)
        <ElementLK>
        labelAnfangsKnoten[] /2
        labelEndKnoten[] /0
        typ 1
        uniqueObjectIdentifier 102
        x 30
        y 18
        parameter[] 10.0
        orientierung 503
        idStringDialog R.1
        <\\ElementLK>
        c (0)
        <ElementCONTROL>
        typ 4
        uniqueObjectIdentifier 200
        x 25
        y 46
        parameter[] 404.0 1.0 1000.0 0.0 0.0 0.5
        orientierung 503
        idStringDialog SIGNAL.1
        <\\ElementCONTROL>
        c (1)
        <ElementCONTROL>
        typ %GATE%
        uniqueObjectIdentifier 201
        x 37
        y 46
        parameter[] 0.0
        coupledReferenceID[] 101
        orientierung 503
        idStringDialog GATE.1
        <\\ElementCONTROL>
        c (2)
        <ElementCONTROL>
        typ %PROBE%
        uniqueObjectIdentifier 202
        x 67
        y 21
        parameter[] 0.0
        coupledReferenceID[] 102
        orientierung 503
        idStringDialog VOLT.1
        <\\ElementCONTROL>
        verbindungCONTROL (0)
        <Connection>
        label NIX_NIX_NIX
        x[] 27 35
        y[] 46 46
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 1
        <\\Connection>
        """;

    /** Control-only model: parses the given .ipes snippet and builds the coupling (no netlist). */
    private static ControlCalculatorBuilder.ControlCoupling buildCoupling(String controlSection)
            throws Exception {
        CircuitModel model = new CircuitFileParser().parse(
                new BufferedReader(new StringReader(controlSection)), "test.ipes");
        return ControlCalculatorBuilder.build(model, null);
    }

    /** Executes the whole chain in topological order for one time step. */
    private static void step(ControlCalculatorBuilder.ControlCoupling coupling) {
        for (AbstractControlCalculatable calculator : coupling.calculators()) {
            calculator.calculateYOUT(DT);
        }
    }
}
