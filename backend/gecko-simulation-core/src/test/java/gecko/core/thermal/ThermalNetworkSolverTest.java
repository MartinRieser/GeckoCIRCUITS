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
package gecko.core.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of the thermal MNA network solver: single- and multi-stage
 * transient responses against the exact analytical Foster step response,
 * steady-state heat balance, mid-ladder heat injection, integration-method
 * consistency, determinism and input validation.
 */
class ThermalNetworkSolverTest {

    /** Ambient temperature of all test networks in degrees Celsius. */
    private static final double AMBIENT_TEMPERATURE = 25.0;

    /** Constant test dissipation of the single-stage network in watts. */
    private static final double SINGLE_STAGE_POWER = 100.0;

    /** Single-stage thermal resistance in K/W. */
    private static final double SINGLE_STAGE_RESISTANCE = 0.5;

    /** Single-stage heat capacitance in J/K (time constant tau = 1 s). */
    private static final double SINGLE_STAGE_CAPACITANCE = 2.0;

    /** Time step of the fine transient runs in seconds (tau/2000). */
    private static final double FINE_STEP = 5e-4;

    /** Time step resolving the fastest pole of the four-stage runs in seconds. */
    private static final double MULTI_STAGE_STEP = 5e-6;

    /**
     * Relative tolerance of transient temperature-rise comparisons.
     *
     * <p>The shared companion models initialize the capacitor history current
     * to zero, so a heat-flow step at the simulation start is smeared over the
     * first trapezoidal step; the resulting O(dt) deficit (about
     * {@code P * dt / (2*C)} for the single-stage network) decays with the
     * system's exponential mode while the steady state stays exact. The
     * tolerance absorbs this inherited first-step artifact, which is far below
     * the error any real companion/topology/sign bug would produce.
     */
    private static final double TRANSIENT_RELATIVE_TOLERANCE = 1e-3;

    /** Absolute tolerance of steady-state temperature comparisons in K. */
    private static final double STEADY_STATE_TOLERANCE = 1e-6;

    /** Datasheet-style four-stage thermal resistances in K/W. */
    private static final double[] FOUR_STAGE_RESISTANCES = {0.05, 0.15, 0.5, 1.3};

    /** Datasheet-style four-stage exponential time constants in seconds. */
    private static final double[] FOUR_STAGE_TIME_CONSTANTS = {0.001, 0.01, 0.1, 1.0};

    /** Physical Cauer ladder heat capacitances in J/K. */
    private static final double[] FOUR_STAGE_CAPACITANCES = {0.01, 0.1, 1.0, 10.0};

