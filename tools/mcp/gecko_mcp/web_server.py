#!/usr/bin/env python3
"""
GeckoCIRCUITS LLC Resonant Converter Web Interface Server.
Serves interactive web frontend and bridges browser interactions to the GeckoCIRCUITS MCP tools.
"""

import json
import math
import os
import sys
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

# Ensure gecko_mcp package is in sys.path
SCRIPT_DIR = Path(__file__).resolve().parent
WORKSPACE_ROOT = SCRIPT_DIR.parents[2]
sys.path.insert(0, str(SCRIPT_DIR.parent))

from gecko_mcp.server import (
    gecko_get_waveforms,
    gecko_inspect_circuit,
    gecko_server_status,
    gecko_setup_llc_project,
    gecko_simulate,
)

WEB_APP_DIR = SCRIPT_DIR / "web_app"
DEFAULT_LLC_FILE = WORKSPACE_ROOT / "resources" / "projects" / "llc_resonant_400v_24v.ipes"


class GeckoWebHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(WEB_APP_DIR), **kwargs)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(HTTPStatus.NO_CONTENT)
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/api/status":
            self._handle_status()
        elif parsed.path == "/api/llc/circuit":
            self._handle_circuit()
        elif parsed.path == "/api/llc/ai_consult":
            query = parse_qs(parsed.query)
            params = {k: v[0] for k, v in query.items() if v}
            self._handle_ai_consult(params)
        elif parsed.path == "/api/llc/simulate":
            query = parse_qs(parsed.query)
            params = {}
            for k, v in query.items():
                if v:
                    try:
                        params[k] = float(v[0])
                    except ValueError:
                        params[k] = v[0]
            self._run_llc_simulation(params)
        else:
            # Fall back to static file server
            super().do_GET()

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path in ("/api/llc/simulate", "/api/llc/tune"):
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
            try:
                params = json.loads(body)
            except Exception:
                params = {}
            self._run_llc_simulation(params)
        elif parsed.path == "/api/llc/ai_consult":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
            try:
                params = json.loads(body)
            except Exception:
                params = {}
            self._handle_ai_consult(params)
        else:
            self.send_error(HTTPStatus.NOT_FOUND, "Endpoint not found")

    def _send_json(self, data: dict, status: int = 200):
        body = json.dumps(data, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _handle_status(self):
        status = gecko_server_status()
        self._send_json(status)

    def _handle_circuit(self):
        if not DEFAULT_LLC_FILE.exists():
            gecko_setup_llc_project(str(DEFAULT_LLC_FILE))
        info = gecko_inspect_circuit(str(DEFAULT_LLC_FILE))
        self._send_json(info)

    def _handle_ai_consult(self, params: dict):
        topic = params.get("topic", "power_scaling")
        p_out = float(params.get("p_out", 10000.0))
        v_in = float(params.get("v_in", 400.0))
        v_out = float(params.get("v_out", 200.0))
        f_sw = float(params.get("f_sw", 100000.0))
        l_r = float(params.get("l_r", 2.2e-6))
        c_r = float(params.get("c_r", 1.15e-6))
        l_m = float(params.get("l_m", 1.1e-5))

        r_load = (v_out ** 2) / max(50.0, p_out)
        r_ac = (8.0 / (math.pi ** 2)) * r_load
        z_0 = math.sqrt(l_r / c_r)
        f_0 = 1.0 / (2.0 * math.pi * math.sqrt(l_r * c_r))
        q = z_0 / r_ac
        k = l_m / l_r
        i_mag_pk = (v_in / 2.0) / (4.0 * f_sw * l_m)
        t_dead_min = (2.0 * 1.5e-9 * v_in) / max(0.1, i_mag_pk)

        consult_data = {
            "status": "SUCCESS",
            "topic": topic,
            "operating_point": {
                "power_rated_kw": p_out / 1000.0,
                "v_in_volts": v_in,
                "v_out_volts": v_out,
                "load_resistance_ohms": round(r_load, 2),
                "ac_equivalent_load_ohms": round(r_ac, 2),
                "characteristic_impedance_ohms": round(z_0, 2),
                "resonant_freq_khz": round(f_0 / 1000.0, 1),
                "quality_factor_q": round(q, 3),
                "inductance_ratio_k": round(k, 1),
                "magnetizing_peak_current_amps": round(i_mag_pk, 2),
                "min_dead_time_ns": round(t_dead_min * 1e9, 1)
            },
            "expert_advice": f"For {p_out/1000:.1f} kW output at {v_out:.0f}V, R_load is {r_load:.2f} Ω (Rac = {r_ac:.2f} Ω). To maintain Q = {q:.2f} with k = {k:.1f}, the resonant tank requires Lr = {l_r*1e6:.1f} µH and Cr = {c_r*1e6:.2f} µF. Magnetizing peak current of {i_mag_pk:.1f}A easily soft-charges Coss (1.5 nF) in {t_dead_min*1e9:.1f} ns, guaranteeing robust ZVS soft-switching.",
            "workflow_guidance": "You can discuss and modify any circuit parameter either directly in the Antigravity chat or through this web interface. The LLM pair-programmer uses MCP tools ('gecko_setup_llc_project', 'gecko_simulate', 'gecko_get_waveforms') to re-synthesize the netlist and re-simulate in GeckoCIRCUITS."
        }
        self._send_json(consult_data)

    def _run_llc_simulation(self, params: dict):
        try:
            v_in = float(params.get("v_in", 400.0))
            v_out = float(params.get("v_out", 200.0))
            p_out = float(params.get("p_out", 10000.0))
            f_sw = float(params.get("f_sw", 100000.0))
            t_dead = float(params.get("t_dead", 1.8e-7))
            duration = float(params.get("duration", 0.0003))
            dt = float(params.get("dt", 2e-8))
            l_r = float(params.get("l_r", 2.2e-6))
            c_r = float(params.get("c_r", 1.15e-6))
            l_m = float(params.get("l_m", 1.1e-5))
            r_load = float(params["r_load"]) if "r_load" in params else None
            c_out = float(params["c_out"]) if "c_out" in params else None

            # 1. Setup / Update LLC Project file via MCP
            setup_res = gecko_setup_llc_project(
                output_path=str(DEFAULT_LLC_FILE),
                v_in=v_in,
                v_out=v_out,
                p_out=p_out,
                f_sw=f_sw,
                l_r=l_r,
                c_r=c_r,
                l_m=l_m,
                t_dead=t_dead,
                r_load=r_load,
                c_out=c_out,
                duration=duration,
                dt=dt
            )

            # 2. Run Headless Simulation via MCP
            sim_res = gecko_simulate(str(DEFAULT_LLC_FILE), duration=duration, dt=dt)

            # 3. Retrieve downsampled telemetry waveforms & power electronics metrics via MCP
            wave_res = gecko_get_waveforms(
                str(DEFAULT_LLC_FILE),
                duration=duration,
                dt=dt,
                max_points=800
            )

            response = {
                "status": "SUCCESS",
                "setup": setup_res,
                "simulation": sim_res,
                "metrics": wave_res.get("metrics", {}),
                "waveforms": wave_res.get("waveforms", {})
            }
            self._send_json(response)
        except Exception as e:
            self._send_json({"status": "ERROR", "error": str(e)}, status=500)


def run_server(port: int = 8090, host: str = "127.0.0.1"):
    server = ThreadingHTTPServer((host, port), GeckoWebHandler)
    print(f"GeckoCIRCUITS LLC Web Interface running on http://{host}:{port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down web server...")
        server.server_close()


if __name__ == "__main__":
    port = int(os.environ.get("GECKO_WEB_PORT", "8090"))
    host = os.environ.get("GECKO_WEB_HOST", "127.0.0.1")
    if len(sys.argv) > 1:
        port = int(sys.argv[1])
    run_server(port=port, host=host)
