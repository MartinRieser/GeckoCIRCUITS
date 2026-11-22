/**
 * Always-visible inline simulation control strip for the top application toolbar.
 * Exposes time step (dt), duration (t_end), solver method, Auto-dt calculation,
 * and live step-count / aliasing diagnostics without opening a modal.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import type {
  EditorComponent,
  SimRunSettings,
  SimulationDefaults,
  SimulationStatus,
} from '../model/types';
import { parseEngineeringValue } from '../model/componentSchema';
import {
  DT_PRESETS,
  T_END_PRESETS,
  detectMaxCircuitFrequency,
  resolveActiveSimSettings,
  suggestAutoTimeStep,
} from './simSettings';
import { STEP_WARNING_THRESHOLD, estimateStepCount, isHighStepCount } from './simSteps';

/** Props for the always-visible top-bar inline simulation control strip. */
export interface SimToolbarStripProps {
  /** Active circuit identifier; disables controls when null. */
  circuitId: string | null;
  /** True while the simulation job is running or pending. */
  isRunning: boolean;
  /** Current engine job status (e.g. RUNNING, PAUSED, COMPLETED). */
  status: SimulationStatus | null;
  /** Normalized progress in `[0, 1]`. */
  progress: number;
  /** Backend/circuit default simulation parameters loaded from the file. */
  defaults?: SimulationDefaults | null;
  /** Explicit user overrides edited during this session. */
  settings?: SimRunSettings | null;
  /** Placed schematic components inspected for Auto-dt and aliasing checks. */
  components: EditorComponent[];
  /** Invoked whenever valid inline simulation parameters change. */
  onSettingsChange: (settings: SimRunSettings) => void;
  /** Starts or triggers a simulation run with the active settings. */
  onQuickRun: () => void;
  /** Pauses an active simulation run. */
  onPause: () => void;
  /** Resumes a paused simulation run. */
  onResume: () => void;
  /** Cancels the currently active simulation run. */
  onCancel: () => void;
  /** Opens the full Simulation Configuration modal. */
  onOpenConfigModal: () => void;
  /** Optional status bar notification callback (e.g. after Auto-dt calculation). */
  onStatusMessage?: (msg: string) => void;
}

/** Formats an estimated solver step count compactly (e.g. `20k steps`, `1.5M steps`). */
function formatCompactSteps(steps: number): string {
  if (!Number.isFinite(steps) || steps <= 0) return '0 steps';
  if (steps >= 1_000_000) {
    const val = (steps / 1_000_000).toFixed(steps >= 10_000_000 ? 0 : 1).replace(/\.0$/, '');
    return `${val}M steps`;
  }
  if (steps >= 1_000) {
    const val = (steps / 1_000).toFixed(steps >= 100_000 ? 0 : 1).replace(/\.0$/, '');
    return `${val}k steps`;
  }
  return `${steps} steps`;
}

