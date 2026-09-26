package gecko.mcp;

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Self-documenting MCP Resources exposed by GeckoCIRCUITS.
 */
public final class McpResources {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private McpResources() {
    }

    /** Single-text-content resource result via the non-deprecated builder APIs. */
    private static McpSchema.ReadResourceResult textResult(String uri, String mimeType, String text) {
        return McpSchema.ReadResourceResult.builder(List.of(
                McpSchema.TextResourceContents.builder(uri, text).mimeType(mimeType).build()
        )).build();
    }

    public static List<SyncResourceSpecification> all() {
        return List.of(
                componentCatalogResource(),
                scriptBlockReferenceResource(),
                controlBlocksReferenceResource(),
                workflowReferenceResource(),
                multiDomainReferenceResource(),
                topologiesReferenceResource(),
                examplesListResource()
        );
    }

    private static SyncResourceSpecification componentCatalogResource() {
        String uri = "gecko://catalog/components";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Component Catalog")
                .description("Complete schema of all GeckoCIRCUITS power and control components with parameter names, units, defaults, and pinouts")
                .mimeType("application/json")
                .build();

        return new SyncResourceSpecification(resource, (exchange, req) -> {
            try {
                String text = JSON.writeValueAsString(ComponentCatalog.toCatalogJson());
                return textResult(uri, "application/json", text);
            } catch (Exception e) {
                return textResult(uri, "text/plain", "Error: " + e.getMessage());
            }
        });
    }

    private static SyncResourceSpecification scriptBlockReferenceResource() {
        String uri = "gecko://reference/script-blocks";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Control Script Block Reference")
                .description("Language specification, built-in variables, math functions, and syntax rules for GeckoCIRCUITS ScriptBlockCalculator (typ 61)")
                .mimeType("text/markdown")
                .build();

        String doc = """
                # GeckoCIRCUITS ScriptBlockCalculator (typ 61) Reference Manual
                
                The `ScriptBlockCalculator` is a high-speed interpreted DSP microcontroller block running synchronously inside the simulation loop at timestep `dt`.
                
                ## Simulation Variables
                - `t`: Current simulation time in seconds (double)
                - `dt`: Simulation timestep in seconds (double, e.g. 1.0e-6)
                - `PI`: Math constant 3.141592653589793
                
                ## Input & Output Arrays
                - `xIN[0 .. anzXIN - 1]`: Array of measured input signals from voltage/current probes
                - `yOUT[0 .. anzYOUT - 1]`: Array of synthesized output signals (gate pulses, telemetry, etc.)
                - The script must conclude with: `return yOUT;`
                
                ## State Variables (<staticVariables>)
                Variables declared in `<staticVariables>` retain their values across simulation timesteps (integrator states, duty cycles, step counters):
                ```java
                <staticVariables>
                int step_pfc = 0;
                double v_int = 0.0;
                <\\staticVariables>
                ```
                
                ## Mathematical Operators & Functions
                - Arithmetic: `+`, `-`, `*`, `/`, `%` (modulo), `^` (power)
                - Relational: `<`, `<=`, `>`, `>=`, `==`, `!=`
                - Logical: `&&`, `||`, `!`
                - Ternary Conditional: `(condition) ? (then_val) : (else_val)`
                - Math functions:
                  - `sin(x)`, `cos(x)`, `tan(x)`
                  - `asin(x)`, `acos(x)`, `atan(x)`, `atan2(y, x)`
                  - `abs(x)`, `sqrt(x)`, `exp(x)`, `log(x)`
                  - `pow(x, y)`, `min(x, y)`, `max(x, y)`
                
                ## Safe Division Best Practices
                Because boolean operators evaluate sub-expressions, always guard denominators using ternary operators:
                ```java
                double v_safe = (v_total > 50.0) ? v_total : 750.0;
                double duty = v_target / v_safe;
                ```
                """;

        return new SyncResourceSpecification(resource, (exchange, req) ->
                textResult(uri, "text/markdown", doc)
        );
    }

