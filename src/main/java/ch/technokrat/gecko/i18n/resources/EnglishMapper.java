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
/*
 * Class used to initialize English key-value pairs.  
 * -No connection to the Wiki Database is needed.
 */
package ch.technokrat.gecko.i18n.resources;

import ch.technokrat.gecko.i18n.DoubleMap;

public class EnglishMapper { 

    /**
     * Creates a new map with all single-line English key-value pairs and
     * returns it
     *
     * @return DoubleMap containing all single-line English key-value pairs
     */
    public static DoubleMap initEnglishMap_single() {
        DoubleMap dm = new DoubleMap();

        for(I18nKeys value : I18nKeys.values()) {
            dm.insertPair(value, value._englishTranslation);
        }

        return dm;
    }

    /**
     * Creates a new map with all multiple-line English key-value pairs and
     * returns it
     *
     * @return DoubleMap containing all multiple-line English key-value pairs
     */
    public static DoubleMap initEnglishMap_multiple() {
        DoubleMap dm = new DoubleMap();

        return dm;
    }
}