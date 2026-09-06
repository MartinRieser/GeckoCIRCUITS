#!/usr/bin/env python3
"""One-shot dev tool: extracts the .ipes templates from the Python MCP
server's generators into Java resources (with numbered hole markers) and
dumps the hole expressions for hand-porting. Also emits golden outputs for
several parameter sets, used by the Java equivalence tests.

Usage (from repo root): python scripts/desktop/extract-templates.py
"""

import ast
import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SERVER = REPO / "tools" / "mcp" / "gecko_mcp" / "server.py"
RES = REPO / "src" / "modules" / "gecko-mcp" / "src" / "main" / "resources" / "templates"
GOLDEN = REPO / "src" / "modules" / "gecko-mcp" / "src" / "test" / "resources" / "golden"
HOLE_MARK = "\u00a7"  # §

PFC_SETS = [
    {},
    {"target_voltage": 60.0, "f_sw": 25000.0},
    {"v_rms": 30.0, "inductance": 0.001, "capacitance": 0.0047},
    {"r_load_base": 12.5, "r_load_step": 50.0, "t_step": 0.06},
    {"duration": 0.05, "dt": 2e-6, "f_grid": 60.0},
]
LLC_SETS = [
    {},
    {"v_in": 300.0, "v_out": 48.0, "p_out": 2000.0},
    {"f_sw": 250000.0, "l_r": 1.0e-6, "c_r": 2.0e-6},
    {"l_m": 2.2e-5, "t_dead": 2.5e-7},
    {"v_out": 24.0, "p_out": 500.0},
]


def extract(tree, func_name):
    for node in ast.walk(tree):
        if isinstance(node, ast.FunctionDef) and node.name == func_name:
            big = max(
                (j for j in ast.walk(node) if isinstance(j, ast.JoinedStr)),
                key=lambda j: len(j.values),
            )
            parts, holes = [], []
            for value in big.values:
                if isinstance(value, ast.Constant):
                    parts.append(value.value)
                else:
                    fmt = ""
                    if value.format_spec:
                        spec = value.format_spec
                        if len(spec.values) == 1 and isinstance(spec.values[0], ast.Constant):
                            fmt = spec.values[0].value
                        else:
                            raise SystemExit("dynamic format spec: " + ast.unparse(spec))
                    holes.append({"expr": ast.unparse(value.value), "format": fmt})
                    parts.append(f"{HOLE_MARK}{len(holes) - 1}{HOLE_MARK}")
            return "".join(parts), holes
    raise SystemExit(f"function not found: {func_name}")


def named_templates(func):
    """Collects interpolated f-string variables assigned in func, each as
    (template-with-markers, holes) — these are nested templates."""
    out = {}
    for node in ast.walk(func):
        if isinstance(node, ast.Assign) and isinstance(node.value, ast.JoinedStr):
            values = node.value.values
            if any(not isinstance(v, ast.Constant) for v in values):
                parts, holes = [], []
                for value in values:
                    if isinstance(value, ast.Constant):
                        parts.append(value.value)
                    else:
                        fmt = ""
                        if value.format_spec:
                            spec = value.format_spec
                            if len(spec.values) == 1 and isinstance(spec.values[0], ast.Constant):
                                fmt = spec.values[0].value
                            else:
                                raise SystemExit("dynamic format spec: " + ast.unparse(spec))
                        holes.append({"expr": ast.unparse(value.value), "format": fmt})
                        parts.append(f"{HOLE_MARK}{len(holes) - 1}{HOLE_MARK}")
                out[node.targets[0].id] = ("".join(parts), holes)
    return out


def named_strings(func):
    """Collects local string variables (JoinedStr/Constant) assigned in func."""
    out = {}
    for node in ast.walk(func):
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name) and isinstance(
                    node.value, (ast.JoinedStr, ast.Constant)
                ):
                    if isinstance(node.value, ast.JoinedStr):
                        if any(not isinstance(v, ast.Constant) for v in node.value.values):
                            continue  # interpolated — handled as a hole
                        text = "".join(v.value for v in node.value.values)
                    else:
                        text = node.value.value
                    out[target.id] = text
    return out


def main():
    import sys
    sys.path.insert(0, str(REPO / "tools" / "mcp"))
    from gecko_mcp.server import generate_pfc_ipes_text, generate_llc_ipes_text

    RES.mkdir(parents=True, exist_ok=True)
    GOLDEN.mkdir(parents=True, exist_ok=True)
    tree = ast.parse(SERVER.read_text(encoding="utf-8"))

    for name, func, sets in (
        ("pfc", generate_pfc_ipes_text, PFC_SETS),
        ("llc", generate_llc_ipes_text, LLC_SETS),
    ):
        template, holes = extract(tree, f"generate_{name}_ipes_text")
        (RES / f"{name}-ipes_content.tpl").write_text(template, encoding="utf-8")
        (RES / f"{name}-holes.json").write_text(json.dumps(holes, indent=1), encoding="utf-8")
        print(f"{name}: {len(holes)} holes")

        func_node = next(n for n in ast.walk(tree)
                         if isinstance(n, ast.FunctionDef) and n.name == f"generate_{name}_ipes_text")
        for var, text in named_strings(func_node).items():
            (RES / f"{name}-{var}.txt").write_text(text, encoding="utf-8")
            print(f"  {name}.{var}: {len(text)} chars")
        for var, (tpl, holes) in named_templates(func_node).items():
            (RES / f"{name}-{var}.tpl").write_text(tpl, encoding="utf-8")
            (RES / f"{name}-{var}-holes.json").write_text(json.dumps(holes, indent=1), encoding="utf-8")
            print(f"  {name}.{var}: template, {len(holes)} holes")
        for i, overrides in enumerate(sets):
            text = func(**overrides)
            (GOLDEN / f"{name}_{i}.ipes").write_text(text, encoding="utf-8")
        print(f"{name}: {len(sets)} goldens")
    # keep holes next to the templates for review
    print("templates + goldens written")


if __name__ == "__main__":
    main()
