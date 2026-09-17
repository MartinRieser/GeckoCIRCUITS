import { describe, expect, it } from 'vitest';
import { routeL, densePoints, orthogonalizePolyline, simplifyCorners, flipRoute, translateWireSegment, routeMovedWire } from '../src/canvas/WireRouter';

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

describe('orthogonalizePolyline', () => {
  it('leaves already-orthogonal segments intact', () => {
    expect(orthogonalizePolyline([[0, 0], [10, 0], [10, 20]])).toEqual([
      [0, 0],
      [10, 0],
      [10, 20],
    ]);
  });

  it('inserts orthogonal elbow into diagonal segment', () => {
    // Dominant horizontal
    expect(orthogonalizePolyline([[0, 0], [20, 10]])).toEqual([
      [0, 0],
      [20, 0],
      [20, 10],
    ]);
    // Dominant vertical
    expect(orthogonalizePolyline([[0, 0], [5, 20]])).toEqual([
      [0, 0],
      [0, 20],
      [5, 20],
    ]);
  });

  it('handles empty and single-point lists gracefully', () => {
    expect(orthogonalizePolyline([])).toEqual([]);
    expect(orthogonalizePolyline([[5, 5]])).toEqual([[5, 5]]);
  });
});

describe('simplifyCorners', () => {
  it('collapses redundant collinear points to corners', () => {
    const raw = [
      [0, 0],
      [1, 0],
      [2, 0],
      [2, 1],
      [2, 2],
    ];
    expect(simplifyCorners(raw)).toEqual([
      [0, 0],
      [2, 0],
      [2, 2],
    ]);
  });
});

describe('flipRoute', () => {
  it('flips an L-route from horizontal-first to vertical-first', () => {
    const route = [
      [10, 10],
      [20, 10],
      [20, 30],
    ];
    expect(flipRoute(route)).toEqual([
      [10, 10],
      [10, 30],
      [20, 30],
    ]);
  });

  it('flips an L-route from vertical-first to horizontal-first', () => {
    const route = [
      [10, 10],
      [10, 30],
      [20, 30],
    ];
    expect(flipRoute(route)).toEqual([
      [10, 10],
      [20, 10],
      [20, 30],
    ]);
  });
});

describe('translateWireSegment', () => {
  it('translates an internal horizontal segment up/down', () => {
    const route = [
      [10, 10],
      [10, 20],
      [30, 20],
      [30, 30],
    ];
    const moved = translateWireSegment(route, 1, { dx: 0, dy: 5 });
    expect(moved).toEqual([
      [10, 10],
      [10, 25],
      [30, 25],
      [30, 30],
    ]);
  });

  it('translates a single horizontal segment into an orthogonal U-shape', () => {
    const route = [
      [10, 10],
      [30, 10],
    ];
    const moved = translateWireSegment(route, 0, { dx: 0, dy: 5 });
    expect(moved).toEqual([
      [10, 10],
      [10, 15],
      [30, 15],
      [30, 10],
    ]);
  });
});

describe('routeMovedWire', () => {
  it('translates rigid wire when both ends move by same delta', () => {
    const pts = [
      [10, 10],
      [20, 10],
      [20, 30],
    ];
    const moved = routeMovedWire(pts, { dx: 5, dy: 8 }, { dx: 5, dy: 8 });
    expect(moved).toEqual([
      [15, 18],
      [25, 18],
      [25, 38],
    ]);
  });

  it('slides preceding corner vertically when horizontal end moves vertically', () => {
    const pts = [
      [10, 10],
      [10, 20],
      [30, 20],
    ];
    // End moves dx=5, dy=10. Last segment was horizontal [10,20]->[30,20].
    // Corner [10,20] should slide vertically by 10 to [10,30].
    // End becomes [35, 30].
    const moved = routeMovedWire(pts, { dx: 0, dy: 0 }, { dx: 5, dy: 10 });
    const corners = simplifyCorners(moved);
    expect(corners).toEqual([
      [10, 10],
      [10, 30],
      [35, 30],
    ]);
  });

  it('routes straight horizontal wire with an orthogonal step when end moves vertically', () => {
    const pts = [
      [4, 4],
      [20, 4],
    ];
    const moved = routeMovedWire(pts, { dx: 0, dy: 0 }, { dx: 10, dy: 12 });
    const corners = simplifyCorners(moved);
    expect(corners.length).toBeGreaterThanOrEqual(3);
    // Every step must be orthogonal
    for (let i = 0; i < corners.length - 1; i++) {
      const p1 = corners[i];
      const p2 = corners[i + 1];
      expect(p1[0] === p2[0] || p1[1] === p2[1]).toBe(true);
    }
    // Starts at (4,4) and ends at (30, 16)
    expect(corners[0]).toEqual([4, 4]);
    expect(corners[corners.length - 1]).toEqual([30, 16]);
  });

  it('separates parallel horizontal wires when moving target component so they do not overlap', () => {
    const wire1 = [
      [4, 4],
      [20, 4],
    ];
    const wire2 = [
      [4, 8],
      [20, 8],
    ];
    const moved1 = routeMovedWire(wire1, { dx: 0, dy: 0 }, { dx: 10, dy: 12 });
    const moved2 = routeMovedWire(wire2, { dx: 0, dy: 0 }, { dx: 10, dy: 12 });

    const c1 = simplifyCorners(moved1);
    const c2 = simplifyCorners(moved2);

    // Wire 1 vertical segment x
    const xTurn1 = c1[1][0];
    // Wire 2 vertical segment x
    const xTurn2 = c2[1][0];

    // The two vertical spine X coordinates must not be the same!
    expect(xTurn1).not.toBe(xTurn2);
  });

  it('handles start terminal moving symmetrically', () => {
    const pts = [
      [20, 4],
      [4, 4],
    ];
    const moved = routeMovedWire(pts, { dx: 10, dy: 12 }, { dx: 0, dy: 0 });
    const corners = simplifyCorners(moved);
    expect(corners[0]).toEqual([30, 16]);
    expect(corners[corners.length - 1]).toEqual([4, 4]);
  });
});

