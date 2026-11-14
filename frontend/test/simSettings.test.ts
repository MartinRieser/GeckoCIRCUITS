import { describe, it, expect } from 'vitest';
import {
  resolveActiveSimSettings,
  detectMaxCircuitFrequency,
  suggestAutoTimeStep,
  DEFAULT_T_END_STR,
  DEFAULT_DT_STR,
  DEFAULT_SOLVER,
} from '../src/simulation/simSettings';
import { ControlComponentType, LkComponentType } from '../src/model/constants';

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
    expect(resolved.tEndStr).toBe('50m');
    expect(resolved.dtStr).toBe('2u');
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

describe('detectMaxCircuitFrequency & suggestAutoTimeStep', () => {
  it('extracts maximum frequency across named keys and periodic param2 sources', () => {
    const res = detectMaxCircuitFrequency([
      {
        name: 'V_ac',
        type: LkComponentType.VOLTAGE_SOURCE,
        family: 'LK',
        parameters: { param0: 402, param2: 50 },
      },
      {
        name: 'PWM.1',
        type: ControlComponentType.SIGNAL_SOURCE,
        family: 'CONTROL',
        parameters: { param0: 403, param2: '200k' },
      },
      {
        name: 'R.1',
        type: LkComponentType.RESISTOR,
        family: 'LK',
        parameters: { frequency: 1000 },
      },
    ]);

    expect(res.maxFreq).toBe(200_000);
    expect(res.sourceName).toBe('PWM.1');
  });

  it('ignores param2 when source is DC mode', () => {
    const res = detectMaxCircuitFrequency([
      {
        name: 'U.1',
        type: LkComponentType.VOLTAGE_SOURCE,
        family: 'LK',
        parameters: { param0: 401, param2: 100_000 },
      },
    ]);
    expect(res.maxFreq).toBe(0);
    expect(res.sourceName).toBeNull();
  });

  it('suggests ~200 steps/period dt when switching frequency is present, or 2000 steps otherwise', () => {
    const sw = suggestAutoTimeStep(
      [{ name: 'PWM.1', type: ControlComponentType.SIGNAL_SOURCE, parameters: { f_sw: 100_000 } }],
      0.01,
    );
    expect(sw.dt).toBeCloseTo(50e-9);
    expect(sw.dtStr).toBe('50n');

    const dc = suggestAutoTimeStep([], 0.02);
    expect(dc.dt).toBeCloseTo(1e-5);
    expect(dc.dtStr).toBe('10u');
  });
});
