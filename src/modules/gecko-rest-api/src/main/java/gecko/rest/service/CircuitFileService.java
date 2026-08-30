package gecko.rest.service;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.ParameterOverrideApplicator;
import gecko.rest.model.circuit.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service for loading and parsing .ipes circuit files.
 * Uses CircuitFileParser and CircuitModel from gecko-simulation-core.
 */
@Service
public class CircuitFileService {

    private final CircuitFileParser parser = new CircuitFileParser();

    // In-memory storage of parsed circuits (circuit ID -> parsed data)
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();

    /**
     * Load circuit from multipart file upload.
     */
    public CircuitLoadResponse loadCircuit(MultipartFile file) {
        try {
            // Read file content
            byte[] content = file.getBytes();
            String filename = file.getOriginalFilename();

            return loadCircuitFromBytes(content, filename);
        } catch (IOException e) {
            return CircuitLoadResponse.failure(file.getOriginalFilename(),
                    "Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Load circuit from base64 encoded content.
     */
    public CircuitLoadResponse loadCircuit(String base64Content, String filename) {
        try {
            // Decode base64
            byte[] content = Base64.getDecoder().decode(base64Content);
            return loadCircuitFromBytes(content, filename);
        } catch (IllegalArgumentException e) {
            return CircuitLoadResponse.failure(filename,
                    "Invalid base64 encoding: " + e.getMessage());
        }
    }

    /**
     * Get detailed circuit information.
     */
    public CircuitInfo getCircuitInfo(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }

        CircuitModel model = parsed.model;

        // Build simulation parameters
        CircuitInfo.SimulationParameters simParams = new CircuitInfo.SimulationParameters(
            model.getSimulationDuration(),
            model.getTimeStep(),
            solverTypeToString(model.getSolverType()),
            model.getPreSimulationTime(),
            model.getPreSimulationTimeStep()
        );

        // Build component counts
        CircuitInfo.ComponentCounts counts = new CircuitInfo.ComponentCounts(
            model.getCircuitComponents().size(),
            model.getControlComponents().size(),
            model.getThermalComponents().size(),
            model.getConnections().size()
        );

        // Build display settings
        CircuitInfo.DisplaySettings displaySettings = new CircuitInfo.DisplaySettings(
            model.getWindowWidth() > 0 ? model.getWindowWidth() : null,
            model.getWindowHeight() > 0 ? model.getWindowHeight() : null,
            model.getFontSize()
        );

        // Build metadata
        CircuitInfo.Metadata metadata = new CircuitInfo.Metadata(
            model.getCreationDate(),
            model.getUniqueFileId()
        );

        return new CircuitInfo(
            circuitId,
            parsed.filename,
            model.getFileVersion(),
            simParams,
            counts,
            displaySettings,
            metadata
        );
    }

    /**
     * Get component list for a circuit.
     */
    public ComponentListResponse getComponents(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }

        CircuitModel model = parsed.model;
        List<ComponentInfo> components = new ArrayList<>();

        // Add circuit components
        for (CircuitModel.ComponentData comp : model.getCircuitComponents()) {
            components.add(componentDataToInfo(comp, "circuit"));
        }

        // Add control components
        for (CircuitModel.ComponentData comp : model.getControlComponents()) {
            components.add(componentDataToInfo(comp, "control"));
        }

        // Add thermal components
        for (CircuitModel.ComponentData comp : model.getThermalComponents()) {
            components.add(componentDataToInfo(comp, "thermal"));
        }

        return new ComponentListResponse(circuitId, components);
    }

    /**
     * Validate a circuit.
     */
    public ValidationResponse validateCircuit(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }

        CircuitModel model = parsed.model;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check simulation parameters
        if (!model.hasValidSimulationParameters()) {
            errors.add("Invalid simulation parameters: time step must be positive and less than duration");
        }

        if (model.getTotalComponentCount() == 0) {
            warnings.add("Circuit contains no components");
        }

        if (errors.isEmpty()) {
            return warnings.isEmpty()
                ? ValidationResponse.success()
                : ValidationResponse.successWithWarnings(warnings);
        } else {
            return ValidationResponse.failure(warnings, errors);
        }
    }

    /**
     * Get the parsed model of a loaded circuit.
     *
     * @return model, or null if the circuit ID is unknown
     */
    public CircuitModel getModel(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        return parsed != null ? parsed.model : null;
    }

    /**
     * Serialize a loaded circuit back to gzip-compressed .ipes bytes
     * using {@link CircuitFileWriter}.
     *
     * @throws ResponseStatusException 404 if circuit not found
     */
    public byte[] getIpesBytes(String circuitId) {
        CircuitState parsed = requireCircuit(circuitId);
        try {
            return CircuitFileWriter.write(parsed.model);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize circuit: " + e.getMessage(), e);
        }
    }

