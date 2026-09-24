package gecko.rest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the ready-line handshake with the desktop shell.
 */
@ExtendWith(OutputCaptureExtension.class)
class EngineReadyLoggerTest {

    @Test
    void printsReadyLineWithActualPortToStdout(CapturedOutput output) {
        WebServer webServer = mock(WebServer.class);
        when(webServer.getPort()).thenReturn(54321);
        WebServerInitializedEvent event = mock(WebServerInitializedEvent.class);
        when(event.getWebServer()).thenReturn(webServer);

        new EngineReadyLogger().onApplicationEvent(event);

        assertEquals("http://127.0.0.1:54321/gecko", EngineReadyLogger.readyUrl(54321));
        assertTrue(output.toString().contains("GECKO_READY http://127.0.0.1:54321/gecko"),
                "stdout should contain the ready line, got: " + output);
    }
}
