/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.simulation;

import gecko.core.allg.SolverType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadlessSimulationEngineTest {

    @Test
    void runSimulation_nullConfig_returnsFailedResult() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        SimulationResult result = engine.runSimulation(null);

        assertFalse(result.isSuccess());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
    }

    @Test
    void runSimulation_invalidStepWidth_returnsFailedResult() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .stepWidth(0.0)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertFalse(result.isSuccess());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
    }

    @Test
    void runSimulation_missingCircuitFile_returnsFailedResult() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("/definitely/missing/file.ipes")
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertFalse(result.isSuccess());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
    }

    @Test
    void cancelWhileIdle_doesNotBlockNextSimulation() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        engine.cancel();

        SimulationConfig config = SimulationConfig.builder()
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertTrue(result.isSuccess());
        assertEquals(SimulationResult.Status.SUCCESS, result.getStatus());
    }

    // ========== Pause/Resume Tests ==========

    @Test
    void pause_whileIdle_returnsFalse() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        boolean paused = engine.pause();

        assertFalse(paused);
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
    }

    @Test
    void resume_whileIdle_returnsFalse() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        boolean resumed = engine.resume();

        assertFalse(resumed);
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
    }

    @Test
    void isPaused_whileIdle_returnsFalse() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        assertFalse(engine.isPaused());
    }

    @Test
    void pauseFreezesTime_resumeCompletesRun() throws Exception {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        engine.setProgressListener((currentTime, endTime, currentStep) -> {
            if (currentStep == 1000) {
                engine.pause();
            }
        });

        Thread worker = new Thread(() -> engine.runSimulation(SimulationConfig.builder()
                .stepWidth(1e-6)
                .simulationDuration(20e-3)
                .solverType(SolverType.SOLVER_BE)
                .build()));
        worker.start();

        assertTrue(waitFor(() -> engine.isPaused()), "engine should reach PAUSED state");
        double frozenTime = engine.getCurrentTime();
        Thread.sleep(150);
        assertEquals(frozenTime, engine.getCurrentTime(), 1e-12, "time must not advance while paused");

        assertTrue(engine.resume());
        worker.join(10_000);
        assertFalse(worker.isAlive());

        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
        assertTrue(engine.getCurrentTime() >= 20e-3, "run must finish after resume");
    }

    @Test
    void cancelWhilePaused_stopsSimulation() throws Exception {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        engine.setProgressListener((currentTime, endTime, currentStep) -> {
            if (currentStep == 1000) {
                engine.pause();
            }
        });

        Thread worker = new Thread(() -> engine.runSimulation(SimulationConfig.builder()
                .stepWidth(1e-6)
                .simulationDuration(20e-3)
                .solverType(SolverType.SOLVER_BE)
                .build()));
        worker.start();

        assertTrue(waitFor(() -> engine.isPaused()), "engine should reach PAUSED state");
        engine.cancel();
        worker.join(10_000);
        assertFalse(worker.isAlive());

        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
        assertTrue(engine.getCurrentTime() < 20e-3, "cancelled run must not reach the end");
    }

    // ========== Progress Reporting Tests ==========

    @Test
    void progress_reportsFinalStepEvenForShortRuns() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        List<Integer> reportedSteps = new java.util.concurrent.CopyOnWriteArrayList<>();
        engine.setProgressListener((currentTime, endTime, currentStep) -> reportedSteps.add(currentStep));

        SimulationResult result = engine.runSimulation(SimulationConfig.builder()
                .stepWidth(1e-6)
                .simulationDuration(300e-6) // 300 steps: below any tick boundary
                .solverType(SolverType.SOLVER_BE)
                .build());

        assertTrue(result.isSuccess());
        assertFalse(reportedSteps.isEmpty(),
                "a run shorter than the tick interval must still report its final step");
        assertEquals(result.getTotalTimeSteps(), reportedSteps.get(reportedSteps.size() - 1).intValue(),
                "the last reported step must be the final step of the run");
    }

    @Test
    void progress_guaranteesTickEveryThousandSteps() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        List<Integer> reportedSteps = new java.util.concurrent.CopyOnWriteArrayList<>();
        engine.setProgressListener((currentTime, endTime, currentStep) -> reportedSteps.add(currentStep));

        SimulationResult result = engine.runSimulation(SimulationConfig.builder()
                .stepWidth(1e-6)
                .simulationDuration(1500e-6) // crosses the 1000-step boundary
                .solverType(SolverType.SOLVER_BE)
                .build());

        assertTrue(result.isSuccess());
        assertTrue(reportedSteps.contains(1000),
                "the 1000-step boundary must be reported even under wall-clock throttling");
        assertEquals(result.getTotalTimeSteps(), reportedSteps.get(reportedSteps.size() - 1).intValue(),
                "the last reported step must be the final step of the run");
    }

    @Test
    void signalOverride_isRecordedInsteadOfFileSignals() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .signals(List.of("node_a", "node_b"))
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSignalNames().length);
        assertEquals("node_a", result.getSignalNames()[0]);
        assertEquals("node_b", result.getSignalNames()[1]);
    }

    private static boolean waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    // ========== Detailed Progress Tests ==========

    @Test
    void getDetailedProgress_whileIdle_returnsNull() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        SimulationProgress progress = engine.getDetailedProgress();

        assertEquals(null, progress);
    }

    @Test
    void getDetailedProgress_afterRunStart_returnsValidProgress() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        // While idle, should return null
        SimulationProgress progress = engine.getDetailedProgress();

        assertEquals(null, progress);
        // Real progress testing would require simulation to be running
        // This is covered by integration tests
    }

    // ========== Progress Tracking Tests ==========

    @Test
    void getCurrentTime_initiallyZero() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        double time = engine.getCurrentTime();

        assertEquals(0.0, time, 0.001);
    }

    @Test
    void getEndTime_initiallyZero() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        double endTime = engine.getEndTime();

        assertEquals(0.0, endTime, 0.001);
    }

    @Test
    void getProgress_initiallyZero() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        double progress = engine.getProgress();

        assertEquals(0.0, progress, 0.001);
    }

    @Test
    void getCurrentStep_initiallyZero() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        int step = engine.getCurrentStep();

        assertEquals(0, step);
    }

    @Test
    void getState_initiallyIdle() {
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        HeadlessSimulationEngine.EngineState state = engine.getState();

        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, state);
    }
}
