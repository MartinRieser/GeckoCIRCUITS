/**
 * Step-count estimation for the simulation parameter bar.
 *
 * The solver executes one step per dt until tEnd is reached, so the step
 * count is the ratio of the two — the primary cost driver of a run.
 */

/** Step counts above this are flagged as potentially long-running. */
export const STEP_WARNING_THRESHOLD = 2_000_000;

/** Estimated solver step count for a time span and step width; 0 if dt is invalid. */
export function estimateStepCount(tEnd: number, dt: number): number {
  return dt > 0 ? Math.round(tEnd / dt) : 0;
}
