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
 * Direct and inverse Clarke transformation between three-phase natural coordinates
 * ({@code a, b, c}) and stationary orthogonal two-phase coordinates ({@code alpha, beta, gamma}).
 *
 * <p>Uses the amplitude-invariant convention standard in electric drive controls,
 * which preserves the peak amplitude of sinusoidal phase quantities:</p>
 *
 * <p><strong>Forward Transformation (Three-Phase to Alpha-Beta):</strong></p>
 * <pre>
 *   alpha = 2/3 * (a - 1/2 * b - 1/2 * c)
 *   beta  = 1/sqrt(3) * (b - c)
 *   gamma = 1/3 * (a + b + c)  (zero-sequence component)
 * </pre>
 *
 * <p><strong>Balanced Three-Phase Simplification (where a + b + c = 0):</strong></p>
 * <pre>
 *   alpha = a
 *   beta  = 1/sqrt(3) * (a + 2 * b)
 *   gamma = 0
 * </pre>
 *
 * <p><strong>Inverse Transformation (Alpha-Beta to Three-Phase):</strong></p>
 * <pre>
 *   a = alpha + gamma
 *   b = -1/2 * alpha + sqrt(3)/2 * beta + gamma
 *   c = -1/2 * alpha - sqrt(3)/2 * beta + gamma
 * </pre>
 *
 * @author GeckoCIRCUITS Team
 * @since v2.18.0 Task L4
 */
public final class ClarkeTransform {

    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double ONE_DIV_SQRT_3 = 1.0 / SQRT_3;
    private static final double SQRT_3_DIV_2 = SQRT_3 / 2.0;
    private static final double TWO_THIRDS = 2.0 / 3.0;
    private static final double ONE_THIRD = 1.0 / 3.0;
    private static final double HALF = 0.5;

    private ClarkeTransform() {
        // Prevent instantiation of static utility class
    }

    /**
     * Stationary two-phase coordinates including zero-sequence component.
     *
     * @param alpha stationary direct axis component [V, A, or Wb]
     * @param beta stationary quadrature axis component [V, A, or Wb]
     * @param gamma zero-sequence homopolar component [V, A, or Wb]
     */
    public record AlphaBeta(double alpha, double beta, double gamma) {
        /**
         * Convenience constructor for balanced systems without zero-sequence component.
         *
         * @param alpha stationary direct axis component
         * @param beta stationary quadrature axis component
         */
        public AlphaBeta(double alpha, double beta) {
            this(alpha, beta, 0.0);
        }
    }

    /**
     * Three-phase natural coordinates.
     *
     * @param a phase A quantity [V, A, or Wb]
     * @param b phase B quantity [V, A, or Wb]
     * @param c phase C quantity [V, A, or Wb]
     */
    public record ThreePhase(double a, double b, double c) {}

    /**
     * Performs direct amplitude-invariant Clarke transformation from three-phase values.
     *
     * @param a phase A instantaneous value
     * @param b phase B instantaneous value
     * @param c phase C instantaneous value
     * @return transformed stationary coordinates (alpha, beta, gamma)
     */
    public static AlphaBeta forward(final double a, final double b, final double c) {
        final double alpha = TWO_THIRDS * (a - HALF * b - HALF * c);
        final double beta = ONE_DIV_SQRT_3 * (b - c);
        final double gamma = ONE_THIRD * (a + b + c);
        return new AlphaBeta(alpha, beta, gamma);
    }

    /**
     * Performs direct Clarke transformation for a balanced three-phase system (a + b + c = 0).
     *
     * @param a phase A instantaneous value
     * @param b phase B instantaneous value
     * @return transformed stationary coordinates (alpha, beta, 0)
     */
    public static AlphaBeta forwardBalanced(final double a, final double b) {
        final double alpha = a;
        final double beta = ONE_DIV_SQRT_3 * (a + 2.0 * b);
        return new AlphaBeta(alpha, beta, 0.0);
    }

    /**
     * Performs inverse Clarke transformation from stationary coordinates to three-phase values.
     *
     * @param alpha stationary direct axis component
     * @param beta stationary quadrature axis component
     * @param gamma zero-sequence homopolar component
     * @return three-phase values (a, b, c)
     */
    public static ThreePhase inverse(final double alpha, final double beta, final double gamma) {
        final double a = alpha + gamma;
        final double b = -HALF * alpha + SQRT_3_DIV_2 * beta + gamma;
        final double c = -HALF * alpha - SQRT_3_DIV_2 * beta + gamma;
        return new ThreePhase(a, b, c);
    }

    /**
     * Performs inverse Clarke transformation for balanced systems (gamma = 0).
     *
     * @param alpha stationary direct axis component
     * @param beta stationary quadrature axis component
     * @return three-phase values (a, b, c)
     */
    public static ThreePhase inverse(final double alpha, final double beta) {
        return inverse(alpha, beta, 0.0);
    }
}
