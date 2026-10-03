package gecko.core.circuit.netlist;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolution contract of the mutual-inductance coupler (type 9):
 * uid references, legacy name fallback, honest build warnings, and
 * synthetic expansion uids that must not collide with real components.
 */
class MutualCouplingResolutionTest {

    /** Two coupled inductors wired as a flyback-ish pair, plus a coupler. */
    private CircuitModel coupledPairModel(double[] couplerRaw, String[] couplerStrings) {
        CircuitModel model = new CircuitModel();

        CircuitModel.ComponentData lp = new CircuitModel.ComponentData(12, "Lp", 24, 10, 503);
        lp.setRawParameters(new double[]{1e-3});
        lp.setUniqueObjectIdentifier(11);
        model.addCircuitComponent(lp);

        CircuitModel.ComponentData ls = new CircuitModel.ComponentData(12, "Ls", 30, 10, 503);
        ls.setRawParameters(new double[]{1e-3});
        ls.setUniqueObjectIdentifier(12);
        model.addCircuitComponent(ls);

        CircuitModel.ComponentData k = new CircuitModel.ComponentData(9, "K", 27, 7, 503);
        k.setRawParameters(couplerRaw);
        if (couplerStrings != null) {
            k.setParameterStrings(couplerStrings.clone());
        }
        model.addCircuitComponent(k);

        // Wires so the build takes the real topology path (wire-less models
        // degenerate into a dummy netlist that ignores couplers entirely).
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{24, 8}, {25, 8}, {26, 8}, {27, 8}, {28, 8}, {29, 8}, {30, 8}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{24, 12}, {24, 13}, {25, 13}, {26, 13}, {27, 13}, {28, 13}, {29, 13}, {30, 13}, {30, 12}}));
        return model;
    }

    @Test
    void resolvedCouplerRegistersWithoutWarnings() {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(
                coupledPairModel(new double[]{0.9, 11.0, 12.0}, null));
        assertFalse(netlist.getAllCouplings().isEmpty(), "coupling must register");
        assertTrue(netlist.getBuildWarnings().isEmpty(),
                "a successfully registered coupler must not emit warnings, got: "
                        + netlist.getBuildWarnings());
    }

    /**
     * Legacy classic export: raw slots carry classic-internal indices that
     * never match a uid; the winding names in parameterString[0..1] must
     * resolve the pair instead.
     */
    @Test
    void legacyCouplerResolvesByWindingNames() {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(
                coupledPairModel(new double[]{0.98, 11.0, 12.0, 14.0, 10.0, 3.0, 4.0},
                        new String[]{"/Lp", "/Ls", "1"}));
        assertFalse(netlist.getAllCouplings().isEmpty(),
                "legacy coupler must resolve via parameterString names");
        assertTrue(netlist.getBuildWarnings().isEmpty(),
                "resolved legacy coupler must not warn, got: " + netlist.getBuildWarnings());
        assertEquals(0.98, netlist.getAllCouplings().get(0).getCouplingCoefficient(), 1e-12);
    }

    @Test
    void couplerWithoutReferenceSlotsWarns() {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(
                coupledPairModel(new double[]{0.9}, null));
        assertTrue(netlist.getAllCouplings().isEmpty());
        assertTrue(netlist.getBuildWarnings().size() >= 1
                        && netlist.getBuildWarnings().get(0).contains("no inductor references"),
                "short raw array must warn, got: " + netlist.getBuildWarnings());
    }

    @Test
    void unresolvableCouplerWarnsPrecisely() {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(
                coupledPairModel(new double[]{0.9, 999.0, 12.0}, null));
        assertTrue(netlist.getAllCouplings().isEmpty());
        assertTrue(netlist.getBuildWarnings().size() >= 1
                        && netlist.getBuildWarnings().get(0).contains("could not be resolved"),
                "dangling uid reference must warn, got: " + netlist.getBuildWarnings());
    }

    /**
     * Transformer (and BJT) expansion elements used to take uid+1/+2, which
     * collides with real components in dense new-format files; they must now
     * allocate above every uid in the model.
     */
    @Test
    void transformerExpansionUidsDoNotCollideWithRealComponents() {
        CircuitModel model = new CircuitModel();

        CircuitModel.ComponentData tr = new CircuitModel.ComponentData(23, "TR", 24, 10, 503);
        tr.setRawParameters(new double[]{10.0, 2.0, 1.0});
        tr.setParameter("param0", 10.0);
        tr.setParameter("param1", 2.0);
        tr.setParameter("param2", 1.0);
        tr.setUniqueObjectIdentifier(10);
        model.addCircuitComponent(tr);

        // Real load resistor deliberately carrying the uid the old code would
        // have assigned to the synthetic primary winding (10 + 1).
        CircuitModel.ComponentData rLoad = new CircuitModel.ComponentData(1, "R_Load", 30, 10, 503);
        rLoad.setRawParameters(new double[]{100.0});
        rLoad.setUniqueObjectIdentifier(11);
        model.addCircuitComponent(rLoad);

        // Wire primary pins (23,8)/(23,16) and secondary pins (25,8)/(25,16) to the resistor
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{23, 8}, {24, 8}, {25, 8}, {25, 12}, {25, 16}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{23, 16}, {24, 16}, {28, 16}, {29, 16}, {30, 16}}));
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{30, 8}, {29, 8}, {28, 8}, {27, 8}, {26, 8}, {25, 8}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        int resistorIdx = netlist.indexOfUid(11);
        assertTrue(resistorIdx >= 0, "real resistor must be in the netlist");
        assertEquals(CircuitTypCore.LK_R, netlist.getType(resistorIdx),
                "uid 11 must resolve to the real resistor, not a synthetic winding");

        // No duplicate uids among elements
        long[] uids = netlist.getElementUids();
        for (int i = 0; i < uids.length; i++) {
            for (int j = i + 1; j < uids.length; j++) {
                assertTrue(uids[i] != uids[j] || uids[i] == 0,
                        "duplicate element uid " + uids[i] + " at " + i + " and " + j);
            }
        }

        List<String> warnings = netlist.getBuildWarnings();
        assertTrue(warnings.isEmpty(), "unexpected warnings: " + warnings);
    }
}
