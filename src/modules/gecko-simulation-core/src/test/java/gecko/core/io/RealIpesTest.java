package gecko.core.io;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: parses a real .ipes file and verifies component extraction.
 *
 * <p>Circuit files are loaded from the classpath ({@code /ipes/...}) so the
 * test does not depend on a developer-specific absolute path.
 */
public class RealIpesTest {

    private static final String IPES_RESOURCE_DIR = "/ipes/";

    private static Path resolveIpes(String fileName) throws IOException {
        Path temp = Files.createTempFile("gecko-ipes-", ".ipes");
        try (InputStream is = RealIpesTest.class.getResourceAsStream(IPES_RESOURCE_DIR + fileName)) {
            assertNotNull(is, "Resource not found on classpath: " + IPES_RESOURCE_DIR + fileName);
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }

    @Test
    void parseEx1_simulationParameters() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(resolveIpes("ex_1.ipes").toAbsolutePath().toString());
        assertTrue(model.getSimulationDuration() > 0, "Duration > 0");
        assertTrue(model.getTimeStep() > 0, "TimeStep > 0");
        System.out.println("ex_1.ipes  duration=" + model.getSimulationDuration()
                + " dt=" + model.getTimeStep()
                + " components=" + model.getCircuitComponents().size());
    }

    @Test
    void parseEx1_circuitComponents() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(resolveIpes("ex_1.ipes").toAbsolutePath().toString());
        for (CircuitModel.ComponentData c : model.getCircuitComponents()) {
            System.out.printf("  [type=%d] %-12s params=%s  xL=%s  yL=%s%n",
                    c.getType(), c.getName(), c.getParameters(),
                    Arrays.toString(c.getTerminalXLabels()),
                    Arrays.toString(c.getTerminalYLabels()));
        }
        assertNotNull(model.getCircuitComponents());
    }

    @Test
    void parseEx1_netlistFromLabels() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(resolveIpes("ex_1.ipes").toAbsolutePath().toString());
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        System.out.printf("Netlist: nodes=%d  vSrc=%d  elements=%d%n",
                netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());

        if (!model.getCircuitComponents().isEmpty()) {
            assertEquals(model.getTotalComponentCount(), netlist.getElementCount(),
                    "Element count should match component count");
        }
    }

    @Test
    void parseEx3Pwm_simulationParameters() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(resolveIpes("ex_3_pwm.ipes").toAbsolutePath().toString());
        assertTrue(model.getSimulationDuration() > 0, "Duration > 0");
        System.out.println("ex_3_pwm.ipes  duration=" + model.getSimulationDuration()
                + " dt=" + model.getTimeStep()
                + " components=" + model.getCircuitComponents().size());
        for (CircuitModel.ComponentData c : model.getCircuitComponents()) {
            System.out.printf("  [type=%d] %-12s param0=%s  xL=%s  yL=%s%n",
                    c.getType(), c.getName(),
                    c.getParameters().get("param0"),
                    Arrays.toString(c.getTerminalXLabels()),
                    Arrays.toString(c.getTerminalYLabels()));
        }
    }
}
