package gecko.mcp;

import gecko.core.allg.SolverType;
import gecko.core.circuit.ComponentTerminals;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.ControlCalculatorBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Design Rule Checker (DRC) and static circuit linter for GeckoCIRCUITS models.
 *
 * <p>Diagnoses circuit problems before simulation execution:
 * <ul>
 *   <li>Missing ground references or floating sub-networks</li>
 *   <li>Dangling / single-connection nodes</li>
 *   <li>Short-circuited components (terminals tied together)</li>
 *   <li>Broken control couplings (dangling gates or probes)</li>
 *   <li>Microcontroller script block syntax and division-by-zero risks</li>
 * </ul>
 * </p>
 */
public final class CircuitValidator {

    public record Diagnostic(
            String level, // "ERROR", "WARNING", "INFO"
            String rule,
            String component,
            String message
    ) {}

    private static final Pattern ELEMENT_LK = Pattern.compile("<ElementLK>([\\s\\S]*?)<[\\\\/]+ElementLK>");
    private static final Pattern ELEMENT_CONTROL = Pattern.compile("<ElementCONTROL>([\\s\\S]*?)<[\\\\/]+ElementCONTROL>");
    private static final Pattern ELEMENT_THERM = Pattern.compile("<ElementTHERM>([\\s\\S]*?)<[\\\\/]+ElementTHERM>");
    private static final Pattern CONNECTION_LK = Pattern.compile("<(?:Connection|Verbindung)>([\\s\\S]*?)<[\\\\/]+(?:Connection|Verbindung)>");

    private CircuitValidator() {
    }

    public static Map<String, Object> validate(Path ipesPath) throws IOException {
        return validate(ipesPath, "compile");
    }

    /**
     * Validates a circuit at the given level: {@code "lint"} runs only the
     * regex design rules; {@code "compile"} (default) additionally parses the
     * file with the engine parser, builds the real MNA netlist and CONTROL
     * graph, and performs a two-step test solve to catch singular matrices
     * and broken couplings before an expensive run.
     */
    public static Map<String, Object> validate(Path ipesPath, String level) throws IOException {
        Map<String, Object> result = validate(ipesPath, IpesSupport.readIpesText(ipesPath), level);
        result.put("file", ipesPath.toString());
        return result;
    }

    private static Map<String, Object> validate(Path ipesPath, String content, String level) {
        Map<String, Object> result = new LinkedHashMap<>(validateContent(content,
                ipesPath != null ? ipesPath.getFileName().toString() : "inline_circuit"));
        if (level == null || !"compile".equalsIgnoreCase(level)) {
            return result;
        }
        List<Map<String, Object>> compileChecks = new ArrayList<>();
        boolean compileOk = true;
        try {
            CircuitModel model = ipesPath != null
                    ? new CircuitFileParser().parse(ipesPath.toString())
                    : new CircuitFileParser().parse(
                            new BufferedReader(new StringReader(content)), "inline_circuit");
            try {
                CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
                for (String warning : netlist.getBuildWarnings()) {
                    compileChecks.add(compileEntry("WARNING", "NETLIST_WARNING", warning));
                }
                ControlCalculatorBuilder.ControlCoupling coupling =
                        ControlCalculatorBuilder.build(model, netlist);
                result.put("control_calculators", coupling.calculators().size());
                result.put("probes", coupling.probes().size());
                result.put("gates", coupling.gateDrives().size());
                result.put("signal_taps", coupling.signalTaps().size());
                compileOk &= testSolve(model, compileChecks);
            } catch (RuntimeException e) {
                compileOk = false;
                compileChecks.add(compileEntry("ERROR", "NETLIST_BUILD", String.valueOf(e.getMessage())));
            }
        } catch (Exception e) {
            compileOk = false;
            compileChecks.add(compileEntry("ERROR", "PARSE", String.valueOf(e.getMessage())));
        }
        result.put("compile_checks", compileChecks);
        result.put("compile_level", "compile");
        boolean lintValid = Boolean.TRUE.equals(result.get("valid"));
        result.put("valid", lintValid && compileOk);
        return result;
    }

    /**
     * Two-step test solve: catches singular matrices, invalid parameter
     * values and nonlinear solver failures cheaply, before any real run.
     */
    private static boolean testSolve(CircuitModel model, List<Map<String, Object>> checks) {
        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .solverType(SolverType.SOLVER_BE)
                .stepWidth(Math.max(model.getTimeStep(), 1e-9))
                .simulationDuration(2 * Math.max(model.getTimeStep(), 1e-9))
                .enableDataLogging(false)
                .build();
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        if (result.isSuccess()) {
            checks.add(compileEntry("INFO", "TEST_SOLVE", "two-step test solve passed"));
            return true;
        }
        checks.add(compileEntry("ERROR", "TEST_SOLVE",
                String.valueOf(result.getErrorMessage())));
        return false;
    }

    private static Map<String, Object> compileEntry(String severity, String code, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("severity", severity);
        entry.put("code", code);
        entry.put("message", message);
        return entry;
    }

    public record Point(int x, int y) {}

    public static final class DisjointSet<T> {
        private final Map<T, T> parent = new HashMap<>();

        public T find(T item) {
            T root = parent.get(item);
            if (root == null) {
                parent.put(item, item);
                return item;
            }
            if (!root.equals(item)) {
                root = find(root);
                parent.put(item, root);
            }
            return root;
        }

