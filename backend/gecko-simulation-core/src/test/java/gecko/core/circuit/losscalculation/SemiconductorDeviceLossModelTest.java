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
package gecko.core.circuit.losscalculation;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemiconductorDeviceLossModelTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("Piecewise-linear model calculates P = V_th * I + R_on * I^2 during conduction")
    void piecewiseLinearConductionLoss() {
        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(0, "D1", CircuitTypCore.LK_D);
        model.configurePiecewiseLinearConduction(0.7, 0.05, 0.0);
        model.configureScaledEnergySwitching(0.0, 0.0, 10.0, 600.0, 0.0);

        // Step 1: Conducting at 10 A
        model.calculateStep(10.0, 1.2, true, 25.0, 1e-4, 1e-4);

        // P_cond = 0.7 * 10 + 0.05 * 10^2 = 7.0 + 5.0 = 12.0 W
        assertEquals(12.0, model.getConductionLoss(), EPSILON);
        assertEquals(0.0, model.getSwitchingLoss(), EPSILON);
        assertEquals(12.0, model.getTotalLoss(), EPSILON);
        assertEquals(12.0 * 1e-4, model.getCumulativeEnergy(), EPSILON);
    }

    @Test
    @DisplayName("Non-conducting switch dissipates zero conduction loss")
    void nonConductingZeroConductionLoss() {
        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(1, "IGBT1", CircuitTypCore.LK_IGBT);
        model.configurePiecewiseLinearConduction(1.5, 0.02, 0.0);

        // Step: Blocking at 400 V with near-zero leakage current
        model.calculateStep(0.0, 400.0, false, 25.0, 1e-4, 1e-4);

        assertEquals(0.0, model.getConductionLoss(), EPSILON);
    }

    @Test
    @DisplayName("Instantaneous product mode calculates p(t) = v(t) * i(t)")
    void instantaneousProductConduction() {
        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(2, "M1", CircuitTypCore.LK_MOSFET);
        model.configureInstantaneousProductConduction();

        model.calculateStep(20.0, 2.5, true, 25.0, 1e-5, 1e-5);
        assertEquals(50.0, model.getConductionLoss(), EPSILON);
    }

    @Test
    @DisplayName("Turn-on and turn-off switching transitions compute scaled energies and power")
    void switchingTransitionsEnergyAndPower() {
        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(3, "T1", CircuitTypCore.LK_IGBT);
        model.configurePiecewiseLinearConduction(0.0, 0.0, 0.0);
        // E_on = 2 mJ, E_off = 1 mJ @ 10 A, 500 V
        model.configureScaledEnergySwitching(2e-3, 1e-3, 10.0, 500.0, 0.0);

        double dt = 1e-5; // 10 microseconds

        // Initial state: OFF at 500 V
        model.calculateStep(0.0, 500.0, false, 25.0, dt, 1e-5);
        assertEquals(0.0, model.getSwitchingLoss(), EPSILON);

        // Transition 1: Turn-ON to 10 A with 500 V previous blocking voltage
        // E_on = 2 mJ * (10/10) * (500/500) = 2 mJ
        // P_sw = 2e-3 / 1e-5 = 200 W
        model.calculateStep(10.0, 1.5, true, 25.0, dt, 2e-5);
        assertEquals(200.0, model.getSwitchingLoss(), EPSILON);

        // Steady ON: no transition
        model.calculateStep(10.0, 1.5, true, 25.0, dt, 3e-5);
        assertEquals(0.0, model.getSwitchingLoss(), EPSILON);

        // Transition 2: Turn-OFF from 10 A to 500 V
        // E_off = 1 mJ * (10/10) * (500/500) = 1 mJ
        // P_sw = 1e-3 / 1e-5 = 100 W
        model.calculateStep(0.0, 500.0, false, 25.0, dt, 4e-5);
        assertEquals(100.0, model.getSwitchingLoss(), EPSILON);
    }

    @Test
    @DisplayName("Thermal averaging window averages instantaneous power over configured duration")
    void thermalAveragingWindow() {
        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(4, "D2", CircuitTypCore.LK_D);
        model.configureInstantaneousProductConduction();
        model.setAveragingWindowDuration(1.0); // 1-second window

        double dt = 0.5;
        // Step 1 at t=0.5: 100 W for 0.5s -> window energy 50 J -> avg = 50 / 1.0 = 50 W
        model.calculateStep(10.0, 10.0, true, 25.0, dt, 0.5);
        assertEquals(50.0, model.getTotalLoss(), EPSILON);

        // Step 2 at t=1.0: 100 W for 0.5s -> window energy 100 J -> avg = 100 / 1.0 = 100 W
        model.calculateStep(10.0, 10.0, true, 25.0, dt, 1.0);
        assertEquals(100.0, model.getTotalLoss(), EPSILON);

        // Step 3 at t=1.5: 0 W for 0.5s -> sample at t=0.5 discarded -> window has step 2 (50J) -> avg = 50 W
        model.calculateStep(0.0, 0.0, false, 25.0, dt, 1.5);
        assertEquals(50.0, model.getTotalLoss(), EPSILON);
    }

    @Test
    @DisplayName("Lookup table conduction evaluates V_on from measurement curves")
    void lookupTableConduction() {
        ConductionLossMeasurementCurve curve25 = new ConductionLossMeasurementCurve(25.0);
        curve25.data = new double[][]{
            {0.0, 5.0, 10.0, 20.0}, // Current [A]
            {0.7, 1.0, 1.3, 1.8}    // Voltage [V]
        };
        DetailedLossLookupTable table = DetailedLossLookupTable.fabric(List.of(curve25), 1);

        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(5, "D_table", CircuitTypCore.LK_D);
        model.configureLookupTableConduction(table);

        model.calculateStep(10.0, 1.3, true, 25.0, 1e-4, 1e-4);
        // V_on(10A, 25C) = 1.3V -> P = 1.3 * 10 = 13.0 W
        assertEquals(13.0, model.getConductionLoss(), 0.05);
    }

    @Test
    @DisplayName("Input validations reject invalid parameters")
    void inputValidation() {
        assertThrows(IllegalArgumentException.class,
            () -> new SemiconductorDeviceLossModel(-1, "D", CircuitTypCore.LK_D));
        assertThrows(IllegalArgumentException.class,
            () -> new SemiconductorDeviceLossModel(0, "", CircuitTypCore.LK_D));
        assertThrows(IllegalArgumentException.class,
            () -> new SemiconductorDeviceLossModel(0, "D", null));

        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(0, "D", CircuitTypCore.LK_D);
        assertThrows(IllegalArgumentException.class,
            () -> model.calculateStep(1.0, 1.0, true, 25.0, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class,
            () -> model.calculateStep(1.0, 1.0, true, 25.0, -1e-4, 1.0));
    }
}
