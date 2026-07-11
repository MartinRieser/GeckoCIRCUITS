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
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * Defines display priority levels for circuit labels. Higher numeric values indicate
 * higher priority. Note that value 3 is intentionally unused.
 * @author andreas
 */
public enum LabelPriority {
    EMPTY_STRING(0),
    LOW(1),
    NORMAL(2),
    FORCE_NAME(4);
    
    private int _numericValue;
    
    LabelPriority(int value) {
        _numericValue = value;
    }
    
    public static LabelPriority getHighestPriority(LabelPriority prio1, LabelPriority prio2) {
        if(prio1._numericValue >= prio2._numericValue) {
            return prio1;
        } else {
            return prio2;
        }
    }

    /**
     * Checks if this priority is greater than the given priority.
     * @param otherPrio the priority to compare against
     * @return true if this priority has a higher numeric value
     */
    public boolean isBiggerThan(LabelPriority otherPrio) {
        return _numericValue > otherPrio._numericValue;
    }
    
}
