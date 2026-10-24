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

        register(new ComponentDef(
                "COUPLED_INDUCTOR", "Coupled Inductor", "POWER_LK", 12, "L",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("inductance", "number", "H", 1e-3, 0, "Nominal inductance in Henrys (slot 10 holds companion matrix A value)"),
                        new ParameterDef("i_init", "number", "A", 0.0, 1, "Initial current in Amperes")
                ),
                "Coupled linear electrical inductor (LK_LKOP2, typ 12) supporting mutual magnetic coupling via MUTUAL_INDUCTANCE (LK_M)."
        ));

        register(new ComponentDef(
                "MUTUAL_INDUCTANCE", "Mutual Inductance Coupler", "POWER_LK", 9, "K",
                List.of(),
                List.of(
                        new ParameterDef("k", "number", "", 0.99, 0, "Magnetic coupling coefficient (0 < k <= 1)")
                ),
                "Mutual inductance coupling between two coupable inductors (LK_LKOP2, typ 12). Raw parameters store [k, uid1, uid2].",
                0
        ));

        register(new ComponentDef(
                "NONLINEAR_INDUCTOR", "Nonlinear Saturating Inductor", "POWER_LK", 2, "L",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("inductance", "number", "H", 1e-3, 0, "Nominal small-signal inductance in Henrys"),
                        new ParameterDef("i_init", "number", "A", 0.0, 1, "Initial current in Amperes")
                ),
                "Saturating inductor with current-dependent inductance table (nonlinX currents, nonlinY inductances)."
        ));

        register(new ComponentDef(
                "LISN", "LISN (Line Impedance Stabilization Network)", "POWER_LK", 13, "LISN",
                List.of("line_in", "eut_out"),
                List.of(),
                "Line Impedance Stabilization Network for conducted electromagnetic compatibility (EMC) measurements."
        ));

        register(new ComponentDef(
                "DC_MOTOR", "DC Motor (Separately Excited)", "POWER_LK", 14, "M_DC",
                List.of("a1", "a2"),
                List.of(
                        new ParameterDef("r_armature", "number", "Ohm", 0.5, 0, "Armature circuit resistance in Ohms"),
                        new ParameterDef("l_armature", "number", "H", 0.01, 1, "Armature circuit inductance in Henrys")
                ),
                "Separately or series excited DC machine."
        ));

        register(new ComponentDef(
                "SYNC_MOTOR_SALIENT", "Synchronous Motor (Salient Pole)", "POWER_LK", 16, "SM",
                List.of("uA", "uB", "uC"),
                List.of(),
                "Three-phase synchronous machine with salient poles.",
                3
        ));

        register(new ComponentDef(
                "SYNC_MOTOR_ROUND", "Synchronous Motor (Round Rotor)", "POWER_LK", 17, "SM",
                List.of("uA", "uB", "uC"),
                List.of(),
                "Three-phase synchronous machine with round rotor (cylindrical rotor).",
                3
        ));

        register(new ComponentDef(
                "INDUCTION_MOTOR_CAGE", "Induction Motor (Cage Type A)", "POWER_LK", 18, "IM",
                List.of("uA", "uB", "uC"),
                List.of(),
                "Three-phase squirrel-cage induction machine (cage type A).",
                3
        ));

        register(new ComponentDef(
                "INDUCTION_MOTOR", "Induction Motor (Generic)", "POWER_LK", 20, "IM",
                List.of("uA", "uB", "uC"),
                List.of(),
                "Generic three-phase induction machine.",
                3
        ));

        register(new ComponentDef(
                "INDUCTION_MOTOR_SAT", "Induction Motor with Saturation", "POWER_LK", 21, "IM",
                List.of("uA", "uB", "uC"),
                List.of(),
                "Three-phase induction machine with magnetic saturation characteristic.",
                3
        ));

        register(new ComponentDef(
                "OPAMP", "Operational Amplifier", "POWER_LK", 22, "OP",
                List.of("in_plus", "in_minus", "out"),
                List.of(
                        new ParameterDef("r_in", "number", "Ohm", 90000.0, 0, "Differential input resistance in Ohms"),
                        new ParameterDef("r_out", "number", "Ohm", 0.12, 1, "Output series resistance in Ohms"),
                        new ParameterDef("f_corner", "number", "Hz", 1e5, 4, "Open loop dominant pole corner frequency in Hertz"),
                        new ParameterDef("gain_dc", "number", "V/V", 10000.0, 5, "Differential open-loop DC voltage gain"),
                        new ParameterDef("u_max", "number", "V", 12.0, 7, "Positive saturation output rail voltage limit"),
                        new ParameterDef("u_min", "number", "V", -12.0, 8, "Negative saturation output rail voltage limit")
                ),
                "Macro-model operational amplifier with finite input/output impedance, gain-bandwidth product, and voltage rail limits.",
                2
        ));

        register(new ComponentDef(
                "RELUCTANCE", "Magnetic Reluctance", "POWER_LK", 24, "Rm",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("reluctance", "number", "1/H", 1.0, 0, "Magnetic reluctance Rm = F / Phi in inverse Henrys")
                ),
                "Linear magnetic reluctance element of the reluctance network (stamped as conductance 1/Rm in MNA)."
        ));

        register(new ComponentDef(
                "RELUCTANCE_COIL", "Reluctance Network Inductor Coil", "POWER_LK", 25, "Lrel",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("inductance", "number", "H", 1e-3, 0, "Winding inductance in Henrys"),
                        new ParameterDef("i_init", "number", "A", 0.0, 1, "Initial winding current in Amperes")
                ),
                "Inductor winding coupled into the magnetic reluctance network."
        ));

        register(new ComponentDef(
                "MMF_SOURCE", "Magnetomotive Force Source", "POWER_LK", 26, "MMF",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("source_type", "number", "", 401.0, 0, "Source waveform code; 401 = constant DC MMF"),
                        new ParameterDef("mmf", "number", "A", 100.0, 1, "Magnetomotive force Theta = N*I in ampere-turns")
                ),
                "Magnetomotive force (MMF) source of the reluctance network (magnetic potential source)."
        ));

        register(new ComponentDef(
                "SUBCIRCUIT_TERMINAL", "Subcircuit Terminal Pin", "POWER_LK", 29, "PIN",
                List.of("in", "out"),
                List.of(),
                "Boundary terminal pin for hierarchical subcircuit modular blocks."
        ));

        register(new ComponentDef(
                "RELUCTANCE_TERMINAL", "Reluctance Network Terminal Pin", "POWER_LK", 30, "RPIN",
                List.of("in", "out"),
                List.of(),
                "Boundary terminal pin for magnetic reluctance subcircuit networks."
        ));

        register(new ComponentDef(
                "GLOBAL_TERMINAL", "Global Ground Terminal", "POWER_LK", 31, "GND",
                List.of("gnd"),
                List.of(),
                "Global named circuit reference net (ground / 0V reference node).",
                1
        ));

        register(new ComponentDef(
                "RELUCTANCE_GLOBAL_TERMINAL", "Reluctance Global Ground Terminal", "POWER_LK", 32, "RGND",
                List.of("gnd"),
                List.of(),
                "Global reference terminal of the magnetic reluctance network.",
                1
        ));

        register(new ComponentDef(
                "DC_MOTOR_PERM", "Permanent Magnet DC Motor", "POWER_LK", 51, "M_DC",
                List.of("a1", "a2"),
                List.of(
                        new ParameterDef("r_armature", "number", "Ohm", 0.5, 0, "Armature circuit resistance in Ohms"),
                        new ParameterDef("l_armature", "number", "H", 2e-3, 1, "Armature circuit inductance in Henrys"),
                        new ParameterDef("k_torque", "number", "N*m/A", 0.1, 2, "Back-EMF and torque constant Km in N*m/A (or V*s/rad)")
                ),
                "Permanent magnet DC machine with armature resistance, inductance, and back-EMF torque constant."
        ));

        register(new ComponentDef(
                "NONLINEAR_RELUCTANCE", "Nonlinear Saturating Reluctance", "POWER_LK", 52, "RmNL",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("reluctance", "number", "1/H", 1.0, 0, "Nominal small-signal magnetic reluctance in inverse Henrys")
                ),
                "Magnetic reluctance element with current/flux-dependent nonlinear B-H saturation characteristic."
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
                "DEADTIME", "Dead-Time Generator", "CONTROL", 1037, "DT",
                List.of("in", "out_hi", "out_lo"),
                List.of(
                        new ParameterDef("dead_time", "number", "s", 1e-6, 0, "Dead-time blanking delay in seconds")
                ),
                "Complementary half-bridge gate driver with turn-on delay (dead-time insertion) to prevent shoot-through cross-conduction.",
                1
        ));

        register(new ComponentDef(
                "SUB", "Subtract", "CONTROL", 1017, "SUB",
                List.of("plus", "minus", "out"),
                List.of(),
                "Subtracts input 1 from input 0: out = plus - minus.",
                2
        ));

        register(new ComponentDef(
                "ADD", "Add", "CONTROL", 1018, "ADD",
                List.of("in0", "in1", "out"),
                List.of(),
                "Adds two signals: out = in0 + in1.",
                2
        ));

        register(new ComponentDef(
                "LIMIT", "Limiter", "CONTROL", 1021, "LIM",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("min", "number", "", -1.0, 0, "Lower limit bound"),
                        new ParameterDef("max", "number", "", 1.0, 1, "Upper limit bound")
                ),
                "Limits signal amplitude between Min and Max bounds."
        ));

        register(new ComponentDef(
                "SAMPLE_HOLD", "Sample and Hold", "CONTROL", 1033, "SH",
                List.of("d", "clk", "out"),
                List.of(),
                "Samples data input when clock signal exceeds 0.5, holds otherwise.",
                2
        ));

        register(new ComponentDef(
                "SPWM_GENERATOR", "Sinusoidal PWM Generator", "CONTROL", 1016, "SPWM",
                List.of("v_mod", "g1", "g2", "g3", "g4"),
                List.of(
                        new ParameterDef("modulation_index", "number", "", 0.85, 0, "Modulation index ma (0..1.2)"),
                        new ParameterDef("carrier_frequency", "number", "Hz", 10000.0, 1, "Triangular carrier frequency in Hz"),
                        new ParameterDef("grid_frequency", "number", "Hz", 50.0, 2, "Fundamental reference frequency in Hz")
                ),
                "High-frequency carrier comparison PWM generator supporting single-phase, H-bridge, and 3-phase modulation with dead-time.",
                1
        ));

        register(new ComponentDef(
                "PI_CONTROLLER", "Anti-Windup PI Controller", "CONTROL", 1016, "PI",
                List.of("v_err", "v_ctrl"),
                List.of(
                        new ParameterDef("kp", "number", "", 1.0, 0, "Proportional gain"),
                        new ParameterDef("ki", "number", "1/s", 100.0, 1, "Integral gain Ki (or Kp/Ti)"),
                        new ParameterDef("limit_min", "number", "", -1.0, 2, "Lower saturation clamping limit"),
                        new ParameterDef("limit_max", "number", "", 1.0, 3, "Upper saturation clamping limit")
                ),
                "Proportional-integral controller with anti-windup clamping, setpoint subtracting, and optional reset input.",
                1
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
                List.of(
                        new ParameterDef("on_time", "number", "s", 4e-3, 0, "Gate pulse duration in seconds"),
                        new ParameterDef("init_freq", "number", "Hz", 50.0, 1, "Initial / line synchronization frequency in Hertz"),
                        new ParameterDef("phase_shift", "number", "deg", 30.0, 2, "Synchronization phase shift in degrees")
                ),
                "Firing angle generator for line-commutated thyristor bridges.",
                2
        ));

        register(new ComponentDef(
                "PMSM_CONTROL", "PMSM Control", "CONTROL", 1052, "PMSM_CTRL",
                List.of(
                        "ia", "ib", "omega", "phi", "nRef",
                        "kpN", "tN", "nLim", "kpI", "tI", "iLim", "fP",
                        "valpha", "vbeta", "vqRef", "vdRef",
                        "iqRef", "idRef", "iq", "id"
                ),
                List.of(
                        new ParameterDef("kp_n", "number", "", 2.0, 0, "Speed PI controller proportional gain"),
                        new ParameterDef("t_n", "number", "s", 0.05, 1, "Speed PI controller integral time constant"),
                        new ParameterDef("n_limit", "number", "A", 20.0, 2, "Speed controller saturation limit / max Iq reference"),
                        new ParameterDef("kp_i", "number", "", 5.0, 3, "Current PI controller proportional gain"),
                        new ParameterDef("t_i", "number", "s", 0.01, 4, "Current PI controller integral time constant"),
                        new ParameterDef("i_limit", "number", "V", 400.0, 5, "Current controller saturation limit / max voltage")
                ),
                "Field-oriented current and speed controller for PMSM machines.",
                12
        ));

        register(new ComponentDef(
                "PMSM_MODULATOR", "PMSM Modulator", "CONTROL", 1053, "PMSM_MOD",
                List.of("valpha", "vbeta", "tri", "vdc", "sA", "sB", "sC"),
                List.of(
                        new ParameterDef("vdc", "number", "V", 600.0, 0, "DC link voltage in Volts (used if vdc pin unwired)")
                ),
                "Modulator / space-vector transform for PMSM machines.",
                4
        ));

        register(new ComponentDef(
                "DEMUX", "Demultiplexer / Vector Splitter", "CONTROL", 1054, "DEMUX",
                List.of("in", "out0", "out1", "out2"),
                List.of(
                        new ParameterDef("num_outputs", "number", "", 3, 0, "Number of scalar output channels extracted from the vector")
                ),
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
                        new ParameterDef("avg_span", "number", "s", 0.02, 0, "Averaging window span duration in seconds"),
                        new ParameterDef("frequency", "number", "Hz", 50.0, 1, "Fundamental tracking frequency in Hertz")
                ),
                "Sliding Discrete Fourier Transform for real-time fundamental harmonic tracking."
        ));

        register(new ComponentDef(
                "SPARSEMATRIX", "Sparse Matrix Modulator", "CONTROL", 1058, "SPM",
                List.of(
                        "fDR", "ur", "us", "ut", "uNmax", "uOUTmax", "fOUT", "phi2",
                        "sRp", "sSp", "sTp", "sRm", "sSm", "sTm", "s1", "s2", "s3"
                ),
                List.of(),
                "Space Vector PWM Modulator for Indirect Sparse Matrix Converters (ISMC).",
                8
        ));

        register(new ComponentDef(
                "MUL", "Multiply", "CONTROL", 1019, "MUL",
                List.of("in0", "in1", "out"),
                List.of(),
                "Multiplies two signals: out = in0 * in1. Legacy editor type number: 14.",
                2
        ));

        register(new ComponentDef(
                "DIV", "Divide", "CONTROL", 1020, "DIV",
                List.of("num", "den", "out"),
                List.of(),
                "Divides numerator by denominator: out = num / den. Legacy editor type number: 15.",
                2
        ));

        register(new ComponentDef(
                "ABS", "Absolute Value", "CONTROL", 1022, "ABS",
                List.of("in", "out"),
                List.of(),
                "Computes absolute value: out = |in|. Legacy editor type number: 32."
        ));

        register(new ComponentDef(
                "SQRT", "Square Root", "CONTROL", 1023, "SQRT",
                List.of("in", "out"),
                List.of(),
                "Computes square root: out = sqrt(in). Legacy editor type number: 43."
        ));

        register(new ComponentDef(
                "EXP", "Exponential", "CONTROL", 1024, "EXP",
                List.of("in", "out"),
                List.of(),
                "Natural exponential function: out = e^(in). Legacy editor type number: 40."
        ));

        register(new ComponentDef(
                "LN", "Natural Logarithm", "CONTROL", 1025, "LN",
                List.of("in", "out"),
                List.of(),
                "Natural logarithm: out = ln(in). Legacy editor type number: 41."
        ));

        register(new ComponentDef(
                "SIN", "Sine", "CONTROL", 1026, "SIN",
                List.of("in", "out"),
                List.of(),
                "Trigonometric sine function: out = sin(in) with in in radians. Legacy editor type number: 34."
        ));

        register(new ComponentDef(
                "COS", "Cosine", "CONTROL", 1027, "COS",
                List.of("in", "out"),
                List.of(),
                "Trigonometric cosine function: out = cos(in) with in in radians. Legacy editor type number: 36."
        ));

        register(new ComponentDef(
                "MIN", "Minimum", "CONTROL", 1028, "MIN",
                List.of("in0", "in1", "out"),
                List.of(),
                "Selects the smaller of two signals: out = min(in0, in1). Legacy editor type number: 49.",
                2
        ));

        register(new ComponentDef(
                "MAX", "Maximum", "CONTROL", 1029, "MAX",
                List.of("in0", "in1", "out"),
                List.of(),
                "Selects the larger of two signals: out = max(in0, in1). Legacy editor type number: 50.",
                2
        ));

        register(new ComponentDef(
                "HYS", "Hysteresis", "CONTROL", 1030, "HYS",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("h", "number", "", 0.1, 0, "Switching threshold window half-width (switches at +H and -H)")
                ),
                "Hysteresis comparator with threshold window +/-H: outputs +1 when in > +H, -1 when in < -H, and latches previous state inside [-H, +H]. Legacy editor type number: 11."
        ));

        register(new ComponentDef(
                "PT2", "PT2 Filter", "CONTROL", 1031, "PT2",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("tau", "number", "s", 0.001, 0, "Filter time constant tau in seconds"),
                        new ParameterDef("k", "number", "", 1.0, 1, "DC Gain K")
                ),
                "Second-order low-pass filter element: G(s) = K / (1 + (tau*s)^2). Legacy editor type number: 9."
        ));

        register(new ComponentDef(
                "PD", "PD / Derivative Element", "CONTROL", 1032, "PD",
                List.of("in", "out"),
                List.of(
                        new ParameterDef("kd", "number", "", 1.0, 0, "Derivative gain Kd for discrete differentiator y = Kd * dx/dt")
                ),
                "Discrete derivative differentiator: y = (Kd / dt) * (in - in_old). Legacy editor type number: 29."
        ));

        register(new ComponentDef(
                "TIME", "Simulation Time", "CONTROL", 1034, "TIME",
                List.of("t"),
                List.of(),
                "Outputs current simulation elapsed time t in seconds. Legacy editor type number: 58.",
                0
        ));

        register(new ComponentDef(
                "XOR", "XOR Gate", "CONTROL", 1035, "XOR",
                List.of("a", "b", "out"),
                List.of(),
                "Logical XOR: output = 1 when exactly one input exceeds 0.5. Legacy editor type number: 21.",
                2
        ));

        register(new ComponentDef(
                "GE", "Greater or Equal", "CONTROL", 1036, "GE",
                List.of("in0", "in1", "out"),
                List.of(),
                "Compares two signals: output = 1 while in0 >= in1, else 0. Legacy editor type number: 45.",
                2
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

        register(new ComponentDef(
                "TH_PvCHIP", "Thermal Power Loss Chip", "THERM", 41, "Pv",
                List.of("p", "n"),
                List.of(
                        new ParameterDef("loss_factor", "number", "", 1.0, 0, "Loss scaling factor (default 1.0)")
                ),
                "Power loss chip injecting semiconductor losses from a coupled power switch (target switch in parameterString[0]) into a thermal node."
        ));

        register(new ComponentDef(
                "TH_GLOBAL_TERMINAL", "Thermal Ground Terminal", "THERM", 50, "TH_GND",
                List.of("gnd"),
                List.of(),
                "Thermal reference/ground terminal (net label '0').",
                1
        ));

        register(new ComponentDef(
                "TH_MODUL", "Thermal Housing Module", "THERM", 42, "MOD",
                List.of("chip", "heatsink"),
                List.of(
                        new ParameterDef("r_th", "number", "K/W", 1.0, 0, "Module thermal resistance in Kelvin per Watt"),
                        new ParameterDef("c_th", "number", "J/K", 1.0, 1, "Module heat capacity in Joules per Kelvin")
                ),
                "Module housing thermal model: Rth/Cth pair between chip and heatsink."
        ));

        register(new ComponentDef(
                "TH_TERMINAL", "Thermal Subcircuit Terminal", "THERM", 49, "T_PIN",
                List.of("in", "out"),
                List.of(),
                "Thermal subcircuit boundary terminal pin."
        ));

        // ====================================================================
        // Special & Documentation Domain Components
        // ====================================================================

        register(new ComponentDef(
                "TEXTBOX", "Markdown Note / Textbox", "SPECIAL", 2001, "NOTE",
                List.of(),
                List.of(
                        new ParameterDef("width", "number", "", 26.0, 1, "Card width in schematic grid units"),
                        new ParameterDef("height", "number", "", 14.0, 2, "Card height in schematic grid units")
                ),
                "Rich Markdown documentation note rendered directly on the schematic canvas for circuit explanations and block diagram documentation.",
                0
        ));
    }

    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    private static void alias(String alias, String target) {
        ALIASES.put(alias.toUpperCase(Locale.ROOT), target.toUpperCase(Locale.ROOT));
    }

    static {
        // Power passives & sources
        alias("R", "RESISTOR");
        alias("RES", "RESISTOR");
        alias("LK_R", "RESISTOR");
        alias("L", "INDUCTOR");
        alias("IND", "INDUCTOR");
        alias("LK_L", "INDUCTOR");
        alias("C", "CAPACITOR");
        alias("CAP", "CAPACITOR");
        alias("LK_C", "CAPACITOR");
        alias("DC_VOLTAGE", "VOLTAGE_SOURCE_DC");
        alias("DC_SOURCE", "VOLTAGE_SOURCE_DC");
        alias("VOLTAGE_DC", "VOLTAGE_SOURCE_DC");
        alias("V_DC", "VOLTAGE_SOURCE_DC");
        alias("VOLTAGE_SOURCE", "VOLTAGE_SOURCE_DC");
        alias("VDC", "VOLTAGE_SOURCE_DC");
        alias("V", "VOLTAGE_SOURCE_DC");
        alias("LK_U", "VOLTAGE_SOURCE_DC");
        alias("AC_VOLTAGE", "VOLTAGE_SOURCE_AC");
        alias("AC_SOURCE", "VOLTAGE_SOURCE_AC");
        alias("VOLTAGE_AC", "VOLTAGE_SOURCE_AC");
        alias("V_AC", "VOLTAGE_SOURCE_AC");
        alias("VAC", "VOLTAGE_SOURCE_AC");
        alias("DC_CURRENT", "CURRENT_SOURCE_DC");
        alias("CURRENT_SOURCE", "CURRENT_SOURCE_DC");
        alias("I_DC", "CURRENT_SOURCE_DC");
        alias("IDC", "CURRENT_SOURCE_DC");
        alias("I", "CURRENT_SOURCE_DC");
        alias("LK_I", "CURRENT_SOURCE_DC");
        alias("AC_CURRENT", "CURRENT_SOURCE_AC");
        alias("I_AC", "CURRENT_SOURCE_AC");
        alias("IAC", "CURRENT_SOURCE_AC");

        // Power switches & semiconductors
        alias("D", "DIODE");
        alias("LK_D", "DIODE");
        alias("SWITCH", "IDEAL_SWITCH");
        alias("IDEALSWITCH", "IDEAL_SWITCH");
        alias("SW", "IDEAL_SWITCH");
        alias("LK_S", "IDEAL_SWITCH");
        alias("THYR", "THYRISTOR");
        alias("SCR", "THYRISTOR");
        alias("LK_THYR", "THYRISTOR");
        alias("LK_TH", "THYRISTOR");
        alias("LK_IGBT", "IGBT");
        alias("MOSFET_TRANSISTOR", "MOSFET");
        alias("LK_MOSFET", "MOSFET");
        alias("TR", "TRANSFORMER");
        alias("TRANSFORMER_IDEAL", "TRANSFORMER");
        alias("LK_TRANS", "TRANSFORMER");
        alias("Q", "BJT");
        alias("NPN", "BJT");
        alias("PNP", "BJT");
        alias("LK_BJT", "BJT");

        // Coupled, magnetic & reluctance
        alias("LKOP2", "COUPLED_INDUCTOR");
        alias("LK_LKOP2", "COUPLED_INDUCTOR");
        alias("LK_M", "MUTUAL_INDUCTANCE");
        alias("MUTUAL_COUPLING", "MUTUAL_INDUCTANCE");
        alias("COUPLER", "MUTUAL_INDUCTANCE");
        alias("SATURATING_INDUCTOR", "NONLINEAR_INDUCTOR");
        alias("RM", "RELUCTANCE");
        alias("REL_RELUCTANCE", "RELUCTANCE");
        alias("LREL", "RELUCTANCE_COIL");
        alias("REL_INDUCTOR", "RELUCTANCE_COIL");
        alias("RELUCTANCE_INDUCTOR", "RELUCTANCE_COIL");
        alias("MMF", "MMF_SOURCE");
        alias("REL_MMF", "MMF_SOURCE");
        alias("RMNL", "NONLINEAR_RELUCTANCE");
        alias("NONLIN_REL", "NONLINEAR_RELUCTANCE");

        // Machines & OpAmps & Terminals
        alias("OP", "OPAMP");
        alias("OP_AMP", "OPAMP");
        alias("OPV1", "OPAMP");
        alias("LK_OPV1", "OPAMP");
        alias("LISN", "LISN");
        alias("LK_LISN", "LISN");
        alias("M_DC", "DC_MOTOR");
        alias("MOTOR_DC", "DC_MOTOR");
        alias("LK_MOTOR", "DC_MOTOR");
        alias("PM_DC_MOTOR", "DC_MOTOR_PERM");
        alias("MOTOR_PERM", "DC_MOTOR_PERM");
        alias("LK_MOTOR_PERM", "DC_MOTOR_PERM");
        alias("PMSM", "PMSM_MOTOR");
        alias("MOTOR_PMSM", "PMSM_MOTOR");
        alias("LK_MOTOR_PMSM", "PMSM_MOTOR");
        alias("SM_SALIENT", "SYNC_MOTOR_SALIENT");
        alias("LK_MOTOR_SMSALIENT", "SYNC_MOTOR_SALIENT");
        alias("SM_ROUND", "SYNC_MOTOR_ROUND");
        alias("LK_MOTOR_SMROUND", "SYNC_MOTOR_ROUND");
        alias("IM_CAGE", "INDUCTION_MOTOR_CAGE");
        alias("LK_MOTOR_IMA", "INDUCTION_MOTOR_CAGE");
        alias("INDUCTION_MOTOR_A", "INDUCTION_MOTOR_CAGE");
        alias("IM", "INDUCTION_MOTOR");
        alias("LK_MOTOR_IMC", "INDUCTION_MOTOR");
        alias("IM_SAT", "INDUCTION_MOTOR_SAT");
        alias("LK_MOTOR_IMSAT", "INDUCTION_MOTOR_SAT");
        alias("PIN", "SUBCIRCUIT_TERMINAL");
        alias("TERMINAL", "SUBCIRCUIT_TERMINAL");
        alias("LK_TERMINAL", "SUBCIRCUIT_TERMINAL");
        alias("RPIN", "RELUCTANCE_TERMINAL");
        alias("REL_TERMINAL", "RELUCTANCE_TERMINAL");
        alias("GND", "GLOBAL_TERMINAL");
        alias("GROUND", "GLOBAL_TERMINAL");
        alias("GLOBAL_NET", "GLOBAL_TERMINAL");
        alias("LK_GLOBAL_TERMINAL", "GLOBAL_TERMINAL");
        alias("RGND", "RELUCTANCE_GLOBAL_TERMINAL");
        alias("REL_GLOBAL_TERMINAL", "RELUCTANCE_GLOBAL_TERMINAL");
        alias("RELUCTANCE_GROUND", "RELUCTANCE_GLOBAL_TERMINAL");

        // Thermal domain
        alias("RTH", "TH_RTH");
        alias("CTH", "TH_CTH");
        alias("THERMAL_RESISTANCE", "TH_RTH");
        alias("THERMAL_RESISTOR", "TH_RTH");
        alias("THERMAL_CAPACITANCE", "TH_CTH");
        alias("THERMAL_CAPACITOR", "TH_CTH");
        alias("HEAT_FLOW", "TH_FLOW");
        alias("HEAT_FLOW_SOURCE", "TH_FLOW");
        alias("TEMPERATURE_SOURCE", "TH_TEMP");
        alias("AMBIENT", "TH_AMBIENT");
        alias("AMBIENT_TEMPERATURE", "TH_AMBIENT");
        alias("THERMAL_POWER_LOSS", "TH_PvCHIP");
        alias("PV_CHIP", "TH_PvCHIP");
        alias("PV", "TH_PvCHIP");
        alias("MOD", "TH_MODUL");
        alias("THERMAL_MODULE", "TH_MODUL");
        alias("MODUL", "TH_MODUL");
        alias("THERMAL_TERMINAL", "TH_TERMINAL");
        alias("THERMAL_GROUND", "TH_GLOBAL_TERMINAL");
        alias("TH_GND", "TH_GLOBAL_TERMINAL");

        // Control domain
        alias("GATE_DRIVER", "GATE");
        alias("VOLT", "VOLTMETER");
        alias("VOLTAGE_PROBE", "VOLTMETER");
        alias("V_PROBE", "VOLTMETER");
        alias("AMM", "AMMETER");
        alias("AMP", "AMMETER");
        alias("CURRENT_PROBE", "AMMETER");
        alias("I_PROBE", "AMMETER");
        alias("OSCILLOSCOPE", "SCOPE");
        alias("SIGNAL", "SIGNAL_SOURCE");
        alias("SIG", "SIGNAL_SOURCE");
        alias("SOURCE_SIGNAL", "SIGNAL_SOURCE");
        alias("CONST", "CONSTANT");
        alias("K", "CONSTANT");
        alias("AMP_GAIN", "GAIN");
        alias("PI_CTRL", "PI");
        alias("PT1_FILTER", "PT1");
        alias("PT2_FILTER", "PT2");
        alias("DERIVATIVE", "PD");
        alias("DIFF", "PD");
        alias("DIFFERENTIATOR", "PD");
        alias("INT", "INTEGRATOR");
        alias("CMP", "COMPARATOR");
        alias("COMPARATOR_PWM", "COMPARATOR");
        alias("LOGIC_AND", "AND");
        alias("LOGIC_OR", "OR");
        alias("INVERTER", "NOT");
        alias("LOGIC_NOT", "NOT");
        alias("LOGIC_XOR", "XOR");
        alias("GREATER_EQUAL", "GE");
        alias("GTE", "GE");
        alias("MUX", "SELECTOR");
        alias("MULTIPLEXER", "SELECTOR");
        alias("TRANSPORT_DELAY", "DELAY");
        alias("SCRIPT", "SCRIPT_BLOCK");
        alias("SCRIPTBLOCK", "SCRIPT_BLOCK");
        alias("FUNC", "SCRIPT_BLOCK");
        alias("MCU", "SCRIPT_BLOCK");
        alias("DSP", "SCRIPT_BLOCK");
        alias("JAVA", "JAVA_BLOCK");
        alias("JAVA_FUNCTION", "JAVA_BLOCK");
        alias("CNATC", "NATIVE_C");
        alias("C_BLOCK", "NATIVE_C");
        alias("DEADTIME_GENERATOR", "DEADTIME");
        alias("DEAD_TIME", "DEADTIME");
        alias("DT", "DEADTIME");
        alias("SPWM", "SPWM_GENERATOR");
        alias("PWM_SPWM", "SPWM_GENERATOR");
        alias("PI_REGULATOR", "PI_CONTROLLER");
        alias("PI_ANTI_WINDUP", "PI_CONTROLLER");
        alias("SUBTRACT", "SUB");
        alias("SUBTRACTION", "SUB");
        alias("ADDER", "ADD");
        alias("SUM", "ADD");
        alias("MULTIPLY", "MUL");
        alias("MULTIPLICATION", "MUL");
        alias("PROD", "MUL");
        alias("PRODUCT", "MUL");
        alias("DIVIDE", "DIV");
        alias("DIVISION", "DIV");
        alias("QUOTIENT", "DIV");
        alias("LIMITER", "LIMIT");
        alias("LIM", "LIMIT");
        alias("SATURATION", "LIMIT");
        alias("ABSOLUTE", "ABS");
        alias("ROOT", "SQRT");
        alias("EXPONENTIAL", "EXP");
        alias("LOG", "LN");
        alias("NATURAL_LOG", "LN");
        alias("SINE", "SIN");
        alias("COSINE", "COS");
        alias("TANGENT", "TAN");
        alias("ARCSIN", "ASIN");
        alias("ARCCOS", "ACOS");
        alias("ARCTAN", "ATAN");
        alias("SQUARE", "SQR");
        alias("POWER", "POW");
        alias("RND", "ROUND");
        alias("SIGNUM", "SIGN");
        alias("SGN", "SIGN");
        alias("EQUAL", "EQ");
        alias("NOTEQUAL", "NE");
        alias("NOT_EQUAL", "NE");
        alias("MINIMUM", "MIN");
        alias("MAXIMUM", "MAX");
        alias("HYSTERESIS", "HYS");
        alias("COUNT", "COUNTER");
        alias("CNT", "COUNTER");
        alias("SIM_TIME", "TIME");
        alias("CLOCK", "TIME");
        alias("SAMPLEHOLD", "SAMPLE_HOLD");
        alias("SAMPLE_AND_HOLD", "SAMPLE_HOLD");
        alias("SH", "SAMPLE_HOLD");
        alias("ABC_DQ", "ABCDQ");
        alias("PARK", "ABCDQ");
        alias("CLARKE", "ABCDQ");
        alias("DQ_ABC", "DQABC");
        alias("INV_PARK", "DQABC");
        alias("THYRISTOR_CONTROL", "THYR_CTRL");
        alias("PMSM_CTRL", "PMSM_CONTROL");
        alias("FOC", "PMSM_CONTROL");
        alias("PMSM_MOD", "PMSM_MODULATOR");
        alias("SVPWM", "PMSM_MODULATOR");
        alias("BUS_SPLITTER", "DEMUX");
        alias("SVM", "SPACE_VECTOR");
        alias("SLIDING_DFT", "SDFT");
        alias("SPM", "SPARSEMATRIX");
        alias("ISMC", "SPARSEMATRIX");

        // Documentation / Special
        alias("NOTE", "TEXTBOX");
        alias("NOTE_BOX", "TEXTBOX");
        alias("ANNOTATION", "TEXTBOX");
    }

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
            Map.entry(7, 1006),   // gain
            Map.entry(8, 1008),   // pt1
            Map.entry(9, 1031),   // pt2
            Map.entry(10, 1007),  // pi
            Map.entry(11, 1030),  // hys
            Map.entry(12, 1018),  // add
            Map.entry(13, 1017),  // sub
            Map.entry(14, 1019),  // mul
            Map.entry(15, 1020),  // div
            Map.entry(18, 1013),  // not
            Map.entry(19, 1011),  // and
            Map.entry(20, 1012),  // or
            Map.entry(21, 1035),  // xor
            Map.entry(25, 1015),  // delay
            Map.entry(26, 1033),  // samplehold
            Map.entry(27, 1021),  // limit
            Map.entry(29, 1032),  // pd
            Map.entry(32, 1022),  // abs
            Map.entry(33, 1044),  // ROUND
            Map.entry(34, 1026),  // SIN
            Map.entry(35, 1038),  // ASIN
            Map.entry(36, 1027),  // COS
            Map.entry(37, 1039),  // ACOS
            Map.entry(38, 1040),  // TAN
            Map.entry(39, 1041),  // ATAN
            Map.entry(40, 1024),  // EXP
            Map.entry(41, 1025),  // LN
            Map.entry(42, 1042),  // SQR
            Map.entry(43, 1023),  // SQRT
            Map.entry(44, 1043),  // POW
            Map.entry(45, 1036),  // GE
            Map.entry(46, 1010),  // GT
            Map.entry(47, 1046),  // EQ
            Map.entry(48, 1047),  // NE
            Map.entry(49, 1028),  // MIN
            Map.entry(50, 1029),  // MAX
            Map.entry(51, 1045),  // SIGN
            Map.entry(53, 1048),  // COUNTER
            Map.entry(58, 1034),  // TIME
            Map.entry(59, 1058),  // SPARSEMATRIX
            Map.entry(63, 1055),  // SPACE_VECTOR
            Map.entry(64, 1009),  // INTEGRATOR
            Map.entry(65, 1049),  // ABCDQ
            Map.entry(66, 1050),  // DQABC
            Map.entry(72, 1051),  // THYR_CTRL
            Map.entry(76, 1052),  // PMSM_CONTROL
            Map.entry(77, 1053),  // PMSM_MODULATOR
            Map.entry(82, 1056),  // SDFT
            Map.entry(84, 1014),  // MUX
            Map.entry(85, 1054)   // DEMUX
    );

    public static Map<String, Object> toComponentEntry(ComponentDef def) {
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
        return entry;
    }

    private static Map<String, Object> generateUsageExample(ComponentDef def) {
        Map<String, Object> example = new LinkedHashMap<>();
        if ("POWER_LK".equals(def.domain())) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("name", def.defaultPrefix() + "1");
            comp.put("type", def.id());
            comp.put("nodes", def.pins().isEmpty() ? List.of("nodeA", "nodeB") : def.pins());
            Map<String, Object> params = new LinkedHashMap<>();
            for (ParameterDef p : def.parameters()) {
                params.put(p.name(), p.defaultValue());
            }
            if (!params.isEmpty()) {
                comp.put("parameters", params);
            }
            example.put("placement_section", "components");
            example.put("example", comp);
        } else if ("THERM".equals(def.domain())) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("name", def.defaultPrefix() + "1");
            comp.put("type", def.id());
            comp.put("nodes", def.pins().isEmpty() ? List.of("th_node1", "th_node2") : def.pins());
            Map<String, Object> params = new LinkedHashMap<>();
            for (ParameterDef p : def.parameters()) {
                params.put(p.name(), p.defaultValue());
            }
            if (!params.isEmpty()) {
                comp.put("parameters", params);
            }
            example.put("placement_section", "thermal_components");
            example.put("example", comp);
        } else if ("SPECIAL".equals(def.domain())) {
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("name", "NOTE_1");
            note.put("text", "Circuit documentation note or block header");
            example.put("placement_section", "notes");
            example.put("example", note);
        } else {
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("name", def.defaultPrefix() + "_1");
            block.put("type", def.id());
            Map<String, Object> params = new LinkedHashMap<>();
            for (ParameterDef p : def.parameters()) {
                params.put(p.name(), p.defaultValue());
            }
            if (!params.isEmpty()) {
                block.put("parameters", params);
            }
            example.put("placement_section", "control.blocks");
            example.put("example", block);
        }
        return example;
    }

    public static Map<String, Object> toCatalogJson() {
        return toCatalogJson(null, null);
    }

    public static Map<String, Object> toCatalogJson(String domainFilter, String componentQuery) {
        if (componentQuery != null && !componentQuery.isBlank()) {
            ComponentDef def = get(componentQuery);
            if (def == null) {
                Map<String, Object> notFound = new LinkedHashMap<>();
                notFound.put("found", false);
                notFound.put("query", componentQuery);
                notFound.put("message", "No component matching '" + componentQuery + "' found in catalog.");
                String upper = componentQuery.trim().toUpperCase(Locale.ROOT);
                List<String> suggestions = REGISTRY.keySet().stream()
                        .filter(k -> k.contains(upper))
                        .limit(5)
                        .toList();
                notFound.put("suggestions", suggestions);
                return notFound;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("found", true);
            result.put("component", toComponentEntry(def));
            List<String> aliases = ALIASES.entrySet().stream()
                    .filter(e -> e.getValue().equalsIgnoreCase(def.id()))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
            result.put("aliases", aliases);
            result.put("usage_example", generateUsageExample(def));
            return result;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> powerList = new ArrayList<>();
        List<Map<String, Object>> controlList = new ArrayList<>();
        List<Map<String, Object>> thermalList = new ArrayList<>();
        List<Map<String, Object>> specialList = new ArrayList<>();

        for (ComponentDef def : REGISTRY.values()) {
            Map<String, Object> entry = toComponentEntry(def);

            switch (def.domain()) {
                case "POWER_LK" -> powerList.add(entry);
                case "THERM" -> thermalList.add(entry);
                case "SPECIAL" -> specialList.add(entry);
                default -> controlList.add(entry);
            }
        }

        if (domainFilter != null && !domainFilter.isBlank()) {
            String norm = domainFilter.trim().toLowerCase(Locale.ROOT);
            Map<String, Object> filtered = new LinkedHashMap<>();
            filtered.put("filtered_domain", norm);
            List<Map<String, Object>> selected;
            switch (norm) {
                case "power", "power_lk", "lk" -> selected = powerList;
                case "control", "ctrl" -> selected = controlList;
                case "thermal", "therm", "th" -> selected = thermalList;
                case "special", "doc", "notes" -> selected = specialList;
                default -> selected = null;
            }
            if (selected != null) {
                filtered.put("components", selected);
                filtered.put("count", selected.size());
                filtered.put("aliases", Collections.unmodifiableMap(ALIASES));
                filtered.put("orientation_codes", orientationCodes());
                filtered.put("grid_and_wiring_rules", gridAndWiringRules());
                return filtered;
            }
        }

        result.put("power_components", powerList);
        result.put("control_components", controlList);
        result.put("thermal_components", thermalList);
        result.put("special_components", specialList);

        List<Map<String, Object>> allComponents = new ArrayList<>();
        allComponents.addAll(powerList);
        allComponents.addAll(controlList);
        allComponents.addAll(thermalList);
        allComponents.addAll(specialList);
        result.put("all_components", allComponents);
        result.put("total_component_count", REGISTRY.size());
        result.put("aliases", Collections.unmodifiableMap(ALIASES));

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
        synthGuide.put("parameterized_control_blocks", "Built-in pre-verified control blocks: 'SPWM_GENERATOR' (carrier comparison, single/three-phase), 'PI_CONTROLLER' (anti-windup clamping, reset, error setpoint), 'DEADTIME_GENERATOR' (complementary half-bridge gate delays with td). Can specify directly under control.parameterized_blocks with auto-coupling to switches.");
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
