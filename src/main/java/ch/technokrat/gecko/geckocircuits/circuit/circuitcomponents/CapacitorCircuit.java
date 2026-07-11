package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

/**
 * Concrete capacitor component for the electrical (LK) power circuit domain.
 * Registers its TYPE_INFO for runtime component fabrication and serialization.
 */
public class CapacitorCircuit extends AbstractCapacitor {
    static final AbstractTypeInfo TYPE_INFO = 
            new CircuitTypeInfo(CapacitorCircuit.class, "C", I18nKeys.CAPACITOR_C_F);       
    
}
