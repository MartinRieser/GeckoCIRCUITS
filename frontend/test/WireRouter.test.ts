import { describe, expect, it } from 'vitest';
import { routeL, densePoints, orthogonalizePolyline, simplifyCorners, flipRoute, translateWireSegment, routeMovedWire, deconflictMovedWires, denseCellsOf, routingBlockedCells } from '../src/canvas/WireRouter';

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

  it('skips duplicate consecutive points instead of looping forever (crash regression)', () => {
    // Hand-edited .ipes data and collapsed drag segments can contain
    // zero-length steps; the old while-loop hung the renderer on them.
    const dense = densePoints([
      { x: 1, y: 1 },
      { x: 1, y: 1 },
      { x: 3, y: 1 },
      { x: 3, y: 1 },
    ]);
    expect(dense).toEqual([{ x: 1, y: 1 }, { x: 2, y: 1 }, { x: 3, y: 1 }]);
    expect([...denseCellsOf([[1, 1], [1, 1], [3, 1]])].sort()).toEqual(['1,1', '2,1', '3,1']);
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

  it('keeps valid at-least-2-point wire when terminal moves to exact same point as other end', () => {
    const pts = [
      [10, 6],
      [14, 6],
    ];
    // Start moves right by 4, landing exactly at (14, 6)
    const moved = routeMovedWire(pts, { dx: 4, dy: 0 }, { dx: 0, dy: 0 });
    expect(moved.length).toBeGreaterThanOrEqual(2);
    expect(moved[0]).toEqual([14, 6]);
    expect(moved[moved.length - 1]).toEqual([14, 6]);
  });

  it('routes cleanly when moving component past the connected terminal horizontally', () => {
    const pts = [
      [10, 6],
      [14, 6],
    ];
    // Start moves right by 8, landing at (18, 6) past the end at (14, 6)
    const moved = routeMovedWire(pts, { dx: 8, dy: 0 }, { dx: 0, dy: 0 });
    const corners = simplifyCorners(moved);
    expect(corners).toEqual([[18, 6], [14, 6]]);
  });

  it('maintains clean orthogonal Manhattan geometry over consecutive movements', () => {
    const pts = [
      [12, 6],
      [14, 6],
    ];
    // First move: dy = 2
    const m1 = routeMovedWire(pts, { dx: 0, dy: 2 }, { dx: 0, dy: 0 });
    const c1 = simplifyCorners(m1);
    expect(c1).toEqual([[12, 8], [13, 8], [13, 6], [14, 6]]);

    // Second move: dx = 2, dy = 0
    const m2 = routeMovedWire(m1, { dx: 2, dy: 0 }, { dx: 0, dy: 0 });
    const c2 = simplifyCorners(m2);
    // Every segment must be strictly orthogonal
    for (let i = 0; i < c2.length - 1; i++) {
      expect(c2[i][0] === c2[i + 1][0] || c2[i][1] === c2[i + 1][1]).toBe(true);
    }
    // Start must be at (14, 8) and end at (14, 6)
    expect(c2[0]).toEqual([14, 8]);
    expect(c2[c2.length - 1]).toEqual([14, 6]);
  });
});

describe('routingBlockedCells', () => {
  it('terminates on multi-channel scope terminals that sit diagonally off-center (crash regression)', () => {
    // The Multi-Scope RLC repro: dragging any component ran the spoke-blocker
    // over the 2-input scope, whose terminals are offset (-2,-1)/(-2,+1) from
    // the center — the old diagonal walker never landed exactly on them and
    // hung the editor on the first mouse move.
    const scope = {
      type: 1003,
      family: 'CONTROL',
      position: [30, 20],
      orientation: 503,
      inputLabels: ['a', 'b'],
    };
    const blocked = routingBlockedCells([scope]);
    expect(blocked.has('28,19')).toBe(true);
    expect(blocked.has('28,21')).toBe(true);
    expect(blocked.has('30,20')).toBe(true);
  });
});

