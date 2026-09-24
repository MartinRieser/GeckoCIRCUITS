/**
 * Orthogonal wire routing, ported from the Swing editor's L-router
 * (Connection.setCurrentPointOnConnection / moveHorizontal / moveVertical).
 *
 * The classic router stores one point per raster step along the run, which is
 * also the connectivity semantics of .ipes files: anything touching a listed
 * raster point is connected. `routeL` returns the corner form used for
 * previewing; `densePoints` expands it to the classic per-step form that is
 * sent to the server when the wire is committed.
 */
import type { Point } from '../model/types';
import { terminalPositions } from '../model/geometry';

export function routeL(start: Point, end: Point, preferHorizontal: boolean | null = null): Point[] {
  if (start.x === end.x && start.y === end.y) {
    return [start];
  }
  if (start.x === end.x || start.y === end.y) {
    return [start, end];
  }
  const horizontalFirst =
    preferHorizontal ?? Math.abs(end.x - start.x) >= Math.abs(end.y - start.y);
  if (horizontalFirst) {
    return [start, { x: end.x, y: start.y }, end];
  }
  return [start, { x: start.x, y: end.y }, end];
}

/** Grid cell key used by the obstacle-aware router. */
function cellKey(p: Point): string {
  return `${p.x},${p.y}`;
}

/** Minimal component shape needed by the obstacle router. */
export interface RoutingComponent {
  type: number;
  family?: string;
  position: number[];
  orientation: number;
  inputLabels?: string[];
  outputLabels?: string[];
  inputs?: unknown[];
  parameters?: Record<string, number | string | boolean>;
}

/**
 * Computes the grid cells a wire must not cross: every component's body cells
 * and terminal cells. Passing through any of these would visually overlay the
 * component and, per .ipes connectivity semantics, silently short its pins.
 */
export function routingBlockedCells(components: RoutingComponent[]): Set<string> {
  const blocked = new Set<string>();
  for (const c of components) {
    const center = { x: c.position[0], y: c.position[1] };
    // Block an L-shaped spoke between the component center and each terminal.
    // Multi-channel pins (scopes, function blocks, 4-pin transformers) sit
    // DIAGONALLY off the center; the naive diagonal walk never lands exactly
    // on such a terminal and hangs, so walk each axis monotonically instead.
    const markSpoke = (term: Point) => {
      let x = center.x;
      let y = center.y;
      const walkX = () => {
        while (x !== term.x) {
          x += Math.sign(term.x - x);
          blocked.add(cellKey({ x, y }));
        }
      };
      const walkY = () => {
        while (y !== term.y) {
          y += Math.sign(term.y - y);
          blocked.add(cellKey({ x, y }));
        }
      };
      if (Math.abs(term.x - center.x) >= Math.abs(term.y - center.y)) {
        walkX();
        walkY();
      } else {
        walkY();
        walkX();
      }
      blocked.add(cellKey(term));
    };
    const t = terminalPositions(c);
    t.input.forEach(markSpoke);
    t.output.forEach(markSpoke);
    blocked.add(cellKey(center));
  }
  return blocked;
}

/** Stepped detour offsets (in grid units) tested when routing around obstacles. */
const ROUTE_DETOUR_OFFSETS = [1, -1, 2, -2, 3, -3, 5, -5, 8, -8] as const;

/**
 * Ordered route candidates between two endpoints: both L orientations, then
 * stepped Z detours that leave the direct band. Shared by the interactive
 * obstacle router and the post-move deconfliction pass.
 */
function routeCandidates(start: Point, end: Point, preferHorizontal: boolean | null = null): Point[][] {
  const candidates: Point[][] = [];
  if (preferHorizontal !== null) {
    candidates.push(routeL(start, end, preferHorizontal));
  }
  candidates.push(routeL(start, end, true));
  candidates.push(routeL(start, end, false));
  for (const off of ROUTE_DETOUR_OFFSETS) {
    candidates.push([start, { x: start.x, y: start.y + off }, { x: end.x, y: start.y + off }, end]);
    candidates.push([start, { x: start.x + off, y: start.y }, { x: start.x + off, y: end.y }, end]);
  }
  return candidates;
}

