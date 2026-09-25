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

import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DomainCoupler domain coupling orchestration.
 * Validates LK ↔ CONTROL ↔ THERM data transfers without GUI dependencies.
 *
 * @author Phase 4 test suite
 * @since v2.18.0 Phase 4 - Domain coupling
 */
class DomainCouplerTest {

    private DomainCoupler coupler;

    @BeforeEach
    void setUp() {
        coupler = new DomainCoupler();
    }

    /**
     * Test 1: Creating a DomainCoupler with default empty configuration
     * should not throw exceptions.
     */
    @Test
    void testCreateEmpty() {
        assertNotNull(coupler, "DomainCoupler should be created successfully");
        assertEquals(0, coupler.getLkNodeVoltagesForControl().length,
            "Default LK→CONTROL should have 0 nodes");
        assertEquals(0, coupler.getControlSignalsForLkSources().length,
            "Default CONTROL→LK should have 0 sources");
    }

    /**
     * Test 2: transferLkToControl() with null netlist should not throw.
     */
    @Test
    void testTransferLkToControlWithNullNetlist() {
        assertDoesNotThrow(() -> {
            coupler.transferLkToControl(null);
        }, "transferLkToControl should handle null netlist gracefully");
    }

    /**
     * Test 3: transferLkToControl() with empty mapping should not throw.
     */
    @Test
    void testTransferLkToControlWithEmptyMapping() {
        CircuitNetlist netlist = new CircuitNetlist();
        assertDoesNotThrow(() -> {
            coupler.transferLkToControl(netlist);
        }, "transferLkToControl should handle empty mapping gracefully");
    }

    /**
     * Test 4: configureLkToControlMapping() stores correct length.
     */
    @Test
    void testConfigureLkToControlMapping() {
        int[] lkNodeIndices = {0, 1, 2, 5};
        coupler.configureLkToControlMapping(lkNodeIndices);

        double[] voltages = coupler.getLkNodeVoltagesForControl();
        assertEquals(4, voltages.length,
            "Configured LK→CONTROL mapping should have 4 voltage slots");
    }

    /**
     * Test 5: configureControlToLkSourceMapping() stores correct length.
     */
    @Test
    void testConfigureControlToLkSourceMapping() {
        int[] controlOutputIndices = {1, 3, 5};
        coupler.configureControlToLkSourceMapping(controlOutputIndices);

        double[] signals = coupler.getControlSignalsForLkSources();
        assertEquals(3, signals.length,
            "Configured CONTROL→LK source mapping should have 3 source slots");
    }

    /**
     * Test 6: configureControlToLkSwitchMapping() correctly converts
     * control outputs (>0.5 = true, ≤0.5 = false).
     */
    @Test
    void testConfigureControlToLkSwitchMapping() {
        int[] controlOutputIndices = {0, 1, 2};
        coupler.configureControlToLkSwitchMapping(controlOutputIndices);

        // Simulate CONTROL outputs: [0.7, 0.3, 1.0]
        coupler.transferControlToLk(new double[]{0.7, 0.3, 1.0});

        boolean[] gates = coupler.getSwitchGateSignals();
        assertEquals(3, gates.length, "Should have 3 switch gates");
        assertTrue(gates[0], "Output 0.7 should map to true (> 0.5)");
        assertFalse(gates[1], "Output 0.3 should map to false (≤ 0.5)");
        assertTrue(gates[2], "Output 1.0 should map to true (> 0.5)");
    }

    /**
     * Test 7: coupleDomainsForTimeStep() with null control netlist
     * should not throw.
     */
    @Test
    void testCoupleDomainWithNullControlNetlist() {
        CircuitNetlist circuitNetlist = new CircuitNetlist();
        assertDoesNotThrow(() -> {
            coupler.coupleDomainsForTimeStep(circuitNetlist, null, 1e-6, 0.0);
        }, "coupleDomainsForTimeStep should handle null control netlist");
    }

