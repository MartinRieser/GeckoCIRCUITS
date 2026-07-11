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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.ControlBlock;
import ch.technokrat.gecko.geckocircuits.control.TextFieldBlock;
import java.util.*;
import java.util.Map.Entry;

/**
 * An ArrayList that maintains a secondary index mapping component types
 * to sub-lists for efficient type-based lookups via {@link #getClassFromContainer(Class)}.
 *
 * @author andreas
 */
public class MapList extends ArrayList<AbstractCircuitSheetComponent> {

    private static final long serialVersionUID = 1L;

    /**
     * The set of types that are tracked in the secondary class-type index map.
     */
    private final Class<?>[] registeredTypes = new Class<?>[]{
        AbstractCircuitBlockInterface.class, ControlBlock.class, AbstractSpecialBlock.class, TextFieldBlock.class,
        ComponentCoupable.class, PotentialCoupable.class, 
        AbstractBlockInterface.class, Connection.class, SubcircuitBlock.class
    };
    private transient final Map<Class<?>, ArrayList<AbstractCircuitSheetComponent>> classMap = new HashMap<Class<?>, ArrayList<AbstractCircuitSheetComponent>>();

    @Override
    public void clear() {
        super.clear();
        classMap.clear();
    }

    /**
     * Removes the element from this list and from all secondary type-index maps.
     * @param o the element to remove
     * @return true if the list was modified
     */
    @Override
    public boolean remove(Object o) {
        for (Entry<Class<?>, ArrayList<AbstractCircuitSheetComponent>> entry : classMap.entrySet()) {
            ArrayList<?> list = entry.getValue();
            if (list.contains(o)) {
                list.remove(o);
            }
        }
        return super.remove(o);
    }

    /**
     * Unsupported operation. The internal map tracking components by class type
     * does not support bulk removal through this method, so calling it is intentionally disabled via assertion.
     *
     * @param c the collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        assert false;
        return super.removeAll(c);
    }

    /**
     * Adds all elements of the given collection to this list, updating the
     * type-index maps for each element.
     * @param c the collection whose elements are to be added
     * @return true if the list was modified
     */
    @Override
    public boolean addAll(Collection<? extends AbstractCircuitSheetComponent> c) {
        for (AbstractCircuitSheetComponent obj : c) {
            this.add(obj);
        }
        return true;
    }

    /**
     * Adds an element to this list and updates the type-index maps for all
     * registered types that the element is an instance of.
     * @param toAdd the element to add
     * @return true (as specified by {@link Collection#add})
     */
    @Override
    public boolean add(AbstractCircuitSheetComponent toAdd) {
        assert toAdd != null;

        for (Class<?> type : registeredTypes) {
            if (type.isInstance(toAdd)) {
                if (classMap.containsKey(type)) {
                    classMap.get(type).add(toAdd);
                } else {
                    ArrayList<AbstractCircuitSheetComponent> newList = new ArrayList<>();
                    newList.add(toAdd);
                    classMap.put(type, newList);
                }
            }
        }
        return super.add(toAdd);
    }

    /**
     * Returns an unmodifiable list of all elements matching the given type,
     * as tracked by the secondary type-index map.
     * @param <T> the type parameter
     * @param type the class type to look up
     * @return an unmodifiable list of matching elements, or an empty list if none found
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getClassFromContainer(final Class<T> type) {
        if (classMap.containsKey(type)) {
            List<T> returnValue = (List<T>) classMap.get(type);
            return Collections.unmodifiableList(returnValue);
        } else {
            return Collections.unmodifiableList(new ArrayList<T>());
        }
    }
}
