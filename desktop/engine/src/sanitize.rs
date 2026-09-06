//! Filesystem-safe filename handling for engine downloads (`.ipes` saves).

/// Replaces characters that are invalid in filenames on Windows (and
/// confusing on other platforms), trims stray dots/whitespace, and falls
/// back to a circuit name when nothing usable remains.
pub fn sanitize_filename(input: &str) -> String {
    let cleaned: String = input
        .chars()
        .map(|c| match c {
            '/' | '\\' | ':' | '*' | '?' | '"' | '<' | '>' | '|' => '_',
            _ => c,
        })
        .collect();
    let trimmed = cleaned.trim().trim_matches('.').trim().to_string();
    if trimmed.is_empty() {
        "circuit.ipes".to_string()
    } else {
        trimmed
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn keeps_normal_names() {
        assert_eq!(
            sanitize_filename("buck_converter.ipes"),
            "buck_converter.ipes"
        );
        assert_eq!(
            sanitize_filename("my circuit v2.ipes"),
            "my circuit v2.ipes"
        );
    }

    #[test]
    fn replaces_windows_invalid_characters() {
        assert_eq!(
            sanitize_filename("a/b\\c:d*e?f\"g<h>i|j.ipes"),
            "a_b_c_d_e_f_g_h_i_j.ipes"
        );
    }

    #[test]
    fn trims_whitespace_and_dots() {
        assert_eq!(sanitize_filename("  name .ipes .. "), "name .ipes");
        assert_eq!(sanitize_filename("..."), "circuit.ipes");
        assert_eq!(sanitize_filename("   "), "circuit.ipes");
        assert_eq!(sanitize_filename(""), "circuit.ipes");
    }

    #[test]
    fn strips_path_components_semantics() {
        // the caller passes the raw suggested name; separators must not survive
        assert_eq!(sanitize_filename("../../etc/passwd"), "_.._etc_passwd");
        assert_eq!(sanitize_filename("C:\\temp\\x.ipes"), "C__temp_x.ipes");
    }
}
