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

import java.util.*;

/**
 * GUI-free representation of a parsed GeckoCIRCUITS circuit model.
 * Contains all simulation parameters and component data extracted from .ipes files.
 *
 * <p>This class is designed for headless operation and contains no Swing/AWT dependencies.</p>
 */
public class CircuitModel {

    // Simulation parameters
    private double simulationDuration;
    private double timeStep;
    private double preSimulationTime;
    private double preSimulationTimeStep;
    private double pauseTime = -1;
    private SolverType solverType = SolverType.SOLVER_BE;

    // File metadata
    private String filePath;
    private int fileVersion;
    private int uniqueFileId;
    private String creationDate;
    private String path = "";

    // Display settings (for reference, not used in headless mode)
    private int displayPixels = 16;
    private int fontSize = 12;
    private String fontType = "Arial";
    private int windowWidth = -1;
    private int windowHeight = -1;
    private String worksheetSize = "600x600";

    // Components
    private final List<ComponentData> circuitComponents = new ArrayList<>();
    private final List<ComponentData> controlComponents = new ArrayList<>();
    private final List<ComponentData> thermalComponents = new ArrayList<>();
    private final List<ComponentData> specialComponents = new ArrayList<>();
    private final List<ConnectionData> connections = new ArrayList<>();

    // Optimizer parameters
    private final Map<String, Double> optimizerParameters = new LinkedHashMap<>();
    private List<String> optimizerNames = new ArrayList<>();
    private List<Double> optimizerValues = new ArrayList<>();

    // Signal names
    private String[] dataContainerSignals = new String[0];

    // Scripting (if present)
    private String scripterCode = "";
    private String scripterImports = "";
    private String scripterDeclarations = "";
    private String scripterExtraFiles = "";

    // File manager (if present)
    private String fileManagerBlock = "";

    // Preservation of raw/unknown tokens (e.g. display flags, worksheet sizes, etc.)
    private final Map<String, String> extraTokens = new LinkedHashMap<>();

    // Constructor
    public CircuitModel() {
    }

    // Getters and setters for simulation parameters

    public double getSimulationDuration() {
        return simulationDuration;
    }

    public void setSimulationDuration(double simulationDuration) {
        this.simulationDuration = simulationDuration;
    }

    public double getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(double timeStep) {
        this.timeStep = timeStep;
    }

    public double getPreSimulationTime() {
        return preSimulationTime;
    }

    public void setPreSimulationTime(double preSimulationTime) {
        this.preSimulationTime = preSimulationTime;
    }

    public double getPreSimulationTimeStep() {
        return preSimulationTimeStep;
    }

    public void setPreSimulationTimeStep(double preSimulationTimeStep) {
        this.preSimulationTimeStep = preSimulationTimeStep;
    }

    public double getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(double pauseTime) {
        this.pauseTime = pauseTime;
    }

    public SolverType getSolverType() {
        return solverType;
    }

    public void setSolverType(SolverType solverType) {
        this.solverType = solverType;
    }

    // File metadata getters/setters

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    public int getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(int fileVersion) {
        this.fileVersion = fileVersion;
    }

    public int getUniqueFileId() {
        return uniqueFileId;
    }

