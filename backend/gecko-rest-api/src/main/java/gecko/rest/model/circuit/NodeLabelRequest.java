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
    String label,

    @Schema(description = "When true, remove the terminal label at terminalIndex instead of "
            + "setting it; labels above shift down and the label array shrinks (used to drop "
            + "scope channels). The last remaining label of a side cannot be removed.")
    Boolean remove
) {
    /** Convenience constructor for the plain set-label request. */
    public NodeLabelRequest(Integer terminalIndex, String side, String label) {
        this(terminalIndex, side, label, null);
    }
}
