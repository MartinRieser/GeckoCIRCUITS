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
import java.util.*;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * A ListModel implementation that uses weak references for its listeners,
 * preventing memory leaks when listeners are not explicitly removed.
 * <p>
 * The {@code _delegate} field uses raw ArrayList to suppress PMD warnings
 * about missing generics; this is an intentional design choice to maintain
 * compatibility with Swing's type-erased listener APIs.</p>
 */
public final class WeakListModel implements ListModel<Object>, Serializable{
  public static final long serialVersionUID = 582811111394392L;

  /** Weak-reference map of registered ListDataListeners. Entries are automatically
   *  garbage-collected when the listener is no longer strongly referenced elsewhere. */
  private transient final Map<ListDataListener, Object> _listenerList =
          Collections.synchronizedMap(new WeakHashMap<ListDataListener, Object>());

  /** Sentinel value used as a placeholder in the weak-reference map. */
  private transient final Object _present = new Object();

  /** The underlying data store. {@code @SuppressWarnings("PMD")} suppresses
   *  a PMD warning about raw generic types, which is acceptable here. */
  @SuppressWarnings("PMD")
  private final ArrayList<Object> _delegate = new ArrayList<Object>();

  @Override
  public int getSize(){
    return _delegate.size();
  }

  @Override
  public Object getElementAt(final int index){
    return _delegate.get(index);
  }

  /**
   * Trims the capacity of the underlying list to its current size.
   */
  public void trimToSize(){
    _delegate.trimToSize();
  }

  /**
   * Ensures the underlying list can hold at least the given number of elements.
   * @param minCapacity the desired minimum capacity
   */
  public void ensureCapacity(final int minCapacity){
    _delegate.ensureCapacity(minCapacity);
  }

  /**
   * @return the number of elements in this model
   */
  public int size(){
    return _delegate.size();
  }

  /**
   * @return true if this model contains no elements
   */
  public boolean isEmpty(){
    return _delegate.isEmpty();
  }

  /**
   * @return an enumeration of all elements in this model
   */
  public Enumeration<Object> elements(){
    return Collections.enumeration(_delegate);
  }

  /**
   * @param elem the element to search for
   * @return true if this model contains the specified element
   */
  public boolean contains(final Object elem){
    return _delegate.contains(elem);
  }

  /**
   * @param elem the element to search for
   * @return the index of the first occurrence, or -1 if not found
   */
  public int indexOf(final Object elem){
    return _delegate.indexOf(elem);
  }

  /**
   * @param elem the element to search for
   * @return the index of the last occurrence, or -1 if not found
   */
  public int lastIndexOf(final Object elem){
    return _delegate.lastIndexOf(elem);
  }

  /**
   * @param index the position to retrieve
   * @return the element at the specified position
   */
  public Object elementAt(final int index){
    return _delegate.get(index);
  }

  /**
   * @return the first element in this model
   */
  public Object firstElement(){
    return _delegate.get(0);
  }

  /**
   * @return the last element in this model
   */
  public Object lastElement(){
    return _delegate.get(_delegate.size() - 1);
  }

  @Override
  public String toString(){
    return _delegate.toString();
  }

  /**
   * Replaces the element at the specified position and fires a
   * {@code contentsChanged} event.
   * @param obj the new element
   * @param index the position to set
   */
  public void setElementAt(final Object obj, final int index){
    _delegate.set(index, obj);
    fireContentsChanged(this, index, index);
  }

  /**
   * Removes the element at the specified position and fires an
   * {@code intervalRemoved} event.
   * @param index the position to remove
   */
  public void removeElementAt(final int index){
    _delegate.remove(index);
    fireIntervalRemoved(this, index, index);
  }

  /**
   * Inserts an element at the specified position and fires an
   * {@code intervalAdded} event.
   * @param obj the element to insert
   * @param index the position to insert at
   */
  public void insertElementAt(final Object obj, final int index){
    _delegate.add(index, obj);
    fireIntervalAdded(this, index, index);
  }

