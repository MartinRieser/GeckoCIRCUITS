package gecko.rest.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * REST controller for managing circuit examples on disk.
 * Supports listing, creating, updating, and deleting example models
 * in the examples/ folder with synchronization to manifest.json.
 */
@RestController
@RequestMapping("/api/v1/examples")
@Tag(name = "Examples", description = "Endpoints for managing circuit example files on disk")
public class ExamplesController {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public record ExampleDto(
            String id,
            String name,
            String category,
            String description,
            String filename,
            String content,
            Boolean custom
    ) {}

    public record ExampleCreateOrUpdateRequest(
            String id,
            String name,
            String category,
            String description,
            String content,
            Boolean overwrite
    ) {}

    private File getExamplesDir() {
        String customProp = System.getProperty("gecko.examples-dir");
        if (customProp == null || customProp.isBlank()) {
            customProp = System.getenv("GECKO_EXAMPLES_DIR");
        }
        if (customProp != null && !customProp.isBlank()) {
            File envDir = new File(customProp.trim());
            if (envDir.exists() && envDir.isDirectory()) {
                return envDir;
            }
        }
        File[] candidates = new File[] {
                new File("examples"),
                new File("../examples"),
                new File("../../examples"),
                new File("resources/examples"),
                new File("../resources/examples")
        };
        for (File c : candidates) {
            if (c.exists() && c.isDirectory()) {
                return c;
            }
        }
        File created = new File("examples");
        if (!created.exists()) {
            try {
                created.mkdirs();
            } catch (Exception ignored) {}
        }
        return created;
    }

