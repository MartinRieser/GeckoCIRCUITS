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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for loading and parsing .ipes circuit files.
 * Uses CircuitFileParser and CircuitModel from gecko-simulation-core.
 */
@Service
public class CircuitFileService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CircuitFileService.class);
    private static final int MAX_STORED_CIRCUITS = 25;

    private final CircuitFileParser parser = new CircuitFileParser();

    // In-memory storage of parsed circuits (circuit ID -> parsed data)
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();

    private void evictOldCircuitsIfNeeded() {
        if (circuits.size() < MAX_STORED_CIRCUITS) {
            return;
        }
        circuits.entrySet().stream()
                .min(Comparator.comparing((Map.Entry<String, CircuitState> e) ->
                        e.getValue().loadedAt != null ? e.getValue().loadedAt : Instant.EPOCH))
                .ifPresent(oldest -> {
                    CircuitState removed = circuits.remove(oldest.getKey());
                    cleanupNativeLibraryDir(removed != null ? removed.model : null);
                    logger.info("Evicted old circuit {} ({}) from memory", oldest.getKey(), oldest.getValue().filename);
                });
    }

    /**
     * Load circuit from multipart file upload.
     */
    public CircuitLoadResponse loadCircuit(MultipartFile file) {
        try {
            // Read file content
            byte[] content = file.getBytes();
            String filename = file.getOriginalFilename();

            return loadCircuitFromBytes(content, filename, null);
        } catch (IOException e) {
            return CircuitLoadResponse.failure(file.getOriginalFilename(),
                    "Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Load circuit from base64 encoded content.
     */
    public CircuitLoadResponse loadCircuit(String base64Content, String filename) {
        return loadCircuit(base64Content, filename, null);
    }

    /**
     * Load circuit from base64 encoded content with optional full disk filePath.
     */
    public CircuitLoadResponse loadCircuit(String base64Content, String filename, String filePath) {
        try {
            // Decode base64
            byte[] content = Base64.getDecoder().decode(base64Content);
            return loadCircuitFromBytes(content, filename, filePath);
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
     * Returns an isolated deep copy of a loaded circuit model (safe for concurrent
     * simulations with parameter overrides without mutating the active editor model).
     *
     * @param circuitId active circuit session ID
     * @return deep-copied CircuitModel, or null if not found
     */
    public CircuitModel getModelCopy(String circuitId) {
        CircuitState parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }
        return copyCircuitModel(parsed.model);
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
            return CircuitFileWriter.write(parsed.model, true);
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

        String effectiveFilename = filename != null ? filename : parsed.filename;
        String sourceName = effectiveFilename;
        if (parsed.model != null && parsed.model.getFilePath() != null && !parsed.model.getFilePath().isBlank()) {
            String prevPath = parsed.model.getFilePath();
            if (filename == null || filename.equals(parsed.filename) || prevPath.endsWith("/" + effectiveFilename) || prevPath.endsWith("\\" + effectiveFilename)) {
                sourceName = prevPath;
            }
        }

        CircuitModel newModel;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            newModel = parser.parse(bais, sourceName);
        } catch (CircuitFileParser.CircuitParseException | IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to parse circuit content: " + e.getMessage(), e);
        }

        if (parsed.model != null && parsed.model.getNativeLibraryDir() != null) {
            newModel.setNativeLibraryDir(parsed.model.getNativeLibraryDir());
        }

        CircuitState replaced = new CircuitState(
                effectiveFilename,
                newModel,
                Instant.now());
        circuits.put(circuitId, replaced);

        return getCircuitInfo(circuitId);
    }

    /**
     * Stages an uploaded NativeC shared library (.dll / .so / .dylib) into the circuit's
     * per-session staging directory so browser-opened circuits without filesystem paths can run it.
     */
    public Map<String, Object> uploadNativeLibrary(String circuitId, String filename, String base64Content) {
        CircuitState parsed = requireCircuit(circuitId);
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Native library filename must not be empty");
        }
        String rawName = filename.replace('\\', '/');
        String baseName = rawName.substring(rawName.lastIndexOf('/') + 1).trim();
        if (baseName.isEmpty() || baseName.equals(".") || baseName.equals("..") || baseName.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid native library filename: " + filename);
        }
        String lower = baseName.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".dll") || lower.endsWith(".so") || lower.endsWith(".dylib"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported shared library extension (expected .dll, .so, or .dylib): " + baseName);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid base64 encoding for native library: " + e.getMessage());
        }

        try {
            Path stagingDir;
            String existingDir = parsed.model.getNativeLibraryDir();
            if (existingDir != null && !existingDir.isBlank()) {
                stagingDir = Path.of(existingDir);
                Files.createDirectories(stagingDir);
            } else {
                stagingDir = Files.createTempDirectory("gecko-native-libs-");
                stagingDir.toFile().deleteOnExit();
                parsed.model.setNativeLibraryDir(stagingDir.toString());
            }
            Path target = stagingDir.resolve(baseName).normalize();
            if (!target.startsWith(stagingDir.normalize())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target path for library");
            }
            Files.write(target, bytes);
            logger.info("Staged NativeC library '{}' ({} bytes) for circuit {}", baseName, bytes.length, circuitId);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("status", "uploaded");
            res.put("circuitId", circuitId);
            res.put("filename", baseName);
            res.put("sizeBytes", bytes.length);
            return res;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to stage native library: " + e.getMessage(), e);
        }
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
        CircuitState removed = circuits.remove(circuitId);
        if (removed == null) {
            return false;
        }
        cleanupNativeLibraryDir(removed.model);
        return true;
    }

    private static void cleanupNativeLibraryDir(CircuitModel model) {
        if (model == null || model.getNativeLibraryDir() == null || model.getNativeLibraryDir().isBlank()) {
            return;
        }
        Path dir = Path.of(model.getNativeLibraryDir());
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // Best-effort cleanup of temp library files (Windows may still hold DLL handles briefly)
                }
            });
        } catch (IOException ignored) {
        }
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
            evictOldCircuitsIfNeeded();
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

    private CircuitLoadResponse loadCircuitFromBytes(byte[] content, String filename, String filePath) {
        try {
            evictOldCircuitsIfNeeded();
            String sourceName = (filePath != null && !filePath.isBlank()) ? filePath : filename;
            // Parse using CircuitFileParser
            CircuitModel model;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                model = parser.parse(bais, sourceName);
            }

            // Generate unique circuit ID
            String circuitId = UUID.randomUUID().toString();

            // Create parsed circuit with timestamp
            CircuitState parsed = new CircuitState(
                filename,
                model,
                Instant.now()
            );

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
            CircuitModel copy = parser.parse(new ByteArrayInputStream(bytes), source.getFilePath());
            copy.setNativeLibraryDir(source.getNativeLibraryDir());
            return copy;
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
            comp.getUniqueObjectIdentifier(),
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
        final Instant loadedAt;
        final AtomicLong version = new AtomicLong();
        /**
         * Monotonic uid allocator. Unlike max+1 allocation this never reuses a
         * uid after the highest-uid component is deleted — a stale reference
         * (e.g. a mutual coupler's raw slot) must never silently rebind to a
         * newly created component.
         */
        private final AtomicLong uidCeiling;
        private final Deque<Edit> undoStack = new ArrayDeque<>();
        private final Deque<Edit> redoStack = new ArrayDeque<>();

        CircuitState(String filename, CircuitModel model, Instant loadedAt) {
            this.filename = filename;
            this.model = model;
            this.loadedAt = loadedAt;
            long max = 0;
            for (CircuitModel.ComponentData comp : model.getAllComponents()) {
                max = Math.max(max, comp.getUniqueObjectIdentifier());
            }
            for (CircuitModel.ConnectionData conn : model.getConnections()) {
                max = Math.max(max, conn.getUniqueObjectIdentifier());
            }
            this.uidCeiling = new AtomicLong(max + 1);
        }

        /** Next never-before-used uid for a new component or connection. */
        long nextUid() {
            return uidCeiling.getAndIncrement();
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
