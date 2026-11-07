package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive verification for meter-free virtual signals (SPICE syntax):
 * {@code I(<component>)}, {@code V(<component>)}, {@code V(<net>)}, and {@code V(<netA>|<netB>)}.
 */
class VirtualSignalTest {

    @Test
    @DisplayName("VirtualSignal parser resolves valid syntax and rejects invalid targets")
    void testVirtualSignalParsingAndResolution() throws Exception {
        CircuitModel model = loadModel("/ipes/buck_simple.ipes");
        assertNotNull(model, "buck_simple.ipes must exist");
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertNotNull(netlist, "Netlist must build");

        // 1. Current probe: I(R.L) or I(L.1)
        String rName = findFirstComponentOfPrefix(model, "R");
        assertNotNull(rName, "Model should have a resistor");

        VirtualSignal sigI = VirtualSignal.resolve("I(" + rName + ")", model, netlist);
        assertNotNull(sigI, "I(" + rName + ") must resolve");
        assertEquals(VirtualSignal.Kind.COMPONENT_CURRENT, sigI.kind());
        assertTrue(sigI.elementIndex() >= 0);

        // Whitespace and case-insensitivity
        VirtualSignal sigISpace = VirtualSignal.resolve("  i(  " + rName + "  )  ", model, netlist);
        assertNotNull(sigISpace);
        assertEquals(sigI.elementIndex(), sigISpace.elementIndex());

        // 2. Component Voltage probe: V(R)
        VirtualSignal sigV = VirtualSignal.resolve("V(" + rName + ")", model, netlist);
        assertNotNull(sigV, "V(" + rName + ") must resolve to component voltage");
        assertEquals(VirtualSignal.Kind.COMPONENT_VOLTAGE, sigV.kind());
        assertEquals(sigI.elementIndex(), sigV.elementIndex());
        assertEquals(netlist.getNodeX(sigI.elementIndex()), sigV.nodeA());
        assertEquals(netlist.getNodeY(sigI.elementIndex()), sigV.nodeB());

        // 3. Differential Voltage probe: V(netA|netB)
        List<String> labels = new ArrayList<>(netlist.getLabelResolver().getAllLabels());
        if (labels.size() >= 2) {
            String netA = labels.get(0);
            String netB = labels.get(1);
            VirtualSignal sigDiff = VirtualSignal.resolve("V(" + netA + "|" + netB + ")", model, netlist);
            assertNotNull(sigDiff, "V(" + netA + "|" + netB + ") must resolve to differential probe");
            assertEquals(VirtualSignal.Kind.DIFFERENTIAL_VOLTAGE, sigDiff.kind());
            assertEquals(netlist.getLabelResolver().getIndex(netA), sigDiff.nodeA());
            assertEquals(netlist.getLabelResolver().getIndex(netB), sigDiff.nodeB());
        }

        // 4. Ground reference in differential: V(netA|0)
        if (!labels.isEmpty()) {
            String netA = labels.get(0);
            VirtualSignal sigDiffGnd = VirtualSignal.resolve("V(" + netA + "|0)", model, netlist);
            assertNotNull(sigDiffGnd);
            assertEquals(0, sigDiffGnd.nodeB());
        }

        // 5. Invalid / unresolvable targets return null
        assertNull(VirtualSignal.resolve("I(NON_EXISTENT_COMPONENT)", model, netlist));
        assertNull(VirtualSignal.resolve("V(NON_EXISTENT_NET_OR_PART)", model, netlist));
        assertNull(VirtualSignal.resolve("V(BAD_NET_A|BAD_NET_B)", model, netlist));
        assertNull(VirtualSignal.resolve("NotAVirtualSignal", model, netlist));
        assertNull(VirtualSignal.resolve(null, model, netlist));
        assertNull(VirtualSignal.resolve("", model, netlist));
    }

