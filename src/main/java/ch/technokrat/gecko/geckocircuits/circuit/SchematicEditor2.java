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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.general.AbstractComponentType;
import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import ch.technokrat.gecko.geckocircuits.general.MainWindow;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitGlobalTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.*;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.undo.UndoableEdit;
import ch.technokrat.modelviewcontrol.AbstractUndoGenericModel;
import ch.technokrat.modelviewcontrol.GroupableUndoManager;

public final class SchematicEditor2 implements MouseListener, MouseMotionListener {

    public final CircuitSheet _circuitSheet = new CircuitSheet(this);
    private SchematicComponentSelection2 _sea;
    public MainWindow win;
    //
    private boolean simulatorActivated = false;
    public static boolean zustandGeaendert = false;  // // indicates whether 'QuitWithoutSaving' needs to be enabled
    //
    private static final double CLICK_RADIUS_RELATIVE = 0.5;  // [0...1]
    // Ansicht: verschiedene Varianten -->
    public static ElementDisplayProperties _lkDisplayMode = new ElementDisplayProperties();
    public static ElementDisplayProperties _thermDisplayMode = new ElementDisplayProperties();
    public static ElementDisplayProperties _controlDisplayMode = new ElementDisplayProperties();

    static {
        if (_controlDisplayMode != null) {
            _controlDisplayMode.showName = false;
        }
    }
    public static Font foLKSmall = new Font("Arial", Font.PLAIN, 8);
    public static Font circuitFont = new Font("Arial", Font.PLAIN, 12);
    public static int DY_ZEILENABSTAND_TXT = circuitFont.getSize() + 3;  // // Parameters, names and other labels --> vertical line spacing
    // // the actual elements (LK, CONTROL, THERM) -->
    //    
    private AbstractBlockInterface _selectedTextFieldToMove;
    private long _lastMouseClickTime;
    public CircuitSheet _visibleCircuitSheet = _circuitSheet;
    private ComponentDirection _lastRotationDirection = ComponentDirection.NORTH_SOUTH;
    private CreateComponentUndoAction _createComponentAction;
    public boolean _singleComponentMouseDrag;
    private long _dragStartTime;
    private static final int EMPTY_BORDER_OFFSET = -5;

    public void updateAllComponentReferences() {
        for (ComponentCoupable elem : _circuitSheet.getAllElements().getClassFromContainer(ComponentCoupable.class)) {
            elem.getComponentCoupling().refreshCoupledReferences(getBlockInterfaceComponents());
        }

    }

    public void updateSingleReference(final AbstractBlockInterface elementToUpdate) {
        if (elementToUpdate instanceof ComponentCoupable) {
            ((ComponentCoupable) elementToUpdate).getComponentCoupling().refreshCoupledReferences(getBlockInterfaceComponents());
        }
    }

