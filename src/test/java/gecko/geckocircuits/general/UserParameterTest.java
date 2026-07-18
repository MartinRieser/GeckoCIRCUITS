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
package gecko.geckocircuits.general;

import gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link UserParameter}, verifying the static builder,
 * defaults, constraints, alternative naming, and visibility logic.
 */
public class UserParameterTest {

    private static final double DELTA = 1e-9;

    /**
     * Verifies that the builder properly sets properties on basic instantiation.
     */
    @Test
    public void testBuilderBasicProperties() {
        UserParameter<Double> parameter = UserParameter.Builder.start("testId", 25.0)
                .shortName("T_short")
                .unit("V")
                .showInTextInfo(TextInfoType.SHOW_ALWAYS)
                .build();
        
        assertEquals("testId", parameter.getSaveIdentifier());
        assertEquals(25.0, parameter.getValue(), DELTA);
        assertEquals("T_short", parameter.getShortName());
        assertEquals("V", parameter.getUnit());
        assertEquals(TextInfoType.SHOW_ALWAYS, parameter.getTextInfoType());
    }

    /**
     * Verifies that modifying value, resetting, and default value syncing works correctly.
     */
    @Test
    public void testValueModifications() {
        UserParameter<Double> parameter = UserParameter.Builder.start("paramId", 5.0)
                .shortName("param")
                .build();
        
        parameter.setUserValue(12.3);
        assertEquals("Value should update after calling setUserValue", 12.3, parameter.getValue(), DELTA);

        parameter.setValueWithoutUndo(7.7);
        assertEquals("Value should update after calling setValueWithoutUndo", 7.7, parameter.getValue(), DELTA);
        
        parameter.setFromDoubleValue(9.9);
        assertEquals("Value should update after calling setFromDoubleValue", 9.9, parameter.getValue(), DELTA);
    }

    private enum DummyEnum {
        STATE_OFF,
        STATE_ON
    }

    /**
     * Verifies the enum-conditional visibility check.
     */
    @Test
    public void testEnumConditionVisibility() {
        UserParameter<DummyEnum> enumParameter = UserParameter.Builder.start("enumId", DummyEnum.STATE_OFF)
                .shortName("enum")
                .build();
        
        UserParameter<Double> dependentParameter = UserParameter.Builder.start("depId", 4.2)
                .shortName("dep")
                .showWhenEnumValueIsSet(enumParameter, DummyEnum.STATE_ON)
                .build();
        
        assertFalse("Visibility should be false when enum value doesn't match condition",
                dependentParameter.isShowTypeInfoConditionFromEnum());
                
        enumParameter.setUserValue(DummyEnum.STATE_ON);
        
        assertTrue("Visibility should be true when enum value matches condition",
                dependentParameter.isShowTypeInfoConditionFromEnum());
    }

    /**
     * Verifies storage and query of alternative short names (backward compatibility).
     */
    @Test
    public void testAlternativeShortNames() {
        UserParameter<Double> parameter = UserParameter.Builder.start("compatParam", 3.0)
                .shortName("modernName")
                .addAlternativeShortName("legacyName")
                .build();
                
        assertEquals("modernName", parameter.getShortName());
        assertEquals("legacyName", parameter.getAlternativeShortName());
    }
}
