package gecko.mcp;

import java.util.*;

/**
 * Authoritative component definitions, schemas, and parameter slot mappings
 * for GeckoCIRCUITS power and control domains.
 *
 * <p>Enables external tools and LLMs to author, inspect, and validate circuits
 * using human-readable names without needing to know internal .ipes slot layouts.</p>
 */
public final class ComponentCatalog {

    public record ParameterDef(
            String name,
            String type, // "number", "string", "boolean"
            String unit,
            double defaultValue,
            int targetSlot,
            String description
    ) {}

    public record ComponentDef(
            String id,
            String displayName,
            String domain, // "POWER_LK" or "CONTROL"
            int typeNumber,
            String defaultPrefix,
            List<String> pins,
            List<ParameterDef> parameters,
            String description
    ) {}

    private static final Map<String, ComponentDef> REGISTRY = new LinkedHashMap<>();

    static {
        // ====================================================================
        // Power Domain (LK) Components
        // ====================================================================

        register(new ComponentDef(
                "RESISTOR", "Resistor", "POWER_LK", 1, "R",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("resistance", "number", "Ohm", 10.0, 0, "Resistance value in Ohms")
                ),
                "Linear electrical resistor"
        ));

        register(new ComponentDef(
                "INDUCTOR", "Inductor", "POWER_LK", 2, "L",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("inductance", "number", "H", 1e-3, 0, "Inductance in Henrys"),
                        new ParameterDef("i_init", "number", "A", 0.0, 1, "Initial current in Amperes")
                ),
                "Linear electrical inductor"
        ));

        register(new ComponentDef(
                "CAPACITOR", "Capacitor", "POWER_LK", 3, "C",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("capacitance", "number", "F", 1e-3, 0, "Capacitance in Farads"),
                        new ParameterDef("v_init", "number", "V", 0.0, 1, "Initial voltage across terminals in Volts")
                ),
                "Linear electrical capacitor. Slots 6 and 7 are automatically synchronized to capacitance for MNA companion stability."
        ));

        register(new ComponentDef(
                "VOLTAGE_SOURCE_DC", "DC Voltage Source", "POWER_LK", 4, "U_DC",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("voltage", "number", "V", 100.0, 1, "Constant DC voltage in Volts")
                ),
                "Ideal DC voltage source (positive terminal p is driven positive relative to negative terminal n)"
        ));

        register(new ComponentDef(
                "VOLTAGE_SOURCE_AC", "AC Sinusoidal Voltage Source", "POWER_LK", 4, "U_AC",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("amplitude", "number", "V", 325.269, 1, "Peak voltage amplitude in Volts (e.g. 325.27V for 230V RMS)"),
                        new ParameterDef("frequency", "number", "Hz", 50.0, 2, "Grid frequency in Hertz"),
                        new ParameterDef("offset", "number", "V", 0.0, 3, "DC offset in Volts"),
                        new ParameterDef("phase_deg", "number", "deg", 0.0, 4, "Phase angle in degrees (e.g. 0 for Phase A, 120 for Phase B, -120 for Phase C)")
                ),
                "Sinusoidal AC voltage source. Enforces v(p) - v(n) = amplitude * sin(2*pi*f*t - phase_deg) + offset."
        ));

        register(new ComponentDef(
                "CURRENT_SOURCE_DC", "DC Current Source", "POWER_LK", 5, "I_DC",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("current", "number", "A", 10.0, 1, "Constant DC current in Amperes")
                ),
                "Ideal DC current source"
        ));

        register(new ComponentDef(
                "DIODE", "Power Diode", "POWER_LK", 6, "D",
                List.of("anode", "cathode"),
                List.of(
                        new ParameterDef("u_forward", "number", "V", 0.7, 1, "Forward threshold voltage drop in Volts"),
                        new ParameterDef("r_on", "number", "Ohm", 0.005, 2, "Conducting ON-state resistance in Ohms"),
                        new ParameterDef("r_off", "number", "Ohm", 1e7, 3, "Blocking OFF-state resistance in Ohms")
                ),
                "Two-terminal semiconductor power diode. Conducts when v(anode) - v(cathode) >= u_forward."
        ));

        register(new ComponentDef(
                "IDEAL_SWITCH", "Ideal Switch", "POWER_LK", 7, "SW",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("r_on", "number", "Ohm", 0.005, 1, "Conducting ON-state resistance in Ohms"),
                        new ParameterDef("r_off", "number", "Ohm", 1e6, 2, "Blocking OFF-state resistance in Ohms"),
                        new ParameterDef("initial_state", "number", "", 0.0, 0, "Initial switch state: 0.0 = OFF, 1.0 = ON")
                ),
                "Purely bidirectional gate-controlled switch. Driven ON (r_on) when coupled GATE signal > 0.5, OFF (r_off) otherwise."
        ));

        register(new ComponentDef(
                "THYRISTOR", "Thyristor (SCR)", "POWER_LK", 8, "THYR",
                List.of("anode", "cathode"),
                List.of(
                        new ParameterDef("u_forward", "number", "V", 0.8, 1, "Forward threshold voltage drop in Volts"),
                        new ParameterDef("r_on", "number", "Ohm", 0.005, 2, "Conducting ON-state resistance in Ohms"),
                        new ParameterDef("r_off", "number", "Ohm", 1e7, 3, "Blocking OFF-state resistance in Ohms")
                ),
                "Line-commutated semiconductor switch. Latches ON when gate triggers and turns OFF at current zero-crossing."
        ));

        register(new ComponentDef(
                "IGBT", "IGBT with Anti-parallel Diode", "POWER_LK", 10, "IGBT",
                List.of("collector", "emitter"),
                List.of(
                        new ParameterDef("u_forward", "number", "V", 1.2, 1, "Collector-emitter forward on-voltage drop in Volts"),
                        new ParameterDef("r_on", "number", "Ohm", 0.005, 2, "Conducting ON-state resistance in Ohms"),
                        new ParameterDef("r_off", "number", "Ohm", 1e7, 3, "Blocking OFF-state resistance in Ohms")
                ),
                "Insulated-Gate Bipolar Transistor driven by a coupled GATE signal."
        ));

        // ====================================================================
        // Control Domain Components
        // ====================================================================

        register(new ComponentDef(
                "VOLTMETER", "Voltmeter Probe", "CONTROL", 1, "VOLT",
                List.of("out"),
                List.of(),
                "Voltage probe coupled to a power component. Outputs the component's branch voltage as a control signal."
        ));

        register(new ComponentDef(
                "AMMETER", "Ammeter Probe", "CONTROL", 2, "AMP",
                List.of("out"),
                List.of(),
                "Current probe coupled to a power component. Outputs the component's branch current as a control signal."
        ));

        register(new ComponentDef(
                "CONSTANT", "Constant Value", "CONTROL", 3, "CONST",
                List.of("out"),
                List.of(
                        new ParameterDef("value", "number", "", 1.0, 0, "Constant output value")
                ),
                "Outputs a constant numerical value."
        ));

        register(new ComponentDef(
                "SIGNAL_SOURCE", "Control Signal Source", "CONTROL", 4, "SIG",
                List.of("out"),
                List.of(
                        new ParameterDef("amplitude", "number", "", 1.0, 1, "Signal amplitude"),
                        new ParameterDef("frequency", "number", "Hz", 50.0, 2, "Signal frequency in Hz"),
                        new ParameterDef("offset", "number", "", 0.0, 3, "Signal DC offset")
                ),
                "Periodic signal generator for reference waveforms."
        ));

        register(new ComponentDef(
                "GATE", "Gate Drive Terminal", "CONTROL", 6, "GATE",
                List.of("in"),
                List.of(),
                "Gate driver coupled to a semiconductor switch (IDEAL_SWITCH, IGBT, MOSFET). Input signal > 0.5 turns the switch ON."
        ));

        register(new ComponentDef(
                "SCRIPT_BLOCK", "Microcontroller DSP Script Block", "CONTROL", 61, "CTRL_MCU",
                List.of("xIN", "yOUT"),
                List.of(),
                "High-speed interpreted control calculator (ScriptBlockCalculator). Supports Java-like syntax, persistent static variables, mathematical functions (sin, cos, abs, sqrt), arrays xIN[] and yOUT[], and simulation variables (t, dt, PI)."
        ));
    }

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("DC_VOLTAGE", "VOLTAGE_SOURCE_DC"),
            Map.entry("DC_SOURCE", "VOLTAGE_SOURCE_DC"),
            Map.entry("VOLTAGE_DC", "VOLTAGE_SOURCE_DC"),
            Map.entry("V_DC", "VOLTAGE_SOURCE_DC"),
            Map.entry("VOLTAGE_SOURCE", "VOLTAGE_SOURCE_DC"),
            Map.entry("AC_VOLTAGE", "VOLTAGE_SOURCE_AC"),
            Map.entry("AC_SOURCE", "VOLTAGE_SOURCE_AC"),
            Map.entry("VOLTAGE_AC", "VOLTAGE_SOURCE_AC"),
            Map.entry("V_AC", "VOLTAGE_SOURCE_AC"),
            Map.entry("DC_CURRENT", "CURRENT_SOURCE_DC"),
            Map.entry("CURRENT_SOURCE", "CURRENT_SOURCE_DC"),
            Map.entry("I_DC", "CURRENT_SOURCE_DC"),
            Map.entry("SWITCH", "IDEAL_SWITCH"),
            Map.entry("IDEALSWITCH", "IDEAL_SWITCH"),
            Map.entry("SCRIPT", "SCRIPT_BLOCK"),
            Map.entry("SCRIPTBLOCK", "SCRIPT_BLOCK"),
            Map.entry("JAVA_BLOCK", "SCRIPT_BLOCK"),
            Map.entry("CONST", "CONSTANT"),
            Map.entry("VOLT", "VOLTMETER"),
            Map.entry("AMM", "AMMETER")
    );

    private static void register(ComponentDef def) {
        REGISTRY.put(def.id().toUpperCase(Locale.ROOT), def);
    }

    public static ComponentDef get(String type) {
        if (type == null) return null;
        String key = type.trim().toUpperCase(Locale.ROOT);
        if (ALIASES.containsKey(key)) {
            key = ALIASES.get(key);
        }
        return REGISTRY.get(key);
    }

    public static Map<String, ComponentDef> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static Map<String, Object> toCatalogJson() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> powerList = new ArrayList<>();
        List<Map<String, Object>> controlList = new ArrayList<>();

        for (ComponentDef def : REGISTRY.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", def.id());
            entry.put("name", def.displayName());
            entry.put("type_number", def.typeNumber());
            entry.put("prefix", def.defaultPrefix());
            entry.put("pins", def.pins());
            entry.put("description", def.description());

            List<Map<String, Object>> params = new ArrayList<>();
            for (ParameterDef p : def.parameters()) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("name", p.name());
                pm.put("type", p.type());
                pm.put("unit", p.unit());
                pm.put("default", p.defaultValue());
                pm.put("description", p.description());
                params.add(pm);
            }
            entry.put("parameters", params);

            if ("POWER_LK".equals(def.domain())) {
                powerList.add(entry);
            } else {
                controlList.add(entry);
            }
        }

        result.put("power_components", powerList);
        result.put("control_components", controlList);
        return result;
    }
}