    private File getWritableExamplesDir() {
        File dir = getExamplesDir();
        if (dir.exists() && dir.canWrite()) {
            return dir;
        }
        File userDir = new File(System.getProperty("user.home"), ".geckocircuits/examples");
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
        return userDir;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "example.ipes";
        }
        String name = new File(filename).getName();
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!name.endsWith(".ipes") && !name.endsWith(".txt")) {
            name += ".ipes";
        }
        return name;
    }

    private File getManifestFile(File dir) {
        return new File(dir, "manifest.json");
    }

    private synchronized List<Map<String, Object>> readManifest(File dir) {
        File manifestFile = getManifestFile(dir);
        if (!manifestFile.exists()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(manifestFile, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private synchronized void writeManifest(File dir, List<Map<String, Object>> manifest) throws IOException {
        File manifestFile = getManifestFile(dir);
        objectMapper.writeValue(manifestFile, manifest);
    }

    /**
     * List all circuit examples available on disk.
     */
    @GetMapping
    @Operation(summary = "List examples", description = "Returns all circuit examples with metadata and content from the examples directory.")
    public ResponseEntity<List<ExampleDto>> listExamples() {
        File dir = getExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);
        Map<String, ExampleDto> result = new LinkedHashMap<>();

        populateExamples(dir, manifest, result, false);

        File userDir = getWritableExamplesDir();
        if (!userDir.getAbsolutePath().equals(dir.getAbsolutePath()) && userDir.exists()) {
            List<Map<String, Object>> userManifest = readManifest(userDir);
            populateExamples(userDir, userManifest, result, true);
        }

        return ResponseEntity.ok(new ArrayList<>(result.values()));
    }

    private void populateExamples(File dir, List<Map<String, Object>> manifest, Map<String, ExampleDto> out, boolean forceCustom) {
        for (Map<String, Object> item : manifest) {
            String id = (String) item.get("id");
            if (id == null) continue;
            String name = (String) item.get("name");
            String category = (String) item.get("category");
            String description = (String) item.get("description");
            String rawFilename = (String) item.get("filename");
            String filename = rawFilename != null ? sanitizeFilename(rawFilename) : null;
            Boolean custom = forceCustom || (item.containsKey("custom") ? (Boolean) item.get("custom") : Boolean.FALSE);

            String content = "";
            if (filename != null) {
                File file = new File(dir, filename);
                if (file.exists() && file.isFile()) {
                    try {
                        content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    } catch (IOException ignored) {}
                }
            }

            out.put(id, new ExampleDto(id, name, category, description, filename, content, custom));
        }
    }

    /**
     * Get a single circuit example by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get example by ID", description = "Returns the example metadata and .ipes circuit content.")
    public ResponseEntity<ExampleDto> getExample(@PathVariable String id) {
        File dir = getWritableExamplesDir();
        ExampleDto dto = findExampleInDir(dir, id);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        File baseDir = getExamplesDir();
        if (!baseDir.getAbsolutePath().equals(dir.getAbsolutePath())) {
            dto = findExampleInDir(baseDir, id);
            if (dto != null) {
                return ResponseEntity.ok(dto);
            }
        }
        return ResponseEntity.notFound().build();
    }

    private ExampleDto findExampleInDir(File dir, String id) {
        if (!dir.exists()) return null;
        List<Map<String, Object>> manifest = readManifest(dir);
        for (Map<String, Object> item : manifest) {
            String itemId = (String) item.get("id");
            String itemName = (String) item.get("name");
            if (id.equals(itemId) || (itemName != null && itemName.trim().equalsIgnoreCase(id.trim()))) {
                String name = (String) item.get("name");
                String category = (String) item.get("category");
                String description = (String) item.get("description");
                String rawFilename = (String) item.get("filename");
                String filename = rawFilename != null ? sanitizeFilename(rawFilename) : null;
                Boolean custom = item.containsKey("custom") ? (Boolean) item.get("custom") : Boolean.FALSE;

                String content = "";
                if (filename != null) {
                    File file = new File(dir, filename);
                    if (file.exists() && file.isFile()) {
                        try {
                            content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                        } catch (IOException ignored) {}
                    }
                }

                return new ExampleDto(itemId, name, category, description, filename, content, custom);
            }
        }
        return null;
    }

    /**
     * Add or overwrite a circuit example in the examples folder.
     */
    @PostMapping
    @Operation(summary = "Create or overwrite example", description = "Saves a circuit example .ipes file and registers it in manifest.json. If an example with this name or ID exists and overwrite is true, it replaces the existing example.")
    public synchronized ResponseEntity<ExampleDto> createExample(@RequestBody ExampleCreateOrUpdateRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Example name is required");
        }
        if (request.content() == null || request.content().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circuit content is required");
        }

        File dir = getWritableExamplesDir();
        File baseDir = getExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);

        // Check if an existing example matches by id or by name (case-insensitive)
        Map<String, Object> existingEntry = findEntryByIdOrName(manifest, request.id(), request.name());
        if (existingEntry == null && !baseDir.getAbsolutePath().equals(dir.getAbsolutePath())) {
            List<Map<String, Object>> baseManifest = readManifest(baseDir);
            Map<String, Object> baseMatch = findEntryByIdOrName(baseManifest, request.id(), request.name());
            if (baseMatch != null) {
                existingEntry = new LinkedHashMap<>(baseMatch);
                manifest.add(existingEntry);
            }
        }

        // Overwrite existing example if requested or if explicit existing ID was provided
        if (existingEntry != null) {
            boolean shouldOverwrite = Boolean.TRUE.equals(request.overwrite())
                    || (request.id() != null && !request.id().trim().isEmpty());
            if (!shouldOverwrite) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An example with the name '" + existingEntry.get("name") + "' already exists. Enable overwrite to update it.");
            }

            String targetId = (String) existingEntry.get("id");
            if (request.name() != null && !request.name().trim().isEmpty()) {
                existingEntry.put("name", request.name().trim());
            }
            if (request.category() != null) {
                existingEntry.put("category", request.category().trim());
            }
            if (request.description() != null) {
                existingEntry.put("description", request.description().trim());
            }

            String rawFilename = (String) existingEntry.get("filename");
            String filename = rawFilename != null && !rawFilename.isBlank()
                    ? sanitizeFilename(rawFilename)
                    : sanitizeFilename(targetId.replace('-', '_') + ".ipes");
            existingEntry.put("filename", filename);

            File circuitFile = new File(dir, filename);
            try {
                Files.writeString(circuitFile.toPath(), request.content(), StandardCharsets.UTF_8);
                writeManifest(dir, manifest);

                Boolean custom = existingEntry.containsKey("custom") ? (Boolean) existingEntry.get("custom") : Boolean.FALSE;
                return ResponseEntity.ok(
                        new ExampleDto(
                                targetId,
                                (String) existingEntry.get("name"),
                                (String) existingEntry.get("category"),
                                (String) existingEntry.get("description"),
                                filename,
                                request.content(),
                                custom
                        )
                );
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write example file: " + e.getMessage());
            }
        }

        // Derive unique ID and filename for brand new example
        String baseId = (request.id() != null && !request.id().trim().isEmpty())
                ? request.id().trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_")
                : request.name().trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");

        if (baseId.isEmpty()) {
            baseId = "example_" + System.currentTimeMillis();
        }

        String id = baseId;
        int suffix = 1;
        while (existsId(manifest, id)) {
            id = baseId + "_" + suffix++;
        }

        String filename = sanitizeFilename(id.replace('-', '_') + ".ipes");
        File circuitFile = new File(dir, filename);

        try {
            Files.writeString(circuitFile.toPath(), request.content(), StandardCharsets.UTF_8);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", id);
            entry.put("name", request.name().trim());
            entry.put("category", request.category() != null ? request.category().trim() : "Custom");
            entry.put("description", request.description() != null ? request.description().trim() : "");
            entry.put("filename", filename);
            entry.put("custom", Boolean.TRUE);

            manifest.add(entry);
            writeManifest(dir, manifest);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ExampleDto(
                            id,
                            (String) entry.get("name"),
                            (String) entry.get("category"),
                            (String) entry.get("description"),
                            filename,
                            request.content(),
                            Boolean.TRUE
                    )
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write example file: " + e.getMessage());
        }
    }

    /**
     * Update an existing circuit example on disk.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update example", description = "Updates example metadata and/or .ipes file content on disk.")
    public synchronized ResponseEntity<ExampleDto> updateExample(
            @PathVariable String id,
            @RequestBody ExampleCreateOrUpdateRequest request
    ) {
        File dir = getWritableExamplesDir();
        File baseDir = getExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);

        Map<String, Object> target = findEntryByIdOrName(manifest, id, id);
        if (target == null && !baseDir.getAbsolutePath().equals(dir.getAbsolutePath())) {
            List<Map<String, Object>> baseManifest = readManifest(baseDir);
            Map<String, Object> baseMatch = findEntryByIdOrName(baseManifest, id, id);
            if (baseMatch != null) {
                target = new LinkedHashMap<>(baseMatch);
                manifest.add(target);
            }
        }

        if (target == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.name() != null && !request.name().trim().isEmpty()) {
            target.put("name", request.name().trim());
        }
        if (request.category() != null) {
            target.put("category", request.category().trim());
        }
        if (request.description() != null) {
            target.put("description", request.description().trim());
        }

        String rawFilename = (String) target.get("filename");
        String filename = rawFilename != null && !rawFilename.isBlank()
                ? sanitizeFilename(rawFilename)
                : sanitizeFilename(((String) target.get("id")).replace('-', '_') + ".ipes");
        target.put("filename", filename);

        String content = request.content();
        File circuitFile = new File(dir, filename);

        try {
            if (content != null && !content.trim().isEmpty()) {
                Files.writeString(circuitFile.toPath(), content, StandardCharsets.UTF_8);
            } else if (circuitFile.exists()) {
                content = Files.readString(circuitFile.toPath(), StandardCharsets.UTF_8);
            } else {
                File baseCircuitFile = new File(baseDir, filename);
                if (baseCircuitFile.exists()) {
                    content = Files.readString(baseCircuitFile.toPath(), StandardCharsets.UTF_8);
                    Files.writeString(circuitFile.toPath(), content, StandardCharsets.UTF_8);
                }
            }

            writeManifest(dir, manifest);

            Boolean custom = target.containsKey("custom") ? (Boolean) target.get("custom") : Boolean.FALSE;
            return ResponseEntity.ok(new ExampleDto(
                    (String) target.get("id"),
                    (String) target.get("name"),
                    (String) target.get("category"),
                    (String) target.get("description"),
                    filename,
                    content,
                    custom
            ));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save updated example: " + e.getMessage());
        }
    }

    private Map<String, Object> findEntryByIdOrName(List<Map<String, Object>> manifest, String id, String name) {
        if (id != null && !id.trim().isEmpty()) {
            for (Map<String, Object> item : manifest) {
                if (id.trim().equalsIgnoreCase(String.valueOf(item.get("id")))) {
                    return item;
                }
            }
        }
        if (name != null && !name.trim().isEmpty()) {
            for (Map<String, Object> item : manifest) {
                String existingName = (String) item.get("name");
                if (existingName != null && existingName.trim().equalsIgnoreCase(name.trim())) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Delete a circuit example from the examples directory.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete example", description = "Deletes an example file and unregisters it from manifest.json.")
    public synchronized ResponseEntity<Void> deleteExample(@PathVariable String id) {
        File dir = getWritableExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);

        Iterator<Map<String, Object>> it = manifest.iterator();
        Map<String, Object> removed = null;
        while (it.hasNext()) {
            Map<String, Object> item = it.next();
            String itemId = (String) item.get("id");
            String itemName = (String) item.get("name");
            if (id.equals(itemId) || (itemName != null && itemName.trim().equalsIgnoreCase(id.trim()))) {
                removed = item;
                it.remove();
                break;
            }
        }

        if (removed == null) {
            return ResponseEntity.notFound().build();
        }

        String rawFilename = (String) removed.get("filename");
        if (rawFilename != null) {
            File circuitFile = new File(dir, sanitizeFilename(rawFilename));
            if (circuitFile.exists()) {
                circuitFile.delete();
            }
        }

        try {
            writeManifest(dir, manifest);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update manifest after deletion: " + e.getMessage());
        }
    }

    private boolean existsId(List<Map<String, Object>> manifest, String id) {
        for (Map<String, Object> item : manifest) {
            if (id.equalsIgnoreCase((String) item.get("id"))) {
                return true;
            }
        }
        return false;
    }
}