    private boolean testRenameLabelWhereOldStillExists(final String originalLabelBeforeRename,
            final List<? extends AbstractBlockInterface> elements) {
        for (AbstractBlockInterface elem : elements) {
            for (TerminalInterface term : elem.getAllTerminals()) {
                final String existingCompareLabel = term.getLabelObject().getLabelString();

                if ((!originalLabelBeforeRename.equals("")) && (existingCompareLabel.equals(originalLabelBeforeRename))) {
                    /**
                     * special case: control input terminal. This terminal does
                     * not generate "output" values, therefore don't consider
                     * this for the link detection!
                     */
                    if (!(term instanceof TerminalControlInput)) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    public void setNewVisibleCircuitSheet(final CircuitSheet newCircuitSheet) {

        if (_mouseMoveMode != MouseMoveMode.MOVE_COMPONENTS) {
            _selectedComponents.clear();
        }

        if (!_selectedComponents.isEmpty()) {
            if (!checkSheetComponentsValidForImport(newCircuitSheet)) {
                return;
            }
        }
        _visibleCircuitSheet.removeMouseListener(this);
        _visibleCircuitSheet.removeMouseMotionListener(this);
        _visibleCircuitSheet.removeKeyListener(win.getKeyAdapter());

        _visibleCircuitSheet = _circuitSheet;
        _visibleCircuitSheet = newCircuitSheet;
        _visibleCircuitSheet.doSetVisibleAction();
        final JPanel centerPanel = new JPanel();
        centerPanel.setBorder(new EmptyBorder(EMPTY_BORDER_OFFSET, EMPTY_BORDER_OFFSET, 0, 0));
        centerPanel.setOpaque(false);
        centerPanel.add(_visibleCircuitSheet);
        MainWindow.getSeScroll().setViewportView(centerPanel);
        _visibleCircuitSheet.addMouseListener(this);
        _visibleCircuitSheet.addMouseMotionListener(this);
        _visibleCircuitSheet.addKeyListener(win.getKeyAdapter());
        MainWindow.getSeScroll().revalidate();

        for (AbstractCircuitSheetComponent searchTerminal
                : _selectedComponents.toArray(new AbstractCircuitSheetComponent[_selectedComponents.size()])) {
            if (searchTerminal instanceof SubCircuitTerminable) {
                _selectedComponents.remove(searchTerminal);
                searchTerminal.deselectViaESCAPE();
            }
        }

        for (AbstractCircuitSheetComponent defineNewParent : _selectedComponents) {
            defineNewParent.setParentCircuitSheet(_visibleCircuitSheet);
        }
        newCircuitSheet.revalidate();
        MainWindow.getSeScroll().repaint();
    }

    private Point findRasterPoint(final MouseEvent mouseEvent) {
        final int dpix = AbstractCircuitSheetComponent.dpix;

        final int mx = mouseEvent.getX();
        final int my = mouseEvent.getY();
        int px = mx / dpix;
        int py = my / dpix;
        final double pxd = mx * 1.0 / dpix, pyd = my * 1.0 / dpix;
        if (Math.abs((px + 1) - pxd) < CLICK_RADIUS_RELATIVE) {
            px++;
        }
        if (Math.abs((py + 1) - pyd) < CLICK_RADIUS_RELATIVE) {
            py++;
        }
        return new Point(px, py);
    }

    public List<AbstractBlockInterface> getBlockInterfaceComponents() {
        return Collections.unmodifiableList(_circuitSheet.getAllElements().getClassFromContainer(AbstractBlockInterface.class));
    }

    void setNewScaling(final int dpix) {
        _circuitSheet.setNewScaling(dpix);
        for (AbstractBlockInterface block : _circuitSheet.getAllElements().getClassFromContainer(AbstractSpecialBlock.class)) {
            if (block instanceof SubcircuitBlock) {
                ((SubcircuitBlock) block)._myCircuitSheet.setNewScaling(dpix);
            }
        }
    }

    public void insertNewElement(AbstractCircuitSheetComponent newElement) {
        if (newElement != null) {
            _selectedComponents.clear();
            _moveStartPoint = new Point(0, 0);
            if (_elementsJustInitialized && !(newElement instanceof ControlBlock) && newElement instanceof AbstractCircuitBlockInterface) {
                ((AbstractCircuitBlockInterface) newElement).setComponentDirection(_lastRotationDirection);
            }
            newElement.setParentCircuitSheet(_visibleCircuitSheet);
            _selectedComponents.add(newElement);
            _mouseMoveMode = MouseMoveMode.MOVE_COMPONENTS;
        }

        if (_mouseMoveMode == MouseMoveMode.MOVE_COMPONENTS) {
            //_elementsJustInitialized = true;
            wirePenVisible = false;
        }
    }

    private boolean checkSheetComponentsValidForImport(CircuitSheet newCircuitSheet) {
        List<AbstractBlockInterface> allNamableComponents = newCircuitSheet.allElements.getClassFromContainer(AbstractBlockInterface.class);
        Set<String> existingComponentNames = new HashSet<String>();
        for (AbstractBlockInterface namable : allNamableComponents) {
            existingComponentNames.add(namable.getStringID());
        }

        Set<ComponentCoupling> problematicComponentCoupling = checkMovedComponentCouplings();
        if (!problematicComponentCoupling.isEmpty()) {
            String problematicLabelString = "";
            for (ComponentCoupling tmp : problematicComponentCoupling) {
                problematicLabelString += "\n" + "Component: " + tmp.getParent().getStringID()
                        + " reference to component: " + tmp._coupledElements[0].getStringID();
            }
            Object[] options = {"Delete component references",
                "Cancel"};
            int n = JOptionPane.showOptionDialog(GeckoSim._win,
                    "The following reference to components are not available in \n"
                    + "the new circuit sheet:\n"
                    + problematicLabelString,
                    "Component label reference conflict detected!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (n == 1) {
                return false;
            }
        }

        Set<PotentialCoupling> problematicLabelReferenceComponent = checkMovedPotentialReferences();
        if (!problematicLabelReferenceComponent.isEmpty()) {
            String problematicLabelString = "";
            for (PotentialCoupling tmp : problematicLabelReferenceComponent) {
                problematicLabelString += "\n" + "Component: " + tmp.getParent().getStringID()
                        + " reference to label: " + tmp.getLabels()[0];
            }
            Object[] options = {"Delete label references",
                "Cancel"};
            int n = JOptionPane.showOptionDialog(GeckoSim._win,
                    "The following reference to the labels are not available in \n"
                    + "the new circuit sheet:\n"
                    + problematicLabelString,
                    "Component label reference conflict detected!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (n == 1) {
                return false;
            }
        }

        Set<String> problematicLabels = checkForMissingLabelReferencesAfterSheetMove();
        if (!problematicLabels.isEmpty()) {
            String problematicLabelString = "";
            for (String tmp : problematicLabels) {
                problematicLabelString += "\n" + "Label: " + tmp;
            }
            Object[] options = {"Delete label references",
                "Cancel"};
            int n = JOptionPane.showOptionDialog(GeckoSim._win,
                    "The following labels will be entirely removed from the old\n"
                    + "circuit sheet. However, they are referenced by components.\n"
                    + problematicLabelString,
                    "Component label reference conflict detected!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (n == 1) {
                return false;
            }
        }

        final List<String> allLabelsInNewSheeet = newCircuitSheet.getAllLocalSheetLabels();
        final Set<String> importedLabels = new LinkedHashSet<String>();

        List<AbstractBlockInterface> foundComponentsForRename = new ArrayList<AbstractBlockInterface>();
        for (AbstractCircuitSheetComponent comp : _selectedComponents) {
            if (comp instanceof AbstractBlockInterface) {
                if (existingComponentNames.contains(((AbstractBlockInterface) comp).getStringID())) {
                    foundComponentsForRename.add((AbstractBlockInterface) comp);
                }
            }

            if (comp instanceof ComponentTerminable) {
                importedLabels.addAll(((ComponentTerminable) comp).getAllNodeLabels());
            }
        }

        if (!foundComponentsForRename.isEmpty()) {
            //Custom button text
            String renameObject = "";
            for (AbstractBlockInterface getName : foundComponentsForRename) {
                renameObject += "\n " + getName.getStringID();
            }
            Object[] options = {"Rename components",
                "Cancel"};
            int n = JOptionPane.showOptionDialog(GeckoSim._win,
                    "The following components have to be renamed\n"
                    + "to import them into the circuit sheet:\n"
                    + renameObject,
                    "Component name conflict detected!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (n == 1) {
                return false;
            }
        }

        List<String> labelsToRename = new ArrayList<String>();

        for (String existingLabel : allLabelsInNewSheeet) {
            if (importedLabels.contains(existingLabel)) {
                labelsToRename.add(existingLabel);
            }
        }

        if (!labelsToRename.isEmpty()) {
            //Custom button text
            String renameObject = "";
            for (String renameLabel : labelsToRename) {
                renameObject += "\n " + renameLabel;
            }
            Object[] options = {"Rename labels", "Merge without rename",
                "Cancel"};
            int n = JOptionPane.showOptionDialog(GeckoSim._win,
                    "The following labels have to be renamed\n"
                    + "to import them into the circuit sheet:\n"
                    + renameObject,
                    "Label name name conflict detected!",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (n == 2) {
                return false;
            }

            if (n == 0) {
                renameConflictingLabels(_selectedComponents, allLabelsInNewSheeet, allLabelsInNewSheeet);
            }
        }

        for (AbstractBlockInterface toRename : foundComponentsForRename) {
            String oldName = toRename.getStringID();
            String newName = IDStringDialog.findUnusedName(toRename.getStringID());
            try {
                toRename.getIDStringDialog().setNewNameChecked(newName);
                updateComponentCouplings(oldName, newName);
            } catch (NameAlreadyExistsException ex) {
                Logger.getLogger(SchematicEditor2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        for (PotentialCoupling coupling : problematicLabelReferenceComponent) {
            for (int i = 0; i < coupling.getLabels().length; i++) {
                coupling.setNewCouplingLabel(i, "");
            }
        }

        for (AbstractCircuitSheetComponent comp : _visibleCircuitSheet.allElements) {
            if (comp instanceof PotentialCoupable) {
                PotentialCoupling coup = ((PotentialCoupable) comp).getPotentialCoupling();
                String[] couplingLabels = coup.getLabels();
                for (int i = 0; i < couplingLabels.length; i++) {
                    if (problematicLabels.contains(couplingLabels[i])) {
                        coup.setNewCouplingLabel(i, "");
                    }
                }
            }
        }

        for (ComponentCoupling removeRef : problematicComponentCoupling) {
            for (int i = 0; i < removeRef._coupledElements.length; i++) {
                removeRef.setNewCouplingElement(i, null);
            }
        }

        return true;
    }

    public void selectAll() {
        _selectedComponents.clear();
        _selectedComponents.addAll(_visibleCircuitSheet.allElements);
        _visibleCircuitSheet.repaint();
    }

    /**
     * when moving components from one sheet to another, labels could "disapear"
     * within a sheet, when all components with a specific labels are shifted to
     * another sheet. Therefore, check if this is the case and give a warning
     * that missing references could occur!
     *
     * @param newCircuitSheet
     */
    private Set<String> checkForMissingLabelReferencesAfterSheetMove() {
        Set<AbstractCircuitSheetComponent> remainingComponents = new HashSet<AbstractCircuitSheetComponent>();
        Set<String> allLabelsBeforeMove = new HashSet<String>();

        allLabelsBeforeMove.addAll(_visibleCircuitSheet.getAllLocalSheetLabels());
        remainingComponents.addAll(_visibleCircuitSheet.allElements);
        remainingComponents.removeAll(_selectedComponents);
        Set<String> remainingLabels = new HashSet<String>();

        allLabelsBeforeMove.remove("");

        for (AbstractCircuitSheetComponent comp : remainingComponents) {
            if (comp instanceof ComponentTerminable) {
                remainingLabels.addAll(((ComponentTerminable) comp).getAllNodeLabels());
            }
        }

        remainingLabels.remove("");

        Set<String> problematicLabels = new HashSet<String>();
        for (AbstractCircuitSheetComponent comp : remainingComponents) {
            if (comp instanceof PotentialCoupable) {
                PotentialCoupling coup = ((PotentialCoupable) comp).getPotentialCoupling();
                for (String tmp : coup.getLabels()) {
                    if (tmp.isEmpty()) {
                        continue;
                    }
                    if (!remainingLabels.contains(tmp)) {
                        problematicLabels.add(tmp);
                    }
                }
            }
        }
        return problematicLabels;
    }

    private Set<PotentialCoupling> checkMovedPotentialReferences() {
        final Set<PotentialCoupling> returnValue = new HashSet<PotentialCoupling>();
        final Set<String> allTransferredLabels = new HashSet<String>();
        for (AbstractCircuitSheetComponent labelSearch : _selectedComponents) {
            if (labelSearch instanceof ComponentTerminable) {
                allTransferredLabels.addAll(((ComponentTerminable) labelSearch).getAllNodeLabels());
            }
        }
        allTransferredLabels.remove("");

        for (AbstractCircuitSheetComponent comp : _selectedComponents) {
            if (comp instanceof PotentialCoupable) {
                PotentialCoupling coupling = ((PotentialCoupable) comp).getPotentialCoupling();
                for (String coupledLabel : coupling.getLabels()) {
                    if (coupledLabel == null || coupledLabel.isEmpty()) {
                        continue;
                    }
                    if (!allTransferredLabels.contains(coupledLabel)) {
                        returnValue.add(coupling);
                    }
                }
            }
        }
        return returnValue;
    }

    private Set<ComponentCoupling> checkMovedComponentCouplings() {
        final Set<ComponentCoupling> returnValue = new HashSet<ComponentCoupling>();

        for (AbstractCircuitSheetComponent comp : _selectedComponents) {
            if (comp instanceof ComponentCoupable) {
                ComponentCoupling coupling = ((ComponentCoupable) comp).getComponentCoupling();

                // forward reference detection
                for (AbstractBlockInterface coupledTo : coupling._coupledElements) {
                    if (coupledTo == null) {
                        continue;
                    }
                    if (!_selectedComponents.contains(coupledTo)) {
                        returnValue.add(coupling);
                    }
                }
            }
            // backward reference detection:
            if (comp instanceof AbstractBlockInterface) {
                AbstractBlockInterface block = (AbstractBlockInterface) comp;
                for (ComponentCoupling coupling : block._isReferencedBy) {
                    if (!_selectedComponents.contains(coupling.getParent())) {
                        returnValue.add(coupling);
                    }
                }
            }

        }

        return returnValue;
    }

    public void setSchematicEditorSelection(SchematicComponentSelection2 sea) {
        _sea = sea;
    }

    public void testCreateNewComponent() {
        if (_sea._typElement == null) {
            return;
        }
        if (_mouseMoveMode == MouseMoveMode.MOVE_COMPONENTS || connectorTestMode) {
            return;
        }
        AbstractComponentType newType = _sea._typElement;
        _sea._typElement = null;
        AbstractBlockInterface newElement = AbstractTypeInfo.fabricNew(newType.getTypeInfo());
        if (newElement != null) {
            _elementsJustInitialized = true;
            _createComponentAction = new CreateComponentUndoAction(Arrays.asList(newElement));
        }
        insertNewElement(newElement);
    }

    private boolean isRightMouseClickActionOrCtrlLeftClick(final MouseEvent me) {
        // on mac's with only one mouse button, you can ctrl+leftclick
        // to emulate a right-click.
        return me.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK || me.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK;
    }

    private void doRightMouseClickAction() {
        if (_mouseMoveMode != MouseMoveMode.MOVE_COMPONENTS) {
            if (_mouseMoveMode != MouseMoveMode.DRAW_CONNECTION) {//              
                this.mouseToggleSelectModeWireModeConnectorTestMode();
            }
        } else {
            this.mouseRotateElement();  // // rotate LK element or THERM element
        }
    }

    private boolean isLabelRenameRequired(List<String> allOriginalLabels, List<String> copiedLabels) {

        Set<String> testSet = new HashSet<String>(allOriginalLabels);
        for (String compare : copiedLabels) {
            if (testSet.contains(compare)) {
                return true;
            }
        }
        return false;
    }

    public enum MouseMoveMode {

        NONE,
        MOVE_COMPONENTS,
        MOVE_TEXT,
        SELECT_WINDOW,
        DRAW_CONNECTION;
    }
    MouseMoveMode _mouseMoveMode = MouseMoveMode.NONE;
    private Set<AbstractCircuitSheetComponent> _selectedComponents = new LinkedHashSet<AbstractCircuitSheetComponent>() {
        @Override
        public boolean remove(Object o) {
            boolean returnValue = super.remove(o);
            if (o instanceof AbstractCircuitSheetComponent) {
                ((AbstractCircuitSheetComponent) o).setModus(ComponentState.FINISHED);
            }
            return returnValue;
        }

        @Override
        public boolean add(AbstractCircuitSheetComponent e) {

            boolean returnValue = super.add(e);
            if (e instanceof AbstractCircuitSheetComponent) {
                e.setModus(ComponentState.SELECTED);
            }
            return returnValue;
        }

        @Override
        public void clear() {
            for (AbstractCircuitSheetComponent comp : this) {
                comp.setModus(ComponentState.FINISHED);
            }
            super.clear();
        }
    };
    private boolean _elementsJustInitialized;  // // so that you can cancel a running initialization, e.g. with the ESCAPE button
    private Point _moveStartPoint = new Point(0, 0);  // // Starting point (ScematicEntry coordinate) for moving/copying elements
    boolean connectorTestMode = false;
    public boolean wirePenVisible = false;
    public int wireModeVersteckt = WIRE_MODE_LK;
    public static final int WIRE_MODE_OFF = 0, WIRE_MODE_LK = 1, WIRE_MODE_CONTROL = 2, WIRE_MODE_THERM = 3, WIRE_MODE_RELUCTANCE = 4;
    //
    int[] xStift, yStift;  // Symbol Zeichenstift
    // if connectorTestMode is set, the Potentailgebiebt can be drawn: ovals at Element-nodes and fat (colored) Connectionen     
    // // Marking rectangle to define larger areas with the mouse:
    int x1markRe, y1markRe, x2markRe, y2markRe;  // echte Pixel-Koord.
    boolean antialiasing = false;    //

    // // you can use this to activate or deactivate the simulator -->
    public void setActivationOfSimulator(boolean simulatorActivated) {
        this.simulatorActivated = simulatorActivated;
    }
    public static SchematicEditor2 Singleton;

    // // so that you can modify the title bar if the changes have not yet been saved -->
    public void setWindow(MainWindow win) {
        this.win = win;
    }

    public SchematicEditor2(final MainWindow win) {
        this.win = win;
        _circuitSheet.setPreferredSize(new Dimension(1000, 1000));

        AbstractCircuitSheetComponent.dpix = 16;  // default-Skalierung                
        xStift = new int[4];
        yStift = new int[4];
        this.resetCircuitSheetsForNewFile();

        _circuitSheet.setDoubleBuffered(true);
        Singleton = this;

    }

    public void resetModelModified() {
        zustandGeaendert = false;
    }

    public boolean getZustandGeaendert() {
        return zustandGeaendert;
    }

    public void setzeFont(int fontSize, String fontTyp) {
        SchematicEditor2.circuitFont = new Font(fontTyp, Font.PLAIN, fontSize);
        SchematicEditor2.foLKSmall = new Font(fontTyp, Font.PLAIN, fontSize * 2 / 3);
        int fs = SchematicEditor2.circuitFont.getSize();
        SchematicEditor2.DY_ZEILENABSTAND_TXT = fs + (fs < 12 ? 2 : 3);
        _circuitSheet.repaint();
    }

    //=======================================================
    // // here all changes in the SchematicEntry are registered for the undo/redo functionality -->
    // // (of course also called from the dialogs)
    public void registerChangeWithNetlistUpdate() {
        this.updateNewNetlists();
        setDirtyFlag();
    }

    public void setDirtyFlag() {
        zustandGeaendert = true;
        _visibleCircuitSheet.repaint();
        win.modifyTitle();
    }

    //for linking up additional files to the blocks
    public void initAdditionalFiles(List<? extends AbstractCircuitSheetComponent> geckoFileables) {
        for (AbstractCircuitSheetComponent elem : geckoFileables) {
            if (elem instanceof GeckoFileable) {
                ((GeckoFileable) elem).initExtraFiles();
            }
        }
        MainWindow.getScripter().initExtraFiles();
    }

    public void resetCircuitSheetsForNewFile() {
        for (AbstractCircuitSheetComponent comp : _circuitSheet.getAllElements().toArray(new AbstractCircuitSheetComponent[0])) {
            comp.deleteComponent();
        }
        // // default settings:
        _mouseMoveMode = MouseMoveMode.NONE;
        _selectedComponents.clear();
        AbstractCircuitGlobalTerminal.ALL_GLOBALS.clear();
        ControlGlobalTerminal.ALL_GLOBALS.clear();
        wirePenVisible = false;
        deleteAllComponents(_selectedComponents);
        setNewVisibleCircuitSheet(_circuitSheet);
        ControlFromEXTERNAL.fromExternals.clear();
        ControlToEXTERNAL.toExternals.clear();

        AbstractUndoGenericModel.undoManager.discardAllEdits();
        CircuitSheet._findNodes.clear();
        CircuitSheet._showNodes.clear();
    }

    public NetListLK getNetlist(final ConnectorType connectorType) {

        List<Connection> shortConnectors = new ArrayList<Connection>();

        List<? extends AbstractCircuitBlockInterface> eLK = null;
        switch (connectorType) {
            case LK_AND_RELUCTANCE:
                eLK = getElementLK();
                break;
            case THERMAL:
                eLK = getElementTHERM();
                break;
            default:
                assert false;
        }

        for (AbstractCircuitBlockInterface elem : eLK) {
            if (elem != null && elem.isCircuitEnabled() == Enabled.DISABLED_SHORT) {
                shortConnectors.addAll(elem.getShortConnectors());
            }
        }

        List<AbstractCircuitBlockInterface> eNew = new ArrayList<AbstractCircuitBlockInterface>();

        for (AbstractCircuitBlockInterface elem : eLK) {
            if (elem != null && elem.isCircuitEnabled() == Enabled.ENABLED) {
                eNew.add(elem);
            }
        }

        Set<Connection> vNew = new LinkedHashSet<Connection>();

        for (Connection verb : getConnection(connectorType)) {
            if (verb != null) {
                if (verb.isCircuitEnabled() == Enabled.ENABLED) {
                    vNew.add(verb);
                }
            }
        }

        for (Connection shortConnector : shortConnectors) {
            vNew.add(shortConnector);
        }

        NetListLK netzliste = NetListLK.fabricIncludingSubcircuits(vNew, eNew);

        return netzliste;
    }

    public List<AbstractCircuitBlockInterface> getElementLK() {
        LinkedList<AbstractCircuitBlockInterface> returnValue = new LinkedList<AbstractCircuitBlockInterface>();
        List<AbstractCircuitBlockInterface> allBlocks = _circuitSheet.getAllElements().getClassFromContainer(AbstractCircuitBlockInterface.class);

        returnValue.addAll(allBlocks);

        for (AbstractCircuitBlockInterface toRemove : allBlocks) {
            if (toRemove.getSimulationDomain() != ConnectorType.LK
                    && toRemove.getSimulationDomain() != ConnectorType.RELUCTANCE
                    && toRemove.getSimulationDomain() != ConnectorType.LK_AND_RELUCTANCE) {
                returnValue.remove(toRemove);
            }
        }
        return returnValue;
    }

    public List<ControlBlock> getElementCONTROL() {
        return Collections.unmodifiableList(_circuitSheet.getAllElements().getClassFromContainer(ControlBlock.class));
    }

    public List<AbstractCircuitBlockInterface> getElementTHERM() {
        LinkedList<AbstractCircuitBlockInterface> toFilter = new LinkedList<AbstractCircuitBlockInterface>();
        toFilter.addAll(_circuitSheet.getAllElements().getClassFromContainer(AbstractCircuitBlockInterface.class));
        for (AbstractCircuitBlockInterface remove : toFilter.toArray(new AbstractCircuitBlockInterface[0])) {
            if (remove.getSimulationDomain() != ConnectorType.THERMAL) {
                toFilter.remove(remove);
            }
        }
        return Collections.unmodifiableList(toFilter);
    }
    
    public List<AbstractSpecialBlock> getElementSpecial() {        
        return Collections.unmodifiableList(_circuitSheet.getAllElements().getClassFromContainer(AbstractSpecialBlock.class));
    }

    public Collection<Connection> getConnection(final ConnectorType connectorType) {
        if (connectorType == ConnectorType.LK_AND_RELUCTANCE) {
            Set<Connection> returnValue = new LinkedHashSet<Connection>();
            returnValue.addAll(getConnection(ConnectorType.LK));
            returnValue.addAll(getConnection(ConnectorType.RELUCTANCE));
            return Collections.unmodifiableCollection(returnValue);
        }

        Set<Connection> returnValue = new LinkedHashSet<Connection>();
        for (AbstractCircuitSheetComponent verbCand : _circuitSheet.getAllElements().getClassFromContainer(Connection.class)) {
            if (verbCand instanceof Connection) {
                Connection verb = (Connection) verbCand;
                if (verb.getSimulationDomain() == connectorType
                        || verb.getSimulationDomain() == ConnectorType.NONE) {
                    returnValue.add(verb);
                }
            }

        }
        return Collections.unmodifiableCollection(returnValue);
    }

    public void loadSavedNetlistFromProjectData(final ProjectData daten,
            final String insertIntoSubCircuitName) {
        for (AbstractCircuitSheetComponent elem : daten._allSheetComponents) {
            elem.findAndSetReferenceToParentSheet(daten.allSubCircuitBlocks, insertIntoSubCircuitName);
        }

        //initAdditionalFiles(daten._allSheetComponents);
        _visibleCircuitSheet.repaint();
    }

    public void toggleEnable(final boolean shortDisableCommand) {
        registerChangeWithNetlistUpdate();
        // // Element groups have been selected ('within the selection rectangle'), but not yet manipulated (e.g. moved) -->
        //System.out.println("deselect() --> markierungsRechteckAktiviert"); 
        //======================
        _mouseMoveMode = MouseMoveMode.NONE;

        DoActionOnSelectedComponents action = new DoActionOnSelectedComponents() {
            @Override
            void operation(final AbstractCircuitSheetComponent component) {
                if (component instanceof AbstractBlockInterface) {

                    AbstractBlockInterface elem = (AbstractBlockInterface) component;
                    switch (elem.isCircuitEnabled()) {
                        case DISABLED:
                            elem._isEnabled.setValue(Enabled.ENABLED);
                            break;
                        case ENABLED:
                            if (shortDisableCommand) {
                                elem._isEnabled.setValue(Enabled.DISABLED_SHORT);
                            } else {
                                elem._isEnabled.setValue(Enabled.DISABLED);
                            }

                            break;
                        case DISABLED_SHORT:
                            elem._isEnabled.setValue(Enabled.ENABLED);
                            break;
                        default:
                            assert false;
                    }
                }

                if (!shortDisableCommand && component instanceof Connection) {
                    final Connection verb = (Connection) component;
                    if (verb.isCircuitEnabled() == Enabled.ENABLED) {
                        verb._isEnabled.setValue(Enabled.DISABLED);
                    } else {
                        verb._isEnabled.setValue(Enabled.ENABLED);
                    }
                }

            }
        };

        action.execute();
        _selectedComponents.clear();

    }

    Graphics2D findGraphicsForComponent(final AbstractCircuitSheetComponent comp, final Graphics2D normal,
            final Graphics2D selected, Graphics2D disabled) {

        if (comp.getModus() == ComponentState.SELECTED) {
            return selected;
        }

        if (comp.isCircuitEnabled() == Enabled.DISABLED) {
            return disabled;
        }
        return normal;
    }

    public void mousePressed(final MouseEvent me) {

        if (me.getClickCount() < 2 && System.currentTimeMillis() - _lastMouseClickTime < 350
                && me.getModifiersEx() != MouseEvent.BUTTON3_DOWN_MASK && _mouseMoveMode != MouseMoveMode.DRAW_CONNECTION) {
            // this is to prevent the following: when trying to double-click a component, but
            // accidentally we move the mouse during the double click, the component is moved,
            // accidentaly. This is annoying, e.g. when shifting a scope and all labels are
            // renamed automatically.
            deselect();
            return;
        }

        if (!(me.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK)) {
            _lastMouseClickTime = System.currentTimeMillis();
        }

        if (!simulatorActivated) {
            return;
        }

        //---------------------------
        // // Determination of coordinates with snap function:
        int mx = me.getX(), my = me.getY();

        boolean isShiftClick = me.isShiftDown();
        final Point clickPoint = findRasterPoint(me);
        final Point mousePosition = new Point(me.getX(), me.getY());
        //
        // **************************
        // rechte Maus-Taste gedrueckt:
        // **************************
        //
        if (isRightMouseClickActionOrCtrlLeftClick(me)) {
            doRightMouseClickAction();
            return;
        }

        // **************************
        // linke Maus-Taste 1x gedrueckt:
        // **************************
        //
        // // if several elements are marked at the same time, they will all be sent at the same time -->
        if (!wirePenVisible && (_selectedComponents.size() > 0 && !isShiftClick) && (me.getClickCount() <= 1)) {

            if (_mouseMoveMode == MouseMoveMode.MOVE_COMPONENTS) {
                for (SubcircuitBlock testClickSub : _circuitSheet.getAllElements().getClassFromContainer(SubcircuitBlock.class)) {
                    if (_selectedComponents.contains(testClickSub)) {
                        continue;
                    }
                    if (_visibleCircuitSheet.equals(testClickSub.getParentCircuitSheet())) {
                        if (testClickSub.elementAngeklickt(clickPoint) > 0) {
                            setNewVisibleCircuitSheet(testClickSub._myCircuitSheet);
                            return;
                        }
                    }
                }
                mouseReleaseSelectedGroup();
                return;
            }
        }

        if (_mouseMoveMode == MouseMoveMode.MOVE_TEXT && me.getClickCount() <= 1) {
            this.mouseReleaseText(mousePosition);
            return;
        }

        // **************************
        // linke Maus-Taste DOPPELT gedrueckt:
        // **************************
        //        
        if (me.getClickCount() > 1) {
            _selectedComponents.clear();
            //this.deselect();
            // starte Element-Dialoge (ParameterDialog bzw. NodeLabelDialog) -->            
            AbstractCircuitSheetComponent clickedBlock = null;

            for (AbstractCircuitSheetComponent elem : _visibleCircuitSheet.getLocalSheetComponents()) {
                if (elem.testDoDoubleClickAction(clickPoint)) {
                    if (elem instanceof TextFieldBlock) { // other components should have click-priority in comparison to
                        // textfield-block!
                        if (clickedBlock == null) {
                            clickedBlock = elem;
                        }
                    } else {
                        clickedBlock = elem;
                    }

                }
            }

            if (clickedBlock != null) {
                _mouseMoveMode = MouseMoveMode.NONE;
                _selectedComponents.clear();
                if (clickedBlock instanceof AbstractBlockInterface) {
                    ((AbstractBlockInterface) clickedBlock).setParameterWithoutUnDo(((AbstractBlockInterface) clickedBlock).getParameter());
                }
                clickedBlock.doDoubleClickAction(clickPoint);
                deselect();
                return;
            }
        }
        // **************************
        // linke Maus-Taste 1x gedrueckt:
        // **************************
        //               

        if (connectorTestMode) {
            _selectedComponents.clear();
            _visibleCircuitSheet.mouseConnectorTest(clickPoint);
            return;
        }

        if (wirePenVisible) {
            if (_mouseMoveMode == MouseMoveMode.DRAW_CONNECTION) {
                this.maus_ConnectionSetzeEndknoten(clickPoint);
            } else {
                this.maus_ConnectionSetzeStartknoten(clickPoint);
            }
            _visibleCircuitSheet.repaint();
            return;
        }

        if (me.getClickCount() <= 1) {
            // // Mark element for editing: only one element can be marked for editing -->

            // // Mark text for relative moving -->
            if (this.mouseMarkTextForEditing(mx, my)) {
                return;
            }
            if (testAndDoSingleSelection(clickPoint, isShiftClick)) {
                return;
            }
        }

        _visibleCircuitSheet.repaint();
        //
        //---------------------------
        if (_mouseMoveMode != MouseMoveMode.SELECT_WINDOW && (!connectorTestMode) && _mouseMoveMode != MouseMoveMode.DRAW_CONNECTION) {
            _mouseMoveMode = MouseMoveMode.SELECT_WINDOW;
            x1markRe = mx;
            y1markRe = my;
            x2markRe = mx;
            y2markRe = my;  // // to avoid a short flicker at the beginning
        } else if (_mouseMoveMode == MouseMoveMode.SELECT_WINDOW) {
            _mouseMoveMode = MouseMoveMode.NONE;
            // // and now deselect all selected elements -->
            _selectedComponents.clear();
        }
    }

    public void deselect() {
        switch (_mouseMoveMode) {
            case SELECT_WINDOW: // pressing escape during dragging the selection window:                
                _mouseMoveMode = MouseMoveMode.NONE;
                break;
            case DRAW_CONNECTION:
                this.deleteSelectedComponents();
                break;
            case MOVE_COMPONENTS:
                if (_elementsJustInitialized) {
                    // // Element just created, but not yet placed: delete and disappear -->
                    //System.out.println("deselect() --> geradeInitialisiert_elementLK/CONTROL/THERM"); 
                    //======================
                    _elementsJustInitialized = false;
                    this.deleteSelectedComponents();
                } else {
                    // // Selected element group has already been MOVED, but not yet placed --> de-mark and place at the old position -->
                    //System.out.println("deselect() --> geradeBeimVerschieben"); 
                    //======================
                    _mouseMoveMode = MouseMoveMode.NONE;
                    _sea._typElement = null;
                    for (AbstractCircuitSheetComponent selected : _selectedComponents) {
                        selected.deselectViaESCAPE();
                    }
                }
                break;
            case MOVE_TEXT:
                _selectedTextFieldToMove._textInfo.setPositionDeselect();
                break;
            case NONE:
                break; // nothing todo!!!
        }

        _selectedComponents.clear();
        _mouseMoveMode = MouseMoveMode.NONE;
        _singleComponentMouseDrag = false;
        _visibleCircuitSheet.repaint();
    }

    public void mouseToggleSelectModeWireModeConnectorTestMode() {

        if (connectorTestMode) {
            connectorTestMode = false;
            wirePenVisible = false;
            win.setConnectorTestCheckBox(false);
            return;
        }

        wirePenVisible = !wirePenVisible;
        _visibleCircuitSheet.repaint();
    }

    public void setConnectorTestMode(boolean checkConnectors) {
        if (checkConnectors) {
            wirePenVisible = false;
            connectorTestMode = true;
            CircuitSheet._showNodes.clear();
        } else {
            wirePenVisible = false;
            connectorTestMode = false;
        }
        _visibleCircuitSheet.repaint();
    }

    // // rotate LK element or THERM element -->
    public void mouseRotateElement() {
        if (_selectedComponents.size() != 1) {
            return;
        }

        AbstractCircuitSheetComponent selectedComponent = _selectedComponents.iterator().next();
        if (!(selectedComponent instanceof AbstractBlockInterface)) {
            return;
        }
        AbstractBlockInterface elementAKTUELL = (AbstractBlockInterface) selectedComponent;
        elementAKTUELL.rotateSymbol();
        _visibleCircuitSheet.repaint();
        _lastRotationDirection = elementAKTUELL.getComponentDirection();
        GeckoSim._win.getLastComponentButton().setComponentDirection(_lastRotationDirection);
    }

    private void mouseReleaseSelectedGroup() {
        final DoActionOnSelectedComponents action = new DoActionOnSelectedComponents() {
            @Override
            void operation(final AbstractCircuitSheetComponent comp) {
                if (comp instanceof AbstractBlockInterface) {
                    ((AbstractBlockInterface) comp).setComponentDirectionUndo();
                }
                if (_createComponentAction != null) {
                    AbstractUndoGenericModel.undoManager.addEdit(_createComponentAction);
                    _createComponentAction = null;
                    comp.absetzenElement();
                    _elementsJustInitialized = false;
                } else {
                    comp.setPositionWithUndo();
                }

            }
        };

        try {
            action.execute();
            _sea._typElement = null;
            _selectedComponents.clear();
            _mouseMoveMode = MouseMoveMode.NONE;
            _singleComponentMouseDrag = false;
            _visibleCircuitSheet.repaint();
            this.registerChangeWithNetlistUpdate();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning!",
                    JOptionPane.WARNING_MESSAGE);
        }

    }

    // // individual text field is placed (after relative shift) -->
    private void mouseReleaseText(final Point position) {
        if (_mouseMoveMode != MouseMoveMode.MOVE_TEXT) {
            return;
        }

        _selectedTextFieldToMove._textInfo.absetzenText(position);
        _visibleCircuitSheet.repaint();
        _singleComponentMouseDrag = false;
        _mouseMoveMode = MouseMoveMode.NONE;
    }

    private abstract class DoActionOnSelectedComponents {

        public final void execute() {
            final int selectionSize = _selectedComponents.size();
            GroupableUndoManager.GroupUndoStart groupStart = null;
            if (selectionSize > 1) {
                groupStart = new GroupableUndoManager.GroupUndoStart();
                AbstractUndoGenericModel.undoManager.addEdit(groupStart);
            }
            for (AbstractCircuitSheetComponent comp : _selectedComponents) {
                operation(comp);
            }

            if (groupStart != null) {
                AbstractUndoGenericModel.undoManager.addEdit(new GroupableUndoManager.GroupUndoStop(groupStart));
            }
        }

        abstract void operation(final AbstractCircuitSheetComponent component);
    }

    private boolean testAndDoSingleSelection(final Point clickPoint, final boolean isShiftClick) {
        _moveStartPoint = clickPoint;
        TextFieldBlock possibleSelectionTextFields = null;
        boolean triangleModification = false;

        for (AbstractCircuitSheetComponent elem : _visibleCircuitSheet.getLocalSheetComponents()) {
            int angeklickt = elem.elementAngeklickt(clickPoint);

            if (angeklickt == 1) {

                if (elem instanceof TextFieldBlock) {
                    possibleSelectionTextFields = (TextFieldBlock) elem;
                    continue;
                }
                if (isShiftClick) {
                    if (_selectedComponents.contains(elem)) {
                        _selectedComponents.remove(elem);
                    } else {
                        _selectedComponents.add(elem);
                    }
                } else {
                    _selectedComponents.add(elem);
                    _mouseMoveMode = MouseMoveMode.MOVE_COMPONENTS;
                }

                _visibleCircuitSheet.repaint();
                return true;
            } else if (angeklickt == 2) {
                triangleModification = true;
            }
        }

        if (possibleSelectionTextFields != null && !triangleModification) {
            if (isShiftClick) {
                if (_selectedComponents.contains(possibleSelectionTextFields)) {
                    _selectedComponents.remove(possibleSelectionTextFields);
                } else {
                    _selectedComponents.add(possibleSelectionTextFields);
                }
            } else {
                _selectedComponents.clear();
                _selectedComponents.add(possibleSelectionTextFields);
                _mouseMoveMode = MouseMoveMode.MOVE_COMPONENTS;
            }

            _visibleCircuitSheet.repaint();
            return true;
        }

        return false;
    }

    // // Mark text (for LK or CONTROL or THERM) for editing with a single click -->
    private boolean mouseMarkTextForEditing(final int x, final int y) {

        for (AbstractCircuitSheetComponent elem : _visibleCircuitSheet.getLocalSheetComponents()) {
            if (elem instanceof AbstractBlockInterface) {
                AbstractBlockInterface block = (AbstractBlockInterface) elem;
                if (block.elementTEXTAngeklickt(x, y)) {
                    _selectedTextFieldToMove = block;
                    _mouseMoveMode = MouseMoveMode.MOVE_TEXT;
                    _selectedTextFieldToMove._textInfo.setPositionTextClickPointInitial(x, y);  // // Shifting the text field relative to the element refers to this initially clicked point
                    _visibleCircuitSheet.repaint();
                    return true;
                }
            }
        }

        return false;
    }

    // // Stop or start pulling ConnectionLK -->
    private void maus_ConnectionSetzeEndknoten(final Point clickPoint) {
        if (_selectedComponents.size() != 1) {
            return;
        }
        ((Connection) _selectedComponents.iterator().next()).setzeEndKnoten(clickPoint.x, clickPoint.y);
        NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.LK_AND_RELUCTANCE), getElementLK());
        NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.THERMAL), getElementTHERM());
        NetlistGeneral.fabricNetzlistComplete(getConnection(ConnectorType.CONTROL), getElementCONTROL());

        _mouseMoveMode = MouseMoveMode.NONE;
        _selectedComponents.clear();
        this.registerChangeWithNetlistUpdate();
    }

    private void maus_ConnectionSetzeStartknoten(final Point clickPoint) {
        _selectedComponents.clear();
        _mouseMoveMode = MouseMoveMode.DRAW_CONNECTION;

        final Connection verbindungAKTUELL = new Connection(ConnectorType.NONE, _visibleCircuitSheet);
        CreateConnectionUndoAction verbAction = new CreateConnectionUndoAction(verbindungAKTUELL);
        AbstractUndoGenericModel.undoManager.addEdit(verbAction);
        verbindungAKTUELL.setParentCircuitSheet(_visibleCircuitSheet);
        _selectedComponents.add(verbindungAKTUELL);
        verbindungAKTUELL.setzeStartKnoten(clickPoint);
    }

    public void mouseMoved(MouseEvent me) {
        if (!simulatorActivated) {
            return;
        }
        Point movePoint = findRasterPoint(me);
        final int px = movePoint.x;
        final int py = movePoint.y;
        //---------------------------
        // // Symbol drawing pen when drawing the connections -->
        int dpix = AbstractCircuitSheetComponent.dpix;
        xStift[0] = dpix * px;
        yStift[0] = dpix * py;
        xStift[1] = (int) (dpix * (px + 0.4));
        yStift[1] = (int) (dpix * (py - 1.5));
        xStift[2] = (int) (dpix * (px + 0.7));
        yStift[2] = (int) (dpix * (py - 1.5));
        xStift[3] = (int) (dpix * (px + 0.1));
        yStift[3] = dpix * py;
        //
        //---------------------------
        // // Elements (LK, CONTROL, THERM) are selected and created/initialized -->
        //                  

        // // Connection (LK or CONTROL or THERM) or element (LK or CONTROL or THERM) is moved around -->
        //        
        switch (_mouseMoveMode) {
            case DRAW_CONNECTION:
                assert _selectedComponents.size() == 1;
                Connection verbindungAKTUELL = (Connection) _selectedComponents.iterator().next();
                verbindungAKTUELL.setzeAktuellenPunktAufConnection(new Point(px, py));
                break;
            case MOVE_COMPONENTS:
                if (!_selectedComponents.isEmpty()) {
                    Point movePosition = new Point(movePoint.x - _moveStartPoint.x, movePoint.y - _moveStartPoint.y);
                    this.moveSelectedComponents(movePosition);
                }
                break;
            case MOVE_TEXT:
                _selectedTextFieldToMove._textInfo.setNewRelativePosition(new Point(me.getX(), me.getY()));
                break;
        }

        _visibleCircuitSheet.repaint();
    }

    public Connection externalCreateAndPlaceNewConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal) {
        Connection connector = new Connection(ConnectorType.NONE, _circuitSheet);
        connector.setzeStartKnoten(new Point(xStart, yStart));

        int xLength = xEnd - xStart;
        int yLength = yEnd - yStart;
        
        int xDir = 1;
        if(xEnd < xStart) {
            xDir = -1;
        }
        
        int yDir = 1;
        if(yEnd < yStart) {
            yDir = -1;
        }
        
        
        int i = 0, j = 0;

        if (startHorizontal) {
            for (; Math.abs(i-xLength) > 0; i += xDir) {
                connector.setzeAktuellenPunktAufConnection(new Point(xStart + i, yStart+j));                
            }
            for (; Math.abs(j-yLength) > 0; j += yDir) {
                connector.setzeAktuellenPunktAufConnection(new Point(xStart + i, yStart + j));
            }
        } else {
            for (; Math.abs(j-yLength) > 0; j += yDir) {
                connector.setzeAktuellenPunktAufConnection(new Point(xStart + i, yStart + j));
            }
            for (; Math.abs(i-xLength) > 0; i += xDir) {
                connector.setzeAktuellenPunktAufConnection(new Point(xStart + i, yStart+j));
            }
        }

        connector.setzeEndKnoten(xStart + i, yStart + j);

        CircuitSheet parentSheet = _circuitSheet.findSubCircuit(elementName);
        connector.setParentCircuitSheet(parentSheet);
        if (parentSheet != _circuitSheet) {
            elementName.substring(elementName.lastIndexOf('#') + 1, elementName.length());
        }

        connector.setParentCircuitSheet(parentSheet);

//        try {
//            newElement.setNewNameChecked(truncatedElementName);
//        } catch (NameAlreadyExistsException ex) {
//            newElement.deleteComponent();
//            throw ex;
//        }
        // // to update any newly connected labels:
        NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.LK_AND_RELUCTANCE), getElementLK());
        NetlistGeneral.fabricNetzlistComplete(getConnection(ConnectorType.CONTROL), getElementCONTROL());
        NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.THERMAL), getElementTHERM());

        this.registerChangeWithNetlistUpdate();
        _visibleCircuitSheet.repaint();
        return connector;
    }

    public AbstractBlockInterface externalCreateAndPlaceNewElement(final String elementName, final AbstractTypeInfo elementCategory,
            final int posX, final int posY)
            throws NameAlreadyExistsException {
        AbstractBlockInterface newElement = AbstractTypeInfo.fabricNew(elementCategory);

        CircuitSheet parentSheet = _circuitSheet.findSubCircuit(elementName);
        newElement.setParentCircuitSheet(parentSheet);
        String truncatedElementName = elementName;
        if (parentSheet != _circuitSheet) {
            truncatedElementName = elementName.substring(elementName.lastIndexOf('#') + 1, elementName.length());
        }
        try {
            newElement.setNewNameChecked(truncatedElementName);
        } catch (NameAlreadyExistsException ex) {
            newElement.deleteComponent();
            throw ex;
        }

        //place the new component
        newElement.setPositionWithoutUndo(posX, posY);
        _sea._typElement = null;
        
        Set<ConnectorType> connectorTypes = new LinkedHashSet<ConnectorType>();
        for(TerminalInterface term : newElement.getAllTerminals()) {
            switch(((AbstractTerminal) term).getCategory()) {
                case CONTROL:
                    connectorTypes.add(ConnectorType.CONTROL);
                    break;
                case THERMAL:
                    connectorTypes.add(ConnectorType.THERMAL);
                    break;
                default:
                    connectorTypes.add(ConnectorType.LK_AND_RELUCTANCE);                    
                    break;
            }
        }
            
        for(ConnectorType ct : connectorTypes) {
            NetlistGeneral.fabricNetzlistComponentLabelUpdate(newElement, ct);
        }        
        setDirtyFlag();
                
        _visibleCircuitSheet.repaint();
        return newElement;
    }

    /**
     *
     * @param movePoint the position where to move the selection. Be careful:
     * the start position of the movement is involved!
     */
    private void moveSelectedComponents(final Point movePoint) {
        for (AbstractCircuitSheetComponent comp : _selectedComponents) {
            comp.moveComponent(new Point(movePoint.x, movePoint.y));
        }
    }

    @Override
    public void mouseDragged(final MouseEvent mouseEvent) {

        if (_mouseMoveMode == MouseMoveMode.MOVE_COMPONENTS) {
            _singleComponentMouseDrag = true;
            _dragStartTime = System.currentTimeMillis();
            final Point movePoint = findRasterPoint(mouseEvent);
            final Point movePosition = new Point(movePoint.x - _moveStartPoint.x, movePoint.y - _moveStartPoint.y);
            this.moveSelectedComponents(movePosition);
            _visibleCircuitSheet.repaint();
        }

        if (_mouseMoveMode == MouseMoveMode.MOVE_TEXT) {
            _selectedTextFieldToMove._textInfo.setNewRelativePosition(new Point(mouseEvent.getX(), mouseEvent.getY()));
            _visibleCircuitSheet.repaint();
            _singleComponentMouseDrag = true;
            _dragStartTime = System.currentTimeMillis();
        }

        if (!simulatorActivated) {
            return;
        }

        int dpix = AbstractCircuitSheetComponent.dpix;
        // // Determination of coordinates with snap function:
        int mx = mouseEvent.getX(), my = mouseEvent.getY(), px = mx / dpix, py = my / dpix;
        double pxd = mx * 1.0 / dpix, pyd = my * 1.0 / dpix;
        if (Math.abs((px + 1) - pxd) < CLICK_RADIUS_RELATIVE) {
            px++;
        }
        if (Math.abs((py + 1) - pyd) < CLICK_RADIUS_RELATIVE) {
            py++;
        }
        //---------------------------
        // // Marking rectangle to define larger areas with the mouse:
        if (_mouseMoveMode == MouseMoveMode.SELECT_WINDOW) {
            x2markRe = mx;
            y2markRe = my;
            _visibleCircuitSheet.repaint();
        }

    }

    public void mouseReleased(final MouseEvent me) {
        if (!simulatorActivated) {
            return;
        }

        if (_singleComponentMouseDrag) {
            if (System.currentTimeMillis() - _dragStartTime > 50) {
                if (!_selectedComponents.isEmpty()) {
                    mouseReleaseSelectedGroup();
                }
                if (_mouseMoveMode == MouseMoveMode.MOVE_TEXT) {
                    mouseReleaseText(new Point(me.getPoint().x, me.getPoint().y));
                }
            }

        }

        // // all elements and connections contained in the marking rectangle must be set to EDIT mode,
        // // everything outside is set to 'DONE' (deactivated for processing) -->
        //
        if (_mouseMoveMode == MouseMoveMode.SELECT_WINDOW && (me.getModifiersEx() != MouseEvent.BUTTON3_DOWN_MASK)) {  // // --> no reaction when pressing the right mouse button
            _selectedComponents.clear();
            _selectedComponents.addAll(_visibleCircuitSheet.getElementsInRectangle(x1markRe, y1markRe, x2markRe, y2markRe));
            _mouseMoveMode = MouseMoveMode.NONE;
            _singleComponentMouseDrag = false;
        }
        _visibleCircuitSheet.repaint();
    }

    @Override
    public void mouseClicked(final MouseEvent me) {
        // nothing todo
    }

    @Override
    public void mouseEntered(final MouseEvent me) {
        _visibleCircuitSheet.requestFocusInWindow();  // // This allows KeyEvents to be processed in MainWindow()
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(SchematicEditor2.class.getName()).log(Level.SEVERE, null, ex);
        }

        testCreateNewComponent();
    }

    @Override
    public void mouseExited(final MouseEvent mouseEvent) {
        // nothing todo
    }

    public void deleteAllComponents(final Collection<AbstractCircuitSheetComponent> toDelete) {
        for (AbstractCircuitSheetComponent comp : toDelete) {
            comp.deleteComponent();
        }
    }

    public void deleteSelectedComponents() {
        _elementsJustInitialized = false;
        deleteAllComponents(_selectedComponents);
        _mouseMoveMode = MouseMoveMode.NONE;
        _singleComponentMouseDrag = false;
        _sea._typElement = null;
        _selectedComponents.clear();
        this.setDirtyFlag();
        _visibleCircuitSheet.repaint();
    }

    public void deleteSelectedComponentsWithUndo() {
        if (_selectedComponents.isEmpty()) {
            return;
        }

        _elementsJustInitialized = false;
        _mouseMoveMode = MouseMoveMode.NONE;
        _singleComponentMouseDrag = false;

        GroupableUndoManager.GroupUndoStart groupStart = new GroupableUndoManager.GroupUndoStart();
        AbstractUndoGenericModel.undoManager.addEdit(groupStart);

        DeleteComponentUndoAction undoAction = new DeleteComponentUndoAction(_selectedComponents);
        AbstractUndoGenericModel.undoManager.addEdit(undoAction);

        for (AbstractCircuitSheetComponent comp : _selectedComponents) {
            comp.deleteComponent();
        }

        GroupableUndoManager.GroupUndoStop groupStop = new GroupableUndoManager.GroupUndoStop(groupStart);
        if (_selectedComponents.size() == 1) {
            groupStop.setParentEditForInfo(undoAction);
        }
        AbstractUndoGenericModel.undoManager.addEdit(groupStop);
        _mouseMoveMode = MouseMoveMode.NONE;
        _singleComponentMouseDrag = false;
        _sea._typElement = null;
        _selectedComponents.clear();
        this.setDirtyFlag();
        _visibleCircuitSheet.repaint();
    }

    //for GeckoSCRIPT
    public void deleteComponent(final AbstractBlockInterface component) {
        component.deleteComponent();
        this.setDirtyFlag();
        _visibleCircuitSheet.repaint();
    }

    // // when labels are renamed or deleted (e.g. when moving or deleting elements),
    // // and these labels are registered by C_VOLT, then C_VOLT should be set to 'not defined'
    // // the same applies to TH_TEMP and labels in THERM
    private void updateNewNetlists() {
        NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.LK_AND_RELUCTANCE), getElementLK());
        NetListLK.fabricExcludingSubcircuits(getConnection(ConnectorType.THERMAL), getElementTHERM());
        NetlistGeneral.fabricNetzlistComplete(getConnection(ConnectorType.CONTROL), getElementCONTROL());
        _circuitSheet.updateAllPotentialCoupables();
    }

    public List<? extends AbstractBlockInterface> getElementInterface(final ConnectorType type) {
        switch (type) {
            case CONTROL:
                return getElementCONTROL();
            case LK_AND_RELUCTANCE:
            case RELUCTANCE:
            case LK:
                return getElementLK();
            case THERMAL:
                return getElementTHERM();
            default:
                assert false : type;
        }
        return null;
    }

    public void updateRenamedLabel(final String originalLabel, String neuerLabel, final ConnectorType renamedLabelType) {        

        if (testRenameLabelWhereOldStillExists(originalLabel, _visibleCircuitSheet.getLocalComponents(renamedLabelType))) {
            return; // if the new label is already present in the model, don't do any update!
        }

        for (AbstractCircuitSheetComponent search : _visibleCircuitSheet.allElements) {
            if (search instanceof PotentialCoupable) {
                final PotentialCoupling potCoup = ((PotentialCoupable) search).getPotentialCoupling();
                if (potCoup.getLinkType() == renamedLabelType || renamedLabelType == ConnectorType.LK && potCoup.getLinkType() == ConnectorType.LK_AND_RELUCTANCE
                        || renamedLabelType == ConnectorType.LK_AND_RELUCTANCE && potCoup.getLinkType() == ConnectorType.LK) {
                    potCoup.renameUpdate(originalLabel, neuerLabel);
                }
            }
        }
    }

    // // (1) An LK switch is renamed --> the correspondingly linked GATE control block must be updated,
    // // so correctly linked to the new LK switch name -->
    // // (2) An LK switch is renamed --> the correspondingly linked PVCHIP-THERM block for loss measurement
    // // needs to be updated, so correctly linked to the new LK switch name -->
    // // (3) The name of LK_KOP2 was renamed --> update any links with couplings LK_M -->
    // // (4) A LK name was renamed --> CURRENT/VIEWMOT - measurements need to be adjusted -->
    // // (5) An LK name is renamed --> Voltage direct control of the LK sources U and I must be adjusted
    // 
    // // (1) A THERM name was renamed --> FLOW - measurements must be adjusted -->
    // 
    public void updateComponentCouplings(final String nameVorher, final String neuerName) {
        for (ComponentCoupable element : _circuitSheet.getAllElements().getClassFromContainer(ComponentCoupable.class)) {
            final ComponentCoupling coupling = element.getComponentCoupling();
            coupling.updateCouplingParameterStrings();
        }
    }

    public void verschiebeAllesImBearbeitungsModus() {
        _mouseMoveMode = MouseMoveMode.MOVE_COMPONENTS;

        final List<TerminalInterface> allTerminals = new ArrayList<TerminalInterface>();

        for (AbstractCircuitSheetComponent comp : _selectedComponents) {
            if (comp instanceof ComponentTerminable) {
                final ComponentTerminable terminable = (ComponentTerminable) comp;
                allTerminals.addAll(terminable.getAllTerminals());
            }
        }

        _moveStartPoint = findMoveAnchorPoint(allTerminals);

    }

    public void kopiereAllesImBearbeitungsModus() {
        _elementsJustInitialized = true;

        final List<String> vecCopiedLabelsALL = new ArrayList<String>();
        final List<String> allOriginalModelLabels = _visibleCircuitSheet.getAllLocalSheetLabels();

        // new copied LK-elements get new names, refering CONTROL-block have to point ot the new names --> 
        final List<AbstractCircuitSheetComponent> exchangeNew = new ArrayList<AbstractCircuitSheetComponent>();
        final List<TerminalInterface> allTerminals = new ArrayList<TerminalInterface>();

        final Random shiftRandom = new Random(System.currentTimeMillis());
        final long shiftValue = shiftRandom.nextLong();

        for (AbstractCircuitSheetComponent elem : _selectedComponents.toArray(new AbstractCircuitSheetComponent[0])) {
            final AbstractCircuitSheetComponent elementAKTUELL = elem.copyFabric(shiftValue);
            _selectedComponents.add(elementAKTUELL);
            elementAKTUELL.setParentCircuitSheet(_visibleCircuitSheet);

            _selectedComponents.remove(elem);
            elem.absetzenElement();

            if (elem instanceof AbstractBlockInterface) {
                exchangeNew.add((AbstractBlockInterface) elementAKTUELL);
            }

            if (elem instanceof ComponentTerminable) {
                final ComponentTerminable terminable = (ComponentTerminable) elem;
                vecCopiedLabelsALL.addAll(terminable.getAllNodeLabels());
                allTerminals.addAll(terminable.getAllTerminals());
            }
        }

        for (AbstractCircuitSheetComponent elem : exchangeNew) {
            if (elem instanceof ComponentCoupable) {
                ((ComponentCoupable) elem).getComponentCoupling().trySetCopyReference(exchangeNew);
            }
        }

        _createComponentAction = new CreateComponentUndoAction(exchangeNew);
        removeDuplicates(vecCopiedLabelsALL);
        renameConflictingLabels(exchangeNew, allOriginalModelLabels, vecCopiedLabelsALL);

        _moveStartPoint = findMoveAnchorPoint(allTerminals);
        if (_mouseMoveMode == MouseMoveMode.SELECT_WINDOW) {
            // // Elements for copying were selected using a rectangle (usually done like this) -->
            _mouseMoveMode = MouseMoveMode.NONE;
            _singleComponentMouseDrag = false;
        } else // // A single element or a single connection was clicked to copy
        // // ... (rather unusual but cannot be ruled out) -->
        {
            if (!allTerminals.isEmpty()) {
                _mouseMoveMode = MouseMoveMode.MOVE_COMPONENTS;
            }
        }
    }

    private static Point findMoveAnchorPoint(final List<TerminalInterface> allInvolvedTerminals) {
        int xLeftUpperCorner = Integer.MAX_VALUE;
        int yLeftUpperCorner = Integer.MAX_VALUE;

        for (TerminalInterface term : allInvolvedTerminals) {
            final Point point = term.getPosition();

            if (point.x < 0 || point.y < 0) { // don't remove. Thermal ambient temperature has a node at negative coordinates!
                continue;
            }

            xLeftUpperCorner = Math.min(xLeftUpperCorner, point.x);
            yLeftUpperCorner = Math.min(yLeftUpperCorner, point.y);
        }
        return new Point(xLeftUpperCorner, yLeftUpperCorner);
    }

    private void renameConflictingLabels(final Collection<? extends AbstractCircuitSheetComponent> exchangeNew,
            final List<String> allNetlistLabels, final List<String> copiedLabels) {
        for (AbstractCircuitSheetComponent search : exchangeNew) {
            if (search instanceof PotentialCoupable) {
                ((PotentialCoupable) search).getPotentialCoupling().saveLabelsBeforeCopyRename();
            }
        }

        final int endIndex = findPossibleEndIndex(copiedLabels, allNetlistLabels);
        renameComponentLabels(exchangeNew, endIndex);

        for (AbstractCircuitSheetComponent search : exchangeNew) {
            if (search instanceof PotentialCoupable) {
                ((PotentialCoupable) search).getPotentialCoupling().tryFindChangedLabels(exchangeNew, endIndex);
            }
        }
    }

    /*
     * // CLIPBOARD? --> GZIPOutputStream out1 = new GZIPOutputStream(new FileOutputStream(Typ.DATNAM)); BufferedWriter out =
     * new BufferedWriter(new OutputStreamWriter(out1)); GZIPInputStream in1 = new GZIPInputStream(new
     * FileInputStream(Typ.DATNAM)); BufferedReader in = new BufferedReader(new InputStreamReader(in1));
     */
    public void export_allesImBearbeitungsModus() {
        try {
            final Clipboard clipBoard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            final Transferable transferable = clipBoard.getContents(null);
            final String transferString = this.writeSelectedElementsToASCIIString();
            final StringSelection tr1 = new StringSelection(transferString);  // // write the selected elements as a string into the clipboard
            clipBoard.setContents(tr1, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        _mouseMoveMode = MouseMoveMode.NONE;
        _visibleCircuitSheet.repaint();
    }

    private String writeSelectedElementsToASCIIString() {
        final StringBuffer stringBuffer = new StringBuffer();
        int kopLK = 0;  // Zaehler

        final Set<AbstractCircuitSheetComponent> allComponentsIncludingSubs = new LinkedHashSet<AbstractCircuitSheetComponent>();
        allComponentsIncludingSubs.addAll(_selectedComponents);

        for (AbstractCircuitSheetComponent searchSub : _selectedComponents) {
            if (searchSub instanceof SubcircuitBlock) {
                SubcircuitBlock subBlock = (SubcircuitBlock) searchSub;
                allComponentsIncludingSubs.addAll(subBlock._myCircuitSheet.getLocalRecursiveSubComponents());
            }
        }

        for (AbstractCircuitSheetComponent elem : allComponentsIncludingSubs) {
            stringBuffer.append("\n" + elem.getExportImportCharacters() + " (" + kopLK + ")\n");
            //elem.getIdentifier().setExportIdentifier();
            elem.exportASCII(stringBuffer);
            //elem.getIdentifier().clearExportIdentifier();
            kopLK++;
        }
        _selectedComponents.clear();
        return stringBuffer.toString();
    }

    public void importFromClipboard() {
        try {
            _selectedComponents.clear();
            final Clipboard clipBoard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            final Transferable transferable = clipBoard.getContents(null);
            final Object readObject = transferable.getTransferData(DataFlavor.stringFlavor);
            final BufferedReader bufReader = new BufferedReader(new StringReader(readObject.toString()));
            final StringBuffer asciiReadFromClipBoard = new StringBuffer();

            for (String line = bufReader.readLine(); line != null; line = bufReader.readLine()) {
                asciiReadFromClipBoard.append(line);
                asciiReadFromClipBoard.append('\n');
            }
            bufReader.close();

            this.readSelectedElementsFromASCIIString(asciiReadFromClipBoard.toString().split("\n"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void readSelectedElementsFromASCIIString(final String[] ascii) {
        final ProjectData datenSpeicher = new ProjectData(ascii, false, null);
        datenSpeicher.shiftComponentReferences();

        _elementsJustInitialized = true;

        final List<String> allOriginalLabels = _visibleCircuitSheet.getAllLocalSheetLabels();
        final List<String> copiedLabels = new ArrayList<String>();
        processElements(datenSpeicher._allSheetComponents, copiedLabels);

        for (AbstractCircuitSheetComponent comp : datenSpeicher._allSheetComponents) {
            String nameOfRootSub = null;
            if (_visibleCircuitSheet instanceof SubCircuitSheet) {
                nameOfRootSub = ((SubCircuitSheet) _visibleCircuitSheet).getCircuitSheetName();
            }
            comp.findAndSetReferenceToParentSheet2(datenSpeicher.allSubCircuitBlocks, nameOfRootSub);
        }

        removeDuplicates(copiedLabels);

        for (AbstractCircuitSheetComponent comp : datenSpeicher._allSheetComponents) {
            if (comp instanceof ComponentCoupable) {
                final ComponentCoupable coupable = (ComponentCoupable) comp;
                coupable.getComponentCoupling().refreshCoupledReferences(getBlockInterfaceComponents());
            }
        }

        for (AbstractCircuitSheetComponent deselectTest
                : datenSpeicher._allSheetComponents.toArray(new AbstractCircuitSheetComponent[0])) {
            if (deselectTest.getParentCircuitSheet() != _visibleCircuitSheet) {
                _selectedComponents.remove(deselectTest);
            }
        }

        final List<AbstractCircuitSheetComponent> visibleComponents = new ArrayList<AbstractCircuitSheetComponent>();
        for (AbstractCircuitSheetComponent testInsert : datenSpeicher._allSheetComponents) {
            if (testInsert.getParentCircuitSheet() == _visibleCircuitSheet) {
                visibleComponents.add(testInsert);
            }
        }

        if (isLabelRenameRequired(allOriginalLabels, copiedLabels)) {
            renameConflictingLabels(datenSpeicher._allSheetComponents, allOriginalLabels, copiedLabels);
        }

        _mouseMoveMode = MouseMoveMode.MOVE_COMPONENTS;
    }

    private void processElements(final Collection<? extends AbstractCircuitSheetComponent> dsc, List<String> copiedLabelNames) {
        for (AbstractCircuitSheetComponent importedElement : dsc) {
            if (importedElement instanceof AbstractBlockInterface) {
                final AbstractBlockInterface block = (AbstractBlockInterface) importedElement;

                final String originalName = block.getStringID();
                importedElement.setParentCircuitSheet(_visibleCircuitSheet);
                block.getIDStringDialog().setRandomStringID();

                try {
                    block.setNewNameChecked(originalName);
                } catch (NameAlreadyExistsException ex) {
                    try {
                        block.setNewNameChecked(IDStringDialog.findUnusedName(originalName));
                    } catch (NameAlreadyExistsException ex1) {
                        Logger.getLogger(SchematicEditor2.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }

            _selectedComponents.add(importedElement);
            importedElement.setParentCircuitSheet(_visibleCircuitSheet);
            if (importedElement instanceof ComponentTerminable) {
                copiedLabelNames.addAll(((ComponentTerminable) importedElement).getAllNodeLabels());
            }
            importedElement.setParentCircuitSheet(_visibleCircuitSheet);
        }
    }

    private void removeDuplicates(final List<String> copiedLabelsCONTROL) {
        for (int i1 = 0; i1 < copiedLabelsCONTROL.size(); i1++) {
            final String s1 = copiedLabelsCONTROL.get(i1);
            for (int i2 = i1 + 1; i2 < copiedLabelsCONTROL.size(); i2++) {
                if (s1.equals(copiedLabelsCONTROL.get(i2))) {
                    copiedLabelsCONTROL.remove(i2);
                    i2--;
                }
            }
        }
    }

    private int findPossibleEndIndex(final List<String> vecCopiedLabelsCONTROL, final List<String> allOrigLabelsCONTROL) {
        boolean nameConflict;
        int endIndex = -1;
        do {
            endIndex++;
            nameConflict = false;
            for (int i1 = 0; i1 < vecCopiedLabelsCONTROL.size(); i1++) {
                final String newLabel = vecCopiedLabelsCONTROL.get(i1) + "." + endIndex;
                for (int i2 = 0; i2 < allOrigLabelsCONTROL.size(); i2++) {
                    if (allOrigLabelsCONTROL.get(i2).equals(newLabel)) {
                        nameConflict = true;
                    }
                }
            }
        } while (nameConflict);
        return endIndex;
    }

    private void renameComponentLabels(final Collection<? extends AbstractCircuitSheetComponent> exchangeNew,
            final int endIndex) {

        // some components have more therminals with the same label
        // object. Therefore, "sparsify" first, i.e. remove duplicates.
        // this is done with a set, where objects can be inserted only once.        
        final Set<CircuitLabel> allLabelsSet = new HashSet<CircuitLabel>();
        for (AbstractCircuitSheetComponent elementNew : exchangeNew) {
            if (elementNew instanceof ComponentTerminable) {
                for (TerminalInterface term : ((ComponentTerminable) elementNew).getAllTerminals()) {
                    allLabelsSet.add(term.getLabelObject());
                }
            }
        }

        for (CircuitLabel label : allLabelsSet) {
            final String lab = label.getLabelString();
            if (!lab.isEmpty()) { // empty labels don't make probems here!
                label.setLabel(lab + "." + endIndex);
            }
        }
    }

    public void setAntialiasing(final boolean selected) {
        antialiasing = selected;
        _visibleCircuitSheet.repaint();
        // save to the properties file:                
        GeckoSim.applicationProps.setProperty("ANTI_ALIASING", ((Boolean) selected).toString());
    }

    private final class CreateConnectionUndoAction implements UndoableEdit {

        Connection _verb;
        CircuitSheet _parentSheet;

        private CreateConnectionUndoAction(final Connection verb) {
            _verb = verb;
            assert verb != null;
        }

        @Override
        public void undo() {
            _parentSheet = _verb.getParentCircuitSheet();
            final List<AbstractCircuitSheetComponent> toDeleteList = new ArrayList<AbstractCircuitSheetComponent>();
            toDeleteList.add(_verb);
            deleteAllComponents(toDeleteList);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() {
            _verb.setParentCircuitSheet(_parentSheet);
//////            _newElement.absetzenElement();
            _elementsJustInitialized = false;
            _selectedComponents.clear();
            _mouseMoveMode = MouseMoveMode.NONE;
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            // nothing todo
        }

        @Override
        public boolean addEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Connection created";
        }

        @Override
        public String getUndoPresentationName() {
            return "Delete connection.";
        }

        @Override
        public String getRedoPresentationName() {
            return "Re-create connection.";
        }
    }

    public final class DeleteComponentUndoAction implements UndoableEdit {

        private final List<AbstractCircuitSheetComponent> _toDeleteElements = new ArrayList<AbstractCircuitSheetComponent>();

        private DeleteComponentUndoAction(final Collection<? extends AbstractCircuitSheetComponent> toDeleteElements) {
            _toDeleteElements.addAll(toDeleteElements);
            assert _toDeleteElements != null;
        }

        @Override
        public void redo() {
            final List<AbstractCircuitSheetComponent> toDeleteList = new ArrayList<AbstractCircuitSheetComponent>();
            toDeleteList.addAll(_toDeleteElements);
            deleteAllComponents(toDeleteList);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            for (AbstractCircuitSheetComponent comp : _toDeleteElements) {

                insertNewElement(comp);

                if (comp instanceof AbstractBlockInterface) {
                    ((AbstractBlockInterface) comp).getIDStringDialog().refreshNameList();
                }

                if (comp instanceof ComponentCoupable) {
                    final ComponentCoupling coupling = ((ComponentCoupable) comp).getComponentCoupling();
                    coupling.refreshCouplingReferences();
                }
            }

            _elementsJustInitialized = false;
            _selectedComponents.clear();
            _mouseMoveMode = MouseMoveMode.NONE;
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            _toDeleteElements.clear();
        }

        @Override
        public boolean addEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Component: " + getDescription() + " created!";
        }

        @Override
        public String getUndoPresentationName() {
            return "Delete component: " + getDescription();
        }

        @Override
        public String getRedoPresentationName() {
            return "Create component: " + getDescription();
        }

        private String getDescription() {
            if (_toDeleteElements.size() == 1) {
                return _toDeleteElements.get(0).toString();
            } else {
                return "Group of components";
            }
        }
    }

    private final class CreateComponentUndoAction implements UndoableEdit {

        private final List<? extends AbstractCircuitSheetComponent> _newElements;

        private CreateComponentUndoAction(final List<? extends AbstractCircuitSheetComponent> newElements) {
            _newElements = new ArrayList<AbstractCircuitSheetComponent>(newElements);
            assert _newElements != null;
        }

        @Override
        public void undo() {
            final List<AbstractCircuitSheetComponent> toDeleteList = new ArrayList<AbstractCircuitSheetComponent>();
            toDeleteList.addAll(_newElements);
            deleteAllComponents(toDeleteList);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void redo() {
            for (AbstractCircuitSheetComponent comp : _newElements) {
                insertNewElement(comp);
                comp.absetzenElement();
                if (comp instanceof AbstractBlockInterface) {
                    ((AbstractBlockInterface) comp).getIDStringDialog().refreshNameList();
                }
                if (comp instanceof ComponentCoupable) {
                    final ComponentCoupling coupling = ((ComponentCoupable) comp).getComponentCoupling();
                    coupling.refreshCouplingReferences();
                }
            }

            _elementsJustInitialized = false;
            _selectedComponents.clear();
            _mouseMoveMode = MouseMoveMode.NONE;
        }

        @Override
        public boolean canRedo() {
            return true;
        }

        @Override
        public void die() {
            _newElements.clear();
        }

        @Override
        public boolean addEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean replaceEdit(final UndoableEdit anEdit) {
            return false;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            return "Component: " + getDescription() + " created!";
        }

        @Override
        public String getUndoPresentationName() {
            return "Create component: " + getDescription();
        }

        @Override
        public String getRedoPresentationName() {
            return "Delete component: " + getDescription();
        }

        private String getDescription() {
            if (_newElements.size() == 1) {
                return _newElements.get(0).toString();
            } else {
                return "Group of components";
            }
        }
    }

    void checkNameOptParameters() {
        for (AbstractBlockInterface block : getBlockInterfaceComponents()) {
            try {
                block.checkNameOptParameter();
            } catch (Exception ex) {
                throw new RuntimeException("Error in component " + block.getStringID() + ":\n" + ex.getMessage());
            }
        }
    }
}
