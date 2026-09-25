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

/**
 * Nonlinear saturation characteristic of a magnetic core branch, relating the
 * branch flux {@code Phi(F)} to the magnetomotive force drop {@code F} across
 * it. All curves share the same two parameters: the unsaturated permeance
 * {@code P_0} (small-signal slope at {@code F = 0}, in H) and the saturation
 * flux {@code Phi_sat} (asymptotic flux magnitude, in Wb). All curves are
 * odd-symmetric, monotonically increasing and continuously differentiable
 * (the piecewise-linear curve at its knee), so the magnetic MNA converges by
 * Newton-Raphson iteration on the differential permeance
 * {@code P_diff = dPhi/dF}.
 *
 * @see MagneticNetworkSolver
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 3: Magnetic Domain Engine)
 */
public final class NonlinearReluctance {

    /** Saturation curve family. */
    public enum CurveKind {

        /** Froelich: {@code Phi = P_0 * F / (1 + P_0 * |F| / Phi_sat)}. */
        FROELICH,

        /** Arctangent: {@code Phi = (2*Phi_sat/pi) * atan(pi*P_0*F / (2*Phi_sat))}. */
        ARCTANGENT,

        /** Hyperbolic tangent: {@code Phi = Phi_sat * tanh(P_0 * F / Phi_sat)}. */
        TANH,

        /**
         * Piecewise-linear: slope {@code P_0} up to the knee flux
         * {@code Phi_sat}, then constant slope {@code P_sat = P_0 / mu_r_sat}.
         */
        PIECEWISE_LINEAR
    }

    /** Floor of the differential permeance relative to P_0, keeps stamps finite at extreme drive. */
    private static final double MIN_RELATIVE_DIFFERENTIAL_PERMEANCE = 1e-12;

    private final CurveKind kind;
    private final double unsaturatedPermeance;
    private final double saturationFlux;
    private final double saturatedPermeance;

    private NonlinearReluctance(final CurveKind kind, final double unsaturatedPermeance,
                                final double saturationFlux, final double saturatedPermeance) {
        this.kind = kind;
        this.unsaturatedPermeance = unsaturatedPermeance;
        this.saturationFlux = saturationFlux;
        this.saturatedPermeance = saturatedPermeance;
    }

    /**
     * Creates a nonlinear saturation characteristic.
     *
     * @param kind saturation curve family
     * @param unsaturatedPermeance small-signal permeance at F = 0 in H, positive
     * @param saturationFlux asymptotic saturation flux magnitude in Wb, positive
     * @param relativeSaturatedPermeability ratio P_0 / P_sat of the fully
     *        saturated slope to the unsaturated slope, greater than 1 (only
     *        used by {@link CurveKind#PIECEWISE_LINEAR})
     * @return nonlinear reluctance characteristic
     *
     * @throws IllegalArgumentException if the kind is null or any parameter is
     *         not finite, non-positive, or out of range
     */
    public static NonlinearReluctance of(final CurveKind kind, final double unsaturatedPermeance,
                                         final double saturationFlux,
                                         final double relativeSaturatedPermeability) {
        if (kind == null) {
            throw new IllegalArgumentException("Saturation curve kind must not be null");
        }
        if (!Double.isFinite(unsaturatedPermeance) || unsaturatedPermeance <= 0.0) {
            throw new IllegalArgumentException("Unsaturated permeance must be finite and positive,"
                    + " got: " + unsaturatedPermeance);
        }
        if (!Double.isFinite(saturationFlux) || saturationFlux <= 0.0) {
            throw new IllegalArgumentException("Saturation flux must be finite and positive, got: "
                    + saturationFlux);
        }
        if (!Double.isFinite(relativeSaturatedPermeability) || relativeSaturatedPermeability <= 1.0) {
            throw new IllegalArgumentException("Relative saturated permeability must be finite and"
                    + " greater than 1, got: " + relativeSaturatedPermeability);
        }
        return new NonlinearReluctance(kind, unsaturatedPermeance, saturationFlux,
                unsaturatedPermeance / relativeSaturatedPermeability);
    }

