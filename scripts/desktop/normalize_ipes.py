#!/usr/bin/env python3
"""Batch Normalizer for GeckoCIRCUITS .ipes Project Files.

Standardizes all .ipes files in the repository to clean, uncompressed,
human-readable UTF-8 plain text with normalized LF line endings.
Eliminates binary GZIP blob issues in git, inspection tools, and IDEs.
"""

import gzip
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]


def normalize_all_ipes(repo_root: Path = REPO_ROOT) -> dict:
    converted = 0
    already_plain = 0
    errors = []

    all_files = sorted(list(repo_root.rglob("*.ipes")))
    print(f"Discovered {len(all_files)} .ipes files in {repo_root}")

    for file_path in all_files:
        # Ignore files inside target, build, or hidden dirs
        parts = file_path.parts
        if any(p.startswith(".") or p in ("target", "build", "dist", "node_modules") for p in parts):
            continue

        try:
            with open(file_path, "rb") as f:
                header = f.read(2)

            is_gzip = len(header) >= 2 and header[0] == 0x1F and header[1] == 0x8B

            if is_gzip:
                with gzip.open(file_path, "rt", encoding="utf-8", errors="replace") as gz:
                    content = gz.read()
                # Normalize line endings to LF
                normalized = content.replace("\r\n", "\n").replace("\r", "\n")
                file_path.write_text(normalized, encoding="utf-8")
                converted += 1
            else:
                # Already plain text, verify it can be read as UTF-8
                content = file_path.read_text(encoding="utf-8")
                # Normalize CRLF if any
                if "\r" in content:
                    normalized = content.replace("\r\n", "\n").replace("\r", "\n")
                    file_path.write_text(normalized, encoding="utf-8")
                already_plain += 1

        except Exception as e:
            errors.append((str(file_path), str(e)))

    print(f"\nNormalization Complete:")
    print(f"  Converted from GZIP to plain UTF-8 text: {converted}")
    print(f"  Already plain text:                      {already_plain}")
    print(f"  Errors:                                  {len(errors)}")

    if errors:
        print("\nErrors encountered:")
        for path, err in errors:
            print(f"  {path}: {err}")
        sys.exit(1)

    return {
        "converted": converted,
        "already_plain": already_plain,
        "total": converted + already_plain,
    }


if __name__ == "__main__":
    normalize_all_ipes()
