package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.general.UserParameter;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * For some components, it makes sense to have a variable input number, e.g. adding, logic-or, multiplication...
 * @author andreas
 */
public abstract class AbstractControlVariableInputs extends ControlBlock implements VariableTerminalNumber {
    private static final long serialVersionUID = 1L;

    private final static int DEFAULT_NUMBER_INPUTS = 1;
    
    /** UserParameter controlling the number of input terminals; stored at array index -1 (not persisted in parameter array). */
    public final transient UserParameter<Integer> _inputTerminalNumber = UserParameter.Builder.
            <Integer>start("numberInputTerminals", DEFAULT_NUMBER_INPUTS).
            addAlternativeSaveIdentifier("anzXIN").
            longName(I18nKeys.NO_INPUT_TERMINALS).
            shortName("numberInputTerminals").
            arrayIndex(this, -1).
            build();
    
    @SuppressWarnings("this-escape")
    public AbstractControlVariableInputs(final int defaultInputs) {
        super(defaultInputs, 1); 
        _inputTerminalNumber.setValueWithoutUndo(defaultInputs);
        _inputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInputTerminalNumber(_inputTerminalNumber.getValue());
            }
        });
    }
    
    /**
     * Sets the number of input terminals and updates the corresponding user parameter.
     *
     * @param number the new number of input terminals
     */
    @Override
    public final void setInputTerminalNumber(final int number) {        
        super.setInputTerminalNumber(number);
        if(_inputTerminalNumber != null) {
            _inputTerminalNumber.setValueWithoutUndo(number);
        }        
    }

    /**
     * Sets the number of output terminals (fixed at 1 output per terminal group).
     *
     * @param number the new number of output terminals
     */
    @Override
    public final void setOutputTerminalNumber(final int number) {
        setOutputTerminalNumber(number, 1);
    }
    
    @Override
    protected Window openDialogWindow() {        
        return new DialogControlVariableInputs(this);        
    }
        
        
}
