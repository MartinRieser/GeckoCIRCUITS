//! Commands exposed to the webview. All logic lives in `gecko-engine`;
//! this file only wires it to Tauri (dialogs, webview, managed state).

use gecko_engine::circuit_files;
use gecko_engine::sanitize::sanitize_filename;
use std::path::Path;
use tauri::AppHandle;
use tauri::Manager;
use tauri::State;
use tauri_plugin_dialog::{DialogExt, MessageDialogButtons};

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

/// Logs messages from webview to webview.log.
#[tauri::command]
pub fn log_webview_message(app: AppHandle, level: String, message: String) {
    let state: State<AppState> = app.state();
    let log_file = state.log_dir.join("webview.log");
    use std::io::Write;
    if let Ok(mut f) = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(log_file)
    {
        let _ = writeln!(f, "[{level}] {message}");
    }
}

/// Sets the native window title.
#[tauri::command]
pub fn set_window_title(app: AppHandle, title: String) -> Result<(), String> {
    if let Some(window) = app.get_webview_window("main") {
        window.set_title(&title).map_err(|error| error.to_string())?;
    }
    Ok(())
}

/// Native confirmation dialog returning true for OK/Yes, false for Cancel/No.
#[tauri::command]
pub fn confirm_dialog(
    app: AppHandle,
    title: Option<String>,
    message: String,
) -> Result<bool, String> {
    let mut builder = if let Some(window) = app.get_webview_window("main") {
        app.dialog().message(&message).parent(&window)
    } else {
        app.dialog().message(&message)
    };
    if let Some(ref t) = title {
        builder = builder.title(t);
    }
    builder = builder.buttons(MessageDialogButtons::OkCancel);
    Ok(builder.blocking_show())
}

/// Native open file dialog for selecting an existing `.ipes` or `.txt` circuit.
/// Pre-focuses the dialog on current file's directory if provided.
/// Returns the opened file payload, or None on cancel.
#[tauri::command]
pub fn open_file_dialog(
    app: AppHandle,
    current_path: Option<String>,
) -> Result<Option<circuit_files::OpenIpesFile>, String> {
    let mut builder = if let Some(window) = app.get_webview_window("main") {
        app.dialog().file().set_parent(&window)
    } else {
        app.dialog().file()
    };
    if let Some(ref p) = current_path {
        let path = Path::new(p);
        if let Some(parent) = path.parent() {
            if parent.is_dir() {
                builder = builder.set_directory(parent);
            }
        }
    }
    let target = builder
        .add_filter("GeckoCIRCUITS Circuit (*.ipes, *.txt)", &["ipes", "txt"])
        .add_filter("All Files (*.*)", &["*"])
        .blocking_pick_file();
    let Some(path) = target else {
        return Ok(None);
    };
    let path = path.into_path().map_err(|error| error.to_string())?;
    let open_file = circuit_files::load_ipes_file(&path.to_string_lossy())?;
    Ok(Some(open_file))
}

/// Direct save overwriting an existing file without opening a dialog.
#[tauri::command]
pub fn save_file_direct(path: String, base64: String) -> Result<(), String> {
    let bytes = circuit_files::decode(&base64)?;
    std::fs::write(&path, bytes).map_err(|error| error.to_string())?;
    Ok(())
}

/// Native save dialog + file write for `.ipes` downloads.
/// Pre-focuses the dialog on current file's directory if provided.
/// Auto-appends `.ipes` if user omitted extension.
/// Returns the chosen path, or None on cancel.
#[tauri::command]
pub fn save_file_dialog(
    app: AppHandle,
    base64: String,
    suggested_name: String,
    current_path: Option<String>,
) -> Result<Option<String>, String> {
    let bytes = circuit_files::decode(&base64)?;
    let suggested = sanitize_filename(&suggested_name);
    let mut builder = if let Some(window) = app.get_webview_window("main") {
        app.dialog().file().set_parent(&window)
    } else {
        app.dialog().file()
    };
    builder = builder.set_file_name(&suggested);
    if let Some(ref p) = current_path {
        let path = Path::new(p);
        if let Some(parent) = path.parent() {
            if parent.is_dir() {
                builder = builder.set_directory(parent);
            }
        }
    }
    let target = builder
        .add_filter("GeckoCIRCUITS Circuit (*.ipes)", &["ipes"])
        .add_filter("All Files (*.*)", &["*"])
        .blocking_save_file();
    let Some(path) = target else {
        return Ok(None);
    };
    let mut path = path.into_path().map_err(|error| error.to_string())?;
    if path.extension().is_none() {
        path.set_extension("ipes");
    }
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
