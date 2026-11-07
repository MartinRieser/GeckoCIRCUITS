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
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentCurrentCalculatorTest {

    private ComponentCurrentCalculator calculator;
    private MatrixSolver matrixSolver;

    private static final double EPSILON = 1.0e-12;
    private static final double DT = 1.0e-5;

    @BeforeEach
    void setUp() {
        calculator = new ComponentCurrentCalculator();
        matrixSolver = new MatrixSolver(SolverType.SOLVER_BE);
    }

    @Test
    void testNullMatrixSolverThrowsException() {
        SimpleNetList netlist = new SimpleNetList();
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateComponentCurrents(null, netlist, 0.01, DT, 0.0, true)
        );
    }

    @Test
    void testNullNetlistThrowsException() {
        matrixSolver.initializeMatrices(5, 1, 1);
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateComponentCurrents(matrixSolver, null, 0.01, DT, 0.0, true)
        );
    }

    @Test
    void testResistorCurrentCalculation() {
        matrixSolver.initializeMatrices(5, 1, 1);
        double[] p = matrixSolver.getP();
        p[0] = 10.0;
        p[1] = 2.0;

        SimpleNetList netlist = new SimpleNetList();
        netlist.addComponent(CircuitTypCore.LK_R, 0, 1, new double[4]);
        netlist.getParameter(0)[0] = 2.0;

        calculator.calculateComponentCurrents(matrixSolver, netlist, 0.01, DT, 0.0, true);

        double[] params = netlist.getParameter(0);
        assertEquals(4.0, params[1], EPSILON, "Resistor current should be 4.0 A");
    }

    @Test
    void testInductorCurrentCalculation() {
        matrixSolver.initializeMatrices(5, 1, 1);
        double[] p = matrixSolver.getP();
        double[] iALT = matrixSolver.getIALT();
        p[0] = 10.0;
        p[1] = 0.0;
        iALT[0] = 5.0;

        SimpleNetList netlist = new SimpleNetList();
        netlist.addComponent(CircuitTypCore.LK_L, 0, 1, new double[15]);
        netlist.getParameter(0)[0] = 1.0e-3;

        calculator.calculateComponentCurrents(matrixSolver, netlist, 0.01, DT, 0.0, true);

        double[] params = netlist.getParameter(0);
        assertEquals(5.1, params[1], 1.0e-10, "Inductor current should be 5.1 A");
    }

    @Test
    void testCapacitorCurrentCalculation() {
        matrixSolver.initializeMatrices(5, 1, 1);
        double[] p = matrixSolver.getP();
        double[] pALT = matrixSolver.getPALT();

        p[0] = 100.0;
        p[1] = 0.0;
        pALT[0] = 99.0;
        pALT[1] = 0.0;

        SimpleNetList netlist = new SimpleNetList();
        double[] capParams = new double[15];
        capParams[6] = 1.0e-6;
        capParams[7] = 1.0e-6;
        netlist.addComponent(CircuitTypCore.LK_C, 0, 1, capParams);

        calculator.calculateComponentCurrents(matrixSolver, netlist, 0.01, DT, 0.0, true);

        double[] params = netlist.getParameter(0);
        assertEquals(0.1, params[1], 1.0e-10, "Capacitor current should be 0.1 A");
    }

    @Test
    void testReturnsFalseForSimplifiedVersion() {
        matrixSolver.initializeMatrices(5, 1, 1);
        SimpleNetList netlist = new SimpleNetList();

        boolean needsRecalculation = calculator.calculateComponentCurrents(
                matrixSolver, netlist, 0.01, DT, 0.0, true);

        assertFalse(needsRecalculation, "Simplified version should return false");
    }

    @Test
    void testZeroCapacitanceDoesNotProduceNaN() {
        matrixSolver.initializeMatrices(5, 1, 2);
        double[] p = matrixSolver.getP();
        p[0] = 10.0;
        p[1] = 0.0;

        SimpleNetList netlist = new SimpleNetList();
        // Uninitialized / all-zero capacitor parameters
        netlist.addComponent(CircuitTypCore.LK_C, 0, 1, new double[15]);
        netlist.addComponent(CircuitTypCore.TH_CTH, 0, 1, new double[15]);

        calculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, DT, 0.0, true);

        assertFalse(Double.isNaN(netlist.getParameter(0)[1]), "Zero-capacitance LK_C current must not be NaN");
        assertFalse(Double.isNaN(netlist.getParameter(1)[1]), "Zero-capacitance TH_CTH current must not be NaN");
        assertEquals(0.0, matrixSolver.getICurrent()[0], EPSILON);
        assertEquals(0.0, matrixSolver.getICurrent()[1], EPSILON);
    }

    @Test
    void testVoltageSourceCurrentExtractedFromMnaSolution() {
        matrixSolver.initializeMatrices(5, 3, 3);
        double[] p = matrixSolver.getP();
        // nodeMax = 5, vsNumber = 1 -> voltageSourceIdx = 6
        p[6] = -4.8;
        p[7] = 12.5;
        p[8] = 3.2;

        SimpleNetList netlist = new SimpleNetList();
        double[] uParams = new double[10];
        uParams[0] = 401;
        uParams[1] = 48.0; // DC voltage parameter in slot 1
        netlist.addComponent(CircuitTypCore.LK_U, 0, 1, 1, uParams);

        double[] tempParams = new double[10];
        tempParams[0] = 401;
        tempParams[1] = 300.0; // Temperature parameter in slot 1
        netlist.addComponent(CircuitTypCore.TH_TEMP, 0, 1, 2, tempParams);

        double[] mmfParams = new double[10];
        mmfParams[0] = 401;
        mmfParams[1] = 100.0; // MMF parameter in slot 1
        netlist.addComponent(CircuitTypCore.REL_MMF, 0, 1, 3, mmfParams);

        calculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, DT, 0.0, true);

        assertEquals(-4.8, matrixSolver.getICurrent()[0], EPSILON,
                "LK_U iCurrent must hold solved MNA branch current, not the 48V voltage parameter");
        assertEquals(12.5, matrixSolver.getICurrent()[1], EPSILON,
                "TH_TEMP iCurrent must hold solved heat flow, not the 300K temperature parameter");
        assertEquals(3.2, matrixSolver.getICurrent()[2], EPSILON,
                "REL_MMF iCurrent must hold solved flux, not the 100A MMF parameter");
    }

    @Test
    void testCoupledInductorCurrentExtractedFromMnaSolution() {
        matrixSolver.initializeMatrices(5, 1, 1);
        matrixSolver.getP()[6] = 7.25; // nodeMax=5 + vsNumber=1

        SimpleNetList netlist = new SimpleNetList();
        double[] lkopParams = new double[10];
        lkopParams[0] = 1e-3;
        netlist.addComponent(CircuitTypCore.LK_LKOP2, 0, 1, 1, lkopParams);

        calculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, DT, 0.0, true);

        assertEquals(7.25, lkopParams[1], EPSILON);
        assertEquals(7.25, matrixSolver.getICurrent()[0], EPSILON);
    }

    @Test
    void testSwitchAndMosfetCurrentAndVoltageSlots() {
        matrixSolver.initializeMatrices(5, 1, 2);
        double[] p = matrixSolver.getP();
        p[0] = 20.0;
        p[1] = 5.0;

        SimpleNetList netlist = new SimpleNetList();
        double[] switchParams = new double[10];
        switchParams[0] = 0.5;   // current rD
        switchParams[1] = 0.5;   // rON (must not be clobbered)
        switchParams[2] = 1e7;   // rOFF
        netlist.addComponent(CircuitTypCore.LK_S, 0, 1, switchParams);

        double[] mosfetParams = new double[10];
        mosfetParams[0] = 0.1;   // current rD
        mosfetParams[1] = 0.8;   // uF
        mosfetParams[2] = 0.1;   // rON
        mosfetParams[3] = 1e7;   // rOFF
        netlist.addComponent(CircuitTypCore.LK_MOSFET, 0, 1, mosfetParams);

        calculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, DT, 0.0, true);

        assertEquals(0.5, switchParams[1], EPSILON, "LK_S rON slot [1] must not be overwritten");
        assertEquals(30.0, switchParams[4], EPSILON, "LK_S current should be in slot 4");
        assertEquals(30.0, matrixSolver.getICurrent()[0], EPSILON);
        assertEquals(150.0, mosfetParams[4], EPSILON, "LK_MOSFET current should be in slot 4");
        assertEquals(150.0, matrixSolver.getICurrent()[1], EPSILON);
    }

    @Test
    void testIgbtStateMachineAndZeroParameterGuards() {
        matrixSolver.initializeMatrices(5, 1, 2);
        double[] p = matrixSolver.getP();
        p[0] = 10.0;
        p[1] = 0.0;

        SimpleNetList netlist = new SimpleNetList();
        // IGBT 1: normal parameters, gate ON -> should flip from rOFF to rON and request stepBack
        double[] igbtNormal = new double[12];
        igbtNormal[0] = 1e7;  // rD = rOFF
        igbtNormal[1] = 0.8;  // uF
        igbtNormal[2] = 0.01; // rON
        igbtNormal[3] = 1e7;  // rOFF
        igbtNormal[8] = 1.0;  // gate ON
        netlist.addComponent(CircuitTypCore.LK_IGBT, 0, 1, igbtNormal);

        // IGBT 2: uninitialized all-zero parameters (rD=0, rON=0, rOFF=0, gate=0)
        // Must not divide by zero or request infinite stepBack when rON == rOFF
        double[] igbtZero = new double[12];
        netlist.addComponent(CircuitTypCore.LK_IGBT, 0, 1, igbtZero);

        boolean stepBack = calculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, DT, 0.0, true);
        assertTrue(stepBack, "IGBT 1 with gate=1 and forward voltage must flip to rON");
        assertEquals(0.01, igbtNormal[0], EPSILON);
        assertTrue(Double.isFinite(igbtZero[4]), "IGBT 2 with zero rD must produce finite current");

        // Now turn gate OFF on both: IGBT 1 flips back to rOFF, IGBT 2 (rON==rOFF==0) does NOT flip
        igbtNormal[0] = 1e7; // already OFF
        igbtNormal[8] = 0.0;
        boolean stepBack2 = calculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, DT, 0.0, true);
        assertFalse(stepBack2, "Uninitialized IGBT with rON == rOFF must not trigger endless stepBack");
    }

    @Test
    void testDiodeAndThyristorStateMachines() {
        matrixSolver.initializeMatrices(5, 1, 2);
        double[] p = matrixSolver.getP();
        p[0] = 5.0;
        p[1] = 0.0;

        SimpleNetList netlist = new SimpleNetList();
        double[] diodeParams = new double[12];
        diodeParams[0] = 1e7;  // blocking
        diodeParams[1] = 0.7;  // uF
        diodeParams[2] = 0.01; // rON
        diodeParams[3] = 1e7;  // rOFF
        netlist.addComponent(CircuitTypCore.LK_D, 0, 1, diodeParams);

        double[] thyrParams = new double[12];
        thyrParams[0] = 1e7;  // blocking
        thyrParams[1] = 0.8;  // uF
        thyrParams[2] = 0.01; // rON
        thyrParams[3] = 1e7;  // rOFF
        thyrParams[8] = 1.0;  // gate ON
        netlist.addComponent(CircuitTypCore.LK_THYR, 0, 1, thyrParams);

        boolean stepBack = calculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, DT, 0.0, true);
        assertTrue(stepBack);
        assertEquals(0.01, diodeParams[0], EPSILON, "Forward-biased diode must switch to rON");
        assertEquals(0.01, thyrParams[0], EPSILON, "Forward-biased gated thyristor must switch to rON");
    }

    @Test
    void testTrapezoidalAndGearShichmanInductorAndCapacitorCurrents() {
        for (SolverType solver : new SolverType[]{SolverType.SOLVER_TRZ, SolverType.SOLVER_GS}) {
            MatrixSolver solverInstance = new MatrixSolver(solver);
            solverInstance.initializeMatrices(5, 1, 2);
            solverInstance.getP()[0] = 12.0;
            solverInstance.getP()[1] = 0.0;
            solverInstance.getPALT()[0] = 10.0;
            solverInstance.getPALTALT()[0] = 8.0;
            solverInstance.getIALT()[0] = 1.0;
            solverInstance.getIALTALT()[0] = 0.5;

            SimpleNetList netlist = new SimpleNetList();
            double[] lParams = new double[15];
            lParams[0] = 1e-3;
            netlist.addComponent(CircuitTypCore.LK_L, 0, 1, lParams);

            double[] cParams = new double[15];
            cParams[0] = 10e-6;
            cParams[6] = 10e-6;
            cParams[7] = 10e-6;
            netlist.addComponent(CircuitTypCore.LK_C, 0, 1, cParams);

            calculator.calculateComponentCurrents(solverInstance, netlist, 1.0, DT, 0.0, true);
            assertTrue(Double.isFinite(solverInstance.getICurrent()[0]) && solverInstance.getICurrent()[0] > 1.0);
            assertTrue(Double.isFinite(solverInstance.getICurrent()[1]) && solverInstance.getICurrent()[1] != 0.0);
        }
    }
}
