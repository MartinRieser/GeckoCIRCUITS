/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.simulation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

/**
 * CSV export of {@link SimulationResult} (time column + one column per signal),
 * shared by the headless CLI and the MCP server.
 */
public final class SimulationCsv {

    private SimulationCsv() {
    }

    public static void write(SimulationResult result, Path output) throws IOException {
        try (PrintWriter writer = new PrintWriter(output.toFile())) {
            String[] signalNames = result.getSignalNames();
            writer.print("time");
            for (String name : signalNames) {
                writer.print("," + name);
            }
            writer.println();

            double[] times = result.getTimeArray();
            float[][] signalData = new float[signalNames.length][];
            for (int i = 0; i < signalNames.length; i++) {
                signalData[i] = result.getSignalData(i);
            }

            for (int t = 0; t < times.length; t++) {
                writer.print(times[t]);
                for (int s = 0; s < signalNames.length; s++) {
                    writer.print(",");
                    if (signalData[s] != null && t < signalData[s].length) {
                        writer.print(signalData[s][t]);
                    }
                }
                writer.println();
            }
        }
    }
}
