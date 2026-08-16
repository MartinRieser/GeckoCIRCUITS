package gecko.rest.controller;

import gecko.rest.model.circuit.*;
import gecko.rest.service.CircuitFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;

/**
 * REST controller for circuit file operations.
 * Provides endpoints for loading, parsing, and querying .ipes circuit files.
 *
 * Uses CircuitFileParser and CircuitModel from gecko-simulation-core.
 */
@RestController
@RequestMapping("/api/v1/circuits")
@Tag(name = "Circuit Files", description = "Circuit file loading and parsing endpoints")
public class CircuitFileController {

    private final CircuitFileService circuitFileService;

    public CircuitFileController(CircuitFileService circuitFileService) {
        this.circuitFileService = circuitFileService;
    }

    /**
     * Load circuit from multipart file upload.
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Load circuit from file upload",
        description = "Upload a .ipes circuit file and parse its structure. " +
                     "Returns circuit ID for subsequent queries."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Circuit loaded successfully",
            content = @Content(schema = @Schema(implementation = CircuitLoadResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file format or parsing error"
        )
    })
    public ResponseEntity<CircuitLoadResponse> loadCircuitFromFile(
            @Parameter(description = "Circuit file (.ipes)")
            @RequestParam("file") MultipartFile file) {

        CircuitLoadResponse response = circuitFileService.loadCircuit(file);

        if ("failed".equals(response.status())) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Load circuit from base64 encoded content.
     */
    @PostMapping(value = "/parse", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Load circuit from base64 content",
        description = "Load a circuit from base64 encoded .ipes file content. " +
                     "Useful for programmatic access without file upload."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Circuit loaded successfully",
            content = @Content(schema = @Schema(implementation = CircuitLoadResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid base64 encoding or parsing error"
        )
    })
    public ResponseEntity<CircuitLoadResponse> loadCircuitFromBase64(
            @Valid @RequestBody CircuitLoadRequest request) {

        CircuitLoadResponse response = circuitFileService.loadCircuit(
            request.content(),
            request.filename()
        );

        if ("failed".equals(response.status())) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get detailed circuit information.
     */
    @GetMapping("/{circuitId}/info")
    @Operation(
        summary = "Get circuit information",
        description = "Retrieve detailed information about a loaded circuit including " +
                     "components, connections, and simulation parameters."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Circuit info retrieved",
            content = @Content(schema = @Schema(implementation = CircuitInfo.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Circuit not found"
        )
    })
    public ResponseEntity<CircuitInfo> getCircuitInfo(
            @Parameter(description = "Circuit ID from load response")
            @PathVariable String circuitId) {

        CircuitInfo info = circuitFileService.getCircuitInfo(circuitId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(info);
    }

    /**
     * Get component list for a circuit.
     */
    @GetMapping("/{circuitId}/components")
    @Operation(
        summary = "Get circuit components",
        description = "Retrieve all components in the circuit with their parameters."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Component list retrieved",
            content = @Content(schema = @Schema(implementation = ComponentListResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Circuit not found"
        )
    })
    public ResponseEntity<ComponentListResponse> getComponents(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId) {

        ComponentListResponse response = circuitFileService.getComponents(circuitId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Validate circuit structure.
     */
    @GetMapping("/{circuitId}/validate")
    @Operation(
        summary = "Validate circuit",
        description = "Check circuit structure for errors and warnings."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Validation completed",
            content = @Content(schema = @Schema(implementation = ValidationResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Circuit not found"
        )
    })
    public ResponseEntity<ValidationResponse> validateCircuit(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId) {

        ValidationResponse response = circuitFileService.validateCircuit(circuitId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get raw circuit file content.
     */
    @GetMapping("/{circuitId}/raw")
    @Operation(
        summary = "Get raw circuit content",
        description = "Retrieve the raw (decompressed) .ipes file content for debugging."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Raw content retrieved",
            content = @Content(mediaType = "text/plain")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Circuit not found"
        )
    })
    public ResponseEntity<String> getRawCircuit(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId) {

        String raw = circuitFileService.getRawCircuit(circuitId);
        if (raw == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(raw);
    }

    /**
     * Download a loaded circuit as a .ipes file, re-serialized with CircuitFileWriter.
     */
    @GetMapping("/{circuitId}/ipes")
    @Operation(
        summary = "Download circuit as .ipes file",
        description = "Serialize the loaded circuit back to the gzip-compressed .ipes format. " +
                     "The file is interchangeable with the GeckoCIRCUITS Swing UI."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Circuit serialized",
            content = @Content(mediaType = "application/gzip")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Circuit not found"
        )
    })
    public ResponseEntity<byte[]> getIpesFile(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId) {

        byte[] bytes = circuitFileService.getIpesBytes(circuitId);
        CircuitInfo info = circuitFileService.getCircuitInfo(circuitId);
        String filename = info != null ? info.filename() : "circuit.ipes";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/gzip"))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    /**
     * Replace circuit content under the same circuit ID (multipart variant).
     */
    @PutMapping(value = "/{circuitId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Replace circuit content (file upload)",
        description = "Replace the circuit stored under the given ID with newly uploaded " +
                     ".ipes content. The circuit ID stays stable for clients."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Circuit replaced",
            content = @Content(schema = @Schema(implementation = CircuitInfo.class))
        ),
        @ApiResponse(responseCode = "400", description = "Unparseable circuit content"),
        @ApiResponse(responseCode = "404", description = "Circuit not found")
    })
    public ResponseEntity<CircuitInfo> replaceCircuitFromFile(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId,
            @Parameter(description = "Circuit file (.ipes)")
            @RequestParam("file") MultipartFile file) {

        try {
            return ResponseEntity.ok(circuitFileService.replaceCircuit(
                    circuitId, file.getBytes(), file.getOriginalFilename()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Replace circuit content under the same circuit ID (base64 variant).
     */
    @PutMapping(value = "/{circuitId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Replace circuit content (base64)",
        description = "Replace the circuit stored under the given ID with base64-encoded " +
                     ".ipes content. The circuit ID stays stable for clients."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Circuit replaced",
            content = @Content(schema = @Schema(implementation = CircuitInfo.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid base64 or unparseable content"),
        @ApiResponse(responseCode = "404", description = "Circuit not found")
    })
    public ResponseEntity<CircuitInfo> replaceCircuitFromBase64(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId,
            @Valid @RequestBody CircuitLoadRequest request) {

        byte[] content;
        try {
            content = Base64.getDecoder().decode(request.content());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid base64 encoding: " + e.getMessage());
        }

        return ResponseEntity.ok(circuitFileService.replaceCircuit(circuitId, content, request.filename()));
    }

    /**
     * Delete circuit from memory.
     */
    @DeleteMapping("/{circuitId}")
    @Operation(
        summary = "Delete circuit",
        description = "Remove circuit from memory. Useful for cleanup after processing."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Circuit deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Circuit not found"
        )
    })
    public ResponseEntity<Void> deleteCircuit(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId) {

        boolean deleted = circuitFileService.deleteCircuit(circuitId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * List all loaded circuits.
     */
    @GetMapping
    @Operation(
        summary = "List all circuits",
        description = "Get a list of all circuits currently loaded in memory."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Circuit list retrieved",
            content = @Content(schema = @Schema(implementation = CircuitListResponse.class))
        )
    })
    public ResponseEntity<CircuitListResponse> getAllCircuits() {
        CircuitListResponse response = circuitFileService.getAllCircuits();
        return ResponseEntity.ok(response);
    }

    /**
     * Clone an existing circuit with optional parameter overrides.
     */
    @PostMapping("/{circuitId}/clone")
    @Operation(
        summary = "Clone a circuit",
        description = "Creates a copy of the circuit with optional parameter overrides. " +
                     "Overrides use dot-notation format (ComponentName.parameterKey)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Circuit cloned successfully",
            content = @Content(schema = @Schema(implementation = CircuitLoadResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Source circuit not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to clone circuit"
        )
    })
    public ResponseEntity<CircuitLoadResponse> cloneCircuit(
            @Parameter(description = "Source circuit ID")
            @PathVariable String circuitId,
            @RequestBody(required = false) CircuitCloneRequest request) {

        java.util.Map<String, Double> overrides = request != null ? request.getOverrides() : null;
        CircuitLoadResponse response = circuitFileService.cloneCircuit(circuitId, overrides);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update circuit simulation parameters.
     */
    @PutMapping("/{circuitId}/parameters")
    @Operation(
        summary = "Update circuit parameters",
        description = "Updates simulation parameters of a loaded circuit in-memory. " +
                     "Only provided fields are updated; null values are ignored."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Parameters updated",
            content = @Content(schema = @Schema(implementation = CircuitInfo.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Circuit not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to update parameters"
        )
    })
    public ResponseEntity<CircuitInfo> updateCircuitParameters(
            @Parameter(description = "Circuit ID")
            @PathVariable String circuitId,
            @Valid @RequestBody CircuitParameterUpdate update) {

        CircuitInfo info = circuitFileService.updateCircuitParameters(circuitId, update);
        return ResponseEntity.ok(info);
    }
}
