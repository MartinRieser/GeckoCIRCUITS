package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke gate for two-terminal power components in the WEB writer dialect:
 * every component is exercised in a minimal series circuit (V source -
 * component - R load with GND return) built from SHORT parameter arrays and
 * wire connectivity - exactly what web-editor-authored circuits look like.
 * This is the path that silently produced all-NaN results before the
 * parameter normalization (see CircuitNetlist.normalizeParameters), so each
 * palette component must keep simulating finite here.
 *
 * Not covered: LKOP2 mutual coupling and the thermal/reluctance domains
 * (need multi-domain setup; covered by the tutorial sweep instead), and
 * gate-driven switch behavior (CONTROL_PARITY W1).
 */
class ComponentSmokeTest {

    private static final int ORIENT_WE = 502;
    private static final int ORIENT_EW = 504;

    private static CircuitModel.ComponentData comp(int type, String name, int x, double[] params) {
        CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, name, x, 10, ORIENT_WE);
        comp.setRawParameters(params);
        comp.setUniqueObjectIdentifier(name.hashCode());
        return comp;
    }

    /** V_dc(48 V) - CUT - R_load(10 Ohm), GND return, wire "V_out" after CUT.
     *  V uses EAST_WEST so its nodeY (output) terminal faces the chain -
     *  the classic U stamp drives nodeY positive relative to nodeX. */
    private static CircuitModel seriesModel(CircuitModel.ComponentData cut) {
        CircuitModel model = new CircuitModel();
        CircuitModel.ComponentData source = comp(4, "V_dc", 4, new double[]{401, 48});
        source.setOrientation(ORIENT_EW);
        model.getCircuitComponents().add(source);
        model.getCircuitComponents().add(cut);
        model.getCircuitComponents().add(comp(1, "R_load", 16, new double[]{10}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {8, 10}}));
        CircuitModel.ConnectionData out = new CircuitModel.ConnectionData("LK", new int[][]{{12, 10}, {14, 10}});
        out.setLabel("V_out");
        model.getConnections().add(out);
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{2, 10}, {2, 14}, {18, 14}, {18, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);
        return model;
    }

    /** I_dc(1 A) - R_load(10 Ohm) variant: asserts the current-source stamp. */
    private static CircuitModel currentSourceModel() {
        CircuitModel model = new CircuitModel();
        model.getCircuitComponents().add(comp(5, "I_dc", 4, new double[]{401, 1}));
        model.getCircuitComponents().add(comp(1, "R_load", 16, new double[]{10}));
        CircuitModel.ConnectionData out = new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {14, 10}});
        out.setLabel("V_out");
        model.getConnections().add(out);
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{18, 10}, {18, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);
        return model;
    }

    private static double runAndMeasureLast(CircuitModel model, String signal) {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertTrue(netlist.getElementCount() >= 2, "netlist must contain the elements");
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("smoke.ipes").circuitModel(model)
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .signals(java.util.List.of(signal))
                .build();
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(),
                () -> "simulation failed: " + result.getErrorMessage());
        DataContainerGlobal container = result.getDataContainer();
        assertNotNull(container, "successful run must carry a data container");
        double last = Double.NaN;
        for (int row = 0; row < container.getRowLength(); row++) {
            if (signal.equals(container.getSignalName(row))) {
            for (int col = 0; col <= container.getMaximumTimeIndex(row); col++) {
                double v = container.getValue(row, col);
                int step = col;
                assertTrue(Double.isFinite(v),
                        () -> signal + " went non-finite at step " + step);
                last = v;
            }
            }
        }
        return last;
    }

    @Test
    void resistorChainSplitsVoltage() {
        CircuitModel model = seriesModel(comp(1, "DUT_R", 10, new double[]{10}));
        double last = runAndMeasureLast(model, "V_out");
        assertEquals(24.0, last, 1.0, "48 V over two 10 Ohm resistors");
    }

    @Test
    void inductorConductsDcAfterTransient() {
        CircuitModel model = seriesModel(comp(2, "DUT_L", 10, new double[]{1e-3}));
        double last = runAndMeasureLast(model, "V_out");
        assertEquals(48.0, last, 2.0, "ideal L is a short for DC after 1 ms");
    }

    @Test
    void capacitorBlocksDc() {
        CircuitModel model = seriesModel(comp(3, "DUT_C", 10, new double[]{1e-6}));
        double last = runAndMeasureLast(model, "V_out");
        assertEquals(0.0, last, 1e-3, "series C carries no DC current");
    }

    @Test
    void diodeConductsForward() {
        CircuitModel model = seriesModel(comp(6, "DUT_D", 10, new double[]{1e7, 0.6, 0.01, 1e7}));
        double last = runAndMeasureLast(model, "V_out");
        assertEquals(48.0, last, 1.0, "forward diode minus its drop");
    }

    @Test
    void unGatedSwitchesStayFinite() {
        // no gate drive linked: gate-less default states differ per component
        // (ideal switch conducts, thyristor/IGBT block) - the gate is that the
        // solve stays finite either way (this caught the all-NaN stamping bug)
        double[][] paramSets = {
                new double[]{0.01, 0.01, 1e7},                       // ideal switch S
                new double[]{1e7, 0.6, 0.01, 1e7},                   // thyristor (no trigger)
                new double[]{0.01, 1e6, 0, 0},                       // IGBT
                new double[]{0.9999, 21.0, 10.0, 24.0, 10.0, 3.0, 8.0}, // MOSFET
        };
        int[] types = {7, 8, 10, 9};
        String[] names = {"S", "THYR", "IGBT", "MOSFET"};
        for (int i = 0; i < types.length; i++) {
            CircuitModel model = seriesModel(comp(types[i], "DUT_" + names[i], 10, paramSets[i]));
            runAndMeasureLast(model, "V_out");
        }
    }

    @Test
    void currentSourceDrivesLoad() {
        double last = runAndMeasureLast(currentSourceModel(), "V_out");
        assertEquals(10.0, last, 0.5, "1 A through 10 Ohm");
    }
}
