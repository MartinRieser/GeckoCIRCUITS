package gecko.mcp;

import java.util.Locale;

/**
 * Python-compatible number formatting, so templates ported from the Python
 * MCP server render byte-identically (guarded by golden equivalence tests).
 */
final class PyFormat {

    private PyFormat() {
    }

    /** Formats {@code value} like Python's {@code f"{value:{spec}}"}. */
    static String format(double value, String spec) {
        String javaSpec = switch (spec) {
            case "" -> null;
            default -> "%" + spec;
        };
        if (javaSpec == null) {
            return pyStr(value);
        }
        return String.format(Locale.ROOT, javaSpec, value);
    }

    /** Python {@code str()} for the plain-float range used by the templates. */
    static String pyStr(double value) {
        return Double.toString(value);
    }

    /** Python {@code str.rstrip()}-style trim (whitespace only, like strip()). */
    static String strip(String text) {
        return text.strip();
    }

    /** Rounds {@code value} to {@code decimals} decimal places; returns {@code 0.0} if non-finite. */
    static double round(double value, int decimals) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}
