package gecko.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The 10 GeckoCIRCUITS tools, ported 1:1 from the Python server (same names,
 * parameters, and result shapes). Handlers return plain Maps/Lists; the MCP
 * layer serializes them to JSON.
 */
final class GeckoTools {

    /** One MCP tool: name, description, JSON schema, and the handler. */
    record ToolSpec(String name, String description, Map<String, Object> inputSchema,
                    ToolHandler handler) {

        /** Handlers may throw; the MCP layer converts that to an error result. */
        interface ToolHandler {
            Object apply(Map<String, Object> args) throws Exception;
        }
    }

    private GeckoTools() {
    }

    static List<ToolSpec> all() {
        return List.of(
                serverStatus(), catalog(), createCircuit(), validateCircuit(), measureMetrics(),
                setupPfc(), setupLlc(), inspectCircuit(),
                patchComponent(), setScriptCode(), simulate(), getWaveforms(), tunePfc());
    }

    // ---------- schemas ----------

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map<String, Object> str(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> num(String description) {
        return Map.of("type", "number", "description", description);
    }

    private static Map<String, Object> bool(String description) {
        return Map.of("type", "boolean", "description", description);
    }

    private static Map<String, Object> arrayOf(String description, String itemType) {
        return Map.of("type", "array", "description", description,
                "items", Map.of("type", itemType));
    }

    private static Map<String, Object> objectOf(String description, Map<String, Object> properties) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "object");
        map.put("description", description);
        if (properties != null) {
            map.put("properties", properties);
        }
        return map;
    }

    private static Map<String, Object> properties(Map<String, Object>... entries) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map<String, Object> entry : entries) {
            properties.putAll(entry);
        }
        return properties;
    }

    // ---------- tools ----------

    private static ToolSpec serverStatus() {
        return new ToolSpec("gecko_server_status",
                "Check GeckoCIRCUITS simulation engine and Java environment status. "
                        + "Returns detected Java version, jar locations, and engine capabilities.",
                objectSchema(properties(), null),
                args -> {
                    Path guiJar = Path.of("src", "modules", "gecko-gui", "target",
                            "gecko-1.0-jar-with-dependencies.jar");
                    Map<String, Object> status = new LinkedHashMap<>();
                    status.put("status", "READY");
                    status.put("platform", System.getProperty("os.name"));
                    status.put("java_version", System.getProperty("java.version"));
                    status.put("java_home", System.getProperty("java.home"));
                    status.put("workspace_root", IpesSupport.workspaceRoot().toString());
                    status.put("gui_jar_exists", Files.exists(guiJar));
                    status.put("gui_jar_path", guiJar.toAbsolutePath().toString());
                    status.put("rest_api_url", "http://localhost:8080/gecko/api/health");
                    status.put("rest_api_status",
                            "Direct Headless Engine (bundled; REST not required)");
                    status.put("capabilities", List.of(
                            "headless_simulation", "microcontroller_script_blocks",
                            "active_interleaved_pfc", "dynamic_load_simulation",
                            "fast_waveform_analysis", "closed_loop_tuning"));
                    return status;
                });
    }

    private static ToolSpec catalog() {
        return new ToolSpec("gecko_catalog",
                "Get the complete, authoritative catalog of all GeckoCIRCUITS power and control components, "
                        + "terminal pinouts, typed parameter definitions, units, defaults, and .ipes slot mappings.",
                objectSchema(properties(), null),
                args -> ComponentCatalog.toCatalogJson());
    }

    private static ToolSpec createCircuit() {
        return new ToolSpec("gecko_create_circuit",
                "Synthesize a new GeckoCIRCUITS (.ipes) model from a high-level schematic netlist. "
                        + "Supports N-terminal components (e.g. the 3-phase PMSM_MOTOR): the 'nodes' array "
                        + "must match the component's catalog pins, split into input and output sides. "
                        + "Automatically computes collision-free 2D schematic layouts, places components, "
                        + "routes net connections, wires control probes and gates, and normalizes companion "
                        + "parameter slots for MNA numerical stability. Expert components without documented "
                        + "parameter names accept 'parameters_raw' (numeric slot vector).",
                objectSchema(properties(
                        Map.of("output_path", str("Output .ipes path relative to workspace (e.g. resources/projects/my_circuit.ipes)")),
                        Map.of("simulation", objectOf("Simulation parameters", properties(
                                Map.of("duration", num("Simulation duration in seconds (default 0.05)")),
                                Map.of("dt", num("Simulation timestep in seconds (default 1e-6)")),
                                Map.of("solver", num("Solver algorithm: 0=Backward Euler (BE), 1=Trapezoidal (TRZ), 2=Gear-Shichman (GS)"))
                        ))),
                        Map.of("components", arrayOf("List of power circuit components to place and connect", "object")),
                        Map.of("control", objectOf("Control domain definitions (probes, microcontroller script blocks, gates)", properties(
                                Map.of("probes", arrayOf("Measurement probes (VOLTMETER, AMMETER) coupled to power components", "object")),
                                Map.of("script_blocks", arrayOf("Microcontroller script blocks (PI loops, PWM generators)", "object")),
                                Map.of("gates", arrayOf("Gate drivers coupling control signals to semiconductor switches", "object"))
                        )))),
                        List.of("output_path", "components")),
                args -> CircuitBuilder.create(args));
    }

    private static ToolSpec validateCircuit() {
        return new ToolSpec("gecko_validate_circuit",
                "Design Rule Checker (DRC) and static circuit linter. Verifies circuit netlist and control "
                        + "graph before running simulation: checks ground reference, floating nodes, short-circuited "
                        + "components, dangling probes/gates, and microcontroller script block syntax/divide-by-zero risks.",
                objectSchema(properties(
                        Map.of("circuit_path", str("Path to the .ipes file to validate")),
                        Map.of("content", str("Optional raw .ipes content string to lint without reading from disk"))),
                        null),
                args -> {
                    String content = strOrNull(args, "content");
                    if (content != null && !content.isBlank()) {
                        return CircuitValidator.validateContent(content, "inline_circuit");
                    }
                    String pathStr = str_(args, "circuit_path", null);
                    if (pathStr == null || pathStr.isBlank()) {
                        throw new IllegalArgumentException("Either circuit_path or content must be provided");
                    }
                    Path path = IpesSupport.resolve(pathStr);
                    if (!Files.exists(path)) {
                        throw new IllegalArgumentException("Circuit file not found: " + path);
                    }
                    return CircuitValidator.validate(path);
                });
    }

    private static ToolSpec measureMetrics() {
        return new ToolSpec("gecko_measure_metrics",
                "Compute comprehensive power electronics converter figures of merit (RMS, peak-to-peak ripple %, "
                        + "average active power, apparent power, power factor, and conversion efficiency) directly "
                        + "on simulation results without streaming massive raw time-series over stdio.",
                objectSchema(properties(
                        Map.of("circuit_path", str("Path to the .ipes file")),
                        Map.of("duration", num("Simulation duration (s); default 20e-3")),
                        Map.of("dt", num("Time step (s); default 1e-6")),
                        Map.of("start_time", num("Start time for steady-state analysis window (s); default last 50%")),
                        Map.of("signals", arrayOf("Signal names to calculate stats for; default all", "string")),
                        Map.of("v_in_signal", str("Input voltage probe signal name (e.g. uIN, v_grid)")),
                        Map.of("i_in_signal", str("Input current probe signal name (e.g. iIN, i_grid)")),
                        Map.of("v_out_signal", str("Output voltage probe signal name (e.g. uOUT, v_dc)")),
                        Map.of("i_out_signal", str("Output load current probe signal name (e.g. iOUT, i_load)")),
                        Map.of("is_three_phase", bool("Set true if input is 3-phase AC; scales 1-phase reference power by 3"))),
                        List.of("circuit_path")),
                args -> {
                    Path path = IpesSupport.resolve(str_(args, "circuit_path", null));
                    if (!Files.exists(path)) {
                        throw new IllegalArgumentException("Circuit file not found: " + path);
                    }
                    Double duration = optionalDouble(args, "duration");
                    if (duration == null) {
                        duration = optionalDouble(args, "simulation_time");
                    }
                    Double dt = optionalDouble(args, "dt");
                    SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(path, duration, dt);

                    double startTime = num_(args, "start_time", -1.0);
                    List<String> signals = null;
                    if (args.get("signals") instanceof List<?> list) {
                        signals = list.stream().map(String::valueOf).toList();
                    }
                    String vIn = strOrNull(args, "v_in_signal");
                    String iIn = strOrNull(args, "i_in_signal");
                    String vOut = strOrNull(args, "v_out_signal");
                    String iOut = strOrNull(args, "i_out_signal");
                    boolean threePhase = Boolean.TRUE.equals(args.get("is_three_phase"))
                            || "true".equalsIgnoreCase(String.valueOf(args.get("is_three_phase")));

                    return PowerMetricsCalculator.calculate(csv, startTime, signals, vIn, iIn, vOut, iOut, threePhase);
                });
    }

    private static ToolSpec setupPfc() {
        return new ToolSpec("gecko_setup_pfc_project",
                "Setup an authentic Active Interleaved PFC Converter project (.ipes file) for "
                        + "GeckoCIRCUITS with mains rectifier, 2-phase interleaved boost, dynamic load "
                        + "step, and a microcontroller script block (PI loop + interleaved PWM).",
                objectSchema(properties(
                        Map.of("output_path", str("Output .ipes path (relative to workspace)")),
                        Map.of("target_voltage", num("DC target voltage (V)")),
                        Map.of("v_rms", num("Grid RMS voltage (V)")),
                        Map.of("f_grid", num("Grid frequency (Hz)")),
                        Map.of("f_sw", num("Switching frequency (Hz)")),
                        Map.of("inductance", num("Boost inductance per phase (H)")),
                        Map.of("capacitance", num("DC bus capacitance (F)")),
                        Map.of("r_load_base", num("Base load resistance (Ohm)")),
                        Map.of("r_load_step", num("Stepped load resistance (Ohm)")),
                        Map.of("t_step", num("Load step time (s)")),
                        Map.of("duration", num("Simulation duration (s)")),
                        Map.of("dt", num("Simulation time step (s)"))), null),
                args -> PfcProject.setup(
                        str_(args, "output_path", "resources/projects/interleaved_pfc_50v.ipes"),
                        new PfcProject.Params(
                                num_(args, "target_voltage", 50.0),
                                num_(args, "v_rms", 24.0),
                                num_(args, "f_grid", 50.0),
                                num_(args, "f_sw", 20000.0),
                                num_(args, "inductance", 0.0008),
                                num_(args, "capacitance", 0.0068),
                                num_(args, "r_load_base", 25.0),
                                num_(args, "r_load_step", 25.0),
                                num_(args, "t_step", 0.05),
                                num_(args, "duration", 0.10),
                                num_(args, "dt", 1e-6))));
    }

    private static ToolSpec setupLlc() {
        return new ToolSpec("gecko_setup_llc_project",
                "Setup an authentic Half-Bridge LLC Resonant Converter project (.ipes file), "
                        + "scalable from 0 to 10 kW (400 V DC input), with ZVS snubber, resonant tank "
                        + "analytics (f0, Z0, Q, k), and a microcontroller PWM script block with dead time.",
                objectSchema(properties(
                        Map.of("output_path", str("Output .ipes path (relative to workspace)")),
                        Map.of("v_in", num("DC input voltage (V)")),
                        Map.of("v_out", num("Output voltage (V)")),
                        Map.of("p_out", num("Output power (W)")),
                        Map.of("f_sw", num("Switching frequency (Hz)")),
                        Map.of("l_r", num("Resonant inductance (H)")),
                        Map.of("c_r", num("Resonant capacitance (F)")),
                        Map.of("l_m", num("Magnetizing inductance (H)")),
                        Map.of("t_dead", num("Gate dead time (s)")),
                        Map.of("r_load", num("Load resistance (Ohm); default derived from v_out/p_out")),
                        Map.of("c_out", num("Output capacitance (F); default by power class")),
                        Map.of("duration", num("Simulation duration (s)")),
                        Map.of("dt", num("Simulation time step (s)"))), null),
                args -> LlcProject.setup(
                        str_(args, "output_path", "resources/projects/llc_resonant_400v_24v.ipes"),
                        new LlcProject.Params(
                                num_(args, "v_in", 400.0),
                                num_(args, "v_out", 200.0),
                                num_(args, "p_out", 10000.0),
                                num_(args, "f_sw", 100000.0),
                                num_(args, "l_r", 2.2e-6),
                                num_(args, "c_r", 1.15e-6),
                                num_(args, "l_m", 1.1e-5),
                                num_(args, "t_dead", 1.8e-7),
                                args.get("r_load") instanceof Number n ? (Double) n.doubleValue() : null,
                                args.get("c_out") instanceof Number n2 ? (Double) n2.doubleValue() : null,
                                num_(args, "duration", 0.0003),
                                num_(args, "dt", 2e-8),
                                num_(args, "r_on", 0.002),
                                num_(args, "c_oss", 1.5e-9))));
    }

    private static ToolSpec inspectCircuit() {
        return new ToolSpec("gecko_inspect_circuit",
                "Inspect a GeckoCIRCUITS .ipes circuit file. Returns simulation parameters, power "
                        + "components, control blocks, script code, and signals.",
                objectSchema(properties(Map.of("circuit_path", str("Path to the .ipes file"))),
                        List.of("circuit_path")),
                args -> {
                    Path path = IpesSupport.resolve(str_(args, "circuit_path", null));
                    if (!Files.exists(path)) {
                        throw new IllegalArgumentException("Circuit file not found: " + path);
                    }
                    return CircuitInspector.inspect(path);
                });
    }

    private static ToolSpec patchComponent() {
        return new ToolSpec("gecko_patch_component",
                "Patch parameters of a specific component in a .ipes circuit file. Example: "
                        + "{\"param0\": 25.0} to set resistance of R.1 to 25 Ohms.",
                objectSchema(properties(
                        Map.of("circuit_path", str("Path to the .ipes file")),
                        Map.of("component_name", str("Component name, e.g. R.1")),
                        Map.of("parameters", Map.of("type", "object",
                                "description", "Parameter updates, e.g. {\"param0\": 25.0}")),
                        Map.of("output_path", str("Optional output path; default overwrites input"))),
                        List.of("circuit_path", "component_name", "parameters")),
                args -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) args.get("parameters");
                    return CircuitPatcher.patchComponent(
                            str_(args, "circuit_path", null),
                            str_(args, "component_name", null),
                            parameters != null ? parameters : Map.of(),
                            strOrNull(args, "output_path"));
                });
    }

    private static ToolSpec setScriptCode() {
        return new ToolSpec("gecko_set_script_code",
                "Update microcontroller source code and state variables in a CTRL_SCRIPT or "
                        + "JAVA_FUNCTION block.",
                objectSchema(properties(
                        Map.of("circuit_path", str("Path to the .ipes file")),
                        Map.of("block_name", str("Script block name, e.g. CTRL_MCU")),
                        Map.of("source_code", str("New script source code")),
                        Map.of("static_variables", str("New static variables code")),
                        Map.of("static_code", str("New static initialization code")),
                        Map.of("output_path", str("Optional output path; default overwrites input"))),
                        List.of("circuit_path", "block_name", "source_code")),
                args -> CircuitPatcher.setScriptCode(
                        str_(args, "circuit_path", null),
                        str_(args, "block_name", null),
                        str_(args, "source_code", ""),
                        str_(args, "static_variables", ""),
                        str_(args, "static_code", ""),
                        strOrNull(args, "output_path")));
    }

    private static ToolSpec simulate() {
        return new ToolSpec("gecko_simulate",
                "Run headless simulation of a GeckoCIRCUITS .ipes circuit file. Returns status, "
                        + "step count, signal names, and wall-clock execution time.",
                objectSchema(properties(
                        Map.of("circuit_path", str("Path to the .ipes file")),
                        Map.of("duration", num("Simulation duration (s); default 20e-3")),
                        Map.of("dt", num("Time step (s); default 1e-6")),
                        Map.of("solver", str("Solver: be | trz | gs"))),
                        List.of("circuit_path")),
                args -> {
                    Path path = IpesSupport.resolve(str_(args, "circuit_path", null));
                    if (!Files.exists(path)) {
                        throw new IllegalArgumentException("Circuit file not found: " + path);
                    }
                    Double duration = optionalDouble(args, "duration");
                    if (duration == null) {
                        duration = optionalDouble(args, "simulation_time");
                    }
                    SimulationService.RunResult run = SimulationService.simulate(path,
                            duration, optionalDouble(args, "dt"),
                            str_(args, "solver", "be"));
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("status", "COMPLETED");
                    result.put("total_steps", run.totalSteps());
                    result.put("signal_names", run.signalNames());
                    result.put("execution_time_ms", run.executionTimeMs());
                    return result;
                });
    }

    private static ToolSpec getWaveforms() {
        return new ToolSpec("gecko_get_waveforms",
                "Run simulation and retrieve time-series waveforms along with key power electronics "
                        + "metrics: steady-state DC voltage, peak-to-peak ripple, RMS values, power "
                        + "factor, and LLC ZVS detection.",
                objectSchema(properties(
                        Map.of("circuit_path", str("Path to the .ipes file")),
                        Map.of("duration", num("Simulation duration (s); default 20e-3")),
                        Map.of("dt", num("Time step (s); default 1e-6")),
                        Map.of("signals", arrayOf("Signal names to return; default all", "string")),
                        Map.of("max_points", num("Downsampling target (default 2000)"))),
                        List.of("circuit_path")),
                args -> {
                    Path path = IpesSupport.resolve(str_(args, "circuit_path", null));
                    if (!Files.exists(path)) {
                        throw new IllegalArgumentException("Circuit file not found: " + path);
                    }
                    List<String> signals = null;
                    if (args.get("signals") instanceof List<?> list) {
                        signals = list.stream().map(String::valueOf).toList();
                    }
                    int maxPoints = (int) num_(args, "max_points", 2000);
                    Double duration = optionalDouble(args, "duration");
                    if (duration == null) {
                        duration = optionalDouble(args, "simulation_time");
                    }
                    SimulationService.ParsedCsv csv =
                            SimulationService.simulateToCsv(path, duration,
                                    optionalDouble(args, "dt"));
                    return WaveformAnalysis.analyse(csv, safeRead(path), signals, maxPoints);
                });
    }

    private static ToolSpec tunePfc() {
        return new ToolSpec("gecko_tune_pfc",
                "Evaluate and tune active PFC controller PI gains (Kp, Ki) on the given circuit. "
                        + "Simulates, checks DC regulation against target_voltage, analyzes ripple, "
                        + "and returns a tuning evaluation report.",
                objectSchema(properties(
                        Map.of("circuit_path", str("Path to the .ipes file")),
                        Map.of("target_voltage", num("Target DC voltage (V); default 50")),
                        Map.of("kp", num("Proportional gain (analysis hint)")),
                        Map.of("ki", num("Integral gain (analysis hint)")),
                        Map.of("simulation_time", num("Simulation duration (s); default 0.1")),
                        Map.of("dt", num("Time step (s); default 1e-6"))),
                        List.of("circuit_path")),
                args -> {
                    double target = num_(args, "target_voltage", 50.0);
                    Map<String, Object> waveforms = getWaveformsResult(args);
                    Map<String, Object> metrics = castMap(waveforms.get("metrics"));
                    Map<String, Object> vout = castMap(metrics.get("output_voltage"));
                    Map<String, Object> grid = castMap(metrics.get("ac_grid"));

                    double actualV = asDouble(vout.get("steady_state_dc_volts"));
                    double vErr = actualV - target;
                    double ripple = asDouble(vout.get("ripple_peak_to_peak_volts"));
                    double pf = asDouble(grid.get("power_factor"));

                    List<String> recommendation = new java.util.ArrayList<>();
                    if (Math.abs(vErr) > 1.0) {
                        recommendation.add(vErr < 0
                                ? String.format(Locale.ROOT,
                                "Output is %.1fV below target %.1fV: increase Ki or current reference limit.",
                                Math.abs(vErr), target)
                                : String.format(Locale.ROOT,
                                "Output is %.1fV above target %.1fV: decrease duty ceiling or increase load.",
                                vErr, target));
                    } else {
                        recommendation.add(String.format(Locale.ROOT,
                                "Voltage regulation accurate: within %.2fV of target %.1fV.",
                                Math.abs(vErr), target));
                    }
                    if (ripple > 2.0) {
                        recommendation.add(String.format(Locale.ROOT,
                                "Output ripple is %.2fV: consider increasing output capacitance C_out "
                                        + "or increasing switching frequency.", ripple));
                    } else {
                        recommendation.add(String.format(Locale.ROOT,
                                "Output ripple is acceptable (%.2fV).", ripple));
                    }

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("target_voltage_volts", target);
                    result.put("measured_voltage_volts", actualV);
                    result.put("voltage_error_volts", round(vErr, 3));
                    result.put("ripple_pp_volts", ripple);
                    result.put("power_factor", pf);
                    result.put("evaluation", recommendation);
                    result.put("full_metrics", metrics);
                    return result;
                });
    }

    // ---------- helpers ----------

    private static Map<String, Object> getWaveformsResult(Map<String, Object> args) throws IOException {
        Path path = IpesSupport.resolve(str_(args, "circuit_path", null));
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Circuit file not found: " + path);
        }
        int maxPoints = (int) num_(args, "max_points", 2000);
        Double duration = optionalDouble(args, "duration");
        if (duration == null) {
            duration = optionalDouble(args, "simulation_time");
        }

        Path runPath = path;
        Path tempFile = null;
        try {
            Double kp = optionalDouble(args, "kp");
            Double ki = optionalDouble(args, "ki");
            if (kp != null || ki != null) {
                String content = IpesSupport.readIpesText(path);
                if (kp != null) {
                    content = content.replaceAll("(?m)^(\\s*double\\s+Kp_v\\s*=\\s*)[0-9.eE+-]+;",
                            "$1" + PyFormat.pyStr(kp) + ";");
                }
                if (ki != null) {
                    content = content.replaceAll("(?m)^(\\s*double\\s+Ki_v\\s*=\\s*)[0-9.eE+-]+;",
                            "$1" + PyFormat.pyStr(ki) + ";");
                }
                tempFile = Files.createTempFile("gecko-mcp-tune-", ".ipes");
                IpesSupport.writeIpesText(tempFile, content, true);
                runPath = tempFile;
            }

            SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(runPath,
                    duration, optionalDouble(args, "dt"));
            return WaveformAnalysis.analyse(csv, safeRead(runPath), null, maxPoints);
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private static String safeRead(Path path) {
        try {
            return IpesSupport.readIpesText(path);
        } catch (IOException e) {
            return "";
        }
    }

    private static String str_(Map<String, Object> args, String key, String fallback) {
        Object value = args.get(key);
        return value != null ? String.valueOf(value) : fallback;
    }

    private static String strOrNull(Map<String, Object> args, String key) {
        return args.get(key) != null ? String.valueOf(args.get(key)) : null;
    }

    private static double num_(Map<String, Object> args, String key, double fallback) {
        return optionalDouble(args, key) != null ? optionalDouble(args, key) : fallback;
    }

    private static Double optionalDouble(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return null;
    }

    private static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value != null ? (Map<String, Object>) value : Map.of();
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}
