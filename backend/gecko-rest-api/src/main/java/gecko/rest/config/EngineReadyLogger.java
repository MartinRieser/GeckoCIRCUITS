package gecko.rest.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Prints the machine-readable ready line the desktop shell waits for.
 *
 * <p>The shell spawns this server with {@code --server.port=0} (ephemeral
 * port), so the real port is only known once the web server is initialized.
 * The shell parses the printed {@code GECKO_READY <base-url>} line from
 * stdout instead of racing a fixed port.</p>
 */
@Component
public class EngineReadyLogger implements ApplicationListener<WebServerInitializedEvent> {

    public static final String READY_PREFIX = "GECKO_READY ";
    private static final Logger LOGGER = LogManager.getLogger(EngineReadyLogger.class);

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        String readyLine = READY_PREFIX + readyUrl(event.getWebServer().getPort());
        LOGGER.info(readyLine);
        // stdout is piped to the shell's readiness reader; log4j console
        // output is buffered, so flush explicitly
        System.out.println(readyLine);
        System.out.flush();
    }

    static String readyUrl(int port) {
        return "http://127.0.0.1:" + port + "/gecko";
    }
}
