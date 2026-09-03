/**
 * Full-Viewport Scope Instrument & Simulation View Tab.
 * Provides high-resolution waveform plotting, simulation configuration,
 * cursor measurements, stacked / overlay modes, channel toggles, and signal metrics.
 * Fully styled for both Dark and Light themes.
 */
import { useState, useMemo, useRef, useEffect } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import type { EditorComponent, SimulationDefaults, SimulationStatus } from '../model/types';
import {
  parseEngineeringValue,
  formatEngineeringValue,
} from '../model/componentSchema';
import { mapSimulationResults } from './chartData';

interface ScopeViewTabProps {
  selectedScope: string; // 'all' or 'SCOPE.1', 'SCOPE.2', etc.
  onSelectScope: (scope: string) => void;
  components: EditorComponent[];
  results: Record<string, number[]> | null;
  status: SimulationStatus | null;
  progress: number;
  circuitId: string | null;
  defaults?: SimulationDefaults | null;
  errorMessage?: string | null;
  theme?: 'dark' | 'light';
  onRunSimulation: (config?: {
    simulationTime: number;
    timeStep: number;
    solverType: string;
    backend?: string;
    signals?: string[];
  }) => void;
  onPauseSimulation?: () => void;
  onResumeSimulation?: () => void;
  onCancelSimulation?: () => void;
}

const TRACE_COLORS_DARK = [
  '#38bdf8', // Sky blue
  '#f43f5e', // Rose
  '#10b981', // Emerald
  '#fbbf24', // Amber
  '#a855f7', // Purple
  '#3b82f6', // Blue
  '#ec4899', // Pink
  '#14b8a6', // Teal
];

const TRACE_COLORS_LIGHT = [
  '#0284c7', // Vibrant Sky blue
  '#e11d48', // Vibrant Rose/red
  '#16a34a', // Vibrant Emerald/green
  '#d97706', // Vibrant Amber/orange
  '#9333ea', // Vibrant Purple
  '#2563eb', // Vibrant Blue
  '#db2777', // Vibrant Pink
  '#0d9488', // Vibrant Teal
];

