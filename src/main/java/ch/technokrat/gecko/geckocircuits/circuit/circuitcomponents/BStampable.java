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
 * Interface for components that can stamp entries into the right-hand side
 * vector (B vector) of the MNA system during circuit simulation.
 */
public interface BStampable {

    /**
     * Stamps this component's contribution into the B vector.
     *
     * @param bVector the right-hand side vector
     * @param time the current simulation time
     * @param deltaT the current time step
     */
    void stampVectorB(double[] bVector, double time, final double deltaT);

    /**
     * Registers the B vector with this component so it can request updates.
     *
     * @param bVector the B vector to register
     */
    void registerBVector(BVector bVector);

    /**
     * Indicates whether this component's B-vector stamp remains constant
     * across time steps, enabling caching optimizations.
     *
     * @return true if the stamp does not change between time steps
     */
    boolean isBasisStampable();
}
