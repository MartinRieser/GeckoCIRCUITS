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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import ch.technokrat.gecko.geckocircuits.circuit.SchematicTextInfo;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractSemiconductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Diode;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.MOSFET;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SemiconductorLossCalculatable;
import ch.technokrat.modelviewcontrol.ModelMVC;

/**
 * Central loss-calculation configuration for semiconductor components.
 * Holds a loss type ({@link LossCalculationDetail}) and delegates to the
 * corresponding simple or detailed loss calculator.
 */
public final class LossProperties implements AbstractLossCalculatorFabric {

    public final ModelMVC<LossCalculationDetail> _lossType = new ModelMVC<LossCalculationDetail>(LossCalculationDetail.SIMPLE,
            "Loss calculation level: ");
    // Properties of the semiconductor:
    // // if detailed semiconductor loss characteristics are specified -->
    //        
    private final AbstractCircuitBlockInterface _parent;    

    /**
     * Creates a new loss configuration for the given semiconductor.
     * @param parent the semiconductor component whose losses are configured
     */
    public LossProperties(final AbstractSemiconductor parent) {
        _lossCalculationDetailed = new LossCalculationDetailed(parent, this);
        _lossCalculationSimple = new LossCalculationSimple(parent);
        _parent = parent;
    }
    private final LossCalculationSimple _lossCalculationSimple;
    public final LossCalculationDetailed _lossCalculationDetailed;

    public LossCalculationDetailed getDetailedLosses() {
        return _lossCalculationDetailed;
    }
    

    /**
     * Exports the loss configuration to ASCII format.
     * @param ascii the buffer to append serialised data to
     */
    public void exportASCII(final StringBuffer ascii) {
        ascii.append("\n<Verluste>");

        _lossCalculationDetailed.exportASCII(ascii);
        ProjectData.appendAsString(ascii.append("\nverlustTyp"), _lossType.getValue().getOldGeckoCIRCUITSOrdinal());
        ascii.append("\n<\\Verluste>");
    }

    /**
     * Imports the loss configuration from an ASCII token map.
     * @param tokenMap the token map containing serialised loss data
     * @return true if loading succeeded, false on error
     */
    public boolean importASCII(final TokenMap tokenMap) {
        _lossCalculationDetailed.importASCII(tokenMap);
        _lossType.setValue(LossCalculationDetail.getFromDeprecatedFileVersion(tokenMap.readDataLine("verlustTyp", 1)));
        return true;  // // 'load OK'--> true; 'Loading error' --> false
    }

    public void copyPropertiesFrom(final LossProperties origLosses) {
        _lossType.setValue(origLosses._lossType.getValue());
        _lossCalculationSimple.copyPropertiesFrom(origLosses._lossCalculationSimple);
        _lossCalculationDetailed.copyPropertiesFrom(origLosses._lossCalculationDetailed);
    }

    public void addTextInfoValue(final SchematicTextInfo textInfo) {

        // // does the file with the loss description even exist?
        final boolean isLossFileOk = _lossCalculationDetailed.checkLinkToSemiconductorFile();
        if (_lossType.getValue() == LossCalculationDetail.DETAILED) {
            if (isLossFileOk) {
                textInfo.addParameter(_lossCalculationDetailed.lossFile.getName());
            } else {
                textInfo.addErrorValue("loss-file not found");
            }
        }
    }

    /**
     * Sets the loss calculation detail level.
     * @param lossCalculationDetail the detail level to use
     */
    public void setLossType(final LossCalculationDetail lossCalculationDetail) {
        _lossType.setValue(lossCalculationDetail);
    }

    /**
     * Returns the current loss calculation detail level.
     * @return the current loss type
     */
    public LossCalculationDetail getLossType() {
        return _lossType.getValue();
    }

    /**
     * Wraps a loss calculator to scale losses for paralleled devices.
     * Reduces the current by the number of parallel devices before delegating,
     * then multiplies the result back.
     */
    private class LossCalculatorParallelWrapper implements AbstractLossCalculator {

        final AbstractLossCalculator _wrapped;
        private double _totalLosses;
        protected int _numberParalleled = 1;

        /**
         * @param toWrap the calculator to delegate to
         * @param numberParalleled the number of parallel devices
         */
        public LossCalculatorParallelWrapper(final AbstractLossCalculator toWrap, final int numberParalleled) {
            super();
            _wrapped = toWrap;
            _numberParalleled = numberParalleled;
        }

