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
    pub backend_url: Mutex<Option<String>>,
    pub log_dir: PathBuf,
    pub engine: Mutex<Option<EngineProcess>>,
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.set_focus();
            }
        }))
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .setup(|app| {
            let log_dir = app.path().app_log_dir()?.join("engine");
            std::fs::create_dir_all(&log_dir)?;
            app.manage(AppState {
                backend_url: Mutex::new(None),
                log_dir,
                engine: Mutex::new(None),
            });
            window::start(app.handle().clone())
        })
        .invoke_handler(tauri::generate_handler![
            commands::get_backend_url,
            commands::open_logs_folder
        ])
        .run(tauri::generate_context!())
        .expect("error while running the GeckoCIRCUITS desktop shell");
}
