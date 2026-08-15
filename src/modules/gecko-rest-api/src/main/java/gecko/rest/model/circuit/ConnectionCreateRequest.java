package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request to create a wire (connection) on a loaded circuit.
 */
@Schema(description = "Create connection request")
public record ConnectionCreateRequest(
    @Schema(description = "Connection domain", allowableValues = {"LK", "CONTROL", "THERM"}, defaultValue = "LK")
    @NotBlank
    String type,

    @Schema(description = "Polyline grid points [[x1,y1],[x2,y2],...]", example = "[[100,200],[140,200],[140,240]]")
    @NotEmpty
    int[][] points,

    @Schema(description = "Optional wire label", example = "switch_node")
    String label
) {}
