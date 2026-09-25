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

import gecko.core.allg.SolverType;
import gecko.core.magnetic.NonlinearReluctance.CurveKind;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of the magnetic MNA network solver: air-gapped inductance
 * {@code L = N^2 / (R_core + R_gap)}, ideal-transformer MMF balance and
 * current/voltage turn ratios through magnetic coupling, and the saturation
 * behavior of a Newton-Raphson converged nonlinear core.
 */
class MagneticNetworkSolverTest {

    /** Core section length in meters. */
    private static final double CORE_LENGTH = 0.2;

    /** Core cross-section in square meters. */
    private static final double CORE_AREA = 1e-4;

    /** Core relative permeability. */
    private static final double CORE_PERMEABILITY = 2000.0;

    /** Air gap length in meters. */
    private static final double GAP_LENGTH = 1e-3;

    /** Turn count of the test windings (primary). */
    private static final int PRIMARY_TURNS = 100;

    /** Turn count of the test windings (secondary). */
    private static final int SECONDARY_TURNS = 50;

    /** Excitation current of the inductance test in amperes. */
    private static final double EXCITATION_CURRENT = 1.0;

    /** Relative tolerance of analytic flux/inductance comparisons. */
    private static final double ANALYTIC_TOLERANCE = 1e-12;

    /** Absolute flux tolerance of the nulled-core MMF balance in Wb. */
    private static final double FLUX_NULL_TOLERANCE = 1e-12;

    @Test
    void inductorWithAirGap_inductanceMatchesReluctanceSum() {
        final MagneticNetworkSolver solver = new MagneticNetworkSolver(SolverType.SOLVER_BE);
        solver.addWinding(new MagneticWinding("w1", PRIMARY_TURNS,
                MagneticNode.of(1), MagneticNode.REFERENCE));
        final ReluctanceBranch core = ReluctanceBranch.ofCore(CORE_LENGTH, CORE_AREA,
                CORE_PERMEABILITY);
        final ReluctanceBranch gap = ReluctanceBranch.ofAirGap(GAP_LENGTH, CORE_AREA);
        solver.addReluctance("core", MagneticNode.of(1), MagneticNode.of(2), core);
        solver.addReluctance("gap", MagneticNode.of(2), MagneticNode.REFERENCE, gap);

        solver.setWindingCurrent("w1", EXCITATION_CURRENT);
        solver.step(1e-3, 0.0);

        final double totalReluctance = core.getReluctance() + gap.getReluctance();
        final double expectedFlux = PRIMARY_TURNS * EXCITATION_CURRENT / totalReluctance;
        assertEquals(expectedFlux, solver.getWindingFlux("w1"),
                ANALYTIC_TOLERANCE * Math.abs(expectedFlux),
                "series core and gap reluctances must add");
        assertEquals(expectedFlux, solver.getBranchFlux("core"),
                ANALYTIC_TOLERANCE * Math.abs(expectedFlux));
        assertEquals(expectedFlux, solver.getBranchFlux("gap"),
                ANALYTIC_TOLERANCE * Math.abs(expectedFlux));

        // L = N * Phi / i = N^2 / (R_core + R_gap)
        final double inductance = PRIMARY_TURNS * solver.getWindingFlux("w1") / EXCITATION_CURRENT;
        assertEquals(PRIMARY_TURNS * PRIMARY_TURNS / totalReluctance, inductance,
                ANALYTIC_TOLERANCE * inductance);
    }

    @Test
    void transformer_currentRatioN2OverN1_nullsTheCoreFlux() {
        final MagneticNetworkSolver solver = transformerSolver();
        final double primaryCurrent = 0.5;
        final double secondaryCurrent = -(double) PRIMARY_TURNS / SECONDARY_TURNS * primaryCurrent;

        // Ideal-transformer current relation I1/I2 = N2/N1 balances the MMFs,
        // so the core flux vanishes
        solver.setWindingCurrent("w1", primaryCurrent);
        solver.setWindingCurrent("w2", secondaryCurrent);
        solver.step(1e-3, 0.0);

        assertEquals(0.0, solver.getWindingFlux("w1"), FLUX_NULL_TOLERANCE);
        assertEquals(0.0, solver.getWindingFlux("w2"), FLUX_NULL_TOLERANCE);
    }

