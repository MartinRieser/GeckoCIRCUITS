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
package gecko.core.datacontainer;

/**
 * Listener interface for notifications about data container updates.
 * Provides a type-safe, decoupled replacement for {@code java.util.Observer}.
 *
 * @author GeckoCIRCUITS Core Team
 */
@FunctionalInterface
public interface DataContainerListener {

    /**
     * Invoked when the observed data container has been updated.
     *
     * @param container the data container that triggered the event
     * @param eventData optional event payload or metadata, or {@code null}
     */
    void onDataContainerUpdate(AbstractDataContainer container, Object eventData);
}
