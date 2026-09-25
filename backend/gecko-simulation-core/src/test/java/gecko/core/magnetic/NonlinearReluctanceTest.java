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
package gecko.core.magnetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gecko.core.magnetic.NonlinearReluctance.CurveKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests of the nonlinear saturation characteristics: shared limit
 * behavior across all curve families (zero-origin, odd symmetry, saturation
 * flux, small-signal slope), numeric derivative consistency, the
 * piecewise-linear knee and input validation.
 */
class NonlinearReluctanceTest {

    /** Small-signal permeance of the test characteristics in H. */
    private static final double PERMEANCE = 2e-3;

    /** Saturation flux magnitude in Wb. */
    private static final double SATURATION_FLUX = 5e-4;

    /** Relative tolerance of curve value comparisons. */
    private static final double CURVE_TOLERANCE = 1e-9;

    /** Relative tolerance of numeric derivative comparisons. */
    private static final double DERIVATIVE_TOLERANCE = 1e-6;

    /** MMF drop step of the central difference quotient in ampere-turns. */
    private static final double DERIVATIVE_STEP = 1e-4;

    @ParameterizedTest
    @EnumSource(CurveKind.class)
    void curves_passThroughOriginWithSmallSignalSlope(final CurveKind kind) {
        final NonlinearReluctance model = NonlinearReluctance.of(kind, PERMEANCE, SATURATION_FLUX);

        assertEquals(0.0, model.flux(0.0), 0.0);
        assertEquals(PERMEANCE, model.differentialPermeance(0.0), PERMEANCE * CURVE_TOLERANCE);

        // moderate drive below the knee: compare against the exact curve shape
        final double mmf = 0.3 * SATURATION_FLUX / PERMEANCE;
        final double expected = switch (kind) {
            case FROELICH -> PERMEANCE * mmf
                    / (1.0 + PERMEANCE * Math.abs(mmf) / SATURATION_FLUX);
            case ARCTANGENT -> 2.0 * SATURATION_FLUX / Math.PI
                    * Math.atan(Math.PI * PERMEANCE * mmf / (2.0 * SATURATION_FLUX));
            case TANH -> SATURATION_FLUX * Math.tanh(PERMEANCE * mmf / SATURATION_FLUX);
            case PIECEWISE_LINEAR -> PERMEANCE * mmf;
        };
        assertEquals(expected, model.flux(mmf), CURVE_TOLERANCE * SATURATION_FLUX);
    }

    @ParameterizedTest
    @EnumSource(CurveKind.class)
    void curves_areOddSymmetric(final CurveKind kind) {
        final NonlinearReluctance model = NonlinearReluctance.of(kind, PERMEANCE, SATURATION_FLUX);

        for (final double mmf : new double[]{1.0, 10.0, 100.0, 1000.0}) {
            assertEquals(-model.flux(mmf), model.flux(-mmf), CURVE_TOLERANCE * SATURATION_FLUX,
                    "flux must be odd-symmetric at F=" + mmf);
        }
    }

    @ParameterizedTest
    @EnumSource(CurveKind.class)
    void curves_saturateAtSaturationFlux(final CurveKind kind) {
        final NonlinearReluctance model = NonlinearReluctance.of(kind, PERMEANCE, SATURATION_FLUX);

        // far beyond the knee the flux approaches the saturation magnitude;
        // the piecewise-linear curve keeps growing with its small saturated
        // slope (49 knees past the knee at the default 1/1000 slope ratio)
        final double deepMMF = 50.0 * SATURATION_FLUX / PERMEANCE;
        final double fluxMagnitude = Math.abs(model.flux(deepMMF));
        assertTrue(fluxMagnitude > 0.9 * SATURATION_FLUX,
                "deep saturation must approach the saturation magnitude");
        if (kind == CurveKind.PIECEWISE_LINEAR) {
            assertTrue(fluxMagnitude < 1.05 * SATURATION_FLUX,
                    "piecewise-linear flux must stay near the saturation magnitude");
        } else {
            assertTrue(fluxMagnitude <= SATURATION_FLUX * (1.0 + 1e-12),
                    "smooth curves must saturate at the saturation magnitude");
        }
    }

