---
title: REST API
description: HTTP API for remote simulation control
---

# REST API

!!! success "Live in v2.16.0+"
    The REST API is fully functional and running in production. This page documents the implemented endpoints and usage examples. Built on Spring Boot 3.2 with `gecko-simulation-core`, enabling headless simulation without GUI dependencies.

The REST API provides an HTTP/REST interface for controlling GeckoCIRCUITS programmatically. It is built on Spring Boot 3.2 and uses the `gecko-simulation-core` module for all simulation logic, enabling cloud deployment, CI/CD automation, and remote computation.

## Quick Start

Deploy and test the REST API with Docker:

```bash
# Start the API server (port 8080)
docker-compose up -d

# Check health
curl http://localhost:8080/actuator/health

# View API documentation
# Open http://localhost:8080/swagger-ui.html in your browser
```

## Architecture

```
┌──────────────┐     HTTP      ┌──────────────────┐
│   Python     │◄─────────────►│  gecko-rest-api   │
│   Browser    │  port 8080    │  (Spring Boot)    │
│   curl       │              │  Uses gecko-core  │
└──────────────┘              └──────────────────┘
```

## API Documentation

Full interactive documentation available at:

**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)** (when running)

OpenAPI specification:

```
GET /v3/api-docs
```

## Authentication

!!! info "Optional in v2.22.0+"
    API key authentication is **disabled by default** for backward compatibility. Enable it in production deployments for security.

### Configuration

API key authentication is controlled via `application.properties`:

```properties
# Enable/disable authentication (default: false)
gecko.api.auth-enabled=true

# Comma-separated list of valid API keys
gecko.api.keys=my-key-1,my-key-2,my-key-3
```

Or via environment variables:

```bash
export GECKO_API_AUTH_ENABLED=true
export GECKO_API_KEYS=my-key-1,my-key-2,my-key-3
```

### Usage

When authentication is enabled, all protected endpoints require the `X-API-Key` header:

#### curl

```bash
# With API key
curl -H "X-API-Key: my-secret-key" \
  http://localhost:8080/api/v1/simulations

# Without API key (only works when auth-enabled=false)
curl http://localhost:8080/api/v1/simulations
```

#### Python

```python
import requests

session = requests.Session()
session.headers.update({"X-API-Key": "my-secret-key"})

# All requests include the API key automatically
response = session.post(
    "http://localhost:8080/api/v1/simulations",
    json={"circuitFile": "path/to/circuit.ipes"}
)

if response.status_code == 401:
    print("Invalid API key")
else:
    print(response.json())
```

#### JavaScript/Node.js

```javascript
const axios = require('axios');

const client = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'X-API-Key': 'my-secret-key'
  }
});

client.post('/api/v1/simulations', {
  circuitFile: 'path/to/circuit.ipes'
})
.then(response => console.log(response.data))
.catch(error => {
  if (error.response.status === 401) {
    console.error('Unauthorized: Invalid API key');
  }
});
```

### Error Response

Invalid or missing API key returns **401 Unauthorized**:

```json
{
  "error": "Unauthorized",
  "message": "Valid X-API-Key header required"
}
```

### Public Endpoints

The following endpoints do not require API authentication:

| Path | Description |
|------|-------------|
| `/api/v1/health` | Health check |
| `/actuator/health` | Actuator health |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI specification |
| `/ws/**` | WebSocket endpoint |
| `/ws-raw/**` | Raw WebSocket endpoint |

### Security Best Practices

1. **Always use HTTPS** in production:
   ```bash
   # Good: HTTPS protects the API key in transit
   curl -H "X-API-Key: my-key" https://your-domain.com/api/v1/simulations

   # Bad: HTTP exposes the key in plain text
   curl -H "X-API-Key: my-key" http://your-domain.com/api/v1/simulations
   ```

2. **Rotate keys regularly** - Update keys quarterly or when team changes:
   ```properties
   gecko.api.keys=new-key-1,old-key-1,emergency-key
   ```

