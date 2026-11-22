/**
 * Pure utilities for Pin-to-Compare Baseline waveform comparison:
 * deep snapshot capture, metric delta calculations, delta formatting,
 * and shared decimated SVG path builder for ghost overlay traces.
 */
import { computeSignalStats, type SignalStats } from './chartData';

export interface BaselineSnapshot {
  /** Monotonic snapshot ID. */
  id: string;
  /** Human-readable label shown in badge/legend. */
  label: string;
  /** Timestamp (ms) when pinned. */
  capturedAt: number;
  /** Circuit modelVersion when pinned. */
  modelVersion: number;
  /** Deep-copied time array and signal arrays from simResults. */
  results: Record<string, number[]>;
  /** Pre-computed per-signal stats for fast delta rendering. */
  stats: Record<string, SignalStats>;
}

export interface SignalMetricDelta {
  baseline: number;
  current: number;
  /** current - baseline */
  absDelta: number;
  /** ((current - baseline) / |baseline|) * 100, or null when |baseline| < 1e-12 */
  pctDelta: number | null;
}

export interface SignalComparison {
  signal: string;
  mean: SignalMetricDelta;
  rms: SignalMetricDelta;
  pkpk: SignalMetricDelta;
  min: SignalMetricDelta;
  max: SignalMetricDelta;
}

const ZERO_EPSILON = 1e-12;
let snapshotSeq = 0;

/**
 * Creates an immutable deep-cloned snapshot of simulation results and pre-computes
 * per-signal statistics so subsequent runs can be compared safely.
 */
export function createBaselineSnapshot(
  results: Record<string, number[]>,
  modelVersion: number,
  label?: string,
): BaselineSnapshot {
  const clonedResults: Record<string, number[]> = {};
  const stats: Record<string, SignalStats> = {};

  for (const [key, arr] of Object.entries(results)) {
    if (!Array.isArray(arr)) continue;
    clonedResults[key] = arr.slice();
    if (key !== 'time') {
      const s = computeSignalStats(clonedResults[key]);
      if (s) stats[key] = s;
    }
  }

  snapshotSeq += 1;
  const now = Date.now();
  const timeStr = new Date(now).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });

  return {
    id: `baseline_${now}_${snapshotSeq}`,
    label: label?.trim() || `Baseline (${timeStr})`,
    capturedAt: now,
    modelVersion,
    results: clonedResults,
    stats,
  };
}

/**
 * Computes signed absolute and percentage delta between a baseline metric and current metric.
 */
export function computeMetricDelta(baseline: number, current: number): SignalMetricDelta {
  const absDelta = current - baseline;
  const pctDelta =
    !Number.isFinite(baseline) || !Number.isFinite(current) || Math.abs(baseline) < ZERO_EPSILON
      ? null
      : (absDelta / Math.abs(baseline)) * 100;

  return {
    baseline,
    current,
    absDelta,
    pctDelta,
  };
}

/**
 * Compares baseline and current SignalStats for each requested signal present in both maps.
 */
export function compareSignalStats(
  baselineStats: Record<string, SignalStats>,
  currentStats: Record<string, SignalStats>,
  signals: string[],
): Record<string, SignalComparison> {
  const out: Record<string, SignalComparison> = {};
  for (const signal of signals) {
    const base = baselineStats[signal];
    const curr = currentStats[signal];
    if (!base || !curr) continue;
    out[signal] = {
      signal,
      mean: computeMetricDelta(base.mean, curr.mean),
      rms: computeMetricDelta(base.rms, curr.rms),
      pkpk: computeMetricDelta(base.pkpk, curr.pkpk),
      min: computeMetricDelta(base.min, curr.min),
      max: computeMetricDelta(base.max, curr.max),
    };
  }
  return out;
}

/**
 * Formats a metric delta into a compact signed badge string (e.g. "+0.42 V (+3.5%)").
 */
export function formatDeltaBadge(
  delta: SignalMetricDelta,
  unit: string,
  formatVal: (v: number, u?: string) => string,
): string {
  if (!Number.isFinite(delta.absDelta) || Math.abs(delta.absDelta) < ZERO_EPSILON) {
    return unit ? `±0 ${unit}` : '±0';
  }
  const sign = delta.absDelta > 0 ? '+' : '-';
  const formattedMag = formatVal(Math.abs(delta.absDelta), unit);
  if (delta.pctDelta === null || !Number.isFinite(delta.pctDelta)) {
    return `${sign}${formattedMag}`;
  }
  const pctSign = delta.pctDelta > 0 ? '+' : delta.pctDelta < 0 ? '-' : '';
  const pctStr = `${pctSign}${Math.abs(delta.pctDelta).toFixed(1)}%`;
  return `${sign}${formattedMag} (${pctStr})`;
}

/**
 * Binary-searches the sample index closest to target timestamp `t` (ascending `time`).
 */
export function findNearestTimeIndex(time: number[], t: number): number {
  let low = 0;
  let high = time.length - 1;
  while (low < high) {
    const mid = Math.floor((low + high) / 2);
    if (time[mid] < t) low = mid + 1;
    else high = mid;
  }
  return low;
}

/**
 * Builds a decimated SVG path string for a waveform series over `[minT, maxT]`.
 * Shared across overlay, multi-graph, and stacked baseline/sweep renderers.
 */
export function buildTraceSvgPath(
  time: number[],
  values: number[],
  minT: number,
  maxT: number,
  mapX: (t: number) => number,
  mapY: (v: number) => number,
  maxPoints = 3000,
): string {
  if (time.length === 0 || values.length === 0) return '';
  const i0 = Math.max(0, Math.min(findNearestTimeIndex(time, minT), values.length - 1, time.length - 1));
  const i1 = Math.max(0, Math.min(findNearestTimeIndex(time, maxT), values.length - 1, time.length - 1));
  if (i0 > i1) return '';

  const len = i1 - i0 + 1;
  const step = Math.max(1, Math.floor(len / maxPoints));
  let d = `M ${mapX(time[i0])} ${mapY(values[i0] ?? 0)}`;
  for (let i = i0 + step; i <= i1; i += step) {
    d += ` L ${mapX(time[i])} ${mapY(values[i] ?? 0)}`;
  }
  if ((i1 - i0) % step !== 0) {
    d += ` L ${mapX(time[i1])} ${mapY(values[i1] ?? 0)}`;
  }
  return d;
}
