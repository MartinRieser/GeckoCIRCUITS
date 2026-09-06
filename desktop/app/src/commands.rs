//! Commands exposed to the webview. All logic lives in `gecko-engine`;
//! this file only wires it to Tauri (dialogs, webview, managed state).

use gecko_engine::circuit_files;
use gecko_engine::sanitize::sanitize_filename;
use tauri::AppHandle;
use tauri::Manager;
use tauri::State;
use tauri_plugin_dialog::DialogExt;

use crate::AppState;

/// Opens the folder holding the engine logs.
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

/// Native save dialog + file write for `.ipes` downloads. The frontend keeps
/// doing the HTTP fetch (it owns the backend URL); the shell only does the
/// dialog and the bytes. Returns the chosen path, or None on cancel.
#[tauri::command]
pub fn save_file_dialog(
    webview: tauri::Webview,
    base64: String,
    suggested_name: String,
) -> Result<Option<String>, String> {
    let bytes = circuit_files::decode(&base64)?;
    let suggested = sanitize_filename(&suggested_name);
    let target = webview
        .app_handle()
        .dialog()
        .file()
        .set_file_name(&suggested)
        .add_filter("GeckoCIRCUITS Circuit (*.ipes)", &["ipes"])
        .add_filter("All Files (*.*)", &["*"])
        .blocking_save_file();
    let Some(path) = target else {
        return Ok(None);
    };
    let path = path.into_path().map_err(|error| error.to_string())?;
    std::fs::write(&path, bytes).map_err(|error| error.to_string())?;
    Ok(Some(path.to_string_lossy().into_owned()))
}

/// Reads a circuit the OS handed to the app (double-click, "Open with",
/// second-launch argument). Restricted to an existing `.ipes` file.
#[tauri::command]
pub fn read_ipes_file(path: String) -> Result<circuit_files::OpenIpesFile, String> {
    circuit_files::load_ipes_file(&path)
}

/// Collects `.ipes` paths from process arguments (double-click launches).
pub fn ipes_paths_from_args() -> Vec<String> {
    circuit_files::filter_ipes_args(std::env::args())
}
