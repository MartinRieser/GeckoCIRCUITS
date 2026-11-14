# VS Code Configuration for GeckoCIRCUITS

This directory contains VS Code configuration files for building, running, and debugging the GeckoCIRCUITS Java 25 backend modules (`gecko-simulation-core`, `gecko-rest-api`, `gecko-mcp`) and frontend tests.

## Quick Start

### 1. Install Recommended Extensions

When opening the repository, VS Code prompts to install the extensions in `.vscode/extensions.json`:

- **Extension Pack for Java** (`vscjava.vscode-java-pack`)
- **Maven for Java** (`vscjava.vscode-maven`)
- **Debugger for Java** (`vscjava.vscode-java-debug`)

Ensure JDK 25 is configured on `JAVA_HOME` (or selected in VS Code Java Runtime settings).

### 2. Build and Test

- Press `Ctrl+Shift+B` (`Cmd+Shift+B` on macOS) to run the default build task (`Build Backend Packages (Skip Tests)`).
- Open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and run `Tasks: Run Task` to access:
  - `Build Backend Packages (Skip Tests)`
  - `Build & Verify All Backend Modules` (`mvn verify --no-transfer-progress`)
  - `Run Backend Tests` (`mvn test --no-transfer-progress`)
  - `Run Frontend Tests` (`npm --prefix frontend test`)
  - `Clean Build Directories` (`mvn clean`)
  - `Maven: Resolve Dependencies` (`mvn dependency:resolve`)

### 3. Launch & Debug

Open the Run and Debug view (`Ctrl+Shift+D`) and select a configuration:

| Configuration | Entry Point | Purpose |
|---------------|-------------|---------|
| **Run GeckoCIRCUITS REST API** | `gecko.rest.GeckoRestApiApplication` | Build and start the Spring Boot API/UI server on port 8080 (`/gecko`) |
| **Run GeckoCIRCUITS REST API (No Build)** | `gecko.rest.GeckoRestApiApplication` | Start immediately without running Maven first |
| **Run GeckoCIRCUITS MCP Server** | `gecko.mcp.GeckoMcpServer` | Start the standalone stdio MCP server |
| **Run Headless Simulation (Current File)** | `gecko.core.GeckoHeadless` | Simulate the currently open `.ipes` file headlessly |

## Files in This Directory

- `tasks.json`: Maven and Vitest task definitions
- `launch.json`: Java 25 run/debug launch configurations
- `settings.json`: Workspace Java, Maven, formatting, and file exclusion settings
- `extensions.json`: Recommended VS Code extensions

## Additional Documentation

- See [README.md](../README.md) for quick-start launchers (`run-gecko.ps1` / `run-gecko.sh` / `run-gecko.bat`).
- See [Developer Guide](../docs/resources/developer-guide.md) for full architecture and build details.
