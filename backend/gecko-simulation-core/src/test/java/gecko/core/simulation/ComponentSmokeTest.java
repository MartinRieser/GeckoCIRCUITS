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
 * wire connectivity - exactly what editor-authored circuits look like.
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
                new double[]{1e7, 0.8, 0.01, 1e7},                   // IGBT
                new double[]{1e7, 0.8, 0.01, 1e7},                   // MOSFET
                new double[]{0.98, 0.0, 0.0},                        // Mutual coupling M
        };
        int[] types = {7, 8, 10, 28, 9};
        String[] names = {"S", "THYR", "IGBT", "MOSFET", "M"};
        for (int i = 0; i < types.length; i++) {
            CircuitModel model = seriesModel(comp(types[i], "DUT_" + names[i], 10, paramSets[i]));
            runAndMeasureLast(model, "V_out");
        }
    }

    @Test
    void allSeriesComponentsWithEmptyParametersSimulateFinite() {
        // Every two-terminal LK/REL/THERM component must survive an empty parameter[] array
        // (e.g. uninitialized web-editor placement or hand-authored .ipes) without NaN or infinite loops.
        int[] types = {1, 2, 3, 6, 7, 8, 10, 12, 24, 28, 46, 47};
        for (int type : types) {
            CircuitModel model = seriesModel(comp(type, "DUT_EMPTY_" + type, 10, new double[0]));
            double last = runAndMeasureLast(model, "V_out");
            assertTrue(Double.isFinite(last), "Empty parameters on type " + type + " must stay finite");
        }
    }

    @Test
    void sourcesWithEmptyParametersSimulateFinite() {
        // Voltage source (4) and Current source (5) with empty parameter[] must default to DC and stay finite
        CircuitModel vModel = seriesModel(comp(1, "DUT_R", 10, new double[]{10}));
        vModel.getCircuitComponents().get(0).setRawParameters(new double[0]);
        double vLast = runAndMeasureLast(vModel, "V_out");
        assertEquals(5.0, vLast, 0.5, "Empty U defaults to 10V DC -> 5V across two 10 Ohm resistors");

        CircuitModel iModel = currentSourceModel();
        iModel.getCircuitComponents().get(0).setRawParameters(new double[0]);
        double iLast = runAndMeasureLast(iModel, "V_out");
        assertEquals(10.0, iLast, 0.5, "Empty I defaults to 1A DC -> 10V across 10 Ohm load");
    }

    @Test
    void gatedSwitchesWithEmptyAndDefaultParametersSwitchCleanly() {
        // Exercises S (7), THYR (8), IGBT (10), and MOSFET (28) driven by SIGNAL (1004) -> GATE (1000)
        // both when switch/signal have empty double[0] parameters and when they have default parameters.
        int[] switchTypes = {7, 8, 10, 28};
        for (int swType : switchTypes) {
            for (boolean emptyParams : new boolean[]{true, false}) {
                double[] swParams = emptyParams
                        ? new double[0]
                        : (swType == 7
                                ? new double[]{1e7, 0.01, 1e7}
                                : new double[]{1e7, 0.8, 0.01, 1e7});
                CircuitModel.ComponentData sw = comp(swType, "SW_" + swType, 10, swParams);
                sw.setUniqueObjectIdentifier(300 + swType);
                sw.setCoupledReferenceID(600 + swType);
                CircuitModel model = seriesModel(sw);

                // Control: SIGNAL (1004) at (13,24) -> GATE (1000) at (25,24)
                CircuitModel.ComponentData sig = new CircuitModel.ComponentData(1004, "SIG", 13, 24, 503);
                sig.setUniqueObjectIdentifier(700 + swType);
                sig.setRawParameters(emptyParams ? new double[0] : new double[]{404.0, 1.0, 10000.0, 0.0, 0.0, 0.5});
                model.addControlComponent(sig);

                CircuitModel.ComponentData gate = new CircuitModel.ComponentData(1000, "GATE", 25, 24, 503);
                gate.setUniqueObjectIdentifier(600 + swType);
                gate.setCoupledReferenceID(300 + swType);
                gate.setRawParameters(emptyParams ? new double[0] : new double[]{0.0});
                model.addControlComponent(gate);

                model.getConnections().add(new CircuitModel.ConnectionData("CONTROL", new int[][]{{15, 24}, {23, 24}}));

                double last = runAndMeasureLast(model, "V_out");
                assertTrue(Double.isFinite(last),
                        "Gated switch type " + swType + " (emptyParams=" + emptyParams + ") must simulate finite");
            }
        }
    }

    @Test
    void issueIpesSwitchedRcWithEmptyIgbtAndSignalParametersChargesCapacitor() throws Exception {
        // Exact reproduction of C:\Users\mhr\Documents\issue.ipes:
        // U (10V DC, orient 503) -> R (1k, orient 504) -> IGBT (empty parameter[], orient 502)
        // -> C (100nF, orient 503) -> GLOBAL_TERMINAL (GND), with SIGNAL (empty parameter[]) -> GATE
        // and VOLT probe across C outputting u_C.
        String issueIpesText = """
                tDURATION 0.02
                dt 1.0E-6
                solverType 0
                e (0)
                <ElementLK>
                typ 4
                idStringDialog U
                uniqueObjectIdentifier 1
                x 12
                y 13
                orientierung 503
                parameter[] 401.0 10.0 50.0 0.0 0.0
                <\\ElementLK>
                e (1)
                <ElementLK>
                typ 1
                idStringDialog R
                uniqueObjectIdentifier 2
                x 18
                y 9
                orientierung 504
                parameter[] 1000.0
                <\\ElementLK>
                e (2)
                <ElementLK>
                typ 10
                idStringDialog IGBT
                uniqueObjectIdentifier 3
                x 26
                y 9
                orientierung 502
                parameter[] 
                coupledReferenceID[] 6
                <\\ElementLK>
                e (3)
                <ElementLK>
                typ 3
                idStringDialog C
                uniqueObjectIdentifier 4
                x 35
                y 12
                orientierung 503
                parameter[] 1.0E-7 0.0
                <\\ElementLK>
                e (4)
                <ElementLK>
                typ 31
                idStringDialog GLOBAL_TERMINAL
                uniqueObjectIdentifier 5
                x 12
                y 19
                orientierung 501
                parameter[] 
                <\\ElementLK>
                c (0)
                <ElementCONTROL>
                typ 1000
                idStringDialog GATE
                uniqueObjectIdentifier 6
                x 25
                y 24
                orientierung 503
                parameter[] 0.0
                coupledReferenceID[] 3
                <\\ElementCONTROL>
                c (1)
                <ElementCONTROL>
                typ 1004
                idStringDialog SIGNAL
                uniqueObjectIdentifier 7
                x 13
                y 24
                orientierung 503
                parameter[] 
                <\\ElementCONTROL>
                c (2)
                <ElementCONTROL>
                typ 1001
                idStringDialog VOLT
                uniqueObjectIdentifier 8
                x 44
                y 12
                orientierung 503
                parameter[] 
                parameterString[] C/NIX_NIX_NIX/0
                labelEndKnoten[] /u_C
                coupledReferenceID[] 4
                <\\ElementCONTROL>
                verbindungLK (0)
                <Connection>
                label NIX_NIX_NIX
                x[] 12 12 16
                y[] 11 9 9
                enabledShorted 1
                parentSheetIdentifier 0
                connectorType 0
                <\\Connection>
                verbindungLK (1)
                <Connection>
                label NIX_NIX_NIX
                x[] 20 24
                y[] 9 9
                enabledShorted 1
                parentSheetIdentifier 0
                connectorType 0
                <\\Connection>
                verbindungLK (2)
                <Connection>
                label NIX_NIX_NIX
                x[] 28 35 35
                y[] 9 9 10
                enabledShorted 1
                parentSheetIdentifier 0
                connectorType 0
                <\\Connection>
                verbindungLK (3)
                <Connection>
                label NIX_NIX_NIX
                x[] 35 35 12
                y[] 14 15 15
                enabledShorted 1
                parentSheetIdentifier 0
                connectorType 0
                <\\Connection>
                verbindungLK (4)
                <Connection>
                label NIX_NIX_NIX
                x[] 12 12
                y[] 15 17
                enabledShorted 1
                parentSheetIdentifier 0
                connectorType 0
                <\\Connection>
                verbindungCONTROL (0)
                <Connection>
                label NIX_NIX_NIX
                x[] 15 23
                y[] 24 24
                enabledShorted 1
                parentSheetIdentifier 0
                connectorType 1
                <\\Connection>
                """;
        CircuitModel model = new gecko.core.io.CircuitFileParser().parse(
                new java.io.BufferedReader(new java.io.StringReader(issueIpesText)), "issue.ipes");
        double finalUc = runAndMeasureLast(model, "u_C");
        assertTrue(finalUc > 8.0 && finalUc < 10.0,
                "Capacitor u_C in issue.ipes must charge to ~9.2V (10V - Vce_sat), got: " + finalUc);
    }

    @Test
    void currentSourceDrivesLoad() {
        double last = runAndMeasureLast(currentSourceModel(), "V_out");
        assertEquals(10.0, last, 0.5, "1 A through 10 Ohm");
    }
}
