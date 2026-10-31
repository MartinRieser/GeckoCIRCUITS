#!/usr/bin/env python3
"""Exports curated examples from frontend/src/model/examples.ts to standalone .ipes files in examples/."""

import re
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
EXAMPLES_TS = REPO / "frontend" / "src" / "model" / "examples.ts"
OUTPUT_DIR = REPO / "examples"


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    src = EXAMPLES_TS.read_text(encoding="utf-8")

    # Match all export const CONST_NAME = `...`;
    consts = {m.group(1): m.group(2) for m in re.finditer(r"export const (\w+) = `(.*?)`;", src, re.S)}

    # Parse EXAMPLES array items
    pattern = re.compile(
        r"\{\s*id:\s*'([^']+)',\s*name:\s*'([^']+)',\s*category:\s*'([^']+)',\s*description:\s*'([^']+)',\s*content:\s*(\w+),\s*\}",
        re.S,
    )

    examples = []
    for m in pattern.finditer(src):
        eid, name, category, desc, const_name = m.groups()
        if const_name not in consts:
            print(f"Warning: {const_name} not found in consts")
            continue
        body = consts[const_name].replace("\\\\", "\\").strip()
        examples.append({
            "id": eid,
            "name": name,
            "category": category,
            "description": desc,
            "content": body,
        })

    print(f"Found {len(examples)} examples in {EXAMPLES_TS.name}")

    readme_lines = [
        "# GeckoCIRCUITS Circuit Examples",
        "",
        "This directory contains curated educational and reference circuit models in native `.ipes` ASCII format.",
        "These files can be opened directly in GeckoCIRCUITS via **File → Open...** or explored inside the application",
        "using **File → Open Example...**.",
        "",
        "| File | Category | Circuit Name | Description |",
        "|---|---|---|---|",
    ]

    manifest = []
    for idx, ex in enumerate(examples, 1):
        filename = f"{ex['id'].replace('-', '_')}.ipes"
        filepath = OUTPUT_DIR / filename
        filepath.write_text(ex["content"] + "\n", encoding="utf-8")
        print(f"  Wrote {filepath.name} ({len(ex['content'])} bytes)")
        manifest.append({
            "id": ex["id"],
            "name": ex["name"],
            "category": ex["category"],
            "description": ex["description"],
            "filename": filename,
        })
        readme_lines.append(
            f"| [`{filename}`](./{filename}) | {ex['category']} | **{ex['name']}** | {ex['description']} |"
        )

    import json
    manifest_path = OUTPUT_DIR / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"  Wrote {manifest_path.name}")

    readme_lines.extend([
        "",
        "## Usage",
        "",
        "1. **In GeckoCIRCUITS GUI**: Select **File → Open Example...** from the top menu to browse with search and descriptions, or click **Open Examples Folder in Explorer** to browse this directory.",
        "2. **Open directly**: In GeckoCIRCUITS, choose **File → Open...** and select any `.ipes` file from this folder.",
        "3. **Headless / Automation**: Use `scripts/verify-examples.py` or the REST API `POST /gecko/api/v1/simulations` to simulate these files programmatically.",
    ])

    readme_path = OUTPUT_DIR / "README.md"
    readme_path.write_text("\n".join(readme_lines) + "\n", encoding="utf-8")
    print(f"  Wrote {readme_path.name}")
    print("Done exporting examples.")


if __name__ == "__main__":
    main()