/**
 * Routes a wire between two endpoints while avoiding blocked cells (component
 * bodies and foreign terminals). Tries both L orientations first, then stepped
 * detours that leave the direct band; falls back to the plain L route when
 * everything is blocked so wiring never becomes impossible.
 */
export function routeAvoidingObstacles(
  start: Point,
  end: Point,
  blocked: ReadonlySet<string>,
  preferHorizontal: boolean | null = null,
): Point[] {
  if (start.x === end.x && start.y === end.y) {
    return [start];
  }
  const endpoints = new Set([cellKey(start), cellKey(end)]);
  const hits = (pts: Point[]) =>
    pts.some((p) => !endpoints.has(cellKey(p)) && blocked.has(cellKey(p)));

  for (const candidate of routeCandidates(start, end, preferHorizontal)) {
    if (candidate.length >= 2 && !hits(densePoints(candidate))) {
      return candidate;
    }
  }
  return routeL(start, end, preferHorizontal);
}

/**
 * Dense raster cells a stored wire polyline covers (orthogonalized first, as
 * rendered), keyed "x,y". Connectivity semantics: anything touching a listed
 * raster point is connected.
 */
export function denseCellsOf(points: number[][]): Set<string> {
  const cells = new Set<string>();
  if (!points || points.length < 2) {
    return cells;
  }
  const corners = simplifyCorners(points);
  for (const p of densePoints(corners.map(([x, y]) => ({ x, y })))) {
    cells.add(cellKey(p));
  }
  return cells;
}

/**
 * Post-move deconfliction: after routeMovedWire slid each wire to follow its
 * terminals, re-check every slid route against component bodies AND every
 * other wire's raster cells — routeMovedWire is purely local and cannot see
 * either, which is how moved wires used to land exactly on top of untouched
 * wires. A slid route that is already clean is returned unchanged (shape
 * memory is preserved); a dirty one is replaced by the first clean L/Z
 * candidate between its (pinned) endpoints. Only the two endpoint cells may
 * touch foreign wires or terminals — that is a legal tap. When no candidate
 * is clean the slid route is kept so a move never becomes impossible; the
 * pre-run validation surfaces the residual overlap instead.
 */
export function deconflictMovedWires(
  slidDenseRoutes: number[][][],
  staticRoutes: number[][][],
  components: RoutingComponent[],
): number[][][] {
  const componentCells = routingBlockedCells(components);
  const staticCells = new Set<string>();
  for (const route of staticRoutes) {
    for (const c of denseCellsOf(route)) {
      staticCells.add(c);
    }
  }

  return slidDenseRoutes.map((slid, i) => {
    if (!slid || slid.length < 2) {
      return slid;
    }
    // Legacy diagonal wires (e.g. rigidly translated fixtures) are left as-is.
    const orthogonal = slid.every(
      (p, k) => k === 0 || p[0] === slid[k - 1][0] || p[1] === slid[k - 1][1],
    );
    if (!orthogonal) {
      return slid;
    }

    const corners = simplifyCorners(slid);
    const start = { x: corners[0][0], y: corners[0][1] };
    const end = { x: corners[corners.length - 1][0], y: corners[corners.length - 1][1] };
    if (start.x === end.x && start.y === end.y) {
      return slid;
    }

    const blocked = new Set<string>(componentCells);
    for (const c of staticCells) {
      blocked.add(c);
    }
    slidDenseRoutes.forEach((other, j) => {
      if (j === i) {
        return;
      }
      for (const c of denseCellsOf(other)) {
        blocked.add(c);
      }
    });

    const endpoints = new Set([cellKey(start), cellKey(end)]);
    const dirty = (pts: Point[]) =>
      pts.some((p) => !endpoints.has(cellKey(p)) && blocked.has(cellKey(p)));
    if (!dirty(densePoints(corners.map(([x, y]) => ({ x, y }))))) {
      return slid;
    }

    for (const candidate of routeCandidates(start, end)) {
      if (candidate.length >= 2 && !dirty(densePoints(candidate))) {
        return densePoints(candidate).map((p) => [p.x, p.y]);
      }
    }
    return slid;
  });
}

