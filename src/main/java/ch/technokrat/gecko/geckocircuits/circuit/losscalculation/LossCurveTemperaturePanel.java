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

import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

/**
 * Panel with radio buttons for selecting a loss curve by temperature.
 * Each curve is represented by a {@link JRadioButton};
 * only one curve can be selected at a time via a {@link ButtonGroup}.
 */
public final class LossCurveTemperaturePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final transient List<JRadioButton> _radioButtons = new ArrayList<JRadioButton>();    
    private final ButtonGroup _buttonGroup = new ButtonGroup();
    private final transient List<ActionListener> _listeners = new ArrayList<ActionListener>();
    
    /**
     * Constructs a panel with radio buttons for each curve in the list.
     * @param curveList the loss curves to display as selectable options
     */
    public LossCurveTemperaturePanel(final List<? extends LossCurve> curveList) {                        
        super();
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Curves", TitledBorder.LEFT, TitledBorder.TOP));
                        
        setGuiButtonsFromList(curveList);                
        setSelectedButton(0);   
        
    }

    /**
     * Returns the index of the currently selected radio button.
     * @return the selected index, or -1 if none is selected
     */
    int getSelectedIndex() {
        for (JRadioButton button : _radioButtons) {
            if (button.isSelected()) {
                return _radioButtons.indexOf(button);
            }
        }
        assert false;
        return -1;
    }
    
    /**
     * Registers an action listener that will be notified when a curve selection changes.
     * @param listener the listener to add
     */
    public void addActionListener(final ActionListener listener) {
        _listeners.add(listener);
    }

    /**
     * Notifies all registered listeners of a selection change.
     * @param event the action event to forward
     */
    private void sendActionEvent(final ActionEvent event) {
        for(ActionListener listener : _listeners) {
            listener.actionPerformed(event);
        }
    }    

    /**
     * Programmatically selects the radio button at the given index.
     * @param toSelect the index of the button to select
     */
    void setSelectedButton(final int toSelect) {
        _radioButtons.get(toSelect).setSelected(true);
    }           

    /**
     * Rebuilds the radio button list from the given curve list, clearing any existing buttons.
     * @param curveList the loss curves to create buttons for
     */
    public void setGuiButtonsFromList(final List<? extends LossCurve> curveList) {
        setLayout(new GridLayout(curveList.size(), 1));                
                
        _radioButtons.clear();        
        this.removeAll();
        for (int i1 = 0; i1 < curveList.size(); i1++) {            
            final JRadioButton newButton = new JRadioButton(curveList.get(i1).getName());            
            _radioButtons.add(newButton);
            newButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent actionEvent) {
                    if(newButton.isSelected()) {
                        sendActionEvent(actionEvent);
                    }
                    
                }
                
            });
                        
            _buttonGroup.add(newButton);
            this.add(newButton);                                                
        }           
        this.updateUI();
    }
    
}
