/**
 * Shared resolution of the effective simulation run settings.
 *
 * Both the top-bar quick run (F5 / ▶ Run) and the Simulation Configuration
 * Modal derive their parameters from this single resolver, so the two can
 * never disagree about which tEnd / dt / solver is currently in effect.
 */
import { formatEngineeringValue, parseEngineeringValue } from '../model/componentSchema';
import type { SimulationDefaults, SimRunSettings } from '../model/types';

/** Built-in duration fallback used when neither settings nor defaults exist. */
export const DEFAULT_T_END_STR = '20m';

/** Built-in time-step fallback used when neither settings nor defaults exist. */
export const DEFAULT_DT_STR = '1u';

/** Built-in solver fallback used when neither settings nor defaults exist. */
export const DEFAULT_SOLVER = 'backward-euler';

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
      ? formatEngineeringValue(defaults.duration, 's')
      : DEFAULT_T_END_STR);
  const dtStr =
    settings?.dt ??
    (defaults?.timeStep !== undefined
      ? formatEngineeringValue(defaults.timeStep, 's')
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
