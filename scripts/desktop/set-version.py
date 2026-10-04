#!/usr/bin/env python3
"""Syncs a release version into the files that carry it.

Usage: python scripts/desktop/set-version.py 1.2.3

Updates desktop/app/tauri.conf.json (installer version), the desktop Cargo.toml
files, and backend/gecko-rest-api application.properties (app.version).
CI calls this with the v* tag before building installers.

Build artifact file names never contain the version: the Maven modules build
as gecko-rest-api.jar / gecko-mcp.jar / gecko-simulation-core.jar (pom
finalName) and are not touched here.
Accepts an optional semver pre-release suffix (e.g. 1.2.3-rc.1) so release
candidate tags can be built without a separate versioning scheme.
"""

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
TAURI_CONF = REPO_ROOT / "desktop" / "app" / "tauri.conf.json"
APP_PROPERTIES = (
    REPO_ROOT / "backend" / "gecko-rest-api" / "src" / "main" / "resources" / "application.properties"
)
CARGO_APP = REPO_ROOT / "desktop" / "app" / "Cargo.toml"
CARGO_ENGINE = REPO_ROOT / "desktop" / "engine" / "Cargo.toml"


def update_cargo_version(cargo_path: Path, new_version: str):
    text = cargo_path.read_text(encoding="utf-8")
    new_text, count = re.subn(r'(?m)^version\s*=\s*"[^"]+"', f'version = "{new_version}"', text, count=1)
    if count == 0:
        raise ValueError(f"Could not find package version in {cargo_path}")
    cargo_path.write_text(new_text, encoding="utf-8")


# Semver with an optional pre-release suffix, e.g. 1.2.3 or 1.2.3-rc.1.
# Every version-carrying file updated below accepts semver pre-release
# identifiers (tauri.conf.json / Cargo.toml require them, Maven is lenient),
# so release candidates like v3.1.0-rc.1 work end to end.
VERSION_PATTERN = r"\d+\.\d+\.\d+(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?"


def main():
    if len(sys.argv) != 2 or not re.fullmatch(VERSION_PATTERN, sys.argv[1].strip().lstrip("v")):
        raise SystemExit(
            "usage: set-version.py <major.minor.patch>[-pre-release] (optional leading 'v')"
        )
    version = sys.argv[1].strip().lstrip("v")

    conf = json.loads(TAURI_CONF.read_text(encoding="utf-8"))
    # The MSI bundler enforces WiX ProductVersion rules: the pre-release
    # identifier must be numeric-only, so "3.1.0-rc.1" is rejected. The
    # installer/bundler version therefore carries the base release only;
    # the full version string (incl. -rc.N) stays in the files below.
    conf["version"] = version.split("-", 1)[0]
    TAURI_CONF.write_text(json.dumps(conf, indent=2) + "\n", encoding="utf-8")

    text = APP_PROPERTIES.read_text(encoding="utf-8")
    APP_PROPERTIES.write_text(
        re.sub(r"(?m)^app\.version=.*$", f"app.version={version}", text),
        encoding="utf-8",
    )

    update_cargo_version(CARGO_APP, version)
    update_cargo_version(CARGO_ENGINE, version)

    print(f"version set to {version} in tauri.conf.json, application.properties, Cargo.toml")


if __name__ == "__main__":
    main()