    private static SyncResourceSpecification controlBlocksReferenceResource() {
        String uri = "gecko://reference/control-blocks";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Native Control Block Reference")
                .description("Executable control blocks (typ 1000-1016) with parameter slots, terminal geometry, "
                        + "wiring rules and the canonical bang-bang regulated buck pattern")
                .mimeType("text/markdown")
                .build();

        String doc = """
                # Native Control Blocks (web catalog, typ 1000-1016)

                The headless engine executes these blocks natively. Chain them with CONTROL wires;
                combine with SCRIPT_BLOCK (1016) for anything more complex than the blocks below.

                ## Block Reference

                | Block | typ | inputs | outputs | parameter slots |
                |-------|-----|--------|---------|-----------------|
                | GATE          | 1000 | 1 (signal)     | 0 | couples to the target switch by name/uid |
                | VOLTMETER     | 1001 | 0              | 1 | couples to a power component or nodeA/nodeB label pair |
                | AMMETER       | 1002 | 0              | 1 | couples to a branch component |
                | SCOPE         | 1003 | 1..N           | 0 | display-only, skipped headlessly |
                | SIGNAL_SOURCE | 1004 | 0              | 1 | 0=waveform (402 sin, 403 triangle, 404 rectangle, 405 random), 1=amplitude, 2=freq Hz, 3=offset, 4=phase rad, 5=duty |
                | CONSTANT      | 1005 | 0              | 1 | 0=value |
                | GAIN          | 1006 | 1              | 1 | 0=k |
                | PI            | 1007 | 1              | 1 | 0=Kp, 1=Ti [s] (series form y = Kp*(u + 1/Ti*integral(u))); Ti<=0 -> P-only |
                | PT1           | 1008 | 1              | 1 | 0=tau [s], unity DC gain |
                | INTEGRATOR    | 1009 | 1 (+reset)     | 1 | 0=initial value; input 2 = reset (>=1 resets, unwired = off) |
                | COMPARATOR    | 1010 | 2 (plus,minus) | 1 | out = 1 while plus > minus |
                | AND           | 1011 | 2              | 1 | 1 iff both inputs > 0.5 |
                | OR            | 1012 | 2              | 1 | 1 iff any input > 0.5 |
                | NOT           | 1013 | 1              | 1 | 1 while input <= 0.5 |
                | SELECTOR      | 1014 | 3 (sel,in0,in1)| 1 | sel > 0.5 passes in1, else in0 |
                | DELAY         | 1015 | 1              | 1 | 0=tau [s] transport delay |
                | SCRIPT_BLOCK  | 1016 | anzXIN         | anzYOUT | source code, static variables, see script-blocks reference |
                | JAVA_BLOCK    | 61   | anzXIN         | anzYOUT | same language as 1016 (classic) |

                Legacy classic-editor numbers 1..6 map to 1001,1002,1005,1004,1003,1000 respectively.

                ## Terminal Geometry (NORTH_SOUTH orientation 503)

                - Input terminal i sits at grid point (x - 2, y + i).
                - Output terminal j sits at grid point (x + 2, y + j).
                - Route CONTROL wires between exactly those points; keep the polylines of
                  different signal wires disjoint (no shared grid points).
                - A labeled output (labelEndKnoten) names a recordable signal channel.

                ## Canonical Pattern: Bang-Bang Regulated Buck

                Power stage: U_DC 12 V -> IDEAL_SWITCH -> node 'sw' -> L 100uH -> node 'vout',
                freewheel DIODE from 'sw' to ground, C 100uF + R load from 'vout' to ground.

                Control chain (all blocks orientation 503):
                1. CONSTANT 'REF' at (14,40), value = 6.0, output labeled 'vref'.
                2. VOLTMETER 'VOLT' at (14,34) coupled to the load resistor, output labeled 'vout'.
                3. COMPARATOR 'CMP' at (24,36): input 0 (plus) <- REF, input 1 (minus) <- VOLT.
                   Wire A: (16,40) -> (20,40) -> (20,36) -> (22,36).
                   Wire B: (16,34) -> (18,34) -> (18,37) -> (22,37).
                4. GATE 'GATE' at (34,36) coupled to the switch; wire (26,36) -> (32,36).

                The loop switches the transistor while vout < ref: the output regulates to the
                reference (bang-bang / sliding-mode control). For a PWM pattern replace the
                comparator input with a PI output versus a triangle SIGNAL_SOURCE carrier.
                """;

        return new SyncResourceSpecification(resource, (exchange, req) ->
                textResult(uri, "text/markdown", doc)
        );
    }

