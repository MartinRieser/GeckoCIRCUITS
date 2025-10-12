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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.*;

/**
 * GUI-enabled utility to capture baseline simulation results for all circuits.
 * This version uses the GUI components required by GeckoCIRCUITS to load circuit files.
 *
 * Usage:
 *   java -cp target/test-classes:target/classes ch.technokrat.regression.BaselineCaptureGUI
 *
 * Or via Maven:
 *   mvn test-compile exec:java -Dexec.mainClass="ch.technokrat.regression.BaselineCaptureGUI" \
 *                               -Dexec.classpathScope="test"
 *
 * Options (via system properties):
 *   -Dcircuits.dir=path/to/circuits  : Specify custom circuits directory
 *   -Dcircuits.filter=Topologies     : Only capture circuits in specific subdirectory
 *   -Dbaseline.overwrite=true        : Overwrite existing baselines
 *   -Dbaseline.verify=true           : Verify by running twice and comparing
 */
public class BaselineCaptureGUI {

    private static final Logger LOGGER = Logger.getLogger(BaselineCaptureGUI.class.getName());

    private boolean overwriteExisting = false;
    private boolean verifyBaselines = false;
    private String filterDirectory = null;

    public static void main(String[] args) {
        setupLogging();

        BaselineCaptureGUI capture = new BaselineCaptureGUI();
        capture.parseOptions();

        try {
            capture.captureBaselines();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Baseline capture failed", e);
            System.exit(1);
        }
    }

    private void parseOptions() {
        overwriteExisting = Boolean.getBoolean("baseline.overwrite");
        verifyBaselines = Boolean.getBoolean("baseline.verify");
        filterDirectory = System.getProperty("circuits.filter");

        LOGGER.info("GUI Baseline Capture Configuration:");
        LOGGER.info("  Overwrite existing: " + overwriteExisting);
        LOGGER.info("  Verify baselines: " + verifyBaselines);
        LOGGER.info("  Filter directory: " + (filterDirectory != null ? filterDirectory : "none"));
        LOGGER.info("  GUI Mode: ENABLED (requires display)");
    }

    private void captureBaselines() throws Exception {
        LOGGER.info("Starting GUI baseline capture...");
        LOGGER.info("Note: This will open GeckoCIRCUITS GUI components for circuit loading");

        // Initialize the GUI test harness
        GoldenReferenceTestHarnessGUI.initialize();

        // Discover circuits
        List<File> circuits;
        if (filterDirectory != null) {
            circuits = CircuitDiscovery.findCircuitsInDirectory(filterDirectory);
            LOGGER.info("Found " + circuits.size() + " circuits in " + filterDirectory);
        } else {
            circuits = CircuitDiscovery.findAllCircuits();
            LOGGER.info("Found " + circuits.size() + " total circuits");
        }

        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        // Process each circuit
        for (int i = 0; i < circuits.size(); i++) {
            File circuitFile = circuits.get(i);
            String circuitName = CircuitDiscovery.getRelativePath(circuitFile);

            LOGGER.info("Processing (" + (i + 1) + "/" + circuits.size() + "): " + circuitName);

            try {
                // Check if baseline already exists
                if (!overwriteExisting && ResultSerializer.baselineExists(circuitName)) {
                    LOGGER.info("  Skipping - baseline already exists");
                    skippedCount++;
                    continue;
                }

                // Run simulation and capture results
                SimulationResult result = GoldenReferenceTestHarnessGUI.runCircuitSimulation(circuitFile);

                // Check if we got any signals
                if (result.getSignals().isEmpty()) {
                    LOGGER.warning("  No signals captured - circuit may not have scopes or simulation failed");
                    failureCount++;
                    continue;
                }

                // Save the baseline
                ResultSerializer.saveAsCSV(result);
                LOGGER.info("  Saved baseline with " + result.getSignals().size() + " signals");
                LOGGER.info("  Checksum: " + result.getChecksum());

                // Verify if requested
                if (verifyBaselines) {
                    verifyBaseline(circuitFile, circuitName, result);
                }

                successCount++;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "  Failed to process " + circuitName, e);
                failureCount++;
            }

            // Small delay between circuits to let GUI settle
            Thread.sleep(1000);
        }

        // Summary
        LOGGER.info("=====================================");
        LOGGER.info("GUI Baseline Capture Complete");
        LOGGER.info("  Total circuits: " + circuits.size());
        LOGGER.info("  Successfully captured: " + successCount);
        LOGGER.info("  Failed: " + failureCount);
        LOGGER.info("  Skipped (existing): " + skippedCount);
        LOGGER.info("=====================================");

        GoldenReferenceTestHarnessGUI.shutdown();
    }

    /**
     * Verify a baseline by running the simulation again and comparing results.
     */
    private void verifyBaseline(File circuitFile, String circuitName, SimulationResult firstRun)
            throws Exception {
        LOGGER.info("  Verifying baseline reproducibility...");

        // Run simulation again
        SimulationResult secondRun = GoldenReferenceTestHarnessGUI.runCircuitSimulation(circuitFile);

        // Compare results
        ResultComparator.ComparisonResult comparison =
            ResultComparator.compare(firstRun, secondRun, ResultComparator.ToleranceLevel.STRICT);

        if (comparison.matches()) {
            LOGGER.info("  Verification PASSED - results are reproducible");
        } else {
            LOGGER.warning("  Verification FAILED - results differ between runs!");
            LOGGER.warning("  Max absolute error: " + comparison.getMaxAbsoluteError());
            LOGGER.warning("  Max relative error: " + comparison.getMaxRelativeError());
            LOGGER.warning("  This indicates non-deterministic behavior!");
        }
    }

    private static void setupLogging() {
        // Configure logging to show INFO and above on console
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);

        // Set console handler to INFO
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.INFO);
                handler.setFormatter(new SimpleFormatter() {
                    @Override
                    public synchronized String format(LogRecord record) {
                        return String.format("[%s] %s: %s%n",
                            record.getLevel(),
                            record.getLoggerName().substring(record.getLoggerName().lastIndexOf('.') + 1),
                            record.getMessage());
                    }
                });
            }
        }
    }
}