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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;

/**
 * Per-diagram settings holding the diagram name and y-axis weight factor.
 * The weight determines the relative vertical space allocated to this diagram
 * in the scope display.
 */
public final class DiagramSettings {
    private static final double DEFAULT_SPACING = 0.2;
    
    private String _nameDiagram = "";        
    private double _yWeightDiagram = DEFAULT_SPACING;    
    
    DiagramSettings() {
        // nothing to do, pure data object
    }
    
    public void setNameDiagram(final String newName) {
        assert newName != null;
        _nameDiagram = newName;
    }        
    
    String getNameDiagram() {
        return _nameDiagram;
    }
    
    /**
     * Sets the y-axis weight (relative vertical space) for this diagram.
     * Valid range is [0, 1], where 0 hides the diagram and 1 allocates maximum
     * space. Weights are normalized across all diagrams in the scope.
     *
     * @param weight the weight value in the range [0, 1]
     */
    public void setWeightDiagram(final double weight) {
        assert weight >= 0;
        assert weight <= 1;
        _yWeightDiagram = weight;
    }
    
    public double getWeightDiagram() {
        return _yWeightDiagram;
    }

    void exportIndividualCONTROL(final StringBuffer ascii) {
        ProjectData.appendAsString(ascii.append("\nnameDiagram"), _nameDiagram);
        ProjectData.appendAsString(ascii.append("\nyWeightDiagram"), _yWeightDiagram);        
    }

    void importASCII(final TokenMap diagramSettingsMap) {
        _nameDiagram = diagramSettingsMap.readDataLine("nameDiagram", _nameDiagram);        
        _yWeightDiagram = diagramSettingsMap.readDataLine("yWeightDiagram", _yWeightDiagram);        
    }
    
    
}
