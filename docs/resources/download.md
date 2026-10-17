---
title: Download
description: Download GeckoCIRCUITS installers, packages, and examples
---

# Download GeckoCIRCUITS

## Latest Release

[:material-download: Download GeckoCIRCUITS Latest Release](https://github.com/MartinRieser/GeckoCIRCUITS/releases/latest){ .md-button .md-button--primary }
[:material-file-document: View GitHub Releases](https://github.com/MartinRieser/GeckoCIRCUITS/releases){ .md-button }

## System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **OS** | Windows 10 (x64), Linux (x86_64 / arm64), macOS 12+ | Windows 11, Ubuntu 22.04+, macOS 13+ (Apple Silicon or Intel) |
| **Java** | *Bundled with desktop app* (No installation needed) | Java 25 (if building from source) |
| **RAM** | 4 GB | 8 GB+ |
| **Disk** | 500 MB | 1 GB |
| **Display** | 1280x720 | 1920x1080+ |

## Platform Packages

Native standalone installers bundling the React editor and simulation engine are published on the [GitHub Releases](https://github.com/MartinRieser/GeckoCIRCUITS/releases) page:

=== "Windows"

    **Installers:** `GeckoCIRCUITS_<version>_x64-setup.exe` (NSIS) or `.msi` (WiX)
    
    1. Download the installer package.
    2. Run the setup wizard (Start Menu, Desktop shortcut, and `.ipes` file association are configured automatically).
    3. Launch **GeckoCIRCUITS**.

=== "macOS"

    **Disk Image:** `GeckoCIRCUITS_<version>_aarch64.dmg`
    
    1. Open the `.dmg` image and drag **GeckoCIRCUITS** into your Applications folder.
    2. On first launch, right-click the app and choose **Open** if prompted by macOS Gatekeeper.

=== "Linux"

    **Packages:** `GeckoCIRCUITS_<version>_amd64.deb` or `GeckoCIRCUITS-<version>-1.x86_64.rpm`
    
    ```bash
    # Debian / Ubuntu / Linux Mint
    sudo dpkg -i GeckoCIRCUITS_<version>_amd64.deb

    # Fedora / RHEL / openSUSE
    sudo rpm -i GeckoCIRCUITS-<version>-1.x86_64.rpm
    ```

## Run from Source

For contributors and developers who want to run from source:

```bash
git clone https://github.com/MartinRieser/GeckoCIRCUITS.git
cd GeckoCIRCUITS

# Starts the simulation engine on localhost:8080 and opens the editor
run-gecko.bat        # Windows
./run-gecko.sh       # Linux / macOS
```

Requirements for running from source: JDK 25, Maven 3.8+, and Node.js 22+.

## Examples Package

Download ready-to-run circuit examples directly from GitHub:

[:material-folder-download: Browse Circuit Examples on GitHub](https://github.com/MartinRieser/GeckoCIRCUITS/tree/main/resources/examples){ .md-button }

The library contains 100+ circuits covering:
- Basic topologies (Buck, Boost, Flyback, Forward)
- Power supplies (LLC, DAB, PFC)
- Motor drives (BLDC, PMSM FOC, Induction)
- Automotive systems (EV Charger, OBC, DCFC, Traction)
- Thermal loss and heatsink design

See the [Circuit Library Guide](circuit-library.md) for full descriptions.

## License

GeckoCIRCUITS is open source software:

- **Open Source License**: [GNU General Public License v3.0](https://github.com/MartinRieser/GeckoCIRCUITS/blob/main/LICENSE) for academic, research, and open-source applications.
- **Commercial Licensing**: Contact the GeckoCIRCUITS team for commercial licensing options.
