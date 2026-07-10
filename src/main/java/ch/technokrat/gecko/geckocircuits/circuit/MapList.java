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
 *
 * @author andreas
 */
public class MapList extends ArrayList<AbstractCircuitSheetComponent> {

    private static final long serialVersionUID = 1L;

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

    @Override
    public boolean removeAll(Collection<?> c) {
        assert false;
        return super.removeAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends AbstractCircuitSheetComponent> c) {
        for (AbstractCircuitSheetComponent obj : c) {
            this.add(obj);
        }
        return true;
    }

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
