/**
 * Orthogonal wire routing, ported from the Swing editor's L-router
 * (Connection.setCurrentPointOnConnection): the wire follows the dominant
 * drag axis first, then turns once.
 */
import type { Point } from '../model/types';

export function routeL(start: Point, end: Point): Point[] {
  if (start.x === end.x && start.y === end.y) {
    return [start];
  }
  if (start.x === end.x || start.y === end.y) {
    return [start, end];
  }
  const dx = Math.abs(end.x - start.x);
  const dy = Math.abs(end.y - start.y);
  // dominant axis first, matching the GUI's drag-direction heuristic
  if (dx >= dy) {
    return [start, { x: end.x, y: start.y }, end];
  }
  return [start, { x: start.x, y: end.y }, end];
}
