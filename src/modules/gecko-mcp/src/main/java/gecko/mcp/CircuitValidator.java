package gecko.mcp;

import java.io.IOException;
import java.nio.file.Files;
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

    private static final Pattern ELEMENT_LK = Pattern.compile("<ElementLK>([\\s\\S]*?)<\\\\ElementLK>");
    private static final Pattern ELEMENT_CONTROL = Pattern.compile("<ElementCONTROL>([\\s\\S]*?)<\\\\ElementCONTROL>");
    private static final Pattern CONNECTION_LK = Pattern.compile("<Connection>([\\s\\S]*?)<\\\\Connection>");

    private CircuitValidator() {
    }

    public static Map<String, Object> validate(Path ipesPath) throws IOException {
        String content = IpesSupport.readIpesText(ipesPath);
        return validateContent(content, ipesPath.getFileName().toString());
    }

    public static Map<String, Object> validateContent(String content, String circuitName) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        // Parse Power Elements
        Map<Long, Map<String, Object>> lkComponents = new LinkedHashMap<>();
        Map<String, List<String>> netToComponents = new LinkedHashMap<>();
        Map<String, Integer> netPointCounts = new LinkedHashMap<>();
        Set<String> allNets = new LinkedHashSet<>();

        Matcher lkMatcher = ELEMENT_LK.matcher(content);
        while (lkMatcher.find()) {
            String block = lkMatcher.group(1);
            long uid = parseLong(group(block, "uniqueObjectIdentifier\\s+([0-9-]+)"), 0L);
            String name = named(group(block, "idStringDialog\\s+([^\\s\\r\\n]+)"), "COMP_" + uid);
            int typ = parseInt(group(block, "typ\\s+([0-9]+)"), 0);

            String nodeA = cleanNode(group(block, "labelAnfangsKnoten\\[\\]\\s+([^\\r\\n]+)"));
            String nodeB = cleanNode(group(block, "labelEndKnoten\\[\\]\\s+([^\\r\\n]+)"));

            allNets.add(nodeA);
            allNets.add(nodeB);

            netToComponents.computeIfAbsent(nodeA, k -> new ArrayList<>()).add(name);
            netToComponents.computeIfAbsent(nodeB, k -> new ArrayList<>()).add(name);

            // Rule 1: Short-circuited component
            if (!nodeA.isEmpty() && nodeA.equalsIgnoreCase(nodeB)) {
                diagnostics.add(new Diagnostic("ERROR", "SHORT_CIRCUIT", name,
                        "Component " + name + " has both terminals connected to the same net: '" + nodeA + "'"));
            }

            Map<String, Object> info = new HashMap<>();
            info.put("name", name);
            info.put("type", typ);
            info.put("nodeA", nodeA);
            info.put("nodeB", nodeB);
            lkComponents.put(uid, info);
        }

        // Parse Connections
        Matcher connMatcher = CONNECTION_LK.matcher(content);
        while (connMatcher.find()) {
            String block = connMatcher.group(1);
            String label = named(group(block, "label\\s+([^\\s\\r\\n]+)"), "");
            int pts = parseInt(group(block, "zeigerAktuell\\s+([0-9]+)"), 0);
            if (!label.isEmpty()) {
                netPointCounts.put(label, pts);
            }
        }

        // Rule 2: Ground existence check
        boolean hasGround = allNets.stream().anyMatch(n -> n.equals("0") || n.equalsIgnoreCase("gnd") || n.equalsIgnoreCase("ground"));
        if (!hasGround) {
            diagnostics.add(new Diagnostic("ERROR", "MISSING_GROUND", "SYSTEM",
                    "Circuit has no explicit ground reference (node '0' or 'GND'). MNA matrix solution requires a reference node."));
        }

        // Rule 3: Single-connection / floating net check
        for (Map.Entry<String, List<String>> entry : netToComponents.entrySet()) {
            String net = entry.getKey();
            List<String> comps = entry.getValue();
            if (comps.size() == 1 && !net.equals("0") && !net.equalsIgnoreCase("gnd")) {
                diagnostics.add(new Diagnostic("WARNING", "FLOATING_NET", comps.get(0),
                        "Net '" + net + "' is only connected to component " + comps.get(0) + " (floating terminal)."));
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
            long coupledId = parseLong(group(block, "coupledReferenceID\\[\\]\\s+([0-9-]+)"), 0L);

            // Rule 4: Control coupling integrity (Probes & Gates)
            if (typ == 1 || typ == 2) { // Voltmeter / Ammeter
                if (coupledId > 0L) {
                    if (!lkComponents.containsKey(coupledId)) {
                        diagnostics.add(new Diagnostic("ERROR", "DANGLING_PROBE", name,
                                "Probe " + name + " has coupledReferenceID=" + coupledId + " which does not match any power component."));
                    }
                } else {
                    String inNode = cleanNode(group(block, "labelAnfangsKnoten\\[\\]\\s+([^\\r\\n]+)"));
                    String outNode = cleanNode(group(block, "labelEndKnoten\\[\\]\\s+([^\\r\\n]+)"));
                    if (inNode.isEmpty() && outNode.isEmpty()) {
                        diagnostics.add(new Diagnostic("ERROR", "DANGLING_PROBE", name,
                                "Probe " + name + " is neither coupled to a component nor connected to circuit nodes."));
                    }
                }
            } else if (typ == 6) { // Gate
                if (coupledId == 0L || !lkComponents.containsKey(coupledId)) {
                    diagnostics.add(new Diagnostic("ERROR", "DANGLING_GATE", name,
                            "Gate " + name + " has coupledReferenceID=" + coupledId + " which does not match any switch component."));
                } else {
                    int targetType = (int) lkComponents.get(coupledId).get("type");
                    if (targetType != 7 && targetType != 10 && targetType != 11 && targetType != 8) {
                        diagnostics.add(new Diagnostic("WARNING", "INVALID_GATE_TARGET", name,
                                "Gate " + name + " is coupled to non-switch component type: " + targetType));
                    }
                }
            } else if (typ == 61) { // ScriptBlock
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
                        // Pinpoint failing statement
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

    private static String cleanNode(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("/")) s = s.substring(1);
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        return s.trim();
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
}
