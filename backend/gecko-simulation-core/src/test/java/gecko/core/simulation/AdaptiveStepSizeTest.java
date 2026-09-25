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
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine-level verification of adaptive step-size control on a passive RC
 * charging transient (no switching events, no CONTROL domain - the legacy
 * control blocks are fixed-dt oriented, so switched-mode circuits with gate
 * drive are out of scope for adaptive accuracy comparisons).
 *
 * <p>Covers: LTE-driven refinement improving accuracy over the coarse fixed
 * step, transparency of the machinery when the tolerance never rejects
 * (adaptive with a huge tolerance must reproduce the fixed run), grid-aligned
 * logging, and determinism.
 */
class AdaptiveStepSizeTest {

    /** Base (coarse) step width the adaptive controller refines within. */
    private static final double COARSE_STEP = 4e-6;

    /** Fine fixed reference step width for the interpolated reference. */
    private static final double FINE_STEP = 0.25e-6;

    /** Smallest width the adaptive controller may shrink to. */
    private static final double ADAPTIVE_MIN_STEP = 1e-7;

    /** Tolerance so large the error test never rejects (transparency check). */
    private static final double NEVER_REJECTING_TOLERANCE = 1e6;

    private static final double DURATION = 500e-6;

    /** Window (in coarse rows) over which the transient accuracy is judged. */
    private static final int ACCURACY_WINDOW_ROWS = 40;

