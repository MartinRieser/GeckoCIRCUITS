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
  clampWindow,
  MIN_SPAN,
  type ViewWindow,
} from './viewWindow';
import { FftPanel } from './FftPanel';
import { LossPanel } from './LossPanel';

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
  const [lossOpen, setLossOpen] = useState(false);

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

  const zoomRange = useCallback(
    (start: number, end: number) => {
      const s = Math.min(start, end);
      const e = Math.max(start, end);
      if (e - s < MIN_SPAN) return;
      setView(clampWindow({ start: s, end: e }, dataT0, dataT1));
    },
    [dataT0, dataT1],
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
                <button
                  type="button"
                  aria-label="Toggle loss panel"
                  title="Semiconductor loss calculator"
                  onClick={() => setLossOpen((prev) => !prev)}
                >
                  Losses
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
                  onZoomRange={zoomRange}
                  onResetZoom={resetZoom}
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
                  onZoomRange={zoomRange}
                  onResetZoom={resetZoom}
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

              {lossOpen && <LossPanel />}
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
                  <div className="cursor-delta-hint" style={{ marginTop: '8px', fontSize: '11px', color: 'var(--text-muted)' }}>
                    💡 <strong>Tip:</strong> Click plot to move Cursor A • <strong>Shift+Click</strong> or <strong>Right-Click</strong> to move Cursor B • Drag tabs (A / B) to slide
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
  onZoomRange,
  onResetZoom,
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
  onZoomRange: (start: number, end: number) => void;
  onResetZoom: () => void;
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

  const handleCursorDragStart = (e: React.PointerEvent, type: 'A' | 'B') => {
    e.stopPropagation();
    e.preventDefault();
    const svg = svgRef.current;
    if (!svg) return;
    const rect = svg.getBoundingClientRect();

    const onMove = (ev: PointerEvent) => {
      const svgX = ((ev.clientX - rect.left) / rect.width) * width;
      const clampedX = Math.max(padLeft, Math.min(width - padRight, svgX));
      const t = minT + ((clampedX - padLeft) / plotW) * (maxT - minT);
      const newIdx = sampleIndexAt(time, t);
      onSetCursor(type, newIdx);
    };

    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  const handleClick = (e: ReactMouseEvent<SVGSVGElement>) => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    if (hoverIndex === null) return;
    const targetCursor: 'A' | 'B' = e.shiftKey ? 'B' : nextCursorSlot(cursorA, cursorB);
    onSetCursor(targetCursor, hoverIndex);
  };

  const handleContextMenu = (e: ReactMouseEvent<SVGSVGElement>) => {
    e.preventDefault();
    if (hoverIndex === null) return;
    onSetCursor('B', hoverIndex);
  };

  const [dragBox, setDragBox] = useState<{ startX: number; currentX: number } | null>(null);
  const suppressClickRef = useRef(false);

  const handlePointerDown = (e: ReactMouseEvent<SVGSVGElement>) => {
    // Middle click or Alt + Left click -> Pan
    if (e.button === 1 || (e.button === 0 && e.altKey)) {
      e.preventDefault();
      const rect = e.currentTarget.getBoundingClientRect();
      let lastX = e.clientX;
      const span = maxT - minT;
      const svgPerScreen = width / (rect.width || 1);
      suppressClickRef.current = true;

      const onMove = (ev: PointerEvent) => {
        const dx = ev.clientX - lastX;
        lastX = ev.clientX;
        onPanSeconds(-(dx * svgPerScreen * span) / plotW);
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      return;
    }

    // Left click -> Box Zoom or Click
    if (e.button === 0) {
      const svg = svgRef.current;
      if (!svg) return;
      const rect = svg.getBoundingClientRect();
      const getSvgX = (clientX: number) => {
        const sx = ((clientX - rect.left) / (rect.width || 1)) * width;
        return Math.max(padLeft, Math.min(width - padRight, sx));
      };

      const startSvgX = getSvgX(e.clientX);
      let currentSvgX = startSvgX;
      suppressClickRef.current = false;

      const onMove = (ev: PointerEvent) => {
        currentSvgX = getSvgX(ev.clientX);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          suppressClickRef.current = true;
          setDragBox({ startX: startSvgX, currentX: currentSvgX });
        }
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          const xA = Math.min(startSvgX, currentSvgX);
          const xB = Math.max(startSvgX, currentSvgX);
          const tA = minT + ((xA - padLeft) / plotW) * (maxT - minT);
          const tB = minT + ((xB - padLeft) / plotW) * (maxT - minT);
          onZoomRange(tA, tB);
        }
        setDragBox(null);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
    }
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
        onDoubleClick={onResetZoom}
        onContextMenu={handleContextMenu}
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
              x={padLeft - 8}
              y={y + 4}
              textAnchor="end"
              fill={textColor}
              fontSize={11}
              fontFamily="monospace"
            >
              {formatEngineeringValue(val, '')}
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
          <g className="cursor-line-a">
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={height - padBottom}
              stroke="#38bdf8"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={height - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            />
            <g
              className="cursor-handle cursor-handle-a"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            >
              <rect
                x={mapX(time[cursorA]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#0284c7"
              />
              <text
                x={mapX(time[cursorA])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                A
              </text>
            </g>
          </g>
        )}

        {/* Cursor B */}
        {cursorB !== null && time[cursorB] !== undefined && (
          <g className="cursor-line-b">
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={height - padBottom}
              stroke="#f43f5e"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={height - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            />
            <g
              className="cursor-handle cursor-handle-b"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            >
              <rect
                x={mapX(time[cursorB]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#e11d48"
              />
              <text
                x={mapX(time[cursorB])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                B
              </text>
            </g>
          </g>
        )}

        {/* Drag Box Zoom Selection */}
        {dragBox && Math.abs(dragBox.currentX - dragBox.startX) > 5 && (
          <g pointerEvents="none">
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop}
              width={Math.abs(dragBox.currentX - dragBox.startX)}
              height={plotH}
              fill="rgba(56, 189, 248, 0.22)"
              stroke="#38bdf8"
              strokeWidth={1.5}
              strokeDasharray="4 3"
            />
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop + 4}
              width={Math.min(140, Math.abs(dragBox.currentX - dragBox.startX))}
              height={18}
              rx={3}
              fill="#0284c7"
              opacity={0.9}
            />
            <text
              x={Math.min(dragBox.startX, dragBox.currentX) + 6}
              y={padTop + 16}
              fill="#ffffff"
              fontSize={10}
              fontWeight={700}
            >
              🔍 Drag to Zoom
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
            <rect
              x={Math.max(padLeft, Math.min(width - padRight - 60, mapX(time[hoverIndex]) - 30))}
              y={height - padBottom + 4}
              width={60}
              height={18}
              rx={3}
              className="hover-pill"
            />
            <text
              x={Math.max(padLeft + 30, Math.min(width - padRight - 30, mapX(time[hoverIndex])))}
              y={height - padBottom + 17}
              textAnchor="middle"
              className="hover-text"
            >
              {formatEngineeringValue(time[hoverIndex], 's')}
            </text>
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
          hint={
            cursorA === null
              ? 'Click: place Cursor A • Shift+Click or Right-Click: place B'
              : cursorB === null
              ? 'Click: place Cursor B • Shift+Click or Right-Click: place B'
              : 'Click: move A • Shift+Click / Right-Click: move B • Drag tabs to slide'
          }
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
  onZoomRange,
  onResetZoom,
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
  onZoomRange: (start: number, end: number) => void;
  onResetZoom: () => void;
  onPanSeconds: (delta: number) => void;
}) {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [dragBox, setDragBox] = useState<{ startX: number; currentX: number } | null>(null);
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
  const plotH = totalH - padTop - padBottom;

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

  const handleCursorDragStart = (e: React.PointerEvent, type: 'A' | 'B') => {
    e.stopPropagation();
    e.preventDefault();
    const svg = svgRef.current;
    if (!svg) return;
    const rect = svg.getBoundingClientRect();

    const onMove = (ev: PointerEvent) => {
      const svgX = ((ev.clientX - rect.left) / rect.width) * width;
      const clampedX = Math.max(padLeft, Math.min(width - padRight, svgX));
      const t = t0 + ((clampedX - padLeft) / plotW) * (t1 - t0);
      const newIdx = sampleIndexAt(time, t);
      onSetCursor(type, newIdx);
    };

    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  const handleClick = (e: ReactMouseEvent<SVGSVGElement>) => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    if (hoverIndex === null) return;
    const targetCursor: 'A' | 'B' = e.shiftKey ? 'B' : nextCursorSlot(cursorA, cursorB);
    onSetCursor(targetCursor, hoverIndex);
  };

  const handleContextMenu = (e: ReactMouseEvent<SVGSVGElement>) => {
    e.preventDefault();
    if (hoverIndex === null) return;
    onSetCursor('B', hoverIndex);
  };

  const handlePointerDown = (e: ReactMouseEvent<SVGSVGElement>) => {
    // Middle click or Alt + Left click -> Pan
    if (e.button === 1 || (e.button === 0 && e.altKey)) {
      e.preventDefault();
      const rect = e.currentTarget.getBoundingClientRect();
      let lastX = e.clientX;
      const span = t1 - t0;
      const svgPerScreen = width / (rect.width || 1);
      suppressClickRef.current = true;

      const onMove = (ev: PointerEvent) => {
        const dx = ev.clientX - lastX;
        lastX = ev.clientX;
        onPanSeconds(-(dx * svgPerScreen * span) / plotW);
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      return;
    }

    // Left click -> Box Zoom or Click
    if (e.button === 0) {
      const svg = svgRef.current;
      if (!svg) return;
      const rect = svg.getBoundingClientRect();
      const getSvgX = (clientX: number) => {
        const sx = ((clientX - rect.left) / (rect.width || 1)) * width;
        return Math.max(padLeft, Math.min(width - padRight, sx));
      };

      const startSvgX = getSvgX(e.clientX);
      let currentSvgX = startSvgX;
      suppressClickRef.current = false;

      const onMove = (ev: PointerEvent) => {
        currentSvgX = getSvgX(ev.clientX);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          suppressClickRef.current = true;
          setDragBox({ startX: startSvgX, currentX: currentSvgX });
        }
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          const xA = Math.min(startSvgX, currentSvgX);
          const xB = Math.max(startSvgX, currentSvgX);
          const tA = t0 + ((xA - padLeft) / plotW) * (t1 - t0);
          const tB = t0 + ((xB - padLeft) / plotW) * (t1 - t0);
          onZoomRange(tA, tB);
        }
        setDragBox(null);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
    }
  };

  const signalMinMax: Record<string, { min: number; max: number }> = {};
  const lanePaths: Record<string, string> = {};

  const iWin0 = sampleIndexAt(time, t0);
  const iWin1 = Math.min(sampleIndexAt(time, t1), time.length - 1);
  const winLen = Math.max(1, iWin1 - iWin0 + 1);
  const step = Math.max(1, Math.floor(winLen / 3000));

  for (let k = 0; k < activeSignals.length; k++) {
    const name = activeSignals[k];
    const arr = signals[name] || [];
    const laneY = padTop + k * (laneH + laneGap);

    let y0 = Infinity;
    let y1 = -Infinity;
    for (let i = iWin0; i <= iWin1; i++) {
      const v = arr[i];
      if (v !== undefined) {
        if (v < y0) y0 = v;
        if (v > y1) y1 = v;
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
    const yPad = (y1 - y0) * 0.1;
    const minY = y0 - yPad;
    const maxY = y1 + yPad;
    signalMinMax[name] = { min: minY, max: maxY };

    const mapLaneY = (v: number) =>
      laneY + laneH - ((v - minY) / (maxY - minY || 1)) * laneH;

    let pathD = '';
    if (winLen > 0 && arr.length > 0) {
      const startIdx = Math.max(0, Math.min(iWin0, arr.length - 1));
      const endIdx = Math.max(0, Math.min(iWin1, arr.length - 1));
      if (startIdx <= endIdx) {
        pathD = `M ${mapX(time[startIdx])} ${mapLaneY(arr[startIdx] ?? 0)}`;
        for (let i = startIdx + step; i <= endIdx; i += step) {
          pathD += ` L ${mapX(time[i])} ${mapLaneY(arr[i] ?? 0)}`;
        }
        if ((endIdx - startIdx) % step !== 0) {
          pathD += ` L ${mapX(time[endIdx])} ${mapLaneY(arr[endIdx] ?? 0)}`;
        }
      }
    }
    lanePaths[name] = pathD;
  }

  return (
    <div className="full-waveform-wrap">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${width} ${totalH}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
        onDoubleClick={onResetZoom}
        onContextMenu={handleContextMenu}
        onPointerDown={handlePointerDown}
      >
        <rect width={width} height={totalH} className="waveform-bg" rx={8} />

        <defs>
          <clipPath id="stacked-plot-clip">
            <rect x={padLeft} y={padTop} width={plotW} height={totalH - padTop - padBottom} />
          </clipPath>
        </defs>

        {/* Lanes */}
        {activeSignals.map((name, i) => {
          const laneY = padTop + i * (laneH + laneGap);
          const colorIdx = allSignals.indexOf(name);
          const color = traceColors[colorIdx % traceColors.length];
          const path = lanePaths[name];
          const mm = signalMinMax[name] || { min: 0, max: 1 };
          const zeroY =
            mm.min <= 0 && mm.max >= 0
              ? laneY + laneH - ((0 - mm.min) / (mm.max - mm.min || 1)) * laneH
              : null;

          return (
            <g key={name} className="stacked-lane">
              {/* Lane background */}
              <rect
                x={padLeft}
                y={laneY}
                width={plotW}
                height={laneH}
                className="waveform-plot-area"
                rx={4}
              />

              {/* Zero line */}
              {zeroY !== null && (
                <line
                  x1={padLeft}
                  y1={zeroY}
                  x2={width - padRight}
                  y2={zeroY}
                  stroke={zeroColor}
                  strokeWidth={1}
                />
              )}

              {/* Y Axis Labels (Min / Max) */}
              <text
                x={padLeft - 8}
                y={laneY + 12}
                textAnchor="end"
                fill={textColor}
                fontSize={10}
                fontFamily="monospace"
              >
                {formatEngineeringValue(mm.max, '')}
              </text>
              <text
                x={padLeft - 8}
                y={laneY + laneH - 2}
                textAnchor="end"
                fill={textColor}
                fontSize={10}
                fontFamily="monospace"
              >
                {formatEngineeringValue(mm.min, '')}
              </text>

              {/* Channel Name Badge */}
              <rect
                x={padLeft + 8}
                y={laneY + 8}
                width={Math.min(120, name.length * 8 + 16)}
                height={18}
                rx={3}
                fill={theme === 'light' ? 'rgba(255,255,255,0.85)' : 'rgba(15,23,42,0.85)'}
              />
              <text
                x={padLeft + 14}
                y={laneY + 21}
                fill={color}
                fontSize={11}
                fontWeight={700}
              >
                {name}
              </text>

              {/* Waveform */}
              <g clipPath="url(#stacked-plot-clip)">
                {path && (
                  <path
                    d={path}
                    fill="none"
                    stroke={color}
                    strokeWidth={2.0}
                    strokeLinejoin="round"
                  />
                )}
              </g>
            </g>
          );
        })}

        {/* Bottom Time Axis Labels */}
        <text
          x={padLeft}
          y={totalH - 12}
          textAnchor="start"
          fill={textColor}
          fontSize={11}
          fontFamily="monospace"
        >
          {formatEngineeringValue(t0, 's')}
        </text>
        <text
          x={padLeft + plotW / 2}
          y={totalH - 12}
          textAnchor="middle"
          fill={textColor}
          fontSize={11}
          fontFamily="monospace"
        >
          {formatEngineeringValue((t0 + t1) / 2, 's')}
        </text>
        <text
          x={width - padRight}
          y={totalH - 12}
          textAnchor="end"
          fill={textColor}
          fontSize={11}
          fontFamily="monospace"
        >
          {formatEngineeringValue(t1, 's')}
        </text>

        {/* Cursor A */}
        {cursorA !== null && time[cursorA] !== undefined && (
          <g className="cursor-line-a">
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={totalH - padBottom}
              stroke="#38bdf8"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={totalH - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            />
            <g
              className="cursor-handle cursor-handle-a"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            >
              <rect
                x={mapX(time[cursorA]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#0284c7"
              />
              <text
                x={mapX(time[cursorA])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                A
              </text>
            </g>
          </g>
        )}

        {/* Cursor B */}
        {cursorB !== null && time[cursorB] !== undefined && (
          <g className="cursor-line-b">
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={totalH - padBottom}
              stroke="#f43f5e"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={totalH - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            />
            <g
              className="cursor-handle cursor-handle-b"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            >
              <rect
                x={mapX(time[cursorB]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#e11d48"
              />
              <text
                x={mapX(time[cursorB])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                B
              </text>
            </g>
          </g>
        )}

        {/* Drag Box Zoom Selection */}
        {dragBox && Math.abs(dragBox.currentX - dragBox.startX) > 5 && (
          <g pointerEvents="none">
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop}
              width={Math.abs(dragBox.currentX - dragBox.startX)}
              height={plotH}
              fill="rgba(56, 189, 248, 0.22)"
              stroke="#38bdf8"
              strokeWidth={1.5}
              strokeDasharray="4 3"
            />
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop + 4}
              width={Math.min(140, Math.abs(dragBox.currentX - dragBox.startX))}
              height={18}
              rx={3}
              fill="#0284c7"
              opacity={0.9}
            />
            <text
              x={Math.min(dragBox.startX, dragBox.currentX) + 6}
              y={padTop + 16}
              fill="#ffffff"
              fontSize={10}
              fontWeight={700}
            >
              🔍 Drag to Zoom
            </text>
          </g>
        )}

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
          hint={
            cursorA === null
              ? 'Click: place Cursor A • Shift+Click or Right-Click: place B'
              : cursorB === null
              ? 'Click: place Cursor B • Shift+Click or Right-Click: place B'
              : 'Click: move A • Shift+Click / Right-Click: move B • Drag tabs to slide'
          }
        />
      )}
    </div>
  );
}
