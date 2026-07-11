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
package ch.technokrat.gecko.geckocircuits.general;


/**
 * Contract for matching suggestion text against user input in auto-suggest
 * fields.
 */
public abstract interface SuggestMatcher
{
  /**
   * Returns true if the data word matches the search word according to
   * the implementation's matching strategy.
   * @param dataWord the candidate suggestion
   * @param searchWord the user's search input
   * @return true if the data word matches
   */
  public abstract boolean matches(String paramString1, String paramString2);
}