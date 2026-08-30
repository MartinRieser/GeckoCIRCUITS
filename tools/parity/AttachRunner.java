import gecko.GeckoRemoteInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Attaches to an ALREADY RUNNING GeckoSim RMI instance (unlike
 * ReferenceRunner, which spawns one), runs the simulation and prints the
 * recorded row count / first+last value of one signal. Diagnostic tool for
 * the legacy-backend debugging flow.
 *
 * Usage: AttachRunner <port> <dt> <tEnd> <signal>
 */
public final class AttachRunner {

    private AttachRunner() {
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        double dt = Double.parseDouble(args[1]);
        double tEnd = Double.parseDouble(args[2]);
        String signal = args[3];

        Registry registry = LocateRegistry.getRegistry("127.0.0.1", port);
        GeckoRemoteInterface gecko = (GeckoRemoteInterface) registry.lookup("GeckoRemoteInterface");
        long session = gecko.connect();
        try {
            gecko.initSimulation(dt, tEnd);
            System.out.println("circuit elements: " + String.join(",", gecko.getCircuitElements()));
            System.out.println("control elements: " + String.join(",", gecko.getControlElements()));
            gecko.runSimulation();
            double[] time = gecko.getTimeArray(signal, 0, Double.MAX_VALUE, 0);
            System.out.println("signal " + signal + " rows=" + (time == null ? 0 : time.length));
            if (time != null && time.length > 0) {
                float[] data = gecko.getSignalData(signal, 0, Double.MAX_VALUE, 0);
                System.out.println("first=" + data[0] + " last=" + data[data.length - 1]);
            }
        } finally {
            try {
                gecko.disconnect(session);
            } catch (Exception ignored) {
                // engine may already be shutting down
            }
        }
    }
}
