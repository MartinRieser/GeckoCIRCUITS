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
import gecko.core.control.calculators.AbstractControlCalculatable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that multiple headless simulations run concurrently in one JVM
 * produce identical, deterministic waveforms with no cross-thread
 * interference: every engine instance, netlist, coupler, and loss engine is
 * self-contained, and the legacy static time fallback is thread-local.
 */
class ConcurrentHeadlessSimulationTest {

    private static final String BUCK_CIRCUIT_REL = "buck_converter_web.ipes";

    /** Pool size of simultaneously running simulations. */
    private static final int THREAD_POOL_SIZE = 8;

    /** Per-future upper bound for a single simulation run. */
    private static final long RUN_TIMEOUT_SECONDS = 120;

    private File resolveCircuit(final String relativePath) {
        java.nio.file.Path path = Paths.get("src/test/resources/ipes", relativePath);
        File file = path.toFile();
        if (!file.exists()) {
            path = Paths.get("backend/gecko-simulation-core/src/test/resources/ipes", relativePath);
            file = path.toFile();
        }
        assertTrue(file.exists(), "Circuit file not found: " + path.toAbsolutePath());
        return file;
    }

    /**
     * Runs eight simulations simultaneously (barrier-synchronized start on an
     * eight-thread pool) and requires every waveform to be bit-identical to a
     * main-thread reference run: no race conditions, no cross-thread state.
     */
    @Test
    void parallelSimulations_produceIdenticalWaveforms() throws Exception {
        final File circuit = resolveCircuit(BUCK_CIRCUIT_REL);
        final SimulationConfig config = SimulationConfig.builder()
                .circuitFile(circuit.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        final SimulationResult reference = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(reference.isSuccess(), "Reference run failed: " + reference.getErrorMessage());
        assertTrue(reference.getSignalNames().length > 0,
                "Reference run must record waveforms for the comparison to be meaningful");

        final ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        final CyclicBarrier startGate = new CyclicBarrier(THREAD_POOL_SIZE);
        try {
            final List<Future<SimulationResult>> futures = new ArrayList<>(THREAD_POOL_SIZE);
            for (int run = 0; run < THREAD_POOL_SIZE; run++) {
                futures.add(pool.submit(() -> {
                    startGate.await(30, TimeUnit.SECONDS);
                    return new HeadlessSimulationEngine().runSimulation(config);
                }));
            }
            for (int run = 0; run < futures.size(); run++) {
                final SimulationResult result = futures.get(run).get(RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(result.isSuccess(), "Concurrent run " + run + " failed: " + result.getErrorMessage());
                assertWaveformsIdentical(reference, result, run);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * The legacy static time fallback must be thread-local: a time set on the
     * test thread must not be visible to a worker thread (and vice versa),
     * while the per-instance time always takes precedence over the fallback.
     */
    @Test
    void legacyStaticTimeFallback_isIsolatedPerThread() throws Exception {
        try {
            AbstractControlCalculatable.setTime(1.0);
            final AbstractControlCalculatable mainThreadProbe = newProbe();

            final ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                final List<Double> workerTimes = pool.submit(() -> {
                    final AbstractControlCalculatable workerProbe = newProbe();
                    final double before = workerProbe.getSimulationTime();
                    AbstractControlCalculatable.setTime(2.0);
                    final double afterSet = workerProbe.getSimulationTime();
                    workerProbe.setSimulationTime(9.0);
                    final double explicit = workerProbe.getSimulationTime();
                    return List.of(before, afterSet, explicit);
                }).get(RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                assertEquals(0.0, workerTimes.get(0), 0.0,
                        "worker thread must start from its own fallback, not the test thread's time");
                assertEquals(2.0, workerTimes.get(1), 0.0,
                        "worker thread must read back its own legacy time");
                assertEquals(9.0, workerTimes.get(2), 0.0,
                        "per-instance time must take precedence over the legacy fallback");
            } finally {
                pool.shutdownNow();
            }

            assertEquals(1.0, mainThreadProbe.getSimulationTime(), 0.0,
                    "worker thread's legacy time write must not leak into the test thread");
        } finally {
            // Reset this thread's fallback so later legacy tests are unaffected.
            AbstractControlCalculatable.setTime(0.0);
        }
    }

    private static AbstractControlCalculatable newProbe() {
        return new AbstractControlCalculatable(0, 1) {
            @Override
            public void calculateYOUT(final double deltaT) {
            }
        };
    }

    /**
     * Requires exact (bitwise) equality of the recorded waveforms: identical
     * runs on the same JVM must produce identical floating-point results, so
     * any difference indicates cross-thread interference.
     */
    private static void assertWaveformsIdentical(final SimulationResult reference,
                                                 final SimulationResult candidate,
                                                 final int runIndex) {
        final String[] referenceNames = reference.getSignalNames();
        assertArrayEquals(referenceNames, candidate.getSignalNames(),
                "Run " + runIndex + " must record the same signals");
        for (int signal = 0; signal < referenceNames.length; signal++) {
            final float[] referenceData = reference.getSignalData(signal);
            final float[] candidateData = candidate.getSignalData(signal);
            assertEquals(referenceData.length, candidateData.length,
                    "Run " + runIndex + " signal '" + referenceNames[signal] + "' length mismatch");
            assertArrayEquals(referenceData, candidateData,
                    "Run " + runIndex + " signal '" + referenceNames[signal]
                            + "' diverged from the reference waveform");
        }
    }
}
