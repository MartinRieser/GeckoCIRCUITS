package gecko.core.simulation;

import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Probe signals recorded flat zero even after the slash-label fix: the
 * netlist label resolver also indexes CONTROL wire labels, so the engine
 * resolved a requested signal like "v_in" to a bogus power-node index
 * (getP()[4] ~= 0) instead of routing it to the VOLT_IN measurement probe.
 * Signal resolution must prefer explicit measurement probes over node
 * labels; this fixture (the Multi-Scope RLC example) has voltmeter outputs
 * whose names deliberately collide with control wire labels.
 */
class MultiScopeProbeSignalTest {

    private static final double EPSILON = 1.0;

    @Test
    void voltmeterProbeSignalsWinOverShadowingControlWireLabels() throws Exception {
        var resource = getClass().getResource("/multi-scope-rlc.ipes");
        assertNotNull(resource, "fixture multi-scope-rlc.ipes must be on the test classpath");
        CircuitModel model = new CircuitFileParser().parse(Path.of(resource.toURI()).toString());

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("multi-scope-rlc.ipes").circuitModel(model)
                .stepWidth(1e-6)
                .simulationDuration(0.01)
                .signals(java.util.List.of("v_in", "v_out", "v_R1", "v_L1", "v_C1"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), () -> "simulation failed: " + result.getErrorMessage());
        DataContainerGlobal container = result.getDataContainer();
        assertNotNull(container, "successful run must carry a data container");

        double vIn = lastValue(container, "v_in");
        double vOut = lastValue(container, "v_out");
        double vC1 = lastValue(container, "v_C1");

        // 24 V step source, settled after 10 ms: the voltmeter probes must
        // report the node voltages (24 V), not the flat 0.0 of the shadowed
        // bogus-node path.
        assertEquals(24.0, vIn, EPSILON, "v_in probe must measure the 24 V source node");
        assertEquals(24.0, vOut, EPSILON, "v_out probe must measure the charged output node");
        assertEquals(24.0, vC1, EPSILON, "v_C1 probe must measure the charged capacitor node");
    }

    private static double lastValue(DataContainerGlobal container, String signalName) {
        for (int row = 0; row < container.getRowLength(); row++) {
            if (signalName.equals(container.getSignalName(row))) {
                return container.getValue(row, container.getMaximumTimeIndex(row));
            }
        }
        throw new AssertionError("signal not recorded: " + signalName);
    }
}