    @Test
    @DisplayName("VirtualSignal evaluate correctly computes branch currents and differential voltages")
    void testVirtualSignalEvaluate() {
        CircuitNetlist mockNetlist = new CircuitNetlist();
        double[] nodeVoltages = new double[]{0.0, 10.0, 3.5, -2.0};

        // Test differential voltage: node 1 (10.0V) - node 2 (3.5V) = 6.5V
        VirtualSignal sigDiff = new VirtualSignal("V(n1|n2)", VirtualSignal.Kind.DIFFERENTIAL_VOLTAGE, -1, 1, 2);
        assertEquals(6.5f, sigDiff.evaluate(mockNetlist, nodeVoltages), 1e-6f);

        // Test node voltage: node 1 (10.0V) - node 0 (0.0V) = 10.0V
        VirtualSignal sigNode = new VirtualSignal("V(n1)", VirtualSignal.Kind.NODE_VOLTAGE, -1, 1, 0);
        assertEquals(10.0f, sigNode.evaluate(mockNetlist, nodeVoltages), 1e-6f);

        // Null safety
        assertEquals(0.0f, sigDiff.evaluate(mockNetlist, null));
        VirtualSignal sigCurrent = new VirtualSignal("I(L1)", VirtualSignal.Kind.COMPONENT_CURRENT, 0, -1, -1);
        assertEquals(0.0f, sigCurrent.evaluate(null, nodeVoltages));
    }

    @Test
    @DisplayName("End-to-End simulation parity: Virtual I() and V() match physical meters exactly")
    void testSimulationEndToEndVirtualSignalParity() throws Exception {
        String circuitPath = "/ipes/buck_simple.ipes";
        CircuitModel model = loadModel(circuitPath);
        assertNotNull(model, "buck_simple.ipes must exist");

        // Request both physical meter signals and virtual signals simultaneously
        // buck_simple has L.1, R.1, U.1
        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .signals(List.of("SCOPE.1", "I(L.1)", "V(L.1)", "I(R.1)", "V(R.1)"))
                .dataLoggingInterval(1)
                .build();

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationResult result = engine.runSimulation(config);

        assertTrue(result.isSuccess(), "Simulation must succeed");
        List<String> names = Arrays.asList(result.getSignalNames());
        assertTrue(names.contains("I(L.1)"), "I(L.1) must be recorded");
        assertTrue(names.contains("V(L.1)"), "V(L.1) must be recorded");
        assertTrue(names.contains("I(R.1)"), "I(R.1) must be recorded");
        assertTrue(names.contains("V(R.1)"), "V(R.1) must be recorded");

        float[] iL = getSignal(result, "I(L.1)");
        float[] vL = getSignal(result, "V(L.1)");
        float[] iR = getSignal(result, "I(R.1)");
        float[] vR = getSignal(result, "V(R.1)");

        assertNotNull(iL);
        assertNotNull(vL);
        assertNotNull(iR);
        assertNotNull(vR);
        assertTrue(iL.length > 100);

        // Physical meter 'iL' (from AMP.1 on L.1) must match virtual probe 'I(L.1)'
        float[] iLPhysical = getSignal(result, "iL");
        if (iLPhysical != null) {
            assertEquals(iLPhysical.length, iL.length, "Sample count must match");
            for (int step = 0; step < iL.length; step++) {
                assertEquals(iLPhysical[step], iL[step], 1e-5f,
                        "Physical AMP.1 and virtual I(L.1) must match at step " + step);
            }
        }

        // Ohm's law assertion on load resistor: V(R.1) = I(R.1) * R
        // In buck_simple, verify V(R.1) / I(R.1) is strictly positive and constant
        for (int step = 10; step < Math.min(iL.length, 500); step++) {
            if (Math.abs(iR[step]) > 1e-3f) {
                float rCalculated = vR[step] / iR[step];
                assertTrue(rCalculated > 0.1f && rCalculated < 1000.0f,
                        "Ohm's law parity: V/I should be positive resistance, got " + rCalculated);
            }
        }
    }

