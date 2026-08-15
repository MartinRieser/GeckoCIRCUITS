#!/usr/bin/env python3
"""
GeckoCIRCUITS Cross-Platform Desktop Packaging Script
Uses JDK's jpackage to create native installers and portable application bundles
for Windows (MSI, EXE, ZIP), macOS (DMG, PKG, ZIP), and Linux (DEB, RPM, TAR.GZ).
"""

import argparse
import os
import platform
import shutil
import subprocess
import sys
import tarfile
import zipfile
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
GUI_MODULE_TARGET = PROJECT_ROOT / "src" / "modules" / "gecko-gui" / "target"
ROOT_TARGET = PROJECT_ROOT / "target"
RESOURCES_DIR = PROJECT_ROOT / "_build" / "resources"
OUTPUT_DIR = PROJECT_ROOT / "dist"

APP_NAME = "GeckoCIRCUITS"
APP_VENDOR = "GeckoCIRCUITS Team"
APP_DESCRIPTION = "GeckoCIRCUITS - Power Electronics Circuit Simulator"
MAIN_CLASS = "gecko.GeckoSim"
DEFAULT_VERSION = "1.0.0"

DEFAULT_JAVA_OPTIONS = [
    "-Xmx3G",
    "-Dpolyglot.js.nashorn-compat=true",
    "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
]


def find_main_jar():
    """Locate the executable fat JAR in gui module target or root target."""
    candidates = [
        GUI_MODULE_TARGET / "gecko-1.0-jar-with-dependencies.jar",
        ROOT_TARGET / "gecko-1.0-jar-with-dependencies.jar",
    ]
    # Also search by glob if version differs
    if GUI_MODULE_TARGET.exists():
        candidates.extend(GUI_MODULE_TARGET.glob("gecko-*-jar-with-dependencies.jar"))
    if ROOT_TARGET.exists():
        candidates.extend(ROOT_TARGET.glob("gecko-*-jar-with-dependencies.jar"))

    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None


def run_command(cmd, cwd=None, check=True):
    """Run shell command with real-time output logging."""
    print(f"\n[RUN] {' '.join(str(c) for c in cmd)}")
    result = subprocess.run(cmd, cwd=cwd, shell=False)
    if check and result.returncode != 0:
        print(f"[ERROR] Command failed with exit code {result.returncode}")
        sys.exit(result.returncode)
    return result.returncode


def build_maven(skip_tests=True):
    """Build the multi-module Maven project and assembly fat JAR."""
    print("\n=== Building GeckoCIRCUITS with Maven ===")
    mvn_cmd = "mvn.cmd" if platform.system() == "Windows" else "mvn"
    cmd = [mvn_cmd, "clean", "package", "assembly:single"]
    if skip_tests:
        cmd.append("-DskipTests")
    cmd.append("--no-transfer-progress")
    run_command(cmd, cwd=PROJECT_ROOT)


def create_zip_archive(source_dir: Path, output_zip: Path):
    """Create a zip archive from a directory."""
    print(f"[ARCHIVE] Creating ZIP: {output_zip}")
    output_zip.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_zip, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, _, files in os.walk(source_dir):
            for file in files:
                file_path = Path(root) / file
                archive_name = file_path.relative_to(source_dir.parent)
                zf.write(file_path, archive_name)
    print(f"[ARCHIVE] Created {output_zip} ({output_zip.stat().st_size / (1024*1024):.1f} MB)")


def create_tar_archive(source_dir: Path, output_tar: Path):
    """Create a tar.gz archive from a directory."""
    print(f"[ARCHIVE] Creating TAR.GZ: {output_tar}")
    output_tar.parent.mkdir(parents=True, exist_ok=True)
    with tarfile.open(output_tar, "w:gz") as tf:
        tf.add(source_dir, arcname=source_dir.name)
    print(f"[ARCHIVE] Created {output_tar} ({output_tar.stat().st_size / (1024*1024):.1f} MB)")


