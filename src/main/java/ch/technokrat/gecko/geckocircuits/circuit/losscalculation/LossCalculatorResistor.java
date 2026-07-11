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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;

/**
 * Loss calculator for resistors using P = I * V (Joule heating) loss calculation.
 * Temperature and deltaT parameters are accepted but ignored, as resistive losses
 * are computed directly from the instantaenous current and voltage.
 */
public final class LossCalculatorResistor implements AbstractLossCalculator {

    final AbstractCircuitBlockInterface _resistor;
    private double _totalLosses;

    /**
     * Creates a loss calculator for the given resistor.
     * @param resistor the resistor component whose losses are calculated
     */
    public LossCalculatorResistor(final AbstractCircuitBlockInterface resistor) {
        super();
        _resistor = resistor;
    }

    /**
     * Calculates total losses as I * V. The temperature and deltaT parameters are
     * accepted for interface compatibility but are not used.
     */
    @Override
    public void calcLosses(final double current, final double temperature, final double deltaT) {
        _totalLosses = _resistor._currentInAmps * _resistor._voltage;
    }

    @Override
    public double getTotalLosses() {
        return _totalLosses;
    }
}
