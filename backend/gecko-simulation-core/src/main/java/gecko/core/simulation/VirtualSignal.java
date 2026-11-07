package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.io.CircuitModel;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a virtual measurement signal resolved dynamically from circuit topology
 * without requiring physical voltmeter or ammeter components on the schematic canvas.
 *
 * <p>Supported naming syntaxes (SPICE standard):
 * <ul>
 *   <li>{@code I(<component>)}: Branch current flowing through the named component (e.g. {@code I(L_filter)}).</li>
 *   <li>{@code V(<component>)}: Differential voltage across the two terminals of the named component (e.g. {@code V(R_load)}).</li>
 *   <li>{@code V(<net>)}: Single-ended voltage of a labeled net relative to ground (e.g. {@code V(load_p)}).</li>
 *   <li>{@code V(<netA>|<netB>)}: Differential voltage between two labeled nets (e.g. {@code V(out_a|out_b)}).</li>
 * </ul>
 *
 * <p>During simulation steps, each {@link VirtualSignal} is evaluated directly against the MNA solution vector
 * and component current array in $O(1)$ time without dynamic string parsing or object allocations.
 */
public final class VirtualSignal {

    /**
     * Classification of virtual measurement quantities.
     */
    public enum Kind {
        /** Branch current flowing through an electrical component. */
        COMPONENT_CURRENT,
        /** Voltage drop across a two-terminal component (nodeX - nodeY). */
        COMPONENT_VOLTAGE,
        /** Single-ended node voltage to reference ground (nodeA - 0). */
        NODE_VOLTAGE,
        /** Differential voltage between two distinct circuit nodes (nodeA - nodeB). */
        DIFFERENTIAL_VOLTAGE
    }

    private static final Pattern CURRENT_PATTERN = Pattern.compile("^I\\s*\\(\\s*([^)]+)\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOLTAGE_PATTERN = Pattern.compile("^V\\s*\\(\\s*([^)]+)\\s*\\)$", Pattern.CASE_INSENSITIVE);

    private final String name;
    private final Kind kind;
    private final int elementIndex;
    private final int nodeA;
    private final int nodeB;

