package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Full editor snapshot of a loaded circuit: everything the web editor
 * renders, in one response. Polled on load and after external change events.
 */
@Schema(description = "Editor model snapshot")
public record EditorModelResponse(
    @Schema(description = "Circuit ID")
    String circuitId,

    @Schema(description = "Current model version (bumped on every edit)", example = "5")
    long modelVersion,

    @Schema(description = "Filename")
    String filename,

    @Schema(description = "Grid render scale (pixels per grid unit)", example = "16")
    int dpix,

    @Schema(description = "Worksheet size in grid units", example = "600x600")
    String worksheetSize,

    @Schema(description = "Components (all domains)")
    List<Component> components,

    @Schema(description = "Wires; index matches the connection list used by PATCH/DELETE")
    List<Wire> connections
) {
    /**
     * Editor view of a component, including terminal labels.
     */
    @Schema(description = "Editor component")
    public record Component(
        @Schema(description = "Type number", example = "1")
        int type,

        @Schema(description = "Component name (unique)", example = "R1")
        String name,

        @Schema(description = "Family: LK, CONTROL, THERM or SPECIAL")
        String family,

        @Schema(description = "Position in grid units", example = "[100, 200]")
        int[] position,

        @Schema(description = "Orientation: 501-504", example = "503")
        int orientation,

        @Schema(description = "Numeric parameters by index", example = "{\"param0\": 100.0}")
        Map<String, Object> parameters,

        @Schema(description = "Input terminal node labels (index = terminal index)")
        String[] inputLabels,

        @Schema(description = "Output terminal node labels (index = terminal index)")
        String[] outputLabels
    ) {}

    /**
     * Editor view of a wire.
     */
    @Schema(description = "Editor wire")
    public record Wire(
        @Schema(description = "Index in the connection list")
        int index,

        @Schema(description = "Connection domain: LK, CONTROL or THERMAL")
        String type,

        @Schema(description = "Wire label")
        String label,

        @Schema(description = "Polyline points in grid units")
        int[][] points
    ) {}
}
