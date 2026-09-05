fn main() {
    // tauri-build embeds the Windows resource (icon/version) via tauri-winres,
    // whose windres step is broken with absolute paths on the windows-gnu
    // toolchain. Local check builds on gnu skip it; release/bundle builds run
    // on MSVC (CI) where the full tauri_build path is used.
    #[cfg(all(target_os = "windows", target_env = "gnu"))]
    {
        println!("cargo:rustc-cfg=desktop");
        println!("cargo:rustc-cfg=dev");
        println!("cargo:rerun-if-changed=tauri.conf.json");
    }

    #[cfg(not(all(target_os = "windows", target_env = "gnu")))]
    tauri_build::build();
}
