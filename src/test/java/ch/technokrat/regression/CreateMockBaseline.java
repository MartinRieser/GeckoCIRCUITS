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
import java.util.logging.Logger;

/**
 * Simple demo to create a mock baseline for testing the infrastructure.
 * This demonstrates that the baseline format works and can be loaded by tests.
 */
public class CreateMockBaseline {

    private static final Logger LOGGER = Logger.getLogger(CreateMockBaseline.class.getName());

    public static void main(String[] args) {
        try {
            // Create a mock baseline for Education_ETHZ/ex_1.ipes
            String circuitName = "Education_ETHZ/ex_1.ipes";
            double tend = 0.001; // 1ms simulation
            double dt = 1e-8;    // 10ns timestep

            SimulationResult mockResult = new SimulationResult(circuitName, tend, dt);

            // Add some mock signal data
            double[] time = new double[100];
            float[] signal1 = new float[100];
            float[] signal2 = new float[100];

            // Generate mock data: sine wave and cosine wave
            for (int i = 0; i < 100; i++) {
                time[i] = i * dt * 1000; // Sample every 1000 steps
                signal1[i] = (float) Math.sin(2 * Math.PI * 1000 * time[i]); // 1kHz sine
                signal2[i] = (float) Math.cos(2 * Math.PI * 1000 * time[i]); // 1kHz cosine
            }

            mockResult.addSignal("Scope1_voltage", time, signal1);
            mockResult.addSignal("Scope1_current", time, signal2);

            // Save the mock baseline
            ResultSerializer.saveAsCSV(mockResult);

            LOGGER.info("Mock baseline created for " + circuitName);
            LOGGER.info("Checksum: " + mockResult.getChecksum());
            LOGGER.info("Signals: " + mockResult.getSignals().size());
            LOGGER.info("");
            LOGGER.info("You can now run:");
            LOGGER.info("  mvn test -Dtest=GoldenReferenceTest -Dcircuit=\"Education_ETHZ/ex_1.ipes\"");
            LOGGER.info("");
            LOGGER.info("This will test the infrastructure with the mock baseline.");

        } catch (IOException e) {
            LOGGER.severe("Failed to create mock baseline: " + e.getMessage());
        }
    }
}