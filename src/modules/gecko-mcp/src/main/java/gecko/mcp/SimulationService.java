package gecko.mcp;

import gecko.core.allg.SolverType;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationCsv;
import gecko.core.simulation.SimulationResult;

import java.io.IOException;
import java.nio.file.Files;
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
        return switch (name.toLowerCase()) {
            case "be", "backward-euler" -> SolverType.SOLVER_BE;
            case "trz", "trapezoidal" -> SolverType.SOLVER_TRZ;
            case "gs", "gear-shichman" -> SolverType.SOLVER_GS;
            default -> throw new IllegalArgumentException("Unknown solver: " + name);
        };
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

    /** Column-wise CSV parse mirroring the Python tool's line parsing. */
    record ParsedCsv(List<String> header, Map<String, List<Double>> columns) {

        static ParsedCsv parse(String content) {
            List<String> lines = List.of(content.split("\n"));
            if (lines.isEmpty()) {
                return new ParsedCsv(List.of(), Map.of());
            }
            List<String> header = List.of(stripCr(lines.get(0)).split(","));
            Map<String, List<Double>> columns = new LinkedHashMap<>();
            for (String name : header) {
                columns.put(name, new ArrayList<>());
            }
            for (int i = 1; i < lines.size(); i++) {
                String line = stripCr(lines.get(i));
                String[] parts = line.split(",", -1);
                if (parts.length != header.size()) {
                    continue;
                }
                try {
                    for (int c = 0; c < header.size(); c++) {
                        columns.get(header.get(c)).add(Double.parseDouble(parts[c]));
                    }
                } catch (NumberFormatException e) {
                    // Python port: malformed rows are skipped
                }
            }
            return new ParsedCsv(header, columns);
        }

        private static String stripCr(String line) {
            return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
        }

        int rowCount() {
            return columns.isEmpty() ? 0 : columns.values().iterator().next().size();
        }
    }
}
