package ch.technokrat.regression;

import java.io.File;

/**
 * Debug script to check baseline path resolution
 */
public class DebugBaseline {
    public static void main(String[] args) {
        String circuitName = "Education_ETHZ/ex_1.ipes";

        System.out.println("Circuit name: " + circuitName);

        String basePath = "src/test/resources/golden/baselines";
        String circuitPath = circuitName.replace(".ipes", "");

        System.out.println("Base path: " + basePath);
        System.out.println("Circuit path: " + circuitPath);

        File baseDir = new File(basePath);
        File circuitDir = new File(baseDir, circuitPath);
        File metadataFile = new File(circuitDir, "_metadata.txt");

        System.out.println("Base dir exists: " + baseDir.exists());
        System.out.println("Base dir absolute: " + baseDir.getAbsolutePath());
        System.out.println("Circuit dir exists: " + circuitDir.exists());
        System.out.println("Circuit dir absolute: " + circuitDir.getAbsolutePath());
        System.out.println("Metadata file exists: " + metadataFile.exists());
        System.out.println("Metadata file absolute: " + metadataFile.getAbsolutePath());

        System.out.println("\nListing contents:");
        File educationDir = new File(baseDir, "Education_ETHZ");
        if (educationDir.exists()) {
            File[] files = educationDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    System.out.println("  " + f.getName() + " (dir: " + f.isDirectory() + ")");
                }
            }
        }

        // Test the method
        boolean exists = ResultSerializer.baselineExists(circuitName);
        System.out.println("\nResultSerializer.baselineExists returns: " + exists);
    }
}