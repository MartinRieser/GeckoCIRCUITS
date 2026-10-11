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


# Semver with an optional pre-release suffix, e.g. 1.2.3 or 1.2.3-rc.1,
# or calendar week format, e.g. 41-2026 (week-year).
SEMVER_PATTERN = r"^\d+\.\d+\.\d+(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$"
CALVER_PATTERN = r"^(\d{1,2})-(\d{4})$"


def main():
    if len(sys.argv) != 2:
        raise SystemExit(
            "usage: set-version.py <major.minor.patch>[-pre-release] | <week>-<year> (optional leading 'v')"
        )
    raw = sys.argv[1].strip().lstrip("v")

    calver_match = re.match(CALVER_PATTERN, raw)
    semver_match = re.match(SEMVER_PATTERN, raw)

    if calver_match:
        week, year = calver_match.group(1), calver_match.group(2)
        app_version = f"{week}-{year}"
        # WiX ProductVersion limits: major <= 255, minor <= 255, build <= 65535.
        # Week (1..53) fits in major, 0 fits in minor, and year fits in build.
        cargo_tauri_version = f"{week}.0.{year}"
    elif semver_match:
        app_version = raw
        # The MSI bundler enforces WiX ProductVersion rules: the pre-release
        # identifier must be numeric-only, so "3.1.0-rc.1" is rejected. The
        # installer/bundler version therefore carries the base release only;
        # the full version string (incl. -rc.N) stays in the files below.
        cargo_tauri_version = raw.split("-", 1)[0]
    else:
        raise SystemExit(
            "usage: set-version.py <major.minor.patch>[-pre-release] | <week>-<year> (optional leading 'v')"
        )

    conf = json.loads(TAURI_CONF.read_text(encoding="utf-8"))
    conf["version"] = cargo_tauri_version
    TAURI_CONF.write_text(json.dumps(conf, indent=2) + "\n", encoding="utf-8")

    text = APP_PROPERTIES.read_text(encoding="utf-8")
    APP_PROPERTIES.write_text(
        re.sub(r"(?m)^app\.version=.*$", f"app.version={app_version}", text),
        encoding="utf-8",
    )

    cargo_version = cargo_tauri_version if calver_match else app_version
    update_cargo_version(CARGO_APP, cargo_version)
    update_cargo_version(CARGO_ENGINE, cargo_version)

    print(
        f"version set to {app_version} (bundler/cargo: {cargo_version}) in "
        f"tauri.conf.json, application.properties, Cargo.toml"
    )


if __name__ == "__main__":
    main()
