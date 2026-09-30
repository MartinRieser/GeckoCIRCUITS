package gecko.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

/**
 * Spring Boot application entry point for GeckoCIRCUITS REST API.
 * Provides HTTP endpoints for circuit simulation and analysis.
 */
@SpringBootApplication
public class GeckoRestApiApplication {

    static {
        // Enforce English locale across all JVM threads for standardized validation messages, logs, and formats
        Locale.setDefault(Locale.ENGLISH);
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        SpringApplication.run(GeckoRestApiApplication.class, args);
    }
}
