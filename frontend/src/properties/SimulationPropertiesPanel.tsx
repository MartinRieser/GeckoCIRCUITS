/**
 * Simulation Properties & Settings Panel.
 * Rendered in the right sidebar (Inspector) when the Simulation workspace tab is active.
 * Houses all simulation controls, solver parameters, display layout options,
 * scope instrument selectors, and CSV export.
 */
import { useState, useEffect, useMemo } from 'react';
import type { EditorComponent, SimulationDefaults, SimulationStatus } from '../model/types';
import {
  parseEngineeringValue,
  formatEngineeringValue,
} from '../model/componentSchema';
import { mapSimulationResults } from '../simulation/chartData';
import { estimateStepCount, STEP_WARNING_THRESHOLD } from '../simulation/simSteps';

interface SimulationPropertiesPanelProps {
  circuitId: string | null;
  status: SimulationStatus | null;
  progress: number;
  defaults?: SimulationDefaults | null;
  errorMessage?: string | null;
  components: EditorComponent[];
  results: Record<string, number[]> | null;
  selectedScope: string;
  onSelectScope: (scope: string) => void;
  displayLayout: 'overlay' | 'stacked';
  onDisplayLayoutChange: (layout: 'overlay' | 'stacked') => void;
  onRunSimulation: (config: {
    simulationTime: number;
    timeStep: number;
    solverType: string;
    backend?: string;
  }) => void;
  onPauseSimulation?: () => void;
  onResumeSimulation?: () => void;
  onCancelSimulation?: () => void;
  onExportCsv?: () => void;
}

export function SimulationPropertiesPanel({
  circuitId,
  status,
  progress,
  defaults,
  errorMessage,
  components,
  results,
  selectedScope,
  onSelectScope,
  displayLayout,
  onDisplayLayoutChange,
  onRunSimulation,
  onPauseSimulation,
  onResumeSimulation,
  onCancelSimulation,
  onExportCsv,
}: SimulationPropertiesPanelProps) {
  const [tEndStr, setTEndStr] = useState('20m');
  const [dtStr, setDtStr] = useState('1u');
  const [solverType, setSolverType] = useState('backward-euler');
  const [backendEngine, setBackendEngine] = useState('headless');

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
      if ((defaults as { backend?: string }).backend) {
        setBackendEngine((defaults as { backend?: string }).backend!);
      }
    }
  }, [defaults]);

  const scopeBlocks = useMemo(() => {
    return components.filter(
      (c) =>
        c.type === 5 ||
        c.type === 1003 ||
        c.name.toUpperCase().startsWith('SCOPE') ||
        c.name.toUpperCase().startsWith('OSZI'),
    );
  }, [components]);

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

  const handleRun = () => {
    const dur = tEndNum !== null && !isNaN(tEndNum) && tEndNum > 0 ? tEndNum : 0.02;
    const step = dtNum !== null && !isNaN(dtNum) && dtNum > 0 ? dtNum : 1e-6;
    onRunSimulation({
      simulationTime: dur,
      timeStep: step,
      solverType,
      backend: backendEngine,
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
        {status && (
          <span className={`sim-status-badge ${status.toLowerCase()}`}>
            {status}
          </span>
        )}
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

            <div className="prop-field">
              <label className="prop-label" htmlFor="insp-backend">
                Simulation Engine
              </label>
              <select
                id="insp-backend"
                className="prop-select"
                value={backendEngine}
                onChange={(e) => setBackendEngine(e.target.value)}
                disabled={isRunning}
              >
                <option value="headless">Gecko Headless Core</option>
                <option value="classic">Classic Simulation</option>
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
