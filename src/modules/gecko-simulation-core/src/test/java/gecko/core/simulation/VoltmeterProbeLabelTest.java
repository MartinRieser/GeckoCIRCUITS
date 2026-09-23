package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Voltmeter probes recorded flat zero in the web editor: the classic .ipes
 * dialect writes measurement labels with a leading '/' (parameterString[]
 * /V_in/0/0) while the label resolver keys them bare, so nodeForLabel missed
 * for both measured nodes and every probe evaluated 0-0 = 0. Scope channels
 * wired to such probes (e.g. the Multi-Scope RLC example) therefore showed no
 * waveforms unless the "all signals" view happened to include node voltages.
 */
class VoltmeterProbeLabelTest {

    private static CircuitModel.ComponentData comp(int type, String name, int x, double[] params) {
        CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, name, x, 10, 502);
        comp.setRawParameters(params);
        comp.setUniqueObjectIdentifier(name.hashCode());
        return comp;
    }

    /** V_dc(48 V) - R_load(10 Ohm), GND return, node "V_out" labeled after the source. */
    private static CircuitModel modelWithVoltmeter() {
        CircuitModel model = new CircuitModel();

        CircuitModel.ComponentData source = comp(4, "V_dc", 4, new double[]{401, 48});
        source.setOrientation(504);
        model.getCircuitComponents().add(source);
        model.getCircuitComponents().add(comp(1, "R_load", 16, new double[]{10}));

        CircuitModel.ConnectionData out = new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {8, 10}, {14, 10}});
        out.setLabel("V_out");
        model.getConnections().add(out);
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{2, 10}, {2, 14}, {18, 14}, {18, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        // Voltmeter in the classic file dialect: the measured-node labels keep
        // their raw slash-prefixed parameterString[] form (the parser strips
        // the '/' only for terminal labels, not for parameterString[]).
        CircuitModel.ComponentData meter = new CircuitModel.ComponentData(1, "VOLT_OUT", 10, 14, 503);
        meter.setParameterStrings(new String[]{"/V_out", "/0", "0"});
        meter.setTerminalYLabels(new String[]{"v_out"});
        meter.setUniqueObjectIdentifier(2001);
        model.getControlComponents().add(meter);

        return model;
    }

    @Test
    void voltmeterWithSlashDialectLabelsRecordsNodeVoltage() {
        CircuitModel model = modelWithVoltmeter();
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertTrue(netlist.getElementCount() >= 2, "netlist must contain the elements");

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("probe-labels.ipes").circuitModel(model)
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .signals(java.util.List.of("v_out"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), () -> "simulation failed: " + result.getErrorMessage());
        DataContainerGlobal container = result.getDataContainer();
        assertNotNull(container, "successful run must carry a data container");

        Float recorded = null;
        for (int row = 0; row < container.getRowLength(); row++) {
            if ("v_out".equals(container.getSignalName(row))) {
                recorded = container.getValue(row, container.getMaximumTimeIndex(row));
            }
        }
        assertNotNull(recorded, "probe output 'v_out' must be recorded");
        assertEquals(48.0, recorded, 1.0,
                "voltmeter with slash-prefixed labels must measure the 48 V node, not 0");
    }
}