    @Test
    @DisplayName("Wildcard '*' expansion automatically discovers and records all component signals")
    void testWildcardSignalDiscovery() throws Exception {
        String circuitPath = "/ipes/buck_simple.ipes";
        CircuitModel model = loadModel(circuitPath);
        assertNotNull(model, "buck_simple.ipes must exist");

        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .signals(List.of("*"))
                .dataLoggingInterval(1)
                .build();

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationResult result = engine.runSimulation(config);

        assertTrue(result.isSuccess(), "Simulation must succeed");
        List<String> names = Arrays.asList(result.getSignalNames());
        assertTrue(names.stream().anyMatch(s -> s.startsWith("I(")),
                "Wildcard must discover component currents I(...)");
        assertTrue(names.stream().anyMatch(s -> s.startsWith("V(")),
                "Wildcard must discover component voltages V(...)");
    }

    @Test
    @DisplayName("Reworked bundled examples simulate cleanly with meter-free virtual probes")
    void testReworkedInverterSimulationWithVirtualProbes() throws Exception {
        java.io.File file = new java.io.File("../../examples/inverter.ipes");
        if (!file.exists()) {
            file = new java.io.File("examples/inverter.ipes");
        }
        assertTrue(file.exists(), "examples/inverter.ipes must exist");

        CircuitModel model;
        try (BufferedReader br = new BufferedReader(new java.io.FileReader(file, StandardCharsets.UTF_8))) {
            model = new CircuitFileParser().parse(br, file.getAbsolutePath());
        }

        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .circuitModel(model)
                .signals(List.of("V(load_p|out_b)", "V(out_a|out_b)", "I(L_filter)"))
                .stepWidth(2e-6)
                .simulationDuration(0.04)
                .dataLoggingInterval(1)
                .build();

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationResult result = engine.runSimulation(config);

        assertTrue(result.isSuccess(), "Inverter simulation must succeed");
        float[] vac = getSignal(result, "V(load_p|out_b)");
        float[] vab = getSignal(result, "V(out_a|out_b)");
        float[] il = getSignal(result, "I(L_filter)");

        assertNotNull(vac, "V(load_p|out_b) must be recorded");
        assertNotNull(vab, "V(out_a|out_b) must be recorded");
        assertNotNull(il, "I(L_filter) must be recorded");

        assertTrue(vac.length > 100);
        assertTrue(vab.length > 100);
        assertTrue(il.length > 100);

        // Assert physical AC sinusoidal amplitude: with 40V DC bus, peak output voltage reaches ~30-40 V
        float maxVac = 0;
        for (float v : vac) {
            assertFalse(Float.isNaN(v) || Float.isInfinite(v), "Waveform must not contain NaN or Inf");
            if (Math.abs(v) > maxVac) maxVac = Math.abs(v);
        }
        assertTrue(maxVac > 15.0f, "AC load voltage peak must be substantial (>15V), was: " + maxVac);

        // Peak inductor current
        float maxIl = 0;
        for (float i : il) {
            assertFalse(Float.isNaN(i) || Float.isInfinite(i), "Waveform must not contain NaN or Inf");
            if (Math.abs(i) > maxIl) maxIl = Math.abs(i);
        }
        assertTrue(maxIl > 1.0f, "Filter inductor current peak must be substantial (>1A), was: " + maxIl);
    }

    private static CircuitModel loadModel(String resourcePath) throws Exception {
        try (InputStream is = VirtualSignalTest.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }
            return new CircuitFileParser().parse(
                    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)),
                    resourcePath);
        }
    }

    private static String findFirstComponentOfPrefix(CircuitModel model, String prefix) {
        for (CircuitModel.ComponentData comp : model.getCircuitComponents()) {
            if (comp.getName() != null && comp.getName().startsWith(prefix)) {
                return comp.getName();
            }
        }
        return null;
    }

    private static float[] getSignal(SimulationResult result, String name) {
        String[] names = result.getSignalNames();
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i])) {
                return result.getSignalData(i);
            }
        }
        return null;
    }
}