describe('denseCellsOf', () => {
  it('lists every raster cell an orthogonal polyline covers', () => {
    const cells = denseCellsOf([[2, 1], [4, 1], [4, 3]]);
    expect([...cells].sort()).toEqual(['2,1', '3,1', '4,1', '4,2', '4,3']);
  });

  it('orthogonalizes diagonal input first, matching what is rendered', () => {
    const cells = denseCellsOf([[0, 0], [4, 2]]);
    expect(cells.has('2,0')).toBe(true);
    expect(cells.has('4,2')).toBe(true);
  });
});

describe('deconflictMovedWires', () => {
  // The reported bug, distilled: a source-resistor-capacitor row over a
  // U-shaped bottom rail; dragging the resistor down by 6 slid its wires
  // onto the rail row, so three different nets drew as one line.
  const rail = [[4, 10], [4, 16], [28, 16], [28, 10]];
  const resistor = { type: 1, family: 'LK', position: [16, 16], orientation: 502 };
  const toDense = (route: { x: number; y: number }[]) =>
    densePoints(route).map((p) => [p.x, p.y]);
  // routeMovedWire output for w1 (end followed R's left terminal down by 6)
  const slidW1 = toDense([
    { x: 8, y: 10 }, { x: 12, y: 10 }, { x: 12, y: 16 }, { x: 14, y: 16 },
  ]);
  // ...and for w2 (start followed R's right terminal down by 6)
  const slidW2 = toDense([
    { x: 18, y: 16 }, { x: 20, y: 16 }, { x: 20, y: 10 }, { x: 24, y: 10 },
  ]);

  it('re-routes a slid wire that runs along a foreign wire', () => {
    const [fixed] = deconflictMovedWires([slidW1], [rail], [resistor]);
    const railCells = denseCellsOf(rail);
    const shared = [...denseCellsOf(fixed)].filter((c) => railCells.has(c));
    // Only the pinned endpoint may touch the rail — the approach must come
    // from a free lane, not along it.
    expect(shared).toEqual(['14,16']);
    const corners = simplifyCorners(fixed);
    expect(corners[0]).toEqual([8, 10]);
    expect(corners[corners.length - 1]).toEqual([14, 16]);
  });

  it('re-routes a wire slid onto the rail from the other side as well', () => {
    const [fixed] = deconflictMovedWires([slidW2], [rail], [resistor]);
    const railCells = denseCellsOf(rail);
    const shared = [...denseCellsOf(fixed)].filter((c) => railCells.has(c));
    expect(shared).toEqual(['18,16']);
    expect(simplifyCorners(fixed)[simplifyCorners(fixed).length - 1]).toEqual([24, 10]);
  });

  it('deconflicts two moved wires against each other too', () => {
    const [fixed1, fixed2] = deconflictMovedWires([slidW1, slidW2], [rail], [resistor]);
    const cells1 = denseCellsOf(fixed1);
    const cells2 = denseCellsOf(fixed2);
    const shared = [...cells1].filter((c) => cells2.has(c));
    expect(shared).toEqual([]);
  });

  it('returns a clean slid route unchanged (shape memory)', () => {
    const clean = toDense([{ x: 2, y: 2 }, { x: 6, y: 2 }, { x: 6, y: 5 }]);
    const [out] = deconflictMovedWires([clean], [rail], [resistor]);
    expect(out).toBe(clean);
  });

  it('keeps the slid route when every candidate lane is blocked', () => {
    const slid = toDense([{ x: 10, y: 10 }, { x: 14, y: 10 }]);
    // Rows covering every Z-detour lane the router knows about
    const walls = [2, 5, 7, 8, 9, 10, 11, 12, 13, 15, 18].map((y) => [[0, y], [20, y]]);
    const [out] = deconflictMovedWires([slid], walls, []);
    expect(out).toBe(slid);
  });

  it('leaves legacy diagonal wires alone', () => {
    const diagonal = [[12, 13], [34, 11]];
    const [out] = deconflictMovedWires([diagonal], [], [{ ...resistor, position: [14, 13] }]);
    expect(out).toBe(diagonal);
  });
});

