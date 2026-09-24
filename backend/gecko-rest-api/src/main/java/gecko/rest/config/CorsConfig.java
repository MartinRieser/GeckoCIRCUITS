package gecko.rest.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS for the desktop shell: the Tauri webview serves the UI from its own
 * origin (tauri://localhost on macOS/Linux, https://tauri.localhost on
 * Windows) while the engine listens on http://127.0.0.1:&lt;random port&gt;.
 * Browser deployments are same-origin and unaffected. The allowed origins
 * can be extended via {@code gecko.api.allowed-origins} (comma separated).
 */
@Configuration
public class CorsConfig {

    static final String ALLOWED_ORIGINS_PROPERTY = "gecko.api.allowed-origins";

    /**
     * Shared CORS source: used by the standalone CorsFilter (headers on every
     * /api response) and by the Spring Security chain (rejects foreign origins).
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${" + ALLOWED_ORIGINS_PROPERTY
                    + ":tauri://localhost,https://tauri.localhost,http://localhost:5173}")
            String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> desktopCorsFilter(UrlBasedCorsConfigurationSource source) {
        // Highest precedence so CORS headers are present on auth/engine error responses too
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
