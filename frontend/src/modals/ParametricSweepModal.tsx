/**
 * Parametric Sweep Modal: lets users pick any numeric component parameter,
 * define Linear or Logarithmic bounds + step count, preview the generated grid,
 * and submit an asynchronous batch sweep via /api/v1/simulations/batch.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import type { EditorComponent, SimRunSettings, SimulationDefaults } from '../model/types';
import { formatEngineeringValue, parseEngineeringValue } from '../model/componentSchema';
import { resolveActiveSimSettings } from '../simulation/simSettings';
import {
  generateSweepValues,
  listSweepableParameters,
  type SweepScaleMode,
  type SweepSession,
  type SweepableParameter,
} from '../simulation/parametricSweep';

export interface ParametricSweepModalProps {
  isOpen: boolean;
  onClose: () => void;
  circuitId: string | null;
  components: EditorComponent[];
  defaults?: SimulationDefaults | null;
  settings?: SimRunSettings | null;
  sweepSession?: SweepSession | null;
  /** Optional pre-selected component name when opened from component Properties. */
  initialComponentName?: string;
  /** Optional pre-selected parameter key when opened from component Properties. */
  initialParamKey?: string;
  onStartSweep: (config: {
    componentName: string;
    paramKey: string;
    paramLabel: string;
    unit: string;
    mode: SweepScaleMode;
    startValue: number;
    endValue: number;
    points: number;
    simulationTime: number;
    timeStep: number;
    solverType: string;
  }) => void | Promise<void>;
  onCancelSweep?: () => void;
}

const MIN_SWEEP_STEPS = 2;
const MAX_SWEEP_STEPS = 25;

function seedBounds(currentValue: number, mode: SweepScaleMode): { start: string; end: string } {
  if (!Number.isFinite(currentValue) || currentValue === 0) {
    return mode === 'log' ? { start: '1m', end: '1' } : { start: '0', end: '10' };
  }
  if (currentValue < 0) {
    return mode === 'log'
      ? { start: '1', end: '10' }
      : { start: formatEngineeringValue(currentValue * 1.5), end: formatEngineeringValue(currentValue * 0.5) };
  }
  const s = currentValue * 0.5;
  const e = currentValue * 1.5;
  return {
    start: formatEngineeringValue(s),
    end: formatEngineeringValue(e),
  };
}

