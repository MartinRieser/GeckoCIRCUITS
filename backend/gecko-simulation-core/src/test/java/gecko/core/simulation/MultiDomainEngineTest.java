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
import static org.junit.jupiter.api.Assertions.assertTrue;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.magnetic.MagneticNetworkSolver;
import gecko.core.magnetic.MagneticWinding;
import gecko.core.magnetic.MagneticNode;
import gecko.core.magnetic.NonlinearReluctance;
import gecko.core.magnetic.NonlinearReluctance.CurveKind;
import gecko.core.thermal.ThermalCoupling;
import gecko.core.thermal.ThermalRCModel;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of the multi-domain engine (Task L1 Step 4): one
 * run couples all three domains - the electrical loop drives a saturating
 * choke whose inductance the magnetic reluctance network updates to the
 * differential inductance of the solved operating point (the induced EMF
 * {@code N * dPhi/dt = L_diff * di/dt} acts through the inductor companion),
 * while the series resistor feeds its ohmic losses into a thermal RC network
 * whose junction temperature feeds back into the resistance.
 *
 * <p>The coupled steady state is analytic: the fully saturated choke releases
 * its EMF, so the current settles at {@code U/R(T*)} with the electro-thermal
 * equilibrium temperature, and the core flux follows the saturation curve at
 * {@code F = N * i*}. Thermal, magnetic and electrical channels are logged
 * simultaneously from the same data container.
 */
class MultiDomainEngineTest {

    /** Supply voltage of the coupled loop in volts. */
    private static final double SUPPLY_VOLTAGE = 12.0;

    /** Cold resistance of the self-heating series resistor in ohms. */
    private static final double RESISTANCE = 6.0;

    /** Copper-like resistance temperature coefficient in 1/K. */
    private static final double ALPHA_RESISTOR = 3.9e-3;

    /** Thermal resistance of the resistor's heat path in K/W. */
    private static final double THERMAL_RESISTANCE = 2.0;

    /** Heat capacitance of the resistor's heat path in J/K (tau = 10 ms). */
    private static final double THERMAL_CAPACITANCE = 5e-3;

    /** Ambient temperature in degrees Celsius. */
    private static final double AMBIENT_TEMPERATURE = 25.0;

    /** Winding turn count of the saturable choke. */
    private static final int WINDING_TURNS = 50;

    /** Unsaturated permeance of the choke core in henrys. */
    private static final double CORE_PERMEANCE = 1e-3;

    /** Saturation flux of the choke core in webers. */
    private static final double SATURATION_FLUX = 1.5e-2;

    /** Electrical time step in seconds. */
    private static final double TIME_STEP = 1e-4;

    /** Simulation duration in seconds (the coupled equilibrium is reached). */
    private static final double DURATION = 0.5;

    /** Absolute tolerance of converged channel comparisons. */
    private static final double CONVERGED_TOLERANCE = 1e-4;

    @Test
    void threeDomains_convergeToTheCoupledEquilibriumWithSimultaneousLogging() {
        final MagneticNetworkSolver magnetic = new MagneticNetworkSolver(SolverType.SOLVER_BE);
        magnetic.addWinding(new MagneticWinding("L1", WINDING_TURNS,
                MagneticNode.of(1), MagneticNode.REFERENCE));
        magnetic.addNonlinearReluctance("core", MagneticNode.of(1), MagneticNode.REFERENCE,
                NonlinearReluctance.of(CurveKind.TANH, CORE_PERMEANCE, SATURATION_FLUX));

        final ThermalRCModel heatsink = ThermalRCModel.cauer(
                new double[]{THERMAL_RESISTANCE}, new double[]{THERMAL_CAPACITANCE},
                AMBIENT_TEMPERATURE);

        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitModel(buildCoupledLoopModel())
                        .stepWidth(TIME_STEP)
                        .simulationDuration(DURATION)
                        .solverType(SolverType.SOLVER_BE)
                        .signals(List.of("uMid", "Tj_R1", "Phi_L1"))
                        .enableThermalDomain(true)
                        .thermalCoupling(ThermalCoupling.forResistor("R1", heatsink,
                                ALPHA_RESISTOR))
                        .enableMagneticDomain(true)
                        .magneticNetwork(magnetic)
                        .build());
        assertTrue(result.isSuccess(), "run failed: " + result.getErrorMessage());

        // coupled steady state: the saturated choke releases its EMF, so the
        // current settles at U/R(T*) with the electro-thermal equilibrium
        final double equilibriumRise = bisect(rise ->
                SUPPLY_VOLTAGE * SUPPLY_VOLTAGE * THERMAL_RESISTANCE
                        / (RESISTANCE * (1.0 + ALPHA_RESISTOR * rise)) - rise, 0.0, 500.0);
        final double equilibriumResistance = RESISTANCE * (1.0 + ALPHA_RESISTOR * equilibriumRise);
        final double equilibriumCurrent = SUPPLY_VOLTAGE / equilibriumResistance;
        final double equilibriumTemperature = AMBIENT_TEMPERATURE + equilibriumRise;
        final double equilibriumFlux = SATURATION_FLUX
                * Math.tanh(CORE_PERMEANCE * WINDING_TURNS * equilibriumCurrent / SATURATION_FLUX);

