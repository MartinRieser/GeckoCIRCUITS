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
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitialConditionSolverTest {

    private InitialConditionSolver solver;
    private MatrixSolver matrixSolver;

    private static final double EPSILON = 1.0e-12;

    @BeforeEach
    void setUp() {
        solver = new InitialConditionSolver(SolverType.SOLVER_BE);
        matrixSolver = new MatrixSolver(SolverType.SOLVER_BE);
    }

    @Test
    void testCapacitorInitialVoltageZeroHonoredEvenWithSavedStateSlots() {
        matrixSolver.initializeMatrices(5, 1, 1);

        SimpleNetList netlist = new SimpleNetList();
        // C with C=2e-5, uC0=0.0 (user set to 0V), but saved state slots 4 and 5 having 5V operating point
        double[] params = new double[]{2.0e-5, 0.0, 0.0, 4.91, 4.91, 0.0};
        netlist.addComponent(CircuitTypCore.LK_C, 1, 0, params);

        solver.setInitialConditions(matrixSolver, netlist, SolverType.SOLVER_BE);

        // Node 1 should be 0.0 V, NOT 4.91 V from saved state slots
        assertEquals(0.0, matrixSolver.getPALT()[1], EPSILON,
                "0V initial voltage must be honored and not fall back to legacy saved-state operating point");
    }

    @Test
    void testCapacitorInitialVoltageNonZero() {
        matrixSolver.initializeMatrices(5, 1, 1);

        SimpleNetList netlist = new SimpleNetList();
        double[] params = new double[]{2.0e-5, 8.5, 0.0, 4.91, 4.91, 0.0};
        netlist.addComponent(CircuitTypCore.LK_C, 1, 0, params);

        solver.setInitialConditions(matrixSolver, netlist, SolverType.SOLVER_BE);

        assertEquals(8.5, matrixSolver.getPALT()[1], EPSILON, "Explicit initial voltage must be applied");
    }

    @Test
    void testInductorInitialCurrentZeroHonoredEvenWithSavedStateSlot() {
        matrixSolver.initializeMatrices(5, 1, 1);

        SimpleNetList netlist = new SimpleNetList();
        // L with L=1e-3, iL0=0.0 (user set to 0A), but saved current slot 2 having 4.25A operating point
        double[] params = new double[]{1.0e-3, 0.0, 4.25};
        netlist.addComponent(CircuitTypCore.LK_L, 1, 0, params);

        solver.setInitialConditions(matrixSolver, netlist, SolverType.SOLVER_BE);

        // Element current should be 0.0 A, NOT 4.25 A from saved state slot
        assertEquals(0.0, matrixSolver.getIALT()[0], EPSILON,
                "0A initial current must be honored and not fall back to legacy saved-state operating point");
    }

    @Test
    void testInductorInitialCurrentNonZero() {
        matrixSolver.initializeMatrices(5, 1, 1);

        SimpleNetList netlist = new SimpleNetList();
        double[] params = new double[]{1.0e-3, 3.2, 4.25};
        netlist.addComponent(CircuitTypCore.LK_L, 1, 0, params);

        solver.setInitialConditions(matrixSolver, netlist, SolverType.SOLVER_BE);

        assertEquals(3.2, matrixSolver.getIALT()[0], EPSILON, "Explicit initial current must be applied");
    }
}
