//! Pure-logic helpers for the GeckoCIRCUITS desktop shell.
//!
//! Everything here is std-only and unit-tested without a display, so it can
//! be verified on any toolchain; the Tauri app crate (`gecko-desktop`) only
//! wires these pieces into the window/event loop.

pub mod circuit_files;
pub mod ready;
pub mod sanitize;
pub mod sidecar;
