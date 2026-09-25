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
package gecko.core.thermal;

/**
 * Immutable data model of a Foster or Cauer thermal impedance network describing
 * the heat path from a power-dissipating chip junction to the ambient environment.
 *
 * <p><b>Foster network</b> - m series-connected parallel RC cells fitted directly
 * from the manufacturer datasheet transient thermal impedance curve
 * {@code Z_th(t) = SUM_i R_i * (1 - exp(-t / tau_i))}, with cell capacitances
 * derived as {@code C_i = tau_i / R_i}. Foster node temperatures other than the
 * junction are mathematical quantities without physical meaning.
 *
 * <p><b>Cauer network</b> - physical RC ladder: thermal resistances
 * {@code R_i} (die attach, TIM, case, heatsink layers) in series from the junction
 * node towards the ambient node, with heat capacitances {@code C_i} from each
 * ladder node to the ambient node. Cauer node temperatures correspond to
 * physical locations along the heat path.
 *
 * <p>The junction driving-point thermal impedance of both structures is a rational
 * function in the Laplace variable {@code s}; {@link #toCauer()} therefore converts
 * a Foster fit into its physically interpretable Cauer ladder by a continued-fraction
 * expansion of {@code Z_th(s)}:
 * <pre>
 *   Z_th(s) = SUM_i R_i / (1 + s*tau_i)
 *           = 1 / (s*C_1 + 1 / (R_1 + 1 / (s*C_2 + 1 / (R_2 + ...))))
 * </pre>
 * The expansion extracts alternating shunt capacitances and series resistances from
 * the leading polynomial coefficients of numerator and denominator. A Foster fit
 * whose continued-fraction expansion produces negative elements has no physical
 * Cauer realization; {@link #toCauer()} rejects such fits.
 *
 * <p>Units: thermal resistance in K/W, heat capacitance in J/K, time constants in
 * seconds, temperatures in degrees Celsius.
 *
 * @see ThermalNetworkSolver
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 1: Thermal Domain Engine)
 */
public final class ThermalRCModel {

    /** Structure kind of the thermal impedance network. */
    public enum NetworkKind {

        /** Series connection of parallel RC cells (datasheet fit). */
        FOSTER,

        /** Physical RC ladder network (layer stack). */
        CAUER
    }

    /** Round-off tolerance for expanded Cauer elements (J/K or K/W). */
    private static final double ELEMENT_REALIZABILITY_TOLERANCE = 1e-12;

    private final NetworkKind kind;
    private final double[] resistances;
    private final double[] capacitances;
    private final double[] timeConstants;
    private final double ambientTemperature;

    private ThermalRCModel(final NetworkKind kind, final double[] resistances,
                           final double[] capacitances, final double[] timeConstants,
                           final double ambientTemperature) {
        if (!Double.isFinite(ambientTemperature)) {
            throw new IllegalArgumentException("Ambient temperature must be finite, got: "
                    + ambientTemperature);
        }
        this.kind = kind;
        this.resistances = resistances.clone();
        this.capacitances = capacitances.clone();
        this.timeConstants = timeConstants.clone();
        this.ambientTemperature = ambientTemperature;
    }

    /**
     * Creates a Foster thermal impedance model from datasheet RC cells.
     *
     * <p>Each cell i contributes the exponential term
     * {@code R_i * (1 - exp(-t / tau_i))} to the junction step response; the
     * cell capacitances are derived as {@code C_i = tau_i / R_i}.
     *
     * @param rThKPerW thermal resistances of the cells in K/W, all positive
     * @param tauSeconds exponential time constants of the cells in seconds, all positive
     * @param ambientTemperatureC ambient temperature in degrees Celsius
     * @return Foster thermal RC model
     *
     * @throws IllegalArgumentException if the arrays are null, empty, of different
     *         lengths, or contain non-positive or non-finite entries
     */
    public static ThermalRCModel foster(final double[] rThKPerW, final double[] tauSeconds,
                                        final double ambientTemperatureC) {
        validateStageArrays(rThKPerW, tauSeconds, "thermal resistances", "time constants");
        final int stageCount = rThKPerW.length;
        final double[] capacitances = new double[stageCount];
        for (int i = 0; i < stageCount; i++) {
            capacitances[i] = tauSeconds[i] / rThKPerW[i];
        }
        return new ThermalRCModel(NetworkKind.FOSTER, rThKPerW, capacitances, tauSeconds,
                ambientTemperatureC);
    }

