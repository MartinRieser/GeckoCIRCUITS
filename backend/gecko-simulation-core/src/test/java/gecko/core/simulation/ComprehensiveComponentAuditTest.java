package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.ComponentTerminals;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive component audit test suite covering:
 * <ul>
 *   <li>All 2-terminal power components across all 4 orientations (501..504) and all 3 solvers (BE, TRZ, GS)</li>
 *   <li>Both empty ({@code new double[0]}) and populated parameter arrays</li>
 *   <li>Voltmeter (1001) and Ammeter (1002) probes attached to every component type (including voltage sources,
 *       thermal components, and reluctance components)</li>
 *   <li>Sinusoidal and DC voltage/current sources with DC offsets</li>
 *   <li>Multi-pin and coupled components (Ideal Transformer 23, BJT 33, Coupled Inductors 12 + Mutual Coupling 9)</li>
 *   <li>End-to-end simulation of control blocks (1004..1058) inside {@link HeadlessSimulationEngine}</li>
 * </ul>
 */
class ComprehensiveComponentAuditTest {

    private static final int[] ALL_ORIENTATIONS = {
            ComponentTerminals.SOUTH_NORTH, // 501
            ComponentTerminals.WEST_EAST,   // 502
            ComponentTerminals.NORTH_SOUTH, // 503
            ComponentTerminals.EAST_WEST    // 504
    };

    private static final SolverType[] ALL_SOLVERS = {
            SolverType.SOLVER_BE,
            SolverType.SOLVER_TRZ,
            SolverType.SOLVER_GS
    };

    private static CircuitModel.ComponentData comp(int type, String name, int x, int y, int orient, double[] params) {
        CircuitModel.ComponentData c = new CircuitModel.ComponentData(type, name, x, y, orient);
        c.setRawParameters(params);
        c.setUniqueObjectIdentifier(Math.abs((long) name.hashCode()) + 100L);
        return c;
    }

    /**
     * Builds a series test circuit: V_dc(48 V) -> CUT(at 10,10 with given orientation) -> R_load(10 Ohm) -> GND,
     * plus a VOLT probe (1001) and AMP probe (1002) coupled to CUT, and a GATE (1000) driven by CONSTANT(1.0)
     * when CUT is a gated switch.
     */
    private static CircuitModel buildSeriesAuditCircuit(
            int cutType, int cutOrientation, double[] cutParams, boolean gateOn) {
        CircuitModel model = new CircuitModel();

        // Voltage source at (4, 10) with EAST_WEST (504): input (nodeX, +) at (6,10), output (nodeY, -) at (2,10)
        CircuitModel.ComponentData vSource = comp(4, "V_dc", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 48.0});
        model.getCircuitComponents().add(vSource);

        // Component under test at (10, 10) with arbitrary orientation
        CircuitModel.ComponentData cut = comp(cutType, "CUT", 10, 10, cutOrientation, cutParams);
        model.getCircuitComponents().add(cut);

        // Load resistor at (16, 10) WEST_EAST (502): input at (14,10), output at (18,10)
        CircuitModel.ComponentData rLoad = comp(1, "R_load", 16, 10, ComponentTerminals.WEST_EAST, new double[]{10.0});
        model.getCircuitComponents().add(rLoad);

        // Compute CUT terminal coordinates for its orientation
        List<int[]> cutTerms = ComponentTerminals.terminalsOf(cut, cut.getPosition(), cutOrientation);
        int[] cutIn = cutTerms.get(0);
        int[] cutOut = cutTerms.get(1);