/** Expands a corner polyline into the classic dense per-raster-step point list. */
export function densePoints(route: Point[]): Point[] {
  const result: Point[] = [];
  for (let i = 0; i < route.length; i++) {
    const cur = route[i];
    if (i === 0) {
      result.push({ ...cur });
      continue;
    }
    const prev = route[i - 1];
    // Duplicate consecutive corners (hand-edited files, collapsed segments)
    // are zero-length steps: skip instead of looping forever below.
    if (cur.x === prev.x && cur.y === prev.y) {
      continue;
    }
    const dx = Math.sign(cur.x - prev.x);
    const dy = Math.sign(cur.y - prev.y);
    let x = prev.x;
    let y = prev.y;
    while (x !== cur.x || y !== cur.y) {
      x += dx;
      y += dy;
      result.push({ x, y });
    }
  }
  return result;
}

/**
 * Ensures any wire polyline is strictly orthogonal (Manhattan).
 * If any adjacent points form a diagonal line, inserts an orthogonal corner
 * so the rendered wire is always composed of clean horizontal and vertical lines.
 */
export function orthogonalizePolyline(points: number[][]): number[][] {
  if (!points || points.length <= 1) {
    return points || [];
  }
  const result: number[][] = [points[0]];
  for (let i = 1; i < points.length; i++) {
    const prev = result[result.length - 1];
    const cur = points[i];
    if (prev[0] === cur[0] || prev[1] === cur[1]) {
      result.push(cur);
    } else {
      // Diagonal segment: insert orthogonal elbow
      const horizontalFirst = Math.abs(cur[0] - prev[0]) >= Math.abs(cur[1] - prev[1]);
      if (horizontalFirst) {
        result.push([cur[0], prev[1]]);
      } else {
        result.push([prev[0], cur[1]]);
      }
      result.push(cur);
    }
  }
  return result;
}

/**
 * Extracts the primary corner vertices of an orthogonal polyline,
 * collapsing redundant collinear points while preserving all elbows and endpoints.
 */
export function simplifyCorners(rawPoints: number[][]): number[][] {
  const points = orthogonalizePolyline(rawPoints);
  if (!points || points.length <= 2) {
    return points || [];
  }
  const corners: number[][] = [points[0]];
  for (let i = 1; i < points.length - 1; i++) {
    const prev = corners[corners.length - 1];
    const cur = points[i];
    const next = points[i + 1];
    const dx1 = Math.sign(cur[0] - prev[0]);
    const dy1 = Math.sign(cur[1] - prev[1]);
    const dx2 = Math.sign(next[0] - cur[0]);
    const dy2 = Math.sign(next[1] - cur[1]);
    if (dx1 !== dx2 || dy1 !== dy2) {
      corners.push(cur);
    }
  }
  corners.push(points[points.length - 1]);
  return corners;
}

/**
 * Toggles the route orientation of an L-route between horizontal-first and vertical-first.
 */
export function flipRoute(rawPoints: number[][]): number[][] {
  const corners = simplifyCorners(rawPoints);
  if (corners.length !== 3) {
    if (corners.length === 2) {
      const [c0, c1] = corners;
      if (c0[0] !== c1[0] && c0[1] !== c1[1]) {
        return [c0, [c0[0], c1[1]], c1];
      }
    }
    return corners;
  }
  const [c0, c1, c2] = corners;
  const flipped = [c0[0] + c2[0] - c1[0], c0[1] + c2[1] - c1[1]];
  return [c0, flipped, c2];
}

