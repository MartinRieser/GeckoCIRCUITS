/**
 * Full-Viewport Scope Instrument & Simulation View Tab.
 * Provides high-resolution waveform plotting, simulation configuration,
 * cursor measurements, stacked / overlay modes, channel toggles, and signal metrics.
 * Fully styled for both Dark and Light themes.
 */
import { useState, useMemo, useEffect, useRef, useCallback } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import type { EditorComponent } from '../model/types';
import { formatEngineeringValue } from '../model/componentSchema';
import { mapSimulationResults } from './chartData';
import { findScopeBlocks, scopeChannels, filterChannels } from './scopes';
import {
  effectiveWindow,
  panWindow,
  zoomWindow,
  type ViewWindow,
} from './viewWindow';
import { FftPanel } from './FftPanel';

interface ScopeViewTabProps {
  selectedScope: string; // 'all' or 'SCOPE.1', 'SCOPE.2', etc.
  components: EditorComponent[];
  results: Record<string, number[]> | null;
  displayLayout: 'overlay' | 'stacked';
  theme?: 'dark' | 'light';
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

/** Binary-searches the sample index closest to time t (time must be ascending). */
function sampleIndexAt(time: number[], t: number): number {
  let low = 0;
  let high = time.length - 1;
  while (low < high) {
    const mid = Math.floor((low + high) / 2);
    if (time[mid] < t) low = mid + 1;
    else high = mid;
  }
  return low;
}

/** Cursor placement cycle: first click sets A, second B, further clicks move A. */
function nextCursorSlot(cursorA: number | null, cursorB: number | null): 'A' | 'B' {
  return cursorA === null ? 'A' : cursorB === null ? 'B' : 'A';
}

/** Floating hover tooltip shared by the overlay and stacked charts. */
function WaveformTooltip({
  time,
  hoverIndex,
  activeSignals,
  signals,
  allSignals,
  traceColors,
  hint,
}: {
  time: number[];
  hoverIndex: number;
  activeSignals: string[];
  signals: Record<string, number[]>;
  allSignals: string[];
  traceColors: string[];
  hint?: string;
}) {
  return (
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
      {hint && <div className="tooltip-hint">{hint}</div>}
    </div>
  );
}

export function ScopeViewTab({
  selectedScope,
  components,
  results,
  displayLayout,
  theme = 'dark',
}: ScopeViewTabProps) {
  const [hiddenSignals, setHiddenSignals] = useState<Record<string, boolean>>({});
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const [cursorA, setCursorA] = useState<number | null>(null);
  const [cursorB, setCursorB] = useState<number | null>(null);
  const [channelSearch, setChannelSearch] = useState('');
  // time-axis view window; null = fit whole simulation
  const [view, setView] = useState<ViewWindow | null>(null);
  const [fftOpen, setFftOpen] = useState(false);

  const traceColors = theme === 'light' ? TRACE_COLORS_LIGHT : TRACE_COLORS_DARK;

  const scopeBlocks = useMemo(() => findScopeBlocks(components), [components]);

  const activeScopeBlock = useMemo(() => {
    if (selectedScope === 'all') return null;
    return scopeBlocks.find((sb) => sb.name === selectedScope) || null;
  }, [scopeBlocks, selectedScope]);

  const { signalNames, timeArray, signalStats } = useMemo(
    () => mapSimulationResults(results),
    [results],
  );

  // a new simulation resets the zoom window
  useEffect(() => {
    setView(null);
  }, [results]);

  const dataT0 = timeArray.length ? timeArray[0] : 0;
  const dataT1 = timeArray.length ? timeArray[timeArray.length - 1] : 1;
  const win = effectiveWindow(view, dataT0, dataT1);

  const zoomAt = useCallback(
    (factor: number, anchor: number) => {
      setView(zoomWindow(effectiveWindow(view, dataT0, dataT1), factor, anchor, dataT0, dataT1));
    },
    [view, dataT0, dataT1],
  );

  const panBy = useCallback(
    (fraction: number) => {
      const span = win.end - win.start;
      setView(panWindow(win, fraction * span, dataT0, dataT1));
    },
    [win, dataT0, dataT1],
  );

  const resetZoom = useCallback(() => setView(null), []);

  // Channels that belong to this Scope
  const scopeChannelNames = useMemo(
    () => scopeChannels(activeScopeBlock, signalNames),
    [selectedScope, activeScopeBlock, signalNames],
  );

  const filteredChannels = useMemo(
    () => filterChannels(scopeChannelNames, channelSearch),
    [scopeChannelNames, channelSearch],
  );

  const visibleSignals = useMemo(() => {
    return filteredChannels.filter((s) => !hiddenSignals[s]);
  }, [filteredChannels, hiddenSignals]);

  const toggleSignal = (name: string) => {
    setHiddenSignals((prev) => ({ ...prev, [name]: !prev[name] }));
  };

  return (
    <div className="scope-view-tab-container">
      {/* Scope Header Bar */}
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
              {scopeChannelNames.length} channel{scopeChannelNames.length === 1 ? '' : 's'}:{' '}
              {scopeChannelNames.join(', ') || 'No signals registered'}
            </div>
          </div>
        </div>
      </div>

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

        {scopeChannelNames.length > 6 && (
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
            <p>Configure parameters in the <strong>Simulation Settings</strong> panel on the right and click <strong>"▶ Run Simulation"</strong> to calculate waveforms.</p>
          </div>
        ) : (
          <div className="scope-tab-main-grid">
            {/* Plot Area */}
            <div className="scope-tab-plot-area">
              <div className="scope-zoom-toolbar" data-testid="scope-zoom-toolbar">
                <span className="scope-window-label" data-testid="scope-window-label">
                  {formatEngineeringValue(win.start, 's')} – {formatEngineeringValue(win.end, 's')}
                </span>
                <button type="button" aria-label="Zoom in" title="Zoom in" onClick={() => zoomAt(0.7, (win.start + win.end) / 2)}>+</button>
                <button type="button" aria-label="Zoom out" title="Zoom out" onClick={() => zoomAt(1 / 0.7, (win.start + win.end) / 2)}>−</button>
                <button type="button" aria-label="Pan left" title="Pan left" onClick={() => panBy(-0.25)}>◀</button>
                <button type="button" aria-label="Pan right" title="Pan right" onClick={() => panBy(0.25)}>▶</button>
                <button type="button" aria-label="Fit whole simulation" title="Fit" onClick={resetZoom}>⟲</button>
                <button
                  type="button"
                  aria-label="Toggle FFT panel"
                  title="FFT spectrum of the visible window"
                  onClick={() => setFftOpen((prev) => !prev)}
                >
                  FFT
                </button>
              </div>
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
                  viewStart={win.start}
                  viewEnd={win.end}
                  onZoomAt={zoomAt}
                  onPanSeconds={(delta) => setView(panWindow(win, delta, dataT0, dataT1))}
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
                  viewStart={win.start}
                  viewEnd={win.end}
                  onZoomAt={zoomAt}
                  onPanSeconds={(delta) => setView(panWindow(win, delta, dataT0, dataT1))}
                />
              )}

              {fftOpen && visibleSignals.length > 0 && (
                <FftPanel
                  time={timeArray}
                  signals={results}
                  activeSignals={visibleSignals}
                  viewStart={win.start}
                  viewEnd={win.end}
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
  viewStart,
  viewEnd,
  onZoomAt,
  onPanSeconds,
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
  viewStart: number;
  viewEnd: number;
  onZoomAt: (factor: number, anchor: number) => void;
  onPanSeconds: (delta: number) => void;
}) {
  const width = 1000;
  const height = 480;
  const padLeft = 70;
  const padRight = 30;
  const padTop = 25;
  const padBottom = 45;

  const plotW = width - padLeft - padRight;
  const plotH = height - padTop - padBottom;

  const svgRef = useRef<SVGSVGElement | null>(null);
  const panState = useRef<{ x0: number; t0: number; moved: boolean } | null>(null);
  const timeRef = useRef(time);
  timeRef.current = time;

  const traceColors = theme === 'light' ? TRACE_COLORS_LIGHT : TRACE_COLORS_DARK;
  const gridColor = theme === 'light' ? '#e2e8f0' : '#334155';
  const textColor = theme === 'light' ? '#64748b' : '#94a3b8';
  const zeroColor = theme === 'light' ? '#94a3b8' : '#64748b';
  const crosshairColor = theme === 'light' ? '#334155' : '#cbd5e1';

  const minT = viewStart;
  const maxT = viewEnd;

  // Y-fit over the VISIBLE slice, so zooming rescales the value axis
  const { minY, maxY } = useMemo(() => {
    let y0 = Infinity;
    let y1 = -Infinity;
    const i0 = sampleIndexAt(time, minT);
    const i1 = sampleIndexAt(time, maxT);
    for (const name of activeSignals) {
      const arr = signals[name] || [];
      for (let i = i0; i <= i1 && i < arr.length; i++) {
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
    return { minY: y0 - yPad, maxY: y1 + yPad };
  }, [time, signals, activeSignals, minT, maxT]);

  // non-passive wheel zoom around the cursor position
  useEffect(() => {
    const svg = svgRef.current;
    if (!svg) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const rect = svg.getBoundingClientRect();
      const svgX = ((e.clientX - rect.left) / rect.width) * width;
      const anchor = minT + ((svgX - padLeft) / plotW) * (maxT - minT);
      onZoomAt(Math.exp(e.deltaY / 500), anchor);
    };
    svg.addEventListener('wheel', onWheel, { passive: false });
    return () => svg.removeEventListener('wheel', onWheel);
  }, [minT, maxT, onZoomAt]);

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

  // trace decimation limited to the visible slice
  const tracePaths = useMemo(() => {
    const i0 = sampleIndexAt(time, minT);
    const i1 = sampleIndexAt(time, maxT);
    return activeSignals.map((name) => {
      const arr = signals[name] || [];
      const iEnd = Math.min(i1, arr.length - 1);
      const len = iEnd - i0 + 1;
      if (len <= 0) return { name, path: '' };

      const step = Math.max(1, Math.floor(len / 3000));
      let d = `M ${mapX(time[i0])} ${mapY(arr[i0])}`;
      for (let i = i0 + step; i <= iEnd; i += step) {
        d += ` L ${mapX(time[i])} ${mapY(arr[i])}`;
      }
      if ((iEnd - i0) % step !== 0) {
        d += ` L ${mapX(time[iEnd])} ${mapY(arr[iEnd])}`;
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
    onHoverIndex(sampleIndexAt(time, t));
  };

  const handleClick = () => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    if (hoverIndex === null) return;
    onSetCursor(nextCursorSlot(cursorA, cursorB), hoverIndex);
  };

  // drag-pan: pointerdown starts a candidate pan; movement beyond 4px pans and
  // cancels the pending cursor click
  const suppressClickRef = useRef(false);
  const handlePointerDown = (e: ReactMouseEvent<SVGSVGElement>) => {
    if (e.button !== 0) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const x0 = e.clientX;
    const span = maxT - minT;
    const svgPerScreen = width / (rect.width || 1);
    panState.current = { x0: x0, t0: 0, moved: false };
    suppressClickRef.current = false;
    const onMove = (ev: PointerEvent) => {
      const st = panState.current;
      if (!st) return;
      const dx = ev.clientX - st.x0;
      if (!st.moved && Math.abs(dx) > 4) {
        st.moved = true;
        suppressClickRef.current = true;
      }
      if (st.moved) {
        // content follows the cursor: drag right moves the window left
        onPanSeconds(-(dx * svgPerScreen * span) / plotW);
        st.x0 = ev.clientX;
      }
    };
    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      panState.current = null;
    };
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  return (
    <div className="full-waveform-wrap">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${width} ${height}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
        onPointerDown={handlePointerDown}
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

        <defs>
          <clipPath id="overlay-plot-clip">
            <rect x={padLeft} y={padTop} width={plotW} height={plotH} />
          </clipPath>
        </defs>

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

        {/* Waveform Traces (clipped to the visible window) */}
        <g clipPath="url(#overlay-plot-clip)">
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
        </g>

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
        <WaveformTooltip
          time={time}
          hoverIndex={hoverIndex}
          activeSignals={activeSignals}
          signals={signals}
          allSignals={allSignals}
          traceColors={traceColors}
          hint="Click to place measurement cursor"
        />
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
  viewStart,
  viewEnd,
  onZoomAt,
  onPanSeconds,
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
  viewStart: number;
  viewEnd: number;
  onZoomAt: (factor: number, anchor: number) => void;
  onPanSeconds: (delta: number) => void;
}) {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const panState = useRef<{ x0: number; moved: boolean } | null>(null);
  const suppressClickRef = useRef(false);
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

  const t0 = viewStart;
  const t1 = viewEnd;

  const mapX = (t: number) => padLeft + ((t - t0) / (t1 - t0 || 1)) * plotW;

  // non-passive wheel zoom around the cursor position
  useEffect(() => {
    const svg = svgRef.current;
    if (!svg) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const rect = svg.getBoundingClientRect();
      const svgX = ((e.clientX - rect.left) / rect.width) * width;
      const anchor = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
      onZoomAt(Math.exp(e.deltaY / 500), anchor);
    };
    svg.addEventListener('wheel', onWheel, { passive: false });
    return () => svg.removeEventListener('wheel', onWheel);
  }, [t0, t1, onZoomAt]);

  const handleMouseMove = (e: ReactMouseEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / rect.width) * width;
    if (svgX < padLeft || svgX > width - padRight) {
      onHoverIndex(null);
      return;
    }

    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    onHoverIndex(sampleIndexAt(time, t));
  };

  const handleClick = () => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    if (hoverIndex === null) return;
    onSetCursor(nextCursorSlot(cursorA, cursorB), hoverIndex);
  };

  // drag-pan, same interaction as the overlay chart
  const handlePointerDown = (e: ReactMouseEvent<SVGSVGElement>) => {
    if (e.button !== 0) return;
    const x0 = e.clientX;
    const rect = e.currentTarget.getBoundingClientRect();
    const span = t1 - t0;
    const svgPerScreen = width / (rect.width || 1);
    panState.current = { x0: x0, moved: false };
    suppressClickRef.current = false;
    const onMove = (ev: PointerEvent) => {
      const st = panState.current;
      if (!st) return;
      const dx = ev.clientX - st.x0;
      if (!st.moved && Math.abs(dx) > 4) {
        st.moved = true;
        suppressClickRef.current = true;
      }
      if (st.moved) {
        onPanSeconds(-(dx * svgPerScreen * span) / plotW);
        st.x0 = ev.clientX;
      }
    };
    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      panState.current = null;
    };
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  return (
    <div className="full-waveform-wrap">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${width} ${totalH}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
        onPointerDown={handlePointerDown}
      >
        <rect width={width} height={totalH} className="waveform-bg" rx={8} />

        <defs>
          <clipPath id="stacked-plot-clip">
            <rect x={padLeft} y={padTop} width={plotW} height={totalH - padTop - padBottom} />
          </clipPath>
        </defs>
        <g clipPath="url(#stacked-plot-clip)">
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

          const iWin0 = sampleIndexAt(time, t0);
          const iWin1 = Math.min(sampleIndexAt(time, t1), arr.length - 1);
          const len = iWin1 - iWin0 + 1;
          let pathD = '';
          if (len > 0) {
            const step = Math.max(1, Math.floor(len / 3000));
            pathD = `M ${mapX(time[iWin0])} ${mapLaneY(arr[iWin0])}`;
            for (let i = iWin0 + step; i <= iWin1; i += step) {
              pathD += ` L ${mapX(time[i])} ${mapLaneY(arr[i])}`;
            }
            if ((iWin1 - iWin0) % step !== 0) {
              pathD += ` L ${mapX(time[iWin1])} ${mapLaneY(arr[iWin1])}`;
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

        </g>

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
        <WaveformTooltip
          time={time}
          hoverIndex={hoverIndex}
          activeSignals={activeSignals}
          signals={signals}
          allSignals={allSignals}
          traceColors={traceColors}
        />
      )}
    </div>
  );
}
