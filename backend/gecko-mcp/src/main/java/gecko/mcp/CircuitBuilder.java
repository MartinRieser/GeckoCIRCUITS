package gecko.mcp;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * High-level circuit synthesis engine for GeckoCIRCUITS.
 *
 * <p>Translates human/LLM-readable JSON circuit definitions into a core
 * {@link CircuitModel} and serializes it through {@link CircuitFileWriter},
 * so generated projects carry the full-fidelity .ipes grammar (gzip, thermal
 * domain, scripter blocks) and round-trip through the engine's parser.</p>
 *
 * <p>Power and thermal components are placed on a collision-free grid with
 * their terminals spread exactly like the classic editor; control blocks use
 * the engine terminal geometry (inputs at rel (-2, -i), outputs at
 * (x + 2, -j) for NORTH_SOUTH) and CONTROL wires are routed orthogonally in
 * disjoint lanes.</p>
 */
public final class CircuitBuilder {

    private CircuitBuilder() {
    }

    /** Grid x where the generic control block columns start. */
    private static final int CONTROL_BLOCK_BASE_X = 40;
    /** Vertical stride of control blocks (tallest block is the 3-input selector). */
    private static final int CONTROL_BLOCK_STRIDE = 5;
    /** Horizontal stride when wrapping control blocks to the next column. */
    private static final int CONTROL_BLOCK_COLUMN_STRIDE = 14;
    /** Lanes of a routed control wire start this far right of its source terminal. */
    private static final int WIRE_LANE_BASE_OFFSET = 2;
    /** Feedback wires (routing leftwards) start this far left of the blocks. */
    private static final int FEEDBACK_LANE_BASE_OFFSET = 4;

    /** Web catalog control type numbers of the specially-placed blocks. */
    private static final int TYP_PROBE_VOLT = CircuitTypCore.CTRL_VOLT.getTypeNumber();
    private static final int TYP_PROBE_AMP = CircuitTypCore.CTRL_AMP.getTypeNumber();
    private static final int TYP_SCRIPT = CircuitTypCore.CTRL_SCRIPT.getTypeNumber();
    private static final int TYP_GATE = CircuitTypCore.CTRL_GATE.getTypeNumber();