    /**
     * Constructs a new virtual signal descriptor.
     *
     * @param name original signal name (e.g. {@code I(L_filter)} or {@code V(out_a|out_b)})
     * @param kind measurement classification
     * @param elementIndex element index in the netlist, or -1 for pure node probes
     * @param nodeA primary/positive terminal node index in the MNA solution vector
     * @param nodeB reference/negative terminal node index in the MNA solution vector (0 for ground)
     */
    public VirtualSignal(String name, Kind kind, int elementIndex, int nodeA, int nodeB) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
        this.elementIndex = elementIndex;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }

    /**
     * Attempts to parse and resolve a raw signal name into a {@link VirtualSignal}
     * using the circuit model and netlist topology.
     *
     * @param rawName candidate signal name (e.g. {@code I(L_filter)}, {@code V(out_a|out_b)}, or standard net label)
     * @param model circuit model containing named components
     * @param netlist compiled circuit netlist with solved element indices and node tables
     * @return resolved {@link VirtualSignal}, or {@code null} if the name does not match virtual syntax or targets cannot be resolved
     */
    public static VirtualSignal resolve(String rawName, CircuitModel model, CircuitNetlist netlist) {
        if (rawName == null || netlist == null) {
            return null;
        }
        final String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // 1. Current probe: I(<component>)
        final Matcher iMatcher = CURRENT_PATTERN.matcher(trimmed);
        if (iMatcher.matches()) {
            final String target = iMatcher.group(1).trim();
            final int elemIdx = resolveElementIndex(model, netlist, target);
            if (elemIdx >= 0) {
                return new VirtualSignal(trimmed, Kind.COMPONENT_CURRENT, elemIdx, -1, -1);
            }
            return null;
        }

        // 2. Voltage probe: V(<component>), V(<net>), or V(<netA>|<netB>)
        final Matcher vMatcher = VOLTAGE_PATTERN.matcher(trimmed);
        if (vMatcher.matches()) {
            final String target = vMatcher.group(1).trim();

            // 2a. Differential net voltage: V(netA|netB)
            if (target.contains("|")) {
                final String[] parts = target.split("\\|", 2);
                final int nodeA = nodeForLabel(netlist, parts[0]);
                final int nodeB = nodeForLabel(netlist, parts[1]);
                if (nodeA >= 0 && nodeB >= 0) {
                    return new VirtualSignal(trimmed, Kind.DIFFERENTIAL_VOLTAGE, -1, nodeA, nodeB);
                }
                return null;
            }

            // 2b. Voltage across component: V(component)
            final int elemIdx = resolveElementIndex(model, netlist, target);
            if (elemIdx >= 0) {
                final int nodeX = netlist.getNodeX(elemIdx);
                final int nodeY = netlist.getNodeY(elemIdx);
                return new VirtualSignal(trimmed, Kind.COMPONENT_VOLTAGE, elemIdx, nodeX, nodeY);
            }

            // 2c. Voltage of net to ground: V(net)
            final int node = nodeForLabel(netlist, target);
            if (node >= 0) {
                return new VirtualSignal(trimmed, Kind.NODE_VOLTAGE, -1, node, 0);
            }
        }

        return null;
    }

    /**
     * Resolves the component element index in the netlist by name or unique ID.
     *
     * @param model circuit model
     * @param netlist circuit netlist
     * @param name component name
     * @return element index, or -1 if not found
     */
    public static int resolveElementIndex(CircuitModel model, CircuitNetlist netlist, String name) {
        if (netlist == null || name == null || name.isBlank()) {
            return -1;
        }
        final int byNetlist = netlist.indexOfElementName(name);
        if (byNetlist >= 0) {
            return byNetlist;
        }
        if (model != null) {
            for (CircuitModel.ComponentData comp : model.getAllComponents()) {
                if (name.equalsIgnoreCase(comp.getName())) {
                    final int index = netlist.indexOfUid(comp.getUniqueObjectIdentifier());
                    if (index >= 0) {
                        return index;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Resolves the node index for a net label.
     *
     * @param netlist circuit netlist
     * @param label net label string (e.g. "load_p", "/dc_pos", "gnd", "0")
     * @return node index in the MNA solution vector, or -1 if the label is unmapped/unknown
     */
    public static int nodeForLabel(CircuitNetlist netlist, String label) {
        if (netlist == null || label == null) {
            return 0;
        }
        final String trimmed = label.trim();
        if (trimmed.isEmpty() || trimmed.equals("NIX_NIX_NIX")) {
            return 0;
        }
        final String bare = trimmed.startsWith("/") ? trimmed.substring(1).trim() : trimmed;
        if (bare.isEmpty() || bare.equals("0") || bare.equalsIgnoreCase("gnd") || bare.equalsIgnoreCase("ground")) {
            return 0;
        }
        if (netlist.getLabelResolver() == null) {
            return -1;
        }
        int index = netlist.getLabelResolver().getIndex(bare);
        if (index < 0 && !bare.equals(trimmed)) {
            index = netlist.getLabelResolver().getIndex(trimmed);
        }
        return index;
    }

    /**
     * Evaluates the physical value of this virtual signal at the current simulation step.
     *
     * @param netlist current circuit netlist (providing component currents)
     * @param nodeVoltages current MNA solution vector $P$ (providing node voltages)
     * @return calculated measurement value as a float
     */
    public float evaluate(CircuitNetlist netlist, double[] nodeVoltages) {
        if (kind == Kind.COMPONENT_CURRENT) {
            if (netlist == null) {
                return 0.0f;
            }
            final double[] currents = netlist.getLastComponentCurrentsRef();
            return (float) (elementIndex >= 0 && currents != null && elementIndex < currents.length
                    ? currents[elementIndex] : 0.0);
        }

        if (nodeVoltages == null) {
            return 0.0f;
        }
        final double vA = nodeA >= 0 && nodeA < nodeVoltages.length ? nodeVoltages[nodeA] : 0.0;
        final double vB = nodeB >= 0 && nodeB < nodeVoltages.length ? nodeVoltages[nodeB] : 0.0;
        return (float) (vA - vB);
    }

    /**
     * Gets the original signal name.
     *
     * @return signal name
     */
    public String name() {
        return name;
    }

    /**
     * Gets the measurement kind.
     *
     * @return measurement kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Gets the element index in the netlist.
     *
     * @return element index, or -1 for pure node probes
     */
    public int elementIndex() {
        return elementIndex;
    }

    /**
     * Gets primary/positive node index.
     *
     * @return node index
     */
    public int nodeA() {
        return nodeA;
    }

    /**
     * Gets reference/negative node index (0 for ground).
     *
     * @return node index
     */
    public int nodeB() {
        return nodeB;
    }

    @Override
    public String toString() {
        return "VirtualSignal[name=" + name + ", kind=" + kind + ", elem=" + elementIndex
                + ", nodeA=" + nodeA + ", nodeB=" + nodeB + "]";
    }
}

