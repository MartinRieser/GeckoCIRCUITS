# GeckoCIRCUITS Circuit Examples

This directory contains curated educational and reference circuit models in native `.ipes` ASCII format.
These files can be opened directly in GeckoCIRCUITS via **File → Open...** or explored inside the application
using **File → Open Example...**.

| File | Category | Circuit Name | Description |
|---|---|---|---|
| [`spwm_inverter_deadtime.ipes`](./spwm_inverter_deadtime.ipes) | Power Electronics | **Closed-Loop SPWM Inverter with Dead-Time (50 Hz)** | Full-bridge 50 Hz inverter with PI regulator and 1.0 µs dead-time shoot-through prevention. |
| [`buck.ipes`](./buck.ipes) | Power Electronics | **DC-DC Buck Converter (Sampled 0.5ms Control)** | 50V to 10V buck converter with 50 Hz input ripple, 2 kHz (0.5ms) discrete S/H control and feedforward. |
| [`sync_buck.ipes`](./sync_buck.ipes) | Power Electronics | **Synchronous Buck Converter (50 kHz)** | High-efficiency step-down converter with complementary active MOSFET switching. |
| [`boost.ipes`](./boost.ipes) | Power Electronics | **DC-DC Boost Converter (Step-Up)** | Step-up converter (12V to 24V) with boost inductor and fast recovery diode. |
| [`buck_boost.ipes`](./buck_boost.ipes) | Power Electronics | **Inverting Buck-Boost Converter (-16V)** | Negative voltage step-up/step-down converter (24V to -16V) with single inductor. |
| [`flyback.ipes`](./flyback.ipes) | Power Electronics | **Flyback Isolated DC-DC Converter** | Galvanically isolated converter with magnetically coupled inductors (k=0.999). |
| [`closed_loop_buck.ipes`](./closed_loop_buck.ipes) | Power Electronics | **Closed-Loop Regulated Buck (PI Control)** | Voltage-mode DC-DC converter regulated to 12.0V by embedded PI script block. |
| [`inverter.ipes`](./inverter.ipes) | Power Electronics | **Single-Phase H-Bridge Inverter (50 Hz)** | Full-bridge DC-AC inverter converting 40V DC bus into 50 Hz sinusoidal AC. |
| [`pfc_boost.ipes`](./pfc_boost.ipes) | Power Electronics | **Active PFC Boost Pre-Regulator (50 Hz)** | Active Power Factor Correction converter regulating 24V AC to 50V DC bus. |
| [`rectifier.ipes`](./rectifier.ipes) | Power Electronics | **Diode Bridge Rectifier (AC to DC)** | Full-bridge Graetz diode rectifier with capacitive reservoir smoothing filter. |
| [`three_scopes.ipes`](./three_scopes.ipes) | Analog & Filter | **Multi-Scope RLC (SCOPE.1 & SCOPE.2)** | Second-order series RLC resonator with multi-instrument oscilloscope monitoring. |
| [`rlc.ipes`](./rlc.ipes) | Analog & Filter | **RLC Resonant Circuit** | Underdamped second-order series RLC transient step response. |
| [`rc.ipes`](./rc.ipes) | Analog & Filter | **RC Low-Pass Filter** | First-order RC filter demonstrating step response and exponential charging. |

## Usage

1. **In GeckoCIRCUITS GUI**: Select **File → Open Example...** from the top menu to browse with search and descriptions, or click **Open Examples Folder in Explorer** to browse this directory.
2. **Open directly**: In GeckoCIRCUITS, choose **File → Open...** and select any `.ipes` file from this folder.
3. **Headless / Automation**: Use `scripts/verify-examples.py` or the REST API `POST /gecko/api/v1/simulations` to simulate these files programmatically.
