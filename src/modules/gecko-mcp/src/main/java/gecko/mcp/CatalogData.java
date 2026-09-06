package gecko.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static component catalog — a direct port of the Python {@code gecko_catalog}
 * data. The LLM uses this to look up valid types and parameters.
 */
final class CatalogData {

    private CatalogData() {
    }

    private static Map<String, Object> component(int type, String name, String prefix,
                                                 List<String> parameters, String notes) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", type);
        entry.put("name", name);
        entry.put("prefix", prefix);
        entry.put("parameters", parameters);
        if (notes != null) {
            entry.put("notes", notes);
        }
        return entry;
    }

    static Map<String, Object> catalog() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("power_domain_lk", List.of(
                component(1, "Resistor", "R", List.of("resistance (Ohm)"), null),
                component(2, "Inductor", "L", List.of("inductance (H)", "initial current (A)"), null),
                component(3, "Capacitor", "C", List.of("capacitance (F)", "initial voltage (V)"), null),
                component(4, "VoltageSource", "U",
                        List.of("source_type", "amplitude (V)", "frequency (Hz)", "dc_offset (V)",
                                "phase (rad)"), null),
                component(6, "Diode", "D",
                        List.of("r_on (Ohm)", "forward_voltage (V)", "r_off (Ohm)"), null),
                component(7, "IdealSwitch", "S",
                        List.of("initial_state", "r_on (Ohm)", "r_off (Ohm)"),
                        "Controlled via GATE coupling"),
                component(10, "IGBT", "IGBT",
                        List.of("r_on (Ohm)", "forward_voltage (V)", "r_off (Ohm)"),
                        "Controlled via GATE coupling")));
        catalog.put("control_domain", List.of(
                component(1, "Voltmeter", "VOLT", List.of("coupledReferenceID"),
                        "Measures voltage across coupled power component"),
                component(2, "Ammeter", "AMM", List.of("coupledReferenceID"),
                        "Measures current through coupled power component"),
                component(3, "Constant", "CONST", List.of("value"), null),
                component(4, "SignalSource", "SIG",
                        List.of("waveform_type", "amplitude", "frequency", "offset", "phase", "duty"),
                        null),
                component(6, "Gate", "GATE", List.of("coupledReferenceID"),
                        "Drives switch with given uniqueObjectIdentifier"),
                component(61, "JavaBlock_CTRL_SCRIPT", "SCRIPT",
                        List.of("anzXIN", "anzYOUT", "sourceCode", "staticVariables", "staticCode"),
                        "Fast interpreted microcontroller block supporting state variables, "
                                + "PI loops, PWM generation, conditionals, and math functions")));
        return catalog;
    }
}
