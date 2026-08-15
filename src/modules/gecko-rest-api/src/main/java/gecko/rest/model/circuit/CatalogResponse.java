package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Component type catalog for the editor palette, derived from the
 * core type registry.
 */
@Schema(description = "Component type catalog")
public record CatalogResponse(
    @Schema(description = "Available component types")
    List<CatalogEntry> types
) {
    /**
     * One placeable component type.
     */
    @Schema(description = "Component type")
    public record CatalogEntry(
        @Schema(description = "Type number used in create requests", example = "1")
        int type,

        @Schema(description = "Type identifier", example = "LK_R")
        String name,

        @Schema(description = "Family for create requests", example = "LK")
        String family
    ) {}
}
