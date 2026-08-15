import { describe, expect, it } from 'vitest';
import { routeL } from '../src/canvas/WireRouter';

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
});
