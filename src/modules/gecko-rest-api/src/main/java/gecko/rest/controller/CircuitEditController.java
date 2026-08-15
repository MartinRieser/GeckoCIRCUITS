package gecko.rest.controller;

import gecko.rest.model.circuit.CatalogResponse;
import gecko.rest.model.circuit.CircuitChangeMessage;
import gecko.rest.model.circuit.ComponentCreateRequest;
import gecko.rest.model.circuit.ComponentPatchRequest;
import gecko.rest.model.circuit.ConnectionCreateRequest;
import gecko.rest.model.circuit.ConnectionPatchRequest;
import gecko.rest.model.circuit.EditorModelResponse;
import gecko.rest.model.circuit.NodeLabelRequest;
import gecko.rest.service.CircuitEditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for editing loaded circuits (P1): component and wire CRUD,
 * node labels, undo/redo and the component type catalog.
 *
 * <p>Every mutation returns a {@link CircuitChangeMessage} and is broadcast
 * on the WebSocket topic {@code /topic/circuits/{circuitId}}.</p>
 */
@RestController
@RequestMapping("/api/v1/circuits")
@Tag(name = "Circuit Editing", description = "Circuit editing endpoints")
public class CircuitEditController {

    private final CircuitEditService editService;

    public CircuitEditController(CircuitEditService editService) {
        this.editService = editService;
    }

    @PostMapping("/{circuitId}/components")
    @Operation(summary = "Create component",
            description = "Create a component. Coordinates are snapped to the circuit's grid raster; "
                    + "an omitted name is generated from the type and made unique.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Component created"),
        @ApiResponse(responseCode = "400", description = "Unknown family/type or invalid parameters"),
        @ApiResponse(responseCode = "404", description = "Circuit not found"),
        @ApiResponse(responseCode = "409", description = "Requested name already exists")
    })
    public ResponseEntity<CircuitChangeMessage> createComponent(
            @PathVariable String circuitId,
            @Valid @RequestBody ComponentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(editService.createComponent(circuitId, request));
    }

    @PatchMapping("/{circuitId}/components/{name}")
    @Operation(summary = "Patch component",
            description = "Move, rotate, rename or set parameters. All fields optional.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Component updated"),
        @ApiResponse(responseCode = "400", description = "Invalid values"),
        @ApiResponse(responseCode = "404", description = "Circuit or component not found"),
        @ApiResponse(responseCode = "409", description = "New name already exists")
    })
    public CircuitChangeMessage patchComponent(
            @PathVariable String circuitId,
            @PathVariable String name,
            @Valid @RequestBody ComponentPatchRequest request) {
        return editService.patchComponent(circuitId, name, request);
    }

    @DeleteMapping("/{circuitId}/components/{name}")
    @Operation(summary = "Delete component")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Component deleted"),
        @ApiResponse(responseCode = "404", description = "Circuit or component not found")
    })
    public CircuitChangeMessage deleteComponent(
            @PathVariable String circuitId,
            @PathVariable String name) {
        return editService.deleteComponent(circuitId, name);
    }

    @PostMapping("/{circuitId}/connections")
    @Operation(summary = "Create wire",
            description = "Create a wire from a polyline of grid points (at least two).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Wire created"),
        @ApiResponse(responseCode = "400", description = "Invalid points or type"),
        @ApiResponse(responseCode = "404", description = "Circuit not found")
    })
    public ResponseEntity<CircuitChangeMessage> createConnection(
            @PathVariable String circuitId,
            @Valid @RequestBody ConnectionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(editService.createConnection(circuitId, request));
    }

    @PatchMapping("/{circuitId}/connections/{index}")
    @Operation(summary = "Patch wire", description = "Replace points and/or label.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wire updated"),
        @ApiResponse(responseCode = "400", description = "Invalid points"),
        @ApiResponse(responseCode = "404", description = "Circuit or wire index not found")
    })
    public CircuitChangeMessage patchConnection(
            @PathVariable String circuitId,
            @Parameter(description = "Wire index in the connection list") @PathVariable int index,
            @Valid @RequestBody ConnectionPatchRequest request) {
        return editService.patchConnection(circuitId, index, request);
    }

    @DeleteMapping("/{circuitId}/connections/{index}")
    @Operation(summary = "Delete wire")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wire deleted"),
        @ApiResponse(responseCode = "404", description = "Circuit or wire index not found")
    })
    public CircuitChangeMessage deleteConnection(
            @PathVariable String circuitId,
            @PathVariable int index) {
        return editService.deleteConnection(circuitId, index);
    }

    @PutMapping("/{circuitId}/nodes/{componentName}")
    @Operation(summary = "Set node label",
            description = "Set the node label of a component terminal. Terminals with equal labels "
                    + "are electrically connected.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Label set"),
        @ApiResponse(responseCode = "400", description = "Invalid side or terminal index"),
        @ApiResponse(responseCode = "404", description = "Circuit or component not found")
    })
    public CircuitChangeMessage setNodeLabel(
            @PathVariable String circuitId,
            @PathVariable String componentName,
            @Valid @RequestBody NodeLabelRequest request) {
        return editService.setNodeLabel(circuitId, componentName, request);
    }

    @PostMapping("/{circuitId}/undo")
    @Operation(summary = "Undo the last edit")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Undone"),
        @ApiResponse(responseCode = "404", description = "Circuit not found"),
        @ApiResponse(responseCode = "409", description = "Nothing to undo")
    })
    public CircuitChangeMessage undo(@PathVariable String circuitId) {
        return editService.undo(circuitId);
    }

    @PostMapping("/{circuitId}/redo")
    @Operation(summary = "Redo the last undone edit")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Redone"),
        @ApiResponse(responseCode = "404", description = "Circuit not found"),
        @ApiResponse(responseCode = "409", description = "Nothing to redo")
    })
    public CircuitChangeMessage redo(@PathVariable String circuitId) {
        return editService.redo(circuitId);
    }

    @GetMapping("/{circuitId}/model")
    @Operation(summary = "Editor model snapshot",
            description = "Full editor state: components with terminal labels, wires with indices, "
                    + "grid scale and model version.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Snapshot returned"),
        @ApiResponse(responseCode = "404", description = "Circuit not found")
    })
    public EditorModelResponse getEditorModel(@PathVariable String circuitId) {
        return editService.getEditorModel(circuitId);
    }

    @GetMapping("/catalog")
    @Operation(summary = "Component type catalog",
            description = "All placeable component types with their type numbers and families.")
    public CatalogResponse getCatalog() {
        return editService.getCatalog();
    }
}
