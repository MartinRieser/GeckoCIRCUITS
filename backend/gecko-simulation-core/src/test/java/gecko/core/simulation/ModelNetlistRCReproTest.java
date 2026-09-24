package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.solver.MatrixSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Model-level physics: U-R-C loop wired WITHOUT crossing component bodies,
 * capacitor + terminal labeled uOut. The capacitor must charge to the source
 * voltage and the uOut label must report it (positive, not reference-shifted).
 */
class ModelNetlistRCReproTest {

    private CircuitModel buildModel() {
        CircuitModel model = new CircuitModel();

        // Voltage source U at (20,14), vertical (orientation 503): terminals (20,12) and (20,16)
        CircuitModel.ComponentData u = new CircuitModel.ComponentData(4, "U", 20, 14, 503);
        double[] uParams = new double[21];
        uParams[0] = 401.0;   // DC
        uParams[1] = 10.0;    // 10 V
        u.setRawParameters(uParams);
        model.addCircuitComponent(u);

        // Resistor R_2 at (18,9), horizontal (504): terminals (16,9) and (20,9)
        CircuitModel.ComponentData r = new CircuitModel.ComponentData(1, "R_2", 18, 9, 504);
        r.setRawParameters(new double[]{1000.0});
        model.addCircuitComponent(r);

        // Capacitor C at (27,15), vertical: terminals (27,13) and (27,17); + terminal labeled uOut
        CircuitModel.ComponentData c = new CircuitModel.ComponentData(3, "C", 27, 15, 503);
        c.setRawParameters(new double[]{100e-9, 0.0});
        c.setTerminalXLabels(new String[]{"uOut"});
        model.addCircuitComponent(c);

        // Wires routed around component bodies (what a correct editor produces)
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{20, 12}, {20, 11}, {20, 10}, {20, 9}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{16, 9}, {16, 8}, {17, 8}, {18, 8}, {19, 8}, {20, 8}, {21, 8}, {22, 8}, {23, 8}, {24, 8}, {25, 8}, {26, 8}, {27, 8}, {27, 9}, {27, 10}, {27, 11}, {27, 12}, {27, 13}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{27, 17}, {26, 17}, {25, 17}, {24, 17}, {23, 17}, {22, 17}, {21, 17}, {20, 17}, {20, 16}}));
        return model;
    }

    @Test
    void modelLevelRCLoopSolves() {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(buildModel());

        int rIndex = -1;
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if (netlist.getType(i) == CircuitTypCore.LK_R) {
                rIndex = i;
            }
        }
        assertTrue(rIndex >= 0, "resistor missing from netlist");
        assertNotEquals(netlist.getNodeX(rIndex), netlist.getNodeY(rIndex),
                "resistor terminals must not be merged into one net (shorted by wire routing)");

        int uOutNode = netlist.getLabelResolver().getIndex("uOut");
        assertTrue(uOutNode >= 0, "uOut label not resolved in netlist");

        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        double dt = 1e-6;
        double t = 0;
        double midValue = 0;
        for (int step = 0; step <= 20000; step++) {
            solver.buildMatrixA(netlist, dt, t, false);
            solver.buildVectorB(netlist, dt, t, false);
            solver.solve();
            solver.updateNodePotentials(dt, t);
            t += dt;
            if (step == 5000) {
                midValue = solver.getP()[uOutNode];
            }
        }
        double uOut = solver.getP()[uOutNode];
        assertTrue(Double.isFinite(uOut), "uOut not finite: " + uOut);
        // after 5 ms >> 5*tau (tau = 0.1 ms) the cap is fully charged
        assertEquals(10.0, uOut, 0.05, "capacitor node should charge to +10 V, got " + uOut);
        assertTrue(midValue > 9.9, "capacitor should already be charged at t=5ms, got " + midValue);
    }
}
