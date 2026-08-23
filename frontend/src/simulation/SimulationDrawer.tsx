/**
 * Simulation Control & Interactive Waveform Results Viewer.
 * Features:
 * - Simulation configuration (tEnd, dt, solver selection) pre-filled from the
 *   circuit's .ipes metadata; recorded-signal selection from the file
 * - Run / Pause / Resume / Cancel with live SSE progress
 * - Interactive multi-trace SVG waveform chart with hover crosshair and tooltips
 * - Signal visibility toggles and color legends
 * - Signal statistics (Min, Max, Peak-to-Peak, RMS)
 * - CSV export
 */
import { useState, useMemo, useRef, useEffect } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import type { SimulationDefaults, SimulationStatus } from '../model/types';
import {
  parseEngineeringValue,
  formatEngineeringValue,
} from '../model/componentSchema';
import { mapSimulationResults } from './chartData';

interface SimulationDrawerProps {
  isOpen: boolean;
  onToggle: () => void;
  circuitId: string | null;
  defaults: SimulationDefaults | null;
  onRunSimulation: (config: {
    simulationTime: number;
    timeStep: number;
    solverType: string;
    signals?: string[];
  }) => void;
  onCancelSimulation?: () => void;
  onPauseSimulation?: () => void;
  onResumeSimulation?: () => void;
  status: SimulationStatus | null;
  progress: number;
  results: Record<string, number[]> | null;
  errorMessage?: string | null;
}

const TRACE_COLORS = [
  '#38bdf8', // Sky blue
  '#f43f5e', // Rose
  '#10b981', // Emerald
  '#fbbf24', // Amber
  '#a855f7', // Purple
  '#3b82f6', // Blue
  '#ec4899', // Pink
  '#14b8a6', // Teal
];

