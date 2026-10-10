package gecko.mcp;

import java.util.*;

/**
 * Authoritative component definitions, schemas, and parameter slot mappings
 * for the GeckoCIRCUITS power, control, and thermal domains.
 *
 * <p>Enables external tools and LLMs to author, inspect, and validate circuits
 * using human-readable names without needing to know internal .ipes slot
 * layouts. The control-block entries use the web catalog type numbers
 * (CircuitTypCore CTRL_* range, 1000..1016) which the headless engine executes
 * natively; the classic editor numbers (1..6) are listed as legacy aliases.
 * Slot layouts mirror the typed parameter classes in
 * {@code gecko.core.circuit.parameters} and are pinned by
 * CatalogSlotCrossCheckTest.</p>
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
            String domain, // "POWER_LK", "CONTROL" or "THERM"
            int typeNumber,
            String defaultPrefix,
            List<String> pins,
            List<ParameterDef> parameters,
            String description,
            /** Number of pins on the X (input / labelAnfangsKnoten) side;
             *  the remaining pins go to the Y (output / labelEndKnoten) side. */
            int xPinCount,
            /** Whether the block participates in headless simulations
             *  (scopes are display-only and are skipped silently). */
            boolean headlessExecutable
    ) {
        /** Two-sided convenience used by most components: first pin on the X
         *  side, the remaining pins on the Y side. */
        public ComponentDef(
                String id, String displayName, String domain, int typeNumber, String defaultPrefix,
                List<String> pins, List<ParameterDef> parameters, String description) {
            this(id, displayName, domain, typeNumber, defaultPrefix, pins, parameters, description,
                    Math.min(1, pins.size()), true);
        }

        /** Multi-pin convenience: explicit X-side pin count, executable by default. */
        public ComponentDef(
                String id, String displayName, String domain, int typeNumber, String defaultPrefix,
                List<String> pins, List<ParameterDef> parameters, String description,
                int xPinCount) {
            this(id, displayName, domain, typeNumber, defaultPrefix, pins, parameters, description,
                    xPinCount, true);
        }
    }

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
                "Linear electrical inductor. Can receive a differential inductance from a magnetic domain winding bound by name."
        ));

        register(new ComponentDef(
                "CAPACITOR", "Capacitor", "POWER_LK", 3, "C",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("capacitance", "number", "F", 1e-6, 0, "Capacitance in Farads"),
                        new ParameterDef("v_init", "number", "V", 0.0, 1, "Initial voltage across terminals in Volts")
                ),
                "Linear electrical capacitor. Slots 6 and 7 are automatically synchronized to capacitance for MNA companion stability."
        ));

        register(new ComponentDef(
                "VOLTAGE_SOURCE_DC", "DC Voltage Source", "POWER_LK", 4, "U_DC",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("source_type", "number", "", 401.0, 0, "Source waveform code; 401 = DC (fixed for this component)"),
                        new ParameterDef("voltage", "number", "V", 100.0, 1, "Constant DC voltage in Volts")
                ),
                "Ideal DC voltage source (positive terminal p is driven positive relative to negative terminal n)"
        ));

        register(new ComponentDef(
                "VOLTAGE_SOURCE_AC", "AC Sinusoidal Voltage Source", "POWER_LK", 4, "U_AC",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("source_type", "number", "", 402.0, 0, "Source waveform code; 402 = sinusoidal (fixed for this component)"),
                        new ParameterDef("amplitude", "number", "V", 325.269, 1, "Peak voltage amplitude in Volts (e.g. 325.27V for 230V RMS); also mirrored to slot 20"),
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
                        new ParameterDef("source_type", "number", "", 401.0, 0, "Source waveform code; 401 = DC (fixed for this component)"),
                        new ParameterDef("current", "number", "A", 10.0, 1, "Constant DC current in Amperes")
                ),
                "Ideal DC current source driving current from n through itself into p"
        ));

        register(new ComponentDef(
                "CURRENT_SOURCE_AC", "AC Sinusoidal Current Source", "POWER_LK", 5, "I_AC",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("source_type", "number", "", 402.0, 0, "Source waveform code; 402 = sinusoidal (fixed for this component)"),
                        new ParameterDef("amplitude", "number", "A", 10.0, 1, "Peak current amplitude in Amperes; also mirrored to slot 20"),
                        new ParameterDef("frequency", "number", "Hz", 50.0, 2, "Frequency in Hertz"),
                        new ParameterDef("offset", "number", "A", 0.0, 3, "DC offset in Amperes"),
                        new ParameterDef("phase_deg", "number", "deg", 0.0, 4, "Phase angle in degrees")
                ),
                "Sinusoidal AC current source"
        ));

        register(new ComponentDef(
                "DIODE", "Power Diode", "POWER_LK", 6, "D",
                List.of("anode", "cathode"),
                List.of(
                        new ParameterDef("u_forward", "number", "V", 0.7, 1, "Forward threshold voltage drop in Volts"),
                        new ParameterDef("r_on", "number", "Ohm", 0.005, 2, "Conducting ON-state resistance in Ohms"),
                        new ParameterDef("r_off", "number", "Ohm", 1e7, 3, "Blocking OFF-state resistance in Ohms")
                ),
                "Two-terminal semiconductor power diode. Conducts when v(anode) - v(cathode) >= u_forward. Slot 0 holds the state machine's dynamic resistance."
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
                        new ParameterDef("r_off", "number", "Ohm", 1e7, 3, "Blocking OFF-state resistance in Ohms"),
                        new ParameterDef("turn_off_delay", "number", "s", 0.0, 9, "Turn-off recovery delay in seconds")
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

        register(new ComponentDef(
                "MOSFET", "Power MOSFET", "POWER_LK", 28, "MOSFET",
                List.of("drain", "source"),
                List.of(
                        new ParameterDef("r_on", "number", "Ohm", 0.005, 2, "Conducting ON-state resistance in Ohms"),
                        new ParameterDef("r_off", "number", "Ohm", 1e7, 3, "Blocking OFF-state resistance in Ohms")
                ),
                "Power MOSFET driven by a coupled GATE signal (on/off resistance slots 2/3, gate slot 8)."
        ));

        register(new ComponentDef(
                "TRANSFORMER", "Ideal Transformer", "POWER_LK", 23, "TR",
                List.of("p1", "p2", "s1", "s2"),
                List.of(
                        new ParameterDef("n1", "number", "", 10.0, 0, "Primary turns count N1"),
                        new ParameterDef("n2", "number", "", 2.0, 1, "Secondary turns count N2"),
                        new ParameterDef("polarity", "number", "", -1.0, 2, "Winding polarity sign (+1 or -1)")
                ),
                "Ideal transformer with 4 terminals: primary pair (p1/p2) and secondary pair (s1/s2), ratio N1:N2.",
                2
        ));

        register(new ComponentDef(
                "BJT", "Bipolar Junction Transistor", "POWER_LK", 33, "BJT",
                List.of("collector", "base", "emitter"),
                List.of(
                        new ParameterDef("beta_f", "number", "", 100.0, 1, "Forward current gain beta_F"),
                        new ParameterDef("beta_b", "number", "", 60.0, 2, "Reverse current gain beta_B"),
                        new ParameterDef("r_base", "number", "Ohm", 0.1, 3, "Base spreading resistance in Ohms"),
                        new ParameterDef("polarity", "number", "", 1.0, 4, "1.0 = NPN, -1.0 = PNP")
                ),
                "Three-terminal bipolar junction transistor (collector and base on the input side, emitter on the output side).",
                2
        ));

        register(new ComponentDef(
                "PMSM_MOTOR", "Permanent Magnet Synchronous Motor", "POWER_LK", 15, "PMSM",
                List.of("uA", "uB", "uC"),
                List.of(),
                "Three-terminal permanent magnet synchronous motor (all pins on the input side). "
                        + "Ships with a typical 10 kW machine preset (from resources/articles/ipes_files/"
                        + "dq_control_pmsm.ipes); override any slot with 'parameters_raw' copied from a "
                        + "reference machine datasheet.",
                3
        ));

        // ====================================================================
        // Control Domain Components (web catalog type numbers; executed natively
        // by the headless engine)
        // ====================================================================

        register(new ComponentDef(
                "GATE", "Gate Drive Terminal", "CONTROL", 1000, "GATE",
                List.of("in"),
                List.of(),
                "Gate driver coupled to a semiconductor switch (IDEAL_SWITCH, IGBT, MOSFET, THYRISTOR) "
                        + "via target name or coupledReferenceID. Input signal > 0.5 turns the switch ON. "
                        + "Legacy editor type number: 6."
        ));

        register(new ComponentDef(
                "VOLTMETER", "Voltmeter Probe", "CONTROL", 1001, "VOLT",
                List.of("out"),
                List.of(),
                "Voltage probe coupled to a power component (branch voltage) or to a node label pair "
                        + "(nodeA/nodeB). Outputs the measured voltage as a control signal. "
                        + "Legacy editor type number: 1."
        ));

        register(new ComponentDef(
                "AMMETER", "Ammeter Probe", "CONTROL", 1002, "AMP",
                List.of("out"),
                List.of(),
                "Current probe coupled to a power component. Outputs the component's branch current as a "
                        + "control signal. Legacy editor type number: 2."
        ));

        register(new ComponentDef(
                "SCOPE", "Oscilloscope", "CONTROL", 1003, "SCOPE",
                List.of("in1"),
                List.of(),
                "Display-only oscilloscope. Skipped silently in headless simulations - use probe output "
                        + "labels or the signals list to record channels instead. Legacy editor type number: 5.",
                1, false
        ));

        register(new ComponentDef(
                "SIGNAL_SOURCE", "Control Signal Source", "CONTROL", 1004, "SIG",
                List.of("out"),
                List.of(
                        new ParameterDef("waveform", "number", "", 402.0, 0, "Waveform code: 402 = sinusoidal, 403 = triangle, 404 = rectangle, 405 = random"),
                        new ParameterDef("amplitude", "number", "", 1.0, 1, "Signal amplitude"),
                        new ParameterDef("frequency", "number", "Hz", 50.0, 2, "Signal frequency in Hz"),
                        new ParameterDef("offset", "number", "", 0.0, 3, "Signal DC offset"),
                        new ParameterDef("phase_rad", "number", "rad", 0.0, 4, "Initial phase angle in radians (control sources use radians, LK sources degrees)"),
                        new ParameterDef("duty", "number", "", 0.5, 5, "Pulse duty ratio (0..1) for rectangle PWM signals")
                ),
                "Periodic signal generator for references and PWM carriers."
        ));

        register(new ComponentDef(
                "CONSTANT", "Constant Value", "CONTROL", 1005, "CONST",
                List.of("out"),
                List.of(
                        new ParameterDef("value", "number", "", 1.0, 0, "Constant output value")
                ),
                "Outputs a constant numerical value. Legacy editor type number: 3."
        ));

        register(new ComponentDef(
                "GAIN", "Gain", "CONTROL", 1006, "GAIN",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("k", "number", "", 1.0, 0, "Multiplication factor")
                ),
                "Multiplies the input signal by a constant factor k: y = k * u."
        ));

        register(new ComponentDef(
                "PI", "PI Controller", "CONTROL", 1007, "PI",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("kp", "number", "", 1.0, 0, "Proportional gain Kp"),
                        new ParameterDef("ti", "number", "s", 0.01, 1, "Integration time Ti in seconds; series form y = Kp*(u + (1/Ti)*integral(u)dt). Non-positive Ti degrades to P-only.")
                ),
                "PI controller in series (reset-time) form with trapezoidal integration."
        ));

        register(new ComponentDef(
                "PT1", "PT1 Low-Pass", "CONTROL", 1008, "PT1",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("tau", "number", "s", 0.001, 0, "Filter time constant in seconds")
                ),
                "First-order low-pass filter with unity DC gain: G(s) = 1/(1 + tau*s)."
        ));

        register(new ComponentDef(
                "INTEGRATOR", "Integrator", "CONTROL", 1009, "INT",
                List.of("in", "reset", "out"),
                List.of(
                        new ParameterDef("initial_value", "number", "", 0.0, 0, "Initial integrator state")
                ),
                "Integrates the input (G(s) = 1/s) starting from the initial value. The second input is an "
                        + "optional reset: a value >= 1 resets the state to the initial value (unwired = no reset).",
                2
        ));

        register(new ComponentDef(
                "COMPARATOR", "Comparator", "CONTROL", 1010, "CMP",
                List.of("plus", "minus", "out"),
                List.of(),
                "Compares two signals: output = 1 while plus > minus, else 0. The canonical PWM modulator "
                        + "is COMPARATOR(control voltage vs triangle carrier) feeding a GATE.",
                2
        ));

        register(new ComponentDef(
                "AND", "AND Gate", "CONTROL", 1011, "AND",
                List.of("a", "b", "out"),
                List.of(),
                "Logical AND: output = 1 only if BOTH inputs exceed 0.5.",
                2
        ));

        register(new ComponentDef(
                "OR", "OR Gate", "CONTROL", 1012, "OR",
                List.of("a", "b", "out"),
                List.of(),
                "Logical OR: output = 1 if ANY input exceeds 0.5.",
                2
        ));

        register(new ComponentDef(
                "NOT", "NOT Gate", "CONTROL", 1013, "NOT",
                List.of("in", "out"),
                List.of(),
                "Logical inverter: output = 1 while the input is at or below 0.5."
        ));

        register(new ComponentDef(
                "SELECTOR", "Selector Multiplexer", "CONTROL", 1014, "MUX",
                List.of("sel", "in0", "in1", "out"),
                List.of(),
                "Passes in0 to the output while the selector is at or below 0.5, in1 while it is above.",
                3
        ));

        register(new ComponentDef(
                "DELAY", "Time Delay", "CONTROL", 1015, "DELAY",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("tau", "number", "s", 0.001, 0, "Transport delay time in seconds")
                ),
                "Delays the input signal by tau seconds (circular buffer, zero output during buffer fill)."
        ));

        register(new ComponentDef(
                "SCRIPT_BLOCK", "Microcontroller DSP Script Block", "CONTROL", 1016, "CTRL_MCU",
                List.of("xIN", "yOUT"),
                List.of(),
                "High-speed interpreted control calculator (ScriptBlockCalculator). Supports Java-like "
                        + "syntax, persistent static variables, mathematical functions (sin, cos, abs, sqrt), "
                        + "arrays xIN[] and yOUT[], and simulation variables (t, dt, PI). Configure with "
                        + "anzXIN/anzYOUT input/output counts; see the script-blocks reference resource."
        ));

        register(new ComponentDef(
                "JAVA_BLOCK", "Classic Java Function Block", "CONTROL", 61, "JAVA",
                List.of("xIN", "yOUT"),
                List.of(),
                "Classic GeckoCIRCUITS Java programmable function block; same script language as "
                        + "SCRIPT_BLOCK. Kept for compatibility with classic projects."
        ));

        register(new ComponentDef(
                "NATIVE_C", "C Library Block (NativeC)", "CONTROL", 88, "CNATC",
                List.of("xIN", "yOUT"),
                List.of(),
                "Firmware-in-the-loop block binding a self-built host shared library (dll/so/dylib) "
                        + "implementing the gecko_c_block.h contract (gecko_init/gecko_step/gecko_deinit). "
                        + "Configure with libraryPath plus anzXIN/anzYOUT.",
                1, true
        ));

        register(new ComponentDef(
                "ASIN", "Arc Sine", "CONTROL", 1038, "ASIN",
                List.of("in", "out"),
                List.of(),
                "Trigonometric arc sine: y = asin(u) in radians."
        ));

        register(new ComponentDef(
                "ACOS", "Arc Cosine", "CONTROL", 1039, "ACOS",
                List.of("in", "out"),
                List.of(),
                "Trigonometric arc cosine: y = acos(u) in radians."
        ));

        register(new ComponentDef(
                "TAN", "Tangent", "CONTROL", 1040, "TAN",
                List.of("in", "out"),
                List.of(),
                "Trigonometric tangent: y = tan(u)."
        ));

        register(new ComponentDef(
                "ATAN", "Arc Tangent", "CONTROL", 1041, "ATAN",
                List.of("in", "out"),
                List.of(),
                "Trigonometric arc tangent: y = atan(u) in radians."
        ));

        register(new ComponentDef(
                "SQR", "Square", "CONTROL", 1042, "SQR",
                List.of("in", "out"),
                List.of(),
                "Square of input signal: y = u^2."
        ));

        register(new ComponentDef(
                "POW", "Power", "CONTROL", 1043, "POW",
                List.of("in0", "in1", "out"),
                List.of(),
                "Power of input signals: y = (in0)^(in1).",
                2
        ));

        register(new ComponentDef(
                "ROUND", "Round", "CONTROL", 1044, "ROUND",
                List.of("in", "out"),
                List.of(),
                "Rounds input signal to nearest integer: y = round(u)."
        ));

        register(new ComponentDef(
                "SIGN", "Signum", "CONTROL", 1045, "SIGN",
                List.of("in", "out"),
                List.of(),
                "Signum function: returns 1 if u > 0, -1 if u < 0, 0 if u == 0."
        ));

        register(new ComponentDef(
                "EQ", "Equal", "CONTROL", 1046, "EQ",
                List.of("in0", "in1", "out"),
                List.of(),
                "Equality comparator: y = 1 if in0 == in1 else 0.",
                2
        ));

        register(new ComponentDef(
                "NE", "Not Equal", "CONTROL", 1047, "NE",
                List.of("in0", "in1", "out"),
                List.of(),
                "Inequality comparator: y = 1 if in0 != in1 else 0.",
                2
        ));

        register(new ComponentDef(
                "COUNTER", "Counter", "CONTROL", 1048, "COUNTER",
                List.of("in", "reset", "out"),
                List.of(),
                "Pulse counter: increments on rising edges, reset resets count to zero.",
                2
        ));

        register(new ComponentDef(
                "ABCDQ", "abc to dq Transformation", "CONTROL", 1049, "ABCDQ",
                List.of("a", "b", "c", "theta", "d", "q", "zero"),
                List.of(),
                "Park/Clarke abc to dq0 coordinate transformation.",
                4
        ));

        register(new ComponentDef(
                "DQABC", "dq to abc Transformation", "CONTROL", 1050, "DQABC",
                List.of("d", "q", "zero", "theta", "a", "b", "c"),
                List.of(),
                "Inverse Park/Clarke dq0 to abc coordinate transformation.",
                4
        ));

        register(new ComponentDef(
                "THYR_CTRL", "Thyristor Control", "CONTROL", 1051, "THYR_CTRL",
                List.of("uSync", "alpha", "gate1", "gate2", "gate3", "gate4", "gate5", "gate6"),
                List.of(),
                "Firing angle generator for line-commutated thyristor bridges.",
                2
        ));

        register(new ComponentDef(
                "PMSM_CONTROL", "PMSM Control", "CONTROL", 1052, "PMSM_CTRL",
                List.of("idRef", "iqRef", "omega", "theta", "vd", "vq"),
                List.of(),
                "Field-oriented current and speed controller for PMSM machines.",
                4
        ));

        register(new ComponentDef(
                "PMSM_MODULATOR", "PMSM Modulator", "CONTROL", 1053, "PMSM_MOD",
                List.of("vd", "vq", "theta", "sA", "sB", "sC"),
                List.of(),
                "Modulator / space-vector transform for PMSM machines.",
                3
        ));

        register(new ComponentDef(
                "DEMUX", "Demultiplexer / Vector Splitter", "CONTROL", 1054, "DEMUX",
                List.of("in", "out0", "out1"),
                List.of(),
                "Splits a vector control signal into scalar components.",
                1
        ));

        register(new ComponentDef(
                "SPACE_VECTOR", "Space Vector Modulator", "CONTROL", 1055, "SVM",
                List.of("valpha", "vbeta", "sA", "sB", "sC"),
                List.of(),
                "Space vector PWM modulator.",
                2
        ));

        register(new ComponentDef(
                "SDFT", "Sliding DFT", "CONTROL", 1056, "SDFT",
                List.of("in", "mag"),
                List.of(
                        new ParameterDef("frequency", "number", "Hz", 50.0, 0, "Fundamental tracking frequency in Hertz")
                ),
                "Sliding Discrete Fourier Transform for real-time fundamental harmonic tracking."
        ));

        // ====================================================================
        // Thermal Domain Components (classic .ipes heat network)
        // ====================================================================

        register(new ComponentDef(
                "TH_RTH", "Thermal Resistance", "THERM", 46, "RTH",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("r_th", "number", "K/W", 1.0, 0, "Thermal resistance in Kelvin per Watt")
                ),
                "Thermal resistance between two thermal nodes (heat-flow analogy of a resistor: 1 K/W = 1 Ohm)."
        ));

        register(new ComponentDef(
                "TH_CTH", "Thermal Capacitance", "THERM", 47, "CTH",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("c_th", "number", "J/K", 1.0, 0, "Thermal capacitance in Joule per Kelvin"),
                        new ParameterDef("t_init", "number", "degC", 25.0, 1, "Initial node temperature in degrees Celsius")
                ),
                "Thermal capacitance (heat capacity) of a node mass. In Cauer networks connect from the node to the ambient reference."
        ));

        register(new ComponentDef(
                "TH_FLOW", "Heat Flow Source", "THERM", 44, "P",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("source_type", "number", "", 401.0, 0, "Source waveform code; 401 = DC (constant heat flow)"),
                        new ParameterDef("power", "number", "W", 10.0, 1, "Dissipated heat power in Watts")
                ),
                "Constant heat-flow source injecting dissipated power (e.g. semiconductor losses) into a thermal node."
        ));

        register(new ComponentDef(
                "TH_TEMP", "Temperature Source", "THERM", 45, "T",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("source_type", "number", "", 401.0, 0, "Source waveform code; 401 = DC (constant temperature)"),
                        new ParameterDef("temperature", "number", "degC", 25.0, 1, "Boundary temperature in degrees Celsius")
                ),
                "Ideal temperature boundary source (heat-flow analogy of a voltage source)."
        ));

        register(new ComponentDef(
                "TH_AMBIENT", "Ambient Zero Reference", "THERM", 48, "TREF",
                List.of("p", "n"),
                List.of(),
                "Passive ambient zero-reference block (classic TREF): both pins belong to one net which "
                        + "acts as the 0-degree reference of the thermal network, like a ground symbol.",
                1, true
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
            Map.entry("AC_CURRENT", "CURRENT_SOURCE_AC"),
            Map.entry("I_AC", "CURRENT_SOURCE_AC"),
            Map.entry("SWITCH", "IDEAL_SWITCH"),
            Map.entry("IDEALSWITCH", "IDEAL_SWITCH"),
            Map.entry("MOSFET_TRANSISTOR", "MOSFET"),
            Map.entry("TRANSFORMER_IDEAL", "TRANSFORMER"),
            Map.entry("SCRIPT", "SCRIPT_BLOCK"),
            Map.entry("SCRIPTBLOCK", "SCRIPT_BLOCK"),
            Map.entry("FUNC", "SCRIPT_BLOCK"),
            Map.entry("CONST", "CONSTANT"),
            Map.entry("K", "CONSTANT"),
            Map.entry("VOLT", "VOLTMETER"),
            Map.entry("AMM", "AMMETER"),
            Map.entry("PI_CTRL", "PI"),
            Map.entry("PT1_FILTER", "PT1"),
            Map.entry("COMPARATOR_PWM", "COMPARATOR"),
            Map.entry("CMP", "COMPARATOR"),
            Map.entry("MUX", "SELECTOR"),
            Map.entry("RTH", "TH_RTH"),
            Map.entry("CTH", "TH_CTH"),
            Map.entry("THERMAL_RESISTANCE", "TH_RTH"),
            Map.entry("THERMAL_CAPACITANCE", "TH_CTH"),
            Map.entry("HEAT_FLOW", "TH_FLOW"),
            Map.entry("TEMPERATURE_SOURCE", "TH_TEMP"),
            Map.entry("AMBIENT", "TH_AMBIENT"),
            Map.entry("ARCSIN", "ASIN"),
            Map.entry("ARCCOS", "ACOS"),
            Map.entry("ARCTAN", "ATAN"),
            Map.entry("SQUARE", "SQR"),
            Map.entry("POWER", "POW"),
            Map.entry("SIGNUM", "SIGN"),
            Map.entry("EQUAL", "EQ"),
            Map.entry("NOTEQUAL", "NE"),
            Map.entry("COUNT", "COUNTER"),
            Map.entry("ABC_DQ", "ABCDQ"),
            Map.entry("DQ_ABC", "DQABC"),
            Map.entry("THYRISTOR_CONTROL", "THYR_CTRL"),
            Map.entry("PMSM_CTRL", "PMSM_CONTROL"),
            Map.entry("PMSM_MOD", "PMSM_MODULATOR"),
            Map.entry("SVM", "SPACE_VECTOR")
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

    /** Legacy classic-editor type numbers of the control blocks (classic -> web). */
    private static final Map<Integer, Integer> LEGACY_CONTROL_TYPES = Map.ofEntries(
            Map.entry(1, 1001),   // voltmeter
            Map.entry(2, 1002),   // ammeter
            Map.entry(3, 1005),   // constant
            Map.entry(4, 1004),   // signal source
            Map.entry(5, 1003),   // scope
            Map.entry(6, 1000),   // gate
            Map.entry(33, 1038),  // ASIN
            Map.entry(35, 1039),  // ACOS
            Map.entry(37, 1040),  // TAN
            Map.entry(38, 1041),  // ATAN
            Map.entry(39, 1042),  // SQR
            Map.entry(42, 1043),  // POW
            Map.entry(44, 1044),  // ROUND
            Map.entry(47, 1045),  // SIGN
            Map.entry(48, 1046),  // EQ
            Map.entry(51, 1047),  // NE
            Map.entry(53, 1048),  // COUNTER
            Map.entry(59, 1049),  // ABCDQ
            Map.entry(63, 1050),  // DQABC
            Map.entry(65, 1051),  // THYR_CTRL
            Map.entry(66, 1052),  // PMSM_CONTROL
            Map.entry(72, 1053),  // PMSM_MODULATOR
            Map.entry(76, 1054),  // DEMUX
            Map.entry(77, 1055),  // SPACE_VECTOR
            Map.entry(82, 1056),  // SDFT
            Map.entry(85, 1058)   // SPARSEMATRIX
    );

    public static Map<String, Object> toCatalogJson() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> powerList = new ArrayList<>();
        List<Map<String, Object>> controlList = new ArrayList<>();
        List<Map<String, Object>> thermalList = new ArrayList<>();

        for (ComponentDef def : REGISTRY.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", def.id());
            entry.put("name", def.displayName());
            entry.put("domain", def.domain());
            entry.put("type_number", def.typeNumber());
            entry.put("prefix", def.defaultPrefix());
            entry.put("pins", def.pins());
            entry.put("input_pins", def.pins().subList(0, def.xPinCount()));
            entry.put("output_pins", def.pins().subList(def.xPinCount(), def.pins().size()));
            entry.put("headless_executable", def.headlessExecutable());
            entry.put("description", def.description());

            List<Map<String, Object>> params = new ArrayList<>();
            for (ParameterDef p : def.parameters()) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("name", p.name());
                pm.put("type", p.type());
                pm.put("unit", p.unit());
                pm.put("default", p.defaultValue());
                pm.put("slot", p.targetSlot());
                pm.put("description", p.description());
                params.add(pm);
            }
            entry.put("parameters", params);

            switch (def.domain()) {
                case "POWER_LK" -> powerList.add(entry);
                case "THERM" -> thermalList.add(entry);
                default -> controlList.add(entry);
            }
        }

        result.put("power_components", powerList);
        result.put("control_components", controlList);
        result.put("thermal_components", thermalList);

        result.put("orientation_codes", orientationCodes());
        result.put("grid_and_wiring_rules", gridAndWiringRules());
        result.put("control_block_geometry", controlBlockGeometry());
        result.put("signal_naming", signalNaming());
        result.put("parameter_overrides", parameterOverrides());
        result.put("solver_options", solverOptions());
        result.put("legacy_control_type_numbers", LEGACY_CONTROL_TYPES);

        Map<String, Object> scriptGuide = new LinkedHashMap<>();
        scriptGuide.put("overview", "Script blocks (SCRIPT_BLOCK, type 1016, or JAVA_BLOCK, type 61) execute C/Java-like control algorithms at each simulation time step.");
        scriptGuide.put("input_signals", "Array xIN[0], xIN[1], ... or aliases u1, u2, ... populated from connected probes/signals.");
        scriptGuide.put("output_signals", "Array yOUT[0], yOUT[1], ... driving connected gates or recorded signals.");
        scriptGuide.put("time_variables", Map.of("t", "Current simulation time in seconds", "dt", "Current simulation time step (dt) in seconds"));
        scriptGuide.put("constants", Map.of("PI", "Math.PI (3.141592653589793)", "E", "Math.E"));
        scriptGuide.put("math_functions", List.of("sin(x)", "cos(x)", "tan(x)", "abs(x)", "sqrt(x)", "exp(x)", "log(x)", "pow(x, y)", "min(x, y)", "max(x, y)"));
        scriptGuide.put("syntax_rules", List.of(
                "Standard statements terminated by semicolons: 'double v = xIN[0];'",
                "Persistent state across time steps MUST be declared in 'static_variables' (e.g. 'int step = 0; double integ = 0.0;')",
                "Variable updates: 'v = v + ki * err * dt;' or 'v += ki * err * dt;'",
                "Conditionals: 'if (cond) { ... } else { ... }' and ternary 'cond ? expr1 : expr2'",
                "End of script: 'return yOUT;'"
        ));
        scriptGuide.put("safety_rules", List.of(
                "Always guard divisions against 0: e.g. '(vDc > 50.0 ? vDc : 750.0)'",
                "Always clamp integrator variables to prevent windup"
        ));
        result.put("script_block_guide", scriptGuide);

        Map<String, Object> synthGuide = new LinkedHashMap<>();
        synthGuide.put("probes", "VOLTMETER targets a power component (e.g. C_1) or node pair; AMMETER targets an inductor or switch branch. 'signal_name' assigns the net label.");
        synthGuide.put("control_blocks", "Control blocks chain through CONTROL wires: place a block, wire its output terminal point to the next block's input terminal point. A labeled output (signal name) makes the value recordable.");
        synthGuide.put("gates", "GATE couples an 'in_signal' to 'target_switch' by component name.");
        synthGuide.put("closed_loop_pattern", "Reference CONSTANT and feedback VOLTMETER feed the two COMPARATOR inputs; the COMPARATOR output drives a GATE. Replace the comparator with a SCRIPT_BLOCK (PI + PWM) for regulated converters.");
        synthGuide.put("thermal_network", "Build heat networks from TH_RTH / TH_CTH between thermal nodes, inject losses with TH_FLOW, pin the reference with TH_AMBIENT, and drive junction temperatures with TH_TEMP sources. Electro-thermal feedback (R(T), Vf(T)) is configured per simulation run, see the multi-domain reference.");
        result.put("circuit_synthesis_guide", synthGuide);

        return result;
    }

    private static Map<String, Object> orientationCodes() {
        Map<String, Object> codes = new LinkedHashMap<>();
        codes.put("501", "SOUTH_NORTH: input terminal at (x, y+2), output terminal at (x, y-2) - power flows upward");
        codes.put("502", "WEST_EAST: input terminal at (x-2, y), output terminal at (x+2, y) - power flows rightward");
        codes.put("503", "NORTH_SOUTH: input terminal at (x, y-2), output terminal at (x, y+2) - power flows downward (default)");
        codes.put("504", "EAST_WEST: input terminal at (x+2, y), output terminal at (x-2, y) - power flows leftward");
        codes.put("terminal_distance", "Two-port terminals sit 2 grid units from the component center along the flow axis");
        return codes;
    }

    private static Map<String, Object> gridAndWiringRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("grid", "All positions are integer grid units; .ipes stores pixels at 16 px per grid unit internally");
        rules.put("wires", "Wires are polylines of grid points; two wires join into one net only where an END POINT of one coincides with a point of the other");
        rules.put("routing", "Never route a wire through a foreign terminal or share a grid point between different nets - that nets them together (short circuit)");
        rules.put("nets", "Terminals join the net of a wire endpoint at the same grid point; net labels ('label' on wires, terminal labels on components) merge nets by name; label '0' or 'gnd' is the ground reference");
        rules.put("ground", "Every galvanically isolated subcircuit needs exactly one ground/label-0 net, otherwise the solver pins a reference automatically and reports a warning");
        return rules;
    }

    private static Map<String, Object> controlBlockGeometry() {
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("inputs", "Input terminal i sits at (x - 2, y + i) for NORTH_SOUTH orientation (rotates with the orientation code)");
        geometry.put("outputs", "Output terminal j sits at (x + 2, y + j) for NORTH_SOUTH orientation");
        geometry.put("wiring", "CONTROL wires (connectorType 1) connect control terminal points; they are topologically separate from power (LK) wires");
        geometry.put("coupling", "Probes and gates additionally couple to POWER components by name or uid; probes can alternatively measure between two node labels");
        return geometry;
    }

    private static Map<String, Object> signalNaming() {
        Map<String, Object> naming = new LinkedHashMap<>();
        naming.put("probe_outputs", "A probe's output terminal label names its channel (e.g. label 'vout' logs as 'vout'); without a label the block name (VOLT.1) is used");
        naming.put("control_taps", "Any labeled control block output (constant, gain, PI, comparator, ...) is recorded under its label");
        naming.put("node_labels", "Labeled power nets are recorded under their net label (e.g. 'vin', 'sw')");
        naming.put("losses", "Per-device loss channels: P_loss_<device>, P_cond_<device>, P_sw_<device>; totals P_loss_total, P_cond_total, P_sw_total, E_loss_total");
        naming.put("thermal_feedback", "Junction temperature channels of electro-thermally coupled devices: Tj_<deviceName>");
        naming.put("magnetic", "Winding flux channels of magnetic domain windings: Phi_<windingName>");
        return naming;
    }

    private static Map<String, Object> parameterOverrides() {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("syntax", "Dotted paths '<component name>.<parameter name>' applied before the netlist is built, e.g. {'R.load.resistance': 4.0}");
        overrides.put("where", "Accepted by gecko_simulate, gecko_get_waveforms and gecko_measure_metrics via the parameter_overrides map - no file edit needed for sweeps");
        return overrides;
    }

    private static Map<String, Object> solverOptions() {
        Map<String, Object> solvers = new LinkedHashMap<>();
        solvers.put("solver", "'be' = backward Euler (robust default), 'trz' = trapezoidal (less numerical damping, 2nd order), 'gs' = Gear-Shichman");
        solvers.put("matrix_solver", "'auto' (default; sparse LU for >= 50 nodes), 'dense' (classic JAMA LU), 'sparse' (Gilbert-Peierls LU with partial pivoting)");
        solvers.put("adaptive", "Opt-in adaptive step size with LTE-based controller: enabled, relative_tolerance (default 1e-3), min/max step bounds");
        solvers.put("semiconductor_model", "'classic_pwl' (piecewise-linear flip state machine, default, matches the classic GUI) or 'shockley_nr' (Newton-Raphson on the Shockley diode equation for smooth convergence)");
        solvers.put("data_logging_interval", "Log every Nth time step (default 1)");
        return solvers;
    }
}
