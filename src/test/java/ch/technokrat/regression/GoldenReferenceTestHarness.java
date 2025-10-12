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
 * Test harness for running simulations headlessly and capturing results.
 * This class provides the core functionality for golden reference testing.
 */
public class GoldenReferenceTestHarness {

    private static final Logger LOGGER = Logger.getLogger(GoldenReferenceTestHarness.class.getName());
    private static boolean initialized = false;

    /**
     * Initialize GeckoCIRCUITS in testing mode.
     * Must be called before running any simulations.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            LOGGER.info("Initializing GeckoCIRCUITS in testing mode...");

            // Set headless mode before any GUI initialization
            System.setProperty("java.awt.headless", "true");

            // Set testing mode before any initialization
            GeckoSim._isTestingMode = true;
            GeckoSim.operatingmode = OperatingMode.EXTERNAL;

            // Disable GUI features
            GeckoSim._initialShow = false;

            // Initialize GeckoCIRCUITS without GUI
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
            guiThread.setName("GeckoCIRCUITS-Testing-Thread");
            guiThread.setPriority(Thread.MIN_PRIORITY);
            guiThread.setDaemon(true); // Allow JVM to exit even if thread doesn't terminate
            guiThread.start();

            // Wait for initialization to complete
            int maxWaitSeconds = 60;
            int waited = 0;
            while (!GeckoSim.mainLoaded && waited < maxWaitSeconds * 100) {
                Thread.sleep(10);
                waited++;
            }

            // Give it more time - some components need additional time
            Thread.sleep(2000);

            if (!GeckoSim.mainLoaded) {
                LOGGER.warning("GeckoCIRCUITS did not fully initialize within " + maxWaitSeconds + " seconds, but continuing...");
                // Don't fail - we can still try to load files
            }

            initialized = true;
            LOGGER.info("GeckoCIRCUITS initialization completed (may have warnings)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Initialization interrupted", e);
        }
    }

    /**
     * Run a simulation on a circuit file and capture the results.
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

            // Load the circuit file - catch common errors
            try {
                // Ensure external interface is available
                if (GeckoSim._win != null) {
                    GeckoExternal.openFile(circuitFile.getAbsolutePath());
                } else {
                    LOGGER.warning("GeckoSim._win is null - GeckoExternal may not be available in headless mode");
                    // Try to use GeckoExternal directly - it may still work without full GUI
                    try {
                        GeckoExternal.openFile(circuitFile.getAbsolutePath());
                    } catch (Exception e2) {
                        LOGGER.log(Level.WARNING, "Failed to open file with GeckoExternal", e2);
                        // Fall back to a simpler approach - see if we can load file manually
                        throw new RuntimeException("Cannot open circuit file - GeckoCIRCUITS GUI components required", e2);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not open circuit file: " + circuitFile.getName(), e);
                throw new RuntimeException("Failed to open circuit file: " + e.getMessage(), e);
            }

            // Small delay to allow file loading
            Thread.sleep(500);

            // Get simulation parameters - these might fail if circuit doesn't load properly
            double dt = 0.0;
            double tend = 0.0;
            try {
                dt = GeckoExternal.get_dt();
                tend = GeckoExternal.get_Tend();
                LOGGER.fine("Simulation parameters: dt=" + dt + ", tend=" + tend);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not get simulation parameters for " + circuitFile.getName(), e);
                // Use default parameters - the simulation might still work
                dt = 1e-6;
                tend = 0.01;
            }

            // Run the simulation
            try {
                GeckoExternal.runSimulation();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Simulation run failed for " + circuitFile.getName(), e);
                throw new RuntimeException("Failed to run simulation: " + e.getMessage(), e);
            }

            // Wait for simulation to complete (with timeout)
            int maxWaitMs = 300000; // 5 minutes
            int waitedMs = 0;
            int checkIntervalMs = 500;

            while (!GeckoSim._testSuccessful && waitedMs < maxWaitMs) {
                Thread.sleep(checkIntervalMs);
                waitedMs += checkIntervalMs;

                // Log progress every 30 seconds
                if (waitedMs % 30000 == 0) {
                    LOGGER.info("Simulation running... " + (waitedMs/1000) + "s elapsed");
                }
            }

            if (!GeckoSim._testSuccessful) {
                throw new RuntimeException("Simulation did not complete successfully or timed out after "
                    + (maxWaitMs/1000) + " seconds for circuit: " + circuitFile.getName());
            }

            LOGGER.info("Simulation completed successfully");

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

            LOGGER.fine("Found " + controlElements.length + " control elements");

            // Try to capture data from scope elements
            for (String elementName : controlElements) {
                if (elementName == null || elementName.trim().isEmpty()) {
                    continue;
                }

                try {
                    // Try to get signal data (this may fail for non-scope elements)
                    // We'll skip a few points for efficiency
                    int skipPoints = 0; // Capture all points initially

                    double[] timeArray = GeckoExternal.getTimeArray(elementName, 0, tend, skipPoints);
                    float[] signalData = GeckoExternal.getSignalData(elementName, 0, tend, skipPoints);

                    if (timeArray != null && signalData != null && timeArray.length > 0) {
                        result.addSignal(elementName, timeArray, signalData);
                        LOGGER.fine("Captured signal: " + elementName + " (" + timeArray.length + " points)");
                    }

                } catch (Exception e) {
                    // Element is not a scope or doesn't have data - skip it
                    LOGGER.finest("Could not capture data from: " + elementName + " - " + e.getMessage());
                }
            }

            // Also try circuit elements that might have output data
            String[] circuitElements = GeckoExternal.getCircuitElements();
            if (circuitElements != null) {
                for (String elementName : circuitElements) {
                    if (elementName == null || elementName.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        double[] timeArray = GeckoExternal.getTimeArray(elementName, 0, tend, 0);
                        float[] signalData = GeckoExternal.getSignalData(elementName, 0, tend, 0);

                        if (timeArray != null && signalData != null && timeArray.length > 0) {
                            result.addSignal("circuit_" + elementName, timeArray, signalData);
                            LOGGER.fine("Captured circuit signal: " + elementName + " (" + timeArray.length + " points)");
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
            LOGGER.info("Shutting down test harness");
            initialized = false;
            // Additional cleanup if needed
        }
    }
}
