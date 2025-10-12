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

import java.util.*;

/**
 * Compares simulation results with configurable tolerance levels.
 * Provides detailed reporting of any differences found.
 */
public class ResultComparator {

    /**
     * Tolerance levels for comparison.
     */
    public enum ToleranceLevel {
        STRICT(1e-12, "Bit-identical"),
        NORMAL(1e-10, "Standard floating-point tolerance"),
        RELAXED(1e-6, "Relaxed for performance optimizations");

        private final double tolerance;
        private final String description;

        ToleranceLevel(double tolerance, String description) {
            this.tolerance = tolerance;
            this.description = description;
        }

        public double getTolerance() {
            return tolerance;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Result of comparing two simulation results.
     */
    public static class ComparisonResult {
        private final boolean matches;
        private final List<String> differences;
        private final double maxAbsoluteError;
        private final double maxRelativeError;
        private final String signalWithMaxError;

        public ComparisonResult(boolean matches, List<String> differences,
                               double maxAbsoluteError, double maxRelativeError,
                               String signalWithMaxError) {
            this.matches = matches;
            this.differences = new ArrayList<>(differences);
            this.maxAbsoluteError = maxAbsoluteError;
            this.maxRelativeError = maxRelativeError;
            this.signalWithMaxError = signalWithMaxError;
        }

        public boolean matches() {
            return matches;
        }

        public List<String> getDifferences() {
            return new ArrayList<>(differences);
        }

        public double getMaxAbsoluteError() {
            return maxAbsoluteError;
        }

        public double getMaxRelativeError() {
            return maxRelativeError;
        }

        public String getSignalWithMaxError() {
            return signalWithMaxError;
        }

        public String getReport() {
            StringBuilder report = new StringBuilder();
            report.append("Comparison Result: ").append(matches ? "PASS" : "FAIL").append("\n");
            report.append("Max Absolute Error: ").append(maxAbsoluteError).append("\n");
            report.append("Max Relative Error: ").append(maxRelativeError).append("\n");
            if (signalWithMaxError != null) {
                report.append("Signal with Max Error: ").append(signalWithMaxError).append("\n");
            }
            if (!differences.isEmpty()) {
                report.append("\nDifferences Found:\n");
                for (String diff : differences) {
                    report.append("  - ").append(diff).append("\n");
                }
            }
            return report.toString();
        }
    }

    /**
     * Compare two simulation results with specified tolerance.
     *
     * @param expected The expected (baseline) result
     * @param actual The actual (current) result
     * @param tolerance The tolerance level to use
     * @return ComparisonResult containing detailed comparison information
     */
    public static ComparisonResult compare(SimulationResult expected, SimulationResult actual,
                                          ToleranceLevel tolerance) {
        return compare(expected, actual, tolerance.getTolerance());
    }

    /**
     * Compare two simulation results with custom tolerance value.
     *
     * @param expected The expected (baseline) result
     * @param actual The actual (current) result
     * @param tolerance The tolerance value (e.g., 1e-10)
     * @return ComparisonResult containing detailed comparison information
     */
    public static ComparisonResult compare(SimulationResult expected, SimulationResult actual,
                                          double tolerance) {
        List<String> differences = new ArrayList<>();
        double maxAbsError = 0.0;
        double maxRelError = 0.0;
        String signalWithMaxError = null;

        // Compare metadata
        if (Math.abs(expected.getSimulationTime() - actual.getSimulationTime()) > tolerance) {
            differences.add("Simulation time differs: expected=" + expected.getSimulationTime() +
                          ", actual=" + actual.getSimulationTime());
        }

        if (Math.abs(expected.getTimestep() - actual.getTimestep()) > tolerance) {
            differences.add("Timestep differs: expected=" + expected.getTimestep() +
                          ", actual=" + actual.getTimestep());
        }

        // Quick checksum comparison
        if (expected.getChecksum() != null && actual.getChecksum() != null) {
            if (!expected.getChecksum().equals(actual.getChecksum())) {
                differences.add("Checksums differ: expected=" + expected.getChecksum() +
                              ", actual=" + actual.getChecksum());
            }
        }

        // Compare signals
        Map<String, SimulationResult.SignalData> expectedSignals = expected.getSignals();
        Map<String, SimulationResult.SignalData> actualSignals = actual.getSignals();

        // Check for missing signals
        for (String signalName : expectedSignals.keySet()) {
            if (!actualSignals.containsKey(signalName)) {
                differences.add("Signal missing in actual result: " + signalName);
            }
        }

        for (String signalName : actualSignals.keySet()) {
            if (!expectedSignals.containsKey(signalName)) {
                differences.add("Unexpected signal in actual result: " + signalName);
            }
        }

        // Compare common signals
        for (String signalName : expectedSignals.keySet()) {
            if (!actualSignals.containsKey(signalName)) {
                continue;
            }

            SimulationResult.SignalData expectedSignal = expectedSignals.get(signalName);
            SimulationResult.SignalData actualSignal = actualSignals.get(signalName);

            float[] expectedValues = expectedSignal.getValues();
            float[] actualValues = actualSignal.getValues();

            if (expectedValues.length != actualValues.length) {
                differences.add("Signal " + signalName + " has different lengths: expected=" +
                              expectedValues.length + ", actual=" + actualValues.length);
                continue;
            }

            // Compare values
            for (int i = 0; i < expectedValues.length; i++) {
                double absError = Math.abs(expectedValues[i] - actualValues[i]);
                double relError = 0.0;

                if (Math.abs(expectedValues[i]) > 1e-15) {
                    relError = absError / Math.abs(expectedValues[i]);
                }

                if (absError > maxAbsError) {
                    maxAbsError = absError;
                    signalWithMaxError = signalName + "[" + i + "]";
                }

                if (relError > maxRelError) {
                    maxRelError = relError;
                }

                if (absError > tolerance && relError > tolerance) {
                    // Only report first few errors per signal to avoid overwhelming output
                    long errorCount = differences.stream()
                        .filter(d -> d.startsWith("Signal " + signalName))
                        .count();

                    if (errorCount < 5) {
                        differences.add("Signal " + signalName + "[" + i + "] differs: expected=" +
                                      expectedValues[i] + ", actual=" + actualValues[i] +
                                      ", absError=" + absError + ", relError=" + relError);
                    } else if (errorCount == 5) {
                        differences.add("Signal " + signalName + " has additional errors (truncated)");
                    }
                }
            }
        }

        boolean matches = differences.isEmpty() || maxAbsError <= tolerance;

        return new ComparisonResult(matches, differences, maxAbsError, maxRelError, signalWithMaxError);
    }

    /**
     * Quick comparison using only checksums.
     * Useful for fast preliminary checks.
     */
    public static boolean quickCompare(SimulationResult expected, SimulationResult actual) {
        if (expected.getChecksum() == null || actual.getChecksum() == null) {
            return false;
        }
        return expected.getChecksum().equals(actual.getChecksum());
    }
}
