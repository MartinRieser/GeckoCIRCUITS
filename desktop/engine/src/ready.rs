//! Parser for the engine's readiness handshake.
//!
//! The engine prints `GECKO_READY <base-url>` once its web server is up.
//! The line can carry a log4j console prefix (the engine logs to stdout
//! too), so the token is searched anywhere in the line.

/// Marker the Java side prints (`EngineReadyLogger`).
pub const READY_PREFIX: &str = "GECKO_READY ";

/// Extracts the base URL (e.g. `http://127.0.0.1:54321/gecko`) from a stdout
/// line. Returns `None` for lines without the marker or with a malformed URL.
pub fn parse_ready_line(line: &str) -> Option<String> {
    let start = line.find(READY_PREFIX)?;
    let url = line[start + READY_PREFIX.len()..].trim();
    if url.starts_with("http://") || url.starts_with("https://") {
        Some(url.to_string())
    } else {
        None
    }
}

/// Waits for the ready line on the engine's stdout, with an overall budget.
/// Engine stdout ends (channel disconnects) if the process dies, which is
/// reported as an error, not as a timeout.
pub fn wait_for_ready(
    lines: &std::sync::mpsc::Receiver<String>,
    budget: std::time::Duration,
) -> Result<String, String> {
    let deadline = std::time::Instant::now() + budget;
    loop {
        let remaining = deadline.saturating_duration_since(std::time::Instant::now());
        if remaining.is_zero() {
            return Err("engine did not become ready in time".to_string());
        }
        match lines.recv_timeout(remaining) {
            Ok(line) => {
                if let Some(url) = parse_ready_line(&line) {
                    return Ok(url);
                }
                // unrelated stdout output (Spring logs etc.) — keep waiting
            }
            Err(std::sync::mpsc::RecvTimeoutError::Timeout) => {
                return Err("engine did not become ready in time".to_string());
            }
            Err(std::sync::mpsc::RecvTimeoutError::Disconnected) => {
                return Err("engine exited before signaling readiness".to_string());
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::mpsc;
    use std::time::Duration;

    #[test]
    fn parses_plain_ready_line() {
        assert_eq!(
            parse_ready_line("GECKO_READY http://127.0.0.1:54321/gecko"),
            Some("http://127.0.0.1:54321/gecko".to_string())
        );
    }

    #[test]
    fn parses_line_with_log_prefix_and_crlf() {
        assert_eq!(
            parse_ready_line("2026-09-05 18:00:00 - g.r.config.EngineReadyLogger - GECKO_READY http://127.0.0.1:8080/gecko\r\n"),
            Some("http://127.0.0.1:8080/gecko".to_string())
        );
    }

    #[test]
    fn rejects_lines_without_marker() {
        assert_eq!(
            parse_ready_line("Started GeckoRestApiApplication in 4.2 seconds"),
            None
        );
        assert_eq!(parse_ready_line(""), None);
    }

    #[test]
    fn rejects_marker_with_malformed_url() {
        assert_eq!(parse_ready_line("GECKO_READY not-a-url"), None);
        assert_eq!(parse_ready_line("GECKO_READY "), None);
    }

    #[test]
    fn wait_for_ready_skips_noise_lines() {
        let (tx, rx) = mpsc::channel();
        for line in ["spring log line", "GECKO_READY http://127.0.0.1:9/gecko"] {
            tx.send(line.to_string()).unwrap();
        }
        drop(tx);
        assert_eq!(
            wait_for_ready(&rx, Duration::from_secs(5)).unwrap(),
            "http://127.0.0.1:9/gecko"
        );
    }

    #[test]
    fn wait_for_ready_reports_dead_process() {
        let (tx, rx) = mpsc::channel::<String>();
        drop(tx);
        let error = wait_for_ready(&rx, Duration::from_secs(5)).unwrap_err();
        assert!(
            error.contains("exited before signaling readiness"),
            "got: {error}"
        );
    }

    #[test]
    fn wait_for_ready_times_out_on_silence() {
        let (_tx, rx) = mpsc::channel::<String>();
        let error = wait_for_ready(&rx, Duration::from_millis(50)).unwrap_err();
        assert!(error.contains("did not become ready"), "got: {error}");
    }
}