  /**
   * Appends an element to the end of the list and fires an
   * {@code intervalAdded} event.
   * @param obj the element to add
   */
  public void addElement(final Object obj){
    final int index = _delegate.size();
    _delegate.add(obj);
    fireIntervalAdded(this, index, index);
  }

  /**
   * Removes the first occurrence of the specified element and fires an
   * {@code intervalRemoved} event if removal succeeded.
   * @param obj the element to remove
   * @return true if the element was removed
   */
  public boolean removeElement(final Object obj){
    final int index = indexOf(obj);
    final boolean couldRemove = _delegate.remove(obj);
    if(index >= 0){
      fireIntervalRemoved(this, index, index);
    }
    return couldRemove;
  }

  /**
   * Removes all elements from the list and fires an {@code intervalRemoved}
   * event covering the entire range that existed before clearing.
   */
  public void removeAllElements(){
    final int index1 = _delegate.size() - 1;
    _delegate.clear();
    if(index1 >= 0){
      fireIntervalRemoved(this, 0, index1);
    }
  }

  @Override
  public void addListDataListener(final ListDataListener dataListener){
    synchronized(this){
      _listenerList.put(dataListener, _present);
    }
  }

  @Override
  public void removeListDataListener(final ListDataListener dataListener){
    synchronized(this){
      _listenerList.remove(dataListener);
    }
  }

  /**
   * @param listenerType the type of listeners to return
   * @return an array of all registered listeners of the given type
   */
  public EventListener[] getListeners(final Class<?> listenerType){
    final Set<ListDataListener> set = _listenerList.keySet();
    return set.toArray(new EventListener[set.size()]);
  }

  /**
   * Notifies all registered listeners that the contents of the list have
   * changed in the specified range. The event object is lazily created
   * and reused for all listeners to reduce allocation overhead.
   * @param source the source of the event
   * @param index0 the start of the changed range
   * @param index1 the end of the changed range
   */
  protected void fireContentsChanged(final Object source, final int index0, final int index1){
    synchronized(this){
      ListDataEvent event = null;

      final Set<ListDataListener> set = new HashSet<ListDataListener>(_listenerList.keySet());
      final Iterator<ListDataListener> iter = set.iterator();

      while(iter.hasNext()){
        if(event == null){
          event = new ListDataEvent(
                  source, ListDataEvent.CONTENTS_CHANGED,
                  index0, index1);
        }
        final ListDataListener ldl = iter.next();
        ldl.contentsChanged(event);
      }
    }
  }

  /**
   * Notifies all registered listeners that elements have been added in the
   * specified range. The event object is lazily created and reused for all
   * listeners.
   * @param source the source of the event
   * @param index0 the start of the added range
   * @param index1 the end of the added range
   */
  protected void fireIntervalAdded(final Object source, final int index0, final int index1){
    synchronized(this){
      ListDataEvent event = null;

      final Set<ListDataListener> set =
              new HashSet<ListDataListener>(_listenerList.keySet());
      final Iterator<ListDataListener> iter = set.iterator();

      while(iter.hasNext()){
        if(event == null){
          event = new ListDataEvent(
                  source, ListDataEvent.INTERVAL_ADDED,
                  index0, index1);
        }
        final ListDataListener ldl = iter.next();
        ldl.intervalAdded(event);
      }
    }
  }

  /**
   * Notifies all registered listeners that elements have been removed from
   * the specified range. The event object is lazily created and reused for
   * all listeners.
   * @param source the source of the event
   * @param index0 the start of the removed range
   * @param index1 the end of the removed range
   */
  protected void fireIntervalRemoved(final Object source, final int index0, final int index1){
    synchronized(this){
      ListDataEvent event = null;

      final Set<ListDataListener> set =
              new HashSet<ListDataListener>(_listenerList.keySet());

      final Iterator<ListDataListener> iter = set.iterator();

      while(iter.hasNext()){
        if(event == null){
          event = new ListDataEvent(
                  source, ListDataEvent.INTERVAL_REMOVED,
                  index0, index1);
        }
        final ListDataListener ldl = iter.next();
        ldl.intervalRemoved(event);
      }
    }
  }
}