        public void union(T itemA, T itemB) {
            T rootA = find(itemA);
            T rootB = find(itemB);
            if (!rootA.equals(rootB)) {
                parent.put(rootA, rootB);
            }
        }
    }

    private record ComponentData(
            long uid,
            String name,
            int typ,
            String domain,
            boolean hasPos,
            int x,
            int y,
            int orient,
            List<String> nodesA,
            List<String> nodesB,
            List<Point> pins
    ) {}

    private record WireData(
            String label,
            int connectorType,
            List<Point> points,
            Point endA,
            Point endB
    ) {}

    public static Map<String, Object> validateContent(String content, String circuitName) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        Map<Long, Map<String, Object>> lkComponents = new LinkedHashMap<>();
        Map<String, Map<String, Object>> lkComponentsByName = new LinkedHashMap<>();
        Map<Long, Map<String, Object>> thermComponents = new LinkedHashMap<>();
        Map<String, Map<String, Object>> thermComponentsByName = new LinkedHashMap<>();
        List<ComponentData> allComponents = new ArrayList<>();
        List<WireData> wires = new ArrayList<>();

        long fallbackUidGen = 100000L;

        // 1. Parse LK Components
        Matcher lkMatcher = ELEMENT_LK.matcher(content);
        while (lkMatcher.find()) {
            String block = lkMatcher.group(1);
            long uid = parseLong(group(block, "uniqueObjectIdentifier\\s+([0-9-]+)"), 0L);
            if (uid == 0L) {
                uid = fallbackUidGen++;
            }
            String name = named(group(block, "idStringDialog\\s+([^\\s\\r\\n]+)"), "COMP_" + uid);
            int typ = parseInt(group(block, "typ\\s+([0-9]+)"), 0);

            Integer posX = parseOptionalInt(group(block, "(?:^|\\n)x\\s+([0-9-]+)"));
            Integer posY = parseOptionalInt(group(block, "(?:^|\\n)y\\s+([0-9-]+)"));
            int orient = parseInt(group(block, "(?:^|\\n)(?:orientierung|orientation)\\s+([0-9]+)"), 503);
            boolean hasPos = posX != null && posY != null;
            int x = hasPos ? posX : 0;
            int y = hasPos ? posY : 0;

            List<String> nodesA = parseNodes(group(block, "labelAnfangsKnoten\\[\\][ \\t]*([^\\r\\n]*)"));
            List<String> nodesB = parseNodes(group(block, "labelEndKnoten\\[\\][ \\t]*([^\\r\\n]*)"));

            List<Point> pins;
            String xinXStr = group(block, "(?:^|\\n)xXIN(?:\\[\\])?\\s+([0-9\\s-]+)");
            String xinYStr = group(block, "(?:^|\\n)yXIN(?:\\[\\])?\\s+([0-9\\s-]+)");
            String youtXStr = group(block, "(?:^|\\n)xYOUT(?:\\[\\])?\\s+([0-9\\s-]+)");
            String youtYStr = group(block, "(?:^|\\n)yYOUT(?:\\[\\])?\\s+([0-9\\s-]+)");
            int[] xinXs = xinXStr != null ? parseIntArray(xinXStr) : new int[0];
            int[] xinYs = xinYStr != null ? parseIntArray(xinYStr) : new int[0];
            int[] youtXs = youtXStr != null ? parseIntArray(youtXStr) : new int[0];
            int[] youtYs = youtYStr != null ? parseIntArray(youtYStr) : new int[0];
            if (xinXs.length > 0 && xinYs.length > 0 && youtXs.length > 0 && youtYs.length > 0) {
                pins = new ArrayList<>();
                pins.add(new Point(xinXs[0], xinYs[0]));
                pins.add(new Point(youtXs[0], youtYs[0]));
                hasPos = true;
            } else {
                pins = computeComponentPins(typ, "LK", hasPos, x, y, orient);
            }

            ComponentData comp = new ComponentData(uid, name, typ, "LK", hasPos, x, y, orient, nodesA, nodesB, pins);
            allComponents.add(comp);

            Map<String, Object> info = new HashMap<>();
            info.put("uid", uid);
            info.put("name", name);
            info.put("type", typ);
            info.put("nodesA", nodesA);
            info.put("nodesB", nodesB);
            lkComponents.put(uid, info);
            if (!name.isBlank()) {
                lkComponentsByName.put(name, info);
            }
        }

