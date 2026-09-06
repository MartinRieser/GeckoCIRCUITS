package gecko.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the extracted .ipes templates and substitutes the numbered hole
 * markers (§0§, §1§, ...) in order. Templates and hole lists come from
 * scripts/desktop/extract-templates.py; golden equivalence tests guard
 * the port against drift.
 */
final class TemplateEngine {

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

    /** Replaces §N§ markers with the given values, in order. */
    static String substitute(String template, String... values) {
        String result = template;
        for (int i = 0; i < values.length; i++) {
            result = result.replace("§" + i + "§", values[i]);
        }
        return result;
    }
}
