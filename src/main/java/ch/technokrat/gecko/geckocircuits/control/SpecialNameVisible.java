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
package ch.technokrat.gecko.geckocircuits.control;

/**
 * For some components, we may define an "extra" visibility of the component name
 * (e.g. Scope, Java-Block). This interface should "replace" the regular showName-
 * behavior.
 * @author andreas
 */
public interface SpecialNameVisible {
    /**
     * Returns whether the component name is visible.
     * @return true if the name is visible
     */
    boolean isNameVisible();
    /**
     * Sets whether the component name should be visible.
     * @param newValue true to make the name visible
     */
    void setNameVisible(final boolean newValue);
}