    /**
     * Replace the content of a loaded circuit under the same circuit ID.
     * Used by the editor save flow; keeps the ID stable for clients.
     *
     * @param content new .ipes file content (gzip or plain ASCII)
     * @param filename new filename, or null to keep the current one
     * @return circuit info of the replaced circuit
     * @throws ResponseStatusException 404 if circuit not found, 400 if content is unparseable
     */
    public CircuitInfo replaceCircuit(String circuitId, byte[] content, String filename) {
        CircuitState parsed = requireCircuit(circuitId);

        CircuitModel newModel;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            newModel = parser.parse(bais, filename != null ? filename : parsed.filename);
        } catch (CircuitFileParser.CircuitParseException | IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to parse circuit content: " + e.getMessage(), e);
        }

        CircuitState replaced = new CircuitState(
                filename != null ? filename : parsed.filename,
                newModel,
                Instant.now());
        replaced.originalContent = content;
        circuits.put(circuitId, replaced);

        return getCircuitInfo(circuitId);
    }

    /**
     * Faithful source bytes of the circuit's last load/save, or null when the
     * circuit was created by a clone (its model was round-tripped anyway).
     * The legacy backend runs on these bytes because the classic GUI cannot
     * reliably open CircuitFileWriter rewrites.
     */
    public byte[] getOriginalBytes(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        return parsed != null ? parsed.originalContent : null;
    }

    /**
     * Get raw circuit file content (decompressed ASCII, re-serialized from the model).
     */
    public String getRawCircuit(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }
        return CircuitFileWriter.writeToString(parsed.model);
    }

    /**
     * Delete circuit from memory.
     */
    public boolean deleteCircuit(String circuitId) {
        return circuits.remove(circuitId) != null;
    }

    /**
     * Get all loaded circuits.
     */
    public CircuitListResponse getAllCircuits() {
        List<CircuitListResponse.CircuitSummary> summaries = circuits.entrySet().stream()
            .map(entry -> {
                String id = entry.getKey();
                CircuitState parsed = entry.getValue();
                return new CircuitListResponse.CircuitSummary(
                    id,
                    parsed.filename,
                    parsed.model.getTotalComponentCount(),
                    parsed.loadedAt.toString()
                );
            })
            .collect(Collectors.toList());

        return new CircuitListResponse(summaries, summaries.size());
    }

    /**
     * Clone an existing circuit with optional parameter overrides.
     * Creates a new independent copy that can be modified without affecting the original.
     *
     * @param circuitId the source circuit ID
     * @param overrides optional parameter overrides (dot-notation ComponentName.parameterKey)
     * @return response containing new circuit ID and metadata
     * @throws ResponseStatusException 404 if circuit not found
     */
    public CircuitLoadResponse cloneCircuit(String circuitId, Map<String, Double> overrides) {
        CircuitState sourceParsed = circuits.get(circuitId);
        if (sourceParsed == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Circuit not found: " + circuitId
            );
        }

        try {
            // Deep copy via .ipes round-trip: guarantees the clone is a complete,
            // independent copy of every stored field (components, wires, labels, extras).
            CircuitModel newModel = copyCircuitModel(sourceParsed.model);

            // Apply parameter overrides if provided
            if (overrides != null && !overrides.isEmpty()) {
                ParameterOverrideApplicator.applyOverrides(newModel, overrides);
            }

            // Generate unique circuit ID
            String newCircuitId = UUID.randomUUID().toString();

            // Create parsed circuit with timestamp
            CircuitState newParsed = new CircuitState(
                sourceParsed.filename,
                newModel,
                Instant.now()
            );

            // Store in memory
            circuits.put(newCircuitId, newParsed);

            return CircuitLoadResponse.success(newCircuitId, sourceParsed.filename, newModel.getTotalComponentCount());

        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to clone circuit: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Update simulation parameters of a loaded circuit.
     * Only provided parameters are updated; null values are ignored.
     *
     * @param circuitId the circuit ID to update
     * @param update parameter update request
     * @return updated circuit info
     * @throws ResponseStatusException 404 if circuit not found
     */
    public CircuitInfo updateCircuitParameters(String circuitId, CircuitParameterUpdate update) {
        CircuitState parsed = circuits.get(circuitId);
        if (parsed == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Circuit not found: " + circuitId
            );
        }

        CircuitModel model = parsed.model;

        // Update simulation duration if provided
        if (update.getSimulationDuration() != null) {
            model.setSimulationDuration(update.getSimulationDuration());
        }

        // Update time step if provided
        if (update.getTimeStep() != null) {
            model.setTimeStep(update.getTimeStep());
        }

        // Update solver type if provided
        if (update.getSolverType() != null) {
            gecko.core.allg.SolverType solverType = stringSolverType(update.getSolverType());
            model.setSolverType(solverType);
        }

        return getCircuitInfo(circuitId);
    }

    // ========== Private Helper Methods ==========

    private CircuitLoadResponse loadCircuitFromBytes(byte[] content, String filename) {
        try {
            // Parse using CircuitFileParser
            CircuitModel model;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                model = parser.parse(bais, filename);
            }

            // Generate unique circuit ID
            String circuitId = UUID.randomUUID().toString();

            // Create parsed circuit with timestamp
            CircuitState parsed = new CircuitState(
                filename,
                model,
                Instant.now()
            );
            parsed.originalContent = content;

            // Store in memory
            circuits.put(circuitId, parsed);

            return CircuitLoadResponse.success(circuitId, filename, model.getTotalComponentCount());

        } catch (CircuitFileParser.CircuitParseException e) {
            return CircuitLoadResponse.failure(filename,
                    "Failed to parse circuit: " + e.getMessage());
        } catch (IOException e) {
            return CircuitLoadResponse.failure(filename,
                    "Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            return CircuitLoadResponse.failure(filename,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Creates a deep copy of a CircuitModel by serializing to .ipes bytes and
     * parsing them back — the same code path used for file save/load, so no
     * field can be forgotten.
     */
    private CircuitModel copyCircuitModel(CircuitModel source) {
        try {
            byte[] bytes = CircuitFileWriter.write(source);
            return parser.parse(new ByteArrayInputStream(bytes), source.getFilePath());
        } catch (IOException | CircuitFileParser.CircuitParseException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to copy circuit: " + e.getMessage(), e);
        }
    }

    private CircuitState requireCircuit(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        if (parsed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit not found: " + circuitId);
        }
        return parsed;
    }

    /**
     * Package-private state access for {@link CircuitEditService} (same package,
     * no extra abstraction layer between the two services).
     */
    CircuitState getState(String circuitId) {
        return circuits.get(circuitId);
    }

    CircuitState requireState(String circuitId) {
        return requireCircuit(circuitId);
    }

    private ComponentInfo componentDataToInfo(CircuitModel.ComponentData comp, String domain) {
        return new ComponentInfo(
            comp.getType(),
            comp.getName(),
            domain,
            comp.getPosition(),
            comp.getOrientation(),
            comp.getParameters()
        );
    }

    private String solverTypeToString(gecko.core.allg.SolverType solverType) {
        return switch (solverType) {
            case SOLVER_BE -> "backward-euler";
            case SOLVER_TRZ -> "trapezoidal";
            case SOLVER_GS -> "gear-shichman";
        };
    }

    private gecko.core.allg.SolverType stringSolverType(String solverType) {
        if (solverType == null) {
            return gecko.core.allg.SolverType.SOLVER_BE;
        }

        return switch (solverType.toLowerCase()) {
            case "trapezoidal", "trz" -> gecko.core.allg.SolverType.SOLVER_TRZ;
            case "gear-shichman", "gs" -> gecko.core.allg.SolverType.SOLVER_GS;
            default -> gecko.core.allg.SolverType.SOLVER_BE;
        };
    }

    // ========== Internal Data Structure ==========

    /**
     * Stored circuit: the model plus editor bookkeeping (change version and
     * bounded undo/redo history, managed by {@link CircuitEditService}).
     * A fresh state (version 0, empty history) is created on load and replace.
     */
    static final class CircuitState {
        record Edit(Runnable undo, Runnable redo) {}

        private static final int MAX_HISTORY = 200;

        String filename;
        CircuitModel model;
        // Faithful source bytes of the last load/save. The classic GUI cannot
        // reliably open CircuitFileWriter rewrites (scope <detail> blocks are
        // not round-tripped), so the legacy backend runs on these bytes.
        byte[] originalContent;
        final Instant loadedAt;
        final AtomicLong version = new AtomicLong();
        private final Deque<Edit> undoStack = new ArrayDeque<>();
        private final Deque<Edit> redoStack = new ArrayDeque<>();

        CircuitState(String filename, CircuitModel model, Instant loadedAt) {
            this.filename = filename;
            this.model = model;
            this.loadedAt = loadedAt;
        }

        /**
         * Records a just-applied edit (undo/redo closures over the live model).
         * Discards the redo history, as usual for command stacks.
         */
        synchronized void recordEdit(Runnable undo, Runnable redo) {
            undoStack.push(new Edit(undo, redo));
            if (undoStack.size() > MAX_HISTORY) {
                undoStack.removeLast();
            }
            redoStack.clear();
        }

        /** Pops the next edit to undo (moves it to the redo stack), or null. */
        synchronized Edit pollUndo() {
            Edit edit = undoStack.poll();
            if (edit != null) {
                redoStack.push(edit);
            }
            return edit;
        }

        /** Pops the next edit to redo (moves it back to the undo stack), or null. */
        synchronized Edit pollRedo() {
            Edit edit = redoStack.poll();
            if (edit != null) {
                undoStack.push(edit);
            }
            return edit;
        }

        synchronized boolean canUndo() {
            return !undoStack.isEmpty();
        }

        synchronized boolean canRedo() {
            return !redoStack.isEmpty();
        }
    }
}
