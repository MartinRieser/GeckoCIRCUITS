/**
 * Shared resolution of the effective simulation run settings.
 *
 * Both the top-bar quick run (F5 / ▶ Run) and the Simulation Configuration
 * Modal derive their parameters from this single resolver, so the two can
 * never disagree about which tEnd / dt / solver is currently in effect.
 */
import { formatEngineeringValue, parseEngineeringValue } from '../model/componentSchema';
import { ControlComponentType, LkComponentType } from '../model/constants';
import type { SimulationDefaults, SimRunSettings } from '../model/types';

/** Built-in duration fallback used when neither settings nor defaults exist. */
export const DEFAULT_T_END_STR = '20m';

/** Built-in time-step fallback used when neither settings nor defaults exist. */
export const DEFAULT_DT_STR = '1u';

/** Built-in solver fallback used when neither settings nor defaults exist. */
export const DEFAULT_SOLVER = 'backward-euler';

/** Quick presets for inline time-step selection. */
export const DT_PRESETS = ['10n', '50n', '100n', '500n', '1u', '5u', '10u', '50u', '100u'] as const;

/** Quick presets for inline simulation duration selection. */
export const T_END_PRESETS = ['1m', '5m', '10m', '20m', '50m', '100m', '500m', '1s'] as const;

/** Structural component subset required for frequency inspection. */
export interface FrequencyInspectableComponent {
  /** Component identifier (e.g. `SIGNAL.1`, `U.1`). */
  name: string;
  /** Numeric component type identifier matching backend enum constants. */
  type?: number;
  /** Component domain family (`LK`, `CONTROL`, etc.). */
  family?: string;
  /** Component parameter dictionary searched for frequency keys. */
  parameters?: Record<string, number | string | boolean>;
}

const FREQ_PARAM_KEYS = ['frequency', 'f', 'f_sw', 'fsw', 'freq'] as const;
const PERIODIC_MODES = new Set([402, 403, 404]);

/** Parses a numeric or engineering-string frequency value in Hz, returning `null` if invalid or non-positive. */
function parsePositiveFrequency(raw: unknown): number | null {
  if (typeof raw === 'number') {
    return Number.isFinite(raw) && raw > 0 ? raw : null;
  }
  if (typeof raw === 'string') {
    const parsed = parseEngineeringValue(raw);
    return parsed !== null && Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }
  return null;
}

/** Returns `true` for periodic source blocks whose legacy `param2` encodes switching/signal frequency in Hz. */
function isPeriodicParam2Source(comp: FrequencyInspectableComponent): boolean {
  if (comp.type === undefined) return false;
  const isSourceType =
    comp.type === LkComponentType.VOLTAGE_SOURCE ||
    comp.type === LkComponentType.CURRENT_SOURCE ||
    comp.type === ControlComponentType.SIGNAL_SOURCE ||
    comp.type === ControlComponentType.LEGACY_SIGNAL_SOURCE;
  if (!isSourceType) return false;
  const mode = Number(comp.parameters?.param0);
  return PERIODIC_MODES.has(mode);
}

/**
 * Scans schematic components for switching/source frequencies and returns the
 * highest positive frequency found (in Hz), or 0 if none are configured.
 */
export function detectMaxCircuitFrequency(
  components: FrequencyInspectableComponent[],
): { maxFreq: number; sourceName: string | null } {
  let maxFreq = 0;
  let sourceName: string | null = null;

  for (const comp of components) {
    const params = comp.parameters;
    if (!params) continue;

    for (const key of FREQ_PARAM_KEYS) {
      const freq = parsePositiveFrequency(params[key]);
      if (freq !== null && freq > maxFreq) {
        maxFreq = freq;
        sourceName = comp.name;
      }
    }

    if (isPeriodicParam2Source(comp)) {
      const freq = parsePositiveFrequency(params.param2);
      if (freq !== null && freq > maxFreq) {
        maxFreq = freq;
        sourceName = comp.name;
      }
    }
  }

  return { maxFreq, sourceName };
}

