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
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.newscope.Cispr16Fft;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Reconstructs and displays a time-domain curve from Fourier coefficients
 * (an, bn) by performing an inverse FFT and plotting it alongside the
 * original reference signal.
 */
@SuppressWarnings({"deprecation", "serial"})
public class FourierCurveReconstruction extends GraferV3 implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_WIDTH = 350;
    private static final int DEFAULT_HEIGHT = 300;
    private static final int AXIS_ORIGIN_X = 75;
    private static final int AXIS_ORIGIN_Y_OFFSET = 30;
    private static final double INIT_MIN = 1e99;
    private static final double INIT_MAX = -1e99;
    private static final double HALF_VALUE = 0.5;

    //----------------------------
    float[] xNew, yNew, yRef;
    private int nMin;
    private double f1;
    private double[] _an, _bn;
    private int widthPix, heightPix, X0xi, X0yi, Y0xi, Y0yi;  // Height, width, X and Y coordinates of the axis cross (all in pixels)
    //-----------------------
    private int mouseMode = GraferImplementation.MAUSMODUS_NIX;
    private int x1Zoom, y1Zoom, x2Zoom, y2Zoom;
    private boolean inDragMode = false;
    // Boundary limits of a diagram regarding mouse click:
    private int[] xGraphMin, xGraphMax, yGraphMin, yGraphMax;
    private int clickedGraphIndex = 0;
    //-----------------------
    private boolean xSliderActive = false;
    private int xSliderPixels;
    private double[] xSliderValue = new double[]{-1, -1};  // // a single pixel point may have multiple values ​​assigned to it
    private double[] yRefValue = new double[]{-1, -1}, yNewValue = new double[]{-1, -1};
    private TechFormat cf = new TechFormat();
    //-----------------------

    /**
     * Constructs the Fourier reconstruction plot from coefficients.
     * @param an cosine coefficients
     * @param bn sine coefficients
     * @param nMin minimum harmonic index
     * @param f1 fundamental frequency
     * @param worksheet the data container for the reference signal
     * @param dataIndex index of the signal in the worksheet
     * @param rng1 start time of the analysis window
     * @param rng2 end time of the analysis window
     */
    @SuppressWarnings("this-escape")
    public FourierCurveReconstruction(
            double[] an, double[] bn, int nMin, double f1, AbstractDataContainer worksheet, int dataIndex, double rng1, double rng2) {
        //---------------------------------------
        widthPix = DEFAULT_WIDTH;
        heightPix = DEFAULT_HEIGHT;
        X0xi = AXIS_ORIGIN_X;
        X0yi = heightPix + AXIS_ORIGIN_Y_OFFSET;
        Y0xi = X0xi;
        Y0yi = X0yi;
        this.setPreferredSize(new Dimension(widthPix + 2 * X0xi, X0yi + (X0yi - heightPix)));  // // for pack() in the parent JFrame
        // Boundary limits for mouse clicking:
        xGraphMin = new int[]{0};
        xGraphMax = new int[]{this.getWidth()};
        yGraphMin = new int[]{0};
        yGraphMax = new int[]{this.getHeight()};
        //---------------------------------------
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.nMin = nMin;
        _an = an;
        _bn = bn;
        this.f1 = f1;
        //=======================================
        // // 'Process' curve --> Convert the Fourier data back into an analog curve
        //
        int lg = 0;


        int startIndex = 0;
        int stopIndex = 0;

        int i = 0;
        while (i < worksheet.getMaximumTimeIndex(0)) {
            double timeValue = worksheet.getTimeValue(i, 0);
            if (timeValue < rng1) {
                startIndex++;
            }

            if (timeValue < rng2) {
                stopIndex++;
            }
            i++;
        }

        int NN = 1;

        while (NN < stopIndex - startIndex) {
            NN *= 2;
        }

        if (NN > stopIndex - startIndex) {
            NN /= 2;
        }
        // ------------------------------------

        xNew = new float[NN];
        yNew = new float[NN];
        yRef = new float[NN];
        double timeSpan = rng2 - rng1;
        int j = startIndex;
        for (i = 0; i < NN; i++) {
            while (worksheet.getTimeValue(j, 0) < rng1 + i * timeSpan / NN) {
                j++;
            }
            xNew[i] = (float) worksheet.getTimeValue(j, 0);
            yRef[i] = worksheet.getValue(dataIndex-1, j);
        }        
        
        for(i = 0; i < an.length; i++) {
            yNew[2* i] = (float) an[i];
            yNew[2*i+1] = (float) bn[i];
        }
        
        Cispr16Fft.realft(yNew, -1);
        
        //=======================================
        DataContainer daten = new DataContainerSimple(3, xNew.length);

        for (int i1 = 0; i1 < xNew.length; i1++) {
            daten.setValue(xNew[i1], 0, i1);
            daten.setValue(yNew[i1], 1, i1);
            daten.setValue(yRef[i1], 2, i1);
        }
        worksheetData = daten;
        this.setCurveTransparency(new double[]{HALF_VALUE, HALF_VALUE});
        //-----------------------
        this.setAxes();
        this.setCurves();
        
    }

    /**
     * Rescales the chart dimensions when the window is resized.
     */
    // // Rescale the chart when the window dimensions are changed -->
    public void resize() {
        //---------------------------------------
        widthPix = this.getWidth() - 2 * X0xi;
        heightPix = this.getHeight() - (2 * 35);
        X0xi = 75;
        X0yi = heightPix + 30;
        Y0xi = X0xi;
        Y0yi = X0yi;
        this.setAxisWidthHeightX0Y0(new int[]{widthPix}, new int[]{heightPix}, new int[]{X0xi}, new int[]{X0yi}, new int[]{Y0xi}, new int[]{Y0yi});
        //---------------------------------------
        // // Area limits for mouse clicking --> is defined here for 2 diagrams
        xGraphMin = new int[]{0};
        xGraphMax = new int[]{this.getWidth()};
        yGraphMin = new int[]{0};
        yGraphMax = new int[]{this.getHeight()};
        //---------------------------------------
    }

    /**
     * Sets the mouse interaction mode (none, zoom, auto-fit, slider).
     * @param mouseMode the mode constant from GraferImplementation
     */
    public void setMouseMode(int mouseMode) {
        this.mouseMode = mouseMode;
        //---------
        if (mouseMode == GraferImplementation.MAUSMODUS_NIX) {
            xSliderActive = false;
            repaint();
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
            //------------------------------------
            double ymin = INIT_MIN, ymax = INIT_MAX;
            for (int i1 = 0; i1 < yNew.length; i1++) {
                if (yNew[i1] > ymax) {
                    ymax = yNew[i1];
                }
                if (yNew[i1] < ymin) {
                    ymin = yNew[i1];
                }
                if (yRef[i1] > ymax) {
                    ymax = yRef[i1];
                }
                if (yRef[i1] < ymin) {
                    ymin = yRef[i1];
                }
            }
            double[] empf = autoAxisLimitRecommendation(ymin, ymax);
            this.setAxesLimits(new double[]{xNew[0]}, new double[]{xNew[xNew.length - 1]}, new boolean[]{true}, new double[]{empf[0]}, new double[]{empf[1]}, new boolean[]{true});
            this.setTickSpacing(new double[]{(0.2 / f1)}, new double[]{empf[4]});
            repaint();
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
        } else if (mouseMode == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) {
            //------------------------------------
            xSliderActive = true;
            xSliderPixels = X0xi;  // // x slider is placed at the beginning
        }
        //---------
    }

    /**
     * Draws the zoom rectangle and value slider overlay on the chart.
     * @param g the graphics context
     */
    // // will be overwritten in order to be able to add text -->
    protected void draw(Graphics g) {
        if ((mouseMode == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) && (inDragMode)) {
            g.setColor(GlobalColors.farbeZoomRechteck);
            int b = Math.abs(x2Zoom - x1Zoom), h = Math.abs(y2Zoom - y1Zoom);
            if ((x1Zoom > x2Zoom) && (y1Zoom > y2Zoom)) {
                g.drawRect(x2Zoom, y2Zoom, b, h);
            } else if ((x1Zoom > x2Zoom) && (y2Zoom > y1Zoom)) {
                g.drawRect(x2Zoom, y1Zoom, b, h);
            } else if ((x1Zoom < x2Zoom) && (y1Zoom > y2Zoom)) {
                g.drawRect(x1Zoom, y2Zoom, b, h);
            } else if ((x1Zoom < x2Zoom) && (y2Zoom > y1Zoom)) {
                g.drawRect(x1Zoom, y1Zoom, b, h);
            }
        }
        if ((mouseMode == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) || (xSliderActive)) {
            g.setColor(Color.red);
            g.drawLine(xSliderPixels, X0yi, xSliderPixels, X0yi - heightPix);
            int x0 = X0xi + widthPix - 15, y0 = X0yi - heightPix + 12, dy = 15;
            g.setColor(Color.white);
            g.fillRect(x0, y0 - 12, 25, 12 + 2 * dy);
            g.setColor(Color.black);
            g.drawString("x = " + cf.formatT(xSliderValue[0], TechFormat.FORMAT_AUTO), x0, y0);
            g.setColor(Color.darkGray);
            g.drawString("y = " + cf.formatT(yRefValue[0], TechFormat.FORMAT_AUTO), x0, y0 + dy);
            g.setColor(Color.blue);
            g.drawString("y = " + cf.formatT(yNewValue[0], TechFormat.FORMAT_AUTO), x0, y0 + 2 * dy);
        }
    }

    /**
     * Configures the chart axes with appropriate bounds and tick spacing.
     */
    public void setAxes() {
        //-------------------------------------
        this.setAxesCount(1, 1);
        this.setAxisWidthHeightX0Y0(new int[]{widthPix}, new int[]{heightPix}, new int[]{X0xi}, new int[]{X0yi}, new int[]{Y0xi}, new int[]{Y0yi});
        this.setAxisColor(new Color[]{Color.black}, new Color[]{Color.black});
        this.setAxesType(new int[]{AXIS_LINEAR}, new int[]{AXIS_LINEAR});
        this.setAxesLineStyle(new int[]{SOLID_PLAIN}, new int[]{SOLID_PLAIN});
        this.setAxesLabels(new String[]{""}, new String[]{""});  // Needed to avoid a NullPointerException
        this.defineGridNormalX(new int[]{0}, new int[]{0});
        this.defineGridNormalY(new int[]{0}, new int[]{0});
        this.setGridLineStyle(new int[]{DOTTED_PLAIN}, new int[]{DOTTED_PLAIN}, new int[]{INVISIBLE}, new int[]{INVISIBLE});
        this.showGridLines(new int[][]{{0, 0}}, new int[][]{{0, 0}}, new int[][]{{0, 0}}, new int[][]{{0, 0}});
        this.setGridColors(new Color[]{Color.lightGray}, new Color[]{Color.lightGray}, new Color[]{Color.lightGray}, new Color[]{Color.lightGray});
        this.setTickCountMinor(new int[]{2}, new int[]{2});
        this.setTickLength(new int[]{4}, new int[]{4}, new int[]{0}, new int[]{0});
        this.setTickAlignment(new boolean[]{true}, new boolean[]{true});
        this.setTickLabelVisible(new boolean[]{true}, new boolean[]{true}, new boolean[]{false}, new boolean[]{false});
        this.setTickLabelPosition(new int[]{20}, new int[]{16});
        this.setTickLabelFont(new Font[]{new Font("Arial", Font.PLAIN, 12)}, new Font[]{new Font("Arial", Font.PLAIN, 12)});
        //=========================================
        double ymin = INIT_MIN, ymax = INIT_MAX;
        for (int i1 = 0; i1 < yNew.length; i1++) {
            if (yNew[i1] > ymax) {
                ymax = yNew[i1];
            }
            if (yNew[i1] < ymin) {
                ymin = yNew[i1];
            }
            if (yRef[i1] > ymax) {
                ymax = yRef[i1];
            }
            if (yRef[i1] < ymin) {
                ymin = yRef[i1];
            }
        }
        double[] empf = autoAxisLimitRecommendation(ymin, ymax);
        while (empf[4] > HALF_VALUE * (ymax - ymin)) {
            empf[4] *= HALF_VALUE;
        }
        //
        this.setAxesLimits(new double[]{xNew[0]}, new double[]{xNew[xNew.length - 1]}, new boolean[]{true}, new double[]{empf[0]}, new double[]{empf[1]}, new boolean[]{true});
        this.setTickSpacing(new double[]{(0.2 / (f1))}, new double[]{empf[4]});
        //-------------------------------------
    }

    /**
     * Configures the curve styles, colors, and data source indices.
     */
    protected void setCurves() {
        //=========================================
        // // to set based on the worksheet data -->
        //-------------------------------------
        this.setCurvesCount(2);
        this.setCurveAxesAssignment(new int[]{0, 0}, new int[]{0, 0});
        this.setCurveIndexWorksheetColumnsXY(new int[][]{{0, 1}, {0, 2}});
        this.setCurvePointSymbolVisible(
                new boolean[]{false, false}, new int[]{20, 20}, new int[]{SYBM_CIRCLE, SYBM_RECT_FILLED}, new Color[]{Color.black, Color.gray});
        this.setCurveClipping(
                new double[]{xNew[0], xNew[0]}, new double[]{xNew[xNew.length - 1], xNew[xNew.length - 1]}, new double[]{0, 0}, new double[]{1, 1},
                new int[]{CLIP_NO, CLIP_NO}, new int[]{CLIP_NO, CLIP_NO}, new int[]{CLIP_NO, CLIP_NO}, new int[]{CLIP_NO, CLIP_NO});
        this.setCurveLineStyle(new int[]{SOLID_PLAIN, SOLID_PLAIN});
        this.setCurveColor(new Color[]{Color.blue, Color.darkGray});
        //-------------------------------------
    }

    //================================================
    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mousePressed(MouseEvent me) {
        //-------------------
        if (mouseMode == GraferImplementation.MAUSMODUS_NIX) {
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
            x1Zoom = me.getX();
            y1Zoom = me.getY();
            inDragMode = true;
        } else if (mouseMode == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) {
        }
        //-------------------
    }

    public void mouseReleased(MouseEvent me) {
        //-------------------
        if (mouseMode == GraferImplementation.MAUSMODUS_NIX) {
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
            //--------------------------------------
            inDragMode = false;
            x2Zoom = me.getX();
            y2Zoom = me.getY();
            // // Converting the zoom coordinates of pixel points into values ​​of the zoom-defining rectangle -->
            double[] xy1 = getValueFromPixel(x1Zoom, y1Zoom);
            double[] xy2 = getValueFromPixel(x2Zoom, y2Zoom);
            if (xy1[0] > xy2[0]) {  // flip x-values
                double q = xy1[0];
                xy1[0] = xy2[0];
                xy2[0] = q;
            }
            if (xy1[1] > xy2[1]) {  // flip y-values
                double q = xy1[1];
                xy1[1] = xy2[1];
                xy2[1] = q;
            }
            double[] empfX = new double[]{xy1[0], xy2[0], -1, -1, autoAxisLimitRecommendation(xy1[0], xy2[0])[4]};
            while (empfX[4] > HALF_VALUE * (xy2[0] - xy1[0])) {
                empfX[4] *= HALF_VALUE;
            }
            double[] empfY = new double[]{xy1[1], xy2[1], -1, -1, autoAxisLimitRecommendation(xy1[1], xy2[1])[4]};
            while (empfY[4] > HALF_VALUE * (xy2[1] - xy1[1])) {
                empfY[4] *= HALF_VALUE;
            }
            //
            // Reset the axes accordingly
            this.setAxesLimits(
                    new double[]{empfX[0]}, new double[]{empfX[1]}, new boolean[]{true},
                    new double[]{empfY[0]}, new double[]{empfY[1]}, new boolean[]{true});
            this.setTickSpacing(new double[]{empfX[4]}, new double[]{empfY[4]});
            repaint();
        } else if (mouseMode == GraferImplementation.MAUSMODUS_WERTANZEIGE_SCHIEBER) {
        }
    }

    @Override
    public void mouseClicked(final MouseEvent mouseEvent) {
    }
    

    public void mouseMoved(final MouseEvent mouseEvent) {
    }

    public void mouseDragged(MouseEvent me) {
        if (mouseMode == GraferImplementation.MAUSMODUS_NIX) {
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_AUTOFIT) {
        } else if (mouseMode == GraferImplementation.MAUSMODUS_ZOOM_FENSTER) {
            if (!inDragMode) {
                return;
            }
            x2Zoom = me.getX();
            y2Zoom = me.getY();
            repaint();
        } else if (xSliderActive) {
            xSliderPixels = me.getX();
            if (xSliderPixels < X0xi) {
                xSliderPixels = X0xi;
            }
            if (xSliderPixels > X0xi + widthPix) {
                xSliderPixels = X0xi + widthPix;
            }
            xSliderValue[0] = getValueFromPixel(xSliderPixels, 0)[0]; 
            // // x value of the slider position
            for (int i1 = 1; i1 < xNew.length; i1++) {
                if ((xNew[i1 - 1] <= xSliderValue[0]) && (xSliderValue[0] <= xNew[i1])) {
                    yNewValue[0] = yNew[i1];
                    yRefValue[0] = yRef[i1];
                    break;
                }
            }
            repaint();
        }
        //-------------------
    }
    //================================================

    // // When the mouse is clicked into the pixel field
    private double[] getValueFromPixel(int xPix, int yPix) {
        //-------------------
        double achseXmin_ = -1, achseYmin_ = -1;
        int xAchseX_ = -1, yAchseY_ = -1;
        double sfX_ = -1, sfY_ = -1;
        int xAchseTyp_ = -1, yAchseTyp_ = -1;
        int indexDiagrammYachse = -1;
        for (int i1 = 0; i1 < indexCurveAssociatedXAxis.length; i1++) {
            if ((_xAxisX[indexCurveAssociatedXAxis[i1]] >= xGraphMin[clickedGraphIndex])
                    && (_xAxisX[indexCurveAssociatedXAxis[i1]] <= xGraphMax[clickedGraphIndex])) {
                achseXmin_ = axisXmin[indexCurveAssociatedXAxis[i1]];
                xAchseX_ = _xAxisX[indexCurveAssociatedXAxis[i1]];
                sfX_ = sfX[indexCurveAssociatedXAxis[i1]];
                xAchseTyp_ = xAxisType[indexCurveAssociatedXAxis[i1]];
                break;
            }
        }
        for (int i1 = 0; i1 < indexCurveAssociatedYAxis.length; i1++) {
            if ((_yAxisY[indexCurveAssociatedYAxis[i1]] >= yGraphMin[clickedGraphIndex])
                    && (_yAxisY[indexCurveAssociatedYAxis[i1]] <= yGraphMax[clickedGraphIndex])) {
                achseYmin_ = axisYmin[indexCurveAssociatedYAxis[i1]];
                yAchseY_ = _yAxisY[indexCurveAssociatedYAxis[i1]];
                sfY_ = sfY[indexCurveAssociatedYAxis[i1]];
                yAchseTyp_ = yAxisType[indexCurveAssociatedYAxis[i1]];
                indexDiagrammYachse = indexCurveAssociatedYAxis[i1];
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
        return new double[]{xWert, yWert, indexDiagrammYachse};
        //-------------------
    }

    // // Determine (x/y) value in pixels to a pair of values
    private int[] getPixelFromValue(double xWert, double yWert, int index_xAchse, int index_yAchse) {
        //-------------------
        double achseXmin_ = axisXmin[index_xAchse];
        int xAchseX_ = _xAxisX[index_xAchse];
        double sfX_ = sfX[index_xAchse];
        int xAchseTyp_ = xAxisType[index_xAchse];
        double achseYmin_ = axisYmin[index_yAchse];
        int yAchseY_ = _yAxisY[index_yAchse];
        double sfY_ = sfY[index_yAchse];
        int yAchseTyp_ = yAxisType[index_yAchse];
        //-------------------
        int xPix = -1, yPix = -1;
        if (xAchseTyp_ == AXIS_LOGARITHMIC) {
            xPix = xAchseX_ + (int) (sfX_ * GraferV3.lg10(xWert / achseXmin_));
        } else if (xAchseTyp_ == AXIS_LINEAR) {
            xPix = xAchseX_ + (int) (sfX_ * (xWert - achseXmin_));
        }
        if (yAchseTyp_ == AXIS_LOGARITHMIC) {
            yPix = yAchseY_ - (int) (sfY_ * GraferV3.lg10(yWert / achseYmin_));
        } else if (yAchseTyp_ == AXIS_LINEAR) {
            yPix = yAchseY_ - (int) (sfY_ * (yWert - achseYmin_));
        }
        return new int[]{xPix, yPix};
        //-------------------
    }
}
