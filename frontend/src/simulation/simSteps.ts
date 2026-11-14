/**
 * Step-count estimation for the simulation parameter bar.
 *
 * The solver executes one step per dt until tEnd is reached, so the step
 * count is the ratio of the two — the primary cost driver of a run.
 */

/** Step counts at or above this threshold (10M) may take noticeable runtime depending on circuit complexity. */
export const STEP_WARNING_THRESHOLD = 10_000_000;

/** Estimated solver step count for a time span and step width; 0 if inputs are invalid or non-finite. */
export function estimateStepCount(tEnd: number, dt: number): number {
  if (!Number.isFinite(tEnd) || !Number.isFinite(dt) || dt <= 0 || tEnd <= 0) return 0;
  // With tEnd > 0 and dt > 0 the ratio is positive; only overflow can break it
  const steps = Math.round(tEnd / dt);
  return Number.isFinite(steps) ? steps : 0;
}

/** Returns true when `steps` reaches or exceeds `STEP_WARNING_THRESHOLD`. */
export function isHighStepCount(steps: number): boolean {
  return Number.isFinite(steps) && steps >= STEP_WARNING_THRESHOLD;
}
