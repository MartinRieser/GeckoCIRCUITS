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
package ch.technokrat.gecko.geckocircuits.scope;

import ch.technokrat.gecko.geckocircuits.general.GlobalColors;
import ch.technokrat.gecko.geckocircuits.general.TechFormat;
import java.awt.AlphaComposite;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated
@SuppressWarnings({"static-access", "cast", "serial", "deprecation"})
public final class GraferImplementation extends GraferV3 implements MouseListener, MouseMotionListener {
    // // Number of intervals on the x-axis in which Hi and Lo values ​​are determined for data compression
    private static final int INTERVALLE_ENTLANG_X = 2000;
    private static final int INDEX_ENCODING_FACTOR = 1000;
    private static final int CLIP_HEIGHT = 10000;
    private static final float ALPHA_COMPOSITE_VALUE = 0.6f;
    
    private static final long serialVersionUID = 364726123473711L;
    private final Scopable _scope;  // callback 
    // XXX private final ScopeSettings _scopeSettings;
    // // specifies how many of the points in the worksheet should be displayed as a curve, see function below
    private int _zvCounter;
    // // the data to be displayed comes from a ZV simulation or from an external one
    // // (static) file? - default: ZV sim.
    public boolean _usesExternalData = false;
    //---------------------------------
    private static final int TXT_DISTANCE_Y = 10;
    public static final int ANZ_DIAGRAM_MAX = 9;  // // Number of maximum possible diagrams in a SCOPE
    public static int DX_IN_LINKS = 60, DX_IN_RECHTS = 70;  // // left and right x indentation of the axes in pixels
    public static int DY_IN_OBEN = 8, DY_IN_UNTEN = 8;  // // y-indentation of the axes in pixels from above or below and y-distance between 2 diagrams
    public static int ABSTAND_BESCHRIFTUNG_XACHSE = 35;  // // There is this much additional distance to the bottom so that the x-axis labels can be set
    private static final int ANZ_AUTO_TICKS = 5;
    private static final int GRID_HIDE_THRESHOLD_Y = 230;
    private static final int GRID_HIDE_THRESHOLD_X = 100;
    private static final double GRID_HIDE_THRESHOLD_RATIO = 2.5;
    private int x1, x2, y1, y2;  // Rechteck-Koordinaten des Zoom-Fensters
    private boolean angeklicktZoom = false;
    //--------
    // Bereichsgrenzen eines Diagramms bezueglich Maus-Klick:
    private int[] xGrfMIN, xGrfMAX, yGrfMIN, yGrfMAX;
    private int indexAngeklickterGraph;
    private boolean controlZoomOn = false;
    private boolean shiftZoomOn = false;
    //==========================================
    //
    // // Graph properties --> adapted to the special SCOPE structure
    public static final int DIAGRAM_TYP_ZV = 91, DIAGRAM_TYP_SGN = 92;
    //
    private int anzGrfVisible;  // // Number of currently visible graphs in the scope
    private int anzDiagram;  // // Number of charts
    //
    public String[] nameDiagram;  // // Labels of the diagrams
    public double[] ySpacingDiagram;  // // how much 'y-part' does the respective diagram have
    public int[] notwendigeHoehePixGRF;  // // with SIGNAL the graph height is specified in pixels, the scope size may be adjusted (for ZV --> '-1')
    public int[] diagramTyp;  // // is the respective diagram a ZV type or a signal type?
    public boolean[] jcbShowLegende;  // // should the curve names be displayed in the form of a legend on the left edge of the graph?
    public boolean[] showAxisX, showAxisY;  // // Show or hide the axes
    //
    public double[] minX, maxX, minY, maxY;  // Achsen-Begrenzungen
    public double[] minXOld, maxXOld, minYOld, maxYOld;  // Achsen-Begrenzungen
    public double[] minXOldOld, maxXOldOld, minYOldOld, maxYOldOld;  // Achsen-Begrenzungen
    public boolean[] autoScaleX, autoScaleY;  // // should the axis limits be automatically adjusted to the worksheet data?
    public int[] xAchsenTyp, yAchsenTyp;  // // Linear or logarithmic?
    public int[] xAchseFarbe, yAchseFarbe;
    public int[] xAchseStil, yAchseStil;
    // TODO: the following fields hide Grafer fields, check if it would make a 
    // problem just to delete it from here!
    public String[] xAxisLabel, yAxisLabel;
    //
    public int[] gridNormalX_associatedXAxis, gridNormalX_associatedYAxis;
    public int[] gridNormalY_associatedXAxis, gridNormalY_associatedYAxis;
    public int[] colorGridNormalX, colorGridNormalXminor, colorGridNormalY, colorGridNormalYminor;
    public int[] lineStyleGridNormalX, lineStyleGridNormalXminor, lineStyleGridNormalY, lineStyleGridNormalYminor;
    public boolean[] xShowGridMaj, xShowGridMin, yShowGridMaj, yShowGridMin;
    //
    public boolean[] xTickAutoSpacing, yTickAutoSpacing;
    public double[] xTickSpacing, yTickSpacing;
    public int[] xNumTicksMinor, yNumTicksMinor;
    public int[] xTickLength, xTickLengthMinor, yTickLength, yTickLengthMinor;
    //
    public boolean[] showLabelsXmaj, showLabelsXmin, showLabelsYmaj, showLabelsYmin;
    //
    private boolean[] zeichneDiagrammUmrandung;  // // to draw if the grid is switched off because the display is too small (in pixel points).
    //-------------------------
    // // Graph properties --> especially for SIGNAL
    public int[] positionSIGNAL;  // // for SIGNAL --> contains the y-position (order) of the signal curve within the individual graph
    public int[] positionSIGNAL_ALT;
    public int[] sgnHeight, sgnDistance;
    public double[] sgnSchwelle;
    //
    //-------------------------
    // // Graph properties to remember when showing and hiding axes -->
    // // ... is read and reset directly from 'DialogGraphProperties'
    public boolean[] ORIGjcbXShowGridMaj, ORIGjcbXShowGridMin;
    public boolean[] ORIGjcbYShowGridMaj, ORIGjcbYShowGridMin;
    public int[] ORIGjcmXlinCol, ORIGjcmYlinCol;
    public int[] ORIGjcmXlinStyl, ORIGjcmYlinStyl;
    public int[] ORIGjtfXtickLengthMaj, ORIGjtfXtickLengthMin;
    public int[] ORIGjtfYtickLengthMaj, ORIGjtfYtickLengthMin;
    public boolean[] ORIGjcbXShowLabelMaj, ORIGjcbXShowLabelMin;
    public boolean[] ORIGjcbYShowLabelMaj, ORIGjcbYShowLabelMin;
    //=========================
    //
    // Connection / Zuordnung  Kurve - Diagramm -->
    public static final int ZUORDNUNG_X = 51, ZUORDNUNG_Y = 52, ZUORDNUNG_SIGNAL = 54, ZUORDNUNG_NIX = 55, ZUORDNUNG_MEAN = 56;
    //
    // Signal-Namen bzw. Worksheet-Headers:
    public int anzSignalePlusZeit;  // // This is how many different columns the worksheet has
    public String[] signalNamen;
    // Zuordnungen Kurven - Diagramme:
    public int[][] matrixZuordnungKurveDiagram;
    //
    // //public int[] membershipX, membershipY;  // Assignment of the curve to the x and y axes
    public int[][] indexWsXY;  // Zuordnung Worksheetdaten - Kurven
    //
    //=========================
    //
    // Kurven-Properties
    public int kurvenanzahl;  // // This is how many different curves are currently displayed in the SCOPE --> corresponds to a 'curve ID'
    // // to assign the curve indices to the assignment matrix:
    public int[] indexDerKurveInDerMatrix;  // Abspeichern in folgendem Format: --> INDEX_ENCODING_FACTOR*i1 +i2 wobei (i1..Graphenanzahl / i2..Kurvenanzahl)
    public int[] indexDerKurveInDerMatrixALT;  // // Storage necessary to display the SIGNAL order correctly
    //
    // // each entry in the linkage matrix corresponds to a potential curve -->
    public int[][] crvAchsenTyp;  // // is updated via the SET method so that the matrix 'matrixAssignmentCurveDiagram' is not forgotten!
    public int[][] crvLineStyle, crvLineColor;
    public boolean[][] crvSymbShow;
    public int[][] crvSymbFrequ;
    public int[][] crvSymbShape, crvSymbColor;
    public int[][] crvClipXmin, crvClipXmax, crvClipYmin, crvClipYmax;
    public double[][] crvClipValXmin, crvClipValXmax, crvClipValYmin, crvClipValYmax;
    //
    public boolean[][] crvFillDigitalCurves;
    public int[][] crvFillingDigitalColor;
    //==========================================
    // // Mouse actions in the diagram window:
    public static final int MAUSMODUS_NIX = 546;  // // Rest position --> Mouse is deactivated
    public static final int MAUSMODUS_ZOOM_AUTOFIT = 547;  // // Chart always adapts to the data values
    public static final int MAUSMODUS_ZOOM_FENSTER = 548;  // // you can mark zoom rectangles with the mouse
    public static final int MAUSMODUS_ZEICHNE_LINIE = 550;  // Linien zeichnen (als Objekte!)
    public static final int MAUSMODUS_WERTANZEIGE_SCHIEBER = 554;  // // a slider can be placed over all diagrams, the corresponding y values ​​and all curves are displayed
    //
    public static final int MOUSE_CLICKED = 780;
    public static final int MOUSE_PRESSED = 781;
    public static final int MOUSE_RELEASED = 782;
    public static final int MOUSE_DRAGGED = 783;
    private int mausModus = MAUSMODUS_NIX;  // default --> Maus deaktiviert
    private int mausModusALT = MAUSMODUS_NIX;  // // so that you can return to the previous mode, e.g. after pressing AutoFit
    //
    private boolean simulationLaeuftGerade = false;
    private boolean nochNichtGeZoomt = true;
    private double[][] worksheetDatenTEMP = null;  // // The simulation data is stored here before zooming
    private int zvCounterTEMP = 0;  // // current pointer before zooming, is reactivated with AUTO_FIT
    //---------------------------------
    private boolean xSchieberAktiv = false;
    private int xSchieberPix;
    private double[] xSchieberWert = new double[]{-1, -1};  // // a single pixel point may have multiple values ​​assigned to it
    private double[][] ySchieberWert;  // // For each curve there is a corresponding ySliderValue point pair for the xSliderValue point pair
    private TechFormat cf = new TechFormat();
    private NumberFormat nf = NumberFormat.getNumberInstance();
    //==========================================
    private ArrayList<String> txtEintraege = new ArrayList<>();
    private int xSchieberPix2;
    private double[] xSchieberWert2 = new double[]{-1, -1};
    private double[][] ySchieberWert2;
    boolean inDiffMode = false;
    double[][] crvTransparency;

    public final void setAnzahlSichtbarerDiagramme(final int number) {
        this.anzGrfVisible = number;
    }

    public final int getAnzahlSichtbarerDiagramme() {
        return this.anzGrfVisible;
    }

    public final void setAnzahlDiagramme(final int number) {
        this.anzDiagram = number;
    }

    public final int getAnzahlDiagramme() {
        return this.anzDiagram;
    }

    public final double getSlider1Value() {
        return xSchieberWert[0];
    }

    public final double getSlider2Value() {
        return xSchieberWert2[0];
    }

