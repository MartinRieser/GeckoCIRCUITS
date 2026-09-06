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
            List<String> nodes = (List<String>) c.get("nodes");
            if (nodes == null || nodes.size() < 2) {
                throw new IllegalArgumentException("Component " + name + " must specify at least 2 nodes");
            }
            String nodeA = nodes.get(0).trim();
            String nodeB = nodes.get(1).trim();

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

            // Compute terminals based on orientation
            // flowVector: EAST_WEST=(-1,0), WEST_EAST=(1,0), SOUTH_NORTH=(0,-1), NORTH_SOUTH=(0,1)
            Point termA = getTerminalA(posX, posY, orient);
            Point termB = getTerminalB(posX, posY, orient);

            netToPoints.computeIfAbsent(nodeA, k -> new LinkedHashSet<>()).add(termA);
            netToPoints.computeIfAbsent(nodeB, k -> new LinkedHashSet<>()).add(termB);

            long uid = nextLkId++;
            componentIdMap.put(name, uid);

            // Normalize parameter array
            double[] paramArray = normalizeParams(def, params);

            placedLk.add(new PlacedComponent(name, def, uid, posX, posY, orient, nodeA, nodeB, paramArray));
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
            List<String> inputs = (List<String>) sb.getOrDefault("inputs", List.of());
            @SuppressWarnings("unchecked")
            List<String> outputs = (List<String>) sb.getOrDefault("outputs", List.of());
            String sourceCode = (String) sb.getOrDefault("source_code", "");
            String staticVariables = (String) sb.getOrDefault("static_variables", "");
            String staticCode = (String) sb.getOrDefault("static_code", "");

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
            sb.append("labelAnfangsKnoten[] /").append(pc.nodeA).append("\n");
            sb.append("labelEndKnoten[] /").append(pc.nodeB).append("\n");
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
            int x, int y, int orientation, String nodeA, String nodeB, double[] parameters
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