    @ParameterizedTest
    @EnumSource(CurveKind.class)
    void curves_areMonotonicallyIncreasing(final CurveKind kind) {
        final NonlinearReluctance model = NonlinearReluctance.of(kind, PERMEANCE, SATURATION_FLUX);

        double previous = Double.NEGATIVE_INFINITY;
        for (double mmf = -1000.0; mmf <= 1000.0; mmf += 1.0) {
            final double flux = model.flux(mmf);
            assertTrue(flux >= previous, "flux must be monotone at F=" + mmf);
            previous = flux;
        }
    }

    @ParameterizedTest
    @EnumSource(CurveKind.class)
    void differentialPermeance_matchesNumericDerivative(final CurveKind kind) {
        final NonlinearReluctance model = NonlinearReluctance.of(kind, PERMEANCE, SATURATION_FLUX);

        // F = 0 is excluded: the central difference is only first-order there
        // (the |F| term of the curves has a second-derivative kink at the origin)
        for (final double mmf : new double[]{0.1, 1.0, 10.0}) {
            final double numeric = (model.flux(mmf + DERIVATIVE_STEP)
                    - model.flux(mmf - DERIVATIVE_STEP)) / (2.0 * DERIVATIVE_STEP);
            assertEquals(numeric, model.differentialPermeance(mmf),
                    DERIVATIVE_TOLERANCE * PERMEANCE,
                    "differential permeance must match dPhi/dF at F=" + mmf);
        }
    }

    @ParameterizedTest
    @EnumSource(CurveKind.class)
    void differentialPermeance_staysFiniteAtExtremeDrive(final CurveKind kind) {
        final NonlinearReluctance model = NonlinearReluctance.of(kind, PERMEANCE, SATURATION_FLUX);

        final double floor = 1e-12 * PERMEANCE;
        assertTrue(model.differentialPermeance(1e6) >= floor,
                "differential permeance must stay above its numerical floor");
        assertTrue(Double.isFinite(model.differentialPermeance(1e6)));
    }

    @Test
    void piecewiseLinear_curveHasContinuousKneeWithReducedSlope() {
        final double ratio = 100.0;
        final NonlinearReluctance model = NonlinearReluctance.of(CurveKind.PIECEWISE_LINEAR,
                PERMEANCE, SATURATION_FLUX, ratio);
        final double kneeMMF = SATURATION_FLUX / PERMEANCE;

        // exactly at the knee both slopes meet: the flux is the saturation flux
        assertEquals(SATURATION_FLUX, model.flux(kneeMMF), CURVE_TOLERANCE * SATURATION_FLUX);
        // below the knee: unsaturated slope; above: reduced slope
        assertEquals(PERMEANCE, model.differentialPermeance(0.5 * kneeMMF), 1e-15);
        assertEquals(PERMEANCE / ratio, model.differentialPermeance(2.0 * kneeMMF), 1e-15);
        // continuity across the knee
        final double eps = 1e-9 * kneeMMF;
        assertEquals(SATURATION_FLUX + PERMEANCE / ratio * eps,
                model.flux(kneeMMF + eps), 1e-15);
    }

    @Test
    void factory_rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> NonlinearReluctance.of(null, PERMEANCE, SATURATION_FLUX));
        assertThrows(IllegalArgumentException.class,
                () -> NonlinearReluctance.of(CurveKind.TANH, 0.0, SATURATION_FLUX));
        assertThrows(IllegalArgumentException.class,
                () -> NonlinearReluctance.of(CurveKind.TANH, -PERMEANCE, SATURATION_FLUX));
        assertThrows(IllegalArgumentException.class,
                () -> NonlinearReluctance.of(CurveKind.TANH, PERMEANCE, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> NonlinearReluctance.of(CurveKind.TANH, PERMEANCE, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> NonlinearReluctance.of(CurveKind.PIECEWISE_LINEAR, PERMEANCE,
                        SATURATION_FLUX, 0.5));
    }

    @Test
    void accessors_returnConstructorValues() {
        final NonlinearReluctance model = NonlinearReluctance.of(CurveKind.FROELICH,
                PERMEANCE, SATURATION_FLUX, 500.0);

        assertEquals(CurveKind.FROELICH, model.getKind());
        assertEquals(PERMEANCE, model.getUnsaturatedPermeance());
        assertEquals(SATURATION_FLUX, model.getSaturationFlux());
        assertEquals(PERMEANCE / 500.0, model.getSaturatedPermeance(), 0.0);
    }
}
