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
package ch.technokrat.gecko.geckocircuits.control.javablock;

/**
 * In fact, this class represents two red triangles: increase terminal number and decrease!
 * In the future, we should split this / refactor
 * @author andreas
 */
public class ControlJavaTriangles {
       public int _xClickMinTerminal, _xClickMaxTerminal, _yKlickMinTerminalADD, _yKlickMaxTerminalADD,
            _yKlickMinTerminalSUB, _yKlickMaxTerminalSUB;  // // Click areas for red triangles --> change the number of terminals

    /**
     * Determines whether the given screen coordinate lies within the "increase terminal number" triangle.
     *
     * @param mouseX the horizontal click coordinate in pixels
     * @param mouseY the vertical click coordinate in pixels
     * @return {@code true} if the click is inside the increase triangle, {@code false} otherwise
     */
    public boolean isIncreaseClicked(final int mouseX, final int mouseY) {
        return _xClickMinTerminal <= mouseX && mouseX <= _xClickMaxTerminal
                && _yKlickMinTerminalADD <= mouseY && mouseY <= _yKlickMaxTerminalADD;
    }

    /**
     * Determines whether the given screen coordinate lies within the "decrease terminal number" triangle.
     *
     * @param mouseX the horizontal click coordinate in pixels
     * @param mouseY the vertical click coordinate in pixels
     * @return {@code true} if the click is inside the decrease triangle, {@code false} otherwise
     */
    public boolean isDecreaseClicked(int mouseX, int mouseY) {
        return _xClickMinTerminal <= mouseX && mouseX <= _xClickMaxTerminal
                && _yKlickMinTerminalSUB <= mouseY && mouseY <= _yKlickMaxTerminalSUB;
    }
}
