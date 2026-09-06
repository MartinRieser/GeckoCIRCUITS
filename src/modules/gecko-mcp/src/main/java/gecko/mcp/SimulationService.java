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

    /** Runs a simulation and parses the exported CSV into per-signal series. */
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
        Path csv = Files.createTempFile("gecko-mcp-", ".csv");
        try {
            SimulationCsv.write(result, csv);
            return ParsedCsv.parse(Files.readString(csv));
        } finally {
            Files.deleteIfExists(csv);
        }
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
