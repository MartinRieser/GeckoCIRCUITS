import { describe, it, expect } from 'vitest';
import {
  resolveActiveSimSettings,
  DEFAULT_T_END_STR,
  DEFAULT_DT_STR,
  DEFAULT_SOLVER,
} from '../src/simulation/simSettings';

describe('resolveActiveSimSettings', () => {
  it('falls back to built-in defaults when nothing is provided', () => {
    const resolved = resolveActiveSimSettings(null, null);
    expect(resolved.tEndStr).toBe(DEFAULT_T_END_STR);
    expect(resolved.dtStr).toBe(DEFAULT_DT_STR);
    expect(resolved.solver).toBe(DEFAULT_SOLVER);
    expect(resolved.tEnd).toBeCloseTo(0.02);
    expect(resolved.dt).toBeCloseTo(1e-6);
  });

  it('prefers user settings over engine defaults', () => {
    const resolved = resolveActiveSimSettings(
      { duration: 0.02, timeStep: 1e-6, solverType: 'backward-euler', signals: [] },
      { tEnd: '100m', dt: '500n', solver: 'trapezoidal' },
    );
    expect(resolved.tEndStr).toBe('100m');
    expect(resolved.dtStr).toBe('500n');
    expect(resolved.solver).toBe('trapezoidal');
    expect(resolved.tEnd).toBeCloseTo(0.1);
    expect(resolved.dt).toBeCloseTo(500e-9);
  });

  it('formats engine defaults when no user settings exist', () => {
    const resolved = resolveActiveSimSettings(
      { duration: 0.05, timeStep: 2e-6, solverType: 'gear-shichman', signals: [] },
      null,
    );
    expect(resolved.tEndStr).toBe('50 m s');
    expect(resolved.dtStr).toBe('2 µ s');
    expect(resolved.solver).toBe('gear-shichman');
    expect(resolved.tEnd).toBeCloseTo(0.05);
    expect(resolved.dt).toBeCloseTo(2e-6);
  });

  it('falls back to built-in values for unparseable settings strings', () => {
    const resolved = resolveActiveSimSettings(null, { tEnd: 'not-a-time', dt: '??', solver: '' });
    expect(resolved.tEnd).toBeCloseTo(0.02);
    expect(resolved.dt).toBeCloseTo(1e-6);
    expect(resolved.solver).toBe('');
  });
});
