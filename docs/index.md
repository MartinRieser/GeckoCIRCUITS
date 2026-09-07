---
title: Home
description: GeckoCIRCUITS - Power Electronics Circuit Simulator
---

# GeckoCIRCUITS

<div class="grid cards" markdown>

-   :material-lightning-bolt:{ .lg .middle } **Power Electronics Simulation**

    ---

    Simulate DC-DC, AC-DC, DC-AC converters with multi-domain support for electrical, thermal, and EMI analysis.

    [:octicons-arrow-right-24: Getting Started](getting-started/index.md)

-   :material-school:{ .lg .middle } **50+ Tutorials**

    ---

    Step-by-step guides from basic circuits to advanced control systems, motor drives, and HVDC applications.

    [:octicons-arrow-right-24: Tutorials](tutorials/index.md)

-   :material-file-document-multiple:{ .lg .middle } **100+ Examples**

    ---

    Ready-to-run circuit files covering automotive, renewable energy, and industrial applications.

    [:octicons-arrow-right-24: Examples](examples/index.md)

-   :material-code-braces:{ .lg .middle } **Desktop App + Automation**

    ---

    A self-contained desktop app — plus REST API and MCP tools to drive simulations from MATLAB, Python, or an LLM.

    [:octicons-arrow-right-24: API Reference](api/index.md)

</div>

## What is GeckoCIRCUITS?

GeckoCIRCUITS is an open-source circuit simulator specialized for power electronics. It provides:

- **Desktop app + web editor** - Modern UI, no Java installation needed
- **Multi-domain simulation** - Electrical, thermal, magnetic, and mechanical
- **Real-time visualization** - Scope view with zoom, cursors, FFT, and THD
- **Firmware-in-the-loop** - Run your real C/C++ control code via NativeC blocks
- **MCP interface** - Let Claude, Cursor, or any LLM build and tune circuits
- **Automation** - REST API for MATLAB, Python, and CI integration
- **Extensive component library** - Switches, diodes, transformers, motors
- **Advanced analysis** - Thermal modeling and semiconductor loss calculation

## Quick Example

```
┌─────────────────────────────────────────┐
│          Buck Converter                  │
│                                          │
│   Vin ──┬──[S]──┬──[L]──┬── Vout        │
│         │       │       │               │
│         │      [D]     [C]    [R]       │
│         │       │       │     │         │
│   GND ──┴───────┴───────┴─────┴── GND   │
│                                          │
│   Vout = D × Vin                        │
└─────────────────────────────────────────┘
```

## Getting Started

