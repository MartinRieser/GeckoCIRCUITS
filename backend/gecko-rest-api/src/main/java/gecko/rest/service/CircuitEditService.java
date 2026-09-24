package gecko.rest.service;

import gecko.core.circuit.ComponentTerminals;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;
import gecko.rest.model.circuit.CatalogResponse;
import gecko.rest.model.circuit.CircuitChangeMessage;
import gecko.rest.model.circuit.ComponentCreateRequest;
import gecko.rest.model.circuit.ComponentInfo;
import gecko.rest.model.circuit.ComponentPatchRequest;
import gecko.rest.model.circuit.ConnectionCreateRequest;
import gecko.rest.model.circuit.ConnectionPatchRequest;
import gecko.rest.model.circuit.EditorModelResponse;
import gecko.rest.model.circuit.NodeLabelRequest;
import gecko.rest.model.circuit.WireInfo;
import gecko.rest.service.CircuitFileService.CircuitState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Circuit editing operations (P1): component/wire CRUD, node labels,
 * undo/redo and change broadcasting. The circuit store itself lives in
 * {@link CircuitFileService}; this service owns the mutation logic.
 *
 * <p>Every successful mutation bumps the circuit's model version, records an
 * undo/redo pair on the bounded history stack and broadcasts a
 * {@link CircuitChangeMessage} on {@code /topic/circuits/{circuitId}}.
 * All mutating methods return that change message; its payload carries the
 * operation result (e.g. the created component as {@link ComponentInfo}).</p>
 */
@Service
public class CircuitEditService {

    private final CircuitFileService circuitFileService;

    // Optional: absent in @WebMvcTest slices without WebSocket infrastructure
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public CircuitEditService(CircuitFileService circuitFileService) {
        this.circuitFileService = circuitFileService;
    }

    // ========== Components ==========

    public CircuitChangeMessage createComponent(String circuitId, ComponentCreateRequest request) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            int reqType = (request.type() != null && request.type() == 6 && "CONTROL".equalsIgnoreCase(request.family()))
                    ? CircuitTypCore.CTRL_GATE.getTypeNumber()
                    : (request.type() != null ? request.type() : 0);
            String family = validateFamilyAndType(request.family(), reqType);
            CircuitModel model = state.model;

            CircuitModel.ComponentData comp = new CircuitModel.ComponentData(
                    reqType,
                    uniqueName(model, request.name(), baseName(reqType)),
                    snap(model, request.x()),
                    snap(model, request.y()),
                    normalizeOrientation(request.orientation()));
            comp.setFamily(family);
            comp.setUniqueObjectIdentifier(nextUid(model));
            applyDefaultParameters(comp);
            if (request.parameters() != null && !request.parameters().isEmpty()) {
                applyParameterMap(comp, request.parameters(), model);
            }

            List<CircuitModel.ComponentData> list = familyList(model, family);
            list.add(comp);

