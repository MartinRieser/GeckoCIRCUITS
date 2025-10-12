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

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Parameterized JUnit test for golden reference validation.
 * This test runs all circuits that have baseline data and compares
 * the current simulation results against the golden reference.
 *
 * To run all tests:
 *   mvn test -Dtest=GoldenReferenceTest
 *
 * To run a specific circuit:
 *   mvn test -Dtest=GoldenReferenceTest -Dcircuit="Topologies/BuckBoost_thermal.ipes"
 */
@RunWith(Parameterized.class)
public class GoldenReferenceTest {

    private static final Logger LOGGER = Logger.getLogger(GoldenReferenceTest.class.getName());

    // Default tolerance level (can be overridden via system property)
    private static final ResultComparator.ToleranceLevel DEFAULT_TOLERANCE =
        ResultComparator.ToleranceLevel.NORMAL;

    private final File circuitFile;
    private final String circuitName;

    public GoldenReferenceTest(File circuitFile, String circuitName) {
        this.circuitFile = circuitFile;
        this.circuitName = circuitName;
    }

    @BeforeClass
    public static void setUpClass() {
        LOGGER.info("Initializing Golden Reference Test Suite");
        GoldenReferenceTestHarness.initialize();
    }

    @AfterClass
    public static void tearDownClass() {
        LOGGER.info("Golden Reference Test Suite completed");
        GoldenReferenceTestHarness.shutdown();
    }

    /**
     * Provide test parameters - all circuits that have baselines.
     */
    @Parameters(name = "{1}")
    public static Collection<Object[]> circuits() {
        List<Object[]> parameters = new ArrayList<>();

        // Check if specific circuit is requested via system property
        String requestedCircuit = System.getProperty("circuit");

        try {
            List<File> allCircuits = CircuitDiscovery.findAllCircuits();
            LOGGER.info("Found " + allCircuits.size() + " total circuits");

            for (File circuitFile : allCircuits) {
                String circuitName = CircuitDiscovery.getRelativePath(circuitFile);

                // If specific circuit requested, only include that one
                if (requestedCircuit != null && !circuitName.equals(requestedCircuit)) {
                    continue;
                }

                // Only include circuits that have baselines
                if (ResultSerializer.baselineExists(circuitName)) {
                    parameters.add(new Object[]{circuitFile, circuitName});
                }
            }

            LOGGER.info("Found " + parameters.size() + " circuits with baselines");

            if (parameters.isEmpty()) {
                LOGGER.warning("No baseline data found. Please run BaselineCapture first.");
                // Add a dummy test that will be skipped
                parameters.add(new Object[]{null, "NO_BASELINES_FOUND"});
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to discover circuits", e);
            parameters.add(new Object[]{null, "DISCOVERY_FAILED"});
        }

        return parameters;
    }

    /**
     * The main test method - runs simulation and compares against baseline.
     */
    @Test
    public void testCircuitMatchesBaseline() throws Exception {
        // Skip if no baselines or discovery failed
        if (circuitFile == null) {
            Assume.assumeTrue("Skipping - no baseline data available", false);
            return;
        }

        LOGGER.info("Testing circuit: " + circuitName);

        // Load the baseline (golden reference)
        SimulationResult baseline = ResultSerializer.loadFromCSV(circuitName);
        assertNotNull("Failed to load baseline for " + circuitName, baseline);

        // Run the current simulation
        SimulationResult actual = GoldenReferenceTestHarness.runCircuitSimulation(circuitFile);
        assertNotNull("Failed to run simulation for " + circuitName, actual);

        // Determine tolerance level
        ResultComparator.ToleranceLevel tolerance = DEFAULT_TOLERANCE;
        String toleranceProp = System.getProperty("tolerance");
        if (toleranceProp != null) {
            try {
                tolerance = ResultComparator.ToleranceLevel.valueOf(toleranceProp.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Invalid tolerance level: " + toleranceProp + ", using default");
            }
        }

        // Compare results
        ResultComparator.ComparisonResult comparison =
            ResultComparator.compare(baseline, actual, tolerance);

        // Log the comparison report
        String report = comparison.getReport();
        if (comparison.matches()) {
            LOGGER.info("PASS: " + circuitName + "\n" + report);
        } else {
            LOGGER.severe("FAIL: " + circuitName + "\n" + report);
        }

        // Assert that results match
        assertTrue("Simulation results do not match baseline for " + circuitName +
                  "\nMax absolute error: " + comparison.getMaxAbsoluteError() +
                  "\nMax relative error: " + comparison.getMaxRelativeError() +
                  "\nSee log for details",
                  comparison.matches());
    }

    /**
     * Additional test to verify checksum matches (quick check).
     */
    @Test
    public void testChecksumMatches() throws Exception {
        if (circuitFile == null) {
            Assume.assumeTrue("Skipping - no baseline data available", false);
            return;
        }

        LOGGER.fine("Quick checksum test for: " + circuitName);

        SimulationResult baseline = ResultSerializer.loadFromCSV(circuitName);
        SimulationResult actual = GoldenReferenceTestHarness.runCircuitSimulation(circuitFile);

        boolean checksumMatches = ResultComparator.quickCompare(baseline, actual);

        if (!checksumMatches) {
            LOGGER.warning("Checksum mismatch for " + circuitName +
                         " (expected: " + baseline.getChecksum() +
                         ", actual: " + actual.getChecksum() + ")");
        }

        assertTrue("Checksum does not match for " + circuitName, checksumMatches);
    }
}
