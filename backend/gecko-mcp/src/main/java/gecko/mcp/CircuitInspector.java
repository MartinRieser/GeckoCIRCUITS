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
package gecko.mcp;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured introspection of a GeckoCIRCUITS .ipes file through the engine's
 * own {@link CircuitFileParser}: reports simulation settings, every component
 * of all four domains with semantic and raw parameters, control blocks with
 * their script sources and couplings, and the resolved net labels.
 */
final class CircuitInspector {

    private CircuitInspector() {
    }

    static Map<String, Object> inspect(Path path) throws IOException,
            CircuitFileParser.CircuitParseException {
        CircuitModel model = new CircuitFileParser().parse(path.toString());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("file", path.toString());
        result.put("dt", model.getTimeStep());
        result.put("duration", model.getSimulationDuration());
        result.put("solver", model.getSolverType() != null ? model.getSolverType().name() : "SOLVER_BE");

        result.put("lk_component_count", model.getCircuitComponents().size());
        result.put("lk_components", describe(model.getCircuitComponents(), "LK"));
        result.put("control_component_count", model.getControlComponents().size());
        result.put("control_components", describe(model.getControlComponents(), "CONTROL"));
        result.put("thermal_component_count", model.getThermalComponents().size());
        result.put("thermal_components", describe(model.getThermalComponents(), "THERM"));
        result.put("special_component_count", model.getSpecialComponents().size());
        result.put("special_components", describe(model.getSpecialComponents(), "SPECIAL"));
        result.put("connection_count", model.getConnections().size());

        // resolved net labels + build warnings from the real netlist compilation
        try {
            CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
            List<String> labels = netlist.getLabelResolver() != null
                    ? new ArrayList<>(netlist.getLabelResolver().getAllLabels()) : List.of();
            result.put("net_labels", labels);
            result.put("netlist_node_count", netlist.getNodeMax() + 1);
            result.put("netlist_warnings", netlist.getBuildWarnings());
        } catch (RuntimeException e) {
            result.put("net_labels", List.of());
            result.put("netlist_warnings", List.of("netlist build failed: " + e.getMessage()));
        }
        return result;
    }

    private static List<Map<String, Object>> describe(List<CircuitModel.ComponentData> components,
                                                      String domain) {
        List<Map<String, Object>> described = new ArrayList<>();
        for (CircuitModel.ComponentData comp : components) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", comp.getName());
            entry.put("type", comp.getType());
            CircuitTypCore type = CircuitTypCore.findByTypeNumber(comp.getType());
            entry.put("type_name", type != null ? type.name() : "TYPE_" + comp.getType());
            entry.put("id", comp.getUniqueObjectIdentifier());
            if (comp.getPosition().length >= 2) {
                entry.put("x", comp.getPosition()[0]);
                entry.put("y", comp.getPosition()[1]);
            }
            entry.put("orientation", comp.getOrientation());
            String[] xLabels = comp.getTerminalXLabels();
            String[] yLabels = comp.getTerminalYLabels();
            if (xLabels != null && xLabels.length > 0) {
                entry.put("in_nodes", String.join(",", xLabels));
            }
            if (yLabels != null && yLabels.length > 0) {
                entry.put("out_nodes", String.join(",", yLabels));
            }
            if ("CONTROL".equals(domain)) {
                appendControlDetails(comp, entry);
            }
            entry.put("parameters", floatList(comp.getRawParameters()));
            described.add(entry);
        }
        return described;
    }

    /** Coupling target and script source of a control block, if present. */
    private static void appendControlDetails(CircuitModel.ComponentData comp, Map<String, Object> entry) {
        String[] coupled = comp.getParameterStrings();
        if (coupled != null && coupled.length > 0 && coupled[0] != null
                && !coupled[0].isBlank() && !coupled[0].contains("NIX_NIX_NIX")) {
            entry.put("coupled_component", coupled[0].replace("/", "").trim());
        }
        if (comp.getCoupledReferenceID() != 0) {
            entry.put("coupled_id", comp.getCoupledReferenceID());
        }
        Object sourceCode = comp.getParameters().get("sourceCode");
        if (sourceCode != null && !String.valueOf(sourceCode).isBlank()) {
            entry.put("sourceCode", String.valueOf(sourceCode));
        }
        Object staticVariables = comp.getParameters().get("staticVariables");
        if (staticVariables != null && !String.valueOf(staticVariables).isBlank()) {
            entry.put("staticVariables", String.valueOf(staticVariables));
        }
    }

    private static List<Double> floatList(double[] parameters) {
        List<Double> values = new ArrayList<>();
        if (parameters == null) {
            return values;
        }
        for (double value : parameters) {
            values.add(value);
        }
        return values;
    }
}
