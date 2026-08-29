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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * GUI-free serializer for GeckoCIRCUITS .ipes circuit files.
 * Serializes {@link CircuitModel} instances into gzip-compressed or plain ASCII .ipes format.
 *
 * <p>Emits the exact token grammar consumed by {@link CircuitFileParser} and GeckoCIRCUITS Swing UI.</p>
 */
public final class CircuitFileWriter {

    private static final String[] DEFAULT_DISPLAY_TOKENS = {
            "ANSICHT_SHOW_LK_NAME",
            "ANSICHT_SHOW_LK_PARAMETER",
            "ANSICHT_SHOW_LK_FLOWDIR",
            "ANSICHT_SHOW_LK_TEXTLINIE",
            "ANSICHT_SHOW_THERM_NAME",
            "ANSICHT_SHOW_THERM_PARAMETER",
            "ANSICHT_SHOW_THERM_FLOWDIR",
            "ANSICHT_SHOW_THERM_TEXTLINIE",
            "ANSICHT_SHOW_CONTROL_NAME",
            "ANSICHT_SHOW_CONTROL_PARAMETER",
            "ANSICHT_SHOW_CONTROL_TEXTLINIE"
    };

    private CircuitFileWriter() {
        // Utility class
    }

    /**
     * Serializes the given circuit model to gzip-compressed .ipes bytes.
     *
     * @param model circuit model to serialize
     * @return gzip-compressed byte array
     * @throws IOException if compression fails
     */
    public static byte[] write(CircuitModel model) throws IOException {
        return write(model, true);
    }

    /**
     * Serializes the given circuit model to byte array.
     *
     * @param model circuit model to serialize
     * @param gzip whether to apply gzip compression
     * @return byte array (gzip-compressed or UTF-8 ASCII)
     * @throws IOException if compression fails
     */
    public static byte[] write(CircuitModel model, boolean gzip) throws IOException {
        if (model == null) {
            throw new IllegalArgumentException("CircuitModel must not be null");
        }
        String ascii = writeToString(model);
        byte[] rawBytes = ascii.getBytes(StandardCharsets.UTF_8);
        if (!gzip) {
            return rawBytes;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(rawBytes);
        }
        return baos.toByteArray();
    }

    /**
     * Serializes the circuit model to a file (gzip-compressed).
     *
     * @param model circuit model to serialize
     * @param file target file path
     * @throws IOException if writing fails
     */
    public static void write(CircuitModel model, Path file) throws IOException {
        write(model, file, true);
    }

