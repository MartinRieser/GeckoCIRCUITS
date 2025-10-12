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

import ch.technokrat.gecko.GeckoExternal;
import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.OperatingMode;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI-enabled version of the golden reference test harness.
 * Use this version when you need to capture baselines for full circuit simulations.
 * Requires a display and GUI components to be available.
 */
public class GoldenReferenceTestHarnessGUI {

    private static final Logger LOGGER = Logger.getLogger(GoldenReferenceTestHarnessGUI.class.getName());
    private static boolean initialized = false;

    /**
     * Initialize GeckoCIRCUITS in GUI mode for baseline capture.
     * Must be called before running any simulations.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            LOGGER.info("Initializing GeckoCIRCUITS in GUI mode for baseline capture...");

            // Enable GUI mode
            System.setProperty("java.awt.headless", "false");
            GeckoSim._isTestingMode = true;
            GeckoSim.operatingmode = OperatingMode.EXTERNAL;
            GeckoSim._initialShow = true; // Allow GUI to be visible

            // Initialize GeckoCIRCUITS with GUI
            Thread guiThread = new Thread() {
                @Override
                public void run() {
                    try {
                        GeckoSim.main(new String[]{});
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "GeckoCIRCUITS initialization had issues, but continuing...", e);
                    }
                }
            };
            guiThread.setName("GeckoCIRCUITS-GUI-Baseline-Capture");
            guiThread.setPriority(Thread.MIN_PRIORITY);
            guiThread.start();

            // Wait for initialization to complete
            int maxWaitSeconds = 120; // Give it more time for GUI initialization
            int waited = 0;
            while (!GeckoSim.mainLoaded && waited < maxWaitSeconds * 100) {
                Thread.sleep(10);
                waited++;
            }

            // Additional wait for GUI components to be ready
            Thread.sleep(5000);

            if (!GeckoSim.mainLoaded) {
                LOGGER.warning("GeckoCIRCUITS did not fully initialize within " + maxWaitSeconds + " seconds, but continuing...");
            }

            if (GeckoSim._win == null) {
                LOGGER.warning("GeckoSim._win is still null, but GeckoExternal may still work...");
            }

            initialized = true;
            LOGGER.info("GeckoCIRCUITS GUI initialization completed");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Initialization interrupted", e);
        }
    }

    /**
     * Run a simulation on a circuit file and capture the results in GUI mode.
     *
     * @param circuitFile The .ipes circuit file to simulate
     * @return SimulationResult containing signal data and metadata
     * @throws Exception if simulation fails
     */
    public static SimulationResult runCircuitSimulation(File circuitFile) throws Exception {
        if (!initialized) {
            initialize();
        }

        if (!circuitFile.exists()) {
            throw new IllegalArgumentException("Circuit file does not exist: " + circuitFile.getAbsolutePath());
        }

        LOGGER.info("Running simulation for: " + circuitFile.getName());

        try {
            // Reset success flag
            GeckoSim._testSuccessful = false;

            // Load the circuit file
            try {
                LOGGER.info("Opening circuit file: " + circuitFile.getAbsolutePath());
                GeckoExternal.openFile(circuitFile.getAbsolutePath());
                LOGGER.info("Circuit file opened successfully");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not open circuit file: " + circuitFile.getName(), e);
                throw new RuntimeException("Failed to open circuit file: " + e.getMessage(), e);
            }

            // Give GUI time to process the file
            Thread.sleep(2000);

            // Get simulation parameters
            double dt = 0.0;
            double tend = 0.0;
            try {
                dt = GeckoExternal.get_dt();
                tend = GeckoExternal.get_Tend();
                LOGGER.info("Simulation parameters: dt=" + dt + ", tend=" + tend);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not get simulation parameters for " + circuitFile.getName(), e);
                dt = 1e-6;
                tend = 0.01;
            }

            // Run the simulation
            try {
                LOGGER.info("Starting simulation...");
                GeckoExternal.runSimulation();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Simulation run failed for " + circuitFile.getName(), e);
                throw new RuntimeException("Failed to run simulation: " + e.getMessage(), e);
            }

            // Wait for simulation to complete
            int maxWaitMs = 600000; // 10 minutes for GUI mode
            int waitedMs = 0;
            int checkIntervalMs = 2000; // Check every 2 seconds
            boolean simulationCompleted = false;

            while (!simulationCompleted && waitedMs < maxWaitMs) {
                Thread.sleep(checkIntervalMs);
                waitedMs += checkIntervalMs;

                // Log progress every 30 seconds
                if (waitedMs % 30000 == 0) {
                    LOGGER.info("Simulation running... " + (waitedMs/1000) + "s elapsed");
                }

                // Check for simulation completion by testing if data is available
                // In GUI mode, _testSuccessful may not be set automatically
                try {
                    String[] controlElements = GeckoExternal.getControlElements();
                    if (controlElements != null && controlElements.length > 0) {
                        // Try to get data from any control element
                        for (String elementName : controlElements) {
                            if (elementName == null || elementName.trim().isEmpty()) {
                                continue;
                            }

                            try {
                                double[] timeArray = GeckoExternal.getTimeArray(elementName, 0, tend, 0);
                                if (timeArray != null && timeArray.length > 0) {
                                    LOGGER.info("Simulation completed - found " + timeArray.length + " data points from " + elementName);
                                    simulationCompleted = true;
                                    GeckoSim._testSuccessful = true; // Set flag for consistency
                                    break;
                                }
                            } catch (Exception e) {
                                // Element doesn't have data - continue checking
                                LOGGER.finest("No data from " + elementName + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.finest("Error checking completion status: " + e.getMessage());
                }
            }

            if (!simulationCompleted) {
                throw new RuntimeException("Simulation did not complete successfully or timed out after "
                    + (maxWaitMs/1000) + " seconds for circuit: " + circuitFile.getName());
            }

            LOGGER.info("Simulation completed successfully");

            // Give more time for signal data to be ready for capture
            Thread.sleep(5000);

            // Capture results
            SimulationResult result = captureResults(circuitFile, dt, tend);

            return result;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Simulation failed for " + circuitFile.getName(), e);
            throw e;
        }
    }

    /**
     * Capture simulation results from all available scopes/signals.
     *
     * @param circuitFile The circuit file that was simulated
     * @param dt Timestep used
     * @param tend End time of simulation
     * @return SimulationResult containing all captured data
     */
    private static SimulationResult captureResults(File circuitFile, double dt, double tend) {
        String circuitName = CircuitDiscovery.getRelativePath(circuitFile);
        SimulationResult result = new SimulationResult(circuitName, tend, dt);

        try {
            // Get all control elements (which includes scopes)
            String[] controlElements = GeckoExternal.getControlElements();

            if (controlElements == null || controlElements.length == 0) {
                LOGGER.warning("No control elements found in circuit: " + circuitName);
                return result.withChecksum();
            }

            LOGGER.info("Found " + controlElements.length + " control elements");

            // Try to capture data from scope elements
            for (String elementName : controlElements) {
                if (elementName == null || elementName.trim().isEmpty()) {
                    continue;
                }

                try {
                    // Try to get signal data with retry mechanism for timing issues
                    double[] timeArray = GeckoExternal.getTimeArray(elementName, 0, tend, 0);
                    float[] signalData = null;

                    if (timeArray != null && timeArray.length > 0) {
                        // Retry signal data capture up to 5 times with delays
                        int maxRetries = 5;
                        for (int retry = 0; retry < maxRetries; retry++) {
                            try {
                                signalData = GeckoExternal.getSignalData(elementName, 0, tend, 0);

                                if (signalData != null && signalData.length > 0) {
                                    LOGGER.info("Got signal data for " + elementName + " on attempt " + (retry + 1));
                                    break;
                                } else if (retry < maxRetries - 1) {
                                    LOGGER.info("No signal data for " + elementName + " (attempt " + (retry + 1) + "/" + maxRetries + "), retrying in 2s...");
                                    Thread.sleep(2000);
                                }
                            } catch (Exception e) {
                                if (retry < maxRetries - 1) {
                                    LOGGER.info("Signal data error for " + elementName + " (attempt " + (retry + 1) + "/" + maxRetries + "): " + e.getMessage());
                                    Thread.sleep(2000);
                                }
                            }
                        }
                    }

                    if (timeArray != null && signalData != null && timeArray.length > 0 && signalData.length > 0) {
                        // Check if arrays have same length
                        if (timeArray.length == signalData.length) {
                            result.addSignal(elementName, timeArray, signalData);
                            LOGGER.info("✅ Captured signal: " + elementName + " (" + timeArray.length + " points)");
                        } else {
                            // Arrays have different lengths - truncate to minimum
                            int minLength = Math.min(timeArray.length, signalData.length);
                            if (minLength > 0) {
                                // Create truncated arrays
                                double[] truncatedTime = new double[minLength];
                                float[] truncatedSignal = new float[minLength];
                                System.arraycopy(timeArray, 0, truncatedTime, 0, minLength);
                                System.arraycopy(signalData, 0, truncatedSignal, 0, minLength);

                                result.addSignal(elementName, truncatedTime, truncatedSignal);
                                LOGGER.info("✅ Captured signal (truncated): " + elementName + " (" + minLength + " points, was " + timeArray.length + " time, " + signalData.length + " signal)");
                            } else {
                                LOGGER.info("❌ No valid data for: " + elementName + " (time: " + timeArray.length + ", signal: " + signalData.length + ")");
                            }
                        }
                    } else {
                        LOGGER.info("❌ No data available for element: " + elementName +
                                   (timeArray != null ? " (time: " + timeArray.length + ")" : " (no time data)") +
                                   (signalData != null ? " (signal: " + signalData.length + ")" : " (no signal data)"));
                    }

                } catch (Exception e) {
                    // Element is not a scope or doesn't have data - log at info level for debugging
                    LOGGER.info("❌ Could not capture data from: " + elementName + " - " + e.getMessage());
                }
            }

            // Also try circuit elements that might have output data
            String[] circuitElements = GeckoExternal.getCircuitElements();
            if (circuitElements != null) {
                LOGGER.info("Found " + circuitElements.length + " circuit elements");
                for (String elementName : circuitElements) {
                    if (elementName == null || elementName.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        double[] timeArray = GeckoExternal.getTimeArray(elementName, 0, tend, 0);
                        float[] signalData = GeckoExternal.getSignalData(elementName, 0, tend, 0);

                        if (timeArray != null && signalData != null && timeArray.length > 0) {
                            result.addSignal("circuit_" + elementName, timeArray, signalData);
                            LOGGER.info("Captured circuit signal: " + elementName + " (" + timeArray.length + " points)");
                        }
                    } catch (Exception e) {
                        // Skip elements without data
                        LOGGER.finest("Could not capture circuit data from: " + elementName);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error capturing results for " + circuitName, e);
        }

        return result.withChecksum();
    }

    /**
     * Shutdown the test harness (cleanup).
     */
    public static synchronized void shutdown() {
        if (initialized) {
            LOGGER.info("Shutting down GUI test harness");
            // Give some time for any cleanup
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            initialized = false;
        }
    }
}