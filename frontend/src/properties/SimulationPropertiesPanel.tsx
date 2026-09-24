/**
 * Simulation Properties & Settings Panel.
 * Rendered in the right sidebar (Inspector) when the Simulation workspace tab is active.
 * Houses all simulation controls, solver parameters, display layout options,
 * scope instrument selectors, and CSV export.
 */
import { useState, useEffect, useMemo } from 'react';
import type { EditorComponent, EditorWire, SimulationDefaults, SimulationStatus } from '../model/types';
import { validateCircuitForSimulation } from '../model/validation';
import {
  parseEngineeringValue,
  formatEngineeringValue,
} from '../model/componentSchema';
import { mapSimulationResults } from '../simulation/chartData';
import { findScopeBlocks } from '../simulation/scopes';
import { estimateStepCount, STEP_WARNING_THRESHOLD } from '../simulation/simSteps';
import type { ScopeController } from '../simulation/useScopeController';

/**
 * Properties for the {@link SimulationPropertiesPanel} sidebar component.
 */
export interface SimulationPropertiesPanelProps {
  /** Identifier of the currently loaded circuit workspace, or null. */
  circuitId: string | null;
  /** Current simulation execution status (e.g. RUNNING, FINISHED, FAILED). */
  status: SimulationStatus | null;
  /** Simulation progress from 0.0 to 1.0. */
  progress: number;
  /** Engine simulation defaults (tend, dt, etc.). */
  defaults?: SimulationDefaults | null;
  /** Error message if simulation failed. */
  errorMessage?: string | null;
  /** Circuit validation warnings produced before running. */
  engineWarnings?: string[];
  /** Schematic components. */
  components: EditorComponent[];
  /** Schematic wires. */
  wires?: EditorWire[];
  /** Simulation results time-series mapping, if available. */
  results: Record<string, number[]> | null;
  /** Currently selected scope block identifier. */
  selectedScope: string;
  /** Callback when user changes the active oscilloscope block. */
  onSelectScope: (scope: string) => void;
  /** Waveform chart display layout mode. */
  displayLayout: 'overlay' | 'stacked';
  /** Callback to change waveform display layout mode. */
  onDisplayLayoutChange: (layout: 'overlay' | 'stacked') => void;
  /** Optional oscilloscope controller instance. */
  scope?: ScopeController;
  /** Callback invoked to start simulation run with specified configuration parameters. */
  onRunSimulation: (config: {
    simulationTime: number;
    timeStep: number;
    solverType: string;
    backend?: string;
  }) => void;
  /** Callback to pause simulation. */
  onPauseSimulation?: () => void;
  /** Callback to resume simulation. */
  onResumeSimulation?: () => void;
  /** Callback to cancel running simulation. */
  onCancelSimulation?: () => void;
  /** Callback to export results as CSV. */
  onExportCsv?: () => void;
  /** Callback to collapse the simulation panel. */
  onCollapse?: () => void;
}

/**
 * Inspector panel for simulation configuration, solver selection, scope instrument switching,
 * time-step warnings, and run/pause/resume/cancel controls.
 */
