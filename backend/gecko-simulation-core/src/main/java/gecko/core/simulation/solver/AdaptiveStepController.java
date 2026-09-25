/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;

/**
 * Step-size policy of the adaptive LTE controller.
 *
 * <p>The engine estimates the local truncation error of each attempted step
 * by re-solving it with the complementary integration method and passing the
 * relative error norm to {@link #evaluate(double, double)}. The policy is the
 * classic I-controller: on acceptance the next step width scales with
 * sqrt(tolerance / error) bounded to {@link #MIN_SHRINK_FACTOR}..
 * {@link #MAX_GROW_FACTOR}, on rejection the step is retried halved. At the
 * minimum step width a too-large error is accepted anyway so the simulation
 * always makes progress.
 *
 * <p>Integration-method note: the complementary-method error estimate is
 * exact for the one-step methods (Backward Euler, Trapezoidal). Gear-Shichman
 * mixes two-step history whose validity degrades across step-width changes;
 * adaptive control with GS therefore converges more conservatively and is
 * not recommended.
 *
 * @see HeadlessSimulationEngine
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class AdaptiveStepController {

    /** Lower bound of the per-step growth/shrink factor on acceptance. */
    public static final double MIN_SHRINK_FACTOR = 0.2;

    /** Upper bound of the per-step growth factor on acceptance. */
    public static final double MAX_GROW_FACTOR = 5.0;

    /** Factor applied to the step width on a rejected step. */
    public static final double REJECT_SHRINK_FACTOR = 0.5;

    /** Divisor deriving the default minimum step width from the base step. */
    public static final int DEFAULT_MIN_STEP_DIVISOR = 100;

    /** Smallest error norm treated as non-zero (a zero norm means exact match). */
    private static final double ERROR_NORM_FLOOR = 1e-300;

    private final double relativeTolerance;
    private final double minStepWidth;
    private final double maxStepWidth;
    private double currentStepWidth;
    private int rejectedSteps;

    /**
     * Creates an adaptive step controller.
     *
     * @param relativeTolerance relative LTE tolerance (must be positive)
     * @param minStepWidth lower step-width bound (must be positive)
     * @param maxStepWidth upper step-width bound (must be >= minStepWidth)
     * @param initialStepWidth first attempted step width (clamped to the bounds)
     */
    public AdaptiveStepController(final double relativeTolerance, final double minStepWidth,
                                  final double maxStepWidth, final double initialStepWidth) {
        if (relativeTolerance <= 0 || !Double.isFinite(relativeTolerance)) {
            throw new IllegalArgumentException(
                    "Relative tolerance must be positive and finite, got: " + relativeTolerance);
        }
        if (minStepWidth <= 0 || !Double.isFinite(minStepWidth)) {
            throw new IllegalArgumentException(
                    "Minimum step width must be positive and finite, got: " + minStepWidth);
        }
        if (maxStepWidth < minStepWidth || !Double.isFinite(maxStepWidth)) {
            throw new IllegalArgumentException("Maximum step width must be >= the minimum, got: "
                    + maxStepWidth);
        }
        this.relativeTolerance = relativeTolerance;
        this.minStepWidth = minStepWidth;
        this.maxStepWidth = maxStepWidth;
        this.currentStepWidth = Math.min(Math.max(initialStepWidth, minStepWidth), maxStepWidth);
    }

    /**
     * Maps a solver type to the complementary method used for the LTE
     * estimate: the one-step methods pair with each other, Gear-Shichman
     * pairs with Backward Euler.
     *
     * @param solverType the configured integration method
     * @return the complementary integration method
     */
    public static SolverType complementaryType(final SolverType solverType) {
        return solverType == SolverType.SOLVER_BE ? SolverType.SOLVER_TRZ : SolverType.SOLVER_BE;
    }

    /**
     * Gets the step width the next attempt should use.
     *
     * @return current step width in seconds
     */
    public double getCurrentStepWidth() {
        return currentStepWidth;
    }

    /**
     * Gets how many steps were rejected by the error test so far.
     *
     * @return rejection count
     */
    public int getRejectedSteps() {
        return rejectedSteps;
    }

    /**
     * Evaluates one attempted step against the error test.
     *
     * @param errorNorm relative local truncation error norm of the attempt
     * @param triedStepWidth step width the attempt used
     * @return the accept/reject decision including the next step width
     */
    public StepDecision evaluate(final double errorNorm, final double triedStepWidth) {
        if (errorNorm <= relativeTolerance) {
            final double factor = errorNorm < ERROR_NORM_FLOOR
                    ? MAX_GROW_FACTOR
                    : Math.sqrt(relativeTolerance / errorNorm);
            currentStepWidth = clampStepWidth(triedStepWidth * clampFactor(factor));
            return StepDecision.accepted(currentStepWidth, false);
        }
        rejectedSteps++;
        final double retryStepWidth = Math.max(triedStepWidth * REJECT_SHRINK_FACTOR, minStepWidth);
        if (retryStepWidth >= triedStepWidth) {
            // Already at the minimum step width: accept to guarantee progress
            currentStepWidth = minStepWidth;
            return StepDecision.accepted(currentStepWidth, true);
        }
        currentStepWidth = retryStepWidth;
        return StepDecision.rejected(retryStepWidth);
    }

    private static double clampFactor(final double factor) {
        return Math.min(Math.max(factor, MIN_SHRINK_FACTOR), MAX_GROW_FACTOR);
    }

    private double clampStepWidth(final double stepWidth) {
        return Math.min(Math.max(stepWidth, minStepWidth), maxStepWidth);
    }

    /**
     * Outcome of one error-test evaluation.
     *
     * @param accepted true when the attempted step is kept
     * @param nextStepWidth step width for the next attempt/step
     * @param acceptedAtFloor true when a too-large error was accepted only
     *                        because the minimum step width was reached
     */
    public record StepDecision(boolean accepted, double nextStepWidth, boolean acceptedAtFloor) {

        static StepDecision accepted(final double nextStepWidth, final boolean atFloor) {
            return new StepDecision(true, nextStepWidth, atFloor);
        }

        static StepDecision rejected(final double retryStepWidth) {
            return new StepDecision(false, retryStepWidth, false);
        }
    }
}
