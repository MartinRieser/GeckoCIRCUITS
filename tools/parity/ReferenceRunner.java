import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * P5 parity harness: runs a circuit in the LEGACY GeckoCIRCUITS engine and
 * dumps the recorded scope signals to CSV (time + one column per signal).
 *
 * Launches the Swing app with RMI remote control enabled, drives it via
 * GeckoRemoteInterface and exits. dt/tEnd are parsed from the .ipes file and
 * passed explicitly to initSimulation(dt, tEnd) — the GUI solver settings do
 * not reliably propagate on this headless-driven path (observed dt=0, which
 * makes the legacy loop spin forever).
 *
 * Usage: ReferenceRunner &lt;geckoFatJar&gt; &lt;ipesFile&gt; &lt;outCsv&gt; &lt;signal[,signal...]> [port]
 */
public final class ReferenceRunner {

    private static final Pattern DT = Pattern.compile("(?m)^dt\\s+([0-9.eE+-]+)\\s*$");
    private static final Pattern T_END = Pattern.compile("(?m)^tDURATION\\s+([0-9.eE+-]+)\\s*$");

    private ReferenceRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: ReferenceRunner <geckoFatJar> <ipesFile> <outCsv> <signal[,signal...]> [port]");
            System.exit(2);
        }
        Path jar = Path.of(args[0]).toAbsolutePath();
        Path ipes = Path.of(args[1]).toAbsolutePath();
        Path outCsv = Path.of(args[2]).toAbsolutePath();
        String[] signals = args[3].split(",");
        int port = args.length > 4 ? Integer.parseInt(args[4]) : 43099;

        if (!Files.isRegularFile(jar) || !Files.isRegularFile(ipes)) {
            System.err.println("Missing input file (jar=" + jar + ", ipes=" + ipes + ")");
            System.exit(2);
        }

        double dt = parseToken(ipes, DT, 1e-6);
        double tEnd = parseToken(ipes, T_END, 1e-3);
        System.out.printf("reference parameters: dt=%g tEnd=%g%n", dt, tEnd);

        Path logFile = outCsv.resolveSibling(outCsv.getFileName() + ".gecko.log");
        Process process = new ProcessBuilder(
                ProcessHandle.current().info().command().orElse("java"),
                "-Xmx1g", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jar.toString(),
                "gecko.GeckoSim", ipes.toString(), "-p", String.valueOf(port))
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();

        try {
            gecko.GeckoRemoteInterface gecko = waitForRmi(port, 120);
            long session = gecko.connect();
            try {
                gecko.initSimulation(dt, tEnd);
                System.out.println("rmi: simulation initialized (dt=" + gecko.get_dt()
                        + ", Tend=" + gecko.get_Tend() + ")");
                if (gecko.get_dt() <= 0) {
                    throw new IllegalStateException("legacy engine has dt<=0, refusing to run");
                }
                gecko.runSimulation();  // blocks until the run finishes
                System.out.println("rmi: simulation finished");

                double[] time = gecko.getTimeArray(signals[0], 0, Double.MAX_VALUE, 0);
                if (time == null || time.length == 0) {
                    throw new IllegalStateException("legacy engine recorded no time steps for "
                            + signals[0] + " (see " + logFile + ")");
                }
                float[][] data = new float[signals.length][];
                for (int i = 0; i < signals.length; i++) {
                    data[i] = gecko.getSignalData(signals[i], 0, Double.MAX_VALUE, 0);
                    if (data[i] == null || data[i].length == 0) {
                        throw new IllegalStateException("legacy engine recorded no data for signal "
                                + signals[i] + " (see " + logFile + ")");
                    }
                }

                writeCsv(outCsv, signals, time, data);
                System.out.println("reference run: " + time.length + " rows x " + signals.length
                        + " signals -> " + outCsv);
            } finally {
                try {
                    gecko.disconnect(session);
                } catch (java.rmi.RemoteException ignored) {
                    // server may already be gone after shutdown
                }
            }
            try {
                gecko.shutdown();
            } catch (java.rmi.RemoteException ignored) {
                // shutdown kills the remote JVM; a reset here is expected
            }
        } finally {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    /** .ipes files are gzip'd ASCII; falls back to plain text. */
    private static double parseToken(Path ipes, Pattern pattern, double fallback) throws IOException {
        String text;
        try (var in = new GZIPInputStream(Files.newInputStream(ipes))) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            text = Files.readString(ipes, StandardCharsets.UTF_8);
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : fallback;
    }

    private static gecko.GeckoRemoteInterface waitForRmi(int port, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        Exception last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                Registry registry = LocateRegistry.getRegistry(port);
                return (gecko.GeckoRemoteInterface) registry.lookup("GeckoRemoteInterface");
            } catch (Exception e) {
                last = e;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("GeckoSim RMI registry did not appear on port " + port, last);
    }

    private static void writeCsv(Path out, String[] signals, double[] time, float[][] data)
            throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(out.toFile())))) {
            writer.print("time");
            for (String signal : signals) {
                writer.print(",");
                writer.print(signal);
            }
            writer.println();
            for (int row = 0; row < time.length; row++) {
                writer.print(time[row]);
                for (float[] series : data) {
                    writer.print(",");
                    writer.print(row < series.length ? series[row] : Double.NaN);
                }
                writer.println();
            }
        }
    }
}
