/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package gecko.core.control.calculators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PmsmControlCalculatorTest {

    private static final double EPSILON = 1e-9;
    private PmsmControlCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PmsmControlCalculator();
        for (int i = 0; i < 12; i++) {
            calculator._inputSignal[i] = new double[1];
        }
    }

    @Test
    @DisplayName("Calculator initializes with 12 inputs and 8 outputs")
    void initializationDimensions() {
        assertEquals(12, calculator._inputSignal.length);
        assertEquals(8, calculator._outputSignal.length);
    }

    @Test
    @DisplayName("Zero current and zero speed references produce zero output signals")
    void zeroInputsProduceZeroOutputs() {
        calculator.calculateYOUT(1e-4);
        for (int i = 0; i < 8; i++) {
            assertEquals(0.0, calculator._outputSignal[i][0], EPSILON);
        }
    }

    @Test
    @DisplayName("Aligned rotor angle (phi=0) isolates direct axis current")
    void alignedRotorCurrents() {
        // Balanced currents: ia = 10, ib = -5 (ic = -5) -> ialpha = 10, ibeta = 0
        calculator._inputSignal[0][0] = 10.0; // ia
        calculator._inputSignal[1][0] = -5.0; // ib
        calculator._inputSignal[2][0] = 0.0;  // w
        calculator._inputSignal[3][0] = 0.0;  // phi = 0
        calculator._inputSignal[4][0] = 0.0;  // n_ref
        calculator._inputSignal[5][0] = 1.0;  // Kp_n
        calculator._inputSignal[6][0] = 1.0;  // T_n
        calculator._inputSignal[7][0] = 50.0; // n_limit
        calculator._inputSignal[8][0] = 1.0;  // Kp_i
        calculator._inputSignal[9][0] = 1.0;  // T_i
        calculator._inputSignal[10][0] = 100.0; // i_limit
        calculator._inputSignal[11][0] = 1.0;   // fP = 1.0 (trigger)

        calculator.calculateYOUT(1e-4);

        // Signal 6: iq, Signal 7: id
        assertEquals(0.0, calculator._outputSignal[6][0], EPSILON, "iq should be 0 when phi=0 and ibeta=0");
        assertEquals(10.0, calculator._outputSignal[7][0], EPSILON, "id should equal ia when phi=0");
    }

    @Test
    @DisplayName("Sampling trigger fP holds previous voltage outputs when fP <= 0.999")
    void pulsePeriodTriggerHolding() {
        calculator._inputSignal[0][0] = 5.0;
        calculator._inputSignal[1][0] = -2.5;
        calculator._inputSignal[2][0] = 100.0;
        calculator._inputSignal[3][0] = 0.5;
        calculator._inputSignal[4][0] = 1500.0;
        calculator._inputSignal[5][0] = 2.0;
        calculator._inputSignal[6][0] = 0.05;
        calculator._inputSignal[7][0] = 40.0;
        calculator._inputSignal[8][0] = 5.0;
        calculator._inputSignal[9][0] = 0.01;
        calculator._inputSignal[10][0] = 200.0;
        calculator._inputSignal[11][0] = 1.0; // Latch triggered

        calculator.calculateYOUT(1e-4);
        double valphaLatched = calculator._outputSignal[0][0];
        double vbetaLatched = calculator._outputSignal[1][0];
        assertTrue(valphaLatched != 0.0 || vbetaLatched != 0.0);

        // Next step without trigger (fP = 0.0) with changed rotor angle
        calculator._inputSignal[3][0] = 1.2;
        calculator._inputSignal[11][0] = 0.0; // No trigger

        calculator.calculateYOUT(1e-4);
        assertEquals(valphaLatched, calculator._outputSignal[0][0], EPSILON, "valpha must be held");
        assertEquals(vbetaLatched, calculator._outputSignal[1][0], EPSILON, "vbeta must be held");
    }

    @Test
    @DisplayName("Output voltages clamp to i_limit upon large speed error")
    void saturationClamping() {
        calculator._inputSignal[4][0] = 100_000.0; // huge speed reference
        calculator._inputSignal[5][0] = 100.0;
        calculator._inputSignal[6][0] = 1.0;
        calculator._inputSignal[7][0] = 50.0; // n_limit
        calculator._inputSignal[8][0] = 100.0;
        calculator._inputSignal[9][0] = 1.0;
        calculator._inputSignal[10][0] = 150.0; // i_limit
        calculator._inputSignal[11][0] = 1.0;

        calculator.calculateYOUT(1e-4);

        // iq_ref clamped to n_limit
        assertEquals(50.0, calculator._outputSignal[4][0], EPSILON);
        // vq_ref clamped to i_limit
        assertEquals(150.0, calculator._outputSignal[2][0], EPSILON);
    }
}
