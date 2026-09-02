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
import type { EditorComponent, SimulationDefaults, SimulationStatus } from '../model/types';
import {
  parseEngineeringValue,
  formatEngineeringValue,
} from '../model/componentSchema';
import { mapSimulationResults } from './chartData';
import { estimateStepCount, STEP_WARNING_THRESHOLD } from './simSteps';

interface SimulationDrawerProps {
  isOpen: boolean;
  onToggle: () => void;
  circuitId: string | null;
  components?: EditorComponent[];
  defaults: SimulationDefaults | null;
  onRunSimulation: (config: {
    simulationTime: number;
    timeStep: number;
    solverType: string;
    backend?: string;
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
  components,
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
  const [backend, setBackend] = useState('headless');
  const [recordedSignals, setRecordedSignals] = useState<string[]>([]);
  const [hiddenSignals, setHiddenSignals] = useState<Record<string, boolean>>({});
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const [seededCircuit, setSeededCircuit] = useState<string | null>(null);

  const [selectedScope, setSelectedScope] = useState<string>('all');
  const [displayLayout, setDisplayLayout] = useState<'overlay' | 'stacked'>('overlay');
  const [channelSearch, setChannelSearch] = useState<string>('');

  const scopeBlocks = useMemo(() => {
    if (!components) return [];
    return components.filter(
      (c) =>
        c.type === 5 ||
        c.type === 1003 ||
        c.name.toUpperCase().startsWith('SCOPE') ||
        c.name.toUpperCase().startsWith('OSZI'),
    );
  }, [components]);

  // Re-seed the parameter inputs whenever another circuit is opened
  useEffect(() => {
    if (!defaults || seededCircuit === circuitId) return;
    setSeededCircuit(circuitId);
    if (defaults.duration > 0) setTEndStr(formatEngineeringValue(defaults.duration));
    if (defaults.timeStep > 0) setDtStr(formatEngineeringValue(defaults.timeStep));
    if (defaults.solverType) setSolverType(defaults.solverType);
    setRecordedSignals(defaults.signals);
    setSelectedScope(scopeBlocks.length > 0 ? scopeBlocks[0].name : 'all');
    setDisplayLayout('overlay');
    setChannelSearch('');
  }, [circuitId, defaults, seededCircuit, scopeBlocks]);

  const isRunning =
    status === 'PENDING' || status === 'RUNNING' || status === 'PAUSED';

  const parsedTEnd = parseEngineeringValue(tEndStr) ?? defaults?.duration ?? 0.02;
  const parsedDt = parseEngineeringValue(dtStr) ?? defaults?.timeStep ?? 1e-6;
  const stepCount = estimateStepCount(parsedTEnd, parsedDt);

  const handleRun = () => {
    if (!circuitId) return;
    const tEnd = parsedTEnd;
    const dt = parsedDt;
    onRunSimulation({
      simulationTime: tEnd,
      timeStep: dt,
      solverType,
      backend,
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

  // Active channels associated with currently selected Scope (or all signals)
  const activeScopeChannels = useMemo(() => {
    if (selectedScope === 'all') {
      return signalNames;
    }
    const matchingScope = scopeBlocks.find((sb) => sb.name === selectedScope);
    if (matchingScope) {
      const channels = matchingScope.inputLabels.filter((l) => l && signalNames.includes(l));
      if (channels.length > 0) return channels;
    }
    return signalNames;
  }, [selectedScope, scopeBlocks, signalNames]);

  // Filtered by search if provided
  const filteredChannels = useMemo(() => {
    if (!channelSearch.trim()) return activeScopeChannels;
    const q = channelSearch.trim().toLowerCase();
    return activeScopeChannels.filter((s) => s.toLowerCase().includes(q));
  }, [activeScopeChannels, channelSearch]);

  // Visible (unhidden) signals sent to the plot
  const visibleSignals = useMemo(() => {
    return filteredChannels.filter((s) => !hiddenSignals[s]);
  }, [filteredChannels, hiddenSignals]);

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

          <div
            className="sim-param-item"
            title={
              backend === 'legacy'
                ? 'Runs the ORIGINAL uploaded file in the classic engine (RMI). Unsaved web edits are not included.'
                : 'Runs the built-in headless engine on the current circuit state.'
            }
          >
            <label htmlFor="sim-backend-select">Engine:</label>
            <select
              id="sim-backend-select"
              className="sim-param-select"
              value={backend}
              onChange={(e) => setBackend(e.target.value)}
              disabled={isRunning}
            >
              <option value="headless">Headless (native)</option>
              <option value="legacy">Classic (exact parity)</option>
            </select>
          </div>

          <div className="sim-param-item steps-info" title={`${parsedTEnd}s / ${parsedDt}s`}>
            {stepCount > STEP_WARNING_THRESHOLD ? (
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
                {/* Scope Instrument & Layout Controls Toolbar */}
                <div className="scope-toolbar">
                  {/* Scope Selector */}
                  <div className="scope-selector-group">
                    <span className="scope-group-label">Scope:</span>
                    {scopeBlocks.length > 3 ? (
                      <select
                        className="scope-select"
                        value={selectedScope}
                        onChange={(e) => setSelectedScope(e.target.value)}
                        title="Select Scope Instrument"
                      >
                        {scopeBlocks.map((sb) => {
                          const channels = sb.inputLabels.filter(Boolean);
                          return (
                            <option key={sb.name} value={sb.name}>
                              📺 {sb.name} ({channels.length} ch: {channels.join(', ')})
                            </option>
                          );
                        })}
                        <option value="all">🌐 All Scopes ({signalNames.length} signals)</option>
                      </select>
                    ) : (
                      <div className="scope-pills">
                        {scopeBlocks.map((sb) => {
                          const isActive = selectedScope === sb.name;
                          const channels = sb.inputLabels.filter(Boolean);
                          return (
                            <button
                              key={sb.name}
                              type="button"
                              className={`scope-pill-btn ${isActive ? 'active' : ''}`}
                              onClick={() => setSelectedScope(sb.name)}
                              title={`Scope ${sb.name} (${channels.join(', ')})`}
                            >
                              📺 {sb.name} <span className="scope-ch-count">{channels.length}</span>
                            </button>
                          );
                        })}
                        <button
                          type="button"
                          className={`scope-pill-btn ${selectedScope === 'all' ? 'active' : ''}`}
                          onClick={() => setSelectedScope('all')}
                          title="All scope signals combined"
                        >
                          🌐 All Scopes <span className="scope-ch-count">{signalNames.length}</span>
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Display Layout Switcher */}
                  <div className="scope-layout-group">
                    <div className="segmented-control">
                      <button
                        type="button"
                        className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
                        onClick={() => setDisplayLayout('overlay')}
                        title="Overlay signals on a unified plot"
                      >
                        📈 Overlay
                      </button>
                      <button
                        type="button"
                        className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
                        onClick={() => setDisplayLayout('stacked')}
                        title={`Stack channels in separate subplots (${activeScopeChannels.length} lanes)`}
                      >
                        📑 Stacked ({activeScopeChannels.length})
                      </button>
                    </div>
                  </div>

                  {/* Search/Filter for large signal counts (> 6) */}
                  {activeScopeChannels.length > 6 && (
                    <div className="scope-search-group">
                      <input
                        type="text"
                        className="scope-search-input"
                        placeholder="Filter channels..."
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

                {/* Active Scope Channels Legend Bar */}
                <div className="sim-legend-bar">
                  <span className="legend-channels-label">Channels:</span>
                  {filteredChannels.map((name) => {
                    const colorIdx = signalNames.indexOf(name);
                    const color = TRACE_COLORS[colorIdx >= 0 ? colorIdx % TRACE_COLORS.length : 0];
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
                          backgroundColor: isHidden ? 'transparent' : `${color}22`,
                        }}
                        title={isHidden ? `Show ${name}` : `Hide ${name}`}
                      >
                        <span className="legend-dot" style={{ backgroundColor: color }} />
                        {name}
                      </button>
                    );
                  })}
                  {filteredChannels.length === 0 && (
                    <span className="legend-none-msg">No channels match filter.</span>
                  )}
                </div>

                {/* SVG Chart: Render Stacked or Standard Waveform Chart */}
                {displayLayout === 'stacked' ? (
                  <StackedWaveformChart
                    time={timeArray}
                    signals={results}
                    activeSignals={visibleSignals}
                    allSignals={signalNames}
                    hoverIndex={hoverIndex}
                    onHoverIndex={setHoverIndex}
                  />
                ) : (
                  <WaveformChart
                    time={timeArray}
                    signals={results}
                    activeSignals={visibleSignals}
                    allSignals={signalNames}
                    hoverIndex={hoverIndex}
                    onHoverIndex={setHoverIndex}
                  />
                )}
              </div>

              {/* Right: Metrics / Statistics Table */}
              <div className="sim-stats-card">
                <div className="stats-header-bar">
                  <span className="stats-header">
                    {selectedScope === 'all' ? 'All Scope Signals' : `${selectedScope} Statistics`}
                  </span>
                  {selectedScope !== 'all' ? (
                    <button
                      type="button"
                      className="stats-filter-toggle"
                      onClick={() => setSelectedScope('all')}
                      title="View statistics for all signals"
                    >
                      View All ({signalNames.length})
                    </button>
                  ) : (
                    scopeBlocks.length > 0 && (
                      <button
                        type="button"
                        className="stats-filter-toggle"
                        onClick={() => setSelectedScope(scopeBlocks[0].name)}
                        title={`Filter to ${scopeBlocks[0].name}`}
                      >
                        Scope Filter
                      </button>
                    )
                  )}
                </div>
                <div className="stats-table-wrap">
                  <table className="stats-table">
                    <thead>
                      <tr>
                        <th>Scope / Signal</th>
                        <th>Min</th>
                        <th>Max</th>
                        <th>Pk-Pk</th>
                        <th>RMS</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredChannels.map((name) => {
                        const st = signalStats[name];
                        if (!st) return null;
                        const colorIdx = signalNames.indexOf(name);
                        const color = TRACE_COLORS[colorIdx >= 0 ? colorIdx % TRACE_COLORS.length : 0];
                        const isHidden = !!hiddenSignals[name];
                        return (
                          <tr
                            key={name}
                            onClick={() => toggleSignal(name)}
                            style={{
                              cursor: 'pointer',
                              opacity: isHidden ? 0.4 : 1,
                            }}
                            title="Click to toggle signal visibility"
                          >
                            <td style={{ color, fontWeight: 600 }}>
                              <span style={{ marginRight: 4 }}>📺</span>
                              {name}
                            </td>
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
 * Stacked Subplots Waveform Chart:
 * Renders each active scope signal in its own distinct horizontal graph lane
 * with individual Y-scaling, label, and synchronized time-axis crosshair.
 */
function StackedWaveformChart({
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
  const laneH = 75;
  const laneGap = 12;
  const padLeft = 60;
  const padRight = 20;
  const padTop = 15;
  const padBottom = 25;
  const totalH = Math.max(260, padTop + padBottom + activeSignals.length * (laneH + laneGap) - laneGap);
  const plotW = width - padLeft - padRight;

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

  return (
    <div className="waveform-container" ref={containerRef}>
      <svg
        viewBox={`0 0 ${width} ${totalH}`}
        className="waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
      >
        <rect width={width} height={totalH} className="waveform-bg" rx={6} />

        {activeSignals.map((name, k) => {
          const arr = signals[name] || [];
          const colorIdx = allSignals.indexOf(name);
          const color = TRACE_COLORS[colorIdx % TRACE_COLORS.length];
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

          // Compute trace path
          const len = Math.min(time.length, arr.length);
          let pathD = '';
          if (len > 0) {
            const step = Math.max(1, Math.floor(len / 2000));
            pathD = `M ${mapX(time[0])} ${mapLaneY(arr[0])}`;
            for (let i = step; i < len; i += step) {
              pathD += ` L ${mapX(time[i])} ${mapLaneY(arr[i])}`;
            }
            if ((len - 1) % step !== 0) {
              pathD += ` L ${mapX(time[len - 1])} ${mapLaneY(arr[len - 1])}`;
            }
          }

          // Zero line if 0 is in range
          const hasZero = minY <= 0 && maxY >= 0;
          const zeroY = hasZero ? mapLaneY(0) : null;

          return (
            <g key={name} className="stacked-lane">
              {/* Lane background */}
              <rect
                x={padLeft}
                y={laneTop}
                width={plotW}
                height={laneH}
                className="waveform-plot-area"
                rx={3}
              />

              {/* Zero reference line */}
              {zeroY !== null && (
                <line
                  x1={padLeft}
                  y1={zeroY}
                  x2={width - padRight}
                  y2={zeroY}
                  stroke="#475569"
                  strokeDasharray="3 3"
                />
              )}

              {/* Min & Max Y-ticks */}
              <text
                x={padLeft - 6}
                y={laneTop + 10}
                fill="#94a3b8"
                fontSize={9}
                textAnchor="end"
              >
                {formatEngineeringValue(maxY)}
              </text>
              <text
                x={padLeft - 6}
                y={laneTop + laneH - 2}
                fill="#94a3b8"
                fontSize={9}
                textAnchor="end"
              >
                {formatEngineeringValue(minY)}
              </text>

              {/* Trace Path */}
              {pathD && (
                <path
                  d={pathD}
                  fill="none"
                  stroke={color}
                  strokeWidth={1.8}
                  strokeLinejoin="round"
                />
              )}

              {/* Signal Badge in top-left */}
              <rect
                x={padLeft + 6}
                y={laneTop + 4}
                width={name.length * 7 + 18}
                height={16}
                rx={3}
                fill="#0f172a"
                fillOpacity={0.85}
              />
              <circle
                cx={padLeft + 12}
                cy={laneTop + 12}
                r={3}
                fill={color}
              />
              <text
                x={padLeft + 18}
                y={laneTop + 15}
                fill={color}
                fontSize={10}
                fontWeight={700}
              >
                {name}
              </text>

              {/* Intersection dot on hover */}
              {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
                <circle
                  cx={mapX(time[hoverIndex])}
                  cy={mapLaneY(arr[hoverIndex] ?? 0)}
                  r={3.5}
                  fill={color}
                  stroke="#ffffff"
                  strokeWidth={1.2}
                />
              )}
            </g>
          );
        })}

        {/* Global time axis at the bottom */}
        <line
          x1={padLeft}
          y1={totalH - padBottom}
          x2={width - padRight}
          y2={totalH - padBottom}
          stroke="#475569"
        />
        <text
          x={padLeft}
          y={totalH - padBottom + 16}
          fill="#94a3b8"
          fontSize={10}
          textAnchor="middle"
        >
          {formatEngineeringValue(t0, 's')}
        </text>
        <text
          x={width - padRight}
          y={totalH - padBottom + 16}
          fill="#94a3b8"
          fontSize={10}
          textAnchor="middle"
        >
          {formatEngineeringValue(t1, 's')}
        </text>

        {/* Crosshair line spanning full stacked height */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <line
            x1={mapX(time[hoverIndex])}
            y1={padTop}
            x2={mapX(time[hoverIndex])}
            y2={totalH - padBottom}
            stroke="#cbd5e1"
            strokeWidth={1}
            strokeDasharray="3 3"
            pointerEvents="none"
          />
        )}
      </svg>

      {/* Floating Tooltip */}
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