        // Wire from V_dc+ (6,10) to cutIn
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{6, 10}, {6, cutIn[1]}, {cutIn[0], cutIn[1]}}));

        // Wire from cutOut to R_load in (14,10) labeled V_out
        CircuitModel.ConnectionData outWire = new CircuitModel.ConnectionData("LK",
                new int[][]{{cutOut[0], cutOut[1]}, {14, cutOut[1]}, {14, 10}});
        outWire.setLabel("V_out");
        model.getConnections().add(outWire);

        // GND return from R_load out (18,10) to V_dc- (2,10)
        CircuitModel.ConnectionData gndWire = new CircuitModel.ConnectionData("LK",
                new int[][]{{18, 10}, {18, 16}, {2, 16}, {2, 10}});
        gndWire.setLabel("GND");
        model.getConnections().add(gndWire);

        // Attach VOLT probe (1001) coupled to CUT
        CircuitModel.ComponentData voltProbe = comp(1001, "V_cut", 6, 22, ComponentTerminals.NORTH_SOUTH, new double[0]);
        voltProbe.setFamily("CONTROL");
        voltProbe.setCoupledReferenceID(cut.getUniqueObjectIdentifier());
        voltProbe.setParameterStrings(new String[]{"CUT", "NIX_NIX_NIX", "0"});
        voltProbe.setTerminalYLabels(new String[]{"V_cut_sig"});
        model.getControlComponents().add(voltProbe);

        // Attach AMP probe (1002) coupled to CUT
        CircuitModel.ComponentData ampProbe = comp(1002, "I_cut", 14, 22, ComponentTerminals.NORTH_SOUTH, new double[0]);
        ampProbe.setFamily("CONTROL");
        ampProbe.setCoupledReferenceID(cut.getUniqueObjectIdentifier());
        ampProbe.setParameterStrings(new String[]{"CUT", "NIX_NIX_NIX", "0"});
        ampProbe.setTerminalYLabels(new String[]{"I_cut_sig"});
        model.getControlComponents().add(ampProbe);

        // If gated switch (LK_S=7, LK_THYR=8, LK_IGBT=10, LK_MOSFET=28), wire CONSTANT -> GATE coupled to CUT
        if (cutType == 7 || cutType == 8 || cutType == 10 || cutType == 28) {
            CircuitModel.ComponentData gateSource = comp(1005, "G_src", 6, 26, ComponentTerminals.NORTH_SOUTH,
                    new double[]{gateOn ? 1.0 : 0.0});
            gateSource.setFamily("CONTROL");
            model.getControlComponents().add(gateSource);

            CircuitModel.ComponentData gateBlock = comp(1000, "GATE_1", 12, 26, ComponentTerminals.NORTH_SOUTH, new double[0]);
            gateBlock.setFamily("CONTROL");
            gateBlock.setCoupledReferenceID(cut.getUniqueObjectIdentifier());
            gateBlock.setParameterStrings(new String[]{"CUT"});
            model.getControlComponents().add(gateBlock);

            // Control wire from G_src output (8,26) to GATE_1 input (10,26)
            model.getConnections().add(new CircuitModel.ConnectionData("CONTROL",
                    new int[][]{{8, 26}, {10, 26}}));
        }

        return model;
    }

    static Stream<Arguments> twoTerminalOrientationsAndSolvers() {
        int[] types = {1, 2, 3, 6, 7, 8, 10, 12, 28};
        Stream.Builder<Arguments> builder = Stream.builder();
        for (int type : types) {
            for (int orient : ALL_ORIENTATIONS) {
                for (SolverType solver : ALL_SOLVERS) {
                    builder.add(Arguments.of(type, orient, solver));
                }
            }
        }
        return builder.build();
    }

    @ParameterizedTest(name = "type={0}, orient={1}, solver={2}")
    @MethodSource("twoTerminalOrientationsAndSolvers")
    void allTwoTerminalComponentsSimulateFiniteAcrossOrientationsAndSolvers(
            int cutType, int orientation, SolverType solver) {
        // Test with empty parameters (verifying safe normalization + solver stability + probe readout)
        CircuitModel model = buildSeriesAuditCircuit(cutType, orientation, new double[0], true);
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("audit.ipes")
                .circuitModel(model)
                .solverType(solver)
                .stepWidth(5e-6)
                .simulationDuration(2e-4)
                .signals(List.of("V_out", "V_cut_sig", "I_cut_sig"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), () -> "Simulation failed for type=" + cutType
                + " orient=" + orientation + " solver=" + solver + ": " + result.getErrorMessage());

        DataContainerGlobal dc = result.getDataContainer();
        assertNotNull(dc);
        double vOutLast = assertSignalFiniteAndGetLast(dc, "V_out");
        double vCutLast = assertSignalFiniteAndGetLast(dc, "V_cut_sig");
        double iCutLast = assertSignalFiniteAndGetLast(dc, "I_cut_sig");

        // KVL check: V_dc (48V) = V_cut + V_out, and I_cut = V_out / R_load (10 Ohm)
        assertEquals(48.0, vCutLast + vOutLast, 1.5,
                "KVL (V_cut + V_out == 48V) violated for type=" + cutType);
        assertEquals(vOutLast / 10.0, iCutLast, 0.25,
                "Ohm's law (I_cut == V_out / 10) violated for type=" + cutType);
    }

    @Test
    void ammeterCoupledToVoltageSourceMeasuresBranchCurrentNotVoltageSetting() {
        CircuitModel model = new CircuitModel();
        // 48V DC voltage source driving a 10 Ohm resistor -> branch current magnitude is 4.8 A (not 48.0!)
        CircuitModel.ComponentData vSource = comp(4, "U_src", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 48.0});
        CircuitModel.ComponentData rLoad = comp(1, "R_load", 12, 10, ComponentTerminals.WEST_EAST, new double[]{10.0});
        model.getCircuitComponents().add(vSource);
        model.getCircuitComponents().add(rLoad);

        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {10, 10}}));
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{14, 10}, {14, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        // Ammeter coupled to U_src by name
        CircuitModel.ComponentData ampU = comp(1002, "AMP_U", 4, 18, ComponentTerminals.NORTH_SOUTH, new double[0]);
        ampU.setFamily("CONTROL");
        ampU.setParameterStrings(new String[]{"U_src"});
        ampU.setTerminalYLabels(new String[]{"I_source"});
        model.getControlComponents().add(ampU);

        // Ammeter coupled to R_load by name
        CircuitModel.ComponentData ampR = comp(1002, "AMP_R", 12, 18, ComponentTerminals.NORTH_SOUTH, new double[0]);
        ampR.setFamily("CONTROL");
        ampR.setParameterStrings(new String[]{"R_load"});
        ampR.setTerminalYLabels(new String[]{"I_load"});
        model.getControlComponents().add(ampR);

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("amp_u.ipes")
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(1e-4)
                .signals(List.of("I_source", "I_load"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess());
        double iSource = assertSignalFiniteAndGetLast(result.getDataContainer(), "I_source");
        double iLoad = assertSignalFiniteAndGetLast(result.getDataContainer(), "I_load");

        assertEquals(4.8, iLoad, 1e-6, "Load current should be 48V / 10 Ohm = 4.8 A");
        assertEquals(4.8, Math.abs(iSource), 1e-6,
                "Ammeter on LK_U must read branch current magnitude (4.8 A), not voltage parameter (48.0 V)");
    }

    @Test
    void sinusoidalCurrentSourceWithDcOffsetHasConsistentPolarity() {
        CircuitModel model = new CircuitModel();
        // Sinusoidal current source: amplitude = 1.0 A, f = 1000 Hz, offset = +2.5 A
        // At t = 0, sin(0) = 0 -> current is +2.5 A -> across 10 Ohm resistor, V_out at t=0 is +25.0 V,
        // and over a full 1ms period, V_out stays between +15.0 V and +35.0 V (never negative!).
        double[] sinParams = new double[25];
        sinParams[0] = 402;   // QUELLE_SIN
        sinParams[1] = 1.0;   // value_dc fallback
        sinParams[2] = 1000.0; // frequency
        sinParams[3] = 2.5;   // DC offset (+2.5 A)
        sinParams[4] = 0.0;   // phase
        sinParams[20] = 1.0;  // amplitude_sin (1.0 A)

        CircuitModel.ComponentData iSin = comp(5, "I_sin", 4, 10, ComponentTerminals.WEST_EAST, sinParams);
        CircuitModel.ComponentData rLoad = comp(1, "R_load", 12, 10, ComponentTerminals.WEST_EAST, new double[]{10.0});
        model.getCircuitComponents().add(iSin);
        model.getCircuitComponents().add(rLoad);

        CircuitModel.ConnectionData out = new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {10, 10}});
        out.setLabel("V_out");
        model.getConnections().add(out);
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{14, 10}, {14, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("isin_offset.ipes")
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(1e-3)
                .signals(List.of("V_out"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess());
        DataContainerGlobal dc = result.getDataContainer();
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
        for (int row = 0; row < dc.getRowLength(); row++) {
            if ("V_out".equals(dc.getSignalName(row))) {
                for (int col = 0; col <= dc.getMaximumTimeIndex(row); col++) {
                    double v = dc.getValue(row, col);
                    minV = Math.min(minV, v);
                    maxV = Math.max(maxV, v);
                }
            }
        }
        assertEquals(15.0, minV, 0.5, "Minimum voltage with +2.5A offset and 1A amplitude on 10 Ohm should be +15V");
        assertEquals(35.0, maxV, 0.5, "Maximum voltage with +2.5A offset and 1A amplitude on 10 Ohm should be +35V");
    }

    @Test
    void thermalDomainComponentsAndProbesSimulateCorrectlyWithThermWires() {
        CircuitModel model = new CircuitModel();
        // TH_TEMP (45) = 100 K driving TH_RTH (46) = 2 K/W in parallel with TH_CTH (47) = 1e-4 J/K
        CircuitModel.ComponentData thTemp = comp(45, "T_src", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 100.0});
        thTemp.setFamily("THERM");
        CircuitModel.ComponentData thRth = comp(46, "Rth_1", 12, 10, ComponentTerminals.WEST_EAST, new double[]{2.0});
        thRth.setFamily("THERM");
        model.getThermalComponents().add(thTemp);
        model.getThermalComponents().add(thRth);

        // Use "THERM" wire type to verify NetlistBuilder accepts both "THERM" and "THERMAL"
        CircuitModel.ConnectionData hotWire = new CircuitModel.ConnectionData("THERM", new int[][]{{6, 10}, {10, 10}});
        hotWire.setLabel("T_hot");
        model.getConnections().add(hotWire);
        CircuitModel.ConnectionData gndWire = new CircuitModel.ConnectionData("THERM",
                new int[][]{{14, 10}, {14, 14}, {2, 14}, {2, 10}});
        gndWire.setLabel("GND");
        model.getConnections().add(gndWire);

        // Probes coupled by name to thermal resistor Rth_1 (without uid coupling, testing elementIndexByName on THERM)
        CircuitModel.ComponentData voltTh = comp(1001, "V_th", 4, 18, ComponentTerminals.NORTH_SOUTH, new double[0]);
        voltTh.setFamily("CONTROL");
        voltTh.setParameterStrings(new String[]{"Rth_1"});
        voltTh.setTerminalYLabels(new String[]{"T_drop"});
        model.getControlComponents().add(voltTh);

        CircuitModel.ComponentData ampTh = comp(1002, "I_th", 12, 18, ComponentTerminals.NORTH_SOUTH, new double[0]);
        ampTh.setFamily("CONTROL");
        ampTh.setParameterStrings(new String[]{"Rth_1"});
        ampTh.setTerminalYLabels(new String[]{"Q_flow"});
        model.getControlComponents().add(ampTh);

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("thermal_audit.ipes")
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(1e-4)
                .signals(List.of("T_hot", "T_drop", "Q_flow"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess());
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.contains("DRC_PROBE_ERROR")),
                () -> "Thermal probes should resolve without DRC_PROBE_ERROR, got: " + result.getWarnings());

        double tHot = assertSignalFiniteAndGetLast(result.getDataContainer(), "T_hot");
        double tDrop = assertSignalFiniteAndGetLast(result.getDataContainer(), "T_drop");
        double qFlow = assertSignalFiniteAndGetLast(result.getDataContainer(), "Q_flow");

        assertEquals(100.0, tHot, 1e-4);
        assertEquals(100.0, tDrop, 1e-4);
        assertEquals(50.0, qFlow, 1e-4, "Heat flow should be 100 K / 2 (K/W) = 50 W");
    }

    @Test
    void reluctanceDomainComponentsAndProbesSimulateCorrectly() {
        CircuitModel model = new CircuitModel();
        // REL_MMF (26) with empty parameters (defaults to 100 A-turns) driving REL_RELUCTANCE (24) with empty parameters (defaults to 1.0 1/H)
        CircuitModel.ComponentData mmf = comp(26, "MMF_1", 4, 10, ComponentTerminals.EAST_WEST, new double[0]);
        CircuitModel.ComponentData rel = comp(24, "Rm_1", 12, 10, ComponentTerminals.WEST_EAST, new double[0]);
        model.getCircuitComponents().add(mmf);
        model.getCircuitComponents().add(rel);

        CircuitModel.ConnectionData fluxWire = new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {10, 10}});
        fluxWire.setLabel("Theta_out");
        model.getConnections().add(fluxWire);
        CircuitModel.ConnectionData gndWire = new CircuitModel.ConnectionData("LK",
                new int[][]{{14, 10}, {14, 14}, {2, 14}, {2, 10}});
        gndWire.setLabel("GND");
        model.getConnections().add(gndWire);

        CircuitModel.ComponentData ampRel = comp(1002, "Phi_probe", 12, 18, ComponentTerminals.NORTH_SOUTH, new double[0]);
        ampRel.setFamily("CONTROL");
        ampRel.setParameterStrings(new String[]{"Rm_1"});
        ampRel.setTerminalYLabels(new String[]{"Phi"});
        model.getControlComponents().add(ampRel);

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("reluctance_audit.ipes")
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(1e-4)
                .signals(List.of("Theta_out", "Phi"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess());
        double theta = assertSignalFiniteAndGetLast(result.getDataContainer(), "Theta_out");
        double phi = assertSignalFiniteAndGetLast(result.getDataContainer(), "Phi");
        assertEquals(100.0, theta, 1e-4);
        assertEquals(100.0, phi, 1e-4);
    }

    @Test
    void multiPinAndCoupledComponentsSimulateWithEmptyAndDefaultParameters() {
        // 1. Ideal Transformer (LK_TRANS = 23) with empty parameters (defaults: n1=10, n2=2, polarity=-1 -> 5:1 step-down)
        CircuitModel transModel = new CircuitModel();
        CircuitModel.ComponentData vPrim = comp(4, "V_prim", 4, 10, ComponentTerminals.SOUTH_NORTH, new double[]{401, 24.0});
        // Transformer at (10, 10) NORTH_SOUTH: primary pins at (9, 8) and (9, 12); secondary pins at (11, 8) and (11, 12)
        CircuitModel.ComponentData trans = comp(23, "TR_1", 10, 10, ComponentTerminals.NORTH_SOUTH, new double[0]);
        CircuitModel.ComponentData rSec = comp(1, "R_sec", 16, 10, ComponentTerminals.NORTH_SOUTH, new double[]{10.0});
        transModel.getCircuitComponents().addAll(List.of(vPrim, trans, rSec));

        transModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{4, 8}, {9, 8}}));
        CircuitModel.ConnectionData primGnd = new CircuitModel.ConnectionData("LK", new int[][]{{4, 12}, {9, 12}});
        primGnd.setLabel("GND");
        transModel.getConnections().add(primGnd);

        CircuitModel.ConnectionData secTop = new CircuitModel.ConnectionData("LK", new int[][]{{11, 8}, {16, 8}});
        secTop.setLabel("V_sec");
        transModel.getConnections().add(secTop);
        CircuitModel.ConnectionData secBot = new CircuitModel.ConnectionData("LK", new int[][]{{11, 12}, {16, 12}});
        secBot.setLabel("GND");
        transModel.getConnections().add(secBot);

        SimulationResult transRes = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitFile("trans.ipes").circuitModel(transModel)
                        .stepWidth(1e-5).simulationDuration(1e-4)
                        .signals(List.of("V_sec")).build());
        assertTrue(transRes.isSuccess(), () -> "Transformer simulation failed: " + transRes.getErrorMessage());
        double vSec = assertSignalFiniteAndGetLast(transRes.getDataContainer(), "V_sec");
        assertEquals(4.8, Math.abs(vSec), 1e-3, "10:2 default ideal transformer secondary magnitude should be 24V / 5 = 4.8V");

        // 2. Bipolar Junction Transistor (LK_BJT = 33) with empty parameters (defaults: betaF=100, betaB=1, rBase=10)
        CircuitModel bjtModel = new CircuitModel();
        CircuitModel.ComponentData vcc = comp(4, "V_cc", 16, 10, ComponentTerminals.SOUTH_NORTH, new double[]{401, 12.0});
        CircuitModel.ComponentData rc = comp(1, "R_c", 10, 5, ComponentTerminals.NORTH_SOUTH, new double[]{10.0});
        // BJT at (10, 10) NORTH_SOUTH: Collector=(10, 8), Base=(8, 10), Emitter=(10, 12)
        CircuitModel.ComponentData bjt = comp(33, "Q_1", 10, 10, ComponentTerminals.NORTH_SOUTH, new double[0]);
        CircuitModel.ComponentData vBase = comp(4, "V_b", 4, 10, ComponentTerminals.SOUTH_NORTH, new double[]{401, 5.0});
        bjtModel.getCircuitComponents().addAll(List.of(vcc, rc, bjt, vBase));

        // V_cc top (16,8) to R_c top (10,3)
        bjtModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{16, 8}, {16, 3}, {10, 3}}));
        // R_c bottom (10,7) to BJT Collector (10,8) labeled V_collector
        CircuitModel.ConnectionData vColWire = new CircuitModel.ConnectionData("LK", new int[][]{{10, 7}, {10, 8}});
        vColWire.setLabel("V_collector");
        bjtModel.getConnections().add(vColWire);
        // V_b top (4,8) to BJT Base (8,10)
        bjtModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{4, 8}, {8, 8}, {8, 10}}));
        // GND return connecting V_b bottom (4,12), BJT Emitter (10,12), V_cc bottom (16,12)
        CircuitModel.ConnectionData bjtGnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{4, 12}, {10, 12}, {16, 12}});
        bjtGnd.setLabel("GND");
        bjtModel.getConnections().add(bjtGnd);

        SimulationResult bjtRes = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitFile("bjt.ipes").circuitModel(bjtModel)
                        .stepWidth(1e-5).simulationDuration(1e-4)
                        .signals(List.of("V_collector")).build());
        assertTrue(bjtRes.isSuccess(), () -> "BJT simulation failed: " + bjtRes.getErrorMessage());
        double vCollector = assertSignalFiniteAndGetLast(bjtRes.getDataContainer(), "V_collector");
        assertTrue(vCollector < 2.0, "Forward-biased NPN BJT should pull V_collector into saturation (< 2V), got " + vCollector);

        // 2. Coupled Inductors (LK_LKOP2 = 12) + Mutual Coupling (LK_M = 9) with string reference fallback
        CircuitModel coupledModel = new CircuitModel();
        // AC source on primary coupled inductor Lcop1, resistor load on secondary coupled inductor Lcop2
        double[] acParams = new double[25];
        acParams[0] = 402; acParams[2] = 1000.0; acParams[20] = 100.0;
        CircuitModel.ComponentData vAc = comp(4, "V_ac", 4, 10, ComponentTerminals.SOUTH_NORTH, acParams);
        CircuitModel.ComponentData l1 = comp(12, "Lcop1", 10, 10, ComponentTerminals.NORTH_SOUTH, new double[]{1e-3, 0.0});
        CircuitModel.ComponentData l2 = comp(12, "Lcop2", 16, 10, ComponentTerminals.NORTH_SOUTH, new double[]{1e-3, 0.0});
        CircuitModel.ComponentData r2 = comp(1, "R_sec2", 22, 10, ComponentTerminals.NORTH_SOUTH, new double[]{100.0});
        // LK_M with 1-element raw parameter (from DEFAULT_PARAMETERS) + parameterStrings naming Lcop1 and Lcop2
        CircuitModel.ComponentData kCoupler = comp(9, "K_1", 13, 6, ComponentTerminals.NORTH_SOUTH, new double[]{0.95});
        kCoupler.setParameterStrings(new String[]{"Lcop1", "Lcop2"});
        coupledModel.getCircuitComponents().addAll(List.of(vAc, l1, l2, r2, kCoupler));

        coupledModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{4, 8}, {10, 8}}));
        CircuitModel.ConnectionData cGnd1 = new CircuitModel.ConnectionData("LK", new int[][]{{4, 12}, {10, 12}});
        cGnd1.setLabel("GND");
        coupledModel.getConnections().add(cGnd1);

        CircuitModel.ConnectionData cSecTop = new CircuitModel.ConnectionData("LK", new int[][]{{16, 8}, {22, 8}});
        cSecTop.setLabel("V_coupled_sec");
        coupledModel.getConnections().add(cSecTop);
        CircuitModel.ConnectionData cGnd2 = new CircuitModel.ConnectionData("LK", new int[][]{{16, 12}, {22, 12}});
        cGnd2.setLabel("GND");
        coupledModel.getConnections().add(cGnd2);

        SimulationResult coupledRes = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder()
                        .circuitFile("coupled.ipes").circuitModel(coupledModel)
                        .stepWidth(1e-5).simulationDuration(2.5e-4)
                        .signals(List.of("V_coupled_sec")).build());
        assertTrue(coupledRes.isSuccess());
        assertTrue(coupledRes.getWarnings().isEmpty(), () -> "Unexpected warnings: " + coupledRes.getWarnings());
        double vCoupled = assertSignalFiniteAndGetLast(coupledRes.getDataContainer(), "V_coupled_sec");
        assertTrue(Math.abs(vCoupled) > 1.0, "Mutually coupled secondary inductor must induce non-zero voltage");
    }

    static Stream<Arguments> controlBlocksAuditSource() {
        return Stream.of(
                // 1-input blocks driven by input A = 0.5
                Arguments.of(1006, new double[]{4.0}, 0.5, 0.0, 2.0),         // GAIN (4 * 0.5 = 2.0)
                Arguments.of(1007, new double[]{2.0, 1.0}, 0.5, 0.0, 1.0),    // PI
                Arguments.of(1008, new double[]{1e-5}, 0.5, 0.0, 0.5),        // PT1 (fast tau -> converges to 0.5)
                Arguments.of(1013, new double[0], 0.0, 0.0, 1.0),             // NOT (0 -> 1)
                Arguments.of(1015, new double[]{1e-5}, 0.5, 0.0, 0.5),        // DELAY
                Arguments.of(1021, new double[]{-0.2, 0.2}, 0.5, 0.0, 0.2),   // LIMIT (clamps 0.5 to 0.2)
                Arguments.of(1022, new double[0], -3.5, 0.0, 3.5),            // ABS
                Arguments.of(1023, new double[0], 9.0, 0.0, 3.0),             // SQRT
                Arguments.of(1024, new double[0], 0.0, 0.0, 1.0),             // EXP(0) = 1
                Arguments.of(1025, new double[0], Math.E, 0.0, 1.0),          // LN(e) = 1
                Arguments.of(1026, new double[0], Math.PI / 2, 0.0, 1.0),     // SIN(pi/2) = 1
                Arguments.of(1027, new double[0], 0.0, 0.0, 1.0),             // COS(0) = 1
                Arguments.of(1030, new double[]{-0.1, 0.1}, 0.5, 0.0, 1.0),   // HYS
                Arguments.of(1031, new double[]{1e-5, 1.0, 1.0}, 0.5, 0.0, 0.5), // PT2
                Arguments.of(1032, new double[]{2.0, 0.0}, 0.5, 0.0, 0.0),    // PD (differentiator on constant input -> 0.0 at steady state)
                Arguments.of(1038, new double[0], 0.0, 0.0, 0.0),             // ASIN(0) = 0
                Arguments.of(1039, new double[0], 1.0, 0.0, 0.0),             // ACOS(1) = 0
                Arguments.of(1040, new double[0], 0.0, 0.0, 0.0),             // TAN(0) = 0
                Arguments.of(1041, new double[0], 1.0, 0.0, Math.PI / 4),     // ATAN(1) = pi/4
                Arguments.of(1042, new double[0], 4.0, 0.0, 16.0),            // SQR(4) = 16
                Arguments.of(1044, new double[0], 3.7, 0.0, 4.0),             // ROUND(3.7) = 4
                Arguments.of(1045, new double[0], -5.0, 0.0, -1.0),           // SIGN(-5) = -1
                // 2-input blocks driven by input A and input B
                Arguments.of(1010, new double[0], 5.0, 2.0, 1.0),             // COMPARATOR (5 > 2 -> 1)
                Arguments.of(1011, new double[0], 1.0, 1.0, 1.0),             // AND (1 & 1 -> 1)
                Arguments.of(1012, new double[0], 0.0, 1.0, 1.0),             // OR (0 | 1 -> 1)
                Arguments.of(1017, new double[0], 7.0, 2.5, 4.5),             // SUB (7 - 2.5 = 4.5)
                Arguments.of(1018, new double[0], 3.0, 4.5, 7.5),             // ADD (3 + 4.5 = 7.5)
                Arguments.of(1019, new double[0], 3.0, 4.0, 12.0),            // MUL (3 * 4 = 12)
                Arguments.of(1020, new double[0], 10.0, 4.0, 2.5),            // DIV (10 / 4 = 2.5)
                Arguments.of(1028, new double[0], 3.0, 8.0, 3.0),             // MIN (3, 8 -> 3)
                Arguments.of(1029, new double[0], 3.0, 8.0, 8.0),             // MAX (3, 8 -> 8)
                Arguments.of(1033, new double[0], 4.2, 1.0, 4.2),             // SAMPLEHOLD
                Arguments.of(1035, new double[0], 1.0, 0.0, 1.0),             // XOR (1 ^ 0 -> 1)
                Arguments.of(1036, new double[0], 3.0, 3.0, 1.0),             // GE (3 >= 3 -> 1)
                Arguments.of(1043, new double[0], 2.0, 3.0, 8.0),             // POW (2^3 = 8)
                Arguments.of(1046, new double[0], 5.0, 5.0, 1.0),             // EQ (5 == 5 -> 1)
                Arguments.of(1047, new double[0], 5.0, 2.0, 1.0)              // NE (5 != 2 -> 1)
        );
    }

    @ParameterizedTest(name = "ctrlType={0}")
    @MethodSource("controlBlocksAuditSource")
    void controlBlocksComputeExpectedTransferInFullEngineRun(
            int ctrlType, double[] params, double inA, double inB, double expectedOut) {
        CircuitModel model = new CircuitModel();
        // Minimal power circuit so the engine runs its full time-stepping loop
        CircuitModel.ComponentData v = comp(4, "V1", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 10.0});
        CircuitModel.ComponentData r = comp(1, "R1", 12, 10, ComponentTerminals.WEST_EAST, new double[]{10.0});
        model.getCircuitComponents().addAll(List.of(v, r));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {10, 10}}));
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{14, 10}, {14, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        // Input constant A at (4, 20) -> output pin at (6, 20)
        CircuitModel.ComponentData constA = comp(1005, "C_A", 4, 20, ComponentTerminals.NORTH_SOUTH, new double[]{inA});
        constA.setFamily("CONTROL");
        model.getControlComponents().add(constA);

        // Input constant B at (4, 24) -> output pin at (6, 24)
        CircuitModel.ComponentData constB = comp(1005, "C_B", 4, 24, ComponentTerminals.NORTH_SOUTH, new double[]{inB});
        constB.setFamily("CONTROL");
        model.getControlComponents().add(constB);

        // Control block under test at (12, 20): input 0 at (10, 20), input 1 at (10, 21), output 0 at (14, 20)
        CircuitModel.ComponentData dut = comp(ctrlType, "CTRL_DUT", 12, 20, ComponentTerminals.NORTH_SOUTH, params);
        dut.setFamily("CONTROL");
        dut.setTerminalYLabels(new String[]{"ctrl_out"});
        model.getControlComponents().add(dut);

        model.getConnections().add(new CircuitModel.ConnectionData("CONTROL", new int[][]{{6, 20}, {10, 20}}));
        model.getConnections().add(new CircuitModel.ConnectionData("CONTROL", new int[][]{{6, 24}, {10, 24}, {10, 21}}));

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile("ctrl_audit.ipes")
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(5e-4)
                .signals(List.of("ctrl_out"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), () -> "Control block " + ctrlType + " failed: " + result.getErrorMessage());
        double actual = assertSignalFiniteAndGetLast(result.getDataContainer(), "ctrl_out");
        assertEquals(expectedOut, actual, 0.05,
                "Control block type " + ctrlType + " output mismatch");
    }

    @Test
    void mosfetAntiparallelBodyDiodeConductsInReverseWhenGateIsOff() {
        CircuitModel model = new CircuitModel();
        // V1 at (4, 10, EAST_WEST): nodeX (+) at (6, 10), nodeY (-) at (2, 10)
        CircuitModel.ComponentData v1 = comp(4, "V1", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 20.0});
        // MOSFET at (10, 10, WEST_EAST): nodeX (Drain) at (8, 10), nodeY (Source) at (12, 10)
        CircuitModel.ComponentData mosfet = comp(28, "M1", 10, 10, ComponentTerminals.WEST_EAST,
                new double[]{1e7, 0.8, 10e-3, 1e7});
        mosfet.setUniqueObjectIdentifier(901L);
        CircuitModel.ComponentData r1 = comp(1, "R1", 16, 10, ComponentTerminals.WEST_EAST, new double[]{10.0});
        model.getCircuitComponents().addAll(List.of(v1, mosfet, r1));

        // Connect V1- (2,10) to MOSFET Drain (8,10) as GND:
        CircuitModel.ConnectionData gndWire = new CircuitModel.ConnectionData("LK",
                new int[][]{{2, 10}, {2, 6}, {8, 6}, {8, 10}});
        gndWire.setLabel("GND");
        model.getConnections().add(gndWire);
        // Connect V1+ (6,10) to R1(18,10), and R1(14,10) to MOSFET Source (12,10):
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{6, 10}, {6, 14}, {18, 14}, {18, 10}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{12, 10}, {14, 10}}));

        // Ammeter coupled to M1
        CircuitModel.ComponentData amp = comp(1002, "AMP_M1", 10, 20, ComponentTerminals.NORTH_SOUTH, new double[0]);
        amp.setFamily("CONTROL");
        amp.setCoupledReferenceID(901L);
        amp.setParameterStrings(new String[]{"M1"});
        amp.setTerminalYLabels(new String[]{"I_M1"});
        model.getControlComponents().add(amp);

        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(1e-4)
                .signals(List.of("I_M1"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), () -> "MOSFET reverse body diode sim failed: " + result.getErrorMessage());
        double current = assertSignalFiniteAndGetLast(result.getDataContainer(), "I_M1");
        // Current from Drain(X) to Source(Y) is -(20 - 0.8) / (10 + 0.01) = -1.918 A
        assertEquals(-1.918, current, 0.02, "MOSFET body diode must conduct in reverse when gate is OFF");
    }

    @Test
    void idealTransformerAndBjtSupportCoupledAmmeterAndVoltmeterProbes() {
        CircuitModel model = new CircuitModel();
        // Part 1: Ideal Transformer TR1 (2:1 ratio) driven by 20V source with 10 Ohm secondary load
        CircuitModel.ComponentData v1 = comp(4, "V1", 4, 10, ComponentTerminals.SOUTH_NORTH, new double[]{401, 20.0});
        CircuitModel.ComponentData tr1 = comp(23, "TR1", 10, 10, ComponentTerminals.NORTH_SOUTH, new double[]{2.0, 1.0, 1.0});
        tr1.setUniqueObjectIdentifier(910L);
        CircuitModel.ComponentData rSec = comp(1, "R_sec", 16, 10, ComponentTerminals.NORTH_SOUTH, new double[]{10.0});
        model.getCircuitComponents().addAll(List.of(v1, tr1, rSec));

        // TR1 NORTH_SOUTH at (10,10): prim pins at (9,8) and (9,12); sec pins at (11,8) and (11,12)
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{4, 8}, {9, 8}}));
        CircuitModel.ConnectionData gnd1 = new CircuitModel.ConnectionData("LK", new int[][]{{4, 12}, {9, 12}, {11, 12}, {16, 12}});
        gnd1.setLabel("GND");
        model.getConnections().add(gnd1);
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{11, 8}, {16, 8}}));

        // Ammeter coupled to TR1 by name, Voltmeter coupled to TR1 by UID
        CircuitModel.ComponentData ampTr = comp(1002, "AMP_TR", 20, 18, ComponentTerminals.NORTH_SOUTH, new double[0]);
        ampTr.setFamily("CONTROL");
        ampTr.setParameterStrings(new String[]{"TR1"});
        ampTr.setTerminalYLabels(new String[]{"I_TR1"});
        CircuitModel.ComponentData voltTr = comp(1001, "VOLT_TR", 20, 22, ComponentTerminals.NORTH_SOUTH, new double[0]);
        voltTr.setFamily("CONTROL");
        voltTr.setCoupledReferenceID(910L);
        voltTr.setTerminalYLabels(new String[]{"V_TR1"});
        model.getControlComponents().addAll(List.of(ampTr, voltTr));

        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(5e-5)
                .signals(List.of("I_TR1", "V_TR1"))
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), () -> "Transformer probe sim failed: " + result.getErrorMessage());
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.contains("DRC_PROBE_ERROR")),
                () -> "Coupled probes on TR1 must not emit DRC_PROBE_ERROR: " + result.getWarnings());
        double vTr = assertSignalFiniteAndGetLast(result.getDataContainer(), "V_TR1");
        double iTr = assertSignalFiniteAndGetLast(result.getDataContainer(), "I_TR1");
        assertEquals(20.0, Math.abs(vTr), 0.1, "Primary voltage on TR1 must be 20V");
        // Secondary voltage is 10V across 10 Ohm -> 1A secondary current -> 0.5A primary current
        assertEquals(0.5, Math.abs(iTr), 0.05, "Primary current on 2:1 TR1 with 1A secondary must be 0.5A");

        // Part 2: BJT Q1 with coupled Ammeter AMP_Q1
        // NORTH_SOUTH voltage source has nodeX (+) at y-2 and nodeY (-) at y+2
        CircuitModel bjtModel = new CircuitModel();
        CircuitModel.ComponentData vcc = comp(4, "V_cc", 20, 10, ComponentTerminals.NORTH_SOUTH, new double[]{401, 12.0});
        CircuitModel.ComponentData rc = comp(1, "R_c", 16, 6, ComponentTerminals.WEST_EAST, new double[]{100.0});
        CircuitModel.ComponentData vbb = comp(4, "V_bb", 2, 10, ComponentTerminals.NORTH_SOUTH, new double[]{401, 5.0});
        CircuitModel.ComponentData rb = comp(1, "R_b", 6, 10, ComponentTerminals.WEST_EAST, new double[]{1000.0});
        CircuitModel.ComponentData bjt = comp(33, "Q1", 12, 10, ComponentTerminals.NORTH_SOUTH, new double[]{0.0, 100.0, 60.0, 0.1, 1.0});
        bjt.setUniqueObjectIdentifier(920L);
        bjtModel.getCircuitComponents().addAll(List.of(vcc, rc, vbb, rb, bjt));

        bjtModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{20, 8}, {20, 6}, {18, 6}}));
        bjtModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{14, 6}, {12, 6}, {12, 8}}));
        bjtModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{2, 8}, {4, 8}, {4, 10}}));
        bjtModel.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{8, 10}, {10, 10}}));
        CircuitModel.ConnectionData gnd2 = new CircuitModel.ConnectionData("LK", new int[][]{{2, 12}, {12, 12}, {20, 12}});
        gnd2.setLabel("GND");
        bjtModel.getConnections().add(gnd2);

        CircuitModel.ComponentData ampBjt = comp(1002, "AMP_Q1", 20, 18, ComponentTerminals.NORTH_SOUTH, new double[0]);
        ampBjt.setFamily("CONTROL");
        ampBjt.setCoupledReferenceID(920L);
        ampBjt.setParameterStrings(new String[]{"Q1"});
        ampBjt.setTerminalYLabels(new String[]{"I_Q1"});
        bjtModel.getControlComponents().add(ampBjt);

        SimulationResult bjtResult = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                .circuitModel(bjtModel)
                .stepWidth(1e-5)
                .simulationDuration(5e-5)
                .signals(List.of("I_Q1"))
                .build());
        assertTrue(bjtResult.isSuccess(), () -> "BJT probe sim failed: " + bjtResult.getErrorMessage());
        assertTrue(bjtResult.getWarnings().stream().noneMatch(w -> w.contains("DRC_PROBE_ERROR")),
                () -> "Coupled Ammeter on Q1 must not emit DRC_PROBE_ERROR: " + bjtResult.getWarnings());
        double iQ1 = assertSignalFiniteAndGetLast(bjtResult.getDataContainer(), "I_Q1");
        assertTrue(Math.abs(iQ1) > 0.05, "Coupled Ammeter on BJT Q1 must measure non-zero collector current, got: " + iQ1);
    }

    private static double assertSignalFiniteAndGetLast(DataContainerGlobal container, String signal) {
        for (int row = 0; row < container.getRowLength(); row++) {
            if (signal.equals(container.getSignalName(row))) {
                double last = Double.NaN;
                for (int col = 0; col <= container.getMaximumTimeIndex(row); col++) {
                    double v = container.getValue(row, col);
                    int step = col;
                    assertTrue(Double.isFinite(v), () -> signal + " non-finite at step " + step);
                    last = v;
                }
                assertFalse(Double.isNaN(last), () -> signal + " had no samples");
                return last;
            }
        }
        fail("Signal '" + signal + "' not found in DataContainer");
        return Double.NaN;
    }
}
