import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Diagnostic: parses a .ipes file (gzip or plain), prints the parsed
 * components/connections, the headless netlist's per-terminal node assignment
 * and - with argument "run" - an in-process engine run with the first samples
 * of every recorded signal. Usage: NetlistDump &lt;file.ipes&gt; [run]
 */
public final class NetlistDump {

    private NetlistDump() {
    }

    public static void main(String[] args) throws Exception {
        byte[] raw = Files.readAllBytes(Path.of(args[0]));
        CircuitModel model;
        try (var in = new ByteArrayInputStream(maybeGunzip(raw))) {
            model = new CircuitFileParser().parse(in, "dump.ipes");
        }

        System.out.println("== components ==");
        for (CircuitModel.ComponentData c : model.getAllComponents()) {
            System.out.printf("%-12s typ=%-4d pos=%s orient=%d labels=%s params0=%s%n",
                    c.getName(), c.getType(), java.util.Arrays.toString(c.getPosition()),
                    c.getOrientation(), java.util.Arrays.toString(c.getTerminalYLabels()),
                    c.getParameterStrings() != null && c.getParameterStrings().length > 0
                            ? c.getParameterStrings()[0] : null);
        }

        CircuitNetlist n = NetlistBuilder.buildFromCircuitModel(model);
        System.out.println("== netlist ==");
        System.out.println("elements=" + n.getElementCount()
                + " nodeMax=" + n.getNodeMax()
                + " voltageSourceMax=" + n.getVoltageSourceMax());
        for (int i = 0; i < n.getElementCount(); i++) {
            StringBuilder ps = new StringBuilder();
            for (int p = 0; p < 8; p++) {
                try {
                    ps.append(String.format("%g ", n.getElementParam(i, p)));
                } catch (Exception e) {
                    break;
                }
            }
            System.out.printf("element %d: nodeX=%d nodeY=%d params=%s%n",
                    i, n.getNodeX(i), n.getNodeY(i), ps);
        }

        if (args.length > 1 && args[1].equals("run")) {
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile("dump.ipes").circuitModel(model)
                    .stepWidth(model.getTimeStep())
                    .simulationDuration(model.getSimulationDuration())
                    .build();
            if (args.length > 2 && args[2].equals("matrix")) {
                gecko.core.simulation.solver.MatrixSolver ms =
                        new gecko.core.simulation.solver.MatrixSolver(config.getSolverSettings().getSolverType());
                ms.initializeMatrices(n.getNodeMax(), n.getVoltageSourceMax(), n.getElementCount());
                double dt = model.getTimeStep();
                ms.buildMatrixA(n, dt, 0.0, false);
                ms.buildVectorB(n, dt, 0.0, false);
                System.out.println("== first-step matrices ==");
                double[][] a = ms.getSystemMatrix();
                for (double[] row : a) {
                    StringBuilder sb = new StringBuilder();
                    for (double v : row) { sb.append(String.format("%12.4g ", v)); }
                    System.out.println(sb);
                }
                StringBuilder bb = new StringBuilder("b: ");
                for (double v : ms.getb()) { bb.append(String.format("%12.4g ", v)); }
                System.out.println(bb);
                ms.solve();
                StringBuilder pp = new StringBuilder("p: ");
                for (double v : ms.getP()) { pp.append(String.format("%12.4g ", v)); }
                System.out.println(pp);
                return;
            }
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            SimulationResult r = engine.runSimulation(config);
            System.out.println("== engine run ==");
            System.out.println("status=" + r.getStatus() + " error=" + r.getErrorMessage()
                    + " steps=" + r.getTotalTimeSteps());
            DataContainerGlobal c = r.getDataContainer();
            if (c != null) {
                for (int row = 0; row < c.getRowLength(); row++) {
                    StringBuilder sb = new StringBuilder(c.getSignalName(row) + ": ");
                    int maxCol = Math.min(8, c.getMaximumTimeIndex(row));
                    for (int col = 0; col <= maxCol; col++) {
                        sb.append(String.format("%g ", c.getValue(row, col)));
                    }
                    System.out.println(sb);
                    System.out.println("last: " + c.getValue(row, c.getMaximumTimeIndex(row)));
                }
            }
        }
    }

    private static byte[] maybeGunzip(byte[] raw) throws Exception {
        if (raw.length >= 2 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b) {
            try (var gz = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                return gz.readAllBytes();
            }
        }
        return raw;
    }
}
