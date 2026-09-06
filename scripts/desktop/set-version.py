#!/usr/bin/env python3
"""Syncs a release version into the files that carry it.

Usage: python scripts/desktop/set-version.py 1.2.3

Updates desktop/app/tauri.conf.json, src/modules/gecko-rest-api application
properties (app.version), and frontend/package.json. CI calls this with the
v* tag before building installers.
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

    print(f"version set to {version} in tauri.conf.json, application.properties, package.json")


if __name__ == "__main__":
    main()
