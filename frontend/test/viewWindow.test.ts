import { describe, expect, it } from 'vitest';
import {
  clampWindow,
  effectiveWindow,
  fullWindow,
  panWindow,
  zoomWindow,
} from '../src/simulation/viewWindow';

describe('view window math', () => {
  it('effectiveWindow falls back to the full data range', () => {
    expect(effectiveWindow(null, 0, 0.02)).toEqual({ start: 0, end: 0.02 });
  });

  it('effectiveWindow guards degenerate data ranges', () => {
    expect(effectiveWindow(null, 0, 0)).toEqual({ start: 0, end: 1 });
  });

  it('zoom keeps the anchor fixed and clamps to the data range', () => {
    const w = { start: 0.004, end: 0.006 };
    const zoomedIn = zoomWindow(w, 0.5, 0.005, 0, 0.02);
    expect(zoomedIn.start).toBeCloseTo(0.0045, 12);
    expect(zoomedIn.end).toBeCloseTo(0.0055, 12);

    // zooming out far beyond the data clamps at the data edges
    const zoomedOut = zoomWindow(w, 100, 0.005, 0, 0.02);
    expect(zoomedOut).toEqual({ start: 0, end: 0.02 });
  });

  it('zoom never collapses below MIN_SPAN', () => {
    const w = { start: 0.005, end: 0.005 + 1e-13 };
    const zoomed = zoomWindow(w, 0.0001, 0.005, 0, 0.02);
    expect(zoomed.end - zoomed.start).toBeGreaterThanOrEqual(1e-12);
  });

  it('pan shifts and clamps', () => {
    const w = { start: 0, end: 0.005 };
    expect(panWindow(w, 0.002, 0, 0.02)).toEqual({ start: 0.002, end: 0.007 });
    const pinned = panWindow(w, 0.05, 0, 0.02);
    expect(pinned.start).toBeCloseTo(0.015, 12);
    expect(pinned.end).toBeCloseTo(0.02, 12);
    const left = panWindow(w, -0.05, 0, 0.02);
    expect(left.start).toBeCloseTo(0, 12);
    expect(left.end).toBeCloseTo(0.005, 12);
  });

  it('clampWindow pulls out-of-range windows back', () => {
    const clamped = clampWindow({ start: -0.01, end: 0.4 }, 0, 0.02);
    expect(clamped.start).toBeCloseTo(0, 12);
    expect(clamped.end).toBeCloseTo(0.02, 12);
  });

  it('fullWindow guards degenerate ranges', () => {
    expect(fullWindow(5, 5)).toEqual({ start: 5, end: 6 });
  });
});