    private static SyncResourceSpecification workflowReferenceResource() {
        String uri = "gecko://reference/workflow";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Project Authoring Workflow")
                .description("Step-by-step workflow for building, validating, simulating and refining "
                        + "GeckoCIRCUITS projects through the MCP tools, with complete JSON cookbook payloads")
                .mimeType("text/markdown")
                .build();

        String doc = """
                # GeckoCIRCUITS Project Authoring Workflow

                Everything needed to build and simulate projects lives in the MCP tools and
                resources - no access to the GeckoCIRCUITS source code is required.

                ## Tool Chain

                1. `gecko_catalog` (or resource `gecko://catalog/components`) - the authoritative
                   component list: type numbers, pins, parameter names/units/defaults/slots,
                   orientation codes, wiring rules, signal naming.
                2. `gecko_create_circuit` - synthesize a .ipes project from a JSON netlist.
                3. `gecko_validate_circuit` - DRC lint + real netlist/control compilation
                   (use `level: "compile"`, the default, to catch wiring/label errors before simulating).
                4. `gecko_list_signals` - enumerate recordable channels (probes, labels, losses, Tj_/Phi_).
                5. `gecko_simulate` - run; returns metadata (losses, max junction temperature, peak flux).
                6. `gecko_get_waveforms` - downsampled time series + per-signal statistics.
                7. `gecko_measure_metrics` - RMS/ripple/power factor/efficiency figures of merit.
                8. `gecko_patch_component` / `gecko_set_script_code` - edit parameters or script code.
                9. Iterate: patch -> validate -> simulate. Use `parameter_overrides` on the
                   simulate family for sweeps without editing the file.

                ## Cookbook 1: RLC Divider (minimal project)

                ```json
                {
                  "output_path": "resources/projects/rlc.ipes",
                  "simulation": {"duration": 0.05, "dt": 1e-6, "solver": 0},
                  "components": [
                    {"name": "V_IN", "type": "DC_VOLTAGE", "nodes": ["n_in", "0"], "parameters": {"voltage": 24.0}},
                    {"name": "R_SERIES", "type": "RESISTOR", "nodes": ["n_in", "n_mid"], "parameters": {"resistance": 2.0}},
                    {"name": "L_FILTER", "type": "INDUCTOR", "nodes": ["n_mid", "n_out"], "parameters": {"inductance": 1e-3}},
                    {"name": "C_FILTER", "type": "CAPACITOR", "nodes": ["n_out", "0"], "parameters": {"capacitance": 100e-6}},
                    {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["n_out", "0"], "parameters": {"resistance": 10.0}}
                  ],
                  "control": {"probes": [
                    {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "u_out"}
                  ]}
                }
                ```
                Expected: u_out settles at 24 V * 10/12 = 20 V.

                ## Cookbook 2: Closed-Loop Buck (native control blocks)

                Power stage: DC source 12 V -> IDEAL_SWITCH 'S1' -> 'sw' -> INDUCTOR 100uH -> 'vout';
                DIODE anode '0' cathode 'sw'; CAPACITOR 100uF and RESISTOR 6 Ohm from 'vout' to '0'.

                ```json
                "control": {
                  "probes": [
                    {"name": "VOLT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "vout"}
                  ],
                  "blocks": [
                    {"type": "CONSTANT",   "name": "REF", "parameters": {"value": 6.0}, "output_label": "vref"},
                    {"type": "COMPARATOR", "name": "CMP", "output_label": "pwm"}
                  ],
                  "wires": [
                    {"from": "REF",  "to": "CMP", "to_input": 0},
                    {"from": "VOLT", "to": "CMP", "to_input": 1}
                  ],
                  "gates": [
                    {"name": "GATE", "target_switch": "S1", "in_signal": "pwm"}
                  ]
                }
                ```
                The comparator drives the switch while vout < 6 V: the output regulates to the
                reference (see gecko://reference/control-blocks for the geometry rules).

                ## Cookbook 3: Diode Rectifier with Thermal Network

                Place TH_RTH (K/W) and TH_CTH (J/K) components between thermal node labels exactly
                like power components, inject losses with TH_FLOW (W) and pin the reference with
                TH_AMBIENT. Couple semiconductor losses to junction temperatures per RUN (not in the
                file) via `gecko_simulate`'s `thermal` block:

                ```json
                "thermal": {
                  "ambient_temperature": 25.0,
                  "couplings": [
                    {"device": "D.1", "kind": "diode",
                     "model": {"kind": "foster", "r_th": [1.5, 0.8], "tau": [0.002, 0.05]},
                     "forward_voltage_slope": -0.002}
                  ]
                }
                ```
                The run logs `Tj_D.1` and evaluates the diode's forward voltage at the junction
                temperature. See gecko://reference/multi-domain for the full schema.

                ## Practical Rules

                - Always label the node '0' (or 'gnd') on one side of every source; every isolated
                  subcircuit needs exactly one ground.
                - Give every probe output a `signal_name`; record channels via the `signals` list
                  or let the engine auto-resolve.
                - Keep wire polylines of different nets disjoint (no shared grid points).
                - Transformers are 4-terminal: nodes order [p1, p2, s1, s2].
                - Motors ship with `parameters_raw` presets; copy slot vectors from datasheets.
                """;

        return new SyncResourceSpecification(resource, (exchange, req) ->
                textResult(uri, "text/markdown", doc)
        );
    }

