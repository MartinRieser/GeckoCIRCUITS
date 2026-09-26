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

        boolean compress = !Boolean.FALSE.equals(request.get("compress"));

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

            long uid = nextLkId++;
            componentIdMap.put(name(c), uid);

            double[] paramArray = normalizeParams(def, params);
            applyRawParameters(paramArray, c.get("parameters_raw"));

            CircuitModel.ComponentData comp =
                    new CircuitModel.ComponentData(def.typeNumber(), name(c), posX, posY, orient);
            comp.setFamily("LK");
            comp.setRawParameters(paramArray);
            comp.setTerminalXLabels(xNodes.toArray(new String[0]));
            comp.setTerminalYLabels(yNodes.toArray(new String[0]));
            comp.setUniqueObjectIdentifier(uid);
            model.addCircuitComponent(comp);
        }

        // 3. Thermal components (same placement rules as power components)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawThermal =
                (List<Map<String, Object>>) request.getOrDefault("thermal_components", List.of());
        Map<String, Set<Point>> thermNetToPoints = new LinkedHashMap<>();
        for (Map<String, Object> c : rawThermal) {
            String typeStr = (String) c.get("type");
            ComponentCatalog.ComponentDef def = ComponentCatalog.get(typeStr);
            if (def == null || !"THERM".equals(def.domain())) {
                throw new IllegalArgumentException(
                        "thermal_components entries must be thermal components, got: '" + typeStr + "'");
            }
            @SuppressWarnings("unchecked")
            List<String> nodes = (List<String>) c.getOrDefault("nodes", List.of());
            if (nodes.size() != 2) {
                throw new IllegalArgumentException("Thermal component " + typeStr + " expects 2 nodes");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) c.getOrDefault("parameters", Map.of());

            int orient = getOrientation(c, def);
            int posX = getInt(c, "x", 4);
            int posY = getInt(c, "y", maxRowY + 30);
            maxRowY = Math.max(maxRowY, posY);

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
            comp.setUniqueObjectIdentifier(nextThermId++);
            model.addThermalComponent(comp);
        }

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
            Long targetUid = componentIdMap.get(targetName);
            if (targetUid == null) {
                throw new IllegalArgumentException(
                        "Probe " + name + " references unknown power component: '" + targetName + "'");
            }
            String signal = (String) p.getOrDefault("signal_name",
                    p.getOrDefault("signal", name.toLowerCase(Locale.ROOT)));
            int typeNum = "AMMETER".equalsIgnoreCase(type) || "AMP".equalsIgnoreCase(type)
                    ? TYP_PROBE_AMP : TYP_PROBE_VOLT;

            long uid = nextCtrlId++;
            ctrlNetToPoints.computeIfAbsent(signal, k -> new LinkedHashSet<>()).add(new Point(12, probeY));
            PlacedControl placed = new PlacedControl(name, typeNum, uid, 10, probeY, 503,
                    List.of("NIX_NIX_NIX"), List.of(signal), targetName, targetUid,
                    Map.of(), Map.of());
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

        // 6. LK and thermal connections: one polyline per net
        for (Map.Entry<String, Set<Point>> entry : netToPoints.entrySet()) {
            model.addConnection(new CircuitModel.ConnectionData("LK", toPoints(entry.getValue())));
        }
        for (Map.Entry<String, Set<Point>> entry : ctrlNetToPoints.entrySet()) {
            model.addConnection(new CircuitModel.ConnectionData("CONTROL", toPoints(entry.getValue())));
        }
        for (Map.Entry<String, Set<Point>> entry : thermNetToPoints.entrySet()) {
            model.addConnection(new CircuitModel.ConnectionData("THERMAL", toPoints(entry.getValue())));
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
            }
        }

        // Handle component-specific derived slots
        switch (def.id()) {
            case "CAPACITOR", "TH_CTH" -> {
                double primary = out[0];
                out[6] = primary; // MNA companion model slot
                out[7] = primary; // nonlinear factor
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
}