    public static Map<String, Object> create(Map<String, Object> request) throws IOException {
        String outputPathStr = (String) request.get("output_path");
        if (outputPathStr == null || outputPathStr.isBlank()) {
            throw new IllegalArgumentException("output_path is required");
        }
        Path outputPath = IpesSupport.resolve(outputPathStr);

        // 1. Simulation parameters
        @SuppressWarnings("unchecked")
        Map<String, Object> simParams = (Map<String, Object>) request.getOrDefault("simulation", Map.of());
        double dt = getDouble(simParams, "dt", 1e-6);
        double duration = getDouble(simParams, "duration",
                getDouble(simParams, "simulation_time", 20e-3));
        Object solverObj = simParams.getOrDefault("solver", 0);
        int solverCode = solverObj instanceof Number n ? n.intValue()
                : switch (String.valueOf(solverObj).toLowerCase(Locale.ROOT)) {
                    case "be", "backward-euler" -> 0;
                    case "trz", "trapezoidal" -> 1;
                    case "gs", "gear-shichman" -> 2;
                    default -> 0;
                };

        boolean compress = Boolean.TRUE.equals(request.get("compress"));

        // 2. Power components
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawComponents =
                (List<Map<String, Object>>) request.getOrDefault("components", List.of());
        if (rawComponents.isEmpty()) {
            throw new IllegalArgumentException("at least one power component is required");
        }

        CircuitModel model = new CircuitModel();
        model.setTimeStep(dt);
        model.setSimulationDuration(duration);
        model.setSolverType(solver(solverCode));

        long nextLkId = 100;
        long nextCtrlId = 2001;
        long nextThermId = 4001;

        Map<String, Long> componentIdMap = new LinkedHashMap<>();
        int gridX = 4;
        int gridY = 8;
        int maxRowY = 8;

        // power nets: net name -> terminal points (one ConnectionData per net)
        Map<String, Set<Point>> netToPoints = new LinkedHashMap<>();
        List<Map<String, Object>> deferredCouplers = new ArrayList<>();
        long[] lkIdCounter = new long[]{nextLkId};
        long[] thermIdCounter = new long[]{nextThermId};

        for (Map<String, Object> c : rawComponents) {
            String typeStr = (String) c.get("type");
            ComponentCatalog.ComponentDef def = ComponentCatalog.get(typeStr);
            if (def == null) {
                throw new IllegalArgumentException("Unknown component type: '" + typeStr + "'");
            }
            if (!"POWER_LK".equals(def.domain())) {
                throw new IllegalArgumentException(
                        "Component '" + typeStr + "' is a " + def.domain() + " component; "
                                + "place it via the thermal_components or control section");
            }

            if (def.typeNumber() == 9) { // MUTUAL_INDUCTANCE / LK_M
                deferredCouplers.add(c);
                continue;
            }

            @SuppressWarnings("unchecked")
            List<String> nodes = (List<String>) c.getOrDefault("nodes", List.of());
            int expectedPins = def.pins().isEmpty() ? 2 : def.pins().size();
            if (nodes.size() != expectedPins) {
                throw new IllegalArgumentException("Component " + typeStr + " expects "
                        + expectedPins + " nodes " + def.pins() + ", got " + nodes.size());
            }

            Object pObj = c.get("parameters");
            if (pObj == null) pObj = c.get("params");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (pObj instanceof Map) ? (Map<String, Object>) pObj : Map.of();

            int orient = getOrientation(c, def);
            int posX = getInt(c, "x", -1);
            int posY = getInt(c, "y", -1);

            if (posX == -1 || posY == -1) {
                posX = gridX;
                posY = gridY;
                gridX += 6;
                if (gridX > 60) {
                    gridX = 4;
                    gridY += 10;
                }
            }
            maxRowY = Math.max(maxRowY, posY);

            // multi-pin split: X side = def.xPinCount pins, Y side = the rest
            List<String> xNodes = nodes.subList(0, def.xPinCount());
            List<String> yNodes = nodes.subList(def.xPinCount(), nodes.size());
            List<Point> termA;
            List<Point> termB;
            if (def.typeNumber() == CircuitTypCore.LK_TRANS.getTypeNumber()) {
                // The netlist expands an ideal transformer into two windings
                // placed one grid pitch left/right of center; the wires must
                // touch exactly those expanded terminals.
                List<Point> primary = windingTerminals(posX, posY, orient, true);
                List<Point> secondary = windingTerminals(posX, posY, orient, false);
                termA = List.of(primary.get(0), primary.get(1));
                termB = List.of(secondary.get(0), secondary.get(1));
            } else if (def.typeNumber() == CircuitTypCore.LK_BJT.getTypeNumber()) {
                // collector = two-port input, base = classic offset (-2, 0),
                // emitter = two-port output
                termA = List.of(getTerminalA(posX, posY, orient), basePoint(posX, posY, orient));
                termB = List.of(getTerminalB(posX, posY, orient));
            } else {
                termA = spreadTerminals(getTerminalA(posX, posY, orient), posX, posY, xNodes.size());
                termB = spreadTerminals(getTerminalB(posX, posY, orient), posX, posY, yNodes.size());
            }
            for (int pin = 0; pin < xNodes.size() && pin < termA.size(); pin++) {
                netToPoints.computeIfAbsent(xNodes.get(pin), k -> new LinkedHashSet<>()).add(termA.get(pin));
            }
            for (int pin = 0; pin < yNodes.size() && pin < termB.size(); pin++) {
                netToPoints.computeIfAbsent(yNodes.get(pin), k -> new LinkedHashSet<>()).add(termB.get(pin));
            }

            long uid = lkIdCounter[0]++;
            componentIdMap.put(name(c), uid);

            double[] paramArray = normalizeParams(def, params);
            applyRawParameters(paramArray, c.get("parameters_raw"));

            CircuitModel.ComponentData comp =
                    new CircuitModel.ComponentData(def.typeNumber(), name(c), posX, posY, orient);
            comp.setFamily("LK");
            comp.setRawParameters(paramArray);
            comp.setTerminalXLabels(xNodes.toArray(new String[0]));
            comp.setTerminalYLabels(yNodes.toArray(new String[0]));
            comp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
            comp.setUniqueObjectIdentifier(uid);
            applyNonlinearInductorSettings(comp, c);
            model.addCircuitComponent(comp);
        }

        // Process coupled inductors (MUTUAL_INDUCTANCE or coupled_inductors list)
        processCoupledInductors(request, deferredCouplers, model, componentIdMap, lkIdCounter);

        // 3. Thermal components & declarative thermal networks
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawThermal =
                (List<Map<String, Object>>) request.getOrDefault("thermal_components", List.of());
        Map<String, Set<Point>> thermNetToPoints = new LinkedHashMap<>();
        int thermY = maxRowY + 14;
        for (Map<String, Object> c : rawThermal) {
            String typeStr = (String) c.get("type");
            ComponentCatalog.ComponentDef def = ComponentCatalog.get(typeStr);
            if (def == null || !"THERM".equals(def.domain())) {
                throw new IllegalArgumentException(
                        "thermal_components entries must be thermal components, got: '" + typeStr + "'");
            }
            @SuppressWarnings("unchecked")
            List<String> nodes = (List<String>) c.getOrDefault("nodes", List.of());
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) c.getOrDefault("parameters", Map.of());

            int orient = getOrientation(c, def);
            int posX = getInt(c, "x", 4);
            int posY = getInt(c, "y", thermY);
            thermY = Math.max(thermY, posY + 6);
            maxRowY = Math.max(maxRowY, posY);

            if (def.typeNumber() == 50) { // TH_GLOBAL_TERMINAL
                String gndNode = !nodes.isEmpty() ? nodes.get(0) : "0";
                thermNetToPoints.computeIfAbsent(gndNode, k -> new LinkedHashSet<>()).add(new Point(posX, posY));
                CircuitModel.ComponentData comp =
                        new CircuitModel.ComponentData(50, name(c), posX, posY, orient);
                comp.setFamily("THERM");
                comp.setRawParameters(new double[]{});
                comp.setTerminalXLabels(new String[]{gndNode});
                comp.setTerminalYLabels(new String[]{gndNode});
                comp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                long uid = thermIdCounter[0]++;
                comp.setUniqueObjectIdentifier(uid);
                componentIdMap.put(name(c), uid);
                model.addThermalComponent(comp);
            } else if (def.typeNumber() == 41) { // TH_PvCHIP
                String targetSwitch = (String) c.getOrDefault("target_switch",
                        c.getOrDefault("target_component", c.get("target")));
                String outNode = nodes.size() == 1 ? nodes.get(0) : (nodes.size() > 1 ? nodes.get(1) : "Tj");
                String inNode = nodes.size() > 1 ? nodes.get(0) : "NIX_NIX_NIX";
                Point termB = getTerminalB(posX, posY, orient);
                thermNetToPoints.computeIfAbsent(outNode, k -> new LinkedHashSet<>()).add(termB);
                if (!"NIX_NIX_NIX".equals(inNode)) {
                    Point termA = getTerminalA(posX, posY, orient);
                    thermNetToPoints.computeIfAbsent(inNode, k -> new LinkedHashSet<>()).add(termA);
                }
                CircuitModel.ComponentData comp =
                        new CircuitModel.ComponentData(41, name(c), posX, posY, orient);
                comp.setFamily("THERM");
                comp.setRawParameters(normalizeParams(def, params));
                comp.setTerminalXLabels(new String[]{inNode});
                comp.setTerminalYLabels(new String[]{outNode});
                if (targetSwitch != null) {
                    comp.setParameterStrings(new String[]{"/" + targetSwitch, "NIX_NIX_NIX", "0"});
                    Long switchUid = componentIdMap.get(targetSwitch);
                    if (switchUid != null) comp.setCoupledReferenceID(switchUid);
                } else {
                    comp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                }
                long uid = thermIdCounter[0]++;
                comp.setUniqueObjectIdentifier(uid);
                componentIdMap.put(name(c), uid);
                model.addThermalComponent(comp);
            } else {
                if (nodes.size() != 2) {
                    throw new IllegalArgumentException("Thermal component " + typeStr + " expects 2 nodes");
                }
                Point termA = getTerminalA(posX, posY, orient);
                Point termB = getTerminalB(posX, posY, orient);
                thermNetToPoints.computeIfAbsent(nodes.get(0), k -> new LinkedHashSet<>()).add(termA);
                thermNetToPoints.computeIfAbsent(nodes.get(1), k -> new LinkedHashSet<>()).add(termB);

                CircuitModel.ComponentData comp =
                        new CircuitModel.ComponentData(def.typeNumber(), name(c), posX, posY, orient);
                comp.setFamily("THERM");
                comp.setRawParameters(normalizeParams(def, params));
                comp.setTerminalXLabels(new String[]{nodes.get(0)});
                comp.setTerminalYLabels(new String[]{nodes.get(1)});
                comp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                long uid = thermIdCounter[0]++;
                comp.setUniqueObjectIdentifier(uid);
                componentIdMap.put(name(c), uid);
                model.addThermalComponent(comp);
            }
        }

        // Synthesize declarative thermal networks
        maxRowY = Math.max(maxRowY, synthesizeThermalNetworks(request, model, componentIdMap, thermNetToPoints, maxRowY + 12, thermIdCounter));

        // 4. Control components
        @SuppressWarnings("unchecked")
        Map<String, Object> control = (Map<String, Object>) request.getOrDefault("control", Map.of());
        int ctrlY = maxRowY + 12;

