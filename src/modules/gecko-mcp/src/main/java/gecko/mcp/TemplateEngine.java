package gecko.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads the extracted .ipes templates and substitutes the numbered hole
 * markers (§0§, §1§, ...). Single-pass regex substitution prevents sequential
 * placeholder collisions.
 */
final class TemplateEngine {

    private static final Pattern HOLE_PATTERN = Pattern.compile("§(\\d+)§");

    private TemplateEngine() {
    }

    static String load(String name) throws IOException {
        try (InputStream in = TemplateEngine.class.getResourceAsStream("/templates/" + name)) {
            if (in == null) {
                throw new IOException("template resource missing: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Replaces §N§ markers with the given values in a single pass. */
    static String substitute(String template, String... values) {
        Matcher matcher = HOLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String replacement = index < values.length && values[index] != null ? values[index] : matcher.group(0);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
