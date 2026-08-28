/* Tutorial sweep for the headless engine: classifies every .ipes under a root
 * directory as CLEAN / SINGULAR / NONFINITE. Compile like the other parity
 * tools (see docs/architecture/CONTROL_PARITY_PLAN.md W5):
 *   javac -cp <gecko-simulation-core-classes+log4j> TutorialSweep.java
 *   java  -cp .:<same> TutorialSweep <repo>/resources/tutorials
 */
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitFileParser;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Smoke sweep: run every tutorial .ipes through the headless engine and classify. */
public final class TutorialSweep {
    private static final int MAX_STEPS = 100_000;

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().toLowerCase().endsWith(".ipes"))
                    .sorted()
                    .forEach(TutorialSweep::probe);
        }
    }

    private static void probe(Path ipes) {
        String rel = ipes.toString().replace('\\', '/');
        System.out.println("FILE " + rel);
        try {
            CircuitModel model = new CircuitFileParser().parse(ipes.toString());
            System.out.println("  parse OK  lk=" + model.getCircuitComponents().size()
                    + " ctrl=" + model.getControlComponents().size()
                    + " therm=" + model.getThermalComponents().size()
                    + " types=" + inventory(model));
            double dt = model.getTimeStep() > 0 ? model.getTimeStep() : 1e-6;
            double fileDur = model.getSimulationDuration() > 0 ? model.getSimulationDuration() : 1e-3;
            double duration = Math.min(fileDur, MAX_STEPS * dt);
            SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                    SimulationConfig.builder()
                            .circuitFile(ipes.toString())
                            .stepWidth(dt)
                            .simulationDuration(duration)
                            .build());
            if (!result.isSuccess()) {
                System.out.println("  SIM FAILED: " + result.getErrorMessage());
                return;
            }
            int bad = 0;
            double worst = 0;
            for (int s = 0; s < result.getSignalNames().length; s++) {
                for (float v : result.getSignalData(s)) {
                    if (!Double.isFinite(v)) { bad++; break; }
                    worst = Math.max(worst, Math.abs(v));
                }
            }
            System.out.println("  SIM OK steps=" + result.getTotalTimeSteps()
                    + " nonFiniteSignals=" + bad + "/" + result.getSignalNames().length
                    + " maxAbs=" + worst);
        } catch (Exception e) {
            System.out.println("  ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static String inventory(CircuitModel model) {
        Map<Integer, Integer> m = new HashMap<>();
        model.getAllComponents().forEach(c -> m.merge(c.getType(), 1, Integer::sum));
        return m.entrySet().stream().sorted((a, b) -> Integer.compare(a.getKey(), b.getKey()))
                .map(e -> e.getKey() + "x" + e.getValue())
                .reduce((a, b) -> a + "," + b).orElse("");
    }
}