export function ParametricSweepModal({
  isOpen,
  onClose,
  circuitId,
  components,
  defaults,
  settings,
  sweepSession,
  initialComponentName,
  initialParamKey,
  onStartSweep,
  onCancelSweep,
}: ParametricSweepModalProps) {
  const sweepableParams = useMemo(() => listSweepableParameters(components), [components]);

  const componentOptions = useMemo(() => {
    const seen = new Map<string, string>();
    for (const p of sweepableParams) {
      if (!seen.has(p.componentName)) {
        seen.set(p.componentName, `${p.componentName} (${p.componentDisplayName})`);
      }
    }
    return Array.from(seen.entries()).map(([name, label]) => ({ name, label }));
  }, [sweepableParams]);

  const [selectedComponent, setSelectedComponent] = useState<string>('');
  const [selectedParamKey, setSelectedParamKey] = useState<string>('');
  const [mode, setMode] = useState<SweepScaleMode>('linear');
  const [startInput, setStartInput] = useState<string>('');
  const [endInput, setEndInput] = useState<string>('');
  const [pointsInput, setPointsInput] = useState<number>(5);

  const prevOpenRef = useRef(false);
  useEffect(() => {
    if (isOpen && !prevOpenRef.current) {
      if (sweepableParams.length > 0) {
        const preferred =
          (initialComponentName &&
            initialParamKey &&
            sweepableParams.find(
              (p) => p.componentName === initialComponentName && p.paramKey === initialParamKey,
            )) ||
          (initialComponentName &&
            sweepableParams.find((p) => p.componentName === initialComponentName)) ||
          sweepableParams[0];
        setSelectedComponent(preferred.componentName);
        setSelectedParamKey(preferred.paramKey);
        const seeded = seedBounds(preferred.currentValue, 'linear');
        setMode('linear');
        setStartInput(seeded.start);
        setEndInput(seeded.end);
        setPointsInput(5);
      } else {
        setSelectedComponent('');
        setSelectedParamKey('');
      }
    }
    prevOpenRef.current = isOpen;
  }, [isOpen, sweepableParams, initialComponentName, initialParamKey]);

  const paramsForComponent = useMemo(
    () => sweepableParams.filter((p) => p.componentName === selectedComponent),
    [sweepableParams, selectedComponent],
  );

  const activeParam: SweepableParameter | null = useMemo(
    () => paramsForComponent.find((p) => p.paramKey === selectedParamKey) ?? paramsForComponent[0] ?? null,
    [paramsForComponent, selectedParamKey],
  );

  const handleComponentChange = (nextComp: string) => {
    setSelectedComponent(nextComp);
    const firstParam = sweepableParams.find((p) => p.componentName === nextComp);
    if (firstParam) {
      setSelectedParamKey(firstParam.paramKey);
      const seeded = seedBounds(firstParam.currentValue, mode);
      setStartInput(seeded.start);
      setEndInput(seeded.end);
    }
  };

  const handleParamChange = (nextKey: string) => {
    setSelectedParamKey(nextKey);
    const found = paramsForComponent.find((p) => p.paramKey === nextKey);
    if (found) {
      const seeded = seedBounds(found.currentValue, mode);
      setStartInput(seeded.start);
      setEndInput(seeded.end);
    }
  };

  const handleModeChange = (nextMode: SweepScaleMode) => {
    setMode(nextMode);
    if (nextMode === 'log') {
      const s = parseEngineeringValue(startInput);
      const e = parseEngineeringValue(endInput);
      if (s === null || s <= 0 || e === null || e <= 0) {
        const seeded = seedBounds(activeParam?.currentValue ?? 1, 'log');
        setStartInput(seeded.start);
        setEndInput(seeded.end);
      }
    }
  };

  const startVal = parseEngineeringValue(startInput);
  const endVal = parseEngineeringValue(endInput);
  const clampedPoints = Math.max(
    MIN_SWEEP_STEPS,
    Math.min(MAX_SWEEP_STEPS, Math.round(Number(pointsInput) || MIN_SWEEP_STEPS)),
  );

  const validationError = useMemo(() => {
    if (!activeParam) return 'No numeric component parameters available in this circuit.';
    if (startVal === null || !Number.isFinite(startVal)) return 'Enter a valid start value (e.g. 100u, 10).';
    if (endVal === null || !Number.isFinite(endVal)) return 'Enter a valid end value (e.g. 500u, 100).';
    if (mode === 'log' && (startVal <= 0 || endVal <= 0)) {
      return 'Logarithmic sweep requires strictly positive (> 0) start and end values.';
    }
    if (pointsInput < MIN_SWEEP_STEPS || pointsInput > MAX_SWEEP_STEPS) {
      return `Steps must be between ${MIN_SWEEP_STEPS} and ${MAX_SWEEP_STEPS}.`;
    }
    return null;
  }, [activeParam, startVal, endVal, mode, pointsInput]);

  const previewValues = useMemo(() => {
    if (validationError || startVal === null || endVal === null) return [];
    return generateSweepValues(mode, startVal, endVal, clampedPoints);
  }, [validationError, mode, startVal, endVal, clampedPoints]);

  useEffect(() => {
    if (!isOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const isRunning = sweepSession?.status === 'RUNNING';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (validationError || !activeParam || startVal === null || endVal === null || !circuitId) return;
    const simCfg = resolveActiveSimSettings(defaults, settings);
    await onStartSweep({
      componentName: activeParam.componentName,
      paramKey: activeParam.paramKey,
      paramLabel: `${activeParam.componentName} · ${activeParam.label}`,
      unit: activeParam.unit,
      mode,
      startValue: startVal,
      endValue: endVal,
      points: clampedPoints,
      simulationTime: simCfg.tEnd,
      timeStep: simCfg.dt,
      solverType: simCfg.solver,
    });
  };

  return (
    <div
      className="modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-label="Parametric Sweep Setup"
      data-testid="parametric-sweep-modal"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="sweep-modal" onClick={(e) => e.stopPropagation()}>
        <div className="sweep-modal-header">
          <div>
            <h3>📈 Parametric Sweep</h3>
            <p className="sweep-modal-subtitle">
              Vary a component parameter across multiple steps and compare waveforms & summary curves.
            </p>
          </div>
          <button type="button" className="shortcuts-modal-close" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="sweep-modal-body">
          <div className="sweep-form-grid">
            <label className="sweep-field">
              <span>Component</span>
              <select
                data-testid="sweep-component-select"
                value={activeParam?.componentName ?? ''}
                onChange={(e) => handleComponentChange(e.target.value)}
                disabled={isRunning || componentOptions.length === 0}
              >
                {componentOptions.map((opt) => (
                  <option key={opt.name} value={opt.name}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="sweep-field">
              <span>Parameter</span>
              <select
                data-testid="sweep-param-select"
                value={activeParam?.paramKey ?? ''}
                onChange={(e) => handleParamChange(e.target.value)}
                disabled={isRunning || paramsForComponent.length === 0}
              >
                {paramsForComponent.map((p) => (
                  <option key={p.paramKey} value={p.paramKey}>
                    {p.label} ({formatEngineeringValue(p.currentValue, p.unit)})
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="sweep-form-grid sweep-three-col">
            <label className="sweep-field">
              <span>Start{activeParam?.unit ? ` (${activeParam.unit})` : ''}</span>
              <input
                type="text"
                data-testid="sweep-start-input"
                value={startInput}
                onChange={(e) => setStartInput(e.target.value)}
                disabled={isRunning}
                placeholder="e.g. 100u"
              />
            </label>

            <label className="sweep-field">
              <span>End{activeParam?.unit ? ` (${activeParam.unit})` : ''}</span>
              <input
                type="text"
                data-testid="sweep-end-input"
                value={endInput}
                onChange={(e) => setEndInput(e.target.value)}
                disabled={isRunning}
                placeholder="e.g. 500u"
              />
            </label>

            <label className="sweep-field">
              <span>Steps (2–25)</span>
              <input
                type="number"
                data-testid="sweep-steps-input"
                min={MIN_SWEEP_STEPS}
                max={MAX_SWEEP_STEPS}
                value={pointsInput}
                onChange={(e) => setPointsInput(Number(e.target.value))}
                disabled={isRunning}
              />
            </label>
          </div>

          <div className="sweep-scale-row">
            <span>Scale:</span>
            <div className="segmented-control">
              <button
                type="button"
                data-testid="sweep-scale-linear"
                className={`segmented-btn ${mode === 'linear' ? 'active' : ''}`}
                onClick={() => handleModeChange('linear')}
                disabled={isRunning}
              >
                Linear
              </button>
              <button
                type="button"
                data-testid="sweep-scale-log"
                className={`segmented-btn ${mode === 'log' ? 'active' : ''}`}
                onClick={() => handleModeChange('log')}
                disabled={isRunning}
              >
                Logarithmic
              </button>
            </div>
          </div>

          {validationError && (
            <div className="sweep-error-msg" role="alert" data-testid="sweep-validation-error">
              ⚠️ {validationError}
            </div>
          )}

          {previewValues.length > 0 && (
            <div className="sweep-preview-box" data-testid="sweep-preview-chips">
              <span className="sweep-preview-label">Preview ({previewValues.length} runs):</span>
              <div className="sweep-chips-wrap">
                {previewValues.map((v, idx) => (
                  <span key={idx} className="sweep-preview-chip">
                    {formatEngineeringValue(v, activeParam?.unit ?? '')}
                  </span>
                ))}
              </div>
            </div>
          )}

          {sweepSession && (
            <div className="sweep-progress-box" data-testid="sweep-progress-box">
              <div className="sweep-progress-header">
                <span>
                  {sweepSession.status === 'RUNNING'
                    ? `Running ${sweepSession.completedRuns} / ${sweepSession.totalRuns}...`
                    : sweepSession.status === 'COMPLETED'
                      ? `Completed ${sweepSession.completedRuns} / ${sweepSession.totalRuns} runs`
                      : sweepSession.errorMessage || `Sweep ${sweepSession.status}`}
                </span>
                <span>{Math.round(sweepSession.progress * 100)}%</span>
              </div>
              <div className="sweep-progress-track">
                <div
                  className="sweep-progress-fill"
                  style={{ width: `${Math.min(100, Math.round(sweepSession.progress * 100))}%` }}
                />
              </div>
            </div>
          )}

          <div className="sweep-modal-footer">
            {isRunning && onCancelSweep ? (
              <button
                type="button"
                className="nav-btn danger"
                data-testid="sweep-cancel-btn"
                onClick={onCancelSweep}
              >
                ⏹ Cancel Sweep
              </button>
            ) : (
              <button type="button" className="nav-btn" onClick={onClose}>
                Close
              </button>
            )}
            <button
              type="submit"
              data-testid="sweep-run-btn"
              className="nav-btn primary-run-btn"
              disabled={!circuitId || Boolean(validationError) || isRunning}
            >
              ▶ Run Sweep
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
