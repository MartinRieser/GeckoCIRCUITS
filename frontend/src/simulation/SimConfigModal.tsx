/**
 * Simulation Configuration & Parameters Modal (SimConfigModal).
 * Prominent dialog for configuring simulation parameters (tEnd, dt, solver),
 * inspecting workload estimates, reviewing connected scopes & signals,
 * and performing circuit pre-run validation checks before starting.
 */
import { useState, useEffect, useMemo, useRef } from 'react';
import type {
  EditorComponent,
  EditorWire,
  SimulationDefaults,
  SimRunSettings,
} from '../model/types';
import { validateCircuitForSimulation } from '../model/validation';
import { parseEngineeringValue } from '../model/componentSchema';
import { findScopeBlocks } from './scopes';
import { estimateStepCount, STEP_WARNING_THRESHOLD } from './simSteps';
import { resolveActiveSimSettings } from './simSettings';

export interface SimConfigModalProps {
  /** Whether the modal is currently visible. */
  isOpen: boolean;
  /** Close callback. */
  onClose: () => void;
  /** Current circuit ID. */
  circuitId: string | null;
  /** Engine simulation defaults (duration, timeStep, solverType). */
  defaults?: SimulationDefaults | null;
  /** Active user-configured settings. */
  settings?: SimRunSettings | null;
  /** Callback when user changes simulation parameters. */
  onSettingsChange?: (settings: SimRunSettings) => void;
  /** Callback to trigger simulation execution with parameters. */
  onRunSimulation: (config: {
    simulationTime: number;
    timeStep: number;
    solverType: string;
    backend?: string;
  }) => void;
  /** Schematic components. */
  components: EditorComponent[];
  /** Schematic wires. */
  wires?: EditorWire[];
  /** Pre-existing engine warnings. */
  engineWarnings?: string[];
}