        // 2. Parse Thermal Elements
        Matcher thermMatcher = ELEMENT_THERM.matcher(content);
        while (thermMatcher.find()) {
            String block = thermMatcher.group(1);
            long uid = parseLong(group(block, "uniqueObjectIdentifier\\s+([0-9-]+)"), 0L);
            if (uid == 0L) {
                uid = fallbackUidGen++;
            }
            String name = named(group(block, "idStringDialog\\s+([^\\s\\r\\n]+)"), "THERM_" + uid);
            int typ = parseInt(group(block, "typ\\s+([0-9]+)"), 0);

            Integer posX = parseOptionalInt(group(block, "(?:^|\\n)x\\s+([0-9-]+)"));
            Integer posY = parseOptionalInt(group(block, "(?:^|\\n)y\\s+([0-9-]+)"));
            int orient = parseInt(group(block, "(?:^|\\n)(?:orientierung|orientation)\\s+([0-9]+)"), 503);
            boolean hasPos = posX != null && posY != null;
            int x = hasPos ? posX : 0;
            int y = hasPos ? posY : 0;

            List<String> nodesA = parseNodes(group(block, "labelAnfangsKnoten\\[\\][ \\t]*([^\\r\\n]*)"));
            List<String> nodesB = parseNodes(group(block, "labelEndKnoten\\[\\][ \\t]*([^\\r\\n]*)"));

            List<Point> pins = computeComponentPins(typ, "THERMAL", hasPos, x, y, orient);

            ComponentData comp = new ComponentData(uid, name, typ, "THERMAL", hasPos, x, y, orient, nodesA, nodesB, pins);
            allComponents.add(comp);

            Map<String, Object> info = new HashMap<>();
            info.put("uid", uid);
            info.put("name", name);
            info.put("type", typ);
            info.put("nodesA", nodesA);
            info.put("nodesB", nodesB);
            thermComponents.put(uid, info);
            if (!name.isBlank()) {
                thermComponentsByName.put(name, info);
            }
        }