3. **Use environment variables** - Never hardcode keys:
   ```bash
   export GECKO_API_KEYS=${SECRETS_MANAGER_GET_API_KEYS}
   java -jar gecko-rest-api.jar
   ```

4. **Different keys per environment**:
   ```bash
   # Development
   GECKO_API_KEYS=dev-key-123

   # Production
   GECKO_API_KEYS=prod-key-456,backup-prod-key-789
   ```

For more details, see [v2.22.0 Release Notes](../releases/2220.md#security-considerations).

## Live Endpoints

### Loss Calculation Endpoints

Calculate power loss for semiconductor devices.

**Switching Loss** (Voltage and Energy Scaling)

```bash
POST /api/v1/loss/switching
Content-Type: application/json
```

```json
{
  "voltage": 400,
  "current": 50,
  "frequency": 20000,
  "temperature": 125,
  "referenceVoltage": 600,
  "referenceCurrent": 100,
  "referenceFrequency": 20000,
  "referenceTemperature": 125,
  "e_on_ref": 5.2e-5,
  "e_off_ref": 3.8e-5
}
```

**Conduction Loss** (Resistance Model)

```bash
POST /api/v1/loss/conduction
```

**Detailed Loss** (Temperature-Dependent Interpolation)

```bash
POST /api/v1/loss/detailed
```

### Circuit File Endpoints

**Parse Circuit File**

```bash
POST /api/v1/circuits/parse
Content-Type: multipart/form-data

curl -F "file=@circuit.ipes" http://localhost:8080/api/v1/circuits/parse
```

**Get Circuit Metadata**

```bash
GET /api/v1/circuits/{circuitId}/info
```

**List Components**

```bash
GET /api/v1/circuits/{circuitId}/components
```

**Validate Circuit**

```bash
POST /api/v1/circuits/{circuitId}/validate
```

**List All Circuits**

```bash
GET /api/v1/circuits
```

**Delete Circuit**

```bash
DELETE /api/v1/circuits/{circuitId}
```

**Clone Circuit**

Create a deep copy of a loaded circuit with optional parameter overrides.

```bash
POST /api/v1/circuits/{circuitId}/clone
Content-Type: application/json
```

Request (with parameter overrides):
```json
{
  "overrides": {
    "R_load.resistance": 12.0,
    "C_dc.capacitance": 4.7e-6,
    "L1.inductance": 100e-6
  }
}
```

Response (201 Created):
```json
{
  "circuitId": "circuit-uuid-new",
  "name": "circuit_clone",
  "componentCount": 24,
  "parameters": {
    "simulationDuration": 0.02,
    "timeStep": 1e-6,
    "solverType": "trapezoidal"
  }
}
```

**Update Circuit Parameters**

Modify simulation parameters (duration, time step, solver) for a loaded circuit.

```bash
PUT /api/v1/circuits/{circuitId}/parameters
Content-Type: application/json
```

Request (all fields optional):
```json
{
  "simulationDuration": 0.05,
  "timeStep": 5e-7,
  "solverType": "gear-shichman"
}
```

**Download Circuit as .ipes File**

Serialize a loaded circuit back to the gzip-compressed `.ipes` format. The file is
interchangeable with the GeckoCIRCUITS Swing UI (upload → edit → download round trip).

```bash
GET /api/v1/circuits/{circuitId}/ipes
```

Response: `application/gzip` binary attachment.

**Replace Circuit Content**

Replace the circuit stored under the given ID with new `.ipes` content (gzip or plain
ASCII). The circuit ID stays stable — this is the save endpoint for editor clients.

```bash
PUT /api/v1/circuits/{circuitId}
Content-Type: multipart/form-data   (file=@circuit.ipes)
# or
Content-Type: application/json       ({"content": "<base64>", "filename": "x.ipes"})
```

### Circuit Editing Endpoints

All editing mutations return a change message
`{circuitId, modelVersion, operation, payload}` and broadcast it on the
WebSocket topic `/topic/circuits/{circuitId}`. Every edit is undoable.

**Create Component**

```bash
POST /api/v1/circuits/{circuitId}/components
Content-Type: application/json
```

Request:
```json
{
  "family": "LK",
  "type": 1,
  "name": "R1",
  "x": 96,
  "y": 96,
  "orientation": 503,
  "parameters": {"param0": 100.0}
}
```

`family`: `LK` or `THERM` (CONTROL/SPECIAL creation not supported yet).
`type`: type number from the catalog. Coordinates are snapped to the grid raster
(`dpix`). Omitted names are generated from the type and made unique; duplicate
explicit names return 409. Parameter keys are `param<index>`.

**Patch Component** (move / rotate / rename / set parameters — all fields optional)

```bash
PATCH /api/v1/circuits/{circuitId}/components/{name}
```

**Delete Component**

```bash
DELETE /api/v1/circuits/{circuitId}/components/{name}
```

**Create Wire** (polyline of grid points, at least two)

```bash
POST /api/v1/circuits/{circuitId}/connections
Content-Type: application/json
{"type": "LK", "points": [[96, 200], [144, 200], [144, 256]], "label": "w1"}
```

**Patch / Delete Wire** (identified by index in the connection list)

```bash
PATCH  /api/v1/circuits/{circuitId}/connections/{index}   {"points": [...], "label": "..."}
DELETE /api/v1/circuits/{circuitId}/connections/{index}
```

**Set Node Label** (terminals with equal labels are electrically connected)

```bash
PUT /api/v1/circuits/{circuitId}/nodes/{componentName}
Content-Type: application/json
{"terminalIndex": 0, "side": "x", "label": "dc_link"}
```

`side`: `x` (input/start terminal) or `y` (output/end terminal).

**Undo / Redo**

```bash
POST /api/v1/circuits/{circuitId}/undo
POST /api/v1/circuits/{circuitId}/redo
```

409 when there is nothing to undo/redo. A new edit discards the redo history.
History is bounded (200 edits per circuit) and reset when the circuit content
is replaced via `PUT /api/v1/circuits/{circuitId}`.

**Component Type Catalog**

```bash
GET /api/v1/circuits/catalog
```

Returns all placeable types with `type` number, `name` and `family`.

**Editor Model Snapshot**

```bash
GET /api/v1/circuits/{circuitId}/model
```

Returns the full editor state in one response: components (with terminal node
labels), wires (with their list indices), grid render scale (`dpix`), worksheet
size and the current `modelVersion`. This is the boot endpoint for editor clients.

Live in: **v2.20.0+**


### Simulation Endpoints

**Submit Simulation** (Async)

```bash
POST /api/v1/simulations
Content-Type: application/json
```

Request:
```json
{
  "circuitId": "previously-uploaded-circuit-uuid",
  "simulationTime": 0.01,
  "timeStep": 1e-7,
  "solverType": "backward-euler",
  "parameters": {
    "PWM.1.dutyCycle": 0.5,
    "R.1.resistance": 10.0
  }
}
```

The circuit can be referenced in one of three ways (exactly one is required):
- `circuitId` — ID of a circuit uploaded via `POST /api/v1/circuits/parse`
- `base64Circuit` — base64-encoded `.ipes` content
- `circuitFile` — server-local `.ipes` file path (requires explicit `simulationTime` and `timeStep`)

When `circuitId` or `base64Circuit` is used, `simulationTime` and `timeStep` are
optional and default to the values stored in the circuit file.

**Solver Types:**
- `backward-euler` - Implicit 1st order (stable, for stiff circuits)
- `trapezoidal` - Implicit 2nd order (balanced accuracy/stability)
- `gear-shichman` - Variable order (best for variable dynamics)

**Get Simulation Status and Results**

```bash
GET /api/v1/simulations/{simulationId}
```

**List All Simulations**

```bash
GET /api/v1/simulations
```

**Cancel Simulation**

```bash
DELETE /api/v1/simulations/{simulationId}
```

**Get Detailed Progress**

```bash
GET /api/v1/simulations/{simulationId}/progress
```

Response fields:
- `preCalcProgress` - Initial condition calculation %
- `mainSimProgress` - Main transient simulation %
- `currentStep` - Current time step number
- `totalSteps` - Total steps to complete
- `estimatedRemainingMs` - Estimated time remaining

**Export Results**

```bash
GET /api/v1/simulations/{simulationId}/export
```


**Batch Job Tracking**

Get aggregated status for all simulations in a batch submitted via v2.19.0+ batch endpoint.

```bash
GET /api/v1/simulations/batch/{batchId}
```

Response:
```json
{
  "batchId": "batch-uuid-12345",
  "totalSimulations": 20,
  "completed": 18,
  "failed": 1,
  "running": 1,
  "pending": 0,
  "overallProgress": 90,
  "done": false,
  "submittedAt": "2026-02-18T10:30:00Z",
  "simulations": {
    "sim-uuid-001": {
      "status": "completed",
      "progress": 100,
      "parameterSet": {"R1.resistance": 10.0}
    },
    "sim-uuid-002": {
      "status": "failed",
      "progress": 45,
      "parameterSet": {"R1.resistance": 50.0},
      "errorMessage": "Convergence failure at t=0.015s"
    }
  },
  "failedIds": ["sim-uuid-002"]
}
```

**Cancel Batch**

Cancels all running and pending simulations in a batch.

```bash
DELETE /api/v1/simulations/batch/{batchId}
```

Returns: `204 No Content` on success

Live in: **v2.20.0+**


### WebSocket Streaming

Real-time progress updates via WebSocket with STOMP message framing. Provides bidirectional communication capability for interactive monitoring and future client-to-server commands.

**WebSocket Endpoints**

- `/ws` - SockJS-compatible WebSocket endpoint (for browsers)
- `/ws-raw` - Raw WebSocket endpoint (for non-browser clients)

Both endpoints support STOMP protocol for message framing.

**Discover WebSocket Connection Details**

```bash
GET /api/v1/simulations/{simulationId}/ws-info
```

Response:
```json
{
  "simulationId": "9a1d5f7e-3b2c-4d9e-8f1a-6c2b4a7d9e3f",
  "stompEndpoint": "/ws",
  "rawWebSocketEndpoint": "/ws-raw",
  "subscribeDestination": "/topic/simulations/9a1d5f7e-3b2c-4d9e-8f1a-6c2b4a7d9e3f",
  "protocol": "STOMP/1.2",
  "supportsBidirectional": true,
  "baseUrl": "http://localhost:8080"
}
```

**JavaScript Browser Client (SockJS)**

```javascript
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/stomp.min.js"></script>

<script>
const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);

client.connect({}, (frame) => {
  console.log('Connected:', frame);
  
  // Subscribe to simulation progress updates
  client.subscribe('/topic/simulations/9a1d5f7e-3b2c-4d9e-8f1a-6c2b4a7d9e3f', (message) => {
    const progress = JSON.parse(message.body);
    console.log(`Progress: ${progress.progress}%`);
    console.log(`Status: ${progress.status}`);
    console.log(`Time: ${progress.currentTime}s / ${progress.endTime}s`);
    
    if (progress.status === 'COMPLETED') {
      client.disconnect();
    }
  });
});

// Graceful disconnect
window.addEventListener('beforeunload', () => {
  if (client && client.connected) {
    client.disconnect();
  }
});
</script>
```

**Python Client (stomp.py)**

```python
import stomp
import json
import time

class ProgressListener(stomp.ConnectionListener):
    def on_message(self, frame):
        progress = json.loads(frame.body)
        print(f"Progress: {progress['progress']}% | Status: {progress['status']}")
        print(f"Time: {progress['currentTime']}s / {progress['endTime']}s")

# Connect via raw WebSocket
conn = stomp.Connection([('localhost', 8080)], wait_on_receipt=True)
conn.set_listener('progress', ProgressListener())
conn.connect()

# Subscribe to simulation progress
conn.subscribe('/topic/simulations/9a1d5f7e-3b2c-4d9e-8f1a-6c2b4a7d9e3f', id=1, ack='auto')

# Keep listening
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    conn.disconnect()
```

**Raw WebSocket (wscat tool)**

```bash
# Install wscat
npm install -g wscat

# Connect to raw WebSocket endpoint
wscat -c ws://localhost:8080/ws-raw

# Send STOMP CONNECT frame
CONNECT
accept-version:1.0,1.1,1.2

^@

# Subscribe to topic
SUBSCRIBE
id:sub-0
destination:/topic/simulations/9a1d5f7e-3b2c-4d9e-8f1a-6c2b4a7d9e3f

^@

# Receive messages...
```

**Progress Message Format**

Both SSE (`/api/v1/simulations/{id}/stream`) and WebSocket (`/topic/simulations/{id}`) use the same JSON message structure:

```json
{
  "simulationId": "9a1d5f7e-3b2c-4d9e-8f1a-6c2b4a7d9e3f",
  "progress": 45.0,
  "currentTime": 0.009,
  "endTime": 0.02,
  "step": 9000,
  "totalSteps": 20000,
  "status": "RUNNING",
  "timestamp": "2026-02-18T10:30:00Z",
  "errorMessage": null
}
```

**Comparison: SSE vs WebSocket**

| Feature | SSE | WebSocket |
|---------|-----|-----------|
| Endpoint | `/api/v1/simulations/{id}/stream` | `/topic/simulations/{id}` |
| Protocol | HTTP/1.1 (text/event-stream) | TCP (STOMP/1.2) |
| Direction | Server → Client only | Bidirectional (client → server coming in future versions) |
| Browser Support | Native EventSource API | Requires SockJS or ws library |
| Auto-Reconnect | Yes (EventSource) | No (application manages) |
| Latency | ~100ms (HTTP) | ~10-50ms (TCP) |
| Overhead | Higher (HTTP headers) | Lower (raw TCP frames) |
| Best For | Simple dashboards | Interactive monitoring, multi-client scenarios |

Both endpoints receive updates simultaneously from the same `broadcastProgress()` call, ensuring synchronized progress messages.

Live in: **v2.21.0+**


### Signal Analysis Endpoints

Post-processing analysis of simulation or raw signal data.

**Signal Characteristics**

Returns 9 key metrics for any waveform.

```bash
POST /api/v1/analysis/characteristics
Content-Type: application/json
```

Request (with raw data):
```json
{
  "data": [0.0, 0.707, 1.0, 0.707, 0.0, -0.707, -1.0, -0.707],
  "sampleRate": 8000.0
}
```

Or reference simulation results:
```json
{
  "simulationId": "uuid",
  "signalName": "V_out",
  "startTime": 0.01,
  "endTime": 0.02
}
```

Response:
```json
{
  "average": 0.0,
  "rms": 0.707,
  "thd": 2.5,
  "min": -1.0,
  "max": 1.0,
  "peakToPeak": 2.0,
  "ripple": 0.01,
  "klirr": 0.025,
  "shapeFactor": 1.11,
  "sampleCount": 20000
}
```

**Fourier Harmonic Analysis**

Decompose signal into harmonic components with amplitude and phase.

```bash
POST /api/v1/analysis/fourier?harmonics=10
Content-Type: application/json
```

Request (raw data):
```json
{
  "data": [0.0, 0.707, 1.0, 0.707, 0.0, -0.707, -1.0, -0.707],
  "sampleRate": 50000.0,
  "baseFrequency": 50.0
}
```

Response:
```json
{
  "baseFrequency": 50.0,
  "harmonics": 10,
  "dcComponent": 0.01,
  "fundamentalAmplitude": 1.0,
  "fundamentalPhaseDegrees": 0.0,
  "cnAmplitudes": [0.01, 1.0, 0.05, 0.02, 0.015, 0.008, 0.005, 0.003, 0.002, 0.001],
  "jnPhases": [0.0, 0.0, 0.1, -0.05, 0.2, -0.1, 0.15, -0.08, 0.12, -0.06]
}
```

**RMS (Root Mean Square)**

Quick RMS calculation for any signal.

```bash
POST /api/v1/analysis/rms
Content-Type: application/json
```

Request:
```json
{
  "data": [0.0, 0.707, 1.0, 0.707, 0.0, -0.707, -1.0, -0.707]
}
```

Response:
```
0.7071067811865476
```

**Analysis Endpoint Table**

| Endpoint | Method | Purpose | Returns |
|----------|--------|---------|---------|
| `/api/v1/analysis/characteristics` | POST | 9 waveform metrics (RMS, THD, ripple, etc.) | CharacteristicsResult |
| `/api/v1/analysis/fourier` | POST | Harmonic decomposition with amplitude & phase | FourierResult |
| `/api/v1/analysis/rms` | POST | RMS calculation | number |

Live in: **v2.19.0+**


## Usage Examples

### Python Client

```python
import requests
import numpy as np

BASE_URL = "http://localhost:8080/api/v1"

# Submit simulation
response = requests.post(f"{BASE_URL}/simulations", json={
    "circuitFile": "base64_encoded_data",
    "parameters": {"PWM.1.dutyCycle": 0.5},
    "simulationTime": 0.001,
    "timeStep": 5e-8,
    "solverType": "trapezoidal"
})

sim_id = response.json()["simulationId"]
print(f"Simulation {sim_id} submitted")

# Poll for completion
import time
while True:
    status = requests.get(f"{BASE_URL}/simulations/{sim_id}").json()
    if status["status"] in ["completed", "failed"]:
        break
    print(f"Progress: {status['progress']['mainSimProgress']}%")
    time.sleep(1)

# Get results
results = status["results"]["measurements"]
print(f"Vout: {results['SCOPE.1.ch1_avg']:.2f} V")
```

### Bash / curl

```bash
# Loss calculation
curl -X POST http://localhost:8080/api/v1/loss/conduction \
  -H "Content-Type: application/json" \
  -d '{"voltage": 400, "current": 50, "temperature": 125}'

# Upload circuit
curl -F "file=@circuit.ipes" http://localhost:8080/api/v1/circuits/parse

# Submit simulation
curl -X POST http://localhost:8080/api/v1/simulations \
  -H "Content-Type: application/json" \
  -d '{"circuitId": "circuit-uuid-from-parse", "simulationTime": 0.01, "timeStep": 1e-7}'
```

## Request / Response Models

### SimulationRequest

| Field | Type | Description |
|-------|------|-------------|
| `circuitId` | string | ID of a previously uploaded circuit (POST /api/v1/circuits/parse) |
| `base64Circuit` | string | Base64-encoded .ipes file content |
| `circuitFile` | string | Server-local .ipes file path (requires simulationTime/timeStep) |
| `simulationTime` | number | Total simulation time (seconds); defaults to circuit value when using circuitId/base64Circuit |
| `timeStep` | number | Integration time step (seconds); defaults to circuit value when using circuitId/base64Circuit |
| `solverType` | enum | backward-euler, trapezoidal, gear-shichman |
| `parameters` | map | Parameter overrides |

### ProgressDetails

| Field | Type | Description |
|-------|------|-------------|
| `preCalcProgress` | number | Initial condition solver progress (0-100%) |
| `mainSimProgress` | number | Main transient simulation progress (0-100%) |
| `currentStep` | number | Current simulation step number |
| `totalSteps` | number | Total steps for complete simulation |
| `estimatedRemainingMs` | number | Estimated milliseconds until completion |

## Deployment

### Docker (Recommended)

```bash
# Using docker-compose
docker-compose up -d

# Manual Docker run
docker run -p 8080:8080 gecko-rest-api:latest
```

## See Also

- [Remote Interface (RMI)](remote-interface.md) - Java RMI integration
- [GeckoSCRIPT](geckoscript-ref.md) - Built-in scripting
- [Python Integration](../tutorials/scripting/python.md)
- [Docker Setup](../getting-started/installation.md#docker)