            state.recordEdit(() -> list.remove(comp), () -> list.add(comp));
            return change(state, circuitId, "createComponent", toInfo(comp, family));
        }
    }

    /**
     * Address of a wire point that was moved together with a component:
     * the owning connection and point index plus the pre-move coordinates,
     * so undo can restore and redo can re-apply the shift.
     */
    record WirePointRef(int connectionIndex, int pointIndex, int originalX, int originalY, int newX, int newY) {}

    /**
     * Patches position, orientation, name and/or parameters of a component.
     *
     * <p>When the component moves or rotates, wire points that sit exactly on one of its
     * terminals (see {@link ComponentTerminals#terminalsOf}) are shifted to the new
     * terminal positions.</p>
     */
    public CircuitChangeMessage patchComponent(String circuitId, String name, ComponentPatchRequest request) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitModel model = state.model;
            CircuitModel.ComponentData comp = findComponent(model, name);

            int[] beforePosition = comp.getPosition().clone();
            int beforeOrientation = comp.getOrientation();
            String beforeName = comp.getName();
            double[] beforeParams = comp.getRawParameters().clone();

            int newX = request.x() != null ? snap(model, request.x()) : beforePosition[0];
            int newY = request.y() != null ? snap(model, request.y()) : beforePosition[1];
            int newOrientation = request.orientation() != null
                    ? normalizeOrientation(request.orientation()) : beforeOrientation;
            String newName = request.newName() != null && !request.newName().isBlank()
                    ? request.newName() : beforeName;
            if (!newName.equals(beforeName)) {
                requireNameFree(model, newName);
            }

            int dx = newX - beforePosition[0];
            int dy = newY - beforePosition[1];
            boolean orientationChanged = newOrientation != beforeOrientation;
            List<WirePointRef> wireEdits = new ArrayList<>();

            if (dx != 0 || dy != 0 || orientationChanged) {
                List<int[]> oldTerminals = ComponentTerminals.terminalsOf(comp, beforePosition, beforeOrientation);
                List<int[]> newTerminals = ComponentTerminals.terminalsOf(comp, new int[]{newX, newY}, newOrientation);
                for (int t = 0; t < oldTerminals.size() && t < newTerminals.size(); t++) {
                    int[] oldT = oldTerminals.get(t);
                    int[] newT = newTerminals.get(t);
                    if (oldT[0] == newT[0] && oldT[1] == newT[1]) continue;

                    for (int wIdx = 0; wIdx < model.getConnections().size(); wIdx++) {
                        CircuitModel.ConnectionData conn = model.getConnections().get(wIdx);
                        int[][] pts = conn.getPoints();
                        if (pts == null) continue;
                        for (int pIdx = 0; pIdx < pts.length; pIdx++) {
                            if (pts[pIdx][0] == oldT[0] && pts[pIdx][1] == oldT[1]) {
                                wireEdits.add(new WirePointRef(wIdx, pIdx, oldT[0], oldT[1], newT[0], newT[1]));
                                pts[pIdx][0] = newT[0];
                                pts[pIdx][1] = newT[1];
                                break;
                            }
                        }
                    }
                }
            }

            comp.getPosition()[0] = newX;
            comp.getPosition()[1] = newY;
            comp.setOrientation(newOrientation);
            comp.setName(newName);
            if (request.parameters() != null && !request.parameters().isEmpty()) {
                applyParameterMap(comp, request.parameters(), model);
            }

            state.recordEdit(
                    () -> {
                        restoreComponent(comp, beforePosition, beforeOrientation, beforeName, beforeParams);
                        for (WirePointRef ref : wireEdits) {
                            if (ref.connectionIndex < model.getConnections().size()) {
                                int[][] pts = model.getConnections().get(ref.connectionIndex).getPoints();
                                if (pts != null && ref.pointIndex < pts.length) {
                                    pts[ref.pointIndex][0] = ref.originalX;
                                    pts[ref.pointIndex][1] = ref.originalY;
                                }
                            }
                        }
                    },
                    () -> {
                        comp.getPosition()[0] = newX;
                        comp.getPosition()[1] = newY;
                        comp.setOrientation(newOrientation);
                        comp.setName(newName);
                        if (request.parameters() != null && !request.parameters().isEmpty()) {
                            applyParameterMap(comp, request.parameters(), model);
                        }
                        for (WirePointRef ref : wireEdits) {
                            if (ref.connectionIndex < model.getConnections().size()) {
                                int[][] pts = model.getConnections().get(ref.connectionIndex).getPoints();
                                if (pts != null && ref.pointIndex < pts.length) {
                                    pts[ref.pointIndex][0] = ref.newX;
                                    pts[ref.pointIndex][1] = ref.newY;
                                }
                            }
                        }
                    });
            return change(state, circuitId, "patchComponent",
                    toInfo(comp, familyOf(model, comp)));
        }
    }

    public CircuitChangeMessage deleteComponent(String circuitId, String name) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitModel model = state.model;
            CircuitModel.ComponentData comp = findComponent(model, name);
            List<CircuitModel.ComponentData> list = containingList(model, comp);

            CircuitModel.ComponentData snapshot = copyComponent(comp);
            list.remove(comp);

            state.recordEdit(() -> list.add(snapshot), () -> list.remove(snapshot));
            return change(state, circuitId, "deleteComponent", Map.of("name", name));
        }
    }

    // ========== Connections (wires) ==========

    public CircuitChangeMessage createConnection(String circuitId, ConnectionCreateRequest request) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitModel model = state.model;
            String type = normalizeConnectionType(request.type());
            int[][] points = snapPoints(model, request.points(), true);

            CircuitModel.ConnectionData conn = new CircuitModel.ConnectionData(type, points);
            conn.setLabel(request.label() != null ? request.label() : "");
            model.getConnections().add(conn);

            state.recordEdit(
                    () -> model.getConnections().remove(conn),
                    () -> model.getConnections().add(conn));
            return change(state, circuitId, "createConnection",
                    toWireInfo(model.getConnections().size() - 1, conn));
        }
    }

    public CircuitChangeMessage patchConnection(String circuitId, int index, ConnectionPatchRequest request) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitModel model = state.model;
            CircuitModel.ConnectionData conn = connectionAt(model, index);

            int[][] beforePoints = conn.getPoints();
            String beforeLabel = conn.getLabel();
            int[][] newPoints = request.points() != null
                    ? snapPoints(model, request.points(), true) : beforePoints;
            String newLabel = request.label() != null ? request.label() : beforeLabel;

            conn.setPoints(newPoints);
            conn.setLabel(newLabel);

            state.recordEdit(
                    () -> {
                        conn.setPoints(beforePoints);
                        conn.setLabel(beforeLabel);
                    },
                    () -> {
                        conn.setPoints(newPoints);
                        conn.setLabel(newLabel);
                    });
            return change(state, circuitId, "patchConnection", toWireInfo(index, conn));
        }
    }

    public CircuitChangeMessage deleteConnection(String circuitId, int index) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitModel model = state.model;
            CircuitModel.ConnectionData conn = connectionAt(model, index);

            model.getConnections().remove(index);

            state.recordEdit(
                    () -> model.getConnections().add(Math.min(index, model.getConnections().size()), conn),
                    () -> model.getConnections().remove(conn));
            return change(state, circuitId, "deleteConnection", Map.of("index", index));
        }
    }

    // ========== Node labels ==========

    /**
     * Sets a node label on a component terminal. Terminals sharing a label
     * are electrically connected (label-based connectivity of the netlist).
     */
    public CircuitChangeMessage setNodeLabel(String circuitId, String componentName, NodeLabelRequest request) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitModel model = state.model;
            CircuitModel.ComponentData comp = findComponent(model, componentName);

            boolean inputSide = switch (request.side().toLowerCase()) {
                case "x", "input" -> true;
                case "y", "output" -> false;
                default -> throw badRequest("side must be 'x' (input) or 'y' (output)");
            };
            int index = request.terminalIndex();
            if (index < 0) {
                throw badRequest("terminalIndex must be >= 0");
            }
            String label = request.label() != null ? request.label() : "";

            String[] beforeRaw = sideArray(comp, inputSide, true).clone();
            String[] beforeSemantic = sideArray(comp, inputSide, false).clone();

            growAndSet(comp, inputSide, index, label);

            state.recordEdit(
                    () -> restoreSide(comp, inputSide, beforeRaw, beforeSemantic),
                    () -> growAndSet(comp, inputSide, index, label));
            return change(state, circuitId, "setNodeLabel", Map.of(
                    "component", componentName, "terminalIndex", index,
                    "side", inputSide ? "x" : "y", "label", label));
        }
    }

    // ========== Undo / Redo ==========

    public CircuitChangeMessage undo(String circuitId) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitState.Edit edit = state.pollUndo();
            if (edit == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nothing to undo");
            }
            edit.undo().run();
            return change(state, circuitId, "undo", Map.of());
        }
    }

    public CircuitChangeMessage redo(String circuitId) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitState.Edit edit = state.pollRedo();
            if (edit == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nothing to redo");
            }
            edit.redo().run();
            return change(state, circuitId, "redo", Map.of());
        }
    }

    // ========== Editor snapshot ==========

    /**
     * Full editor snapshot: components with terminal labels, wires with
     * indices, render scale and current version.
     */
    public EditorModelResponse getEditorModel(String circuitId) {
        CircuitState state = requireState(circuitId);
        synchronized (state) {
            CircuitModel model = state.model;

            List<EditorModelResponse.Component> components = new ArrayList<>();
            appendComponents(components, model.getCircuitComponents(), "LK");
            appendComponents(components, model.getControlComponents(), "CONTROL");
            appendComponents(components, model.getThermalComponents(), "THERM");
            appendComponents(components, model.getSpecialComponents(), "SPECIAL");

            List<EditorModelResponse.Wire> wires = new ArrayList<>();
            for (int i = 0; i < model.getConnections().size(); i++) {
                CircuitModel.ConnectionData conn = model.getConnections().get(i);
                wires.add(new EditorModelResponse.Wire(i, conn.getType(), conn.getLabel(), conn.getPoints()));
            }

            return new EditorModelResponse(circuitId, state.version.get(), state.filename,
                    model.getDisplayPixels(), model.getWorksheetSize(), components, wires,
                    simulationDefaults(model));
        }
    }

    /**
     * Sim panel pre-fill values from the file metadata; signals default to
     * the circuit's node labels when the file carries no dataContainerSignals.
     */
    private static EditorModelResponse.SimulationDefaults simulationDefaults(CircuitModel model) {
        List<String> signals = new ArrayList<>();
        String[] stored = model.getDataContainerSignals();
        if (stored != null) {
            for (String signal : stored) {
                if (signal != null && !signal.isBlank() && !signal.equals("[]")) {
                    signals.add(signal);
                }
            }
        }
        if (signals.isEmpty()) {
            LinkedHashSet<String> labels = new LinkedHashSet<>();
            for (CircuitModel.ComponentData comp : model.getAllComponents()) {
                collectLabel(labels, comp.getTerminalXLabels());
                collectLabel(labels, comp.getTerminalYLabels());
            }
            for (CircuitModel.ConnectionData conn : model.getConnections()) {
                collectLabel(labels, new String[] {conn.getLabel()});
            }
            signals.addAll(labels);
        }
        return new EditorModelResponse.SimulationDefaults(
                model.getTimeStep(), model.getSimulationDuration(),
                model.getSolverType().toString(), signals);
    }

    private static void collectLabel(Set<String> target, String[] labels) {
        for (String label : labels) {
            if (label == null) {
                continue;
            }
            String trimmed = label.trim();
            String lower = trimmed.toLowerCase();
            if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("NIX_NIX_NIX")
                    || lower.equals("0") || lower.equals("gnd") || lower.equals("ground")) {
                continue;
            }
            target.add(trimmed);
        }
    }

    private static void appendComponents(List<EditorModelResponse.Component> target,
                                         List<CircuitModel.ComponentData> source, String family) {
        for (CircuitModel.ComponentData comp : source) {
            Map<String, Object> params = new LinkedHashMap<>(comp.getParameters());
            String[] pStrings = comp.getParameterStrings();
            if (pStrings != null && pStrings.length > 0 && pStrings[0] != null) {
                String first = pStrings[0].trim();
                if (first.startsWith("/")) first = first.substring(1);
                if (!first.isEmpty() && !first.equalsIgnoreCase("NIX_NIX_NIX")) {
                    if (comp.getType() == 1 || comp.getType() == 1001 || (comp.getName() != null && comp.getName().startsWith("VOLT"))) {
                        String second = pStrings.length > 1 && pStrings[1] != null ? pStrings[1].trim() : "";
                        if (second.startsWith("/")) second = second.substring(1);
                        if (!second.isEmpty() && !second.equalsIgnoreCase("NIX_NIX_NIX")) {
                            params.put("nodeA", first);
                            params.put("nodeB", second);
                        } else {
                            params.put("coupledComponent", first);
                        }
                    } else {
                        params.put("coupledComponent", first);
                    }
                }
            }
            if (comp.getParameters().containsKey("nodeA")) {
                params.put("nodeA", comp.getParameters().get("nodeA"));
            }
            if (comp.getParameters().containsKey("nodeB")) {
                params.put("nodeB", comp.getParameters().get("nodeB"));
            }
            if (comp.getParameters().containsKey("coupledComponent")) {
                params.put("coupledComponent", comp.getParameters().get("coupledComponent"));
            }
            target.add(new EditorModelResponse.Component(
                    comp.getType(), comp.getName(), family, comp.getPosition(), comp.getOrientation(),
                    params, comp.getTerminalXLabels(), comp.getTerminalYLabels()));
        }
    }

    // ========== Catalog ==========

    private static volatile List<CatalogResponse.CatalogEntry> catalog;

    public CatalogResponse getCatalog() {
        if (catalog == null) {
            List<CatalogResponse.CatalogEntry> entries = new ArrayList<>();
            for (CircuitTypCore typ : CircuitTypCore.values()) {
                if (typ.isTerminal() || typ == CircuitTypCore.C_JAVA_FUNCTION) {
                    continue;
                }
                String family;
                if (typ.isThermal()) {
                    family = "THERM";
                } else if (typ.isControl()) {
                    family = "CONTROL";
                } else {
                    family = "LK";
                }
                entries.add(new CatalogResponse.CatalogEntry(
                        typ.getTypeNumber(), typ.name(), family));
            }
            catalog = entries;
        }
        return new CatalogResponse(catalog);
    }

    // ========== Helpers ==========

    private CircuitState requireState(String circuitId) {
        CircuitState state = circuitFileService.getState(circuitId);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit not found: " + circuitId);
        }
        return state;
    }

    private static String validateFamilyAndType(String family, Integer type) {
        if (family == null || type == null) {
            throw badRequest("family and type are required");
        }
        switch (family.toUpperCase()) {
            case "LK" -> {
                if (!CircuitTypCore.isValidTypeNumber(type)) {
                    throw badRequest("Unknown type number: " + type);
                }
                if (CircuitTypCore.fromTypeNumber(type).isThermal()) {
                    throw badRequest("Type " + CircuitTypCore.fromTypeNumber(type).name()
                            + " belongs to family THERM");
                }
                return "LK";
            }
            case "THERM" -> {
                if (!CircuitTypCore.isValidTypeNumber(type) || !CircuitTypCore.fromTypeNumber(type).isThermal()) {
                    throw badRequest("Unknown thermal type number: " + type);
                }
                return "THERM";
            }
            case "CONTROL" -> {
                int effType = switch (type) {
                    case 1 -> CircuitTypCore.CTRL_VOLT.getTypeNumber();
                    case 2 -> CircuitTypCore.CTRL_AMP.getTypeNumber();
                    case 3, 6 -> CircuitTypCore.CTRL_GATE.getTypeNumber();
                    case 5 -> CircuitTypCore.CTRL_SCOPE.getTypeNumber();
                    default -> type;
                };
                if (!CircuitTypCore.isValidTypeNumber(effType) || !CircuitTypCore.fromTypeNumber(effType).isControl()) {
                    throw badRequest("Unknown control type number: " + type);
                }
                return "CONTROL";
            }
            default -> throw badRequest(
                    "family must be LK, THERM or CONTROL");
        }
    }

    private static String normalizeConnectionType(String type) {
        if (type == null || type.isBlank()) {
            return "LK";
        }
        return switch (type.toUpperCase()) {
            case "LK" -> "LK";
            case "CONTROL" -> "CONTROL";
            case "THERM", "THERMAL" -> "THERMAL";
            default -> throw badRequest("type must be LK, CONTROL or THERM");
        };
    }

    private static String baseName(int type) {
        String name = CircuitTypCore.fromTypeNumber(type).name();
        for (String prefix : new String[] {"LK_", "TH_", "REL_", "CTRL_"}) {
            if (name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }

    private static String uniqueName(CircuitModel model, String requested, String base) {
        String candidate = requested != null && !requested.isBlank() ? requested : base;
        if (findComponentOrNull(model, candidate) == null) {
            return candidate;
        }
        if (candidate.equals(base)) {
            for (int suffix = 2; ; suffix++) {
                candidate = base + "_" + suffix;
                if (findComponentOrNull(model, candidate) == null) {
                    return candidate;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Component name already exists: " + requested);
    }

    private static void requireNameFree(CircuitModel model, String name) {
        if (findComponentOrNull(model, name) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Component name already exists: " + name);
        }
    }

    private static CircuitModel.ComponentData findComponentOrNull(CircuitModel model, String name) {
        for (CircuitModel.ComponentData comp : model.getAllComponents()) {
            if (comp.getName().equals(name)) {
                return comp;
            }
        }
        return null;
    }

    private static CircuitModel.ComponentData findComponent(CircuitModel model, String name) {
        CircuitModel.ComponentData comp = findComponentOrNull(model, name);
        if (comp == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found: " + name);
        }
        return comp;
    }

    private static List<CircuitModel.ComponentData> familyList(CircuitModel model, String family) {
        return switch (family) {
            case "LK" -> model.getCircuitComponents();
            case "THERM" -> model.getThermalComponents();
            case "CONTROL" -> model.getControlComponents();
            default -> model.getSpecialComponents();
        };
    }

    private static List<CircuitModel.ComponentData> containingList(CircuitModel model,
                                                                   CircuitModel.ComponentData comp) {
        for (List<CircuitModel.ComponentData> list : List.of(model.getCircuitComponents(),
                model.getControlComponents(), model.getThermalComponents(), model.getSpecialComponents())) {
            if (list.contains(comp)) {
                return list;
            }
        }
        throw new IllegalStateException("Component not in any family list: " + comp.getName());
    }

    private static String familyOf(CircuitModel model, CircuitModel.ComponentData comp) {
        if (model.getCircuitComponents().contains(comp)) {
            return "LK";
        }
        if (model.getControlComponents().contains(comp)) {
            return "CONTROL";
        }
        if (model.getThermalComponents().contains(comp)) {
            return "THERM";
        }
        return "SPECIAL";
    }

    private static CircuitModel.ConnectionData connectionAt(CircuitModel model, int index) {
        List<CircuitModel.ConnectionData> connections = model.getConnections();
        if (index < 0 || index >= connections.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No connection with index " + index + " (valid: 0.." + Math.max(0, connections.size() - 1) + ")");
        }
        return connections.get(index);
    }

    /**
     * Grid raster snapping: .ipes coordinates are integer grid units
     * (dpix is only the pixel render scale), so snapping = rounding.
     */
    private static int snap(CircuitModel model, int value) {
        return Math.round((float) value);
    }

    private static int[][] snapPoints(CircuitModel model, int[][] points, boolean requireTwo) {
        if (points == null || (requireTwo && points.length < 2)) {
            throw badRequest("A wire needs at least two [x, y] points");
        }
        int[][] snapped = new int[points.length][2];
        for (int i = 0; i < points.length; i++) {
            if (points[i] == null || points[i].length != 2) {
                throw badRequest("Each point must be [x, y]");
            }
            snapped[i][0] = snap(model, points[i][0]);
            snapped[i][1] = snap(model, points[i][1]);
        }
        return snapped;
    }

    /**
     * Orientation codes of the .ipes format (ComponentDirection old ordinals):
     * 501 = SOUTH_NORTH, 502 = WEST_EAST, 503 = NORTH_SOUTH (default),
     * 504 = EAST_WEST. Rotation cycles 503 -> 504 -> 501 -> 502 -> 503.
     */
    private static final int[] ORIENTATION_CYCLE = {503, 504, 501, 502};

    private static int normalizeOrientation(Integer orientation) {
        if (orientation == null) {
            return ORIENTATION_CYCLE[0];
        }
        for (int valid : ORIENTATION_CYCLE) {
            if (valid == orientation) {
                return orientation;
            }
        }
        throw badRequest("orientation must be one of 501 (SOUTH_NORTH), 502 (WEST_EAST), "
                + "503 (NORTH_SOUTH), 504 (EAST_WEST)");
    }

    /** Next orientation in the GUI rotation cycle (right-click / 'r' key). */
    static int rotateOrientation(int orientation) {
        for (int i = 0; i < ORIENTATION_CYCLE.length; i++) {
            if (ORIENTATION_CYCLE[i] == orientation) {
                return ORIENTATION_CYCLE[(i + 1) % ORIENTATION_CYCLE.length];
            }
        }
        return ORIENTATION_CYCLE[0];
    }

    /**
     * Default parameter arrays for new components, mirroring the classic GUI's
     * constructor defaults (see AbstractResistor, AbstractCapacitor, AbstractCircuitSource,
     * AbstractSemiconductor, AbstractSwitch). Sources carry the full classic layout:
     * [0]=source type code, [1]=DC value, [2]=frequency, [3]=offset, [4]=phase,
     * [20]=sinus amplitude. Switches/diodes: [0]=dynamic resistance (state),
     * then type-specific values.
     */
    private static final Map<Integer, double[]> DEFAULT_PARAMETERS = Map.ofEntries(
            Map.entry(1, new double[]{1000.0}),
            Map.entry(2, new double[]{3.0e-4, 0.0}),
            Map.entry(3, new double[]{100.0e-9, 0.0}),
            Map.entry(4, sourceDefaults(10.0, 325.0)),
            Map.entry(5, sourceDefaults(1.0, 1.0)),
            Map.entry(6, new double[]{10.0e-3, 0.6, 10.0e-3, 1.0e7}),
            Map.entry(7, new double[]{1.0e7, 10.0e-3, 1.0e7}),
            // Thermal & reluctance domain defaults follow CircuitTypCore numbering:
            // 41=TH_PvCHIP (Pv chip loss), 42=TH_MODUL (Rth/Cth), 44=TH_FLOW,
            // 45=TH_TEMP (temperature source), 46=TH_RTH (thermal resistance),
            // 47=TH_CTH (thermal capacitance), 48=TH_AMBIENT
            Map.entry(41, new double[]{10.0}),
            Map.entry(42, new double[]{1.0, 1.0}),
            Map.entry(44, new double[]{401.0, 10.0}),
            Map.entry(45, new double[]{401.0, 25.0}),
            Map.entry(46, new double[]{1.0}),
            Map.entry(47, new double[]{1.0, 25.0}),
            Map.entry(48, new double[]{401.0, 25.0}),
            // 9 = mutual coupling k; 23 = ideal transformer n1/n2/polarity;
            // 33 = BJT betaF/betaR/rBase/polarity (param0 unused)
            Map.entry(9, new double[]{0.98}),
            Map.entry(23, new double[]{10.0, 2.0, -1.0}),
            Map.entry(33, new double[]{0.0, 100.0, 60.0, 0.1, 1.0}),
            Map.entry(1000, new double[]{0.0}));

    private static double[] sourceDefaults(double dcValue, double amplitude) {
        double[] params = new double[21];
        params[0] = 401.0;   // QUELLE_DC
        params[1] = dcValue;
        params[2] = 50.0;    // frequency
        params[20] = amplitude;
        return params;
    }

    private static void applyDefaultParameters(CircuitModel.ComponentData comp) {
        if (comp.getType() == CircuitTypCore.CTRL_SCRIPT.getTypeNumber()
                || comp.getType() == CircuitTypCore.C_JAVA_FUNCTION.getTypeNumber()) {
            comp.setParameter("sourceCode", "yOUT[0] = xIN[0];");
            comp.setParameter("anzXIN", 1);
            comp.setParameter("anzYOUT", 1);
        }
        double[] defaults = DEFAULT_PARAMETERS.get(comp.getType());
        if (defaults == null) {
            return;
        }
        comp.setRawParameters(defaults.clone());
        for (int i = 0; i < defaults.length; i++) {
            comp.setParameter("param" + i, defaults[i]);
        }
        comp.setParameter(CircuitModel.ComponentData.resolveParameterKey(comp.getType()), defaults[0]);
    }

    private static void applyParameterMap(CircuitModel.ComponentData comp, Map<String, Object> parameters, CircuitModel model) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        int maxIndex = -1;
        for (String key : parameters.keySet()) {
            Integer idx = parameterIndex(key);
            if (idx != null) {
                maxIndex = Math.max(maxIndex, idx);
            } else if (!isNamedControlParameter(comp, key) && !"coupledComponent".equals(key)) {
                throw badRequest("Parameter keys must be 'param<index>', got: " + key);
            }
        }

        if (maxIndex >= 0) {
            double[] raw = Arrays.copyOf(comp.getRawParameters(),
                    Math.max(comp.getRawParameters().length, maxIndex + 1));
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                Integer idx = parameterIndex(entry.getKey());
                if (idx != null && entry.getValue() instanceof Number n) {
                    raw[idx] = n.doubleValue();
                }
            }
            int typ = comp.getType();
            // When initial condition (param1) is modified, synchronize/clear legacy operating-point cache slots
            // so stale state from previously loaded or paused runs never overrides the user's setting.
            if (parameters.containsKey("param1") && parameters.get("param1") instanceof Number n) {
                double val = n.doubleValue();
                if (typ == 3) { // Capacitor: raw[1]=uC0, raw[2]=i0, raw[3]=uC, raw[4]=pX, raw[5]=pY
                    if (raw.length > 2) raw[2] = 0.0;
                    if (raw.length > 3) raw[3] = val;
                    if (raw.length > 4) raw[4] = val;
                    if (raw.length > 5) raw[5] = 0.0;
                } else if (typ == 2) { // Inductor: raw[1]=iL0, raw[2]=i0
                    if (raw.length > 2) raw[2] = val;
                }
            }
            comp.setRawParameters(raw);
            // Keep the semantic primary alias (resistance/inductance/capacitance)
            // in sync with param0 so consumers of the named key never see a stale value.
            if (raw.length > 0 && (typ == 1 || typ == 2 || typ == 3)) {
                comp.setParameter(CircuitModel.ComponentData.resolveParameterKey(typ), raw[0]);
            }
        }
        parameters.forEach(comp::setParameter);
        if (parameters.containsKey("nodeA") || parameters.containsKey("nodeB")
                || parameters.containsKey("positiveNode") || parameters.containsKey("negativeNode")) {
            String nA = parameters.containsKey("nodeA")
                    ? String.valueOf(parameters.get("nodeA"))
                    : String.valueOf(parameters.getOrDefault("positiveNode", ""));
            String nB = parameters.containsKey("nodeB")
                    ? String.valueOf(parameters.get("nodeB"))
                    : String.valueOf(parameters.getOrDefault("negativeNode", "0"));
            if (nA == null || nA.equals("null")) nA = "";
            if (nB == null || nB.equals("null")) nB = "0";
            comp.setParameter("nodeA", nA);
            comp.setParameter("nodeB", nB);
            comp.setParameterStrings(new String[]{nA, nB, "0"});
            comp.setCoupledReferenceID(0);
            comp.setParameter("coupledComponent", "");
        } else if (parameters.containsKey("coupledComponent") || parameters.containsKey("measuredComponent")) {
            Object val = parameters.containsKey("coupledComponent")
                    ? parameters.get("coupledComponent")
                    : parameters.get("measuredComponent");
            String coupled = val == null ? "" : val.toString().trim();
            if (coupled.isEmpty() || coupled.equalsIgnoreCase("none") || coupled.equalsIgnoreCase("NIX_NIX_NIX")) {
                comp.setParameter("coupledComponent", "");
                comp.setCoupledReferenceID(0);
                if (comp.getParameterStrings() != null && comp.getParameterStrings().length > 0) {
                    comp.getParameterStrings()[0] = "NIX_NIX_NIX";
                }
            } else {
                comp.setParameter("coupledComponent", coupled);
                comp.setParameter("nodeA", "");
                comp.setParameter("nodeB", "");
                String[] pStrings = comp.getParameterStrings();
                if (pStrings == null || pStrings.length == 0) {
                    comp.setParameterStrings(new String[]{coupled, "NIX_NIX_NIX", "0"});
                } else {
                    pStrings[0] = coupled;
                    if (pStrings.length > 1) {
                        pStrings[1] = "NIX_NIX_NIX";
                    }
                }
                if (model != null) {
                    CircuitModel.ComponentData target = findComponentOrNull(model, coupled);
                    if (target != null) {
                        comp.setCoupledReferenceID(target.getUniqueObjectIdentifier());
                    } else {
                        comp.setCoupledReferenceID(0);
                    }
                }
            }
        }
    }

    private static boolean isNamedControlParameter(CircuitModel.ComponentData comp, String key) {
        return "CONTROL".equals(comp.getFamily()) && (
                "sourceCode".equals(key) ||
                "staticCode".equals(key) ||
                "staticVariables".equals(key) ||
                "anzXIN".equals(key) ||
                "anzYOUT".equals(key) ||
                "nodeA".equals(key) ||
                "nodeB".equals(key) ||
                "positiveNode".equals(key) ||
                "negativeNode".equals(key) ||
                "measuredComponent".equals(key)
        );
    }

    private static Integer parameterIndex(String key) {
        if (!key.startsWith("param")) {
            return null;
        }
        try {
            return Integer.parseInt(key.substring("param".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long nextUid(CircuitModel model) {
        long max = 0;
        for (CircuitModel.ComponentData comp : model.getAllComponents()) {
            max = Math.max(max, comp.getUniqueObjectIdentifier());
        }
        for (CircuitModel.ConnectionData conn : model.getConnections()) {
            max = Math.max(max, conn.getUniqueObjectIdentifier());
        }
        return max + 1;
    }

    private static void restoreComponent(CircuitModel.ComponentData comp, int[] position, int orientation,
                                         String name, double[] params) {
        comp.getPosition()[0] = position[0];
        comp.getPosition()[1] = position[1];
        comp.setOrientation(orientation);
        comp.setName(name);
        comp.setRawParameters(params);
    }

    /** Raw side arrays when raw is non-empty, otherwise the semantic arrays. */
    private static String[] sideArray(CircuitModel.ComponentData comp, boolean inputSide, boolean raw) {
        if (raw) {
            return inputSide ? comp.getRawTerminalXLabels() : comp.getRawTerminalYLabels();
        }
        return inputSide ? comp.getTerminalXLabels() : comp.getTerminalYLabels();
    }

    /**
     * Grows the terminal label arrays of the given side to {@code index + 1}
     * entries and sets the label.
     */
    private static void growAndSet(CircuitModel.ComponentData comp, boolean inputSide, int index, String label) {
        String[] raw = grown(sideArray(comp, inputSide, true), index, label);
        String[] semantic = grown(sideArray(comp, inputSide, false), index, label);
        if (inputSide) {
            comp.setRawTerminalXLabels(raw);
            comp.setTerminalXLabels(semantic);
        } else {
            comp.setRawTerminalYLabels(raw);
            comp.setTerminalYLabels(semantic);
        }
    }

    private static void restoreSide(CircuitModel.ComponentData comp, boolean inputSide,
                                    String[] raw, String[] semantic) {
        if (inputSide) {
            comp.setRawTerminalXLabels(raw);
            comp.setTerminalXLabels(semantic);
        } else {
            comp.setRawTerminalYLabels(raw);
            comp.setTerminalYLabels(semantic);
        }
    }

    private static String[] grown(String[] array, int index, String label) {
        String[] result = Arrays.copyOf(array, Math.max(array.length, index + 1));
        result[index] = label;
        return result;
    }

    private static CircuitModel.ComponentData copyComponent(CircuitModel.ComponentData source) {
        CircuitModel.ComponentData copy = new CircuitModel.ComponentData(
                source.getType(), source.getName(), source.getPosition()[0], source.getPosition()[1],
                source.getOrientation());
        copy.setTerminalXLabels(source.getTerminalXLabels().clone());
        copy.setTerminalYLabels(source.getTerminalYLabels().clone());
        copy.setRawTerminalXLabels(source.getRawTerminalXLabels().clone());
        copy.setRawTerminalYLabels(source.getRawTerminalYLabels().clone());
        copy.setRawParameters(source.getRawParameters().clone());
        copy.setParameterStrings(source.getParameterStrings().clone());
        copy.setNameOpt(source.getNameOpt().clone());
        copy.setUniqueObjectIdentifier(source.getUniqueObjectIdentifier());
        copy.setEnabledShorted(source.getEnabledShorted());
        copy.setParentSheetIdentifier(source.getParentSheetIdentifier());
        copy.setFamily(source.getFamily());
        source.getParameters().forEach(copy::setParameter);
        source.getExtraLines().forEach(copy::addExtraLine);
        return copy;
    }

    private static ComponentInfo toInfo(CircuitModel.ComponentData comp, String family) {
        return new ComponentInfo(comp.getType(), comp.getName(), family,
                comp.getPosition(), comp.getOrientation(), comp.getParameters());
    }

    private static WireInfo toWireInfo(int index, CircuitModel.ConnectionData conn) {
        return new WireInfo(index, conn.getType(), conn.getLabel(), conn.getPoints());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    // ========== Version bump + broadcast ==========

    private CircuitChangeMessage change(CircuitState state, String circuitId, String operation, Object payload) {
        CircuitChangeMessage message = new CircuitChangeMessage(
                circuitId, state.version.incrementAndGet(), operation, payload);
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/circuits/" + circuitId, message);
            } catch (Exception ignored) {
                // a failed broadcast must never break the edit itself
            }
        }
        return message;
    }
}
