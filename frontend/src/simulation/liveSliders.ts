/**
 * Pure utilities for Live Parameter Sliders ("What-If" Bar):
 * range derivation, linear/log slider coordinate mapping, and clean engineering rounding.
 */

export const MAX_PINNED_SLIDERS = 4;
export const SLIDER_STEPS = 1000;

export interface PinnedSlider {
  /** Unique ID: `${componentName}.${paramKey}` */
  id: string;
  componentName: string;
  paramKey: string;
  label: string;
  unit: string;
  /** Value captured when pinned, used by the Reset (↺) button. */
  initialValue: number;
  min: number;
  max: number;
  scale: 'linear' | 'log';
}

/**
 * Derives sensible default slider bounds and scale around a current numeric parameter value.
 */
export function deriveDefaultSliderBounds(
  value: number,
  defMin?: number,
  defMax?: number,
): { min: number; max: number; scale: 'linear' | 'log' } {
  if (!Number.isFinite(value)) {
    return { min: defMin ?? 0, max: defMax ?? 10, scale: 'linear' };
  }

  if (value > 0) {
    let min = defMin !== undefined ? Math.max(defMin, value * 0.1) : value * 0.1;
    let max = defMax !== undefined ? Math.min(defMax, value * 3.0) : value * 3.0;
    if (min <= 0) min = Math.max(value * 0.1, 1e-15);
    if (max <= min) max = min * 10;
    const scale = min > 0 && max / min >= 20 ? 'log' : 'linear';
    return { min, max, scale };
  }

  if (value === 0) {
    const min = defMin ?? 0;
    const max = defMax !== undefined && defMax > min ? defMax : min + 10;
    return { min, max, scale: 'linear' };
  }

  // Negative value
  const min = defMin !== undefined ? Math.max(defMin, value * 2) : value * 2;
  const max = defMax !== undefined && defMax > min ? Math.min(defMax, 0) : 0;
  return {
    min,
    max: max > min ? max : min + Math.abs(value),
    scale: 'linear',
  };
}

/**
 * Maps an engineering value into `[0, SLIDER_STEPS]` along linear or logarithmic scale.
 */
export function valueToSliderPosition(
  value: number,
  min: number,
  max: number,
  scale: 'linear' | 'log',
): number {
  if (!Number.isFinite(value) || !Number.isFinite(min) || !Number.isFinite(max) || max <= min) {
    return 0;
  }
  const clamped = Math.max(min, Math.min(max, value));
  if (scale === 'log' && min > 0 && max > 0) {
    const logMin = Math.log10(min);
    const logMax = Math.log10(max);
    const ratio = (Math.log10(clamped) - logMin) / (logMax - logMin || 1);
    return Math.round(Math.max(0, Math.min(1, ratio)) * SLIDER_STEPS);
  }
  const ratio = (clamped - min) / (max - min);
  return Math.round(Math.max(0, Math.min(1, ratio)) * SLIDER_STEPS);
}

/**
 * Maps a slider step position in `[0, SLIDER_STEPS]` back to a cleanly rounded
 * 4-significant-figure engineering value.
 */
export function sliderPositionToValue(
  pos: number,
  min: number,
  max: number,
  scale: 'linear' | 'log',
): number {
  if (!Number.isFinite(min) || !Number.isFinite(max) || max <= min) return min;
  const t = Math.max(0, Math.min(SLIDER_STEPS, pos)) / SLIDER_STEPS;
  let raw: number;
  if (scale === 'log' && min > 0 && max > 0) {
    const logMin = Math.log10(min);
    const logMax = Math.log10(max);
    raw = Math.pow(10, logMin + t * (logMax - logMin));
  } else {
    raw = min + t * (max - min);
  }
  if (Math.abs(raw) < 1e-18) return 0;
  return Number(raw.toPrecision(4));
}
