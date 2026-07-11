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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.general.MainWindow;
import ch.technokrat.gecko.geckocircuits.general.GeckoFile;
import ch.technokrat.gecko.geckocircuits.general.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculationSimple;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossProperties;
import ch.technokrat.gecko.geckocircuits.control.Operationable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for semiconductor components (diodes, switches).
 * Provides on/off resistance parameters, current-dependent loss coefficients,
 * parallel device count, and loss file management.
 *
 * <p>Parameter array indices: index 2 = on-resistance, index 3 = off-resistance,
 * index 6 = on current coefficient (kOn), index 7 = off current coefficient (kOff),
 * index 12 = number of paralleled devices.</p>
 *
 * @author andy diodes and switches are abstractSemiconductors.
 */
public abstract class AbstractSemiconductor extends AbstractTwoPortPowerCircuitBlock implements SemiconductorLossCalculatable, CurrentMeasurable,
        DirectVoltageMeasurable, Operationable {

    public final UserParameter<Double> _onResistance = UserParameter.Builder.
            <Double>start("onResistance", AbstractSwitch.RD_ON_DEFAULT).
            longName(I18nKeys.ON_RESISTANCE).
            shortName("rON").
            unit("Ohm").
            arrayIndex(this, getOnResistanceIndex()).
            build();
    public final UserParameter<Double> _offResistance = UserParameter.Builder.
            <Double>start("offResistance", AbstractSwitch.RD_OFF_DEFAULT).
            longName(I18nKeys.OFF_RESISTANCE).
            shortName("rOFF").
            unit("Ohm").
            arrayIndex(this, getOffResistanceIndex()).
            build();
    public final UserParameter<Double> kOn = UserParameter.Builder.
            <Double>start("kON", 20e-6).
            longName(I18nKeys.CURRENT_DEPENDENT_COEFFICIENT_ON).
            shortName("k_on").
            arrayIndex(this, 6).
            unit("Ws/A").
            build();
    public final UserParameter<Double> kOff = UserParameter.Builder.
            <Double>start("kOFF", 30e-6).
            longName(I18nKeys.CURRENT_DEPENDENT_COEFFICIENT_OFF).
            shortName("k_off").
            unit("Ws/A").
            arrayIndex(this, 7).
            build();
    public final UserParameter<Integer> numberParalleled = UserParameter.Builder.
            <Integer>start("numberParalleled", 1).
            longName(I18nKeys.NUMBER_OF_DEVICES_PARALLEL).
            shortName("paralleled").
            unit("unitless").
            arrayIndex(this, 12).
            build();
    public final UserParameter<Double> uK = UserParameter.Builder.
            <Double>start("uSWnorm", LossCalculationSimple.UK_DEFAULT_VALUE).
            longName(I18nKeys.BLOCKING_VOLTAGE_FOR_SWITCHING).
            shortName("uK").
            arrayIndex(this, -1).
            unit("V").
            build();

    @SuppressWarnings("this-escape")
    public AbstractSemiconductor() {                               
    }

    /**
     * this "hacks" are just for backwards-compatibility. In the old
     * GeckoCIRCUITS versions, the ideal switch has an on resistance parameter
     * index of 1, in all other switches it is 2. The same "shift" applies for
     * off-resistance.
     *
     * @return
     */
    int getOnResistanceIndex() {
        return 2;
    }

    /**
     * this "hacks" are just for backwards-compatibility. In the old
     * GeckoCIRCUITS versions, the ideal switch has an on resistance parameter
     * index of 1, in all other switches it is 2. The same "shift" applies for
     * off-resistance.
     *
     * @return
     */
    int getOffResistanceIndex() {
        return 3;
    }

    /**
     * Hook for subclasses to add external files (e.g. loss data files).
     *
     * @param _newFilesToAdd list of files to add
     */
    public void addFiles(List<GeckoFile> _newFilesToAdd) {
    }

    @SuppressWarnings("unchecked")
    /**
     * Returns the list of files associated with this semiconductor (e.g. loss
     * files), or an empty list if the component is not loss-calculatable.
     *
     * @return unmodifiable list of associated Gecko files
     */
    public List<GeckoFile> getFiles() {
        if (this instanceof LossCalculatable) {
            List<GeckoFile> returnValue = new ArrayList<GeckoFile>();
            GeckoFile lossFile = ((LossProperties) ((LossCalculatable) this).getLossCalculation()).getDetailedLosses().lossFile;
            returnValue.add(lossFile);
            return returnValue;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public void removeLocalComponentFiles(List<GeckoFile> filesToRemove) {
    }

    @Override
    /**
     * Returns the list of scriptable operations for this semiconductor, such
     * as setting a loss file at runtime.
     *
     * @return unmodifiable list of operation interfaces
     */
    public List<OperationInterface> getOperationEnumInterfaces() {
        List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        returnValue.add(new OperationInterface("setLossFile", I18nKeys.SET_LOSS_FILE_DOC) {
            @Override
            public Object doOperation(final Object parameterValue) {
                if(!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Error: argument must be a String containing a file name");
                }
                File lossFile = new File((String) parameterValue);

                //if it doesn't exist, try first to see if it is in the same directory as the currently open model file
                if (!lossFile.exists()) {
                    final File modelFile = new File(MainWindow.getOpenFileName());
                    final String currentModelDirectory = modelFile.getParent();
                    final String correctedFileName = currentModelDirectory + System.getProperty("file.separator") + parameterValue;
                    lossFile = new File(correctedFileName);
                }

                if (lossFile.exists() && !lossFile.isDirectory()) {
                    final String foundLossFileName = lossFile.getAbsolutePath();
                    if (foundLossFileName.endsWith(".scl")) {                                                
                        ((LossProperties) getLossCalculation()).getDetailedLosses().readLossesFromFileAndSetDetailedLossType(foundLossFileName);                        
                    } else {                        
                        throw new RuntimeException("Invalid loss file " + foundLossFileName);
                    }
                } else {
                    throw new RuntimeException("Specified loss file: " + parameterValue + " does not exist.");
                }
                return null;
            }
        });

        return Collections.unmodifiableList(returnValue);
    }
}
