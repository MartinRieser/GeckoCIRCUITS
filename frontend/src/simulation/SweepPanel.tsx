/**
 * Sweep Summary Drawer Panel: displays an interactive XY curve plot
 * ("Swept Parameter Value vs. Signal Metric") and a clickable results table
 * where users can highlight runs on the scope or apply a winning value to the circuit.
 */
import { useEffect, useMemo, useState } from 'react';
import { formatEngineeringValue } from '../model/componentSchema';
import { inferSignalUnit } from './ScopeViewTab';
import {
  exportSweepSummaryCsv,
  sweepRunColor,
  type SweepSession,
  type SweepSummaryMetric,
} from './parametricSweep';

export interface SweepPanelProps {
  sweepSession: SweepSession | null;
  availableSignals: string[];
  theme?: 'dark' | 'light';
  overlayEnabled: boolean;
  onToggleOverlay: (enabled: boolean) => void;
  selectedSignal: string;
  onSelectSignal: (signal: string) => void;
  highlightedRunIndex: number | null;
  onHighlightRun: (index: number | null) => void;
  onApplyValue?: (componentName: string, paramKey: string, value: number) => void;
  onClearSweep: () => void;
}

const METRIC_LABELS: Record<SweepSummaryMetric, string> = {
  mean: 'Mean (Avg)',
  rms: 'RMS',
  pkpk: 'Peak-to-Peak',
  max: 'Maximum',
  min: 'Minimum',
};

