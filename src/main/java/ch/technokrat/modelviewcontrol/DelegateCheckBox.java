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
import javax.swing.JCheckBox;

/**
 * A JCheckBox that acts as an MVC view for a Boolean-valued {@link ModelMVC}.
 * When the checkbox is toggled, the model value is updated; when the model
 * changes, the checkbox selection is synchronised.
 *
 * @param <M> the Boolean model type
 * @author andy
 */
public class DelegateCheckBox<M extends ModelMVC<Boolean>> extends JCheckBox
    implements IGenericMVCView<M>, ActionListener {
        private static final long serialVersionUID = 159473276254167L;
    /**
     * The action listener that propagates checkbox toggles to the model.
     */
    private transient ActionListener _listener;
    /**
     * The Boolean MVC model this checkbox view is bound to.
     */
    private transient ModelMVC<Boolean> _model;

     /**
     * Binds this checkbox view to the given Boolean model.
     *
     * @param model        the Boolean model to bind
     * @param undoRedoText label text for undo/redo actions
     */
    @Override
    public void registerModel(M model, String undoRedoText) {
        this.setSelected(model.getValue());
        _model = model;
        _model.addModelListener(this);

        _listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                
                _model.setValue(isSelected());
            }
        };

        addActionListener(_listener);

    }

    /**
     * Detaches this view from the model, removing all registered listeners.
     */
    @Override
    public void unregisterModel() {
        if(_model != null) {
            _model.removeModelListener(this);
            _model = null;
        }
    }

    /**
     * Synchronizes the checkbox selection state with the underlying model's value
     * when an action event is received.
     *
     * @param e the action event triggering the update
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        this.setSelected(_model.getValue());
    }



}
