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
            String content
    ) {}

    private File getExamplesDir() {
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
            created.mkdirs();
        }
        return created;
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
        List<ExampleDto> result = new ArrayList<>();

        for (Map<String, Object> item : manifest) {
            String id = (String) item.get("id");
            String name = (String) item.get("name");
            String category = (String) item.get("category");
            String description = (String) item.get("description");
            String filename = (String) item.get("filename");
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

            result.add(new ExampleDto(id, name, category, description, filename, content, custom));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get a single circuit example by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get example by ID", description = "Returns the example metadata and .ipes circuit content.")
    public ResponseEntity<ExampleDto> getExample(@PathVariable String id) {
        File dir = getExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);

        for (Map<String, Object> item : manifest) {
            if (id.equals(item.get("id"))) {
                String name = (String) item.get("name");
                String category = (String) item.get("category");
                String description = (String) item.get("description");
                String filename = (String) item.get("filename");
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

                return ResponseEntity.ok(new ExampleDto(id, name, category, description, filename, content, custom));
            }
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Add a new circuit example to the examples folder.
     */
    @PostMapping
    @Operation(summary = "Create example", description = "Saves a new circuit example .ipes file and registers it in manifest.json.")
    public ResponseEntity<ExampleDto> createExample(@RequestBody ExampleCreateOrUpdateRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Example name is required");
        }
        if (request.content() == null || request.content().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circuit content is required");
        }

        File dir = getExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);

        // Derive unique ID and filename
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

        String filename = id.replace('-', '_') + ".ipes";
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
    public ResponseEntity<ExampleDto> updateExample(
            @PathVariable String id,
            @RequestBody ExampleCreateOrUpdateRequest request
    ) {
        File dir = getExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);

        Map<String, Object> target = null;
        for (Map<String, Object> item : manifest) {
            if (id.equals(item.get("id"))) {
                target = item;
                break;
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

        String filename = (String) target.get("filename");
        if (filename == null || filename.isBlank()) {
            filename = id.replace('-', '_') + ".ipes";
            target.put("filename", filename);
        }

        String content = request.content();
        File circuitFile = new File(dir, filename);

        try {
            if (content != null && !content.trim().isEmpty()) {
                Files.writeString(circuitFile.toPath(), content, StandardCharsets.UTF_8);
            } else if (circuitFile.exists()) {
                content = Files.readString(circuitFile.toPath(), StandardCharsets.UTF_8);
            }

            writeManifest(dir, manifest);

            Boolean custom = target.containsKey("custom") ? (Boolean) target.get("custom") : Boolean.FALSE;
            return ResponseEntity.ok(new ExampleDto(
                    id,
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

    /**
     * Delete a circuit example from the examples directory.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete example", description = "Deletes an example file and unregisters it from manifest.json.")
    public ResponseEntity<Void> deleteExample(@PathVariable String id) {
        File dir = getExamplesDir();
        List<Map<String, Object>> manifest = readManifest(dir);

        Iterator<Map<String, Object>> it = manifest.iterator();
        Map<String, Object> removed = null;
        while (it.hasNext()) {
            Map<String, Object> item = it.next();
            if (id.equals(item.get("id"))) {
                removed = item;
                it.remove();
                break;
            }
        }

        if (removed == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = (String) removed.get("filename");
        if (filename != null) {
            File circuitFile = new File(dir, filename);
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