export function ScopeViewTab({
  selectedScope,
  onSelectScope,
  components,
  results,
  status,
  progress,
  circuitId,
  defaults,
  errorMessage,
  theme = 'dark',
  onRunSimulation,
  onPauseSimulation,
  onResumeSimulation,
  onCancelSimulation,
}: ScopeViewTabProps) {
  const [displayLayout, setDisplayLayout] = useState<'overlay' | 'stacked'>('overlay');
  const [hiddenSignals, setHiddenSignals] = useState<Record<string, boolean>>({});
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const [cursorA, setCursorA] = useState<number | null>(null);
  const [cursorB, setCursorB] = useState<number | null>(null);
  const [channelSearch, setChannelSearch] = useState('');

  // Simulation Parameters state
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

  const traceColors = theme === 'light' ? TRACE_COLORS_LIGHT : TRACE_COLORS_DARK;

  const scopeBlocks = useMemo(() => {
    return components.filter(
      (c) =>
        c.type === 5 ||
        c.type === 1003 ||
        c.name.toUpperCase().startsWith('SCOPE') ||
        c.name.toUpperCase().startsWith('OSZI'),
    );
  }, [components]);

  const activeScopeBlock = useMemo(() => {
    if (selectedScope === 'all') return null;
    return scopeBlocks.find((sb) => sb.name === selectedScope) || null;
  }, [scopeBlocks, selectedScope]);

  const { signalNames, timeArray, signalStats } = useMemo(
    () => mapSimulationResults(results),
    [results],
  );

  // Channels that belong to this Scope
  const scopeChannels = useMemo(() => {
    if (selectedScope === 'all' || !activeScopeBlock) {
      return signalNames;
    }
    const channels = activeScopeBlock.inputLabels.filter((l) => l && signalNames.includes(l));
    return channels.length > 0 ? channels : signalNames;
  }, [selectedScope, activeScopeBlock, signalNames]);

  const filteredChannels = useMemo(() => {
    if (!channelSearch.trim()) return scopeChannels;
    const q = channelSearch.trim().toLowerCase();
    return scopeChannels.filter((s) => s.toLowerCase().includes(q));
  }, [scopeChannels, channelSearch]);

  const visibleSignals = useMemo(() => {
    return filteredChannels.filter((s) => !hiddenSignals[s]);
  }, [filteredChannels, hiddenSignals]);

  const toggleSignal = (name: string) => {
    setHiddenSignals((prev) => ({ ...prev, [name]: !prev[name] }));
  };

  const isRunning = status === 'PENDING' || status === 'RUNNING';
  const isPaused = status === 'PAUSED';

  const tEndNum = parseEngineeringValue(tEndStr);
  const dtNum = parseEngineeringValue(dtStr);

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
    if (!results || !timeArray.length) return;
    const exportSignals = visibleSignals.length > 0 ? visibleSignals : signalNames;
    const headers = ['time', ...exportSignals];
    const rows = [headers.join(',')];

    for (let i = 0; i < timeArray.length; i++) {
      const row = [timeArray[i], ...exportSignals.map((s) => results[s]?.[i] ?? 0)];
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
    <div className="scope-view-tab-container">
      {/* Scope Header Toolbar */}
      <div className="scope-tab-header">
        <div className="scope-tab-title-group">
          <div className="scope-badge-icon">
            {selectedScope === 'all' ? '📊' : '📺'}
          </div>
          <div>
            <div className="scope-tab-title">
              {selectedScope === 'all' ? 'Simulation Overview (All Scopes & Signals)' : `Scope Instrument: ${selectedScope}`}
            </div>
            <div className="scope-tab-subtitle">
              {scopeChannels.length} channel{scopeChannels.length === 1 ? '' : 's'}:{' '}
              {scopeChannels.join(', ') || 'No signals registered'}
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div className="scope-tab-actions">
          {/* Quick Scope Switcher Pills / Dropdown */}
          <div className="scope-pills" style={{ display: 'flex', gap: 4, marginRight: 8 }}>
            <button
              type="button"
              className={`scope-pill-btn ${selectedScope === 'all' ? 'active' : ''}`}
              onClick={() => onSelectScope('all')}
              title="Show all recorded signals"
            >
              🌐 All Signals ({signalNames.length})
            </button>
            {scopeBlocks.map((sb) => {
              const chCount = sb.inputLabels.filter(Boolean).length;
              return (
                <button
                  key={sb.name}
                  type="button"
                  className={`scope-pill-btn ${selectedScope === sb.name ? 'active' : ''}`}
                  onClick={() => onSelectScope(sb.name)}
                  title={`Focus on ${sb.name}`}
                >
                  📺 {sb.name} ({chCount})
                </button>
              );
            })}
          </div>

          {/* Display Mode Toggle */}
          <div className="segmented-control">
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
              onClick={() => setDisplayLayout('overlay')}
              title="Overlay all channels on a single large graph"
            >
              📈 Overlay
            </button>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
              onClick={() => setDisplayLayout('stacked')}
              title={`Stack each channel in its own subplot lane (${scopeChannels.length} lanes)`}
            >
              📑 Stacked Lanes ({scopeChannels.length})
            </button>
          </div>

          {/* Run / Pause / Cancel Controls */}
          {!isRunning && !isPaused && (
            <button
              type="button"
              className="sim-btn run"
              onClick={handleRun}
              disabled={!circuitId}
              title="Run simulation"
            >
              ▶ Run Simulation
            </button>
          )}

          {isRunning && (
            <>
              {onPauseSimulation && (
                <button
                  type="button"
                  className="sim-btn pause"
                  onClick={onPauseSimulation}
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
                  title="Cancel simulation"
                >
                  ⏹ Cancel
                </button>
              )}
            </>
          )}

          {isPaused && (
            <>
              {onResumeSimulation && (
                <button
                  type="button"
                  className="sim-btn resume"
                  onClick={onResumeSimulation}
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
                  title="Cancel simulation"
                >
                  ⏹ Cancel
                </button>
              )}
            </>
          )}

          {results && signalNames.length > 0 && (
            <button
              type="button"
              className="sim-btn export"
              onClick={handleExportCsv}
              title="Export visible channels to CSV"
            >
              Export CSV
            </button>
          )}
        </div>
      </div>

      {/* Progress track */}
      {isRunning && (
        <div className="sim-progress-track">
          <div
            className="sim-progress-bar"
            style={{ width: `${Math.max(5, Math.min(100, progress * 100))}%` }}
          />
        </div>
      )}

      {/* Error message if any */}
      {errorMessage && (
        <div className="sim-error-banner">
          <span className="error-icon">✕</span>
          <span className="error-text">{errorMessage}</span>
        </div>
      )}

      {/* Channel Pills Legend */}
      <div className="scope-tab-legend-bar">
        <span className="legend-channels-label">Active Traces:</span>
        {filteredChannels.map((name) => {
          const colorIdx = signalNames.indexOf(name);
          const color = traceColors[colorIdx >= 0 ? colorIdx % traceColors.length : 0];
          const isHidden = !!hiddenSignals[name];
          return (
            <button
              key={name}
              type="button"
              className={`legend-pill ${isHidden ? 'hidden' : 'active'}`}
              onClick={() => toggleSignal(name)}
              style={{
                borderColor: color,
                color: isHidden ? 'var(--text-dim)' : 'var(--text)',
                backgroundColor: isHidden ? 'transparent' : `${color}22`,
              }}
              title={isHidden ? `Show ${name}` : `Hide ${name}`}
            >
              <span className="legend-dot" style={{ backgroundColor: color }} />
              {name}
            </button>
          );
        })}

        {scopeChannels.length > 6 && (
          <div className="scope-search-group" style={{ marginLeft: 'auto' }}>
            <input
              type="text"
              className="scope-search-input"
              placeholder="Search traces..."
              value={channelSearch}
              onChange={(e) => setChannelSearch(e.target.value)}
            />
            {channelSearch && (
              <button
                type="button"
                className="scope-search-clear"
                onClick={() => setChannelSearch('')}
              >
                ×
              </button>
            )}
          </div>
        )}
      </div>

      {/* Main Workspace Body */}
      <div className="scope-tab-body">
        {!results || signalNames.length === 0 ? (
          <div className="scope-empty-state">
            <div className="empty-icon">{selectedScope === 'all' ? '📊' : '📺'}</div>
            <h3>No simulation data available</h3>
            <p>Configure parameters above and click <strong>"Run Simulation"</strong> to calculate waveforms.</p>
            <button
              type="button"
              className="sim-btn run"
              onClick={handleRun}
              disabled={!circuitId || isRunning}
              style={{ marginTop: 12 }}
            >
              ▶ Run Simulation
            </button>
          </div>
        ) : (
          <div className="scope-tab-main-grid">
            {/* Plot Area */}
            <div className="scope-tab-plot-area">
              {displayLayout === 'stacked' ? (
                <FullScreenStackedChart
                  time={timeArray}
                  signals={results}
                  activeSignals={visibleSignals}
                  allSignals={signalNames}
                  theme={theme}
                  hoverIndex={hoverIndex}
                  onHoverIndex={setHoverIndex}
                  cursorA={cursorA}
                  cursorB={cursorB}
                  onSetCursor={(type, idx) => {
                    if (type === 'A') setCursorA(idx);
                    else setCursorB(idx);
                  }}
                />
              ) : (
                <FullScreenOverlayChart
                  time={timeArray}
                  signals={results}
                  activeSignals={visibleSignals}
                  allSignals={signalNames}
                  theme={theme}
                  hoverIndex={hoverIndex}
                  onHoverIndex={setHoverIndex}
                  cursorA={cursorA}
                  cursorB={cursorB}
                  onSetCursor={(type, idx) => {
                    if (type === 'A') setCursorA(idx);
                    else setCursorB(idx);
                  }}
                />
              )}
            </div>

            {/* Bottom Channel Metrics & Cursor Delta Measurements */}
            <div className="scope-tab-metrics-panel">
              {/* Cursor Measurement Delta */}
              {cursorA !== null && cursorB !== null && timeArray[cursorA] !== undefined && timeArray[cursorB] !== undefined && (
                <div className="cursor-delta-card">
                  <div className="cursor-delta-title">📐 Cursor Measurement (A → B)</div>
                  <div className="cursor-delta-grid">
                    <div className="delta-item">
                      <span className="delta-label">t_A:</span>
                      <span className="delta-val">{formatEngineeringValue(timeArray[cursorA], 's')}</span>
                    </div>
                    <div className="delta-item">
                      <span className="delta-label">t_B:</span>
                      <span className="delta-val">{formatEngineeringValue(timeArray[cursorB], 's')}</span>
                    </div>
                    <div className="delta-item highlight">
                      <span className="delta-label">Δt:</span>
                      <span className="delta-val">{formatEngineeringValue(Math.abs(timeArray[cursorB] - timeArray[cursorA]), 's')}</span>
                    </div>
                    <div className="delta-item highlight">
                      <span className="delta-label">Freq (1/Δt):</span>
                      <span className="delta-val">
                        {Math.abs(timeArray[cursorB] - timeArray[cursorA]) > 0
                          ? formatEngineeringValue(1 / Math.abs(timeArray[cursorB] - timeArray[cursorA]), 'Hz')
                          : '—'}
                      </span>
                    </div>
                    <button
                      type="button"
                      className="delta-clear-btn"
                      onClick={() => {
                        setCursorA(null);
                        setCursorB(null);
                      }}
                    >
                      Clear Cursors
                    </button>
                  </div>
                </div>
              )}

              {/* Statistics Table */}
              <div className="scope-table-card">
                <table className="scope-metrics-table">
                  <thead>
                    <tr>
                      <th>Signal Trace</th>
                      <th>Minimum</th>
                      <th>Maximum</th>
                      <th>Peak-to-Peak</th>
                      <th>RMS</th>
                      <th>Mean (Avg)</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredChannels.map((name) => {
                      const st = signalStats[name];
                      if (!st) return null;
                      const colorIdx = signalNames.indexOf(name);
                      const color = traceColors[colorIdx >= 0 ? colorIdx % traceColors.length : 0];
                      const isHidden = !!hiddenSignals[name];
                      const arr = results[name] || [];
                      const mean = arr.length > 0 ? arr.reduce((a, b) => a + b, 0) / arr.length : 0;
                      return (
                        <tr
                          key={name}
                          onClick={() => toggleSignal(name)}
                          className={isHidden ? 'row-hidden' : ''}
                          title="Click row to toggle trace visibility"
                        >
                          <td style={{ color, fontWeight: 700 }}>
                            <span className="legend-dot" style={{ backgroundColor: color, marginRight: 6 }} />
                            {name}
                          </td>
                          <td>{formatEngineeringValue(st.min)}</td>
                          <td>{formatEngineeringValue(st.max)}</td>
                          <td>{formatEngineeringValue(st.pkpk)}</td>
                          <td>{formatEngineeringValue(st.rms)}</td>
                          <td>{formatEngineeringValue(mean)}</td>
                          <td>
                            <span className={`channel-status-pill ${isHidden ? 'off' : 'on'}`}>
                              {isHidden ? 'Hidden' : 'Active'}
                            </span>
                          </td>
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
    </div>
  );
}

/** Full-Screen Overlay Waveform Chart */
function FullScreenOverlayChart({
  time,
  signals,
  activeSignals,
  allSignals,
  theme,
  hoverIndex,
  onHoverIndex,
  cursorA,
  cursorB,
  onSetCursor,
}: {
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  allSignals: string[];
  theme: 'dark' | 'light';
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
  cursorA: number | null;
  cursorB: number | null;
  onSetCursor: (type: 'A' | 'B', idx: number) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const width = 1000;
  const height = 480;
  const padLeft = 70;
  const padRight = 30;
  const padTop = 25;
  const padBottom = 45;

  const plotW = width - padLeft - padRight;
  const plotH = height - padTop - padBottom;

  const traceColors = theme === 'light' ? TRACE_COLORS_LIGHT : TRACE_COLORS_DARK;
  const gridColor = theme === 'light' ? '#e2e8f0' : '#334155';
  const textColor = theme === 'light' ? '#64748b' : '#94a3b8';
  const zeroColor = theme === 'light' ? '#94a3b8' : '#64748b';
  const crosshairColor = theme === 'light' ? '#334155' : '#cbd5e1';

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

    const yPad = (y1 - y0) * 0.08;
    return { minT: t0, maxT: t1, minY: y0 - yPad, maxY: y1 + yPad };
  }, [time, signals, activeSignals]);

  const mapX = (t: number) => padLeft + ((t - minT) / (maxT - minT || 1)) * plotW;
  const mapY = (v: number) => padTop + plotH - ((v - minY) / (maxY - minY || 1)) * plotH;

  const yTicks = useMemo(() => {
    const count = 7;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minY + (i / count) * (maxY - minY);
      ticks.push({ val, y: mapY(val) });
    }
    return ticks;
  }, [minY, maxY, plotH, padTop]);

  const xTicks = useMemo(() => {
    const count = 9;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minT + (i / count) * (maxT - minT);
      ticks.push({ val, x: mapX(val) });
    }
    return ticks;
  }, [minT, maxT, plotW, padLeft]);

  const tracePaths = useMemo(() => {
    return activeSignals.map((name) => {
      const arr = signals[name] || [];
      const len = Math.min(time.length, arr.length);
      if (len === 0) return { name, path: '' };

      const step = Math.max(1, Math.floor(len / 3000));
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
    let low = 0;
    let high = time.length - 1;
    while (low < high) {
      const mid = Math.floor((low + high) / 2);
      if (time[mid] < t) low = mid + 1;
      else high = mid;
    }
    onHoverIndex(low);
  };

  const handleClick = () => {
    if (hoverIndex === null) return;
    if (cursorA === null) {
      onSetCursor('A', hoverIndex);
    } else if (cursorB === null) {
      onSetCursor('B', hoverIndex);
    } else {
      onSetCursor('A', hoverIndex);
    }
  };

  return (
    <div className="full-waveform-wrap" ref={containerRef}>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
      >
        <rect width={width} height={height} className="waveform-bg" rx={8} />
        <rect
          x={padLeft}
          y={padTop}
          width={plotW}
          height={plotH}
          className="waveform-plot-area"
          rx={4}
        />

        {/* Y Gridlines */}
        {yTicks.map(({ val, y }, i) => (
          <g key={i}>
            <line
              x1={padLeft}
              y1={y}
              x2={width - padRight}
              y2={y}
              stroke={gridColor}
              strokeDasharray="3 4"
            />
            <text
              x={padLeft - 10}
              y={y + 4}
              fill={textColor}
              fontSize={11}
              fontWeight={600}
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
              stroke={gridColor}
              strokeDasharray="3 4"
            />
            <text
              x={x}
              y={height - padBottom + 20}
              fill={textColor}
              fontSize={11}
              fontWeight={600}
              textAnchor="middle"
            >
              {formatEngineeringValue(val, 's')}
            </text>
          </g>
        ))}

        {/* Zero Line */}
        {minY <= 0 && maxY >= 0 && (
          <line
            x1={padLeft}
            y1={mapY(0)}
            x2={width - padRight}
            y2={mapY(0)}
            stroke={zeroColor}
            strokeWidth={1.2}
          />
        )}

        {/* Waveform Traces */}
        {tracePaths.map(({ name, path }) => {
          const colorIdx = allSignals.indexOf(name);
          const color = traceColors[colorIdx % traceColors.length];
          return (
            <path
              key={name}
              d={path}
              fill="none"
              stroke={color}
              strokeWidth={2.2}
              strokeLinejoin="round"
            />
          );
        })}

        {/* Cursor A */}
        {cursorA !== null && time[cursorA] !== undefined && (
          <g className="cursor-line-a" pointerEvents="none">
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={height - padBottom}
              stroke="#38bdf8"
              strokeWidth={1.5}
            />
            <rect
              x={mapX(time[cursorA]) - 14}
              y={padTop - 18}
              width={28}
              height={16}
              rx={3}
              fill="#0284c7"
            />
            <text
              x={mapX(time[cursorA])}
              y={padTop - 6}
              fill="#ffffff"
              fontSize={10}
              fontWeight={800}
              textAnchor="middle"
            >
              A
            </text>
          </g>
        )}

        {/* Cursor B */}
        {cursorB !== null && time[cursorB] !== undefined && (
          <g className="cursor-line-b" pointerEvents="none">
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={height - padBottom}
              stroke="#f43f5e"
              strokeWidth={1.5}
            />
            <rect
              x={mapX(time[cursorB]) - 14}
              y={padTop - 18}
              width={28}
              height={16}
              rx={3}
              fill="#e11d48"
            />
            <text
              x={mapX(time[cursorB])}
              y={padTop - 6}
              fill="#ffffff"
              fontSize={10}
              fontWeight={800}
              textAnchor="middle"
            >
              B
            </text>
          </g>
        )}

        {/* Hover Crosshair & Values */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <g className="hover-crosshair" pointerEvents="none">
            <line
              x1={mapX(time[hoverIndex])}
              y1={padTop}
              x2={mapX(time[hoverIndex])}
              y2={height - padBottom}
              stroke={crosshairColor}
              strokeWidth={1}
              strokeDasharray="3 3"
            />
            {activeSignals.map((name) => {
              const val = signals[name]?.[hoverIndex];
              if (val === undefined) return null;
              const colorIdx = allSignals.indexOf(name);
              const color = traceColors[colorIdx % traceColors.length];
              return (
                <circle
                  key={name}
                  cx={mapX(time[hoverIndex])}
                  cy={mapY(val)}
                  r={4.5}
                  fill={color}
                  stroke="#ffffff"
                  strokeWidth={1.5}
                />
              );
            })}
          </g>
        )}
      </svg>

      {/* Floating Hover Tooltip */}
      {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
        <div className="waveform-tooltip">
          <div className="tooltip-time">
            t = {formatEngineeringValue(time[hoverIndex], 's')}
          </div>
          {activeSignals.map((name) => {
            const val = signals[name]?.[hoverIndex];
            if (val === undefined) return null;
            const colorIdx = allSignals.indexOf(name);
            const color = traceColors[colorIdx % traceColors.length];
            return (
              <div key={name} className="tooltip-signal-row">
                <span className="tooltip-dot" style={{ backgroundColor: color }} />
                <span className="tooltip-name">{name}:</span>
                <span className="tooltip-val">{formatEngineeringValue(val)}</span>
              </div>
            );
          })}
          <div className="tooltip-hint">Click to place measurement cursor</div>
        </div>
      )}
    </div>
  );
}

/** Full-Screen Stacked Subplots Waveform Chart */
function FullScreenStackedChart({
  time,
  signals,
  activeSignals,
  allSignals,
  theme,
  hoverIndex,
  onHoverIndex,
  cursorA,
  cursorB,
  onSetCursor,
}: {
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  allSignals: string[];
  theme: 'dark' | 'light';
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
  cursorA: number | null;
  cursorB: number | null;
  onSetCursor: (type: 'A' | 'B', idx: number) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const width = 1000;
  const laneH = 120; // spacious 120px height per channel!
  const laneGap = 16;
  const padLeft = 70;
  const padRight = 30;
  const padTop = 25;
  const padBottom = 35;
  const totalH = Math.max(480, padTop + padBottom + activeSignals.length * (laneH + laneGap) - laneGap);
  const plotW = width - padLeft - padRight;

  const traceColors = theme === 'light' ? TRACE_COLORS_LIGHT : TRACE_COLORS_DARK;
  const textColor = theme === 'light' ? '#64748b' : '#94a3b8';
  const zeroColor = theme === 'light' ? '#cbd5e1' : '#475569';
  const crosshairColor = theme === 'light' ? '#334155' : '#cbd5e1';

  const t0 = time[0] || 0;
  const t1 = time[time.length - 1] || 1;

  const mapX = (t: number) => padLeft + ((t - t0) / (t1 - t0 || 1)) * plotW;

  const handleMouseMove = (e: ReactMouseEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / rect.width) * width;
    if (svgX < padLeft || svgX > width - padRight) {
      onHoverIndex(null);
      return;
    }

    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    let low = 0;
    let high = time.length - 1;
    while (low < high) {
      const mid = Math.floor((low + high) / 2);
      if (time[mid] < t) low = mid + 1;
      else high = mid;
    }
    onHoverIndex(low);
  };

  const handleClick = () => {
    if (hoverIndex === null) return;
    if (cursorA === null) {
      onSetCursor('A', hoverIndex);
    } else if (cursorB === null) {
      onSetCursor('B', hoverIndex);
    } else {
      onSetCursor('A', hoverIndex);
    }
  };

  return (
    <div className="full-waveform-wrap" ref={containerRef}>
      <svg
        viewBox={`0 0 ${width} ${totalH}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
      >
        <rect width={width} height={totalH} className="waveform-bg" rx={8} />

        {activeSignals.map((name, k) => {
          const arr = signals[name] || [];
          const colorIdx = allSignals.indexOf(name);
          const color = traceColors[colorIdx % traceColors.length];
          const laneTop = padTop + k * (laneH + laneGap);

          let y0 = Infinity;
          let y1 = -Infinity;
          for (let i = 0; i < arr.length; i++) {
            if (arr[i] < y0) y0 = arr[i];
            if (arr[i] > y1) y1 = arr[i];
          }
          if (!Number.isFinite(y0) || !Number.isFinite(y1)) {
            y0 = -1;
            y1 = 1;
          }
          if (y0 === y1) {
            y0 -= 1;
            y1 += 1;
          }
          const yPad = (y1 - y0) * 0.1;
          const minY = y0 - yPad;
          const maxY = y1 + yPad;

          const mapLaneY = (v: number) =>
            laneTop + laneH - ((v - minY) / (maxY - minY || 1)) * laneH;

          const len = Math.min(time.length, arr.length);
          let pathD = '';
          if (len > 0) {
            const step = Math.max(1, Math.floor(len / 3000));
            pathD = `M ${mapX(time[0])} ${mapLaneY(arr[0])}`;
            for (let i = step; i < len; i += step) {
              pathD += ` L ${mapX(time[i])} ${mapLaneY(arr[i])}`;
            }
            if ((len - 1) % step !== 0) {
              pathD += ` L ${mapX(time[len - 1])} ${mapLaneY(arr[len - 1])}`;
            }
          }

          const hasZero = minY <= 0 && maxY >= 0;
          const zeroY = hasZero ? mapLaneY(0) : null;

          return (
            <g key={name} className="stacked-lane">
              <rect
                x={padLeft}
                y={laneTop}
                width={plotW}
                height={laneH}
                className="waveform-plot-area"
                rx={4}
              />

              {zeroY !== null && (
                <line
                  x1={padLeft}
                  y1={zeroY}
                  x2={width - padRight}
                  y2={zeroY}
                  stroke={zeroColor}
                  strokeDasharray="3 3"
                />
              )}

              {/* Y Ticks */}
              <text
                x={padLeft - 8}
                y={laneTop + 14}
                fill={textColor}
                fontSize={10}
                fontWeight={600}
                textAnchor="end"
              >
                {formatEngineeringValue(maxY)}
              </text>
              <text
                x={padLeft - 8}
                y={laneTop + laneH - 4}
                fill={textColor}
                fontSize={10}
                fontWeight={600}
                textAnchor="end"
              >
                {formatEngineeringValue(minY)}
              </text>

              {/* Path */}
              {pathD && (
                <path
                  d={pathD}
                  fill="none"
                  stroke={color}
                  strokeWidth={2.2}
                  strokeLinejoin="round"
                />
              )}

              {/* Lane Badge */}
              <rect
                x={padLeft + 8}
                y={laneTop + 6}
                width={name.length * 8 + 24}
                height={20}
                rx={4}
                fill={theme === 'light' ? '#f1f5f9' : '#0f172a'}
                stroke={theme === 'light' ? '#cbd5e1' : '#334155'}
                fillOpacity={0.9}
              />
              <circle
                cx={padLeft + 16}
                cy={laneTop + 16}
                r={4}
                fill={color}
              />
              <text
                x={padLeft + 24}
                y={laneTop + 20}
                fill={color}
                fontSize={11}
                fontWeight={700}
              >
                {name}
              </text>

              {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
                <circle
                  cx={mapX(time[hoverIndex])}
                  cy={mapLaneY(arr[hoverIndex] ?? 0)}
                  r={4.5}
                  fill={color}
                  stroke="#ffffff"
                  strokeWidth={1.5}
                />
              )}
            </g>
          );
        })}

        {/* Global time axis */}
        <line
          x1={padLeft}
          y1={totalH - padBottom}
          x2={width - padRight}
          y2={totalH - padBottom}
          stroke={theme === 'light' ? '#cbd5e1' : '#475569'}
        />
        <text
          x={padLeft}
          y={totalH - padBottom + 18}
          fill={textColor}
          fontSize={11}
          fontWeight={600}
          textAnchor="middle"
        >
          {formatEngineeringValue(t0, 's')}
        </text>
        <text
          x={width - padRight}
          y={totalH - padBottom + 18}
          fill={textColor}
          fontSize={11}
          fontWeight={600}
          textAnchor="middle"
        >
          {formatEngineeringValue(t1, 's')}
        </text>

        {/* Crosshair Line */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <line
            x1={mapX(time[hoverIndex])}
            y1={padTop}
            x2={mapX(time[hoverIndex])}
            y2={totalH - padBottom}
            stroke={crosshairColor}
            strokeWidth={1}
            strokeDasharray="3 3"
            pointerEvents="none"
          />
        )}
      </svg>

      {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
        <div className="waveform-tooltip">
          <div className="tooltip-time">
            t = {formatEngineeringValue(time[hoverIndex], 's')}
          </div>
          {activeSignals.map((name) => {
            const val = signals[name]?.[hoverIndex];
            if (val === undefined) return null;
            const colorIdx = allSignals.indexOf(name);
            const color = traceColors[colorIdx % traceColors.length];
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
