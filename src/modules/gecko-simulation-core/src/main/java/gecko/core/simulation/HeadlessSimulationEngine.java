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

import gecko.core.allg.SolverSettingsCore;
import gecko.core.datacontainer.ContainerStatus;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.io.ParameterOverrideApplicator;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.InitialConditionSolver;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.ControlCalculatorBuilder;
import gecko.core.simulation.ControlNetlist;
import gecko.core.simulation.DomainCoupler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless simulation engine for running GeckoCIRCUITS simulations without GUI.
 * Suitable for REST APIs, CLI tools, batch processing, and cloud deployment.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
 * SimulationConfig config = SimulationConfig.builder()
 *     .circuitFile("path/to/circuit.ipes")
 *     .stepWidth(1e-6)
 *     .simulationDuration(20e-3)
 *     .build();
 *
 * SimulationResult result = engine.runSimulation(config);
 * if (result.isSuccess()) {
 *     double[] times = result.getTimeArray();
 *     float[] voltages = result.getSignalData(0);
 * }
 * }</pre>
 */
public class HeadlessSimulationEngine {

    private static final Logger LOGGER = LogManager.getLogger(HeadlessSimulationEngine.class);

    /**
     * Current state of the simulation engine.
     */
    public enum EngineState {
        /** Engine is idle and ready to run a simulation */
        IDLE,
        /** Engine is running a simulation */
        RUNNING,
        /** Engine is paused */
        PAUSED,
        /** Engine has been cancelled */
        CANCELLED
    }

    private final AtomicReference<EngineState> state = new AtomicReference<>(EngineState.IDLE);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    // Progress tracking
    private volatile double currentTime = 0;
    private volatile double endTime = 0;
    private volatile int currentStep = 0;
    private volatile long simulationStartTime = 0;

    // Event listener
    private SimulationProgressListener progressListener;

    /** Candidate progress tick interval, in simulation steps. */
    private static final int PROGRESS_TICK_STEPS = 100;

    /**
     * Step interval at which a progress callback is delivered unconditionally,
     * even if the wall-clock throttle would suppress it. Keeps the listener
     * usable as a deterministic step hook (e.g. pause at a fixed step).
     */
    private static final int PROGRESS_GUARANTEED_TICK_STEPS = 1000;

    /** Minimum wall-clock time between two throttled progress callbacks, in ms. */
    private static final long PROGRESS_MIN_INTERVAL_MS = 50;

    /** Max semiconductor state re-solves per time step (legacy limit). */
    private static final int MAX_SEMICONDUCTOR_ITERATIONS = 10_000;

    // Solver components
    private MatrixSolver matrixSolver;
    private ComponentCurrentCalculator componentCurrentCalculator;
    private InitialConditionSolver initialConditionSolver;

    // Circuit and control netlists
    private CircuitNetlist circuitNetlist;
    private ControlNetlist controlNetlist;
    private ControlCalculatorBuilder.ControlCoupling controlCoupling;

    // Domain coupling orchestrator
    private DomainCoupler domainCoupler;

    /**
     * Creates a new HeadlessSimulationEngine.
     */
    public HeadlessSimulationEngine() {
    }

    /**
     * Runs a simulation with the specified configuration.
     * This method blocks until the simulation completes.
     *
     * @param config the simulation configuration
     * @return the simulation result
     */
    public SimulationResult runSimulation(SimulationConfig config) {
        if (config == null) {
            return SimulationResult.failed("Simulation configuration is required");
        }

        if (!state.compareAndSet(EngineState.IDLE, EngineState.RUNNING)) {
            return SimulationResult.failed("Engine is already running a simulation");
        }

        cancelRequested.set(false);
        long startTime = System.currentTimeMillis();
        simulationStartTime = startTime;

        try {
            return executeSimulation(config, startTime);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return SimulationResult.failed("Simulation error: " + msg);
        } finally {
            state.set(EngineState.IDLE);
        }
    }