    private SimulationResult run(final SimulationConfig config) {
        final SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), "run failed: " + result.getErrorMessage());
        return result;
    }

    private SimulationConfig.Builder baseBuilder() {
        return SimulationConfig.builder()
                .circuitModel(buildRcModel())
                .simulationDuration(DURATION)
                .solverType(SolverType.SOLVER_BE)
                .signals(List.of("uOut"));
    }

    /**
     * Passive RC charging circuit (10 V source, 1 kOhm, 100 nF, tau = 100 us)
     * with the capacitor voltage labeled "uOut".
     */
    private CircuitModel buildRcModel() {
        final CircuitModel model = new CircuitModel();

        // Voltage source U at (20,14), vertical (orientation 503)
        final CircuitModel.ComponentData u = new CircuitModel.ComponentData(4, "U", 20, 14, 503);
        final double[] uParams = new double[21];
        uParams[0] = 401.0;   // DC source
        uParams[1] = 10.0;    // 10 V
        u.setRawParameters(uParams);
        model.addCircuitComponent(u);

        // Resistor R at (18,9), horizontal (504)
        final CircuitModel.ComponentData r = new CircuitModel.ComponentData(1, "R", 18, 9, 504);
        r.setRawParameters(new double[]{1000.0});
        model.addCircuitComponent(r);

        // Capacitor C at (27,15), vertical; + terminal labeled uOut
        final CircuitModel.ComponentData c = new CircuitModel.ComponentData(3, "C", 27, 15, 503);
        c.setRawParameters(new double[]{100e-9, 0.0});
        c.setTerminalXLabels(new String[]{"uOut"});
        model.addCircuitComponent(c);

        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{20, 12}, {20, 11}, {20, 10}, {20, 9}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{16, 9}, {16, 8}, {17, 8}, {18, 8}, {19, 8}, {20, 8}, {21, 8},
                        {22, 8}, {23, 8}, {24, 8}, {25, 8}, {26, 8}, {27, 8}, {27, 9},
                        {27, 10}, {27, 11}, {27, 12}, {27, 13}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{27, 17}, {26, 17}, {25, 17}, {24, 17}, {23, 17}, {22, 17},
                        {21, 17}, {20, 17}, {20, 16}}));
        return model;
    }

    /** Linear interpolation of the fine reference at the requested time. */
    private static double interpolate(final double[] times, final float[] values, final double time) {
        int i = 1;
        while (i < times.length - 1 && times[i] < time) {
            i++;
        }
        final double t0 = times[i - 1];
        final double t1 = times[i];
        final double fraction = t1 > t0 ? (time - t0) / (t1 - t0) : 0.0;
        return values[i - 1] + fraction * (values[i] - values[i - 1]);
    }

    /** Maximum deviation of a run's uOut rows from the interpolated reference. */
    private double maxDeviationFromReference(final SimulationResult result,
                                             final double[] referenceTimes,
                                             final float[] referenceValues) {
        final float[] data = result.getSignalData(0);
        final int rows = result.getDataContainer().getMaximumTimeIndex(0) + 1;
        double maxDeviation = 0.0;
        for (int row = 1; row <= Math.min(ACCURACY_WINDOW_ROWS, rows - 1); row++) {
            final double time = row * COARSE_STEP;
            final double reference = interpolate(referenceTimes, referenceValues, time);
            maxDeviation = Math.max(maxDeviation, Math.abs(data[row] - reference));
        }
        return maxDeviation;
    }

    @Test
    void adaptiveRun_shrinksDuringFastTransient_andImprovesAccuracy() {
        final SimulationResult coarse = run(baseBuilder().stepWidth(COARSE_STEP).build());
        final SimulationResult fine = run(baseBuilder().stepWidth(FINE_STEP).build());
        final SimulationResult adaptive = run(baseBuilder().stepWidth(COARSE_STEP)
                .adaptiveStepSize(true).minStepWidth(ADAPTIVE_MIN_STEP).build());

        assertEquals(Boolean.TRUE, adaptive.getMetadata().get("adaptive"));
        assertTrue((Integer) adaptive.getMetadata().get("rejectedSteps") > 0,
                "the fast start of the RC transient must trigger rejected steps");
        assertTrue(adaptive.getTotalTimeSteps() > coarse.getTotalTimeSteps(),
                "adaptive mode must take extra refinement steps");
        assertEquals(coarse.getTotalTimeSteps() - 1,
                adaptive.getDataContainer().getMaximumTimeIndex(0),
                "adaptive mode must log the full uniform base grid");

        // Accuracy: the adaptive rows track the fine reference (interpolated
        // at the coarse grid times) closer than the coarse fixed rows
        final double[] fineTimes = new double[fine.getDataContainer().getMaximumTimeIndex(0) + 1];
        for (int i = 0; i < fineTimes.length; i++) {
            fineTimes[i] = i * FINE_STEP;
        }
        final float[] fineValues = fine.getSignalData(0);
        final double adaptiveDeviation = maxDeviationFromReference(adaptive, fineTimes, fineValues);
        final double coarseDeviation = maxDeviationFromReference(coarse, fineTimes, fineValues);
        assertTrue(adaptiveDeviation < coarseDeviation,
                "adaptive deviation " + adaptiveDeviation
                        + " must beat the coarse deviation " + coarseDeviation);
    }

    @Test
    void adaptiveRun_neverRejectingTolerance_reproducesFixedRun() {
        final SimulationResult coarse = run(baseBuilder().stepWidth(COARSE_STEP).build());
        final SimulationResult adaptive = run(baseBuilder().stepWidth(COARSE_STEP)
                .adaptiveStepSize(true).relativeTolerance(NEVER_REJECTING_TOLERANCE).build());

        assertEquals(coarse.getTotalTimeSteps(), adaptive.getTotalTimeSteps(),
                "without rejections the step trajectory must match the fixed run");
        assertArrayEquals(coarse.getSignalData(0), adaptive.getSignalData(0),
                "the adaptive machinery must be transparent when dt never changes");
    }

    @Test
    void adaptiveRun_isDeterministic() {
        final SimulationResult first = run(baseBuilder().stepWidth(COARSE_STEP)
                .adaptiveStepSize(true).minStepWidth(ADAPTIVE_MIN_STEP).build());
        final SimulationResult second = run(baseBuilder().stepWidth(COARSE_STEP)
                .adaptiveStepSize(true).minStepWidth(ADAPTIVE_MIN_STEP).build());

        assertEquals(first.getTotalTimeSteps(), second.getTotalTimeSteps(),
                "identical configs must produce identical step counts");
        assertArrayEquals(first.getSignalData(0), second.getSignalData(0),
                "identical configs must produce identical waveforms");
    }
}
