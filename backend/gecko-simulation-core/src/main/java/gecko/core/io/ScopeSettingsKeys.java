package gecko.core.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical key names and block markers for the per-scope display settings
 * persisted inside the {@code <scopeSettings>} sub-block of .ipes element
 * blocks: scopeLayout, yScaleMode, hiddenSignals and cursorsEnabled.
 *
 * <p>Single source of truth shared by {@link CircuitFileParser},
 * {@link CircuitFileWriter} and the REST parameter whitelist in the edit
 * service, so the three can never drift apart: a key renamed here
 * automatically changes parsing, writing and whitelist validation.</p>
 *
 * <p>Value formats: scopeLayout and yScaleMode are plain strings
 * ({@code overlay}/{@code stacked} and {@code auto}/{@code fixed}),
 * cursorsEnabled is a boolean, and hiddenSignals is stored as a
 * comma-separated signal-name list — signal (net) labels must therefore not
 * contain commas for the round trip to survive.</p>
 */
public final class ScopeSettingsKeys {

    /** Opening marker of the persisted sub-block. */
    public static final String BLOCK_START = "<scopeSettings>";

    /** Closing marker of the persisted sub-block (classic .ipes backslash style). */
    public static final String BLOCK_END = "<\\scopeSettings>";

    /** Display layout mode: 'overlay' or 'stacked'. */
    public static final String SCOPE_LAYOUT = "scopeLayout";

    /** Vertical scale mode: 'auto' or 'fixed'. */
    public static final String Y_SCALE_MODE = "yScaleMode";

    /** Comma-separated list of hidden channel names. */
    public static final String HIDDEN_SIGNALS = "hiddenSignals";

    /** Whether measurement cursors are enabled. */
    public static final String CURSORS_ENABLED = "cursorsEnabled";

    /** All setting keys in the deterministic write order of the sub-block. */
    public static final List<String> ORDERED_KEYS =
            Arrays.asList(SCOPE_LAYOUT, Y_SCALE_MODE, HIDDEN_SIGNALS, CURSORS_ENABLED);

    /** All setting keys as a set for membership checks. */
    public static final Set<String> ALL_KEYS =
            Collections.unmodifiableSet(new LinkedHashSet<>(ORDERED_KEYS));

    /** The subset of keys whose values are booleans rather than strings. */
    public static final Set<String> BOOLEAN_KEYS = Set.of(CURSORS_ENABLED);

    private ScopeSettingsKeys() {
        // constants holder only
    }
}