    @Test
    void transformer_mmfsAddAroundTheCoreLoop() {
        final MagneticNetworkSolver solver = transformerSolver();
        final ReluctanceBranch core = ReluctanceBranch.ofReluctance(1.0 / 1e-3);
        final double primaryCurrent = 0.5;
        final double secondaryCurrent = 0.3;

        solver.setWindingCurrent("w1", primaryCurrent);
        solver.setWindingCurrent("w2", secondaryCurrent);
        solver.step(1e-3, 0.0);

        final double expectedFlux = (PRIMARY_TURNS * primaryCurrent
                + SECONDARY_TURNS * secondaryCurrent) / core.getReluctance();
        assertEquals(expectedFlux, solver.getWindingFlux("w1"),
                ANALYTIC_TOLERANCE * expectedFlux,
                "both windings must link the same core flux (MMF balance)");
        assertEquals(expectedFlux, solver.getWindingFlux("w2"),
                ANALYTIC_TOLERANCE * expectedFlux);
    }

    @Test
    void transformer_voltageRatioFollowsTurnsRatio() {
        final MagneticNetworkSolver solver = transformerSolver();
        final ReluctanceBranch core = ReluctanceBranch.ofReluctance(1.0 / 1e-3);
        final double amplitude = 0.1;
        final double frequency = 50.0;
        final double dt = 1e-5;
        final int steps = 4000;

        double previousCurrent = 0.0;
        double maxPrimaryVoltage = 0.0;
        for (int step = 1; step <= steps; step++) {
            final double time = step * dt;
            final double current = amplitude * Math.sin(2.0 * Math.PI * frequency * time);
            solver.setWindingCurrent("w1", current);
            solver.setWindingCurrent("w2", 0.0);
            solver.step(dt, time);

            final double flux = solver.getWindingFlux("w1");
            assertEquals(PRIMARY_TURNS * current / core.getReluctance(), flux,
                    ANALYTIC_TOLERANCE * Math.abs(flux),
                    "magnetizing flux must follow N1*i1/R at every sample");

            final double primaryVoltage = solver.getWindingVoltage("w1");
            final double secondaryVoltage = solver.getWindingVoltage("w2");
            assertEquals((double) SECONDARY_TURNS / PRIMARY_TURNS * primaryVoltage,
                    secondaryVoltage, 1e-9 * Math.max(1.0, Math.abs(primaryVoltage)),
                    "open-secondary voltage ratio must equal the turns ratio");

            final double discreteFluxDerivative = (double) PRIMARY_TURNS * PRIMARY_TURNS
                    * (current - previousCurrent) / dt / core.getReluctance();
            assertEquals(discreteFluxDerivative, primaryVoltage,
                    1e-9 * Math.max(1.0, Math.abs(primaryVoltage)),
                    "induced EMF must equal N * dPhi/dt");
            maxPrimaryVoltage = Math.max(maxPrimaryVoltage, Math.abs(primaryVoltage));
            previousCurrent = current;
        }

        // peak EMF of the magnetizing inductance: N1^2 * I * omega / R
        final double expectedPeak = (double) PRIMARY_TURNS * PRIMARY_TURNS * amplitude
                * 2.0 * Math.PI * frequency / core.getReluctance();
        assertEquals(expectedPeak, maxPrimaryVoltage, 1e-3 * expectedPeak,
                "peak EMF must match the magnetizing inductance analytics");
    }

    @Test
    void coreSaturation_fluxCompressesAndDifferentialInductanceDrops() {
        final MagneticNetworkSolver solver = new MagneticNetworkSolver(SolverType.SOLVER_BE);
        solver.addWinding(new MagneticWinding("w", 10, MagneticNode.of(1),
                MagneticNode.REFERENCE));
        final double unsaturatedPermeance = 1e-2;
        final double saturationFlux = 1e-3;
        solver.addNonlinearReluctance("core", MagneticNode.of(1), MagneticNode.REFERENCE,
                NonlinearReluctance.of(CurveKind.TANH, unsaturatedPermeance, saturationFlux));

        // small signal: L = N^2 * P_0
        solver.setWindingCurrent("w", 1e-4);
        solver.step(1e-3, 0.0);
        final double smallSignalInductance = 10.0 * solver.getWindingFlux("w") / 1e-4;
        assertEquals(100.0 * unsaturatedPermeance, smallSignalInductance, 1e-4);

        // deep saturation: the flux compresses toward the saturation magnitude
        solver.setWindingCurrent("w", 0.1);
        solver.step(1e-3, 1e-3);
        final double fluxAt01 = solver.getWindingFlux("w");
        assertTrue(Math.abs(fluxAt01) > 0.99 * saturationFlux,
                "deep drive must approach the saturation flux");
        assertTrue(Math.abs(fluxAt01) < 1.001 * saturationFlux,
                "the flux must not exceed the saturation magnitude");
        final double saturatedInductance = 10.0 * Math.abs(fluxAt01) / 0.1;
        assertTrue(saturatedInductance < 0.2 * smallSignalInductance,
                "saturation must drop the secant inductance");

        // differential inductance N * dPhi/di collapses far into saturation
        solver.setWindingCurrent("w", 0.101);
        solver.step(1e-3, 2e-3);
        final double differentialInductance = 10.0
                * (solver.getWindingFlux("w") - fluxAt01) / 0.001;
        assertTrue(differentialInductance < 1e-2 * smallSignalInductance,
                "differential inductance must collapse in saturation");

        // winding and core branch carry the same series flux
        assertEquals(solver.getWindingFlux("w"), solver.getBranchFlux("core"),
                1e-12 * saturationFlux);
    }

