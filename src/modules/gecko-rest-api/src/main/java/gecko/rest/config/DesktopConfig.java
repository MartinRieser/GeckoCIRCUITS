package gecko.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans for the desktop-shell integration. The shell passes its own PID via
 * {@code --gecko.parent-pid=<pid>}; the watchdog then exits the engine (code
 * 71) when that process disappears. Value 0 (default) disables the watchdog
 * for browser/dev deployments.
 */
@Configuration
public class DesktopConfig {

    @Bean
    public ParentWatchdog parentWatchdog(@Value("${gecko.parent-pid:0}") long parentPid) {
        ParentWatchdog watchdog = new ParentWatchdog(parentPid, 5_000,
                pid -> ProcessHandle.of(pid).isPresent(),
                () -> System.exit(71));
        watchdog.start();
        return watchdog;
    }
}
