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
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * .ipes patching through the full-fidelity model path: the file is parsed
 * with the engine's {@link CircuitFileParser}, modified on the
 * {@link CircuitModel}, and rewritten with {@link CircuitFileWriter} — so a
 * patch is correct by construction instead of regex-based, and accepts
 * catalog parameter names ({"resistance": 25}) in addition to raw slots
 * ({"param0": 25}).
 */
final class CircuitPatcher {

    private CircuitPatcher() {
    }

    static Map<String, Object> patchComponent(String circuitPath, String componentName,
                                              Map<String, Object> parameters,
                                              String outputPath) throws IOException,
            CircuitFileParser.CircuitParseException {
        Path path = IpesSupport.resolve(circuitPath);
        if (!IpesSupport.exists(path)) {
            throw new IllegalArgumentException("Circuit file not found: " + path);
        }
        CircuitModel model = new CircuitFileParser().parse(path.toString());
        CircuitModel.ComponentData component = findComponent(model, componentName);
        if (component == null) {
            throw new IllegalArgumentException("Component '" + componentName + "' not found in circuit");
        }

        int applied = 0;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            double value = asDouble(entry.getValue());
            if (key.startsWith("param")) {
                component.setParameter(key, value);
                applied++;
                continue;
            }
            Integer slot = catalogSlot(component, key);
            if (slot != null) {
                component.setParameter("param" + slot, value);
                applied++;
                continue;
            }
            throw new IllegalArgumentException("Unknown parameter '" + key + "' for component '"
                    + componentName + "' (type " + component.getType() + "); use param<slot> "
                    + "or a catalog parameter name");
        }
        if (applied == 0) {
            throw new IllegalArgumentException("no parameter updates provided");
        }

        Path target = outputPath != null ? IpesSupport.resolve(outputPath) : path;
        CircuitFileWriter.write(model, target, true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("component", componentName);
        result.put("applied", applied);
        result.put("updated_file", target.toString());
        return result;
    }

    /** Resolves a catalog parameter name to its raw slot for the component's type. */
    private static Integer catalogSlot(CircuitModel.ComponentData component, String name) {
        for (ComponentCatalog.ComponentDef def : ComponentCatalog.all().values()) {
            if (def.typeNumber() != component.getType()) {
                continue;
            }
            for (ComponentCatalog.ParameterDef parameter : def.parameters()) {
                if (parameter.name().equalsIgnoreCase(name)
                        || parameter.name().toLowerCase(Locale.ROOT)
                                .equals(name.toLowerCase(Locale.ROOT).replace("_", ""))) {
                    return parameter.targetSlot();
                }
            }
        }
        return null;
    }

    private static CircuitModel.ComponentData findComponent(CircuitModel model, String componentName) {
        for (CircuitModel.ComponentData comp : model.getAllComponents()) {
            if (componentName.equals(comp.getName())) {
                return comp;
            }
        }
        return null;
    }

    static Map<String, Object> setScriptCode(String circuitPath, String blockName, String sourceCode,
                                             String staticVariables, String staticCode,
                                             String outputPath) throws IOException,
            CircuitFileParser.CircuitParseException {
        Path path = IpesSupport.resolve(circuitPath);
        if (!IpesSupport.exists(path)) {
            throw new IllegalArgumentException("Circuit file not found: " + path);
        }
        CircuitModel model = new CircuitFileParser().parse(path.toString());
        CircuitModel.ComponentData block = findComponent(model, blockName);
        if (block == null) {
            throw new IllegalArgumentException("Script block '" + blockName + "' not found in circuit");
        }
        if (block.getType() != CircuitTypCore.C_JAVA_FUNCTION.getTypeNumber()
                && block.getType() != CircuitTypCore.CTRL_SCRIPT.getTypeNumber()) {
            throw new IllegalArgumentException("Component '" + blockName + "' is not a script block "
                    + "(typ " + block.getType() + ")");
        }

        block.setParameter("sourceCode", sourceCode.strip());
        if (staticVariables != null && !staticVariables.isEmpty()) {
            block.setParameter("staticVariables", staticVariables.strip());
        }
        if (staticCode != null && !staticCode.isEmpty()) {
            block.setParameter("staticCode", staticCode.strip());
        }
        stripScriptExtraLines(block);

        Path target = outputPath != null ? IpesSupport.resolve(outputPath) : path;
        CircuitFileWriter.write(model, target, true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("block", blockName);
        result.put("updated_file", target.toString());
        return result;
    }

    /**
     * Removes the script sub-block lines (sourceCode/staticCode/importCode/
     * staticVariables) a classic file keeps in the component's extraLines:
     * the parameters map is now authoritative, and the writer would otherwise
     * emit the stale script next to (or instead of) the updated one.
     */
    private static void stripScriptExtraLines(CircuitModel.ComponentData block) {
        List<String> stripped = new ArrayList<>();
        boolean insideScriptTag = false;
        for (String line : block.getExtraLines()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<sourceCode>") || trimmed.startsWith("<staticCode>")
                    || trimmed.startsWith("<importCode>") || trimmed.startsWith("<staticVariables>")) {
                insideScriptTag = true;
                continue;
            }
            if (insideScriptTag && (trimmed.startsWith("<\\sourceCode>") || trimmed.startsWith("<\\staticCode>")
                    || trimmed.startsWith("<\\importCode>") || trimmed.startsWith("<\\staticVariables>"))) {
                insideScriptTag = false;
                continue;
            }
            if (!insideScriptTag) {
                stripped.add(line);
            }
        }
        block.getExtraLines().clear();
        block.getExtraLines().addAll(stripped);
    }

    private static double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
