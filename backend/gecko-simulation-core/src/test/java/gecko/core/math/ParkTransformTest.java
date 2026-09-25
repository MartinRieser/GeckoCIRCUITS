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

class ParkTransformTest {

    private static final double EPSILON = 1e-12;

    @Test
    @DisplayName("Park transform at theta=0 yields d=alpha, q=beta")
    void alignedAtZeroAngle() {
        double alpha = 50.0;
        double beta = 25.0;

        ParkTransform.DirectQuadrature dq = ParkTransform.forward(alpha, beta, 0.0);
        assertEquals(50.0, dq.d(), EPSILON);
        assertEquals(25.0, dq.q(), EPSILON);

        ClarkeTransform.AlphaBeta ab = ParkTransform.inverse(dq.d(), dq.q(), 0.0);
        assertEquals(alpha, ab.alpha(), EPSILON);
        assertEquals(beta, ab.beta(), EPSILON);
    }

    @Test
    @DisplayName("Park transform at theta=pi/2 rotates coordinates by 90 degrees")
    void rotationByNinetyDegrees() {
        double alpha = 100.0;
        double beta = 0.0;
        double theta = Math.PI / 2.0;

        ParkTransform.DirectQuadrature dq = ParkTransform.forward(alpha, beta, theta);
        assertEquals(0.0, dq.d(), EPSILON);
        assertEquals(-100.0, dq.q(), EPSILON);

        ClarkeTransform.AlphaBeta ab = ParkTransform.inverse(dq.d(), dq.q(), theta);
        assertEquals(alpha, ab.alpha(), EPSILON);
        assertEquals(beta, ab.beta(), EPSILON);
    }

    @Test
    @DisplayName("Synchronous alignment: when theta matches vector angle, q-axis is zero")
    void synchronousOrientationProducesPureDirectComponent() {
        double alpha = 30.0;
        double beta = 40.0;
        double theta = Math.atan2(beta, alpha); // angle of vector (magnitude 50)

        ParkTransform.DirectQuadrature dq = ParkTransform.forward(alpha, beta, theta);
        assertEquals(50.0, dq.d(), EPSILON);
        assertEquals(0.0, dq.q(), EPSILON);

        ClarkeTransform.AlphaBeta ab = ParkTransform.inverse(dq.d(), dq.q(), theta);
        assertEquals(alpha, ab.alpha(), EPSILON);
        assertEquals(beta, ab.beta(), EPSILON);
    }

    @Test
    @DisplayName("Arbitrary rotation round-trip preserves alpha-beta values")
    void roundTripArbitraryAngles() {
        double[] angles = {0.123, -0.456, 1.25, 3.1415, 5.89, -2.5};
        double alpha = 12.34;
        double beta = -56.78;

        for (double theta : angles) {
            ParkTransform.DirectQuadrature dq = ParkTransform.forward(alpha, beta, theta);
            ClarkeTransform.AlphaBeta ab = ParkTransform.inverse(dq.d(), dq.q(), theta);
            assertEquals(alpha, ab.alpha(), 1e-10, "Alpha mismatch at theta=" + theta);
            assertEquals(beta, ab.beta(), 1e-10, "Beta mismatch at theta=" + theta);
        }
    }
}
