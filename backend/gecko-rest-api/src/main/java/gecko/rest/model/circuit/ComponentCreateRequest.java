package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request to create a component on a loaded circuit.
 */
@Schema(description = "Create component request")
public record ComponentCreateRequest(
    @Schema(description = "Component family", allowableValues = {"LK", "THERM"})
    @NotBlank
    String family,

    @Schema(description = "Component type number (see catalog)", example = "1")
    @NotNull
    Integer type,

    @Schema(description = "Component name; auto-generated and made unique when omitted", example = "R1")
    String name,

    @Schema(description = "X grid position", example = "100")
    @NotNull
    Integer x,

    @Schema(description = "Y grid position", example = "200")
    @NotNull
    Integer y,

    @Schema(description = "Orientation: 501 SOUTH_NORTH, 502 WEST_EAST, 503 NORTH_SOUTH (default), 504 EAST_WEST", example = "503")
    Integer orientation,

    @Schema(description = "Component parameters by key", example = "{\"param0\": 100.0, \"sourceCode\": \"yOUT[0]=xIN[0];\"}")
    Map<String, Object> parameters
) {}
