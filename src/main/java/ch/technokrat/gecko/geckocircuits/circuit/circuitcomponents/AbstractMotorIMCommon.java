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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.general.UserParameter;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

/**
 * Shared parameter definitions for induction machine (IM) models.
 * Defines stator leakage inductance, rotor resistance and leakage
 * inductance, and initial stator flux values common to all IM variants.
 *
 * @author andy
 */
public abstract class AbstractMotorIMCommon extends AbstractThreePhaseMotor {
        
    final UserParameter<Double> statorLeakageInductance = UserParameter.Builder.
            <Double>start("statorLeakageInductance", 0.0010).
            longName(I18nKeys.STATOR_LEAKAGE_INDUCTANCE).
            shortName("Ls_leak").
            unit("H").
            arrayIndex(this, 18).
            build();
    final UserParameter<Double> rotorResistance = UserParameter.Builder.
            <Double>start("rotorResistance", 1.0).
            longName(I18nKeys.ROTOR_RESISTANCE).
            shortName("Rr").
            unit("ohm").
            arrayIndex(this, 19).
            build();
    final UserParameter<Double> rotorLeakageInductance = UserParameter.Builder.
            <Double>start("rotorLeakageInductance", 0.0018).
            longName(I18nKeys.ROTOR_LEAKAGE_INDUCTANCE).
            shortName("Lr_leak").
            unit("H").
            arrayIndex(this, 20).
            build();  
    
    public final UserParameter<Double> initialStatorFluxD = UserParameter.Builder.
            <Double>start("initialStatorFluxD", 0.0).
            longName(I18nKeys.INITIAL_STATOR_FLUX_D).
            shortName("fluxS_d").
            unit("A").
            arrayIndex(this, getInitialStatorFluxIndexD()).
            build();
    public final UserParameter<Double> initialStatorFluxQ = UserParameter.Builder.
            <Double>start("initialStatorFluxQ", 0.0).
            longName(I18nKeys.INITIAL_STATOR_FLUX_Q).
            shortName("fluxS_q").
            unit("A").
            arrayIndex(this, getInitialStatorFluxIndexQ()).
            build();           
    
    @SuppressWarnings("this-escape")
    protected AbstractMotorIMCommon() {
    }

    @Override
    final int getInertiaIndex() {
        return 14;
    }

    @Override
    final int getFrictionCoefficientIndex() {
        return 13;
    }        
    
    int getPolePairIndex() {
        return 16;
    }
    
    int getStatorResistanceIndex() {
        return 17;
    }
    
    /** @return the parameter array index for the initial stator current (phase A) */
    abstract int getInitialStatorCurrentIndexA();
    /** @return the parameter array index for the initial stator current (phase B) */
    abstract int getInitialStatorCurrentIndexB();

    /** @return the parameter array index for the initial stator flux (d-axis) */
    abstract int getInitialStatorFluxIndexD();
    /** @return the parameter array index for the initial stator flux (q-axis) */
    abstract int getInitialStatorFluxIndexQ();                
}
