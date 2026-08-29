import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P5 parity harness: runs the same circuit through the NEW headless engine
 * (gecko-rest-api) and stores the exported results CSV.
 *
 * Pure JDK HTTP client, no dependencies. The REST server must be running.
 *
 * Usage: NewEngineRunner &lt;baseUrl&gt; &lt;ipesFile&gt; &lt;outCsv&gt; [signal,signal,...] [tEndOverride]
 */
public final class NewEngineRunner {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final long RUN_TIMEOUT_MS = 180_000;

    private NewEngineRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: NewEngineRunner <baseUrl> <ipesFile> <outCsv> [signals] [tEndOverride]");
            System.exit(2);
        }
        String baseUrl = args[0].endsWith("/") ? args[0].substring(0, args[0].length() - 1)
                : args[0] + "/gecko";
        Path ipes = Path.of(args[1]).toAbsolutePath();
        Path outCsv = Path.of(args[2]).toAbsolutePath();

        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(ipes));
        StringBuilder json = new StringBuilder("{\"base64Circuit\":\"").append(base64).append('"');
        if (args.length > 3 && !args[3].isBlank()) {
            json.append(",\"signals\":[");
            String[] signals = args[3].split(",");
            for (int i = 0; i < signals.length; i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append('"').append(signals[i]).append('"');
            }
            json.append(']');
        }
        if (args.length > 4 && !args[4].isBlank()) {
            json.append(String.format(Locale.ROOT, ",\"simulationTime\":%s", args[4]));
        }
        json.append('}');

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest submit = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/simulations"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        HttpResponse<String> response = client.send(submit, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IllegalStateException("simulation submit failed: HTTP " + response.statusCode()
                    + " " + response.body());
        }
        String simulationId = extract(response.body(), "\"simulationId\"\\s*:\\s*\"([^\"]+)\"");
        System.out.println("new engine simulation: " + simulationId);

        String status = awaitCompletion(client, baseUrl, simulationId);
        if (!"COMPLETED".equals(status)) {
            throw new IllegalStateException("new engine simulation ended with status " + status);
        }

        HttpRequest export = HttpRequest.newBuilder(
                        URI.create(baseUrl + "/api/v1/simulations/" + simulationId + "/export"))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> csv = client.send(export, HttpResponse.BodyHandlers.ofString());
        if (csv.statusCode() != 200) {
            throw new IllegalStateException("results export failed: HTTP " + csv.statusCode()
                    + " " + csv.body());
        }
        Files.writeString(outCsv, csv.body(), StandardCharsets.UTF_8);
        System.out.println("new engine results -> " + outCsv);
    }

    private static String awaitCompletion(HttpClient client, String baseUrl, String simulationId)
            throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + RUN_TIMEOUT_MS;
        Pattern statusPattern = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");
        while (System.currentTimeMillis() < deadline) {
            HttpRequest poll = HttpRequest.newBuilder(
                            URI.create(baseUrl + "/api/v1/simulations/" + simulationId))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(poll, HttpResponse.BodyHandlers.ofString());
            Matcher matcher = statusPattern.matcher(response.body());
            if (matcher.find()) {
                String status = matcher.group(1);
                if ("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
                    return status;
                }
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("new engine simulation did not finish in time: " + simulationId);
    }

    private static String extract(String json, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("cannot parse response: " + json);
        }
        return matcher.group(1);
    }
}
