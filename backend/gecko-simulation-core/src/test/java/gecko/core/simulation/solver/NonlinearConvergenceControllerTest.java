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

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.parameters.DiodeParameters;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Newton-Raphson diode controller: the written linearization
 * satisfies the affine identity, fixed operating points converge, and the
 * iteration cap triggers the piecewise-linear fallback.
 */
class NonlinearConvergenceControllerTest {

    private static final double FORWARD_VOLTAGE = 0.7;
    private static final double RATED_CURRENT = 2.0;

    /** Netlist with a single LK_D element (element 0, nodes 1-0). */
    private static CircuitNetlist singleDiodeNetlist() {
        final CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(
                new CircuitTypCore[]{CircuitTypCore.LK_D},
                new int[]{1},
                new int[]{0},
                new int[]{-1},
                new double[][]{{0.001, FORWARD_VOLTAGE, 0.001, 10.0e7, RATED_CURRENT, 0.0}},
                1, 0, 1);
        return netlist;
    }

    private static double[] potentials(final double diodeVoltage) {
        return new double[]{0.0, diodeVoltage};
    }

    @Test
    void netlistWithoutDiodes_convergesImmediately() {
        final CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(
                new CircuitTypCore[]{CircuitTypCore.LK_R},
                new int[]{1},
                new int[]{0},
                new int[]{-1},
                new double[][]{{100.0}},
                1, 0, 1);
        final NonlinearConvergenceController controller =
                NonlinearConvergenceController.createFromNetlist(netlist);

        assertEquals(0, controller.getDiodeCount());
        assertTrue(controller.updateDiodeStamps(new double[]{0.0, 1.0}, netlist));
    }

    @Test
    void writtenStamps_implementTheNewtonLinearization() {
        final CircuitNetlist netlist = singleDiodeNetlist();
        final NonlinearConvergenceController controller =
                NonlinearConvergenceController.createFromNetlist(netlist);
        final double v0 = 0.6;
        controller.updateDiodeStamps(potentials(v0), netlist);

        // The stamped (rD, uF) pair must reproduce i(v0) + g(v0) * (v - v0)
        final double[] params = netlist.getParameter(0);
        final double rD = params[DiodeParameters.INDEX_CURRENT_RESISTANCE];
        final double uF = params[DiodeParameters.INDEX_FORWARD_VOLTAGE];
        final double i0 = params[DiodeParameters.INDEX_CURRENT];
        final double probeVoltage = 0.65;
        final double linearized = (probeVoltage - uF) / rD;
        final double expected = i0 + i0 / (v0 - uF) * (probeVoltage - v0);

        assertEquals(expected, linearized, 1e-9 * Math.abs(expected),
                "the stamps must realize the tangent of the Shockley curve at v0");
        assertTrue(rD > 0.0 && rD <= 1.0 / NonlinearConvergenceController.MIN_CONDUCTANCE_S,
                "the conductance must stay within its clamp");
    }

    @Test
    void fixedOperatingPoint_convergesOnTheSecondUpdate() {
        final CircuitNetlist netlist = singleDiodeNetlist();
        final NonlinearConvergenceController controller =
                NonlinearConvergenceController.createFromNetlist(netlist);

        assertFalse(controller.updateDiodeStamps(potentials(0.6), netlist),
                "the first update has no previous voltage and cannot report convergence");
        assertTrue(controller.updateDiodeStamps(potentials(0.6), netlist),
                "an unchanged operating point must converge immediately");
    }

    @Test
    void oscillatingVoltages_triggerPiecewiseLinearFallback() {
        final CircuitNetlist netlist = singleDiodeNetlist();
        final NonlinearConvergenceController controller =
                NonlinearConvergenceController.createFromNetlist(netlist);

        boolean converged = true;
        for (int iteration = 0; iteration <= NonlinearConvergenceController.MAX_NEWTON_ITERATIONS;
                iteration++) {
            converged = controller.updateDiodeStamps(
                    potentials(iteration % 2 == 0 ? 0.6 : 0.65), netlist);
        }
        assertFalse(converged, "oscillation must exhaust the Newton iteration cap");
        assertTrue(controller.isFallbackToPiecewiseLinear(),
                "the cap must activate the piecewise-linear fallback");

        controller.beginStep();
        assertFalse(controller.isFallbackToPiecewiseLinear(),
                "a new step must reset the fallback");
    }

    @Test
    void calibration_usesTheRatedCurrentSlot() {
        final CircuitNetlist netlist = singleDiodeNetlist();
        final NonlinearConvergenceController controller =
                NonlinearConvergenceController.createFromNetlist(netlist);
        assertEquals(1, controller.getDiodeCount());

        // The model carries the rated current at the forward voltage: stamped
        // at v = V_f the linearized current must equal the rated current
        controller.updateDiodeStamps(potentials(FORWARD_VOLTAGE), netlist);
        final double[] params = netlist.getParameter(0);
        assertEquals(RATED_CURRENT, params[DiodeParameters.INDEX_CURRENT], 1e-9 * RATED_CURRENT,
                "the Shockley curve must pass through (V_f, rated current)");
    }
}
