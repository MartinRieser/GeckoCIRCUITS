package gecko.mcp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generates resources/projects/buck_sampled_50v_to_10v.ipes through the MCP builder and dumps
 * waveforms for the report. Tunables via -Dkp, -Dti, -DL, -DC (defaults = shipped design).
 */
public class BuckReportRunner {

    private static final String ARTIFACT_DIR = "C:/Users/mhr/.gemini/antigravity-ide/brain/aad4d96a-233a-4640-baea-ef6c16868c92";
    private static final String TARGET_IPES = "c:/Users/mhr/Documents/GeckoCIRCUITS/resources/projects/buck_sampled_50v_to_10v.ipes";

    private static double p(String k, double d) { return Double.parseDouble(System.getProperty(k, String.valueOf(d))); }

    private static Map<String, Object> blk(String name, String type, int x, int y, Map<String, Object> params, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name); m.put("type", type); m.put("x", x); m.put("y", y); m.put("orientation", 503);
        if (!params.isEmpty()) m.put("parameters", params);
        if (label != null) m.put("output_label", label);
        return m;
    }

    private static Map<String, Object> wire(String from, String to, int toInput, int[]... pts) {
        List<List<Integer>> l = new ArrayList<>();
        for (int[] q : pts) l.add(List.of(q[0], q[1]));
        return Map.of("from", from, "to", to, "to_input", toInput, "points", l);
    }

    private static Map<String, Object> net(String n, int[]... pts) {
        List<List<Integer>> l = new ArrayList<>();
        for (int[] q : pts) l.add(List.of(q[0], q[1]));
        return Map.of("net", n, "points", l);
    }

    private static Map<String, Object> comp(String n, String t, int x, int y, int o, List<String> nodes, Map<String, Object> par) {
        return Map.of("name", n, "type", t, "x", x, "y", y, "orientation", o, "nodes", nodes, "parameters", par);
    }

    static Map<String, Object> design(double kp, double ti, double L, double C, double duration) {
        List<Map<String, Object>> comps = List.of(
                comp("V_AC", "VOLTAGE_SOURCE_AC", 6, 6, 503, List.of("in_tot", "in_dc"),
                        Map.of("amplitude", 5.0, "frequency", 50.0, "phase", 0.0)),
                comp("V_DC", "VOLTAGE_SOURCE_DC", 6, 12, 503, List.of("in_dc", "0"), Map.of("voltage", 50.0)),
                comp("SW1", "IDEAL_SWITCH", 16, 4, 502, List.of("in_tot", "sw_node"), Map.of("r_on", 0.01, "r_off", 1e7)),
                comp("D1", "DIODE", 22, 11, 501, List.of("0", "sw_node"), Map.of("r_on", 0.01, "r_off", 1e7, "u_f", 0.7)),
                comp("L1", "INDUCTOR", 28, 4, 502, List.of("sw_node", "out"), Map.of("inductance", L)),
                comp("C1", "CAPACITOR", 36, 11, 503, List.of("out", "0"), Map.of("capacitance", C)),
                comp("R_LOAD", "RESISTOR", 44, 11, 503, List.of("out", "0"), Map.of("resistance", 5.0)),
                Map.of("name", "GND", "type", "GROUND", "x", 6, "y", 20, "orientation", 503, "nodes", List.of("0")));

        // power wiring: orthogonal, junctions are explicit vertices
        List<Map<String, Object>> power = List.of(
                net("in_dc", new int[]{6, 8}, new int[]{6, 10}),
                net("in_tot", new int[]{6, 4}, new int[]{14, 4}),
                net("sw_node", new int[]{18, 4}, new int[]{22, 4}, new int[]{26, 4}),
                net("sw_node", new int[]{22, 4}, new int[]{22, 9}),
                net("out", new int[]{30, 4}, new int[]{36, 4}, new int[]{44, 4}),
                net("out", new int[]{36, 4}, new int[]{36, 9}),
                net("out", new int[]{44, 4}, new int[]{44, 9}),
                net("0", new int[]{6, 14}, new int[]{6, 18}, new int[]{22, 18}, new int[]{36, 18}, new int[]{44, 18}),
                net("0", new int[]{22, 13}, new int[]{22, 18}),
                net("0", new int[]{36, 13}, new int[]{36, 18}),
                net("0", new int[]{44, 13}, new int[]{44, 18}));

        Map<String, Object> clk = Map.of("waveform", 404.0, "amplitude", 1.0, "frequency", 2000.0, "offset", 0.0, "duty", 0.1);

        List<Map<String, Object>> probes = List.of(
                Map.of("name", "VM_OUT", "type", "VOLTMETER", "x", 6, "y", 28, "orientation", 503,
                        "target_component", "R_LOAD", "signal_name", "v_out"),
                Map.of("name", "VM_IN", "type", "VOLTMETER", "x", 6, "y", 38, "orientation", 503,
                        "nodeA", "in_tot", "nodeB", "0", "signal_name", "v_in"),
                Map.of("name", "VM_SW", "type", "VOLTMETER", "x", 6, "y", 48, "orientation", 503,
                        "nodeA", "sw_node", "nodeB", "0", "signal_name", "v_sw"));

        List<Map<String, Object>> blocks = List.of(
                // --- row A (y=28): sampled feedback trim ---
                blk("SH_ADC", "SAMPLE_HOLD", 14, 28, Map.of(), null),
                blk("CLK_FB", "SIGNAL_SOURCE", 6, 33, clk, null),
                blk("V_REF", "CONSTANT", 18, 24, Map.of("value", 10.0), null),
                blk("SUB_ERR", "SUB", 26, 28, Map.of(), null),
                blk("PI_REG", "PI", 34, 28, Map.of("kp", kp, "ti", ti), null),
                blk("ADD_FF", "ADD", 44, 28, Map.of(), null),
                blk("LIMIT", "LIMIT", 52, 28, Map.of("min", 0.02, "max", 0.90), "duty"),
                blk("TRI", "SIGNAL_SOURCE", 54, 33, Map.of("waveform", 403.0, "amplitude", 0.5, "frequency", 20000.0, "offset", 0.5), null),
                blk("CMP", "COMPARATOR", 62, 28, Map.of(), null),
                // --- row B (y=38): sampled feed-forward D = Vref / Vin ---
                blk("SH_VIN", "SAMPLE_HOLD", 14, 38, Map.of(), null),
                blk("CLK_FF", "SIGNAL_SOURCE", 6, 43, clk, null),
                blk("V_REF_FF", "CONSTANT", 18, 34, Map.of("value", 10.0), null),
                blk("DIV_FF", "DIV", 26, 38, Map.of(), null),
                // display oscilloscope for switch-node PWM waveform
                blk("SCOPE", "SCOPE", 16, 48, Map.of(), null));

        List<Map<String, Object>> gates = List.of(
                Map.of("name", "GATE_SW1", "type", "GATE", "x", 70, "y", 28, "orientation", 503,
                        "target_switch", "SW1", "in_signal", "pwm_gate"));

        List<Map<String, Object>> wires = List.of(
                wire("VM_OUT", "SH_ADC", 0, new int[]{8, 28}, new int[]{12, 28}),
                wire("CLK_FB", "SH_ADC", 1, new int[]{8, 33}, new int[]{10, 33}, new int[]{10, 29}, new int[]{12, 29}),
                wire("V_REF", "SUB_ERR", 0, new int[]{20, 24}, new int[]{22, 24}, new int[]{22, 28}, new int[]{24, 28}),
                wire("SH_ADC", "SUB_ERR", 1, new int[]{16, 28}, new int[]{18, 28}, new int[]{18, 29}, new int[]{24, 29}),
                wire("SUB_ERR", "PI_REG", 0, new int[]{28, 28}, new int[]{32, 28}),
                wire("PI_REG", "ADD_FF", 0, new int[]{36, 28}, new int[]{42, 28}),
                wire("ADD_FF", "LIMIT", 0, new int[]{46, 28}, new int[]{50, 28}),
                wire("LIMIT", "CMP", 0, new int[]{54, 28}, new int[]{60, 28}),
                wire("TRI", "CMP", 1, new int[]{56, 33}, new int[]{56, 29}, new int[]{60, 29}),
                wire("CMP", "GATE_SW1", 0, new int[]{64, 28}, new int[]{68, 28}),
                // feed-forward row
                wire("VM_IN", "SH_VIN", 0, new int[]{8, 38}, new int[]{12, 38}),
                wire("CLK_FF", "SH_VIN", 1, new int[]{8, 43}, new int[]{10, 43}, new int[]{10, 39}, new int[]{12, 39}),
                wire("V_REF_FF", "DIV_FF", 0, new int[]{20, 34}, new int[]{22, 34}, new int[]{22, 38}, new int[]{24, 38}),
                wire("SH_VIN", "DIV_FF", 1, new int[]{16, 38}, new int[]{18, 38}, new int[]{18, 39}, new int[]{24, 39}),
                wire("DIV_FF", "ADD_FF", 1, new int[]{28, 38}, new int[]{40, 38}, new int[]{40, 29}, new int[]{42, 29}),
                wire("VM_SW", "SCOPE", 0, new int[]{8, 48}, new int[]{14, 48}));

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("output_path", TARGET_IPES);
        d.put("simulation", Map.of("duration", duration, "dt", 2e-6, "solver", 0));
        d.put("components", comps);
        d.put("power_connections", power);
        d.put("control", Map.of("probes", probes, "blocks", blocks, "gates", gates, "wires", wires));
        d.put("notes", List.of(Map.of("name", "INFO", "x", 78, "y", 40, "width", 34, "height", 12,
                "text", "## 50 V -> 10 V buck, sampled control\n- Input 50 V DC + 5 V / 50 Hz\n- Control sampled at 2 kHz (0.5 ms)\n- Feed-forward D = Vref/Vin + PI trim\n- 20 kHz triangle PWM, no script blocks")));
        return d;
    }

    @Test
    void generateAndSimulateBuckReport() throws Exception {
        double kp = p("kp", 0.01), ti = p("ti", 0.003), L = p("L", 1e-3), C = p("C", 100e-6);
        double dur = p("dur", 0.1);
        Path ipes = Paths.get(TARGET_IPES);
        Path art = Paths.get(ARTIFACT_DIR);

        assertEquals("CREATED", CircuitBuilder.create(design(kp, ti, L, C, dur)).get("status"));
        Map<String, Object> val = CircuitValidator.validate(ipes);
        assertTrue((Boolean) val.get("valid"), "DRC: " + val.get("diagnostics"));

        SchematicSvgRenderer.exportSvg(ipes, art.resolve("buck_schematic_light.svg"), "light", 1.0);
        SchematicSvgRenderer.exportSvg(ipes, art.resolve("buck_schematic_dark.svg"), "dark", 1.0);

        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(ipes, dur, 2e-6);
        List<Double> t = csv.columns().get("time"), vin = csv.columns().get("v_in"),
                vsw = csv.columns().get("v_sw"), vout = csv.columns().get("v_out"), duty = csv.columns().get("duty");
        assertNotNull(duty, "duty tap missing; have " + csv.columns().keySet());

        Path csvPath = art.resolve("scratch/buck_waveforms.csv");
        Files.createDirectories(csvPath.getParent());
        StringBuilder sb = new StringBuilder("time,v_in,v_sw,v_out,duty\n");
        double sum = 0, mn = 1e9, mx = -1e9; int n = 0;
        for (int i = 0; i < t.size(); i++) {
            sb.append(t.get(i)).append(',').append(vin.get(i)).append(',').append(vsw.get(i)).append(',')
              .append(vout.get(i)).append(',').append(duty.get(i)).append('\n');
            if (t.get(i) >= dur * 0.6) { double v = vout.get(i); sum += v; mn = Math.min(mn, v); mx = Math.max(mx, v); n++; }
        }
        Files.writeString(csvPath, sb.toString());
        System.out.printf("RESULTS kp=%.4f ti=%.4f L=%.2e C=%.2e : mean=%.4f V  pkpk=%.4f V (min %.3f max %.3f)%n",
                kp, ti, L, C, sum / n, mx - mn, mn, mx);
    }
}
