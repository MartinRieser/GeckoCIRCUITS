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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;


/**
 * Abstract calculator for voltage sources using modified nodal analysis (MNA).
 * Introduces an auxiliary current variable ({@code _z}) so that the source
 * current becomes an unknown in the system matrix.
 */
public abstract class AbstractVoltageSourceCalculator extends CircuitComponent<AbstractTwoPortPowerCircuitBlock>
    implements AStampable, DirectCurrentCalculatable, HistoryUpdatable {

    protected int _z = -1;


    public AbstractVoltageSourceCalculator(final AbstractTwoPortPowerCircuitBlock parent) {
        super(parent);
    }
    
    
    /**
     * Stamps the MNA matrix entries for this voltage source using the auxiliary
     * variable index {@code _z}. The conductance entries link the two terminal
     * nodes to the auxiliary current variable row/column.
     *
     * @param matrix the system admittance matrix A
     * @param deltaT the current time step (unused for ideal sources)
     */
    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        assert _z > 0;
        matrix[matrixIndices[0]][_z] += (+1.0);
        matrix[matrixIndices[1]][_z] += (-1.0);
        matrix[_z][matrixIndices[0]] += (+1.0);
        matrix[_z][matrixIndices[1]] += (-1.0);

    }

    @Override
    public final int getZValue() {
        return _z;
    }

    @Override
    public final void setZValue(final int value) {
        _z = value;
    }

    @Override
    public final void updateHistory(final double[] potentials) {
        // System.out.println("function: " + _function + " " + _z);
        _current = potentials[_z];  // Voltage source currents are exceptionally also stored in the node potential vector as unknowns
        _potential1 = potentials[matrixIndices[0]];
        _potential2 = potentials[matrixIndices[1]];
    }            
}
