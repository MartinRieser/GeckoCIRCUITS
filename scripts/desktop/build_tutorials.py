#!/usr/bin/env python3
"""Tutorial Project Builder for GeckoCIRCUITS.

Builds, validates, and simulates authentic .ipes project files for
tutorials in docs/tutorials/ (acdc, dcac, dcdc).
"""

import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.desktop.mcp_client import GeckoMcpClient


def build_single_phase_rectifier(client: GeckoMcpClient):
    print("\n--- 1. Single-Phase Diode Bridge Rectifier (docs/tutorials/acdc/single-phase-rectifier.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/acdc/single-phase-rectifier.ipes",
        "simulation": {
            "duration": 0.06,
            "dt": 1e-5,
            "solver": 0,
        },
        "components": [
            {
                "name": "VAC",
                "type": "VOLTAGE_SOURCE_AC",
                "nodes": ["ac_p", "ac_n"],
                "parameters": {"amplitude": 325.27, "frequency": 50.0, "phase_deg": 0.0},
            },
            {
                "name": "D1",
                "type": "DIODE",
                "nodes": ["ac_p", "dc_p"],
                "parameters": {"u_forward": 0.7, "r_on": 0.01},
            },
            {
                "name": "D2",
                "type": "DIODE",
                "nodes": ["ac_n", "dc_p"],
                "parameters": {"u_forward": 0.7, "r_on": 0.01},
            },
            {
                "name": "D3",
                "type": "DIODE",
                "nodes": ["0", "ac_p"],
                "parameters": {"u_forward": 0.7, "r_on": 0.01},
            },
            {
                "name": "D4",
                "type": "DIODE",
                "nodes": ["0", "ac_n"],
                "parameters": {"u_forward": 0.7, "r_on": 0.01},
            },
            {
                "name": "C_FILT",
                "type": "CAPACITOR",
                "nodes": ["dc_p", "0"],
                "parameters": {"capacitance": 1000e-6},
            },
            {
                "name": "R_LOAD",
                "type": "RESISTOR",
                "nodes": ["dc_p", "0"],
                "parameters": {"resistance": 100.0},
            },
        ],
        "control": {
            "probes": [
                {"name": "VM_AC", "type": "VOLTMETER", "target_component": "VAC", "signal_name": "v_ac"},
                {"name": "VM_DC", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_dc"},
                {"name": "AM_LOAD", "type": "AMMETER", "target_component": "R_LOAD", "signal_name": "i_load"},
            ]
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/acdc/single-phase-rectifier.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/acdc/single-phase-rectifier.ipes", "duration": 0.06, "dt": 1e-5},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))

    waves = client.call_tool(
        "gecko_get_waveforms",
        {"circuit_path": "docs/tutorials/acdc/single-phase-rectifier.ipes", "duration": 0.06, "dt": 1e-5},
    )
    for sig, stats in waves.get("statistics", {}).items():
        print(f"  {sig}: mean={stats['mean']:.2f}, min={stats['min']:.2f}, max={stats['max']:.2f}, pp={stats['peak_to_peak']:.2f}")


def build_three_phase_rectifier(client: GeckoMcpClient):
    print("\n--- 2. Three-Phase Diode Bridge Rectifier (docs/tutorials/acdc/three-phase-rectifier.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/acdc/three-phase-rectifier.ipes",
        "simulation": {
            "duration": 0.04,
            "dt": 1e-5,
            "solver": 0,
        },
        "components": [
            {
                "name": "V_A",
                "type": "VOLTAGE_SOURCE_AC",
                "nodes": ["va", "0"],
                "parameters": {"amplitude": 326.6, "frequency": 50.0, "phase_deg": 0.0},
            },
            {
                "name": "V_B",
                "type": "VOLTAGE_SOURCE_AC",
                "nodes": ["vb", "0"],
                "parameters": {"amplitude": 326.6, "frequency": 50.0, "phase_deg": -120.0},
            },
            {
                "name": "V_C",
                "type": "VOLTAGE_SOURCE_AC",
                "nodes": ["vc", "0"],
                "parameters": {"amplitude": 326.6, "frequency": 50.0, "phase_deg": 120.0},
            },
            {"name": "D1", "type": "DIODE", "nodes": ["va", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D2", "type": "DIODE", "nodes": ["0", "va"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D3", "type": "DIODE", "nodes": ["vb", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D4", "type": "DIODE", "nodes": ["0", "vb"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D5", "type": "DIODE", "nodes": ["vc", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D6", "type": "DIODE", "nodes": ["0", "vc"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "C_FILT", "type": "CAPACITOR", "nodes": ["dc_p", "0"], "parameters": {"capacitance": 470e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["dc_p", "0"], "parameters": {"resistance": 50.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_VA", "type": "VOLTMETER", "target_component": "V_A", "signal_name": "v_phase_a"},
                {"name": "VM_DC", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_dc"},
                {"name": "AM_LOAD", "type": "AMMETER", "target_component": "R_LOAD", "signal_name": "i_load"},
            ]
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/acdc/three-phase-rectifier.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/acdc/three-phase-rectifier.ipes", "duration": 0.04, "dt": 1e-5},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))

    waves = client.call_tool(
        "gecko_get_waveforms",
        {"circuit_path": "docs/tutorials/acdc/three-phase-rectifier.ipes", "duration": 0.04, "dt": 1e-5},
    )
    for sig, stats in waves.get("metrics", {}).get("signal_statistics", {}).items():
        print(f"  {sig}: mean={stats['mean']:.2f}, min={stats['min']:.2f}, max={stats['max']:.2f}, pp={stats['peak_to_peak']:.2f}")


def build_boost_pfc(client: GeckoMcpClient):
    print("\n--- 3. Active Boost PFC (docs/tutorials/acdc/boost-pfc.ipes) ---")
    pfc_res = client.call_tool(
        "gecko_setup_pfc_project",
        {
            "output_path": "docs/tutorials/acdc/boost-pfc.ipes",
            "target_voltage": 400.0,
            "v_rms": 230.0,
            "f_grid": 50.0,
            "f_sw": 65000.0,
            "inductance": 0.0004,
            "capacitance": 0.00047,
            "r_load_base": 400.0,
            "r_load_step": 400.0,
            "t_step": 0.05,
            "duration": 0.06,
            "dt": 1e-6,
        },
    )
    print("Setup PFC:", pfc_res.get("status"), "topology:", pfc_res.get("topology"))

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/acdc/boost-pfc.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/acdc/boost-pfc.ipes", "duration": 0.06, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_vienna_rectifier(client: GeckoMcpClient):
    print("\n--- 4. Vienna Rectifier (docs/tutorials/acdc/vienna-rectifier.ipes) ---")
    import shutil
    src = REPO_ROOT / "resources/tutorials/3xx_acdc_rectifiers/303_vienna_rectifier/three-phase_ViennaRectifier_simpleControl_250kW.ipes"
    dst = REPO_ROOT / "docs/tutorials/acdc/vienna-rectifier.ipes"
    shutil.copy2(src, dst)
    print("Copied authentic 3-phase Vienna Rectifier project to docs/tutorials/acdc/vienna-rectifier.ipes")

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/acdc/vienna-rectifier.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/acdc/vienna-rectifier.ipes", "duration": 0.02, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals count:", len(sim.get("signal_names", [])))


def build_smps_230v_to_3v3(client: GeckoMcpClient):
    print("\n--- 5. 230V AC to 3.3V DC SMPS (docs/tutorials/acdc/smps-230v-to-3v3.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/acdc/smps-230v-to-3v3.ipes",
        "simulation": {
            "duration": 0.05,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            {
                "name": "V_IN",
                "type": "VOLTAGE_SOURCE_AC",
                "nodes": ["mains_p", "mains_n"],
                "parameters": {"amplitude": 325.27, "frequency": 50.0, "phase_deg": 0.0},
            },
            {
                "name": "TRANS_1",
                "type": "TRANSFORMER",
                "nodes": ["mains_p", "mains_n", "sec_p", "sec_n"],
                "parameters": {"n1": 19.0, "n2": 1.0, "polarity": -1.0},
            },
            {"name": "D1", "type": "DIODE", "nodes": ["sec_p", "dclink"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D2", "type": "DIODE", "nodes": ["sec_n", "dclink"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D3", "type": "DIODE", "nodes": ["0", "sec_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D4", "type": "DIODE", "nodes": ["0", "sec_n"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "C_BULK", "type": "CAPACITOR", "nodes": ["dclink", "0"], "parameters": {"capacitance": 4700e-6}},
            {"name": "S_1", "type": "SWITCH", "nodes": ["dclink", "sw_node"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D_FW", "type": "DIODE", "nodes": ["0", "sw_node"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "L_1", "type": "INDUCTOR", "nodes": ["sw_node", "out"], "parameters": {"inductance": 33e-6}},
            {"name": "C_1", "type": "CAPACITOR", "nodes": ["out", "0"], "parameters": {"capacitance": 470e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["out", "0"], "parameters": {"resistance": 0.55}},
        ],
        "control": {
            "probes": [
                {"name": "VM_SEC", "type": "VOLTMETER", "target_component": "C_BULK", "signal_name": "v_dclink"},
                {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_out"},
                {"name": "AM_LOAD", "type": "AMMETER", "target_component": "R_LOAD", "signal_name": "i_load"},
            ],
            "script_blocks": [
                {
                    "name": "PWM_GEN",
                    "in_signals": ["v_out"],
                    "out_signals": ["s_pwm"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_sw = 50000.0;
                            phase += dt * f_sw;
                            if (phase >= 1.0) phase -= 1.0;
                            yOUT[0] = (phase < 0.22) ? 1.0 : 0.0;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "GATE_1", "target_switch": "S_1", "in_signal": "s_pwm"}
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/acdc/smps-230v-to-3v3.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/acdc/smps-230v-to-3v3.ipes", "duration": 0.05, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_single_phase_inverter(client: GeckoMcpClient):
    print("\n--- 6. Single-Phase Inverter (docs/tutorials/dcac/single-phase-inverter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcac/single-phase-inverter.ipes",
        "simulation": {
            "duration": 0.04,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            {"name": "V_DC", "type": "VOLTAGE_SOURCE_DC", "nodes": ["dc_p", "0"], "parameters": {"voltage": 400.0}},
            {"name": "S1", "type": "SWITCH", "nodes": ["dc_p", "sw_a"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D1", "type": "DIODE", "nodes": ["sw_a", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "S2", "type": "SWITCH", "nodes": ["sw_a", "0"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D2", "type": "DIODE", "nodes": ["0", "sw_a"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "S3", "type": "SWITCH", "nodes": ["dc_p", "sw_b"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D3", "type": "DIODE", "nodes": ["sw_b", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "S4", "type": "SWITCH", "nodes": ["sw_b", "0"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D4", "type": "DIODE", "nodes": ["0", "sw_b"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "L_FILT", "type": "INDUCTOR", "nodes": ["sw_a", "out_a"], "parameters": {"inductance": 2e-3}},
            {"name": "C_FILT", "type": "CAPACITOR", "nodes": ["out_a", "sw_b"], "parameters": {"capacitance": 10e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["out_a", "sw_b"], "parameters": {"resistance": 20.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_AC", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_ac"},
                {"name": "AM_LOAD", "type": "AMMETER", "target_component": "R_LOAD", "signal_name": "i_load"},
                {"name": "VM_DC", "type": "VOLTMETER", "target_component": "V_DC", "signal_name": "v_dc"},
            ],
            "script_blocks": [
                {
                    "name": "SPWM",
                    "in_signals": ["v_ac"],
                    "out_signals": ["g1", "g2", "g3", "g4"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_line = 50.0;
                            double f_carrier = 20000.0;
                            double m_a = 0.85;
                            phase += dt * f_carrier;
                            if (phase >= 1.0) phase -= 1.0;
                            double tri = 2.0 * Math.abs(phase - 0.5);
                            double ref_a = 0.5 * (1.0 + m_a * Math.sin(2.0 * PI * f_line * t));
                            double ref_b = 0.5 * (1.0 - m_a * Math.sin(2.0 * PI * f_line * t));
                            double s1 = (ref_a > tri) ? 1.0 : 0.0;
                            double s2 = 1.0 - s1;
                            double s3 = (ref_b > tri) ? 1.0 : 0.0;
                            double s4 = 1.0 - s3;
                            yOUT[0] = s1;
                            yOUT[1] = s2;
                            yOUT[2] = s3;
                            yOUT[3] = s4;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "GATE_1", "target_switch": "S1", "in_signal": "g1"},
                {"name": "GATE_2", "target_switch": "S2", "in_signal": "g2"},
                {"name": "GATE_3", "target_switch": "S3", "in_signal": "g3"},
                {"name": "GATE_4", "target_switch": "S4", "in_signal": "g4"},
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcac/single-phase-inverter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcac/single-phase-inverter.ipes", "duration": 0.04, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_three_phase_inverter(client: GeckoMcpClient):
    print("\n--- 7. Three-Phase Inverter (docs/tutorials/dcac/three-phase-inverter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcac/three-phase-inverter.ipes",
        "simulation": {
            "duration": 0.04,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            {"name": "V_DC", "type": "VOLTAGE_SOURCE_DC", "nodes": ["dc_p", "0"], "parameters": {"voltage": 700.0}},
            # Leg A
            {"name": "S1", "type": "SWITCH", "nodes": ["dc_p", "sw_a"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D1", "type": "DIODE", "nodes": ["sw_a", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "S4", "type": "SWITCH", "nodes": ["sw_a", "0"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D4", "type": "DIODE", "nodes": ["0", "sw_a"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            # Leg B
            {"name": "S3", "type": "SWITCH", "nodes": ["dc_p", "sw_b"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D3", "type": "DIODE", "nodes": ["sw_b", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "S6", "type": "SWITCH", "nodes": ["sw_b", "0"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D6", "type": "DIODE", "nodes": ["0", "sw_b"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            # Leg C
            {"name": "S5", "type": "SWITCH", "nodes": ["dc_p", "sw_c"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D5", "type": "DIODE", "nodes": ["sw_c", "dc_p"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "S2", "type": "SWITCH", "nodes": ["sw_c", "0"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D2", "type": "DIODE", "nodes": ["0", "sw_c"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            # Filter inductors
            {"name": "LA", "type": "INDUCTOR", "nodes": ["sw_a", "out_a"], "parameters": {"inductance": 2e-3}},
            {"name": "LB", "type": "INDUCTOR", "nodes": ["sw_b", "out_b"], "parameters": {"inductance": 2e-3}},
            {"name": "LC", "type": "INDUCTOR", "nodes": ["sw_c", "out_c"], "parameters": {"inductance": 2e-3}},
            # Filter capacitors
            {"name": "CA", "type": "CAPACITOR", "nodes": ["out_a", "star_n"], "parameters": {"capacitance": 10e-6}},
            {"name": "CB", "type": "CAPACITOR", "nodes": ["out_b", "star_n"], "parameters": {"capacitance": 10e-6}},
            {"name": "CC", "type": "CAPACITOR", "nodes": ["out_c", "star_n"], "parameters": {"capacitance": 10e-6}},
            # Star load
            {"name": "RA", "type": "RESISTOR", "nodes": ["out_a", "star_n"], "parameters": {"resistance": 20.0}},
            {"name": "RB", "type": "RESISTOR", "nodes": ["out_b", "star_n"], "parameters": {"resistance": 20.0}},
            {"name": "RC", "type": "RESISTOR", "nodes": ["out_c", "star_n"], "parameters": {"resistance": 20.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_PH_A", "type": "VOLTMETER", "target_component": "RA", "signal_name": "v_phase_a"},
                {"name": "VM_PH_B", "type": "VOLTMETER", "target_component": "RB", "signal_name": "v_phase_b"},
                {"name": "VM_PH_C", "type": "VOLTMETER", "target_component": "RC", "signal_name": "v_phase_c"},
                {"name": "AM_LOAD_A", "type": "AMMETER", "target_component": "RA", "signal_name": "i_load_a"},
            ],
            "script_blocks": [
                {
                    "name": "SPWM_3PH",
                    "in_signals": ["v_phase_a"],
                    "out_signals": ["g1", "g4", "g3", "g6", "g5", "g2"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_line = 50.0;
                            double f_carrier = 10000.0;
                            double m_a = 0.85;
                            phase += dt * f_carrier;
                            if (phase >= 1.0) phase -= 1.0;
                            double tri = 2.0 * Math.abs(phase - 0.5);
                            double ref_a = 0.5 * (1.0 + m_a * Math.sin(2.0 * PI * f_line * t));
                            double ref_b = 0.5 * (1.0 + m_a * Math.sin(2.0 * PI * f_line * t - 2.0943951));
                            double ref_c = 0.5 * (1.0 + m_a * Math.sin(2.0 * PI * f_line * t + 2.0943951));
                            yOUT[0] = (ref_a > tri) ? 1.0 : 0.0;
                            yOUT[1] = 1.0 - yOUT[0];
                            yOUT[2] = (ref_b > tri) ? 1.0 : 0.0;
                            yOUT[3] = 1.0 - yOUT[2];
                            yOUT[4] = (ref_c > tri) ? 1.0 : 0.0;
                            yOUT[5] = 1.0 - yOUT[4];
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "G_S1", "target_switch": "S1", "in_signal": "g1"},
                {"name": "G_S4", "target_switch": "S4", "in_signal": "g4"},
                {"name": "G_S3", "target_switch": "S3", "in_signal": "g3"},
                {"name": "G_S6", "target_switch": "S6", "in_signal": "g6"},
                {"name": "G_S5", "target_switch": "S5", "in_signal": "g5"},
                {"name": "G_S2", "target_switch": "S2", "in_signal": "g2"},
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcac/three-phase-inverter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcac/three-phase-inverter.ipes", "duration": 0.04, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_npc_inverter(client: GeckoMcpClient):
    print("\n--- 8. 3-Level NPC Multilevel Inverter (docs/tutorials/dcac/npc-inverter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcac/npc-inverter.ipes",
        "simulation": {
            "duration": 0.04,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            # Split DC bus: +350V / 0V / -350V
            {"name": "V_DC1", "type": "VOLTAGE_SOURCE_DC", "nodes": ["dc_p", "0"], "parameters": {"voltage": 350.0}},
            {"name": "V_DC2", "type": "VOLTAGE_SOURCE_DC", "nodes": ["0", "dc_n"], "parameters": {"voltage": 350.0}},
            {"name": "C1", "type": "CAPACITOR", "nodes": ["dc_p", "0"], "parameters": {"capacitance": 1000e-6}},
            {"name": "C2", "type": "CAPACITOR", "nodes": ["0", "dc_n"], "parameters": {"capacitance": 1000e-6}},
            # 4 Series Switches in NPC leg
            {"name": "S1", "type": "SWITCH", "nodes": ["dc_p", "n1"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "S2", "type": "SWITCH", "nodes": ["n1", "sw_out"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "S3", "type": "SWITCH", "nodes": ["sw_out", "n2"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "S4", "type": "SWITCH", "nodes": ["n2", "dc_n"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            # Neutral clamping diodes
            {"name": "D_CL1", "type": "DIODE", "nodes": ["0", "n1"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D_CL2", "type": "DIODE", "nodes": ["n2", "0"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            # Output LC filter and load
            {"name": "L_FILT", "type": "INDUCTOR", "nodes": ["sw_out", "out"], "parameters": {"inductance": 2e-3}},
            {"name": "C_FILT", "type": "CAPACITOR", "nodes": ["out", "0"], "parameters": {"capacitance": 10e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["out", "0"], "parameters": {"resistance": 20.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_out"},
                {"name": "AM_LOAD", "type": "AMMETER", "target_component": "R_LOAD", "signal_name": "i_load"},
                {"name": "VM_C1", "type": "VOLTMETER", "target_component": "C1", "signal_name": "v_c1"},
                {"name": "VM_C2", "type": "VOLTMETER", "target_component": "C2", "signal_name": "v_c2"},
            ],
            "script_blocks": [
                {
                    "name": "NPC_PWM",
                    "in_signals": ["v_out"],
                    "out_signals": ["g1", "g2", "g3", "g4"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_line = 50.0;
                            double f_carrier = 10000.0;
                            double m_a = 0.85;
                            phase += dt * f_carrier;
                            if (phase >= 1.0) phase -= 1.0;
                            double tri = 2.0 * Math.abs(phase - 0.5);
                            double ref = m_a * Math.sin(2.0 * PI * f_line * t);
                            double s1 = 0.0;
                            double s2 = 0.0;
                            double s3 = 0.0;
                            double s4 = 0.0;
                            if (ref >= 0.0) {
                                s1 = (ref > tri) ? 1.0 : 0.0;
                                s2 = 1.0;
                                s3 = 1.0 - s1;
                                s4 = 0.0;
                            } else {
                                s1 = 0.0;
                                s4 = (-ref > tri) ? 1.0 : 0.0;
                                s3 = 1.0;
                                s2 = 1.0 - s4;
                            }
                            yOUT[0] = s1;
                            yOUT[1] = s2;
                            yOUT[2] = s3;
                            yOUT[3] = s4;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "GATE_S1", "target_switch": "S1", "in_signal": "g1"},
                {"name": "GATE_S2", "target_switch": "S2", "in_signal": "g2"},
                {"name": "GATE_S3", "target_switch": "S3", "in_signal": "g3"},
                {"name": "GATE_S4", "target_switch": "S4", "in_signal": "g4"},
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcac/npc-inverter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcac/npc-inverter.ipes", "duration": 0.04, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_mmc_converter(client: GeckoMcpClient):
    print("\n--- 9. Modular Multilevel Converter (MMC) (docs/tutorials/dcac/mmc-converter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcac/mmc-converter.ipes",
        "simulation": {
            "duration": 0.04,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            # DC bus: +400V / -400V (800V total)
            {"name": "V_DC1", "type": "VOLTAGE_SOURCE_DC", "nodes": ["dc_p", "0"], "parameters": {"voltage": 400.0}},
            {"name": "V_DC2", "type": "VOLTAGE_SOURCE_DC", "nodes": ["0", "dc_n"], "parameters": {"voltage": 400.0}},
            # Upper arm inductor
            {"name": "L_ARM_UP", "type": "INDUCTOR", "nodes": ["dc_p", "arm_up"], "parameters": {"inductance": 5e-3}},
            # Upper submodule (HBSM)
            {"name": "S_U1", "type": "SWITCH", "nodes": ["arm_up", "ac_node"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 1.0}},
            {"name": "S_U2", "type": "SWITCH", "nodes": ["arm_up", "c_up_node"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "C_SM_UP", "type": "CAPACITOR", "nodes": ["c_up_node", "ac_node"], "parameters": {"capacitance": 2200e-6}},
            # Lower submodule (HBSM)
            {"name": "S_L1", "type": "SWITCH", "nodes": ["ac_node", "arm_lo"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 1.0}},
            {"name": "S_L2", "type": "SWITCH", "nodes": ["ac_node", "c_lo_node"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "C_SM_LO", "type": "CAPACITOR", "nodes": ["c_lo_node", "arm_lo"], "parameters": {"capacitance": 2200e-6}},
            # Lower arm inductor
            {"name": "L_ARM_LO", "type": "INDUCTOR", "nodes": ["arm_lo", "dc_n"], "parameters": {"inductance": 5e-3}},
            # AC Load
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["ac_node", "0"], "parameters": {"resistance": 20.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_AC", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_ac"},
                {"name": "AM_LOAD", "type": "AMMETER", "target_component": "R_LOAD", "signal_name": "i_ac"},
                {"name": "AM_UPPER", "type": "AMMETER", "target_component": "L_ARM_UP", "signal_name": "i_arm_up"},
            ],
            "script_blocks": [
                {
                    "name": "MMC_CTRL",
                    "in_signals": ["v_ac"],
                    "out_signals": ["g_u1", "g_u2", "g_l1", "g_l2"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_line = 50.0;
                            double f_carrier = 5000.0;
                            phase += dt * f_carrier;
                            if (phase >= 1.0) phase -= 1.0;
                            double tri = 2.0 * Math.abs(phase - 0.5);
                            double m_a = 0.8;
                            double ref = 0.5 * (1.0 + m_a * Math.sin(2.0 * PI * f_line * t));
                            double su = (ref > tri) ? 1.0 : 0.0;
                            double sl = 1.0 - su;
                            yOUT[0] = 1.0 - su;
                            yOUT[1] = su;
                            yOUT[2] = 1.0 - sl;
                            yOUT[3] = sl;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "G_U1", "target_switch": "S_U1", "in_signal": "g_u1"},
                {"name": "G_U2", "target_switch": "S_U2", "in_signal": "g_u2"},
                {"name": "G_L1", "target_switch": "S_L1", "in_signal": "g_l1"},
                {"name": "G_L2", "target_switch": "S_L2", "in_signal": "g_l2"},
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcac/mmc-converter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcac/mmc-converter.ipes", "duration": 0.04, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_buck_converter(client: GeckoMcpClient):
    print("\n--- 10. Buck Converter (docs/tutorials/dcdc/buck-converter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcdc/buck-converter.ipes",
        "simulation": {
            "duration": 0.01,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            {"name": "V_IN", "type": "VOLTAGE_SOURCE_DC", "nodes": ["in", "0"], "parameters": {"voltage": 24.0}},
            {"name": "S1", "type": "SWITCH", "nodes": ["in", "sw_node"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D_FW", "type": "DIODE", "nodes": ["0", "sw_node"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "L1", "type": "INDUCTOR", "nodes": ["sw_node", "out"], "parameters": {"inductance": 100e-6}},
            {"name": "C1", "type": "CAPACITOR", "nodes": ["out", "0"], "parameters": {"capacitance": 100e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["out", "0"], "parameters": {"resistance": 6.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_out"},
                {"name": "VM_IN", "type": "VOLTMETER", "target_component": "V_IN", "signal_name": "v_in"},
                {"name": "AM_L", "type": "AMMETER", "target_component": "L1", "signal_name": "i_L"},
                {"name": "VM_SW", "type": "VOLTMETER", "target_component": "D_FW", "signal_name": "v_sw"},
            ],
            "script_blocks": [
                {
                    "name": "PWM_50K",
                    "in_signals": ["v_out"],
                    "out_signals": ["pwm"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_sw = 50000.0;
                            phase += dt * f_sw;
                            if (phase >= 1.0) phase -= 1.0;
                            yOUT[0] = (phase < 0.50) ? 1.0 : 0.0;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "GATE_1", "target_switch": "S1", "in_signal": "pwm"}
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcdc/buck-converter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcdc/buck-converter.ipes", "duration": 0.01, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_boost_converter(client: GeckoMcpClient):
    print("\n--- 11. Boost Converter (docs/tutorials/dcdc/boost-converter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcdc/boost-converter.ipes",
        "simulation": {
            "duration": 0.015,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            {"name": "V_IN", "type": "VOLTAGE_SOURCE_DC", "nodes": ["in", "0"], "parameters": {"voltage": 12.0}},
            {"name": "L1", "type": "INDUCTOR", "nodes": ["in", "sw_node"], "parameters": {"inductance": 100e-6}},
            {"name": "S1", "type": "SWITCH", "nodes": ["sw_node", "0"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "D1", "type": "DIODE", "nodes": ["sw_node", "out"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "C1", "type": "CAPACITOR", "nodes": ["out", "0"], "parameters": {"capacitance": 220e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["out", "0"], "parameters": {"resistance": 12.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_out"},
                {"name": "VM_IN", "type": "VOLTMETER", "target_component": "V_IN", "signal_name": "v_in"},
                {"name": "AM_L", "type": "AMMETER", "target_component": "L1", "signal_name": "i_L"},
                {"name": "AM_D", "type": "AMMETER", "target_component": "D1", "signal_name": "i_diode"},
            ],
            "script_blocks": [
                {
                    "name": "PWM_50K",
                    "in_signals": ["v_out"],
                    "out_signals": ["pwm"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_sw = 50000.0;
                            phase += dt * f_sw;
                            if (phase >= 1.0) phase -= 1.0;
                            yOUT[0] = (phase < 0.50) ? 1.0 : 0.0;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "GATE_1", "target_switch": "S1", "in_signal": "pwm"}
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcdc/boost-converter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcdc/boost-converter.ipes", "duration": 0.015, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_buck_boost_converter(client: GeckoMcpClient):
    print("\n--- 12. Inverting Buck-Boost Converter (docs/tutorials/dcdc/buck-boost-converter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcdc/buck-boost-converter.ipes",
        "simulation": {
            "duration": 0.015,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            {"name": "V_IN", "type": "VOLTAGE_SOURCE_DC", "nodes": ["in", "0"], "parameters": {"voltage": 24.0}},
            {"name": "S1", "type": "SWITCH", "nodes": ["in", "sw_node"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            {"name": "L1", "type": "INDUCTOR", "nodes": ["sw_node", "0"], "parameters": {"inductance": 150e-6}},
            {"name": "D1", "type": "DIODE", "nodes": ["out", "sw_node"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "C1", "type": "CAPACITOR", "nodes": ["0", "out"], "parameters": {"capacitance": 220e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["0", "out"], "parameters": {"resistance": 10.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_out"},
                {"name": "VM_IN", "type": "VOLTMETER", "target_component": "V_IN", "signal_name": "v_in"},
                {"name": "AM_L", "type": "AMMETER", "target_component": "L1", "signal_name": "i_L"},
            ],
            "script_blocks": [
                {
                    "name": "PWM_50K",
                    "in_signals": ["v_out"],
                    "out_signals": ["pwm"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_sw = 50000.0;
                            phase += dt * f_sw;
                            if (phase >= 1.0) phase -= 1.0;
                            yOUT[0] = (phase < 0.40) ? 1.0 : 0.0;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "GATE_1", "target_switch": "S1", "in_signal": "pwm"}
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcdc/buck-boost-converter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcdc/buck-boost-converter.ipes", "duration": 0.015, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_flyback_converter(client: GeckoMcpClient):
    print("\n--- 13. Flyback Converter (docs/tutorials/dcdc/flyback-converter.ipes) ---")
    import re
    with open(REPO_ROOT / "frontend/src/model/examples.ts", "r", encoding="utf-8") as f:
        text = f.read()
    m = re.search(r"export const FLYBACK_IPES = `(.*?)`;", text, re.DOTALL)
    if not m:
        raise RuntimeError("FLYBACK_IPES not found in examples.ts")
    content = m.group(1).replace("\\`", "`").replace("\\${", "${")
    out_file = REPO_ROOT / "docs/tutorials/dcdc/flyback-converter.ipes"
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(content)
    print("Created docs/tutorials/dcdc/flyback-converter.ipes from verified FLYBACK_IPES")

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcdc/flyback-converter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcdc/flyback-converter.ipes", "duration": 0.001, "dt": 1e-7},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_forward_converter(client: GeckoMcpClient):
    print("\n--- 14. Forward Converter (docs/tutorials/dcdc/forward-converter.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/dcdc/forward-converter.ipes",
        "simulation": {
            "duration": 0.005,
            "dt": 1e-7,
            "solver": 0,
        },
        "components": [
            # Primary side
            {"name": "V_IN", "type": "VOLTAGE_SOURCE_DC", "nodes": ["in", "0"], "parameters": {"voltage": 48.0}},
            {"name": "S1", "type": "SWITCH", "nodes": ["sw_node", "0"], "parameters": {"r_on": 0.01, "r_off": 1e6, "initial_state": 0.0}},
            # Forward Transformer: 48V to 24V (2:1 ratio)
            {"name": "TR1", "type": "TRANSFORMER", "nodes": ["in", "sw_node", "sec_p", "0"], "parameters": {"n1": 2.0, "n2": 1.0, "polarity": -1.0}},
            # Primary RCD Clamp / Reset snubber
            {"name": "D_CLAMP", "type": "DIODE", "nodes": ["sw_node", "clamp_mid"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "R_CLAMP", "type": "RESISTOR", "nodes": ["clamp_mid", "in"], "parameters": {"resistance": 200.0}},
            {"name": "C_CLAMP", "type": "CAPACITOR", "nodes": ["clamp_mid", "in"], "parameters": {"capacitance": 47e-9}},
            # Secondary Rectification and LC Filter
            {"name": "D_FWD", "type": "DIODE", "nodes": ["sec_p", "fw_node"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "D_FW", "type": "DIODE", "nodes": ["0", "fw_node"], "parameters": {"u_forward": 0.7, "r_on": 0.01}},
            {"name": "L_OUT", "type": "INDUCTOR", "nodes": ["fw_node", "out"], "parameters": {"inductance": 100e-6}},
            {"name": "C_OUT", "type": "CAPACITOR", "nodes": ["out", "0"], "parameters": {"capacitance": 220e-6}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["out", "0"], "parameters": {"resistance": 5.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_out"},
                {"name": "VM_IN", "type": "VOLTMETER", "target_component": "V_IN", "signal_name": "v_in"},
                {"name": "VM_SW", "type": "VOLTMETER", "target_component": "S1", "signal_name": "v_sw"},
                {"name": "AM_L", "type": "AMMETER", "target_component": "L_OUT", "signal_name": "i_L"},
            ],
            "script_blocks": [
                {
                    "name": "PWM_100K",
                    "in_signals": ["v_out"],
                    "out_signals": ["pwm"],
                    "static_variables": "double phase = 0.0;",
                    "code": """
                            double f_sw = 100000.0;
                            phase += dt * f_sw;
                            if (phase >= 1.0) phase -= 1.0;
                            yOUT[0] = (phase < 0.35) ? 1.0 : 0.0;
                            return yOUT;
                            """,
                }
            ],
            "gates": [
                {"name": "GATE_1", "target_switch": "S1", "in_signal": "pwm"}
            ],
        },
    }

    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res)

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/dcdc/forward-converter.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/dcdc/forward-converter.ipes", "duration": 0.005, "dt": 1e-7},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_loss_calculation(client: GeckoMcpClient):
    print("\n--- 15. Loss Calculation (docs/tutorials/thermal/loss-calculation.ipes) ---")
    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/thermal/loss-calculation.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/thermal/loss-calculation.ipes", "duration": 0.01, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals count:", len(sim.get("signal_names", [])))


def build_junction_temperature(client: GeckoMcpClient):
    print("\n--- 16. Junction Temperature (docs/tutorials/thermal/junction-temperature.ipes) ---")
    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/thermal/junction-temperature.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/thermal/junction-temperature.ipes", "duration": 0.02, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals count:", len(sim.get("signal_names", [])))


def build_heatsink_design(client: GeckoMcpClient):
    print("\n--- 17. Heatsink Design (docs/tutorials/thermal/heatsink-design.ipes) ---")
    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/thermal/heatsink-design.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/thermal/heatsink-design.ipes", "duration": 0.01, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals count:", len(sim.get("signal_names", [])))


def build_magnetic_domain(client: GeckoMcpClient):
    print("\n--- 18. Magnetic Domain (docs/tutorials/magnetics/magnetic-domain.ipes) ---")
    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/magnetics/magnetic-domain.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/magnetics/magnetic-domain.ipes", "duration": 0.04, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals count:", len(sim.get("signal_names", [])))


def build_transformer_design(client: GeckoMcpClient):
    print("\n--- 19. Transformer Design (docs/tutorials/magnetics/transformer-design.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/magnetics/transformer-design.ipes",
        "simulation": {
            "duration": 0.04,
            "dt": 1e-6,
            "solver": 0,
        },
        "components": [
            {"name": "V_AC", "type": "VOLTAGE_SOURCE_AC", "nodes": ["n_in", "0"], "parameters": {"amplitude": 325.27, "frequency": 50.0}},
            {"name": "R_P", "type": "RESISTOR", "nodes": ["n_in", "n_lp"], "parameters": {"resistance": 0.5}},
            {"name": "L_SIGMA_P", "type": "INDUCTOR", "nodes": ["n_lp", "n_pri"], "parameters": {"inductance": 50e-6}},
            {"name": "R_FE", "type": "RESISTOR", "nodes": ["n_pri", "0"], "parameters": {"resistance": 10000.0}},
            {"name": "L_M", "type": "INDUCTOR", "nodes": ["n_pri", "0"], "parameters": {"inductance": 50e-3}},
            {"name": "XFMR", "type": "TRANSFORMER", "nodes": ["n_pri", "0", "n_sec", "0"], "parameters": {"n1": 10.0, "n2": 2.5, "polarity": -1.0}},
            {"name": "L_SIGMA_S", "type": "INDUCTOR", "nodes": ["n_sec", "n_ls"], "parameters": {"inductance": 3.125e-6}},
            {"name": "R_S", "type": "RESISTOR", "nodes": ["n_ls", "n_out"], "parameters": {"resistance": 0.03125}},
            {"name": "R_LOAD", "type": "RESISTOR", "nodes": ["n_out", "0"], "parameters": {"resistance": 5.0}},
        ],
        "control": {
            "probes": [
                {"name": "V_SEC", "type": "VOLTMETER", "target_component": "R_LOAD", "signal_name": "v_sec"},
                {"name": "V_PRI", "type": "VOLTMETER", "target_component": "V_AC", "signal_name": "v_pri"},
            ]
        },
    }
    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res.get("status"))

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/magnetics/transformer-design.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/magnetics/transformer-design.ipes", "duration": 0.04, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


def build_inductor_saturation(client: GeckoMcpClient):
    print("\n--- 20. Inductor Saturation (docs/tutorials/magnetics/inductor-saturation.ipes) ---")
    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/magnetics/inductor-saturation.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/magnetics/inductor-saturation.ipes", "duration": 0.005, "dt": 1e-6},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals count:", len(sim.get("signal_names", [])))


def build_mechanical_systems(client: GeckoMcpClient):
    print("\n--- 21. Mechanical Systems (docs/tutorials/magnetics/mechanical-systems.ipes) ---")
    circuit_def = {
        "output_path": "docs/tutorials/magnetics/mechanical-systems.ipes",
        "simulation": {
            "duration": 0.15,
            "dt": 1e-5,
            "solver": 0,
        },
        "components": [
            {"name": "TAU_M", "type": "DC_VOLTAGE", "nodes": ["n_drive", "0"], "parameters": {"voltage": 50.0}},
            {"name": "R_INT", "type": "RESISTOR", "nodes": ["n_drive", "w1"], "parameters": {"resistance": 0.5}},
            {"name": "J1_MOTOR", "type": "CAPACITOR", "nodes": ["w1", "0"], "parameters": {"capacitance": 0.05}},
            {"name": "K_SHAFT", "type": "INDUCTOR", "nodes": ["w1", "w2"], "parameters": {"inductance": 1e-3}},
            {"name": "B_SHAFT", "type": "RESISTOR", "nodes": ["w1", "w2"], "parameters": {"resistance": 10.0}},
            {"name": "J2_LOAD", "type": "CAPACITOR", "nodes": ["w2", "0"], "parameters": {"capacitance": 0.15}},
            {"name": "B_LOAD", "type": "RESISTOR", "nodes": ["w2", "0"], "parameters": {"resistance": 5.0}},
        ],
        "control": {
            "probes": [
                {"name": "VM_W1", "type": "VOLTMETER", "target_component": "J1_MOTOR", "signal_name": "omega_motor"},
                {"name": "VM_W2", "type": "VOLTMETER", "target_component": "J2_LOAD", "signal_name": "omega_load"},
            ]
        },
    }
    res = client.call_tool("gecko_create_circuit", circuit_def)
    print("Create:", res.get("status"))

    val = client.call_tool(
        "gecko_validate_circuit",
        {"circuit_path": "docs/tutorials/magnetics/mechanical-systems.ipes", "level": "compile"},
    )
    print("Validate:", val.get("valid"), "Diagnostics:", val.get("diagnostics"))

    sim = client.call_tool(
        "gecko_simulate",
        {"circuit_path": "docs/tutorials/magnetics/mechanical-systems.ipes", "duration": 0.15, "dt": 1e-5},
    )
    print("Simulate:", sim.get("status"), "steps:", sim.get("total_steps"), "signals:", sim.get("signal_names"))


if __name__ == "__main__":
    with GeckoMcpClient() as client:
        # ACDC
        build_single_phase_rectifier(client)
        build_three_phase_rectifier(client)
        build_boost_pfc(client)
        build_vienna_rectifier(client)
        build_smps_230v_to_3v3(client)
        # DCAC
        build_single_phase_inverter(client)
        build_three_phase_inverter(client)
        build_npc_inverter(client)
        build_mmc_converter(client)
        # DCDC
        build_buck_converter(client)
        build_boost_converter(client)
        build_buck_boost_converter(client)
        build_flyback_converter(client)
        build_forward_converter(client)
        # Thermal
        build_loss_calculation(client)
        build_junction_temperature(client)
        build_heatsink_design(client)
        # Magnetics & Mechanical
        build_magnetic_domain(client)
        build_transformer_design(client)
        build_inductor_saturation(client)
        build_mechanical_systems(client)

