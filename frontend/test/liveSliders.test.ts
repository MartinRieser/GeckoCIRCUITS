import { describe, expect, it } from 'vitest';
import {
  deriveDefaultSliderBounds,
  MAX_PINNED_SLIDERS,
  sliderPositionToValue,
  SLIDER_STEPS,
  valueToSliderPosition,
} from '../src/simulation/liveSliders';

describe('liveSliders utilities', () => {
  it('enforces max pinned sliders constant of 4', () => {
    expect(MAX_PINNED_SLIDERS).toBe(4);
  });

  it('derives sensible bounds for positive, zero, and negative values', () => {
    const pos = deriveDefaultSliderBounds(100e-6);
    expect(pos.min).toBeCloseTo(10e-6, 10);
    expect(pos.max).toBeCloseTo(300e-6, 10);
    expect(pos.scale).toBe('log');

    const zero = deriveDefaultSliderBounds(0);
    expect(zero.min).toBe(0);
    expect(zero.max).toBe(10);
    expect(zero.scale).toBe('linear');

    const neg = deriveDefaultSliderBounds(-5);
    expect(neg.min).toBe(-10);
    expect(neg.max).toBe(0);
    expect(neg.scale).toBe('linear');
  });

  it('round-trips linear and logarithmic slider positions with clean precision', () => {
    const linPos = valueToSliderPosition(25, 0, 100, 'linear');
    expect(linPos).toBe(250);
    expect(sliderPositionToValue(linPos, 0, 100, 'linear')).toBe(25);

    const logMid = valueToSliderPosition(100, 10, 1000, 'log');
    expect(logMid).toBe(SLIDER_STEPS / 2);
    expect(sliderPositionToValue(logMid, 10, 1000, 'log')).toBe(100);

    // Clamps out-of-range inputs cleanly
    expect(valueToSliderPosition(-50, 10, 100, 'linear')).toBe(0);
    expect(valueToSliderPosition(999, 10, 100, 'linear')).toBe(SLIDER_STEPS);
  });
});
