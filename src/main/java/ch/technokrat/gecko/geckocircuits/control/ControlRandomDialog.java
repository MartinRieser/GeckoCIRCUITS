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

package ch.technokrat.gecko.geckocircuits.control;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author andy
 */
/**
 * Dialog for the Random signal source block, providing a simple info label.
 */
class ControlRandomDialog extends DialogElementCONTROL<ControlSignalSource>{
    private static final long serialVersionUID = 1L;

    /**
     * Creates the dialog for the Random signal source block.
     *
     * @param element the ControlSignalSource to configure
     */
    public ControlRandomDialog(ControlSignalSource element) {
        super(element);
    }

    @Override
    void buildIndividualGUI() {        
        jpM = new JPanel();
        JLabel infoLabel = new JLabel("Random walk output");
        jpM.add(infoLabel);
    }
    
    
    
}
