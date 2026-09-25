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
package gecko.core.circuit.semiconductors;

/**
 * Smooth Shockley junction diode model for Newton-Raphson convergence.
 *
 * <p>Junction current and conductance:
 * <pre>
 *   i(v) = Is * (exp(v / (n * Vt)) - 1)
 *   g(v) = di/dv = Is / (n * Vt) * exp(v / (n * Vt))
 * </pre>
 *
 * <p>The saturation current is auto-calibrated so the exponential passes
 * exactly through the element's classic operating point (rated current at
 * forward voltage): {@code Is = I_ref * exp(-V_f / (n * Vt))} - no new user
 * inputs are needed and the smooth curve matches the piecewise-linear
 * threshold in the rated point. The exponential argument is clamped to avoid
 * floating-point overflow at strong forward bias.
 *
 * @see gecko.core.simulation.solver.NonlinearConvergenceController
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class ShockleyDiodeModel {

    /** Clamped exponential argument; exp(40) ~ 2.35e17 bounds the current. */
    public static final double MAX_EXPONENT = 40.0;

    private final double saturationCurrent;
    private final double emissionCoefficient;
    private final double thermalVoltage;

    private ShockleyDiodeModel(final double saturationCurrent, final double emissionCoefficient,
                               final double thermalVoltage) {
        this.saturationCurrent = saturationCurrent;
        this.emissionCoefficient = emissionCoefficient;
        this.thermalVoltage = thermalVoltage;
    }

    /**
     * Creates a Shockley model calibrated to pass through the classic
     * piecewise-linear operating point: rated current at forward voltage.
     *
     * @param forwardVoltage forward voltage V_f in volts (>= 0)
     * @param referenceCurrent rated current I_ref in amperes (> 0)
     * @param emissionCoefficient ideality factor n (> 0)
     * @param thermalVoltage thermal voltage Vt = kT/q in volts (> 0)
     * @return calibrated model instance
     */
    public static ShockleyDiodeModel fromOperatingPoint(final double forwardVoltage,
                                                        final double referenceCurrent,
                                                        final double emissionCoefficient,
                                                        final double thermalVoltage) {
        if (forwardVoltage < 0.0 || !Double.isFinite(forwardVoltage)) {
            throw new IllegalArgumentException(
                    "Forward voltage must be non-negative and finite, got: " + forwardVoltage);
        }
        if (referenceCurrent <= 0.0 || !Double.isFinite(referenceCurrent)) {
            throw new IllegalArgumentException(
                    "Reference current must be positive and finite, got: " + referenceCurrent);
        }
        if (emissionCoefficient <= 0.0 || !Double.isFinite(emissionCoefficient)) {
            throw new IllegalArgumentException(
                    "Emission coefficient must be positive and finite, got: " + emissionCoefficient);
        }
        if (thermalVoltage <= 0.0 || !Double.isFinite(thermalVoltage)) {
            throw new IllegalArgumentException(
                    "Thermal voltage must be positive and finite, got: " + thermalVoltage);
        }
        final double exponentAtOperatingPoint = forwardVoltage / (emissionCoefficient * thermalVoltage);
        final double saturationCurrent = referenceCurrent * Math.exp(-exponentAtOperatingPoint);
        return new ShockleyDiodeModel(saturationCurrent, emissionCoefficient, thermalVoltage);
    }

    /**
     * Junction current at the given junction voltage.
     *
     * @param voltage junction voltage in volts
     * @return junction current in amperes
     */
    public double current(final double voltage) {
        return saturationCurrent * Math.exp(clampedExponent(voltage)) - saturationCurrent;
    }

    /**
     * Small-signal conductance di/dv at the given junction voltage.
     *
     * @param voltage junction voltage in volts
     * @return conductance in siemens
     */
    public double conductance(final double voltage) {
        return saturationCurrent / (emissionCoefficient * thermalVoltage)
                * Math.exp(clampedExponent(voltage));
    }

    /**
     * Gets the calibrated saturation current.
     *
     * @return saturation current Is in amperes
     */
    public double getSaturationCurrent() {
        return saturationCurrent;
    }

    private double clampedExponent(final double voltage) {
        final double exponent = voltage / (emissionCoefficient * thermalVoltage);
        return Math.min(exponent, MAX_EXPONENT);
    }
}