    /**
     * Creates a Cauer thermal impedance model from a physical RC ladder.
     *
     * <p>Stage i consists of the series thermal resistance {@code R_i} and the
     * shunt heat capacitance {@code C_i}; the stage time constant is reported
     * as {@code R_i * C_i} (for multi-stage ladders these are not the exponential
     * pole time constants of the response).
     *
     * @param rThKPerW series thermal resistances in K/W, all positive
     * @param cThJPerK shunt heat capacitances in J/K, all positive
     * @param ambientTemperatureC ambient temperature in degrees Celsius
     * @return Cauer thermal RC model
     *
     * @throws IllegalArgumentException if the arrays are null, empty, of different
     *         lengths, or contain non-positive or non-finite entries
     */
    public static ThermalRCModel cauer(final double[] rThKPerW, final double[] cThJPerK,
                                       final double ambientTemperatureC) {
        validateStageArrays(rThKPerW, cThJPerK, "thermal resistances", "heat capacitances");
        final int stageCount = rThKPerW.length;
        final double[] timeConstants = new double[stageCount];
        for (int i = 0; i < stageCount; i++) {
            timeConstants[i] = rThKPerW[i] * cThJPerK[i];
        }
        return new ThermalRCModel(NetworkKind.CAUER, rThKPerW, cThJPerK, timeConstants,
                ambientTemperatureC);
    }

