#!/usr/bin/env python3
"""
Remote Control Client for GeckoCIRCUITS MCP Server.
Demonstrates how an LLM agent (Google Antigravity, Copilot, etc.) drives GeckoCIRCUITS:
1. Connects to the gecko-circuits MCP Server via standard stdio transport.
2. Discovers tools via MCP list_tools.
3. Sets up the Interleaved PFC project (.ipes file) via gecko_setup_pfc_project.
4. Inspects circuit topology, netlist, and microcontroller script via gecko_inspect_circuit.
5. Executes the simulation via gecko_simulate.
6. Retrieves telemetry, output DC voltage, ripple, and power factor via gecko_get_waveforms.
7. Evaluates closed-loop regulation via gecko_tune_pfc.
"""

import asyncio
import json
import sys
from pathlib import Path
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
SERVER_SCRIPT = WORKSPACE_ROOT / "tools" / "mcp" / "gecko_mcp" / "server.py"


async def main():
    print("=" * 80)
    print(" GECKOCIRCUITS MCP REMOTE CONTROL CLIENT")
    print(" Connecting LLM Harness to GeckoCIRCUITS via Model Context Protocol")
    print("=" * 80)

    server_params = StdioServerParameters(
        command="uv",
        args=["run", "--with", "mcp<2", "python", str(SERVER_SCRIPT)],
        cwd=str(WORKSPACE_ROOT)
    )

    async with stdio_client(server_params) as (read_stream, write_stream):
        async with ClientSession(read_stream, write_stream) as session:
            # 1. MCP Handshake & Protocol Initialization
            print("\n[MCP Step 1] Initializing MCP Session...")
            init_res = await session.initialize()
            print(f" -> MCP Server connected: {init_res.serverInfo.name} v{init_res.serverInfo.version}")

            # 2. Discover MCP Tools
            print("\n[MCP Step 2] Discovering Available GeckoCIRCUITS Tools...")
            tools_list = await session.list_tools()
            print(f" -> Found {len(tools_list.tools)} MCP tools:")
            for t in tools_list.tools:
                first_line = t.description.strip().split("\n")[0] if t.description else ""
                print(f"    * {t.name:<25}: {first_line}")

            # 3. Check Gecko Server Status
            print("\n[MCP Step 3] Calling 'gecko_server_status'...")
            res = await session.call_tool("gecko_server_status", {})
            status_data = json.loads(res.content[0].text) if hasattr(res.content[0], "text") else res.content[0]
            print(f" -> Status: {status_data.get('status')}")
            print(f" -> Java Executable: {status_data.get('java_executable')}")
            print(f" -> Java Version: {status_data.get('java_version')}")
            print(f" -> Engine Capabilities: {', '.join(status_data.get('capabilities', []))}")

            # 4. Setup .ipes Project File via MCP
            project_path = "resources/projects/interleaved_pfc_50v.ipes"
            print(f"\n[MCP Step 4] Calling 'gecko_setup_pfc_project' (Output: {project_path})...")
            setup_args = {
                "output_path": project_path,
                "target_voltage": 50.0,
                "v_rms": 24.0,
                "f_grid": 50.0,
                "f_sw": 20000.0,
                "inductance": 0.0008,
                "capacitance": 0.0068,
                "r_load_base": 25.0,
                "r_load_step": 25.0,
                "t_step": 0.05,
                "duration": 0.10,
                "dt": 1e-6
            }
            res = await session.call_tool("gecko_setup_pfc_project", setup_args)
            setup_data = json.loads(res.content[0].text)
            print(f" -> Project Created: {setup_data.get('file')}")
            print(f" -> File Size: {setup_data.get('file_size_bytes')} bytes (gzipped .ipes)")
            print(f" -> Topology: {setup_data.get('topology')}")
            print(f" -> Power Components: {setup_data.get('power_components_count')}, Control Components: {setup_data.get('control_components_count')}")

            # 5. Inspect Circuit File via MCP
            print(f"\n[MCP Step 5] Calling 'gecko_inspect_circuit' on '{project_path}'...")
            res = await session.call_tool("gecko_inspect_circuit", {"circuit_path": project_path})
            inspect_data = json.loads(res.content[0].text)
            print(f" -> dt: {inspect_data.get('dt')}s, Duration: {inspect_data.get('duration')}s")
            print(f" -> LK Power Elements ({inspect_data.get('lk_component_count')}):")
            for comp in inspect_data.get("lk_components", []):
                params_str = ", ".join(f"{p:g}" for p in comp.get("parameters", [])[:3])
                print(f"    - {comp.get('name'):<8} (typ {comp.get('type'):<2}): In={comp.get('in_nodes'):<4} Out={comp.get('out_nodes'):<6} Params=[{params_str}]")

            print(f" -> Control Elements ({inspect_data.get('control_component_count')}):")
            for comp in inspect_data.get("control_components", []):
                extra = f"coupledReferenceID={comp.get('coupled_id')}" if comp.get('coupled_id') else f"typ={comp.get('type')}"
                print(f"    - {comp.get('name'):<10}: {extra}")

            # 6. Run Simulation via MCP
            print(f"\n[MCP Step 6] Calling 'gecko_simulate'...")
            res = await session.call_tool("gecko_simulate", {
                "circuit_path": project_path,
                "duration": 0.10,
                "dt": 1e-6
            })
            sim_data = json.loads(res.content[0].text)
            print(f" -> Simulation Status: {sim_data.get('status')}")
            print(f" -> Total Steps Computed: {sim_data.get('total_steps'):,}")
            print(f" -> Logged Signals: {', '.join(sim_data.get('signal_names', []))}")

            # 7. Get Waveforms & Key Metrics via MCP
            print(f"\n[MCP Step 7] Calling 'gecko_get_waveforms'...")
            res = await session.call_tool("gecko_get_waveforms", {
                "circuit_path": project_path,
                "duration": 0.10,
                "dt": 1e-6,
                "max_points": 1000
            })
            wave_data = json.loads(res.content[0].text)
            metrics = wave_data.get("metrics", {})
            vout = metrics.get("output_voltage", {})
            grid = metrics.get("ac_grid", {})
            print(f" -> Telemetry Points Returned: {wave_data.get('returned_points')}")
            print(f" -> Output Voltage DC: {vout.get('steady_state_dc_volts')} V (Target: 50.0 V)")
            print(f" -> Peak-to-Peak Ripple: {vout.get('ripple_peak_to_peak_volts')} V ({vout.get('ripple_percentage')}%)")
            print(f" -> AC Grid RMS: {grid.get('v_rms')} V_rms, Grid Current: {grid.get('i_rms')} A_rms")
            print(f" -> Active Power: {grid.get('active_power_watts')} W, Apparent: {grid.get('apparent_power_va')} VA")
            print(f" -> Power Factor (PF): {grid.get('power_factor')}")

            # 8. Tune & Closed-Loop Evaluation via MCP
            print(f"\n[MCP Step 8] Calling 'gecko_tune_pfc' (Target: 50.0V)...")
            res = await session.call_tool("gecko_tune_pfc", {
                "circuit_path": project_path,
                "target_voltage": 50.0,
                "simulation_time": 0.10
            })
            tune_data = json.loads(res.content[0].text)
            print(f" -> Measured Voltage: {tune_data.get('measured_voltage_volts')} V")
            print(f" -> Regulation Error: {tune_data.get('voltage_error_volts')} V")
            print(f" -> Tuning Evaluation:")
            for eval_line in tune_data.get("evaluation", []):
                print(f"    * {eval_line}")

            print("\n" + "=" * 80)
            print(" MCP REMOTE CONTROL DEMONSTRATION COMPLETED SUCCESSFULLY!")
            print("=" * 80)


if __name__ == "__main__":
    asyncio.run(main())