/** Rounds a positive value up to the nearest clean 1–2–5 engineering step (e.g. `100n`, `200n`, `500n`). */
function snapToClean125(raw: number): number {
  if (!Number.isFinite(raw) || raw <= 0) return 1e-6;
  const clamped = Math.max(raw, 1e-12);
  const exp = Math.floor(Math.log10(clamped));
  const base = 10 ** exp;
  const mantissa = clamped / base;
  const snapped =
    mantissa <= 1.5 ? 1 : mantissa <= 3.5 ? 2 : mantissa <= 7.5 ? 5 : 10;
  return Number((snapped * base).toPrecision(3));
}

/**
 * Formats a positive time value in seconds into a compact SPICE/engineering
 * token without spaces or trailing unit (e.g. `2e-6` -> `'2u'`, `0.04` -> `'40m'`).
 */
export function formatCompactTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) return DEFAULT_DT_STR;
  const tiers: Array<{ scale: number; suffix: string }> = [
    { scale: 1, suffix: 's' },
    { scale: 1e-3, suffix: 'm' },
    { scale: 1e-6, suffix: 'u' },
    { scale: 1e-9, suffix: 'n' },
    { scale: 1e-12, suffix: 'p' },
  ];
  for (const { scale, suffix } of tiers) {
    if (seconds >= scale * 0.999999) {
      const scaled = Number((seconds / scale).toPrecision(4));
      return `${scaled}${suffix}`;
    }
  }
  const scaled = Number((seconds / 1e-12).toPrecision(4));
  return `${scaled}p`;
}

/**
 * Suggests an appropriate simulation time step (dt) based on the fastest
 * circuit frequency (targeting ~200 steps per switching period) and total duration.
 */
export function suggestAutoTimeStep(
  components: FrequencyInspectableComponent[],
  tEnd: number,
): { dt: number; dtStr: string; reason: string } {
  const safeTEnd = Number.isFinite(tEnd) && tEnd > 0 ? tEnd : 0.02;
  const { maxFreq, sourceName } = detectMaxCircuitFrequency(components);

  let rawDt: number;
  let reason: string;
  if (maxFreq > 0) {
    rawDt = 1 / (200 * maxFreq);
    reason = `200 pts/period for ${formatEngineeringValue(maxFreq, 'Hz')}${sourceName ? ` (${sourceName})` : ''}`;
  } else {
    rawDt = safeTEnd / 2000;
    reason = `2,000 steps across ${formatEngineeringValue(safeTEnd, 's')}`;
  }

  let dt = snapToClean125(rawDt);
  const maxAllowedDt = safeTEnd / 500;
  if (dt > maxAllowedDt) {
    dt = snapToClean125(maxAllowedDt);
  }

  return {
    dt,
    dtStr: formatCompactTime(dt),
    reason,
  };
}

/** Fully resolved simulation run settings in both string and numeric form. */
export interface ResolvedSimSettings {
  /** Effective duration in its displayed engineering-notation string form. */
  tEndStr: string;
  /** Effective time step in its displayed engineering-notation string form. */
  dtStr: string;
  /** Effective solver integration method. */
  solver: string;
  /** Parsed duration in seconds (never null; falls back to DEFAULT_T_END_STR). */
  tEnd: number;
  /** Parsed time step in seconds (never null; falls back to DEFAULT_DT_STR). */
  dt: number;
}

/**
 * Resolves the effective simulation settings with the precedence
 * user settings > engine defaults > built-in fallbacks, and parses the
 * resulting strings once. Invalid strings fall back to the built-in defaults
 * so a quick run never executes with arbitrary hard-coded numbers.
 */
export function resolveActiveSimSettings(
  defaults?: SimulationDefaults | null,
  settings?: SimRunSettings | null,
): ResolvedSimSettings {
  const tEndStr =
    settings?.tEnd ??
    (defaults?.duration !== undefined
      ? formatCompactTime(defaults.duration)
      : DEFAULT_T_END_STR);
  const dtStr =
    settings?.dt ??
    (defaults?.timeStep !== undefined
      ? formatCompactTime(defaults.timeStep)
      : DEFAULT_DT_STR);
  const solver = settings?.solver ?? defaults?.solverType ?? DEFAULT_SOLVER;
  return {
    tEndStr,
    dtStr,
    solver,
    tEnd: parseEngineeringValue(tEndStr) ?? parseEngineeringValue(DEFAULT_T_END_STR) ?? 0.02,
    dt: parseEngineeringValue(dtStr) ?? parseEngineeringValue(DEFAULT_DT_STR) ?? 1e-6,
  };
}
