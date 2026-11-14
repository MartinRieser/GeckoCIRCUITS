package gecko.core.io;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
        assertFalse(model.getCircuitComponents().isEmpty(), "Components should be parsed");
    }

    @Test
    void parseEx1_circuitComponents() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(resolveIpes("ex_1.ipes").toAbsolutePath().toString());
        assertNotNull(model.getCircuitComponents());
        assertFalse(model.getCircuitComponents().isEmpty(), "Components should not be empty");
    }

    @Test
    void parseEx1_netlistFromLabels() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(resolveIpes("ex_1.ipes").toAbsolutePath().toString());
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Only electrical + thermal components become netlist elements; control
        // blocks (scope, probes) are measurement-only and must not collide
        // with the LK type-number space.
        int expected = model.getCircuitComponents().size() + model.getThermalComponents().size();
        assertEquals(expected, netlist.getElementCount(),
                "Element count should match LK + THERM component count");
    }

    @Test
    void parseEx3Pwm_simulationParameters() throws Exception {
        CircuitModel model = new CircuitFileParser().parse(resolveIpes("ex_3_pwm.ipes").toAbsolutePath().toString());
        assertTrue(model.getSimulationDuration() > 0, "Duration > 0");
        assertFalse(model.getCircuitComponents().isEmpty(), "Components should be parsed");
    }
}
