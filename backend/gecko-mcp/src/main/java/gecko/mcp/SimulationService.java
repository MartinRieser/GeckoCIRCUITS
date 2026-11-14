package gecko.mcp;

import gecko.core.allg.SolverType;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-process headless simulation, replacing the Python server's subprocess
 * calls to {@code gecko.core.GeckoHeadless} with direct engine use.
 */
final class SimulationService {

    private SimulationService() {
    }

    record RunResult(long totalSteps, List<String> signalNames, long executionTimeMs) {
    }

    static SolverType solver(String name) {
        return RunOptions.solver(name);
    }

    static RunResult simulate(Path circuit, Double duration, Double dt, String solver) throws IOException {
        double simDuration = duration != null ? duration : 20e-3;
        double step = dt != null ? dt : 1e-6;
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(circuit.toString())
                .solverType(solver(solver))
                .stepWidth(step)
                .simulationDuration(simDuration)
                .build();
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        if (!result.isSuccess()) {
            throw new IllegalStateException("Simulation failed: " + result.getErrorMessage());
        }
        List<String> names = new ArrayList<>(List.of(result.getSignalNames()));
        return new RunResult(result.getTotalTimeSteps(), names, result.getExecutionTimeMs());
    }

    /** Runs a simulation and constructs per-signal series directly from memory. */
    static ParsedCsv simulateToCsv(Path circuit, Double duration, Double dt) throws IOException {
        double simDuration = duration != null ? duration : 20e-3;
        double step = dt != null ? dt : 1e-6;
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(circuit.toString())
                .solverType(SolverType.SOLVER_BE)
                .stepWidth(step)
                .simulationDuration(simDuration)
                .build();
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        if (!result.isSuccess()) {
            throw new IllegalStateException("Simulation failed: " + result.getErrorMessage());
        }
        return fromSimulationResult(result);
    }

    /**
     * Runs a simulation with the full {@link RunOptions} argument surface and
     * constructs per-signal series directly from memory.
     */
    static ParsedCsv simulateToCsv(Path circuit, Map<String, Object> args) {
        SimulationResult result = RunOptions.runRaw(circuit, args);
        if (!result.isSuccess()) {
            throw new IllegalStateException("Simulation failed: " + result.getErrorMessage());
        }
        return fromSimulationResult(result);
    }

    static ParsedCsv fromSimulationResult(SimulationResult result) {
        String[] signalNames = result.getSignalNames();
        List<String> header = new ArrayList<>(signalNames.length + 1);
        header.add("time");
        header.addAll(List.of(signalNames));

        double[] times = result.getTimeArray();
        int rowCount = times.length;
        Map<String, List<Double>> columns = new LinkedHashMap<>();

        List<Double> timeCol = new ArrayList<>(rowCount);
        for (double t : times) {
            timeCol.add(t);
        }
        columns.put("time", timeCol);

        for (int s = 0; s < signalNames.length; s++) {
            float[] data = result.getSignalData(s);
            List<Double> col = new ArrayList<>(rowCount);
            if (data != null) {
                for (int t = 0; t < Math.min(rowCount, data.length); t++) {
                    col.add((double) data[t]);
                }
            }
            columns.put(signalNames[s], col);
        }
        return new ParsedCsv(header, columns);
    }

    /** Per-signal time-series table extracted from simulation results. */
    record ParsedCsv(List<String> header, Map<String, List<Double>> columns) {

        int rowCount() {
            return columns.isEmpty() ? 0 : columns.values().iterator().next().size();
        }
    }
}
