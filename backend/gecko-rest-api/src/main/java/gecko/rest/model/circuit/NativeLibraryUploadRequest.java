package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to upload a compiled shared library (.dll, .so, .dylib) for a
 * NativeC (typ 88) block when opening circuits via a web browser.
 */
@Schema(description = "Upload a compiled NativeC shared library associated with an active circuit")
public record NativeLibraryUploadRequest(
    @Schema(description = "Shared library filename (basename)", example = "buck_ctrl.dll")
    @NotBlank
    String filename,

    @Schema(description = "Base64-encoded binary content of the shared library")
    @NotBlank
    String content
) {}
