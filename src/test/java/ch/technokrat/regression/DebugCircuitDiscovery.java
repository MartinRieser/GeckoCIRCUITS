package ch.technokrat.regression;

import java.io.File;
import java.util.List;
import java.util.Collection;

/**
 * Debug script to check circuit discovery and baseline matching
 */
public class DebugCircuitDiscovery {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Circuit Discovery Debug ===");

        // Get all circuits
        List<File> allCircuits = CircuitDiscovery.findAllCircuits();
        System.out.println("Found " + allCircuits.size() + " total circuits");

        int baselineCount = 0;
        String targetCircuit = "Education_ETHZ/ex_1.ipes";

        for (File circuitFile : allCircuits) {
            String circuitName = CircuitDiscovery.getRelativePath(circuitFile);

            // Look for our target circuit
            if (circuitName.equals(targetCircuit)) {
                System.out.println("\nFound target circuit: " + circuitName);
                System.out.println("File exists: " + circuitFile.exists());
                System.out.println("File absolute: " + circuitFile.getAbsolutePath());

                boolean hasBaseline = ResultSerializer.baselineExists(circuitName);
                System.out.println("Has baseline: " + hasBaseline);

                if (hasBaseline) {
                    baselineCount++;
                    System.out.println("✓ Would include in test parameters");
                }
            }
        }

        System.out.println("\n=== Checking all circuits with baselines ===");
        for (File circuitFile : allCircuits) {
            String circuitName = CircuitDiscovery.getRelativePath(circuitFile);
            boolean hasBaseline = ResultSerializer.baselineExists(circuitName);
            if (hasBaseline) {
                System.out.println("✓ " + circuitName + " has baseline");
                baselineCount++;
            }
        }

        System.out.println("\nTotal circuits with baselines: " + baselineCount);
    }
}