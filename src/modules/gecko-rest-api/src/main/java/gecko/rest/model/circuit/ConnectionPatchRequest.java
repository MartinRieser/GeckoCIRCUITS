package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to modify a wire. All fields optional; only provided fields are changed.
 */
@Schema(description = "Patch connection request (all fields optional)")
public record ConnectionPatchRequest(
    @Schema(description = "New polyline grid points", example = "[[100,200],[160,200],[160,240]]")
    int[][] points,

    @Schema(description = "New wire label", example = "switch_node")
    String label
) {}
