package gecko.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only .ipes inspection — a faithful port of the Python server's
 * {@code gecko_inspect_circuit}, including its regex-based (not parser-based)
 * character. Deviations are bug-for-bug on purpose so LLM clients see the
 * same output shape.
 */
final class CircuitInspector {

    private static final Pattern ELEMENT_LK = Pattern.compile("<ElementLK>([\\s\\S]*?)<\\\\ElementLK>");
    private static final Pattern ELEMENT_CONTROL = Pattern.compile("<ElementCONTROL>([\\s\\S]*?)<\\\\ElementCONTROL>");
    private static final Pattern DT = Pattern.compile("dt\\s+([0-9.eE+-]+)");
    private static final Pattern DURATION = Pattern.compile("(?:tend|dauer|duration)\\s+([0-9.eE+-]+)", Pattern.CASE_INSENSITIVE);

    private CircuitInspector() {
    }

    static Map<String, Object> inspect(Path path) throws IOException {
        String content = IpesSupport.readIpesText(path);

        List<Map<String, Object>> lkElements = new ArrayList<>();
        Matcher matcher = ELEMENT_LK.matcher(content);
        while (matcher.find()) {
            String block = matcher.group(1);
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("name", named(group(block, "idStringDialog\\s+([^\\s\\r\\n]+)"), "unknown"));
            element.put("type", (int) doubleOr(group(block, "typ\\s+([0-9]+)"), 0));
            element.put("id", (int) doubleOr(group(block, "uniqueObjectIdentifier\\s+([0-9-]+)"), 0));
            element.put("parameters", floatList(block));
            element.put("in_nodes", named(group(block, "labelAnfangsKnoten\\[\\]\\s+([^\\r\\n]+)"), ""));
            element.put("out_nodes", named(group(block, "labelEndKnoten\\[\\]\\s+([^\\r\\n]+)"), ""));
            lkElements.add(element);
        }

        List<Map<String, Object>> ctrlElements = new ArrayList<>();
        matcher = ELEMENT_CONTROL.matcher(content);
        while (matcher.find()) {
            String block = matcher.group(1);
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("name", named(group(block, "idStringDialog\\s+([^\\s\\r\\n]+)"), "unknown"));
            element.put("type", (int) doubleOr(group(block, "typ\\s+([0-9]+)"), 0));
            element.put("id", (int) doubleOr(group(block, "uniqueObjectIdentifier\\s+([0-9-]+)"), 0));
            element.put("coupled_id", (int) doubleOr(group(block, "coupledReferenceID\\[\\]\\s+([0-9-]+)"), 0));
            Matcher source = Pattern.compile("<sourceCode>([\\s\\S]*?)<\\\\sourceCode>").matcher(block);
            if (source.find()) {
                element.put("sourceCode", source.group(1).strip());
            }
            ctrlElements.add(element);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("file", path.toString());
        result.put("dt", doubleOr(group(content, DT.pattern()), 1e-6));
        result.put("duration", doubleOr(group(content, DURATION.pattern()), 0.02));
        result.put("lk_component_count", lkElements.size());
        result.put("lk_components", lkElements);
        result.put("control_component_count", ctrlElements.size());
        result.put("control_components", ctrlElements);
        return result;
    }

    private static String group(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String named(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private static double doubleOr(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static List<Double> floatList(String block) {
        String parameters = group(block, "parameter\\[\\]\\s+([^\\r\\n]+)");
        List<Double> values = new ArrayList<>();
        if (parameters == null) {
            return values;
        }
        for (String token : parameters.trim().split("\\s+")) {
            if (!token.isEmpty()) {
                try {
                    values.add(Double.parseDouble(token));
                } catch (NumberFormatException e) {
                    // Python port: float() of garbage would raise; here we skip
                }
            }
        }
        return values;
    }
}
