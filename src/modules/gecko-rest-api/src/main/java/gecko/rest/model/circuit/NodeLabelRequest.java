package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to set a node label on a component terminal. Terminals with equal
 * labels are electrically connected (label-based connectivity).
 */
@Schema(description = "Set node label request")
public record NodeLabelRequest(
    @Schema(description = "Terminal index on the component", example = "0")
    @NotNull
    Integer terminalIndex,

    @Schema(description = "Terminal side: x = input/start, y = output/end", allowableValues = {"x", "y"})
    @NotBlank
    String side,

    @Schema(description = "Node label; equal labels connect terminals", example = "dc_link")
    String label
) {}
