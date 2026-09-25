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
package gecko.core.simulation;

import gecko.core.allg.SolverType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine-level verification that the sparse MNA backend reproduces the dense
 * reference waveforms on a real switched power circuit (buck converter with
 * semiconductor state machine and control domain).
 */
class SparseSolverEngineTest {

    /** Relative waveform tolerance of the dense-vs-sparse comparison. */
    private static final double WAVEFORM_RELATIVE_TOLERANCE = 1e-6;

    private static final String BUCK_CIRCUIT_REL = "buck_converter_web.ipes";

    private File resolveCircuit(final String relativePath) {
        java.nio.file.Path path = Paths.get("src/test/resources/ipes", relativePath);
        File file = path.toFile();
        if (!file.exists()) {
            path = Paths.get("backend/gecko-simulation-core/src/test/resources/ipes", relativePath);
            file = path.toFile();
        }
        assertTrue(file.exists(), "Circuit file not found: " + path.toAbsolutePath());
        return file;
    }

    private SimulationResult runBuck(final MatrixSolverKind kind) {
        final File circuit = resolveCircuit(BUCK_CIRCUIT_REL);
        final HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        final SimulationResult result = engine.runSimulation(SimulationConfig.builder()
                .circuitFile(circuit.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(SolverType.SOLVER_BE)
                .matrixSolverKind(kind)
                .build());
        assertTrue(result.isSuccess(), kind + " run failed: " + result.getErrorMessage());
        return result;
    }

    @Test
    void sparseBackend_reproducesDenseWaveforms() {
        final SimulationResult dense = runBuck(MatrixSolverKind.DENSE);
        final SimulationResult sparse = runBuck(MatrixSolverKind.SPARSE);

        assertEquals("MatrixSolver", dense.getMetadata().get("matrixSolver"));
        assertEquals("SparseMatrixSolver", sparse.getMetadata().get("matrixSolver"));

        final String[] names = dense.getSignalNames();
        assertEquals(names.length, sparse.getSignalNames().length,
                "both backends must record the same signals");
        assertTrue(names.length > 0, "waveforms must be recorded for the comparison");

        for (int signal = 0; signal < names.length; signal++) {
            assertEquals(names[signal], sparse.getSignalNames()[signal]);
            final float[] denseData = dense.getSignalData(signal);
            final float[] sparseData = sparse.getSignalData(signal);
            assertEquals(denseData.length, sparseData.length, "sample count must match");
            for (int i = 0; i < denseData.length; i++) {
                final double scale = Math.max(1.0, Math.abs(denseData[i]));
                assertEquals(denseData[i], sparseData[i], WAVEFORM_RELATIVE_TOLERANCE * scale,
                        "waveform '" + names[signal] + "' diverges at sample " + i);
            }
        }
    }

    @Test
    void autoKind_smallCircuit_selectsDense() {
        final SimulationResult result = runBuck(MatrixSolverKind.AUTO);
        assertEquals("MatrixSolver", result.getMetadata().get("matrixSolver"),
                "a small circuit must run on the dense reference backend");
    }
}
