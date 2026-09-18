---
title: Download
description: Download GeckoCIRCUITS installers, packages, and examples
---

# Download GeckoCIRCUITS

## Latest Release

[:material-download: Download GeckoCIRCUITS v3.0.0](https://github.com/MartinRieser/GeckoCIRCUITS/releases/latest){ .md-button .md-button--primary }
[:material-file-document: View Release Notes](../releases/3000.md){ .md-button }

## System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **OS** | Windows 10 (x64), Linux (x86_64 / arm64), macOS 11+ | Windows 11, Ubuntu 22.04+, macOS 12+ (Apple Silicon or Intel) |
| **Java** | *Bundled with desktop app* (No installation needed) | Java 25 (if building from source) |
| **RAM** | 4 GB | 8 GB+ |
| **Disk** | 500 MB | 1 GB |
| **Display** | 1280x720 | 1920x1080+ |

## Platform Packages

Installers and portable bundles are published on the [GitHub Releases](https://github.com/MartinRieser/GeckoCIRCUITS/releases) page:

=== "Windows"

    **Installer:** `GeckoCIRCUITS_<version>_x64-setup.exe` or `.msi`
    
    1. Download the installer.
    2. Run the setup wizard (Start Menu, Desktop shortcut, and `.ipes` file association are configured automatically).
    3. Launch **GeckoCIRCUITS**.

    **Portable (Classic GUI):** `GeckoCIRCUITS-<version>-windows-x64-portable.zip`
    - Legacy zero-install archive with `run-gecko.bat`. For the new GUI, use the installer above or `run-web-editor.bat`.


=== "macOS"

    **Disk Image:** `GeckoCIRCUITS_<version>_x64.dmg` (Intel) or `_aarch64.dmg` (Apple Silicon)
    
    1. Open the `.dmg` image and drag **GeckoCIRCUITS** to Applications.
    2. On first launch, right-click the app and choose **Open** if prompted by Gatekeeper.

=== "Linux"

    **Packages:** `gecko-circuits_<version>_amd64.deb`, `*.rpm`, or `*.AppImage`
    
    ```bash
    # Debian / Ubuntu
    sudo dpkg -i gecko-circuits_<version>_amd64.deb

    # Fedora / RHEL
    sudo rpm -i gecko-circuits-<version>-1.x86_64.rpm
    ```

    **Portable:** `GeckoCIRCUITS-<version>-linux-x64-portable.tar.gz`

## Run from Source (Web Editor)

For contributors and developers who want to run from source:

```bash
git clone https://github.com/MartinRieser/GeckoCIRCUITS.git
cd GeckoCIRCUITS

# Starts the simulation engine on localhost:8080 and opens the web editor
run-web-editor.bat        # Windows
./run-web-editor.sh       # Linux / macOS
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
