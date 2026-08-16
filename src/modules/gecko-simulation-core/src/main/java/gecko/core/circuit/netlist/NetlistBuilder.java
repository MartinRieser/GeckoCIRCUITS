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
        allComponents.addAll(model.getControlComponents());
        allComponents.addAll(model.getThermalComponents());

        if (allComponents.isEmpty()) {
            return buildEmpty(0, 0, 0);
        }

        // Count how many terminals have real (non-sentinel) net labels from classic file export
        long explicitLabelCount = allComponents.stream()
                .flatMap(c -> Stream.concat(Arrays.stream(c.getTerminalXLabels()), Arrays.stream(c.getTerminalYLabels())))
                .filter(NetlistBuilder::isValidLabel)
                .count();

        // If circuit has explicit terminal labels on components (e.g. from classic GeckoCIRCUITS file), use label matching
        if (explicitLabelCount >= allComponents.size()) {
            return buildFromComponentsWithLabels(allComponents);
        }

        List<CircuitModel.ConnectionData> connections = model.getConnections();
        if (connections != null && !connections.isEmpty()) {
            return buildFromWiresAndComponents(allComponents, connections);
        }

        if (explicitLabelCount > 0) {
            return buildFromComponentsWithLabels(allComponents);
        }
        return buildWithEstimatedDimensions(allComponents);
    }

    /**
     * Extracts circuit topology by tracing schematic wires to component terminals.
     */
    private static CircuitNetlist buildFromWiresAndComponents(
            List<CircuitModel.ComponentData> components,
            List<CircuitModel.ConnectionData> connections) {

        DisjointSet<GridPoint> pointDs = new DisjointSet<>();
        Map<String, GridPoint> labelToPoint = new HashMap<>();
        Set<GridPoint> groundPoints = new HashSet<>();

        // 1. Union wire points
        for (CircuitModel.ConnectionData conn : connections) {
            int[][] pts = conn.getPoints();
            if (pts == null || pts.length == 0) continue;

            GridPoint first = new GridPoint(pts[0][0], pts[0][1]);
            for (int k = 1; k < pts.length; k++) {
                GridPoint next = new GridPoint(pts[k][0], pts[k][1]);
                pointDs.union(first, next);
            }

            // Wire label
            if (isValidLabel(conn.getLabel())) {
                String lbl = conn.getLabel().trim();
                if (isGroundLabel(lbl)) {
                    groundPoints.add(first);
                } else {
                    if (labelToPoint.containsKey(lbl)) {
                        pointDs.union(first, labelToPoint.get(lbl));
                    } else {
                        labelToPoint.put(lbl, first);
                    }
                }
            }
        }

        // 2. Map component terminals to points
        int elementCount = components.size();
        GridPoint[][] compTerminals = new GridPoint[elementCount][2];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = components.get(i);
            GridPoint[] terms = computeComponentTerminals(comp);
            compTerminals[i] = terms;

            // Check terminal X labels
            for (String l : comp.getTerminalXLabels()) {
                if (isValidLabel(l)) {
                    String lbl = l.trim();
                    if (isGroundLabel(lbl)) {
                        groundPoints.add(terms[0]);
                    } else {
                        if (labelToPoint.containsKey(lbl)) {
                            pointDs.union(terms[0], labelToPoint.get(lbl));
                        } else {
                            labelToPoint.put(lbl, terms[0]);
                        }
                    }
                }
            }

            // Check terminal Y labels
            for (String l : comp.getTerminalYLabels()) {
                if (isValidLabel(l)) {
                    String lbl = l.trim();
                    if (isGroundLabel(lbl)) {
                        groundPoints.add(terms[1]);
                    } else {
                        if (labelToPoint.containsKey(lbl)) {
                            pointDs.union(terms[1], labelToPoint.get(lbl));
                        } else {
                            labelToPoint.put(lbl, terms[1]);
                        }
                    }
                }
            }

            // Type 31 is Ground (LK_GLOBAL_TERMINAL)
            if (comp.getType() == 31) {
                groundPoints.add(terms[0]);
                groundPoints.add(terms[1]);
            }
        }

        // 3. Assign node indices
        // Ground is node 0
        Map<GridPoint, Integer> rootToNode = new HashMap<>();
        for (GridPoint gp : groundPoints) {
            rootToNode.put(pointDs.find(gp), 0);
        }

        int nextNode = 1;
        for (int i = 0; i < elementCount; i++) {
            for (int t = 0; t < 2; t++) {
                GridPoint root = pointDs.find(compTerminals[i][t]);
                if (!rootToNode.containsKey(root)) {
                    rootToNode.put(root, nextNode++);
                }
            }
        }

        // If no explicit ground was specified, map the root of the first negative terminal to node 0
        if (groundPoints.isEmpty() && elementCount > 0) {
            GridPoint defaultGroundRoot = pointDs.find(compTerminals[0][1]);
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
            CircuitModel.ComponentData comp = components.get(i);
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
        double[][] params = new double[elementCount][16];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = components.get(i);
            nodeX[i] = rootToNode.getOrDefault(pointDs.find(compTerminals[i][0]), 0);
            nodeY[i] = rootToNode.getOrDefault(pointDs.find(compTerminals[i][1]), 0);

            for (int p = 0; p < 16; p++) {
                Object val = comp.getParameters().get("param" + p);
                params[i][p] = (val instanceof Number) ? ((Number) val).doubleValue() : 0.0;
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

        // expose net labels so simulations can resolve signals like "V_out"
        for (Map.Entry<String, GridPoint> entry : labelToPoint.entrySet()) {
            Integer node = rootToNode.get(pointDs.find(entry.getValue()));
            if (node != null) {
                netlist.getLabelResolver().addLabel(entry.getKey(), node);
            }
        }
        for (GridPoint gp : groundPoints) {
            netlist.getLabelResolver().addLabel("GND", 0);
            break;
        }
        return netlist;
    }

    private static GridPoint[] computeComponentTerminals(CircuitModel.ComponentData comp) {
        int x = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[0] : 0;
        int y = comp.getPosition() != null && comp.getPosition().length >= 2 ? comp.getPosition()[1] : 0;
        int orient = comp.getOrientation() != 0 ? comp.getOrientation() : 503;

        GridPoint p0, p1;
        switch (orient) {
            case 502 -> { // WEST_EAST (0 deg)
                p0 = new GridPoint(x - 2, y);
                p1 = new GridPoint(x + 2, y);
            }
            case 504 -> { // EAST_WEST (180 deg)
                p0 = new GridPoint(x + 2, y);
                p1 = new GridPoint(x - 2, y);
            }
            case 501 -> { // SOUTH_NORTH (270 deg)
                p0 = new GridPoint(x, y + 2);
                p1 = new GridPoint(x, y - 2);
            }
            default -> { // NORTH_SOUTH (90 deg, 503)
                p0 = new GridPoint(x, y - 2);
                p1 = new GridPoint(x, y + 2);
            }
        }
        return new GridPoint[]{p0, p1};
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

    /**
     * Build netlist from components that have terminal labels (from .ipes file parsing).
     */
    private static CircuitNetlist buildFromComponentsWithLabels(List<CircuitModel.ComponentData> components) {
        Map<String, Integer> labelToNode = new LinkedHashMap<>();
        labelToNode.put("0", 0);
        labelToNode.put("", 0);
        labelToNode.put("GND", 0);
        labelToNode.put("gnd", 0);
        int nextNode = 1;

        for (CircuitModel.ComponentData comp : components) {
            for (String label : comp.getTerminalXLabels()) {
                if (isValidLabel(label) && !labelToNode.containsKey(label)) {
                    labelToNode.put(label, nextNode++);
                }
            }
            for (String label : comp.getTerminalYLabels()) {
                if (isValidLabel(label) && !labelToNode.containsKey(label)) {
                    labelToNode.put(label, nextNode++);
                }
            }
        }

        int nodeCount = Math.max(nextNode, 1);
        int elementCount = components.size();

        int voltageSourceCount = 0;
        int[] voltageSourceNumbers = new int[elementCount];
        CircuitTypCore[] types = new CircuitTypCore[elementCount];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = components.get(i);
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
        double[][] params = new double[elementCount][16];

        for (int i = 0; i < elementCount; i++) {
            CircuitModel.ComponentData comp = components.get(i);

            String[] xLabels = comp.getTerminalXLabels();
            nodeX[i] = (xLabels.length > 0 && isValidLabel(xLabels[0]))
                    ? labelToNode.getOrDefault(xLabels[0], 0) : 0;

            String[] yLabels = comp.getTerminalYLabels();
            nodeY[i] = (yLabels.length > 0 && isValidLabel(yLabels[0]))
                    ? labelToNode.getOrDefault(yLabels[0], 0) : 0;

            for (int p = 0; p < 16; p++) {
                Object val = comp.getParameters().get("param" + p);
                params[i][p] = (val instanceof Number) ? ((Number) val).doubleValue() : 0.0;
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
                nodeCount > 0 ? nodeCount - 1 : 0, voltageSourceCount, elementCount);
        for (Map.Entry<String, Integer> entry : labelToNode.entrySet()) {
            if (!entry.getKey().isBlank()) {
                netlist.getLabelResolver().addLabel(entry.getKey(), entry.getValue());
            }
        }
        return netlist;
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
