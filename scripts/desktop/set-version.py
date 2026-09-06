#!/usr/bin/env python3
"""Syncs a release version into the files that carry it.

Usage: python scripts/desktop/set-version.py 1.2.3

Updates desktop/app/tauri.conf.json, desktop Cargo.toml files, pom.xml files,
src/modules/gecko-rest-api application properties (app.version),
and frontend/package.json. CI calls this with the v* tag before building installers.
"""

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
TAURI_CONF = REPO_ROOT / "desktop" / "app" / "tauri.conf.json"
APP_PROPERTIES = (
    REPO_ROOT / "src" / "modules" / "gecko-rest-api" / "src" / "main" / "resources" / "application.properties"
)
PACKAGE_JSON = REPO_ROOT / "frontend" / "package.json"
CARGO_APP = REPO_ROOT / "desktop" / "app" / "Cargo.toml"
CARGO_ENGINE = REPO_ROOT / "desktop" / "engine" / "Cargo.toml"
POM_ROOT = REPO_ROOT / "pom.xml"
POM_REST_API = REPO_ROOT / "src" / "modules" / "gecko-rest-api" / "pom.xml"
POM_MCP = REPO_ROOT / "src" / "modules" / "gecko-mcp" / "pom.xml"


def update_pom_artifact_version(pom_path: Path, artifact_id: str, new_version: str):
    text = pom_path.read_text(encoding="utf-8")
    pattern = rf"(<artifactId>{re.escape(artifact_id)}</artifactId>\s*<version>)[^<]+(</version>)"
    new_text, count = re.subn(pattern, rf"\g<1>{new_version}\g<2>", text)
    if count == 0:
        raise ValueError(f"Could not find artifactId {artifact_id} with version in {pom_path}")
    pom_path.write_text(new_text, encoding="utf-8")


def update_cargo_version(cargo_path: Path, new_version: str):
    text = cargo_path.read_text(encoding="utf-8")
    new_text, count = re.subn(r'(?m)^version\s*=\s*"[^"]+"', f'version = "{new_version}"', text, count=1)
    if count == 0:
        raise ValueError(f"Could not find package version in {cargo_path}")
    cargo_path.write_text(new_text, encoding="utf-8")


def main():
    if len(sys.argv) != 2 or not re.fullmatch(r"\d+\.\d+\.\d+", sys.argv[1].strip().lstrip("v")):
        raise SystemExit("usage: set-version.py <major.minor.patch> (optional leading 'v')")
    version = sys.argv[1].strip().lstrip("v")

    conf = json.loads(TAURI_CONF.read_text(encoding="utf-8"))
    conf["version"] = version
    TAURI_CONF.write_text(json.dumps(conf, indent=2) + "\n", encoding="utf-8")

    text = APP_PROPERTIES.read_text(encoding="utf-8")
    APP_PROPERTIES.write_text(
        re.sub(r"(?m)^app\.version=.*$", f"app.version={version}", text),
        encoding="utf-8",
    )

    package = json.loads(PACKAGE_JSON.read_text(encoding="utf-8"))
    package["version"] = version
    PACKAGE_JSON.write_text(json.dumps(package, indent=2) + "\n", encoding="utf-8")

    update_cargo_version(CARGO_APP, version)
    update_cargo_version(CARGO_ENGINE, version)

    update_pom_artifact_version(POM_ROOT, "gecko-parent", version)
    update_pom_artifact_version(POM_REST_API, "gecko-rest-api", version)
    update_pom_artifact_version(POM_MCP, "gecko-mcp", version)

    print(f"version set to {version} in tauri.conf.json, application.properties, package.json, Cargo.toml, pom.xml")


if __name__ == "__main__":
    main()
