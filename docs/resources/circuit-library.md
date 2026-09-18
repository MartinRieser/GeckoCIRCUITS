---
title: Circuit Library
description: Browse all available circuit files included with GeckoCIRCUITS
---

# Circuit Library

GeckoCIRCUITS includes 100+ circuit example files (`.ipes`) organized by topic.

## How to Open

### In the Desktop App
- **Double-click** any `.ipes` file in your file manager to open it directly.
- Or launch **GeckoCIRCUITS**, choose **File > Open**, and select the file from `resources/` (or drag-and-drop onto the canvas).

### From Source (Web Editor)
```bash
# Windows
run-web-editor.bat

# Linux / macOS
./run-web-editor.sh
```
Inside the editor window, click **File > Open** or drag-and-drop the `.ipes` file directly onto the canvas.


## Getting Started

| File | Description |
|------|-------------|
| `1xx.../101.../ex_1.ipes` | First simulation example |
| `1xx.../103.../ex_3_pwm.ipes` | PWM basics |

## DC-DC Converters

| File | Topology |
|------|----------|
| `2xx.../201.../buck_simple.ipes` | Buck converter |
| `2xx.../201.../A_Buck.ipes` | Buck with control |
| `2xx.../202.../boost_simple.ipes` | Boost converter |
| `2xx.../202.../B_Boost.ipes` | Boost with control |
| `2xx.../203.../buckBoost_simple.ipes` | Buck-Boost |
| `2xx.../203.../cuk_simple.ipes` | Cuk converter |
| `2xx.../203.../sepic_simple.ipes` | SEPIC converter |

## AC-DC Rectifiers

| File | Topology |
|------|----------|
| `3xx.../301.../2phaseDiodeBridge_AC-Inductor.ipes` | Single-phase diode bridge rectifier with AC inductor |
| `3xx.../301.../2phaseDiodeBridge_DC-Inductor.ipes` | Single-phase diode bridge rectifier with DC inductor |
| `3xx.../301.../A_Rectifier.ipes` | Uncontrolled full-wave rectifier |
| `3xx.../302.../boostPFC.ipes` | Single-phase boost PFC open-loop stage |
| `3xx.../302.../boostPFC_currentControl.ipes` | Boost PFC with average current mode control |
| `3xx.../303.../three-phase_ViennaRectifier_simpleControl_250kW.ipes` | Three-phase Vienna rectifier (250 kW) |

## Automotive & Battery Charging

| File | Description |
|------|-------------|
| `resources/examples/automotive/dc_fast_charger/11kw_three_phase_vienna_charger.ipes` | 11 kW 3-phase Vienna active PFC DC fast charger |
| `resources/projects/interleaved_pfc_50v.ipes` | Interleaved boost PFC converter stage |
| `resources/projects/llc_resonant_400v_24v.ipes` | 400V to 24V isolated LLC resonant DC-DC converter |

## DC-AC Inverters

| File | Topology |
|------|----------|
| `4xx.../401.../singlePhase_PWM_converter.ipes` | Single-phase inverter |
| `4xx.../402.../inverter.ipes` | Three-phase PWM inverter |
| `4xx.../402.../three-phase_VSR_simpleControl_250kW.ipes` | Three-phase VSR (250 kW) |

## Thermal Simulation

| File | Description |
|------|-------------|
| `5xx.../501.../BuckBoost_thermal.ipes` | Buck-boost with thermal model |
| `5xx.../502.../ThreePhase-VSR_10kW_thermal.ipes` | Three-phase thermal loss analysis |
| `5xx.../502.../ThreePhase-VSR_10kW_thermal_with_java.ipes` | Thermal simulation with Java coupling |

## EMI Filters

| File | Description |
|------|-------------|
| `6xx.../602.../CMFilter1Stage.ipes` | Common-mode filter (1-stage) |
| `6xx.../602.../CMFilter2Stage.ipes` | Common-mode filter (2-stage) |
| `6xx.../602.../DMFilter1Stage.ipes` | Differential-mode filter (1-stage) |
| `6xx.../602.../DMFilter2Stage.ipes` | Differential-mode filter (2-stage) |

## Scripting Examples

| File | Description |
|------|-------------|
| `7xx.../701.../GeckoSCRIPT.ipes` | GeckoSCRIPT demo |
| `7xx.../701.../buck_control.ipes` | Script-controlled buck converter |
| `7xx.../704.../demo_JAVA_Block.ipes` | Java block demo |
| `7xx.../704.../JavaBlockPMSM.ipes` | PMSM with Java control |

## Advanced Topics

| File | Description |
|------|-------------|
| `8xx.../801.../UltraSparseMatrixConverter.ipes` | Ultra-sparse matrix converter |
| `8xx.../802.../indirect_matrix_java_with_PMSM_control.ipes` | Indirect matrix converter PMSM control |
| `8xx.../802.../sparseMatrix_java_with_PMSM_control.ipes` | Sparse matrix PMSM control |
| `8xx.../803.../Swiss_Rect.ipes` | Swiss rectifier optimization model |
| `8xx.../804.../ThyristorControlBlock.ipes` | Thyristor control block circuit |
| `8xx.../804.../thyristor_RL_3phBridge.ipes` | Three-phase thyristor bridge rectifier |

## Analog Circuits

| File | Description |
|------|-------------|
| `2xx.../204.../OpAmp.ipes` | Operational amplifier basic circuits |
| `2xx.../204.../opamp_invertingIntegrator.ipes` | Op-amp inverting integrator |
| `2xx.../204.../opamp_invertingDifferentiator.ipes` | Op-amp inverting differentiator |
| `2xx.../204.../opamp_3rdOrderBessel.ipes` | 3rd-order Bessel active filter |

## Contributing Circuits

See the [Contributing Guide](contributing.md) for how to add circuits to the library.
