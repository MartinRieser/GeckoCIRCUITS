package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.ComponentTerminals;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.MatrixSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strategy 2: Physics Conservation Laws (KCL, KVL, and Tellegen's Theorem) as Automated Oracles.
 *
 * <p>Tellegen's theorem proves that for any lumped physical network obeying Kirchhoff's laws:
 * <pre>
 *     &Sigma;_{k=1}^B v_k(t) &middot; i_k(t) = 0
 * </pre>
 * holds at every instant of time regardless of whether elements are linear, nonlinear,
 * time-varying, or dynamic. Any sign inversion in MNA stampers, current calculations,
 * or history terms produces a non-zero power residual.</p>
 */
class PhysicsConservationOracleTest {

    private static final double POWER_TOLERANCE_WATTS = 1.0e-3;
    private static final double KCL_TOLERANCE_AMPS = 1.0e-4;

    private static CircuitModel.ComponentData comp(int type, String name, int x, int y, int orient, double[] params) {
        CircuitModel.ComponentData c = new CircuitModel.ComponentData(type, name, x, y, orient);
        c.setRawParameters(params);
        c.setUniqueObjectIdentifier(Math.abs((long) name.hashCode()) + 200L);
        return c;
    }

    /**
     * Asserts KCL at every node and Tellegen power balance (&Sigma; V_k &middot; I_k == 0) across all steps.
     */
    private static void assertConservationLaws(CircuitModel model, SolverType solverType, double dt, int stepCount) {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        MatrixSolver solver = new MatrixSolver(solverType);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());

        solver.buildMatrixA(netlist, dt, 0.0, false);

        ComponentCurrentCalculator currentCalc = new ComponentCurrentCalculator();

        for (int step = 0; step < stepCount; step++) {
            double time = step * dt;
            solver.buildMatrixA(netlist, dt, time, false);
            solver.buildVectorB(netlist, dt, time, false);
            solver.solve();

            // Run semiconductor convergence loop (re-solving until switch states stabilize)
            double stoergroesse = 1.0;
            boolean isNewIteration = false;
            int errorCounter = 0;
            while (true) {
                boolean statesChanged = currentCalc.calculateComponentCurrents(
                        solver, netlist, stoergroesse, dt, time, isNewIteration, errorCounter);
                if (!statesChanged) {
                    break;
                }
                isNewIteration = true;
                if (++errorCounter > 100) {
                    fail("Switch states did not converge at step " + step);
                }
                if (errorCounter > 2) {
                    stoergroesse *= 0.99;
                }
                solver.buildMatrixA(netlist, dt, time, false);
                solver.buildVectorB(netlist, dt, time, false);
                solver.solve();
            }

            double[] p = solver.getP();
            double[] iCurrent = solver.getICurrent();
            int elementCount = netlist.getElementCount();

            // 1. KCL Check at each non-ground node
            double[] nodeKclResidual = new double[netlist.getNodeMax() + 1];
            for (int e = 0; e < elementCount; e++) {
                int nx = netlist.getNodeX(e);
                int ny = netlist.getNodeY(e);
                double current = iCurrent[e];
                // Current flows from nx to ny
                if (nx <= netlist.getNodeMax()) nodeKclResidual[nx] -= current;
                if (ny <= netlist.getNodeMax()) nodeKclResidual[ny] += current;
            }

            for (int n = 1; n <= netlist.getNodeMax(); n++) {
                final int node = n;
                final int currentStep = step;
                final double currentTime = time;
                assertEquals(0.0, nodeKclResidual[n], KCL_TOLERANCE_AMPS,
                        () -> "KCL violated at node " + node + " at step " + currentStep + " (time=" + currentTime + ")");
            }

            // 2. Tellegen's Theorem Check: Sum(V_branch * I_branch) == 0
            double totalPower = 0.0;
            for (int e = 0; e < elementCount; e++) {
                int nx = netlist.getNodeX(e);
                int ny = netlist.getNodeY(e);
                double vx = nx < p.length ? p[nx] : 0.0;
                double vy = ny < p.length ? p[ny] : 0.0;
                double vBranch = vx - vy;
                double iBranch = iCurrent[e];
                totalPower += vBranch * iBranch;
            }

            final int currentStep = step;
            final double currentTime = time;
            final double finalPower = totalPower;
            assertEquals(0.0, totalPower, POWER_TOLERANCE_WATTS,
                    () -> "Tellegen power balance violated at step " + currentStep + " (time=" + currentTime + ", P=" + finalPower + " W)");

            solver.updateNodePotentials(dt, time);
        }
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class)
    void rlcSeriesResonantCircuitSatisfiesTellegenAndKcl(SolverType solver) {
        CircuitModel model = new CircuitModel();
        // V_dc(24V) - R(5 Ohm) - L(1mH) - C(10uF) - GND
        CircuitModel.ComponentData vSrc = comp(4, "V_dc", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 24.0});
        CircuitModel.ComponentData r = comp(1, "R1", 10, 10, ComponentTerminals.WEST_EAST, new double[]{5.0});
        CircuitModel.ComponentData l = comp(2, "L1", 16, 10, ComponentTerminals.WEST_EAST, new double[]{1.0e-3});
        CircuitModel.ComponentData c = comp(3, "C1", 22, 10, ComponentTerminals.WEST_EAST, new double[]{10.0e-6});
        model.getCircuitComponents().addAll(List.of(vSrc, r, l, c));

        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {8, 10}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{12, 10}, {14, 10}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{18, 10}, {20, 10}}));
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{24, 10}, {24, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        assertConservationLaws(model, solver, 5.0e-6, 100);
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class)
    void resistiveBridgeCircuitSatisfiesTellegenAndKcl(SolverType solver) {
        CircuitModel model = new CircuitModel();
        // Wheatstone bridge: V_src (10V) across (R1-R2) and (R3-R4) with bridge resistor R5
        CircuitModel.ComponentData vSrc = comp(4, "V_in", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 10.0});
        CircuitModel.ComponentData r1 = comp(1, "R1", 10, 6, ComponentTerminals.WEST_EAST, new double[]{100.0});
        CircuitModel.ComponentData r2 = comp(1, "R2", 16, 6, ComponentTerminals.WEST_EAST, new double[]{150.0});
        CircuitModel.ComponentData r3 = comp(1, "R3", 10, 14, ComponentTerminals.WEST_EAST, new double[]{200.0});
        CircuitModel.ComponentData r4 = comp(1, "R4", 16, 14, ComponentTerminals.WEST_EAST, new double[]{120.0});
        CircuitModel.ComponentData r5 = comp(1, "R5", 13, 10, ComponentTerminals.NORTH_SOUTH, new double[]{50.0});
        model.getCircuitComponents().addAll(List.of(vSrc, r1, r2, r3, r4, r5));

        // Connect source to bridge top & bottom
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {8, 10}, {8, 6}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{8, 10}, {8, 14}}));
        // Bridge center connection
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{12, 6}, {13, 8}, {14, 6}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{12, 14}, {13, 12}, {14, 14}}));
        // Return to ground
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{18, 6}, {20, 6}, {20, 14}, {18, 14}, {20, 10}, {20, 18}, {2, 18}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        assertConservationLaws(model, solver, 1.0e-5, 20);
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class)
    void diodeHalfWaveRectifierSatisfiesTellegenAndKcl(SolverType solver) {
        CircuitModel model = new CircuitModel();
        // AC voltage source (50Hz, 50V peak) -> Diode -> R_load (20 Ohm)
        double[] acParams = new double[25];
        acParams[0] = 402;
        acParams[2] = 50.0;
        acParams[20] = 50.0;
        CircuitModel.ComponentData vAc = comp(4, "V_ac", 4, 10, ComponentTerminals.EAST_WEST, acParams);
        CircuitModel.ComponentData diode = comp(6, "D1", 10, 10, ComponentTerminals.WEST_EAST, new double[]{10e-3, 0.7, 10e-3, 1e7});
        CircuitModel.ComponentData rLoad = comp(1, "R_L", 16, 10, ComponentTerminals.WEST_EAST, new double[]{20.0});
        model.getCircuitComponents().addAll(List.of(vAc, diode, rLoad));

        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {8, 10}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{12, 10}, {14, 10}}));
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{18, 10}, {18, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        assertConservationLaws(model, solver, 2.0e-5, 100);
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class)
    void parallelCurrentSourceAndCapacitorSatisfiesTellegenAndKcl(SolverType solver) {
        CircuitModel model = new CircuitModel();
        // I_dc (2 A) in parallel with C (50 uF) and R (100 Ohm)
        CircuitModel.ComponentData iSrc = comp(5, "I1", 4, 10, ComponentTerminals.WEST_EAST, new double[]{401, 2.0});
        CircuitModel.ComponentData r1 = comp(1, "R1", 10, 10, ComponentTerminals.WEST_EAST, new double[]{100.0});
        CircuitModel.ComponentData c1 = comp(3, "C1", 16, 10, ComponentTerminals.WEST_EAST, new double[]{50.0e-6});
        model.getCircuitComponents().addAll(List.of(iSrc, r1, c1));

        // Connect tops (input terminals) together
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {8, 10}, {14, 10}}));
        // Connect bottoms (output terminals) to GND
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{12, 10}, {18, 10}, {18, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        assertConservationLaws(model, solver, 1.0e-5, 50);
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class)
    void thermalDomainNetworkSatisfiesEnergyConservation(SolverType solver) {
        CircuitModel model = new CircuitModel();
        // TH_TEMP (50 K) -> TH_RTH (2 K/W) -> TH_CTH (1e-3 J/K) in parallel with TH_RTH (5 K/W) -> GND
        CircuitModel.ComponentData tSrc = comp(45, "T_in", 4, 10, ComponentTerminals.EAST_WEST, new double[]{401, 50.0});
        tSrc.setFamily("THERM");
        CircuitModel.ComponentData rth1 = comp(46, "Rth_1", 10, 10, ComponentTerminals.WEST_EAST, new double[]{2.0});
        rth1.setFamily("THERM");
        CircuitModel.ComponentData cth1 = comp(47, "Cth_1", 16, 6, ComponentTerminals.WEST_EAST, new double[]{1.0e-3});
        cth1.setFamily("THERM");
        CircuitModel.ComponentData rth2 = comp(46, "Rth_2", 16, 14, ComponentTerminals.WEST_EAST, new double[]{5.0});
        rth2.setFamily("THERM");
        model.getThermalComponents().addAll(List.of(tSrc, rth1, cth1, rth2));

        model.getConnections().add(new CircuitModel.ConnectionData("THERMAL", new int[][]{{6, 10}, {8, 10}}));
        model.getConnections().add(new CircuitModel.ConnectionData("THERMAL", new int[][]{{12, 10}, {14, 10}, {14, 6}}));
        model.getConnections().add(new CircuitModel.ConnectionData("THERMAL", new int[][]{{14, 10}, {14, 14}}));
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("THERMAL",
                new int[][]{{18, 6}, {20, 6}, {20, 14}, {18, 14}, {20, 10}, {20, 18}, {2, 18}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        assertConservationLaws(model, solver, 1.0e-5, 50);
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class)
    void mosfetAntiparallelBodyDiodeFreewheelingSatisfiesTellegenAndKcl(SolverType solver) {
        CircuitModel model = new CircuitModel();
        // Reverse-biased DC source (-20V at Drain relative to Source) -> MOSFET (gate OFF, body diode uF=0.8V) -> R (10 Ohm)
        // Because V_SD = 20V > 0.8V, the antiparallel body diode conducts ~1.92 A while satisfying KCL and Tellegen's theorem
        CircuitModel.ComponentData vSrc = comp(4, "V_rev", 4, 10, ComponentTerminals.WEST_EAST, new double[]{401, 20.0});
        CircuitModel.ComponentData mosfet = comp(28, "M1", 10, 10, ComponentTerminals.WEST_EAST, new double[]{1e7, 0.8, 10e-3, 1e7});
        CircuitModel.ComponentData rLoad = comp(1, "R1", 16, 10, ComponentTerminals.WEST_EAST, new double[]{10.0});
        model.getCircuitComponents().addAll(List.of(vSrc, mosfet, rLoad));

        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{6, 10}, {8, 10}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{12, 10}, {14, 10}}));
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{18, 10}, {18, 14}, {2, 14}, {2, 10}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        assertConservationLaws(model, solver, 1.0e-5, 40);
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class)
    void bjtCommonEmitterAmplifierSatisfiesTellegenAndKcl(SolverType solver) {
        CircuitModel model = new CircuitModel();
        // V_cc (12V) -> R_C (100 Ohm) -> BJT C(12,8), E(12,12) -> GND, with V_bb (5V) -> R_B (1000 Ohm) -> BJT B(10,10)
        CircuitModel.ComponentData vcc = comp(4, "V_cc", 20, 10, ComponentTerminals.SOUTH_NORTH, new double[]{401, 12.0});
        CircuitModel.ComponentData rc = comp(1, "R_c", 16, 6, ComponentTerminals.WEST_EAST, new double[]{100.0});
        CircuitModel.ComponentData vbb = comp(4, "V_bb", 2, 10, ComponentTerminals.SOUTH_NORTH, new double[]{401, 5.0});
        CircuitModel.ComponentData rb = comp(1, "R_b", 6, 10, ComponentTerminals.WEST_EAST, new double[]{1000.0});
        CircuitModel.ComponentData bjt = comp(33, "Q1", 12, 10, ComponentTerminals.NORTH_SOUTH, new double[]{100.0, 1.0, 10.0, 1.0});
        model.getCircuitComponents().addAll(List.of(vcc, rc, vbb, rb, bjt));

        // V_cc+(20,8) -> (20,6) -> R_c(18,6); R_c(14,6) -> (12,6) -> Q1_C(12,8)
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{20, 8}, {20, 6}, {18, 6}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{14, 6}, {12, 6}, {12, 8}}));
        // V_bb+(2,8) -> (2,10) wait: V_bb at (2,10) SOUTH_NORTH has + at (2,8), - at (2,12)
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{2, 8}, {4, 8}, {4, 10}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{8, 10}, {10, 10}}));
        // Ground return: V_bb-(2,12), Q1_E(12,12), V_cc-(20,12)
        CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK",
                new int[][]{{2, 12}, {12, 12}, {20, 12}});
        gnd.setLabel("GND");
        model.getConnections().add(gnd);

        assertConservationLaws(model, solver, 1.0e-5, 40);
    }
}
