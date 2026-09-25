/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package gecko.core.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClarkeTransformTest {

    private static final double EPSILON = 1e-12;

    @Test
    @DisplayName("Balanced 3-phase system at angle 0 transforms to alpha=1, beta=0, gamma=0")
    void balancedAngleZero() {
        double a = 1.0;
        double b = -0.5;
        double c = -0.5;

        ClarkeTransform.AlphaBeta res = ClarkeTransform.forward(a, b, c);
        assertEquals(1.0, res.alpha(), EPSILON);
        assertEquals(0.0, res.beta(), EPSILON);
        assertEquals(0.0, res.gamma(), EPSILON);

        ClarkeTransform.AlphaBeta resBalanced = ClarkeTransform.forwardBalanced(a, b);
        assertEquals(1.0, resBalanced.alpha(), EPSILON);
        assertEquals(0.0, resBalanced.beta(), EPSILON);
        assertEquals(0.0, resBalanced.gamma(), EPSILON);
    }

    @Test
    @DisplayName("Balanced 3-phase system at angle pi/2 transforms to alpha=0, beta=1")
    void balancedAnglePiDiv2() {
        double theta = Math.PI / 2.0;
        double a = Math.cos(theta); // 0
        double b = Math.cos(theta - 2.0 * Math.PI / 3.0); // sin(pi/6) = 0.5 * sqrt(3)
        double c = Math.cos(theta + 2.0 * Math.PI / 3.0); // -0.5 * sqrt(3)

        ClarkeTransform.AlphaBeta res = ClarkeTransform.forward(a, b, c);
        assertEquals(0.0, res.alpha(), EPSILON);
        assertEquals(1.0, res.beta(), EPSILON);
        assertEquals(0.0, res.gamma(), EPSILON);

        ClarkeTransform.AlphaBeta resBalanced = ClarkeTransform.forwardBalanced(a, b);
        assertEquals(0.0, resBalanced.alpha(), EPSILON);
        assertEquals(1.0, resBalanced.beta(), EPSILON);
    }

    @Test
    @DisplayName("Round-trip inverse transformation reproduces original three-phase quantities")
    void roundTripInverse() {
        double a = 325.0;
        double b = -150.0;
        double c = -175.0;

        ClarkeTransform.AlphaBeta ab = ClarkeTransform.forward(a, b, c);
        ClarkeTransform.ThreePhase inv = ClarkeTransform.inverse(ab.alpha(), ab.beta(), ab.gamma());

        assertEquals(a, inv.a(), EPSILON);
        assertEquals(b, inv.b(), EPSILON);
        assertEquals(c, inv.c(), EPSILON);
    }

    @Test
    @DisplayName("Unbalanced homopolar component is properly isolated in gamma")
    void homopolarGammaIsolation() {
        double a = 10.0;
        double b = 10.0;
        double c = 10.0; // pure zero-sequence

        ClarkeTransform.AlphaBeta res = ClarkeTransform.forward(a, b, c);
        assertEquals(0.0, res.alpha(), EPSILON);
        assertEquals(0.0, res.beta(), EPSILON);
        assertEquals(10.0, res.gamma(), EPSILON);

        ClarkeTransform.ThreePhase inv = ClarkeTransform.inverse(res.alpha(), res.beta(), res.gamma());
        assertEquals(10.0, inv.a(), EPSILON);
        assertEquals(10.0, inv.b(), EPSILON);
        assertEquals(10.0, inv.c(), EPSILON);
    }

    @Test
    @DisplayName("Two-argument inverse reconstructs a balanced three-phase set (c = -(a + b))")
    void inverseTwoArgumentBalancedOverload() {
        final double a = 0.8;
        final double b = -0.3;

        final ClarkeTransform.AlphaBeta ab = ClarkeTransform.forwardBalanced(a, b);
        final ClarkeTransform.ThreePhase inv = ClarkeTransform.inverse(ab.alpha(), ab.beta());

        assertEquals(a, inv.a(), EPSILON);
        assertEquals(b, inv.b(), EPSILON);
        assertEquals(-a - b, inv.c(), EPSILON);
    }
}
