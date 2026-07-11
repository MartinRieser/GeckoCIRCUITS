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
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * Interface for circuit components that can have their voltage measured
 * directly across a specific connector type.
 * @author andreas
 */
public interface DirectVoltageMeasurable {
    /**
     * Returns the measurement components for the given connector type.
     * @param connectorType the type of connector to measure
     * @return array of measurement block interfaces
     */
    AbstractBlockInterface[] getDirectVoltageMeasurementComponents(final ConnectorType connectorType);    
}