def package_windows(jar_path: Path, version: str, pkg_types: list, output_dir: Path):
    """Build Windows packages (MSI, app-image, portable ZIP)."""
    icon_path = RESOURCES_DIR / "GeckoCIRCUITS.ico"
    file_assoc = RESOURCES_DIR / "file-association-ipes.properties"
    input_dir = jar_path.parent
    main_jar_name = jar_path.name

    base_cmd = [
        "jpackage",
        "--name", APP_NAME,
        "--app-version", version,
        "--vendor", APP_VENDOR,
        "--description", APP_DESCRIPTION,
        "--input", str(input_dir),
        "--main-jar", main_jar_name,
        "--main-class", MAIN_CLASS,
        "--dest", str(output_dir),
    ]

    for opt in DEFAULT_JAVA_OPTIONS:
        base_cmd.extend(["--java-options", opt])

    if icon_path.exists():
        base_cmd.extend(["--icon", str(icon_path)])

    # Build requested package types
    if "app-image" in pkg_types or "portable" in pkg_types or "all" in pkg_types:
        app_image_dir = output_dir / "app-image"
        app_image_dir.mkdir(parents=True, exist_ok=True)
        cmd = base_cmd.copy()
        cmd[cmd.index(str(output_dir))] = str(app_image_dir)
        cmd.extend(["--type", "app-image"])
        run_command(cmd)

        # Create portable zip
        portable_app = app_image_dir / APP_NAME
        if portable_app.exists():
            zip_dest = output_dir / f"{APP_NAME}-{version}-windows-x64-portable.zip"
            create_zip_archive(portable_app, zip_dest)

    if "msi" in pkg_types or "installer" in pkg_types or "all" in pkg_types:
        cmd = base_cmd.copy()
        cmd.extend([
            "--type", "msi",
            "--win-menu",
            "--win-shortcut",
            "--win-dir-chooser",
            "--win-menu-group", APP_NAME,
        ])
        if file_assoc.exists():
            cmd.extend(["--file-associations", str(file_assoc)])
        run_command(cmd, check=False)

    if "exe" in pkg_types:
        cmd = base_cmd.copy()
        cmd.extend([
            "--type", "exe",
            "--win-menu",
            "--win-shortcut",
            "--win-dir-chooser",
            "--win-menu-group", APP_NAME,
        ])
        if file_assoc.exists():
            cmd.extend(["--file-associations", str(file_assoc)])
        run_command(cmd, check=False)


def package_macos(jar_path: Path, version: str, pkg_types: list, output_dir: Path):
    """Build macOS packages (DMG, PKG, app-image, portable ZIP)."""
    icon_path = RESOURCES_DIR / "GeckoCIRCUITS.icns"
    file_assoc = RESOURCES_DIR / "file-association-ipes.properties"
    input_dir = jar_path.parent
    main_jar_name = jar_path.name

    base_cmd = [
        "jpackage",
        "--name", APP_NAME,
        "--app-version", version,
        "--vendor", APP_VENDOR,
        "--description", APP_DESCRIPTION,
        "--input", str(input_dir),
        "--main-jar", main_jar_name,
        "--main-class", MAIN_CLASS,
        "--dest", str(output_dir),
        "--mac-package-name", APP_NAME,
    ]

    for opt in DEFAULT_JAVA_OPTIONS:
        base_cmd.extend(["--java-options", opt])

    if icon_path.exists():
        base_cmd.extend(["--icon", str(icon_path)])

    if "app-image" in pkg_types or "portable" in pkg_types or "all" in pkg_types:
        app_image_dir = output_dir / "app-image"
        app_image_dir.mkdir(parents=True, exist_ok=True)
        cmd = base_cmd.copy()
        cmd[cmd.index(str(output_dir))] = str(app_image_dir)
        cmd.extend(["--type", "app-image"])
        run_command(cmd)

        app_bundle = app_image_dir / f"{APP_NAME}.app"
        if app_bundle.exists():
            zip_dest = output_dir / f"{APP_NAME}-{version}-macos-app.zip"
            create_zip_archive(app_bundle, zip_dest)

    if "dmg" in pkg_types or "installer" in pkg_types or "all" in pkg_types:
        cmd = base_cmd.copy()
        cmd.extend(["--type", "dmg"])
        if file_assoc.exists():
            cmd.extend(["--file-associations", str(file_assoc)])
        run_command(cmd)

    if "pkg" in pkg_types:
        cmd = base_cmd.copy()
        cmd.extend(["--type", "pkg"])
        if file_assoc.exists():
            cmd.extend(["--file-associations", str(file_assoc)])
        run_command(cmd)


