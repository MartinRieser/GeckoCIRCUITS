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

import ch.technokrat.gecko.geckocircuits.general.SolverType;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class CapacitorCalculator extends CircuitComponent implements AStampable, BStampable,
         DirectCurrentCalculatable, CurrentCalculatable, HistoryUpdatable {

    public static boolean initCapacitor = false;
    private double _capacitance = 100E-6;
    private double _initialValue = 0;
    private VoltageSourceCalculator initVoltageSource;
    // ok, this is a "public" variable... bad, but useful here
    public static boolean capError = false;
    private int _z;
    // Note: _bVector is used at line 207 in registerBVector(), IDE warning is false positive
    private BVector _bVector;
    private boolean _isNonLinear = false;
    private CapacitanceCharacteristic _NonLinearCapacitance;
    private double _newOldCapRatio = 0;
    private double _nonLinearCorrectionCurrent = 0;
    private boolean _capCorrected = false;
    private double _maxAbsVal = 0; //for fast steady state solving

    public CapacitorCalculator(final AbstractCapacitor parent) {
        super(parent);
        _needsOldPotCurrent = true;
        _oldCurrent = 0;
        _capacitance = parent._capacitance.getValue();
        _initialValue = parent._initialValue.getValue();
    }

    @Override
    public void stampMatrixA(double[][] matrix, double dt) {
        //  +C/dt

        if (initCapacitor && _initialValue != 0) {
            initVoltageSource = new VoltageSourceCalculator(-_initialValue, matrixIndices[0], 
                    matrixIndices[1], _z, _componentNumber, _parent);
            initVoltageSource.stampMatrixA(matrix, dt);
        } else {
            double aW = 0;
            if (_solverType == SolverType.SOLVER_BE) {
                aW = _capacitance / dt;
            } else if (_solverType == SolverType.SOLVER_TRZ) {
                aW = 2 * _capacitance / dt;
            } else if (_solverType == SolverType.SOLVER_GS) {
                aW = 1.5 * _capacitance / dt;
            }
            matrix[matrixIndices[0]][matrixIndices[0]] += (+aW);
            matrix[matrixIndices[1]][matrixIndices[1]] += (+aW);
            matrix[matrixIndices[0]][matrixIndices[1]] += (-aW);
            matrix[matrixIndices[1]][matrixIndices[0]] += (-aW);
        }
    }

    @Override
    public final void stampVectorB(double[] b, double t, double dt) {

        if (initCapacitor && _initialValue != 0) {
            initVoltageSource.stampVectorB(b, t, dt);
        } else {
            double bW = 0;
            if (_isNonLinear) {
                if (_solverType == SolverType.SOLVER_BE) {
                    bW = (_capacitance / dt) * (_potential1 - _potential2) + _newOldCapRatio * _oldCurrent;
                } else if (_solverType == SolverType.SOLVER_TRZ) {
                    bW = 2 * (_capacitance / dt) * (_potential1 - _potential2) + _oldCurrent + _newOldCapRatio * _oldCurrent;
                } else if (_solverType == SolverType.SOLVER_GS) {
                    bW = (_capacitance / dt) * (2 * (_potential1 - _potential2) - 0.5 * (_potential1 - _potential2)) + _newOldCapRatio * _oldCurrent;
                }
            } else {
                if (_solverType == SolverType.SOLVER_BE) {
                    bW = (_capacitance / dt) * (_potential1 - _potential2);
                } else if (_solverType == SolverType.SOLVER_TRZ) {
                    bW = 2 * (_capacitance / dt) * (_potential1 - _potential2) + _oldCurrent;
                } else if (_solverType == SolverType.SOLVER_GS) {
                    bW = (_capacitance / dt) * (2 * (_potential1 - _potential2) - 0.5 * (_potential1 - _potential2));
                }
            }
            b[matrixIndices[0]] += (+bW);
            b[matrixIndices[1]] += (-bW);
        }

    }

    @Override
    public final void calculateCurrent(final double[] p, final double dt, final double t) {

        if (initCapacitor && _initialValue != 0) {
            initVoltageSource.updateHistory(p);
            _current = initVoltageSource.getCurrent();
        } else {
            if (_solverType == SolverType.SOLVER_BE) {
                _current = _capacitance / dt * ((p[matrixIndices[0]] - p[matrixIndices[1]]) - (_potential1 - _potential2));
            } else if (_solverType == SolverType.SOLVER_TRZ) {
                _current = 2 * (_capacitance / dt) * ((p[matrixIndices[0]] - p[matrixIndices[1]]) - (_potential1 - _potential2)) - _oldCurrent;
            } else if (_solverType == SolverType.SOLVER_GS) {
                _current = _capacitance / dt * (1.5 * (p[matrixIndices[0]] - p[matrixIndices[1]]) - 2 * (_potential1 - _potential2) + 0.5 * (_potOld1 - _potOld2));
            }

            if (_isNonLinear) {
                if (!DiodeCalculator.inSwitchErrorMode) {
                    if (((_current + _nonLinearCorrectionCurrent) * (_oldCurrent)) < 0) //wrong value of current calculated, capacitance must be changed!
                    {
                        capError = true;
                        _capacitance = (1 - _newOldCapRatio) * _capacitance; //set capacitance to new value (reconstructed from the ratio, see updateNonLinearCapacitance method
                    } else if (!_capCorrected) //everything is fine, add correction current
                    {
                        _current += _nonLinearCorrectionCurrent;
                    }
                } else //do not change capacitance if in diode error mode
                {
                    _current += _nonLinearCorrectionCurrent;
                }
                _capCorrected = false;
            }
        }

    }

    public void setZValue(int z) {
        assert z > 0;
        _z = z;
    }

    public void setInitialValue(double value) {
        _initialValue = value;
        _maxAbsVal = Math.abs(_initialValue);
        _voltage = _initialValue;
    }

    public double getInitialValue() {
        return _initialValue;
    }

    @Override
    public void registerBVector(BVector bvector) {
        _bVector = bvector;

    }

    @Override
    public boolean isBasisStampable() {
        return false;
    }

    @Override
    public final void updateHistory(double[] p) {
        _voltage = p[matrixIndices[0]] - p[matrixIndices[1]];
        _potOld1 = _potential1;
        _potOld2 = _potential2;
        _potential1 = p[matrixIndices[0]];
        _potential2 = p[matrixIndices[1]];
        _oldOldCurrent = _oldCurrent;
        _oldCurrent = _current;
        if (_maxAbsVal < Math.abs(_voltage)) {
            _maxAbsVal = Math.abs(_voltage);
        }
    }

    public int getZValue() {
        return _z;
    }

    public void setNonLinear(boolean isnonlinear) {
        _isNonLinear = isnonlinear;
    }

    public void setCapacitanceCharacteristic(CapacitanceCharacteristic varcap) {
        _NonLinearCapacitance = varcap;
    }

    public boolean isNonLinear() {
        return _isNonLinear;
    }

    public void updateNonLinearCapacitance() {
        double new_capacitance;
        if (initCapacitor) {
            new_capacitance = _NonLinearCapacitance.getCapacitanceAtV(_initialValue);
        } else {
            new_capacitance = _NonLinearCapacitance.getCapacitanceAtV(Math.abs(_potential1 - _potential2));
        }

        _newOldCapRatio = 1 - (new_capacitance / _capacitance);
        _nonLinearCorrectionCurrent = _newOldCapRatio * _oldCurrent;

        if (initCapacitor) {
            _capacitance = new_capacitance;
        }         else if (Math.abs((_capacitance - new_capacitance) / (_capacitance + new_capacitance)) > 0.1 && !DiodeCalculator.inSwitchErrorMode) //do not change capacitance unless this deviation is more than 10%, to save effort in recalculating A matrix
        {
            _capacitance = new_capacitance;
            capError = true;
            _newOldCapRatio = 0;
            _capCorrected = true;
        }

    }

    //for fast steady state solving - need max. abs. value of state variable during a simulation
    public double getMaxAbsVal() {
        return _maxAbsVal;
    }
}