        @Override
        public void calcLosses(final double current, final double temperature, final double deltaT) {
            final double reducedCurrent = current / _numberParalleled;
            _wrapped.calcLosses(reducedCurrent, temperature, deltaT);
            _totalLosses = _wrapped.getTotalLosses() * _numberParalleled;
        }

        @Override
        public double getTotalLosses() {
            return _totalLosses;
        }
    }

    /**
     * Extension of {@link LossCalculatorParallelWrapper} that also provides
     * separate switching and conduction loss values via {@link LossCalculationSplittable}.
     */
    private class LossCalculatorParallelWrapperWithSplit extends LossCalculatorParallelWrapper
            implements LossCalculationSplittable {

        /**
         * @param toWrap the calculator to delegate to
         * @param numberParalleld the number of parallel devices
         */
        public LossCalculatorParallelWrapperWithSplit(final AbstractLossCalculator toWrap, final int numberParalleld) {
            super(toWrap, numberParalleld);
            assert toWrap instanceof LossCalculationSplittable;
        }

        @Override
        public double getSwitchingLoss() {
            return ((LossCalculationSplittable) _wrapped).getSwitchingLoss() * _numberParalleled;
        }

        @Override
        public double getConductionLoss() {
            return ((LossCalculationSplittable) _wrapped).getConductionLoss() * _numberParalleled;
        }

    }

    /**
     * Combines the losses of a MOSFET with those of its anti-parallel diode,
     * providing aggregated switching and conduction losses.
     */
    private final class LossCalculatorAdditionalDiode implements AbstractLossCalculator, LossCalculationSplittable {

        private final Diode _diode;
        private final AbstractLossCalculator _original;
        private final AbstractLossCalculator _diodeLosses;
        private final LossCalculationSplittable _splittable;
        private double _conductionLosses;
        private double _switchingLosses;

        /**
         * @param original the MOSFET loss calculator
         * @param diode the anti-parallel diode
         */
        public LossCalculatorAdditionalDiode(final AbstractLossCalculator original, final Diode diode) {
            super();
            _diode = diode;
            _original = original;
            assert original instanceof LossCalculationSplittable;
            _splittable = (LossCalculationSplittable) original;
            _diodeLosses = ((SemiconductorLossCalculatable) _diode).getLossCalculation().lossCalculatorFabric();
        }

        @Override
        public void calcLosses(final double current, final double temperature, final double deltaT) {
            _original.calcLosses(current, temperature, deltaT);
            final double diodeCurrent = _diode._currentInAmps;  // aktueller Strom in Diode                                    
            _diodeLosses.calcLosses(diodeCurrent, temperature, deltaT);
            _conductionLosses = _splittable.getConductionLoss() + ((LossCalculationSplittable) _diodeLosses).getConductionLoss();
            _switchingLosses = _splittable.getSwitchingLoss() + ((LossCalculationSplittable) _diodeLosses).getSwitchingLoss();
        }

        @Override
        public double getTotalLosses() {
            return _switchingLosses + _conductionLosses;
        }

        @Override
        public double getSwitchingLoss() {
            return _switchingLosses;
        }

        @Override
        public double getConductionLoss() {
            return _conductionLosses;
        }
    }

    /**
     * Fabricates the appropriate loss calculator based on the current loss type,
     * optionally wrapping it with parallel-device and anti-parallel diode logic.
     * @return a configured loss calculator for the parent semiconductor
     */
    @Override
    public AbstractLossCalculator lossCalculatorFabric() {
        AbstractLossCalculator returnValue = null;

        switch (_lossType.getValue()) {
            case SIMPLE:
                returnValue = _lossCalculationSimple.lossCalculatorFabric();
                break;
            case DETAILED:
                returnValue = _lossCalculationDetailed.lossCalculatorFabric();
                break;
            default:
                assert false;
        }

        if (_parent instanceof MOSFET) {
            final Diode diodeElement = ((MOSFET) _parent).getAntiParallelDiode();
            returnValue = new LossCalculatorAdditionalDiode(returnValue, diodeElement);
        }

        if (_parent instanceof AbstractSemiconductor) {
            int numberParalleled = ((AbstractSemiconductor) _parent).numberParalleled.getValue();
            if (numberParalleled > 1) {
                if (returnValue instanceof LossCalculationSplittable) {
                    return new LossCalculatorParallelWrapperWithSplit(returnValue, numberParalleled);
                } else {
                    return new LossCalculatorParallelWrapper(returnValue, numberParalleled);
                }
            }
        }
        return returnValue;

    }
}