    private static SyncResourceSpecification multiDomainReferenceResource() {
        String uri = "gecko://reference/multi-domain";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Multi-Domain Simulation Reference")
                .description("Thermal, magnetic and loss-domain configuration for the simulate tool family: "
                        + "electro-thermal feedback, reluctance networks, and the resulting signal channels")
                .mimeType("text/markdown")
                .build();

        String doc = """
                # Multi-Domain Simulation (thermal / magnetic / losses)

                The simulate tool family (`gecko_simulate`, `gecko_get_waveforms`,
                `gecko_measure_metrics`) accepts optional per-run domain configuration mapped
                directly onto the engine's SimulationConfig.

                ## Electro-Thermal Feedback

                ```json
                "thermal": {
                  "ambient_temperature": 25.0,
                  "couplings": [
                    {"device": "R.1", "kind": "resistor",
                     "model": {"kind": "cauer", "r_th": [2.0], "c_th": [0.005]},
                     "temperature_coefficient": 0.0039},
                    {"device": "D.1", "kind": "diode",
                     "model": {"kind": "foster", "r_th": [1.5, 0.8], "tau": [0.002, 0.05]},
                     "forward_voltage_slope": -0.002}
                  ]
                }
                ```

                - `device` names the LK component (R.1, D.1, ...) exactly as placed.
                - `kind` selects the feedback law: `resistor` evaluates R(T) = R0*(1+alpha*(T-T0))
                  with `temperature_coefficient` alpha; `diode` evaluates the forward voltage
                  Vf(T) = Vf0 + slope*(T-T0) and the thermal voltage k_B*(T+273.15)/q.
                - `model` is a Foster ladder (`r_th` + `tau` time constants) or Cauer ladder
                  (`r_th` + `c_th`); Foster models are converted to Cauer automatically.
                - The coupled device dissipates its electrical loss into the thermal network and
                  evaluates its temperature-dependent parameters at the junction temperature.
                - Channels: `Tj_<device>` logs the junction temperature; run metadata reports
                  `maxJunctionTemperature`.

                ## Thermal Networks in the Circuit File

                Heat networks can also be authored as .ipes components (domain THERM):
                TH_RTH (thermal resistance, K/W), TH_CTH (heat capacity, J/K, slot 1 = initial
                temperature), TH_FLOW (constant heat source, W), TH_TEMP (temperature boundary,
                degC) and TH_AMBIENT (passive 0-degree reference). They behave exactly like an
                MNA circuit with temperature as node potential.

                ## Magnetic Domain (reluctance networks)

                ```json
                "magnetic": {
                  "networks": [{
                    "windings": [
                      {"name": "L1", "turns": 50, "node_a": 1, "node_b": 0}
                    ],
                    "branches": [
                      {"name": "core",  "node_a": 1, "node_b": 0,
                       "nonlinear": {"curve": "tanh", "permeance": 1e-3, "saturation_flux": 1.5e-2}}
                    ]
                  }]
                }
                ```

                - A winding binds BY NAME to a power-domain INDUCTOR (LK_L); each step the engine
                  feeds the winding current into the magnetic network and writes the differential
                  inductance back into the inductor, so saturation appears in the electrical waveforms.
                - Branch types: linear `reluctance` (A-turns/Wb, e.g. air gap l/(mu0*A)),
                  or `nonlinear` with curves `froelich`, `arctangent`, `tanh`, `piecewise_linear`
                  (parameters: unsaturated `permeance` P0 [H], `saturation_flux` [Wb], optional
                  saturation permeability `ratio`).
                - `node_a`/`node_b` are magnetic node indices (0 = reference); windings are MMF
                  sources F = N*i between their nodes.
                - Channels: `Phi_<winding>` logs the winding flux; metadata reports `peakFlux`.

                ## Loss Channels

                Every semiconductor gets conduction/switching loss models automatically:
                per-device `P_loss_<name>`, `P_cond_<name>`, `P_sw_<name>`; totals
                `P_loss_total`, `P_cond_total`, `P_sw_total`, `E_loss_total`.
                The loss engine accepts the ambient temperature from `thermal.ambient_temperature`.
                """;

        return new SyncResourceSpecification(resource, (exchange, req) ->
                textResult(uri, "text/markdown", doc)
        );
    }

