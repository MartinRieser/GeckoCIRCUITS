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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the adaptive step-size policy: growth bounded by the factor clamp,
 * rejection halving, minimum-step-width floor acceptance, and validation.
 */
class AdaptiveStepControllerTest {

    private static final double TOLERANCE = 1e-3;
    private static final double MIN_STEP = 1e-9;
    private static final double MAX_STEP = 1e-5;
    private static final double INITIAL_STEP = 1e-6;

    private AdaptiveStepController newController() {
        return new AdaptiveStepController(TOLERANCE, MIN_STEP, MAX_STEP, INITIAL_STEP);
    }

    @Test
    void zeroError_growsByMaxFactor() {
        final AdaptiveStepController controller = newController();
        final AdaptiveStepController.StepDecision decision = controller.evaluate(0.0, INITIAL_STEP);

        assertTrue(decision.accepted());
        assertEquals(INITIAL_STEP * AdaptiveStepController.MAX_GROW_FACTOR,
                decision.nextStepWidth(), 0.0);
        assertEquals(INITIAL_STEP * AdaptiveStepController.MAX_GROW_FACTOR,
                controller.getCurrentStepWidth(), 0.0);
    }

    @Test
    void smallError_scalesStepBySqrtOfToleranceRatio() {
        final AdaptiveStepController controller = newController();
        // err = tol / 4 -> factor = sqrt(4) = 2
        final AdaptiveStepController.StepDecision decision =
                controller.evaluate(TOLERANCE / 4.0, INITIAL_STEP);

        assertTrue(decision.accepted());
        assertEquals(INITIAL_STEP * 2.0, decision.nextStepWidth(), 1e-15);
    }

    @Test
    void growthIsClampedToMaxStepWidth() {
        final AdaptiveStepController controller = newController();
        controller.evaluate(0.0, MAX_STEP);

        assertEquals(MAX_STEP, controller.getCurrentStepWidth(), 0.0,
                "growth must not exceed the maximum step width");
    }

    @Test
    void largeError_rejectsAndHalvesTheStep() {
        final AdaptiveStepController controller = newController();
        final AdaptiveStepController.StepDecision decision =
                controller.evaluate(100.0 * TOLERANCE, INITIAL_STEP);

        assertFalse(decision.accepted());
        assertEquals(INITIAL_STEP * AdaptiveStepController.REJECT_SHRINK_FACTOR,
                decision.nextStepWidth(), 0.0);
        assertEquals(1, controller.getRejectedSteps());
    }

    @Test
    void rejectionAtMinimumStepWidth_acceptsToGuaranteeProgress() {
        final AdaptiveStepController controller =
                new AdaptiveStepController(TOLERANCE, MIN_STEP, MAX_STEP, MIN_STEP);
        final AdaptiveStepController.StepDecision decision =
                controller.evaluate(100.0 * TOLERANCE, MIN_STEP);

        assertTrue(decision.accepted(), "at the floor the step must be accepted");
        assertTrue(decision.acceptedAtFloor());
        assertEquals(MIN_STEP, decision.nextStepWidth(), 0.0);
    }

    @Test
    void rejectThenFloorSequence_terminatesAtMinimum() {
        final AdaptiveStepController controller = newController();
        AdaptiveStepController.StepDecision decision =
                controller.evaluate(100.0 * TOLERANCE, 3.0 * MIN_STEP);
        assertFalse(decision.accepted());
        assertEquals(1.5 * MIN_STEP, decision.nextStepWidth(), 0.0,
                "halving 3x the minimum gives 1.5x the minimum");

        decision = controller.evaluate(100.0 * TOLERANCE, decision.nextStepWidth());
        assertFalse(decision.accepted());
        assertEquals(MIN_STEP, decision.nextStepWidth(), 0.0,
                "halving below the minimum must clamp to the minimum");

        decision = controller.evaluate(100.0 * TOLERANCE, decision.nextStepWidth());
        assertTrue(decision.accepted(), "retrying at the minimum must accept");
    }

    @Test
    void acceptanceJustBelowTolerance_growsOnlySlightly() {
        final AdaptiveStepController controller = newController();
        // err = 0.99 * tol -> factor = sqrt(1/0.99) ~ 1.005 (no clamping)
        final AdaptiveStepController.StepDecision decision =
                controller.evaluate(TOLERANCE * 0.99, INITIAL_STEP);

        assertTrue(decision.accepted());
        assertEquals(INITIAL_STEP * Math.sqrt(1.0 / 0.99), decision.nextStepWidth(), 1e-12);
    }

    @Test
    void tinyNonZeroError_isClampedToMaxGrowFactor() {
        final AdaptiveStepController controller = newController();
        final AdaptiveStepController.StepDecision decision =
                controller.evaluate(Double.MIN_VALUE, INITIAL_STEP);

        assertTrue(decision.accepted());
        assertEquals(INITIAL_STEP * AdaptiveStepController.MAX_GROW_FACTOR,
                decision.nextStepWidth(), 0.0);
    }

    @Test
    void constructor_rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveStepController(0.0, MIN_STEP, MAX_STEP, INITIAL_STEP));
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveStepController(TOLERANCE, 0.0, MAX_STEP, INITIAL_STEP));
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveStepController(TOLERANCE, MAX_STEP, MIN_STEP, INITIAL_STEP));
    }

    @Test
    void complementaryType_pairsOneStepMethodsAndGearShichmanWithBackwardEuler() {
        assertEquals(SolverType.SOLVER_TRZ,
                AdaptiveStepController.complementaryType(SolverType.SOLVER_BE));
        assertEquals(SolverType.SOLVER_BE,
                AdaptiveStepController.complementaryType(SolverType.SOLVER_TRZ));
        assertEquals(SolverType.SOLVER_BE,
                AdaptiveStepController.complementaryType(SolverType.SOLVER_GS));
    }
}