        Map<String, Set<Point>> ctrlNetToPoints = new LinkedHashMap<>();
        // name -> PlacedControl for explicit wire routing
        Map<String, PlacedControl> controlsByName = new LinkedHashMap<>();

        int probeY = ctrlY;
        for (Map<String, Object> p : probeList(control)) {
            String name = name(p, "PROBE." + nextCtrlId);
            String type = (String) p.getOrDefault("type", "VOLTMETER");
            String targetName = (String) p.getOrDefault("target_component",
                    p.getOrDefault("target", p.get("component")));
            Long targetUid = targetName != null ? componentIdMap.get(targetName) : null;
            String nodeA = (String) p.get("nodeA");
            String nodeB = (String) p.getOrDefault("nodeB", "0");
            if (targetUid == null && nodeA == null) {
                throw new IllegalArgumentException(
                        "Probe " + name + " references unknown power/thermal component: '" + targetName + "'");
            }
            String signal = (String) p.getOrDefault("signal_name",
                    p.getOrDefault("signal", name.toLowerCase(Locale.ROOT)));
            int typeNum = "AMMETER".equalsIgnoreCase(type) || "AMP".equalsIgnoreCase(type)
                    ? TYP_PROBE_AMP : TYP_PROBE_VOLT;

            long uid = nextCtrlId++;
            ctrlNetToPoints.computeIfAbsent(signal, k -> new LinkedHashSet<>()).add(new Point(12, probeY));
            List<String> inLabels = List.of("NIX_NIX_NIX");
            List<String> outLabels = List.of(signal);
            Map<String, Object> strParams = new LinkedHashMap<>();
            if (nodeA != null) {
                strParams.put("nodeA", nodeA);
                strParams.put("nodeB", nodeB != null ? nodeB : "0");
            }
            PlacedControl placed = new PlacedControl(name, typeNum, uid, 10, probeY, 503,
                    inLabels, outLabels, targetName, targetUid != null ? targetUid : 0L,
                    Map.of(), strParams);
            controlsByName.put(name, placed);
            model.addControlComponent(placed.toComponentData());
            probeY++;
        }

        int mcuY = ctrlY;
        for (Map<String, Object> sb : scriptList(control)) {
            String name = name(sb, "CTRL_MCU");
            @SuppressWarnings("unchecked")
            List<String> inputs = (List<String>) sb.getOrDefault("inputs",
                    sb.getOrDefault("in_signals", sb.getOrDefault("input_signals", List.of())));
            @SuppressWarnings("unchecked")
            List<String> outputs = (List<String>) sb.getOrDefault("outputs",
                    sb.getOrDefault("out_signals", sb.getOrDefault("output_signals", List.of())));
            String sourceCode = (String) sb.getOrDefault("source_code",
                    sb.getOrDefault("code", sb.getOrDefault("sourceCode", "")));
            String staticVariables = (String) sb.getOrDefault("static_variables",
                    sb.getOrDefault("staticVariables", ""));
            String staticCode = (String) sb.getOrDefault("static_code",
                    sb.getOrDefault("staticCode", ""));

            long uid = nextCtrlId++;
            for (int inIdx = 0; inIdx < inputs.size(); inIdx++) {
                ctrlNetToPoints.computeIfAbsent(inputs.get(inIdx), k -> new LinkedHashSet<>())
                        .add(new Point(14, mcuY + inIdx));
            }
            for (int outIdx = 0; outIdx < outputs.size(); outIdx++) {
                ctrlNetToPoints.computeIfAbsent(outputs.get(outIdx), k -> new LinkedHashSet<>())
                        .add(new Point(18, mcuY + outIdx));
            }

            PlacedControl placed = new PlacedControl(name, TYP_SCRIPT, uid, 16, mcuY, 503,
                    inputs, outputs, null, 0L,
                    Map.of("sourceCode", sourceCode, "staticCode", staticCode,
                            "staticVariables", staticVariables,
                            "anzXIN", (double) inputs.size(), "anzYOUT", (double) outputs.size()),
                    Map.of());
            controlsByName.put(name, placed);
            model.addControlComponent(placed.toComponentData());
            mcuY += Math.max(inputs.size(), outputs.size()) + 2;
        }

        int gateY = ctrlY;
        for (Map<String, Object> g : gateList(control)) {
            String name = name(g, "GATE." + nextCtrlId);
            String targetName = (String) g.getOrDefault("target_switch",
                    g.getOrDefault("target_component", g.getOrDefault("target", g.get("switch"))));
            Long targetUid = componentIdMap.get(targetName);
            if (targetUid == null) {
                throw new IllegalArgumentException(
                        "Gate " + name + " references unknown switch: '" + targetName + "'");
            }
            String signal = (String) g.getOrDefault("in_signal",
                    g.getOrDefault("signal", g.get("signal_name")));

            long uid = nextCtrlId++;
            if (signal != null) {
                ctrlNetToPoints.computeIfAbsent(signal, k -> new LinkedHashSet<>()).add(new Point(24, gateY));
            }
            PlacedControl placed = new PlacedControl(name, TYP_GATE, uid, 26, gateY, 503,
                    List.of(signal != null ? signal : "NIX_NIX_NIX"), List.of("NIX_NIX_NIX"),
                    targetName, targetUid, Map.of(), Map.of());
            controlsByName.put(name, placed);
            model.addControlComponent(placed.toComponentData());
            gateY++;
        }

        // Generic control blocks from the catalog (CONSTANT, PI, COMPARATOR, ...)
        int blockX = CONTROL_BLOCK_BASE_X;
        int blockY = ctrlY;
        for (Map<String, Object> b : blockList(control)) {
            String typeStr = (String) b.get("type");
            ComponentCatalog.ComponentDef def = ComponentCatalog.get(typeStr);
            if (def == null || !"CONTROL".equals(def.domain())) {
                throw new IllegalArgumentException(
                        "control blocks entries must be control components, got: '" + typeStr + "'");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) b.getOrDefault("parameters", Map.of());
            String outputLabel = (String) b.getOrDefault("output_label", b.get("signal_name"));

            long uid = nextCtrlId++;
            PlacedControl placed = new PlacedControl(name(b, def.defaultPrefix() + "." + uid),
                    def.typeNumber(), uid, blockX, blockY, 503,
                    List.of(), outputLabel == null ? List.of("NIX_NIX_NIX") : List.of(outputLabel),
                    null, 0L, normalizeControlParams(def, params),
                    controlStringParams(b));
            controlsByName.put(placed.name(), placed);
            model.addControlComponent(placed.toComponentData());
            blockY += CONTROL_BLOCK_STRIDE;
            if (blockY > ctrlY + 40) {
                blockY = ctrlY;
                blockX += CONTROL_BLOCK_COLUMN_STRIDE;
            }
        }