    public final void setSimulationLaeuftGerade(final boolean simIsRunning) {
        this.simulationLaeuftGerade = simIsRunning;
        this.nochNichtGeZoomt = true;
        //-----------------
        if (worksheetDatenTEMP != null) {
            for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                    worksheetData.setValue(worksheetDatenTEMP[i1][i2], i1, i2);
                }
            }
            worksheetDatenTEMP = null;
            nochNichtGeZoomt = true;
        }
        //-----------------
    }

    public final void setCrvAchsenTyp(final int im1, final int im2, final int typ) {
        matrixZuordnungKurveDiagram[im1][im2] = typ;
        crvAchsenTyp[im1][im2] = typ;
    }

    public final int getCrvAchsenTyp(final int im1, final int im2) {
        return matrixZuordnungKurveDiagram[im1][im2];
    }

    public GraferImplementation(final Scopable scope) {
        this._scope = scope;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        //------------------------

        // XXX _scopeSettings = scope.getScopeSettings();
        // // XXX _scopeSettings.loadSettings(this);  // 'this' is parameterized here

        //------------------------
    }

    // // specifies how many of the points in the worksheet should be displayed as a curve
    // // Transfer as an array so that the transfer can be done as a reference
    public final void setZVCounter(final int zvCounter) {
        this._zvCounter = zvCounter;
    }

    // // for calling external data that is no longer changed (in contrast to the continuous curve buildup in circuit simulation) -->
    // // or when initializing/changing the number of curves for the simulation
    public final void setzeKurvenUndWorksheetDaten(final String[] header, final DataContainer workSheet) {
        this.worksheetData = workSheet;

        this.signalNamen = header;
        this.anzSignalePlusZeit = header.length;
        //------------------------
        // XXX _scopeSettings.usesExternalData = this._usesExternalData;
        // // XXX _scopeSettings.update_ZVs(anzSignalePlusZeit, signalNamen);  // default initialization of the curves or when the number of curves changes
        // // XXX _scopeSettings.loadSettings(this);  // the parameters defined in 'ScopeSettings' are imported
        //--------------------------
        if (this._usesExternalData) {
            // // Data is imported from external -->
            this.definiereAchsenbegrenzungenImAutoZoom(workSheet);  // // minX[],maxX[],minY[],maxY[],minY2[],maxY2[] are calculated from 'worksheetData'
            this.initClipping();  // // crvClipValXmin[][],crvClipValXmax[][],crvClipValYmin[][],crvClipValYmax[][] are calculated
            this.initAutotickSpacing();  // // requires 'minX[],maxX[],minY[],maxY[],...' for calculation
        } else {
            // // Data comes from the running ZV simulation / SCOPEs are initialized here before simulation data is available -->
            // // (1) // minX[],maxX[],minY[],maxY[],minY2[],maxY2[] are arbitrarily set initially:
            for (int i1 = 0; i1 < minX.length; i1++) {
                // XXX minX[i1] = SimulationKernel.t1SCOPE;
                // XXX maxX[i1] = SimulationKernel.t2SCOPE;
                // XXX minY[i1] = -10;
                // XXX maxY[i1] = +10;
            }
            // // (2) crvClipValXmin[][],crvClipValXmax[][],crvClipValYmin[][],crvClipValYmax[][] are calculated
            this.initClipping();
            // // (3) Auto-Ticks // require minX[],maxX[],minY[],maxY[],... for calculation
            this.initAutotickSpacing();
        }
        //--------------------------
        // // for the slider:
        ySchieberWert = new double[worksheetData.getRowLength() - 1][2];
        ySchieberWert2 = new double[worksheetData.getRowLength() - 1][2];

        //-------------------------------------
        this.setAxes();  // // the default values ​​of the axes are defined and properly prepared and passed on to GraferV3, the tick parameters were determined in 'initAutotickSpacing()'
        this.setCurves();  // // the default values ​​of the curves (defined in 'setDefault_ZVs') are properly prepared and passed on to GraferV3
        //--------------------------
        this.repaint();
    }

    // // is called regularly by the simulator to update the curve images -->
    //
    public void akualisiereKurvenUndWorksheetDaten(final double t1, final double t2) {
        //--------------------------
        this.definiereAchsenbegrenzungenNumerischeSimulation(t1, t2);  // // minX[],maxX[],minY[],maxY[] are calculated from 'worksheetData'
        //--------------------------
        this.setAxes();  // // the default values ​​of the axes are defined and properly prepared and passed on to GraferV3, the tick parameters were determined in 'initAutotickSpacing()'
        this.setCurves();  // // the default values ​​of the curves (defined in 'setDefault_ZVs') are properly prepared and passed on to GraferV3
        //--------------------------
        this.possiblyHideGridLines();
        this.repaint();
    }

    // // Overwritten so that you can easily draw SIGNAL curves -->
    @Override
    protected void drawCurves(final Graphics g) {
        if (worksheetData == null) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g;
        int zd = 0;  // Beschriftungs-Nummerierung in y-Richtung
        for (int i1 = 0; i1 < numCurves; i1++) {
            if (matrixZuordnungKurveDiagram[indexDerKurveInDerMatrix[i1] / INDEX_ENCODING_FACTOR][indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR] == ZUORDNUNG_SIGNAL) {
                try {
                    zeichneEinzelneSIGNALKurve(g2, i1);
                } catch (Exception e) {
                    Logger.getLogger(GraferImplementation.class.getName()).log(Level.WARNING, "Error drawing SIGNAL curve.", e);
                }  // SIGNAL --> siehe Implementierung gleich unten
            } else {
                int anzKurvenpunkteImWorksheet = worksheetData.getColumnLength();
                if (!_usesExternalData) {
                    anzKurvenpunkteImWorksheet = _zvCounter;
                }
                try {
                    drawSingleCurve(g2, i1, anzKurvenpunkteImWorksheet);
                } catch (Exception e) {
                    Logger.getLogger(GraferImplementation.class.getName()).log(Level.WARNING, "Error drawing ZV curve.", e);
                }  // // ZV --> is standard in 'GraferV3'
                //----------
                if ((i1 > 0) && (_yAxisY[indexCurveAssociatedYAxis[i1]] != _yAxisY[indexCurveAssociatedYAxis[i1 - 1]])) {
                    zd = 0;
                } else {
                    if (i1 > 0) {
                        zd++;
                    }
                }
                this.beschrifteNamenDerEinzelnenZVKurve(g2, i1, zd);  // // ZV curve labeling: for the sake of generality, is not implemented in 'GraferV3' but rather below
            }
        }
    }

    // // Labeling of the curve names of the ZV curves in the graph-->
    // i1 ... KurvenNummer
    private void beschrifteNamenDerEinzelnenZVKurve(final Graphics2D g2D, final int i1, final int zd) {
        //--------------------------------
        int yLinksObenKurve = _yAxisY[indexCurveAssociatedYAxis[i1]] - heightPix[indexCurveAssociatedYAxis[i1]];
        String name = signalNamen[indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR];
        cf.setMaximumDigits(4);
        String wert = cf.formatT(ySchieberWert[indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR - 1][0], TechFormat.FORMAT_AUTO);

        if (inDiffMode) {
            int index = indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR - 1;
            wert = cf.formatT(ySchieberWert2[index][0] - ySchieberWert[index][0], TechFormat.FORMAT_AUTO);
        }

        // // if the slider is activated, the y value is displayed instead of the names -->
        int delta = 16;  // // Distance between the signal names in the graph legend
        g2D.setColor(curveColor[i1]);
        if (xSchieberAktiv) {
            Font oldFont = g2D.getFont();
            Font tmpFont = new Font("Arial", Font.PLAIN, 9);
            g2D.setFont(tmpFont);

            g2D.drawString(name + " =", this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y, yLinksObenKurve + g2D.getFont().getSize() + 2 * zd * delta);
            String labelString = "";
            if (inDiffMode) {
                labelString += "diff ";
            }
            labelString += wert;
            g2D.drawString(labelString, this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y, yLinksObenKurve + g2D.getFont().getSize() + 2 * zd * delta + delta);
            g2D.setFont(oldFont);
        } else {
            g2D.drawString(name, this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y, yLinksObenKurve + g2D.getFont().getSize() + zd * delta);
        }
        //--------------------------------
    }

    // // Overwritten so that you can easily draw a 'grid' for SIGNAL curves -->
    @Override
    protected void drawCoordinateAxes(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        calculateSliderValues();
        //
        //===============================================
        valueTickX = new double[numAxesX][];
        tickX = new int[numAxesX][];
        valueTickXminor = new double[numAxesX][];
        tickXminor = new int[numAxesX][];
        valueTickY = new double[numAxesY][];
        tickY = new int[numAxesY][];
        valueTickYminor = new double[numAxesY][];
        tickYminor = new int[numAxesY][];
        //
        sfX = new double[numAxesX];
        sfY = new double[numAxesY];
        //===============================================
        // // if the grid lines are hidden (automatically because the diagram is too small in pixel points),
        // // then a border box is drawn for the diagram -->
        //


        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            if (zeichneDiagrammUmrandung[i1]) {
                g2.setColor(Color.lightGray);
                g2.drawRect(_xAxisX[i1], _xAxisY[i1] - heightPix[i1], widthPix[i1], heightPix[i1]);
            }
        }
        //===============================================
        // // x-axes --> there is no messing around (it's the same for ZV and SIGNAL)
        for (int i1 = 0; i1 < numAxesX; i1++) {
            drawSingleCoordinateAxisX(g2, i1);
        }
        // // y-axes --> is different for SIGNAL (grid and labels)
        for (int i1 = 0; i1 < numAxesY; i1++) {
            if (diagramTyp[i1] == GraferImplementation.DIAGRAM_TYP_ZV) {
                drawSingleCoordinateAxisY(g2, i1);  // // the ticks for the grid are also calculated here
            } else {
                zeichneEinzelneSIGNALKoordinatenAchse_Y(g2, i1);
            }
        }
        //------------------------
        drawGridNormalX(g);
        drawGridNormalY(g);
        //===============================================
        // // subsequently the
        // // be covered
        // // the methods 'this.drawGrid_NormalX(g)' and 'this.drawGrid_NormalY(g)' cannot precede the loop for drawing the axes
        // // be put because in 'this.drawSingleCoordinateAxis_X(g2,i1)' and 'this.drawSingleCoordinateAxis_Y(g2,i1)' first of all
        // // the grid needs to be calculated
        //
        final GeneralPath grL = new GeneralPath();
        //
        for (int i1 = 0; i1 < numAxesX; i1++) {
            g2.setColor(colorAxesX[i1]);
            // TODO: replace with switch statement
            if (lineStyleAxesX[i1] == SOLID_PLAIN) {
                g2.setStroke(str_SOLID_PLAIN);
            } else if (lineStyleAxesX[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
            } else if (lineStyleAxesX[i1] == SOLID_FAT_1) {
                g2.setStroke(str_SOLID_FAT_1);
            } else if (lineStyleAxesX[i1] == SOLID_FAT_2) {
                g2.setStroke(str_SOLID_FAT_2);
            } else if (lineStyleAxesX[i1] == DOTTED_PLAIN) {
                g2.setStroke(str_DOTTED_PLAIN);
            } else if (lineStyleAxesX[i1] == DOTTED_FAT) {
                g2.setStroke(str_DOTTED_FAT);
            }
            //-----------------------
            // // now draw the line:
            grL.reset();
            grL.moveTo(_xAxisX[i1], _xAxisY[i1]);
            grL.lineTo(_xAxisX[i1] + widthPix[i1], _xAxisY[i1]);
            if (lineStyleAxesX[i1] != GraferV3.INVISIBLE) {
                g2.draw(grL);
                g2.drawString(xAxisLabel[i1], _xAxisX[i1] + widthPix[i1] / 2, _xAxisY[i1] + posXtickLabels[i1]);
            }
            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        }
        for (int i1 = 0; i1 < numAxesY; i1++) {
            g2.setColor(colorAxesY[i1]);
            // TODO: replace with switch statement
            if (lineStyleAxesY[i1] == SOLID_PLAIN) {
                g2.setStroke(str_SOLID_PLAIN);
            } else if (lineStyleAxesY[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
            } else if (lineStyleAxesY[i1] == SOLID_FAT_1) {
                g2.setStroke(str_SOLID_FAT_1);
            } else if (lineStyleAxesY[i1] == SOLID_FAT_2) {
                g2.setStroke(str_SOLID_FAT_2);
            } else if (lineStyleAxesY[i1] == DOTTED_PLAIN) {
                g2.setStroke(str_DOTTED_PLAIN);
            } else if (lineStyleAxesY[i1] == DOTTED_FAT) {
                g2.setStroke(str_DOTTED_FAT);
            }
            //-----------------------
            // // now draw the line:
            grL.reset();
            grL.moveTo(_yAxisX[i1], _yAxisY[i1]);
            grL.lineTo(_yAxisX[i1], _yAxisY[i1] - heightPix[i1]);
            if (lineStyleAxesY[i1] != GraferV3.INVISIBLE) {
                g2.draw(grL);
                g2.drawString(yAxisLabel[i1], _yAxisX[i1] - posYtickLabels[i1], _yAxisY[i1] - heightPix[i1] / 2);
            }
            g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        }
        //==================================
    }

    // // Overwrite for SIGNAL:
    protected void zeichneEinzelneSIGNALKoordinatenAchse_Y(final Graphics2D g2, final int i1) {

        GeneralPath grL = new GeneralPath();
        // i1 ... AchsenNummer --> Achtung: pro Graph gibt es je zwei y-Achsen
        // // SIGNAL --> y-axis is ALWAYS 'LIN'
        //
        //==================================
        // // there is a y tick at '0' and one at '1'; namely for each SIGNAL course within the corresponding graph
        int z = 0;
        for (int i3 = 0; i3 < indexDerKurveInDerMatrix.length; i3++) {
            final int grf = indexDerKurveInDerMatrix[i3] / INDEX_ENCODING_FACTOR;
            if (grf == i1 / 2) {
                z++;
            }
        }
        //------------------------
        //
        final int anzTicks = 2 * z;
        valueTickY[i1] = new double[anzTicks];  // // y numerical value associated with the tick --> is not used here
        tickY[i1] = new int[anzTicks];  // Pixel-Position
        tickY[i1][0] = _yAxisY[i1];
        for (int i2 = 0; i2 < anzTicks; i2++) {
            if (i2 % 2 == 0) {
                valueTickY[i1][i2] = 0.0;
                if (i2 > 0) {
                    tickY[i1][i2] = tickY[i1][i2 - 1] - sgnDistance[i1];
                }
            } else {
                valueTickY[i1][i2] = 1.0;
                tickY[i1][i2] = tickY[i1][i2 - 1] - sgnHeight[i1];
            }
        }
        // // no minor ticks with SIGNAL -->
        final int yMinorTicksAnzahl = 0;
        valueTickYminor[i1] = new double[yMinorTicksAnzahl];
        tickYminor[i1] = new int[yMinorTicksAnzahl];
        //==================================
        if (i1 % 2 != 0) {
            return;  // // only the left y-axis is drawn!
        }        //
        g2.setColor(colorAxesY[i1]);

        // TODO: replace with switch expression!
        if (lineStyleAxesY[i1] == SOLID_PLAIN) {
            g2.setStroke(str_SOLID_PLAIN);
        } else if (lineStyleAxesY[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
        } else if (lineStyleAxesY[i1] == SOLID_FAT_1) {
            g2.setStroke(str_SOLID_FAT_1);
        } else if (lineStyleAxesY[i1] == SOLID_FAT_2) {
            g2.setStroke(str_SOLID_FAT_2);
        } else if (lineStyleAxesY[i1] == DOTTED_PLAIN) {
            g2.setStroke(str_DOTTED_PLAIN);
        } else if (lineStyleAxesY[i1] == DOTTED_FAT) {
            g2.setStroke(str_DOTTED_FAT);
        }
        //-----------------------
        // // now draw the line:
        grL.reset();
        grL.moveTo(_yAxisX[i1], _yAxisY[i1]);
        grL.lineTo(_yAxisX[i1], _yAxisY[i1] - heightPix[i1]);
        if (lineStyleAxesY[i1] != GraferV3.INVISIBLE) {
            g2.draw(grL);
            g2.drawString(yAxisLabel[i1], _yAxisX[i1] - posYtickLabels[i1], _yAxisY[i1] - heightPix[i1] / 2);
        }
        g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        //==================================
    }

    private void reorderLine(final int[] positionSIGNAL, int z1, int z2) {
        // // the line to be processed is marked in the array 'positionSIGNAL[]' from z1 to z2 -->
        int[] toBeOrdered = new int[z2 - z1];
        int anzAlteEintraege = 0;  // ungleich '-1'
        for (int i1 = z1; i1 <= z2 - 1; i1++) {
            toBeOrdered[i1 - z1] = positionSIGNAL[i1];
            if (positionSIGNAL[i1] != -1) {
                anzAlteEintraege++;
            }
        }
        // zuerst alles ungleich '-1' durchgehend aufsteigend nummerieren --> 
        for (int zahl = 0; zahl < anzAlteEintraege; zahl++) {
            boolean noX = true;
            while (noX) {
                boolean lokNoX = true;
                for (int i1 = 0; i1 < toBeOrdered.length; i1++) {
                    if (toBeOrdered[i1] == zahl) {
                        lokNoX = false;
                        noX = false;
                    }
                }
                if (lokNoX) {
                    for (int i1 = 0; i1 < toBeOrdered.length; i1++) {
                        if ((toBeOrdered[i1] != -1) && (toBeOrdered[i1] > zahl)) {
                            toBeOrdered[i1]--;
                        }
                    }
                }
            }
        }
        // // now number everything '-1' after the old values ​​in ascending order -->
        for (int i1 = 0; i1 < toBeOrdered.length; i1++) {
            if (toBeOrdered[i1] == -1) {
                toBeOrdered[i1] = anzAlteEintraege;
                anzAlteEintraege++;
            }
        }
        //-----------
        for (int i1 = z1; i1 <= z2 - 1; i1++) {
            positionSIGNAL[i1] = toBeOrdered[i1 - z1];
        }
        //-----------
    }

    // // y-order of the SIGNAL progressions -->
    // // if 'number of curves' was changed, the unchanged SIGNAL ZVs must be retained!
    // // therefore the old value of 'indexDerKurveInDerMatrix[]' must be saved and a corresponding conversion must be carried out
    private void setzeYPositionDerSIGNALverlaeufe() {
        //-------------------------------------
        if (positionSIGNAL != null) {
            //------------------------
            // // updated because the number of curves could have changed, first mark everything with '-1' -->
            positionSIGNAL = new int[kurvenanzahl];
            for (int i1 = 0; i1 < kurvenanzahl; i1++) {
                positionSIGNAL[i1] = -1;
            }
            // 
            // // old SIGNAL positions are copied into the new 'positionSIGNAL' field,
            // // the values ​​that have not been overwritten are still marked with a negative sign
            for (int i1 = 0; i1 < indexDerKurveInDerMatrix.length; i1++) {
                for (int i2 = 0; i2 < indexDerKurveInDerMatrixALT.length; i2++) {
                    if (indexDerKurveInDerMatrix[i1] == indexDerKurveInDerMatrixALT[i2]) {
                        positionSIGNAL[i1] = positionSIGNAL_ALT[i2];
                    }
                }
            }
            // 
            // // now go through the ConnectionMatrix line by line:
            // // number the old entries continuously from 0 upwards in each line,
            // // then number the '-1' entries in ascending order -->
            int z1 = 0, z2 = 0;
            while (z2 < kurvenanzahl) {
                while ((z2 < kurvenanzahl) && (indexDerKurveInDerMatrix[z1] / INDEX_ENCODING_FACTOR == indexDerKurveInDerMatrix[z2] / INDEX_ENCODING_FACTOR)) {
                    z2++;
                }
                this.reorderLine(positionSIGNAL, z1, z2);
                z1 = z2;
            }

        } else {
            //-------------------------------------
            positionSIGNAL = new int[kurvenanzahl];
            int positionsZaehler = 0;
            positionSIGNAL[0] = positionsZaehler;
            positionsZaehler++;
            for (int i1 = 1; i1 < kurvenanzahl; i1++) {
                if (indexDerKurveInDerMatrix[i1] / INDEX_ENCODING_FACTOR != indexDerKurveInDerMatrix[i1 - 1] / INDEX_ENCODING_FACTOR) {
                    positionsZaehler = 0;  // // Reset with new graph
                }
                positionSIGNAL[i1] = positionsZaehler;
                positionsZaehler++;
            }
            // // Note: the curve in the matrix on the far left always has 'positionSIGNAL==0'
            //
            this.speichereALTeWerteFuerPosition();
            //-------------------------------------
        }

    }

    // // for access from 'DialogOrdnungSIGNAL' -->
    public int[] getPositionSIGNAL() {
        return positionSIGNAL;
    }

    public void setPositionSIGNAL(final int[] positionSIGNAL) {
        this.positionSIGNAL = positionSIGNAL;
        this.speichereALTeWerteFuerPosition();
    }

    private void speichereALTeWerteFuerPosition() {
        if ((positionSIGNAL == null) || (indexDerKurveInDerMatrix == null)) {
            return;
        }
        //-------------------------------------
        positionSIGNAL_ALT = new int[positionSIGNAL.length];
        System.arraycopy(positionSIGNAL, 0, positionSIGNAL_ALT, 0, positionSIGNAL.length);
        //-------------------------------------
        // // keep a copy of 'indexDerKurveInDerMatrix' so that the display sequence of SIGNAL can be carried out correctly:
        indexDerKurveInDerMatrixALT = new int[indexDerKurveInDerMatrix.length];
        System.arraycopy(indexDerKurveInDerMatrix, 0, indexDerKurveInDerMatrixALT, 0, indexDerKurveInDerMatrix.length);
        //-------------------------------------
    }

    public void calculateRequiredHeightSignalGraph() {
        //-------------------------------------
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            notwendigeHoehePixGRF[i1] = -1;  // default --> kein SIGNAL-Graph sondern ZV-Graph
            if (diagramTyp[i1] == GraferImplementation.DIAGRAM_TYP_SGN) {
                int anzSGN = 0;  // // Number of SIGNAL curves per SIGNAL graph
                for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                    if (crvAchsenTyp[i1][i2] == GraferImplementation.ZUORDNUNG_SIGNAL) {
                        anzSGN++;
                    }
                }
                notwendigeHoehePixGRF[i1] = anzSGN * (sgnHeight[i1] + sgnDistance[i1]);
                notwendigeHoehePixGRF[i1] += (DY_IN_OBEN + DY_IN_UNTEN);

            }
        }
    }

    private int getHeightForZVInPixels() {
        // // the height available for the ZVs, ie. Total height minus SIGNAL heights -->
        int height = this.getHeight() - ABSTAND_BESCHRIFTUNG_XACHSE;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if (diagramTyp[i1] == GraferImplementation.DIAGRAM_TYP_SGN) {
                height -= notwendigeHoehePixGRF[i1];
            }
        }
        return height;
    }

    private void zeichneEinzelneSIGNALKurve(Graphics2D g2, int i1) {

        final GeneralPath grL = new GeneralPath();
        // i1 ... KurvenNummer
        //===============================================
        // // only the calculated data points are drawn --> zvCounter
        //----------------------
        final int[] xPix = new int[_zvCounter];
        final int[] yPix = new int[_zvCounter];
        final int x0Kurve = _xAxisX[indexCurveAssociatedXAxis[i1]];  // // associated x-axis defines x0 of the curve
        final int y0Kurve = _yAxisY[indexCurveAssociatedYAxis[i1]] - (notwendigeHoehePixGRF[indexCurveAssociatedXAxis[i1]] - (DY_IN_OBEN + DY_IN_UNTEN));  // // associated y-axis defines y0 of the curve, 'necessaryHeightPixGRF[i1]' for optical inversion (curve at the top left)
        final int delta = sgnDistance[indexCurveAssociatedXAxis[i1]] + sgnHeight[indexCurveAssociatedXAxis[i1]];

        //
        for (int i2 = 0; i2 < _zvCounter; i2++) {
            final double xValue = worksheetData.getValue(curve_index_worksheetColumns_XY[i1][0], i2);
            if (xAxisType[indexCurveAssociatedXAxis[i1]] == AXIS_LINEAR) {
                xPix[i2] = x0Kurve + (int) (sfX[indexCurveAssociatedXAxis[i1]] * (xValue - axisXmin[indexCurveAssociatedXAxis[i1]]));
            } else if ((xAxisType[indexCurveAssociatedXAxis[i1]] == AXIS_LOGARITHMIC)) {
                xPix[i2] = x0Kurve + (int) (sfX[indexCurveAssociatedXAxis[i1]] * lg10(xValue / axisXmin[indexCurveAssociatedXAxis[i1]]));
            }
            //------------------
            double yValue = worksheetData.getValue(curve_index_worksheetColumns_XY[i1][1], i2);
            // // Threshold turns the analog signal into a digital signal -->
            try {
                if (yValue < sgnSchwelle[indexCurveAssociatedXAxis[i1]]) {
                    yValue = positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]];
                } else {
                    yValue = (positionSIGNAL[i1] * delta);
                }
                yPix[i2] = (y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]]) + (int) yValue;
            } catch (Exception e) {
                Logger.getLogger(GraferImplementation.class.getName()).log(Level.WARNING, "Error computing SIGNAL pixel value.", e);
            }
        }
        //--------------------------------
        g2.setColor(curveColor[i1]);

        // TODO: replace with switch statement
        if (curveLineStyle[i1] == SOLID_PLAIN) {
            g2.setStroke(str_SOLID_PLAIN);
        } else if (curveLineStyle[i1] == INVISIBLE) {     // nix machen, weil unsichtbar
        } else if (curveLineStyle[i1] == SOLID_FAT_1) {
            g2.setStroke(str_SOLID_FAT_1);
        } else if (curveLineStyle[i1] == SOLID_FAT_2) {
            g2.setStroke(str_SOLID_FAT_2);
        } else if (curveLineStyle[i1] == DOTTED_PLAIN) {
            g2.setStroke(str_DOTTED_PLAIN);
        } else if (curveLineStyle[i1] == DOTTED_FAT) {
            g2.setStroke(str_DOTTED_FAT);
        } else {
            assert false;
        }
        //-----------------------
        // // for labeling the SIGNAL-ZV in the graph -->
        final String name = signalNamen[indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR];
        if (xSchieberAktiv) {
            g2.drawString(
                    (ySchieberWert[indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR - 1][0] < sgnSchwelle[indexCurveAssociatedXAxis[i1]] ? "off" : "on"),
                    DX_IN_LINKS - 30,
                    (y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]]) + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]]));
        }
        g2.drawString(
                name,
                this.getWidth() - DX_IN_RECHTS + TXT_DISTANCE_Y,
                (y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]]) + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]]));
        //-----------------------
        // // now draw the SIGNAL line:
        g2.setClip(x0Kurve + 1, 0, widthPix[indexCurveAssociatedYAxis[i1]] - 2, CLIP_HEIGHT);

        grL.reset();
        if (curveLineStyle[i1] != GraferV3.INVISIBLE) {
            grL.moveTo(xPix[0], yPix[0]);
            for (int i5 = 1; i5 < _zvCounter; i5++) {
                if (yPix[i5] != yPix[i5 - 1]) {  // // Switching process is implemented in the middle between 2 data points --> visual improvement
                    grL.lineTo((xPix[i5 - 1] + xPix[i5]) / 2, yPix[i5 - 1]);
                    grL.lineTo((xPix[i5 - 1] + xPix[i5]) / 2, yPix[i5]);
                }
                grL.lineTo(xPix[i5], yPix[i5]);
            }
            //---------------
            // // optional color filling of the digital signals to better distinguish between '0' and '1' -->
            final GeneralPath grFill = new GeneralPath();
            grFill.append(grL.getPathIterator(null), false);
            final int nullLinie = (y0Kurve + sgnDistance[indexCurveAssociatedXAxis[i1]]) + (int) (positionSIGNAL[i1] * delta + sgnHeight[indexCurveAssociatedXAxis[i1]]);
            grFill.lineTo(xPix[_zvCounter - 1], nullLinie);
            grFill.lineTo(xPix[0], nullLinie);
            if (yPix[0] < nullLinie) {
                grFill.lineTo(xPix[0], yPix[0]);
            }
            //grFill.closePath();
            //---------------
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / INDEX_ENCODING_FACTOR);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR);
            if (crvFillDigitalCurves[im1][im2]) {
                final Color fuellFarbe = GraferV3.selectColor(crvFillingDigitalColor[im1][im2]);
                g2.setColor(fuellFarbe);
                g2.fill(grFill.createTransformedShape(null));
            }
            //---------------
            g2.setColor(curveColor[i1]);

            final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALPHA_COMPOSITE_VALUE);
            g2.setComposite(ac);
            g2.draw(grL);
            //---------------
        }
        g2.setStroke(str_SOLID_PLAIN);  // wieder auf 'default' setzen
        g2.setClip(null);

        final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        g2.setComposite(ac);

        //===============================================
    }

    // // is called by 'DigitalDialogGraphProperties' and 'DialogGraphProperties' -->
    public void definiereAchsenbegrenzungenImAutoZoom() {
        this.definiereAchsenbegrenzungenImAutoZoom(worksheetData);
    }

    private void definiereAchsenbegrenzungenImAutoZoom(DataContainer ws) {
        //--------------------------
        final double[] tickAbstandY = new double[ANZ_DIAGRAM_MAX], tickAbstandY2 = new double[ANZ_DIAGRAM_MAX];
        //--------------------------
        // // to increase efficiency: the smallest and largest values ​​are determined for each WS column -->
        final double[] w1 = new double[ws.getRowLength()], w2 = new double[ws.getRowLength()];
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {
            w1[i1] = +1e99;
            w2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {  // // goes through the columns
            for (int i2 = 0; i2 < _zvCounter; i2++) {  // // goes through the selected column line by line
                if (ws.getValue(i1, i2) < w1[i1]) {
                    w1[i1] = ws.getValue(i1, i2);
                }
                if (ws.getValue(i1, i2) > w2[i1]) {
                    w2[i1] = ws.getValue(i1, i2);
                }
            }
        }
        //--------------------------
        for (int i1 = 0; i1 < minX.length; i1++) {
            minX[i1] = +1e99;
            maxX[i1] = -1e99;
            minY[i1] = +1e99;
            maxY[i1] = -1e99;
        }
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {   // // goes through the lines
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X) {
                    minX[i1] = w1[i2];
                    maxX[i1] = w2[i2];
                    // // --> sufficient because there is only one X-axis per matrix row
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                    // // --> Comparison with the limitations of any other Y-axes
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_SIGNAL) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                    // // is only necessary if you switch DIGITAL --> ANALOG
                }
            }
            // // if Y is defined, but not Y2, then there is still no axis limit for the Y2 axis (and vice versa)
            // // --> the Y2 limits are then set equal to the Y limits (and vice versa) -->
            //if ((minY2[i1]==+1e99)||(maxY2[i1]==-1e99)) { minY2[i1]=minY[i1];   maxY2[i1]=maxY[i1]; }
            //if ((minY[i1] ==+1e99)||(maxY[i1] ==-1e99)) { minY[i1]= minY2[i1];  maxY[i1]= maxY2[i1]; }
            //
            // 'schoenere' Bereichsgrenzen -->
            final double[] autoEmpf = autoAxisLimitRecommendation(minY[i1], maxY[i1]);
            minY[i1] = autoEmpf[0];
            maxY[i1] = autoEmpf[1];
            tickAbstandY[i1] = autoEmpf[4];
        }
        //--------------------------
        double[] xx1 = new double[anzGrfVisible], xx2 = new double[anzGrfVisible];  // X-Achse
        double[] yy1 = new double[anzGrfVisible], yy2 = new double[anzGrfVisible];  // // Y-axis --> Still needs to be customized!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // // is auto-scaling turned on?
        for (int i1 = 0; i1 < xx1.length; i1++) {
            xx1[i1] = minX[i1];
            xx2[i1] = maxX[i1];
            scX[i1] = autoScaleX[i1];
        }
        for (int i1 = 0; i1 < yy1.length; i1++) {
            yy1[i1] = minY[i1];
            yy2[i1] = maxY[i1];
            scY[i1] = autoScaleY[i1];
        }
        this.setAxesLimits(xx1, xx2, scX, yy1, yy2, scY);
        //-------------------
        // initAutoTickSpacing() -->
        for (int i1 = 0; i1 < ANZ_DIAGRAM_MAX; i1++) {
            xTickSpacing[i1] = this.getAutoTickSpacingX(i1);
            yTickSpacing[i1] = tickAbstandY[i1];
        }
        this.setTickSpacing(xTickSpacing, yTickSpacing);
        //
        repaint();
        //--------------------------
    }

    private void definiereAchsenbegrenzungenNumerischeSimulation(double t1, double t2) {
        //--------------------------
        final DataContainer ws = this.worksheetData;
        final double[] tickAbstandY = new double[ANZ_DIAGRAM_MAX], tickAbstandY2 = new double[ANZ_DIAGRAM_MAX];
        //--------------------------
        // // to increase efficiency:
        // // The smallest and largest values ​​are determined for each WS column -->
        final double[] w1 = new double[worksheetData.getRowLength()], w2 = new double[worksheetData.getRowLength()];
        for (int i1 = 0; i1 < w1.length; i1++) {
            w1[i1] = +1e99;
            w2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < ws.getRowLength(); i1++) {  // // goes through the columns
            for (int i2 = 0; i2 < _zvCounter + 1; i2++) {  // // goes through the selected column line by line
                if (ws.getValue(i1, i2) < w1[i1]) {
                    w1[i1] = ws.getValue(i1, i2);
                }
                if (ws.getValue(i1, i2) > w2[i1]) {
                    w2[i1] = ws.getValue(i1, i2);
                }
            }
        }
        //--------------------------
        for (int i1 = 0; i1 < minX.length; i1++) {
            minY[i1] = +1e99;
            maxY[i1] = -1e99;   // minX[i1]=+1e99;   maxX[i1]=-1e99;
        }
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {   // // goes through the lines
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X) {
                    minX[i1] = t1;
                    maxX[i1] = t2;
                    // // --> sufficient because there is only one X-axis per matrix row
                    if (minX[i1] == maxX[i1]) {
                        minX[i1] = 0;
                        maxX[i1] = 0.020;
                    }
                } else if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y) {
                    if (w1[i2] < minY[i1]) {
                        minY[i1] = w1[i2];
                    }
                    if (w2[i2] > maxY[i1]) {
                        maxY[i1] = w2[i2];
                    }
                }
            }
            // // if Y is defined, but not Y2, then there is still no axis limit for the Y2 axis (and vice versa)
            // // --> the Y2 limits are then set equal to the Y limits (and vice versa) -->
            //if ((minY2[i1]==+1e99)||(maxY2[i1]==-1e99)) { minY2[i1]=minY[i1];   maxY2[i1]=maxY[i1]; }
            //if ((minY[i1] ==+1e99)||(maxY[i1] ==-1e99)) { minY[i1]= minY2[i1];  maxY[i1]= maxY2[i1]; }
            //
            // 'schoenere' Bereichsgrenzen -->
            final double[] autoEmpf = autoAxisLimitRecommendation(minY[i1], maxY[i1]);
            minY[i1] = autoEmpf[0];
            maxY[i1] = autoEmpf[1];
            tickAbstandY[i1] = autoEmpf[4];
        }
        //--------------------------
        // // CLIPPING: Can only be called if 'worksheet' and 'minX[],maxX[],...' are defined -->
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {
            for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                if ((matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X) || (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y)) {
                    crvClipValXmin[i1][i2] = this.getXClipAchse(i1, i2)[0];
                    crvClipValXmax[i1][i2] = this.getXClipAchse(i1, i2)[1];
                    crvClipValYmin[i1][i2] = this.getYClipAchse(i1, i2)[0];
                    crvClipValYmax[i1][i2] = this.getYClipAchse(i1, i2)[1];
                }
            }
        }
        //----------------------------------------------
        // initAutoTickSpacing() -->
        //
        for (int i1 = 0; i1 < ANZ_DIAGRAM_MAX; i1++) {
            xTickSpacing[i1] = this.getAutoTickSpacingX(i1);
            yTickSpacing[i1] = tickAbstandY[i1];  // this.getAutoTickSpacingY(i1);
        }
        repaint();
        //--------------------------
    }

    @Override
    public void setCurves() {
        if (matrixZuordnungKurveDiagram == null) {
            return;
        }
        //-------------------------------------
        kurvenanzahl = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                if ((matrixZuordnungKurveDiagram[i1][i2] == GraferImplementation.ZUORDNUNG_Y) || (matrixZuordnungKurveDiagram[i1][i2] == GraferImplementation.ZUORDNUNG_SIGNAL)) {
                    kurvenanzahl++;
                }
            }
        }
        this.setCurvesCount(kurvenanzahl);
        //-------------------------------------
        this.speichereALTeWerteFuerPosition();
        //
        indexDerKurveInDerMatrix = new int[kurvenanzahl];  // // to assign the curve indices to the assignment matrix
        //
        int[] zugehoerigkeitX = new int[kurvenanzahl];
        int[] zugehoerigkeitY = new int[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            zugehoerigkeitX[i1] = -1;
            zugehoerigkeitY[i1] = -1;
        }

        for (int kurvenIndex = 0; kurvenIndex < kurvenanzahl; kurvenIndex++) {
            for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
                final int zugX = i1;  // // because all curves of a graph see the same x-axis
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if ((matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y) || (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_SIGNAL)) {
                        zugehoerigkeitX[kurvenIndex] = zugX;
                        zugehoerigkeitY[kurvenIndex] = i1;
                        indexDerKurveInDerMatrix[kurvenIndex] = INDEX_ENCODING_FACTOR * i1 + i2;
                        kurvenIndex++;
                    }
                }
            }
        }
        this.setCurveAxesAssignment(zugehoerigkeitX, zugehoerigkeitY);
        //-------------------------------------
        this.setzeYPositionDerSIGNALverlaeufe();
        //-------------------------------------
        indexWsXY = new int[kurvenanzahl][2];
        //
        for (int kurvenIndex = 0; kurvenIndex < kurvenanzahl; kurvenIndex++) {
            for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
                int zugX = -1;
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X) {
                        zugX = i2;
                    }
                }
                for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                    if ((matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y) || (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_SIGNAL)) {
                        indexWsXY[kurvenIndex][0] = zugX;
                        indexWsXY[kurvenIndex][1] = i2;
                        kurvenIndex++;
                    }
                }
            }
        }
        this.setCurveIndexWorksheetColumnsXY(indexWsXY);
        //
        //=====================================
        int[] crvAchsenTypLok = new int[kurvenanzahl];  // // For each matrix entry there is a unique axis type (X or Y or Y2)
        int[] crvLineStyleLok = new int[kurvenanzahl];
        int[] crvLineColorLok = new int[kurvenanzahl];
        final double[] crvTransparencyLok = new double[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / INDEX_ENCODING_FACTOR);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR);
            crvAchsenTypLok[i1] = crvAchsenTyp[im1][im2];
            crvLineStyleLok[i1] = crvLineStyle[im1][im2];
            crvLineColorLok[i1] = crvLineColor[im1][im2];
            crvTransparencyLok[i1] = crvTransparency[im1][im2];
        }
        this.setCurveLineStyle(crvLineStyleLok);
        //
        Color[] linienFarbe = new Color[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            linienFarbe[i1] = GraferV3.selectColor(crvLineColorLok[i1]);
        }
        this.setCurveColor(linienFarbe);
        this.setCurveTransparency(crvTransparencyLok);
        //=====================================
        boolean[] crvSymbShowLok = new boolean[kurvenanzahl];
        int[] crvSymbFrequLok = new int[kurvenanzahl];
        int[] crvSymbShapeLok = new int[kurvenanzahl];
        int[] crvSymbColorLok = new int[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / INDEX_ENCODING_FACTOR);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR);
            crvSymbShowLok[i1] = crvSymbShow[im1][im2];
            crvSymbFrequLok[i1] = crvSymbFrequ[im1][im2];
            crvSymbShapeLok[i1] = crvSymbShape[im1][im2];
            crvSymbColorLok[i1] = crvSymbColor[im1][im2];
        }
        Color[] crvSymbFarbeLok = new Color[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            crvSymbFarbeLok[i1] = GraferV3.selectColor(crvSymbColorLok[i1]);
        }
        this.setCurvePointSymbolVisible(crvSymbShowLok, crvSymbFrequLok, crvSymbShapeLok, crvSymbFarbeLok);
        //=====================================
        // // what type of clipping (axis, no clipping, value) ?  -->
        int[] crvClipXminLok = new int[kurvenanzahl], crvClipXmaxLok = new int[kurvenanzahl], crvClipYminLok = new int[kurvenanzahl], crvClipYmaxLok = new int[kurvenanzahl];
        // // if clipping to value, what specific numerical value?  -->
        double[] crvClipValXminLok = new double[kurvenanzahl], crvClipValXmaxLok = new double[kurvenanzahl];
        double[] crvClipValYminLok = new double[kurvenanzahl], crvClipValYmaxLok = new double[kurvenanzahl];
        for (int i1 = 0; i1 < kurvenanzahl; i1++) {
            final int im1 = (int) (indexDerKurveInDerMatrix[i1] / INDEX_ENCODING_FACTOR);
            final int im2 = (int) (indexDerKurveInDerMatrix[i1] % INDEX_ENCODING_FACTOR);
            crvClipXminLok[i1] = crvClipXmin[im1][im2];
            crvClipXmaxLok[i1] = crvClipXmax[im1][im2];
            crvClipYminLok[i1] = crvClipYmin[im1][im2];
            crvClipYmaxLok[i1] = crvClipYmax[im1][im2];
            crvClipValXminLok[i1] = crvClipValXmin[im1][im2];
            crvClipValXmaxLok[i1] = crvClipValXmax[im1][im2];
            crvClipValYminLok[i1] = crvClipValYmin[im1][im2];
            crvClipValYmaxLok[i1] = crvClipValYmax[im1][im2];
        }
        this.setCurveClipping(crvClipValXminLok, crvClipValXmaxLok, crvClipValYminLok, crvClipValYmaxLok, crvClipXminLok, crvClipXmaxLok, crvClipYminLok, crvClipYmaxLok);
        //=====================================
    }

    @Override
    public void setAxes() {
        //-------------------------------------
        anzGrfVisible = 0;  // // Number of graphs to display (i.e. visible==true)
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            anzGrfVisible++;
        }
        this.setAxesCount(anzGrfVisible, anzGrfVisible);
        // 
        zeichneDiagrammUmrandung = new boolean[anzGrfVisible];
        //-----------
        // // Area limits for mouse clicking --> is defined here for 2 diagrams
        final int breite = this.getWidth(), hoehe = this.getHeightForZVInPixels();
        xGrfMIN = new int[anzGrfVisible];
        xGrfMAX = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xGrfMIN.length; i1++) {
            xGrfMIN[i1] = 0;
            xGrfMAX[i1] = this.getWidth();
        }
        yGrfMIN = new int[anzGrfVisible];
        yGrfMAX = new int[anzGrfVisible];
        double ySpGes = 0;   // // Weighting of the y-axes
        int iyy = 1;  // // Index for y-axis
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        yGrfMIN[0] = 0;
        yGrfMAX[0] = (diagramTyp[0] == DIAGRAM_TYP_ZV) ? 0 + (int) (hoehe * (ySpacingDiagram[0] / ySpGes)) : 0 + notwendigeHoehePixGRF[0];
        for (int i1 = 1; i1 < this.getAnzahlDiagramme(); i1++) {
            yGrfMIN[iyy] = yGrfMAX[iyy - 1];
            yGrfMAX[iyy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? yGrfMIN[iyy] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : yGrfMIN[iyy] + notwendigeHoehePixGRF[i1];
            iyy++;
        }
        //-----------
        int[] laenge_xAchse = new int[anzGrfVisible], posX_xAchse = new int[anzGrfVisible], posY_xAchse = new int[anzGrfVisible];
        int[] laenge_yAchse = new int[anzGrfVisible], posX_yAchse = new int[anzGrfVisible], posY_yAchse = new int[anzGrfVisible];
        int ix = 0, iy = 0;  // // Index for x and y axes
        ySpGes = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            // X-Achse:
            laenge_xAchse[ix] = breite - (DX_IN_LINKS + DX_IN_RECHTS);
            posX_xAchse[ix] = DX_IN_LINKS;
            if (ix == 0) {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - DY_IN_UNTEN) : notwendigeHoehePixGRF[i1] - DY_IN_UNTEN;
            } else {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? posY_xAchse[ix - 1] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : posY_xAchse[ix - 1] + notwendigeHoehePixGRF[i1];
            }
            ix++;
            // Y-Achse:
            laenge_yAchse[iy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - (DY_IN_OBEN + DY_IN_UNTEN)) : notwendigeHoehePixGRF[i1] - (DY_IN_OBEN + DY_IN_UNTEN);
            posX_yAchse[iy] = posX_xAchse[ix - 1];
            posY_yAchse[iy] = posY_xAchse[ix - 1];
            iy++;
        }
        this.setAxisWidthHeightX0Y0(laenge_xAchse, laenge_yAchse, posX_xAchse, posY_xAchse, posX_yAchse, posY_yAchse);
        //-----------
        double[] x1 = new double[anzGrfVisible], x2 = new double[anzGrfVisible];  // X-Achse
        double[] y1 = new double[anzGrfVisible], y2 = new double[anzGrfVisible];  // // Y-axis --> Still needs to be customized!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // // is auto-scaling turned on?
        for (int i1 = 0; i1 < x1.length; i1++) {
            x1[i1] = minX[i1];
            x2[i1] = maxX[i1];
            scX[i1] = autoScaleX[i1];
        }
        for (int i1 = 0; i1 < y1.length; i1++) {
            y1[i1] = minY[i1];
            y2[i1] = maxY[i1];
            scY[i1] = autoScaleY[i1];
        }
        this.setAxesLimits(x1, x2, scX, y1, y2, scY);
        //-----------
        final String[] xAchseBeschriftungLok = new String[anzGrfVisible];
        final String[] yAchseBeschriftungLok = new String[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseBeschriftungLok.length; i1++) {
            xAchseBeschriftungLok[i1] = xAxisLabel[i1];
        }
        for (int i1 = 0; i1 < yAchseBeschriftungLok.length; i1++) {
            yAchseBeschriftungLok[i1] = yAxisLabel[i1];
        }
        this.setAxesLabels(xAchseBeschriftungLok, yAchseBeschriftungLok);
        //-----------
        int[] xAchseTypLoc = new int[anzGrfVisible], yAchseTypLoc = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseTypLoc.length; i1++) {
            xAchseTypLoc[i1] = xAchsenTyp[i1];
        }
        for (int i1 = 0; i1 < yAchseTypLoc.length; i1++) {
            yAchseTypLoc[i1] = yAchsenTyp[i1];
        }
        this.setAxesType(xAchseTypLoc, yAchseTypLoc);
        //-----------
        Color[] xAchseFarbeLok = new Color[anzGrfVisible], yAchseFarbeLok = new Color[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseFarbeLok.length; i1++) {
            xAchseFarbeLok[i1] = GraferV3.selectColor(xAchseFarbe[i1]);
        }
        for (int i1 = 0; i1 < yAchseFarbeLok.length; i1++) {
            yAchseFarbeLok[i1] = GraferV3.selectColor(yAchseFarbe[i1]);
        }
        this.setAxisColor(xAchseFarbeLok, yAchseFarbeLok);
        //-----------
        int[] xAchseStilLok = new int[anzGrfVisible], yAchseStilLok = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAchseStilLok.length; i1++) {
            xAchseStilLok[i1] = xAchseStil[i1];
        }
        for (int i1 = 0; i1 < yAchseStilLok.length; i1++) {
            yAchseStilLok[i1] = yAchseStil[i1];
        }
        this.setAxesLineStyle(xAchseStilLok, yAchseStilLok);
        //-----------
        //
        gridNormalX_associatedXAxis = new int[anzGrfVisible];  // Grid normal auf X-Achse
        gridNormalX_associatedYAxis = new int[anzGrfVisible];  // Grid normal auf X-Achse
        for (int i1 = 0; i1 < gridNormalX_associatedXAxis.length; i1++) {
            gridNormalX_associatedXAxis[i1] = i1;
        }
        for (int i1 = 0; i1 < gridNormalX_associatedYAxis.length; i1++) {
            gridNormalX_associatedYAxis[i1] = i1;
        }
        this.defineGridNormalX(gridNormalX_associatedXAxis, gridNormalX_associatedYAxis);
        //
        gridNormalY_associatedXAxis = new int[anzGrfVisible];  // Grid normal auf Y-Achse
        gridNormalY_associatedYAxis = new int[anzGrfVisible];  // Grid normal auf Y-Achse
        for (int i1 = 0; i1 < gridNormalY_associatedXAxis.length; i1++) {
            gridNormalY_associatedXAxis[i1] = i1;
        }
        for (int i1 = 0; i1 < gridNormalY_associatedYAxis.length; i1++) {
            gridNormalY_associatedYAxis[i1] = i1;
        }
        this.defineGridNormalY(gridNormalY_associatedXAxis, gridNormalY_associatedYAxis);
        //
        final Color[] farbeGridNormalXLok = new Color[anzGrfVisible];
        final Color[] farbeGridNormalXminorLok = new Color[farbeGridNormalXLok.length];
        for (int i1 = 0; i1 < farbeGridNormalXLok.length; i1++) {
            farbeGridNormalXLok[i1] = GraferV3.selectColor(colorGridNormalX[i1]);
            farbeGridNormalXminorLok[i1] = GraferV3.selectColor(colorGridNormalXminor[i1]);
        }
        final Color[] farbeGridNormalYLok = new Color[anzGrfVisible];
        final Color[] farbeGridNormalYminorLok = new Color[farbeGridNormalYLok.length];
        for (int i1 = 0; i1 < farbeGridNormalYLok.length; i1++) {
            farbeGridNormalYLok[i1] = GraferV3.selectColor(colorGridNormalY[i1]);
            farbeGridNormalYminorLok[i1] = GraferV3.selectColor(colorGridNormalYminor[i1]);
        }
        this.setGridColors(farbeGridNormalXLok, farbeGridNormalYLok, farbeGridNormalXminorLok, farbeGridNormalYminorLok);
        //-----------
        int[] linStilGridNormalXLok = new int[anzGrfVisible];
        int[] linStilGridNormalXminorLok = new int[farbeGridNormalXLok.length];
        for (int i1 = 0; i1 < linStilGridNormalXLok.length; i1++) {
            linStilGridNormalXLok[i1] = lineStyleGridNormalX[i1];
            linStilGridNormalXminorLok[i1] = lineStyleGridNormalXminor[i1];
        }
        final int[] linStilGridNormalYLok = new int[anzGrfVisible];
        final int[] linStilGridNormalYminorLok = new int[farbeGridNormalYLok.length];
        for (int i1 = 0; i1 < linStilGridNormalYLok.length; i1++) {
            linStilGridNormalYLok[i1] = lineStyleGridNormalY[i1];
            linStilGridNormalYminorLok[i1] = lineStyleGridNormalYminor[i1];
        }
        this.setGridLineStyle(linStilGridNormalXLok, linStilGridNormalYLok, linStilGridNormalXminorLok, linStilGridNormalYminorLok);
        //-----------
        final int[][] showGridNormalXmajLok = new int[anzGrfVisible][2], showGridNormalXminLok = new int[anzGrfVisible][2];

        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalX_associatedXAxis[i1];
            final int indexAchseY = gridNormalX_associatedYAxis[i1];
            showGridNormalXmajLok[i1][0] = (xShowGridMaj[i1]) ? indexAchseX : -1;  // // '-1' means: Do not draw grid line for this axis combination
            showGridNormalXmajLok[i1][1] = (xShowGridMaj[i1]) ? indexAchseY : -1;
            showGridNormalXminLok[i1][0] = (xShowGridMin[i1]) ? indexAchseX : -1;
            showGridNormalXminLok[i1][1] = (xShowGridMin[i1]) ? indexAchseY : -1;
        }
        int[][] yShowGridMajor = new int[anzGrfVisible][2], yShowGridMinor = new int[anzGrfVisible][2];

        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalY_associatedXAxis[i1];
            final int indexAchseY = gridNormalY_associatedYAxis[i1];
            yShowGridMajor[i1][0] = (yShowGridMaj[i1]) ? indexAchseX : -1;  // // '-1' means: Do not draw grid line for this axis combination
            yShowGridMajor[i1][1] = (yShowGridMaj[i1]) ? indexAchseY : -1;
            yShowGridMinor[i1][0] = (yShowGridMin[i1]) ? indexAchseX : -1;
            yShowGridMinor[i1][1] = (yShowGridMin[i1]) ? indexAchseY : -1;
        }
        this.showGridLines(showGridNormalXmajLok, showGridNormalXminLok, yShowGridMajor, yShowGridMinor);
        //-----------
        final boolean[] xTickAutoSpacing = new boolean[anzGrfVisible];
        final boolean[] yTickAutoSpacing = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < xTickAutoSpacing.length; i1++) {
            xTickAutoSpacing[i1] = this.xTickAutoSpacing[i1];
        }
        for (int i1 = 0; i1 < yTickAutoSpacing.length; i1++) {
            yTickAutoSpacing[i1] = this.yTickAutoSpacing[i1];
        }
        this.setTickAutoSpacing(xTickAutoSpacing, yTickAutoSpacing);
        //-----------
        double[] xTickSpacingLok = new double[anzGrfVisible];
        double[] yTickSpacingLok = new double[anzGrfVisible];
        for (int i1 = 0; i1 < xTickSpacingLok.length; i1++) {
            xTickSpacingLok[i1] = xTickSpacing[i1];
        }
        for (int i1 = 0; i1 < yTickSpacingLok.length; i1++) {
            yTickSpacingLok[i1] = yTickSpacing[i1];
        }
        this.setTickSpacing(xTickSpacingLok, yTickSpacingLok);
        //-----------
        int[] xAnzTicksMinorLok = new int[anzGrfVisible];
        int[] yAnzTicksMinorLok = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xAnzTicksMinorLok.length; i1++) {
            xAnzTicksMinorLok[i1] = xNumTicksMinor[i1];
        }
        for (int i1 = 0; i1 < yAnzTicksMinorLok.length; i1++) {
            yAnzTicksMinorLok[i1] = yNumTicksMinor[i1];
        }
        this.setTickCountMinor(xAnzTicksMinorLok, yAnzTicksMinorLok);
        //-----------
        final int[] xTickLength = new int[anzGrfVisible], xTickLengthMin = new int[anzGrfVisible];
        final int[] yTickLength = new int[anzGrfVisible], yTickLengthMin = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xTickLength.length; i1++) {
            xTickLength[i1] = xTickLength[i1];
            xTickLengthMin[i1] = xTickLengthMinor[i1];
        }
        for (int i1 = 0; i1 < yTickLength.length; i1++) {
            yTickLength[i1] = yTickLength[i1];
            yTickLengthMin[i1] = yTickLengthMinor[i1];
        }
        this.setTickLength(xTickLength, yTickLength, xTickLengthMin, yTickLengthMin);
        //-----------
        final boolean[] showLabelsXMaj = new boolean[anzGrfVisible], showLabelsXMin = new boolean[anzGrfVisible];
        final boolean[] showLabelsYMax = new boolean[anzGrfVisible], showLabelsYMin = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < showLabelsXMaj.length; i1++) {
            showLabelsXMaj[i1] = showLabelsXmaj[i1];
            showLabelsXMin[i1] = showLabelsXmin[i1];
        }
        for (int i1 = 0; i1 < showLabelsYMax.length; i1++) {
            showLabelsYMax[i1] = showLabelsYmaj[i1];
            showLabelsYMin[i1] = showLabelsYmin[i1];
        }
        this.setTickLabelVisible(showLabelsXMaj, showLabelsYMax, showLabelsXMin, showLabelsYMin);
        //-----------
        boolean[] showXTicksBottom = new boolean[anzGrfVisible], showYTicksLeft = new boolean[anzGrfVisible];
        for (int i1 = 0; i1 < showXTicksBottom.length; i1++) {
            showXTicksBottom[i1] = true;
        }
        for (int i1 = 0; i1 < showYTicksLeft.length; i1++) {
            showYTicksLeft[i1] = true;
        }
        this.setTickAlignment(showXTicksBottom, showYTicksLeft);
        //-----------
        int[] posXtickLabels = new int[anzGrfVisible], posYtickLabels = new int[anzGrfVisible];
        for (int i1 = 0; i1 < posXtickLabels.length; i1++) {
            posXtickLabels[i1] = 30;
        }
        for (int i1 = 0; i1 < posYtickLabels.length; i1++) {
            posYtickLabels[i1] = yTickLength[i1] + 4; //45;
        }
        this.setTickLabelPosition(posXtickLabels, posYtickLabels);
        //-----------
        Font[] foX = new Font[anzGrfVisible], foY = new Font[anzGrfVisible];
        for (int i1 = 0; i1 < foX.length; i1++) {
            foX[i1] = new Font("Arial", Font.PLAIN, 11);
        }
        for (int i1 = 0; i1 < foY.length; i1++) {
            foY[i1] = new Font("Arial", Font.PLAIN, 11);
        }
        this.setTickLabelFont(foX, foY);
        //-------------------------------------
    }

    public void aktualisiereAchsenNachResizing() {
        //-------------------------------------
        // // Area limits for mouse clicking --> is defined here for 2 diagrams
        final int breite = this.getWidth(), hoehe = this.getHeightForZVInPixels();
        xGrfMIN = new int[anzGrfVisible];
        xGrfMAX = new int[anzGrfVisible];
        for (int i1 = 0; i1 < xGrfMIN.length; i1++) {
            xGrfMIN[i1] = 0;
            xGrfMAX[i1] = this.getWidth();
        }
        yGrfMIN = new int[anzGrfVisible];
        yGrfMAX = new int[anzGrfVisible];
        double ySpGes = 0;   // // Weighting of the y-axes
        int iyy = 1;  // // Index for y-axis
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        yGrfMIN[0] = 0;
        yGrfMAX[0] = (diagramTyp[0] == DIAGRAM_TYP_ZV) ? 0 + (int) (hoehe * (ySpacingDiagram[0] / ySpGes)) : 0 + notwendigeHoehePixGRF[0];
        for (int i1 = 1; i1 < this.getAnzahlDiagramme(); i1++) {
            yGrfMIN[iyy] = yGrfMAX[iyy - 1];
            yGrfMAX[iyy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? yGrfMIN[iyy] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : yGrfMIN[iyy] + notwendigeHoehePixGRF[i1];
            iyy++;
        }
        //-------------------------------------
        int[] laenge_xAchse = new int[anzGrfVisible], posX_xAchse = new int[anzGrfVisible], posY_xAchse = new int[anzGrfVisible];
        int[] laenge_yAchse = new int[anzGrfVisible], posX_yAchse = new int[anzGrfVisible], posY_yAchse = new int[anzGrfVisible];
        int ix = 0, iy = 0;  // // Index for x and y axes
        ySpGes = 0;
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            if ((diagramTyp[i1] == DIAGRAM_TYP_ZV)) {
                ySpGes += ySpacingDiagram[i1];
            }
        }
        for (int i1 = 0; i1 < this.getAnzahlDiagramme(); i1++) {
            // X-Achse:
            laenge_xAchse[ix] = breite - (DX_IN_LINKS + DX_IN_RECHTS);
            posX_xAchse[ix] = DX_IN_LINKS;
            if (ix == 0) {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - DY_IN_UNTEN) : notwendigeHoehePixGRF[i1] - DY_IN_UNTEN;
            } else {
                posY_xAchse[ix] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? posY_xAchse[ix - 1] + (int) (hoehe * (ySpacingDiagram[i1] / ySpGes)) : posY_xAchse[ix - 1] + notwendigeHoehePixGRF[i1];
            }
            ix++;
            // Y-Achse:
            laenge_yAchse[iy] = (diagramTyp[i1] == DIAGRAM_TYP_ZV) ? (int) (hoehe * (ySpacingDiagram[i1] / ySpGes) - (DY_IN_OBEN + DY_IN_UNTEN)) : notwendigeHoehePixGRF[i1] - (DY_IN_OBEN + DY_IN_UNTEN);
            posX_yAchse[iy] = posX_xAchse[ix - 1];
            posY_yAchse[iy] = posY_xAchse[ix - 1];
            iy++;
        }
        this.setAxisWidthHeightX0Y0(laenge_xAchse, laenge_yAchse, posX_xAchse, posY_xAchse, posX_yAchse, posY_yAchse);
        //-------------
        try {
            this.possiblyHideGridLines();
        } catch (NullPointerException e) {
            Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "Nullpointer-Exception after resizing.");
        }
    }

    public void possiblyHideGridLines() {
        //-------------------------------------
        // // if the diagrams are drawn in a very small window, then the grid lines may need to be hidden,
        // // to maintain a certain level of clarity -->
        // 
        final double px1 = GRID_HIDE_THRESHOLD_Y, px2 = GRID_HIDE_THRESHOLD_X, pxr = GRID_HIDE_THRESHOLD_RATIO;
        final int[][] showGridXMax = new int[anzGrfVisible][2], showGridXMin = new int[anzGrfVisible][2];
        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalX_associatedXAxis[i1];
            final int indexAchseY = gridNormalX_associatedYAxis[i1];
            showGridXMax[i1][0] = (xShowGridMaj[i1]) ? indexAchseX : -1;  // // '-1' means: Do not draw grid line for this axis combination
            showGridXMax[i1][1] = (xShowGridMaj[i1]) ? indexAchseY : -1;
            showGridXMin[i1][0] = (xShowGridMin[i1]) ? indexAchseX : -1;
            showGridXMin[i1][1] = (xShowGridMin[i1]) ? indexAchseY : -1;
            zeichneDiagrammUmrandung[i1] = false;
            if (widthPix[indexAchseX] < px1 * pxr) {
                showGridXMin[i1][0] = -1;
                showGridXMin[i1][1] = -1;
            }
            if (widthPix[indexAchseX] < px2 * pxr) {
                showGridXMax[i1][0] = -1;
                showGridXMax[i1][1] = -1;
                zeichneDiagrammUmrandung[i1] = true;
            }
        }

        final int[][] showGridNormalYmajLok = new int[anzGrfVisible][2], showGridNormalYminLok = new int[anzGrfVisible][2];
        for (int i1 = 0; i1 < anzGrfVisible; i1++) {
            final int indexAchseX = gridNormalY_associatedXAxis[i1];
            final int indexAchseY = gridNormalY_associatedYAxis[i1];
            showGridNormalYmajLok[i1][0] = (yShowGridMaj[i1]) ? indexAchseX : -1;  // // '-1' means: Do not draw grid line for this axis combination
            showGridNormalYmajLok[i1][1] = (yShowGridMaj[i1]) ? indexAchseY : -1;
            showGridNormalYminLok[i1][0] = (yShowGridMin[i1]) ? indexAchseX : -1;
            showGridNormalYminLok[i1][1] = (yShowGridMin[i1]) ? indexAchseY : -1;
            if (heightPix[indexAchseY] < px1) {
                showGridNormalYminLok[i1][0] = -1;
                showGridNormalYminLok[i1][1] = -1;
            }
            if (heightPix[indexAchseY] < px2) {
                showGridNormalYmajLok[i1][0] = -1;
                showGridNormalYmajLok[i1][1] = -1;
                zeichneDiagrammUmrandung[i1] = true;
            }
        }
        this.showGridLines(showGridXMax, showGridXMin, showGridNormalYmajLok, showGridNormalYminLok);
        //-------------------------------------
    }

    public void setMouseMode(final int mausModus) {
        this.mausModusALT = this.mausModus;  // alten Zustand abspeichern
        this.mausModus = mausModus;  // // go to the new state
        //--------------------------
        switch (mausModus) {
            case MAUSMODUS_NIX:
                xSchieberAktiv = false;  // aktives Ausschalten des Schiebers
                this.repaint();
                break;
            case MAUSMODUS_ZOOM_AUTOFIT:
                this.mouseMode_ZOOM_AUTOFIT();  // xSchieber unveraendert 
                break;
            case MAUSMODUS_ZOOM_FENSTER:
                break;
            case MAUSMODUS_ZEICHNE_LINIE:
                break;
            case MAUSMODUS_WERTANZEIGE_SCHIEBER:
                if (!xSchieberAktiv) {
                    xSchieberAktiv = true;
                    xSchieberPix = _xAxisX[0];  // // x slider is placed at the beginning: same for all diagrams, defined in GraferV3
                    xSchieberPix2 = _xAxisX[0];
                    this.repaint();
                }
                break;
            default:
                Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "message");
        }
        //--------------------------
    }

    //=================================================
    //=================================================
    //=================================================
    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mouseMode_ZOOM_WINDOW(mouseX, mouseY, MOUSE_CLICKED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mouseX, mouseY, MOUSE_CLICKED, me);
        }
    }

    public void mousePressed(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mouseMode_ZOOM_WINDOW(mouseX, mouseY, MOUSE_PRESSED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mouseX, mouseY, MOUSE_PRESSED, me);
        }
    }

    public void mouseReleased(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }
        final int mouseX = me.getX(), mouseY = me.getY();
        if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mouseMode_ZOOM_WINDOW(mouseX, mouseY, MOUSE_RELEASED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mouseX, mouseY, MOUSE_RELEASED, me);
        }
    }

    public void mouseMoved(MouseEvent me) {
    }

    public void mouseDragged(MouseEvent me) {
        if (simulationLaeuftGerade) {
            return;
        }


        int mx = me.getX(), my = me.getY();
        if (mausModus == MAUSMODUS_NIX); else if (mausModus == MAUSMODUS_ZOOM_AUTOFIT); else if (mausModus == MAUSMODUS_ZOOM_FENSTER) {
            mouseMode_ZOOM_WINDOW(mx, my, MOUSE_DRAGGED, me.isControlDown(), me.isShiftDown());
        } else if (mausModus == MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            mouseMode_VALUE_DISPLAY_SLIDER(mx, my, MOUSE_DRAGGED, me);
        }
    }
    //=================================================

    public void mouseMode_ZOOM_AUTOFIT() {
        //--------------

        if (worksheetDatenTEMP != null) {
            for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                    worksheetData.setValue(worksheetDatenTEMP[i1][i2], i1, i2);
                }
            }
            worksheetDatenTEMP = null;
            _zvCounter = zvCounterTEMP;
            zvCounterTEMP = 0;
            nochNichtGeZoomt = true;
        }
        this.definiereAchsenbegrenzungenImAutoZoom(worksheetData);
        mausModus = mausModusALT;
        _scope.updateMouseMode(mausModus);
        //--------------
    }

    void undoZoom() {
        zoomRectangle(true);
    }

    public void mouseMode_ZOOM_WINDOW(int mx, int my, int mausAktion, boolean isControlDown, boolean isShiftDown) {



        if (mx < _xAxisX[0]) {
            mx = _xAxisX[0];
        }
        if (mx > _xAxisX[0] + widthPix[0]) {
            mx = _xAxisX[0] + widthPix[0];
        }
        switch (mausAktion) {
            //--------------------------
            case MOUSE_PRESSED:
                angeklicktZoom = true;
                x1 = mx;
                y1 = my;
                try {
                    indexAngeklickterGraph = 0;
                    while (!((xGrfMIN[indexAngeklickterGraph] <= mx) && (mx <= xGrfMAX[indexAngeklickterGraph])
                            && (yGrfMIN[indexAngeklickterGraph] <= my) && (my <= yGrfMAX[indexAngeklickterGraph]))) {
                        indexAngeklickterGraph++;
                    }
                } catch (Exception e) {
                    indexAngeklickterGraph = -1;
                }
                break;
            //--------------------------
            case MOUSE_RELEASED:
                angeklicktZoom = false;
                x2 = mx;
                y2 = my;
                if (Math.abs(x1 - x2) > 1 || Math.abs(y1 - y2) > 1) {
                    this.zoomRectangle(false);
                }
                indexAngeklickterGraph = -1;
                break;
            //--------------------------
            case MOUSE_DRAGGED:
                if (indexAngeklickterGraph == -1) {
                    return;  // kein Graph angeklickt
                }
                int x2old = x2;
                int y2old = y2;
                if (angeklicktZoom) {
                    if (mx < xGrfMIN[indexAngeklickterGraph]) {
                        mx = xGrfMIN[indexAngeklickterGraph];
                    }
                    if (mx > xGrfMAX[indexAngeklickterGraph]) {
                        mx = xGrfMAX[indexAngeklickterGraph];
                    }

                    if (!isControlDown) {
                        if (my < yGrfMIN[indexAngeklickterGraph]) {
                            my = yGrfMIN[indexAngeklickterGraph];
                        }
                        if (my > yGrfMAX[indexAngeklickterGraph]) {
                            my = yGrfMAX[indexAngeklickterGraph];
                        }
                        y2 = my;
                        controlZoomOn = false;
                    } else {
                        y2 = y1 + 1;
                        controlZoomOn = true;
                    }

                    if (isShiftDown) {
                        shiftZoomOn = true;
                        x2 = x1 + 1;
                    } else {
                        shiftZoomOn = false;
                        x2 = mx;
                    }



                    int drawStartx = Math.min(x1, Math.min(x2, x2old)) - 25;
                    int drawStarty = Math.min(y1, Math.min(y2, y2old)) - 25;
                    int drawWidthx = Math.abs(x1 - x2) + 100;
                    int drawWidthy = Math.abs(y1 - y2) + 100;
                    drawWidthx = Math.max(drawWidthx, Math.abs(x2old - x1) + 50);
                    drawWidthy = Math.max(drawWidthy, Math.abs(y2old - y1) + 50);
                    repaint(drawStartx, drawStarty, drawWidthx, drawWidthy);
                }
                break;
            //--------------------------
            default:
                break;
        }
    }



    public void mouseMode_VALUE_DISPLAY_SLIDER(int mx, int my, int mausAktion, MouseEvent me) {
        switch (mausAktion) {
            //--------------------------
            case MOUSE_PRESSED:
                break;
            //--------------------------
            case MOUSE_RELEASED:
                break;
            //--------------------------
            case MOUSE_DRAGGED:
                if ((mausModus != MAUSMODUS_ZOOM_FENSTER) && (xSchieberAktiv)) {
                    //-------------
                    try {
                        indexAngeklickterGraph = 0;
                        while (!((xGrfMIN[indexAngeklickterGraph] <= mx) && (mx <= xGrfMAX[indexAngeklickterGraph])
                                && (yGrfMIN[indexAngeklickterGraph] <= my) && (my <= yGrfMAX[indexAngeklickterGraph]))) {
                            indexAngeklickterGraph++;
                        }
                    } catch (Exception e) {
                        indexAngeklickterGraph = -1;
                    }
                    //-------------
                    if ((me.getModifiersEx() & java.awt.event.InputEvent.BUTTON1_DOWN_MASK) != 0 && !me.isControlDown()) {
                        inDiffMode = false;
                        xSchieberPix = mx;
                    } else {
                        inDiffMode = true;
                        xSchieberPix2 = mx;
                    }
                    calculateSliderValues();

                    repaint();

                }
                break;
            //--------------------------
            default:
                break;
        }
        //------------------------------

    }

    private void calculateSliderValues() {
        if (!inDiffMode) {
            if (xSchieberPix < _xAxisX[0]) {
                xSchieberPix = _xAxisX[0];
            }
            if (xSchieberPix > _xAxisX[0] + widthPix[0]) {
                xSchieberPix = _xAxisX[0] + widthPix[0];
            }
            try {
                xSchieberWert[0] = getValueFromPixel(xSchieberPix, 0)[0];
            } catch (Exception e) {
                Logger.getLogger(GraferImplementation.class.getName()).log(Level.WARNING, "Error computing slider x-value.", e);
            }  // // x value of the slider position
            int index = findSliderTimeIndex(xSchieberWert[0]);
            if (index >= 0) {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert[i2][0] = worksheetData.getValue(i2 + 1, index);
                }
            } else {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert[i2][0] = 0;
                }
            }
        } else {
            if (xSchieberPix2 < _xAxisX[0]) {
                xSchieberPix2 = _xAxisX[0];
            }
            if (xSchieberPix2 > _xAxisX[0] + widthPix[0]) {
                xSchieberPix2 = _xAxisX[0] + widthPix[0];
            }
            try {
                xSchieberWert2[0] = getValueFromPixel(xSchieberPix2, 0)[0];
            } catch (Exception e) {
            }  // // x value of the slider position
            int index = findSliderTimeIndex(xSchieberWert2[0]);
            if (index >= 0) {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert2[i2][0] = worksheetData.getValue(i2 + 1, index);
                }
            } else {
                for (int i2 = 0; i2 < ySchieberWert.length; i2++) {
                    ySchieberWert2[i2][0] = 0;
                }
            }
        }

    }

    private int findSliderTimeIndex(double sliderValue) {
        try {

            int i1 = 1;

            try {
                for (int startIndex = 1; startIndex < worksheetData.getColumnLength(); startIndex += 10) {
                    if (sliderValue >= worksheetData.getValue(0, startIndex - 1)) {
                        i1 = startIndex - 10;
                        break;
                    }
                }
                if (i1 < 1 || i1 > worksheetData.getColumnLength() - 1) {
                    i1 = 1;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (i1 = 1; i1 < worksheetData.getColumnLength(); i1++) {
                if (sliderValue >= worksheetData.getValue(0, i1 - 1)) {
                    if ((sliderValue <= worksheetData.getValue(0, i1))) {
                        return i1;
                    }

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return -1;
    }



    // // When the mouse is clicked into the pixel field -->
    private double[] getValueFromPixel(int xPix, int yPix) {
        //-------------------
        double achseXmin_ = -1, achseYmin_ = -1;
        int xAchseX_ = -1, yAchseY_ = -1;
        double sfX_ = -1, sfY_ = -1;
        int xAchseTyp_ = -1, yAchseTyp_ = -1;
        int indexYAchse = -1;
        for (int i1 = 0; i1 < indexCurveAssociatedXAxis.length; i1++) {
            if ((_xAxisX[indexCurveAssociatedXAxis[i1]] >= xGrfMIN[indexAngeklickterGraph])
                    && (_xAxisX[indexCurveAssociatedXAxis[i1]] <= xGrfMAX[indexAngeklickterGraph])) {
                achseXmin_ = axisXmin[indexCurveAssociatedXAxis[i1]];
                xAchseX_ = _xAxisX[indexCurveAssociatedXAxis[i1]];
                sfX_ = sfX[indexCurveAssociatedXAxis[i1]];
                xAchseTyp_ = xAxisType[indexCurveAssociatedXAxis[i1]];
                break;
            }
        }
        for (int i1 = 0; i1 < indexCurveAssociatedYAxis.length; i1++) {
            if ((_yAxisY[indexCurveAssociatedYAxis[i1]] >= yGrfMIN[indexAngeklickterGraph])
                    && (_yAxisY[indexCurveAssociatedYAxis[i1]] <= yGrfMAX[indexAngeklickterGraph])) {
                achseYmin_ = axisYmin[indexCurveAssociatedYAxis[i1]];
                yAchseY_ = _yAxisY[indexCurveAssociatedYAxis[i1]];
                sfY_ = sfY[indexCurveAssociatedYAxis[i1]];
                yAchseTyp_ = yAxisType[indexCurveAssociatedYAxis[i1]];
                indexYAchse = indexCurveAssociatedYAxis[i1];
                break;
            }
        }
        //-------------------
        double xWert = -1, yWert = -1;
        if (xAchseTyp_ == AXIS_LOGARITHMIC) {
            xWert = achseXmin_ * Math.pow(10.0, ((xPix - xAchseX_) / sfX_));
        } else if (xAchseTyp_ == AXIS_LINEAR) {
            xWert = achseXmin_ + (xPix - xAchseX_) / sfX_;
        }
        if (yAchseTyp_ == AXIS_LOGARITHMIC) {
            yWert = achseYmin_ * Math.pow(10.0, ((yAchseY_ - yPix) / sfY_));
        } else if (yAchseTyp_ == AXIS_LINEAR) {
            yWert = achseYmin_ + (yAchseY_ - yPix) / sfY_;
        }
        return new double[]{xWert, yWert, indexYAchse};
        //-------------------
    }

    // // Determine (x/y) value in pixels to a pair of values ​​-->
    // TODO: why is yWert not final?
    private int[] getPixelFromValue(final double xWert, double yWert, final int index_xAchse, final int index_yAchse) {
        try {
            //-------------------
            final double achseXminLok = axisXmin[index_xAchse];
            final int xAchseXLok = _xAxisX[index_xAchse];
            final double sfX_ = sfX[index_xAchse];
            final int xAchseTyp_ = xAxisType[index_xAchse];
            final double achseYmin_ = axisYmin[index_yAchse];
            final int yAchseY_ = _yAxisY[index_yAchse];
            final double sfY_ = sfY[index_yAchse];
            final int yAchseTyp_ = yAxisType[index_yAchse];
            //-------------------
            int xPix = -1, yPix = -1;
            if (xAchseTyp_ == AXIS_LOGARITHMIC) {
                xPix = (int) (sfX_ * Math.log10(xWert / achseXminLok) + xAchseXLok);

            } else if (xAchseTyp_ == AXIS_LINEAR) {
                xPix = (int) ((xWert - achseXminLok) * sfX_ + xAchseXLok);
            }
            if (yAchseTyp_ == AXIS_LOGARITHMIC) {
                yWert = achseYmin_ * Math.pow(10.0, ((yAchseY_ - yPix) / sfY_));
            } else if (yAchseTyp_ == AXIS_LINEAR) {
                yPix = (int) (yAchseY_ - (yWert - achseYmin_) * sfY_);
            }
            return new int[]{xPix, yPix};

        } catch (Exception ex) {
            ex.printStackTrace();
            return new int[]{-1, -1};
        }
        //-------------------
    }

    @Override
    protected void draw(final Graphics graphics) {

        //-------------------
        switch (mausModus) {
            case MAUSMODUS_NIX:
                break;
            case MAUSMODUS_ZOOM_AUTOFIT:
                break;
            case MAUSMODUS_ZOOM_FENSTER:
                graphics.setColor(GlobalColors.farbeZoomRechteck);
                final int dx = Math.abs(x1 - x2),
                 dy = Math.abs(y1 - y2);

                if ((x1 < x2) && (y1 < y2)) {
                    graphics.drawRect(x1, y1, dx, dy);
                } else if ((x1 < x2) && (y1 > y2)) {
                    graphics.drawRect(x1, y2, dx, dy);
                } else if ((x1 > x2) && (y1 < y2)) {
                    graphics.drawRect(x2, y1, dx, dy);
                } else if ((x1 > x2) && (y1 > y2)) {
                    graphics.drawRect(x2, y2, dx, dy);
                }
                break;
            case MAUSMODUS_ZEICHNE_LINIE:
                break;
            case MAUSMODUS_WERTANZEIGE_SCHIEBER:
                // // The slider should also be visible with some other mouse mode settings
                // // therefore: Display depends on 'xSchieberAktiv', see below -->
                break;
            default:
                Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "Default in case statement reached.");
                break;
        }
        //-------------------
        if (xSchieberAktiv) {

            final int dx = 7, dy = 1, dyFont = 9;
            graphics.setColor(Color.white);
            graphics.fillRect(dx, this.getHeight() - dy - dyFont, 80, dyFont);
            graphics.setColor(Color.red);

            // changed here: don't use the pixel value for the slider, but
            // the x-Value, and re-calculate the pixel from that value
            // otherwise, zooming or changing the window size makes problems/does
            // not update correctly.
            final int xSPix = getPixelFromValue(xSchieberWert[0], 0, 0, 0)[0];
            graphics.drawLine(xSPix, yGrfMIN[0], xSPix, yGrfMAX[anzGrfVisible - 1]);

            cf.setMaximumDigits(6);
            graphics.drawString("t = " + cf.formatT((float) xSchieberWert[0], TechFormat.FORMAT_AUTO), dx, this.getHeight() - dy);

            graphics.setColor(Color.green);
            final int xSPix2 = getPixelFromValue(xSchieberWert2[0], 0, 0, 0)[0];
            graphics.drawLine(xSPix2, yGrfMIN[0], xSPix2, yGrfMAX[anzGrfVisible - 1]);
            graphics.drawString("t = " + cf.formatT((float) xSchieberWert2[0], TechFormat.FORMAT_AUTO), dx + 130, this.getHeight() - dy);

            if (xSchieberWert2[0] >= 0) {
                graphics.setColor(Color.black);
                graphics.drawString("dt = " + cf.formatT((float) (xSchieberWert2[0] - xSchieberWert[0]), TechFormat.FORMAT_AUTO), dx + 260, this.getHeight() - dy);
            }

        }
        
        //-------------------
    }

    private void zoomRectangle(final boolean isUndoZoom) {
        //-------------------
        // // (1) a rectangle zoom is made for one of the diagrams:
        // // the x area also applies to all other diagrams, the y area of ​​the corresponding diagram corresponds to the zoom rectangle
        //
        int indexAxis = -1;
        if (isUndoZoom) {
            for (int i = 0; i < minX.length; i++) {
                minX[i] = minXOld[i];
                minY[i] = minYOld[i];
                maxX[i] = maxXOld[i];
                maxY[i] = maxYOld[i];

                minXOld[i] = minXOldOld[i];
                minYOld[i] = minYOldOld[i];
                maxXOld[i] = maxXOldOld[i];
                maxYOld[i] = maxYOldOld[i];
            }

            try {
                this.getChangedDataResolution(minX[0], maxX[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();                
            }
        } else {
            for (int i = 0; i < minX.length; i++) {

                minXOldOld[i] = minXOld[i];
                minYOldOld[i] = minYOld[i];
                maxXOldOld[i] = maxXOld[i];
                maxYOldOld[i] = maxYOld[i];

                minXOld[i] = minX[i];
                minYOld[i] = minY[i];
                maxXOld[i] = maxX[i];
                maxYOld[i] = maxY[i];
            }


            double[] x1y1 = this.getValueFromPixel(x1, y1);
            double[] x2y2 = this.getValueFromPixel(x2, y2);

            if (shiftZoomOn) {
                x1y1[0] = minX[0];
                x2y2[0] = maxX[0];
            }

            indexAxis = (int) x1y1[2];
            try {
                this.getChangedDataResolution(x1y1[0], x2y2[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();                
            }
            //
            double tMIN = -1, tMAX = -1, yMIN = -1, yMAX = -1;

            if (x1y1[0] < x2y2[0]) {
                tMIN = x1y1[0];
                tMAX = x2y2[0];
            } else {
                tMIN = x2y2[0];
                tMAX = x1y1[0];
            }
            if (x1y1[1] < x2y2[1]) {
                yMIN = x1y1[1];
                yMAX = x2y2[1];
            } else {
                yMIN = x2y2[1];
                yMAX = x1y1[1];
            }

            for (int i1 = 0; i1 < minX.length; i1++) {
                minX[i1] = tMIN;
                maxX[i1] = tMAX;
            }  // // new x-range limit for all diagrams

            if (!controlZoomOn) {
                minY[indexAxis] = yMIN;
                maxY[indexAxis] = yMAX;  // // the ZoomRectangle values ​​for Y only for the selected diagram
            }
            
            
        }

        //-------------------
        // // (2) for all other diagrams the y-range is fitted -->
        //
        final double[] value1 = new double[worksheetData.getRowLength()], value2 = new double[worksheetData.getRowLength()];
        for (int i1 = 0; i1 < worksheetData.getRowLength(); i1++) {
            value1[i1] = +1e99;
            value2[i1] = -1e99;
        }  // init
        for (int i1 = 0; i1 < worksheetData.getRowLength(); i1++) // // goes through the columns
        {
            for (int i2 = 0; i2 < _zvCounter + 1; i2++) {  // // goes through the selected column line by line
                if (i2 < worksheetData.getColumnLength()) {
                    if (worksheetData.getValue(i1, i2) < value1[i1]) {
                        value1[i1] = worksheetData.getValue(i1, i2);
                    }
                    if (worksheetData.getValue(i1, i2) > value2[i1]) {
                        value2[i1] = worksheetData.getValue(i1, i2);
                    }
                }
            }
        }
        for (int i1 = 0; i1 < minX.length; i1++) {
            if (i1 != indexAxis) {
                minY[i1] = +1e99;
                maxY[i1] = -1e99;
            }
        }
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {
            for (int i2 = 0; i2 < anzSignalePlusZeit; i2++) {
                if ((i1 != indexAxis) && (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y)) {
                    if (value1[i2] < minY[i1]) {
                        minY[i1] = value1[i2];
                    }
                    if (value2[i2] > maxY[i1]) {
                        maxY[i1] = value2[i2];
                    }
                }
            }
        }
        // 'schoenere' Bereichsgrenzen -->
        for (int i1 = 0; i1 < minY.length; i1++) {
            final double[] autoEmpf = autoAxisLimitRecommendation(minY[i1], maxY[i1]);
            minY[i1] = autoEmpf[0];
            maxY[i1] = autoEmpf[1];
            //tickAbstandY[i1]= autoEmpf[4];
        }
        //-------------------
        double[] xx1 = new double[anzGrfVisible], xx2 = new double[anzGrfVisible];  // X-Achse
        double[] yy1 = new double[anzGrfVisible], yy2 = new double[anzGrfVisible];  // // Y-axis --> Still needs to be customized!!
        boolean[] scX = new boolean[anzGrfVisible], scY = new boolean[anzGrfVisible];  // // is auto-scaling turned on?
        for (int i1 = 0; i1 < xx1.length; i1++) {
            xx1[i1] = minX[i1];
            xx2[i1] = maxX[i1];
            scX[i1] = autoScaleX[i1];
        }
        for (int i1 = 0; i1 < yy1.length; i1++) {
            yy1[i1] = minY[i1];
            yy2[i1] = maxY[i1];
            scY[i1] = autoScaleY[i1];
        }
        this.setAxesLimits(xx1, xx2, scX, yy1, yy2, scY);
        //-------------------
        double[] xTickSpacingLok = new double[anzGrfVisible];
        double[] yTickSpacingLok = new double[anzGrfVisible];
        for (int i1 = 0; i1 < xTickSpacingLok.length; i1++) {
            xTickSpacingLok[i1] = this.getAutoTickSpacingX(i1);
            xTickSpacing[i1] = xTickSpacingLok[i1];
        }
        for (int i1 = 0; i1 < yTickSpacingLok.length; i1++) {
            yTickSpacingLok[i1] = this.getAutoTickSpacingY(i1);
            yTickSpacing[i1] = yTickSpacingLok[i1];
        }
        this.setTickSpacing(xTickSpacingLok, yTickSpacingLok);
        //-------------------
        x1 = -1;
        x2 = -1;
        y1 = -1;
        y2 = -1;  // --> Ausblenden des Zoom-Rechtecks

        repaint();
        //-------------------
    }

    // // In the SCOPE representation, the data is reduced to a few INDEX_ENCODING_FACTOR (pixel) points in Hi-Lo representation in order to achieve the efficiency
    // // to significantly increase the graphical representation. For example, if you zoom in, the data must be included
    // // be reloaded with a significantly higher resolution and transferred in Hi-Lo display so that the graphical representation
    // // loses non-important information (e.g. 'frayed' ripple curves, disappeared peaks, rectangles become triangles, etc.)
    //
    private void getChangedDataResolution(double x1, double x2) {

        try {
            // // save worksheet[][] data supplied by the simulator as long as you don't continue simulating
            if (nochNichtGeZoomt) {
                nochNichtGeZoomt = false;
                worksheetDatenTEMP = new double[worksheetData.getRowLength()][worksheetData.getColumnLength()];
                for (int i1 = 0; i1 < worksheetDatenTEMP.length; i1++) {
                    for (int i2 = 0; i2 < worksheetDatenTEMP[0].length; i2++) {
                        worksheetDatenTEMP[i1][i2] = worksheetData.getValue(i1, i2);
                    }
                }
                zvCounterTEMP = _zvCounter;
            }

            // // correct order of x1 and x2 -->
            if (x1 > x2) {
                final double tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            // // x1 and x2 describe the area boundaries --> load RAM data -->
            final int lg1 = worksheetData.getRowLength(), lg2 = worksheetData.getColumnLength();
            final DataContainer wsRAM = _scope.getZVDataInRAM();  // hochaufloesende Daten im RAM
            int estimatedIndex = (int) (_zvCounter * 1.0 / lg2 * wsRAM.getColumnLength());
            //-------------
            // entsprechende Bereichsgrenzen in RAM-Daten finden -->
            final double xmin = wsRAM.getValue(0, 0);  // exakt
            if (estimatedIndex >= wsRAM.getColumnLength()) {
                estimatedIndex = wsRAM.getColumnLength() - 1;
            }
            double xmax = wsRAM.getValue(0, estimatedIndex);  // estimated

            // // if you simulate incompletely with 'Pause' and then zoom more than once:
            while (xmax == 0) {
                estimatedIndex = (int) (0.8 * estimatedIndex);
                xmax = wsRAM.getValue(0, estimatedIndex);
            }
            int zeigerX1_RAM = (int) ((x1 - xmin) / (xmax - xmin) * estimatedIndex);  // vorerst estimated
            if (zeigerX1_RAM < 0) {
                zeigerX1_RAM = 0;
            }
            int zeigerX2RAM = (int) ((x2 - xmin) / (xmax - xmin) * estimatedIndex);  // vorerst estimated
            //
            double x1RAM = wsRAM.getValue(0, zeigerX1_RAM);  // vorerst estimated
            try {
                if (x1RAM < x1) {
                    while ((x1RAM = wsRAM.getValue(0, zeigerX1_RAM)) < x1) {
                        zeigerX1_RAM++;
                    }
                } else {
                    while ((x1RAM = wsRAM.getValue(0, zeigerX1_RAM)) > x1) {
                        zeigerX1_RAM--;
                    }
                }
            } catch (Exception e) {
                return;
            }  // Zoom in einen Bereich ohne Daten


            double x2RAM = wsRAM.getValue(0, zeigerX2RAM);  // vorerst estimated


            while (x2RAM == 0) {
                zeigerX2RAM--;
                x2RAM = wsRAM.getValue(0, zeigerX2RAM);
            }

            if (x2RAM < x2) {
                for (; wsRAM.getValue(0, zeigerX2RAM) != 0 && x2RAM < x2; x2RAM = wsRAM.getValue(0, zeigerX2RAM)) {
                    zeigerX2RAM++;
                }
            } else {
                for (; x2RAM > x2; x2RAM = wsRAM.getValue(0, zeigerX2RAM)) {
                    zeigerX2RAM--;
                }
            }


            final int maximumIndex = wsRAM.getMaximumTimeIndex();
            zeigerX2RAM = Math.min(maximumIndex, zeigerX2RAM);
            x2RAM = wsRAM.getValue(0, zeigerX2RAM);

            zeigerX1_RAM -= 2;
            if (zeigerX1_RAM < 0) {
                zeigerX1_RAM = 0;
            }


            //-------------
            // // Reduce RAM data to Hi-Lo with SCOPE resolution -->
            //
            int zvC = 0;  // // local counter in the compressed SCOPE data
            final double dtSCOPE = (x2RAM - x1RAM) / INTERVALLE_ENTLANG_X;
            if (dtSCOPE > wsRAM.getTimeIntervalResolution()) {
                int lowerIndex = zeigerX1_RAM;
                int higherIndex = zeigerX2RAM;

                for (int worksheetIndex = 0; worksheetIndex < INTERVALLE_ENTLANG_X + 2; worksheetIndex++) {
                    final double timeValue = x1RAM + worksheetIndex * dtSCOPE;
                    worksheetData.setValue(timeValue, 0, 2 * worksheetIndex);
                    worksheetData.setValue(timeValue + dtSCOPE, 0, 2 * worksheetIndex + 1);

                    while (higherIndex < maximumIndex && wsRAM.getEstimatedTimeValue(lowerIndex) < timeValue) {
                        lowerIndex++;
                    }

                    higherIndex = lowerIndex;

                    while (higherIndex < maximumIndex && wsRAM.getEstimatedTimeValue(higherIndex) < timeValue + dtSCOPE) {
                        higherIndex++;
                    }

                    for (int i1 = 0; i1 < lg1 - 1; i1++) {
                        final HiLoData hiLo = wsRAM.getHiLoValue(i1 + 1, lowerIndex, higherIndex);
                        final double meanValue = 0.5 * (hiLo.yHi + hiLo.yLo);
                        double oldMeanValue = 0;
                        try {
                            oldMeanValue = 0.5 * (worksheetData.getValue(i1 + 1, 2 * worksheetIndex - 1) + worksheetData.getValue(i1 + 1, 2 * worksheetIndex - 2));
                        } catch (Exception ex) {
                            oldMeanValue = meanValue;
                        }

                        if (meanValue < oldMeanValue) {
                            worksheetData.setValue(hiLo.yHi, i1 + 1, 2 * worksheetIndex);
                            worksheetData.setValue(hiLo.yLo, i1 + 1, 2 * worksheetIndex + 1);
                        } else {
                            worksheetData.setValue(hiLo.yLo, i1 + 1, 2 * worksheetIndex);
                            worksheetData.setValue(hiLo.yHi, i1 + 1, 2 * worksheetIndex + 1);
                        }

                    }
                }
                _zvCounter = 2 * INTERVALLE_ENTLANG_X;

            } else { // single point can be resolved:
                for (int i2 = zeigerX1_RAM + 1; i2 < zeigerX2RAM + 1 && i2 < maximumIndex; i2++) {
                    final double time = wsRAM.getValue(0, i2);
                    if (zvC < worksheetData.getColumnLength()) {
                        worksheetData.setValue(time, 0, zvC);
                        for (int i1 = 0; i1 < lg1 - 1; i1++) {
                            final double value = wsRAM.getValue(i1 + 1, i2);
                            worksheetData.setValue(value, i1 + 1, zvC);
                        }
                    }
                    zvC++;
                    _zvCounter = zvC;
                }
            }
            //-------------
            // // Update ZV data in the worksheet display -->
            _scope.loadWorkSheet();

            //-------------
            //System.out.println("x1RAM= "+x1RAM+"\tx2RAM= "+x2RAM);
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    public void initClipping() {
        // // Can only be called if 'worksheet' and 'minX[],maxX[],...' are defined -->
        //----------------------------------------------
        for (int i1 = 0; i1 < matrixZuordnungKurveDiagram.length; i1++) {
            for (int i2 = 0; i2 < matrixZuordnungKurveDiagram[0].length; i2++) {
                if ((matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_X)
                        || (matrixZuordnungKurveDiagram[i1][i2] == ZUORDNUNG_Y)) {
                    crvClipValXmin[i1][i2] = this.getXClipAchse(i1, i2)[0];
                    crvClipValXmax[i1][i2] = this.getXClipAchse(i1, i2)[1];
                    crvClipValYmin[i1][i2] = this.getYClipAchse(i1, i2)[0];
                    crvClipValYmax[i1][i2] = this.getYClipAchse(i1, i2)[1];
                }
            }
        }
    }

    public double[] getXClipNo(final int im1, final int im2) {
        // // X-Clip --> Worksheet data of the assigned X-axis is searched
        // // CLIP_NO means: Worksheet data is limiting
        //------------------
        // // (1) Find the assigned X-axis:
        int indexX = -1;
        for (int i1 = 0; i1 < worksheetData.getRowLength(); i1++) {
            if (matrixZuordnungKurveDiagram[im1][i1] == ZUORDNUNG_X) {
                indexX = i1;
            }
        }
        if (indexX == -1) {
            Logger.getLogger(GraferImplementation.class.getName()).log(Level.SEVERE, "Index error in plot.");
        }
        // // (2) Find min and max values ​​in this column:
        double wsMIN = 1e99, wsMAX = -1e99;
        for (int i1 = 0; i1 < worksheetData.getColumnLength(); i1++) {
            if (worksheetData.getValue(indexX, i1) < wsMIN) {
                wsMIN = worksheetData.getValue(indexX, i1);
            }
            if (worksheetData.getValue(indexX, i1) > wsMAX) {
                wsMAX = worksheetData.getValue(indexX, i1);
            }
        }
        return new double[]{wsMIN, wsMAX};
    }

    public double[] getYClipNo(final int im1, final int im2) {
        // // Y-Clip --> Worksheet data is searched
        // // CLIP_NO means: Worksheet data is limiting
        double wsMIN = 1e99, wsMAX = -1e99;
        for (int i1 = 0; i1 < worksheetData.getColumnLength(); i1++) {
            if (worksheetData.getValue(im2, i1) < wsMIN) {
                wsMIN = worksheetData.getValue(im2, i1);
            }
            if (worksheetData.getValue(im2, i1) > wsMAX) {
                wsMAX = worksheetData.getValue(im2, i1);
            }
        }
        return new double[]{wsMIN, wsMAX};
    }

    public double[] getXClipAchse(final int im1, final int im2) {
        // // CLIP_ACHSE means: Axis is limiting
        return new double[]{minX[im1], maxX[im1]};
    }

    public double[] getYClipAchse(final int im1, final int im2) {
        // // CLIP_ACHSE means: Axis is limiting
        // // Attention: distinction between Y axis and Y2 axis -->
        if (matrixZuordnungKurveDiagram[im1][im2] == ZUORDNUNG_Y) {
            return new double[]{minY[im1], maxY[im1]};
        } else {
            // dh. dort gibt es keine Y-Achsen-Begrenzung
            return new double[]{-1, -1};
        }
    }

    
    public void initAutotickSpacing() {
        for (int i1 = 0; i1 < ANZ_DIAGRAM_MAX; i1++) {
            xTickSpacing[i1] = this.getAutoTickSpacingX(i1);
            yTickSpacing[i1] = this.getAutoTickSpacingY(i1);
        }
    }

    /**
     * 
     * @param im1
     * @return
     */
    public double getAutoTickSpacingX(final int im1) {
        return (maxX[im1] - minX[im1]) / ANZ_AUTO_TICKS;
    }

    public double getAutoTickSpacingY(final int im1) {
        return (maxY[im1] - minY[im1]) / ANZ_AUTO_TICKS;
    }
}
