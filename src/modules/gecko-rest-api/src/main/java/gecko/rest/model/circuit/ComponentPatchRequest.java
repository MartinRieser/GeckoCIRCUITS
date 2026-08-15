package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Request to modify a component. All fields optional; only provided
 * fields are changed.
 */
@Schema(description = "Patch component request (all fields optional)")
public record ComponentPatchRequest(
    @Schema(description = "New X grid position", example = "120")
    Integer x,

    @Schema(description = "New Y grid position", example = "240")
    Integer y,

    @Schema(description = "New orientation: 501 SOUTH_NORTH, 502 WEST_EAST, 503 NORTH_SOUTH, 504 EAST_WEST", example = "503")
    Integer orientation,

    @Schema(description = "New component name (must stay unique)", example = "R_load")
    String newName,

    @Schema(description = "Numeric parameters by index (merged into existing)", example = "{\"param0\": 47.0}")
    Map<String, Double> parameters
) {}