export function SimConfigModal({
  isOpen,
  onClose,
  circuitId,
  defaults,
  settings: liftedSettings,
  onSettingsChange,
  onRunSimulation,
  components,
  wires = [],
  engineWarnings = [],
}: SimConfigModalProps) {
  // Effective defaults resolved through the shared resolver so the modal and
  // the top-bar quick run always agree on the current parameters
  const { tEndStr: defaultDurationStr, dtStr: defaultDtStr, solver: defaultSolver } = useMemo(
    () => resolveActiveSimSettings(defaults, null),
    [defaults],
  );

  const [tEndInput, setTEndInput] = useState<string>(
    liftedSettings?.tEnd ?? defaultDurationStr,
  );
  const [dtInput, setDtInput] = useState<string>(
    liftedSettings?.dt ?? defaultDtStr,
  );
  const [solverInput, setSolverInput] = useState<string>(
    liftedSettings?.solver ?? defaultSolver,
  );
  const [pendingWarnings, setPendingWarnings] = useState<string[] | null>(null);

  // Sync inputs only when modal transitions from closed to open,
  // preventing user keystrokes from being overwritten during active editing.
  const prevIsOpenRef = useRef(false);
  useEffect(() => {
    if (isOpen && !prevIsOpenRef.current) {
      setTEndInput(liftedSettings?.tEnd ?? defaultDurationStr);
      setDtInput(liftedSettings?.dt ?? defaultDtStr);
      setSolverInput(liftedSettings?.solver ?? defaultSolver);
      setPendingWarnings(null);
    }
    prevIsOpenRef.current = isOpen;
  }, [isOpen, liftedSettings, defaultDurationStr, defaultDtStr, defaultSolver]);

  // Track backdrop mouse-down to distinguish genuine outside clicks from
  // text-selection drag releases starting inside input fields.
  const isBackdropMouseDownRef = useRef(false);

  const handleBackdropMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
    isBackdropMouseDownRef.current = e.target === e.currentTarget;
  };

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (isBackdropMouseDownRef.current && e.target === e.currentTarget) {
      onClose();
    }
    isBackdropMouseDownRef.current = false;
  };

  // Any press inside the dialog clears the backdrop flag, so a drag starting
  // in an input can never be mistaken for a deliberate backdrop click — even
  // after an earlier backdrop press was abandoned without a click (e.g. the
  // mouse was released outside the window).
  const handleDialogMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
    isBackdropMouseDownRef.current = false;
    e.stopPropagation();
  };

  // Numerical parsing: valid means parseable, finite, and strictly positive.
  // Invalid input collapses to null so downstream code never re-checks.
  const parsedTEnd = parseEngineeringValue(tEndInput);
  const parsedDt = parseEngineeringValue(dtInput);
  const tEndNum =
    parsedTEnd !== null && Number.isFinite(parsedTEnd) && parsedTEnd > 0 ? parsedTEnd : null;
  const dtNum =
    parsedDt !== null && Number.isFinite(parsedDt) && parsedDt > 0 ? parsedDt : null;

  const isValidTEnd = tEndNum !== null;
  const isValidDt = dtNum !== null;
  const isValidTimeRatio = tEndNum !== null && dtNum !== null && dtNum <= tEndNum;

  const estSteps = useMemo(
    () => (tEndNum !== null && dtNum !== null ? estimateStepCount(tEndNum, dtNum) : 0),
    [tEndNum, dtNum],
  );

  const isHeavyRun = estSteps > STEP_WARNING_THRESHOLD;

  // Active scope blocks in schematic
  const scopeBlocks = useMemo(() => findScopeBlocks(components), [components]);

  // Circuit validation warnings combined with pre-existing engine warnings
  const allWarnings = useMemo(() => {
    const direct = validateCircuitForSimulation(components, wires);
    const combined = [...direct, ...(engineWarnings || [])];
    return Array.from(new Set(combined));
  }, [components, wires, engineWarnings]);

  // Close on Escape key
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleStartRun = () => {
    if (tEndNum === null || dtNum === null || dtNum > tEndNum) return;

    // Check circuit validation
    if (allWarnings.length > 0 && pendingWarnings === null) {
      setPendingWarnings(allWarnings);
      return;
    }

    const nextSettings: SimRunSettings = {
      tEnd: tEndInput.trim(),
      dt: dtInput.trim(),
      solver: solverInput,
    };
    onSettingsChange?.(nextSettings);

    onRunSimulation({
      simulationTime: tEndNum,
      timeStep: dtNum,
      solverType: solverInput,
      backend: 'headless',
    });

    onClose();
  };

  const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleStartRun();
    }
  };

  return (
    <div
      className="modal-backdrop sim-config-modal-backdrop"
      onMouseDown={handleBackdropMouseDown}
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="sim-config-modal-title"
    >
      <div
        className="shortcuts-modal sim-config-dialog"
        onMouseDown={handleDialogMouseDown}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="shortcuts-modal-header">
          <div className="shortcuts-modal-title" id="sim-config-modal-title">
            <span>⚙ Simulation Configuration</span>
            {circuitId && (
              <span className="workspace-tab-badge" style={{ fontSize: 11 }}>
                {components.length} components
              </span>
            )}
          </div>
          <button
            type="button"
            className="shortcuts-modal-close"
            onClick={onClose}
            aria-label="Close configuration modal"
          >
            ✕
          </button>
        </div>

        {/* Modal Body */}
        <div className="shortcuts-modal-body sim-config-body">
          {/* Section 1: Timing Parameters */}
          <div className="sim-config-section">
            <div className="sim-config-section-title">⏱ Simulation Timing</div>
            <div className="sim-config-timing-grid">
              <div className="prop-field">
                <label className="prop-label" htmlFor="sim-cfg-tend">
                  Total Duration (t_end)
                </label>
                <input
                  id="sim-cfg-tend"
                  type="text"
                  className={`prop-input ${!isValidTEnd ? 'input-invalid' : ''}`}
                  value={tEndInput}
                  onChange={(e) => setTEndInput(e.target.value)}
                  onKeyDown={handleInputKeyDown}
                  placeholder="e.g. 20m, 100m, 1.5s"
                />
                <span className="prop-unit-hint">
                  {isValidTEnd ? `${(tEndNum * 1000).toLocaleString()} ms` : 'Enter valid time (e.g. 20m)'}
                </span>
              </div>

              <div className="prop-field">
                <label className="prop-label" htmlFor="sim-cfg-dt">
                  Time Step (dt)
                </label>
                <input
                  id="sim-cfg-dt"
                  type="text"
                  className={`prop-input ${!isValidDt ? 'input-invalid' : ''}`}
                  value={dtInput}
                  onChange={(e) => setDtInput(e.target.value)}
                  onKeyDown={handleInputKeyDown}
                  placeholder="e.g. 1u, 100n, 10u"
                />
                <span className="prop-unit-hint">
                  {isValidDt ? `${(dtNum * 1e6).toLocaleString()} µs` : 'Enter valid step (e.g. 1u)'}
                </span>
              </div>
            </div>

            {!isValidTimeRatio && isValidTEnd && isValidDt && (
              <div className="warning-banner sim-config-warning">
                ⚠️ Time step dt must be smaller than or equal to duration t_end
              </div>
            )}
          </div>

          {/* Section 2: Solver Algorithm Selection */}
          <div className="sim-config-section">
            <div className="sim-config-section-title">📐 Solver & Numerical Method</div>
            <div className="prop-field">
              <label className="prop-label" htmlFor="sim-cfg-solver">
                Differential Equation Solver
              </label>
              <select
                id="sim-cfg-solver"
                className="prop-input"
                value={solverInput}
                onChange={(e) => setSolverInput(e.target.value)}
              >
                <option value="backward-euler">Backward Euler (Stiff, Stable, Default)</option>
                <option value="trapezoidal">Trapezoidal Rule (High Accuracy, Low Damping)</option>
                <option value="gear-shichman">Gear / Shichman (Variable Order)</option>
              </select>
            </div>
          </div>

          {/* Section 3: Workload Estimation */}
          <div className="sim-config-section workload-box">
            <div className="workload-box-header">
              <span className="workload-box-title">
                Workload Estimation
              </span>
              <span className={`sim-step-pill ${isHeavyRun ? 'warning' : 'ok'}`}>
                {estSteps.toLocaleString()} steps
              </span>
            </div>
            <div className="workload-box-note">
              {isHeavyRun
                ? `⚠️ Step count is very large (> ${STEP_WARNING_THRESHOLD.toLocaleString()}). Execution may take longer or consume significant memory.`
                : '✓ Optimal step count for interactive real-time simulation.'}
            </div>
          </div>

          {/* Section 4: Connected Scopes & Instruments */}
          <div className="sim-config-section">
            <div className="sim-config-section-title">
              📺 Active Instruments ({scopeBlocks.length} Scope{scopeBlocks.length === 1 ? '' : 's'})
            </div>
            {scopeBlocks.length > 0 ? (
              <div className="sim-config-scope-chips">
                {scopeBlocks.map((sb) => {
                  const activeLabels = (sb.inputLabels || []).filter(
                    (l) => l && l.trim() && l !== 'NIX_NIX_NIX',
                  );
                  return (
                    <div key={sb.name} className="sim-config-scope-chip">
                      <span className="sim-config-scope-chip-name">{sb.name}</span>
                      <span className="sim-config-scope-chip-traces">
                        ({activeLabels.length} trace{activeLabels.length === 1 ? '' : 's'})
                      </span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="sim-config-hint">
                ℹ No oscilloscope blocks placed. Probe voltages and branch currents can still be measured.
              </div>
            )}
          </div>

          {/* Section 5: Pre-run Validation Warnings */}
          {(allWarnings.length > 0 || pendingWarnings !== null) && (
            <div className="warning-banner sim-config-warning">
              <div className="sim-config-warning-title">
                ⚠️ Circuit Validation Notice ({allWarnings.length} item{allWarnings.length === 1 ? '' : 's'}):
              </div>
              <ul className="sim-config-warning-list">
                {allWarnings.map((warn, i) => (
                  <li key={i}>{warn}</li>
                ))}
              </ul>
              {pendingWarnings !== null && (
                <div className="sim-config-warning-note">
                  Click &quot;Run Anyway&quot; to execute simulation despite notices.
                </div>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer Actions */}
        <div className="sim-config-footer">
          <button type="button" className="sim-config-btn cancel" onClick={onClose}>
            Cancel
          </button>

          <button
            type="button"
            className={`sim-config-btn primary ${pendingWarnings !== null ? 'warn' : ''}`}
            onClick={handleStartRun}
            disabled={!isValidTEnd || !isValidDt || !isValidTimeRatio}
          >
            {pendingWarnings !== null ? '▶ Run Anyway' : '▶ Start Simulation'}
          </button>
        </div>
      </div>
    </div>
  );
}