        final float[] midVoltage = result.getSignalData(0);
        final float[] junctionTemperature = result.getSignalData(1);
        final float[] windingFlux = result.getSignalData(2);
        final int lastRow = result.getDataContainer().getMaximumTimeIndex(0);
        assertEquals(lastRow, result.getDataContainer().getMaximumTimeIndex(1),
                "all domain channels must be logged over the same grid");
        assertEquals(lastRow, result.getDataContainer().getMaximumTimeIndex(2),
                "all domain channels must be logged over the same grid");

        // at the fixed point the choke voltage vanishes (dPhi/dt = 0)
        assertEquals(0.0, midVoltage[lastRow], CONVERGED_TOLERANCE,
                "the saturated choke must release its EMF at equilibrium");
        assertEquals(equilibriumTemperature, junctionTemperature[lastRow], 1e-3,
                "junction temperature must reach the electro-thermal equilibrium");
        assertEquals(equilibriumFlux, windingFlux[lastRow],
                1e-4 * SATURATION_FLUX,
                "core flux must follow the saturation curve at the equilibrium current");

        // saturation evidence: the equilibrium current is far beyond the knee
        final double kneeCurrent = SATURATION_FLUX / (CORE_PERMEANCE * WINDING_TURNS);
        assertTrue(equilibriumCurrent > 5.0 * kneeCurrent,
                "the coupled equilibrium must drive the core deep into saturation");

        assertEquals(equilibriumTemperature,
                (Double) result.getMetadata().get("maxJunctionTemperature"), 1e-3);
        assertEquals(Math.abs(equilibriumFlux),
                (Double) result.getMetadata().get("peakFlux"), 1e-4 * SATURATION_FLUX,
                "peak flux must match the converged core flux (monotone charge)");
    }

    @Test
    void magneticDomain_disabledByDefault() {
        final CircuitModel model = buildCoupledLoopModel();

        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitModel(model)
                        .stepWidth(TIME_STEP)
                        .simulationDuration(0.01)
                        .solverType(SolverType.SOLVER_BE)
                        .signals(List.of("uMid"))
                        .build());
        assertTrue(result.isSuccess(), "run failed: " + result.getErrorMessage());
        assertEquals(0.0, (Double) result.getMetadata().get("peakFlux"), 0.0,
                "no magnetic network configured - no flux");

        // without the magnetic domain the choke keeps its plain inductance
        final float[] midVoltage = result.getSignalData(0);
        assertTrue(Math.abs(midVoltage[midVoltage.length - 1]) > 1e-3,
                "a linear 2.5 H choke with tau = 0.42 s must still carry voltage after 10 ms");
    }

    @Test
    void magneticWindingOnNonInductor_failsRunWithExplanation() {
        final CircuitModel model = buildCoupledLoopModel();
        final MagneticNetworkSolver magnetic = new MagneticNetworkSolver(SolverType.SOLVER_BE);
        magnetic.addWinding(new MagneticWinding("R1", WINDING_TURNS,
                MagneticNode.of(1), MagneticNode.REFERENCE));
        magnetic.addNonlinearReluctance("core", MagneticNode.of(1), MagneticNode.REFERENCE,
                NonlinearReluctance.of(CurveKind.TANH, CORE_PERMEANCE, SATURATION_FLUX));

        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitModel(model)
                        .stepWidth(TIME_STEP)
                        .simulationDuration(0.01)
                        .enableMagneticDomain(true)
                        .magneticNetwork(magnetic)
                        .build());

        assertTrue(!result.isSuccess(), "winding on a resistor must fail the run");
        assertTrue(result.getErrorMessage().contains("LK_L"),
                "error message must name the expected element type: "
                        + result.getErrorMessage());
    }

    /**
     * Builds the coupled loop: 12 V source, self-heating 6 ohm resistor and
     * the EMF-carrying choke inductor "L1" whose terminal is labeled "uMid".
     */
    private static CircuitModel buildCoupledLoopModel() {
        final CircuitModel model = new CircuitModel();

        final CircuitModel.ComponentData u = new CircuitModel.ComponentData(4, "U1", 20, 14, 503);
        final double[] uParams = new double[21];
        uParams[0] = 401.0;
        uParams[1] = SUPPLY_VOLTAGE;
        u.setRawParameters(uParams);
        model.addCircuitComponent(u);

        final CircuitModel.ComponentData r = new CircuitModel.ComponentData(1, "R1", 18, 9, 504);
        r.setRawParameters(new double[]{RESISTANCE});
        model.addCircuitComponent(r);

        final CircuitModel.ComponentData l = new CircuitModel.ComponentData(2, "L1", 12, 9, 504);
        l.setRawParameters(new double[]{WINDING_TURNS * WINDING_TURNS * CORE_PERMEANCE});
        l.setTerminalXLabels(new String[]{"uMid"});
        model.addCircuitComponent(l);

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
     * Solves a scalar equilibrium equation by bisection; the function must be
     * positive at the lower and negative at the upper bracket.
     *
     * @param function equilibrium residual function
     * @param lowerBound lower bracket
     * @param upperBound upper bracket
     * @return root of the function
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
