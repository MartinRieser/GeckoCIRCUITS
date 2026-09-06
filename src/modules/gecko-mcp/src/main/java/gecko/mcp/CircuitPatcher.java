package gecko.mcp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * .ipes patching — faithful port of the Python {@code gecko_patch_component}
 * and {@code gecko_set_script_code} tools.
 */
final class CircuitPatcher {

    private CircuitPatcher() {
    }

    // Tempered block pattern: unlike the Python original, it cannot skip past
    // other element blocks, so the FIRST parameter[] in the match really
    // belongs to the named component (the Python regex patched the wrong
    // element when the target was not the first block).
    private static final String GUARD = "(?:(?!<Element(?:LK|CONTROL)>|<\\\\Element(?:LK|CONTROL)>)[\\s\\S])*?";

    private static Pattern blockPattern(String kind, String componentName) {
        return Pattern.compile("(<Element" + kind + ">(" + GUARD + ")idStringDialog\\s+"
                + Pattern.quote(componentName) + "[\\s\\S]*?<\\\\Element" + kind + ">)");
    }

    static Map<String, Object> patchComponent(String circuitPath, String componentName,
                                              Map<String, Object> parameters,
                                              String outputPath) throws IOException {
        Path path = IpesSupport.resolve(circuitPath);
        if (!IpesSupport.exists(path)) {
            throw new IllegalArgumentException("Circuit file not found: " + path);
        }
        String content = IpesSupport.readIpesText(path);

        Matcher matcher = blockPattern("(?:LK|CONTROL)", componentName).matcher(content);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Component '" + componentName + "' not found in circuit");
        }
        String block = matcher.group(1);

        Matcher paramMatch = Pattern.compile("parameter\\[\\]\\s+([^\\r\\n]+)").matcher(block);
        String newBlock = block;
        if (paramMatch.find()) {
            String[] tokens = paramMatch.group(1).trim().split("\\s+");
            double[] params = new double[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                params[i] = Double.parseDouble(tokens[i]);
            }
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey();
                double value = asDouble(entry.getValue());
                if (key.startsWith("param")) {
                    try {
                        int index = Integer.parseInt(key.substring(5));
                        if (index < params.length) {
                            params[index] = value;
                        }
                    } catch (NumberFormatException e) {
                        // Python port: int() failure is ignored
                    }
                } else if (key.equals("resistance") || key.equals("inductance")
                        || key.equals("capacitance") || key.equals("amplitude")) {
                    if (params.length > 0) {
                        params[0] = value;
                    }
                }
            }
            StringBuilder line = new StringBuilder("parameter[] ");
            for (double param : params) {
                line.append(PyFormat.pyStr(param)).append(' ');
            }
            newBlock = newBlock.replaceFirst("parameter\\[\\]\\s+[^\\r\\n]+",
                    Matcher.quoteReplacement(line.toString()));
        }

        content = content.replace(block, newBlock);
        Path target = outputPath != null ? IpesSupport.resolve(outputPath) : path;
        IpesSupport.writeIpesText(target, content, true);
        return Map.of("status", "SUCCESS", "component", componentName, "updated_file", target.toString());
    }

    static Map<String, Object> setScriptCode(String circuitPath, String blockName, String sourceCode,
                                             String staticVariables, String staticCode,
                                             String outputPath) throws IOException {
        Path path = IpesSupport.resolve(circuitPath);
        if (!IpesSupport.exists(path)) {
            throw new IllegalArgumentException("Circuit file not found: " + path);
        }
        String content = IpesSupport.readIpesText(path);

        Pattern pattern = blockPattern("CONTROL", blockName);
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Script block '" + blockName + "' not found in circuit");
        }
        String block = matcher.group(1);
        String newBlock = block;

        newBlock = replaceOrInsertTag(newBlock, "sourceCode", sourceCode.strip());
        if (staticVariables != null && !staticVariables.isEmpty()) {
            newBlock = replaceOrInsertTag(newBlock, "staticVariables", staticVariables.strip());
        }
        if (staticCode != null && !staticCode.isEmpty()) {
            newBlock = replaceOrInsertTag(newBlock, "staticCode", staticCode.strip());
        }

        content = content.replace(block, newBlock);
        Path target = outputPath != null ? IpesSupport.resolve(outputPath) : path;
        IpesSupport.writeIpesText(target, content, true);
        return Map.of("status", "SUCCESS", "block", blockName, "updated_file", target.toString());
    }

    /** Replaces {@code <tag>...<\tag>} with {@code <tag>\ntext\n<\tag>}, or inserts it before closing block tag if absent. */
    private static String replaceOrInsertTag(String text, String tag, String newContent) {
        String replacement = "<" + tag + ">\n" + newContent + "\n<\\" + tag + ">";
        String tagRegex = "<" + tag + ">[\\s\\S]*?<\\\\" + tag + ">";
        if (Pattern.compile(tagRegex).matcher(text).find()) {
            return text.replaceAll(tagRegex, Matcher.quoteReplacement(replacement));
        }
        int insertPos = text.lastIndexOf("<\\ElementCONTROL>");
        if (insertPos >= 0) {
            return text.substring(0, insertPos) + replacement + "\n" + text.substring(insertPos);
        }
        return text + "\n" + replacement;
    }

    private static double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
