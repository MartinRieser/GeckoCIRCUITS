/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit.netlist;

import gecko.core.circuit.ComponentTerminals;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;

import java.util.*;
import java.util.stream.Stream;

/**
 * Factory class for building CircuitNetlist from various sources.
 *
 * <p>Bridges the gap between circuit file parsing (CircuitModel) and simulation
 * (CircuitNetlist/INetList). Provides static factory methods to construct netlists
 * with proper initialization of MNA (Modified Nodal Analysis) data structures.</p>
 */
public class NetlistBuilder {

    private NetlistBuilder() {
        // Utility class - not instantiable
    }

    private record GridPoint(int x, int y) {}

    private static class DisjointSet<T> {
        private final Map<T, T> parent = new HashMap<>();

        public T find(T item) {
            parent.putIfAbsent(item, item);
            if (!parent.get(item).equals(item)) {
                parent.put(item, find(parent.get(item)));
            }
            return parent.get(item);
        }

        public void union(T a, T b) {
            T rootA = find(a);
            T rootB = find(b);
            if (!rootA.equals(rootB)) {
                parent.put(rootA, rootB);
            }
        }
    }

    /**
     * Build a simple CircuitNetlist for testing with given dimensions.
     */
    public static CircuitNetlist buildEmpty(
            int nodeCount, int voltageSourceCount, int elementCount) {

        if (nodeCount < 0) {
            throw new IllegalArgumentException("nodeCount must be non-negative, got: " + nodeCount);
        }
        if (voltageSourceCount < 0) {
            throw new IllegalArgumentException("voltageSourceCount must be non-negative, got: " + voltageSourceCount);
        }
        if (elementCount < 0) {
            throw new IllegalArgumentException("elementCount must be non-negative, got: " + elementCount);
        }

        CircuitTypCore[] types = new CircuitTypCore[elementCount];
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] voltageSourceNr = new int[elementCount];
        double[][] params = new double[elementCount][];