export function SimulationDrawer({
  isOpen,
  onToggle,
  circuitId,
  defaults,
  onRunSimulation,
  onCancelSimulation,
  onPauseSimulation,
  onResumeSimulation,
  status,
  progress,
  results,
  errorMessage,
}: SimulationDrawerProps) {
  const [tEndStr, setTEndStr] = useState('20m');
  const [dtStr, setDtStr] = useState('1u');
  const [solverType, setSolverType] = useState('backward-euler');
  const [recordedSignals, setRecordedSignals] = useState<string[]>([]);
  const [hiddenSignals, setHiddenSignals] = useState<Record<string, boolean>>({});
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const [seededCircuit, setSeededCircuit] = useState<string | null>(null);

  // Re-seed the parameter inputs whenever another circuit is opened
  useEffect(() => {
    if (!defaults || seededCircuit === circuitId) return;
    setSeededCircuit(circuitId);
    if (defaults.duration > 0) setTEndStr(formatEngineeringValue(defaults.duration));
    if (defaults.timeStep > 0) setDtStr(formatEngineeringValue(defaults.timeStep));
    if (defaults.solverType) setSolverType(defaults.solverType);
    setRecordedSignals(defaults.signals);
  }, [circuitId, defaults, seededCircuit]);

  const isRunning =
    status === 'PENDING' || status === 'RUNNING' || status === 'PAUSED';

  const parsedTEnd = parseEngineeringValue(tEndStr) ?? defaults?.duration ?? 0.02;
  const parsedDt = parseEngineeringValue(dtStr) ?? defaults?.timeStep ?? 1e-6;
  const stepCount = parsedDt > 0 ? Math.round(parsedTEnd / parsedDt) : 0;

  const handleRun = () => {
    if (!circuitId) return;
    const tEnd = parsedTEnd;
    const dt = parsedDt;
    onRunSimulation({
      simulationTime: tEnd,
      timeStep: dt,
      solverType,
      signals: recordedSignals.length > 0 ? recordedSignals : undefined,
    });
  };

  const toggleRecorded = (name: string) => {
    setRecordedSignals((prev) =>
      prev.includes(name) ? prev.filter((s) => s !== name) : [...prev, name],
    );
  };

  // Signal names and time vector
  const { signalNames, timeArray, signalStats } = useMemo(
    () => mapSimulationResults(results),
    [results],
  );

  const toggleSignal = (name: string) => {
    setHiddenSignals((prev) => ({ ...prev, [name]: !prev[name] }));
  };

  const handleExportCsv = () => {
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
    a.download = `simulation_results_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className={`sim-drawer ${isOpen ? 'open' : 'closed'}`}>
      {/* Drawer header / control bar */}
      <div className="sim-header">
        <div className="sim-title-group">
          <button
            type="button"
            className="sim-toggle-btn"
            onClick={onToggle}
            title={isOpen ? 'Collapse Simulation Panel' : 'Expand Simulation Panel'}
          >
            {isOpen ? '▼' : '▲'} Simulation
          </button>
          {status && (
            <span className={`sim-status-badge ${status.toLowerCase()}`}>
              {status}
            </span>
          )}
        </div>

        <div className="sim-params-bar">
          <div className="sim-param-item">
            <label htmlFor="sim-tend-input">Duration (tEnd):</label>
            <input
              id="sim-tend-input"
              type="text"
              className="sim-param-input"
              value={tEndStr}
              onChange={(e) => setTEndStr(e.target.value)}
              placeholder="e.g. 20m, 0.05"
              disabled={isRunning}
            />
          </div>

          <div className="sim-param-item">
            <label htmlFor="sim-dt-input">Time step (dt):</label>
            <input
              id="sim-dt-input"
              type="text"
              className="sim-param-input"
              value={dtStr}
              onChange={(e) => setDtStr(e.target.value)}
              placeholder="e.g. 1u, 1e-6"
              disabled={isRunning}
            />
          </div>

          <div className="sim-param-item">
            <label htmlFor="sim-solver-select">Solver:</label>
            <select
              id="sim-solver-select"
              className="sim-param-select"
              value={solverType}
              onChange={(e) => setSolverType(e.target.value)}
              disabled={isRunning}
            >
              <option value="backward-euler">Backward Euler</option>
              <option value="trapezoidal">Trapezoidal</option>
              <option value="gear-shichman">Gear-Shichman</option>
            </select>
          </div>

          <div className="sim-param-item steps-info" title={`${parsedTEnd}s / ${parsedDt}s`}>
            {stepCount > 2_000_000 ? (
              <span className="steps-warn">{stepCount.toLocaleString()} steps</span>
            ) : (
              stepCount > 0 && <span>{stepCount.toLocaleString()} steps</span>
            )}
          </div>

          {isRunning ? (
            <>
              {status === 'PAUSED' ? (
                <button
                  type="button"
                  className="sim-btn run"
                  onClick={onResumeSimulation}
                  title="Resume the paused simulation"
                >
                  Resume
                </button>
              ) : (
                <button
                  type="button"
                  className="sim-btn pause"
                  onClick={onPauseSimulation}
                  title="Pause the running simulation"
                >
                  Pause
                </button>
              )}
              <button
                type="button"
                className="sim-btn cancel"
                onClick={onCancelSimulation}
              >
                Cancel
              </button>
            </>
          ) : (
            <button
              type="button"
              className="sim-btn run"
              onClick={handleRun}
              disabled={!circuitId}
              title="Run simulation on active circuit"
            >
              Run Simulation
            </button>
          )}

          {results && signalNames.length > 0 && (
            <button
              type="button"
              className="sim-btn export"
              onClick={handleExportCsv}
              title="Export results as CSV"
            >
              Export CSV
            </button>
          )}
        </div>
      </div>

      {/* Recorded-signal selection (from the file's dataContainerSignals,
          falling back to the circuit's node labels) */}
      {!isRunning && defaults && defaults.signals.length > 0 && (
        <div className="sim-signals-bar">
          <span className="sim-signals-label">Record:</span>
          {defaults.signals.map((name) => {
            const active = recordedSignals.includes(name);
            return (
              <button
                key={name}
                type="button"
                className={`legend-pill ${active ? 'active' : 'hidden'}`}
                onClick={() => toggleRecorded(name)}
                title={active ? 'Exclude this signal from the next run' : 'Record this signal in the next run'}
              >
                <span className="legend-dot" />
                {name}
              </button>
            );
          })}
        </div>
      )}

      {/* Progress bar */}
      {isRunning && (
        <div className={`sim-progress-track ${status === 'PAUSED' ? 'paused' : ''}`}>
          <div
            className="sim-progress-bar"
            style={{ width: `${Math.max(5, Math.min(100, progress * 100))}%` }}
          />
        </div>
      )}

      {/* Drawer content (Waveforms & Statistics) */}
      {isOpen && (
        <div className="sim-content">
          {errorMessage && (
            <div className="sim-error-banner">
              Simulation error: {errorMessage}
            </div>
          )}

          {!results || signalNames.length === 0 ? (
            <div className="sim-empty-state">
              <span className="sim-empty-text">
                {isRunning
                  ? 'Running simulation...'
                  : status === 'COMPLETED'
                    ? 'Simulation completed but no signals were recorded. Ensure the circuit has measurable nodes (e.g. voltage across a component).'
                    : 'Click "Run Simulation" above to calculate and plot circuit waveforms.'}
              </span>
            </div>
          ) : (
            <div className="sim-results-grid">
              {/* Left: Waveform plot */}
              <div className="sim-plot-card">
                {/* Signal legend toggles */}
                <div className="sim-legend-bar">
                  {signalNames.map((name, i) => {
                    const color = TRACE_COLORS[i % TRACE_COLORS.length];
                    const isHidden = !!hiddenSignals[name];
                    return (
                      <button
                        key={name}
                        type="button"
                        className={`legend-pill ${isHidden ? 'hidden' : 'active'}`}
                        onClick={() => toggleSignal(name)}
                        style={{
                          borderColor: color,
                          color: isHidden ? '#64748b' : '#f8fafc',
                          backgroundColor: isHidden
                            ? 'transparent'
                            : `${color}22`,
                        }}
                      >
                        <span
                          className="legend-dot"
                          style={{ backgroundColor: color }}
                        />
                        {name}
                      </button>
                    );
                  })}
                </div>

                {/* SVG Chart */}
                <WaveformChart
                  time={timeArray}
                  signals={results}
                  activeSignals={signalNames.filter((s) => !hiddenSignals[s])}
                  allSignals={signalNames}
                  hoverIndex={hoverIndex}
                  onHoverIndex={setHoverIndex}
                />
              </div>

              {/* Right: Metrics / Statistics Table */}
              <div className="sim-stats-card">
                <div className="stats-header">Signal Statistics</div>
                <div className="stats-table-wrap">
                  <table className="stats-table">
                    <thead>
                      <tr>
                        <th>Signal</th>
                        <th>Min</th>
                        <th>Max</th>
                        <th>Pk-Pk</th>
                        <th>RMS</th>
                      </tr>
                    </thead>
                    <tbody>
                      {signalNames.map((name, i) => {
                        const st = signalStats[name];
                        if (!st) return null;
                        const color = TRACE_COLORS[i % TRACE_COLORS.length];
                        return (
                          <tr key={name}>
                            <td style={{ color, fontWeight: 600 }}>{name}</td>
                            <td>{formatEngineeringValue(st.min)}</td>
                            <td>{formatEngineeringValue(st.max)}</td>
                            <td>{formatEngineeringValue(st.pkpk)}</td>
                            <td>{formatEngineeringValue(st.rms)}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * Interactive SVG Waveform Chart with gridlines, auto-scaling, and crosshair cursor.
 */
function WaveformChart({
  time,
  signals,
  activeSignals,
  allSignals,
  hoverIndex,
  onHoverIndex,
}: {
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  allSignals: string[];
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const width = 640;
  const height = 260;
  const padLeft = 60;
  const padRight = 20;
  const padTop = 20;
  const padBottom = 35;

  const plotW = width - padLeft - padRight;
  const plotH = height - padTop - padBottom;

  const { minT, maxT, minY, maxY } = useMemo(() => {
    if (!time.length) return { minT: 0, maxT: 1, minY: -1, maxY: 1 };
    const t0 = time[0];
    const t1 = time[time.length - 1] || 1;

    let y0 = Infinity;
    let y1 = -Infinity;

    for (const name of activeSignals) {
      const arr = signals[name] || [];
      for (let i = 0; i < arr.length; i++) {
        if (arr[i] < y0) y0 = arr[i];
        if (arr[i] > y1) y1 = arr[i];
      }
    }

    if (!Number.isFinite(y0) || !Number.isFinite(y1)) {
      y0 = -1;
      y1 = 1;
    }
    if (y0 === y1) {
      y0 -= 1;
      y1 += 1;
    }

    // Add 8% padding to Y range
    const yPad = (y1 - y0) * 0.08;
    return { minT: t0, maxT: t1, minY: y0 - yPad, maxY: y1 + yPad };
  }, [time, signals, activeSignals]);

  const mapX = (t: number) => padLeft + ((t - minT) / (maxT - minT || 1)) * plotW;
  const mapY = (v: number) => padTop + plotH - ((v - minY) / (maxY - minY || 1)) * plotH;

  // Grid tick marks
  const yTicks = useMemo(() => {
    const count = 5;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minY + (i / count) * (maxY - minY);
      ticks.push({ val, y: mapY(val) });
    }
    return ticks;
  }, [minY, maxY, plotH, padTop]);

  const xTicks = useMemo(() => {
    const count = 6;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minT + (i / count) * (maxT - minT);
      ticks.push({ val, x: mapX(val) });
    }
    return ticks;
  }, [minT, maxT, plotW, padLeft]);

  // Generate SVG path for each active trace
  const tracePaths = useMemo(() => {
    return activeSignals.map((name) => {
      const arr = signals[name] || [];
      const len = Math.min(time.length, arr.length);
      if (len === 0) return { name, path: '' };

      // Subsample if there are more than 2000 points to keep rendering fast
      const step = Math.max(1, Math.floor(len / 2000));
      let d = `M ${mapX(time[0])} ${mapY(arr[0])}`;
      for (let i = step; i < len; i += step) {
        d += ` L ${mapX(time[i])} ${mapY(arr[i])}`;
      }
      if ((len - 1) % step !== 0) {
        d += ` L ${mapX(time[len - 1])} ${mapY(arr[len - 1])}`;
      }
      return { name, path: d };
    });
  }, [activeSignals, time, signals, minT, maxT, minY, maxY]);

  const handleMouseMove = (e: ReactMouseEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / rect.width) * width;
    if (svgX < padLeft || svgX > width - padRight) {
      onHoverIndex(null);
      return;
    }

    const t = minT + ((svgX - padLeft) / plotW) * (maxT - minT);
    // Find closest index in time array
    let low = 0;
    let high = time.length - 1;
    while (low < high) {
      const mid = Math.floor((low + high) / 2);
      if (time[mid] < t) low = mid + 1;
      else high = mid;
    }
    onHoverIndex(low);
  };

  return (
    <div className="waveform-container" ref={containerRef}>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
      >
        {/* Background */}
        <rect width={width} height={height} className="waveform-bg" rx={6} />

        {/* Plot area background */}
        <rect
          x={padLeft}
          y={padTop}
          width={plotW}
          height={plotH}
          className="waveform-plot-area"
          rx={3}
        />

        {/* Y Gridlines */}
        {yTicks.map(({ val, y }, i) => (
          <g key={i}>
            <line
              x1={padLeft}
              y1={y}
              x2={width - padRight}
              y2={y}
              stroke="#334155"
              strokeDasharray="2 3"
            />
            <text
              x={padLeft - 8}
              y={y + 3}
              fill="#94a3b8"
              fontSize={10}
              textAnchor="end"
            >
              {formatEngineeringValue(val)}
            </text>
          </g>
        ))}

        {/* X Gridlines */}
        {xTicks.map(({ val, x }, i) => (
          <g key={i}>
            <line
              x1={x}
              y1={padTop}
              x2={x}
              y2={height - padBottom}
              stroke="#334155"
              strokeDasharray="2 3"
            />
            <text
              x={x}
              y={height - padBottom + 16}
              fill="#94a3b8"
              fontSize={10}
              textAnchor="middle"
            >
              {formatEngineeringValue(val, 's')}
            </text>
          </g>
        ))}

        {/* Traces */}
        {tracePaths.map(({ name, path }) => {
          const colorIdx = allSignals.indexOf(name);
          const color = TRACE_COLORS[colorIdx % TRACE_COLORS.length];
          return (
            <path
              key={name}
              d={path}
              fill="none"
              stroke={color}
              strokeWidth={1.8}
              strokeLinejoin="round"
            />
          );
        })}

        {/* Hover Crosshair & Values */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <g className="hover-crosshair" pointerEvents="none">
            {/* Vertical crosshair line */}
            <line
              x1={mapX(time[hoverIndex])}
              y1={padTop}
              x2={mapX(time[hoverIndex])}
              y2={height - padBottom}
              stroke="#cbd5e1"
              strokeWidth={1}
              strokeDasharray="3 3"
            />

            {/* Trace intersection dots */}
            {activeSignals.map((name) => {
              const val = signals[name]?.[hoverIndex];
              if (val === undefined) return null;
              const colorIdx = allSignals.indexOf(name);
              const color = TRACE_COLORS[colorIdx % TRACE_COLORS.length];
              return (
                <circle
                  key={name}
                  cx={mapX(time[hoverIndex])}
                  cy={mapY(val)}
                  r={4}
                  fill={color}
                  stroke="#ffffff"
                  strokeWidth={1.5}
                />
              );
            })}
          </g>
        )}
      </svg>

      {/* Floating Tooltip displaying exact values at hover */}
      {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
        <div className="waveform-tooltip">
          <div className="tooltip-time">
            t = {formatEngineeringValue(time[hoverIndex], 's')}
          </div>
          {activeSignals.map((name) => {
            const val = signals[name]?.[hoverIndex];
            if (val === undefined) return null;
            const colorIdx = allSignals.indexOf(name);
            const color = TRACE_COLORS[colorIdx % TRACE_COLORS.length];
            return (
              <div key={name} className="tooltip-signal-row">
                <span className="tooltip-dot" style={{ backgroundColor: color }} />
                <span className="tooltip-name">{name}:</span>
                <span className="tooltip-val">{formatEngineeringValue(val)}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
