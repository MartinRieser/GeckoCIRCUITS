//! Native save dialog for engine-initiated downloads (`.ipes` saves come as
//! Content-Disposition downloads from the REST API).

use gecko_engine::sanitize::sanitize_filename;
use std::path::Path;
use tauri::webview::DownloadEvent;
use tauri::Manager;
use tauri::Webview;
use tauri_plugin_dialog::DialogExt;

/// Returning `true` lets the download proceed at `*destination`; `false`
/// cancels it.
pub fn handle_download(webview: Webview, event: DownloadEvent) -> bool {
    match event {
        DownloadEvent::Requested { destination, .. } => {
            let suggested = suggested_name(destination);
            let chosen = webview
                .app_handle()
                .dialog()
                .file()
                .set_file_name(&suggested)
                .add_filter("GeckoCIRCUITS Circuit (*.ipes)", &["ipes"])
                .add_filter("All Files (*.*)", &["*"])
                .blocking_save_file();
            match chosen.and_then(|path| path.into_path().ok()) {
                Some(path) => {
                    *destination = path;
                    true
                }
                None => false,
            }
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
