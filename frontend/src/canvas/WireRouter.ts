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
  if (!rawPoints || rawPoints.length < 2) return rawPoints || [];

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
  if (corners.length < 2) {
    return rawPoints.map(([x, y]) => [x + endDelta.dx, y + endDelta.dy]);
  }

  let updatedCorners = corners.map((pt) => [...pt]);

  // Case: only start moves, end is stationary
  if (
    (startDelta.dx !== 0 || startDelta.dy !== 0) &&
    endDelta.dx === 0 &&
    endDelta.dy === 0
  ) {
    const reversed = updatedCorners.reverse();
    const moved = adjustEndpoint(reversed, startDelta);
    updatedCorners = moved.reverse();
  }
  // Case: only end moves, start is stationary
  else if (
    (endDelta.dx !== 0 || endDelta.dy !== 0) &&
    startDelta.dx === 0 &&
    startDelta.dy === 0
  ) {
    updatedCorners = adjustEndpoint(updatedCorners, endDelta);
  }
  // Case: both move by different deltas
  else {
    const rev = updatedCorners.reverse();
    const afterStart = adjustEndpoint(rev, startDelta).reverse();
    updatedCorners = adjustEndpoint(afterStart, endDelta);
  }

  // Convert corners back to dense points
  const dense = densePoints(updatedCorners.map(([x, y]) => ({ x, y }))).map((p) => [p.x, p.y]);
  return dense;
}

function adjustEndpoint(corners: number[][], delta: { dx: number; dy: number }): number[][] {
  const n = corners.length;
  if (n < 2) return corners;
  const c = corners.map((pt) => [...pt]);
  const pLast = c[n - 1];
  const pPrev = c[n - 2];
  const targetEnd = [pLast[0] + delta.dx, pLast[1] + delta.dy];

  const isHorizontal = pPrev[1] === pLast[1];
  const isVertical = pPrev[0] === pLast[0];

  if (isHorizontal) {
    if (delta.dy === 0) {
      // Moved only horizontally along the lead axis
      c[n - 1] = targetEnd;
      return simplifyCorners(c);
    }
    // Terminal moved vertically (and maybe horizontally)
    if (n >= 3) {
      // Multi-segment wire: slide the preceding corner vertically
      c[n - 2][1] = pPrev[1] + delta.dy;
      c[n - 1] = targetEnd;
      return simplifyCorners(c);
    }
    // Single straight horizontal segment [pPrev, pLast]:
    const x0 = pPrev[0];
    const y0 = pPrev[1];
    const x1 = targetEnd[0];
    const y1 = targetEnd[1];

    if (x0 === x1) {
      return simplifyCorners([pPrev, targetEnd]);
    }

    // Stagger turn position based on pin y0 so parallel horizontal wires don't collapse onto the same line
    const stagger = (y0 % 8 === 0 ? -2 : 2) * (delta.dy > 0 ? 1 : -1);
    let xTurn = Math.round((x0 + x1) / 2) + stagger;
    const minX = Math.min(x0, x1) + 1;
    const maxX = Math.max(x0, x1) - 1;
    if (minX <= maxX) {
      xTurn = Math.max(minX, Math.min(maxX, xTurn));
    }
    return simplifyCorners([pPrev, [xTurn, y0], [xTurn, y1], targetEnd]);
  } else if (isVertical) {
    if (delta.dx === 0) {
      // Moved only vertically along the lead axis
      c[n - 1] = targetEnd;
      return simplifyCorners(c);
    }
    // Terminal moved horizontally (and maybe vertically)
    if (n >= 3) {
      // Multi-segment wire: slide the preceding corner horizontally
      c[n - 2][0] = pPrev[0] + delta.dx;
      c[n - 1] = targetEnd;
      return simplifyCorners(c);
    }
    // Single straight vertical segment [pPrev, pLast]:
    const x0 = pPrev[0];
    const y0 = pPrev[1];
    const x1 = targetEnd[0];
    const y1 = targetEnd[1];

    if (y0 === y1) {
      return simplifyCorners([pPrev, targetEnd]);
    }

    // Stagger turn position based on pin x0 so parallel vertical wires don't collapse onto the same line
    const stagger = (x0 % 8 === 0 ? -2 : 2) * (delta.dx > 0 ? 1 : -1);
    let yTurn = Math.round((y0 + y1) / 2) + stagger;
    const minY = Math.min(y0, y1) + 1;
    const maxY = Math.max(y0, y1) - 1;
    if (minY <= maxY) {
      yTurn = Math.max(minY, Math.min(maxY, yTurn));
    }
    return simplifyCorners([pPrev, [x0, yTurn], [x1, yTurn], targetEnd]);
  }

  // Fallback if not strictly orthogonal initially
  c[n - 1] = targetEnd;
  return simplifyCorners(c);
}

