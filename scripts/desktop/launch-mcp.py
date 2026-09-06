#!/usr/bin/env python3
"""Cross-platform launcher for GeckoCIRCUITS Java MCP server.

Locates a suitable JDK 25 (bundled runtime, JAVA_HOME, ~/.jdks, or PATH)
and starts the shaded gecko-mcp jar over stdio.
"""

import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]


def find_java() -> Path:
    # 1. Bundled desktop engine runtime
    bundled = REPO_ROOT / "desktop" / "app" / "engine" / "runtime" / "bin" / ("java.exe" if sys.platform == "win32" else "java")
    if bundled.is_file():
        return bundled

    # 2. JAVA_HOME
    if os.environ.get("JAVA_HOME"):
        cand = Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if sys.platform == "win32" else "java")
        if cand.is_file():
            return cand

    # 3. ~/.jdks
    jdk_home = Path.home() / ".jdks"
    if jdk_home.is_dir():
        for cand in sorted(jdk_home.glob("jdk-25*"), reverse=True):
            exe = cand / "bin" / ("java.exe" if sys.platform == "win32" else "java")
            if exe.is_file():
                return exe

    # 4. Program Files on Windows
    if sys.platform == "win32":
        for base_name in ("Program Files/Java", "Program Files/Eclipse Adoptium"):
            base = Path("C:/") / base_name
            if base.is_dir():
                for cand in sorted(base.glob("jdk-25*"), reverse=True):
                    exe = cand / "bin" / "java.exe"
                    if exe.is_file():
                        return exe

    # 5. Default java on PATH
    import shutil
    resolved = shutil.which("java")
    if resolved:
        return Path(resolved)

    raise SystemExit("ERROR: JDK 25 java executable not found.")


def find_jar() -> Path:
    # 1. Bundled in desktop engine
    bundled = REPO_ROOT / "desktop" / "app" / "engine" / "gecko-mcp.jar"
    if bundled.is_file():
        return bundled

    # 2. Target in gecko-mcp module
    target_dir = REPO_ROOT / "src" / "modules" / "gecko-mcp" / "target"
    if target_dir.is_dir():
        jars = list(target_dir.glob("gecko-mcp-*-jar-with-dependencies.jar"))
        if jars:
            return sorted(jars, key=lambda p: p.stat().st_mtime, reverse=True)[0]

    raise SystemExit("ERROR: gecko-mcp shaded jar not found. Run 'python scripts/desktop/build-engine.py' or 'mvn package -pl src/modules/gecko-mcp'.")


def main():
    java = find_java()
    jar = find_jar()
    cmd = [str(java), "-jar", str(jar)] + sys.argv[1:]
    
    if sys.platform == "win32":
        import subprocess
        sys.exit(subprocess.call(cmd))
    else:
        os.execv(str(java), cmd)


if __name__ == "__main__":
    main()
