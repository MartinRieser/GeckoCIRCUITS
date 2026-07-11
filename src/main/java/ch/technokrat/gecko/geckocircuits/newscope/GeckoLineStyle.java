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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Enum defining line styles for scope curves, each with a corresponding
 * BasicStroke width and optional dash pattern.
 */
public enum GeckoLineStyle {
    /**
     * WARNING: Don't change the order of the enumeration constants, ordinal() is used!
     */
    /** Solid 1px width line. */
    SOLID_PLAIN(-3333330, new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f)),
    /** Invisible line (0.1px width, effectively hidden). */
    INVISIBLE(-3333331, new BasicStroke(0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f)),
    /** Solid 2px width fat line. */
    SOLID_FAT_1(-3333332, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f)),
    /** Solid 3px width extra fat line. */
    SOLID_FAT_2(-3333333, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f)),
    /** Dotted 1px line with 4px dash, 4px gap pattern. */
    DOTTED_PLAIN(-3333334, new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
    BasicStroke.JOIN_ROUND, 0.1f, new float[]{4, 4}, 0)),
    /** Dotted 3px fat line with 4px dash, 4px gap pattern. */
    DOTTED_FAT(-3333335, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
    BasicStroke.JOIN_ROUND, 0.1f, new float[]{4, 4}, 0)),
    /** Solid 0.5px thin line. */
    SOLID_THIN(-3333336, new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.1f));

    static GeckoLineStyle getFromOrdinal(final int ordinal) {
        for (GeckoLineStyle val : GeckoLineStyle.values()) {
            if (val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }
    private final int _code;
    private final Stroke _stroke;

    GeckoLineStyle(final int code, final Stroke stroke) {
        this._code = code;
        this._stroke = stroke;
    }

    public int code() {
        return _code;
    }

    public Stroke stroke() {
        return _stroke;
    }
    

    public static GeckoLineStyle setzeLinienstilSelektiert(final int ordinal) {
        for (GeckoLineStyle val : GeckoLineStyle.values()) {
            if (val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }

    /**
     * Warning: this method intends so modify the parameter
     * @param g2d 
     */
    public void setStrokeStyle(final Graphics2D g2d) {
        g2d.setStroke(this._stroke);
    }

    public static GeckoLineStyle getFromCode(final int gLSCode) {
        for (GeckoLineStyle val : GeckoLineStyle.values()) {
            if (val._code == gLSCode) {
                return val;
            }
        }
        
        // default:
        return GeckoLineStyle.SOLID_PLAIN;
    }
}