=== "Desktop App (recommended)"

    1. Download the installer for your OS from the
       [Releases](https://github.com/MartinRieser/GeckoCIRCUITS/releases) page.
    2. Install and launch **GeckoCIRCUITS** — no Java required.
    3. Open an example from the **Examples** menu and press **Run**.

=== "Build from Source"

    ```bash
    git clone https://github.com/MartinRieser/GeckoCIRCUITS.git
    cd GeckoCIRCUITS

    # web editor: engine on localhost:8080 + editor in the browser
    run-web-editor.bat        # Windows
    ./run-web-editor.sh       # Linux / macOS
    ```

See the [Installation Guide](getting-started/installation.md) for details.

## Tutorial Roadmap

```mermaid
graph LR
    A[101 First Simulation] --> B[102 Basic Circuits]
    B --> C[103 PWM Basics]
    C --> D[201 Buck Converter]
    D --> E[202 Boost Converter]
    E --> F[Advanced Topics]

    C --> G[301 Diode Rectifier]
    G --> H[302 PFC Basics]

    D --> I[501 Loss Calculation]
    I --> J[502 Junction Temp]
    J --> K[503 Heatsink Design]
```

## Featured Examples

| Example | Description | Difficulty |
|---------|-------------|------------|
| [Buck Converter](examples/basic/buck.md) | Step-down DC-DC with PWM control | Beginner |
| [Boost PFC](tutorials/acdc/pfc-basics.md) | Power factor correction | Intermediate |
| [PMSM FOC](examples/motor-drives/pmsm-foc.md) | Field-oriented motor control | Advanced |
| [EV Charger](examples/automotive/ev-charger.md) | Level 2 AC charging | Intermediate |
| [MMC Converter](tutorials/dcac/mmc-converter.md) | Modular multilevel for HVDC | Advanced |

## Comparison with Other Tools

| Feature | GeckoCIRCUITS | PLECS | PSIM | SIMBA | LTspice | QSPICE |
|---------|---------------|-------|------|-------|---------|--------|
| **Licensing** | Open Source | Commercial | Commercial | Commercial | Freeware | Freeware |
| Power Electronics Focus | ✅ | ✅ | ✅ | ✅ | ⚠️ General | ✅ |
| Ideal Switch Models | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Thermal Simulation | ✅ Built-in | ✅ Built-in | ✅ Add-on | ✅ Built-in | ⚠️ Manual | ⚠️ SPICE |
| Magnetic Domain | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Mechanical Domain | ✅ | ✅ | ✅ | ✅ | ⚠️ Manual | ⚠️ Manual |
| Motor Models (PMSM/BLDC/IM) | ✅ | ✅ | ✅ Add-on | ✅ | ⚠️ Manual | ⚠️ Manual |
| MATLAB/Python Integration | ✅ REST + RMI | ✅ Blockset | ✅ Co-sim | ✅ Python | ❌ | ❌ |
| Python Scripting | ✅ | ✅ XML-RPC | ✅ API | ✅ Native | ❌ | ✅ |
| C/C++ Custom Blocks | ✅ FFM (dll/so/dylib) | ✅ C-Script | ✅ C-block | ✅ C-code | ❌ | ✅ C++/Verilog |
| Code Generation | ❌ | ✅ Coder | ✅ | ❌ | ❌ | ❌ |
| REST API + MCP (LLM) | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Cloud/Online Version | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Analysis Tools (Bode, SS) | ✅ | ✅ | ✅ | ✅ AC Sweep | ⚠️ AC | ⚠️ AC |
| EMI/Conducted EMC | ⚠️ LISN models | ⚠️ Limited | ✅ | ⚠️ | ✅ | ✅ |
| SiC/GaN Device Models | ✅ | ✅ | ✅ | ✅ | ⚠️ | ✅ Native |
| PSIM Import | ❌ | ❌ | N/A | ✅ | ❌ | ❌ |

**Legend:** ✅ = Full support | ⚠️ = Partial/Manual | ❌ = Not available

### Tool Characteristics

| Tool | Best For | Website |
|------|----------|---------|
| **GeckoCIRCUITS** | Power electronics education, MATLAB integration, open-source | [GitHub](https://github.com/MartinRieser/GeckoCIRCUITS) |
| **PLECS** | Professional power electronics, Simulink co-sim, code generation | [plexim.com](https://www.plexim.com) |
| **PSIM** | Motor drives, SmartCtrl design, embedded code | [altair.com/psim](https://altair.com/psim) |
| **SIMBA** | Modern UI, Python-first workflow, cloud simulation | [simba.io](https://www.simba.io) |
| **LTspice** | Analog circuits, component-level SPICE | [analog.com](https://www.analog.com/ltspice) |
| **QSPICE** | Mixed-signal, SiC/GaN power devices, fast simulation | [qorvo.com](https://www.qorvo.com/design-hub/design-tools/interactive/qspice) |

## Community

- :fontawesome-brands-github: [GitHub Repository](https://github.com/MartinRieser/GeckoCIRCUITS)
- :material-bug: [Issue Tracker](https://github.com/MartinRieser/GeckoCIRCUITS/issues)
- :material-forum: [Discussions](https://github.com/MartinRieser/GeckoCIRCUITS/discussions)

## Citation

If you use GeckoCIRCUITS in your research, please cite:

```bibtex
@software{geckocircuits,
  title = {GeckoCIRCUITS: Power Electronics Circuit Simulator},
  author = {GeckoCIRCUITS Team},
  year = {2024},
  url = {https://github.com/MartinRieser/GeckoCIRCUITS}
}
```

---

<div class="grid cards" markdown>

-   :material-download:{ .lg .middle } **Download**

    Get the latest release

    [:octicons-arrow-right-24: Download](resources/download.md)

-   :material-book-open-variant:{ .lg .middle } **Documentation**

    Complete user guide

    [:octicons-arrow-right-24: Docs](getting-started/index.md)

-   :material-help-circle:{ .lg .middle } **Support**

    FAQ and troubleshooting

    [:octicons-arrow-right-24: Help](resources/faq.md)

</div>