export function SimulationPropertiesPanel({
  circuitId,
  status,
  progress,
  defaults,
  errorMessage,
  engineWarnings,
  components,
  wires,
  results,
  selectedScope,
  onSelectScope,
  displayLayout,
  onDisplayLayoutChange,
  scope,
  onRunSimulation,
  onPauseSimulation,
  onResumeSimulation,
  onCancelSimulation,
  onExportCsv,
  onCollapse,
}: SimulationPropertiesPanelProps) {
  const [tEndStr, setTEndStr] = useState('20m');
  const [dtStr, setDtStr] = useState('1u');
  const [solverType, setSolverType] = useState('backward-euler');

  useEffect(() => {
    if (defaults) {
      if (defaults.duration !== undefined) {
        setTEndStr(formatEngineeringValue(defaults.duration, 's'));
      }
      if (defaults.timeStep !== undefined) {
        setDtStr(formatEngineeringValue(defaults.timeStep, 's'));
      }
      if (defaults.solverType) {
        setSolverType(defaults.solverType);
      }
    }
  }, [defaults]);

  const scopeBlocks = useMemo(() => findScopeBlocks(components), [components]);

  const { signalNames, timeArray, signalStats } = useMemo(
    () => mapSimulationResults(results),
    [results],
  );

  const isRunning = status === 'PENDING' || status === 'RUNNING';
  const isPaused = status === 'PAUSED';

  const tEndNum = parseEngineeringValue(tEndStr);
  const dtNum = parseEngineeringValue(dtStr);
  const estSteps = estimateStepCount(tEndNum ?? 0.02, dtNum ?? 1e-6);
  const isHeavyRun = estSteps > STEP_WARNING_THRESHOLD;

  // Pre-run validation: first click on "Run Simulation" surfaces likely circuit
  // mistakes (dangling wires, missing source, unsupported components); a second
  // click ("Run Anyway") starts the run regardless.
  const [pendingWarnings, setPendingWarnings] = useState<string[] | null>(null);

  useEffect(() => {
    setPendingWarnings(null);
  }, [circuitId]);

  const handleRun = () => {
    const warnings = validateCircuitForSimulation(components, wires ?? []);
    if (warnings.length > 0 && pendingWarnings === null) {
      setPendingWarnings(warnings);
      return;
    }
    setPendingWarnings(null);
    const dur = tEndNum !== null && !isNaN(tEndNum) && tEndNum > 0 ? tEndNum : 0.02;
    const step = dtNum !== null && !isNaN(dtNum) && dtNum > 0 ? dtNum : 1e-6;
    onRunSimulation({
      simulationTime: dur,
      timeStep: step,
      solverType,
      backend: 'headless',
    });
  };

  const handleExportCsv = () => {
    if (onExportCsv) {
      onExportCsv();
      return;
    }
    if (!results || !timeArray.length) return;
    const headers = ['time', ...signalNames];
    const rows = [headers.join(',')];

    for (let i = 0; i < timeArray.length; i++) {
      const row = [timeArray[i], ...signalNames.map((s) => results[s]?.[i] ?? 0)];
      rows.push(row.join(','));
    }

    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${selectedScope}_results_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="properties-container sim-properties-panel">
      {/* Header */}
      <div className="properties-header-bar">
        <span className="properties-title">Simulation Settings</span>
        <div className="properties-header-actions" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          {status && (
            <span className={`sim-status-badge ${status.toLowerCase()}`}>
              {status}
            </span>
          )}
          {onCollapse && (
            <button
              type="button"
              className="action-icon-btn collapse-btn"
              onClick={onCollapse}
              title="Collapse simulation settings (Ctrl+I)"
            >
              ▶
            </button>
          )}
        </div>
      </div>

      <div className="properties-body">
        {/* Run Controls Box */}
        <div className="prop-section" style={{ marginBottom: 12 }}>
          <div className="sim-run-actions-vertical" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {!isRunning && !isPaused && (
              <button
                type="button"
                className="sim-btn run"
                onClick={handleRun}
                disabled={!circuitId}
                style={{ width: '100%', justifyContent: 'center', padding: '9px 12px', fontSize: '13px' }}
                title="Run circuit simulation"
              >
                ▶ Run Simulation
              </button>
            )}

            {pendingWarnings && pendingWarnings.length > 0 && !isRunning && (
              <div className="sim-warnings" role="alert">
                <div className="sim-warnings-title">Check before running:</div>
                <ul>
                  {pendingWarnings.map((w) => (
                    <li key={w}>{w}</li>
                  ))}
                </ul>
                <button
                  type="button"
                  className="sim-btn run"
                  onClick={handleRun}
                  style={{ width: '100%', justifyContent: 'center', padding: '7px 12px', fontSize: '12px' }}
                  title="Ignore the warnings above and start the simulation"
                >
                  Run Anyway
                </button>
              </div>
            )}

            {isRunning && (
              <div style={{ display: 'flex', gap: 6 }}>
                {onPauseSimulation && (
                  <button
                    type="button"
                    className="sim-btn pause"
                    onClick={onPauseSimulation}
                    style={{ flex: 1, justifyContent: 'center' }}
                    title="Pause simulation"
                  >
                    ⏸ Pause
                  </button>
                )}
                {onCancelSimulation && (
                  <button
                    type="button"
                    className="sim-btn cancel"
                    onClick={onCancelSimulation}
                    style={{ flex: 1, justifyContent: 'center' }}
                    title="Cancel simulation"
                  >
                    ⏹ Cancel
                  </button>
                )}
              </div>
            )}

            {isPaused && (
              <div style={{ display: 'flex', gap: 6 }}>
                {onResumeSimulation && (
                  <button
                    type="button"
                    className="sim-btn resume"
                    onClick={onResumeSimulation}
                    style={{ flex: 1, justifyContent: 'center' }}
                    title="Resume simulation"
                  >
                    ▶ Resume
                  </button>
                )}
                {onCancelSimulation && (
                  <button
                    type="button"
                    className="sim-btn cancel"
                    onClick={onCancelSimulation}
                    style={{ flex: 1, justifyContent: 'center' }}
                    title="Cancel simulation"
                  >
                    ⏹ Cancel
                  </button>
                )}
              </div>
            )}

            {isRunning && (
              <div className="sim-progress-track" style={{ marginTop: 4 }}>
                <div
                  className="sim-progress-bar"
                  style={{ width: `${Math.max(5, Math.min(100, progress * 100))}%` }}
                />
              </div>
            )}

            {errorMessage && (
              <div className="sim-error-banner" style={{ marginTop: 6, fontSize: '11px' }}>
                <span className="error-icon">✕</span>
                <span className="error-text">{errorMessage}</span>
              </div>
            )}

            {!isRunning && engineWarnings && engineWarnings.length > 0 && (
              <div className="sim-warnings" style={{ marginTop: 6 }}>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>
                  ⚠️ Simulation ran, but:
                </div>
                <ul style={{ margin: '0 0 0 16px', padding: 0 }}>
                  {engineWarnings.map((w, i) => (
                    <li key={i} style={{ fontSize: '11px' }}>{w}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>

        {/* Solver Parameters Section */}
        <div className="prop-section">
          <div className="prop-section-title">Solver Parameters</div>
          <div className="prop-fields-list">
            <div className="prop-field">
              <label className="prop-label" htmlFor="insp-tend">
                Duration (tEnd)
              </label>
              <input
                id="insp-tend"
                type="text"
                className="prop-input"
                value={tEndStr}
                onChange={(e) => setTEndStr(e.target.value)}
                placeholder="e.g. 20m, 0.05"
                disabled={isRunning}
              />
            </div>

            <div className="prop-field">
              <label className="prop-label" htmlFor="insp-dt">
                Time Step (dt)
              </label>
              <input
                id="insp-dt"
                type="text"
                className="prop-input"
                value={dtStr}
                onChange={(e) => setDtStr(e.target.value)}
                placeholder="e.g. 1u, 1e-6"
                disabled={isRunning}
              />
            </div>

            <div className="prop-field">
              <label className="prop-label" htmlFor="insp-solver">
                Integration Method
              </label>
              <select
                id="insp-solver"
                className="prop-select"
                value={solverType}
                onChange={(e) => setSolverType(e.target.value)}
                disabled={isRunning}
              >
                <option value="backward-euler">Backward Euler</option>
                <option value="trapezoidal">Trapezoidal</option>
                <option value="gear-shichman">Gear-Shichman</option>
              </select>
            </div>

            <div className="prop-field" style={{ marginTop: 4 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '11px', color: 'var(--text-muted)' }}>
                <span>Estimated Steps:</span>
                <strong style={{ color: isHeavyRun ? 'var(--wire)' : 'var(--text)' }}>
                  {estSteps.toLocaleString()}
                </strong>
              </div>
              {isHeavyRun && (
                <div style={{ fontSize: '10px', color: 'var(--wire)', marginTop: 2 }}>
                  ⚠ Over 1M steps may affect browser plot rendering speed.
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Display Layout Mode Section */}
        <div className="prop-section">
          <div className="prop-section-title">Display Layout</div>
          <div className="segmented-control" style={{ width: '100%', display: 'flex' }}>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
              onClick={() => onDisplayLayoutChange('overlay')}
              style={{ flex: 1, textAlign: 'center', justifyContent: 'center' }}
              title="Overlay all channels on a single combined graph"
            >
              📈 Overlay
            </button>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
              onClick={() => onDisplayLayoutChange('stacked')}
              style={{ flex: 1, textAlign: 'center', justifyContent: 'center' }}
              title="Stack each channel in its own subplot lane"
            >
              📑 Stacked Lanes
            </button>
          </div>
        </div>

        {/* Scope Selection & Instruments Section */}
        <div className="prop-section">
          <div className="prop-section-title">Scope Instrument</div>
          {/* Scope Dropdown */}
          <div style={{ marginBottom: 8 }}>
            <select
              id="insp-scope-select"
              className="prop-select"
              value={selectedScope}
              onChange={(e) => onSelectScope(e.target.value)}
              title="Select Scope instrument to view"
            >
              <option value="all">🌐 All Scopes & Signals ({signalNames.length})</option>
              {scopeBlocks.map((sb) => {
                const chCount = sb.inputLabels.filter(Boolean).length;
                const labels = sb.inputLabels.filter(Boolean).join(', ');
                return (
                  <option key={sb.name} value={sb.name}>
                    📺 {sb.name} ({chCount} ch{labels ? `: ${labels}` : ''})
                  </option>
                );
              })}
            </select>
          </div>

          {/* Scope List Item Buttons */}
          <div className="prop-scopes-list" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <button
              type="button"
              className={`scope-nav-item ${selectedScope === 'all' ? 'active' : ''}`}
              onClick={() => onSelectScope('all')}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '7px 10px',
                borderRadius: '6px',
                border: '1px solid var(--border)',
                background: selectedScope === 'all' ? 'var(--surface-hover)' : 'var(--surface)',
                color: selectedScope === 'all' ? 'var(--accent)' : 'var(--text)',
                cursor: 'pointer',
                fontSize: '12px',
                fontWeight: selectedScope === 'all' ? 700 : 500,
              }}
            >
              <span>🌐 All Scopes & Signals</span>
              <span className="workspace-tab-badge">{signalNames.length}</span>
            </button>

            {scopeBlocks.map((sb) => {
              const chCount = sb.inputLabels.filter(Boolean).length;
              const isSelected = selectedScope === sb.name;
              return (
                <button
                  key={sb.name}
                  type="button"
                  className={`scope-nav-item ${isSelected ? 'active' : ''}`}
                  onClick={() => onSelectScope(sb.name)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '7px 10px',
                    borderRadius: '6px',
                    border: '1px solid var(--border)',
                    background: isSelected ? 'var(--surface-hover)' : 'var(--surface)',
                    color: isSelected ? 'var(--accent)' : 'var(--text)',
                    cursor: 'pointer',
                    fontSize: '12px',
                    fontWeight: isSelected ? 700 : 500,
                  }}
                >
                  <span>📺 {sb.name}</span>
                  <span className="workspace-tab-badge">{chCount} ch</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Oscilloscope Settings & Controls */}
        {scope && (
          <div className="prop-section sim-oszi-section" data-testid="sidebar-oszi-controls">
            <div className="prop-section-title">Oscilloscope Controls</div>

            {/* Horizontal / Timebase */}
            <div className="sim-sub-heading">Horizontal (Timebase)</div>
            <div className="sim-oszi-row">
              <span className="sim-oszi-badge" title="Time per major division">
                ⏱ <strong>{formatEngineeringValue(scope.timePerDiv, 's')}/div</strong>
              </span>
              <button
                type="button"
                className="sim-mini-btn"
                onClick={scope.fit}
                title="Fit Full Simulation Waveform"
              >
                ⟲ Fit
              </button>
            </div>

            <div className="sim-btn-row" style={{ marginTop: 6 }}>
              <div className="sim-btn-group-2">
                <button
                  type="button"
                  className="sim-mini-btn"
                  onClick={() => scope.zoomAt(0.7, (scope.dataT0 + scope.dataT1) / 2)}
                  title="Zoom In Timebase (+)"
                >
                  Zoom +
                </button>
                <button
                  type="button"
                  className="sim-mini-btn"
                  onClick={() => scope.zoomAt(1.4, (scope.dataT0 + scope.dataT1) / 2)}
                  title="Zoom Out Timebase (−)"
                >
                  Zoom −
                </button>
              </div>
              <div className="sim-btn-group-2">
                <button
                  type="button"
                  className="sim-mini-btn"
                  onClick={() => scope.pan(-0.2)}
                  title="Pan Left (◀)"
                >
                  ◀ Pan
                </button>
                <button
                  type="button"
                  className="sim-mini-btn"
                  onClick={() => scope.pan(0.2)}
                  title="Pan Right (▶)"
                >
                  Pan ▶
                </button>
              </div>
            </div>

            {/* Vertical & Channels */}
            <div className="sim-sub-heading" style={{ marginTop: 12 }}>Vertical (Scale & Channels)</div>
            <div style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
              <button
                type="button"
                className={`sim-toggle-btn ${scope.yScaleMode === 'fixed' ? 'active' : ''}`}
                onClick={() => scope.setYScaleMode((prev) => (prev === 'fixed' ? 'auto' : 'fixed'))}
                title={scope.yScaleMode === 'fixed' ? 'Locked: Zooming time preserves voltage scale' : 'Auto: Rescales voltage to fit visible points'}
                style={{ flex: 1 }}
              >
                ↕ Scale: {scope.yScaleMode === 'fixed' ? 'Fixed' : 'Auto'}
              </button>
            </div>

            {/* Channel toggles */}
            <div className="sim-channels-list">
              {scope.scopeChannelNames.map((name) => {
                const color = scope.colorOf(name);
                const isHidden = !!scope.hiddenSignals[name];
                return (
                  <button
                    key={name}
                    type="button"
                    className={`sim-channel-badge ${isHidden ? 'hidden' : 'active'}`}
                    onClick={() => scope.toggleSignal(name)}
                    style={{
                      borderColor: color,
                      color: isHidden ? 'var(--text-dim)' : color,
                    }}
                    title={isHidden ? `Show ${name}` : `Hide ${name}`}
                  >
                    <span className="sim-ch-led" style={{ backgroundColor: color }} />
                    {name}
                  </button>
                );
              })}
            </div>

            {/* Cursors & Measurements (The ONE Location!) */}
            <div className="sim-sub-heading" style={{ marginTop: 12 }}>Cursors & Measurement</div>
            <div style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
              <button
                type="button"
                className={`sim-toggle-btn ${scope.cursorsEnabled ? 'active' : ''}`}
                onClick={() => {
                  if (scope.cursorsEnabled) {
                    scope.clearCursors();
                  } else {
                    scope.setCursorPreset();
                  }
                }}
                style={{ flex: 1 }}
              >
                📍 Cursors: {scope.cursorsEnabled ? 'ON' : 'OFF'}
              </button>
            </div>

            {scope.cursorsEnabled && (
              <div className="sim-cursor-controls-box">
                <div style={{ display: 'flex', gap: 4, marginBottom: 8 }}>
                  <button
                    type="button"
                    className={`sim-mini-btn ${scope.activeCursor === 'A' ? 'active' : ''}`}
                    onClick={() => scope.setActiveCursor('A')}
                    style={{ flex: 1 }}
                    title="Select Cursor A"
                  >
                    [A]
                  </button>
                  <button
                    type="button"
                    className={`sim-mini-btn ${scope.activeCursor === 'B' ? 'active' : ''}`}
                    onClick={() => scope.setActiveCursor('B')}
                    style={{ flex: 1 }}
                    title="Select Cursor B"
                  >
                    [B]
                  </button>
                  <button
                    type="button"
                    className="sim-mini-btn"
                    onClick={scope.setCursorPreset}
                    title="Snap to 25% and 75% of waveform"
                  >
                    25/75%
                  </button>
                  <button
                    type="button"
                    className="sim-mini-btn"
                    onClick={scope.clearCursors}
                    title="Clear Cursors"
                  >
                    ✕
                  </button>
                </div>

                <div className="sim-hint-text">
                  💡 Drag [A] / [B] handles on plot, or click plot to position active cursor.
                </div>

                {/* THE SINGLE LOCATION FOR CURSOR MEASUREMENTS */}
                {scope.cursorMeasurements && (
                  <div className="sim-cursor-card">
                    <div className="sim-cursor-time-grid">
                      <div className="sim-cursor-time-item">
                        <span className="sim-cursor-lbl">tA:</span>
                        <span className="sim-cursor-val">
                          {scope.cursorMeasurements.timeA !== null
                            ? formatEngineeringValue(scope.cursorMeasurements.timeA, 's')
                            : '—'}
                        </span>
                      </div>
                      <div className="sim-cursor-time-item">
                        <span className="sim-cursor-lbl">tB:</span>
                        <span className="sim-cursor-val">
                          {scope.cursorMeasurements.timeB !== null
                            ? formatEngineeringValue(scope.cursorMeasurements.timeB, 's')
                            : '—'}
                        </span>
                      </div>
                    </div>

                    {scope.cursorMeasurements.dt !== null && (
                      <div className="sim-cursor-delta-row">
                        <div>
                          <span className="sim-cursor-lbl">Δt: </span>
                          <strong className="sim-cursor-highlight">
                            {formatEngineeringValue(scope.cursorMeasurements.dt, 's')}
                          </strong>
                        </div>
                        <div>
                          <span className="sim-cursor-lbl">1/Δt: </span>
                          <strong className="sim-cursor-highlight">
                            {scope.cursorMeasurements.freq !== null
                              ? formatEngineeringValue(scope.cursorMeasurements.freq, 'Hz')
                              : '—'}
                          </strong>
                        </div>
                      </div>
                    )}

                    {scope.cursorMeasurements.channels.length > 0 && (
                      <div className="sim-cursor-table-wrap">
                        <table className="sim-cursor-table">
                          <thead>
                            <tr>
                              <th>Ch</th>
                              <th>A</th>
                              <th>B</th>
                              <th>Δ</th>
                            </tr>
                          </thead>
                          <tbody>
                            {scope.cursorMeasurements.channels.map((ch) => {
                              const isCurrent = ch.name.toLowerCase().startsWith('i');
                              const unit = isCurrent ? 'A' : 'V';
                              return (
                                <tr key={ch.name}>
                                  <td style={{ color: ch.color, fontWeight: 600 }}>
                                    <span
                                      className="sim-ch-led-mini"
                                      style={{ backgroundColor: ch.color }}
                                    />
                                    {ch.name}
                                  </td>
                                  <td>{ch.valA !== null ? formatEngineeringValue(ch.valA, unit) : '—'}</td>
                                  <td>{ch.valB !== null ? formatEngineeringValue(ch.valB, unit) : '—'}</td>
                                  <td className="sim-delta-col">
                                    {ch.delta !== null
                                      ? `${ch.delta >= 0 ? '+' : ''}${formatEngineeringValue(ch.delta, unit)}`
                                      : '—'}
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Detailed Analysis Tools */}
            <div className="sim-sub-heading" style={{ marginTop: 12 }}>Detailed Analysis</div>
            <div className="sim-tools-row">
              <button
                type="button"
                className={`sim-mini-btn ${scope.drawerTab === 'metrics' ? 'active' : ''}`}
                onClick={() => scope.setDrawerTab((prev) => (prev === 'metrics' ? null : 'metrics'))}
                title="View detailed signal statistics table in bottom drawer"
              >
                📊 Statistics
              </button>
              <button
                type="button"
                className={`sim-mini-btn ${scope.drawerTab === 'fft' ? 'active' : ''}`}
                onClick={() => scope.setDrawerTab((prev) => (prev === 'fft' ? null : 'fft'))}
                title="FFT Harmonic Spectrum Analyzer in bottom drawer"
              >
                〰 FFT
              </button>
              <button
                type="button"
                className={`sim-mini-btn ${scope.drawerTab === 'losses' ? 'active' : ''}`}
                onClick={() => scope.setDrawerTab((prev) => (prev === 'losses' ? null : 'losses'))}
                title="Semiconductor Loss Breakdown in bottom drawer"
              >
                ⚡ Losses
              </button>
            </div>
          </div>
        )}

        {/* Results & Export Summary */}
        {results && signalNames.length > 0 && (
          <div className="prop-section">
            <div className="prop-section-title">Signals & Export</div>
            <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: 8 }}>
              {signalNames.length} signals recorded ({Object.keys(signalStats).length} computed)
            </div>
            <button
              type="button"
              className="sim-btn export"
              onClick={handleExportCsv}
              style={{ width: '100%', justifyContent: 'center' }}
              title="Export waveform data to CSV"
            >
              Export CSV File
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
