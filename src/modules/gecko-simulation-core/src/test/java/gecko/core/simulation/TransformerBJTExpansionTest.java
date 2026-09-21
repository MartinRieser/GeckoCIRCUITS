package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.solver.MatrixSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hidden-subcircuit expansions of the two classic composite components:
 * the ideal transformer (type 9, two coupled voltage-source windings) and
 * the BJT (type 33, base resistor + junction diodes + beta current sources).
 * Both must produce physically correct solutions, not inert/shorted elements.
 */
class TransformerBJTExpansionTest {

    /** Runs a netlist to steady state and returns the solver. */
    private MatrixSolver runToSteadyState(CircuitNetlist netlist, int steps) {
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        double dt = 1e-6;
        double t = 0;
        for (int step = 0; step <= steps; step++) {
            solver.buildMatrixA(netlist, dt, t, false);
            solver.buildVectorB(netlist, dt, t, false);
            solver.solve();
            solver.updateNodePotentials(dt, t);
            t += dt;
        }
        return solver;
    }

    // ===== Ideal transformer =====

    /**
     * 10 V source directly across the primary (n1:n2 = 10:2, gain 5); a
     * 100 ohm load on the secondary. Expect v_sec = 10/5 = 2 V, load current
     * 20 mA, and power conservation: 10 V * 4 mA primary = 2 V * 20 mA.
     */
    @Test
    void idealTransformerScalesSecondaryVoltage() {
        CircuitModel model = new CircuitModel();

        // V 10 V DC at (20,14) vertical: terminals (20,12) and (20,16)
        CircuitModel.ComponentData u = new CircuitModel.ComponentData(4, "U", 20, 14, 503);
        double[] uParams = new double[21];
        uParams[0] = 401.0;
        uParams[1] = 10.0;
        u.setRawParameters(uParams);
        model.addCircuitComponent(u);

        // Transformer TR (classic LK_TRANS) at (24,14) vertical: primary pins
        // (23,12)/(23,16), secondary pins (25,12)/(25,16). n1:n2 = 10:2, +1.
        CircuitModel.ComponentData tr = new CircuitModel.ComponentData(23, "TR", 24, 14, 503);
        tr.setRawParameters(new double[]{10.0, 2.0, 1.0});
        tr.setParameter("param0", 10.0);
        tr.setParameter("param1", 2.0);
        tr.setParameter("param2", 1.0);
        tr.setUniqueObjectIdentifier(500);
        model.addCircuitComponent(tr);

        // Load resistor 100 ohm at (28,14) vertical: terminals (28,12)/(28,16)
        CircuitModel.ComponentData rLoad = new CircuitModel.ComponentData(1, "R_Load", 28, 14, 503);
        rLoad.setRawParameters(new double[]{100.0});
        model.addCircuitComponent(rLoad);

        // Primary loop wires: V+ to primary top, primary bottom to V-
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{20, 12}, {21, 12}, {22, 12}, {23, 12}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{23, 16}, {22, 16}, {21, 16}, {20, 16}}));
        // Secondary to load
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{25, 12}, {26, 12}, {27, 12}, {28, 12}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{25, 16}, {26, 16}, {27, 16}, {28, 16}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // The transformer must expand into a voltage-source pair, not disappear
        CircuitNetlist finalNetlist = netlist;
        int primIdx = -1;
        int secIdx = -1;
        for (int i = 0; i < finalNetlist.getElementCount(); i++) {
            if (finalNetlist.getType(i) == CircuitTypCore.LK_U) {
                primIdx = i;
            }
            if (finalNetlist.getType(i) == CircuitTypCore.LK_TRANS) {
                secIdx = i;
            }
        }
        assertTrue(primIdx >= 0, "transformer primary (LK_U) missing from netlist");
        assertTrue(secIdx >= 0, "transformer secondary (LK_TRANS) missing from netlist");

        MatrixSolver solver = runToSteadyState(netlist, 2000);

        double vPrim = solver.getP()[netlist.getNodeX(primIdx)] - solver.getP()[netlist.getNodeY(primIdx)];
        assertEquals(10.0, vPrim, 1e-6, "primary must sit directly across the 10 V source");

        double vSec = solver.getP()[netlist.getNodeX(secIdx)] - solver.getP()[netlist.getNodeY(secIdx)];
        assertEquals(2.0, Math.abs(vSec), 1e-6,
                "secondary load voltage must be 10 V / gain 5 = 2 V, got " + vSec);

        // Power conservation: 10 V * i_prim = 2 V * 20 mA = 40 mW
        int zPrim = netlist.getNodeMax() + netlist.getVoltageSourceNumber(primIdx);
        double iPrim = Math.abs(solver.getP()[zPrim]);
        assertEquals(0.004, iPrim, 1e-9, "primary current must be 40 mW / 10 V = 4 mA, got " + iPrim);
    }

    // ===== BJT =====

    /**
     * NPN high-side switch: 10 V supply through a 1 k load into the
     * collector, emitter grounded, base driven with 2 V through 1 k.
     * beta * i_base >> available collector current, so the transistor
     * saturates: v_C collapses to ~0 V. With an inert BJT the collector
     * would sit at the full 10 V.
     */
    @Test
    void npnSwitchSaturatesAndConducts() {
        CircuitModel model = new CircuitModel();

        // BJT Q at (24,12) vertical: C=(24,10), B=(22,12), E=(24,14)
        CircuitModel.ComponentData q = new CircuitModel.ComponentData(33, "Q", 24, 12, 503);
        q.setRawParameters(new double[4]);
        q.setParameter("param1", 100.0); // forward beta
        q.setParameter("param2", 60.0);  // backward beta
        q.setParameter("param3", 0.1);   // base resistance
        q.setParameter("param4", 1.0);   // NPN
        model.addCircuitComponent(q);

        // Supply 10 V at (28,12) vertical: terminals (28,10)/(28,14)
        CircuitModel.ComponentData vcc = new CircuitModel.ComponentData(4, "Vcc", 28, 12, 503);
        double[] vccParams = new double[21];
        vccParams[0] = 401.0;
        vccParams[1] = 10.0;
        vcc.setRawParameters(vccParams);
        model.addCircuitComponent(vcc);

        // Load resistor 1 k at (26,8) horizontal: terminals (24,8)/(28,8)
        CircuitModel.ComponentData rLoad = new CircuitModel.ComponentData(1, "R_Load", 26, 8, 504);
        rLoad.setRawParameters(new double[]{1000.0});
        rLoad.setUniqueObjectIdentifier(600);
        model.addCircuitComponent(rLoad);

        // Base drive 2 V at (14,12) vertical: terminals (14,10)/(14,14)
        CircuitModel.ComponentData vb = new CircuitModel.ComponentData(4, "Vb", 14, 12, 503);
        double[] vbParams = new double[21];
        vbParams[0] = 401.0;
        vbParams[1] = 2.0;
        vb.setRawParameters(vbParams);
        model.addCircuitComponent(vb);

        // Base resistor 1 k at (18,12) horizontal: terminals (16,12)/(20,12)
        CircuitModel.ComponentData rb = new CircuitModel.ComponentData(1, "R_B", 18, 12, 504);
        rb.setRawParameters(new double[]{1000.0});
        model.addCircuitComponent(rb);

        // Collector up to the load, load to supply +
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{24, 10}, {24, 9}, {24, 8}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{28, 8}, {28, 9}, {28, 10}}));
        // Base drive: Vb+ through R_B to the base pin
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{14, 10}, {14, 11}, {14, 12}, {15, 12}, {16, 12}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{20, 12}, {21, 12}, {22, 12}}));
        // Emitter to the common bottom rail, both source returns on it
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{28, 14}, {27, 14}, {26, 14}, {25, 14}, {24, 14}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{14, 14}, {14, 15}, {14, 16}, {15, 16}, {16, 16}, {17, 16}, {18, 16}, {19, 16}, {20, 16}, {21, 16}, {22, 16}, {23, 16}, {24, 16}, {25, 16}, {26, 16}, {26, 15}, {26, 14}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Expansion: 5 synthetic elements replace the inert BJT
        assertTrue(netlist.getElementCount() >= 5, "BJT expansion must add elements");

        MatrixSolver solver = runToSteadyState(netlist, 2000);

        int rIdx = netlist.indexOfUid(600);
        assertTrue(rIdx >= 0, "load resistor missing from netlist");

        double vAcrossLoad = solver.getP()[netlist.getNodeX(rIdx)] - solver.getP()[netlist.getNodeY(rIdx)];
        double vDrop = Math.abs(vAcrossLoad);
        assertTrue(vDrop > 9.0,
                "saturated BJT must pull the collector low (load sees ~10 V), got " + vDrop);
    }

    /**
     * Classic mutual-inductance coupler (type 9, k between two coupled
     * inductors LKOP2): a 1 mH primary charged through a source loop induces
     * voltage on a 1 mH secondary with k = 0.9, M = 0.9 mH. The secondary
     * drives a resistive load and must show a non-zero coupled response.
     */
    @Test
    void mutualCouplingTransfersEnergyBetweenInductors() {
        CircuitModel model = new CircuitModel();

        // Primary inductor Lp 1 mH at (24,10) vertical: terminals (24,8)/(24,12)
        CircuitModel.ComponentData lp = new CircuitModel.ComponentData(12, "Lp", 24, 10, 503);
        lp.setRawParameters(new double[]{1e-3});
        lp.setUniqueObjectIdentifier(11);
        model.addCircuitComponent(lp);

        // Secondary inductor Ls 1 mH at (30,10) vertical: terminals (30,8)/(30,12)
        CircuitModel.ComponentData ls = new CircuitModel.ComponentData(12, "Ls", 30, 10, 503);
        ls.setRawParameters(new double[]{1e-3});
        ls.setUniqueObjectIdentifier(12);
        model.addCircuitComponent(ls);

        // Coupling declaration K: k = 0.9 between uids 11 and 12
        CircuitModel.ComponentData k = new CircuitModel.ComponentData(9, "K", 27, 7, 503);
        k.setRawParameters(new double[]{0.9, 11.0, 12.0});
        model.addCircuitComponent(k);

        // Primary loop: source 10 V DC at (20,10) vertical (20,8)/(20,12)
        CircuitModel.ComponentData u = new CircuitModel.ComponentData(4, "U", 20, 10, 503);
        double[] uParams = new double[21];
        uParams[0] = 401.0;
        uParams[1] = 10.0;
        u.setRawParameters(uParams);
        model.addCircuitComponent(u);

        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{20, 8}, {21, 8}, {22, 8}, {23, 8}, {24, 8}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{20, 12}, {20, 13}, {20, 14}, {21, 14}, {22, 14}, {23, 14}, {24, 14}, {24, 13}, {24, 12}}));

        // Secondary loop with load 10 ohm at (30,14) vertical (30,12)/(30,16)
        CircuitModel.ComponentData rLoad = new CircuitModel.ComponentData(1, "R_Load", 30, 14, 503);
        rLoad.setRawParameters(new double[]{10.0});
        rLoad.setUniqueObjectIdentifier(600);
        model.addCircuitComponent(rLoad);
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{30, 12}, {30, 13}, {30, 14}}));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{30, 16}, {30, 17}, {30, 18}}));
        // close the secondary loop back to the top of Ls
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{30, 18}, {29, 18}, {28, 18}, {28, 17}, {28, 16}, {28, 15}, {28, 14}, {28, 13}, {28, 12}, {28, 11}, {28, 10}, {28, 9}, {28, 8}, {29, 8}, {30, 8}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertTrue(!netlist.getAllCouplings().isEmpty(), "mutual coupling must be registered");

        MatrixSolver solver = runToSteadyState(netlist, 2000);

        int rIdx = netlist.indexOfUid(600);
        assertTrue(rIdx >= 0, "load resistor missing from netlist");
        double vLoad = Math.abs(solver.getP()[netlist.getNodeX(rIdx)] - solver.getP()[netlist.getNodeY(rIdx)]);
        assertTrue(vLoad > 1e-6, "coupled secondary must drive the load, got " + vLoad);
        assertTrue(Double.isFinite(vLoad), "coupled solve must stay finite");
    }
}
