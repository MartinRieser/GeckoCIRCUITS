//! Window creation and the engine startup sequence.

#[cfg(not(debug_assertions))]
use std::time::Duration;
use tauri::AppHandle;
use tauri::Manager;
use tauri::WebviewUrl;
use tauri::WebviewWindowBuilder;

/// Queue shim installed before any page script runs: payloads arriving before
/// the editor registers its handler are buffered, not dropped.
const OPEN_FILE_INIT: &str = "window.__geckoOpenFileQueue=window.__geckoOpenFileQueue||[];\
window.__geckoOpenFile=function(p){\
(window.__geckoOpenFileHandler?window.__geckoOpenFileHandler:\
function(q){window.__geckoOpenFileQueue.push(q);})(p);};";

#[cfg(not(debug_assertions))]
use crate::download;
#[cfg(not(debug_assertions))]
use crate::AppState;
#[cfg(not(debug_assertions))]
use gecko_engine::ready;
#[cfg(not(debug_assertions))]
use gecko_engine::sidecar;
#[cfg(not(debug_assertions))]
use std::path::Path;
#[cfg(not(debug_assertions))]
const ENGINE_START_BUDGET: Duration = Duration::from_secs(90);

pub fn start(app: AppHandle) -> Result<(), Box<dyn std::error::Error>> {
    #[cfg(debug_assertions)]
    return start_dev(app);

    #[cfg(not(debug_assertions))]
    return start_release(app);
}

/// Dev mode: no sidecar; the Vite dev server proxies to an engine the
/// developer starts separately (same-origin, no backend injection needed).
#[cfg(debug_assertions)]
fn start_dev(app: AppHandle) -> Result<(), Box<dyn std::error::Error>> {
    let url: tauri::Url = "http://localhost:5173".parse()?;
    WebviewWindowBuilder::new(&app, "main", WebviewUrl::External(url))
        .title("GeckoCIRCUITS (dev)")
        .inner_size(1400.0, 900.0)
        .min_inner_size(1024.0, 700.0)
        .initialization_script(OPEN_FILE_INIT)
        .build()?;
    open_files_from_args(&app);
    Ok(())
}

/// Release mode: spawn the bundled engine, wait for its ready line, then
/// open the editor window with the backend origin injected.
#[cfg(not(debug_assertions))]
fn start_release(app: AppHandle) -> Result<(), Box<dyn std::error::Error>> {
    let state: tauri::State<AppState> = app.state();
    let log_path = state.log_dir.join("engine.log");

    let startup = start_engine(&app, &log_path).and_then(|spawned| {
        ready::wait_for_ready(&spawned.stdout_lines, ENGINE_START_BUDGET)
            .map(|url| (spawned.process, url))
    });

    let (process, backend_url) = match startup {
        Ok(pair) => pair,
        Err(message) => {
            fatal_dialog(
                &app,
                &format!(
                    "The simulation engine failed to start.\n\n{message}\n\nEngine log: {}",
                    log_path.display()
                ),
            );
            std::process::exit(1);
        }
    };

    *state.backend_url.lock().unwrap() = Some(backend_url.clone());
    // EngineProcess moves into managed state; Drop kills it when the shell exits
    *state.engine.lock().unwrap() = Some(process);

    create_main_window(&app, &backend_url)?;
    open_files_from_args(&app);
    Ok(())
}

#[cfg(not(debug_assertions))]
fn start_engine(app: &AppHandle, log_path: &Path) -> Result<sidecar::SpawnedEngine, String> {
    let resource_dir = app
        .path()
        .resource_dir()
        .map_err(|error| format!("cannot locate resource directory: {error}"))?;
    let paths = sidecar::engine_paths(&resource_dir)?;
    sidecar::spawn(&paths, log_path, std::process::id())
        .map_err(|error| format!("cannot start {}: {error}", paths.java.display()))
}

#[cfg(not(debug_assertions))]
fn create_main_window(app: &AppHandle, backend_url: &str) -> Result<(), tauri::Error> {
    let init_script = format!("window.__GECKO_BACKEND__ = '{backend_url}';{OPEN_FILE_INIT}");
    WebviewWindowBuilder::new(app, "main", WebviewUrl::App("index.html".into()))
        .title("GeckoCIRCUITS")
        .inner_size(1400.0, 900.0)
        .min_inner_size(1024.0, 700.0)
        .initialization_script(&init_script)
        .on_download(download::handle_download)
        .build()?;
    Ok(())
}

/// Opens `.ipes` paths given on the command line (double-click / "Open with").
pub fn open_files_from_args(app: &AppHandle) {
    open_file_paths(app, crate::commands::ipes_paths_from_args());
}

/// Forwards circuits to the editor; payloads queue in the webview until the
/// frontend registers its handler.
pub fn open_file_paths(app: &AppHandle, paths: Vec<String>) {
    if paths.is_empty() {
        return;
    }
    let Some(window) = app.get_webview_window("main") else {
        return;
    };
    for path in paths {
        match crate::commands::read_ipes_file(path) {
            Ok(open_file) => {
                let payload =
                    serde_json::to_string(&open_file).unwrap_or_else(|_| "{}".to_string());
                if let Err(error) = window.eval(format!("window.__geckoOpenFile({payload});")) {
                    eprintln!("open-file forward failed: {error}");
                }
            }
            Err(message) => eprintln!("open-file skipped: {message}"),
        }
    }
}

#[cfg(not(debug_assertions))]
fn fatal_dialog(app: &AppHandle, message: &str) {
    use tauri_plugin_dialog::DialogExt;
    app.dialog()
        .message(message)
        .title("GeckoCIRCUITS")
        .blocking_show();
}
