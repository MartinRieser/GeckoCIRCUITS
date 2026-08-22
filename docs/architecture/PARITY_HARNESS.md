# GeckoCIRCUITS Parity Verification Harness (P5)

The parity harness validates that simulation results produced by the new headless simulation engine (`gecko-simulation-core` / `gecko-rest-api`) are numerically identical to the reference output from the legacy Swing engine (`gecko-gui` / `GeckoSim`).

## Architecture & Workflow

```
                        ┌────────────────────────┐
                        │ Curated .ipes Circuit  │
                        └──────────┬─────────────┘
                                   │
                 ┌─────────────────┴─────────────────┐
                 ▼                                   ▼
    ┌─────────────────────────┐         ┌─────────────────────────┐
    │ ReferenceRunner (Legacy)│         │ NewEngineRunner (REST)  │
    │  - Starts headless AWT  │         │  - POSTs .ipes to REST  │
    │  - RMI simulation loop  │         │  - Polls / gets results │
    │  - Dumps scope signals  │         │  - Dumps scope signals  │
    └────────────┬────────────┘         └────────────┬────────────┘
                 │                                   │
                 ▼                                   ▼
          <name>-ref.csv                      <name>-new.csv
                 │                                   │
                 └─────────────────┬─────────────────┘
                                   ▼
                        ┌─────────────────────┐
                        │     CompareCsv      │
                        │ - Time alignment    │
                        │ - Max abs/rel error │
                        │ - Tolerance check   │
                        └──────────┬──────────┘
                                   ▼
                         PASS / FAIL Report
```

## Running the Harness

### Standalone PowerShell Script
Run the automated orchestrator from the repository root:
```powershell
powershell -ExecutionPolicy Bypass -File tools/parity/run-parity.ps1
```

Options:
- `-RelTol <double>`: Maximum relative error tolerance (default: `1e-3` / `0.1%`).
- `-AbsTol <double>`: Maximum absolute error tolerance (default: `5e-3`).
- `-BaseUrl <string>`: REST API URL (default: `http://localhost:8080`).
- `-RmiPort <int>`: Legacy RMI registry port (default: `43099`).

### Via Maven
The harness is configured as an opt-in Maven profile (`-Pparity`):
```bash
mvn -Pparity verify
```

## Tolerances & Float32 Precision

The legacy Swing scope data container stores signal traces in `float32` arrays and applies decimation once buffers fill. The default comparison tolerances (`rel = 1e-3`, `abs = 5e-3`) reflect this legacy scope data path, while the underlying raw solver algorithms agree to ~1e-4 relative error.

## Adding New Test Circuits

1. Place the test circuit file in `tools/parity/circuits/<name>.ipes`.
2. Ensure the `.ipes` file defines `dataContainerSignals[]` for the signals of interest (e.g. `u_out`, `i_in`).
3. Add the circuit definition to the `$circuits` array in `tools/parity/run-parity.ps1`:
   ```powershell
   $circuits = @(
       @{ Name = 'rc-lowpass';   Signals = 'u_out' },
       @{ Name = 'rl-transient'; Signals = 'u_l' },
       @{ Name = 'rlc-series';   Signals = 'u_c,u_l' },
       @{ Name = 'my-new-test';  Signals = 'v_out' }
   )
   ```
4. Run `run-parity.ps1` to execute and verify comparison output.

Reports are saved to `tools/parity/results/YYYYMMDD-HHMMSS.txt`.
