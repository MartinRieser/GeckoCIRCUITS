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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to discover all .ipes circuit files in the resources directory.
 */
public class CircuitDiscovery {

    private static final String RESOURCES_DIR = "resources";
    private static final String FILE_EXTENSION = ".ipes";

    /**
     * Find all .ipes files in the resources directory.
     * @return List of File objects for each circuit
     */
    public static List<File> findAllCircuits() throws IOException {
        File resourcesDir = new File(RESOURCES_DIR);
        if (!resourcesDir.exists() || !resourcesDir.isDirectory()) {
            throw new IOException("Resources directory not found: " + resourcesDir.getAbsolutePath());
        }

        final List<File> circuitFiles = new ArrayList<>();

        Files.walkFileTree(resourcesDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(FILE_EXTENSION)) {
                    circuitFiles.add(file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Skip files we can't access
                return FileVisitResult.CONTINUE;
            }
        });

        return circuitFiles;
    }

    /**
     * Find circuits in a specific subdirectory of resources.
     * @param subdirectory The subdirectory to search (e.g., "Topologies")
     * @return List of File objects for circuits in that subdirectory
     */
    public static List<File> findCircuitsInDirectory(String subdirectory) throws IOException {
        File targetDir = new File(RESOURCES_DIR, subdirectory);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new IOException("Subdirectory not found: " + targetDir.getAbsolutePath());
        }

        final List<File> circuitFiles = new ArrayList<>();

        Files.walkFileTree(targetDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(FILE_EXTENSION)) {
                    circuitFiles.add(file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return circuitFiles;
    }

    /**
     * Get relative path from resources directory for a circuit file.
     * Useful for creating organized baseline structure.
     */
    public static String getRelativePath(File circuitFile) {
        File resourcesDir = new File(RESOURCES_DIR);
        Path resourcesPath = resourcesDir.toPath().toAbsolutePath();
        Path filePath = circuitFile.toPath().toAbsolutePath();

        try {
            // Use forward slashes consistently for cross-platform compatibility
            return resourcesPath.relativize(filePath).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            // File is not in resources directory
            return circuitFile.getName();
        }
    }
}
