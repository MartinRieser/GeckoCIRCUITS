---
title: Packaging & Distribution
description: Building GeckoCIRCUITS desktop installers, headless JARs, and Docker images
---

# Packaging & Distribution

GeckoCIRCUITS ships in three distribution forms:

1. **Desktop application (`desktop/`)** — A Tauri 2 native shell bundling the React frontend, Spring Boot engine (`gecko-rest-api.jar`), MCP server (`gecko-mcp.jar`), and a custom JDK 25 `jlink` runtime. See [Desktop Application](desktop-app.md) for runtime architecture and log locations.
2. **Standalone JARs (`backend/`)** — Executable Spring Boot REST API and shaded MCP server JARs.
3. **Docker container (`docker/`)** — Alpine JRE 25 image running the REST API with embedded web UI.

---

## 1. Desktop Installers (Tauri 2)

Native installers (NSIS `.exe` / `.msi` on Windows, `.dmg` on macOS, `.deb` / `.rpm` / AppImage on Linux) are built via `scripts/desktop/build-all.bat` (Windows MSVC), `scripts/desktop/build-all.sh` (Linux/macOS), or `.github/workflows/desktop.yml`.

### Prerequisites

- **JDK 25** (Eclipse Temurin or equivalent with `jdeps` and `jlink`)
- **Node.js 22+** and npm
- **Python 3** (for `scripts/desktop/build-engine.py`)
- **Rust stable** + `@tauri-apps/cli@^2` (`stable-x86_64-pc-windows-msvc` required on Windows)
- **Linux system libraries** (Debian/Ubuntu): `libwebkit2gtk-4.1-dev`, `libgtk-3-dev`, `libayatana-appindicator3-dev`, `librsvg2-dev`, `libxdo-dev`, `libssl-dev`, `libfuse2`, `patchelf`, `rpm`, `fakeroot`

### One-Shot Build

```bash
# Linux / macOS
./scripts/desktop/build-all.sh

# Windows (MSVC toolchain required)
scripts\desktop\build-all.bat
```

### Step-by-Step Build

```bash
# 1. Build frontend assets into backend/gecko-rest-api static resources
npm --prefix frontend run build:spring

# 2. Build REST API and MCP server JARs
mvn -pl backend/gecko-rest-api,backend/gecko-mcp -am package -DskipTests

# 3. Assemble desktop/app/engine/ (jlink runtime, JARs, gecko_c_block.h, VERSION) and smoke test
python scripts/desktop/build-engine.py

# 4. Build platform installers in desktop/app/src-tauri/target/release/bundle/
cd desktop/app
npx @tauri-apps/cli@^2 build
```

`build-engine.py` finishes with a smoke test running `gecko.core.GeckoHeadless` against `tools/parity/circuits/rc-lowpass.ipes` through the bundled `jlink` runtime via Spring Boot's `PropertiesLauncher`.

### Automated Release CI

`.github/workflows/desktop.yml` runs on tag pushes (`v*`) or manual `workflow_dispatch`:

- Synchronizes app version via `scripts/desktop/set-version.py`
- Builds installers across `windows-latest`, `macos-latest`, and `ubuntu-latest`
- Optionally publishes GitHub release artifacts when triggered on a version tag or with `upload_release: true`

---

## 2. Standalone Server & MCP JARs

```bash
# Build all backend modules and run unit/integration tests
mvn clean verify

# Built artifacts:
#   backend/gecko-rest-api/target/gecko-rest-api.jar   (Spring Boot REST + UI server)
#   backend/gecko-mcp/target/gecko-mcp.jar             (shaded stdio MCP server)
```

Run the REST API + Web UI locally on `http://localhost:8080/gecko/`:

```bash
java -jar backend/gecko-rest-api/target/gecko-rest-api.jar
```

Run the headless CLI directly from the Spring Boot JAR:

```bash
java -Dloader.main=gecko.core.GeckoHeadless \
     -cp backend/gecko-rest-api/target/gecko-rest-api.jar \
     org.springframework.boot.loader.launch.PropertiesLauncher \
     --circuit resources/projects/buck_sampled_50v_to_10v.ipes --output results.csv --quiet
```

---

## 3. Docker Container

`docker/Dockerfile.api` builds a multi-stage `eclipse-temurin:25-jre-alpine` image exposing port `8080` with a `/gecko/api/health` health check:

```bash
# Using docker compose from docker/
docker compose -f docker/docker-compose.yml up --build

# Or build directly from backend/
docker build -f docker/Dockerfile.api -t geckocircuits-api backend/
docker run -p 8080:8080 geckocircuits-api
```
