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

import ch.technokrat.gecko.geckocircuits.general.GlobalColors;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 * Simple informational dialog that displays a read-only message to the user.
 * Used for control blocks that have no configurable parameters but need to
 * show status or usage information.
 */
class DialogSimpleInfoMessage extends DialogWindowWithoutInput {
    private static final long serialVersionUID = 1L;
    private final String _displayMessage;

    public DialogSimpleInfoMessage(ControlBlock aThis, String displayMessage) {
        super(aThis);
        _displayMessage = displayMessage;
    }

    @Override
    protected void buildIndividualGUI() {
        ((TitledBorder) jpM.getBorder()).setTitle("Information");
        JPanel pSIN = new JPanel();
        final JLabel labPar1 = new JLabel(_displayMessage);        
        labPar1.setForeground(GlobalColors.LAB_COLOR_DIALOG_1);
        pSIN.add(labPar1);
        jpM.add(pSIN, BorderLayout.CENTER);     
        con.add(jpM, BorderLayout.CENTER);
        super.buildIndividualGUI();
    }
}
