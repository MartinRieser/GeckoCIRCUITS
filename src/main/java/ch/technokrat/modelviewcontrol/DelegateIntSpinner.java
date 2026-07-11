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
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;

/**
 * A JSpinner that acts as an MVC view for an Integer-valued {@link ModelMVC}.
 * Spinner changes propagate to the model and vice-versa.
 *
 * @param <M> the Integer model type
 * @author andy
 */
public class DelegateIntSpinner<M extends ModelMVC<Integer>> extends JSpinner
        implements IGenericMVCView<M>, ActionListener {

    private static final long serialVersionUID = 759473276284147L;
    /**
     * The change listener that propagates spinner edits to the model.
     */
    private transient ChangeListener _changeListener;
    /**
     * The Integer MVC model this spinner view is bound to.
     */
    private transient ModelMVC<Integer> _model;

    /**
     * Binds this spinner view to the given Integer model.
     *
     * @param integerModel  the Integer model to bind
     * @param undoRedoText label text for undo/redo actions
     */
    @Override
    public void registerModel(M integerModel, String undoRedoText) {
        assert integerModel != null;
        this.setValue(integerModel.getValue());
        _model = integerModel;
        _model.addModelListener(this);


        _changeListener = new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                if(_model != null) {
                    _model.setValue(getIntegerValue());
                }
            }
        };
        addChangeListener(_changeListener);
    }

    /**
     * Detaches this view from the model, removing all registered listeners.
     */
    @Override
    public void unregisterModel() {
        if(_model!= null) {
            _model.removeModelListener(this);
            _model = null;
        }
    }

    /**
     * Retrieves the current spinner value as an Integer.
     *
     * @return the current value cast to Integer
     */
    public Integer getIntegerValue() {
        return (Integer) (super.getValue());
    }

    /**
     * Synchronizes the spinner value with the underlying model's value.
     *
     * @param evt the action event triggering the update
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        this.setValue(_model.getValue());
    }
}