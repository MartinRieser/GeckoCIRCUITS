import { describe, expect, it } from 'vitest';
import { estimateStepCount, STEP_WARNING_THRESHOLD } from '../src/simulation/simSteps';

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

  it('warns above two million steps', () => {
    expect(STEP_WARNING_THRESHOLD).toBe(2_000_000);
  });
});
