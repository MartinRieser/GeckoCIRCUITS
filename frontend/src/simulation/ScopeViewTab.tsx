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
  const [activeCursor, setActiveCursor] = useState<'A' | 'B'>('A');
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

  const [yView, setYView] = useState<{ min: number; max: number } | null>(null);
  const [yScaleMode, setYScaleMode] = useState<'fixed' | 'auto'>('fixed');

  // a new simulation resets the zoom window
  useEffect(() => {
    setView(null);
    setYView(null);
  }, [results]);

  const dataT0 = timeArray.length ? timeArray[0] : 0;
  const dataT1 = timeArray.length ? timeArray[timeArray.length - 1] : 1;
  const win = effectiveWindow(view, dataT0, dataT1);
  const timePerDiv = (win.end - win.start) / 10;

  const zoomAt = useCallback(
    (factor: number, anchor: number) => {
      setView(zoomWindow(effectiveWindow(view, dataT0, dataT1), factor, anchor, dataT0, dataT1));
    },
    [view, dataT0, dataT1],
  );

  const stepTimebase = useCallback(
    (direction: 'in' | 'out') => {
      const center = (win.start + win.end) / 2;
      zoomAt(direction === 'in' ? 0.5 : 2.0, center);
    },
    [win, zoomAt],
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

  const resetZoom = useCallback(() => {
    setView(null);
    setYView(null);
  }, []);

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
              <div className="scope-control-bar" data-testid="scope-zoom-toolbar">
                <div className="scope-tb-group">
                  <span className="tb-badge" title="Horizontal Timebase per Division (10 graticule divisions)">
                    ⏱ <strong>{formatEngineeringValue(timePerDiv, 's')}/div</strong>
                  </span>
                  <div className="tb-btn-group">
                    <button type="button" title="Timebase: Zoom Out (−)" onClick={() => stepTimebase('out')}>−</button>
                    <button type="button" title="Timebase: Zoom In (+)" onClick={() => stepTimebase('in')}>+</button>
                  </div>
                  <div className="tb-btn-group">
                    <button type="button" title="Pan Left" onClick={() => panBy(-0.25)}>◀</button>
                    <button type="button" title="Pan Right" onClick={() => panBy(0.25)}>▶</button>
                  </div>
                  <button
                    type="button"
                    className="scope-action-btn"
                    title="Fit Full Simulation (Double-click plot or Esc)"
                    onClick={resetZoom}
                  >
                    ⟲ Fit
                  </button>
                  <button
                    type="button"
                    className={`scope-action-btn ${yScaleMode === 'fixed' ? 'active' : ''}`}
                    title={yScaleMode === 'fixed' ? 'Y-Scale is LOCKED: Zooming time does not rescale voltage' : 'Y-Scale is AUTO: Rescales voltage to fit visible time'}
                    onClick={() => setYScaleMode((prev) => (prev === 'fixed' ? 'auto' : 'fixed'))}
                  >
                    ↕ Scale: {yScaleMode === 'fixed' ? 'Fixed' : 'Auto'}
                  </button>
                  <button
                    type="button"
                    className={`scope-action-btn ${fftOpen ? 'active' : ''}`}
                    title="Toggle FFT spectrum of visible window"
                    onClick={() => setFftOpen((prev) => !prev)}
                  >
                    FFT
                  </button>
                  <button
                    type="button"
                    className={`scope-action-btn ${lossOpen ? 'active' : ''}`}
                    title="Toggle semiconductor loss calculator"
                    onClick={() => setLossOpen((prev) => !prev)}
                  >
                    Losses
                  </button>
                  <button
                    type="button"
                    className={`scope-action-btn ${activeCursor === 'A' ? 'active' : ''}`}
                    style={activeCursor === 'A' ? { borderColor: '#38bdf8', color: '#38bdf8', background: 'rgba(56, 189, 248, 0.18)' } : {}}
                    title="Cursor A (Click plot to place or move A)"
                    onClick={() => {
                      setActiveCursor('A');
                      if (cursorA === null && timeArray.length > 0) {
                        setCursorA(Math.floor(timeArray.length * 0.25));
                      }
                    }}
                  >
                    📍 Cursor [A]
                  </button>
                  <button
                    type="button"
                    className={`scope-action-btn ${activeCursor === 'B' ? 'active' : ''}`}
                    style={activeCursor === 'B' ? { borderColor: '#f43f5e', color: '#f43f5e', background: 'rgba(244, 63, 94, 0.18)' } : {}}
                    title="Cursor B (Click plot to place or move B)"
                    onClick={() => {
                      setActiveCursor('B');
                      if (cursorB === null && timeArray.length > 0) {
                        setCursorB(Math.floor(timeArray.length * 0.65));
                      }
                    }}
                  >
                    📍 Cursor [B]
                  </button>
                </div>

                {/* Always-visible Cursor HUD: zero scrolling needed! */}
                <div className="scope-cursor-hud">
                  {cursorA === null && cursorB === null ? (
                    <div className="hud-cursor-chips">
                      <button
                        type="button"
                        className="scope-action-btn"
                        style={{ padding: '2px 8px', fontSize: '11px', borderColor: 'var(--accent)', color: 'var(--accent)' }}
                        title="Place Cursors A & B across the waveform"
                        onClick={() => {
                          if (timeArray.length > 0) {
                            setCursorA(Math.floor(timeArray.length * 0.25));
                            setCursorB(Math.floor(timeArray.length * 0.65));
                          }
                        }}
                      >
                        📍 Place Cursors
                      </button>
                      <span className="hud-hint">
                        💡 Click plot to set A • Shift+Click sets B
                      </span>
                    </div>
                  ) : (
                    <div className="hud-cursor-chips">
                      {cursorA !== null && timeArray[cursorA] !== undefined && (
                        <button
                          type="button"
                          className={`cursor-chip chip-a ${activeCursor === 'A' ? 'selected' : ''}`}
                          style={{ cursor: 'pointer' }}
                          title="Cursor A (Click to select for moving)"
                          onClick={() => setActiveCursor('A')}
                        >
                          <strong>A:</strong> {formatEngineeringValue(timeArray[cursorA], 's')}
                        </button>
                      )}
                      {cursorB !== null && timeArray[cursorB] !== undefined && (
                        <button
                          type="button"
                          className={`cursor-chip chip-b ${activeCursor === 'B' ? 'selected' : ''}`}
                          style={{ cursor: 'pointer' }}
                          title="Cursor B (Click to select for moving)"
                          onClick={() => setActiveCursor('B')}
                        >
                          <strong>B:</strong> {formatEngineeringValue(timeArray[cursorB], 's')}
                        </button>
                      )}
                      {cursorA !== null && cursorB !== null && timeArray[cursorA] !== undefined && timeArray[cursorB] !== undefined && (
                        <>
                          <span className="cursor-chip chip-delta" title="Delta Time (t_B - t_A)">
                            <strong>Δt:</strong> {formatEngineeringValue(Math.abs(timeArray[cursorB] - timeArray[cursorA]), 's')}
                          </span>
                          <span className="cursor-chip chip-freq" title="Frequency (1/Δt)">
                            <strong>f:</strong> {Math.abs(timeArray[cursorB] - timeArray[cursorA]) > 0
                              ? formatEngineeringValue(1 / Math.abs(timeArray[cursorB] - timeArray[cursorA]), 'Hz')
                              : '—'}
                          </span>
                        </>
                      )}
                      {/* Active Signal Value & Delta Chips */}
                      {visibleSignals.map((name) => {
                        const valA = cursorA !== null && results?.[name]?.[cursorA] !== undefined ? results[name][cursorA] : null;
                        const valB = cursorB !== null && results?.[name]?.[cursorB] !== undefined ? results[name][cursorB] : null;
                        if (valA === null && valB === null) return null;
                        const delta = valA !== null && valB !== null ? valB - valA : null;
                        const unit = inferSignalUnit(name);
                        const colorIdx = signalNames.indexOf(name);
                        const color = traceColors[colorIdx >= 0 ? colorIdx % traceColors.length : 0];
                        return (
                          <span
                            key={name}
                            className="cursor-chip chip-signal"
                            title={`${name}: A=${valA !== null ? formatEngineeringValue(valA, unit) : '—'}, B=${valB !== null ? formatEngineeringValue(valB, unit) : '—'}, Δ=${delta !== null ? formatEngineeringValue(delta, unit) : '—'}`}
                            style={{ borderLeft: `3px solid ${color}` }}
                          >
                            <span className="signal-dot" style={{ backgroundColor: color }} />
                            <strong style={{ color }}>{name}:</strong>
                            {valA !== null && <span className="chip-sub">A:{formatEngineeringValue(valA, unit)}</span>}
                            {valB !== null && <span className="chip-sub">B:{formatEngineeringValue(valB, unit)}</span>}
                            {delta !== null && (
                              <span className="chip-delta-val" title={`Δ${name} = B - A (${delta >= 0 ? '+' : ''}${delta})`}>
                                Δ:{delta >= 0 ? '+' : ''}{formatEngineeringValue(delta, unit)}
                              </span>
                            )}
                          </span>
                        );
                      })}
                      <button
                        type="button"
                        className="hud-clear-btn"
                        title="Clear all measurement cursors"
                        onClick={() => {
                          setCursorA(null);
                          setCursorB(null);
                        }}
                      >
                        ✕ Clear Cursors
                      </button>
                    </div>
                  )}
                </div>
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

                  {/* Per-signal Delta Breakdown */}
                  {visibleSignals.length > 0 && (
                    <div className="cursor-signal-delta-wrap" style={{ marginTop: '14px' }}>
                      <table className="cursor-signal-table">
                        <thead>
                          <tr>
                            <th>Signal Trace</th>
                            <th>Value at A</th>
                            <th>Value at B</th>
                            <th>Delta (B − A)</th>
                            <th>|Delta|</th>
                            <th>Slew Rate (ΔY/Δt)</th>
                          </tr>
                        </thead>
                        <tbody>
                          {visibleSignals.map((name) => {
                            const valA = results?.[name]?.[cursorA] ?? 0;
                            const valB = results?.[name]?.[cursorB] ?? 0;
                            const dtVal = Math.abs(timeArray[cursorB] - timeArray[cursorA]);
                            const dY = valB - valA;
                            const absDY = Math.abs(dY);
                            const slew = dtVal > 0 ? dY / dtVal : 0;
                            const unit = inferSignalUnit(name);
                            const colorIdx = signalNames.indexOf(name);
                            const color = traceColors[colorIdx >= 0 ? colorIdx % traceColors.length : 0];
                            return (
                              <tr key={name}>
                                <td>
                                  <span className="signal-dot" style={{ backgroundColor: color, display: 'inline-block', width: 8, height: 8, borderRadius: '50%', marginRight: 6 }} />
                                  <strong style={{ color }}>{name}</strong>
                                </td>
                                <td>{formatEngineeringValue(valA, unit)}</td>
                                <td>{formatEngineeringValue(valB, unit)}</td>
                                <td style={{ fontWeight: 700, color: dY >= 0 ? '#10b981' : '#f43f5e' }}>
                                  {dY >= 0 ? '+' : ''}{formatEngineeringValue(dY, unit)}
                                </td>
                                <td>{formatEngineeringValue(absDY, unit)}</td>
                                <td>{slew !== 0 ? formatEngineeringValue(slew, `${unit}/s`) : '—'}</td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
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

        {/* Delta badge between A and B in plot */}
        {cursorA !== null && cursorB !== null && time[cursorA] !== undefined && time[cursorB] !== undefined && (() => {
          const dt = Math.abs(time[cursorB] - time[cursorA]);
          const midX = (mapX(time[cursorA]) + mapX(time[cursorB])) / 2;
          const sigDeltas = activeSignals.slice(0, 2).map((sig) => {
            const vA = signals[sig]?.[cursorA];
            const vB = signals[sig]?.[cursorB];
            if (vA === undefined || vB === undefined) return null;
            const diff = vB - vA;
            const u = inferSignalUnit(sig);
            return `Δ${sig}: ${diff >= 0 ? '+' : ''}${formatEngineeringValue(diff, u)}`;
          }).filter(Boolean);

          const badgeText = `Δt: ${formatEngineeringValue(dt, 's')}${sigDeltas.length ? ' • ' + sigDeltas.join(' • ') : ''}`;
          const badgeW = Math.max(90, badgeText.length * 6.5 + 18);

          return (
            <g pointerEvents="none">
              <rect
                x={midX - badgeW / 2}
                y={padTop + 6}
                width={badgeW}
                height={20}
                rx={4}
                fill="rgba(15, 23, 42, 0.90)"
                stroke="#10b981"
                strokeWidth={1.2}
              />
              <text
                x={midX}
                y={padTop + 20}
                fill="#34d399"
                fontSize={10.5}
                fontWeight={700}
                fontFamily="monospace"
                textAnchor="middle"
              >
                {badgeText}
              </text>
            </g>
          );
        })()}

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

        {/* Delta badge between A and B in plot */}
        {cursorA !== null && cursorB !== null && time[cursorA] !== undefined && time[cursorB] !== undefined && (() => {
          const dt = Math.abs(time[cursorB] - time[cursorA]);
          const midX = (mapX(time[cursorA]) + mapX(time[cursorB])) / 2;
          const sigDeltas = activeSignals.slice(0, 2).map((sig) => {
            const vA = signals[sig]?.[cursorA];
            const vB = signals[sig]?.[cursorB];
            if (vA === undefined || vB === undefined) return null;
            const diff = vB - vA;
            const u = inferSignalUnit(sig);
            return `Δ${sig}: ${diff >= 0 ? '+' : ''}${formatEngineeringValue(diff, u)}`;
          }).filter(Boolean);

          const badgeText = `Δt: ${formatEngineeringValue(dt, 's')}${sigDeltas.length ? ' • ' + sigDeltas.join(' • ') : ''}`;
          const badgeW = Math.max(90, badgeText.length * 6.5 + 18);

          return (
            <g pointerEvents="none">
              <rect
                x={midX - badgeW / 2}
                y={padTop + 6}
                width={badgeW}
                height={20}
                rx={4}
                fill="rgba(15, 23, 42, 0.90)"
                stroke="#10b981"
                strokeWidth={1.2}
              />
              <text
                x={midX}
                y={padTop + 20}
                fill="#34d399"
                fontSize={10.5}
                fontWeight={700}
                fontFamily="monospace"
                textAnchor="middle"
              >
                {badgeText}
              </text>
            </g>
          );
        })()}

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
