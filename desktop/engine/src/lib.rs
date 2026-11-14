//! Pure-logic helpers for the GeckoCIRCUITS desktop shell.
//!
//! Everything here is unit-tested without a display using `std`, `base64`, and
//! `serde`, so it can be verified on any toolchain; the Tauri app crate
//! (`gecko-desktop`) wires these pieces into the window/event loop.

pub mod circuit_files;
pub mod ready;
pub mod sanitize;
pub mod sidecar;
