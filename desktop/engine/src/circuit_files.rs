//! Circuit-file handling for the desktop shell: validation of OS-provided
//! paths, argument filtering, and base64 transport encoding.

use base64::Engine as _;
use serde::Serialize;
use std::path::PathBuf;

/// Payload handed to the frontend for "open circuit" flows.
#[derive(Serialize)]
pub struct OpenIpesFile {
    pub name: String,
    pub base64: String,
}

/// Validates that the OS-provided path really is an existing `.ipes` circuit.
pub fn validate_ipes_path(path: &str) -> Result<PathBuf, String> {
    let path = PathBuf::from(path);
    let is_ipes = path
        .extension()
        .and_then(|extension| extension.to_str())
        .map(|extension| extension.eq_ignore_ascii_case("ipes"))
        .unwrap_or(false);
    if !is_ipes {
        return Err(format!("not a .ipes circuit: {}", path.display()));
    }
    if !path.is_file() {
        return Err(format!("file not found: {}", path.display()));
    }
    Ok(path)
}

/// Collects `.ipes` paths from process arguments (double-click launches).
pub fn filter_ipes_args<I, S>(args: I) -> Vec<String>
where
    I: IntoIterator<Item = S>,
    S: AsRef<str>,
{
    args.into_iter()
        .skip(1)
        .filter(|arg| arg.as_ref().to_lowercase().ends_with(".ipes"))
        .map(|arg| arg.as_ref().to_string())
        .collect()
}

/// Loads and encodes a circuit for the webview, after validation.
pub fn load_ipes_file(path: &str) -> Result<OpenIpesFile, String> {
    let path = validate_ipes_path(path)?;
    let name = path
        .file_name()
        .map(|name| name.to_string_lossy().into_owned())
        .unwrap_or_else(|| "circuit.ipes".to_string());
    let bytes = std::fs::read(&path).map_err(|error| error.to_string())?;
    Ok(OpenIpesFile {
        name,
        base64: encode(&bytes),
    })
}

pub fn encode(bytes: &[u8]) -> String {
    base64::engine::general_purpose::STANDARD.encode(bytes)
}

pub fn decode(text: &str) -> Result<Vec<u8>, String> {
    base64::engine::general_purpose::STANDARD
        .decode(text)
        .map_err(|error| format!("invalid base64: {error}"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ipes_path_validation() {
        let dir = std::env::temp_dir().join(format!("gecko-circuit-files-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let circuit = dir.join("circuit.ipes");
        std::fs::write(&circuit, b"GeckoSimulationProject").unwrap();

        assert!(validate_ipes_path(circuit.to_str().unwrap()).is_ok());
        assert!(validate_ipes_path(dir.join("circuit.IPES").to_str().unwrap()).is_ok());
        assert!(
            validate_ipes_path(dir.join("missing.ipes").to_str().unwrap())
                .unwrap_err()
                .contains("file not found")
        );
        assert!(validate_ipes_path(dir.join("other.txt").to_str().unwrap())
            .unwrap_err()
            .contains("not a .ipes"));
        assert!(validate_ipes_path(dir.join("noext").to_str().unwrap())
            .unwrap_err()
            .contains("not a .ipes"));
        let _ = std::fs::remove_dir_all(&dir);
    }

    #[test]
    fn argument_filter_keeps_only_circuits() {
        let args = ["app.exe", "C:\\a.ipes", "--flag", "D:/x/y.IPES"];
        assert_eq!(
            filter_ipes_args(args),
            vec!["C:\\a.ipes".to_string(), "D:/x/y.IPES".to_string()]
        );
        assert!(filter_ipes_args(["app.exe"]).is_empty());
    }

    #[test]
    fn base64_round_trip() {
        let bytes = b"GeckoSimulationProject\x00\xff\x01";
        assert_eq!(decode(&encode(bytes)).unwrap(), bytes);
        assert!(decode("not base64!").is_err());
    }

    #[test]
    fn load_rejects_and_accepts() {
        let dir =
            std::env::temp_dir().join(format!("gecko-circuit-files-load-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let circuit = dir.join("my.ipes");
        std::fs::write(&circuit, b"content").unwrap();

        let loaded = load_ipes_file(circuit.to_str().unwrap()).unwrap();
        assert_eq!(loaded.name, "my.ipes");
        assert_eq!(decode(&loaded.base64).unwrap(), b"content");
        assert!(load_ipes_file(dir.join("x.txt").to_str().unwrap()).is_err());
        let _ = std::fs::remove_dir_all(&dir);
    }
}
