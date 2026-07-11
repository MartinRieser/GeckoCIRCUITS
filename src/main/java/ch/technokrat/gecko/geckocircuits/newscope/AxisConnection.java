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

/**
 * Defines axis assignments/mappings for scope signals.
 *
 * <p>German prefix {@code ZUORDNUNG} translates to "assignment" or "mapping".</p>
 */
public enum AxisConnection {
  /** Mapping of the signal to the primary X-axis. */
  ASSIGNMENT_X(51, "X"),
  /** Mapping of the signal to the primary Y-axis. */
  ASSIGNMENT_Y(52, "Y"),
  /** Mapping of the signal to the secondary Y-axis (Y2). */
  ASSIGNMENT_Y2(53, "Y2"),
  /** Generic mapping of the signal (unassigned axis). */
  ASSIGNMENT_SIGNAL(54, "sg"),
  /** No axis mapping (none/disabled). */
  ASSIGNMENT_NONE(55, "-");
  private String _displayString;
  private int _code;

  AxisConnection(final int code, final String displayString){
    _code = code;
    _displayString = displayString;
  }

  static AxisConnection getFromCode(final int code){
    for(AxisConnection val : AxisConnection.values()){
      if(val.getCode() == code){
        return val;
      }
    }
    return AxisConnection.ASSIGNMENT_NONE;
  }

  int getCode(){
    return _code;
  }

  @Override
  public String toString(){
    return _displayString;
  }

  public AxisConnection iterateNext(final boolean signal){

    if(signal){
      switch(this){
        case ASSIGNMENT_SIGNAL:
          return AxisConnection.ASSIGNMENT_NONE;
        case ASSIGNMENT_NONE:
          return AxisConnection.ASSIGNMENT_SIGNAL;
        default:
          assert false : this;
          break;
      }
    }


    switch(this){
      case ASSIGNMENT_NONE:
        return AxisConnection.ASSIGNMENT_Y;
      case ASSIGNMENT_Y:
        return AxisConnection.ASSIGNMENT_Y2;
      case ASSIGNMENT_Y2:
        return AxisConnection.ASSIGNMENT_NONE;
      default:
        assert false;
        break;

    }
    return null;
  }
};