    /**
     * Test 8: coupleDomainsForTimeStep() with empty control netlist
     * should not throw and should skip CONTROL execution.
     */
    @Test
    void testCoupleDomainWithEmptyControlNetlist() {
        CircuitNetlist circuitNetlist = new CircuitNetlist();
        ControlNetlist controlNetlist = ControlNetlist.createEmpty();

        assertFalse(controlNetlist.hasCalculators(),
            "Empty control netlist should have no calculators");

        assertDoesNotThrow(() -> {
            coupler.coupleDomainsForTimeStep(circuitNetlist, controlNetlist, 1e-6, 0.0);
        }, "coupleDomainsForTimeStep should handle empty control netlist");
    }

    /**
     * Test 9: configureThermToLkDeviceMapping() + getDeviceTemperature() resolve
     * the junction temperature through the configured thermal node index, and
     * unmapped devices fall back to the supplied default.
     */
    @Test
    void testConfigureThermToLkDeviceMapping() {
        coupler.configureThermArray(2);
        coupler.setThermNodeTemperature(0, 25.0);
        coupler.setThermNodeTemperature(1, 125.0);
        coupler.configureThermToLkDeviceMapping(new int[] {1, -1});

        assertEquals(125.0, coupler.getDeviceTemperature(0, 25.0), 1e-9,
            "Device 0 mapped to node 1 should read node 1's temperature");
        assertEquals(25.0, coupler.getDeviceTemperature(1, 25.0), 1e-9,
            "Device 1 unmapped (-1) should fall back to the default temperature");
    }

    /**
     * Test 10: getDeviceTemperature() returns the fallback for a negative device
     * index, a device index beyond the mapping, a mapping entry pointing beyond
     * the thermal node array, and a coupler without any configured mapping.
     */
    @Test
    void testGetDeviceTemperatureFallbacks() {
        coupler.configureThermArray(1);
        coupler.setThermNodeTemperature(0, 90.0);
        coupler.configureThermToLkDeviceMapping(new int[] {0, 5});

        assertEquals(25.0, coupler.getDeviceTemperature(-1, 25.0), 1e-9,
            "Negative device index should fall back");
        assertEquals(25.0, coupler.getDeviceTemperature(2, 25.0), 1e-9,
            "Device index beyond the mapping length should fall back");
        assertEquals(25.0, coupler.getDeviceTemperature(1, 25.0), 1e-9,
            "Mapping entry pointing beyond the thermal node array should fall back");

        DomainCoupler unmappedCoupler = new DomainCoupler();
        assertEquals(25.0, unmappedCoupler.getDeviceTemperature(0, 25.0), 1e-9,
            "Coupler without configured mapping should fall back");
    }

    /**
     * Test 11: the configured mapping is a defensive copy - mutating the
     * caller's array afterwards must not change the resolution.
     */
    @Test
    void testConfigureThermToLkDeviceMappingDefensiveCopy() {
        coupler.configureThermArray(2);
        coupler.setThermNodeTemperature(0, 40.0);
        coupler.setThermNodeTemperature(1, 140.0);

        int[] mapping = {0, 1};
        coupler.configureThermToLkDeviceMapping(mapping);
        mapping[0] = 1;

        assertEquals(40.0, coupler.getDeviceTemperature(0, 25.0), 1e-9,
            "Mapping must be stored as a defensive copy");
    }

    /**
     * Test 12: a null thermal node mapping is rejected.
     */
    @Test
    void testConfigureThermToLkDeviceMappingNullRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> coupler.configureThermToLkDeviceMapping(null),
            "Null mapping must be rejected");
    }

    /**
     * Test 13: setThermNodeTemperature() writes in-range nodes and silently
     * ignores out-of-range indices (same contract as setLkPowerLoss()).
     */
    @Test
    void testSetThermNodeTemperatureBoundsCheck() {
        coupler.configureThermArray(1);

        assertDoesNotThrow(() -> coupler.setThermNodeTemperature(-1, 100.0),
            "Out-of-range node index should be ignored");
        assertDoesNotThrow(() -> coupler.setThermNodeTemperature(5, 100.0),
            "Out-of-range node index should be ignored");

        coupler.setThermNodeTemperature(0, 75.0);
        assertEquals(75.0, coupler.getThermTemperatures()[0], 1e-9,
            "In-range node temperature should be stored");
    }
}
