/**
 * Full-Viewport Scope Instrument & Simulation View Tab.
 * Provides high-resolution waveform plotting, simulation configuration,
 * cursor measurements, stacked / overlay modes, channel toggles, and signal metrics.
 * Fully styled for both Dark and Light themes.
 */
import { useState, useMemo, useEffect, useRef, useCallback } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import type { EditorComponent, SimulationStatus } from '../model/types';
import { formatEngineeringValue } from '../model/componentSchema';
import {
  effectiveWindow,
  panWindow,
  zoomWindow,
  clampWindow,
  MIN_SPAN,
} from './viewWindow';
import { FftPanel } from './FftPanel';
import { LossPanel } from './LossPanel';
import type { ScopeController } from './useScopeController';
import { useScopeController } from './useScopeController';

export interface ScopeViewTabProps {
  selectedScope: string; // 'all' or 'SCOPE.1', 'SCOPE.2', etc.
  components: EditorComponent[];
  results: Record<string, number[]> | null;
  displayLayout: 'overlay' | 'stacked';
  theme?: 'dark' | 'light';
  onDisplayLayoutChange?: (layout: 'overlay' | 'stacked') => void;
  status?: SimulationStatus | null;
  filename?: string | null;
  scope?: ScopeController;
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

/** Infers physical SI engineering unit from the signal or probe name. */
export function inferSignalUnit(name: string): string {
  const lower = name.toLowerCase();
  if (lower.startsWith('v') || lower.startsWith('u') || lower.includes('_v') || lower.includes('volt')) return 'V';
  if (lower.startsWith('i') || lower.includes('_i') || lower.includes('curr') || lower.startsWith('amp')) return 'A';
  if (lower.startsWith('p') || lower.includes('watt') || lower.includes('power')) return 'W';
  if (lower.startsWith('temp') || lower.includes('th_') || lower.endsWith('_c')) return '°C';
  return '';
}


/** Floating hover tooltip shared by the overlay and stacked charts. */
function WaveformTooltip({
  time,
  hoverIndex,
  activeSignals,
  signals,
  allSignals,
  traceColors,
}: {
  time: number[];
  hoverIndex: number;
  activeSignals: string[];
  signals: Record<string, number[]>;
  allSignals: string[];
  traceColors: string[];
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
    </div>
  );
}
/** Responsive container dimensions hook */
function useContainerDimensions<T extends HTMLElement>() {
  const ref = useRef<T | null>(null);
  const [dimensions, setDimensions] = useState<{ width: number; height: number }>({
    width: 1000,
    height: 480,
  });

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const update = () => {
      const rect = el.getBoundingClientRect();
      if (rect.width > 50 && rect.height > 50) {
        setDimensions({
          width: Math.round(rect.width),
          height: Math.round(rect.height),
        });
      }
    };

    update();

    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver((entries) => {
        for (const entry of entries) {
          const { width, height } = entry.contentRect;
          if (width > 50 && height > 50) {
            setDimensions({
              width: Math.round(width),
              height: Math.round(height),
            });
          }
        }
      });
      observer.observe(el);
      return () => observer.disconnect();
    } else {
      window.addEventListener('resize', update);
      return () => window.removeEventListener('resize', update);
    }
  }, []);

  return [ref, dimensions] as const;
}

