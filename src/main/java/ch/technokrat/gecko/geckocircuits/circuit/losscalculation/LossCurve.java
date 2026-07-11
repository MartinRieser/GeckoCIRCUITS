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

import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import ch.technokrat.gecko.geckocircuits.general.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

/**
 * Abstract base class for loss curves using the template method pattern.
 * Concrete subclasses implement {@link #exportIndividual(StringBuffer)} and
 * {@link #importIndividual(TokenMap)} to handle curve-specific data.
 */
public abstract class LossCurve {
    
    /**
     * Curve data table. Expected layout: {@code data[0]} = x-values, {@code data[1]} = y-values.
     */
    public double[][] data;
    
    /**
     * Junction temperature parameter, default 0.0 &deg;C.
     */
    final UserParameter<Double> tj = UserParameter.Builder.
            <Double>start("tj", 0.0).            
            longName(I18nKeys.TEMP_AT_WHICH).
            shortName("curveTemperature").
            unit("C").            
            build();            

    /**
     * Imports curve data and individual subclass data from a token map.
     * @param tokenMap the token map containing serialised curve data
     */
    final void importASCII(final TokenMap tokenMap) {        
        data = tokenMap.readDataLine("data[][]", data);
                
        importIndividual(tokenMap);
        tj.readFromTokenMap(tokenMap);        
    }

    /**
     * Exports curve data and individual subclass data to the given buffer.
     * @param ascii the buffer to append the serialised curve to
     */
    final void exportASCII(final StringBuffer ascii) {
        
        ascii.append("\n<" + getXMLTag() + ">");
        ProjectData.appendAsString(ascii.append("\ndata"), data);
        tj.writeXMLToFile(ascii);
        exportIndividual(ascii);
        ascii.append("\n<\\" + getXMLTag() + ">");                        
    }
    
    public String getName() {
        return ((int) (double) tj.getValue()) + "°C";
    }

    /**
     * Returns the XML tag used for serialising this curve type.
     * @return the XML tag string
     */
    abstract String getXMLTag();

    /**
     * Hook method for subclasses to export additional data. Default implementation does nothing.
     * @param ascii the buffer to append subclass-specific data to
     */
    protected void exportIndividual(final StringBuffer ascii) {        
        // nothing todo - template method pattern
    }

    /**
     * Hook method for subclasses to import additional data. Default implementation does nothing.
     * @param tokenMap the token map containing subclass-specific data
     */
    protected void importIndividual(final TokenMap tokenMap) {
        // nothing todo - template method pattern
    }
    
    
    public void setCurveData(double[][] newData) {
        this.data = new double[newData.length][];
        for(int i = 0; i < newData.length; i++) {
            data[i] = new double[newData[i].length];
            System.arraycopy(newData[i], 0, data[i], 0, newData[i].length);
        }
    }
    
    public double[][] getCurveData() {
        double[][] returnValue = new double[data.length][];
        for(int i = 0; i < data.length; i++) {
            returnValue[i] = new double[data[i].length];
            System.arraycopy(data[i], 0, returnValue[i], 0, data[i].length);
        }
        return returnValue;
    }
    
}
