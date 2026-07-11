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
package ch.technokrat.modelviewcontrol;

import java.io.Serializable;

/**
 * Concrete MVC model that stores a single generic value with undo/redo support.
 * Extends {@link AbstractUndoGenericModel} to provide automatic undo management.
 *
 * @param <T> the type of the stored value
 * @author andy
 */
public class ModelMVC<T> extends AbstractUndoGenericModel<T>
        implements Serializable{
  private static final long serialVersionUID = 784635241326447L;

  /**
   * Optional description object used by {@link #toString()} for display purposes,
   * e.g. "Undo dielectric constant".
   */
  private transient Object _descriptionObject = null;

  /**
   * Creates a model with the given initial value.
   * @param initValue the initial value
   */
  public ModelMVC(T initValue){
    super(initValue);
  }

  /**
   * Creates a model with the given initial value and description object.
   * @param initValue the initial value
   * @param descriptionObject an object whose {@code toString()} provides a
   *        human-readable description, used e.g. in undo messages
   */
  public ModelMVC(T initValue, Object descriptionObject){
    super(initValue);
    _descriptionObject = descriptionObject;
  }

  /**
   * Returns the description object's string representation, or a class-based
   * default if no description is set.
   * @return the description string
   */
  @Override
  public String toString(){
      if(_descriptionObject != null) {
          return _descriptionObject.toString();
      } else {
          return getClass().getName() + "_" + hashCode();
      }    
  }
    
}
