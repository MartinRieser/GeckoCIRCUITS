#!/usr/bin/env python3
"""Example-circuit quality gate for the GeckoCIRCUITS web editor.

Extracts every example embedded in frontend/src/model/examples.ts, runs it
headlessly through the REST simulation engine, and asserts that

  1. the simulation completes,
  2. every signal named in dataContainerSignals[] is recorded,
  3. the time vector spans the file's tDURATION,
  4. recorded signals are finite and non-degenerate (no NaN blow-ups, no
     stuck-at-constant traces),
  5. per-example physics plausibility holds (steady-state windows, ripple
     bounds, power factor) - see ASSERTIONS below.

Exit code 0 = all examples PASS. This is the gate that must run before any
change to examples.ts (or to the engine semantics examples rely on) is
declared done; see .agents/skills/example-verification/SKILL.md.

Usage:
  python scripts/verify-examples.py                     # all examples
  python scripts/verify-examples.py --only pfc-boost    # subset
  python scripts/verify-examples.py --base-url http://localhost:8080

Requires a running backend:
  mvn -f pom.xml -pl backend/gecko-rest-api -am package -DskipTests
  java -jar backend/gecko-rest-api/target/gecko-rest-api.jar
"""

import argparse
import base64
import json
import math
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
EXAMPLES_TS = REPO / "frontend" / "src" / "model" / "examples.ts"
DEFAULT_BASE = "http://localhost:8080"
RUN_TIMEOUT_S = 300
POLL_INTERVAL_S = 0.5


# --------------------------------------------------------------------------
# Plausibility assertions per example id.
#
# Each check is (signal, window_fraction, function, min, max, label) where
# window_fraction selects the tail of the run (steady state) and function is
# applied to that window's samples. Windows are deliberately generous: the
# gate must catch broken physics (limit cycles, blow-ups, wrong topology),
# not quantise engineering taste.
# --------------------------------------------------------------------------
def _mean(x):
    return sum(x) / len(x)


def _pp(x):
    return max(x) - min(x)


def _pf(volt, curr, v_rms_expected):
    """Displacement/shape power factor of a rectified-current PFC stage."""
    p = sum(abs(u) * i for u, i in zip(volt, curr)) / len(volt)
    i_rms = math.sqrt(sum(i * i for i in curr) / len(curr))
    return p / (v_rms_expected * i_rms)


ASSERTIONS = {
    # 24 V buck at D=0.5 -> ~12 V; diode drop and R_ds losses keep it slightly low
    "buck": [("v_out", 0.4, _mean, 10.0, 14.0, "steady-state mean ~12 V")],
    "sync-buck": [("v_out", 0.4, _mean, 10.0, 14.0, "steady-state mean ~12 V")],
    # 12 V boost at D=0.5 -> ~24 V (12 ms covers the startup overshoot)
    "boost": [("v_out", 0.25, _mean, 21.0, 27.0, "settled mean ~24 V")],
    # 24 V inverting buck-boost at D=0.40 -> ~-16 V
    "buck-boost": [("v_out", 0.4, _mean, -19.0, -13.0, "steady-state mean ~-16 V")],
    # closed-loop PI regulates 12.0 V
    "closed-loop-buck": [("v_out_scope", 0.5, _mean, 11.0, 13.0, "regulated 12 V")],
    # Graetz bridge on 24 Vrms (34 Vpk): bus sits just below the rectified peak
    "rectifier": [
        ("v_ac", 1.0, _pp, 60.0, 70.0, "AC swing ~68 Vpp (34 Vpk sine)"),
        ("v_dc", 0.3, _mean, 29.0, 34.0, "bus ~32.6 V (peak - 2 diode drops)"),
    ],
    # second-order RLC step: capacitor charges to the 24 V step (with ringing)
    "rlc": [("v_out", 0.2, _mean, 21.0, 27.0, "settles to 24 V step")],
    # first-order RC step: capacitor charges to 10 V, never above
    "rc": [
        ("v_out", 0.2, _mean, 8.0, 12.0, "settles to 10 V step"),
        ("v_out", 1.0, lambda x: max(x), 0.0, 10.5, "no overshoot (first order)"),
    ],
    # classic reference: 100 V step, R=10, C=100u (tau=1 ms, 5 ms = 5 tau)
    "rc-classic": [("u_out", 0.2, _mean, 90.0, 100.0, "settles to 100 V step (5 tau)")],
    # series RLC step in the multi-scope demo: settles to the 24 V step
    "three-scopes": [("v_out", 0.2, _mean, 21.0, 27.0, "settles to 24 V step")],
    # PFC: 50 V bus, bounded ripple, near-unity power factor (24 Vrms grid)
    "pfc-boost": [
        ("v_dc_meas", 0.6, _mean, 48.0, 52.0, "bus regulated to 50 V"),
        ("v_dc_meas", 0.6, _pp, 0.1, 20.0, "100 Hz ripple bounded (<20 Vpp)"),
        ("i_l", 0.6, _mean, 3.0, 6.0, "mean rectified current ~P/V_rec"),
    ],
}


# --------------------------------------------------------------------------
# examples.ts extraction
# --------------------------------------------------------------------------
def load_examples():
    src = EXAMPLES_TS.read_text(encoding="utf-8")
    consts = {m.group(1): m.group(2) for m in re.finditer(r"export const (\w+) = `(.*?)`;", src, re.S)}
    examples = []
    for m in re.finditer(r"id: '([\w-]+)',[\s\S]*?content: (\w+),", src):
        eid, const = m.group(1), m.group(2)
        if const not in consts:
            continue
        body = consts[const].replace("\\\\", "\\").lstrip("\n")
        sig_match = re.search(r"dataContainerSignals\[\][ ]?(/[^\n]*)", body)
        dur_match = re.search(r"tDURATION[ ]?([\d.eE+-]+)", body)
        signals = [s for s in (sig_match.group(1) if sig_match else "").strip().split("/") if s]
        examples.append({
            "id": eid,
            "content": body,
            "signals": signals,
            "duration": float(dur_match.group(1)) if dur_match else None,
        })
    return examples