    @Test
    void singleStageCauer_trzMatchesCompanionRecurrenceExactly() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);

        // Reference: the classic trapezoidal companion recurrence that the
        // MNA system implements - (G + 2C/dt) * T_n = P + 2C/dt * T_{n-1}
        // + i_{n-1} with i_n = 2C/dt * (T_n - T_{n-1}) - i_{n-1}
        final double companion = 2.0 * SINGLE_STAGE_CAPACITANCE / FINE_STEP;
        final double conductance = 1.0 / SINGLE_STAGE_RESISTANCE;
        double recurrenceTemperature = 0.0;
        double recurrenceCurrent = 0.0;
        for (int step = 0; step < 1000; step++) {
            final double nextTemperature = (SINGLE_STAGE_POWER + companion * recurrenceTemperature
                    + recurrenceCurrent) / (conductance + companion);
            recurrenceCurrent = companion * (nextTemperature - recurrenceTemperature)
                    - recurrenceCurrent;
            recurrenceTemperature = nextTemperature;
            solver.step(FINE_STEP, (step + 1) * FINE_STEP);
            assertEquals(recurrenceTemperature,
                    solver.getJunctionTemperature() - AMBIENT_TEMPERATURE, 1e-9,
                    "solver must reproduce the trapezoidal companion recurrence");
        }
    }

    @Test
    void singleStageCauer_trzTransientMatchesAnalyticResponse() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        final TransientRun run = new TransientRun(solver, FINE_STEP);

        final double tau = SINGLE_STAGE_RESISTANCE * SINGLE_STAGE_CAPACITANCE;
        for (final double sampleTime : new double[]{0.5, 1.0, 2.0, 5.0}) {
            run.advanceTo(sampleTime);
            final double expectedRise = SINGLE_STAGE_POWER * SINGLE_STAGE_RESISTANCE
                    * (1.0 - Math.exp(-sampleTime / tau));
            assertJunctionRiseMatches(solver, expectedRise);
        }
    }

    @Test
    void singleStage_steadyStateReachesAmbientPlusPowerTimesResistance() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        new TransientRun(solver, 1e-3).advanceTo(30.0);

        final double expectedSteadyState = AMBIENT_TEMPERATURE
                + SINGLE_STAGE_POWER * SINGLE_STAGE_RESISTANCE;
        assertEquals(expectedSteadyState, solver.getJunctionTemperature(), STEADY_STATE_TOLERANCE);
        assertEquals(AMBIENT_TEMPERATURE, solver.getTemperature(ThermalNode.AMBIENT), 1e-9);
        assertEquals(AMBIENT_TEMPERATURE, solver.getAmbientTemperature());
        assertEquals(solver.getTemperature(solver.getJunctionNode()),
                solver.getJunctionTemperature(), 0.0);
    }

    @Test
    void singleStage_ambientHeatFlowMatchesDissipation() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        new TransientRun(solver, 1e-3).advanceTo(30.0);

        assertEquals(SINGLE_STAGE_POWER, solver.getHeatFlowIntoAmbient(), 1e-6);
    }

    @Test
    void fourStageFoster_cauerTransformationTransientMatchesAnalyticResponse() {
        final ThermalRCModel foster = ThermalRCModel.foster(FOUR_STAGE_RESISTANCES,
                FOUR_STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);
        final ThermalNetworkSolver solver = new ThermalNetworkSolver(foster.toCauer(),
                SolverType.SOLVER_TRZ);
        solver.addHeatSource("chip", solver.getJunctionNode());
        solver.setDeviceHeatFlow("chip", 10.0);
        final TransientRun run = new TransientRun(solver, MULTI_STAGE_STEP);

        for (final double sampleTime : new double[]{0.05, 0.5, 2.0}) {
            run.advanceTo(sampleTime);
            assertJunctionRiseMatches(solver, foster.junctionStepResponse(sampleTime, 10.0)
                    - AMBIENT_TEMPERATURE);
        }
    }

    @Test
    void fourStageFoster_directFosterSimulationMatchesAnalyticResponse() {
        final ThermalRCModel foster = ThermalRCModel.foster(FOUR_STAGE_RESISTANCES,
                FOUR_STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);
        final ThermalNetworkSolver solver = new ThermalNetworkSolver(foster,
                SolverType.SOLVER_TRZ);
        solver.addHeatSource("chip", solver.getJunctionNode());
        solver.setDeviceHeatFlow("chip", 10.0);
        final TransientRun run = new TransientRun(solver, MULTI_STAGE_STEP);

        for (final double sampleTime : new double[]{0.05, 0.5, 2.0}) {
            run.advanceTo(sampleTime);
            assertJunctionRiseMatches(solver, foster.junctionStepResponse(sampleTime, 10.0)
                    - AMBIENT_TEMPERATURE);
        }
    }

    @Test
    void fourStageFoster_cauerSteadyStateMatchesSumOfResistances() {
        final ThermalRCModel foster = ThermalRCModel.foster(FOUR_STAGE_RESISTANCES,
                FOUR_STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);
        final ThermalNetworkSolver solver = new ThermalNetworkSolver(foster.toCauer(),
                SolverType.SOLVER_TRZ);
        solver.addHeatSource("chip", solver.getJunctionNode());
        solver.setDeviceHeatFlow("chip", 10.0);
        new TransientRun(solver, 0.01).advanceTo(100.0);

        double resistanceSum = 0.0;
        for (final double resistance : FOUR_STAGE_RESISTANCES) {
            resistanceSum += resistance;
        }
        assertEquals(AMBIENT_TEMPERATURE + 10.0 * resistanceSum,
                solver.getJunctionTemperature(), STEADY_STATE_TOLERANCE);
    }

    @Test
    void heatFlowStepChange_responseFollowsSuperposition() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        final TransientRun run = new TransientRun(solver, FINE_STEP);
        final double tau = SINGLE_STAGE_RESISTANCE * SINGLE_STAGE_CAPACITANCE;
        final double switchTime = 1.0;
        final double increasedPower = 300.0;

        // First segment: constant 100 W; second segment: 300 W, i.e. the
        // original step plus a 200 W step at the switch time (linearity)
        run.advanceTo(switchTime);
        solver.setDeviceHeatFlow("device", increasedPower);
        for (final double sampleTime : new double[]{1.5, 3.0}) {
            run.advanceTo(sampleTime);
            final double expectedRise = SINGLE_STAGE_RESISTANCE * (
                    SINGLE_STAGE_POWER * (1.0 - Math.exp(-sampleTime / tau))
                    + (increasedPower - SINGLE_STAGE_POWER)
                            * (1.0 - Math.exp(-(sampleTime - switchTime) / tau)));
            assertJunctionRiseMatches(solver, expectedRise);
        }
    }

    @Test
    void midLadderHeatInjection_reachesPartialResistanceSteadyState() {
        final ThermalRCModel model = ThermalRCModel.cauer(FOUR_STAGE_RESISTANCES,
                FOUR_STAGE_CAPACITANCES, AMBIENT_TEMPERATURE);
        final ThermalNetworkSolver solver = new ThermalNetworkSolver(model, SolverType.SOLVER_TRZ);
        solver.addHeatSource("periphery", ThermalNode.of(2));
        solver.setDeviceHeatFlow("periphery", 10.0);
        // 400 s = 30 time constants of the slowest stage (tau = 13 s), so the
        // residual transient is below the steady-state tolerance
        new TransientRun(solver, 0.05).advanceTo(400.0);

        // At DC no heat flows through R1, so junction and injection node share
        // the temperature; the drop to ambient spans R2..R4 only
        final double resistanceDownstream = FOUR_STAGE_RESISTANCES[1]
                + FOUR_STAGE_RESISTANCES[2] + FOUR_STAGE_RESISTANCES[3];
        assertEquals(AMBIENT_TEMPERATURE + 10.0 * resistanceDownstream,
                solver.getJunctionTemperature(), STEADY_STATE_TOLERANCE);
        assertEquals(AMBIENT_TEMPERATURE + 10.0 * FOUR_STAGE_RESISTANCES[3],
                solver.getTemperature(ThermalNode.of(4)), STEADY_STATE_TOLERANCE);
    }

    @Test
    void initialTemperatureOverride_decaysBackToAmbient() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        final double preheatedTemperature = 125.0;
        solver.setDeviceHeatFlow("device", 0.0);
        solver.setInitialTemperature(solver.getJunctionNode(), preheatedTemperature);
        assertEquals(preheatedTemperature, solver.getJunctionTemperature(), 1e-9);

        final TransientRun run = new TransientRun(solver, FINE_STEP);
        final double tau = SINGLE_STAGE_RESISTANCE * SINGLE_STAGE_CAPACITANCE;
        for (final double sampleTime : new double[]{0.5, 2.0}) {
            run.advanceTo(sampleTime);
            final double expectedRise = (preheatedTemperature - AMBIENT_TEMPERATURE)
                    * Math.exp(-sampleTime / tau);
            assertJunctionRiseMatches(solver, expectedRise);
        }
    }

    @Test
    void backwardEulerAndTrapezoidal_firstOrderVersusSecondOrderAccuracy() {
        // Both methods carry an O(dt) transient error for a heat-flow step
        // (first-order truncation for backward Euler, first-step jump
        // smearing for the trapezoidal companion); the backward Euler error
        // grows with the elapsed time while the trapezoidal artifact decays,
        // so the comparison runs at t = 5 where the margin is a factor of 5
        final ThermalNetworkSolver backwardEuler = singleStageCauerSolver(SolverType.SOLVER_BE);
        final ThermalNetworkSolver trapezoidal = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        final TransientRun backwardEulerRun = new TransientRun(backwardEuler, FINE_STEP);
        final TransientRun trapezoidalRun = new TransientRun(trapezoidal, FINE_STEP);
        backwardEulerRun.advanceTo(5.0);
        trapezoidalRun.advanceTo(5.0);

        final double tau = SINGLE_STAGE_RESISTANCE * SINGLE_STAGE_CAPACITANCE;
        final double expectedRise = SINGLE_STAGE_POWER * SINGLE_STAGE_RESISTANCE
                * (1.0 - Math.exp(-5.0 / tau));
        final double backwardEulerError = Math.abs(
                backwardEuler.getJunctionTemperature() - AMBIENT_TEMPERATURE - expectedRise);
        final double trapezoidalError = Math.abs(
                trapezoidal.getJunctionTemperature() - AMBIENT_TEMPERATURE - expectedRise);
        assertTrue(backwardEulerError > 2.0 * trapezoidalError,
                "first-order backward Euler error " + backwardEulerError
                        + " must exceed the trapezoidal error " + trapezoidalError);
        assertTrue(backwardEulerError < 0.05 * expectedRise,
                "backward Euler transient must stay within a 5 percent band");

        // 30 s = 30 time constants: the decayed mode is fully dead, so the
        // backward Euler fixed point equals the analytic steady state exactly
        backwardEulerRun.advanceTo(30.0);
        assertEquals(AMBIENT_TEMPERATURE + SINGLE_STAGE_POWER * SINGLE_STAGE_RESISTANCE,
                backwardEuler.getJunctionTemperature(), STEADY_STATE_TOLERANCE);
    }

    @Test
    void identicalConfigs_produceIdenticalTrajectories() {
        final ThermalNetworkSolver first = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        final ThermalNetworkSolver second = singleStageCauerSolver(SolverType.SOLVER_TRZ);

        for (int step = 0; step < 2000; step++) {
            first.step(FINE_STEP, (step + 1) * FINE_STEP);
            second.step(FINE_STEP, (step + 1) * FINE_STEP);
            if (step % 100 == 0) {
                assertEquals(first.getJunctionTemperature(), second.getJunctionTemperature(), 0.0,
                        "identical configs must produce bit-identical temperatures");
                assertEquals(first.getHeatFlowIntoAmbient(), second.getHeatFlowIntoAmbient(), 0.0);
            }
        }
    }

    @Test
    void netlist_exposesModelStructureForLogging() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);

        assertEquals(2, solver.getThermalNodeCount(), "ambient node plus one ladder node");
        // 1 resistance + 1 capacitance + 1 ambient source + 1 heat flow source
        assertEquals(4, solver.getNetlist().getElementCount());
        assertEquals(CircuitTypCore.TH_TEMP, solver.getNetlist().getType(2));
        assertEquals(CircuitTypCore.TH_FLOW, solver.getNetlist().getType(3));
    }

    @Test
    void constructor_rejectsNullModelAndNullSolverType() {
        final ThermalRCModel model = ThermalRCModel.cauer(new double[]{SINGLE_STAGE_RESISTANCE},
                new double[]{SINGLE_STAGE_CAPACITANCE}, AMBIENT_TEMPERATURE);
        assertThrows(IllegalArgumentException.class,
                () -> new ThermalNetworkSolver(null, SolverType.SOLVER_TRZ));
        assertThrows(IllegalArgumentException.class,
                () -> new ThermalNetworkSolver(model, null));
    }

    @Test
    void step_rejectsInvalidTimeStep() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);
        assertThrows(IllegalArgumentException.class, () -> solver.step(0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> solver.step(-FINE_STEP, 0.0));
        assertThrows(IllegalArgumentException.class, () -> solver.step(Double.NaN, 0.0));
    }

    @Test
    void heatSources_rejectInvalidRegistrationsAndUpdates() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);

        assertThrows(IllegalArgumentException.class, () -> solver.addHeatSource(null,
                solver.getJunctionNode()));
        assertThrows(IllegalArgumentException.class, () -> solver.addHeatSource("  ",
                solver.getJunctionNode()));
        assertThrows(IllegalArgumentException.class, () -> solver.addHeatSource("other",
                ThermalNode.of(99)));
        assertThrows(IllegalArgumentException.class, () -> solver.addHeatSource("other", null));
        assertThrows(IllegalArgumentException.class, () -> solver.setDeviceHeatFlow("unknown", 1.0));
        assertThrows(IllegalArgumentException.class, () -> solver.setDeviceHeatFlow("device",
                Double.NaN));
        // 'device' is already registered by the factory helper
        assertThrows(IllegalArgumentException.class, () -> solver.addHeatSource("device",
                solver.getJunctionNode()));
    }

    @Test
    void getTemperature_rejectsUnknownAndNullNodes() {
        final ThermalNetworkSolver solver = singleStageCauerSolver(SolverType.SOLVER_TRZ);

        assertThrows(IllegalArgumentException.class, () -> solver.getTemperature(null));
        assertThrows(IllegalArgumentException.class, () -> solver.getTemperature(ThermalNode.of(99)));
        assertThrows(IllegalArgumentException.class,
                () -> solver.setInitialTemperature(ThermalNode.of(99), 50.0));
        assertThrows(IllegalArgumentException.class,
                () -> solver.setInitialTemperature(solver.getJunctionNode(), Double.NaN));
    }

    /**
     * Creates a single-stage Cauer solver with a junction heat source at
     * {@link #SINGLE_STAGE_POWER} watts.
     *
     * @param solverType integration method of the solver
     * @return configured thermal network solver
     */
    private static ThermalNetworkSolver singleStageCauerSolver(final SolverType solverType) {
        final ThermalRCModel model = ThermalRCModel.cauer(
                new double[]{SINGLE_STAGE_RESISTANCE},
                new double[]{SINGLE_STAGE_CAPACITANCE}, AMBIENT_TEMPERATURE);
        final ThermalNetworkSolver solver = new ThermalNetworkSolver(model, solverType);
        solver.addHeatSource("device", solver.getJunctionNode());
        solver.setDeviceHeatFlow("device", SINGLE_STAGE_POWER);
        return solver;
    }

    /**
     * Steps a solver with a fixed step width up to absolute target times.
     * Internally counts steps, so successive {@link #advanceTo(double)} calls
     * continue exactly where the previous one stopped without floating-point
     * time accumulation.
     */
    private static final class TransientRun {
        private final ThermalNetworkSolver solver;
        private final double dt;
        private int stepsDone;

        /**
         * Creates a transient run driver.
         *
         * @param solver thermal solver to advance
         * @param dt fixed step width in seconds
         */
        TransientRun(final ThermalNetworkSolver solver, final double dt) {
            this.solver = solver;
            this.dt = dt;
        }

        /**
         * Steps until the total simulated time reaches the target (which must
         * be an integer multiple of the step width).
         *
         * @param targetTime absolute target simulation time in seconds
         */
        void advanceTo(final double targetTime) {
            final int targetSteps = (int) Math.round(targetTime / dt);
            for (; stepsDone < targetSteps; stepsDone++) {
                solver.step(dt, (stepsDone + 1) * dt);
            }
        }
    }

    /**
     * Asserts that the junction temperature rise above ambient matches the
     * expected value within the relative transient tolerance.
     *
     * @param solver thermal solver to read
     * @param expectedRise expected temperature rise above ambient in K
     */
    private static void assertJunctionRiseMatches(final ThermalNetworkSolver solver,
                                                  final double expectedRise) {
        final double actualRise = solver.getJunctionTemperature() - AMBIENT_TEMPERATURE;
        assertEquals(expectedRise, actualRise,
                Math.abs(expectedRise) * TRANSIENT_RELATIVE_TOLERANCE,
                "junction temperature rise must match the analytic response");
    }
}