/**
 * Translates a segment of an orthogonal wire, preserving Manhattan geometry.
 */
export function translateWireSegment(
  rawPoints: number[][],
  segmentIndex: number,
  delta: { dx: number; dy: number },
): number[][] {
  const corners = simplifyCorners(rawPoints);
  if (!corners || corners.length < 2 || segmentIndex < 0 || segmentIndex >= corners.length - 1) {
    return corners;
  }
  const c = corners.map((pt) => [...pt]);
  const p1 = c[segmentIndex];
  const p2 = c[segmentIndex + 1];
  const isHorizontal = p1[1] === p2[1];
  const isVertical = p1[0] === p2[0];

  if (isHorizontal) {
    if (delta.dy === 0) return corners;
    const newY = p1[1] + delta.dy;

    // Internal segment
    if (segmentIndex > 0 && segmentIndex < c.length - 2) {
      c[segmentIndex][1] = newY;
      c[segmentIndex + 1][1] = newY;
      return simplifyCorners(c);
    }

    // Single-segment wire
    if (c.length === 2) {
      return simplifyCorners([p1, [p1[0], newY], [p2[0], newY], p2]);
    }

    // First segment of multi-segment wire
    if (segmentIndex === 0) {
      c[segmentIndex + 1][1] = newY;
      return simplifyCorners([p1, [p1[0], newY], ...c.slice(1)]);
    }

    // Last segment of multi-segment wire
    if (segmentIndex === c.length - 2) {
      c[segmentIndex][1] = newY;
      return simplifyCorners([...c.slice(0, segmentIndex + 1), [p2[0], newY], p2]);
    }
  } else if (isVertical) {
    if (delta.dx === 0) return corners;
    const newX = p1[0] + delta.dx;

    // Internal segment
    if (segmentIndex > 0 && segmentIndex < c.length - 2) {
      c[segmentIndex][0] = newX;
      c[segmentIndex + 1][0] = newX;
      return simplifyCorners(c);
    }

    // Single-segment wire
    if (c.length === 2) {
      return simplifyCorners([p1, [newX, p1[1]], [newX, p2[1]], p2]);
    }

    // First segment
    if (segmentIndex === 0) {
      c[segmentIndex + 1][0] = newX;
      return simplifyCorners([p1, [newX, p1[1]], ...c.slice(1)]);
    }

    // Last segment
    if (segmentIndex === c.length - 2) {
      c[segmentIndex][0] = newX;
      return simplifyCorners([...c.slice(0, segmentIndex + 1), [newX, p2[1]], p2]);
    }
  }

  return corners;
}

/**
 * Routes a wire when one or both of its endpoints are moved (e.g. during component drag).
 * Maintains strictly orthogonal Manhattan geometry, sliding adjacent corners along their
 * perpendicular axes rather than introducing diagonal kinks.
 * Returns dense raster points so connectivity and mid-wire taps remain intact.
 */
