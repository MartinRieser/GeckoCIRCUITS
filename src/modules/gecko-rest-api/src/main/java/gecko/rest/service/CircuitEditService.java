package gecko.rest.service;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;
import gecko.rest.model.circuit.CatalogResponse;
import gecko.rest.model.circuit.CircuitChangeMessage;
import gecko.rest.model.circuit.ComponentCreateRequest;
import gecko.rest.model.circuit.ComponentInfo;
import gecko.rest.model.circuit.ComponentPatchRequest;
import gecko.rest.model.circuit.ConnectionCreateRequest;
import gecko.rest.model.circuit.ConnectionPatchRequest;
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
import java.util.List;
import java.util.Map;

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
            String family = validateFamilyAndType(request.family(), request.type());
            CircuitModel model = state.model;

            CircuitModel.ComponentData comp = new CircuitModel.ComponentData(
                    request.type(),
                    uniqueName(model, request.name(), baseName(request.type())),
                    snap(model, request.x()),
                    snap(model, request.y()),
                    normalizeOrientation(request.orientation()));
            comp.setFamily(family);
            comp.setUniqueObjectIdentifier(nextUid(model));
            if (request.parameters() != null && !request.parameters().isEmpty()) {
                applyParameterMap(comp, request.parameters());
            }

            List<CircuitModel.ComponentData> list = familyList(model, family);
            list.add(comp);

            state.recordEdit(() -> list.remove(comp), () -> list.add(comp));
            return change(state, circuitId, "createComponent", toInfo(comp, family));
        }
    }

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

            comp.getPosition()[0] = newX;
            comp.getPosition()[1] = newY;
            comp.setOrientation(newOrientation);
            comp.setName(newName);
            if (request.parameters() != null && !request.parameters().isEmpty()) {
                applyParameterMap(comp, request.parameters());
            }

            state.recordEdit(
                    () -> restoreComponent(comp, beforePosition, beforeOrientation, beforeName, beforeParams),
                    () -> {
                        comp.getPosition()[0] = newX;
                        comp.getPosition()[1] = newY;
                        comp.setOrientation(newOrientation);
                        comp.setName(newName);
                        if (request.parameters() != null && !request.parameters().isEmpty()) {
                            applyParameterMap(comp, request.parameters());
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

    // ========== Catalog ==========

    private static volatile List<CatalogResponse.CatalogEntry> catalog;

    public CatalogResponse getCatalog() {
        if (catalog == null) {
            List<CatalogResponse.CatalogEntry> entries = new ArrayList<>();
            for (CircuitTypCore typ : CircuitTypCore.values()) {
                if (typ.isTerminal()) {
                    continue;
                }
                entries.add(new CatalogResponse.CatalogEntry(
                        typ.getTypeNumber(), typ.name(), typ.isThermal() ? "THERM" : "LK"));
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
            default -> throw badRequest(
                    "family must be LK or THERM (CONTROL/SPECIAL creation is not supported yet)");
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
        for (String prefix : new String[] {"LK_", "TH_", "REL_"}) {
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

    private static int snap(CircuitModel model, int value) {
        int raster = model.getDisplayPixels();
        if (raster <= 1) {
            return value;
        }
        return Math.round((float) value / raster) * raster;
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

    private static int normalizeOrientation(Integer orientation) {
        int value = orientation != null ? orientation : 0;
        if (value < 0 || value > 3) {
            throw badRequest("orientation must be 0-3");
        }
        return value;
    }

    private static void applyParameterMap(CircuitModel.ComponentData comp, Map<String, Double> parameters) {
        int maxIndex = -1;
        for (String key : parameters.keySet()) {
            Integer idx = parameterIndex(key);
            if (idx == null) {
                throw badRequest("Parameter keys must be 'param<index>', got: " + key);
            }
            maxIndex = Math.max(maxIndex, idx);
        }

        double[] raw = Arrays.copyOf(comp.getRawParameters(),
                Math.max(comp.getRawParameters().length, maxIndex + 1));
        for (Map.Entry<String, Double> entry : parameters.entrySet()) {
            if (entry.getValue() == null) {
                throw badRequest("Parameter value must not be null: " + entry.getKey());
            }
            raw[parameterIndex(entry.getKey())] = entry.getValue();
        }
        comp.setRawParameters(raw);
        parameters.forEach(comp::setParameter);
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