export function ScopeViewTab({
  selectedScope,
  components,
  results,
  displayLayout,
  theme = 'dark',
  status,
  filename,
  scope,
}: ScopeViewTabProps) {
  const fallbackScope = useScopeController({
    results,
    components,
    selectedScope,
    theme,
  });
  const ctrl = scope ?? fallbackScope;

  const {
    view,
    setView,
    timePerDiv,
    dataT0,
    dataT1,
    yScaleMode,
    yView,
    setYView,
    signalNames,
    timeArray,
    signalStats,
    scopeChannelNames,
    hiddenSignals,
    toggleSignal,
    traceColors,
    cursorA,
    setCursorA,
    cursorB,
    setCursorB,
    activeCursor,
    setActiveCursor,
    drawerTab,
    setDrawerTab,
  } = ctrl;

  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const [currentLayout, setCurrentLayout] = useState<'overlay' | 'stacked'>(displayLayout);
  const [chartContainerRef, chartDimensions] = useContainerDimensions<HTMLDivElement>();

  useEffect(() => {
    setCurrentLayout(displayLayout);
  }, [displayLayout]);

  const win = effectiveWindow(view, dataT0, dataT1);

  const zoomAt = useCallback(
    (factor: number, anchor: number) => {
      setView(zoomWindow(effectiveWindow(view, dataT0, dataT1), factor, anchor, dataT0, dataT1));
    },
    [view, dataT0, dataT1, setView],
  );

  const zoomRange = useCallback(
    (start: number, end: number) => {
      const s = Math.min(start, end);
      const e = Math.max(start, end);
      if (e - s < MIN_SPAN) return;
      setView(clampWindow({ start: s, end: e }, dataT0, dataT1));
    },
    [dataT0, dataT1, setView],
  );

  const resetZoom = useCallback(() => {
    setView(null);
    setYView(null);
  }, [setView, setYView]);

  const filteredChannels = scopeChannelNames;

  const visibleSignals = useMemo(() => {
    return filteredChannels.filter((s) => !hiddenSignals[s]);
  }, [filteredChannels, hiddenSignals]);

  const isRunning = status === 'RUNNING' || status === 'PENDING';

  return (
    <div className="scope-view-tab-container">
      {/* Top Header Bar */}
      <div className="scope-tab-header">
        <div className="scope-tab-title-group">
          <div className="scope-badge-icon">
            {selectedScope === 'all' ? '📊' : '📺'}
          </div>
          <div>
            <span className="scope-tab-title">
              {selectedScope === 'all' ? 'Simulation Results (All Signals)' : `Scope: ${selectedScope}`}
            </span>
            <span className="scope-tab-subtitle">
              {scopeChannelNames.length} trace{scopeChannelNames.length === 1 ? '' : 's'}
              {filename ? ` • ${filename}` : ''}
            </span>
          </div>
        </div>

        <div className="scope-tab-actions">
          {/* Acquisition Status Pill */}
          <div className={`dso-status-pill ${isRunning ? 'running' : results ? 'auto' : 'stop'}`}>
            <span className="dso-status-dot" />
            {isRunning ? 'RUNNING' : results ? 'AUTO' : 'STOP'}
          </div>
        </div>
      </div>

      {/* DSO Main Viewport (Screen Bezel + Optional Drawer) */}
      <div className="dso-viewport">
        {!results || signalNames.length === 0 ? (
          <div className="scope-empty-state">
            <div className="empty-icon">{selectedScope === 'all' ? '📊' : '📺'}</div>
            <h3>No simulation data available</h3>
            <p>Configure parameters in the <strong>Simulation Settings</strong> panel on the right and click <strong>"▶ Run Simulation"</strong> to calculate waveforms.</p>
          </div>
        ) : (
          <>
            {/* The Oscilloscope Screen Bezel */}
            <div className="dso-screen-bezel">
              {/* On-Screen Display (OSD) Top Bar */}
              <div className="dso-screen-osd">
                <div className="dso-osd-left">
                  <span className={`dso-status-pill ${isRunning ? 'running' : 'auto'}`}>
                    <span className="dso-status-dot" />
                    {isRunning ? 'RUN' : 'AUTO'}
                  </span>
                  <span className="dso-osd-item">
                    <span>M:</span>
                    <strong className="dso-osd-val">{formatEngineeringValue(timePerDiv, 's')}/div</strong>
                  </span>
                  <span className="dso-osd-item">
                    <span>Delay:</span>
                    <strong className="dso-osd-val">{formatEngineeringValue(win.start, 's')}</strong>
                  </span>
                  <span className="dso-osd-item">
                    <span>Span:</span>
                    <strong className="dso-osd-val">{formatEngineeringValue(win.end - win.start, 's')}</strong>
                  </span>
                </div>

                <div className="dso-osd-right">
                  {visibleSignals.slice(0, 3).map((sig) => {
                    const colorIdx = signalNames.indexOf(sig);
                    const color = traceColors[colorIdx >= 0 ? colorIdx % traceColors.length : 0];
                    return (
                      <span key={sig} className="dso-osd-channel-tag" style={{ borderLeft: `3px solid ${color}` }}>
                        <span style={{ color }}>{sig}</span>
                      </span>
                    );
                  })}
                  <span className="dso-osd-item">
                    <span>Pts:</span>
                    <strong className="dso-osd-val">{timeArray.length}</strong>
                  </span>
                </div>
              </div>

              {/* Central Chart Viewport with responsive ResizeObserver */}
              <div className="dso-chart-container" ref={chartContainerRef}>
                {currentLayout === 'stacked' ? (
                  <FullScreenStackedChart
                    width={chartDimensions.width}
                    height={chartDimensions.height}
                    time={timeArray}
                    signals={results}
                    activeSignals={visibleSignals}
                    allSignals={signalNames}
                    theme={theme}
                    hoverIndex={hoverIndex}
                    onHoverIndex={setHoverIndex}
                    cursorA={cursorA}
                    cursorB={cursorB}
                    activeCursor={activeCursor}
                    onSetActiveCursor={setActiveCursor}
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
                    width={chartDimensions.width}
                    height={chartDimensions.height}
                    time={timeArray}
                    signals={results}
                    activeSignals={visibleSignals}
                    allSignals={signalNames}
                    theme={theme}
                    hoverIndex={hoverIndex}
                    onHoverIndex={setHoverIndex}
                    cursorA={cursorA}
                    cursorB={cursorB}
                    activeCursor={activeCursor}
                    onSetActiveCursor={setActiveCursor}
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
                    yScaleMode={yScaleMode}
                    yZoomRange={yView}
                    onZoomYRange={setYView}
                  />
                )}
              </div>

              {/* Screen Bottom Quick-Measurement Strip */}
              <div className="dso-quick-measure-bar">
                <div className="dso-quick-tiles">
                  {visibleSignals.slice(0, 3).map((name) => {
                    const st = signalStats[name];
                    if (!st) return null;
                    const unit = inferSignalUnit(name);
                    const colorIdx = signalNames.indexOf(name);
                    const color = traceColors[colorIdx >= 0 ? colorIdx % traceColors.length : 0];
                    return (
                      <div key={name} className="dso-measure-tile" style={{ borderLeft: `3px solid ${color}` }}>
                        <span style={{ color, fontWeight: 700 }}>{name}</span>
                        <span>Vpp: <strong className="dso-measure-tile-val">{formatEngineeringValue(st.pkpk, unit)}</strong></span>
                        <span>Vrms: <strong className="dso-measure-tile-val">{formatEngineeringValue(st.rms, unit)}</strong></span>
                        <span>Mean: <strong className="dso-measure-tile-val">{formatEngineeringValue(st.mean, unit)}</strong></span>
                      </div>
                    );
                  })}
                </div>

                <button
                  type="button"
                  className="dso-drawer-toggle-btn"
                  onClick={() => setDrawerTab((prev) => (prev ? null : 'metrics'))}
                >
                  📊 Analysis Drawer {drawerTab ? '▼' : '▲'}
                </button>
              </div>
            </div>

            {/* Collapsible Bottom Analysis Drawer */}
            {drawerTab && (
              <div className="dso-bottom-drawer">
                <div className="dso-drawer-tabs">
                  <div className="dso-tab-group">
                    <button
                      type="button"
                      className={`dso-tab-btn ${drawerTab === 'metrics' ? 'active' : ''}`}
                      onClick={() => setDrawerTab('metrics')}
                    >
                      📊 Signal Statistics
                    </button>
                    <button
                      type="button"
                      className={`dso-tab-btn ${drawerTab === 'fft' ? 'active' : ''}`}
                      onClick={() => setDrawerTab('fft')}
                    >
                      〰 FFT Spectrum
                    </button>
                    <button
                      type="button"
                      className={`dso-tab-btn ${drawerTab === 'losses' ? 'active' : ''}`}
                      onClick={() => setDrawerTab('losses')}
                    >
                      ⚡ Semiconductor Losses
                    </button>
                  </div>
                  <button
                    type="button"
                    className="dso-drawer-close-btn"
                    onClick={() => setDrawerTab(null)}
                    title="Minimize Analysis Drawer"
                  >
                    ✕ Minimize
                  </button>
                </div>

                <div className="dso-drawer-content">
                  {drawerTab === 'metrics' && (
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
                          const unit = inferSignalUnit(name);
                          return (
                            <tr
                              key={name}
                              onClick={() => toggleSignal(name)}
                              className={isHidden ? 'row-hidden' : ''}
                              title="Click row to toggle trace visibility"
                            >
                              <td style={{ color, fontWeight: 700 }}>
                                <span className="dso-ch-led" style={{ backgroundColor: color, color, marginRight: 6 }} />
                                {name}
                              </td>
                              <td>{formatEngineeringValue(st.min, unit)}</td>
                              <td>{formatEngineeringValue(st.max, unit)}</td>
                              <td>{formatEngineeringValue(st.pkpk, unit)}</td>
                              <td>{formatEngineeringValue(st.rms, unit)}</td>
                              <td>{formatEngineeringValue(st.mean, unit)}</td>
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
                  )}

                  {drawerTab === 'fft' && (
                    <FftPanel
                      time={timeArray}
                      signals={results}
                      activeSignals={visibleSignals}
                      viewStart={win.start}
                      viewEnd={win.end}
                    />
                  )}

                  {drawerTab === 'losses' && <LossPanel />}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

/** Full-Screen Overlay Waveform Chart */
function FullScreenOverlayChart({
  width = 1000,
  height = 480,
  time,
  signals,
  activeSignals,
  allSignals,
  theme,
  hoverIndex,
  onHoverIndex,
  cursorA,
  cursorB,
  activeCursor,
  onSetActiveCursor,
  onSetCursor,
  viewStart,
  viewEnd,
  onZoomAt,
  onZoomRange,
  onResetZoom,
  onPanSeconds,
  yScaleMode,
  yZoomRange,
  onZoomYRange,
}: {
  width?: number;
  height?: number;
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  allSignals: string[];
  theme: 'dark' | 'light';
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
  cursorA: number | null;
  cursorB: number | null;
  activeCursor?: 'A' | 'B';
  onSetActiveCursor?: (c: 'A' | 'B') => void;
  onSetCursor: (type: 'A' | 'B', idx: number) => void;
  viewStart: number;
  viewEnd: number;
  onZoomAt: (factor: number, anchor: number) => void;
  onZoomRange: (start: number, end: number) => void;
  onResetZoom: () => void;
  onPanSeconds: (delta: number) => void;
  yScaleMode: 'fixed' | 'auto';
  yZoomRange: { min: number; max: number } | null;
  onZoomYRange: (range: { min: number; max: number } | null) => void;
}) {
  const padLeft = 68;
  const padRight = 24;
  const padTop = 20;
  const padBottom = 34;

  const plotW = Math.max(100, width - padLeft - padRight);
  const plotH = Math.max(100, height - padTop - padBottom);

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

  // Global min/max across all active signals over the entire simulation (rock-steady scale)
  const { globalMinY, globalMaxY } = useMemo(() => {
    let y0 = Infinity;
    let y1 = -Infinity;
    for (const name of activeSignals) {
      const arr = signals[name] || [];
      for (let i = 0; i < arr.length; i++) {
        const v = arr[i];
        if (v !== undefined) {
          if (v < y0) y0 = v;
          if (v > y1) y1 = v;
        }
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
    return { globalMinY: y0 - yPad, globalMaxY: y1 + yPad };
  }, [activeSignals, signals]);

  // Visible slice min/max (used only when user switches to 'auto' Y-scale)
  const { sliceMinY, sliceMaxY } = useMemo(() => {
    let y0 = Infinity;
    let y1 = -Infinity;
    const i0 = sampleIndexAt(time, minT);
    const i1 = sampleIndexAt(time, maxT);
    for (const name of activeSignals) {
      const arr = signals[name] || [];
      for (let i = i0; i <= i1 && i < arr.length; i++) {
        const v = arr[i];
        if (v !== undefined) {
          if (v < y0) y0 = v;
          if (v > y1) y1 = v;
        }
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
    return { sliceMinY: y0 - yPad, sliceMaxY: y1 + yPad };
  }, [time, signals, activeSignals, minT, maxT]);

  // If 2D box zoomed, use yZoomRange; else fixed or slice
  const { minY, maxY } = useMemo(() => {
    if (yZoomRange) {
      return { minY: yZoomRange.min, maxY: yZoomRange.max };
    }
    if (yScaleMode === 'auto') {
      return { minY: sliceMinY, maxY: sliceMaxY };
    }
    return { minY: globalMinY, maxY: globalMaxY };
  }, [yZoomRange, yScaleMode, sliceMinY, sliceMaxY, globalMinY, globalMaxY]);

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
    const count = 8;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minY + (i / count) * (maxY - minY);
      ticks.push({ val, y: mapY(val) });
    }
    return ticks;
  }, [minY, maxY, plotH, padTop]);

  const xTicks = useMemo(() => {
    const count = 10;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minT + (i / count) * (maxT - minT);
      ticks.push({ val, x: mapX(val) });
    }
    return ticks;
  }, [minT, maxT, plotW, padLeft]);

  // Center axes sub-division tick marks (5 minor ticks per division)
  const centerTicks = useMemo(() => {
    const xMid = padLeft + plotW / 2;
    const yMid = padTop + plotH / 2;
    const ticks: { x1: number; y1: number; x2: number; y2: number }[] = [];

    // Horizontal center line subdivision ticks (10 divisions * 5 sub = 50 intervals)
    for (let div = 0; div < 10; div++) {
      for (let sub = 1; sub < 5; sub++) {
        const x = padLeft + ((div + sub / 5) / 10) * plotW;
        ticks.push({ x1: x, y1: yMid - 3, x2: x, y2: yMid + 3 });
      }
    }

    // Vertical center line subdivision ticks (8 divisions * 5 sub = 40 intervals)
    for (let div = 0; div < 8; div++) {
      for (let sub = 1; sub < 5; sub++) {
        const y = padTop + ((div + sub / 5) / 8) * plotH;
        ticks.push({ x1: xMid - 3, y1: y, x2: xMid + 3, y2: y });
      }
    }

    return ticks;
  }, [padLeft, padTop, plotW, plotH]);

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
    onSetActiveCursor?.(type);
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
    const rect = e.currentTarget.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / (rect.width || 1)) * width;
    if (svgX < padLeft || svgX > width - padRight) return;
    const t = minT + ((svgX - padLeft) / plotW) * (maxT - minT);
    const targetIdx = sampleIndexAt(time, t);
    let targetCursor: 'A' | 'B' = 'A';
    if (e.shiftKey) {
      targetCursor = 'B';
      onSetActiveCursor?.('B');
    } else if (cursorA === null) {
      targetCursor = 'A';
      onSetActiveCursor?.('B');
    } else if (cursorB === null) {
      targetCursor = 'B';
      onSetActiveCursor?.('A');
    } else {
      targetCursor = activeCursor ?? 'A';
    }
    onSetCursor(targetCursor, targetIdx);
  };

  const handleContextMenu = (e: ReactMouseEvent<SVGSVGElement>) => {
    e.preventDefault();
    const rect = e.currentTarget.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / (rect.width || 1)) * width;
    if (svgX < padLeft || svgX > width - padRight) return;
    const t = minT + ((svgX - padLeft) / plotW) * (maxT - minT);
    const targetIdx = sampleIndexAt(time, t);
    onSetCursor('B', targetIdx);
    onSetActiveCursor?.('B');
  };

  const [dragBox, setDragBox] = useState<{ startX: number; startY: number; currentX: number; currentY: number } | null>(null);
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

    // Left click -> 2D Box Zoom or Click
    if (e.button === 0) {
      const svg = svgRef.current;
      if (!svg) return;
      const rect = svg.getBoundingClientRect();
      const getSvgX = (clientX: number) => {
        const sx = ((clientX - rect.left) / (rect.width || 1)) * width;
        return Math.max(padLeft, Math.min(width - padRight, sx));
      };
      const getSvgY = (clientY: number) => {
        const sy = ((clientY - rect.top) / (rect.height || 1)) * height;
        return Math.max(padTop, Math.min(height - padBottom, sy));
      };

      const startSvgX = getSvgX(e.clientX);
      const startSvgY = getSvgY(e.clientY);
      let currentSvgX = startSvgX;
      let currentSvgY = startSvgY;
      suppressClickRef.current = false;

      const onMove = (ev: PointerEvent) => {
        currentSvgX = getSvgX(ev.clientX);
        currentSvgY = getSvgY(ev.clientY);
        if (Math.abs(currentSvgX - startSvgX) > 5 || Math.abs(currentSvgY - startSvgY) > 5) {
          suppressClickRef.current = true;
          setDragBox({ startX: startSvgX, startY: startSvgY, currentX: currentSvgX, currentY: currentSvgY });
        }
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
        const dx = Math.abs(currentSvgX - startSvgX);
        const dy = Math.abs(currentSvgY - startSvgY);
        if (dx > 6 || dy > 6) {
          if (dx > 6) {
            const xA = Math.min(startSvgX, currentSvgX);
            const xB = Math.max(startSvgX, currentSvgX);
            const tA = minT + ((xA - padLeft) / plotW) * (maxT - minT);
            const tB = minT + ((xB - padLeft) / plotW) * (maxT - minT);
            onZoomRange(tA, tB);
          }
          if (dy > 12) {
            const yA = Math.min(startSvgY, currentSvgY);
            const yB = Math.max(startSvgY, currentSvgY);
            const vTop = maxY - ((yA - padTop) / plotH) * (maxY - minY);
            const vBottom = maxY - ((yB - padTop) / plotH) * (maxY - minY);
            const vMin = Math.min(vTop, vBottom);
            const vMax = Math.max(vTop, vBottom);
            if (vMax - vMin > 1e-12) {
              onZoomYRange({ min: vMin, max: vMax });
            }
          }
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
              strokeDasharray="2 3"
              strokeWidth={i === 4 ? 1.2 : 0.75}
            />
            <text
              x={padLeft - 8}
              y={y + 4}
              textAnchor="end"
              fill={textColor}
              fontSize={10}
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
              strokeDasharray="2 3"
              strokeWidth={i === 5 ? 1.2 : 0.75}
            />
            <text
              x={x}
              y={height - padBottom + 16}
              fill={textColor}
              fontSize={10}
              fontWeight={600}
              textAnchor="middle"
              fontFamily="monospace"
            >
              {formatEngineeringValue(val, 's')}
            </text>
          </g>
        ))}

        {/* Center-Axis Subdivision Ticks (Classic DSO graticule) */}
        {centerTicks.map((t, idx) => (
          <line
            key={`ct-${idx}`}
            x1={t.x1}
            y1={t.y1}
            x2={t.x2}
            y2={t.y2}
            stroke={gridColor}
            strokeWidth={1}
            opacity={0.65}
          />
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

        {/* Channel Ground Reference Markers on left axis */}
        {activeSignals.map((name) => {
          const colorIdx = allSignals.indexOf(name);
          const color = traceColors[colorIdx >= 0 ? colorIdx % traceColors.length : 0];
          const y0 = mapY(0);
          if (y0 < padTop - 6 || y0 > height - padBottom + 6) return null;
          const clampedY = Math.max(padTop + 6, Math.min(height - padBottom - 6, y0));
          return (
            <g key={`gnd-${name}`} className="channel-ground-marker">
              <path
                d={`M ${padLeft - 18} ${clampedY - 6} L ${padLeft - 5} ${clampedY - 6} L ${padLeft} ${clampedY} L ${padLeft - 5} ${clampedY + 6} L ${padLeft - 18} ${clampedY + 6} Z`}
                fill={color}
                opacity={0.9}
              />
              <text
                x={padLeft - 11}
                y={clampedY + 3.5}
                fill="#ffffff"
                fontSize={8.5}
                fontWeight={800}
                fontFamily="monospace"
                textAnchor="middle"
              >
                {colorIdx + 1}
              </text>
            </g>
          );
        })}

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

        {/* Drag Box Zoom Selection (2D box or Time-only) */}
        {dragBox && (Math.abs(dragBox.currentX - dragBox.startX) > 5 || Math.abs(dragBox.currentY - dragBox.startY) > 5) && (() => {
          const bx = Math.min(dragBox.startX, dragBox.currentX);
          const by = Math.min(dragBox.startY, dragBox.currentY);
          const bw = Math.abs(dragBox.currentX - dragBox.startX);
          const bh = Math.abs(dragBox.currentY - dragBox.startY);
          const is2D = bh > 12;
          const tA = minT + ((bx - padLeft) / plotW) * (maxT - minT);
          const tB = minT + (((bx + bw) - padLeft) / plotW) * (maxT - minT);
          const dt = tB - tA;

          return (
            <g pointerEvents="none">
              <rect
                x={bx}
                y={is2D ? by : padTop}
                width={bw}
                height={is2D ? bh : plotH}
                fill="rgba(56, 189, 248, 0.22)"
                stroke="#38bdf8"
                strokeWidth={1.5}
                strokeDasharray="4 3"
              />
              <rect
                x={bx}
                y={Math.max(padTop, (is2D ? by : padTop) - 22)}
                width={is2D ? 160 : 130}
                height={18}
                rx={3}
                fill="#0284c7"
                opacity={0.95}
              />
              <text
                x={bx + 6}
                y={Math.max(padTop, (is2D ? by : padTop) - 22) + 13}
                fill="#ffffff"
                fontSize={10}
                fontWeight={700}
              >
                🔍 {is2D ? '2D Box Zoom' : 'Time Zoom'} ({formatEngineeringValue(dt, 's')})
              </text>
            </g>
          );
        })()}

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
              x={Math.max(padLeft, Math.min(width - padRight - 72, mapX(time[hoverIndex]) - 36))}
              y={height - padBottom + 4}
              width={72}
              height={18}
              rx={4}
              fill="#0f172a"
              stroke="#38bdf8"
              strokeWidth={1.2}
              className="hover-pill"
            />
            <text
              x={Math.max(padLeft + 36, Math.min(width - padRight - 36, mapX(time[hoverIndex])))}
              y={height - padBottom + 17}
              textAnchor="middle"
              fill="#38bdf8"
              fontSize={10.5}
              fontWeight={700}
              fontFamily="monospace"
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
        />
      )}
    </div>
  );
}

/** Full-Screen Stacked Subplots Waveform Chart */
function FullScreenStackedChart({
  width = 1000,
  height = 480,
  time,
  signals,
  activeSignals,
  allSignals,
  theme,
  hoverIndex,
  onHoverIndex,
  cursorA,
  cursorB,
  activeCursor,
  onSetActiveCursor,
  onSetCursor,
  viewStart,
  viewEnd,
  onZoomAt,
  onZoomRange,
  onResetZoom,
  onPanSeconds,
}: {
  width?: number;
  height?: number;
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  allSignals: string[];
  theme: 'dark' | 'light';
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
  cursorA: number | null;
  cursorB: number | null;
  activeCursor?: 'A' | 'B';
  onSetActiveCursor?: (c: 'A' | 'B') => void;
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
  const padLeft = 68;
  const padRight = 24;
  const padTop = 20;
  const padBottom = 30;
  const laneGap = 12;
  const count = activeSignals.length || 1;
  const availableH = height - padTop - padBottom;
  const idealLaneH = Math.max(64, Math.floor((availableH - (count - 1) * laneGap) / count));
  const laneH = idealLaneH;
  const totalH = Math.max(height, padTop + padBottom + count * (laneH + laneGap) - laneGap);
  const plotW = Math.max(100, width - padLeft - padRight);
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
    onSetActiveCursor?.(type);
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
    const rect = e.currentTarget.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / rect.width) * width;
    if (svgX < padLeft || svgX > width - padRight) return;
    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    const targetIdx = sampleIndexAt(time, t);

    let targetCursor: 'A' | 'B' = 'A';
    if (e.shiftKey) {
      targetCursor = 'B';
      onSetActiveCursor?.('B');
    } else if (cursorA === null) {
      targetCursor = 'A';
      onSetActiveCursor?.('B');
    } else if (cursorB === null) {
      targetCursor = 'B';
      onSetActiveCursor?.('A');
    } else {
      targetCursor = activeCursor ?? 'A';
    }
    onSetCursor(targetCursor, targetIdx);
  };

  const handleContextMenu = (e: ReactMouseEvent<SVGSVGElement>) => {
    e.preventDefault();
    const rect = e.currentTarget.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / rect.width) * width;
    if (svgX < padLeft || svgX > width - padRight) return;
    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    const targetIdx = sampleIndexAt(time, t);
    onSetCursor('B', targetIdx);
    onSetActiveCursor?.('B');
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

        {/* Crosshair Line & Pill */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <g className="hover-crosshair" pointerEvents="none">
            <line
              x1={mapX(time[hoverIndex])}
              y1={padTop}
              x2={mapX(time[hoverIndex])}
              y2={totalH - padBottom}
              stroke={crosshairColor}
              strokeWidth={1}
              strokeDasharray="3 3"
            />
            <rect
              x={Math.max(padLeft, Math.min(width - padRight - 72, mapX(time[hoverIndex]) - 36))}
              y={totalH - padBottom + 4}
              width={72}
              height={18}
              rx={4}
              fill="#0f172a"
              stroke="#38bdf8"
              strokeWidth={1.2}
              className="hover-pill"
            />
            <text
              x={Math.max(padLeft + 36, Math.min(width - padRight - 36, mapX(time[hoverIndex])))}
              y={totalH - padBottom + 17}
              textAnchor="middle"
              fill="#38bdf8"
              fontSize={10.5}
              fontWeight={700}
              fontFamily="monospace"
              className="hover-text"
            >
              {formatEngineeringValue(time[hoverIndex], 's')}
            </text>
          </g>
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