    /**
     * Validates the two stage parameter arrays shared by both factory methods.
     *
     * @param first first stage array (thermal resistances)
     * @param second second stage array (capacitances or time constants)
     * @param firstName display name of the first array for error messages
     * @param secondName display name of the second array for error messages
     */
    private static void validateStageArrays(final double[] first, final double[] second,
                                            final String firstName, final String secondName) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Stage arrays must not be null");
        }
        if (first.length == 0) {
            throw new IllegalArgumentException("Thermal RC model needs at least one stage");
        }
        if (first.length != second.length) {
            throw new IllegalArgumentException("Stage array length mismatch: " + firstName
                    + " has " + first.length + " entries but " + secondName + " has "
                    + second.length);
        }
        for (int i = 0; i < first.length; i++) {
            requirePositive(first[i], firstName + "[" + i + "]");
            requirePositive(second[i], secondName + "[" + i + "]");
        }
    }

    /**
     * Validates that a stage entry is finite and positive.
     *
     * @param value entry value
     * @param name display name of the entry for error messages
     */
    private static void requirePositive(final double value, final String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive, got: " + value);
        }
    }

    /**
     * Returns the physically interpretable Cauer ladder of this network.
     *
     * <p>For a CAUER model this is the model itself. For a FOSTER model the
     * continued-fraction expansion of the driving-point impedance
     * {@code Z_th(s)} is computed (see the class documentation); the result
     * reproduces the same junction thermal impedance.
     *
     * @return Cauer model with the same junction thermal impedance
     *
     * @throws IllegalStateException if the Foster fit has no physical Cauer
     *         realization (the expansion produced a non-positive element)
     */
    public ThermalRCModel toCauer() {
        if (kind == NetworkKind.CAUER) {
            return this;
        }
        final CauerExpansion expansion = expandFosterToCauer();
        final double[] stageTimeConstants = new double[expansion.resistances().length];
        for (int i = 0; i < stageTimeConstants.length; i++) {
            stageTimeConstants[i] = expansion.resistances()[i] * expansion.capacitances()[i];
        }
        return new ThermalRCModel(NetworkKind.CAUER, expansion.resistances(),
                expansion.capacitances(), stageTimeConstants, ambientTemperature);
    }

    /** Expanded Cauer ladder elements of one Foster-to-Cauer transformation. */
    private record CauerExpansion(double[] resistances, double[] capacitances) {
    }

    /**
     * Computes the Foster-to-Cauer continued-fraction expansion.
     *
     * <p>The driving-point impedance {@code Z(s) = N(s)/D(s)} with
     * {@code D(s) = PROD (1 + s*tau_i)} and
     * {@code N(s) = SUM_i R_i * PROD_{j != i} (1 + s*tau_j)} is expanded by
     * alternating extraction of a shunt capacitance (ratio of the leading
     * coefficients of denominator and numerator of the admittance) and a series
     * resistance (ratio of the leading coefficients of the impedance), each
     * removing the highest power of {@code s}. Polynomials are stored as
     * coefficient arrays in ascending powers of {@code s}.
     *
     * @return the Cauer ladder elements
     *
     * @throws IllegalStateException if the expansion produced a non-positive element
     */
    private CauerExpansion expandFosterToCauer() {
        final int stageCount = resistances.length;
        final double[] cauerR = new double[stageCount];
        final double[] cauerC = new double[stageCount];

        double[] numerator = buildFosterNumerator();
        double[] denominator = buildFosterDenominator();

        for (int k = 0; k < stageCount; k++) {
            // Shunt capacitance: admittance Y = denominator/numerator grows like
            // s * lead(denominator)/lead(numerator) for s -> infinity
            final double capacitance = denominator[denominator.length - 1]
                    / numerator[numerator.length - 1];
            cauerC[k] = capacitance;
            // denominator -= s * capacitance * numerator: the leading power
            // cancels exactly in exact arithmetic, so the slot is dropped
            denominator = subtractScaledShifted(denominator, numerator, capacitance);
            // Series resistance: impedance Z = numerator/denominator tends to
            // lead(numerator)/lead(denominator) for s -> infinity
            final double resistance = numerator[numerator.length - 1]
                    / denominator[denominator.length - 1];
            cauerR[k] = resistance;
            // numerator -= resistance * denominator (same degree, no shift):
            // the leading power cancels exactly in exact arithmetic
            numerator = subtractScaled(numerator, denominator, resistance);

            if (!isPositiveElement(capacitance) || !isPositiveElement(resistance)) {
                throw new IllegalStateException("Foster fit has no physical Cauer realization: "
                        + "expansion produced C[" + k + "]=" + capacitance + " J/K, R[" + k + "]="
                        + resistance + " K/W");
            }
        }
        return new CauerExpansion(cauerR, cauerC);
    }

    /**
     * Subtracts {@code scale * s * subtrahend} from {@code minuend} and drops
     * the highest power whose coefficient cancels exactly in exact arithmetic.
     *
     * @param minuend polynomial coefficient array (ascending powers)
     * @param subtrahend polynomial coefficient array (ascending powers), one
     *        degree below minuend
     * @param scale scaling factor of the shifted subtrahend
     * @return difference with the highest-power slot removed; empty when the
     *         minuend was a constant
     */
    private static double[] subtractScaledShifted(final double[] minuend, final double[] subtrahend,
                                                  final double scale) {
        final double[] difference = new double[minuend.length - 1];
        if (difference.length == 0) {
            return difference;
        }
        difference[0] = minuend[0];
        for (int i = 1; i < difference.length; i++) {
            difference[i] = minuend[i] - scale * subtrahend[i - 1];
        }
        return difference;
    }

    /**
     * Subtracts {@code scale * subtrahend} from {@code minuend} (same degree,
     * no shift) and drops the highest power whose coefficient cancels exactly
     * in exact arithmetic.
     *
     * @param minuend polynomial coefficient array (ascending powers)
     * @param subtrahend polynomial coefficient array (ascending powers), same
     *        degree as minuend
     * @param scale scaling factor of the subtrahend
     * @return difference with the highest-power slot removed; empty when the
     *         minuend was a constant
     */
    private static double[] subtractScaled(final double[] minuend, final double[] subtrahend,
                                           final double scale) {
        final double[] difference = new double[minuend.length - 1];
        for (int i = 0; i < difference.length; i++) {
            difference[i] = minuend[i] - scale * subtrahend[i];
        }
        return difference;
    }

    /**
     * Builds the numerator polynomial {@code N(s)} of the Foster driving-point
     * impedance: {@code N(s) = SUM_i R_i * PROD_{j != i} (1 + s*tau_j)}.
     *
     * @return coefficient array in ascending powers of s, degree stageCount - 1
     */
    private double[] buildFosterNumerator() {
        final int stageCount = resistances.length;
        final double[] numerator = new double[stageCount];
        for (int i = 0; i < stageCount; i++) {
            final double[] term = multiplyFirstOrderFactors(i);
            for (int j = 0; j < term.length; j++) {
                numerator[j] += resistances[i] * term[j];
            }
        }
        return numerator;
    }

    /**
     * Builds the denominator polynomial {@code D(s)} of the Foster driving-point
     * impedance: {@code D(s) = PROD_i (1 + s*tau_i)}.
     *
     * @return coefficient array in ascending powers of s, degree stageCount
     */
    private double[] buildFosterDenominator() {
        final int stageCount = resistances.length;
        double[] denominator = new double[]{1.0};
        for (int i = 0; i < stageCount; i++) {
            final double[] next = new double[denominator.length + 1];
            for (int j = 0; j < denominator.length; j++) {
                next[j] += denominator[j];
                next[j + 1] += timeConstants[i] * denominator[j];
            }
            denominator = next;
        }
        return denominator;
    }

    /**
     * Multiplies out {@code PROD_{j != i} (1 + s*tau_j)}.
     *
     * @param excludedIndex cell index excluded from the product
     * @return coefficient array in ascending powers of s, degree stageCount - 1
     */
    private double[] multiplyFirstOrderFactors(final int excludedIndex) {
        final int stageCount = resistances.length;
        double[] product = new double[]{1.0};
        for (int i = 0; i < stageCount; i++) {
            if (i == excludedIndex) {
                continue;
            }
            final double[] next = new double[product.length + 1];
            for (int j = 0; j < product.length; j++) {
                next[j] += product[j];
                next[j + 1] += timeConstants[i] * product[j];
            }
            product = next;
        }
        return product;
    }

    /**
     * Checks that an expanded Cauer element is physically realizable. The
     * value must be finite; a tolerance admits floating-point round-off on
     * exactly-zero elements while rejecting genuinely negative or unbounded
     * expansions.
     *
     * @param value expanded element value
     * @return true if the element is usable as a physical ladder element
     */
    private static boolean isPositiveElement(final double value) {
        return Double.isFinite(value)
                && (value > 0.0 || Math.abs(value) <= ELEMENT_REALIZABILITY_TOLERANCE);
    }

    /**
     * Computes the analytic junction step response of a FOSTER model.
     *
     * <p>{@code T_j(t) = T_ambient + P * SUM_i R_i * (1 - exp(-t / tau_i))}.
     * Not available for CAUER models, whose exponential terms are not the
     * individual stage time constants.
     *
     * @param timeSeconds time since the power step in seconds, non-negative
     * @param powerWatt applied step power in watts
     * @return junction temperature in degrees Celsius
     *
     * @throws IllegalStateException if this is a CAUER model
     * @throws IllegalArgumentException if timeSeconds is negative or not finite,
     *         or powerWatt is not finite
     */
    public double junctionStepResponse(final double timeSeconds, final double powerWatt) {
        if (kind != NetworkKind.FOSTER) {
            throw new IllegalStateException(
                    "Analytic step response is only available for FOSTER models");
        }
        if (!Double.isFinite(timeSeconds) || timeSeconds < 0.0) {
            throw new IllegalArgumentException("Time must be finite and non-negative, got: "
                    + timeSeconds);
        }
        if (!Double.isFinite(powerWatt)) {
            throw new IllegalArgumentException("Power must be finite, got: " + powerWatt);
        }
        double rise = 0.0;
        for (int i = 0; i < resistances.length; i++) {
            rise += resistances[i] * (1.0 - Math.exp(-timeSeconds / timeConstants[i]));
        }
        return ambientTemperature + powerWatt * rise;
    }

    /**
     * Gets the network structure kind.
     *
     * @return FOSTER or CAUER
     */
    public NetworkKind getKind() {
        return kind;
    }

    /**
     * Gets the number of RC stages.
     *
     * @return stage count, at least 1
     */
    public int getStageCount() {
        return resistances.length;
    }

    /**
     * Gets the thermal resistance of one stage.
     *
     * @param stageIndex stage index (0-based)
     * @return thermal resistance in K/W
     */
    public double getResistance(final int stageIndex) {
        return resistances[stageIndex];
    }

    /**
     * Gets the heat capacitance of one stage.
     *
     * @param stageIndex stage index (0-based)
     * @return heat capacitance in J/K
     */
    public double getCapacitance(final int stageIndex) {
        return capacitances[stageIndex];
    }

    /**
     * Gets the time constant of one stage. For FOSTER models these are the
     * exponential pole time constants of the response; for CAUER models they
     * are the stage products {@code R_i * C_i}.
     *
     * @param stageIndex stage index (0-based)
     * @return stage time constant in seconds
     */
    public double getTimeConstant(final int stageIndex) {
        return timeConstants[stageIndex];
    }

    /**
     * Gets a copy of all stage thermal resistances.
     *
     * @return thermal resistances in K/W, ordered from junction to ambient
     */
    public double[] getResistances() {
        return resistances.clone();
    }

    /**
     * Gets a copy of all stage heat capacitances.
     *
     * @return heat capacitances in J/K, ordered from junction to ambient
     */
    public double[] getCapacitances() {
        return capacitances.clone();
    }

    /**
     * Gets a copy of all stage time constants.
     *
     * @return stage time constants in seconds
     */
    public double[] getTimeConstants() {
        return timeConstants.clone();
    }

    /**
     * Gets the ambient temperature of the network.
     *
     * @return ambient temperature in degrees Celsius
     */
    public double getAmbientTemperature() {
        return ambientTemperature;
    }
}
