//! GeckoCIRCUITS desktop shell: spawns the Java engine as a hidden sidecar,
//! waits for its readiness line, then opens the editor window pointed at
//! the engine's local REST API.
//!
//! Debug builds do not spawn the engine — `tauri dev` talks to the Vite dev
//! server and an engine started separately (e.g. `run-web-editor.bat`).

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod commands;
#[cfg(not(debug_assertions))]
mod download;
mod window;

use gecko_engine::sidecar::EngineProcess;
use std::path::PathBuf;
use std::sync::Mutex;
use tauri::Manager;

pub struct AppState {
    /// Circuits arriving before the main window exists (startup race with a
    /// second instance / macOS Finder open).
    pub pending_opens: Mutex<Vec<String>>,
    pub log_dir: PathBuf,
    /// Holds the engine process; Drop kills it when the shell exits.
    pub engine: Mutex<Option<EngineProcess>>,
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, args, _cwd| {
            let circuits: Vec<String> = args
                .iter()
                .filter(|arg| arg.to_lowercase().ends_with(".ipes"))
                .cloned()
                .collect();
            window::open_file_paths(app, circuits);
            if let Some(existing) = app.get_webview_window("main") {
                let _ = existing.set_focus();
            }
        }))
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .setup(|app| {
            let log_dir = app.path().app_log_dir()?.join("engine");
            std::fs::create_dir_all(&log_dir)?;
            app.manage(AppState {
                pending_opens: Mutex::new(Vec::new()),
                log_dir,
                engine: Mutex::new(None),
            });
            window::start(app.handle().clone())
        })
        .invoke_handler(tauri::generate_handler![
            commands::open_logs_folder,
            commands::read_ipes_file,
            commands::save_file_dialog
        ])
        .build(tauri::generate_context!())
        .expect("error while building the GeckoCIRCUITS desktop shell")
        .run(|app, event| {
            // macOS: files opened via Finder while the app is running
            #[cfg(target_os = "macos")]
            if let tauri::RunEvent::Opened { files } = event {
                let paths: Vec<String> = files
                    .iter()
                    .map(|path| path.to_string_lossy().into_owned())
                    .collect();
                window::open_file_paths(app, paths);
            }
            #[cfg(not(target_os = "macos"))]
            {
                let _ = (app, event);
            }
        });
}
