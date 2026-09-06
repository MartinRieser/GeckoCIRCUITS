#!/usr/bin/env python3
"""Writes gecko-mcp launcher scripts + an LLM client config snippet next to an
app installation (the directory containing engine/gecko-mcp.jar).

Usage: python scripts/desktop/write-mcp-launchers.py [--dest DIR]
Default dest: desktop/dist/mcp
"""

import argparse
import json
import platform
import stat
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

UNIX_BODY = ('#!/usr/bin/env bash\n'
             'DIR="$(cd "$(dirname "$0")" && pwd)"\n'
             'exec "$DIR/engine/runtime/bin/java" -jar "$DIR/engine/gecko-mcp.jar" "$@"\n')
WIN_BODY = ('@echo off\r\n'
            'set "DIR=%~dp0"\r\n'
            '"%DIR%engine\\runtime\\bin\\java.exe" -jar "%DIR%engine\\gecko-mcp.jar" %*\r\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dest", default=str(REPO_ROOT / "desktop" / "dist" / "mcp"),
                        help="output directory for the launchers")
    args = parser.parse_args()

    dest = Path(args.dest)
    dest.mkdir(parents=True, exist_ok=True)

    sh = dest / "gecko-mcp.sh"
    sh.write_text(UNIX_BODY, newline="\n")
    sh.chmod(sh.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    (dest / "gecko-mcp.bat").write_text(WIN_BODY, newline="\r\n")

    command = str(sh.resolve()) if platform.system() != "Windows" else \
        str((dest / "gecko-mcp.bat").resolve())
    config = {"mcpServers": {"gecko-circuits": {"command": command}}}
    (dest / "mcp-client-config.json").write_text(json.dumps(config, indent=2) + "\n")

    print(f"launchers + config written to {dest}")
    print("LLM client config (Claude Desktop / Cursor / ZCode):")
    print(json.dumps(config, indent=2))


if __name__ == "__main__":
    main()
