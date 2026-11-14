---
title: MATLAB and Octave Integration
description: Controlling GeckoCIRCUITS from MATLAB/Octave via the REST API
---

# 702 - MATLAB Integration

Integrate GeckoCIRCUITS circuit simulation into MATLAB or GNU Octave workflows using the **REST API** (`http://localhost:8080/gecko/api/v1/...`), or load exported CSV waveforms directly for plotting and control analysis.

!!! warning "Legacy RMI Removed"
    The legacy Java RMI interface (`GeckoRemoteInterface`, `--remote`, `--mmf`, and `GeckoCustomMMF`) belonged to the retired Classic Swing application (archived at git tag `classic-v1.0-final`). Current releases use the HTTP REST API instead, requiring no Java classpath setup in MATLAB.

---

## Setup

### Step 1: Start the GeckoCIRCUITS Engine

Launch the desktop app or run the REST server from a checkout:

```bash
# Windows
run-gecko.bat

# Linux / macOS
./run-gecko.sh

# Or run the built REST API JAR directly
java -jar backend/gecko-rest-api/target/gecko-rest-api.jar
```

Verify the server responds:

```matlab
health = webread('http://localhost:8080/gecko/api/health');
disp(health);
```

---

## Running Simulations from MATLAB

### Submit a Simulation and Retrieve Waveforms

```matlab
baseUrl = 'http://localhost:8080/gecko/api/v1';
opts = weboptions('MediaType', 'application/json', 'Timeout', 30);

% Submit simulation using a circuit file and parameter overrides
req = struct( ...
    'circuitFile', 'resources/projects/buck_sampled_50v_to_10v.ipes', ...
    'simulationTime', 0.005, ...
    'timeStep', 1e-6, ...
    'solverType', 'backward-euler', ...
    'parameters', struct('R_load.resistance', 10.0) ...
);

sim = webwrite([baseUrl '/simulations'], req, opts);

% Poll until completion
while strcmp(sim.status, 'PENDING') || strcmp(sim.status, 'RUNNING')
    pause(0.1);
    sim = webread([baseUrl '/simulations/' sim.simulationId], opts);
end

if ~strcmp(sim.status, 'COMPLETED')
    error('Simulation failed: %s', sim.errorMessage);
end

% Fetch results and compute waveform characteristics
results = webread([baseUrl '/simulations/' sim.simulationId '/results'], opts);
```

### Post-Processing via `/analysis/characteristics`

```matlab
charReq = struct('simulationId', sim.simulationId, 'signalName', 'V_out');
metrics = webwrite([baseUrl '/analysis/characteristics'], charReq, opts);
fprintf('Vout avg = %.3f V, rms = %.3f V, ripple = %.4f\n', ...
    metrics.average, metrics.rms, metrics.ripple);
```

---

## Parameter Sweeps

### Duty / Load Sweep Example

You can submit multiple runs individually or submit a single batch sweep using `POST /gecko/api/v1/simulations/batch`:

```matlab
baseUrl = 'http://localhost:8080/gecko/api/v1';
opts = weboptions('MediaType', 'application/json', 'Timeout', 30);

loads = [2, 5, 10, 20, 50];
vout_avg = zeros(size(loads));

for i = 1:length(loads)
    req = struct( ...
        'circuitFile', 'resources/projects/buck_sampled_50v_to_10v.ipes', ...
        'simulationTime', 0.005, ...
        'timeStep', 1e-6, ...
        'parameters', struct('R_load.resistance', loads(i)) ...
    );
    sim = webwrite([baseUrl '/simulations'], req, opts);
    while strcmp(sim.status, 'PENDING') || strcmp(sim.status, 'RUNNING')
        pause(0.1);
        sim = webread([baseUrl '/simulations/' sim.simulationId], opts);
    end
    m = webwrite([baseUrl '/analysis/characteristics'], ...
        struct('simulationId', sim.simulationId, 'signalName', 'V_out'), opts);
    vout_avg(i) = m.average;
end

figure;
plot(loads, vout_avg, 'b-o', 'LineWidth', 1.5);
xlabel('Load Resistance (Ohm)');
ylabel('Average Output Voltage (V)');
grid on;
```

---

## Simulink Co-Simulation Notes (Historical)

Legacy S-Function sources and R2010a Simulink examples from ETH Zurich remain archived in:

- `resources/tutorials/7xx_scripting_automation/703_simulink_cosimulation/`
- `resources/tutorials/7xx_scripting_automation/702_matlab_integration/`

They target the retired `classic-v1.0-final` Swing/RMI architecture and are kept for historical reference.

## Related Guides

- [REST API Reference](../../api/rest-api.md) — Full endpoint, batch, and analysis schemas
- [Remote Interface (Legacy RMI Notice)](../../api/remote-interface.md)
- [Python Integration](python.md) — `geckopy` SDK & SciPy optimization
- [NativeC C/C++ Blocks](../../native-c-blocks.md) — Firmware-in-the-loop C/C++ shared libraries
