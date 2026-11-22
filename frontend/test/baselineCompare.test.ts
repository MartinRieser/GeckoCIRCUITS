import { describe, expect, it } from 'vitest';
import {
  buildTraceSvgPath,
  compareSignalStats,
  computeMetricDelta,
  createBaselineSnapshot,
  formatDeltaBadge,
} from '../src/simulation/baselineCompare';
import { mapSimulationResults } from '../src/simulation/chartData';
import { formatEngineeringValue } from '../src/model/componentSchema';

describe('baselineCompare utilities', () => {
  it('createBaselineSnapshot deep-clones waveform arrays and computes stats', () => {
    const raw = {
      time: [0, 1e-3, 2e-3],
      V_out: [10, 12, 14],
      I_L: [1, 2, 3],
    };
    const snap = createBaselineSnapshot(raw, 7, 'Initial Run');

    // Mutate input arrays after capture
    raw.V_out[0] = 999;
    raw.time.push(3e-3);

    expect(snap.label).toBe('Initial Run');
    expect(snap.modelVersion).toBe(7);
    expect(snap.results.time).toEqual([0, 1e-3, 2e-3]);
    expect(snap.results.V_out).toEqual([10, 12, 14]);
    expect(snap.stats.V_out.mean).toBeCloseTo(12, 6);
    expect(snap.stats.V_out.pkpk).toBeCloseTo(4, 6);
    expect(snap.stats.I_L.max).toBe(3);
  });

  it('computeMetricDelta handles positive, negative, and near-zero baselines', () => {
    const pos = computeMetricDelta(10, 12.5);
    expect(pos.absDelta).toBeCloseTo(2.5, 8);
    expect(pos.pctDelta).toBeCloseTo(25, 8);

    const neg = computeMetricDelta(-10, -12);
    expect(neg.absDelta).toBeCloseTo(-2, 8);
    expect(neg.pctDelta).toBeCloseTo(-20, 8);

    const zeroBase = computeMetricDelta(0, 5);
    expect(zeroBase.absDelta).toBe(5);
    expect(zeroBase.pctDelta).toBeNull();
  });

  it('compareSignalStats only includes signals present in both baseline and current runs', () => {
    const base = mapSimulationResults({
      time: [0, 1e-3],
      V_out: [10, 10],
      I_old: [2, 2],
    });
    const curr = mapSimulationResults({
      time: [0, 1e-3],
      V_out: [11, 13],
      I_new: [3, 3],
    });

    const cmp = compareSignalStats(base.signalStats, curr.signalStats, ['V_out', 'I_old', 'I_new']);
    expect(Object.keys(cmp)).toEqual(['V_out']);
    expect(cmp.V_out.mean.absDelta).toBeCloseTo(2, 6);
    expect(cmp.V_out.mean.pctDelta).toBeCloseTo(20, 6);
    expect(cmp.V_out.pkpk.absDelta).toBeCloseTo(2, 6);
    expect(cmp.V_out.pkpk.pctDelta).toBeNull();
  });

  it('formatDeltaBadge renders signed magnitudes and percentages', () => {
    const dPos = computeMetricDelta(10, 10.5);
    expect(formatDeltaBadge(dPos, 'V', formatEngineeringValue)).toBe('+0.5 V (+5.0%)');

    const dNeg = computeMetricDelta(10, 8);
    expect(formatDeltaBadge(dNeg, 'V', formatEngineeringValue)).toBe('-2 V (-20.0%)');

    const dZero = computeMetricDelta(5, 5);
    expect(formatDeltaBadge(dZero, 'A', formatEngineeringValue)).toBe('±0 A');

    const dNullPct = computeMetricDelta(0, 0.25);
    expect(formatDeltaBadge(dNullPct, 'A', formatEngineeringValue)).toBe('+0.25 A');
  });

  it('buildTraceSvgPath maps samples cleanly across differing time arrays', () => {
    const path = buildTraceSvgPath(
      [0, 0.5, 1.0],
      [0, 10, 20],
      0,
      1.0,
      (t) => t * 100,
      (v) => 100 - v,
    );
    expect(path).toBe('M 0 100 L 50 90 L 100 80');
    expect(buildTraceSvgPath([], [1], 0, 1, (t) => t, (v) => v)).toBe('');
  });
});