/** Parses an engineering-notation time string, returning a positive number in seconds or `null`. */
function parsePositiveTime(raw: string): number | null {
  const parsed = parseEngineeringValue(raw);
  return parsed !== null && Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

/**
 * Always-visible inline simulation control strip displaying Run/Pause/Cancel,
 * editable time step (`dt`) with preset + Auto-dt dropdown, duration (`t_end`),
 * solver selector, and live step-count / aliasing status pill.
 */
export function SimToolbarStrip({
  circuitId,
  isRunning,
  status,
  progress,
  defaults,
  settings,
  components,
  onSettingsChange,
  onQuickRun,
  onPause,
  onResume,
  onCancel,
  onOpenConfigModal,
  onStatusMessage,
}: SimToolbarStripProps) {
  const resolved = useMemo(
    () => resolveActiveSimSettings(defaults, settings),
    [defaults, settings],
  );

  const [dtDraft, setDtDraft] = useState(resolved.dtStr);
  const [tEndDraft, setTEndDraft] = useState(resolved.tEndStr);
  const [timingOpen, setTimingOpen] = useState(false);
  const timingWrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setDtDraft(resolved.dtStr);
  }, [resolved.dtStr]);

  useEffect(() => {
    setTEndDraft(resolved.tEndStr);
  }, [resolved.tEndStr]);

  useEffect(() => {
    if (!timingOpen) return;
    const onMouseDown = (e: MouseEvent) => {
      if (timingWrapRef.current && !timingWrapRef.current.contains(e.target as Node)) {
        setTimingOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setTimingOpen(false);
      }
    };
    window.addEventListener('mousedown', onMouseDown);
    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('mousedown', onMouseDown);
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [timingOpen]);

  const parsedDt = parsePositiveTime(dtDraft);
  const parsedTEnd = parsePositiveTime(tEndDraft);
  const isDtValid = parsedDt !== null;
  const isTEndValid = parsedTEnd !== null;
  const isRatioValid = parsedDt !== null && parsedTEnd !== null && parsedDt <= parsedTEnd;

  const effDt = parsedDt ?? resolved.dt;
  const effTEnd = parsedTEnd ?? resolved.tEnd;

  const estSteps = useMemo(
    () => (isDtValid && isTEndValid && isRatioValid ? estimateStepCount(effTEnd, effDt) : 0),
    [isDtValid, isTEndValid, isRatioValid, effTEnd, effDt],
  );

  const { maxFreq, sourceName } = useMemo(
    () => detectMaxCircuitFrequency(components),
    [components],
  );

  const isAliasingRisk = maxFreq > 0 && effDt > 1 / (20 * maxFreq);
  const isHeavyRun = isHighStepCount(estSteps);

  const stepHealth: { tone: 'ok' | 'warn' | 'error'; label: string; title: string } = useMemo(() => {
    if (!isDtValid || !isTEndValid) {
      return {
        tone: 'error',
        label: 'Invalid',
        title: 'Enter positive engineering values for dt and t_end (e.g. 1u, 20m)',
      };
    }
    if (!isRatioValid) {
      return {
        tone: 'error',
        label: 'dt > t_end',
        title: 'Time step dt must be smaller than or equal to simulation duration t_end',
      };
    }
    if (isAliasingRisk) {
      return {
        tone: 'warn',
        label: `${formatCompactSteps(estSteps)} ⚠`,
        title: `Time step may be too coarse for ${sourceName || 'circuit'} (${Math.round(maxFreq)} Hz). Click Auto or Setup to refine dt.`,
      };
    }
    if (isHeavyRun) {
      return {
        tone: 'warn',
        label: `${formatCompactSteps(estSteps)} ⚠`,
        title: `Very large simulation (${estSteps.toLocaleString()} steps >= ${STEP_WARNING_THRESHOLD.toLocaleString()}). May take noticeable runtime; click to review timing setup.`,
      };
    }
    return {
      tone: 'ok',
      label: `${formatCompactSteps(estSteps)} ✓`,
      title: `${estSteps.toLocaleString()} solver steps (click for full Simulation Setup)`,
    };
  }, [isDtValid, isTEndValid, isRatioValid, isAliasingRisk, isHeavyRun, estSteps, maxFreq, sourceName]);

  const commitSettings = (patch: Partial<SimRunSettings>) => {
    onSettingsChange({
      tEnd: resolved.tEndStr,
      dt: resolved.dtStr,
      solver: resolved.solver,
      ...patch,
    });
  };

  const handleDtChange = (val: string) => {
    setDtDraft(val);
    if (parsePositiveTime(val) !== null) {
      commitSettings({ dt: val.trim() });
    }
  };

  const handleDtBlur = () => {
    if (parsePositiveTime(dtDraft) === null) {
      setDtDraft(resolved.dtStr);
    }
  };

  const handleTEndChange = (val: string) => {
    setTEndDraft(val);
    if (parsePositiveTime(val) !== null) {
      commitSettings({ tEnd: val.trim() });
    }
  };

  const handleTEndBlur = () => {
    if (parsePositiveTime(tEndDraft) === null) {
      setTEndDraft(resolved.tEndStr);
    }
  };

  const handleAutoDt = () => {
    const suggestion = suggestAutoTimeStep(components, effTEnd);
    setDtDraft(suggestion.dtStr);
    commitSettings({ dt: suggestion.dtStr });
    onStatusMessage?.(`Auto-dt set to ${suggestion.dtStr} (${suggestion.reason})`);
  };

  const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (isDtValid && isTEndValid && isRatioValid && circuitId) {
        onQuickRun();
      }
    }
  };

  const solverShort =
    resolved.solver === 'trapezoidal'
      ? 'Trap'
      : resolved.solver === 'gear-shichman'
        ? 'Gear'
        : 'BE';

  const showActiveControls = isRunning || status === 'PAUSED';

  return (
    <div className="nav-sim-group" data-testid="sim-toolbar-strip">
      {showActiveControls ? (
        <>
          <div className="nav-sim-progress" title="Simulation progress">
            <span className="nav-sim-spinner" />
            <span>{Math.round(progress * 100)}%</span>
          </div>
          <button
            type="button"
            className="nav-btn"
            onClick={status === 'PAUSED' ? onResume : onPause}
            title={status === 'PAUSED' ? 'Resume Simulation' : 'Pause Simulation'}
          >
            {status === 'PAUSED' ? '▶' : '⏸'}
          </button>
          <button
            type="button"
            className="nav-btn danger"
            onClick={onCancel}
            title="Cancel Simulation"
          >
            ⏹
          </button>
        </>
      ) : (
        <>
          <button
            type="button"
            className="nav-btn primary-run-btn"
            onClick={onQuickRun}
            disabled={!circuitId}
            title="Run Simulation with current parameters (F5)"
          >
            ▶ Run <span className="kbd-shortcut run-kbd">F5</span>
          </button>

          {/* Compact Timing Chip + Popover */}
          <div className="sim-timing-wrap" ref={timingWrapRef}>
            <button
              type="button"
              data-testid="toolbar-timing-chip"
              className={`sim-timing-chip ${stepHealth.tone}${timingOpen ? ' open' : ''}`}
              onClick={() => setTimingOpen((prev) => !prev)}
              disabled={!circuitId}
              aria-expanded={timingOpen}
              aria-haspopup="dialog"
              title={`Simulation timing: Duration ${resolved.tEndStr}s, Step ${resolved.dtStr}s, Solver ${solverShort} (${stepHealth.label}). Click to adjust.`}
            >
              <span className={`sim-timing-dot ${stepHealth.tone}`} aria-hidden="true" />
              <span>
                {resolved.tEndStr} · {resolved.dtStr} · {solverShort}
              </span>
              {stepHealth.tone !== 'ok' && <span className="sim-timing-warn">⚠</span>}
              <span className="sim-timing-caret">▾</span>
            </button>

            {timingOpen && (
              <div
                className="sim-timing-popover"
                role="dialog"
                aria-label="Simulation Timing"
                data-testid="toolbar-timing-popover"
              >
                <div className="sim-timing-row">
                  <span className="sim-inline-label">Duration (t_end)</span>
                  <div className="sim-inline-field">
                    <input
                      type="text"
                      data-testid="toolbar-tend-input"
                      aria-label="Simulation Duration (t_end)"
                      className={`sim-inline-input ${!isTEndValid || !isRatioValid ? 'invalid' : ''}`}
                      value={tEndDraft}
                      onChange={(e) => handleTEndChange(e.target.value)}
                      onBlur={handleTEndBlur}
                      onKeyDown={handleInputKeyDown}
                      disabled={!circuitId}
                      placeholder="20m"
                    />
                    <select
                      data-testid="toolbar-tend-presets"
                      aria-label="Duration Presets"
                      className="sim-inline-preset-select"
                      value=""
                      disabled={!circuitId}
                      title="Choose a simulation duration (t_end) preset"
                      onChange={(e) => {
                        if (e.target.value) {
                          setTEndDraft(e.target.value);
                          commitSettings({ tEnd: e.target.value });
                        }
                      }}
                    >
                      <option value="" disabled>
                        ▾
                      </option>
                      {T_END_PRESETS.map((preset) => (
                        <option key={preset} value={preset}>
                          {preset}s
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="sim-timing-row">
                  <span className="sim-inline-label">Time Step (dt)</span>
                  <div className="sim-inline-field">
                    <input
                      type="text"
                      data-testid="toolbar-dt-input"
                      aria-label="Time Step (dt)"
                      className={`sim-inline-input ${!isDtValid || !isRatioValid ? 'invalid' : ''}`}
                      value={dtDraft}
                      onChange={(e) => handleDtChange(e.target.value)}
                      onBlur={handleDtBlur}
                      onKeyDown={handleInputKeyDown}
                      disabled={!circuitId}
                      placeholder="1u"
                    />
                    <select
                      data-testid="toolbar-dt-presets"
                      aria-label="Time Step Presets"
                      className="sim-inline-preset-select"
                      value=""
                      disabled={!circuitId}
                      title="Choose a time step (dt) preset or Auto-dt"
                      onChange={(e) => {
                        const chosen = e.target.value;
                        if (chosen === '__auto__') {
                          handleAutoDt();
                        } else if (chosen) {
                          setDtDraft(chosen);
                          commitSettings({ dt: chosen });
                        }
                      }}
                    >
                      <option value="" disabled>
                        ▾
                      </option>
                      <option value="__auto__">⚡ Auto-dt (from circuit)</option>
                      {DT_PRESETS.map((preset) => (
                        <option key={preset} value={preset}>
                          {preset}s
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      data-testid="toolbar-auto-dt-btn"
                      className="sim-inline-auto-btn"
                      onClick={handleAutoDt}
                      disabled={!circuitId}
                      title="Auto-calculate optimal time step (dt) from circuit frequencies"
                    >
                      ⚡ Auto
                    </button>
                  </div>
                </div>

                <div className="sim-timing-row">
                  <span className="sim-inline-label">Solver</span>
                  <div className="sim-timing-solver-wrap">
                    <select
                      data-testid="toolbar-solver-select"
                      aria-label="Solver Method"
                      className="sim-inline-solver-select"
                      value={resolved.solver}
                      disabled={!circuitId}
                      title="Differential equation solver integration method"
                      onChange={(e) => commitSettings({ solver: e.target.value })}
                    >
                      <option value="backward-euler">Backward Euler (BE)</option>
                      <option value="trapezoidal">Trapezoidal (Trap)</option>
                      <option value="gear-shichman">Gear-Shichman (Gear)</option>
                    </select>
                    <button
                      type="button"
                      data-testid="toolbar-steps-pill"
                      className={`sim-toolbar-steps-pill ${stepHealth.tone}`}
                      onClick={() => {
                        setTimingOpen(false);
                        onOpenConfigModal();
                      }}
                      disabled={!circuitId}
                      title={stepHealth.title}
                    >
                      {stepHealth.label}
                    </button>
                  </div>
                </div>

                <div className="sim-timing-footer">
                  <button
                    type="button"
                    data-testid="toolbar-open-setup-btn"
                    className="sim-timing-setup-link"
                    onClick={() => {
                      setTimingOpen(false);
                      onOpenConfigModal();
                    }}
                  >
                    ⚙ Full Simulation Setup…
                  </button>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