    /**
     * Executes the actual simulation loop.
     */
    private SimulationResult executeSimulation(SimulationConfig config, long startTime) {
        CircuitModel circuitModel = parseCircuitModel(config);
        SolverSettingsCore settings = config.getSolverSettings();
        double dt = settings.getStepWidth();
        double duration = settings.getSimulationDuration();
        validateSimulationSettings(dt, duration);

        endTime = duration;
        currentTime = 0;
        currentStep = 0;

        // Calculate expected number of steps
        int expectedSteps = calculateExpectedSteps(dt, duration);

        // Build netlists from circuit model
        // Apply parameter overrides before building the netlist
        if (circuitModel != null && !config.getParameterOverrides().isEmpty()) {
            ParameterOverrideApplicator.applyOverrides(circuitModel, config.getParameterOverrides());
        }

        circuitNetlist = NetlistBuilder.buildFromCircuitModel(circuitModel);

        // Build the CONTROL domain: calculators from the control blocks, gate
        // drives for switches and measurement probes, coupled via
        // coupledReferenceID like the classic editor
        controlCoupling = ControlCalculatorBuilder.build(circuitModel, circuitNetlist);
        controlNetlist = ControlNetlist.createEmpty();
        controlNetlist.setSortedCalculators(controlCoupling.calculators());
        controlCoupling.initialize(dt);

        // Signal selection: explicit request > file's stored signals >
        // node labels and probe names (classic-like default logging)
        String[] signalNames = resolveSignalNames(config, circuitModel, circuitNetlist, controlCoupling);

        // Initialize matrix solver
        matrixSolver = new MatrixSolver(settings.getSolverType());
        componentCurrentCalculator = new ComponentCurrentCalculator();
        initialConditionSolver = new InitialConditionSolver(settings.getSolverType());

        // Initialize domain coupler for orchestrating LK, CONTROL, THERM domains
        domainCoupler = new DomainCoupler();

        // Re-initialize matrix solver with real netlist dimensions
        if (circuitNetlist.getElementCount() > 0) {
            matrixSolver.initializeMatrices(
                circuitNetlist.getNodeMax(),
                circuitNetlist.getVoltageSourceMax(),
                circuitNetlist.getElementCount()
            );
        } else {
            // Fallback for empty circuit: use signal-based dimensions
            int nodeCount = signalNames.length;
            int voltageSourceCount = 0;
            int elementCount = signalNames.length;
            matrixSolver.initializeMatrices(nodeCount, voltageSourceCount, elementCount);
        }

        // Resolve each requested signal name to a node index via the netlist's
        // labels (e.g. "V_out"), to a CONTROL measurement probe (e.g. "VOLT.1"
        // or its labeled output "u1"), or to a labeled control signal tap
        // (e.g. "gate"). Signals that resolve to nothing are dropped with a
        // warning instead of logging silent zeros (classic container semantics:
        // only connected measurement curves produce columns).
        java.util.List<String> resolvedNames = new ArrayList<>(signalNames.length);
        java.util.List<Integer> resolvedIndices = new ArrayList<>(signalNames.length);
        int[] signalNodes = new int[signalNames.length];
        ControlCalculatorBuilder.Probe[] signalProbes = new ControlCalculatorBuilder.Probe[signalNames.length];
        ControlCalculatorBuilder.SignalTap[] signalTaps = new ControlCalculatorBuilder.SignalTap[signalNames.length];
        for (int i = 0; i < signalNames.length; i++) {
            signalNodes[i] = circuitNetlist != null
                    ? circuitNetlist.getLabelResolver().getIndex(signalNames[i]) : -1;
            if (signalNodes[i] < 0) {
                for (ControlCalculatorBuilder.Probe probe : controlCoupling.probes()) {
                    if (probe.name().equals(signalNames[i])) {
                        signalProbes[i] = probe;
                        break;
                    }
                }
            }
            if (signalNodes[i] < 0 && signalProbes[i] == null) {
                for (ControlCalculatorBuilder.SignalTap tap : controlCoupling.signalTaps()) {
                    if (tap.name().equals(signalNames[i])) {
                        signalTaps[i] = tap;
                        break;
                    }
                }
            }
            if (signalNodes[i] >= 0 || signalProbes[i] != null || signalTaps[i] != null) {
                resolvedNames.add(signalNames[i]);
                resolvedIndices.add(i);
            } else {
                LOGGER.warn("Signal '{}' cannot be resolved to a node, measurement probe or "
                        + "labeled control output - not recorded", signalNames[i]);
            }
        }
        if (resolvedIndices.size() < signalNames.length) {
            signalNames = resolvedNames.toArray(new String[0]);
            int[] keptNodes = new int[signalNames.length];
            ControlCalculatorBuilder.Probe[] keptProbes = new ControlCalculatorBuilder.Probe[signalNames.length];
            ControlCalculatorBuilder.SignalTap[] keptTaps = new ControlCalculatorBuilder.SignalTap[signalNames.length];
            for (int k = 0; k < resolvedIndices.size(); k++) {
                int i = resolvedIndices.get(k);
                keptNodes[k] = signalNodes[i];
                keptProbes[k] = signalProbes[i];
                keptTaps[k] = signalTaps[i];
            }
            signalNodes = keptNodes;
            signalProbes = keptProbes;
            signalTaps = keptTaps;
        }

        // Create data container for results
        DataContainerGlobal dataContainer = new DataContainerGlobal();
        dataContainer.init(signalNames.length, expectedSteps + 1, signalNames, "time [s]");
        dataContainer.setContainerStatus(ContainerStatus.RUNNING);

        // Main simulation loop
        float[] values = new float[signalNames.length];

        // Initial conditions (legacy semantics): inductor initial current and
        // capacitor initial voltage from parameters seed the solver history,
        // so files saved mid-run restart at their saved operating point like
        // the classic GUI.
        if (circuitNetlist != null && circuitNetlist.getElementCount() > 0) {
            initialConditionSolver.setInitialConditions(matrixSolver, circuitNetlist, settings.getSolverType());
        }

        long lastProgressTime = startTime;

        while (currentTime <= duration) {
            awaitResumeOrCancel();

            if (cancelRequested.get()) {
                dataContainer.setContainerStatus(ContainerStatus.PAUSED);
                return SimulationResult.builder()
                        .status(SimulationResult.Status.CANCELLED)
                        .dataContainer(dataContainer)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .totalTimeSteps(currentStep)
                        .simulatedTime(currentTime)
                        .build();
            }

            // Phase 4: Execute domain coupling (LK → CONTROL → LK)
            // Orchestrates data transfer between circuit, control, and thermal domains
            domainCoupler.coupleDomainsForTimeStep(circuitNetlist, controlNetlist, dt, currentTime);

            // Gate-driven switches: rewrite the switch resistance from the
            // current control signals before the matrix is built
            controlCoupling.applyGateSignals(circuitNetlist);

            // Real MNA solver: build and solve circuit matrices
            if (circuitNetlist != null && circuitNetlist.getElementCount() > 0) {
                matrixSolver.buildMatrixA(circuitNetlist, dt, currentTime, false);
                matrixSolver.buildVectorB(circuitNetlist, dt, currentTime, false);
                matrixSolver.solve();

                // Semiconductor state machine (port of legacy
                // doDiodeErrorsRecalculations): flip diode/thyristor/IGBT states
                // until stable, re-solving the SAME time step (history is not
                // shifted between iterations).
                double stoergroesse = 1.0;
                boolean isNewIteration = false;
                int errorCounter = 0;
                while (componentCurrentCalculator.calculateComponentCurrents(
                        matrixSolver, circuitNetlist, stoergroesse, dt, currentTime,
                        isNewIteration, errorCounter)) {
                    isNewIteration = true;
                    if (++errorCounter > MAX_SEMICONDUCTOR_ITERATIONS) {
                        throw new IllegalStateException(
                                "Numerical instability of switch states at t=" + currentTime);
                    }
                    if (errorCounter > 2) {
                        stoergroesse *= 0.99;
                    }
                    matrixSolver.buildMatrixA(circuitNetlist, dt, currentTime, false);
                    matrixSolver.buildVectorB(circuitNetlist, dt, currentTime, false);
                    matrixSolver.solve();
                }

                // Shift history for next time step
                matrixSolver.updateNodePotentials(dt, currentTime);

                // Store results back into netlist
                circuitNetlist.storeResults(matrixSolver.getP(), matrixSolver.getIALT());

                // Refresh CONTROL measurement probes (voltmeter/ammeter)
                controlCoupling.updateProbes(circuitNetlist, matrixSolver.getP());
            }

            // Sample the CURRENT state for data logging AFTER the solve: like
            // the classic engine, the row logged at time t holds the state of
            // the solve for [t-dt, t].
            for (int sigIdx = 0; sigIdx < values.length; sigIdx++) {
                ControlCalculatorBuilder.Probe probe = signalProbes[sigIdx];
                if (probe != null) {
                    values[sigIdx] = (float) probe.outputHolder()._outputSignal[0][0];
                    continue;
                }
                ControlCalculatorBuilder.SignalTap tap = signalTaps[sigIdx];
                if (tap != null) {
                    values[sigIdx] = (float) tap.source()._outputSignal[0][0];
                    continue;
                }
                int node = signalNodes[sigIdx];
                values[sigIdx] = node >= 0 && circuitNetlist != null
                        && node < matrixSolver.getP().length
                        ? (float) matrixSolver.getP()[node] : 0.0f;
            }

            if (config.isDataLoggingEnabled() &&
                    (currentStep % config.getDataLoggingInterval() == 0)) {
                dataContainer.insertValuesAtEnd(values, currentTime);
            }

            currentTime += dt;
            currentStep++;

            // Report progress: candidates every PROGRESS_TICK_STEPS steps, throttled
            // to at most one callback per PROGRESS_MIN_INTERVAL_MS. Callbacks on
            // PROGRESS_GUARANTEED_TICK_STEPS boundaries and at the end of the run
            // are always delivered.
            if (progressListener != null && (currentStep % PROGRESS_TICK_STEPS == 0 || currentTime >= duration)) {
                long now = System.currentTimeMillis();
                if (currentStep % PROGRESS_GUARANTEED_TICK_STEPS == 0
                        || currentTime >= duration
                        || now - lastProgressTime >= PROGRESS_MIN_INTERVAL_MS) {
                    lastProgressTime = now;
                    progressListener.onProgress(currentTime, duration, currentStep);
                }
            }
        }

        dataContainer.setContainerStatus(ContainerStatus.FINISHED);
        long executionTimeMs = System.currentTimeMillis() - startTime;

        return SimulationResult.builder()
                .status(SimulationResult.Status.SUCCESS)
                .dataContainer(dataContainer)
                .executionTimeMs(executionTimeMs)
                .totalTimeSteps(currentStep)
                .simulatedTime(currentTime)
                .metadata("solver", settings.getSolverType().toString())
                .metadata("dt", dt)
                .metadata("circuitFile", config.getCircuitFilePath() != null
                        ? config.getCircuitFilePath() : "in-memory model")
                .metadata("parameterOverrides", config.getParameterOverrides().size())
                .build();
    }

