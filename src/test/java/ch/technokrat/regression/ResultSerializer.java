/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.regression;

import java.io.*;
import java.util.Map;

/**
 * Serializes and deserializes SimulationResult objects to/from disk.
 * Supports both CSV (human-readable) and binary formats.
 */
public class ResultSerializer {

    private static final String BASELINE_DIR = "src/test/resources/golden/baselines";
    private static final String METADATA_DIR = "src/test/resources/golden/metadata";

    /**
     * Save a simulation result as CSV files (one per signal).
     * Creates a directory structure matching the circuit location.
     *
     * @param result The simulation result to save
     * @throws IOException if writing fails
     */
    public static void saveAsCSV(SimulationResult result) throws IOException {
        File baseDir = new File(BASELINE_DIR);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        // Create subdirectory based on circuit path
        String circuitPath = result.getCircuitName().replace(".ipes", "");
        File circuitDir = new File(baseDir, circuitPath);
        circuitDir.mkdirs();

        // Save metadata
        File metadataFile = new File(circuitDir, "_metadata.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(metadataFile))) {
            writer.println("circuit=" + result.getCircuitName());
            writer.println("simulationTime=" + result.getSimulationTime());
            writer.println("timestep=" + result.getTimestep());
            writer.println("checksum=" + result.getChecksum());
            writer.println("signalCount=" + result.getSignals().size());
        }

        // Save each signal as CSV
        for (Map.Entry<String, SimulationResult.SignalData> entry : result.getSignals().entrySet()) {
            String signalName = sanitizeFilename(entry.getKey());
            File signalFile = new File(circuitDir, signalName + ".csv");

            SimulationResult.SignalData signal = entry.getValue();
            double[] timeArray = signal.getTimeArray();
            float[] values = signal.getValues();

            try (PrintWriter writer = new PrintWriter(new FileWriter(signalFile))) {
                writer.println("time,value");
                for (int i = 0; i < timeArray.length; i++) {
                    writer.println(timeArray[i] + "," + values[i]);
                }
            }
        }
    }

    /**
     * Load a simulation result from CSV files.
     *
     * @param circuitName The relative path of the circuit (e.g., "Topologies/BuckBoost_thermal.ipes")
     * @return The loaded SimulationResult
     * @throws IOException if reading fails or files don't exist
     */
    public static SimulationResult loadFromCSV(String circuitName) throws IOException {
        File baseDir = new File(BASELINE_DIR);
        String circuitPath = circuitName.replace(".ipes", "");
        File circuitDir = new File(baseDir, circuitPath);

        if (!circuitDir.exists() || !circuitDir.isDirectory()) {
            throw new IOException("Baseline not found for circuit: " + circuitName);
        }

        // Load metadata
        File metadataFile = new File(circuitDir, "_metadata.txt");
        if (!metadataFile.exists()) {
            throw new IOException("Metadata file not found: " + metadataFile.getAbsolutePath());
        }

        double simulationTime = 0;
        double timestep = 0;
        String checksum = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("simulationTime=")) {
                    simulationTime = Double.parseDouble(line.substring("simulationTime=".length()));
                } else if (line.startsWith("timestep=")) {
                    timestep = Double.parseDouble(line.substring("timestep=".length()));
                } else if (line.startsWith("checksum=")) {
                    checksum = line.substring("checksum=".length());
                }
            }
        }

        SimulationResult result = new SimulationResult(circuitName, simulationTime, timestep);

        // Load all CSV files in the directory
        File[] csvFiles = circuitDir.listFiles((dir, name) -> name.endsWith(".csv"));
        if (csvFiles == null) {
            return result.withChecksum();
        }

        for (File csvFile : csvFiles) {
            String signalName = csvFile.getName().replace(".csv", "");
            loadSignalFromCSV(csvFile, signalName, result);
        }

        return result.withChecksum();
    }

    private static void loadSignalFromCSV(File csvFile, String signalName, SimulationResult result) throws IOException {
        java.util.List<Double> timeList = new java.util.ArrayList<>();
        java.util.List<Float> valueList = new java.util.ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    timeList.add(Double.parseDouble(parts[0]));
                    valueList.add(Float.parseFloat(parts[1]));
                }
            }
        }

        double[] timeArray = new double[timeList.size()];
        float[] values = new float[valueList.size()];

        for (int i = 0; i < timeList.size(); i++) {
            timeArray[i] = timeList.get(i);
            values[i] = valueList.get(i);
        }

        result.addSignal(signalName, timeArray, values);
    }

    /**
     * Sanitize filename to remove invalid characters.
     */
    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Check if a baseline exists for a given circuit.
     */
    public static boolean baselineExists(String circuitName) {
        File baseDir = new File(BASELINE_DIR);
        String circuitPath = circuitName.replace(".ipes", "");
        File circuitDir = new File(baseDir, circuitPath);
        File metadataFile = new File(circuitDir, "_metadata.txt");
        return metadataFile.exists();
    }
}
