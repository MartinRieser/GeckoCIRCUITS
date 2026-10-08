#!/usr/bin/env python3
"""Builds the desktop engine bundle: jlink runtime + Spring Boot jar.

Produces the contents of desktop/app/engine/ that the Tauri shell ships as
bundle resources:
  gecko-rest-api.jar   executable Spring Boot jar (embeds the built UI)
  runtime/             jlink Java runtime image (module list derived from
                       jdeps plus a pinned safety list)
  VERSION              engine version string

Finishes with a smoke test: the bundled runtime must run a real headless
simulation through the Boot jar (via PropertiesLauncher, because plain
-cp cannot see Boot's nested dependency jars) and produce CSV output.
"""

import argparse
import csv
import subprocess
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
APP_DIR = REPO_ROOT / "desktop" / "app"
ENGINE_DIR = APP_DIR / "engine"
JAR_TARGET = REPO_ROOT / "backend" / "gecko-rest-api" / "target"
MCP_TARGET = REPO_ROOT / "backend" / "gecko-mcp" / "target"
def read_app_version():
    """Engine version from app.version; CI syncs it from the git tag via set-version.py."""
    props = (REPO_ROOT / "backend" / "gecko-rest-api" / "src" / "main"
             / "resources" / "application.properties")
    for line in props.read_text(encoding="utf-8").splitlines():
        if line.startswith("app.version="):
            return line.split("=", 1)[1].strip()
    raise SystemExit(f"ERROR: app.version missing in {props}")


def find_rest_jar():
    jar = JAR_TARGET / "gecko-rest-api.jar"
    if not jar.is_file():
        raise SystemExit(f"ERROR: {jar} not found (run: mvn -pl backend/gecko-rest-api -am package)")
    return jar


def find_mcp_jar():
    jar = MCP_TARGET / "gecko-mcp.jar"
    if not jar.is_file():
        raise SystemExit(f"ERROR: {jar} not found (run: mvn -pl backend/gecko-mcp -am package)")
    return jar
SMOKE_CIRCUIT = REPO_ROOT / "tools" / "parity" / "circuits" / "rc-lowpass.ipes"

# Safety net on top of jdeps: Spring/tomcat/jackson load some modules
# reflectively that static analysis misses. Keep this list short and justified:
#   java.desktop         JNA and AWTColour classes referenced by the engine core
#   jdk.unsupported      sun.misc.Unsafe users (objenesis/bytebuddy style)
#   jdk.crypto.*         HTTPS/TLS for REST clients
#   jdk.zipfs            Boot nested-jar filesystem provider
#   java.net.http        REST/tooling clients
PINNED_MODULES = [
    "java.desktop", "java.instrument", "java.management", "java.naming",
    "java.net.http", "java.security.jgss", "java.sql",
    "jdk.crypto.cryptoki", "jdk.crypto.ec", "jdk.unsupported", "jdk.zipfs",
]


def run(cmd, **kwargs):
    import os
    import shutil
    parts = [str(part) for part in cmd]
    # Windows: npm/mvn are .cmd shims CreateProcess cannot find by bare name
    if not os.sep in parts[0] and "/" not in parts[0]:
        resolved = shutil.which(parts[0])
        if resolved:
            parts[0] = resolved
    print(f"  $ {' '.join(parts)}")
    return subprocess.run(parts, check=True, **kwargs)


