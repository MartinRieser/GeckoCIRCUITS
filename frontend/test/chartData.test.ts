import { describe, it, expect } from 'vitest';
import { mapSimulationResults, computeSignalStats } from '../src/simulation/chartData';

describe('mapSimulationResults', () => {
  it('returns empty chart data for null or empty results', () => {
    expect(mapSimulationResults(null)).toEqual({
      signalNames: [],
      timeArray: [],
      signalStats: {},
    });
    expect(mapSimulationResults({})).toEqual({
      signalNames: [],
      timeArray: [],
      signalStats: {},
    });
  });

  it('splits the time vector from signal traces and keeps insertion order', () => {
    const { signalNames, timeArray } = mapSimulationResults({
      time: [0, 1e-6, 2e-6],
      V_out: [0.0, 0.5, 1.0],
      I_in: [1.0, 1.0, 1.0],
    });

    expect(signalNames).toEqual(['V_out', 'I_in']);
    expect(timeArray).toEqual([0, 1e-6, 2e-6]);
  });

  it('computes min/max/pkpk/rms per signal', () => {
    const { signalStats } = mapSimulationResults({
      time: [0, 1, 2, 3],
      sine: [0, 1, 0, -1],
      const: [2, 2, 2, 2],
    });

    expect(signalStats['sine']).toEqual({
      min: -1,
      max: 1,
      pkpk: 2,
      mean: 0,
      rms: Math.sqrt(0.5),
    });
    expect(signalStats['const']).toMatchObject({ min: 2, max: 2, pkpk: 0, rms: 2 });
  });

  it('skips statistics for empty signal arrays', () => {
    const { signalStats } = mapSimulationResults({
      time: [0],
      ghost: [],
    });

    expect(signalStats['ghost']).toBeUndefined();
    expect(computeSignalStats([])).toBeNull();
  });
});
