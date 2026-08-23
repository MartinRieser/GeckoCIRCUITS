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
package gecko.core.io;

import gecko.core.allg.SolverType;
import gecko.core.circuit.CircuitFileConstants;
import gecko.core.circuit.TokenMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * GUI-free parser for GeckoCIRCUITS .ipes circuit files.
 * Extracts simulation parameters and component data for headless operation.
 *
 * <p>This parser is designed for REST APIs, CLI tools, and batch processing,
 * containing no Swing/AWT dependencies.</p>
 */
public class CircuitFileParser {

    private static final String NIX = CircuitFileConstants.NIX;
    private static final String SEPARATOR_ASCII_STRINGARRAY = CircuitFileConstants.SEPARATOR_ASCII_STRINGARRAY;

    private static final Set<String> KNOWN_ELEMENT_TOKENS = Set.of(
            "labelAnfangsKnoten[]",
            "labelEndKnoten[]",
            "enabledShorted",
            "parentSheetIdentifier",
            "typ",
            "uniqueObjectIdentifier",
            "x",
            "y",
            "parameter[]",
            "parameter",
            "parameterString[]",
            "parameterString",
            "nameOpt[]",
            "nameOpt",
            "orientierung",
            "idStringDialog"
    );

    /**
     * Parses a .ipes circuit file and returns the circuit model.
     *
     * @param filePath path to the .ipes file
     * @return parsed circuit model
     * @throws IOException if the file cannot be read
     * @throws CircuitParseException if the file format is invalid
     */
    public CircuitModel parse(String filePath) throws IOException, CircuitParseException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Circuit file not found: " + filePath);
        }

        String[] lines = readFileLines(file);
        return parseLines(lines, filePath);
    }

    /**
     * Parses circuit data from a BufferedReader.
     *
     * @param reader the reader containing circuit data
     * @param sourceName name/path for error messages
     * @return parsed circuit model
     * @throws IOException if reading fails
     * @throws CircuitParseException if the format is invalid
     */
    public CircuitModel parse(BufferedReader reader, String sourceName) throws IOException, CircuitParseException {
        List<String> lineList = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lineList.add(line);
        }
        String[] lines = lineList.toArray(new String[0]);
        return parseLines(lines, sourceName);
    }

    /**
     * Parses circuit data from an InputStream.
     *
     * @param inputStream the input stream containing circuit data
     * @param sourceName name/path for error messages
     * @return parsed circuit model
     * @throws IOException if reading fails
     * @throws CircuitParseException if the format is invalid
     */
    public CircuitModel parse(InputStream inputStream, String sourceName) throws IOException, CircuitParseException {
        byte[] bytes = inputStream.readAllBytes();
        if (isGzipCompressed(bytes)) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bytes)), StandardCharsets.UTF_8))) {
                return parse(reader, sourceName);
            }
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            return parse(reader, sourceName);
        }
    }

    /**
     * Reads all lines from a .ipes file (handles gzip compression).
     */
    private String[] readFileLines(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            InputStream inputStream;

            if (isGzipCompressed(file)) {
                inputStream = new GZIPInputStream(fis);
            } else {
                inputStream = fis;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                return lines.toArray(new String[0]);
            }
        }
    }

    /**
     * Checks if a file is gzip compressed by examining magic bytes.
     */
    private boolean isGzipCompressed(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] signature = new byte[2];
            int read = fis.read(signature);
            if (read < 2) {
                return false;
            }
            return (signature[0] == (byte) 0x1f) && (signature[1] == (byte) 0x8b);
        }
    }

    private boolean isGzipCompressed(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        return (data[0] == (byte) 0x1f) && (data[1] == (byte) 0x8b);
    }

    /**
     * Parses the array of lines into a CircuitModel.
     */
    private CircuitModel parseLines(String[] lines, String filePath) throws CircuitParseException {
        CircuitModel model = new CircuitModel();
        model.setFilePath(filePath);

        Map<String, Integer> lineTokenMap = buildTokenMap(lines);
        TokenMap tokenMap = new TokenMap(lines, true);

        // Parse simulation parameters
        parseSimulationParameters(model, lines, lineTokenMap);

        // Parse display settings
        parseDisplaySettings(model, lines, lineTokenMap);

        // Parse file metadata
        parseFileMetadata(model, lines, lineTokenMap);

        // Parse optimizer parameters
        parseOptimizerParameters(model, lines, lineTokenMap);

        // Parse scripting blocks
        parseScripterBlocks(model, lines, lineTokenMap);

        // Parse file manager block
        parseFileManagerBlock(model, lines, lineTokenMap);

        // Parse signal names
        parseSignalNames(model, lines, lineTokenMap);

        // Parse extra global tokens (ANSICHT_SHOW_*, etc.)
        parseExtraTokens(model, lines, lineTokenMap);

        // Parse connections and components
        parseConnections(tokenMap, model);
        parseCircuitComponents(tokenMap, model);

        // Validate pre-simulation time step
        if (model.getPreSimulationTimeStep() <= 0) {
            model.setPreSimulationTimeStep(model.getTimeStep());
        }

        return model;
    }

    /**
     * Builds a map from token names to line numbers.
     */
    private Map<String, Integer> buildTokenMap(String[] lines) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }

            int tokenStart = 0;
            while (tokenStart < line.length() && Character.isWhitespace(line.charAt(tokenStart))) {
                tokenStart++;
            }
            if (tokenStart >= line.length()) {
                continue;
            }

            char firstChar = line.charAt(tokenStart);
            if (Character.isDigit(firstChar)) {
                continue;
            }

            int tokenEnd = tokenStart;
            while (tokenEnd < line.length() && !Character.isWhitespace(line.charAt(tokenEnd))) {
                tokenEnd++;
            }

            String token = line.substring(tokenStart, tokenEnd);
            if (!map.containsKey(token)) {
                map.put(token, i);
            }
        }

        return map;
    }

    /**
     * Parses simulation-related parameters.
     */
    private void parseSimulationParameters(CircuitModel model, String[] lines,
                                           Map<String, Integer> tokenMap) throws CircuitParseException {
        if (tokenMap.containsKey("tDURATION")) {
            model.setSimulationDuration(readDouble(lines, tokenMap, "tDURATION", 0.02));
        }

        if (tokenMap.containsKey("dt")) {
            model.setTimeStep(readDouble(lines, tokenMap, "dt", 1e-6));
        }

        if (tokenMap.containsKey("tPAUSE")) {
            model.setPauseTime(readDouble(lines, tokenMap, "tPAUSE", -1));
        }

        if (tokenMap.containsKey("T_pre")) {
            model.setPreSimulationTime(readDouble(lines, tokenMap, "T_pre", -1));
        }

        if (tokenMap.containsKey("dt_pre")) {
            model.setPreSimulationTimeStep(readDouble(lines, tokenMap, "dt_pre", 0));
        }

        if (tokenMap.containsKey("solverType")) {
            int solverIndex = readInt(lines, tokenMap, "solverType", 0);
            model.setSolverType(SolverType.fromOldGeckoIndex(solverIndex));
        }
    }

    /**
     * Parses display settings.
     */
    private void parseDisplaySettings(CircuitModel model, String[] lines,
                                      Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("dpix")) {
            model.setDisplayPixels(readInt(lines, tokenMap, "dpix", 16));
        }

        if (tokenMap.containsKey("fontSize")) {
            model.setFontSize(readInt(lines, tokenMap, "fontSize", 12));
        }

        if (tokenMap.containsKey("fontTyp")) {
            String fontLine = lines[tokenMap.get("fontTyp")];
            String fontType = fontLine.substring("fontTyp ".length()).trim();
            model.setFontType(fontType);
        }

        if (tokenMap.containsKey("fensterWidth")) {
            model.setWindowWidth(readInt(lines, tokenMap, "fensterWidth", -1));
        }

        if (tokenMap.containsKey("fensterHeight")) {
            model.setWindowHeight(readInt(lines, tokenMap, "fensterHeight", -1));
        }

        if (tokenMap.containsKey("worksheetSize")) {
            String wsLine = lines[tokenMap.get("worksheetSize")];
            String[] parts = wsLine.split("\\s+");
            if (parts.length >= 2) {
                model.setWorksheetSize(parts[1]);
            }
        }

        if (tokenMap.containsKey("path")) {
            String pathLine = lines[tokenMap.get("path")];
            if (pathLine.length() > "path ".length()) {
                model.setPath(pathLine.substring("path ".length()).trim());
            }
        }
    }

    /**
     * Parses file metadata.
     */
    private void parseFileMetadata(CircuitModel model, String[] lines,
                                   Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("FileVersion")) {
            model.setFileVersion(readInt(lines, tokenMap, "FileVersion", -1));
        }

        if (tokenMap.containsKey("UniqueFileId")) {
            model.setUniqueFileId(readInt(lines, tokenMap, "UniqueFileId", 0));
        }

        if (tokenMap.containsKey("DtStor")) {
            String dateLine = lines[tokenMap.get("DtStor")];
            String[] parts = dateLine.split("\\s+");
            if (parts.length >= 2) {
                model.setCreationDate(parts[1]);
            }
        }
    }

    /**
     * Parses optimizer parameters.
     */
    private void parseOptimizerParameters(CircuitModel model, String[] lines,
                                          Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("optimizerName[]") || tokenMap.containsKey("optimizerName")) {
            List<String> names = readStringArray(lines, tokenMap, "optimizerName[]");
            List<Double> values = readDoubleArray(lines, tokenMap, "optimizerValue[]");

            model.setOptimizerNames(names);
            model.setOptimizerValues(values);

            int count = Math.min(names.size(), values.size());
            for (int i = 0; i < count; i++) {
                String name = names.get(i);
                Double value = values.get(i);
                if (!name.isEmpty() && !name.equals(NIX) && value != null && !value.isNaN()) {
                    model.setOptimizerParameter(name, value);
                }
            }
        }
    }

    /**
     * Parses scripting code blocks.
     */
    private void parseScripterBlocks(CircuitModel model, String[] lines,
                                     Map<String, Integer> tokenMap) {
        model.setScripterCode(readBlockContent(lines, tokenMap, "<scripterCode>", "<\\scripterCode>"));
        model.setScripterImports(readBlockContent(lines, tokenMap, "<scripterImports>", "<\\scripterImports>"));
        model.setScripterDeclarations(readBlockContent(lines, tokenMap,
                "<scripterDeclarations>", "<\\scripterDeclarations>"));
        model.setScripterExtraFiles(readBlockContent(lines, tokenMap,
                "<extraScriptSourceFiles>", "<\\extraScriptSourceFiles>"));
    }

    /**
     * Parses file manager block.
     */
    private void parseFileManagerBlock(CircuitModel model, String[] lines,
                                       Map<String, Integer> tokenMap) {
        model.setFileManagerBlock(readBlockContent(lines, tokenMap,
                "<GeckoFileManager>", "<\\GeckoFileManager>"));
    }

    /**
     * Parses data container signal names.
     */
    private void parseSignalNames(CircuitModel model, String[] lines,
                                  Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("dataContainerSignals[]") || tokenMap.containsKey("dataContainerSignals")) {
            List<String> signals = readStringArray(lines, tokenMap, "dataContainerSignals[]");
            model.setDataContainerSignals(signals.toArray(new String[0]));
        }
    }

    /**
     * Parses additional global tokens (such as display mode flags).
     */
    private void parseExtraTokens(CircuitModel model, String[] lines, Map<String, Integer> lineTokenMap) {
        for (Map.Entry<String, Integer> entry : lineTokenMap.entrySet()) {
            String token = entry.getKey();
            int lineIdx = entry.getValue();
            if (token.startsWith("ANSICHT_SHOW_") || token.startsWith("worksheetSize")) {
                String line = lines[lineIdx].trim();
                int spaceIdx = line.indexOf(' ');
                if (spaceIdx > 0) {
                    model.setExtraToken(token, line.substring(spaceIdx + 1).trim());
                }
            }
        }
    }

    /**
     * Parses wire connections.
     */
    private void parseConnections(TokenMap tokenMap, CircuitModel model) {
        parseConnectionsForDomain(tokenMap, "verbindungLK", "LK", 0, model);
        parseConnectionsForDomain(tokenMap, "verbindungCONTROL", "CONTROL", 1, model);
        parseConnectionsForDomain(tokenMap, "verbindungTHERM", "THERMAL", 2, model);
    }

    /**
     * Parses wire connections of one domain. Wire coordinates are written as
     * {@code x[]}/{@code y[]} data lines; some legacy writers emit them as
     * plain {@code x}/{@code y}, which is accepted as a fallback. A
     * non-numeric {@code connectorType} (e.g. the literal "LK") keeps the
     * domain's default instead of dropping the connection.
     */
    private void parseConnectionsForDomain(TokenMap tokenMap, String tokenKey, String type,
                                          int defaultConnectorType, CircuitModel model) {
        TokenMap connBlock;
        while ((connBlock = tokenMap.getSpecialBlockTokenMap(tokenKey)) != null) {
            try {
                String label = connBlock.readDataLine("label", "");
                int[] xPoints = connBlock.readDataLine("x[]", (int[]) null);
                if (xPoints == null || xPoints.length == 0) {
                    xPoints = connBlock.readDataLine("x", new int[0]);
                }
                int[] yPoints = connBlock.readDataLine("y[]", (int[]) null);
                if (yPoints == null || yPoints.length == 0) {
                    yPoints = connBlock.readDataLine("y", new int[0]);
                }
                int length = Math.min(xPoints.length, yPoints.length);
                int[][] points = new int[length][2];
                for (int i = 0; i < length; i++) {
                    points[i][0] = xPoints[i];
                    points[i][1] = yPoints[i];
                }

                CircuitModel.ConnectionData conn = new CircuitModel.ConnectionData(type, points);
                conn.setLabel(label.equals(NIX) ? "" : label);
                conn.setEnabledShorted(connBlock.readDataLine("enabledShorted", 0));
                conn.setParentSheetIdentifier(connBlock.readDataLine("parentSheetIdentifier", 0L));
                conn.setUniqueObjectIdentifier(connBlock.readDataLine("uniqueObjectIdentifier", 0L));

                // readDataLine(int) never throws: it logs and returns the
                // default for non-numeric values such as "connectorType LK"
                conn.setConnectorType(connBlock.readDataLine("connectorType", defaultConnectorType));

                model.addConnection(conn);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Parses all component blocks (<ElementLK>, <ElementCONTROL>, <ElementTHERM>, <ElementSPECIAL>).
     *
     * <p>The classic format marks blocks with the short tokens {@code e},
     * {@code c}, {@code eTH} and {@code sp}; some writers instead use the
     * block tag names (e.g. {@code ElementLK}) in mixed case, which are
     * accepted as aliases for the same domains.</p>
     */
    private void parseCircuitComponents(TokenMap tokenMap, CircuitModel model) {
        parseComponentsForDomain(tokenMap, "e", "LK", model);
        parseComponentsForDomain(tokenMap, "ElementLK", "LK", model);
        parseComponentsForDomain(tokenMap, "c", "CONTROL", model);
        parseComponentsForDomain(tokenMap, "ElementCONTROL", "CONTROL", model);
        parseComponentsForDomain(tokenMap, "ElementControl", "CONTROL", model);
        parseComponentsForDomain(tokenMap, "eTH", "THERM", model);
        parseComponentsForDomain(tokenMap, "ElementTHERM", "THERM", model);
        parseComponentsForDomain(tokenMap, "ElementTherm", "THERM", model);
        parseComponentsForDomain(tokenMap, "sp", "SPECIAL", model);
        parseComponentsForDomain(tokenMap, "ElementSPECIAL", "SPECIAL", model);
        parseComponentsForDomain(tokenMap, "ElementSpecial", "SPECIAL", model);
    }

    private void parseComponentsForDomain(TokenMap tokenMap, String tokenKey, String family, CircuitModel model) {
        TokenMap elementBlock;
        while ((elementBlock = tokenMap.getSpecialBlockTokenMap(tokenKey)) != null) {
            try {
                int type = elementBlock.readDataLine("typ", -1);
                if (type < 0) {
                    continue;
                }

                String name = elementBlock.readDataLine("idStringDialog", "");
                if (name.equals(NIX)) {
                    name = "";
                }
                int x = elementBlock.readDataLine("x", 0);
                int y = elementBlock.readDataLine("y", 0);
                int orientation = elementBlock.readDataLine("orientierung", 0);

                List<Double> paramsList = readComponentParameters(elementBlock);
                double[] params = paramsList.stream()
                        .mapToDouble(v -> v != null ? v : Double.NaN)
                        .toArray();

                String[] xLabels = elementBlock.readDataLine("labelAnfangsKnoten[]", new String[0]);
                String[] yLabels = elementBlock.readDataLine("labelEndKnoten[]", new String[0]);
                String[] paramStrings = elementBlock.readDataLine("parameterString[]", new String[0]);
                String[] nameOpt = elementBlock.readDataLine("nameOpt[]", new String[0]);
                long uniqueId = elementBlock.readDataLine("uniqueObjectIdentifier", 0L);
                int enabledShorted = elementBlock.readDataLine("enabledShorted", 0);
                long parentSheetId = elementBlock.readDataLine("parentSheetIdentifier", 0L);

                CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, name, x, y, orientation);
                comp.setFamily(family);
                comp.setRawParameters(params);
                comp.setRawTerminalXLabels(xLabels);
                comp.setRawTerminalYLabels(yLabels);
                comp.setTerminalXLabels(filterNix(xLabels));
                comp.setTerminalYLabels(filterNix(yLabels));
                comp.setParameterStrings(paramStrings);
                comp.setNameOpt(nameOpt);
                comp.setUniqueObjectIdentifier(uniqueId);
                comp.setEnabledShorted(enabledShorted);
                comp.setParentSheetIdentifier(parentSheetId);

                for (int i = 0; i < params.length; i++) {
                    comp.setParameter("param" + i, params[i]);
                }
                if (params.length > 0) {
                    comp.setParameter(resolveParameterKey(type), params[0]);
                }

                // Preserve extra block lines (e.g. XML parameters, loss models, etc.)
                if (elementBlock.asciiLines != null) {
                    for (String blockLine : elementBlock.asciiLines) {
                        String trimmed = blockLine.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        String token = trimmed.split("\\s+")[0];
                        if (!KNOWN_ELEMENT_TOKENS.contains(token)) {
                            comp.addExtraLine(blockLine);
                        }
                    }
                }

                switch (family) {
                    case "CONTROL" -> model.addControlComponent(comp);
                    case "THERM" -> model.addThermalComponent(comp);
                    case "SPECIAL" -> model.addSpecialComponent(comp);
                    default -> model.addCircuitComponent(comp);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Reads a double array from a TokenMap, handling "/" separator format used in .ipes files.
     */
    private List<Double> readDoubleArrayFromTokenMap(TokenMap tokenMap, String identifier) {
        List<Double> result = new ArrayList<>();
        try {
            String line = tokenMap.getLineString(identifier, "");
            if (line == null || line.isEmpty()) {
                return result;
            }

            int arrayStart = line.indexOf("[] ");
            if (arrayStart < 0) {
                return result;
            }

            String arrayPart = line.substring(arrayStart + 3).trim();
            if (arrayPart.equals("null")) {
                return result;
            }

            String[] parts = arrayPart.split(SEPARATOR_ASCII_STRINGARRAY, -1);
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    result.add(Double.NaN);
                } else {
                    try {
                        result.add(Double.parseDouble(trimmed));
                    } catch (NumberFormatException e) {
                        result.add(Double.NaN);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * Reads the parameter[] array from a component block.
     */
    private List<Double> readComponentParameters(TokenMap elementBlock) {
        List<Double> result = elementBlock.readDataLineDoubleArray("parameter[]");
        if (result != null && !result.isEmpty()) {
            return result;
        }
        return readDoubleArrayFromTokenMap(elementBlock, "parameter[]");
    }

    /**
     * Returns the semantic parameter key for the primary value of a component type.
     */
    private static String resolveParameterKey(int type) {
        return switch (type) {
            case 1 -> "resistance";     // LK_R
            case 2 -> "inductance";     // LK_L
            case 3 -> "capacitance";    // LK_C
            case 4 -> "amplitude";      // LK_U voltage source
            case 5 -> "amplitude";      // LK_I current source
            case 6 -> "forwardVoltage"; // LK_D diode
            case 7 -> "resistance";     // LK_S ideal switch (on-resistance)
            default -> "value";
        };
    }

    /**
     * Filters out "NIX_NIX_NIX" placeholder labels, returning real label names only.
     */
    private static String[] filterNix(String[] labels) {
        if (labels == null) {
            return new String[0];
        }
        List<String> filtered = new ArrayList<>();
        for (String label : labels) {
            if (label != null && !label.equals(NIX) && !label.isBlank()) {
                filtered.add(label.trim());
            }
        }
        return filtered.toArray(new String[0]);
    }

    // ==================== Utility methods ====================

    private double readDouble(String[] lines, Map<String, Integer> tokenMap,
                              String key, double defaultValue) {
        try {
            if (!tokenMap.containsKey(key)) {
                return defaultValue;
            }
            String line = lines[tokenMap.get(key)];
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                return Double.parseDouble(parts[1]);
            }
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

    private int readInt(String[] lines, Map<String, Integer> tokenMap,
                        String key, int defaultValue) {
        try {
            if (!tokenMap.containsKey(key)) {
                return defaultValue;
            }
            String line = lines[tokenMap.get(key)];
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

    private List<String> readStringArray(String[] lines, Map<String, Integer> tokenMap, String key) {
        List<String> result = new ArrayList<>();
        String actualKey = key;
        if (!tokenMap.containsKey(actualKey)) {
            if (actualKey.endsWith("[]") && tokenMap.containsKey(actualKey.substring(0, actualKey.length() - 2))) {
                actualKey = actualKey.substring(0, actualKey.length() - 2);
            } else if (!actualKey.endsWith("[]") && tokenMap.containsKey(actualKey + "[]")) {
                actualKey = actualKey + "[]";
            } else {
                return result;
            }
        }

        String line = lines[tokenMap.get(actualKey)];
        int startIndex = line.indexOf("[]");
        String arrayPart;
        if (startIndex >= 0) {
            arrayPart = line.substring(startIndex + 2).trim();
        } else {
            int spaceIdx = line.indexOf(' ');
            if (spaceIdx < 0) {
                return result;
            }
            arrayPart = line.substring(spaceIdx + 1).trim();
        }

        if (arrayPart.isEmpty() || arrayPart.equals("null")) {
            return result;
        }

        if (arrayPart.contains(SEPARATOR_ASCII_STRINGARRAY)) {
            String[] parts = arrayPart.split(SEPARATOR_ASCII_STRINGARRAY, -1);
            int startIdx = arrayPart.startsWith(SEPARATOR_ASCII_STRINGARRAY) ? 1 : 0;
            for (int i = startIdx; i < parts.length; i++) {
                String trimmed = parts[i].trim();
                if (trimmed.equals(NIX)) {
                    result.add("");
                } else {
                    result.add(trimmed);
                }
            }
        } else {
            String[] parts = arrayPart.split("\\s+");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.equals(NIX)) {
                    result.add("");
                } else {
                    result.add(trimmed);
                }
            }
        }

        return result;
    }

    private List<Double> readDoubleArray(String[] lines, Map<String, Integer> tokenMap, String key) {
        List<Double> result = new ArrayList<>();
        String actualKey = key;
        if (!tokenMap.containsKey(actualKey)) {
            if (actualKey.endsWith("[]") && tokenMap.containsKey(actualKey.substring(0, actualKey.length() - 2))) {
                actualKey = actualKey.substring(0, actualKey.length() - 2);
            } else if (!actualKey.endsWith("[]") && tokenMap.containsKey(actualKey + "[]")) {
                actualKey = actualKey + "[]";
            } else {
                return result;
            }
        }

        String line = lines[tokenMap.get(actualKey)];
        int startIndex = line.indexOf("[]");
        String arrayPart;
        if (startIndex >= 0) {
            arrayPart = line.substring(startIndex + 2).trim();
        } else {
            int spaceIdx = line.indexOf(' ');
            if (spaceIdx < 0) {
                return result;
            }
            arrayPart = line.substring(spaceIdx + 1).trim();
        }

        if (arrayPart.isEmpty() || arrayPart.equals("null")) {
            return result;
        }

        String[] parts;
        int startIdx = 0;
        if (arrayPart.contains(SEPARATOR_ASCII_STRINGARRAY)) {
            parts = arrayPart.split(SEPARATOR_ASCII_STRINGARRAY, -1);
            if (arrayPart.startsWith(SEPARATOR_ASCII_STRINGARRAY)) {
                startIdx = 1;
            }
        } else {
            parts = arrayPart.split("\\s+");
        }

        for (int i = startIdx; i < parts.length; i++) {
            String trimmed = parts[i].trim();
            if (trimmed.isEmpty()) {
                result.add(Double.NaN);
                continue;
            }
            try {
                result.add(Double.parseDouble(trimmed));
            } catch (NumberFormatException e) {
                result.add(Double.NaN);
            }
        }

        return result;
    }

    private String readBlockContent(String[] lines, Map<String, Integer> tokenMap,
                                    String startTag, String endTag) {
        if (!tokenMap.containsKey(startTag)) {
            return "";
        }

        int startLine = tokenMap.get(startTag);
        StringBuilder content = new StringBuilder();

        for (int i = startLine + 1; i < lines.length; i++) {
            if (lines[i].startsWith(endTag)) {
                break;
            }
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(lines[i]);
        }

        return content.toString();
    }

    /**
     * Exception thrown when parsing fails.
     */
    public static class CircuitParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public CircuitParseException(String message) {
            super(message);
        }

        public CircuitParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