    private static SyncResourceSpecification topologiesReferenceResource() {
        String uri = "gecko://reference/topologies";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Power Topologies Guide")
                .description("Design guidelines and equations for standard power converter topologies (Vienna Rectifier, Interleaved PFC, LLC Resonant, Buck)")
                .mimeType("text/markdown")
                .build();

        String doc = """
                # Power Converter Topologies Reference Guide
                
                ## 1. 3-Phase 3-Level Vienna Rectifier (Active PFC)
                - **Input**: 3-phase AC ($230\\text{ V}_{\\text{RMS}}$ phase, $400\\text{ V}_{\\text{RMS}}$ line-to-line)
                - **DC Link**: Split DC bus $V_{dc} = 750\\text{ V}$ ($2 \\times 375\\text{ V}$)
                - **Modulation Index**: $M = \\frac{2 \\hat{U}_{ph}}{V_{dc}} = \\frac{2 \\times 325.27}{750} = 0.8674$
                - **Feedforward Duty Cycle**: $d_k(t) = 1.0 - M |\\sin_k(\\omega t)|$
                - **Current Feedback**: $d_k(t) = 1.0 - M |\\sin_k| + \\text{sgn}(\\sin_k) \\cdot K_p (i_{ref,k} - i_{meas,k})$
                - **Nominal Peak Current**: $I_{pk} = \\frac{P_{out}}{3 V_{ph,rms}} \\sqrt{2}$ (e.g. $22.55\\text{ A}$ for $11\\text{ kW}$)
                
                ## 2. DC-DC Buck Step-Down Converter
                - **Transfer Ratio**: $V_{out} = D \\cdot V_{in}$
                - **Nominal Duty**: $D = \\frac{V_{out}}{V_{in}}$ (e.g. $49.5 / 750 = 0.0660$)
                - **Inductor Ripple**: $\\Delta I_L = \\frac{(V_{in} - V_{out}) D}{f_{sw} L}$
                - **Hard Voltage Limit**: Clamp $D \\le \\frac{V_{max}}{V_{in}}$ to strictly enforce overvoltage safety.
                
                ## 3. Half-Bridge LLC Resonant Converter
                - **Resonant Frequency**: $f_0 = \\frac{1}{2 \\pi \\sqrt{L_r C_r}}$
                - **Characteristic Impedance**: $Z_0 = \\sqrt{L_r / C_r}$
                - **ZVS Operation**: Frequency modulated around $f_0$; zero voltage switching achieved via magnetizing current $I_m$ discharging $C_{oss}$ during dead time.
                """;

        return new SyncResourceSpecification(resource, (exchange, req) ->
                textResult(uri, "text/markdown", doc)
        );
    }

    private static SyncResourceSpecification examplesListResource() {
        String uri = "gecko://examples/list";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Workspace Example Circuits")
                .description("Index of available example .ipes circuit models in the repository")
                .mimeType("application/json")
                .build();

        return new SyncResourceSpecification(resource, (exchange, req) -> {
            try {
                Path root = IpesSupport.workspaceRoot();
                Path examplesDir = root.resolve("resources/examples");
                List<Map<String, Object>> examples = new ArrayList<>();

                if (Files.exists(examplesDir)) {
                    try (Stream<Path> stream = Files.walk(examplesDir)) {
                        stream.filter(p -> p.toString().endsWith(".ipes")).forEach(p -> {
                            Map<String, Object> entry = new LinkedHashMap<>();
                            entry.put("path", root.relativize(p).toString().replace('\\', '/'));
                            entry.put("name", p.getFileName().toString().replace(".ipes", ""));
                            try {
                                entry.put("size_bytes", Files.size(p));
                            } catch (IOException ignored) {}
                            examples.add(entry);
                        });
                    }
                }

                String text = JSON.writeValueAsString(examples);
                return textResult(uri, "application/json", text);
            } catch (Exception e) {
                return textResult(uri, "text/plain", "Error listing examples: " + e.getMessage());
            }
        });
    }
}