export function SweepPanel({
  sweepSession,
  availableSignals,
  theme = 'dark',
  overlayEnabled,
  onToggleOverlay,
  selectedSignal,
  onSelectSignal,
  highlightedRunIndex,
  onHighlightRun,
  onApplyValue,
  onClearSweep,
}: SweepPanelProps) {
  const [metric, setMetric] = useState<SweepSummaryMetric>('mean');

  const signalOptions = useMemo(() => {
    const set = new Set<string>(availableSignals);
    if (sweepSession) {
      for (const run of sweepSession.runs) {
        Object.keys(run.stats).forEach((s) => set.add(s));
      }
    }
    return Array.from(set);
  }, [availableSignals, sweepSession]);

  useEffect(() => {
    if (signalOptions.length > 0 && !signalOptions.includes(selectedSignal)) {
      onSelectSignal(signalOptions[0]);
    }
  }, [signalOptions, selectedSignal, onSelectSignal]);

  if (!sweepSession) {
    return (
      <div className="sweep-panel-empty">
        No parametric sweep data yet. Click <strong>📈 Sweep</strong> in the toolbar to run one.
      </div>
    );
  }

  const sigUnit = inferSignalUnit(selectedSignal);
  const completedPoints = sweepSession.runs
    .map((run) => {
      const st = run.stats[selectedSignal];
      const y = st ? st[metric] : undefined;
      return y !== undefined && Number.isFinite(y)
        ? { index: run.index, x: run.parameterValue, y, color: sweepRunColor(run.index, sweepSession.totalRuns, theme) }
        : null;
    })
    .filter((pt): pt is NonNullable<typeof pt> => pt !== null);

  const handleDownloadCsv = () => {
    if (!selectedSignal) return;
    const csv = exportSweepSummaryCsv(sweepSession, selectedSignal);
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `sweep_${sweepSession.componentName}_${sweepSession.paramKey}_${selectedSignal}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // SVG XY Plot dimensions
  const w = 440;
  const h = 180;
  const padL = 60;
  const padR = 18;
  const padT = 16;
  const padB = 30;
  const plotW = w - padL - padR;
  const plotH = h - padT - padB;

  let x0 = Infinity;
  let x1 = -Infinity;
  let y0 = Infinity;
  let y1 = -Infinity;
  for (const pt of completedPoints) {
    if (pt.x < x0) x0 = pt.x;
    if (pt.x > x1) x1 = pt.x;
    if (pt.y < y0) y0 = pt.y;
    if (pt.y > y1) y1 = pt.y;
  }
  if (!Number.isFinite(x0)) {
    x0 = 0;
    x1 = 1;
  } else if (x0 === x1) {
    x0 -= 1;
    x1 += 1;
  }
  if (!Number.isFinite(y0)) {
    y0 = 0;
    y1 = 1;
  } else if (y0 === y1) {
    y0 -= 1;
    y1 += 1;
  }
  const yPad = (y1 - y0) * 0.1;
  const minY = y0 - yPad;
  const maxY = y1 + yPad;

  const mapX = (x: number) => padL + ((x - x0) / (x1 - x0 || 1)) * plotW;
  const mapY = (y: number) => padT + plotH - ((y - minY) / (maxY - minY || 1)) * plotH;
  const linePath =
    completedPoints.length > 0
      ? completedPoints.map((pt, i) => `${i === 0 ? 'M' : 'L'} ${mapX(pt.x)} ${mapY(pt.y)}`).join(' ')
      : '';

  return (
    <div className="sweep-panel" data-testid="sweep-panel">
      <div className="sweep-panel-toolbar">
        <div className="sweep-panel-controls">
          <span className="sweep-panel-title">
            📈 {sweepSession.paramLabel}
          </span>
          <label className="sweep-inline-select">
            <span>Signal:</span>
            <select
              data-testid="sweep-signal-select"
              value={selectedSignal}
              onChange={(e) => onSelectSignal(e.target.value)}
            >
              {signalOptions.map((sig) => (
                <option key={sig} value={sig}>
                  {sig}
                </option>
              ))}
            </select>
          </label>
          <label className="sweep-inline-select">
            <span>Metric:</span>
            <select
              data-testid="sweep-metric-select"
              value={metric}
              onChange={(e) => setMetric(e.target.value as SweepSummaryMetric)}
            >
              {Object.entries(METRIC_LABELS).map(([k, lbl]) => (
                <option key={k} value={k}>
                  {lbl}
                </option>
              ))}
            </select>
          </label>
          <label className="sweep-overlay-toggle">
            <input
              type="checkbox"
              data-testid="sweep-overlay-checkbox"
              checked={overlayEnabled}
              onChange={(e) => onToggleOverlay(e.target.checked)}
            />
            <span>Overlay on Scope</span>
          </label>
        </div>

        <div className="sweep-panel-actions">
          <button
            type="button"
            className="sim-mini-btn"
            onClick={handleDownloadCsv}
            disabled={completedPoints.length === 0}
          >
            📥 Export CSV
          </button>
          <button
            type="button"
            className="sim-mini-btn"
            data-testid="sweep-clear-btn"
            onClick={onClearSweep}
          >
            ✕ Clear Sweep
          </button>
        </div>
      </div>

      <div className="sweep-panel-split">
        {/* XY Summary Chart */}
        <div className="sweep-xy-chart-wrap">
          <svg
            viewBox={`0 0 ${w} ${h}`}
            className="sweep-xy-svg"
            data-testid="sweep-xy-chart"
          >
            <rect width={w} height={h} className="waveform-bg" rx={6} />
            <rect x={padL} y={padT} width={plotW} height={plotH} className="waveform-plot-area" rx={4} />
            {/* Y Axis labels */}
            {[minY, (minY + maxY) / 2, maxY].map((yv, idx) => (
              <g key={idx}>
                <line
                  x1={padL}
                  y1={mapY(yv)}
                  x2={w - padR}
                  y2={mapY(yv)}
                  stroke="var(--chart-grid)"
                  strokeDasharray="2 3"
                  strokeWidth={0.75}
                />
                <text
                  x={padL - 6}
                  y={mapY(yv) + 3}
                  textAnchor="end"
                  fill="var(--chart-text)"
                  fontSize={9.5}
                  fontFamily="monospace"
                >
                  {formatEngineeringValue(yv, sigUnit)}
                </text>
              </g>
            ))}
            {/* X Axis labels */}
            {[x0, (x0 + x1) / 2, x1].map((xv, idx) => (
              <text
                key={idx}
                x={mapX(xv)}
                y={h - 8}
                textAnchor={idx === 0 ? 'start' : idx === 2 ? 'end' : 'middle'}
                fill="var(--chart-text)"
                fontSize={9.5}
                fontFamily="monospace"
              >
                {formatEngineeringValue(xv, sweepSession.unit)}
              </text>
            ))}
            {linePath && (
              <path d={linePath} fill="none" stroke="var(--accent)" strokeWidth={2} />
            )}
            {completedPoints.map((pt) => {
              const isSelected = highlightedRunIndex === pt.index;
              return (
                <g key={pt.index} style={{ cursor: 'pointer' }} onClick={() => onHighlightRun(isSelected ? null : pt.index)}>
                  <title>{`Run ${pt.index + 1}: ${formatEngineeringValue(pt.x, sweepSession.unit)} → ${formatEngineeringValue(pt.y, sigUnit)}`}</title>
                  <circle
                    data-testid={`sweep-xy-point-${pt.index}`}
                    cx={mapX(pt.x)}
                    cy={mapY(pt.y)}
                    r={isSelected ? 6.5 : 4.5}
                    fill={pt.color}
                    stroke="#ffffff"
                    strokeWidth={isSelected ? 2 : 1.2}
                  />
                </g>
              );
            })}
          </svg>
        </div>

        {/* Summary Table */}
        <div className="sweep-table-wrap">
          <table className="scope-metrics-table">
            <thead>
              <tr>
                <th>Run</th>
                <th>Value</th>
                <th>Mean</th>
                <th>RMS</th>
                <th>Pk-Pk</th>
                <th>Status</th>
                {onApplyValue && <th>Action</th>}
              </tr>
            </thead>
            <tbody>
              {sweepSession.runs.map((run) => {
                const st = run.stats[selectedSignal];
                const color = sweepRunColor(run.index, sweepSession.totalRuns, theme);
                const isHighlighted = highlightedRunIndex === run.index;
                return (
                  <tr
                    key={run.index}
                    data-testid={`sweep-row-${run.index}`}
                    className={isHighlighted ? 'sweep-row-selected' : ''}
                    onClick={() => onHighlightRun(isHighlighted ? null : run.index)}
                  >
                    <td style={{ color, fontWeight: 700 }}>
                      <span className="dso-ch-led" style={{ backgroundColor: color, color, marginRight: 6 }} />
                      #{run.index + 1}
                    </td>
                    <td>{formatEngineeringValue(run.parameterValue, sweepSession.unit)}</td>
                    <td>{st ? formatEngineeringValue(st.mean, sigUnit) : '—'}</td>
                    <td>{st ? formatEngineeringValue(st.rms, sigUnit) : '—'}</td>
                    <td>{st ? formatEngineeringValue(st.pkpk, sigUnit) : '—'}</td>
                    <td>{run.status}</td>
                    {onApplyValue && (
                      <td>
                        <button
                          type="button"
                          className="sim-mini-btn"
                          data-testid={`sweep-apply-btn-${run.index}`}
                          onClick={(e) => {
                            e.stopPropagation();
                            onApplyValue(sweepSession.componentName, sweepSession.paramKey, run.parameterValue);
                          }}
                          title={`Apply ${formatEngineeringValue(run.parameterValue, sweepSession.unit)} to ${sweepSession.componentName}`}
                        >
                          Apply
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
