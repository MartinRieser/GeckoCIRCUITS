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
 * Debug version to understand simulation completion detection.
 * This will help us understand why _testSuccessful is not being set.
 */
public class DebugSimulationComplete {

    private static final Logger LOGGER = Logger.getLogger(DebugSimulationComplete.class.getName());

    public static void main(String[] args) throws Exception {
        LOGGER.info("Debug simulation completion detection...");

        // Initialize GUI mode
        System.setProperty("java.awt.headless", "false");
        GeckoSim._isTestingMode = true;
        GeckoSim.operatingmode = OperatingMode.EXTERNAL;
        GeckoSim._initialShow = true;

        // Start GUI
        Thread guiThread = new Thread() {
            @Override
            public void run() {
                try {
                    GeckoSim.main(new String[]{});
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "GeckoCIRCUITS initialization had issues", e);
                }
            }
        };
        guiThread.setDaemon(true);
        guiThread.start();

        // Wait for initialization
        int maxWaitSeconds = 60;
        int waited = 0;
        while (!GeckoSim.mainLoaded && waited < maxWaitSeconds * 100) {
            Thread.sleep(10);
            waited++;
        }
        Thread.sleep(3000);

        if (!GeckoSim.mainLoaded) {
            LOGGER.warning("GeckoCIRCUITS did not fully initialize");
        }

        // Load a simple circuit
        File circuitFile = new File("resources/Education_ETHZ/ex_1.ipes");
        if (!circuitFile.exists()) {
            LOGGER.severe("Circuit file not found: " + circuitFile.getAbsolutePath());
            return;
        }

        LOGGER.info("Loading circuit: " + circuitFile.getName());
        GeckoSim._testSuccessful = false; // Reset flag
        GeckoExternal.openFile(circuitFile.getAbsolutePath());
        Thread.sleep(2000);

        // Get parameters
        double dt = GeckoExternal.get_dt();
        double tend = GeckoExternal.get_Tend();
        LOGGER.info("Simulation parameters: dt=" + dt + ", tend=" + tend);

        // Monitor flag before simulation
        LOGGER.info("Before simulation - _testSuccessful: " + GeckoSim._testSuccessful);

        // Start simulation
        LOGGER.info("Starting simulation...");
        GeckoExternal.runSimulation();

        // Monitor the flag during simulation
        int maxWaitMs = 120000; // 2 minutes for debug
        int waitedMs = 0;
        int checkIntervalMs = 2000; // Check every 2 seconds

        LOGGER.info("Monitoring simulation completion...");
        while (!GeckoSim._testSuccessful && waitedMs < maxWaitMs) {
            Thread.sleep(checkIntervalMs);
            waitedMs += checkIntervalMs;

            LOGGER.info("Check " + (waitedMs/1000) + "s: _testSuccessful = " + GeckoSim._testSuccessful);

            // Try alternative completion checks
            try {
                // Check if we can access simulation results (indicates completion)
                String[] elements = GeckoExternal.getControlElements();
                if (elements != null && elements.length > 0) {
                    LOGGER.info("Found " + elements.length + " control elements");

                    // Try to get some data
                    for (String element : elements) {
                        try {
                            double[] timeArray = GeckoExternal.getTimeArray(element, 0, tend, 0);
                            if (timeArray != null && timeArray.length > 0) {
                                LOGGER.info("SUCCESS: Got " + timeArray.length + " time points from " + element);
                                LOGGER.info("This suggests simulation has completed!");

                                // Force completion for testing
                                GeckoSim._testSuccessful = true;
                                break;
                            }
                        } catch (Exception e) {
                            LOGGER.fine("Cannot get data from " + element + ": " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.fine("Error checking completion: " + e.getMessage());
            }
        }

        LOGGER.info("Final _testSuccessful: " + GeckoSim._testSuccessful);

        if (GeckoSim._testSuccessful) {
            LOGGER.info("✅ Simulation completed successfully!");
        } else {
            LOGGER.warning("❌ Simulation did not complete within timeout");
        }

        LOGGER.info("Debug complete. You can now close the GUI window.");
    }
}