        // 5. CONTROL wires routed between the terminal points of named blocks
        int wireIndex = 0;
        for (Map<String, Object> w : wireList(control)) {
            String from = (String) w.get("from");
            String to = (String) w.get("to");
            PlacedControl source = from != null ? controlsByName.get(from) : null;
            PlacedControl target = to != null ? controlsByName.get(to) : null;
            if (source == null || target == null) {
                throw new IllegalArgumentException("Control wire '" + from + "' -> '" + to
                        + "' references unknown control blocks");
            }
            int fromOutput = (int) getDouble(w, "from_output", 0);
            int toInput = (int) getDouble(w, "to_input", 0);
            Point start = source.outputPoint(503, fromOutput);
            Point end = target.inputPoint(503, toInput);
            List<Point> path = routeWire(start, end, wireIndex++);
            model.addConnection(new CircuitModel.ConnectionData("CONTROL", toPoints(path)));
        }

        // 6. LK, CONTROL, and THERMAL connections: one polyline per net
        for (Map.Entry<String, Set<Point>> entry : netToPoints.entrySet()) {
            CircuitModel.ConnectionData conn = new CircuitModel.ConnectionData("LK", toPoints(entry.getValue()));
            conn.setConnectorType(0);
            if (entry.getKey() != null && !entry.getKey().isBlank()) {
                conn.setLabel(entry.getKey());
            }
            model.addConnection(conn);
        }
        for (Map.Entry<String, Set<Point>> entry : ctrlNetToPoints.entrySet()) {
            CircuitModel.ConnectionData conn = new CircuitModel.ConnectionData("CONTROL", toPoints(entry.getValue()));
            conn.setConnectorType(1);
            if (entry.getKey() != null && !entry.getKey().isBlank()) {
                conn.setLabel(entry.getKey());
            }
            model.addConnection(conn);
        }
        for (Map.Entry<String, Set<Point>> entry : thermNetToPoints.entrySet()) {
            CircuitModel.ConnectionData conn = new CircuitModel.ConnectionData("THERMAL", toPoints(entry.getValue()));
            conn.setConnectorType(2);
            if (entry.getKey() != null && !entry.getKey().isBlank()) {
                conn.setLabel(entry.getKey());
            }
            model.addConnection(conn);
        }