    /**
     * Creates a nonlinear saturation characteristic with the default
     * fully-saturated slope {@code P_0 / 1000} for the piecewise-linear kind.
     *
     * @param kind saturation curve family
     * @param unsaturatedPermeance small-signal permeance at F = 0 in H, positive
     * @param saturationFlux asymptotic saturation flux magnitude in Wb, positive
     * @return nonlinear reluctance characteristic
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static NonlinearReluctance of(final CurveKind kind, final double unsaturatedPermeance,
                                         final double saturationFlux) {
        return of(kind, unsaturatedPermeance, saturationFlux, DEFAULT_SATURATION_PERMEABILITY_RATIO);
    }

    /** Default ratio P_0 / P_sat of the piecewise-linear saturated slope. */
    public static final double DEFAULT_SATURATION_PERMEABILITY_RATIO = 1000.0;

    /**
     * Computes the branch flux at the given MMF drop.
     *
     * @param mmfDrop MMF drop across the branch in ampere-turns
     * @return branch flux in webers
     */
    public double flux(final double mmfDrop) {
        final double f = mmfDrop;
        return switch (kind) {
            case FROELICH -> unsaturatedPermeance * f
                    / (1.0 + unsaturatedPermeance * Math.abs(f) / saturationFlux);
            case ARCTANGENT -> 2.0 * saturationFlux / Math.PI
                    * Math.atan(Math.PI * unsaturatedPermeance * f / (2.0 * saturationFlux));
            case TANH -> saturationFlux * Math.tanh(unsaturatedPermeance * f / saturationFlux);
            case PIECEWISE_LINEAR -> piecewiseLinearFlux(f);
        };
    }

    /**
     * Piecewise-linear flux: linear with {@code P_0} up to the knee flux
     * {@code Phi_sat}, then continuing with the saturated slope.
     *
     * @param f MMF drop in ampere-turns
     * @return branch flux in webers
     */
    private double piecewiseLinearFlux(final double f) {
        final double kneeMMF = saturationFlux / unsaturatedPermeance;
        if (Math.abs(f) <= kneeMMF) {
            return unsaturatedPermeance * f;
        }
        final double sign = Math.signum(f);
        return sign * (saturationFlux + saturatedPermeance * (Math.abs(f) - kneeMMF));
    }

    /**
     * Computes the differential permeance {@code dPhi/dF} at the given MMF
     * drop - the Newton-Raphson stamp value of the branch. The result is
     * floored at {@code P_0 * 1e-12} so the MNA stamp stays finite for extreme
     * drives where the smooth curves saturate to machine precision.
     *
     * @param mmfDrop MMF drop across the branch in ampere-turns
     * @return differential permeance in H
     */
    public double differentialPermeance(final double mmfDrop) {
        final double permeance = switch (kind) {
            case FROELICH -> {
                final double denominator = 1.0
                        + unsaturatedPermeance * Math.abs(mmfDrop) / saturationFlux;
                yield unsaturatedPermeance / (denominator * denominator);
            }
            case ARCTANGENT -> {
                final double u = Math.PI * unsaturatedPermeance * mmfDrop / (2.0 * saturationFlux);
                yield unsaturatedPermeance / (1.0 + u * u);
            }
            case TANH -> {
                final double u = unsaturatedPermeance * mmfDrop / saturationFlux;
                yield unsaturatedPermeance * (1.0 - Math.tanh(u) * Math.tanh(u));
            }
            case PIECEWISE_LINEAR -> Math.abs(mmfDrop) <= saturationFlux / unsaturatedPermeance
                    ? unsaturatedPermeance : saturatedPermeance;
        };
        return Math.max(permeance, MIN_RELATIVE_DIFFERENTIAL_PERMEANCE * unsaturatedPermeance);
    }

    /**
     * Gets the curve family.
     *
     * @return saturation curve kind
     */
    public CurveKind getKind() {
        return kind;
    }

    /**
     * Gets the unsaturated (small-signal) permeance.
     *
     * @return permeance at F = 0 in H
     */
    public double getUnsaturatedPermeance() {
        return unsaturatedPermeance;
    }

    /**
     * Gets the saturation flux magnitude.
     *
     * @return asymptotic flux magnitude in Wb
     */
    public double getSaturationFlux() {
        return saturationFlux;
    }

    /**
     * Gets the fully saturated slope (piecewise-linear kind only).
     *
     * @return saturated permeance in H
     */
    public double getSaturatedPermeance() {
        return saturatedPermeance;
    }
}
