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

import java.io.Serializable;

/**
 * Data container for a conduction loss measurement curve, storing a U/I characteristic
 * (voltage vs. current) measured at a specific junction temperature.
 */
public class ConductionLossMeasurementCurve extends LossCurve implements Serializable {
    private static final long serialVersionUID = 1L;
   
    // // Data container with the following format for data[][] -->
    // U [V] - I [A]
    // ..      ..
    // ..      ..
    // usw.
    // // Parameter: T_junction --> specified during the measurement
    //
    /**
     * @param tj the junction temperature at which the measurement was taken
     */
    public ConductionLossMeasurementCurve(double tj) {
        this.tj.setValueWithoutUndo(tj);
    }

    /**
     * Creates a deep copy of this curve, duplicating the data matrix and junction
     * temperature.
     *
     * @return a new independent copy of this curve
     */
    public ConductionLossMeasurementCurve copy() {
        ConductionLossMeasurementCurve copy = new ConductionLossMeasurementCurve(-1);
        copy.data = new double[this.data.length][this.data[0].length];
        for (int i1 = 0; i1 < this.data.length; i1++) {
            for (int i2 = 0; i2 < this.data[0].length; i2++) {
                copy.data[i1][i2] = this.data[i1][i2];
            }
        }
        copy.tj.setValueWithoutUndo(this.tj.getValue());
        return copy;
    }            

    @Override
    String getXMLTag() {
        return "ConductionLossMeasurementCurve";
    }                
}