    /**
     * Blocks while the simulation is paused. An interrupt while parked
     * is treated as a cancellation request (e.g. executor shutdown).
     */
    private void awaitResumeOrCancel() {
        while (state.get() == EngineState.PAUSED && !cancelRequested.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelRequested.set(true);
            }
        }
    }

    /**
     * Requests cancellation of the running simulation.
     * The simulation will stop at the next opportunity.
     */
    public void cancel() {
        EngineState current = state.get();
        if (current == EngineState.RUNNING || current == EngineState.PAUSED) {
            cancelRequested.set(true);
        }
    }

    /**
     * Gets the current engine state.
     *
     * @return current state
     */
    public EngineState getState() {
        return state.get();
    }

    /**
     * Gets the current simulation time.
     *
     * @return current time in seconds
     */
    public double getCurrentTime() {
        return currentTime;
    }

    /**
     * Gets the end time of the simulation.
     *
     * @return end time in seconds
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Gets the current simulation progress as a percentage.
     *
     * @return progress from 0.0 to 1.0
     */
    public double getProgress() {
        if (endTime <= 0) {
            return 0;
        }
        return Math.min(1.0, currentTime / endTime);
    }

    /**
     * Gets the current time step number.
     *
     * @return current step
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Sets a progress listener for simulation progress updates.
     *
     * @param listener the progress listener, or null to remove
     */
    public void setProgressListener(SimulationProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Pauses the running simulation.
     * The simulation will pause at the next time step.
     * Has no effect if the simulation is not running.
     *
     * @return true if pause was requested, false if simulation is not running
     */
    public boolean pause() {
        return state.compareAndSet(EngineState.RUNNING, EngineState.PAUSED);
    }

    /**
     * Resumes a paused simulation.
     * Has no effect if the simulation is not paused.
     *
     * @return true if resume was successful, false if simulation was not paused
     */
    public boolean resume() {
        return state.compareAndSet(EngineState.PAUSED, EngineState.RUNNING);
    }

    /**
     * Checks if the simulation is currently paused.
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return state.get() == EngineState.PAUSED;
    }

    /**
     * Gets detailed progress information including time, steps, and ETA.
     * Useful for real-time monitoring and progress bars.
     *
     * @return detailed progress information, or null if no simulation is running
     */
    public SimulationProgress getDetailedProgress() {
        EngineState currentState = state.get();
        if (currentState == EngineState.IDLE) {
            return null;
        }

        double overallProgress = getProgress();

        // For now, pre-calculation progress is 0 (will be implemented with real solver)
        double preCalcProgress = 0.0;
        double mainSimProgress = overallProgress;

        // Estimate remaining time based on current progress
        Long estimatedRemainingMs = null;
        if (overallProgress > 0.01) { // Only estimate after 1% to avoid division by zero
            long elapsedMs = System.currentTimeMillis() - simulationStartTime;
            long totalEstimatedMs = (long) (elapsedMs / overallProgress);
            estimatedRemainingMs = totalEstimatedMs - elapsedMs;
        }

        // Calculate expected total steps
        int totalSteps = (int) Math.ceil(endTime / (currentTime / Math.max(1, currentStep)));
        if (totalSteps <= 0) {
            totalSteps = currentStep + 1000; // Fallback estimate
        }

        return new SimulationProgress(
            overallProgress,
            preCalcProgress,
            mainSimProgress,
            currentStep,
            totalSteps,
            currentTime,
            endTime,
            estimatedRemainingMs,
            currentState
        );
    }

    /**
     * Listener interface for simulation progress updates.
     */
    @FunctionalInterface
    public interface SimulationProgressListener {
        /**
         * Called periodically during simulation with progress information.
         *
         * @param currentTime current simulation time in seconds
         * @param endTime total simulation time in seconds
         * @param currentStep current time step number
         */
        void onProgress(double currentTime, double endTime, int currentStep);
    }



    private static int calculateExpectedSteps(double dt, double duration) {
        double rawSteps = Math.ceil(duration / dt);
        if (!Double.isFinite(rawSteps) || rawSteps > Integer.MAX_VALUE - 1) {
            throw new IllegalArgumentException("Simulation step count is too large");
        }
        return Math.max(1, (int) rawSteps);
    }

    private static void validateSimulationSettings(double dt, double duration) {
        if (!Double.isFinite(dt) || dt <= 0) {
            throw new IllegalArgumentException("Step width must be a finite value > 0");
        }
        if (!Double.isFinite(duration) || duration <= 0) {
            throw new IllegalArgumentException("Simulation duration must be a finite value > 0");
        }
    }

    private static CircuitModel parseCircuitModel(SimulationConfig config) {
        if (config.getCircuitModel() != null) {
            return config.getCircuitModel();
        }

        String circuitPath = config.getCircuitFilePath();
        if (circuitPath == null || circuitPath.isBlank()) {
            return null;
        }

        CircuitFileParser parser = new CircuitFileParser();
        try {
            return parser.parse(circuitPath);
        } catch (IOException | CircuitFileParser.CircuitParseException ex) {
            throw new IllegalArgumentException("Unable to parse circuit file '" + circuitPath + "': " + ex.getMessage(), ex);
        }
    }

    private static String[] resolveSignalNames(SimulationConfig config, CircuitModel circuitModel,
                                              CircuitNetlist circuitNetlist,
                                              ControlCalculatorBuilder.ControlCoupling controlCoupling) {
        if (config.getSignals() != null && !config.getSignals().isEmpty()) {
            return config.getSignals().toArray(new String[0]);
        }
        if (circuitModel != null && circuitModel.getDataContainerSignals() != null) {
            // skip positional placeholders ("", NIX-mapped) and the "[]" empty-list marker
            String[] cleaned = java.util.Arrays.stream(circuitModel.getDataContainerSignals())
                    .filter(s -> s != null && !s.isBlank() && !s.equals("[]"))
                    .toArray(String[]::new);
            if (cleaned.length > 0) {
                return cleaned;
            }
        }
        List<String> names = new ArrayList<>();
        if (circuitNetlist != null && circuitNetlist.getLabelResolver() != null) {
            for (String label : circuitNetlist.getLabelResolver().getAllLabels()) {
                if (label != null && !label.isBlank() && !label.equalsIgnoreCase("GND") && !label.startsWith("NIX")) {
                    names.add(label);
                }
            }
        }
        if (controlCoupling != null) {
            for (ControlCalculatorBuilder.Probe probe : controlCoupling.probes()) {
                if (!names.contains(probe.name())) {
                    names.add(probe.name());
                }
            }
            for (ControlCalculatorBuilder.SignalTap tap : controlCoupling.signalTaps()) {
                if (!names.contains(tap.name())) {
                    names.add(tap.name());
                }
            }
        }
        if (!names.isEmpty()) {
            return names.toArray(new String[0]);
        }
        return new String[] {"V_out", "I_in", "P_loss"};
    }
}
