import { describe, expect, it } from 'vitest';
import { routeL, densePoints } from '../src/canvas/WireRouter';

describe('routeL', () => {
  it('returns straight line for aligned points', () => {
    expect(routeL({ x: 10, y: 20 }, { x: 30, y: 20 })).toEqual([
      { x: 10, y: 20 },
      { x: 30, y: 20 },
    ]);
    expect(routeL({ x: 10, y: 20 }, { x: 10, y: 40 })).toEqual([
      { x: 10, y: 20 },
      { x: 10, y: 40 },
    ]);
  });

  it('routes dominant-x drags horizontally first', () => {
    expect(routeL({ x: 10, y: 20 }, { x: 20, y: 25 })).toEqual([
      { x: 10, y: 20 },
      { x: 20, y: 20 },
      { x: 20, y: 25 },
    ]);
  });

  it('routes dominant-y drags vertically first', () => {
    expect(routeL({ x: 10, y: 20 }, { x: 12, y: 40 })).toEqual([
      { x: 10, y: 20 },
      { x: 10, y: 40 },
      { x: 12, y: 40 },
    ]);
  });

  it('handles diagonal as horizontal-first (dx >= dy)', () => {
    expect(routeL({ x: 0, y: 0 }, { x: 5, y: 5 })).toEqual([
      { x: 0, y: 0 },
      { x: 5, y: 0 },
      { x: 5, y: 5 },
    ]);
  });

  it('degenerate point collapses to single point', () => {
    expect(routeL({ x: 7, y: 7 }, { x: 7, y: 7 })).toEqual([{ x: 7, y: 7 }]);
  });

  it('sticky preference overrides the dominant-axis heuristic (classic _movementWestEast)', () => {
    // |dy| > |dx| would route vertically first, but the drag started horizontally
    expect(routeL({ x: 10, y: 20 }, { x: 12, y: 40 }, true)).toEqual([
      { x: 10, y: 20 },
      { x: 12, y: 20 },
      { x: 12, y: 40 },
    ]);
    expect(routeL({ x: 10, y: 20 }, { x: 20, y: 25 }, false)).toEqual([
      { x: 10, y: 20 },
      { x: 10, y: 25 },
      { x: 20, y: 25 },
    ]);
  });
});

describe('densePoints', () => {
  it('expands an L-route into one point per raster step (classic export format)', () => {
    expect(
      densePoints([
        { x: 2, y: 1 },
        { x: 5, y: 1 },
        { x: 5, y: 3 },
      ]),
    ).toEqual([
      { x: 2, y: 1 },
      { x: 3, y: 1 },
      { x: 4, y: 1 },
      { x: 5, y: 1 },
      { x: 5, y: 2 },
      { x: 5, y: 3 },
    ]);
  });

  it('expands backwards and vertical segments', () => {
    expect(
      densePoints([
        { x: 5, y: 5 },
        { x: 3, y: 5 },
        { x: 3, y: 6 },
      ]),
    ).toEqual([
      { x: 5, y: 5 },
      { x: 4, y: 5 },
      { x: 3, y: 5 },
      { x: 3, y: 6 },
    ]);
  });

  it('keeps a straight two-point wire dense so mid-wire taps connect', () => {
    const dense = densePoints([
      { x: 8, y: 10 },
      { x: 12, y: 10 },
    ]);
    expect(dense).toHaveLength(5);
    expect(dense).toContainEqual({ x: 10, y: 10 });
  });
});