def package_linux(jar_path: Path, version: str, pkg_types: list, output_dir: Path):
    """Build Linux packages (DEB, RPM, app-image, portable TAR.GZ)."""
    icon_path = RESOURCES_DIR / "GeckoCIRCUITS.png"
    file_assoc = RESOURCES_DIR / "file-association-ipes.properties"
    input_dir = jar_path.parent
    main_jar_name = jar_path.name
    linux_app_name = "geckocircuits"

    base_cmd = [
        "jpackage",
        "--name", linux_app_name,
        "--app-version", version,
        "--vendor", APP_VENDOR,
        "--description", APP_DESCRIPTION,
        "--input", str(input_dir),
        "--main-jar", main_jar_name,
        "--main-class", MAIN_CLASS,
        "--dest", str(output_dir),
        "--linux-shortcut",
        "--linux-menu-group", "Science;Education;Engineering;",
        "--linux-app-category", "Education",
    ]

    for opt in DEFAULT_JAVA_OPTIONS:
        base_cmd.extend(["--java-options", opt])

    if icon_path.exists():
        base_cmd.extend(["--icon", str(icon_path)])

    if "app-image" in pkg_types or "portable" in pkg_types or "all" in pkg_types:
        app_image_dir = output_dir / "app-image"
        app_image_dir.mkdir(parents=True, exist_ok=True)
        cmd = base_cmd.copy()
        cmd[cmd.index(str(output_dir))] = str(app_image_dir)
        cmd.extend(["--type", "app-image"])
        run_command(cmd)

        portable_app = app_image_dir / linux_app_name
        if portable_app.exists():
            tar_dest = output_dir / f"{APP_NAME}-{version}-linux-x64-portable.tar.gz"
            create_tar_archive(portable_app, tar_dest)

    if "deb" in pkg_types or "installer" in pkg_types or "all" in pkg_types:
        cmd = base_cmd.copy()
        cmd.extend(["--type", "deb", "--linux-deb-maintainer", "geckocircuits@users.noreply.github.com"])
        if file_assoc.exists():
            cmd.extend(["--file-associations", str(file_assoc)])
        run_command(cmd)

    if "rpm" in pkg_types or "installer" in pkg_types or "all" in pkg_types:
        cmd = base_cmd.copy()
        cmd.extend(["--type", "rpm"])
        if file_assoc.exists():
            cmd.extend(["--file-associations", str(file_assoc)])
        run_command(cmd, check=False)  # Continue if rpm-build isn't installed locally


def main():
    parser = argparse.ArgumentParser(description="Build native GeckoCIRCUITS packages using jpackage.")
    parser.add_argument("--version", default=DEFAULT_VERSION, help=f"Application version (default: {DEFAULT_VERSION})")
    parser.add_argument("--type", nargs="+", default=["all"], help="Package types (msi, exe, dmg, pkg, deb, rpm, app-image, portable, all)")
    parser.add_argument("--dest", default=str(OUTPUT_DIR), help="Output destination directory")
    parser.add_argument("--rebuild", action="store_true", help="Rebuild fat JAR with Maven first")
    parser.add_argument("--skip-maven", action="store_true", help="Skip Maven build even if JAR is missing")
    args = parser.parse_args()

    current_os = platform.system()
    dest_dir = Path(args.dest).resolve()
    dest_dir.mkdir(parents=True, exist_ok=True)

    jar_path = find_main_jar()
    if args.rebuild or (jar_path is None and not args.skip_maven):
        build_maven()
        jar_path = find_main_jar()

    if jar_path is None:
        print(f"[ERROR] Could not find executable JAR. Run with --rebuild or run 'mvn clean package assembly:single' first.")
        sys.exit(1)

    print(f"\n==========================================")
    print(f" GeckoCIRCUITS Desktop Packager")
    print(f" Platform: {current_os}")
    print(f" Version:  {args.version}")
    print(f" JAR:      {jar_path}")
    print(f" Output:   {dest_dir}")
    print(f" Types:    {args.type}")
    print(f"==========================================")

    if current_os == "Windows":
        package_windows(jar_path, args.version, args.type, dest_dir)
    elif current_os == "Darwin":
        package_macos(jar_path, args.version, args.type, dest_dir)
    elif current_os == "Linux":
        package_linux(jar_path, args.version, args.type, dest_dir)
    else:
        print(f"[ERROR] Unsupported OS: {current_os}")
        sys.exit(1)

    print("\n=== Generated Packages ===")
    for item in dest_dir.iterdir():
        if item.is_file():
            size_mb = item.stat().st_size / (1024 * 1024)
            print(f" - {item.name} ({size_mb:.1f} MB)")
    print("\nPackaging completed successfully!")


if __name__ == "__main__":
    main()
