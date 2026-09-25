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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.thermal.ThermalCoupling;
import gecko.core.thermal.ThermalRCModel;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Engine-level verification of the closed electro-thermal loop (Task L1
 * Step 2): device dissipation feeds the thermal RC network, and the solved
 * junction temperature feeds back into the electrical parameters until the
 * coupled system reaches its analytic thermal-electrical equilibrium.
 *
 * <p>Covers: resistor self-heating with a positive temperature coefficient
 * (current decreases until equilibrium), diode self-heating with the
 * negative forward-voltage temperature coefficient, and configuration-error
 * handling for unknown coupling devices.
 */
class ElectroThermalFeedbackTest {

    /** Supply voltage of the divider circuit in volts. */
    private static final double SUPPLY_VOLTAGE = 10.0;

    /** Cold resistance of both divider resistors in ohms. */
    private static final double RESISTANCE = 10.0;

    /** Copper-like resistance temperature coefficient in 1/K. */
    private static final double ALPHA_RESISTOR = 3.9e-3;

    /** Thermal resistance of the single-stage networks in K/W. */
    private static final double THERMAL_RESISTANCE = 10.0;

    /** Heat capacitance of the single-stage networks in J/K (tau = 10 ms). */
    private static final double THERMAL_CAPACITANCE = 1e-3;

    /** Ambient temperature in degrees Celsius. */
    private static final double AMBIENT_TEMPERATURE = 25.0;

    /** Electrical time step of the coupled runs in seconds. */
    private static final double TIME_STEP = 1e-4;

    /** Simulation duration (100 thermal time constants) in seconds. */
    private static final double DURATION = 1.0;

    /** Absolute tolerance of converged electrical quantities. */
    private static final double CONVERGED_TOLERANCE = 1e-4;

    /** Absolute tolerance of converged junction temperatures in K. */
    private static final double TEMPERATURE_TOLERANCE = 1e-3;

    @Test
    void resistorSelfHeating_convergesToAnalyticEquilibrium() {
        final CircuitModel model = buildDividerModel();
        final ThermalRCModel thermal = ThermalRCModel.cauer(new double[]{THERMAL_RESISTANCE},
                new double[]{THERMAL_CAPACITANCE}, AMBIENT_TEMPERATURE);

        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitModel(model)
                        .stepWidth(TIME_STEP)
                        .simulationDuration(DURATION)
                        .solverType(SolverType.SOLVER_BE)
                        .signals(List.of("uMid"))
                        .enableThermalDomain(true)
                        .thermalCoupling(ThermalCoupling.forResistor("R2", thermal, ALPHA_RESISTOR))
                        .build());
        assertTrue(result.isSuccess(), "run failed: " + result.getErrorMessage());

        // Analytic equilibrium: T_j = T_amb + P(T_j) * R_th with the
        // temperature-dependent divider dissipation P(T) = V^2*R1^2 / ((R1+R2(T))^2 * R2(T))
        final double equilibriumRise = bisect(rise -> {
            final double r2 = RESISTANCE * (1.0 + ALPHA_RESISTOR * rise);
            final double vR2 = SUPPLY_VOLTAGE * r2 / (RESISTANCE + r2);
            return THERMAL_RESISTANCE * vR2 * vR2 / r2 - rise;
        }, 0.0, 100.0);
        final double r2Equilibrium = RESISTANCE * (1.0 + ALPHA_RESISTOR * equilibriumRise);
        final double expectedMidVoltage = SUPPLY_VOLTAGE * r2Equilibrium / (RESISTANCE + r2Equilibrium);

        final float[] midVoltage = result.getSignalData(0);
        final int lastRow = result.getDataContainer().getMaximumTimeIndex(0);
        assertEquals(expectedMidVoltage, midVoltage[lastRow], CONVERGED_TOLERANCE,
                "divider midpoint must converge to the electro-thermal equilibrium");
        assertTrue(midVoltage[lastRow] > SUPPLY_VOLTAGE / 2.0 + 0.1,
                "self-heating must shift the divider midpoint up (R2 grows)");

