/**
 * Pure utilities for Parametric Sweep discovery, sweep grid generation,
 * color ramp mapping, and CSV export.
 */
import type { EditorComponent } from '../model/types';
import {
  getComponentMeta,
  isParameterVisible,
  isTextboxComponent,
  resolveParameterDef,
} from '../model/componentSchema';
import { isScopeComponent } from './scopes';
import type { SignalStats } from './chartData';

export type SweepScaleMode = 'linear' | 'log';
export type SweepSummaryMetric = 'mean' | 'rms' | 'pkpk' | 'max' | 'min';

export interface SweepableParameter {
  componentName: string;
  componentDisplayName: string;
  paramKey: string;
  /** Exact dot-notation key sent to backend ParameterOverrideApplicator: `${componentName}.${paramKey}` */
  parameterPath: string;
  label: string;
  unit: string;
  currentValue: number;
  min?: number;
  max?: number;
}

export interface SweepRunResult {
  index: number;
  simulationId: string;
  parameterValue: number;
  status: 'COMPLETED' | 'FAILED';
  errorMessage?: string;
  results?: Record<string, number[]>;
  stats: Record<string, SignalStats>;
}

export interface SweepSession {
  batchId: string;
  parameterPath: string;
  componentName: string;
  paramKey: string;
  paramLabel: string;
  unit: string;
  mode: SweepScaleMode;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  progress: number;
  totalRuns: number;
  completedRuns: number;
  runs: SweepRunResult[];
  errorMessage?: string;
}

const NON_SWEEPABLE_KEYS = new Set([
  'anzXIN',
  'anzYOUT',
  'width',
  'height',
  'sourceCode',
  'staticCode',
  'staticVariables',
  'text',
  'libraryPath',
  'coupledComponent',
]);

/**
 * Discovers all visible numeric, non-enum component parameters suitable for
 * parametric sweeps or live sliders.
 */
export function listSweepableParameters(components: EditorComponent[]): SweepableParameter[] {
  const result: SweepableParameter[] = [];

  for (const comp of components || []) {
    if (isTextboxComponent(comp) || isScopeComponent(comp)) continue;
    const meta = getComponentMeta(comp.type, comp.family, comp.name);
    if (meta.disabled) continue;

    for (const def of meta.parameters) {
      if (!isParameterVisible(def, comp.parameters)) continue;
      const resolved = resolveParameterDef(def, comp.parameters);
      if (resolved.options && resolved.options.length > 0) continue;
      if (NON_SWEEPABLE_KEYS.has(resolved.key)) continue;
      if (typeof resolved.defaultValue !== 'number') continue;

      const raw = comp.parameters?.[resolved.key] ?? resolved.defaultValue;
      const numVal = typeof raw === 'number' ? raw : Number(raw);
      if (!Number.isFinite(numVal) || raw === '') continue;

      result.push({
        componentName: comp.name,
        componentDisplayName: meta.displayName,
        paramKey: resolved.key,
        parameterPath: `${comp.name}.${resolved.key}`,
        label: resolved.label,
        unit: resolved.unit ?? '',
        currentValue: numVal,
        min: resolved.min,
        max: resolved.max,
      });
    }
  }

  return result;
}

/**
 * Generates exact sweep values matching backend SimulationService.expandParameterSets.
 */
export function generateSweepValues(
  mode: SweepScaleMode,
  start: number,
  end: number,
  points: number,
): number[] {
  const n = Math.max(0, Math.floor(points));
  if (n === 0 || !Number.isFinite(start) || !Number.isFinite(end)) return [];
  if (mode === 'log' && (start <= 0 || end <= 0)) return [];
  if (n === 1) return [start];

  const out: number[] = [];
  if (mode === 'linear') {
    for (let i = 0; i < n; i++) {
      out.push(start + (i * (end - start)) / (n - 1));
    }
    return out;
  }

  const logStart = Math.log10(start);
  const logEnd = Math.log10(end);
  for (let i = 0; i < n; i++) {
    out.push(Math.pow(10, logStart + (i * (logEnd - logStart)) / (n - 1)));
  }
  return out;
}

/**
 * Produces an accessible color ramp across sweep steps (cyan -> violet -> rose).
 */
export function sweepRunColor(index: number, total: number, theme: 'dark' | 'light'): string {
  const t = total <= 1 ? 0 : Math.max(0, Math.min(1, index / (total - 1)));
  const hue = Math.round(198 + t * 150);
  const lightness = theme === 'light' ? '42%' : '62%';
  return `hsl(${hue}, 82%, ${lightness})`;
}

/**
 * Formats a parametric sweep summary table as CSV for the selected signal.
 */
export function exportSweepSummaryCsv(session: SweepSession, signal: string): string {
  const unitSuffix = session.unit ? ` (${session.unit})` : '';
  const header = ['run', `${session.paramLabel}${unitSuffix}`, 'status', 'min', 'max', 'pkpk', 'mean', 'rms'].join(',');
  const rows = session.runs.map((run) => {
    const st = run.stats[signal];
    return [
      String(run.index + 1),
      String(run.parameterValue),
      run.status,
      st ? String(st.min) : '',
      st ? String(st.max) : '',
      st ? String(st.pkpk) : '',
      st ? String(st.mean) : '',
      st ? String(st.rms) : '',
    ].join(',');
  });
  return [header, ...rows].join('\n');
}
