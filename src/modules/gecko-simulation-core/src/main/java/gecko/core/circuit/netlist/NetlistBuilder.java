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

        List<CircuitModel.ComponentData> branchComponents = new ArrayList<>();
        for (CircuitModel.ComponentData comp : allComponents) {
            if (!isNonBranchComponent(comp.getType())) {
                branchComponents.add(comp);
            }
        }

        if (branchComponents.isEmpty()) {
            return buildEmpty(0, 0, 0);
        }

        // Count how many terminals have real (non-sentinel) net labels from classic file export
        long explicitLabelCount = allComponents.stream()
                .flatMap(c -> Stream.concat(Arrays.stream(c.getTerminalXLabels()), Arrays.stream(c.getTerminalYLabels())))
                .filter(NetlistBuilder::isValidLabel)
                .count();

        // If circuit has explicit terminal labels on components (from classic GeckoCIRCUITS file export),
        // use label matching with series terminal coordinate sharing
        if (explicitLabelCount > 0) {
            return buildFromComponentsWithLabels(allComponents, model.getConnections());
        }

        List<CircuitModel.ConnectionData> connections = model.getConnections();
        if (connections != null && !connections.isEmpty()) {
            return buildFromWiresAndComponents(allComponents, connections);
        }

        return buildWithEstimatedDimensions(allComponents);
    }

    /**
     * Extracts circuit topology by tracing schematic wires to component terminals.
     */
    private static CircuitNetlist buildFromWiresAndComponents(
            List<CircuitModel.ComponentData> components,
            List<CircuitModel.ConnectionData> connections) {

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
        List<CircuitModel.ComponentData> branchComponents = new ArrayList<>();
        for (CircuitModel.ComponentData comp : components) {
            int typ = comp.getType();
            GridPoint[] terms = computeComponentTerminals(comp);

            // Register labels / grounds
            for (int t = 0; t < 2; t++) {
                String key = wireNets.netKey(terms[t]);
                pointDs.find(key);
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

            if (typ == 31) {
                groundPoints.add(wireNets.netKey(terms[0]));
                groundPoints.add(wireNets.netKey(terms[1]));
            }

            // Only electrical/thermal branches are added to MNA netlist elements
            if (!isNonBranchComponent(typ)) {
                branchComponents.add(comp);
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
            }
            types[i] = typ;

            if (typ == CircuitTypCore.LK_U || typ == CircuitTypCore.LK_LKOP2) {
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

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                maxNodeIndex, voltageSourceCount, elementCount);
        netlist.setSingularityEntries(calculateSingularityEntries(maxNodeIndex, elementCount, nodeX, nodeY));
        long[] uids = new long[elementCount];
        for (int i = 0; i < elementCount; i++) {
            uids[i] = branchComponents.get(i).getUniqueObjectIdentifier();
        }
        netlist.setElementUids(uids);

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
    }

    private static WireNets buildWireNets(List<CircuitModel.ConnectionData> connections) {
        WireNets nets = new WireNets();
        if (connections == null) {
            return nets;
        }
        List<List<GridPoint>> wires = new ArrayList<>();
        for (CircuitModel.ConnectionData conn : connections) {
            if (!"LK".equalsIgnoreCase(conn.getType()) || conn.getPoints() == null) {
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
        return trimmed.equals("0") || trimmed.equals("gnd") || trimmed.equals("ground");
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
                                                                List<CircuitModel.ConnectionData> connections) {
        Map<String, Integer> labelToNode = new LinkedHashMap<>();
        labelToNode.put("0", 0);
        labelToNode.put("GND", 0);
        labelToNode.put("gnd", 0);
        int nextNode = 1;

        for (CircuitModel.ComponentData comp : components) {
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

        List<CircuitModel.ComponentData> branchComponents = new ArrayList<>();
        for (CircuitModel.ComponentData comp : components) {
            if (!isNonBranchComponent(comp.getType())) {
                branchComponents.add(comp);
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

        // Map net identities (wire nets and unattached terminal points) to node
        // indices. Two labels sharing one wire net alias to the same node (first wins).
        Map<String, Integer> rootToNode = new HashMap<>();
        for (int i = 0; i < elementCount; i++) {
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

        for (int i = 0; i < elementCount; i++) {
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

        int nodeCount = Math.max(nextNode, 1);

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
            }
            types[i] = typ;

            if (typ == CircuitTypCore.LK_U || typ == CircuitTypCore.LK_LKOP2) {
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

        int maxNodeIndex = nodeCount > 0 ? nodeCount - 1 : 0;
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                maxNodeIndex, voltageSourceCount, elementCount);
        netlist.setSingularityEntries(calculateSingularityEntries(maxNodeIndex, elementCount, nodeX, nodeY));
        long[] uids = new long[elementCount];
        for (int i = 0; i < elementCount; i++) {
            uids[i] = branchComponents.get(i).getUniqueObjectIdentifier();
        }
        netlist.setElementUids(uids);

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