def find_jdk25(explicit=None):
    """Locates a JDK 25 home: --jdk, JAVA_HOME, ~/.jdks, Program Files, macOS Homebrew/JVM."""
    candidates = []
    if explicit:
        candidates.append(Path(explicit))
    import os
    if os.environ.get("JAVA_HOME"):
        candidates.append(Path(os.environ["JAVA_HOME"]))
    jdk_home = Path.home() / ".jdks"
    if jdk_home.is_dir():
        candidates.extend(sorted(jdk_home.glob("jdk-25*"), reverse=True))
    for program_dir in ("Program Files/Java", "Program Files/Eclipse Adoptium"):
        base = Path("C:/") / program_dir
        if base.is_dir():
            candidates.extend(sorted(base.glob("jdk-25*"), reverse=True))
    # Linux system and SDKMAN JVM paths
    for linux_dir in (Path("/usr/lib/jvm"), Path("/usr/java"), Path("/opt/java")):
        if linux_dir.is_dir():
            candidates.extend(sorted(linux_dir.glob("*25*"), reverse=True))
    sdkman_dir = Path.home() / ".sdkman" / "candidates" / "java"
    if sdkman_dir.is_dir():
        candidates.extend(sorted(sdkman_dir.glob("*25*"), reverse=True))
    # macOS Homebrew and system JVM paths
    for mac_dir in (
        Path("/opt/homebrew/opt/openjdk"),
        Path("/opt/homebrew/opt/openjdk@25"),
        Path("/usr/local/opt/openjdk"),
        Path("/usr/local/opt/openjdk@25"),
    ):
        if mac_dir.is_dir():
            candidates.append(mac_dir)
    jvm_dir = Path("/Library/Java/JavaVirtualMachines")
    if jvm_dir.is_dir():
        for jvm in sorted(jvm_dir.glob("*25*.jdk"), reverse=True):
            candidates.append(jvm)

    # Each candidate may be a JDK root or a macOS bundle needing Contents/Home
    expanded = []
    for c in candidates:
        expanded.append(c)
        expanded.append(c / "libexec" / "openjdk.jdk" / "Contents" / "Home")
        expanded.append(c / "Contents" / "Home")

    for candidate in expanded:
        if not candidate.is_dir():
            continue
        java_exe = candidate / "bin" / "java.exe"
        java_unix = candidate / "bin" / "java"
        if not (java_exe.is_file() or java_unix.is_file()):
            continue
        release = candidate / "release"
        if release.is_file() and 'JAVA_VERSION="25' in release.read_text(errors="ignore"):
            return candidate
    raise SystemExit("ERROR: no JDK 25 found (use --jdk <home>)")


def build_frontend():
    print("== Building frontend (embeds into the jar) ==")
    run(["npm", "--prefix", REPO_ROOT / "frontend", "run", "build:spring"])


def build_jar():
    print("== Building gecko-rest-api + gecko-mcp jars ==")
    run(["mvn", "-pl", "backend/gecko-rest-api,backend/gecko-mcp", "-am",
         "package", "-DskipTests", "-q"], cwd=REPO_ROOT)
    return find_rest_jar()


def build_mcp_jar():
    """The MCP server ships next to the engine; its main artifact is the
    shaded jar so LLM clients need nothing else installed."""
    return find_mcp_jar()


def derive_modules(jdk, jar):
    """jdeps on the Boot fat jar (best effort) unioned with the pinned list."""
    jdeps_modules = set()
    try:
        result = run([jdk / "bin" / "jdeps", "--ignore-missing-deps",
                      "--print-module-deps", "--multi-release", "25", jar],
                     capture_output=True, text=True)
        # last stdout line holds the comma list
        for line in reversed(result.stdout.strip().splitlines()):
            if "," in line or "." in line:
                jdeps_modules = {m for m in line.strip().split(",") if m.startswith("java.") or m.startswith("jdk.")}
                break
    except subprocess.CalledProcessError as error:
        print(f"  jdeps failed (continuing with pinned list): {error}")
    modules = sorted(set(PINNED_MODULES) | jdeps_modules)
    print(f"  modules: {', '.join(modules)}")
    return modules