        // 3. Parse Connections / Wires
        Matcher connMatcher = CONNECTION_LK.matcher(content);
        while (connMatcher.find()) {
            String block = connMatcher.group(1);
            String label = named(group(block, "(?:^|\\n)label\\s+([^\\s\\r\\n]+)"), "");
            if (label.equalsIgnoreCase("NIX_NIX_NIX")) {
                label = "";
            }
            int connType = parseInt(group(block, "(?:^|\\n)connectorType\\s+([0-9]+)"), 0);
            String xLine = group(block, "(?:^|\\n)x(?:\\[\\])?\\s+([0-9\\s-]+)");
            String yLine = group(block, "(?:^|\\n)y(?:\\[\\])?\\s+([0-9\\s-]+)");
            int[] xs = parseIntArray(xLine);
            int[] ys = parseIntArray(yLine);
            int len = Math.min(xs.length, ys.length);
            if (len == 0) continue;

            List<Point> rawPoints = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                rawPoints.add(new Point(xs[i], ys[i]));
            }
            Point endA = rawPoints.get(0);
            Point endB = rawPoints.get(rawPoints.size() - 1);
            wires.add(new WireData(label, connType, rawPoints, endA, endB));
        }

        // 4. Build Wire Connectivity Graphs per Domain (Endpoint-Junction Rule)
        Map<String, List<WireData>> wiresByDomain = new HashMap<>();
        for (WireData w : wires) {
            String dom = w.connectorType == 2 ? "TH" : (w.connectorType == 1 ? "CTRL" : "LK");
            wiresByDomain.computeIfAbsent(dom, k -> new ArrayList<>()).add(w);
        }

        Map<String, DisjointSet<Integer>> wireUfByDomain = new HashMap<>();
        Map<String, Map<Point, List<Integer>>> wiresThroughPointByDomain = new HashMap<>();

        for (String dom : List.of("LK", "TH", "CTRL")) {
            List<WireData> dWires = wiresByDomain.getOrDefault(dom, List.of());
            DisjointSet<Integer> dUf = new DisjointSet<>();
            Map<Point, List<Integer>> dPoints = new HashMap<>();
            for (int i = 0; i < dWires.size(); i++) {
                dUf.find(i);
                WireData w = dWires.get(i);
                for (Point p : w.points) {
                    dPoints.computeIfAbsent(p, k -> new ArrayList<>()).add(i);
                }
            }
            // Wires merge ONLY when an endpoint touches another wire
            for (int i = 0; i < dWires.size(); i++) {
                WireData w = dWires.get(i);
                for (Point end : List.of(w.endA, w.endB)) {
                    for (int j : dPoints.getOrDefault(end, List.of())) {
                        dUf.union(i, j);
                    }
                }
            }
            wireUfByDomain.put(dom, dUf);
            wiresThroughPointByDomain.put(dom, dPoints);
        }

        // 5. Global Net Resolution (Union-Find)
        DisjointSet<String> uf = new DisjointSet<>();
        Set<String> registeredElements = new LinkedHashSet<>();

        // Register wire nets and propagate wire labels
        for (String dom : List.of("LK", "TH", "CTRL")) {
            List<WireData> dWires = wiresByDomain.getOrDefault(dom, List.of());
            DisjointSet<Integer> dUf = wireUfByDomain.get(dom);
            for (int i = 0; i < dWires.size(); i++) {
                int root = dUf.find(i);
                String wireNetKey = "W:" + dom + ":" + root;
                registeredElements.add(wireNetKey);
                WireData w = dWires.get(i);
                if (!w.label.isEmpty()) {
                    String lKey = isGroundLabel(w.label) ? "L:" + dom + ":0" : "L:" + dom + ":" + cleanLabel(w.label).toLowerCase();
                    registeredElements.add(lKey);
                    uf.union(wireNetKey, lKey);
                }
            }
        }

        // Attach component terminals to wire nets or label equivalence classes
        Map<String, List<String>> pinsAtPoint = new HashMap<>();

        for (ComponentData comp : allComponents) {
            String dom = "THERMAL".equals(comp.domain) ? "TH" : "LK";
            int termCount = comp.typ == 9 ? 0
                    : (comp.typ == 23 ? 4
                    : (comp.typ == 33 ? 3
                    : (comp.typ == 31 || comp.typ == 50 || comp.typ == 41 ? 1 : 2)));
            Map<Point, List<Integer>> dPoints = wiresThroughPointByDomain.get(dom);
            DisjointSet<Integer> dUf = wireUfByDomain.get(dom);

            for (int t = 0; t < termCount; t++) {
                String tKey = "T:" + comp.uid + ":" + t;
                registeredElements.add(tKey);

                if (comp.typ == 31 && "LK".equals(dom)) { // LK_GLOBAL_TERMINAL
                    String gndKey = "L:LK:0";
                    registeredElements.add(gndKey);
                    uf.union(tKey, gndKey);
                } else if (comp.typ == 50 && "TH".equals(dom)) { // TH_GLOBAL_TERMINAL
                    String gndKey = "L:TH:0";
                    registeredElements.add(gndKey);
                    uf.union(tKey, gndKey);
                }

                if (comp.hasPos && t < comp.pins.size()) {
                    Point pin = comp.pins.get(t);
                    List<Integer> touchingWires = dPoints != null ? dPoints.get(pin) : null;
                    if (touchingWires != null && !touchingWires.isEmpty()) {
                        int rootW = dUf.find(touchingWires.get(0));
                        String wireNetKey = "W:" + dom + ":" + rootW;
                        uf.union(tKey, wireNetKey);
                    }
                    pinsAtPoint.computeIfAbsent(dom + ":" + pin.x + "," + pin.y, k -> new ArrayList<>()).add(tKey);
                }

                String lbl = getTerminalLabel(comp, t);
                if (lbl != null && !lbl.isEmpty() && !lbl.equalsIgnoreCase("NIX_NIX_NIX")) {
                    String lKey = isGroundLabel(lbl) ? "L:" + dom + ":0" : "L:" + dom + ":" + cleanLabel(lbl).toLowerCase();
                    registeredElements.add(lKey);
                    uf.union(tKey, lKey);
                }
            }
        }

        // Union coincident component terminals at the same coordinate
        for (List<String> coincident : pinsAtPoint.values()) {
            if (coincident.size() > 1) {
                String first = coincident.get(0);
                for (int i = 1; i < coincident.size(); i++) {
                    uf.union(coincident.get(i), first);
                }
            }
        }

        // 6. Canonical Net Names Resolution
        Map<String, Set<String>> classMembers = new HashMap<>();
        for (String el : registeredElements) {
            String root = uf.find(el);
            classMembers.computeIfAbsent(root, k -> new LinkedHashSet<>()).add(el);
        }

        Map<String, String> rootToCanonicalNet = new HashMap<>();
        int syntheticNetCounter = 1;
        for (Map.Entry<String, Set<String>> entry : classMembers.entrySet()) {
            String root = entry.getKey();
            Set<String> members = entry.getValue();
            String canonical = resolveCanonicalName(members, syntheticNetCounter++);
            rootToCanonicalNet.put(root, canonical);
        }

        // 7. Map Nets to Components
        Map<String, List<String>> netToComponents = new LinkedHashMap<>();
        Map<String, Integer> netTerminalCounts = new LinkedHashMap<>();
        Set<String> allNets = new LinkedHashSet<>();

        for (WireData w : wires) {
            if (!w.label.isEmpty() && w.connectorType == 0) {
                String dom = "LK";
                String lKey = isGroundLabel(w.label) ? "L:" + dom + ":0" : "L:" + dom + ":" + cleanLabel(w.label).toLowerCase();
                String root = uf.find(lKey);
                String net = rootToCanonicalNet.getOrDefault(root, cleanLabel(w.label));
                allNets.add(net);
            }
        }

        for (ComponentData comp : allComponents) {
            if ("THERMAL".equals(comp.domain) || comp.typ == 31 || comp.typ == 9) {
                continue;
            }
            int termCount = comp.typ == 23 ? 4 : (comp.typ == 33 ? 3 : 2);
            for (int t = 0; t < termCount; t++) {
                String tKey = "T:" + comp.uid + ":" + t;
                String root = uf.find(tKey);
                String net = rootToCanonicalNet.get(root);
                if (net == null) continue;

                allNets.add(net);
                netToComponents.computeIfAbsent(net, k -> new ArrayList<>()).add(comp.name);
                netTerminalCounts.put(net, netTerminalCounts.getOrDefault(net, 0) + 1);
            }
        }

        // Rule 1: Short-circuited component check across net equivalence classes
        for (ComponentData comp : allComponents) {
            if ("THERMAL".equals(comp.domain) || comp.typ == 31 || comp.typ == 9 || comp.typ == 22) {
                continue;
            }
            if (comp.typ == 23) { // LK_TRANS
                String pIn = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":0"));
                String pOut = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":1"));
                if (pIn != null && pOut != null && pIn.equals(pOut)) {
                    diagnostics.add(new Diagnostic("ERROR", "SHORT_CIRCUIT", comp.name,
                            "Component " + comp.name + " has primary terminals connected to the same net: '" + pIn + "'"));
                }
                String sIn = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":2"));
                String sOut = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":3"));
                if (sIn != null && sOut != null && sIn.equals(sOut)) {
                    diagnostics.add(new Diagnostic("ERROR", "SHORT_CIRCUIT", comp.name,
                            "Component " + comp.name + " has secondary terminals connected to the same net: '" + sIn + "'"));
                }
            } else if (comp.typ == 33) { // LK_BJT
                String cNet = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":0"));
                String eNet = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":2"));
                if (cNet != null && eNet != null && cNet.equals(eNet)) {
                    diagnostics.add(new Diagnostic("ERROR", "SHORT_CIRCUIT", comp.name,
                            "Component " + comp.name + " has collector and emitter connected to the same net: '" + cNet + "'"));
                }
            } else {
                boolean hasA = isTerminalConnected(comp, 0, wiresThroughPointByDomain.get("LK"));
                boolean hasB = isTerminalConnected(comp, 1, wiresThroughPointByDomain.get("LK"));
                if (hasA && hasB) {
                    String netA = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":0"));
                    String netB = rootToCanonicalNet.get(uf.find("T:" + comp.uid + ":1"));
                    if (netA != null && netB != null && netA.equals(netB)) {
                        diagnostics.add(new Diagnostic("ERROR", "SHORT_CIRCUIT", comp.name,
                                "Component " + comp.name + " has both terminals connected to the same net: '" + netA + "'"));
                    }
                }
            }

            if (comp.nodesA.size() > 1 || comp.nodesB.size() > 1) {
                checkPortShort(comp.nodesA, comp.name, "input port", diagnostics);
                checkPortShort(comp.nodesB, comp.name, "output port", diagnostics);
            }
        }

        // Rule 2: Ground existence check
        boolean hasGround = allNets.stream().anyMatch(CircuitValidator::isGroundLabel);
        long powerCompCount = allComponents.stream().filter(c -> "LK".equals(c.domain) && c.typ != 31 && c.typ != 9).count();
        if (powerCompCount > 0 && !hasGround) {
            diagnostics.add(new Diagnostic("ERROR", "MISSING_GROUND", "SYSTEM",
                    "Circuit has no explicit ground reference (node '0' or 'GND'). MNA matrix solution requires a reference node."));
        }

        // Rule 3: Single-connection / floating net check
        for (Map.Entry<String, Integer> entry : netTerminalCounts.entrySet()) {
            String net = entry.getKey();
            int count = entry.getValue();
            if (count == 1 && !isGroundLabel(net)) {
                List<String> comps = netToComponents.get(net);
                String compName = comps != null && !comps.isEmpty() ? comps.get(0) : "UNKNOWN";
                diagnostics.add(new Diagnostic("WARNING", "FLOATING_NET", compName,
                        "Net '" + net + "' is only connected to component " + compName + " (floating terminal)."));
            }
        }

        // Parse Control Elements
        Map<Long, Map<String, Object>> ctrlComponents = new LinkedHashMap<>();
        Matcher ctrlMatcher = ELEMENT_CONTROL.matcher(content);
        while (ctrlMatcher.find()) {
            String block = ctrlMatcher.group(1);
            long uid = parseLong(group(block, "uniqueObjectIdentifier\\s+([0-9-]+)"), 0L);
            String name = named(group(block, "idStringDialog\\s+([^\\s\\r\\n]+)"), "CTRL_" + uid);
            int typ = parseInt(group(block, "typ\\s+([0-9]+)"), 0);
            long coupledId = parseLong(group(block, "coupledReferenceID(?:\\[\\])?[ \\t]+([0-9-]+)"), 0L);
            String coupledName = extractFirstParameterString(block);
            List<String> paramStrings = extractParameterStrings(block);

            Integer posX = parseOptionalInt(group(block, "(?:^|\\n)x\\s+([0-9-]+)"));
            Integer posY = parseOptionalInt(group(block, "(?:^|\\n)y\\s+([0-9-]+)"));
            boolean hasPos = posX != null && posY != null;
            int x = hasPos ? posX : 0;
            int y = hasPos ? posY : 0;

            // Rule 4: Control coupling integrity (Probes & Gates)
            if (typ == 1 || typ == 2 || typ == 1001 || typ == 1002) { // Voltmeter / Ammeter
                String inNode = cleanNode(group(block, "labelAnfangsKnoten(?:\\[\\])?[ \\t]*([^\\r\\n]*)"));
                String outNode = cleanNode(group(block, "labelEndKnoten(?:\\[\\])?[ \\t]*([^\\r\\n]*)"));

                boolean hasCoupledComp = (coupledId != 0L && (lkComponents.containsKey(coupledId) || thermComponents.containsKey(coupledId)))
                        || (coupledName != null && (lkComponentsByName.containsKey(coupledName) || thermComponentsByName.containsKey(coupledName)));
                boolean hasMeasuredNodes = (!inNode.isEmpty() || !outNode.isEmpty()) || (paramStrings.size() >= (typ == 1 || typ == 1001 ? 1 : 2));
                boolean touchesWires = hasPos && (
                        (wiresThroughPointByDomain.get("CTRL") != null && wiresThroughPointByDomain.get("CTRL").containsKey(new Point(x, y)))
                        || (wiresThroughPointByDomain.get("LK") != null && wiresThroughPointByDomain.get("LK").containsKey(new Point(x, y)))
                );

                if (!hasCoupledComp && !hasMeasuredNodes && !touchesWires) {
                    diagnostics.add(new Diagnostic("ERROR", "DANGLING_PROBE", name,
                            "Probe " + name + " is neither coupled to a component nor connected to circuit nodes."));
                }
            } else if (typ == 6 || typ == 1000) { // Gate
                Map<String, Object> target = null;
                if (coupledId != 0L && lkComponents.containsKey(coupledId)) {
                    target = lkComponents.get(coupledId);
                } else if (coupledName != null && lkComponentsByName.containsKey(coupledName)) {
                    target = lkComponentsByName.get(coupledName);
                }

                if (target == null) {
                    diagnostics.add(new Diagnostic("ERROR", "DANGLING_GATE", name,
                            "Gate " + name + " has coupledReferenceID=" + coupledId
                            + (coupledName != null ? " (target '" + coupledName + "')" : "")
                            + " which does not match any switch component."));
                } else {
                    int targetType = (int) target.get("type");
                    if (!isSwitchType(targetType)) {
                        diagnostics.add(new Diagnostic("WARNING", "INVALID_GATE_TARGET", name,
                                "Gate " + name + " is coupled to non-switch component type: " + targetType));
                    }
                }
            } else if (typ == 88) { // NativeC C-library block
                String libPath = "";
                String classicSel = "";
                for (String rawLine : block.split("\n")) {
                    String line = rawLine.trim();
                    if (line.startsWith("libraryPath ")) {
                        libPath = line.substring("libraryPath ".length()).trim();
                    } else if (line.startsWith("nativeCLibrary ")) {
                        classicSel = line.substring("nativeCLibrary ".length()).trim();
                    }
                }
                if (libPath.isEmpty() && classicSel.isEmpty()) {
                    diagnostics.add(new Diagnostic("ERROR", "NATIVEC_NO_LIBRARY", name,
                            "C library block " + name + " has no library configured (libraryPath)."));
                }
            } else if (typ == 61 || typ == 1016) { // ScriptBlock
                int numIn = parseInt(group(block, "anzXIN\\s+([0-9]+)"), 0);
                int numOut = parseInt(group(block, "anzYOUT\\s+([0-9]+)"), 1);
                String src = named(group(block, "<sourceCode>([\\s\\S]*?)<\\\\sourceCode>"), "");
                String staticCode = named(group(block, "<staticCode>([\\s\\S]*?)<\\\\staticCode>"), "");
                String staticVars = named(group(block, "<staticVariables>([\\s\\S]*?)<\\\\staticVariables>"), "");

                // Validate script compilation directly and identify failing statement
                try {
                    gecko.core.control.calculators.ScriptBlockCalculator calc =
                            new gecko.core.control.calculators.ScriptBlockCalculator(numIn, numOut, src, staticCode, staticVars);
                    if (!calc.isCompiled()) {
                        String failingStmt = "";
                        String normalized = gecko.core.control.calculators.ScriptBlockCalculator.normalizeCode(staticVars + "\n" + src);
                        for (String stmt : normalized.split(";")) {
                            String trimmed = stmt.trim();
                            if (trimmed.isEmpty()) continue;
                            try {
                                new gecko.core.control.calculators.ScriptBlockCalculator(numIn, numOut, trimmed + ";");
                            } catch (Exception ex) {
                                failingStmt = trimmed;
                                break;
                            }
                        }
                        String detail = failingStmt.isEmpty() ? "" : " at statement [" + failingStmt + "]";
                        diagnostics.add(new Diagnostic("ERROR", "SCRIPT_COMPILE_ERROR", name,
                                "Script block " + name + " compilation failed" + detail + ": " + calc.getCompileError()));
                    }
                } catch (Exception e) {
                    diagnostics.add(new Diagnostic("ERROR", "SCRIPT_COMPILE_ERROR", name,
                            "Script block " + name + " error: " + e.getMessage()));
                }

                // Rule 5: Script block linter
                checkScriptBlock(name, src, diagnostics);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("name", name);
            info.put("type", typ);
            ctrlComponents.put(uid, info);
        }

        long errorCount = diagnostics.stream().filter(d -> "ERROR".equals(d.level())).count();
        long warningCount = diagnostics.stream().filter(d -> "WARNING".equals(d.level())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("circuit", circuitName);
        result.put("valid", errorCount == 0);
        result.put("error_count", errorCount);
        result.put("warning_count", warningCount);
        result.put("power_components", lkComponents.size());
        result.put("thermal_components", thermComponents.size());
        result.put("control_components", ctrlComponents.size());
        result.put("nets_count", allNets.size());

        List<Map<String, String>> diagList = new ArrayList<>();
        for (Diagnostic d : diagnostics) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("level", d.level());
            m.put("rule", d.rule());
            m.put("component", d.component());
            m.put("message", d.message());
            diagList.add(m);
        }
        result.put("diagnostics", diagList);
        return result;
    }

    private static boolean isSwitchType(int typ) {
        return typ == 7 || typ == 8 || typ == 10 || typ == 11 || typ == 33 || typ == 6;
    }

    private static String extractFirstParameterString(String block) {
        String ps = group(block, "parameterString(?:\\[\\])?[ \\t]+([^\\r\\n]+)");
        if (ps == null) return null;
        ps = ps.trim();
        if (ps.startsWith("/")) ps = ps.substring(1);
        int slash = ps.indexOf('/');
        if (slash >= 0) ps = ps.substring(0, slash);
        ps = ps.trim();
        if (ps.isEmpty() || ps.equalsIgnoreCase("NIX_NIX_NIX")) return null;
        return ps;
    }

    private static List<String> extractParameterStrings(String block) {
        String ps = group(block, "parameterString(?:\\[\\])?[ \\t]+([^\\r\\n]+)");
        if (ps == null || ps.isBlank()) return List.of();
        List<String> list = new ArrayList<>();
        for (String part : ps.trim().split("/")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("NIX_NIX_NIX")) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private static boolean isTerminalConnected(ComponentData comp, int t, Map<Point, List<Integer>> lkWires) {
        if (comp.hasPos && t < comp.pins.size()) {
            if (lkWires != null && lkWires.containsKey(comp.pins.get(t))) {
                return true;
            }
        }
        String lbl = getTerminalLabel(comp, t);
        return lbl != null && !lbl.isEmpty() && !lbl.equalsIgnoreCase("NIX_NIX_NIX");
    }

    private static void checkScriptBlock(String blockName, String sourceCode, List<Diagnostic> diagnostics) {
        if (sourceCode.isBlank()) {
            diagnostics.add(new Diagnostic("WARNING", "EMPTY_SCRIPT", blockName,
                    "Script block " + blockName + " has empty source code."));
            return;
        }

        // Check for unbalanced braces
        int openBraces = 0;
        int closeBraces = 0;
        for (char ch : sourceCode.toCharArray()) {
            if (ch == '{') openBraces++;
            if (ch == '}') closeBraces++;
        }
        if (openBraces != closeBraces) {
            diagnostics.add(new Diagnostic("ERROR", "UNBALANCED_BRACES", blockName,
                    "Script block " + blockName + " has unbalanced braces: " + openBraces + " '{' vs " + closeBraces + " '}'"));
        }

        // Check for non-short-circuit division in boolean conditions, e.g. "x > 0 && 1 / x"
        if (sourceCode.matches("(?s).*&&\\s*[^&|;]*?/\\s*[a-zA-Z0-9_]+.*")) {
            diagnostics.add(new Diagnostic("WARNING", "DIVIDE_BY_ZERO_RISK", blockName,
                    "Script block contains division inside logical '&&' expression. ScriptBlockCalculator does not short-circuit sub-expressions; guard divisions with ternary or safe variable bounds."));
        }
    }

    private static void checkPortShort(List<String> nodes, String compName, String portName, List<Diagnostic> diagnostics) {
        if (nodes.size() < 2) return;
        Set<String> unique = new HashSet<>(nodes);
        if (unique.size() == 1) {
            String net = nodes.get(0);
            if (!net.isEmpty() && !net.equalsIgnoreCase("NIX_NIX_NIX")) {
                diagnostics.add(new Diagnostic("ERROR", "SHORT_CIRCUIT", compName,
                        "Component " + compName + " has all " + portName + " terminals connected to the same net: '" + net + "'"));
            }
        }
    }

    private static List<String> parseNodes(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> list = new ArrayList<>();
        for (String part : raw.trim().split("/")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("NIX_NIX_NIX")) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private static String cleanNode(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.equalsIgnoreCase("NIX_NIX_NIX")) return "";
        if (s.startsWith("/")) s = s.substring(1);
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        s = s.trim();
        return s.equalsIgnoreCase("NIX_NIX_NIX") ? "" : s;
    }

    private static String group(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String named(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null) return fallback;
        try { return Long.parseLong(value.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static List<Point> computeComponentPins(int typ, String domain, boolean hasPos, int x, int y, int orient) {
        List<Point> pins = new ArrayList<>();
        if (!hasPos) {
            return pins;
        }
        if ("LK".equals(domain)) {
            if (typ == 23) { // LK_TRANS
                int primX, primY, primOrient, secX, secY, secOrient;
                switch (orient) {
                    case ComponentTerminals.SOUTH_NORTH -> { primX = x + 1; primY = y; primOrient = 501; secX = x - 1; secY = y; secOrient = 501; }
                    case ComponentTerminals.EAST_WEST -> { primX = x; primY = y - 1; primOrient = 504; secX = x; secY = y + 1; secOrient = 504; }
                    case ComponentTerminals.WEST_EAST -> { primX = x; primY = y + 1; primOrient = 502; secX = x; secY = y - 1; secOrient = 502; }
                    default -> { primX = x - 1; primY = y; primOrient = 503; secX = x + 1; secY = y; secOrient = 503; }
                }
                int[] pDir = ComponentTerminals.flowVector(primOrient);
                int[] sDir = ComponentTerminals.flowVector(secOrient);
                pins.add(new Point(primX - pDir[0] * 2, primY - pDir[1] * 2));
                pins.add(new Point(primX + pDir[0] * 2, primY + pDir[1] * 2));
                pins.add(new Point(secX - sDir[0] * 2, secY - sDir[1] * 2));
                pins.add(new Point(secX + sDir[0] * 2, secY + sDir[1] * 2));
            } else if (typ == 33) { // LK_BJT
                int[] dir = ComponentTerminals.flowVector(orient);
                pins.add(new Point(x - dir[0] * 2, y - dir[1] * 2));
                pins.add(rotatePinOffset(x, y, orient, -2, 0));
                pins.add(new Point(x + dir[0] * 2, y + dir[1] * 2));
            } else if (typ == 31) { // LK_GLOBAL_TERMINAL
                pins.add(new Point(x, y));
            } else if (typ == 9) { // LK_M mutual coupler has no pins
                return pins;
            } else {
                int[] dir = ComponentTerminals.flowVector(orient);
                pins.add(new Point(x - dir[0] * 2, y - dir[1] * 2));
                pins.add(new Point(x + dir[0] * 2, y + dir[1] * 2));
            }
        } else if ("THERMAL".equals(domain)) {
            if (typ == 50) { // TH_GLOBAL_TERMINAL
                pins.add(new Point(x, y));
            } else if (typ == 41) { // TH_PvCHIP: single output terminal on the Y side (Tj)
                int[] dir = ComponentTerminals.flowVector(orient);
                pins.add(new Point(x + dir[0] * 2, y + dir[1] * 2));
            } else {
                int[] dir = ComponentTerminals.flowVector(orient);
                pins.add(new Point(x - dir[0] * 2, y - dir[1] * 2));
                pins.add(new Point(x + dir[0] * 2, y + dir[1] * 2));
            }
        }
        return pins;
    }

    private static String getTerminalLabel(ComponentData comp, int t) {
        if (comp.typ == 23) {
            return switch (t) {
                case 0 -> comp.nodesA.size() > 0 ? comp.nodesA.get(0) : null;
                case 1 -> comp.nodesA.size() > 1 ? comp.nodesA.get(1) : null;
                case 2 -> comp.nodesB.size() > 0 ? comp.nodesB.get(0) : null;
                case 3 -> comp.nodesB.size() > 1 ? comp.nodesB.get(1) : null;
                default -> null;
            };
        } else if (comp.typ == 33) {
            return switch (t) {
                case 0 -> comp.nodesA.size() > 0 ? comp.nodesA.get(0) : null;
                case 1 -> null;
                case 2 -> comp.nodesB.size() > 0 ? comp.nodesB.get(0) : null;
                default -> null;
            };
        } else if (comp.typ == 31 || comp.typ == 50) {
            return comp.nodesA.size() > 0 ? comp.nodesA.get(0) : (comp.nodesB.size() > 0 ? comp.nodesB.get(0) : null);
        } else if (comp.typ == 41) {
            return comp.nodesB.size() > 0 ? comp.nodesB.get(0) : (comp.nodesA.size() > 0 ? comp.nodesA.get(0) : null);
        } else {
            return switch (t) {
                case 0 -> comp.nodesA.size() > 0 ? comp.nodesA.get(0) : null;
                case 1 -> comp.nodesB.size() > 0 ? comp.nodesB.get(0) : null;
                default -> null;
            };
        }
    }

    private static String resolveCanonicalName(Set<String> set, int fallbackId) {
        for (String item : set) {
            if (item.startsWith("L:")) {
                String lbl = item.substring(item.lastIndexOf(':') + 1);
                if (isGroundLabel(lbl)) {
                    return "0";
                }
            }
        }
        for (String item : set) {
            if (item.startsWith("L:")) {
                String lbl = item.substring(item.lastIndexOf(':') + 1);
                if (!lbl.isEmpty() && !lbl.equalsIgnoreCase("NIX_NIX_NIX")) {
                    return lbl;
                }
            }
        }
        for (String item : set) {
            if (item.startsWith("W:")) {
                String id = item.substring(item.indexOf(":", 2) + 1);
                return "NET_" + id.replace(':', '_');
            }
        }
        return "NET_" + fallbackId;
    }

    private static List<Point> rasterizeSegment(Point p1, Point p2) {
        List<Point> points = new ArrayList<>();
        int dx = Integer.compare(p2.x, p1.x);
        int dy = Integer.compare(p2.y, p1.y);
        int currX = p1.x;
        int currY = p1.y;
        points.add(new Point(currX, currY));
        while (currX != p2.x || currY != p2.y) {
            if (currX != p2.x) currX += dx;
            if (currY != p2.y) currY += dy;
            points.add(new Point(currX, currY));
        }
        return points;
    }

    private static int[] parseIntArray(String line) {
        if (line == null || line.isBlank()) return new int[0];
        String[] parts = line.trim().split("\\s+");
        List<Integer> list = new ArrayList<>();
        for (String p : parts) {
            try {
                list.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException ignored) {}
        }
        int[] res = new int[list.size()];
        for (int i = 0; i < list.size(); i++) res[i] = list.get(i);
        return res;
    }

    private static Point rotatePinOffset(int cx, int cy, int orient, int posX, int posY) {
        return switch (orient) {
            case ComponentTerminals.EAST_WEST -> new Point(cx + posY, cy + posX);
            case ComponentTerminals.SOUTH_NORTH -> new Point(cx - posX, cy + posY);
            case ComponentTerminals.WEST_EAST -> new Point(cx - posY, cy - posX);
            default -> new Point(cx + posX, cy - posY); // NORTH_SOUTH (503)
        };
    }

    private static boolean isGroundLabel(String label) {
        if (label == null) return false;
        String trimmed = label.trim().toLowerCase();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        return trimmed.equals("0") || trimmed.equals("gnd") || trimmed.equals("ground");
    }

    private static String cleanLabel(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("/")) s = s.substring(1).trim();
        if (s.equalsIgnoreCase("NIX_NIX_NIX")) return "";
        return s;
    }

    private static Integer parseOptionalInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