        for (int i = 0; i < elementCount; i++) {
            types[i] = CircuitTypCore.LK_R;  // Default to resistor
            nodeX[i] = 0;                     // Connected to ground
            nodeY[i] = 0;                     // Connected to ground
            voltageSourceNr[i] = -1;          // Not a voltage source
            params[i] = new double[1];        // Single parameter array
            params[i][0] = 0.0;               // Zero resistance
        }

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNr, params,
                            nodeCount > 0 ? nodeCount - 1 : 0,  // maxNodeIndex
                            voltageSourceCount,                   // maxVoltageSourceIndex
                            elementCount);

        return netlist;
    }

    /**
     * Build a CircuitNetlist from a parsed CircuitModel.
     */
    public static CircuitNetlist buildFromCircuitModel(CircuitModel model) {
        if (model == null) {
            return buildEmpty(0, 0, 0);
        }

        List<CircuitModel.ComponentData> allComponents = new ArrayList<>();
        allComponents.addAll(model.getCircuitComponents());
        allComponents.addAll(model.getThermalComponents());
        // Control-domain blocks are deliberately excluded: their file type numbers
        // come from a separate CONTROL namespace (e.g. typ 1 = VOLT probe) and would
        // collide with LK type numbers (typ 1 = resistor), creating phantom elements.

        if (allComponents.isEmpty()) {
            return buildEmpty(0, 0, 0);
        }

        // Ideal transformers (type 23) expand into their hidden-subcircuit
        // winding pair before any topology work, so every build path sees
        // ordinary two-terminal elements. The winding pairs are registered as
        // VCVS + z-current-mirror couplings on the finished netlist.
        // Mutual-inductance couplers (type 9) are declared between existing
        // coupled inductors and are registered on the finished netlist.
        TransformerExpansion expansion = expandIdealTransformers(allComponents);
        allComponents = expansion.components();
        List<CircuitModel.ComponentData> mutualCouplers = new ArrayList<>();
        for (CircuitModel.ComponentData comp : allComponents) {
            if (comp.getType() == 9) {
                mutualCouplers.add(comp);
            }
        }

        List<CircuitModel.ComponentData> branchComponents = new ArrayList<>();
        for (CircuitModel.ComponentData comp : allComponents) {
            if (!isNonBranchComponent(comp.getType())) {
                branchComponents.add(comp);
            }
        }

        if (branchComponents.isEmpty()) {
            return buildEmpty(0, 0, 0);
        }

        List<CircuitModel.ConnectionData> connections = model.getConnections();
        boolean hasWires = hasSchematicWires(connections);

        // Count how many terminals have real (non-sentinel) net labels from classic file export
        long explicitLabelCount = allComponents.stream()
                .flatMap(c -> Stream.concat(Arrays.stream(c.getTerminalXLabels()), Arrays.stream(c.getTerminalYLabels())))
                .filter(NetlistBuilder::isValidLabel)
                .count();

        // If circuit has explicit terminal labels on components (from classic GeckoCIRCUITS file export)
        // or complex-pin components, use label matching with series terminal coordinate sharing
        if (explicitLabelCount > 0) {
            return buildFromComponentsWithLabels(allComponents, connections, expansion.pairs(), mutualCouplers);
        }

        if (hasWires) {
            return buildFromWiresAndComponents(allComponents, connections, expansion.pairs(), mutualCouplers);
        }

        return buildWithEstimatedDimensions(allComponents);
    }

    /** Result of the ideal-transformer component expansion. */
    private record TransformerExpansion(
            List<CircuitModel.ComponentData> components,
            List<WindingPair> pairs) {
    }

    /** One expanded transformer: primary/secondary synthetic windings + ratio gain. */
    private record WindingPair(
            CircuitModel.ComponentData prim,
            CircuitModel.ComponentData sec,
            double gain) {
    }

    /**
     * Classic {@code TerminalRelativePosition.getPointFromDirection}: rotates a
     * component-relative pin offset (posX, posY with posY pointing "up") into
     * grid coordinates for the component's orientation.
     */
    private static GridPoint rotatePinOffset(CircuitModel.ComponentData comp, int posX, int posY) {
        int cx = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[0] : 0;
        int cy = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[1] : 0;
        int orient = comp.getOrientation() != 0 ? comp.getOrientation() : ComponentTerminals.NORTH_SOUTH;
        return switch (orient) {
            case ComponentTerminals.EAST_WEST -> new GridPoint(cx + posY, cy + posX);
            case ComponentTerminals.SOUTH_NORTH -> new GridPoint(cx - posX, cy + posY);
            case ComponentTerminals.WEST_EAST -> new GridPoint(cx - posY, cy - posX);
            default -> new GridPoint(cx + posX, cy - posY); // NORTH_SOUTH
        };
    }

    /**
     * Terminal points for components with more than two pins. BJT (33):
     * collector = two-port input, base = classic offset (-2, 0), emitter =
     * two-port output.
     */
    private static GridPoint[] computeComponentTerminalsAny(CircuitModel.ComponentData comp) {
        if (comp.getType() == 33) {
            GridPoint[] ce = computeComponentTerminals(comp);
            return new GridPoint[]{ce[0], rotatePinOffset(comp, -2, 0), ce[1]};
        }
        return computeComponentTerminals(comp);
    }

    /** First positive value from the parameter map ("param<idx>"), else dflt. */
    private static double positiveParameter(CircuitModel.ComponentData comp, int idx, double dflt) {
        Object v = comp.getParameters().get("param" + idx);
        double d = v instanceof Number n ? n.doubleValue() : 0.0;
        return d != 0.0 ? d : dflt;
    }

    /**
     * Replaces every ideal transformer (classic LK_TRANS, type 23) with its
     * hidden subcircuit: two two-terminal voltage-source windings placed so
     * their pins coincide with the transformer's four schematic pins for any
     * orientation. The primary is an LK_U with the controlled-source type
     * (its voltage follows the secondary through a VCVS coupling), the
     * secondary an LK_TRANS-expanded winding whose current mirrors the
     * primary's.
     */
    private static TransformerExpansion expandIdealTransformers(
            List<CircuitModel.ComponentData> components) {
        List<CircuitModel.ComponentData> result = new ArrayList<>(components.size());
        List<WindingPair> pairs = new ArrayList<>();
        for (CircuitModel.ComponentData comp : components) {
            if (comp.getType() != 23) {
                result.add(comp);
                continue;
            }

            double n1 = positiveParameter(comp, 0, 10.0);
            double n2 = positiveParameter(comp, 1, 2.0);
            double polarity = comp.getParameters().get("param2") instanceof Number p ? p.doubleValue() : -1.0;
            if (polarity == 0.0) {
                polarity = -1.0;
            }
            double gain = polarity * n1 / n2;

            int x = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[0] : 0;
            int y = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[1] : 0;
            int orient = comp.getOrientation() != 0 ? comp.getOrientation() : ComponentTerminals.NORTH_SOUTH;

            // Winding placements per orientation: the primary pins are the two
            // XIN pins (one 2-port pitch left of center), the secondary pins
            // the two YOUT pins (one pitch right of center).
            int primX, primY, primOrient, secX, secY, secOrient;
            switch (orient) {
                case ComponentTerminals.SOUTH_NORTH:
                    primX = x + 1; primY = y; primOrient = ComponentTerminals.SOUTH_NORTH;
                    secX = x - 1; secY = y; secOrient = ComponentTerminals.SOUTH_NORTH;
                    break;
                case ComponentTerminals.EAST_WEST:
                    primX = x; primY = y - 1; primOrient = ComponentTerminals.EAST_WEST;
                    secX = x; secY = y + 1; secOrient = ComponentTerminals.EAST_WEST;
                    break;
                case ComponentTerminals.WEST_EAST:
                    primX = x; primY = y + 1; primOrient = ComponentTerminals.WEST_EAST;
                    secX = x; secY = y - 1; secOrient = ComponentTerminals.WEST_EAST;
                    break;
                default: // NORTH_SOUTH
                    primX = x - 1; primY = y; primOrient = ComponentTerminals.NORTH_SOUTH;
                    secX = x + 1; secY = y; secOrient = ComponentTerminals.NORTH_SOUTH;
                    break;
            }

            String[] xLabels = comp.getTerminalXLabels();
            String[] yLabels = comp.getTerminalYLabels();
            String xl0 = xLabels.length > 0 ? xLabels[0] : "";
            String xl1 = xLabels.length > 1 ? xLabels[1] : "";
            String yl0 = yLabels.length > 0 ? yLabels[0] : "";
            String yl1 = yLabels.length > 1 ? yLabels[1] : "";

            // Primary: LK_U with QUELLE_VOLTAGECONTROLLED_DIRECTLY — its
            // b-side stays zero; the winding voltage comes from the VCVS
            // coupling sensing the secondary.
            CircuitModel.ComponentData prim = new CircuitModel.ComponentData(
                    CircuitTypCore.LK_U.getTypeNumber(), comp.getName() + " prim", primX, primY, primOrient);
            prim.setRawParameters(new double[21]);
            prim.setParameter("param0", 399.0); // QUELLE_VOLTAGECONTROLLED_DIRECTLY
            prim.setParameter("param14", 0.0);
            prim.setTerminalXLabels(new String[]{xl0});
            prim.setTerminalYLabels(new String[]{xl1});

            // Secondary: LK_TRANS — carries the z-current (KCL column) with
            // no z-row voltage equation; mirrored onto the primary current.
            CircuitModel.ComponentData sec = new CircuitModel.ComponentData(
                    CircuitTypCore.LK_TRANS.getTypeNumber(), comp.getName() + " sec", secX, secY, secOrient);
            sec.setRawParameters(new double[21]);
            sec.setTerminalXLabels(new String[]{yl0});
            sec.setTerminalYLabels(new String[]{yl1});

            long uid = comp.getUniqueObjectIdentifier();
            prim.setUniqueObjectIdentifier(uid + 1);
            sec.setUniqueObjectIdentifier(uid + 2);

            result.add(prim);
            result.add(sec);
            pairs.add(new WindingPair(prim, sec, gain));
        }
        return new TransformerExpansion(result, pairs);
    }

    /**
     * Assembles and returns the finished netlist: init, island reference
     * pinning, build warnings, couplings, and element uids (branch components
     * followed by any synthetic expansion elements).
     */
    private static CircuitNetlist finishNetlist(
            CircuitTypCore[] types, int[] nodeX, int[] nodeY, int[] voltageSourceNumbers,
            double[][] params, int maxNodeIndex, int voltageSourceCount, int elementCount,
            List<String> buildWarnings,
            List<CircuitModel.ComponentData> branchComponents, int regularElementCount,
            long[] extraUids,
            List<int[]> vccsPairs, List<Double> vccsGains,
            List<WindingPair> windingPairs,
            List<CircuitModel.ComponentData> mutualCouplers) {
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                maxNodeIndex, voltageSourceCount, elementCount);
        netlist.setSingularityEntries(calculateSingularityEntries(maxNodeIndex, elementCount, nodeX, nodeY, types, voltageSourceNumbers));
        buildWarnings.forEach(netlist::addBuildWarning);
        for (int i = 0; i < vccsPairs.size(); i++) {
            netlist.registerVccs(vccsPairs.get(i)[0], vccsPairs.get(i)[1], vccsGains.get(i));
        }
        for (WindingPair wp : windingPairs) {
            int pi = branchComponents.indexOf(wp.prim());
            int si = branchComponents.indexOf(wp.sec());
            if (pi >= 0 && si >= 0) {
                netlist.registerVcvs(pi, si, wp.gain());
                netlist.registerZCurrentMirror(si, pi, wp.gain());
            }
        }
        long[] uids = new long[elementCount];
        for (int i = 0; i < regularElementCount && i < elementCount; i++) {
            uids[i] = branchComponents.get(i).getUniqueObjectIdentifier();
        }
        for (int i = 0; i < extraUids.length && regularElementCount + i < elementCount; i++) {
            uids[regularElementCount + i] = extraUids[i];
        }
        netlist.setElementUids(uids);
        registerMutualCouplers(netlist, mutualCouplers, buildWarnings);
        return netlist;
    }

    /**
     * Registers the classic mutual-inductance couplers (type 9): each declares
     * k between two coupled inductors, identified by their unique object ids
     * (raw parameters 1 and 2). M = k * sqrt(L1 * L2) lands in the solver's
     * z-row cross terms.
     */
    private static void registerMutualCouplers(CircuitNetlist netlist,
                                               List<CircuitModel.ComponentData> mutualCouplers,
                                               List<String> buildWarnings) {
        for (CircuitModel.ComponentData coupler : mutualCouplers) {
            double[] raw = coupler.getRawParameters();
            if (raw == null || raw.length < 3) {
                continue;
            }
            double k = raw[0];
            int idx1 = netlist.indexOfUid((long) raw[1]);
            int idx2 = netlist.indexOfUid((long) raw[2]);
            if (k <= 0 || k > 1 || idx1 < 0 || idx2 < 0
                    || netlist.getType(idx1) != CircuitTypCore.LK_LKOP2
                    || netlist.getType(idx2) != CircuitTypCore.LK_LKOP2) {
                buildWarnings.add("Component '" + coupler.getName()
                        + "' (mutual inductance) could not be resolved to two coupled inductors and is ignored");
                continue;
            }
            double l1 = netlist.getParameter(idx1)[0];
            double l2 = netlist.getParameter(idx2)[0];
            if (l1 <= 0 || l2 <= 0) {
                buildWarnings.add("Component '" + coupler.getName()
                        + "' (mutual inductance) references inductors with invalid inductance and is ignored");
                continue;
            }
            netlist.registerMutualCoupling(idx1, idx2, k, l1, l2);
        }
    }

    /**
     * Extracts circuit topology by tracing schematic wires to component terminals.
     */
    private static CircuitNetlist buildFromWiresAndComponents(
            List<CircuitModel.ComponentData> components,
            List<CircuitModel.ConnectionData> connections,
            List<WindingPair> windingPairs,
            List<CircuitModel.ComponentData> mutualCouplers) {

        WireNets wireNets = buildWireNets(connections);
        DisjointSet<String> pointDs = new DisjointSet<>();
        Map<String, String> labelToKey = new HashMap<>();
        Set<String> groundPoints = new HashSet<>();

        // 1. Wire labels name the whole wire net
        for (CircuitModel.ConnectionData conn : connections) {
            int[][] pts = conn.getPoints();
            if (pts == null || pts.length == 0) continue;

            if (isValidLabel(conn.getLabel())) {
                String lbl = conn.getLabel().trim();
                String key = wireNets.netKey(new GridPoint(pts[0][0], pts[0][1]));
                pointDs.find(key);
                if (isGroundLabel(lbl)) {
                    groundPoints.add(key);
                } else if (labelToKey.containsKey(lbl)) {
                    pointDs.union(key, labelToKey.get(lbl));
                } else {
                    labelToKey.put(lbl, key);
                }
            }
        }

        // 2. Map component terminals to points
        // Count how many terminals reside at each GridPoint to detect coincident (pin-to-pin) terminals
        Map<GridPoint, Integer> terminalUsageCount = new HashMap<>();
        for (CircuitModel.ComponentData comp : components) {
            GridPoint[] terms = computeComponentTerminals(comp);
            for (int t = 0; t < 2; t++) {
                terminalUsageCount.put(terms[t], terminalUsageCount.getOrDefault(terms[t], 0) + 1);
            }
        }

        List<String> buildWarnings = new ArrayList<>();
        List<CircuitModel.ComponentData> branchComponents = new ArrayList<>();
        for (CircuitModel.ComponentData comp : components) {
            int typ = comp.getType();
            GridPoint[] terms = computeComponentTerminals(comp);

            // Register labels / grounds
            for (int t = 0; t < 2; t++) {
                String key = wireNets.netKey(terms[t]);
                pointDs.find(key);

                boolean touchesWire = wireNets.touchesWire(terms[t]);
                boolean isCoincident = terminalUsageCount.getOrDefault(terms[t], 0) > 1;
                boolean hasOpticalConnection = touchesWire || isCoincident;

                // If terminal has no optical connection (floating in empty space), it must NOT be connected
                // to any net by label! It remains an isolated open-circuit node.
                if (hasOpticalConnection) {
                    for (String l : t == 0 ? comp.getTerminalXLabels() : comp.getTerminalYLabels()) {
                        if (isValidLabel(l)) {
                            String lbl = l.trim();
                            if (isGroundLabel(lbl)) {
                                groundPoints.add(key);
                            } else if (labelToKey.containsKey(lbl)) {
                                pointDs.union(key, labelToKey.get(lbl));
                            } else {
                                labelToKey.put(lbl, key);
                            }
                        }
                    }
                }
            }

            if (typ == 31) {
                groundPoints.add(wireNets.netKey(terms[0]));
                groundPoints.add(wireNets.netKey(terms[1]));
            }

            // Only electrical/thermal branches are added to MNA netlist elements;
            // BJTs (33) are collected for the hidden-subcircuit expansion below
            if (!isNonBranchComponent(typ) && typ != 33) {
                branchComponents.add(comp);
            } else if (typ == 9) {
                // only reachable if transformer expansion did not run — flag it
                buildWarnings.add("Component '" + comp.getName()
                        + "' (transformer) has no simulation model and is ignored");
            }
        }

        List<CircuitModel.ComponentData> bjts = new ArrayList<>();
        for (CircuitModel.ComponentData comp : components) {
            if (comp.getType() == 33) {
                bjts.add(comp);
            }
        }

        int elementCount = branchComponents.size();
        GridPoint[][] compTerminals = new GridPoint[elementCount][2];
        for (int i = 0; i < elementCount; i++) {
            compTerminals[i] = computeComponentTerminals(branchComponents.get(i));
        }

        // 3. Assign node indices
        // Ground is node 0
        Map<String, Integer> rootToNode = new HashMap<>();
        for (String gk : groundPoints) {
            rootToNode.put(pointDs.find(gk), 0);
        }

        int nextNode = 1;
        for (int i = 0; i < elementCount; i++) {
            for (int t = 0; t < 2; t++) {
                String root = pointDs.find(wireNets.netKey(compTerminals[i][t]));
                if (!rootToNode.containsKey(root)) {
                    rootToNode.put(root, nextNode++);
                }
            }
        }

        // If no explicit ground was specified, map the root of the first negative terminal to node 0
        if (groundPoints.isEmpty() && elementCount > 0) {
            String defaultGroundRoot = pointDs.find(wireNets.netKey(compTerminals[0][1]));
            int oldNode = rootToNode.getOrDefault(defaultGroundRoot, 1);
            if (oldNode != 0) {
                rootToNode.put(defaultGroundRoot, 0);
            }
        }

        int maxNodeIndex = 0;
        for (int idx : rootToNode.values()) {
            if (idx > maxNodeIndex) maxNodeIndex = idx;
        }

        // 4. Build component type, source number, node arrays, and parameter arrays
        int voltageSourceCount = 0;
        int[] voltageSourceNumbers = new int[elementCount];
        CircuitTypCore[] types = new CircuitTypCore[elementCount];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = branchComponents.get(i);
            CircuitTypCore typ;
            try {
                typ = CircuitTypCore.fromTypeNumber(comp.getType());
            } catch (IllegalArgumentException e) {
                typ = CircuitTypCore.LK_R;
                buildWarnings.add("Component '" + comp.getName() + "' has unknown type "
                        + comp.getType() + " and is simulated as a resistor");
            }
            types[i] = typ;

            if (typ == CircuitTypCore.LK_U || typ == CircuitTypCore.LK_LKOP2 || typ == CircuitTypCore.LK_TRANS) {
                voltageSourceNumbers[i] = ++voltageSourceCount;
            } else {
                voltageSourceNumbers[i] = -1;
            }
        }

        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        double[][] params = new double[elementCount][40];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = branchComponents.get(i);
            nodeX[i] = rootToNode.getOrDefault(pointDs.find(wireNets.netKey(compTerminals[i][0])), 0);
            nodeY[i] = rootToNode.getOrDefault(pointDs.find(wireNets.netKey(compTerminals[i][1])), 0);

            if (comp.getRawParameters() != null) {
                int copyLen = Math.min(comp.getRawParameters().length, 40);
                System.arraycopy(comp.getRawParameters(), 0, params[i], 0, copyLen);
            }
            for (int p = 0; p < 40; p++) {
                Object val = comp.getParameters().get("param" + p);
                if (val instanceof Number) {
                    params[i][p] = ((Number) val).doubleValue();
                }
            }
            if (params[i][0] == 0.0) {
                Object primary = comp.getParameters().get(CircuitModel.ComponentData.resolveParameterKey(comp.getType()));
                if (primary instanceof Number) {
                    params[i][0] = ((Number) primary).doubleValue();
                }
            }
        }

        // ===== BJT hidden-subcircuit expansion =====
        // Port of the classic BJT model: five two-terminal elements around an
        // internal mid node — base resistor, collector/emitter junction
        // diodes, and beta-scaled current sources driven by the base-resistor
        // voltage. NPN pulls current from C and E into mid; PNP mirrors all
        // three element directions.
        List<int[]> vccsPairs = new ArrayList<>();
        List<Double> vccsGains = new ArrayList<>();
        List<Long> bjtUids = new ArrayList<>();
        if (!bjts.isEmpty()) {
            int extra = bjts.size() * 5;
            types = Arrays.copyOf(types, elementCount + extra);
            nodeX = Arrays.copyOf(nodeX, elementCount + extra);
            nodeY = Arrays.copyOf(nodeY, elementCount + extra);
            voltageSourceNumbers = Arrays.copyOf(voltageSourceNumbers, elementCount + extra);
            double[][] grownParams = new double[elementCount + extra][];
            System.arraycopy(params, 0, grownParams, 0, elementCount);
            params = grownParams;

            double rOn = 10e-3;
            double uF = 0.6;
            double rOff = 1e7;

            for (CircuitModel.ComponentData bjt : bjts) {
                double betaF = positiveParameter(bjt, 1, 100.0);
                double betaB = positiveParameter(bjt, 2, 60.0);
                double rBase = positiveParameter(bjt, 3, 0.1);
                boolean npn = positiveParameter(bjt, 4, 1.0) >= 0;

                GridPoint[] pins = computeComponentTerminalsAny(bjt); // C, B, E
                int[] pinNodes = new int[3];
                for (int t = 0; t < 3; t++) {
                    String root = pointDs.find(wireNets.netKey(pins[t]));
                    Integer existing = rootToNode.get(root);
                    if (existing != null) {
                        pinNodes[t] = existing;
                    } else {
                        // unwired pin: isolated node, pinned by the island logic
                        pinNodes[t] = nextNode;
                        rootToNode.put(root, nextNode);
                        nextNode++;
                    }
                }
                int cNode = pinNodes[0];
                int bNode = pinNodes[1];
                int eNode = pinNodes[2];
                int mid = nextNode++;
                maxNodeIndex = mid;

                int rbIdx = elementCount++;
                types[rbIdx] = CircuitTypCore.LK_R;
                nodeX[rbIdx] = bNode;
                nodeY[rbIdx] = mid;
                voltageSourceNumbers[rbIdx] = -1;
                params[rbIdx] = new double[40];
                params[rbIdx][0] = rBase;

                int dcIdx = elementCount++;
                types[dcIdx] = CircuitTypCore.LK_D;
                nodeX[dcIdx] = npn ? mid : cNode;
                nodeY[dcIdx] = npn ? cNode : mid;
                voltageSourceNumbers[dcIdx] = -1;
                params[dcIdx] = new double[]{rOn, uF, rOn, rOff};

                int deIdx = elementCount++;
                types[deIdx] = CircuitTypCore.LK_D;
                nodeX[deIdx] = npn ? mid : eNode;
                nodeY[deIdx] = npn ? eNode : mid;
                voltageSourceNumbers[deIdx] = -1;
                params[deIdx] = new double[]{rOn, uF, rOn, rOff};

                int fcIdx = elementCount++;
                types[fcIdx] = CircuitTypCore.LK_I;
                nodeX[fcIdx] = npn ? cNode : mid;
                nodeY[fcIdx] = npn ? mid : cNode;
                voltageSourceNumbers[fcIdx] = -1;
                params[fcIdx] = new double[40];
                params[fcIdx][0] = 399; // QUELLE_VOLTAGECONTROLLED_DIRECTLY: b-side stays 0

                int feIdx = elementCount++;
                types[feIdx] = CircuitTypCore.LK_I;
                nodeX[feIdx] = npn ? eNode : mid;
                nodeY[feIdx] = npn ? mid : eNode;
                voltageSourceNumbers[feIdx] = -1;
                params[feIdx] = new double[40];
                params[feIdx][0] = 399;

                vccsPairs.add(new int[]{fcIdx, rbIdx});
                vccsPairs.add(new int[]{feIdx, rbIdx});
                vccsGains.add(betaF / rBase);
                vccsGains.add(betaB / rBase);
                long uid = bjt.getUniqueObjectIdentifier();
                for (int k = 1; k <= 5; k++) {
                    bjtUids.add(uid + k);
                }
            }
        }

        CircuitNetlist netlist = finishNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                maxNodeIndex, voltageSourceCount, elementCount, buildWarnings,
                branchComponents, branchComponents.size(),
                bjtUids.stream().mapToLong(Long::longValue).toArray(),
                vccsPairs, vccsGains, windingPairs, mutualCouplers);

        // expose net labels so simulations can resolve signals like "V_out"
        for (Map.Entry<String, String> entry : labelToKey.entrySet()) {
            Integer node = rootToNode.get(pointDs.find(entry.getValue()));
            if (node != null) {
                netlist.getLabelResolver().addLabel(entry.getKey(), node);
            }
        }
        for (String gk : groundPoints) {
            netlist.getLabelResolver().addLabel("GND", 0);
            break;
        }
        return netlist;
    }

    /**
     * Input and output terminal of a two-port component at {@code ±TERMINAL_DISTANCE}
     * grid units along the flow direction; orientation 0 falls back to NORTH_SOUTH.
     */
    private static GridPoint[] computeComponentTerminals(CircuitModel.ComponentData comp) {
        int x = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[0] : 0;
        int y = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[1] : 0;
        int orient = comp.getOrientation() != 0 ? comp.getOrientation() : ComponentTerminals.NORTH_SOUTH;

        int[] dir = ComponentTerminals.flowVector(orient);
        GridPoint input = new GridPoint(
                x - dir[0] * ComponentTerminals.TERMINAL_DISTANCE,
                y - dir[1] * ComponentTerminals.TERMINAL_DISTANCE);
        GridPoint output = new GridPoint(
                x + dir[0] * ComponentTerminals.TERMINAL_DISTANCE,
                y + dir[1] * ComponentTerminals.TERMINAL_DISTANCE);
        return new GridPoint[]{input, output};
    }

    /**
     * Classic wire connectivity model. Each wire forms one conductor; two
     * wires merge only when one wire's path contains the other's ENDPOINT
     * (T-junction or corner junction). Wires that merely cross at a point
     * that is mid-path for both stay separate — the classic {@code Connection}
     * exposes only its two endpoint terminals to
     * {@code PotentialArea.geometricOnSamePotential}, so a shared mid-path
     * point alone connects nothing. Terminals attach to a wire when they lie
     * anywhere on its path.
     */
    private static final class WireNets {
        private final Map<GridPoint, Integer> pointToWireId = new HashMap<>();
        private final DisjointSet<Integer> wireIds = new DisjointSet<>();

        /** Stable identity of the net a schematic point belongs to. */
        String netKey(GridPoint p) {
            Integer wireId = pointToWireId.get(p);
            return wireId != null ? "W" + wireIds.find(wireId) : "P" + p.x + "," + p.y;
        }

        boolean touchesWire(GridPoint p) {
            return pointToWireId.containsKey(p);
        }
    }

    private static boolean hasSchematicWires(List<CircuitModel.ConnectionData> connections) {
        if (connections == null) return false;
        for (CircuitModel.ConnectionData conn : connections) {
            if (conn.getPoints() != null && conn.getPoints().length > 0) {
                String type = conn.getType();
                if (type == null || "LK".equalsIgnoreCase(type) || "THERMAL".equalsIgnoreCase(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNonStandardPinComponents(List<CircuitModel.ComponentData> components) {
        if (components == null) return false;
        for (CircuitModel.ComponentData comp : components) {
            // LK_OPV1 (22) is an op-amp with asymmetric 4-pin geometry that requires classic label mapping
            if (comp.getType() == 22) {
                return true;
            }
        }
        return false;
    }

    private static WireNets buildWireNets(List<CircuitModel.ConnectionData> connections) {
        WireNets nets = new WireNets();
        if (connections == null) {
            return nets;
        }
        List<List<GridPoint>> wires = new ArrayList<>();
        for (CircuitModel.ConnectionData conn : connections) {
            if (conn.getPoints() == null) {
                continue;
            }
            String type = conn.getType();
            if (type != null && !"LK".equalsIgnoreCase(type) && !"THERMAL".equalsIgnoreCase(type)) {
                continue;
            }
            List<GridPoint> path = new ArrayList<>();
            for (int[] pt : conn.getPoints()) {
                GridPoint gp = new GridPoint(pt[0], pt[1]);
                if (path.isEmpty() || !gp.equals(path.get(path.size() - 1))) {
                    path.add(gp);
                }
            }
            if (!path.isEmpty()) {
                wires.add(path);
            }
        }
        DisjointSet<Integer> ids = nets.wireIds;
        Map<GridPoint, List<Integer>> wiresThroughPoint = new HashMap<>();
        for (int i = 0; i < wires.size(); i++) {
            ids.find(i);
            for (GridPoint gp : wires.get(i)) {
                wiresThroughPoint.computeIfAbsent(gp, k -> new ArrayList<>()).add(i);
                nets.pointToWireId.putIfAbsent(gp, i);
            }
        }
        for (int i = 0; i < wires.size(); i++) {
            List<GridPoint> path = wires.get(i);
            GridPoint[] ends = {path.get(0), path.get(path.size() - 1)};
            for (GridPoint end : ends) {
                for (int j : wiresThroughPoint.getOrDefault(end, List.of())) {
                    ids.union(i, j);
                }
            }
        }
        return nets;
    }

    private static boolean isValidLabel(String label) {
        if (label == null) return false;
        String trimmed = label.trim();
        return !trimmed.isEmpty() && !trimmed.equalsIgnoreCase("NIX_NIX_NIX");
    }

    private static boolean isGroundLabel(String label) {
        if (label == null) return false;
        String trimmed = label.trim().toLowerCase();
        return trimmed.equals("0") || trimmed.equals("/0") || trimmed.equals("gnd") || trimmed.equals("ground");
    }

    private static boolean isNonBranchComponent(int typ) {
        return typ == 9 || typ == 30 || typ == 31 || typ == 41 || typ == 42;
    }

    /**
     * Build netlist from components that have terminal labels (from .ipes file parsing).
     * Labeled terminals connect by equal label; unlabelled terminals connect through
     * wire topology (union-find over wire points) or coincident terminal points.
     */
    private static CircuitNetlist buildFromComponentsWithLabels(List<CircuitModel.ComponentData> components,
                                                                List<CircuitModel.ConnectionData> connections,
                                                                List<WindingPair> windingPairs,
                                                                List<CircuitModel.ComponentData> mutualCouplers) {
        boolean hasWires = hasSchematicWires(connections);
        boolean hasComplex = hasNonStandardPinComponents(components)
                || components.stream().anyMatch(c -> c.getType() >= 40 && c.getType() <= 49);

        List<String> buildWarnings = new ArrayList<>();
        List<CircuitModel.ComponentData> branchComponents = new ArrayList<>();
        for (CircuitModel.ComponentData comp : components) {
            if (!isNonBranchComponent(comp.getType())) {
                branchComponents.add(comp);
            } else if (comp.getType() == 9) {
                // the only electrical branch element silently dropped — flag it
                buildWarnings.add("Component '" + comp.getName()
                        + "' (transformer) has no simulation model and is ignored");
            }
        }

        int elementCount = branchComponents.size();
        GridPoint[][] compTerminals = new GridPoint[elementCount][2];
        for (int i = 0; i < elementCount; i++) {
            compTerminals[i] = computeComponentTerminals(branchComponents.get(i));
        }

        // Classic wire topology so unlabelled terminals connect through wires,
        // not only through coincident terminal coordinates
        WireNets wireNets = buildWireNets(connections);

        Map<GridPoint, Integer> terminalUsageCount = new HashMap<>();
        for (CircuitModel.ComponentData comp : components) {
            GridPoint[] terms = computeComponentTerminals(comp);
            for (int t = 0; t < 2; t++) {
                terminalUsageCount.put(terms[t], terminalUsageCount.getOrDefault(terms[t], 0) + 1);
            }
        }

        boolean[] isConnected = new boolean[elementCount];
        for (int i = 0; i < elementCount; i++) {
            if (!hasWires || hasComplex) {
                isConnected[i] = true;
            } else {
                boolean touchesWire = wireNets.touchesWire(compTerminals[i][0]) || wireNets.touchesWire(compTerminals[i][1]);
                boolean isCoincident = terminalUsageCount.getOrDefault(compTerminals[i][0], 0) > 1
                        || terminalUsageCount.getOrDefault(compTerminals[i][1], 0) > 1;
                isConnected[i] = touchesWire || isCoincident;
            }
        }

        Map<String, Integer> labelToNode = new LinkedHashMap<>();
        labelToNode.put("0", 0);
        labelToNode.put("/0", 0);
        labelToNode.put("GND", 0);
        labelToNode.put("gnd", 0);
        int nextNode = 1;

        if (connections != null) {
            for (CircuitModel.ConnectionData conn : connections) {
                if (conn == null) continue;
                String lbl = conn.getLabel();
                if (isValidLabel(lbl) && !isGroundLabel(lbl) && !labelToNode.containsKey(lbl.trim())) {
                    labelToNode.put(lbl.trim(), nextNode++);
                }
            }
        }

        for (int i = 0; i < elementCount; i++) {
            if (hasWires && !isConnected[i]) {
                continue;
            }
            CircuitModel.ComponentData comp = branchComponents.get(i);
            for (String label : comp.getTerminalXLabels()) {
                if (isValidLabel(label) && !isGroundLabel(label) && !labelToNode.containsKey(label)) {
                    labelToNode.put(label, nextNode++);
                }
            }
            for (String label : comp.getTerminalYLabels()) {
                if (isValidLabel(label) && !isGroundLabel(label) && !labelToNode.containsKey(label)) {
                    labelToNode.put(label, nextNode++);
                }
            }
        }

        // Map net identities (wire nets and unattached terminal points) to node
        // indices. Two labels sharing one wire net alias to the same node (first wins).
        Map<String, Integer> rootToNode = new HashMap<>();

        if (connections != null) {
            for (CircuitModel.ConnectionData conn : connections) {
                if (conn == null) continue;
                String lbl = conn.getLabel();
                int[][] pts = conn.getPoints();
                if (isValidLabel(lbl) && pts != null && pts.length > 0) {
                    int node = isGroundLabel(lbl) ? 0 : labelToNode.getOrDefault(lbl.trim(), 0);
                    String root = wireNets.netKey(new GridPoint(pts[0][0], pts[0][1]));
                    Integer existing = rootToNode.putIfAbsent(root, node);
                    if (existing != null && !lbl.trim().isEmpty()) {
                        labelToNode.put(lbl.trim(), existing);
                    }
                }
            }
        }

        for (int i = 0; i < elementCount; i++) {
            if (hasWires && !isConnected[i]) {
                continue;
            }
            CircuitModel.ComponentData comp = branchComponents.get(i);
            String[] xLabels = comp.getTerminalXLabels();
            if (xLabels.length > 0 && isValidLabel(xLabels[0])) {
                int node = isGroundLabel(xLabels[0]) ? 0 : labelToNode.getOrDefault(xLabels[0], 0);
                Integer existing = rootToNode.putIfAbsent(wireNets.netKey(compTerminals[i][0]), node);
                if (existing != null && !xLabels[0].trim().isEmpty()) {
                    labelToNode.put(xLabels[0].trim(), existing);
                }
            }
            String[] yLabels = comp.getTerminalYLabels();
            if (yLabels.length > 0 && isValidLabel(yLabels[0])) {
                int node = isGroundLabel(yLabels[0]) ? 0 : labelToNode.getOrDefault(yLabels[0], 0);
                Integer existing = rootToNode.putIfAbsent(wireNets.netKey(compTerminals[i][1]), node);
                if (existing != null && !yLabels[0].trim().isEmpty()) {
                    labelToNode.put(yLabels[0].trim(), existing);
                }
            }
        }

        if (connections != null) {
            for (CircuitModel.ConnectionData conn : connections) {
                if (conn == null) continue;
                String lbl = conn.getLabel();
                int[][] pts = conn.getPoints();
                if (isValidLabel(lbl) && pts != null && pts.length > 0) {
                    String root = wireNets.netKey(new GridPoint(pts[0][0], pts[0][1]));
                    Integer resolved = rootToNode.get(root);
                    if (resolved != null && !isGroundLabel(lbl)) {
                        labelToNode.put(lbl.trim(), resolved);
                    }
                }
            }
        }

        for (int i = 0; i < elementCount; i++) {
            if (hasWires && !isConnected[i]) {
                continue;
            }
            CircuitModel.ComponentData comp = branchComponents.get(i);
            String[] xLabels = comp.getTerminalXLabels();
            if (xLabels.length == 0 || !isValidLabel(xLabels[0])) {
                String root = wireNets.netKey(compTerminals[i][0]);
                if (!rootToNode.containsKey(root)) {
                    rootToNode.put(root, nextNode++);
                }
            }
            String[] yLabels = comp.getTerminalYLabels();
            if (yLabels.length == 0 || !isValidLabel(yLabels[0])) {
                String root = wireNets.netKey(compTerminals[i][1]);
                if (!rootToNode.containsKey(root)) {
                    rootToNode.put(root, nextNode++);
                }
            }
        }

        int voltageSourceCount = 0;
        int[] voltageSourceNumbers = new int[elementCount];
        CircuitTypCore[] types = new CircuitTypCore[elementCount];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = branchComponents.get(i);
            CircuitTypCore typ;
            try {
                typ = CircuitTypCore.fromTypeNumber(comp.getType());
            } catch (IllegalArgumentException e) {
                typ = CircuitTypCore.LK_R;
                buildWarnings.add("Component '" + comp.getName() + "' has unknown type "
                        + comp.getType() + " and is simulated as a resistor");
            }
            types[i] = typ;

            if (typ == CircuitTypCore.LK_U || typ == CircuitTypCore.LK_LKOP2 || typ == CircuitTypCore.LK_TRANS) {
                voltageSourceNumbers[i] = ++voltageSourceCount;
            } else {
                voltageSourceNumbers[i] = -1;
            }
        }

        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        double[][] params = new double[elementCount][40];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = branchComponents.get(i);

            if (hasWires && !isConnected[i]) {
                nodeX[i] = nextNode++;
                nodeY[i] = nextNode++;
            } else {
                String[] xLabels = comp.getTerminalXLabels();
                if (xLabels.length > 0 && isValidLabel(xLabels[0])) {
                    nodeX[i] = isGroundLabel(xLabels[0]) ? 0 : labelToNode.getOrDefault(xLabels[0], 0);
                } else {
                    nodeX[i] = rootToNode.getOrDefault(wireNets.netKey(compTerminals[i][0]), 0);
                }

                String[] yLabels = comp.getTerminalYLabels();
                if (yLabels.length > 0 && isValidLabel(yLabels[0])) {
                    nodeY[i] = isGroundLabel(yLabels[0]) ? 0 : labelToNode.getOrDefault(yLabels[0], 0);
                } else {
                    nodeY[i] = rootToNode.getOrDefault(wireNets.netKey(compTerminals[i][1]), 0);
                }
            }

            if (comp.getRawParameters() != null) {
                int copyLen = Math.min(comp.getRawParameters().length, 40);
                System.arraycopy(comp.getRawParameters(), 0, params[i], 0, copyLen);
            }
            for (int p = 0; p < 40; p++) {
                Object val = comp.getParameters().get("param" + p);
                if (val instanceof Number) {
                    params[i][p] = ((Number) val).doubleValue();
                }
            }
            if (params[i][0] == 0.0) {
                Object primary = comp.getParameters().get(CircuitModel.ComponentData.resolveParameterKey(comp.getType()));
                if (primary instanceof Number) {
                    params[i][0] = ((Number) primary).doubleValue();
                }
            }
        }

        int nodeCount = Math.max(nextNode, 1);

        int maxNodeIndex = nodeCount > 0 ? nodeCount - 1 : 0;
        CircuitNetlist netlist = finishNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                maxNodeIndex, voltageSourceCount, elementCount, buildWarnings,
                branchComponents, branchComponents.size(), new long[0],
                new ArrayList<>(), new ArrayList<>(), windingPairs, mutualCouplers);

        for (Map.Entry<String, Integer> entry : labelToNode.entrySet()) {
            if (!entry.getKey().isBlank()) {
                netlist.getLabelResolver().addLabel(entry.getKey(), entry.getValue());
            }
        }
        return netlist;
    }

    /**
     * Calculates reference node indices (singularity entries) for all connected subcircuits.
     */
    public static int[] calculateSingularityEntries(int maxNodeIndex, int elementCount, int[] nodeX, int[] nodeY) {
        return calculateSingularityEntries(maxNodeIndex, elementCount, nodeX, nodeY, null, null);
    }

    /**
     * Calculates reference node indices (singularity entries) for all connected subcircuits.
     *
     * Each galvanic island gets exactly one pinned node acting as its 0 V
     * reference. The choice is semantic, not arbitrary, because measured
     * label signals are read relative to it:
     * <ol>
     *   <li>an island containing node 0 pins node 0,</li>
     *   <li>otherwise an island containing the return (nodeY) terminal of a
     *       voltage source pins that terminal, so source-driven potentials
     *       read positive as users expect,</li>
     *   <li>otherwise the lowest-indexed member is pinned (legacy behavior).</li>
     * </ol>
     */
    public static int[] calculateSingularityEntries(int maxNodeIndex, int elementCount, int[] nodeX, int[] nodeY,
                                                    CircuitTypCore[] types, int[] voltageSourceNumbers) {
        if (maxNodeIndex < 0) {
            return new int[]{0};
        }
        DisjointSet<Integer> ds = new DisjointSet<>();
        for (int i = 0; i <= maxNodeIndex; i++) {
            ds.find(i);
        }
        for (int i = 0; i < elementCount; i++) {
            if (nodeX[i] >= 0 && nodeX[i] <= maxNodeIndex && nodeY[i] >= 0 && nodeY[i] <= maxNodeIndex) {
                ds.union(nodeX[i], nodeY[i]);
            }
        }
        int root0 = ds.find(0);
        Map<Integer, Integer> groupToRepresentative = new LinkedHashMap<>();
        groupToRepresentative.put(root0, 0);
        for (int i = 1; i <= maxNodeIndex; i++) {
            int r = ds.find(i);
            if (!groupToRepresentative.containsKey(r)) {
                groupToRepresentative.put(r, i);
            }
        }
        // Prefer a voltage source's return terminal as the island reference so
        // source-driven nets measure positive instead of the reference falling
        // on an arbitrary measured node
        java.util.Set<Integer> overriddenRoots = new java.util.HashSet<>();
        if (types != null && voltageSourceNumbers != null) {
            for (int i = 0; i < elementCount; i++) {
                if (voltageSourceNumbers[i] <= 0 || types[i] == null) {
                    continue;
                }
                int returnNode = nodeY[i];
                if (returnNode <= 0 || returnNode > maxNodeIndex) {
                    continue;
                }
                int r = ds.find(returnNode);
                if (r == root0 || overriddenRoots.contains(r) || !groupToRepresentative.containsKey(r)) {
                    continue;
                }
                groupToRepresentative.put(r, returnNode);
                overriddenRoots.add(r);
            }
        }
        return groupToRepresentative.values().stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Build netlist with estimated dimensions (backward compatible mode).
     */
    private static CircuitNetlist buildWithEstimatedDimensions(List<CircuitModel.ComponentData> components) {
        int totalComponents = components.size();
        int estimatedNodeCount = Math.max(1, totalComponents / 2 + 1);
        int estimatedVoltageSourceCount = Math.max(0, totalComponents / 5);

        return buildEmpty(
            estimatedNodeCount,
            estimatedVoltageSourceCount,
            totalComponents
        );
    }
}