        final double expectedJunction = AMBIENT_TEMPERATURE + equilibriumRise;
        assertEquals(expectedJunction, (Double) result.getMetadata().get("maxJunctionTemperature"),
                TEMPERATURE_TOLERANCE, "junction temperature must reach the analytic equilibrium");
    }

    @Test
    void diodeSelfHeating_negativeTemperatureCoefficientReducesLoss() {
        final CircuitModel model = buildDiodeLoopModel();
        final ThermalRCModel thermal = ThermalRCModel.cauer(new double[]{2.0},
                new double[]{THERMAL_CAPACITANCE}, AMBIENT_TEMPERATURE);
        final double forwardVoltageSlope = 1e-3;

        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitModel(model)
                        .stepWidth(TIME_STEP)
                        .simulationDuration(0.5)
                        .solverType(SolverType.SOLVER_BE)
                        .signals(List.of("uDp", "uDm"))
                        .enableThermalDomain(true)
                        .thermalCoupling(ThermalCoupling.forDiode("D", thermal, forwardVoltageSlope))
                        .build());
        assertTrue(result.isSuccess(), "run failed: " + result.getErrorMessage());

        // Analytic equilibrium of the 1 A diode loop: P = (V_f0 - k_T*dT + I*R_on)*I
        // and dT = P * R_th, solved in closed form
        final double diodeCurrent = 1.0;
        final double onResistance = 0.01;
        final double thermalResistance = 2.0;
        final double coldLoss = (0.7 + diodeCurrent * onResistance) * diodeCurrent;
        final double equilibriumRise = thermalResistance * coldLoss
                / (1.0 + thermalResistance * diodeCurrent * forwardVoltageSlope);
        final double expectedLoss = coldLoss - forwardVoltageSlope * equilibriumRise;

        final float[] anodePotential = result.getSignalData(0);
        final float[] cathodePotential = result.getSignalData(1);
        final int lastRow = result.getDataContainer().getMaximumTimeIndex(0);
        final double diodeDrop = Math.abs(
                (double) anodePotential[lastRow] - cathodePotential[lastRow]);
        // The diode drop equals V_f(T*) + I*R_on regardless of which loop
        // node the reference pinning selects
        assertEquals(expectedLoss / diodeCurrent, diodeDrop, CONVERGED_TOLERANCE,
                "diode drop must converge to the electro-thermal equilibrium");
        assertTrue(diodeDrop < coldLoss / diodeCurrent,
                "negative forward-voltage temperature coefficient must reduce the "
                        + "conduction loss as the junction heats up");

        final double expectedJunction = AMBIENT_TEMPERATURE + equilibriumRise;
        assertEquals(expectedJunction, (Double) result.getMetadata().get("maxJunctionTemperature"),
                TEMPERATURE_TOLERANCE, "junction temperature must reach the analytic equilibrium");
    }

    @Test
    void unknownCouplingDevice_failsRunWithExplanation() {
        final CircuitModel model = buildDividerModel();
        final ThermalRCModel thermal = ThermalRCModel.cauer(new double[]{THERMAL_RESISTANCE},
                new double[]{THERMAL_CAPACITANCE}, AMBIENT_TEMPERATURE);

        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitModel(model)
                        .stepWidth(TIME_STEP)
                        .simulationDuration(DURATION)
                        .enableThermalDomain(true)
                        .thermalCoupling(ThermalCoupling.forResistor("Missing", thermal, 0.0))
                        .build());

        assertFalse(result.isSuccess(), "coupling to an unknown device must fail the run");
        assertTrue(result.getErrorMessage().contains("Missing"),
                "error message must name the unknown device: " + result.getErrorMessage());
    }

    /**
     * Builds the resistive voltage divider: 10 V source, fixed R1 = 10 ohm,
     * self-heating R2 = 10 ohm whose upper terminal is labeled "uMid".
     */
    private static CircuitModel buildDividerModel() {
        final CircuitModel model = new CircuitModel();

        final CircuitModel.ComponentData u = new CircuitModel.ComponentData(4, "U", 20, 14, 503);
        final double[] uParams = new double[21];
        uParams[0] = 401.0;
        uParams[1] = SUPPLY_VOLTAGE;
        u.setRawParameters(uParams);
        model.addCircuitComponent(u);

        final CircuitModel.ComponentData r1 = new CircuitModel.ComponentData(1, "R1", 18, 9, 504);
        r1.setRawParameters(new double[]{RESISTANCE});
        model.addCircuitComponent(r1);

        final CircuitModel.ComponentData r2 = new CircuitModel.ComponentData(1, "R2", 12, 9, 504);
        r2.setRawParameters(new double[]{RESISTANCE});
        r2.setTerminalXLabels(new String[]{"uMid"});
        model.addCircuitComponent(r2);

        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{20, 12}, {20, 11}, {20, 10}, {20, 9}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{16, 9}, {15, 9}, {14, 9}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{10, 9}, {10, 10}, {10, 11}, {10, 12}, {10, 13}, {10, 14}, {10, 15},
                        {10, 16}, {11, 16}, {12, 16}, {13, 16}, {14, 16}, {15, 16}, {16, 16},
                        {17, 16}, {18, 16}, {19, 16}, {20, 16}}));
        return model;
    }

    /**
     * Builds the diode loop: 1 A DC current source driving the diode in
     * forward direction, so the diode dissipates (V_f + I*R_on)*I.
     */
    private static CircuitModel buildDiodeLoopModel() {
        final CircuitModel model = new CircuitModel();

        final CircuitModel.ComponentData i = new CircuitModel.ComponentData(5, "I", 20, 14, 503);
        final double[] iParams = new double[21];
        iParams[0] = 401.0;
        iParams[1] = 1.0;
        i.setRawParameters(iParams);
        model.addCircuitComponent(i);

        final CircuitModel.ComponentData d = new CircuitModel.ComponentData(6, "D", 14, 9, 504);
        d.setRawParameters(new double[]{0.01, 0.7, 0.01, 1e7});
        d.setTerminalXLabels(new String[]{"uDp"});
        d.setTerminalYLabels(new String[]{"uDm"});
        model.addCircuitComponent(d);

        // The two wires must not touch each other (or the opposite
        // terminals), otherwise the loop shorts and the diode carries no
        // current: the return path runs below/around the source lane
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{20, 16}, {20, 17}, {20, 18}, {19, 18}, {18, 18}, {17, 18}, {16, 18},
                        {15, 18}, {14, 18}, {13, 18}, {12, 18}, {11, 18}, {10, 18}, {10, 17},
                        {10, 16}, {10, 15}, {10, 14}, {10, 13}, {10, 12}, {10, 11}, {10, 10},
                        {10, 9}, {10, 8}, {10, 7}, {11, 7}, {12, 7}, {13, 7}, {14, 7}, {15, 7},
                        {16, 7}, {16, 8}, {16, 9}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{12, 9}, {12, 10}, {12, 11}, {12, 12}, {13, 12}, {14, 12}, {15, 12},
                        {16, 12}, {17, 12}, {18, 12}, {19, 12}, {20, 12}}));
        return model;
    }

    /**
     * Solves a scalar equilibrium equation by bisection. The function must be
     * positive at the lower bound and negative at the upper bound.
     *
     * @param function equilibrium residual function
     * @param lowerBound lower bracket of the solution
     * @param upperBound upper bracket of the solution
     * @return root of the function within the bisection tolerance
     */
    private static double bisect(final java.util.function.DoubleUnaryOperator function,
                                 final double lowerBound, final double upperBound) {
        final double tolerance = 1e-10;
        double low = lowerBound;
        double high = upperBound;
        while (high - low > tolerance) {
            final double mid = 0.5 * (low + high);
            if (function.applyAsDouble(mid) > 0.0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return 0.5 * (low + high);
    }
}
