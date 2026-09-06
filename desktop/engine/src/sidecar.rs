//! Engine process handling: resolving the bundled engine, building its
//! command line, spawning with log capture, and guaranteed teardown.

use std::io::{BufRead, BufReader, Write};
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::sync::mpsc::{channel, Receiver};
use std::thread;

/// Locations of the bundled Java runtime and engine jar inside the
/// installer's resource directory (`engine/runtime/bin/java`, `engine/*.jar`).
#[derive(Debug)]
pub struct EnginePaths {
    pub java: PathBuf,
    pub jar: PathBuf,
}

pub fn java_binary_name() -> &'static str {
    if cfg!(windows) {
        "java.exe"
    } else {
        "java"
    }
}

/// Resolves the engine under the given resource directory. The bundled jlink
/// runtime is required — silently using an unknown system Java would fail
/// later with a class-version error, which is much harder to diagnose.
pub fn engine_paths(resource_dir: &Path) -> Result<EnginePaths, String> {
    let base = resource_dir.join("engine");
    let jar = base.join("gecko-rest-api.jar");
    if !jar.is_file() {
        return Err(format!("engine jar not found: {}", jar.display()));
    }
    let java = base.join("runtime").join("bin").join(java_binary_name());
    if !java.is_file() {
        return Err(format!(
            "bundled Java runtime not found: {}",
            java.display()
        ));
    }
    Ok(EnginePaths { java, jar })
}

/// Command line for the engine: local-only, ephemeral port (printed via the
/// `GECKO_READY` line), heap sized like the existing launchers, and the
/// shell's PID for `ParentWatchdog`.
pub fn build_java_args(jar: &Path, parent_pid: u32) -> Vec<String> {
    vec![
        "-Xmx2g".to_string(),
        "-jar".to_string(),
        jar.to_string_lossy().into_owned(),
        "--server.port=0".to_string(),
        "--server.address=127.0.0.1".to_string(),
        format!("--gecko.parent-pid={parent_pid}"),
    ]
}

/// A running engine JVM; killed and reaped on drop so quitting the shell
/// never leaks the backend.
pub struct EngineProcess {
    child: Option<Child>,
}

impl EngineProcess {
    fn new(child: Child) -> Self {
        Self { child: Some(child) }
    }

    #[cfg(test)]
    fn for_tests(child: Child) -> Self {
        Self::new(child)
    }

    pub fn kill(&mut self) {
        if let Some(mut child) = self.child.take() {
            let _ = child.kill();
            let _ = child.wait();
        }
    }
}

impl Drop for EngineProcess {
    fn drop(&mut self) {
        self.kill();
    }
}

/// Spawned engine plus the channel feeding its stdout lines (for the
/// readiness handshake).
pub struct SpawnedEngine {
    pub process: EngineProcess,
    pub stdout_lines: Receiver<String>,
}

/// Spawns the engine JVM, piping stdout line-by-line into a channel and both
/// streams into log files next to the given log path.
pub fn spawn(
    paths: &EnginePaths,
    log_path: &Path,
    parent_pid: u32,
) -> std::io::Result<SpawnedEngine> {
    let mut command = Command::new(&paths.java);
    command
        .args(build_java_args(&paths.jar, parent_pid))
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        // 0x08000000 = CREATE_NO_WINDOW: no console window may flash
        command.creation_flags(0x0800_0000);
    }
    let mut child = command.spawn()?;

    let stdout = child.stdout.take().expect("stdout was piped");
    let stderr = child.stderr.take().expect("stderr was piped");

    let (sender, receiver) = channel();
    let out_log = log_path.to_path_buf();
    thread::spawn(move || {
        let reader = BufReader::new(stdout);
        for line in reader.lines().map_while(Result::ok) {
            if sender.send(line.clone()).is_err() {
                break; // shell side went away
            }
            append_line(&out_log, &line);
        }
    });
    let err_log = log_path.to_path_buf();
    thread::spawn(move || {
        let reader = BufReader::new(stderr);
        for line in reader.lines().map_while(Result::ok) {
            append_line(&err_log, &line);
        }
    });

    Ok(SpawnedEngine {
        process: EngineProcess::new(child),
        stdout_lines: receiver,
    })
}

fn append_line(log_path: &Path, line: &str) {
    if let Ok(mut file) = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(log_path)
    {
        let _ = writeln!(file, "{line}");
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolves_bundled_runtime_when_present() {
        let root = tempfile_dir();
        let jar = root.join("engine").join("gecko-rest-api.jar");
        let java = root
            .join("engine")
            .join("runtime")
            .join("bin")
            .join(java_binary_name());
        std::fs::create_dir_all(java.parent().unwrap()).unwrap();
        std::fs::write(&jar, b"jar").unwrap();
        std::fs::write(&java, b"java").unwrap();

        let paths = engine_paths(&root).expect("should resolve");
        assert_eq!(paths.java, java);
        assert_eq!(paths.jar, jar);
        cleanup(&root);
    }

    #[test]
    fn rejects_when_bundled_runtime_is_missing() {
        let root = tempfile_dir();
        let jar = root.join("engine").join("gecko-rest-api.jar");
        std::fs::create_dir_all(jar.parent().unwrap()).unwrap();
        std::fs::write(&jar, b"jar").unwrap();

        let error = engine_paths(&root).unwrap_err();
        assert!(error.contains("runtime not found"), "got: {error}");
        cleanup(&root);
    }

    #[test]
    fn rejects_when_jar_is_missing() {
        let root = tempfile_dir();
        std::fs::create_dir_all(root.join("engine")).unwrap();
        let error = engine_paths(&root).unwrap_err();
        assert!(error.contains("jar not found"), "got: {error}");
        cleanup(&root);
    }

    #[test]
    fn java_args_cover_port_address_heap_and_watchdog() {
        let args = build_java_args(Path::new("/opt/engine/gecko-rest-api.jar"), 4242);
        assert_eq!(
            args,
            vec![
                "-Xmx2g".to_string(),
                "-jar".to_string(),
                "/opt/engine/gecko-rest-api.jar".to_string(),
                "--server.port=0".to_string(),
                "--server.address=127.0.0.1".to_string(),
                "--gecko.parent-pid=4242".to_string(),
            ]
        );
    }

    #[test]
    fn kill_terminates_the_child() {
        let mut command = long_running_test_process();
        let child = command.spawn().expect("spawn test process");
        let mut process = EngineProcess::for_tests(child);
        process.kill();
        // kill() consumed the child; a second kill is a safe no-op
        process.kill();
    }

    #[cfg(windows)]
    fn long_running_test_process() -> Command {
        let mut command = Command::new("ping");
        command.args(["-n", "30", "127.0.0.1"]);
        command
    }

    #[cfg(not(windows))]
    fn long_running_test_process() -> Command {
        let mut command = Command::new("sleep");
        command.arg("30");
        command
    }

    fn tempfile_dir() -> PathBuf {
        static COUNTER: std::sync::atomic::AtomicU32 = std::sync::atomic::AtomicU32::new(0);
        let unique = COUNTER.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        let dir =
            std::env::temp_dir().join(format!("gecko-engine-test-{}-{unique}", std::process::id()));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        dir
    }

    fn cleanup(dir: &Path) {
        let _ = std::fs::remove_dir_all(dir);
    }
}
