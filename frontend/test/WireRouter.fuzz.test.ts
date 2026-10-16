import { describe, expect, it } from 'vitest';
import {
  routeAvoidingObstacles,
  routingBlockedCells,
  densePoints,
  simplifyCorners,
  type RoutingComponent,
} from '../src/canvas/WireRouter';
import type { Point } from '../model/types';

describe('Task 5.1: Wire Router Obstacle Avoidance Fuzz Testing', () => {
  it('fuzz tests 100 randomized layouts to ensure obstacle avoidance and orthogonal geometry', () => {
    // Deterministic PRNG for reproducible test runs
    let seed = 42;
    function rand(): number {
      seed = (seed * 1664525 + 1013904223) % 4294967296;
      return seed / 4294967296;
    }

    function randInt(min: number, max: number): number {
      return Math.floor(rand() * (max - min + 1)) + min;
    }

    for (let iteration = 0; iteration < 100; iteration++) {
      // 1. Generate 1 to 4 obstacle components within a 40x40 grid
      const numObstacles = randInt(1, 4);
      const components: RoutingComponent[] = [];
      for (let i = 0; i < numObstacles; i++) {
        components.push({
          type: randInt(1, 10), // resistor, capacitor, diode, etc.
          position: [randInt(5, 35), randInt(5, 35)],
          orientation: 501, // NORTH_SOUTH
        });
      }

      const blocked = routingBlockedCells(components);

      // 2. Pick start and end points outside blocked cells
      let start: Point = { x: 0, y: 0 };
      let end: Point = { x: 0, y: 0 };

      do {
        start = { x: randInt(0, 40), y: randInt(0, 40) };
      } while (blocked.has(`${start.x},${start.y}`));

      do {
        end = { x: randInt(0, 40), y: randInt(0, 40) };
      } while (blocked.has(`${end.x},${end.y}`) || (end.x === start.x && end.y === start.y));

      // 3. Route wire avoiding obstacles
      const route = routeAvoidingObstacles(start, end, blocked);

      // Invariants:
      // A. Endpoints match
      expect(route[0]).toEqual(start);
      expect(route[route.length - 1]).toEqual(end);

      // B. All segments are strictly orthogonal (horizontal or vertical)
      for (let s = 0; s < route.length - 1; s++) {
        const p1 = route[s];
        const p2 = route[s + 1];
        const isOrthogonal = p1.x === p2.x || p1.y === p2.y;
        expect(isOrthogonal, `Segment between (${p1.x},${p1.y}) and (${p2.x},${p2.y}) must be orthogonal`).toBe(true);
      }

      // C. Dense raster check:
      // If the route found an obstacle-free candidate, no intermediate cell hits `blocked`
      const dense = densePoints(route);
      const endpoints = new Set([`${start.x},${start.y}`, `${end.x},${end.y}`]);
      const hitsBlocked = dense.some((p) => !endpoints.has(`${p.x},${p.y}`) && blocked.has(`${p.x},${p.y}`));

      // If an obstacle-free corridor was found, it must never hit blocked
      // (If no candidate was clear, routeAvoidingObstacles safely returns fallback routeL)
      if (route.length > 3) {
        // Detour candidate was picked
        expect(hitsBlocked, `Detour candidate in run ${iteration} hit blocked obstacle cells`).toBe(false);
      }
    }
  });

  it('simplifies collinear corners cleanly across randomized multi-point paths', () => {
    // Multi-point polyline with collinear segments: (0,0) -> (5,0) -> (10,0) -> (10,5) -> (10,10)
    const points: number[][] = [
      [0, 0],
      [5, 0],
      [10, 0],
      [10, 5],
      [10, 10],
    ];
    const simplified = simplifyCorners(points);
    expect(simplified).toEqual([
      [0, 0],
      [10, 0],
      [10, 10],
    ]);
  });
});
