//! Non-blocking destination handler for webview-initiated downloads (e.g. CSV
//! waveform exports or fallback `.ipes` downloads). `.ipes` File -> Save As
//! uses the async `save_file_dialog` Tauri command instead so the main UI
//! thread is never blocked inside WebView2's synchronous `DownloadStarting` callback.

use gecko_engine::sanitize::sanitize_filename;
use std::path::Path;
use tauri::webview::DownloadEvent;
use tauri::Manager;
use tauri::Webview;

/// Returning `true` lets the download proceed at `*destination`; `false`
/// cancels it.
pub fn handle_download(webview: Webview, event: DownloadEvent) -> bool {
    match event {
        DownloadEvent::Requested { destination, .. } => {
            let suggested = suggested_name(destination);
            if let Ok(dl_dir) = webview.app_handle().path().download_dir() {
                if dl_dir.is_dir() {
                    *destination = dl_dir.join(&suggested);
                    return true;
                }
            }
            if let Some(parent) = destination.parent() {
                *destination = parent.join(&suggested);
            }
            true
        }
        DownloadEvent::Finished { .. } => true,
        // the enum is #[non_exhaustive]; unknown future variants are harmless
        _ => true,
    }
}

fn suggested_name(destination: &Path) -> String {
    destination
        .file_name()
        .map(|name| sanitize_filename(&name.to_string_lossy()))
        .unwrap_or_else(|| "circuit.ipes".to_string())
}
