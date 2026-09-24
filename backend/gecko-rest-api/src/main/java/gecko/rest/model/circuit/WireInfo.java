package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Wire representation with its index in the circuit's connection list
 * (the index is the identifier used by PATCH/DELETE).
 */
@Schema(description = "Circuit wire/connection")
public record WireInfo(
    @Schema(description = "Index in the connection list", example = "3")
    int index,

    @Schema(description = "Connection domain", example = "LK")
    String type,

    @Schema(description = "Wire label", example = "switch_node")
    String label,

    @Schema(description = "Polyline grid points", example = "[[100,200],[140,200]]")
    int[][] points
) {}