    /**
     * Serializes the circuit model to a file.
     *
     * @param model circuit model to serialize
     * @param file target file path
     * @param gzip whether to apply gzip compression
     * @throws IOException if writing fails
     */
    public static void write(CircuitModel model, Path file, boolean gzip) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Target file path must not be null");
        }
        byte[] bytes = write(model, gzip);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, bytes);
    }

    /**
     * Generates the line-oriented ASCII representation of the circuit model.
     *
     * @param model circuit model to convert
     * @return ASCII string matching .ipes file format
     */
    public static String writeToString(CircuitModel model) {
        if (model == null) {
            throw new IllegalArgumentException("CircuitModel must not be null");
        }

        StringBuilder sb = new StringBuilder();
        int elementCounter = 0;

        // 1. Wire Connections
        for (CircuitModel.ConnectionData conn : model.getConnections()) {
            String connHeader = switch (conn.getType()) {
                case "CONTROL" -> "verbindungCONTROL";
                case "THERMAL" -> "verbindungTHERM";
                default -> "verbindungLK";
            };

            sb.append("\n").append(connHeader).append(" (").append(elementCounter).append(")\n");
            sb.append("<Verbindung>");

            sb.append("\nlabel");
            SerializationUtils.appendAsString(sb, conn.getLabel());

            int[][] points = conn.getPoints();
            int[] xPoints = new int[points.length];
            int[] yPoints = new int[points.length];
            for (int i = 0; i < points.length; i++) {
                xPoints[i] = points[i][0];
                yPoints[i] = points[i][1];
            }
            sb.append("\nx");
            SerializationUtils.appendIntArray(sb, xPoints);
            sb.append("\ny");
            SerializationUtils.appendIntArray(sb, yPoints);

            sb.append("\nenabledShorted");
            SerializationUtils.appendAsString(sb, conn.getEnabledShorted());

            sb.append("\nparentSheetIdentifier");
            SerializationUtils.appendAsString(sb, conn.getParentSheetIdentifier());

            sb.append("\nconnectorType");
            SerializationUtils.appendAsString(sb, conn.getConnectorType());

            if (conn.getUniqueObjectIdentifier() != 0) {
                sb.append("\nuniqueObjectIdentifier");
                SerializationUtils.appendAsString(sb, conn.getUniqueObjectIdentifier());
            }

            sb.append("\n<\\Verbindung>\n");
            elementCounter++;
        }

        // 2. Components across all domains
        List<CircuitModel.ComponentData> allComponents = new ArrayList<>();
        allComponents.addAll(model.getCircuitComponents());
        allComponents.addAll(model.getControlComponents());
        allComponents.addAll(model.getThermalComponents());
        allComponents.addAll(model.getSpecialComponents());

        for (CircuitModel.ComponentData comp : allComponents) {
            String exportChar;
            String saveId;
            switch (comp.getFamily()) {
                case "CONTROL" -> {
                    exportChar = "c";
                    saveId = "ElementCONTROL";
                }
                case "THERM" -> {
                    exportChar = "eTH";
                    saveId = "ElementTHERM";
                }
                case "SPECIAL" -> {
                    exportChar = "sp";
                    saveId = "ElementSPECIAL";
                }
                default -> {
                    exportChar = "e";
                    saveId = "ElementLK";
                }
            }

            sb.append("\n").append(exportChar).append(" (").append(elementCounter).append(")\n");
            sb.append("<").append(saveId).append(">");

            sb.append("\nlabelAnfangsKnoten");
            SerializationUtils.appendStringArray(sb, comp.getRawTerminalXLabels());

            sb.append("\nlabelEndKnoten");
            SerializationUtils.appendStringArray(sb, comp.getRawTerminalYLabels());

            sb.append("\nenabledShorted");
            SerializationUtils.appendAsString(sb, comp.getEnabledShorted());

            sb.append("\nparentSheetIdentifier");
            SerializationUtils.appendAsString(sb, comp.getParentSheetIdentifier());

            sb.append("\ntyp");
            SerializationUtils.appendAsString(sb, comp.getType());

            sb.append("\nuniqueObjectIdentifier");
            SerializationUtils.appendAsString(sb, comp.getUniqueObjectIdentifier());

            if (comp.getCoupledReferenceID() != 0) {
                sb.append("\ncoupledReferenceID[]");
                SerializationUtils.appendAsString(sb, comp.getCoupledReferenceID());
                sb.append(" ");
            }

            sb.append("\nx");
            SerializationUtils.appendAsString(sb, comp.getPosition()[0]);

            sb.append("\ny");
            SerializationUtils.appendAsString(sb, comp.getPosition()[1]);

            sb.append("\nparameter");
            SerializationUtils.appendDoubleArray(sb, comp.getRawParameters());

            sb.append("\nparameterString");
            SerializationUtils.appendStringArray(sb, comp.getParameterStrings());

            sb.append("\nnameOpt");
            SerializationUtils.appendStringArray(sb, comp.getNameOpt());

            sb.append("\norientierung");
            SerializationUtils.appendAsString(sb, comp.getOrientation());

            sb.append("\nidStringDialog");
            SerializationUtils.appendAsString(sb, comp.getName());

            for (String extraLine : comp.getExtraLines()) {
                sb.append("\n").append(extraLine);
            }

            sb.append("\n\n<\\").append(saveId).append(">\n");
            elementCounter++;
        }

        // 3. Optimizer section
        sb.append("\noptimizerName");
        SerializationUtils.appendStringArray(sb, model.getOptimizerNames());
        sb.append("\noptimizerValue");
        SerializationUtils.appendDoubleArray(sb, model.getOptimizerValues());

        // 4. Scripter section
        sb.append("\n<scripterCode>\n").append(model.getScripterCode()).append("\n<\\scripterCode>");
        sb.append("\n<scripterImports>\n").append(model.getScripterImports()).append("\n<\\scripterImports>");
        sb.append("\n<scripterDeclarations>\n").append(model.getScripterDeclarations()).append(" \n<\\scripterDeclarations>");
        sb.append("\n<extraScriptSourceFiles>\n").append(model.getScripterExtraFiles()).append(" \n<\\extraScriptSourceFiles>");

        // 5. File Manager block (if present)
        sb.append("\n");
        if (model.getFileManagerBlock() != null && !model.getFileManagerBlock().isEmpty()) {
            sb.append("\n<GeckoFileManager>\n").append(model.getFileManagerBlock()).append("\n<\\GeckoFileManager>\n");
        }
        sb.append("\n\n");

        // 6. Metadata and Simulation parameters
        String dateStr = model.getCreationDate() != null && !model.getCreationDate().isEmpty()
                ? model.getCreationDate()
                : LocalDate.now().toString();
        sb.append("\nDtStor ").append(dateStr);
        sb.append("\ntDURATION ").append(model.getSimulationDuration());
        sb.append("\ndt ").append(model.getTimeStep());
        sb.append("\ntPAUSE ").append(model.getPauseTime());
        sb.append("\nT_pre ").append(model.getPreSimulationTime());
        sb.append("\ndt_pre ").append(model.getPreSimulationTimeStep());
        sb.append("\nsolverType ").append(model.getSolverType() != null ? model.getSolverType().getOldGeckoIndex() : 0);
        sb.append("\npath ").append(model.getPath() != null ? model.getPath() : "");

        // 7. Display settings
        sb.append("\n\ndpix ").append(model.getDisplayPixels());
        sb.append("\nfontSize ").append(model.getFontSize());
        sb.append("\nfontTyp ").append(model.getFontType() != null ? model.getFontType() : "Arial");
        sb.append("\nfensterWidth ").append(model.getWindowWidth());
        sb.append("\nfensterHeight ").append(model.getWindowHeight());
        if (model.getWorksheetSize() != null && !model.getWorksheetSize().isEmpty()) {
            sb.append("\nworksheetSize ").append(model.getWorksheetSize());
        }

        // 8. Display mode flags
        Map<String, String> extra = model.getExtraTokens();
        for (String tokenName : DEFAULT_DISPLAY_TOKENS) {
            if (extra.containsKey(tokenName)) {
                sb.append("\n").append(tokenName).append(" ").append(extra.get(tokenName));
            } else {
                boolean defVal = !tokenName.contains("FLOWDIR");
                sb.append("\n").append(tokenName).append(" ").append(defVal);
            }
        }
        for (Map.Entry<String, String> entry : extra.entrySet()) {
            if (!entry.getKey().startsWith("ANSICHT_SHOW_") && !entry.getKey().equals("worksheetSize")) {
                sb.append("\n").append(entry.getKey()).append(" ").append(entry.getValue());
            }
        }

        // 9. Version metadata
        int fileVer = model.getFileVersion() > 0 ? model.getFileVersion() : 175;
        sb.append("\nFileVersion ").append(fileVer);
        sb.append("\nUniqueFileId ").append(model.getUniqueFileId());

        // 10. Signal names
        sb.append("\ndataContainerSignals");
        SerializationUtils.appendStringArray(sb, model.getDataContainerSignals());

        // 11. Footer
        sb.append("\n\n=======================\n ");

        return sb.toString();
    }
}
