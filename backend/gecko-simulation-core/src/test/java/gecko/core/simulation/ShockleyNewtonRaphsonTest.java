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
package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.semiconductors.ShockleyDiodeModel;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.solver.NonlinearConvergenceController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end verification of the Shockley Newton-Raphson semiconductor mode:
 * a source-diode-resistor circuit must settle at the analytically solvable
 * Shockley operating point, distinct from the piecewise-linear point.
 */
class ShockleyNewtonRaphsonTest {

    private static final double SUPPLY_VOLTAGE = 10.0;
    private static final double LOAD_RESISTANCE = 1000.0;
    private static final double FORWARD_VOLTAGE = 0.7;
    private static final double RATED_CURRENT = 1.0;

    private CircuitModel buildDiodeModel() {
        final CircuitModel model = new CircuitModel();

        // Voltage source U at (20,14), vertical (orientation 503)
        final CircuitModel.ComponentData u = new CircuitModel.ComponentData(4, "U", 20, 14, 503);
        final double[] uParams = new double[21];
        uParams[0] = 401.0;   // DC source
        uParams[1] = SUPPLY_VOLTAGE;
        u.setRawParameters(uParams);
        model.addCircuitComponent(u);

        // Diode D at (24,15), vertical: anode (24,13) top, cathode (24,17);
        // both terminals labeled - the asserted diode drop uIn - uOut is
        // independent of which island node the builder pins to ground
        final CircuitModel.ComponentData d = new CircuitModel.ComponentData(6, "D", 24, 15, 503);
        d.setRawParameters(new double[]{0.001, FORWARD_VOLTAGE, 0.001, 10.0e7, RATED_CURRENT, 0.0});
        d.setTerminalXLabels(new String[]{"uIn"});
        d.setTerminalYLabels(new String[]{"uOut"});
        model.addCircuitComponent(d);

        // Resistor R at (30,17), horizontal (504)
        final CircuitModel.ComponentData r =
                new CircuitModel.ComponentData(1, "R", 30, 17, 504);
        r.setRawParameters(new double[]{LOAD_RESISTANCE});
        model.addCircuitComponent(r);

        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{20, 12}, {20, 11}, {20, 10}, {20, 9}, {20, 8}, {21, 8}, {22, 8},
                        {23, 8}, {24, 8}, {24, 9}, {24, 10}, {24, 11}, {24, 12}, {24, 13}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{24, 17}, {25, 17}, {26, 17}, {27, 17}, {28, 17}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{32, 17}, {32, 18}, {20, 18}, {20, 16}}));
        return model;
    }

    private SimulationResult run(final SemiconductorModelKind model) {
        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitModel(buildDiodeModel())
                        .stepWidth(1e-6)
                        .simulationDuration(1e-3)
                        .solverType(SolverType.SOLVER_BE)
                        .signals(List.of("uIn", "uOut"))
                        .semiconductorModel(model)
                        .build());
        assertTrue(result.isSuccess(), model + " run failed: " + result.getErrorMessage());
        return result;
    }

    /** Bisection solve of Is (exp(v/Vt) - 1) = (V_supply - v) / R_load. */
    private static double shockleyOperatingPoint() {
        final ShockleyDiodeModel diode = ShockleyDiodeModel.fromOperatingPoint(
                FORWARD_VOLTAGE, RATED_CURRENT,
                NonlinearConvergenceController.DEFAULT_EMISSION_COEFFICIENT,
                NonlinearConvergenceController.DEFAULT_THERMAL_VOLTAGE_V);
        double low = 0.0;
        double high = SUPPLY_VOLTAGE;
        for (int i = 0; i < 100; i++) {
            final double mid = 0.5 * (low + high);
            final double residual = diode.current(mid)
                    - (SUPPLY_VOLTAGE - mid) / LOAD_RESISTANCE;
            if (residual < 0.0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return 0.5 * (low + high);
    }

    @Test
    void shockleyMode_settlesAtTheAnalyticOperatingPoint() {
        final double expected = shockleyOperatingPoint();
        final double actual = diodeDrop(run(SemiconductorModelKind.SHOCKLEY_NEWTON_RAPHSON));

        assertEquals(expected, actual, 1e-4,
                "the Newton-Raphson run must settle at the Shockley operating point");
    }

    @Test
    void piecewiseLinearMode_keepsTheClassicOperatingPoint() {
        // PWL point: (v - V_f) / rOn = (V_supply - v) / R_load with rOn = 1 mOhm
        final double pwlDrop = FORWARD_VOLTAGE
                + (SUPPLY_VOLTAGE - FORWARD_VOLTAGE) * 0.001 / (LOAD_RESISTANCE + 0.001);
        final double actual = diodeDrop(run(SemiconductorModelKind.CLASSIC_PIECEWISE_LINEAR));

        assertEquals(pwlDrop, actual, 1e-4,
                "the default mode must keep the piecewise-linear operating point");
    }

    /** Diode junction voltage from the labeled anode/cathode potentials. */
    private double diodeDrop(final SimulationResult result) {
        final float[] anode = result.getSignalData(0);
        final float[] cathode = result.getSignalData(1);
        return anode[anode.length - 1] - cathode[cathode.length - 1];
    }
}