export function routeMovedWire(
  rawPoints: number[][],
  startDelta: { dx: number; dy: number },
  endDelta: { dx: number; dy: number },
): number[][] {
  if (!rawPoints || rawPoints.length === 0) return rawPoints || [];
  if (rawPoints.length === 1) {
    const p = rawPoints[0];
    return [
      [p[0] + startDelta.dx, p[1] + startDelta.dy],
      [p[0] + startDelta.dx, p[1] + startDelta.dy],
    ];
  }

  // If neither end moved, return original
  if (startDelta.dx === 0 && startDelta.dy === 0 && endDelta.dx === 0 && endDelta.dy === 0) {
    return rawPoints;
  }

  // If both ends moved by the exact same delta (rigid translation of the entire wire):
  if (startDelta.dx === endDelta.dx && startDelta.dy === endDelta.dy) {
    const dx = startDelta.dx;
    const dy = startDelta.dy;
    return rawPoints.map(([x, y]) => [x + dx, y + dy]);
  }

  const corners = simplifyCorners(rawPoints);
  const startPt = corners[0];
  const endPt = corners[corners.length - 1];
  const newStart: [number, number] = [startPt[0] + startDelta.dx, startPt[1] + startDelta.dy];
  const newEnd: [number, number] = [endPt[0] + endDelta.dx, endPt[1] + endDelta.dy];

  // If newStart and newEnd are identical (wire collapsed to 0 length):
  if (newStart[0] === newEnd[0] && newStart[1] === newEnd[1]) {
    return [newStart, newEnd];
  }

  // If wire was a straight 2-point wire originally
  if (corners.length === 2) {
    const wasHorizontal = startPt[1] === endPt[1];
    let routeCorners: number[][];

    if (newStart[1] === newEnd[1] || newStart[0] === newEnd[0]) {
      // Direct straight line
      routeCorners = [newStart, newEnd];
    } else if (wasHorizontal) {
      // Originally horizontal: turn vertically in the middle
      let midX = Math.round((newStart[0] + newEnd[0]) / 2);
      const minX = Math.min(newStart[0], newEnd[0]);
      const maxX = Math.max(newStart[0], newEnd[0]);
      // Small deterministic offset to separate parallel horizontal wires when there is ample room
      if (maxX - minX >= 4) {
        const stagger = (startPt[1] % 8 === 0 ? -1 : 1) * (newEnd[1] > newStart[1] ? 1 : -1);
        const candidateX = midX + stagger;
        if (candidateX > minX && candidateX < maxX) {
          midX = candidateX;
        }
      }
      routeCorners = [newStart, [midX, newStart[1]], [midX, newEnd[1]], newEnd];
    } else {
      // Originally vertical: turn horizontally in the middle
      let midY = Math.round((newStart[1] + newEnd[1]) / 2);
      const minY = Math.min(newStart[1], newEnd[1]);
      const maxY = Math.max(newStart[1], newEnd[1]);
      if (maxY - minY >= 4) {
        const stagger = (startPt[0] % 8 === 0 ? -1 : 1) * (newEnd[0] > newStart[0] ? 1 : -1);
        const candidateY = midY + stagger;
        if (candidateY > minY && candidateY < maxY) {
          midY = candidateY;
        }
      }
      routeCorners = [newStart, [newStart[0], midY], [newEnd[0], midY], newEnd];
    }

    const simplified = simplifyCorners(routeCorners);
    const dense = densePoints(simplified.map(([x, y]) => ({ x, y }))).map((p) => [p.x, p.y]);
    return dense.length >= 2 ? dense : [newStart, newEnd];
  }

  // Multi-segment wire (corners.length >= 3)
  let updatedCorners = corners.map((pt) => [...pt]);

  // Case: only start moves
  if ((startDelta.dx !== 0 || startDelta.dy !== 0) && endDelta.dx === 0 && endDelta.dy === 0) {
    const reversed = [...updatedCorners].reverse();
    const moved = adjustEndpoint(reversed, startDelta);
    updatedCorners = [...moved].reverse();
  }
  // Case: only end moves
  else if ((endDelta.dx !== 0 || endDelta.dy !== 0) && startDelta.dx === 0 && startDelta.dy === 0) {
    updatedCorners = adjustEndpoint(updatedCorners, endDelta);
  }
  // Case: both move by different deltas
  else {
    const reversed = [...updatedCorners].reverse();
    const afterStart = [...adjustEndpoint(reversed, startDelta)].reverse();
    updatedCorners = adjustEndpoint(afterStart, endDelta);
  }

  const simplified = simplifyCorners(updatedCorners);
  const dense = densePoints(simplified.map(([x, y]) => ({ x, y }))).map((p) => [p.x, p.y]);
  return dense.length >= 2 ? dense : [newStart, newEnd];
}

