package gecko.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip-aware .ipes text IO and workspace path resolution, mirroring the
 * Python server's conventions (GECKO_HOME or working directory).
 */
final class IpesSupport {

    private static final byte[] GZIP_MAGIC = {0x1f, (byte) 0x8b};

    private IpesSupport() {
    }

    /**
     * Resolves the GeckoCIRCUITS workspace root directory by checking the
     * {@code GECKO_HOME} environment variable first, then walking up parent
     * directories from the current working directory until locating the repository root.
     *
     * @return normalized absolute path to the workspace root
     */
    static Path workspaceRoot() {
        String home = System.getenv("GECKO_HOME");
        if (home != null && !home.isBlank()) {
            Path path = Path.of(home).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                return path;
            }
        }
        Path cur = Path.of("").toAbsolutePath().normalize();
        Path p = cur;
        while (p != null) {
            if (Files.exists(p.resolve("resources")) && (Files.exists(p.resolve(".git")) || Files.exists(p.resolve("pom.xml")))) {
                return p;
            }
            p = p.getParent();
        }
        return cur;
    }

    /**
     * Resolves a user-supplied file path string, anchoring relative paths against
     * {@link #workspaceRoot()}.
     *
     * @param path absolute or workspace-relative file path
     * @return resolved {@link Path}
     */
    static Path resolve(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("circuit_path is required");
        }
        Path candidate = Path.of(path);
        return candidate.isAbsolute() ? candidate : workspaceRoot().resolve(candidate);
    }

    /**
     * Returns {@code true} if the specified path exists on disk.
     */
    static boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * Checks whether a {@code .ipes} file on disk is GZIP-compressed by inspecting
     * its 2-byte magic header ({@code 0x1f 0x8b}).
     *
     * @param file path to inspect
     * @return {@code true} if the file exists and starts with the GZIP magic bytes
     * @throws IOException if reading the file fails
     */
    static boolean isGzipped(Path file) throws IOException {
        if (!Files.exists(file)) {
            return false;
        }
        byte[] raw = Files.readAllBytes(file);
        return raw.length >= 2 && raw[0] == GZIP_MAGIC[0] && raw[1] == GZIP_MAGIC[1];
    }

    /**
     * Reads the UTF-8 text content of a {@code .ipes} file, transparently
     * decompressing it if GZIP-encoded.
     *
     * @param file path to the {@code .ipes} file
     * @return decoded UTF-8 text content
     * @throws IOException if reading or decompression fails
     */
    static String readIpesText(Path file) throws IOException {
        byte[] raw = Files.readAllBytes(file);
        if (raw.length >= 2 && raw[0] == GZIP_MAGIC[0] && raw[1] == GZIP_MAGIC[1]) {
            try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(file))) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return new String(raw, StandardCharsets.UTF_8);
    }

    /**
     * Writes UTF-8 text content to a {@code .ipes} file, optionally GZIP-compressing
     * the output stream.
     *
     * @param file     destination file path
     * @param content  raw text content to write
     * @param compress {@code true} to GZIP-compress, {@code false} for plain text
     * @throws IOException if creating directories or writing fails
     */
    static void writeIpesText(Path file, String content, boolean compress) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        if (compress) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(file))) {
                gzip.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
    }
}