# --------------------------------------------------------------------------
# REST engine
# --------------------------------------------------------------------------
def http_json(url, payload=None, timeout=60):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(
        url, data=data,
        headers={"Content-Type": "application/json"} if data else {},
        method="POST" if data is not None else "GET",
    )
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.load(r)


def server_alive(base):
    try:
        http_json(base + "/gecko/api/v1/circuits/catalog", timeout=5)
        return True
    except Exception:
        return False


def run_simulation(base, ipes_text):
    body = {"base64Circuit": base64.b64encode(ipes_text.encode("utf-8")).decode()}
    resp = http_json(base + "/gecko/api/v1/simulations", body)
    sid = resp["simulationId"]
    deadline = time.monotonic() + RUN_TIMEOUT_S
    while time.monotonic() < deadline:
        st = http_json(f"{base}/gecko/api/v1/simulations/{sid}")
        if st.get("status") in ("COMPLETED", "FAILED", "CANCELLED", "ERROR"):
            return st
        time.sleep(POLL_INTERVAL_S)
    raise TimeoutError(f"simulation {sid} did not finish within {RUN_TIMEOUT_S}s")


# --------------------------------------------------------------------------
# checks
# --------------------------------------------------------------------------
def check_example(ex, results):
    problems = []
    if not results:
        return ["no results returned"]

    t = results.get("time")
    if not t:
        return ["no time vector in results"]

    if ex["duration"]:
        if t[-1] < 0.9 * ex["duration"]:
            problems.append(f"time span {t[-1]:.4g}s << tDURATION {ex['duration']:.4g}s")

    for sig in ex["signals"]:
        data = results.get(sig)
        if data is None:
            problems.append(f"recorded signal '{sig}' missing from results")
            continue
        if any(not math.isfinite(v) for v in data):
            problems.append(f"signal '{sig}' contains NaN/inf")
            continue
        if min(data) == max(data):
            # A constant NONZERO trace is legitimate physics (DC input node of
            # a step-response example). Constant zero means the probe/script
            # tap is dangling - the signal name has no producer.
            if abs(max(data)) < 1e-9:
                problems.append(f"signal '{sig}' is stuck at 0 (dangling tap?)")
            continue
        peak = max(abs(v) for v in data)
        if peak > 1e6:
            problems.append(f"signal '{sig}' blew up (|max|={peak:.3g})")

    for sig, tail, fn, lo, hi, label in ASSERTIONS.get(ex["id"], []):
        data = results.get(sig)
        if not data:
            continue  # missing-signal already reported above
        n = len(data)
        window = data[int(n * (1 - tail)):]
        value = fn(window)
        ok = lo <= value <= hi
        mark = "OK " if ok else "FAIL"
        print(f"    [{mark}] {sig}: {label} = {value:.3g} (allowed {lo}..{hi})")
        if not ok:
            problems.append(f"plausibility: {sig} {label} = {value:.3g} outside [{lo}, {hi}]")

    # PFC power factor (needs both signals present)
    if ex["id"] == "pfc-boost":
        u, i = results.get("u_grid"), results.get("i_l")
        if u and i:
            n = min(len(u), len(i))
            tail = int(n * 0.4)
            pf = _pf(u[tail:n], i[tail:n], 24.0)
            ok = pf > 0.9
            print(f"    [{'OK ' if ok else 'FAIL'}] power factor = {pf:.3f} (>0.9)")
            if not ok:
                problems.append(f"plausibility: power factor {pf:.3f} <= 0.9")

    return problems


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--base-url", default=DEFAULT_BASE)
    ap.add_argument("--only", help="comma-separated example ids")
    args = ap.parse_args()

    if not EXAMPLES_TS.exists():
        sys.exit(f"examples file not found: {EXAMPLES_TS}")
    examples = load_examples()
    if args.only:
        wanted = {s.strip() for s in args.only.split(",")}
        examples = [e for e in examples if e["id"] in wanted]
    if not examples:
        sys.exit("no examples matched")

    if not server_alive(args.base_url):
        sys.exit(
            f"backend not reachable at {args.base_url}\n"
            "start it first:\n"
            "  mvn -f pom.xml -pl backend/gecko-rest-api -am package -DskipTests\n"
            "  java -jar backend/gecko-rest-api/target/gecko-rest-api.jar"
        )

    failures = 0
    for ex in examples:
        print(f"== {ex['id']} ({', '.join(ex['signals']) or 'no recorded signals'}) ...", flush=True)
        try:
            st = run_simulation(args.base_url, ex["content"])
        except Exception as e:  # noqa: BLE001 - report and continue with others
            print(f"    FAIL simulation could not run: {e}")
            failures += 1
            continue
        if st.get("status") != "COMPLETED":
            msg = (st.get("errorMessage") or "")[:200]
            print(f"    FAIL status={st.get('status')} {msg}")
            failures += 1
            continue
        problems = check_example(ex, st.get("results") or {})
        if problems:
            failures += 1
            for p in problems:
                print(f"    FAIL {p}")
        else:
            print("    PASS")

    print(f"\n{'ALL PASS' if failures == 0 else str(failures) + ' EXAMPLE(S) FAILED'} "
          f"({len(examples)} checked)")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