function adjustEndpoint(corners: number[][], delta: { dx: number; dy: number }): number[][] {
  const n = corners.length;
  if (n < 2) return corners;
  const c = corners.map((pt) => [...pt]);
  const pLast = c[n - 1];
  const pPrev = c[n - 2];
  const targetEnd: [number, number] = [pLast[0] + delta.dx, pLast[1] + delta.dy];

  if (delta.dx === 0 && delta.dy === 0) return corners;

  const isHorizontal = pPrev[1] === pLast[1];
  const isVertical = pPrev[0] === pLast[0];

  if (isHorizontal) {
    if (delta.dy === 0) {
      // Moved only horizontally along the lead axis
      c[n - 1] = targetEnd;
      return simplifyCorners(c);
    }
    // Terminal moved vertically (and possibly horizontally)
    if (n >= 3) {
      const newPrevY = pPrev[1] + delta.dy;
      const prevPrevY = c[n - 3][1];
      const hadDir = Math.sign(pPrev[1] - prevPrevY);
      const newDir = Math.sign(newPrevY - prevPrevY);

      if (hadDir === 0 || newDir === hadDir || newDir === 0) {
        c[n - 2][1] = newPrevY;
        c[n - 1] = targetEnd;
        return simplifyCorners(c);
      }
      return simplifyCorners([...c.slice(0, n - 1), [pLast[0], targetEnd[1]], targetEnd]);
    }

    // Single straight horizontal segment [pPrev, pLast]:
    const x0 = pPrev[0];
    const y0 = pPrev[1];
    const x1 = targetEnd[0];
    const y1 = targetEnd[1];

    if (x0 === x1 || y0 === y1) {
      return simplifyCorners([pPrev, targetEnd]);
    }

    let midX = Math.round((x0 + x1) / 2);
    const minX = Math.min(x0, x1);
    const maxX = Math.max(x0, x1);
    if (maxX - minX >= 4) {
      const stagger = (y0 % 8 === 0 ? -1 : 1) * (delta.dy > 0 ? 1 : -1);
      const candidateX = midX + stagger;
      if (candidateX > minX && candidateX < maxX) {
        midX = candidateX;
      }
    }
    return simplifyCorners([pPrev, [midX, y0], [midX, y1], targetEnd]);
  } else if (isVertical) {
    if (delta.dx === 0) {
      // Moved only vertically along the lead axis
      c[n - 1] = targetEnd;
      return simplifyCorners(c);
    }
    // Terminal moved horizontally (and possibly vertically)
    if (n >= 3) {
      const newPrevX = pPrev[0] + delta.dx;
      const prevPrevX = c[n - 3][0];
      const hadDir = Math.sign(pPrev[0] - prevPrevX);
      const newDir = Math.sign(newPrevX - prevPrevX);

      if (hadDir === 0 || newDir === hadDir || newDir === 0) {
        c[n - 2][0] = newPrevX;
        c[n - 1] = targetEnd;
        return simplifyCorners(c);
      }
      return simplifyCorners([...c.slice(0, n - 1), [targetEnd[0], pLast[1]], targetEnd]);
    }

    // Single straight vertical segment [pPrev, pLast]:
    const x0 = pPrev[0];
    const y0 = pPrev[1];
    const x1 = targetEnd[0];
    const y1 = targetEnd[1];

    if (x0 === x1 || y0 === y1) {
      return simplifyCorners([pPrev, targetEnd]);
    }

    let midY = Math.round((y0 + y1) / 2);
    const minY = Math.min(y0, y1);
    const maxY = Math.max(y0, y1);
    if (maxY - minY >= 4) {
      const stagger = (x0 % 8 === 0 ? -1 : 1) * (delta.dx > 0 ? 1 : -1);
      const candidateY = midY + stagger;
      if (candidateY > minY && candidateY < maxY) {
        midY = candidateY;
      }
    }
    return simplifyCorners([pPrev, [x0, midY], [x1, midY], targetEnd]);
  }

  // Fallback if not strictly orthogonal initially
  c[n - 1] = targetEnd;
  return simplifyCorners(c);
}

