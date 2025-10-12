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
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * Test that verifies the mock baseline infrastructure works correctly.
 * This test loads existing baseline files and verifies they can be loaded,
 * compared, and have valid checksums without requiring actual simulation runs.
 */
@RunWith(Parameterized.class)
public class MockBaselineTest {

    private static final Logger LOGGER = Logger.getLogger(MockBaselineTest.class.getName());

    @Parameterized.Parameter
    public String circuitName;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> circuits() throws IOException {
        return CircuitDiscovery.findAllCircuits().stream()
            .map(CircuitDiscovery::getRelativePath)
            .filter(ResultSerializer::baselineExists)
            .sorted()
            .collect(java.util.stream.Collectors.toList());
    }

    @Test
    public void testBaselineExists() {
        assertTrue("Baseline should exist for " + circuitName,
                  ResultSerializer.baselineExists(circuitName));
    }

    @Test
    public void testBaselineCanBeLoaded() throws Exception {
        SimulationResult baseline = ResultSerializer.loadFromCSV(circuitName);
        assertNotNull("Baseline should load successfully for " + circuitName, baseline);
        assertEquals("Circuit name should match", circuitName, baseline.getCircuitName());
        assertFalse("Should have at least one signal", baseline.getSignals().isEmpty());
        assertTrue("Should have valid simulation end time", baseline.getSimulationTime() > 0);
        assertTrue("Should have valid timestep", baseline.getTimestep() > 0);
    }

    @Test
    public void testBaselineHasValidChecksum() throws Exception {
        SimulationResult baseline = ResultSerializer.loadFromCSV(circuitName);
        String checksum = baseline.getChecksum();
        assertNotNull("Checksum should not be null", checksum);
        assertFalse("Checksum should not be empty", checksum.isEmpty());
        assertEquals("Checksum should be 64 characters (SHA-256)", 64, checksum.length());
    }

    @Test
    public void testBaselineSignalsHaveValidData() throws Exception {
        SimulationResult baseline = ResultSerializer.loadFromCSV(circuitName);

        for (SimulationResult.SignalData signal : baseline.getSignals().values()) {
            assertNotNull("Signal name should not be null", signal.getName());
            assertFalse("Signal name should not be empty", signal.getName().isEmpty());
            assertNotNull("Time data should not be null", signal.getTimeArray());
            assertNotNull("Value data should not be null", signal.getValues());
            assertEquals("Time and value arrays should have same length",
                        signal.getTimeArray().length, signal.getValues().length);
            assertTrue("Should have at least one data point", signal.getTimeArray().length > 0);

            double[] timeArray = signal.getTimeArray();
            float[] values = signal.getValues();

            // Check for valid numeric values (no NaN or Infinity)
            for (int i = 0; i < values.length; i++) {
                assertFalse("Time should not be NaN at index " + i, Double.isNaN(timeArray[i]));
                assertFalse("Time should not be infinite at index " + i,
                           Double.isInfinite(timeArray[i]));
                assertFalse("Value should not be NaN at index " + i, Float.isNaN(values[i]));
                assertFalse("Value should not be infinite at index " + i,
                           Float.isInfinite(values[i]));
            }

            // Check that time is monotonically increasing
            for (int i = 1; i < timeArray.length; i++) {
                assertTrue("Time should be monotonically increasing at index " + i,
                          timeArray[i] > timeArray[i-1]);
            }
        }
    }

    @Test
    public void testBaselineConsistency() throws Exception {
        // Load the same baseline twice and verify they match exactly
        SimulationResult baseline1 = ResultSerializer.loadFromCSV(circuitName);
        SimulationResult baseline2 = ResultSerializer.loadFromCSV(circuitName);

        assertEquals("Circuit names should match", baseline1.getCircuitName(), baseline2.getCircuitName());
        assertEquals("Simulation end time should match", baseline1.getSimulationTime(), baseline2.getSimulationTime(), 1e-15);
        assertEquals("Timestep should match", baseline1.getTimestep(), baseline2.getTimestep(), 1e-20);
        assertEquals("Checksums should match", baseline1.getChecksum(), baseline2.getChecksum());
        assertEquals("Number of signals should match", baseline1.getSignals().size(), baseline2.getSignals().size());

        // Compare each signal
        for (String signalName : baseline1.getSignals().keySet()) {
            SimulationResult.SignalData signal1 = baseline1.getSignals().get(signalName);
            SimulationResult.SignalData signal2 = baseline2.getSignals().get(signalName);

            assertEquals("Signal names should match", signal1.getName(), signal2.getName());
            assertArrayEquals("Time arrays should match", signal1.getTimeArray(), signal2.getTimeArray(), 1e-15);
            assertArrayEquals("Value arrays should match", signal1.getValues(), signal2.getValues(), 1e-10f);
        }
    }

    @Test
    public void testBaselineComparisonWorks() throws Exception {
        SimulationResult baseline = ResultSerializer.loadFromCSV(circuitName);

        // Compare baseline with itself - should match perfectly
        ResultComparator.ComparisonResult comparison =
            ResultComparator.compare(baseline, baseline, ResultComparator.ToleranceLevel.STRICT);

        assertTrue("Baseline should compare perfectly with itself", comparison.matches());
        assertEquals("Max absolute error should be zero", 0.0, comparison.getMaxAbsoluteError(), 1e-15);
        assertEquals("Max relative error should be zero", 0.0, comparison.getMaxRelativeError(), 1e-15);
        assertTrue("Should have no differences", comparison.getDifferences().isEmpty());
    }
}