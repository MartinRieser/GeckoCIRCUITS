//! Commands exposed to the webview.

use crate::AppState;
use tauri::AppHandle;
use tauri::Manager;
use tauri::State;

/// The engine's base URL (e.g. `http://127.0.0.1:54321/gecko`), as a
/// fallback if the initialization script ever loses a race with page scripts.
#[tauri::command]
pub fn get_backend_url(state: State<'_, AppState>) -> Option<String> {
    state.backend_url.lock().unwrap().clone()
}

/// Opens the folder holding the engine logs (used by Help > troubleshooting).
#[tauri::command]
pub fn open_logs_folder(app: AppHandle) -> Result<(), String> {
    use tauri_plugin_opener::OpenerExt;
    let state: State<AppState> = app.state();
    let dir = state.log_dir.clone();
    if !dir.is_dir() {
        std::fs::create_dir_all(&dir).map_err(|error| error.to_string())?;
    }
    app.opener()
        .open_path(dir.to_string_lossy(), None::<&str>)
        .map_err(|error| error.to_string())
}
