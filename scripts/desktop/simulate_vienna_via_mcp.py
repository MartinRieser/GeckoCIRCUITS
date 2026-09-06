#!/usr/bin/env python3
"""Synthesize, validate, and simulate the 11 kW Vienna Battery Charger using ONLY the GeckoCIRCUITS MCP Server.

This script communicates exclusively via the Model Context Protocol (MCP) JSON-RPC stdio
interface to demonstrate autonomous circuit design without inspecting internal source code.
"""

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "scripts" / "desktop"))

from mcp_client import McpSession


def run():
    session = McpSession()
    try:
        # 1. Initialize MCP Session
        print("=" * 70)
        print("STEP 1: Initialize MCP Session")
        print("=" * 70)
        init = session.initialize()
        print(f"Connected to MCP Server: {init.get('serverInfo')}")

        # 2. Inspect Component Catalog via MCP
        print("\n" + "=" * 70)
        print("STEP 2: Query gecko_catalog via MCP")
        print("=" * 70)
        catalog = session.call_tool("gecko_catalog", {})
        power_types = [c["type"] for c in catalog.get("power_components", [])]
        control_types = [c["type"] for c in catalog.get("control_components", [])]
        print(f"Available Power Components: {power_types}")
        print(f"Available Control Components: {control_types}")

        # 3. Define Circuit Netlist for 11 kW 3-Phase Vienna Battery Charger
        print("\n" + "=" * 70)
        print("STEP 3: Synthesize Circuit via gecko_create_circuit MCP Tool")
        print("=" * 70)

        output_path = "resources/projects/vienna_battery_charger_mcp.ipes"

        # Microcontroller DSP script code for Vienna modulation and battery charge regulation
        mcu_code = """
// Measurements from probes (strictly aligned with in_signals order)
double vc1        = xIN[0]; // Upper DC bus capacitor voltage (vdc_pos to m_mid) [V]
double vc2        = xIN[1]; // Lower DC bus capacitor voltage (m_mid to 0) [V]
double v_bat_meas = xIN[2]; // Battery terminal voltage [V]
double i_a_meas   = xIN[3]; // Phase A boost inductor current [A]
double i_b_meas   = xIN[4]; // Phase B boost inductor current [A]
double i_c_meas   = xIN[5]; // Phase C boost inductor current [A]
double i_bat_meas = xIN[6]; // Battery charging current [A]

// Total intermediate DC link voltage (nominal 750 V)
double v_dc_total = vc1 + vc2;

// 1. Grid Synchronization & Phase Reference Generation (50 Hz European Mains)
double omega = 2.0 * PI * 50.0;
double sin_a = sin(omega * t);
double sin_b = sin(omega * t - 2.094395102); // -120 deg
double sin_c = sin(omega * t + 2.094395102); // +120 deg

// 2. Intermediate DC Link Voltage Regulation (PFC Outer Loop: V_ref = 750.0 V)
double v_dc_ref = 750.0;
double e_vdc = v_dc_ref - v_dc_total;

double Kp_vdc = 0.080;
double Ki_vdc = 10.0;
v_dc_int = v_dc_int + Ki_vdc * e_vdc * dt;
if (v_dc_int < -15.0) { v_dc_int = -15.0; }
if (v_dc_int > 20.0)  { v_dc_int = 20.0; }

// Nominal feedforward peak phase current: (11 kW / (3 * 230V_rms)) * sqrt(2) = 22.55 A
double i_pfc_cmd = 22.55 + Kp_vdc * e_vdc + v_dc_int;
if (i_pfc_cmd < 1.0)  { i_pfc_cmd = 1.0; }
if (i_pfc_cmd > 32.0) { i_pfc_cmd = 32.0; }

// Sinusoidal reference currents for unity power factor
double i_ref_a = i_pfc_cmd * sin_a;
double i_ref_b = i_pfc_cmd * sin_b;
double i_ref_c = i_pfc_cmd * sin_c;

// 3. Vienna 3-Level Switching Modulation (Carrier: 25 kHz, 40 steps @ 1us)
step_pfc = (step_pfc + 1) % 40;
double tri_pfc = (step_pfc < 20) ? (step_pfc / 20.0) : ((40 - step_pfc) / 20.0);

// Dynamic safe DC link voltage
double vdc_safe = (v_dc_total > 50.0) ? v_dc_total : 750.0;
double mod_idx = 2.0 * 325.269 / vdc_safe;
if (mod_idx > 0.95) { mod_idx = 0.95; }

// Phase A current shaping duty
double sgn_a = (sin_a >= 0.0) ? 1.0 : -1.0;
double err_ia = i_ref_a - i_a_meas;
double duty_a = 1.0 - mod_idx * abs(sin_a) + sgn_a * 0.003 * err_ia;
if (duty_a < 0.02) { duty_a = 0.02; }
if (duty_a > 0.98) { duty_a = 0.98; }

// Phase B current shaping duty
double sgn_b = (sin_b >= 0.0) ? 1.0 : -1.0;
double err_ib = i_ref_b - i_b_meas;
double duty_b = 1.0 - mod_idx * abs(sin_b) + sgn_b * 0.003 * err_ib;
if (duty_b < 0.02) { duty_b = 0.02; }
if (duty_b > 0.98) { duty_b = 0.98; }

// Phase C current shaping duty
double sgn_c = (sin_c >= 0.0) ? 1.0 : -1.0;
double err_ic = i_ref_c - i_c_meas;
double duty_c = 1.0 - mod_idx * abs(sin_c) + sgn_c * 0.003 * err_ic;
if (duty_c < 0.02) { duty_c = 0.02; }
if (duty_c > 0.98) { duty_c = 0.98; }

// Vienna Gate Pulses: SW conducts to clamp to midpoint M when tri < duty
yOUT[0] = (tri_pfc < duty_a) ? 1.0 : 0.0; // GATE_A
yOUT[1] = (tri_pfc < duty_b) ? 1.0 : 0.0; // GATE_B
yOUT[2] = (tri_pfc < duty_c) ? 1.0 : 0.0; // GATE_C

// 4. DC-DC Battery Charger Output Voltage Regulation (< 52.0 V, target 49.5 V)
double v_bat_target = 49.50;
double e_vbat = v_bat_target - v_bat_meas;

double Kp_dcdc = 0.0015;
double Ki_dcdc = 4.0;
v_bat_int = v_bat_int + Ki_dcdc * e_vbat * dt;
if (v_bat_int < -0.015) { v_bat_int = -0.015; }
if (v_bat_int > 0.015)  { v_bat_int = 0.015; }

// Dynamic feedforward duty cycle: V_bat_target / vdc_safe
double duty_ff = v_bat_target / vdc_safe;
double duty_dcdc = duty_ff + Kp_dcdc * e_vbat + v_bat_int;

// Strict safety clamps:
// 1. Dynamic overvoltage prevention: duty <= 50.5 / vdc_safe
// 2. Absolute hard ceiling: duty <= 0.0665 (at 750V -> max 49.87V)
double duty_max = 50.50 / vdc_safe;
if (duty_max > 0.0665) { duty_max = 0.0665; }
if (duty_dcdc < 0.005) { duty_dcdc = 0.005; }
if (duty_dcdc > duty_max) { duty_dcdc = duty_max; }

// Instantaneous hardware-like overvoltage protection:
if (v_bat_meas >= 50.80) {
    duty_dcdc = 0.0;
}

// DC-DC Carrier: 10 kHz, 100 steps @ 1us
step_dcdc = (step_dcdc + 1) % 100;
double ramp_dcdc = step_dcdc / 100.0;

yOUT[3] = (ramp_dcdc < duty_dcdc) ? 1.0 : 0.0; // GATE_DCDC

return yOUT;
"""

        circuit_def = {
            "output_path": output_path,
            "simulation": {
                "duration": 0.10,
                "dt": 1.0e-6,
                "solver": 0  # Backward Euler
            },
            "components": [
                # 3-Phase 230V RMS grid (325.269V peak)
                {"name": "U_A", "type": "AC_VOLTAGE", "nodes": ["ua", "n_grid"],
                 "parameters": {"amplitude": 325.269, "frequency": 50.0, "offset": 0.0, "phase_deg": 0.0}},
                {"name": "U_B", "type": "AC_VOLTAGE", "nodes": ["ub", "n_grid"],
                 "parameters": {"amplitude": 325.269, "frequency": 50.0, "offset": 0.0, "phase_deg": 120.0}},
                {"name": "U_C", "type": "AC_VOLTAGE", "nodes": ["uc", "n_grid"],
                 "parameters": {"amplitude": 325.269, "frequency": 50.0, "offset": 0.0, "phase_deg": -120.0}},

                # Boost Inductors (1 mH per phase)
                {"name": "L_A", "type": "INDUCTOR", "nodes": ["ua", "va_sw"], "parameters": {"inductance": 1.0e-3}},
                {"name": "L_B", "type": "INDUCTOR", "nodes": ["ub", "vb_sw"], "parameters": {"inductance": 1.0e-3}},
                {"name": "L_C", "type": "INDUCTOR", "nodes": ["uc", "vc_sw"], "parameters": {"inductance": 1.0e-3}},

                # Vienna Rectifier Leg A
                {"name": "D_A_HI", "type": "DIODE", "nodes": ["va_sw", "vdc_pos"], "parameters": {"u_forward": 0.7, "r_on": 0.005}},
                {"name": "D_A_LO", "type": "DIODE", "nodes": ["0", "va_sw"], "parameters": {"u_forward": 0.7, "r_on": 0.005}},
                {"name": "SW_A", "type": "SWITCH", "nodes": ["va_sw", "m_mid"], "parameters": {"r_on": 0.01, "r_off": 1e6}},

                # Vienna Rectifier Leg B
                {"name": "D_B_HI", "type": "DIODE", "nodes": ["vb_sw", "vdc_pos"], "parameters": {"u_forward": 0.7, "r_on": 0.005}},
                {"name": "D_B_LO", "type": "DIODE", "nodes": ["0", "vb_sw"], "parameters": {"u_forward": 0.7, "r_on": 0.005}},
                {"name": "SW_B", "type": "SWITCH", "nodes": ["vb_sw", "m_mid"], "parameters": {"r_on": 0.01, "r_off": 1e6}},

                # Vienna Rectifier Leg C
                {"name": "D_C_HI", "type": "DIODE", "nodes": ["vc_sw", "vdc_pos"], "parameters": {"u_forward": 0.7, "r_on": 0.005}},
                {"name": "D_C_LO", "type": "DIODE", "nodes": ["0", "vc_sw"], "parameters": {"u_forward": 0.7, "r_on": 0.005}},
                {"name": "SW_C", "type": "SWITCH", "nodes": ["vc_sw", "m_mid"], "parameters": {"r_on": 0.01, "r_off": 1e6}},

                # Split DC Link Capacitors (2 mF each, pre-charged to 375V for clean start)
                {"name": "C_1", "type": "CAPACITOR", "nodes": ["vdc_pos", "m_mid"], "parameters": {"capacitance": 2.0e-3, "v_init": 375.0}},
                {"name": "C_2", "type": "CAPACITOR", "nodes": ["m_mid", "0"], "parameters": {"capacitance": 2.0e-3, "v_init": 375.0}},

                # DC-DC Step-Down Stage to Battery (<52V, 11 kW)
                {"name": "SW_DCDC", "type": "SWITCH", "nodes": ["vdc_pos", "vx_buck"], "parameters": {"r_on": 0.01, "r_off": 1e6}},
                {"name": "D_FREE", "type": "DIODE", "nodes": ["0", "vx_buck"], "parameters": {"u_forward": 0.7, "r_on": 0.005}},
                {"name": "L_OUT", "type": "INDUCTOR", "nodes": ["vx_buck", "v_bat"], "parameters": {"inductance": 0.25e-3}},
                {"name": "C_OUT", "type": "CAPACITOR", "nodes": ["v_bat", "0"], "parameters": {"capacitance": 4.7e-3, "v_init": 49.5}},
                {"name": "R_BAT", "type": "RESISTOR", "nodes": ["v_bat", "0"], "parameters": {"resistance": 0.2227}}
            ],
            "control": {
                "probes": [
                    {"name": "VM_C1", "type": "VOLTMETER", "target_component": "C_1", "signal_name": "v_c1"},
                    {"name": "VM_C2", "type": "VOLTMETER", "target_component": "C_2", "signal_name": "v_c2"},
                    {"name": "VM_BAT", "type": "VOLTMETER", "target_component": "R_BAT", "signal_name": "v_bat"},
                    {"name": "AM_LA", "type": "AMMETER", "target_component": "L_A", "signal_name": "i_la"},
                    {"name": "AM_LB", "type": "AMMETER", "target_component": "L_B", "signal_name": "i_lb"},
                    {"name": "AM_LC", "type": "AMMETER", "target_component": "L_C", "signal_name": "i_lc"},
                    {"name": "AM_BAT", "type": "AMMETER", "target_component": "L_OUT", "signal_name": "i_bat"},
                    {"name": "VM_GRID_A", "type": "VOLTMETER", "target_component": "U_A", "signal_name": "v_grid_a"}
                ],
                "script_blocks": [
                    {
                        "name": "CTRL_MCU",
                        "in_signals": ["v_c1", "v_c2", "v_bat", "i_la", "i_lb", "i_lc", "i_bat", "v_grid_a"],
                        "out_signals": ["s_gate_a", "s_gate_b", "s_gate_c", "s_gate_dcdc"],
                        "static_variables": "int step_pfc = 0; int step_dcdc = 0; double v_dc_int = 0.0; double v_bat_int = 0.0;",
                        "code": mcu_code
                    }
                ],
                "gates": [
                    {"name": "GATE_A", "target_switch": "SW_A", "in_signal": "s_gate_a"},
                    {"name": "GATE_B", "target_switch": "SW_B", "in_signal": "s_gate_b"},
                    {"name": "GATE_C", "target_switch": "SW_C", "in_signal": "s_gate_c"},
                    {"name": "GATE_DCDC", "target_switch": "SW_DCDC", "in_signal": "s_gate_dcdc"}
                ]
            }
        }

        build_res = session.call_tool("gecko_create_circuit", circuit_def)
        print("Synthesis Result:")
        print(json.dumps(build_res, indent=2))

        # 4. Pre-Simulation Design Rule Checking via MCP
        print("\n" + "=" * 70)
        print("STEP 4: Pre-Simulation DRC via gecko_validate_circuit MCP Tool")
        print("=" * 70)
        val_res = session.call_tool("gecko_validate_circuit", {"circuit_path": output_path})
        print("Validation Result:")
        print(json.dumps(val_res, indent=2))
        if not val_res.get("valid"):
            print("ERROR: Circuit validation failed. Aborting simulation.")
            sys.exit(1)

        # 5. Simulate and Calculate Metrics via MCP
        print("\n" + "=" * 70)
        print("STEP 5: Simulate & Compute Figures of Merit via gecko_measure_metrics MCP Tool")
        print("=" * 70)
        metrics_req = {
            "circuit_path": output_path,
            "duration": 0.10,
            "dt": 1.0e-6,
            "start_time": 0.06,  # Steady-state window 60 ms -> 100 ms
            "v_in_signal": "v_grid_a",
            "i_in_signal": "i_la",
            "v_out_signal": "v_bat",
            "i_out_signal": "i_bat",
            "is_three_phase": True
        }
        metrics_res = session.call_tool("gecko_measure_metrics", metrics_req)
        print("Simulation & Converter Metrics Result:")
        print(json.dumps(metrics_res, indent=2))

        # 6. Verify User Specifications
        print("\n" + "=" * 70)
        print("STEP 6: Specification Compliance Verification")
        print("=" * 70)
        sys_metrics = metrics_res.get("system_metrics", {})
        signals = metrics_res.get("signals", {})

        p_out_kw = sys_metrics.get("p_out_kw", 0.0)
        p_in_kw = sys_metrics.get("p_in_kw", 0.0)
        pf = sys_metrics.get("power_factor", 0.0)
        eff = sys_metrics.get("efficiency_percent", 0.0)

        v_bat_stats = signals.get("v_bat", {})
        v_bat_mean = v_bat_stats.get("mean", 0.0)
        v_bat_max = v_bat_stats.get("max", 0.0)
        v_bat_ripple = v_bat_stats.get("peak_to_peak_ripple", 0.0)

        i_bat_stats = signals.get("i_bat", {})
        i_bat_mean = i_bat_stats.get("mean", 0.0)

        print(f"Output Power:       {p_out_kw:.3f} kW (Specification: 11.0 kW)")
        print(f"Battery Voltage:    {v_bat_mean:.2f} V (Specification: < 52.0 V)")
        print(f"Max Peak Voltage:   {v_bat_max:.2f} V (Strict Safety Limit < 52.0 V)")
        print(f"Voltage Ripple:     {v_bat_ripple:.2f} V pk-pk ({v_bat_stats.get('ripple_percent', 0.0):.2f}%)")
        print(f"Charging Current:   {i_bat_mean:.2f} A")
        print(f"Grid Power Factor:  {pf:.4f} (Active PFC requirement)")
        print(f"System Efficiency:  {eff:.2f}%")

        assert 10.0 <= p_out_kw <= 12.0, f"Output power {p_out_kw} kW outside 10-12 kW range"
        assert v_bat_max < 52.0, f"Battery voltage max {v_bat_max} V violates < 52.0 V requirement"
        assert pf >= 0.90, f"Power factor {pf} below 0.90"
        print("\n>>> ALL SPECIFICATIONS STRICTLY MET USING MCP SERVER ONLY! <<<")

    finally:
        session.close()


if __name__ == "__main__":
    run()
