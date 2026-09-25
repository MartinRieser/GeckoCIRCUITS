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

/**
 * Direct and inverse Park transformation between stationary orthogonal coordinates
 * ({@code alpha, beta}) and rotating reference frame coordinates ({@code d, q})
 * synchronized to a spatial angle {@code theta}.
 *
 * <p><strong>Forward Transformation (Stationary to Synchronous Rotating Frame):</strong></p>
 * <pre>
 *   d =  alpha * cos(theta) + beta * sin(theta)
 *   q = -alpha * sin(theta) + beta * cos(theta)
 * </pre>
 *
 * <p><strong>Inverse Transformation (Synchronous Rotating Frame to Stationary):</strong></p>
 * <pre>
 *   alpha = d * cos(theta) - q * sin(theta)
 *   beta  = d * sin(theta) + q * cos(theta)
 * </pre>
 *
 * @author GeckoCIRCUITS Team
 * @since v2.18.0 Task L4
 */
public final class ParkTransform {

    private ParkTransform() {
        // Prevent instantiation of static utility class
    }

    /**
     * Direct and quadrature axis components in the synchronous rotating frame.
     *
     * @param d direct axis component [V, A, or Wb]
     * @param q quadrature axis component [V, A, or Wb]
     */
    public record DirectQuadrature(double d, double q) {}

    /**
     * Performs direct Park transformation from stationary alpha-beta frame to rotating d-q frame.
     *
     * @param alpha stationary direct axis component
     * @param beta stationary quadrature axis component
     * @param theta electrical rotation angle [rad]
     * @return synchronous rotating frame components (d, q)
     */
    public static DirectQuadrature forward(final double alpha, final double beta, final double theta) {
        final double cosTheta = Math.cos(theta);
        final double sinTheta = Math.sin(theta);
        final double d = alpha * cosTheta + beta * sinTheta;
        final double q = -alpha * sinTheta + beta * cosTheta;
        return new DirectQuadrature(d, q);
    }

    /**
     * Performs inverse Park transformation from rotating d-q frame to stationary alpha-beta frame.
     *
     * @param d direct axis component in rotating frame
     * @param q quadrature axis component in rotating frame
     * @param theta electrical rotation angle [rad]
     * @return stationary coordinates (alpha, beta)
     */
    public static ClarkeTransform.AlphaBeta inverse(final double d, final double q, final double theta) {
        final double cosTheta = Math.cos(theta);
        final double sinTheta = Math.sin(theta);
        final double alpha = d * cosTheta - q * sinTheta;
        final double beta = d * sinTheta + q * cosTheta;
        return new ClarkeTransform.AlphaBeta(alpha, beta, 0.0);
    }
}
