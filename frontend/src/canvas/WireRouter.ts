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