    public void setUniqueFileId(int uniqueFileId) {
        this.uniqueFileId = uniqueFileId;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    // Display settings

    public int getDisplayPixels() {
        return displayPixels;
    }

    public void setDisplayPixels(int displayPixels) {
        this.displayPixels = displayPixels;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontType() {
        return fontType;
    }

    public void setFontType(String fontType) {
        this.fontType = fontType;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public String getWorksheetSize() {
        return worksheetSize;
    }

    public void setWorksheetSize(String worksheetSize) {
        this.worksheetSize = worksheetSize != null ? worksheetSize : "600x600";
    }

    // Component access

    public List<ComponentData> getCircuitComponents() {
        return circuitComponents;
    }

    public void addCircuitComponent(ComponentData component) {
        component.setFamily("LK");
        circuitComponents.add(component);
    }

    public List<ComponentData> getControlComponents() {
        return controlComponents;
    }

    public void addControlComponent(ComponentData component) {
        component.setFamily("CONTROL");
        controlComponents.add(component);
    }

    public List<ComponentData> getThermalComponents() {
        return thermalComponents;
    }

    public void addThermalComponent(ComponentData component) {
        component.setFamily("THERM");
        thermalComponents.add(component);
    }

    public List<ComponentData> getSpecialComponents() {
        return specialComponents;
    }

    public void addSpecialComponent(ComponentData component) {
        component.setFamily("SPECIAL");
        specialComponents.add(component);
    }

    public List<ComponentData> getAllComponents() {
        List<ComponentData> all = new ArrayList<>(getTotalComponentCount());
        all.addAll(circuitComponents);
        all.addAll(controlComponents);
        all.addAll(thermalComponents);
        all.addAll(specialComponents);
        return Collections.unmodifiableList(all);
    }

    public List<ConnectionData> getConnections() {
        return connections;
    }

    public void addConnection(ConnectionData connection) {
        connections.add(connection);
    }

    // Optimizer parameters

    public Map<String, Double> getOptimizerParameters() {
        return optimizerParameters;
    }

    public void setOptimizerParameter(String name, double value) {
        optimizerParameters.put(name, value);
    }

    public List<String> getOptimizerNames() {
        return optimizerNames;
    }

    public void setOptimizerNames(List<String> optimizerNames) {
        this.optimizerNames = optimizerNames != null ? optimizerNames : new ArrayList<>();
    }

    public List<Double> getOptimizerValues() {
        return optimizerValues;
    }

    public void setOptimizerValues(List<Double> optimizerValues) {
        this.optimizerValues = optimizerValues != null ? optimizerValues : new ArrayList<>();
    }

    // Signal names

    public String[] getDataContainerSignals() {
        return dataContainerSignals;
    }

    public void setDataContainerSignals(String[] dataContainerSignals) {
        this.dataContainerSignals = dataContainerSignals != null ? dataContainerSignals : new String[0];
    }

    // Scripting

    public String getScripterCode() {
        return scripterCode;
    }

    public void setScripterCode(String scripterCode) {
        this.scripterCode = scripterCode != null ? scripterCode : "";
    }

    public String getScripterImports() {
        return scripterImports;
    }

    public void setScripterImports(String scripterImports) {
        this.scripterImports = scripterImports != null ? scripterImports : "";
    }

    public String getScripterDeclarations() {
        return scripterDeclarations;
    }

    public void setScripterDeclarations(String scripterDeclarations) {
        this.scripterDeclarations = scripterDeclarations != null ? scripterDeclarations : "";
    }

    public String getScripterExtraFiles() {
        return scripterExtraFiles;
    }

    public void setScripterExtraFiles(String scripterExtraFiles) {
        this.scripterExtraFiles = scripterExtraFiles != null ? scripterExtraFiles : "";
    }

    // File Manager

    public String getFileManagerBlock() {
        return fileManagerBlock;
    }

    public void setFileManagerBlock(String fileManagerBlock) {
        this.fileManagerBlock = fileManagerBlock != null ? fileManagerBlock : "";
    }

    // Raw / extra tokens

    public Map<String, String> getExtraTokens() {
        return extraTokens;
    }

    public void setExtraToken(String key, String value) {
        extraTokens.put(key, value);
    }

    // Utility methods

    /**
     * Gets the total number of components in the circuit.
     *
     * @return total component count
     */
    public int getTotalComponentCount() {
        return circuitComponents.size() + controlComponents.size() + thermalComponents.size() + specialComponents.size();
    }

    /**
     * Checks if the circuit model has valid simulation parameters.
     *
     * @return true if parameters are valid
     */
    public boolean hasValidSimulationParameters() {
        return simulationDuration > 0 && timeStep > 0 && timeStep < simulationDuration;
    }

    @Override
    public String toString() {
        return "CircuitModel{" +
                "simulationDuration=" + simulationDuration +
                ", timeStep=" + timeStep +
                ", solverType=" + solverType +
                ", circuitComponents=" + circuitComponents.size() +
                ", controlComponents=" + controlComponents.size() +
                ", thermalComponents=" + thermalComponents.size() +
                ", specialComponents=" + specialComponents.size() +
                ", connections=" + connections.size() +
                '}';
    }

    /**
     * Represents a circuit component's data.
     */
    public static class ComponentData {
        private final int type;
        private String name;
        private final Map<String, Object> parameters;
        private final int[] position; // x, y coordinates
        private int orientation;
        private String[] terminalXLabels = new String[0];
        private String[] terminalYLabels = new String[0];
        private String[] rawTerminalXLabels = new String[0];
        private String[] rawTerminalYLabels = new String[0];
        private double[] rawParameters = new double[0];
        private String[] parameterStrings = new String[0];
        private String[] nameOpt = new String[0];
        private long uniqueObjectIdentifier;
        private long coupledReferenceID;
        private int enabledShorted;
        private long parentSheetIdentifier;
        private String family = "LK";
        private final List<String> extraLines = new ArrayList<>();

        public ComponentData(int type, String name) {
            this(type, name, 0, 0, 0);
        }

        public ComponentData(int type, String name, int x, int y, int orientation) {
            this.type = type;
            this.name = name != null ? name : "";
            this.parameters = new HashMap<>();
            this.position = new int[]{x, y};
            this.orientation = orientation;
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name != null ? name : "";
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameter(String key, Object value) {
            parameters.put(key, value);
            if (value instanceof Number num && rawParameters.length > 0) {
                if (key.startsWith("param")) {
                    try {
                        int idx = Integer.parseInt(key.substring("param".length()));
                        if (idx >= 0 && idx < rawParameters.length) {
                            rawParameters[idx] = num.doubleValue();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } else if (key.equals(resolveParameterKey(type))) {
                    rawParameters[0] = num.doubleValue();
                }
            }
        }

        public double[] getRawParameters() {
            if (rawParameters.length == 0 && !parameters.isEmpty()) {
                List<Double> list = new ArrayList<>();
                int idx = 0;
                while (parameters.containsKey("param" + idx)) {
                    Object val = parameters.get("param" + idx);
                    list.add(val instanceof Number n ? n.doubleValue() : Double.NaN);
                    idx++;
                }
                if (list.isEmpty()) {
                    String semanticKey = resolveParameterKey(type);
                    if (parameters.containsKey(semanticKey)) {
                        Object val = parameters.get(semanticKey);
                        list.add(val instanceof Number n ? n.doubleValue() : 0.0);
                    }
                }
                if (!list.isEmpty()) {
                    return list.stream().mapToDouble(Double::doubleValue).toArray();
                }
            }
            return rawParameters;
        }

        public void setRawParameters(double[] rawParameters) {
            this.rawParameters = rawParameters != null ? rawParameters : new double[0];
        }

        public String[] getParameterStrings() {
            return parameterStrings;
        }

        public void setParameterStrings(String[] parameterStrings) {
            this.parameterStrings = parameterStrings != null ? parameterStrings : new String[0];
        }

        public String[] getNameOpt() {
            return nameOpt;
        }

        public void setNameOpt(String[] nameOpt) {
            this.nameOpt = nameOpt != null ? nameOpt : new String[0];
        }

        public int[] getPosition() {
            return position;
        }

        public int getOrientation() {
            return orientation;
        }

        public void setOrientation(int orientation) {
            this.orientation = orientation;
        }

        public String[] getTerminalXLabels() {
            return terminalXLabels;
        }

        public void setTerminalXLabels(String[] terminalXLabels) {
            this.terminalXLabels = terminalXLabels != null ? terminalXLabels : new String[0];
        }

        public String[] getTerminalYLabels() {
            return terminalYLabels;
        }

        public void setTerminalYLabels(String[] terminalYLabels) {
            this.terminalYLabels = terminalYLabels != null ? terminalYLabels : new String[0];
        }

        public String[] getRawTerminalXLabels() {
            return rawTerminalXLabels.length > 0 ? rawTerminalXLabels : terminalXLabels;
        }

        public void setRawTerminalXLabels(String[] rawTerminalXLabels) {
            this.rawTerminalXLabels = rawTerminalXLabels != null ? rawTerminalXLabels : new String[0];
        }

        public String[] getRawTerminalYLabels() {
            return rawTerminalYLabels.length > 0 ? rawTerminalYLabels : terminalYLabels;
        }

        public void setRawTerminalYLabels(String[] rawTerminalYLabels) {
            this.rawTerminalYLabels = rawTerminalYLabels != null ? rawTerminalYLabels : new String[0];
        }

        public long getUniqueObjectIdentifier() {
            return uniqueObjectIdentifier;
        }

        public void setUniqueObjectIdentifier(long uniqueObjectIdentifier) {
            this.uniqueObjectIdentifier = uniqueObjectIdentifier;
        }

        /**
         * Unique object identifier of the coupled partner component, e.g. the
         * power component a CONTROL gate/voltmeter block is attached to
         * ({@code coupledReferenceID[]} in the .ipes file); 0 = not coupled.
         */
        public long getCoupledReferenceID() {
            return coupledReferenceID;
        }

        public void setCoupledReferenceID(long coupledReferenceID) {
            this.coupledReferenceID = coupledReferenceID;
        }

        public int getEnabledShorted() {
            return enabledShorted;
        }

        public void setEnabledShorted(int enabledShorted) {
            this.enabledShorted = enabledShorted;
        }

        public long getParentSheetIdentifier() {
            return parentSheetIdentifier;
        }

        public void setParentSheetIdentifier(long parentSheetIdentifier) {
            this.parentSheetIdentifier = parentSheetIdentifier;
        }

        public String getFamily() {
            return family;
        }

        public void setFamily(String family) {
            this.family = family != null ? family : "LK";
        }

        public List<String> getExtraLines() {
            return extraLines;
        }

        public void addExtraLine(String line) {
            if (line != null) {
                extraLines.add(line);
            }
        }

        @Override
        public String toString() {
            return "ComponentData{type=" + type + ", name='" + name + "', family='" + family + "'}";
        }

        /**
         * Returns the semantic parameter key for the primary value of a component type.
         */
        public static String resolveParameterKey(int type) {
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
    }

    /**
     * Represents a connection between components.
     */
    public static class ConnectionData {
        private final String type; // LK, CONTROL, THERMAL
        private int[][] points;
        private String label = "";
        private long uniqueObjectIdentifier;
        private int enabledShorted;
        private long parentSheetIdentifier;
        private int connectorType;

        public ConnectionData(String type, int[][] points) {
            this.type = type != null ? type : "LK";
            this.points = points != null ? points : new int[0][2];
        }

        public String getType() {
            return type;
        }

        public int[][] getPoints() {
            return points;
        }

        public void setPoints(int[][] points) {
            this.points = points != null ? points : new int[0][2];
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label != null ? label : "";
        }

        public long getUniqueObjectIdentifier() {
            return uniqueObjectIdentifier;
        }

        public void setUniqueObjectIdentifier(long uniqueObjectIdentifier) {
            this.uniqueObjectIdentifier = uniqueObjectIdentifier;
        }

        public int getEnabledShorted() {
            return enabledShorted;
        }

        public void setEnabledShorted(int enabledShorted) {
            this.enabledShorted = enabledShorted;
        }

        public long getParentSheetIdentifier() {
            return parentSheetIdentifier;
        }

        public void setParentSheetIdentifier(long parentSheetIdentifier) {
            this.parentSheetIdentifier = parentSheetIdentifier;
        }

        public int getConnectorType() {
            return connectorType;
        }

        public void setConnectorType(int connectorType) {
            this.connectorType = connectorType;
        }

        @Override
        public String toString() {
            return "ConnectionData{type='" + type + "', points=" + points.length + "}";
        }
    }
}
