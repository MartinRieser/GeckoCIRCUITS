package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Change event broadcast on the WebSocket topic {@code /topic/circuits/{circuitId}}
 * after every successful edit (including undo/redo).
 */
@Schema(description = "Circuit change event")
public record CircuitChangeMessage(
    @Schema(description = "Circuit the change belongs to")
    String circuitId,

    @Schema(description = "Monotonic model version after the change", example = "7")
    long modelVersion,

    @Schema(description = "Operation name, e.g. createComponent, undo", example = "createComponent")
    String operation,

    @Schema(description = "Operation-specific payload")
    Object payload
) {}
