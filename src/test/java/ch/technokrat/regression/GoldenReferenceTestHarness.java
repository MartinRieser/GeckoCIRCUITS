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
            GeckoSim._isTestingMode = true;
            GeckoSim.operatingmode = OperatingMode.EXTERNAL;

            // Start GUI thread but keep it invisible
            Thread guiThread = new Thread() {
                @Override
                public void run() {
                    GeckoSim.main(new String[]{});
                }
            };
            guiThread.setName("GeckoCIRCUITS-Testing-Thread");
            guiThread.setPriority(Thread.MIN_PRIORITY);
            guiThread.start();

            // Wait for initialization to complete
            int maxWaitSeconds = 60;
            int waited = 0;
            while (!GeckoSim.mainLoaded && waited < maxWaitSeconds * 100) {
                Thread.sleep(10);
                waited++;
            }

            if (!GeckoSim.mainLoaded) {
                throw new RuntimeException("GeckoCIRCUITS failed to initialize within " + maxWaitSeconds + " seconds");
            }

            initialized = true;
            LOGGER.info("GeckoCIRCUITS initialized successfully");

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

            // Load the circuit file
            GeckoExternal.openFile(circuitFile.getAbsolutePath());

            // Small delay to allow file loading
            Thread.sleep(100);

            // Get simulation parameters before running
            double dt = GeckoExternal.get_dt();
            double tend = GeckoExternal.get_Tend();

            LOGGER.fine("Simulation parameters: dt=" + dt + ", tend=" + tend);

            // Run the simulation
            GeckoExternal.runSimulation();

            // Wait for simulation to complete (with timeout)
            int maxWaitMs = 300000; // 5 minutes
            int waitedMs = 0;
            int checkIntervalMs = 100;

            while (!GeckoSim._testSuccessful && waitedMs < maxWaitMs) {
                Thread.sleep(checkIntervalMs);
                waitedMs += checkIntervalMs;

                // Check if simulation is still running (optional: add check here)
            }

            if (!GeckoSim._testSuccessful) {
                throw new RuntimeException("Simulation did not complete successfully or timed out after "
                    + (maxWaitMs/1000) + " seconds");
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
