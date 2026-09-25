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
import gecko.core.simulation.solver.AdaptiveStepController;
import gecko.core.simulation.solver.MnaSolver;
import gecko.core.simulation.solver.MnaSolverFactory;
import gecko.core.simulation.solver.NonlinearConvergenceController;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.InitialConditionSolver;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.INetList;
import gecko.core.circuit.parameters.DiodeParameters;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.ControlCalculatorBuilder;
import gecko.core.simulation.ControlNetlist;
import gecko.core.simulation.DomainCoupler;
import gecko.core.circuit.losscalculation.SemiconductorLossEngine;
import gecko.core.simulation.solver.sparse.SparseMatrixSolver;

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

    /** Grid-alignment tolerance of the adaptive logging, relative to dt. */
    private static final double GRID_ALIGNMENT_EPSILON = 1e-9;

    /** Absolute floor of the LTE error-norm scale (mixed abs/rel norm). */
    private static final double LTE_ABSOLUTE_FLOOR = 1e-3;

    private static final int MAX_SEMICONDUCTOR_ITERATIONS = 10_000;

    // Solver components
    private MnaSolver matrixSolver;
    private ComponentCurrentCalculator componentCurrentCalculator;
    private InitialConditionSolver initialConditionSolver;

    // Circuit and control netlists
    private CircuitNetlist circuitNetlist;
    private ControlNetlist controlNetlist;
    private ControlCalculatorBuilder.ControlCoupling controlCoupling;

    // Domain coupling orchestrator
    private DomainCoupler domainCoupler;

    // Semiconductor loss calculation engine
    private SemiconductorLossEngine lossEngine;

    /**
     * Gets the semiconductor loss calculation engine.
     *
     * @return loss engine instance, or null if no simulation has been initialized
     */
    public SemiconductorLossEngine getLossEngine() {
        return lossEngine;
    }

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

        // Initialize matrix solver (dense reference or sparse backend by kind)
        final int expectedNodes = circuitNetlist != null ? circuitNetlist.getNodeMax() : signalNames.length;
        final int expectedVoltageSources = circuitNetlist != null ? circuitNetlist.getVoltageSourceMax() : 0;
        matrixSolver = MnaSolverFactory.create(settings.getSolverType(), config.getMatrixSolverKind(),
                expectedNodes + expectedVoltageSources + 1);
        componentCurrentCalculator = new ComponentCurrentCalculator();
        initialConditionSolver = new InitialConditionSolver(settings.getSolverType());
        // Shockley Newton-Raphson mode (opt-in): the controller owns the diode
        // slots; the piecewise-linear machine keeps thyristors and IGBTs
        final NonlinearConvergenceController nrController =
                config.getSemiconductorModel() == SemiconductorModelKind.SHOCKLEY_NEWTON_RAPHSON
                        && circuitNetlist != null
                        ? NonlinearConvergenceController.createFromNetlist(circuitNetlist)
                        : null;

        // Initialize domain coupler for orchestrating LK, CONTROL, THERM domains
        domainCoupler = new DomainCoupler();

        // Initialize semiconductor loss engine
        lossEngine = new SemiconductorLossEngine().initializeFromNetlist(circuitNetlist);

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
        boolean[] signalIsLoss = new boolean[signalNames.length];
        for (int i = 0; i < signalNames.length; i++) {
            // Probes and taps first: voltmeter output labels (e.g. "v_in")
            // usually also exist as CONTROL wire labels, and the netlist label
            // resolver indexes those too - resolving them as power nodes first
            // shadows the probe with a bogus node index and logs flat zeros.
            for (ControlCalculatorBuilder.Probe probe : controlCoupling.probes()) {
                if (probe.name().equals(signalNames[i])
                        || (probe.componentName() != null && probe.componentName().equals(signalNames[i]))) {
                    signalProbes[i] = probe;
                    break;
                }
            }
            if (signalProbes[i] == null) {
                for (ControlCalculatorBuilder.SignalTap tap : controlCoupling.signalTaps()) {
                    if (tap.name().equals(signalNames[i])) {
                        signalTaps[i] = tap;
                        break;
                    }
                }
            }
            signalNodes[i] = signalProbes[i] == null && signalTaps[i] == null && circuitNetlist != null
                    ? circuitNetlist.getLabelResolver().getIndex(signalNames[i]) : -1;

            if (signalProbes[i] == null && signalTaps[i] == null && signalNodes[i] < 0 && lossEngine != null) {
                signalIsLoss[i] = lossEngine.hasLossSignal(signalNames[i]);
            }

            if (signalNodes[i] >= 0 || signalProbes[i] != null || signalTaps[i] != null || signalIsLoss[i]) {
                resolvedNames.add(signalNames[i]);
                resolvedIndices.add(i);
            } else {
                LOGGER.warn("Signal '{}' cannot be resolved to a node, measurement probe, "
                        + "labeled control output, or loss channel - not recorded", signalNames[i]);
            }
        }
        if (resolvedIndices.size() < signalNames.length) {
            signalNames = resolvedNames.toArray(new String[0]);
            int[] keptNodes = new int[signalNames.length];
            ControlCalculatorBuilder.Probe[] keptProbes = new ControlCalculatorBuilder.Probe[signalNames.length];
            ControlCalculatorBuilder.SignalTap[] keptTaps = new ControlCalculatorBuilder.SignalTap[signalNames.length];
            boolean[] keptLoss = new boolean[signalNames.length];
            for (int k = 0; k < resolvedIndices.size(); k++) {
                int i = resolvedIndices.get(k);
                keptNodes[k] = signalNodes[i];
                keptProbes[k] = signalProbes[i];
                keptTaps[k] = signalTaps[i];
                keptLoss[k] = signalIsLoss[i];
            }
            signalNodes = keptNodes;
            signalProbes = keptProbes;
            signalTaps = keptTaps;
            signalIsLoss = keptLoss;
        }

        // Adaptive step-size control (opt-in): a shadow solver of the
        // complementary integration method provides the LTE estimate; it is
        // seeded with the primary solver's history and the converged switch
        // states each step, so the primary state is never rolled back
        final AdaptiveStepController stepController;
        final MnaSolver shadowSolver;
        if (config.isAdaptiveStepSize() && circuitNetlist != null
                && circuitNetlist.getElementCount() > 0) {
            final int solverMatrixSize = circuitNetlist.getNodeMax()
                    + circuitNetlist.getVoltageSourceMax() + 1;
            stepController = new AdaptiveStepController(
                    config.getRelativeTolerance(),
                    config.getMinStepWidth() > 0
                            ? config.getMinStepWidth()
                            : dt / AdaptiveStepController.DEFAULT_MIN_STEP_DIVISOR,
                    config.getMaxStepWidth() > 0 ? Math.min(config.getMaxStepWidth(), dt) : dt,
                    dt);
            shadowSolver = MnaSolverFactory.create(
                    AdaptiveStepController.complementaryType(settings.getSolverType()),
                    matrixSolver instanceof SparseMatrixSolver
                            ? MatrixSolverKind.SPARSE : MatrixSolverKind.DENSE,
                    solverMatrixSize);
            shadowSolver.initializeMatrices(circuitNetlist.getNodeMax(),
                    circuitNetlist.getVoltageSourceMax(), circuitNetlist.getElementCount());
        } else {
            stepController = null;
            shadowSolver = null;
        }

        // Create data container for results (adaptive mode logs on the same
        // uniform base grid as fixed-dt mode; only the step trajectory differs)
        DataContainerGlobal dataContainer = new DataContainerGlobal();
        dataContainer.init(signalNames.length, expectedSteps + 1, signalNames, "time [s]");
        dataContainer.setContainerStatus(ContainerStatus.RUNNING);

        // Main simulation loop
        float[] values = new float[signalNames.length];

        // Adaptive mode: number of base-grid rows already logged; the last
        // grid row is the last grid point at or before the duration (the
        // fixed-dt loop logs exactly the same rows)
        int loggedGridRows = 0;
        final int lastGridRow = (int) Math.floor(duration / dt + GRID_ALIGNMENT_EPSILON);

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

            // Determine this step's width: adaptive controller or the fixed dt
            // Adaptive mode refines dt within the base grid; fixed mode
            // keeps the legacy constant dt. Adaptive steps never cross a
            // logging-grid point: each step ends on the grid (or mid-cell for
            // a finer requested width), so the row logged at grid time t holds
            // the exact state at t like the classic fixed-dt engine.
            double attemptDt = stepController != null ? stepController.getCurrentStepWidth() : dt;
            if (stepController != null) {
                // Distance to the next grid point strictly after currentTime
                // (the row at a grid point we are sitting on logs at the end
                // of this very iteration)
                final double nextGridIndex =
                        Math.floor(currentTime / dt + GRID_ALIGNMENT_EPSILON) + 1.0;
                final double dtToNextGrid = nextGridIndex * dt - currentTime;
                attemptDt = Math.min(attemptDt,
                        Math.max(dtToNextGrid, dt * GRID_ALIGNMENT_EPSILON));
            }

            // Phase 4: Execute domain coupling (LK → CONTROL → LK) once per
            // step; rejected adaptive attempts re-solve only the power domain
            // Orchestrates data transfer between circuit, control, and thermal domains
            domainCoupler.coupleDomainsForTimeStep(circuitNetlist, controlNetlist, attemptDt, currentTime);

            // Gate-driven switches: rewrite the switch resistance from the
            // current control signals before the matrix is built
            controlCoupling.applyGateSignals(circuitNetlist);

            // Step-start element state for adaptive rollback / shadow evaluation
            final double[][] preAttemptParameters = stepController != null && circuitNetlist != null
                    && circuitNetlist.getElementCount() > 0
                    ? snapshotElementParameters(circuitNetlist) : null;

            if (nrController != null) {
                nrController.beginStep();
                componentCurrentCalculator.setDiodesHandledByNewtonRaphson(true);
            }

            boolean accepted = false;
            while (!accepted) {
                // Real MNA solver: build and solve circuit matrices
                if (circuitNetlist != null && circuitNetlist.getElementCount() > 0) {
                    matrixSolver.buildMatrixA(circuitNetlist, attemptDt, currentTime, false);
                    matrixSolver.buildVectorB(circuitNetlist, attemptDt, currentTime, false);
                    matrixSolver.solve();

                    // Semiconductor state machine (port of legacy
                    // doDiodeErrorsRecalculations): flip diode/thyristor/IGBT states
                    // until stable, re-solving the SAME time step (history is not
                    // shifted between iterations).
                    double stoergroesse = 1.0;
                    boolean isNewIteration = false;
                    int errorCounter = 0;
                    boolean converged = false;
                    while (!converged) {
                        boolean statesChanged = componentCurrentCalculator.calculateComponentCurrents(
                                matrixSolver, circuitNetlist, stoergroesse, attemptDt, currentTime,
                                isNewIteration, errorCounter);
                        boolean diodesConverged = true;
                        if (nrController != null) {
                            if (nrController.isFallbackToPiecewiseLinear()) {
                                // Newton gave up for this step: the piecewise-linear
                                // machine takes the diode slots back
                                componentCurrentCalculator.setDiodesHandledByNewtonRaphson(false);
                            }
                            diodesConverged = nrController.updateDiodeStamps(
                                    matrixSolver.getP(), circuitNetlist);
                        }
                        converged = !statesChanged && diodesConverged;
                        if (converged) {
                            break;
                        }
                        isNewIteration = true;
                        if (++errorCounter > MAX_SEMICONDUCTOR_ITERATIONS) {
                            throw new IllegalStateException(
                                    "Numerical instability of switch states at t=" + currentTime);
                        }
                        if (errorCounter > 2) {
                            stoergroesse *= 0.99;
                        }
                        matrixSolver.buildMatrixA(circuitNetlist, attemptDt, currentTime, false);
                        matrixSolver.buildVectorB(circuitNetlist, attemptDt, currentTime, false);
                        matrixSolver.solve();
                    }
                }

                if (stepController == null || circuitNetlist == null
                        || circuitNetlist.getElementCount() == 0) {
                    accepted = true;
                    break;
                }

                // Adaptive error test: LTE estimate from the complementary
                // integration method decides acceptance and the next width
                final double errorNorm = estimateLocalTruncationError(
                        shadowSolver, attemptDt, currentTime, preAttemptParameters);
                if (LOGGER.isDebugEnabled() && currentStep < 40) {
                    LOGGER.debug("step={} t={} tryDt={} err={}", currentStep, currentTime,
                            attemptDt, errorNorm);
                }
                final AdaptiveStepController.StepDecision decision =
                        stepController.evaluate(errorNorm, attemptDt);
                if (decision.accepted()) {
                    accepted = true;
                } else {
                    // Roll back the element state and retry with the smaller width
                    restoreElementParameters(circuitNetlist, preAttemptParameters);
                    attemptDt = decision.nextStepWidth();
                }
            }

            if (circuitNetlist != null && circuitNetlist.getElementCount() > 0) {
                // Shift history for next time step
                matrixSolver.updateNodePotentials(attemptDt, currentTime);

                // Store results back into netlist
                circuitNetlist.storeResults(matrixSolver.getP(), matrixSolver.getIALT());

                // Refresh CONTROL measurement probes (voltmeter/ammeter)
                controlCoupling.updateProbes(circuitNetlist, matrixSolver.getP());

                // Calculate semiconductor conduction and switching losses and couple to thermal domain
                if (lossEngine != null) {
                    lossEngine.calculateStep(circuitNetlist, domainCoupler, attemptDt, currentTime);
                }
            }

            // Sample the CURRENT state for data logging AFTER the solve: like
            // the classic engine, the row logged at time t holds the state of
            // the solve for [t-dt, t].
            for (int sigIdx = 0; sigIdx < values.length; sigIdx++) {
                if (signalIsLoss[sigIdx]) {
                    values[sigIdx] = (float) lossEngine.evaluateLossSignal(signalNames[sigIdx]);
                    continue;
                }
                ControlCalculatorBuilder.Probe probe = signalProbes[sigIdx];
                if (probe != null) {
                    values[sigIdx] = (float) probe.outputHolder()._outputSignal[0][0];
                    continue;
                }
                ControlCalculatorBuilder.SignalTap tap = signalTaps[sigIdx];
                if (tap != null) {
                    int outIdx = tap.outputIndex();
                    values[sigIdx] = (float) (outIdx < tap.source()._outputSignal.length
                            ? tap.source()._outputSignal[outIdx][0] : 0.0);
                    continue;
                }
                int node = signalNodes[sigIdx];
                values[sigIdx] = node >= 0 && circuitNetlist != null
                        && node < matrixSolver.getP().length
                        ? (float) matrixSolver.getP()[node] : 0.0f;
            }

            if (config.isDataLoggingEnabled()) {
                if (stepController == null) {
                    if (currentStep % config.getDataLoggingInterval() == 0) {
                        dataContainer.insertValuesAtEnd(values, currentTime);
                    }
                } else {
                    // Log on the uniform base grid: each accepted step (dt <=
                    // base dt) crosses at most one grid point; the row logged
                    // at grid time t holds the state of the step covering it
                    if (LOGGER.isDebugEnabled() && currentStep < 40) {
                        LOGGER.debug("step={} t={} gridRow={} threshold={} expectedSteps={}",
                                currentStep, currentTime, loggedGridRows,
                                (loggedGridRows * dt - GRID_ALIGNMENT_EPSILON * dt),
                                expectedSteps);
                    }
                    while (loggedGridRows <= lastGridRow
                            && currentTime >= loggedGridRows * dt - GRID_ALIGNMENT_EPSILON * dt) {
                        if (loggedGridRows % config.getDataLoggingInterval() == 0) {
                            dataContainer.insertValuesAtEnd(values, loggedGridRows * dt);
                        }
                        loggedGridRows++;
                    }
                }
            }

            currentTime += attemptDt;
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

        // Adaptive mode: the run may end between grid points; the remaining
        // grid rows are logged with the final state so the container always
        // carries the full expectedSteps + 1 uniform rows
        if (stepController != null) {
            while (loggedGridRows <= lastGridRow) {
                if (config.isDataLoggingEnabled()
                        && loggedGridRows % config.getDataLoggingInterval() == 0) {
                    dataContainer.insertValuesAtEnd(values, loggedGridRows * dt);
                }
                loggedGridRows++;
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
                .warnings(collectEngineWarnings())
                .metadata("solver", settings.getSolverType().toString())
                .metadata("matrixSolver", matrixSolver.getClass().getSimpleName())
                .metadata("adaptive", stepController != null)
                .metadata("rejectedSteps", stepController != null
                        ? stepController.getRejectedSteps() : 0)
                .metadata("dt", dt)
                .metadata("circuitFile", config.getCircuitFilePath() != null
                        ? config.getCircuitFilePath() : "in-memory model")
                .metadata("parameterOverrides", config.getParameterOverrides().size())
                .metadata("totalConductionLoss", lossEngine != null ? lossEngine.getTotalConductionLosses() : 0.0)
                .metadata("totalSwitchingLoss", lossEngine != null ? lossEngine.getTotalSwitchingLosses() : 0.0)
                .metadata("totalLossEnergy", lossEngine != null ? lossEngine.getCumulativeEnergy() : 0.0)
                .build();
    }

    /**
     * Non-fatal issues from netlist build and matrix stamping: unknown type
     * numbers, transformers without a model, element types with no stamper.
     */
    private java.util.List<String> collectEngineWarnings() {
        java.util.List<String> warnings = new ArrayList<>();
        if (circuitNetlist != null) {
            warnings.addAll(circuitNetlist.getBuildWarnings());
        }
        if (matrixSolver != null) {
            warnings.addAll(matrixSolver.getSkippedElementWarnings());
        }
        return warnings;
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



    /**
     * Deep-copies every element's parameter array (companion currents,
     * switch states) so a rejected adaptive attempt can roll back.
     */
    private static double[][] snapshotElementParameters(final INetList netlist) {
        final int count = netlist.getElementCount();
        final double[][] snapshot = new double[count][];
        for (int i = 0; i < count; i++) {
            snapshot[i] = netlist.getParameter(i).clone();
        }
        return snapshot;
    }

    /**
     * Restores a snapshot taken by {@link #snapshotElementParameters(INetList)}.
     */
    private static void restoreElementParameters(final INetList netlist,
                                                 final double[][] snapshot) {
        for (int i = 0; i < snapshot.length; i++) {
            System.arraycopy(snapshot[i], 0, netlist.getParameter(i), 0, snapshot[i].length);
        }
    }

    /**
     * Estimates the local truncation error of the attempted step by
     * re-solving the same step on a shadow solver of the complementary
     * integration method. The shadow sees the step-start companion state
     * (from the snapshot) combined with the switch states converged by the
     * attempt; the primary's post-attempt state is put back afterwards.
     *
     * @param attemptDt the attempted step width
     * @param time the current simulation time
     * @param preAttemptParameters element state snapshot from the step start
     * @return relative mixed error norm over all node potentials and z-currents
     */
    private double estimateLocalTruncationError(final MnaSolver shadowSolver,
                                                final double attemptDt, final double time,
                                                final double[][] preAttemptParameters) {
        // Seed the shadow with the primary's un-shifted history vectors
        System.arraycopy(matrixSolver.getPALT(), 0, shadowSolver.getPALT(), 0, matrixSolver.getMatrixSize());
        System.arraycopy(matrixSolver.getPALTALT(), 0, shadowSolver.getPALTALT(), 0, matrixSolver.getMatrixSize());
        System.arraycopy(matrixSolver.getPALTALTALT(), 0, shadowSolver.getPALTALTALT(), 0, matrixSolver.getMatrixSize());
        System.arraycopy(matrixSolver.getIALT(), 0, shadowSolver.getIALT(), 0, matrixSolver.getIALT().length);
        System.arraycopy(matrixSolver.getIALTALT(), 0, shadowSolver.getIALTALT(), 0, matrixSolver.getIALT().length);
        System.arraycopy(matrixSolver.getIALTALTALT(), 0, shadowSolver.getIALTALTALT(), 0, matrixSolver.getIALT().length);

        // Shadow state: step-start companion values + converged switch states
        final double[][] postAttemptParameters = snapshotElementParameters(circuitNetlist);
        restoreElementParameters(circuitNetlist, preAttemptParameters);
        for (int i = 0; i < postAttemptParameters.length; i++) {
            circuitNetlist.getParameter(i)[DiodeParameters.INDEX_CURRENT_RESISTANCE]
                    = postAttemptParameters[i][DiodeParameters.INDEX_CURRENT_RESISTANCE];
        }
        shadowSolver.buildMatrixA(circuitNetlist, attemptDt, time, false);
        shadowSolver.buildVectorB(circuitNetlist, attemptDt, time, false);
        shadowSolver.solve();
        final double[] shadowPotentials = shadowSolver.getP().clone();
        restoreElementParameters(circuitNetlist, postAttemptParameters);

        // Relative mixed norm: per-entry error over max(|primary|, floor)
        final double[] primaryPotentials = matrixSolver.getP();
        double maxError = 0.0;
        for (int i = 0; i < primaryPotentials.length; i++) {
            final double scale = Math.max(Math.abs(primaryPotentials[i]), LTE_ABSOLUTE_FLOOR);
            maxError = Math.max(maxError,
                    Math.abs(primaryPotentials[i] - shadowPotentials[i]) / scale);
        }
        return maxError;
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
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();

        // 1. Explicit request signals (from frontend or API caller)
        if (config.getSignals() != null && !config.getSignals().isEmpty()) {
            for (String s : config.getSignals()) {
                if (isValidSignalName(s)) {
                    names.add(s.trim());
                }
            }
        }

        // 2. All Scope channels from all Scope blocks in the circuit model
        if (circuitModel != null) {
            for (CircuitModel.ComponentData comp : circuitModel.getControlComponents()) {
                if (comp.getType() == 5 || comp.getType() == 1003
                        || (comp.getName() != null && (comp.getName().startsWith("SCOPE") || comp.getName().startsWith("OSZI")))) {
                    String[] inLabels = comp.getTerminalXLabels();
                    if (inLabels != null) {
                        for (String l : inLabels) {
                            if (isValidSignalName(l)) {
                                names.add(l.trim());
                            }
                        }
                    }
                }
            }
        }

        // 3. Measurement probes (Voltmeters, Ammeters) and signal taps
        if (controlCoupling != null) {
            for (ControlCalculatorBuilder.Probe probe : controlCoupling.probes()) {
                if (isValidSignalName(probe.name())) {
                    names.add(probe.name().trim());
                }
            }
            for (ControlCalculatorBuilder.SignalTap tap : controlCoupling.signalTaps()) {
                if (isValidSignalName(tap.name())) {
                    names.add(tap.name().trim());
                }
            }
        }

        // 4. Stored data container signals from original file
        if (circuitModel != null && circuitModel.getDataContainerSignals() != null) {
            for (String s : circuitModel.getDataContainerSignals()) {
                if (isValidSignalName(s)) {
                    names.add(s.trim());
                }
            }
        }

        // 5. Net labels from netlist
        if (circuitNetlist != null && circuitNetlist.getLabelResolver() != null) {
            for (String label : circuitNetlist.getLabelResolver().getAllLabels()) {
                if (isValidSignalName(label)) {
                    names.add(label.trim());
                }
            }
        }

        if (!names.isEmpty()) {
            return names.toArray(new String[0]);
        }
        return new String[] {"V_out", "I_in", "P_loss"};
    }

    private static boolean isValidSignalName(String s) {
        if (s == null) return false;
        String trimmed = s.trim();
        return !trimmed.isEmpty()
                && !trimmed.equals("[]")
                && !trimmed.equals("NIX")
                && !trimmed.equalsIgnoreCase("NIX_NIX_NIX")
                && !trimmed.equals("0")
                && !trimmed.equalsIgnoreCase("GND");
    }
}
