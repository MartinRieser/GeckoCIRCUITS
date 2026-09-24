package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.simulation.solver.MatrixSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused physics repro: DC source + resistor loop must produce v(node1) = 10 V.
 */
class VRLoopPhysicsReproTest {

    @Test
    void dcSourceResistorLoopSolvesToSourceVoltage() {
        // Element 0: LK_U between node 1 (vIn) and node 0 (ground), DC 10 V, voltage source number 1
        // Element 1: LK_R 1 kOhm between node 1 and node 0
        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R};
        int[] nodeX = {1, 1};
        int[] nodeY = {0, 0};
        int[] vsn = {1, -1};
        double[] uParams = new double[21];
        uParams[0] = 401.0;
        uParams[1] = 10.0;
        double[] rParams = new double[]{1000.0};
        double[][] params = {uParams, rParams};

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, vsn, params, 1, 1, 2);

        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        double dt = 1e-6;
        for (int step = 0; step < 10; step++) {
            solver.buildMatrixA(netlist, dt, step * dt, false);
            solver.buildVectorB(netlist, dt, step * dt, false);
            solver.solve();
            solver.updateNodePotentials(dt, step * dt);
        }
        double vIn = solver.getP()[1];
        assertTrue(Double.isFinite(vIn), "vIn not finite: " + vIn);
        assertEquals(10.0, vIn, 1e-6, "node 1 should sit at the 10 V source potential, got " + vIn);
    }
}
