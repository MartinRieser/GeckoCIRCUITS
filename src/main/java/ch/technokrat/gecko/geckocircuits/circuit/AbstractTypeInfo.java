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

import ch.technokrat.gecko.geckocircuits.general.AbstractComponentType;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract registry for component type metadata. Each concrete subclass
 * registers itself into several static maps keyed by class, ID string, and
 * component enum, enabling runtime lookup, fabrication, and serialization
 * of circuit components.
 */
public abstract class AbstractTypeInfo {

    /** Maps a component class to its component enum value. */
    static Map<Class<? extends AbstractBlockInterface>, AbstractComponentType> _classEnumMap = new HashMap<Class<? extends AbstractBlockInterface>, AbstractComponentType>();
    /** Maps a component class to its AbstractTypeInfo instance. */
    static Map<Class<? extends AbstractBlockInterface>, AbstractTypeInfo> _classTypeMap = new HashMap<Class<? extends AbstractBlockInterface>, AbstractTypeInfo>();
    /** Maps a component ID string to its AbstractTypeInfo instance. */
    static Map<String, AbstractTypeInfo> _stringTypeMap = new HashMap<String, AbstractTypeInfo>();
    /** Maps a component enum to its AbstractTypeInfo instance. */
    static Map<AbstractComponentType, AbstractTypeInfo> _enumTypeMap = new HashMap<AbstractComponentType, AbstractTypeInfo>();
    
    public static final Map<String, AbstractComponentType> _exportImportEnumMap = new HashMap<String, AbstractComponentType>();
    
    static Set<Class<? extends AbstractBlockInterface>> _uniqueClassSet = new HashSet<Class<? extends AbstractBlockInterface>>();
    static Set<String> _uniqueTestSet = new HashSet<String>() {
        @Override
        public boolean add(final String insertTest) {
            assert !this.contains(insertTest) : " Error: ID String is used multiple times: " + insertTest;
            return super.add(insertTest); //To change body of generated methods, choose Tools | Templates.
        }
    };
    static Set<AbstractComponentType> _allRegisteredComponentEnums = new HashSet<AbstractComponentType>();
    static Set<AbstractTypeInfo> _allRegisteredTypeInfos = new HashSet<AbstractTypeInfo>();            

    /**
     * Looks up the type info from a component enum value.
     *
     * @param _typElement the component enum to look up
     * @return the matching type info, or null if not registered
     */
    static AbstractTypeInfo getTypeFromEnum(final AbstractComponentType _typElement) {
        return _enumTypeMap.get(_typElement);
    }

    /**
     * Looks up the type info by component element name string.
     * Throws a {@link RuntimeException} if the name is not found.
     *
     * @param elementType the component name string
     * @return the matching type info
     * @throws RuntimeException if no component with the given name exists
     */
    public static AbstractTypeInfo getFromComponentName(String elementType) {
        if(_stringTypeMap.containsKey(elementType)) {
            return _stringTypeMap.get(elementType);
        } else {
            throw new RuntimeException("Error: a component with type \"" + elementType + "\" does not exist!");
        }
        
    }
    
    public final I18nKeys _typeDescription;
    public final I18nKeys _typeDescriptionVerbose;    
    public final String _fixedIDString;
    public final Class<? extends AbstractBlockInterface> _typeClass;
    public AbstractComponentType _parentType;

