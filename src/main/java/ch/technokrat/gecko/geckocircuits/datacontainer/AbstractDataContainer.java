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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSeries;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import java.util.Observable;

/**
 * A powerful data storage object, keeps data information as e.g.
 * minumum-maximum values, ...
 * @author andy
 */
@SuppressWarnings("deprecation")
public abstract class AbstractDataContainer extends Observable{
  /**
   * @param row the row where to search inside
   * @param columnMax maximum column value
   * @param columnMin minimum column value      
   * @return the MinMax-Data from columnOld to column
   */
  public abstract HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax);

  /**
   * @param row the signal row index
   * @param column the sample index within the row
   * @return the value at the specified position
   */
  public abstract float getValue(final int row, final int column);

  /** @return the number of rows (signals) in this container */
  public abstract int getRowLength();

  /**
   * @param index the sample index
   * @param row the signal row index
   * @return the time value at the specified position
   */
  public abstract double getTimeValue(final int index, final int row);

  /**
   * @param row the signal row index
   * @return the maximum time index for the given row
   */
  public abstract int getMaximumTimeIndex(final int row);

  /**
   * @param intervalStart start of the time interval
   * @param intervalStop end of the time interval
   * @param columnIndex the signal column index
   * @return the data value(s) within the specified time interval
   */
  public abstract Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex);

  /**
   * @param row the signal row index
   * @return the absolute minimum/maximum hi-lo data for the entire row
   */
  public abstract HiLoData getAbsoluteMinMaxValue(int row);


  /**
   * @param time the time value to search for
   * @param row the signal row index
   * @return the index of the sample closest to the given time
   */
  public abstract int findTimeIndex(final double time, final int row);

  /**
   * @param row the signal row index
   * @return the display name of the signal at the given row
   */
  public abstract String getSignalName(final int row);

  /** @return the name of the x-axis data (e.g. "time") */
  public abstract String getXDataName();

  /** @return the current container status */
  public abstract ContainerStatus getContainerStatus();

  /**
   * @param containerStatus the new container status
   */
  public abstract void setContainerStatus(final ContainerStatus containerStatus);

  /**
   * @param row the signal row index
   * @return true if the row contains NaN or invalid numbers
   */
  public abstract boolean isInvalidNumbers(final int row);

  /**
   * @param row the signal row index
   * @return the time series object for the given row
   */
  public abstract AbstractTimeSeries getTimeSeries(final int row);

  /** @return a flat float array of all data values */
  public abstract float[] getDataArray();
  
  public String getSubcircuitSignalPath(final int row) {      
      return "";
  };

    void setSignalPathName(int containerRowIndex, String subcircuitPath) {        
    }
    
}
