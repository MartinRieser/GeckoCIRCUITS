# GeckoCIRCUITS REST API

Headless REST API for circuit simulation, editing, and signal analysis using `gecko-simulation-core`.

## Context Path & Base URL

The Spring Boot application serves all routes under `/gecko` (`server.servlet.context-path=/gecko` in `src/main/resources/application.properties`):

- **Health Check**: `http://localhost:8080/gecko/api/health`
- **API Metadata**: `http://localhost:8080/gecko/api/info`
- **Swagger UI**: `http://localhost:8080/gecko/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/gecko/api-docs`

When started by the Tauri desktop shell, the backend binds to `127.0.0.1:<ephemeral port>` and prints `GECKO_READY http://127.0.0.1:<port>/gecko` to stdout.

## Quick Start

### Using Maven

Run from the repository root:

```bash
# Build and install gecko-simulation-core, then start the REST API
mvn install -DskipTests -pl backend/gecko-simulation-core
mvn spring-boot:run -pl backend/gecko-rest-api
```

### Using Docker

```bash
# Build and start via Compose (from repository root)
docker compose -f docker/docker-compose.yml up --build -d

# Or using helper scripts
./scripts/build-docker.sh
./scripts/run-docker.sh
```

## API Endpoints

### Health & Info
- `GET /gecko/api/health` - Health check
- `GET /gecko/api/info` - API metadata
- `GET /gecko/api/docs` - Documentation links

### Simulations & Streaming
- `POST /gecko/api/v1/simulations` - Submit asynchronous simulation
- `POST /gecko/api/v1/simulations/batch` - Submit parameter sweep or batch simulations
- `GET /gecko/api/v1/simulations` - List simulations
- `GET /gecko/api/v1/simulations/{id}` - Get simulation status
- `GET /gecko/api/v1/simulations/{id}/results` - Get simulation waveforms
- `GET /gecko/api/v1/simulations/{id}/results/{signal}` - Get specific signal
- `GET /gecko/api/v1/simulations/{id}/progress` - Get simulation progress
- `GET /gecko/api/v1/simulations/{id}/stream` - Stream progress updates via SSE
- `GET /gecko/api/v1/simulations/{id}/ws-info` - WebSocket / STOMP subscription details (`/ws`, `/ws-raw`)
- `DELETE /gecko/api/v1/simulations/{id}` - Cancel simulation
- `POST /gecko/api/v1/simulations/{id}/export` - Export results as CSV

### Circuits, Editing, Analysis & Losses
- `POST /gecko/api/v1/circuits` / `GET /gecko/api/v1/circuits/{id}` - Load, inspect, clone, export `.ipes` files (`CircuitFileController`)
- `POST/PATCH/DELETE /gecko/api/v1/circuits/{id}/components` & `.../connections` - Interactive circuit editing (`CircuitEditController`)
- `GET /gecko/api/v1/examples` - List, load, update, or delete `.ipes` examples (`ExamplesController`)
- `POST /gecko/api/v1/analysis/fourier` & `.../characteristics` - FFT / harmonic and waveform metrics (`AnalysisController`)
- `POST /gecko/api/v1/loss/switching`, `.../conduction`, `.../detailed` - Semiconductor loss calculation (`LossCalculationController`)

## Build & Test

```bash
# Build bootable fat JAR (output: backend/gecko-rest-api/target/gecko-rest-api.jar)
mvn clean package -pl backend/gecko-rest-api

# Run REST API tests, Checkstyle, and PMD
mvn test checkstyle:check pmd:check -pl backend/gecko-rest-api \
  -D"checkstyle.config.location=../../checkstyle.xml" \
  -D"pmd.rulesets=../../pmd-ruleset.xml"
```

## Architecture

- **Spring Boot 4.1.1 / Java 25** - WebMVC, WebSocket (STOMP + raw), Validation, Spring Security
- **SpringDoc OpenAPI 3.1** - Swagger UI and OpenAPI schema generation
- **gecko-simulation-core** - Headless MNA, control, thermal, and magnetic simulation engine (GUI-free, enforced by Maven Enforcer)

## Security & Logging

- Optional `X-API-Key` authentication controlled via `gecko.api.auth-enabled` and `gecko.api.keys` in `application.properties`
- Rolling log written to `${GECKO_LOG_DIR:logs}/rest-api.log`

## License

GPL v3 - See `LICENSE` in the repository root.