    /**
     * Constructs a new type info entry and registers it into the static
     * {@link #_classTypeMap} and {@link #_stringTypeMap} maps, then performs
     * a consistency check for duplicate IDs.
     *
     * @param typeClass the component class
     * @param idString the unique identifier string for this component type
     * @param typeDescription the short i18n description
     * @param typeDescriptionVerbose the verbose i18n description
     */
    @SuppressWarnings("this-escape")
    public AbstractTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription, final I18nKeys typeDescriptionVerbose) {
        _typeDescription = typeDescription;
        _typeDescriptionVerbose = typeDescriptionVerbose;
        _fixedIDString = idString;
        _typeClass = typeClass;
        _classTypeMap.put(_typeClass, this);
        _stringTypeMap.put(idString, this);                
        _allRegisteredTypeInfos.add(this);
        doConsistencyCheck();
    }

    public AbstractTypeInfo(final Class<? extends AbstractBlockInterface> typeClass, final String idString, final I18nKeys typeDescription) {
        this(typeClass, idString, typeDescription, typeDescription);
    }
    
    public abstract ConnectorType getSimulationDomain();
    
    /**
     * Validates that the ID string and class are unique (not already registered).
     * Called during construction.
     */
    public void doConsistencyCheck() {                        
        assert !_fixedIDString.isEmpty();                
        assert !_uniqueTestSet.contains(_fixedIDString) : "Error: ID string is used twice! " + _fixedIDString;
        _uniqueTestSet.add(_fixedIDString);        
        assert !_uniqueClassSet.contains(_typeClass) : "Error: the class is already registered!";
        _uniqueClassSet.add(_typeClass);
    }
    
    public static AbstractTypeInfo getTypeInfoFromClass(Class<? extends AbstractBlockInterface> aClass) {
        return _classTypeMap.get(aClass);
    }
    
    public static AbstractComponentType getTypeEnumFromClass(Class<? extends AbstractBlockInterface> aClass) {
        return _classEnumMap.get(aClass);
    }
            
    /**
     * Registers the parent component enum, populating the class-to-enum and
     * enum-to-typeinfo maps and the export/import map.
     *
     * @param parentType the component enum to associate with this type info
     */
    public void addParentEnum(final AbstractComponentType parentType) {
        assert !_allRegisteredComponentEnums.contains(parentType);                
        _allRegisteredComponentEnums.add(parentType);
        _parentType = parentType;
        _classEnumMap.put(_typeClass, parentType);
        _enumTypeMap.put(parentType, this);
        if(!_exportImportEnumMap.containsKey(this.getExportImportCharacters())) {
            _exportImportEnumMap.put(this.getExportImportCharacters(), parentType);        
        }         

    }
    

    /**
     * Looks up the type info from a type string.
     * Throws an {@link IllegalArgumentException} if the string is not found.
     *
     * @param elementType the type string to look up
     * @return the matching type info
     * @throws IllegalArgumentException if the type string is not registered
     */
    public static AbstractTypeInfo getTypeFromString(final String elementType) {
        if(_stringTypeMap.containsKey(elementType)) {
            return _stringTypeMap.get(elementType);
        } else {
            throw new IllegalArgumentException("String type " + elementType + " could not be found!");
        }
    }
        
    /**
     * Factory method to create a new instance of the component represented by
     * this type info.
     *
     * @return a newly fabricated component instance
     */
    public abstract AbstractBlockInterface fabric();
    
    public abstract String getExportImportCharacters();
    public abstract String getSaveIdentifier(); 
    
    /**
     * Fabricates a component from a file token map, used during file import.
     *
     * @param typ the component enum type
     * @param tokenMap the token map containing serialized component data
     * @return a newly fabricated and imported component instance
     */
    public static final AbstractBlockInterface fabricFromFile(final AbstractComponentType typ, TokenMap tokenMap) {        
        final AbstractBlockInterface returnValue = typ.getTypeInfo().fabric();
        returnValue.importASCII(tokenMap);        
        return returnValue;
    }
    
    /**
     * Fabricates a new component and places it on the visible circuit sheet,
     * calling post-construction initialization.
     *
     * @param typ the type info describing the component to create
     * @return a newly fabricated component placed on the schematic
     */
    public static final AbstractBlockInterface fabricNew(final AbstractTypeInfo typ) {        
        final AbstractBlockInterface returnValue = typ.fabric();
        returnValue.setParentCircuitSheet(SchematicEditor2.Singleton._visibleCircuitSheet);
        returnValue.doOperationAfterNewConstruction();
        return returnValue;
    }
    
    /**
     * Fabricates a hidden sub-circuit component as a child of the given parent.
     *
     * @param typ the component enum type to fabricate
     * @param parent the parent circuit component
     * @return a newly fabricated hidden sub-circuit component
     */
    public static AbstractCircuitBlockInterface fabricHiddenSub(final AbstractComponentType typ,
            final AbstractCircuitSheetComponent parent) {
        final AbstractCircuitBlockInterface returnValue = (AbstractCircuitBlockInterface) typ.getTypeInfo().fabric();
        returnValue.setParent(parent);
        return returnValue;
    }
    
}