    @Test
    void topologyChangesAfterFirstStep_areRejected() {
        final MagneticNetworkSolver solver = singleWindingLoop();
        solver.setWindingCurrent("w1", 1.0);
        solver.step(1e-3, 0.0);

        assertThrows(IllegalStateException.class,
                () -> solver.addReluctance("extra", MagneticNode.of(1), MagneticNode.REFERENCE,
                        ReluctanceBranch.ofReluctance(1e3)));
        assertThrows(IllegalStateException.class,
                () -> solver.addWinding(new MagneticWinding("w2", 10,
                        MagneticNode.of(1), MagneticNode.REFERENCE)));
    }

    @Test
    void invalidInputs_areRejected() {
        final MagneticNetworkSolver solver = singleWindingLoop();

        assertThrows(IllegalArgumentException.class,
                () -> solver.setWindingCurrent("unknown", 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> solver.setWindingCurrent("w1", Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> solver.getWindingFlux("unknown"));
        assertThrows(IllegalArgumentException.class,
                () -> solver.getWindingVoltage("unknown"));
        assertThrows(IllegalArgumentException.class,
                () -> solver.getBranchFlux("unknown"));
        assertThrows(IllegalArgumentException.class, () -> solver.step(0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> solver.step(-1e-3, 0.0));
        assertThrows(IllegalArgumentException.class, () -> solver.step(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> solver.addReluctance("b", MagneticNode.of(1), null,
                        ReluctanceBranch.ofReluctance(1e3)));
        assertThrows(IllegalArgumentException.class,
                () -> solver.addReluctance("w1", MagneticNode.of(1), MagneticNode.REFERENCE,
                        ReluctanceBranch.ofReluctance(1e3)));
        assertThrows(IllegalArgumentException.class,
                () -> solver.addWinding(new MagneticWinding("w1", 10,
                        MagneticNode.of(1), MagneticNode.REFERENCE)));
        assertThrows(IllegalArgumentException.class, () -> solver.addWinding(null));
        assertThrows(IllegalArgumentException.class, () -> solver.getNodeMmf(null));
        assertThrows(IllegalArgumentException.class,
                () -> solver.getNodeMmf(MagneticNode.of(99)));
    }

    @Test
    void nodeMmf_readsRelativePotentials() {
        final MagneticNetworkSolver solver = singleWindingLoop();
        solver.setWindingCurrent("w1", 1.0);
        solver.step(1e-3, 0.0);

        // before any step the potentials are zero; after the step the driven
        // node sits at the winding MMF above the pinned reference
        assertEquals(0.0, solver.getNodeMmf(MagneticNode.REFERENCE), 0.0);
        assertTrue(solver.getNodeMmf(MagneticNode.of(1)) != 0.0);
    }

    /**
     * Creates a solver with one winding on a linear core in a single loop.
     *
     * @return configured magnetic network solver
     */
    private static MagneticNetworkSolver singleWindingLoop() {
        final MagneticNetworkSolver solver = new MagneticNetworkSolver(SolverType.SOLVER_BE);
        solver.addWinding(new MagneticWinding("w1", PRIMARY_TURNS,
                MagneticNode.of(1), MagneticNode.REFERENCE));
        solver.addReluctance("core", MagneticNode.of(1), MagneticNode.REFERENCE,
                ReluctanceBranch.ofReluctance(1e4));
        return solver;
    }

    /**
     * Creates a two-winding transformer: both windings and the core reluctance
     * in one series magnetic loop (nodes 1 - 2 - 3).
     *
     * @return configured magnetic network solver
     */
    private static MagneticNetworkSolver transformerSolver() {
        final MagneticNetworkSolver solver = new MagneticNetworkSolver(SolverType.SOLVER_BE);
        solver.addWinding(new MagneticWinding("w1", PRIMARY_TURNS,
                MagneticNode.of(1), MagneticNode.of(2)));
        solver.addWinding(new MagneticWinding("w2", SECONDARY_TURNS,
                MagneticNode.of(2), MagneticNode.of(3)));
        solver.addReluctance("core", MagneticNode.of(3), MagneticNode.of(1),
                ReluctanceBranch.ofReluctance(1.0 / 1e-3));
        return solver;
    }
}