        // 7. Serialize through the full-fidelity core writer
        byte[] bytes = CircuitFileWriter.write(model, compress);
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.write(outputPath, bytes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "CREATED");
        result.put("output_path", outputPath.toString());
        result.put("power_components_count", model.getCircuitComponents().size());
        result.put("control_components_count", model.getControlComponents().size());
        result.put("thermal_components_count", model.getThermalComponents().size());
        result.put("nets_count", netToPoints.size());
        result.put("control_signals_count", ctrlNetToPoints.size());
        return result;
    }

    // ---------- placement helpers ----------

    private static String name(Map<String, Object> c) {
        return name(c, "COMP." + System.identityHashCode(c));
    }

    private static String name(Map<String, Object> c, String fallback) {
        String name = (String) c.get("name");
        return name != null && !name.isBlank() ? name : fallback;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> probeList(Map<String, Object> control) {
        Object value = control.getOrDefault("probes", List.of());
        return value instanceof List ? (List<Map<String, Object>>) value : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> scriptList(Map<String, Object> control) {
        Object value = control.getOrDefault("script_blocks", List.of());
        return value instanceof List ? (List<Map<String, Object>>) value : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> gateList(Map<String, Object> control) {
        Object value = control.getOrDefault("gates", List.of());
        return value instanceof List ? (List<Map<String, Object>>) value : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> blockList(Map<String, Object> control) {
        Object value = control.getOrDefault("blocks", List.of());
        return value instanceof List ? (List<Map<String, Object>>) value : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> wireList(Map<String, Object> control) {
        Object value = control.getOrDefault("wires", List.of());
        return value instanceof List ? (List<Map<String, Object>>) value : List.of();
    }

    /** Semantic control parameters (coupled component, measurement node pair). */
    private static Map<String, Object> controlStringParams(Map<String, Object> spec) {
        Map<String, Object> strings = new LinkedHashMap<>();
        for (String key : new String[]{"coupledComponent", "nodeA", "nodeB"}) {
            Object value = spec.get(key);
            if (value != null) {
                strings.put(key, String.valueOf(value));
            }
        }
        return strings;
    }

    private static Map<String, Object> normalizeControlParams(ComponentCatalog.ComponentDef def,
                                                              Map<String, Object> inputParams) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (ComponentCatalog.ParameterDef p : def.parameters()) {
            double value = inputParams.containsKey(p.name())
                    ? getDouble(inputParams, p.name(), p.defaultValue()) : p.defaultValue();
            normalized.put("param" + p.targetSlot(), value);
        }
        return normalized;
    }

    /** Grid point of the X (input) terminal for the given orientation. */
    private static Point getTerminalA(int x, int y, int orientation) {
        return switch (orientation) {
            case 501 -> new Point(x, y + 2);  // SOUTH_NORTH: flow (0, -1), input (x, y+2)
            case 502 -> new Point(x - 2, y);  // WEST_EAST:   flow (1, 0),  input (x-2, y)
            case 504 -> new Point(x + 2, y);  // EAST_WEST:   flow (-1, 0), input (x+2, y)
            default  -> new Point(x, y - 2);  // NORTH_SOUTH: flow (0, 1),  input (x, y-2)
        };
    }

    private static Point getTerminalB(int x, int y, int orientation) {
        return switch (orientation) {
            case 501 -> new Point(x, y - 2);  // SOUTH_NORTH: flow (0, -1), output (x, y-2)
            case 502 -> new Point(x + 2, y);  // WEST_EAST:   flow (1, 0),  output (x+2, y)
            case 504 -> new Point(x - 2, y);  // EAST_WEST:   flow (-1, 0), output (x-2, y)
            default  -> new Point(x, y + 2);  // NORTH_SOUTH: flow (0, 1),  output (x, y+2)
        };
    }

    /**
     * Terminals of the ideal transformer's expanded windings, mirroring
     * NetlistBuilder's expansion: the primary sits one grid pitch on the
     * input side of center, the secondary one pitch on the output side, both
     * as two-ports in the component's orientation. {@code primary} selects
     * the winding; the returned points are [input, output].
     */
    private static List<Point> windingTerminals(int x, int y, int orientation, boolean primary) {
        int wx;
        int wy;
        switch (orientation) {
            case 501 -> { wx = x + 1; wy = y; }             // SOUTH_NORTH: input side is +x
            case 504 -> { wx = x; wy = y - 1; }             // EAST_WEST:   input side is -y
            case 502 -> { wx = x; wy = y + 1; }             // WEST_EAST:   input side is +y
            default -> { wx = x - 1; wy = y; }              // NORTH_SOUTH: input side is -x
        }
        if (!primary) {
            wx = 2 * x - wx;
            wy = 2 * y - wy;
        }
        return List.of(getTerminalA(wx, wy, orientation), getTerminalB(wx, wy, orientation));
    }

    /**
     * BJT base terminal at the classic relative offset (-2, 0), rotated with
     * the component orientation exactly like NetlistBuilder.rotatePinOffset.
     */
    private static Point basePoint(int x, int y, int orientation) {
        return switch (orientation) {
            case 501 -> new Point(x + 2, y);   // SOUTH_NORTH
            case 502 -> new Point(x, y + 2);   // WEST_EAST
            case 504 -> new Point(x, y - 2);   // EAST_WEST
            default -> new Point(x - 2, y);    // NORTH_SOUTH
        };
    }

    /**
     * Terminals of one component side for multi-pin elements: the side anchor
     * plus pins spread perpendicular to the flow direction, 2 grid units
     * apart, centered on the anchor (N pins at offsets -(N-1) .. +(N-1)).
     */
    private static List<Point> spreadTerminals(Point anchor, int cx, int cy, int count) {
        List<Point> points = new ArrayList<>(count);
        if (count <= 1) {
            points.add(anchor);
            return points;
        }
        int dx = (anchor.x() - cx) / 2; // 0 or +/-1 per axis
        int dy = (anchor.y() - cy) / 2;
        int perpX = -dy;
        int perpY = dx;
        for (int i = 0; i < count; i++) {
            int offset = 2 * i - 2 * (count - 1) / 2; // 2 units apart, centered
            points.add(new Point(anchor.x() + perpX * offset, anchor.y() + perpY * offset));
        }
        return points;
    }

    /**
     * Routes an orthogonal CONTROL wire between two terminal points: forward
     * wires lane just right of the source, feedback wires lane to the left of
     * both blocks, each wire in its own disjoint lane.
     */
    private static List<Point> routeWire(Point start, Point end, int wireIndex) {
        List<Point> path = new ArrayList<>();
        path.add(start);
        if (end.x() > start.x()) {
            int laneX = start.x() + WIRE_LANE_BASE_OFFSET + 2 * wireIndex;
            path.add(new Point(laneX, start.y()));
            path.add(new Point(laneX, end.y()));
        } else {
            int laneX = Math.min(start.x(), end.x()) - FEEDBACK_LANE_BASE_OFFSET - 2 * wireIndex;
            path.add(new Point(laneX, start.y()));
            path.add(new Point(laneX, end.y()));
        }
        path.add(end);
        return path;
    }

    private static int[][] toPoints(Collection<Point> points) {
        return points.stream().map(p -> new int[]{p.x(), p.y()}).toArray(int[][]::new);
    }

    /** Overrides parameter slots with a raw numeric vector (expert escape
     *  hatch for components without documented parameter names, e.g. motors). */
    private static void applyRawParameters(double[] slots, Object raw) {
        if (!(raw instanceof List<?> values)) {
            return;
        }
        for (int i = 0; i < values.size() && i < slots.length; i++) {
            if (values.get(i) instanceof Number n) {
                slots[i] = n.doubleValue();
            }
        }
    }

    private static int getOrientation(Map<String, Object> c, ComponentCatalog.ComponentDef def) {
        Object orientObj = c.get("orientation");
        if (orientObj instanceof Number n) {
            return n.intValue();
        }
        if (orientObj instanceof String s) {
            String lower = s.toLowerCase(Locale.ROOT);
            if (lower.contains("west_east") || lower.contains("horizontal") || lower.equals("502")) return 502;
            if (lower.contains("south_north") || lower.equals("501")) return 501;
            if (lower.contains("east_west") || lower.equals("504")) return 504;
            if (lower.contains("north_south") || lower.contains("vertical") || lower.equals("503")) return 503;
        }

        // Sensible defaults by component category
        return switch (def.id()) {
            case "CAPACITOR", "VOLTMETER", "CONSTANT", "GAIN", "PI", "PT1", "INTEGRATOR",
                 "COMPARATOR", "AND", "OR", "NOT", "SELECTOR", "DELAY", "TH_CTH" -> 503; // Vertical
            case "VOLTAGE_SOURCE_AC", "VOLTAGE_SOURCE_DC" -> 504; // EAST_WEST for positive nodeA
            default -> 502; // Horizontal by default
        };
    }

    private static double[] normalizeParams(ComponentCatalog.ComponentDef def, Map<String, Object> inputParams) {
        double[] out = new double[22]; // Allocate plenty of slots (0..21)
        Arrays.fill(out, 0.0);

        // Fill defaults first
        for (ComponentCatalog.ParameterDef p : def.parameters()) {
            out[p.targetSlot()] = p.defaultValue();
        }

        // Apply provided values
        for (ComponentCatalog.ParameterDef p : def.parameters()) {
            if (inputParams.containsKey(p.name())) {
                out[p.targetSlot()] = getDouble(inputParams, p.name(), p.defaultValue());
            } else if ("r_th".equals(p.name()) && inputParams.containsKey("resistance")) {
                out[p.targetSlot()] = getDouble(inputParams, "resistance", p.defaultValue());
            } else if ("c_th".equals(p.name()) && inputParams.containsKey("capacitance")) {
                out[p.targetSlot()] = getDouble(inputParams, "capacitance", p.defaultValue());
            }
        }

        // Handle component-specific derived slots
        switch (def.id()) {
            case "CAPACITOR", "TH_CTH" -> {
                double primary = out[0];
                out[6] = primary; // MNA companion model slot
                out[7] = primary; // nonlinear factor
            }
            case "COUPLED_INDUCTOR", "LKOP2" -> {
                double primary = out[0];
                out[10] = primary; // companion matrix A slot
            }
            case "VOLTAGE_SOURCE_AC", "CURRENT_SOURCE_AC" -> {
                out[0] = 402.0; // SourceType.QUELLE_SIN
                out[20] = out[1]; // Amplitude slot 20
            }
            case "VOLTAGE_SOURCE_DC", "CURRENT_SOURCE_DC", "TH_FLOW", "TH_TEMP" -> {
                out[0] = 401.0; // SourceType.QUELLE_DC
            }
            case "DIODE", "THYRISTOR" -> {
                double uF = out[1] > 0 ? out[1] : 0.7;
                double rOn = out[2] > 0 ? out[2] : 0.005;
                double rOff = out[3] > 0 ? out[3] : 1e7;
                out[0] = rOff; // Initial resistance rD
                out[1] = uF;
                out[2] = rOn;
                out[3] = rOff;
            }
            case "MOSFET" -> {
                double rOn = out[2] > 0 ? out[2] : 0.005;
                double rOff = out[3] > 0 ? out[3] : 1e7;
                out[0] = rOff; // initial blocking state resistance
                out[2] = rOn;
                out[3] = rOff;
            }
            case "IDEAL_SWITCH" -> {
                double rOn = out[1] > 0 ? out[1] : 0.005;
                double rOff = out[2] > 0 ? out[2] : 1e6;
                out[0] = 0.0; // initial off
                out[1] = rOn;
                out[2] = rOff;
            }
            case "IGBT" -> {
                double uF = out[1] > 0 ? out[1] : 1.2;
                double rOn = out[2] > 0 ? out[2] : 0.005;
                double rOff = out[3] > 0 ? out[3] : 1e7;
                out[0] = rOff;
                out[1] = uF;
                out[2] = rOn;
                out[3] = rOff;
            }
            case "PMSM_MOTOR" -> {
                // Typical 10 kW machine preset copied verbatim from the
                // verified reference circuit dq_control_pmsm.ipes; every
                // slot stays overridable via parameters_raw.
                double[] preset = {
                        7.552931615989641, -11.45789154912143, 3.904959933131788, 314.1492380776522,
                        2999.9042465166613, 15.944421290389943, 10.012972849851, 0.191, 2.1e-4, 4.0e-4,
                        10.0, 3.0, 1.0, 0.005, 0.005, 0.0, 0.0, 0.0, 0.0, -1.0, -1.0};
                System.arraycopy(preset, 0, out, 0, preset.length);
            }
        }

        return out;
    }

    private static SolverType solver(int code) {
        return switch (code) {
            case 1 -> SolverType.SOLVER_TRZ;
            case 2 -> SolverType.SOLVER_GS;
            default -> SolverType.SOLVER_BE;
        };
    }

    private static double getDouble(Map<String, Object> map, String key, double fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private static int getInt(Map<String, Object> map, String key, int fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    /** Integer grid point of the schematic raster. */
    private record Point(int x, int y) {}

    /**
     * One placed control block with the data needed for explicit wire routing:
     * the engine terminal geometry derives input {@code i} at (x - 2, y + i)
     * and output {@code j} at (x + 2, y + j) for NORTH_SOUTH orientation.
     */
    private record PlacedControl(String name, int type, long uid, int x, int y, int orientation,
                                 List<String> inNodes, List<String> outNodes,
                                 String coupledComponent, long coupledUid,
                                 Map<String, Object> numericParams, Map<String, Object> stringParams) {

        Point inputPoint(int orientationCode, int index) {
            return terminalPoint(x, y, orientationCode, true, index);
        }

        Point outputPoint(int orientationCode, int index) {
            return terminalPoint(x, y, orientationCode, false, index);
        }

        private static Point terminalPoint(int cx, int cy, int orientation, boolean input, int index) {
            // mirrors ControlCalculatorBuilder.terminalPoint
            int px = input ? -2 : 2;
            int py = input ? -index : -index;
            int dx;
            int dy;
            switch (orientation) {
                case 504 -> { dx = py; dy = px; }
                case 501 -> { dx = -px; dy = py; }
                case 502 -> { dx = -py; dy = -px; }
                default -> { dx = px; dy = -py; } // NORTH_SOUTH (503)
            }
            return new Point(cx + dx, cy + dy);
        }

        CircuitModel.ComponentData toComponentData() {
            CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, name, x, y, orientation);
            comp.setFamily("CONTROL");
            comp.setUniqueObjectIdentifier(uid);
            if (coupledComponent != null) {
                comp.setParameterStrings(new String[]{coupledComponent});
                if (coupledUid != 0) {
                    comp.setCoupledReferenceID(coupledUid);
                }
            } else if (stringParams.containsKey("nodeA")) {
                String nA = String.valueOf(stringParams.get("nodeA"));
                String nB = String.valueOf(stringParams.getOrDefault("nodeB", "0"));
                comp.setParameterStrings(new String[]{"/" + nA, "/" + nB, "0"});
            }
            if (!inNodes.isEmpty() && !inNodes.get(0).equals("NIX_NIX_NIX")) {
                comp.setTerminalXLabels(inNodes.toArray(new String[0]));
            }
            if (!outNodes.isEmpty() && !outNodes.get(0).equals("NIX_NIX_NIX")) {
                comp.setTerminalYLabels(outNodes.toArray(new String[0]));
            }
            for (Map.Entry<String, Object> entry : numericParams.entrySet()) {
                comp.setParameter(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Object> entry : stringParams.entrySet()) {
                comp.setParameter(entry.getKey(), entry.getValue());
            }
            if (numericParams.isEmpty() && stringParams.isEmpty()) {
                comp.setRawParameters(new double[]{0.0});
            }
            return comp;
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyNonlinearInductorSettings(CircuitModel.ComponentData comp, Map<String, Object> c) {
        String typeStr = (String) c.get("type");
        boolean isNonlinear = "NONLINEAR_INDUCTOR".equalsIgnoreCase(typeStr)
                || Boolean.TRUE.equals(c.get("is_nonlinear"))
                || Boolean.TRUE.equals(c.get("isNonlinear"))
                || c.containsKey("saturation_table")
                || c.containsKey("saturation_currents")
                || c.containsKey("nonlinX");
        if (!isNonlinear) {
            return;
        }
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        if (c.get("saturation_table") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof List<?> pair && pair.size() >= 2) {
                    xList.add(((Number) pair.get(0)).doubleValue());
                    yList.add(((Number) pair.get(1)).doubleValue());
                }
            }
        } else if (c.get("saturation_currents") instanceof List<?> xs && c.get("saturation_inductances") instanceof List<?> ys) {
            for (Object x : xs) if (x instanceof Number n) xList.add(n.doubleValue());
            for (Object y : ys) if (y instanceof Number n) yList.add(n.doubleValue());
        } else if (c.get("nonlinX") instanceof List<?> xs && c.get("nonlinY") instanceof List<?> ys) {
            for (Object x : xs) if (x instanceof Number n) xList.add(n.doubleValue());
            for (Object y : ys) if (y instanceof Number n) yList.add(n.doubleValue());
        }
        if (xList.isEmpty() || yList.isEmpty()) {
            double lNom = comp.getRawParameters() != null && comp.getRawParameters().length > 0
                    ? comp.getRawParameters()[0] : 1e-3;
            xList = List.of(0.0, 10.0, 15.0, 30.0);
            yList = List.of(lNom, lNom, lNom * 0.3, lNom * 0.2);
        }
        comp.addExtraLine("berechneVerluste false");
        comp.addExtraLine("isNonlinear true");
        StringBuilder sbX = new StringBuilder("nonlinX[] ");
        for (double v : xList) sbX.append(v).append(" ");
        comp.addExtraLine(sbX.toString());
        StringBuilder sbY = new StringBuilder("nonlinY[] ");
        for (double v : yList) sbY.append(v).append(" ");
        comp.addExtraLine(sbY.toString());
        comp.addExtraLine("nonLinearCharHashValue 0");
    }

    @SuppressWarnings("unchecked")
    private static void processCoupledInductors(
            Map<String, Object> request,
            List<Map<String, Object>> deferredCouplers,
            CircuitModel model,
            Map<String, Long> componentIdMap,
            long[] lkIdGen) {
        List<Map<String, Object>> couplers = new ArrayList<>(deferredCouplers);
        if (request.get("coupled_inductors") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    couplers.add((Map<String, Object>) m);
                }
            }
        }

        for (Map<String, Object> c : couplers) {
            String name = (String) c.get("name");
            double k = getDouble(c, "k", 0.99);
            if (k <= 0.0 || k > 1.0) {
                k = 0.99;
            }

            String ind1Name = null;
            String ind2Name = null;
            if (c.get("inductors") instanceof List<?> inds && inds.size() >= 2) {
                ind1Name = String.valueOf(inds.get(0));
                ind2Name = String.valueOf(inds.get(1));
            } else {
                ind1Name = (String) c.getOrDefault("inductor1", c.getOrDefault("target_1", c.get("target1")));
                ind2Name = (String) c.getOrDefault("inductor2", c.getOrDefault("target_2", c.get("target2")));
            }

            if (ind1Name == null || ind2Name == null) {
                throw new IllegalArgumentException("Coupled inductor definition requires two inductor names");
            }

            CircuitModel.ComponentData ind1 = null;
            CircuitModel.ComponentData ind2 = null;
            for (CircuitModel.ComponentData comp : model.getCircuitComponents()) {
                if (ind1Name.equals(comp.getName())) ind1 = comp;
                if (ind2Name.equals(comp.getName())) ind2 = comp;
            }

            if (ind1 == null || ind2 == null) {
                throw new IllegalArgumentException("Coupled inductors references unknown inductor: "
                        + (ind1 == null ? ind1Name : ind2Name));
            }

            // Upgrade referenced inductors to LK_LKOP2 (typ 12) if needed
            upgradeToCoupledInductor(ind1);
            upgradeToCoupledInductor(ind2);

            String kName = name != null && !name.isBlank() ? name : "K_" + ind1.getName() + "_" + ind2.getName();
            int kX = (ind1.getPosition()[0] + ind2.getPosition()[0]) / 2;
            int kY = Math.min(ind1.getPosition()[1], ind2.getPosition()[1]) - 3;

            CircuitModel.ComponentData kComp = new CircuitModel.ComponentData(9, kName, kX, kY, 503);
            kComp.setFamily("LK");
            long uid = lkIdGen[0]++;
            kComp.setUniqueObjectIdentifier(uid);
            componentIdMap.put(kName, uid);

            double[] raw = new double[40];
            raw[0] = k;
            raw[1] = (double) ind1.getUniqueObjectIdentifier();
            raw[2] = (double) ind2.getUniqueObjectIdentifier();
            kComp.setRawParameters(raw);
            kComp.setParameterStrings(new String[]{"/" + ind1.getName(), "/" + ind2.getName(), "1"});
            kComp.setTerminalXLabels(new String[]{"NIX_NIX_NIX"});
            kComp.setTerminalYLabels(new String[]{"NIX_NIX_NIX"});

            model.addCircuitComponent(kComp);
        }
    }

    private static void upgradeToCoupledInductor(CircuitModel.ComponentData comp) {
        if (comp.getType() == 2) { // LK_L -> LK_LKOP2 (12)
            comp.setType(12);
            double[] p = comp.getRawParameters();
            if (p != null && p.length > 0) {
                double[] up = new double[40];
                System.arraycopy(p, 0, up, 0, p.length);
                up[10] = up[0]; // INDEX_EFFECTIVE_L
                comp.setRawParameters(up);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static int synthesizeThermalNetworks(
            Map<String, Object> request,
            CircuitModel model,
            Map<String, Long> componentIdMap,
            Map<String, Set<Point>> thermNetToPoints,
            int startY,
            long[] thermIdGen) {
        List<Map<String, Object>> networks = (List<Map<String, Object>>) request.get("thermal_networks");
        if (networks == null || networks.isEmpty()) {
            return startY;
        }

        int curY = startY;
        for (Map<String, Object> net : networks) {
            String targetSwitch = (String) net.getOrDefault("target_switch",
                    net.getOrDefault("target_component", net.get("target")));
            String name = (String) net.getOrDefault("name",
                    targetSwitch != null ? "TH_" + targetSwitch : "TH_NET");
            String modelType = ((String) net.getOrDefault("model_type", "foster")).toLowerCase(Locale.ROOT);
            String junctionNode = (String) net.getOrDefault("junction_node",
                    targetSwitch != null ? "Tj_" + targetSwitch : "Tj_" + name);
            String ambientNode = (String) net.getOrDefault("ambient_node", "T_amb_" + name);
            double ambientTemp = getDouble(net, "ambient_temperature", 25.0);

            // Extract stages
            List<Double> rList = new ArrayList<>();
            List<Double> cList = new ArrayList<>();
            if (net.get("stages") instanceof List<?> stageList) {
                for (Object item : stageList) {
                    if (item instanceof Map<?, ?> sm) {
                        rList.add(getDouble((Map<String, Object>) sm, "r", 0.5));
                        cList.add(getDouble((Map<String, Object>) sm, "c", 0.05));
                    }
                }
            } else {
                if (net.get("r_th") instanceof List<?> rs) {
                    for (Object r : rs) if (r instanceof Number n) rList.add(n.doubleValue());
                }
                if (net.get("c_th") instanceof List<?> cs) {
                    for (Object c : cs) if (c instanceof Number n) cList.add(n.doubleValue());
                }
            }

            if (rList.isEmpty()) {
                rList.add(getDouble(net, "r_th", 0.5));
                cList.add(getDouble(net, "c_th", 0.05));
            }
            while (cList.size() < rList.size()) {
                cList.add(0.01);
            }

            int netX = getInt(net, "x", 32);
            int netY = getInt(net, "y", curY);

            // 1. Loss injection: TH_PvCHIP (coupled to switch) and/or TH_FLOW
            int pvY = netY;
            if (targetSwitch != null) {
                CircuitModel.ComponentData pv = new CircuitModel.ComponentData(41, "Pv_" + targetSwitch, netX, pvY, 503);
                pv.setFamily("THERM");
                pv.setRawParameters(new double[]{1.0});
                pv.setTerminalXLabels(new String[]{"NIX_NIX_NIX"});
                pv.setTerminalYLabels(new String[]{junctionNode});
                pv.setParameterStrings(new String[]{"/" + targetSwitch, "NIX_NIX_NIX", "0"});
                Long swUid = componentIdMap.get(targetSwitch);
                if (swUid != null) {
                    pv.setCoupledReferenceID(swUid);
                }
                long pvUid = thermIdGen[0]++;
                pv.setUniqueObjectIdentifier(pvUid);
                componentIdMap.put(pv.getName(), pvUid);
                model.addThermalComponent(pv);

                Point termPv = getTerminalB(netX, pvY, 503);
                thermNetToPoints.computeIfAbsent(junctionNode, k -> new LinkedHashSet<>()).add(termPv);
            }
            if (net.containsKey("heat_flow") || targetSwitch == null) {
                double heatFlow = getDouble(net, "heat_flow", 50.0);
                int flowX = targetSwitch != null ? netX + 4 : netX;
                CircuitModel.ComponentData flow = new CircuitModel.ComponentData(44, "P_" + name, flowX, pvY, 503);
                flow.setFamily("THERM");
                flow.setRawParameters(new double[]{401.0, heatFlow});
                flow.setTerminalXLabels(new String[]{"0"});
                flow.setTerminalYLabels(new String[]{junctionNode});
                flow.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                long flowUid = thermIdGen[0]++;
                flow.setUniqueObjectIdentifier(flowUid);
                model.addThermalComponent(flow);

                thermNetToPoints.computeIfAbsent("0", k -> new LinkedHashSet<>()).add(getTerminalA(flowX, pvY, 503));
                thermNetToPoints.computeIfAbsent(junctionNode, k -> new LinkedHashSet<>()).add(getTerminalB(flowX, pvY, 503));
            }

            // 2. RC Ladder stages (4 grid units vertical pitch)
            int stageCount = rList.size();
            for (int i = 0; i < stageCount; i++) {
                String nodeFrom = (i == 0) ? junctionNode : (name + "_n" + i);
                String nodeTo = (i == stageCount - 1) ? ambientNode : (name + "_n" + (i + 1));
                double rVal = rList.get(i);
                double cVal = cList.get(i);
                int stageY = pvY + 4 * (i + 1);

                if ("cauer".equals(modelType)) {
                    // Cauer ladder: Rth in series, Cth in shunt to ambient
                    CircuitModel.ComponentData rComp = new CircuitModel.ComponentData(
                            46, name + "_Rth" + (i + 1), netX, stageY, 503);
                    rComp.setFamily("THERM");
                    rComp.setRawParameters(new double[]{rVal});
                    rComp.setTerminalXLabels(new String[]{nodeFrom});
                    rComp.setTerminalYLabels(new String[]{nodeTo});
                    rComp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                    rComp.setUniqueObjectIdentifier(thermIdGen[0]++);
                    model.addThermalComponent(rComp);
                    thermNetToPoints.computeIfAbsent(nodeFrom, k -> new LinkedHashSet<>()).add(getTerminalA(netX, stageY, 503));
                    thermNetToPoints.computeIfAbsent(nodeTo, k -> new LinkedHashSet<>()).add(getTerminalB(netX, stageY, 503));

                    CircuitModel.ComponentData cComp = new CircuitModel.ComponentData(
                            47, name + "_Cth" + (i + 1), netX + 4, stageY, 503);
                    cComp.setFamily("THERM");
                    double[] cParams = new double[40];
                    cParams[0] = cVal;
                    cParams[6] = cVal;
                    cParams[7] = cVal;
                    cComp.setRawParameters(cParams);
                    cComp.setTerminalXLabels(new String[]{nodeFrom});
                    cComp.setTerminalYLabels(new String[]{ambientNode});
                    cComp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                    cComp.setUniqueObjectIdentifier(thermIdGen[0]++);
                    model.addThermalComponent(cComp);
                    thermNetToPoints.computeIfAbsent(nodeFrom, k -> new LinkedHashSet<>()).add(getTerminalA(netX + 4, stageY, 503));
                    thermNetToPoints.computeIfAbsent(ambientNode, k -> new LinkedHashSet<>()).add(getTerminalB(netX + 4, stageY, 503));
                } else {
                    // Foster chain: parallel Rth and Cth between nodeFrom and nodeTo
                    CircuitModel.ComponentData rComp = new CircuitModel.ComponentData(
                            46, name + "_Rth" + (i + 1), netX, stageY, 503);
                    rComp.setFamily("THERM");
                    rComp.setRawParameters(new double[]{rVal});
                    rComp.setTerminalXLabels(new String[]{nodeFrom});
                    rComp.setTerminalYLabels(new String[]{nodeTo});
                    rComp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                    rComp.setUniqueObjectIdentifier(thermIdGen[0]++);
                    model.addThermalComponent(rComp);
                    thermNetToPoints.computeIfAbsent(nodeFrom, k -> new LinkedHashSet<>()).add(getTerminalA(netX, stageY, 503));
                    thermNetToPoints.computeIfAbsent(nodeTo, k -> new LinkedHashSet<>()).add(getTerminalB(netX, stageY, 503));

                    CircuitModel.ComponentData cComp = new CircuitModel.ComponentData(
                            47, name + "_Cth" + (i + 1), netX + 4, stageY, 503);
                    cComp.setFamily("THERM");
                    double[] cParams = new double[40];
                    cParams[0] = cVal;
                    cParams[6] = cVal;
                    cParams[7] = cVal;
                    cComp.setRawParameters(cParams);
                    cComp.setTerminalXLabels(new String[]{nodeFrom});
                    cComp.setTerminalYLabels(new String[]{nodeTo});
                    cComp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
                    cComp.setUniqueObjectIdentifier(thermIdGen[0]++);
                    model.addThermalComponent(cComp);
                    thermNetToPoints.computeIfAbsent(nodeFrom, k -> new LinkedHashSet<>()).add(getTerminalA(netX + 4, stageY, 503));
                    thermNetToPoints.computeIfAbsent(nodeTo, k -> new LinkedHashSet<>()).add(getTerminalB(netX + 4, stageY, 503));
                }
            }

            // 3. Ambient boundary source & ground terminal
            int ambY = pvY + 4 * (stageCount + 1);
            CircuitModel.ComponentData tAmb = new CircuitModel.ComponentData(
                    45, "Tamb_" + name, netX, ambY, 503);
            tAmb.setFamily("THERM");
            tAmb.setRawParameters(new double[]{401.0, ambientTemp});
            tAmb.setTerminalXLabels(new String[]{ambientNode});
            tAmb.setTerminalYLabels(new String[]{"0"});
            tAmb.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
            tAmb.setUniqueObjectIdentifier(thermIdGen[0]++);
            model.addThermalComponent(tAmb);
            thermNetToPoints.computeIfAbsent(ambientNode, k -> new LinkedHashSet<>()).add(getTerminalA(netX, ambY, 503));
            thermNetToPoints.computeIfAbsent("0", k -> new LinkedHashSet<>()).add(getTerminalB(netX, ambY, 503));

            CircuitModel.ComponentData gnd = new CircuitModel.ComponentData(
                    50, "GND_TH_" + name, netX, ambY + 2, 503);
            gnd.setFamily("THERM");
            gnd.setRawParameters(new double[]{});
            gnd.setTerminalXLabels(new String[]{"0"});
            gnd.setTerminalYLabels(new String[]{"0"});
            gnd.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
            gnd.setUniqueObjectIdentifier(thermIdGen[0]++);
            model.addThermalComponent(gnd);
            thermNetToPoints.computeIfAbsent("0", k -> new LinkedHashSet<>()).add(new Point(netX, ambY + 2));

            curY = ambY + 6;
        }
        return curY;
    }
}
