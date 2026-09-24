package gecko.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * High-level circuit synthesis engine for GeckoCIRCUITS.
 *
 * <p>Translates human/LLM-readable JSON circuit definitions into fully valid,
 * collision-free, MNA-normalized GeckoCIRCUITS (.ipes) files.</p>
 */
public final class CircuitBuilder {

    private CircuitBuilder() {
    }

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
        double duration = getDouble(simParams, "duration", getDouble(simParams, "dauer", 0.05));
        int solverType = getInt(simParams, "solver", 0); // 0=BE, 1=TRZ, 2=GS

        // 2. Extract power components
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawComponents = (List<Map<String, Object>>) request.getOrDefault("components", List.of());
        if (rawComponents.isEmpty()) {
            throw new IllegalArgumentException("At least one component must be specified");
        }

        // 3. Extract control domain definitions
        @SuppressWarnings("unchecked")
        Map<String, Object> controlConfig = (Map<String, Object>) request.getOrDefault("control", Map.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawProbes = (List<Map<String, Object>>) controlConfig.getOrDefault("probes", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawScriptBlocks = (List<Map<String, Object>>) controlConfig.getOrDefault("script_blocks", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawGates = (List<Map<String, Object>>) controlConfig.getOrDefault("gates", List.of());

        // Process components and assign IDs
        long nextLkId = 1001;
        Map<String, Long> componentIdMap = new LinkedHashMap<>();
        List<PlacedComponent> placedLk = new ArrayList<>();
        Map<String, Set<Point>> netToPoints = new LinkedHashMap<>();

        int gridX = 4;
        int gridY = 8;
        int maxRowY = gridY;

        for (int i = 0; i < rawComponents.size(); i++) {
            Map<String, Object> c = rawComponents.get(i);
            String name = (String) c.get("name");
            if (name == null || name.isBlank()) {
                name = "COMP." + (i + 1);
            }
            String typeStr = (String) c.get("type");
            ComponentCatalog.ComponentDef def = ComponentCatalog.get(typeStr);
            if (def == null) {
                throw new IllegalArgumentException("Unknown component type: '" + typeStr + "'. Check gecko_catalog for valid types.");
            }

            @SuppressWarnings("unchecked")
            List<String> nodesRaw = (List<String>) c.get("nodes");
            int expectedPins = def.pins().isEmpty() ? 2 : def.pins().size();
            if (nodesRaw == null || nodesRaw.size() != expectedPins) {
                throw new IllegalArgumentException("Component " + name + " (" + def.id() + ") requires "
                        + expectedPins + " nodes " + def.pins() + ", got "
                        + (nodesRaw == null ? 0 : nodesRaw.size()));
            }
            List<String> nodes = nodesRaw.stream().map(String::trim).toList();
            List<String> xNodes = nodes.subList(0, def.xPinCount());
            List<String> yNodes = nodes.subList(def.xPinCount(), nodes.size());

            Object pObj = c.get("parameters");
            if (pObj == null) pObj = c.get("params");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (pObj instanceof Map) ? (Map<String, Object>) pObj : Map.of();

            // Determine position and orientation
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

            // Terminals per side, spread perpendicular to the flow direction
            // for multi-pin elements (2 grid units apart, centered) exactly
            // like the classic editor's scopes and motors
            List<Point> termA = spreadTerminals(getTerminalA(posX, posY, orient), posX, posY, xNodes.size());
            List<Point> termB = spreadTerminals(getTerminalB(posX, posY, orient), posX, posY, yNodes.size());

            for (int pin = 0; pin < xNodes.size(); pin++) {
                netToPoints.computeIfAbsent(xNodes.get(pin), k -> new LinkedHashSet<>()).add(termA.get(pin));
            }
            for (int pin = 0; pin < yNodes.size(); pin++) {
                netToPoints.computeIfAbsent(yNodes.get(pin), k -> new LinkedHashSet<>()).add(termB.get(pin));
            }

            long uid = nextLkId++;
            componentIdMap.put(name, uid);

            // Normalize parameter array; expert components may override raw
            // slots directly (parameters_raw), e.g. motor machine data
            double[] paramArray = normalizeParams(def, params);
            applyRawParameters(paramArray, c.get("parameters_raw"));

            placedLk.add(new PlacedComponent(name, def, uid, posX, posY, orient, xNodes, yNodes, paramArray));
        }

        // Process control components
        long nextCtrlId = 2001;
        List<PlacedControl> placedCtrl = new ArrayList<>();
        Map<String, Set<Point>> ctrlNetToPoints = new LinkedHashMap<>();

        int ctrlY = maxRowY + 12;

        // Probes (Voltmeter, Ammeter)
        int probeY = ctrlY;
        for (Map<String, Object> p : rawProbes) {
            String name = (String) p.getOrDefault("name", "PROBE." + nextCtrlId);
            String type = (String) p.getOrDefault("type", "VOLTMETER");
            String targetName = (String) p.getOrDefault("target_component", p.getOrDefault("target", p.get("component")));
            Long targetUid = componentIdMap.get(targetName);
            if (targetUid == null) {
                throw new IllegalArgumentException("Probe " + name + " references unknown power component: '" + targetName + "'");
            }
            String signal = (String) p.getOrDefault("signal_name", p.getOrDefault("signal", name.toLowerCase(Locale.ROOT)));

            int typeNum = "AMMETER".equalsIgnoreCase(type) || "AMP".equalsIgnoreCase(type) ? 2 : 1;
            long uid = nextCtrlId++;

            // Control probe placed at x=10, output terminal at (12, probeY)
            Point outTerm = new Point(12, probeY);
            ctrlNetToPoints.computeIfAbsent(signal, k -> new LinkedHashSet<>()).add(outTerm);

            placedCtrl.add(new PlacedControl(name, typeNum, uid, 10, probeY, 503, targetUid,
                    List.of("NIX_NIX_NIX"), List.of(signal), "", "", ""));
            probeY++;
        }

        // Script blocks
        int mcuY = ctrlY;
        for (Map<String, Object> sb : rawScriptBlocks) {
            String name = (String) sb.getOrDefault("name", "CTRL_MCU");
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
            // Script block placed at x=16, inputs at x=14, outputs at x=18
            for (int inIdx = 0; inIdx < inputs.size(); inIdx++) {
                String sig = inputs.get(inIdx);
                ctrlNetToPoints.computeIfAbsent(sig, k -> new LinkedHashSet<>()).add(new Point(14, mcuY + inIdx));
            }
            for (int outIdx = 0; outIdx < outputs.size(); outIdx++) {
                String sig = outputs.get(outIdx);
                ctrlNetToPoints.computeIfAbsent(sig, k -> new LinkedHashSet<>()).add(new Point(18, mcuY + outIdx));
            }

            placedCtrl.add(new PlacedControl(name, 61, uid, 16, mcuY, 503, 0L,
                    inputs, outputs, sourceCode, staticVariables, staticCode));
            mcuY += Math.max(inputs.size(), outputs.size()) + 2;
        }

        // Gates
        int gateY = ctrlY;
        for (Map<String, Object> g : rawGates) {
            String name = (String) g.getOrDefault("name", "GATE." + nextCtrlId);
            String targetName = (String) g.getOrDefault("target_switch", g.getOrDefault("target_component", g.getOrDefault("target", g.get("switch"))));
            Long targetUid = componentIdMap.get(targetName);
            if (targetUid == null) {
                throw new IllegalArgumentException("Gate " + name + " references unknown switch: '" + targetName + "'");
            }
            String signal = (String) g.getOrDefault("in_signal", g.getOrDefault("signal", g.get("signal_name")));

            long uid = nextCtrlId++;
            // Gate placed at x=26, input terminal at (24, gateY)
            if (signal != null) {
                ctrlNetToPoints.computeIfAbsent(signal, k -> new LinkedHashSet<>()).add(new Point(24, gateY));
            }

            placedCtrl.add(new PlacedControl(name, 6, uid, 26, gateY, 503, targetUid,
                    List.of(signal != null ? signal : "NIX_NIX_NIX"), List.of("NIX_NIX_NIX"), "", "", ""));
            gateY++;
        }

        // Build .ipes content
        StringBuilder sb = new StringBuilder();
        sb.append("GeckoSimulationProject\n");
        sb.append("version 2.0\n");
        sb.append("simulationParameters\n");
        sb.append(String.format(Locale.ROOT, "dt %.6e\n", dt));
        sb.append(String.format(Locale.ROOT, "tend %.4f\n", duration));
        sb.append(String.format(Locale.ROOT, "dauer %.4f\n", duration));
        sb.append("solverType ").append(solverType).append("\n");
        sb.append("<\\simulationParameters>\n\n");

        // Power connections (verbindungLK)
        sb.append("verbindungLeistungskreisANZAHL ").append(netToPoints.size()).append("\n\n");
        int netIdx = 0;
        for (Map.Entry<String, Set<Point>> entry : netToPoints.entrySet()) {
            String netName = entry.getKey();
            Set<Point> pts = entry.getValue();
            sb.append("verbindungLK (").append(netIdx++).append(")\n");
            sb.append("<Connection>\n");
            sb.append("label ").append(netName).append("\n");
            sb.append("zeigerAktuell ").append(pts.size()).append("\n");

            sb.append("x[]");
            for (Point p : pts) sb.append(" ").append(p.x);
            sb.append("\n");

            sb.append("y[]");
            for (Point p : pts) sb.append(" ").append(p.y);
            sb.append("\n");

            sb.append("xPix[]");
            for (Point p : pts) sb.append(" ").append(p.x * 16);
            sb.append("\n");

            sb.append("yPix[]");
            for (Point p : pts) sb.append(" ").append(p.y * 16);
            sb.append("\n");

            sb.append("enabled true\n");
            sb.append("connectorType 0\n");
            sb.append("<\\Connection>\n\n");
        }

        // Power components (ElementLK)
        sb.append("ElementLKAnzahl ").append(placedLk.size()).append("\n\n");
        for (int i = 0; i < placedLk.size(); i++) {
            PlacedComponent pc = placedLk.get(i);
            sb.append("e (").append(i).append(")\n");
            sb.append("<ElementLK>\n");
            sb.append("labelAnfangsKnoten[] ").append(labelArray(pc.xNodes)).append("\n");
            sb.append("labelEndKnoten[] ").append(labelArray(pc.yNodes)).append("\n");
            sb.append("enabledShorted 1\n");
            sb.append("typ ").append(pc.def.typeNumber()).append("\n");
            sb.append("uniqueObjectIdentifier ").append(pc.uid).append("\n");
            sb.append("x ").append(pc.x).append("\n");
            sb.append("y ").append(pc.y).append("\n");

            sb.append("parameter[]");
            for (double v : pc.parameters) {
                sb.append(" ").append(formatDouble(v));
            }
            sb.append(" \n");

            sb.append("orientierung ").append(pc.orientation).append("\n");
            sb.append("idStringDialog ").append(pc.name).append("\n");
            sb.append("<\\ElementLK>\n\n");
        }

        // Control components (ElementCONTROL)
        sb.append("ElementCONTROLAnzahl ").append(placedCtrl.size()).append("\n\n");
        for (int i = 0; i < placedCtrl.size(); i++) {
            PlacedControl ctl = placedCtrl.get(i);
            sb.append("c (").append(i).append(")\n");
            sb.append("<ElementCONTROL>\n");

            sb.append("labelAnfangsKnoten[] /").append(String.join("/", ctl.inNodes)).append("\n");
            sb.append("labelEndKnoten[] /").append(String.join("/", ctl.outNodes)).append("\n");
            sb.append("enabledShorted 1\n");
            sb.append("parentSheetIdentifier 0\n");
            sb.append("typ ").append(ctl.type).append("\n");
            sb.append("uniqueObjectIdentifier ").append(ctl.uid).append("\n");
            sb.append("x ").append(ctl.x).append("\n");
            sb.append("y ").append(ctl.y).append("\n");

            sb.append("parameter[] 0.0\n");
            if (ctl.coupledId > 0) {
                sb.append("coupledReferenceID[] ").append(ctl.coupledId).append("\n");
            }
            sb.append("orientierung ").append(ctl.orientation).append("\n");
            sb.append("idStringDialog ").append(ctl.name).append("\n");

            if (ctl.type == 61) {
                sb.append("parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0\n");
                sb.append("anzXIN ").append(ctl.inNodes.size()).append("\n");
                sb.append("anzYOUT ").append(ctl.outNodes.size()).append("\n");
                sb.append("showName true\n");
                sb.append("<sourceCode>\n").append(ctl.sourceCode).append("\n<\\sourceCode>\n");
                sb.append("<staticCode>\n").append(ctl.staticCode).append("\n<\\staticCode>\n");
                sb.append("<importCode>\n<\\importCode>\n");
                sb.append("<staticVariables>\n").append(ctl.staticVariables).append("\n<\\staticVariables>\n");
            }
            sb.append("<\\ElementCONTROL>\n\n");
        }

        // Control connections (verbindungCONTROL)
        sb.append("verbindungCONTROLAnzahl ").append(ctrlNetToPoints.size()).append("\n\n");
        int ctrlNetIdx = 0;
        for (Map.Entry<String, Set<Point>> entry : ctrlNetToPoints.entrySet()) {
            String netName = entry.getKey();
            Set<Point> pts = entry.getValue();
            sb.append("verbindungCONTROL (").append(ctrlNetIdx++).append(")\n");
            sb.append("<Connection>\n");
            sb.append("label ").append(netName).append("\n");
            sb.append("zeigerAktuell ").append(pts.size()).append("\n");

            sb.append("x[]");
            for (Point p : pts) sb.append(" ").append(p.x);
            sb.append("\n");

            sb.append("y[]");
            for (Point p : pts) sb.append(" ").append(p.y);
            sb.append("\n");

            sb.append("enabledShorted 1\n");
            sb.append("parentSheetIdentifier 0\n");
            sb.append("connectorType 1\n");
            sb.append("<\\Connection>\n\n");
        }

        // Ensure parent directory exists and write output
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "CREATED");
        result.put("output_path", outputPath.toString());
        result.put("power_components_count", placedLk.size());
        result.put("control_components_count", placedCtrl.size());
        result.put("nets_count", netToPoints.size());
        result.put("control_signals_count", ctrlNetToPoints.size());
        return result;
    }

    private record Point(int x, int y) {}

    private record PlacedComponent(
            String name, ComponentCatalog.ComponentDef def, long uid,
            int x, int y, int orientation, List<String> xNodes, List<String> yNodes, double[] parameters
    ) {}

    private record PlacedControl(
            String name, int type, long uid, int x, int y, int orientation,
            long coupledId, List<String> inNodes, List<String> outNodes,
            String sourceCode, String staticVariables, String staticCode
    ) {}

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

    /** .ipes label-array encoding: "/l1/l2/l3"; an empty side carries no labels. */
    private static String labelArray(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "";
        }
        return "/" + String.join("/", labels);
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
            case "CAPACITOR", "VOLTMETER" -> 503; // Vertical
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
            case "CAPACITOR" -> {
                double cap = out[0];
                out[6] = cap; // MNA companion model slot
                out[7] = cap; // nonlinear factor
            }
            case "VOLTAGE_SOURCE_AC" -> {
                out[0] = 402.0; // SourceType.QUELLE_SIN
                out[20] = out[1]; // Amplitude slot 20
            }
            case "VOLTAGE_SOURCE_DC" -> {
                out[0] = 401.0; // SourceType.QUELLE_DC
            }
            case "DIODE" -> {
                double uF = out[1] > 0 ? out[1] : 0.7;
                double rOn = out[2] > 0 ? out[2] : 0.005;
                double rOff = out[3] > 0 ? out[3] : 1e7;
                out[0] = rOff; // Initial resistance rD
                out[1] = uF;
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

    private static String formatDouble(double v) {
        if (Double.isFinite(v)) {
            if (Math.abs(v) >= 1e6 || (Math.abs(v) <= 1e-4 && v != 0.0)) {
                return String.format(Locale.ROOT, "%.6e", v);
            }
            return String.valueOf(v);
        }
        return "0.0";
    }
}
