/**
 * Time-axis view window math for the scope views: zoom, pan, clamping.
 * Pure functions — the charts translate these into pixel geometry.
 */

export interface ViewWindow {
  start: number;
  end: number;
}

/** Below this span a window is considered degenerate and refuses to shrink. */
export const MIN_SPAN = 1e-12;

export function fullWindow(t0: number, t1: number): ViewWindow {
  if (t1 <= t0) return { start: t0, end: t0 + 1 };
  return { start: t0, end: t1 };
}

/** The window actually rendered: the user window clipped to the data range,
 *  or the full data range when no user window is set (fit mode). */
export function effectiveWindow(view: ViewWindow | null, t0: number, t1: number): ViewWindow {
  const full = fullWindow(t0, t1);
  if (!view) return full;
  return clampWindow(view, full.start, full.end);
}

export function clampWindow(w: ViewWindow, t0: number, t1: number): ViewWindow {
  let start = w.start;
  let end = w.end;
  if (end - start < MIN_SPAN) {
    const mid = (start + end) / 2;
    start = mid - MIN_SPAN / 2;
    end = mid + MIN_SPAN / 2;
  }
  const span = end - start;
  // translate (don't shrink) so the window stays inside the data range
  if (start < t0) {
    start = t0;
    end = t0 + span;
  }
  if (end > t1) {
    end = t1;
    start = Math.max(t0, t1 - span);
  }
  return { start, end };
}

/** factor < 1 zooms in, factor > 1 zooms out; `anchor` (a time) stays fixed. */
export function zoomWindow(
  w: ViewWindow,
  factor: number,
  anchor: number,
  t0: number,
  t1: number,
): ViewWindow {
  const start = anchor + (w.start - anchor) * factor;
  const end = anchor + (w.end - anchor) * factor;
  return clampWindow({ start, end }, t0, t1);
}

export function panWindow(w: ViewWindow, delta: number, t0: number, t1: number): ViewWindow {
  return clampWindow({ start: w.start + delta, end: w.end + delta }, t0, t1);
}
