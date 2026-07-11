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
package ch.technokrat.modelviewcontrol;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;

public class DelegateNumericTextField <M extends ModelMVC<Double>> extends JTextField
        implements IGenericMVCView<M>, ActionListener {
        private static final long serialVersionUID = 759956473825447L;
        private transient ActionListener _listener;
        private transient ModelMVC<Double> _model;


        @SuppressWarnings("this-escape")
        public DelegateNumericTextField() {
            this.setText("0.0");
        }

    /**
     * Registers the text field with a Double ModelMVC, setting up synchronization
     * and a listener for text input action events.
     *
     * @param textModel the model representing a double value
     * @param undoRedoText the description text for undo/redo actions
     */
    @Override
    public void registerModel(M textModel, String undoRedoText) {
        assert textModel != null;
        setText(textModel.getValue().toString());

        textModel.addModelListener(this);
        _model = textModel;

        _listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    _model.setValue(Double.parseDouble(getText()));
                } catch (NumberFormatException ex) {
                    setText(_model.getValue().toString());
                }
            }
        };

        addActionListener(_listener);


    }

    /**
     * Unregisters the model listener and cleans up the action listener reference.
     */
    @Override
    public void unregisterModel() {
        removeActionListener(_listener);
        if(_model != null) {
            _model.removeModelListener(this);
            _model = null;
        }
    }

    /**
     * Synchronizes the text field display with the underlying model's double value.
     *
     * @param evt the action event triggering the update
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        setText(_model.getValue().toString());
    }
    
    /**
     * Parses the current text field contents and saves the parsed double value into the model.
     * If the input is not a valid double, resets the text to the model's current value.
     */
    public void saveValue() {
        try {
            _model.setValue(Double.parseDouble(getText()));
        } catch (NumberFormatException ex) {
            setText(_model.getValue().toString());
        }
    }




}