package gecko.rest.service;

import gecko.core.datacontainer.ContainerStatus;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.SimulationResult;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulation backend that drives the REAL classic GeckoCIRCUITS engine
 * (gecko-gui's {@code gecko.GeckoSim}) headlessly via its RMI remote control,
 * exactly like the parity harness {@code ReferenceRunner}. Used for circuits
 * whose results the pure-headless engine cannot reproduce yet (control-domain
 * blocks, saved operating points).
 *
 * <p>No compile-time dependency on the GUI module: the remote stub is invoked
 * reflectively, so the enforcer's Swing/AWT ban stays satisfied. Requires the
 * gecko fat jar at runtime ({@code gecko.legacy.gui-jar}).
 */
@Service
public class LegacySimulationBackend {

    private static final Logger LOGGER = LogManager.getLogger(LegacySimulationBackend.class);
    private static final String REMOTE_NAME = "GeckoRemoteInterface";
    private static final long STARTUP_TIMEOUT_S = 120;
    private static final long RUN_TIMEOUT_MS = 600_000;

    private final String guiJar;
    private final String javaExecutable;
    private volatile java.net.URLClassLoader remoteInterfaceLoader;

    /** The single actively running legacy process, if any. */
    private ActiveRun activeRun;

    private static final class ActiveRun {
        final Process process;
        final double tEnd;
        volatile Object remote;
        volatile double lastSimulatedTime;
        volatile boolean cancelled;

        ActiveRun(Process process, double tEnd) {
            this.process = process;
            this.tEnd = tEnd;
        }
    }

    public LegacySimulationBackend(
            @Value("${gecko.legacy.gui-jar:}") String guiJar,
            @Value("${gecko.legacy.java-executable:java}") String javaExecutable) {
        this.javaExecutable = javaExecutable;
        this.guiJar = resolveGuiJar(guiJar);
    }

    /** Whether the classic engine jar could be located. */
    public boolean isAvailable() {
        return guiJar != null;
    }

    public String configurationHint() {
        return guiJar != null
                ? "legacy engine: " + guiJar
                : "legacy backend unavailable: set gecko.legacy.gui-jar to the gecko fat jar "
                  + "(gecko-gui/target/gecko-1.0-jar-with-dependencies.jar)";
    }

    private static String resolveGuiJar(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Files.isRegularFile(Path.of(configured)) ? configured : null;
        }
        String[] candidates = {
                "src/modules/gecko-gui/target/gecko-1.0-jar-with-dependencies.jar",
                "../gecko-gui/target/gecko-1.0-jar-with-dependencies.jar",
                "gecko-gui/target/gecko-1.0-jar-with-dependencies.jar"
        };
        for (String candidate : candidates) {
            if (Files.isRegularFile(Path.of(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Runs the circuit in the classic engine and maps the recorded scope data
     * into a {@link SimulationResult}.
     *
     * @param ipesBytes faithful .ipes content (the classic GUI cannot open
     *                  writer rewrites reliably)
     * @param model     parsed model, used to label unnamed measurement blocks
     * @param signals   signal names to export; empty = the file's stored names
     */
    public SimulationResult run(byte[] ipesBytes, CircuitModel model, double dt, double tEnd,
                                List<String> signals) {
        if (guiJar == null) {
            return SimulationResult.failed(configurationHint());
        }
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("gecko-legacy-", ".ipes");
            Files.write(tempFile, ipesBytes);

            int port = freePort();
            Process process = new ProcessBuilder(
                    javaExecutable, "-Xmx1g", "-Djava.rmi.server.hostname=127.0.0.1",
                    "-cp", guiJar, "gecko.GeckoSim", tempFile.toAbsolutePath().toString(),
                    "-p", String.valueOf(port))
                    .redirectErrorStream(true)
                    .redirectOutput(tempFile.resolveSibling("gecko-legacy.log").toFile())
                    .start();

            ActiveRun run = new ActiveRun(process, tEnd);
            synchronized (this) {
                if (activeRun != null) {
                    process.destroy();
                    throw new IllegalStateException("A legacy engine run is already active");
                }
                activeRun = run;
            }

            run.remote = waitForRemote(port);
            Object remote = run.remote;

            long session = (Long) call(remote, "connect");
            try {
                labelMeasurementBlocks(remote, model);
                call(remote, "initSimulation", new Class<?>[]{double.class, double.class}, dt, tEnd);

                // NOTE: no concurrent RMI polling during runSimulation - the
                // classic remote implementation dispatches on a single thread;
                // concurrent calls stall the run (verified empirically).
                // Progress therefore jumps from 0 to 1 at completion.
                call(remote, "runSimulation");

                return exportResult(remote, model, signals);
            } finally {
                try {
                    call(remote, "disconnect", new Class<?>[]{long.class}, session);
                } catch (Exception ignored) {
                    // server may already be gone after shutdown
                }
                try {
                    call(remote, "shutdown");
                } catch (Exception ignored) {
                    // shutdown kills the remote JVM; a reset here is expected
                }
            }
        } catch (Exception e) {
            boolean cancelled = activeRun != null && activeRun.cancelled;
            if (cancelled) {
                return SimulationResult.cancelled();
            }
            String message = e instanceof InvocationTargetException && e.getCause() != null
                    ? e.getCause().getMessage() : e.getMessage();
            LOGGER.error("Legacy backend run failed", e);
            return SimulationResult.failed("Legacy engine error: " + message);
        } finally {
            synchronized (this) {
                ActiveRun run = activeRun;
                activeRun = null;
                if (run != null) {
                    run.process.destroy();
                }
            }
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // temp cleanup is best effort
                }
            }
        }
    }

    /** Destroys the running classic engine process; the blocked run aborts. */
    public synchronized void cancelActive() {
        ActiveRun run = activeRun;
        if (run != null) {
            run.cancelled = true;
            run.process.destroy();
        }
    }

    /** Fraction [0..1] of the active legacy run, or -1. */
    public synchronized double activeProgress() {
        ActiveRun run = activeRun;
        if (run == null || run.tEnd <= 0) {
            return -1;
        }
        return Math.min(1.0, run.lastSimulatedTime / run.tEnd);
    }

    // ------------------------------------------------------------------

    private Remote waitForRemote(int port) throws Exception {
        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_S * 1000L;
        Exception last = null;
        while (System.currentTimeMillis() < deadline) {
            if (activeRun != null && activeRun.cancelled) {
                throw new IllegalStateException("Cancelled during startup");
            }
            try {
                return (Remote) lookupRemote(port);
            } catch (Exception e) {
                last = e;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Classic engine RMI registry did not appear on port " + port, last);
    }

    /**
     * Looks up the remote stub with the GUI jar on the context classloader:
     * RMI builds the client proxy from the interface class, which the REST
     * module must not depend on at compile time (enforcer bans Swing/AWT),
     * so it is loaded reflectively from the configured jar.
     */
    private Object lookupRemote(int port) throws Exception {
        if (remoteInterfaceLoader == null) {
            remoteInterfaceLoader = new java.net.URLClassLoader(
                    new java.net.URL[]{Path.of(guiJar).toUri().toURL()},
                    getClass().getClassLoader());
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(remoteInterfaceLoader);
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            return registry.lookup(REMOTE_NAME);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    /** Reflective RMI invocation - no compile-time dependency on the GUI module. */
    private static Object call(Object remote, String method, Class<?>[] types, Object... args)
            throws Exception {
        try {
            return remote.getClass().getMethod(method, types).invoke(remote, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }
    }

    private static Object call(Object remote, String method) throws Exception {
        return call(remote, method, new Class<?>[0]);
    }

    /**
     * Names unnamed VOLT/AMP measurement blocks after themselves so the
     * classic data container records named columns for them.
     */
    private void labelMeasurementBlocks(Object remote, CircuitModel model) throws Exception {
        if (model == null) {
            return;
        }
        for (CircuitModel.ComponentData comp : model.getControlComponents()) {
            String name = comp.getName();
            if (name == null || !(name.startsWith("VOLT.") || name.startsWith("AMP."))) {
                continue;
            }
            String[] labels = comp.getTerminalYLabels();
            boolean labeled = labels.length > 0 && labels[0] != null
                    && !labels[0].isBlank() && !labels[0].equals("NIX_NIX_NIX");
            if (!labeled) {
                call(remote, "setOutputNodeName", new Class<?>[]{String.class, int.class, String.class},
                        name, 0, name);
            }
        }
    }

    /**
     * Export signal names for a legacy run: the request's signals if given,
     * else the file's stored names, else the measurement blocks' output
     * labels, else the block names.
     */
    public static List<String> exportSignalNames(CircuitModel model, List<String> requested) {
        if (requested != null && !requested.isEmpty()) {
            return requested;
        }
        List<String> names = new ArrayList<>();
        if (model != null && model.getDataContainerSignals() != null) {
            for (String s : model.getDataContainerSignals()) {
                if (s != null && !s.isBlank() && !s.equals("[]") && !names.contains(s)) {
                    names.add(s);
                }
            }
            if (!names.isEmpty()) {
                return names;
            }
        }
        if (model != null) {
            for (CircuitModel.ComponentData comp : model.getControlComponents()) {
                String name = comp.getName();
                if (name == null || !(name.startsWith("VOLT.") || name.startsWith("AMP."))) {
                    continue;
                }
                String[] labels = comp.getTerminalYLabels();
                String export = labels.length > 0 && labels[0] != null
                        && !labels[0].isBlank() && !labels[0].equals("NIX_NIX_NIX")
                        ? labels[0].trim() : name;
                if (!names.contains(export)) {
                    names.add(export);
                }
            }
        }
        return names;
    }

    private SimulationResult exportResult(Object remote, CircuitModel model, List<String> requested)
            throws Exception {
        List<String> names = exportSignalNames(model, requested);
        List<String> available = new ArrayList<>();
        List<float[]> series = new ArrayList<>();
        double[] time = null;
        for (String name : names) {
            double[] t = (double[]) call(remote, "getTimeArray",
                    new Class<?>[]{String.class, double.class, double.class, int.class},
                    name, 0.0, Double.MAX_VALUE, 0);
            if (t == null || t.length == 0) {
                LOGGER.warn("Legacy engine recorded no data for signal '{}' - not exported", name);
                continue;
            }
            if (time == null) {
                time = t;
            }
            float[] data = (float[]) call(remote, "getSignalData",
                    new Class<?>[]{String.class, double.class, double.class, int.class},
                    name, 0.0, Double.MAX_VALUE, 0);
            if (data == null || data.length == 0) {
                LOGGER.warn("Legacy engine recorded no data for signal '{}' - not exported", name);
                continue;
            }
            available.add(name);
            series.add(data);
        }
        if (time == null || available.isEmpty()) {
            return SimulationResult.failed("Legacy engine recorded no data for the requested signals");
        }

        DataContainerGlobal container = new DataContainerGlobal();
        container.init(available.size(), time.length, available.toArray(new String[0]), "time [s]");
        container.setContainerStatus(ContainerStatus.RUNNING);
        float[] row = new float[available.size()];
        for (int r = 0; r < time.length; r++) {
            for (int s = 0; s < row.length; s++) {
                row[s] = r < series.get(s).length ? series.get(s)[r] : 0.0f;
            }
            container.insertValuesAtEnd(row, time[r]);
        }
        container.setContainerStatus(ContainerStatus.FINISHED);

        return SimulationResult.builder()
                .status(SimulationResult.Status.SUCCESS)
                .dataContainer(container)
                .executionTimeMs(0)
                .totalTimeSteps(time.length)
                .simulatedTime(time[time.length - 1])
                .metadata("backend", "legacy")
                .build();
    }

    private Thread startProgressPoller(ActiveRun run) {
        Thread poller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Object time = call(run.remote, "getSimulationTime");
                    if (time instanceof Number number) {
                        run.lastSimulatedTime = number.doubleValue();
                    }
                } catch (Exception ignored) {
                    // polling must never kill the run
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "legacy-progress-poller");
        poller.setDaemon(true);
        poller.start();
        return poller;
    }

    private static int freePort() throws IOException {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (this) {
            ActiveRun run = activeRun;
            if (run != null) {
                run.cancelled = true;
                run.process.destroy();
            }
        }
    }
}