def make_runtime(jdk, modules):
    print("== Creating jlink runtime ==")
    runtime = ENGINE_DIR / "runtime"
    java_exe = runtime / "bin" / ("java.exe" if sys.platform == "win32" else "java")
    if runtime.exists():
        import shutil
        try:
            shutil.rmtree(runtime)
        except OSError as e:
            if java_exe.is_file():
                print(f"  Existing runtime in use ({e}); reusing valid runtime at {runtime}")
                return
            raise
    run([jdk / "bin" / "jlink",
         "--add-modules", ",".join(modules),
         "--output", runtime,
         "--strip-debug", "--no-man-pages", "--no-header-files",
         "--compress=zip-6"])


def bundle(jar, version):
    ENGINE_DIR.mkdir(parents=True, exist_ok=True)
    target_jar = ENGINE_DIR / "gecko-rest-api.jar"
    print(f"== Copying {jar.name} -> {target_jar} ==")
    target_jar.write_bytes(jar.read_bytes())
    mcp_jar = ENGINE_DIR / "gecko-mcp.jar"
    print(f"== Copying gecko-mcp shaded jar -> {mcp_jar} ==")
    mcp_jar.write_bytes(build_mcp_jar().read_bytes())
    # ship the NativeC C/C++ interface header so users can include it in
    # their firmware projects without unpacking the jar
    header_src = (REPO_ROOT / "backend" / "gecko-simulation-core" / "src"
                  / "main" / "resources" / "gecko" / "nativec" / "gecko_c_block.h")
    print(f"== Copying {header_src.name} -> {ENGINE_DIR} ==")
    (ENGINE_DIR / header_src.name).write_bytes(header_src.read_bytes())
    (ENGINE_DIR / "VERSION").write_text(version + "\n")


def smoke_test():
    print("== Smoke test: headless simulation through the bundled engine ==")
    java = ENGINE_DIR / "runtime" / "bin" / ("java.exe" if sys.platform == "win32" else "java")
    jar = ENGINE_DIR / "gecko-rest-api.jar"
    with tempfile.TemporaryDirectory() as tmp:
        output = Path(tmp) / "smoke.csv"
        # PropertiesLauncher + loader.main: Boot jars hide dependencies in
        # BOOT-INF/lib, so plain -cp cannot run GeckoHeadless directly
        run([java,
             "-Dloader.main=gecko.core.GeckoHeadless",
             "-cp", jar,
             "org.springframework.boot.loader.launch.PropertiesLauncher",
             "--circuit", SMOKE_CIRCUIT, "--output", output, "--quiet"])
        with output.open() as handle:
            rows = list(csv.reader(handle))
        # The engine exports all node voltages in addition to labeled probe
        # signals, so match the columns we depend on instead of the exact
        # header: time first, and the scope signal u_out somewhere after it.
        header = rows[0] if rows else []
        if len(rows) < 1000 or not header or header[0] != "time" or "u_out" not in header:
            raise SystemExit(
                f"ERROR: unexpected smoke output (header={header}, {len(rows)} rows)"
            )
        print(f"  {len(rows) - 1} data rows, header {header}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", help="JDK 25 home for jlink/jdeps")
    parser.add_argument("--version", default=None,
                        help="engine version string (default: app.version from application.properties)")
    parser.add_argument("--skip-frontend", action="store_true", help="skip npm build")
    parser.add_argument("--skip-mvn", action="store_true", help="reuse existing jar")
    args = parser.parse_args()

    version = args.version or read_app_version()
    jdk = find_jdk25(args.jdk)
    print(f"== Using JDK 25: {jdk} ==")
    # mvn must compile with JDK 25 (class file 69); the machine default may be older
    import os
    os.environ["JAVA_HOME"] = str(jdk)
    if not SMOKE_CIRCUIT.is_file():
        raise SystemExit(f"ERROR: smoke circuit missing: {SMOKE_CIRCUIT}")

    if not args.skip_frontend:
        build_frontend()
    jar = find_rest_jar() if args.skip_mvn else build_jar()

    modules = derive_modules(jdk, jar)
    make_runtime(jdk, modules)
    bundle(jar, version)
    smoke_test()
    print("== Engine bundle OK ==")


if __name__ == "__main__":
    main()
