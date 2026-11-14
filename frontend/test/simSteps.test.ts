import { describe, expect, it } from 'vitest';
import { estimateStepCount, isHighStepCount, STEP_WARNING_THRESHOLD } from '../src/simulation/simSteps';

describe('estimateStepCount', () => {
  it('estimates steps from tEnd and dt', () => {
    expect(estimateStepCount(0.02, 1e-6)).toBe(20_000);
    expect(estimateStepCount(1, 0.5)).toBe(2);
  });

  it('returns 0 for a non-positive step width', () => {
    expect(estimateStepCount(0.02, 0)).toBe(0);
    expect(estimateStepCount(0.02, -1e-6)).toBe(0);
  });

  it('rounds fractional step counts', () => {
    expect(estimateStepCount(1, 0.3)).toBe(3);
  });

  it('flags only very large multi-million-step simulations (>= 10M steps)', () => {
    expect(STEP_WARNING_THRESHOLD).toBe(10_000_000);
    expect(isHighStepCount(600_000)).toBe(false);
    expect(isHighStepCount(2_000_000)).toBe(false);
    expect(isHighStepCount(10_000_000)).toBe(true);
  });
});
