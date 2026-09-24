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

    static Path workspaceRoot() {
        String home = System.getenv("GECKO_HOME");
        if (home != null && !home.isBlank()) {
            Path path = Path.of(home).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                return path;
            }
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    /** Relative paths resolve against the workspace root, like the Python server. */
    static Path resolve(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("circuit_path is required");
        }
        Path candidate = Path.of(path);
        return candidate.isAbsolute() ? candidate : workspaceRoot().resolve(candidate);
    }

    static boolean exists(Path path) {
        return Files.exists(path);
    }

    static String readIpesText(Path file) throws IOException {
        byte[] raw = Files.readAllBytes(file);
        if (raw.length >= 2 && raw[0] == GZIP_MAGIC[0] && raw[1] == GZIP_MAGIC[1]) {
            try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(file))) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return new String(raw, StandardCharsets.UTF_8);
    }

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
