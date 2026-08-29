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
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.INetList;

/**
 * Calculates component currents from node potentials after solving Ax=b system.
 *
 * Extracted from legacy LKMatrices.calculateComponentCurrents() method (lines 596-1144).
 * This class implements the current calculation phase of the circuit simulation,
 * computing branch currents based on component types and node voltages.
 */
@SuppressWarnings("fallthrough")
public class ComponentCurrentCalculator {

    private static final double FAST_NULL_R = 1.0e-12;
    private static final double FAST_NULL_L = 1.0e-12;

    /** Above this resistance a semiconductor branch counts as blocking (legacy rDoffDEFAULT). */
    private static final double RD_OFF_THRESHOLD = 1.0e7;

    /**
     * Calculates component currents after solving the MNA system Ax=b.
     * Legacy-compatible overload with default disturbance and error counter.
     */
    public boolean calculateComponentCurrents(
            MatrixSolver matrixSolver,
            INetList netlist,
            double perturbation,
            double dt,
            double time,
            boolean isNewIteration) {
        return calculateComponentCurrents(matrixSolver, netlist, 1.0, dt, time, isNewIteration, 0);
    }

    /**
     * Calculates component currents and runs the piecewise-linear semiconductor
     * state machine (port of legacy {@code LKMatrices.calculateComponentCurrents}).
     *
     * @param stoergroesse     disturbance factor shrinking the switching thresholds
     *                         when states oscillate (legacy anti-stuck mechanism)
     * @param errorCounter     number of state re-solves already done for this step;
     *                         widens the acceptance threshold after 300/600 flips
     * @return true when a diode/thyristor/IGBT flipped its state — the caller must
     *         re-build and re-solve the same time step (legacy einSchrittZurueck)
     */
    public boolean calculateComponentCurrents(
            MatrixSolver matrixSolver,
            INetList netlist,
            double stoergroesse,
            double dt,
            double time,
            boolean isNewIteration,
            int errorCounter) {

        if (matrixSolver == null) {
            throw new IllegalArgumentException("Matrix solver cannot be null");
        }
        if (netlist == null) {
            throw new IllegalArgumentException("Netlist cannot be null");
        }

        boolean stepBack = false;
        double acceptanceThreshold = errorCounter > 600 ? 0.2 : errorCounter > 300 ? 0.1 : 0.0;

        double[] p = matrixSolver.getP();
        double[] pALT = matrixSolver.getPALT();
        double[] pALTALT = matrixSolver.getPALTALT();
        double[] iALT = matrixSolver.getIALT();
        double[] iALTALT = matrixSolver.getIALTALT();
        double[] iCurrent = matrixSolver.getICurrent();
        SolverType solverType = matrixSolver.getSolverType();

        for (int elementIdx = 0; elementIdx < netlist.getElementCount(); elementIdx++) {
            CircuitTypCore componentType = netlist.getType(elementIdx);
            int nodeX = netlist.getNodeX(elementIdx);
            int nodeY = netlist.getNodeY(elementIdx);
            double[] parameters = netlist.getParameter(elementIdx);

            switch (componentType) {

                case LK_R:
                case REL_RELUCTANCE:
                case TH_RTH:
                case TH_AMBIENT: {
                    double resistance = parameters[0];
                    if (resistance < FAST_NULL_R) {
                        resistance = FAST_NULL_R;
                    }
                    parameters[1] = (p[nodeX] - p[nodeY]) / resistance;
                    break;
                }

                case LK_S:
                case LK_MOSFET: {
                    double resistance = parameters[0];
                    if (resistance < FAST_NULL_R) {
                        resistance = FAST_NULL_R;
                    }
                    // legacy storage: LK_S -> [3]=i,[4]=u; MOSFET -> [4]=i,[5]=u.
                    // params[1] holds the rOn slot of LK_S and must not be clobbered.
                    writeCurrentAndVoltage(parameters, 4,
                            (p[nodeX] - p[nodeY]) / resistance, p[nodeX] - p[nodeY]);
                    break;
                }

                case LK_L:
                case NONLIN_REL: {
                    double inductance = parameters[0];
                    double voltage = p[nodeX] - p[nodeY];

                    if (inductance < FAST_NULL_L) {
                        if (solverType == SolverType.SOLVER_BE) {
                            parameters[1] = iALT[elementIdx] + dt / FAST_NULL_L * voltage;
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            parameters[1] = iALT[elementIdx] + dt / (2 * FAST_NULL_L) *
                                    (voltage + (pALT[nodeX] - pALT[nodeY]));
                        } else if (solverType == SolverType.SOLVER_GS) {
                            parameters[1] = 2.0 / 3.0 * dt / FAST_NULL_L * voltage +
                                    4.0 / 3.0 * iALT[elementIdx] - 1.0 / 3.0 * iALTALT[elementIdx];
                        }
                    } else {
                        if (solverType == SolverType.SOLVER_BE) {
                            parameters[1] = iALT[elementIdx] + dt / inductance * voltage;
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            parameters[1] = iALT[elementIdx] + dt / (2 * inductance) *
                                    (voltage + (pALT[nodeX] - pALT[nodeY]));
                        } else if (solverType == SolverType.SOLVER_GS) {
                            parameters[1] = 2.0 / 3.0 * dt / inductance * voltage +
                                    4.0 / 3.0 * iALT[elementIdx] - 1.0 / 3.0 * iALTALT[elementIdx];
                        }
                    }
                    break;
                }

                case TH_CTH:
                    parameters[6] = parameters[0];
                    parameters[7] = parameters[0];
                    /* falls through */
                case LK_C: {
                    double capacitance = parameters[6];
                    double nonlinearFactor = parameters[7];
                    double fac = 1.0 - nonlinearFactor / capacitance;
                    double nonLinearCorrectionCurrent = -fac * parameters[10];

                    double voltage = p[nodeX] - p[nodeY];
                    double previousVoltage = pALT[nodeX] - pALT[nodeY];

                    if (isNewIteration) {
                        if (solverType == SolverType.SOLVER_BE) {
                            parameters[1] = capacitance / dt * (voltage - previousVoltage);
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            parameters[1] = 2 * capacitance / dt * (voltage - previousVoltage) - iALT[elementIdx];
                        } else if (solverType == SolverType.SOLVER_GS) {
                            double twoStepsBack = pALTALT[nodeX] - pALTALT[nodeY];
                            parameters[1] = capacitance / dt * (1.5 * voltage - 2 * previousVoltage + 0.5 * twoStepsBack);
                        }
                        parameters[10] = parameters[1];
                        parameters[1] += nonLinearCorrectionCurrent;
                    } else {
                        if (solverType == SolverType.SOLVER_BE) {
                            parameters[1] = capacitance / dt * (voltage - previousVoltage);
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            parameters[1] = 2 * capacitance / dt * (voltage - previousVoltage) - iALT[elementIdx];
                        } else if (solverType == SolverType.SOLVER_GS) {
                            double twoStepsBack = pALTALT[nodeX] - pALTALT[nodeY];
                            parameters[1] = capacitance / dt * (1.5 * voltage - 2 * previousVoltage + 0.5 * twoStepsBack);
                        }
                        parameters[10] = parameters[1];
                        parameters[1] += nonLinearCorrectionCurrent;
                    }
                    break;
                }

                case LK_I:
                case TH_FLOW: {
                    int sourceType = (int) parameters[0];
                    switch (sourceType) {
                        case SourceType.QUELLE_DC_NEW:
                        case SourceType.QUELLE_DC:
                            parameters[1] = parameters[1];
                            break;
                        case SourceType.QUELLE_SIGNALGESTEUERT_NEW:
                        case SourceType.QUELLE_SIGNALGESTEUERT:
                            parameters[1] = parameters[1];
                            break;
                        case SourceType.QUELLE_SIN_NEW:
                        case SourceType.QUELLE_SIN: {
                            double amplitude = parameters[20];
                            double frequency = parameters[2];
                            double phase = parameters[4];
                            double offset = parameters[3];
                            parameters[1] = amplitude * Math.sin(2 * Math.PI * frequency * time -
                                    Math.toRadians(phase)) + offset;
                            break;
                        }
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW:
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY:
                            parameters[1] = 0.0;
                            break;
                        default:
                            break;
                    }
                    break;
                }

                case LK_U:
                case REL_MMF:
                case TH_TEMP:
                    break;

                case LK_D: {
                    // Port of the legacy diode model: params[0]=current rD, [1]=uF,
                    // [2]=rOn, [3]=rOff, [4]=i, [5]=u. The piecewise-linear state
                    // flip sets [0] and requests a re-solve of this time step.
                    double rD = parameters[0];
                    double uf = parameters[1];
                    double voltage = p[nodeX] - p[nodeY];
                    writeCurrentAndVoltage(parameters, 4, (voltage - uf) / rD, voltage);
                    boolean conducting = rD < RD_OFF_THRESHOLD;
                    if (conducting && voltage < stoergroesse * uf + acceptanceThreshold) {
                        parameters[0] = parameters[3];
                        stepBack = true;
                    } else if (!conducting && rD >= RD_OFF_THRESHOLD
                            && voltage > stoergroesse * uf - acceptanceThreshold) {
                        parameters[0] = parameters[2];
                        stepBack = true;
                    }
                    break;
                }

                case LK_THYR: {
                    double rD = parameters[0];
                    double uf = parameters[1];
                    double voltage = p[nodeX] - p[nodeY];
                    writeCurrentAndVoltage(parameters, 4, (voltage - uf) / rD, voltage);
                    if (voltage < stoergroesse * uf + acceptanceThreshold && rD < 0.5 * parameters[3]) {
                        if (time - parameters[11] > 3 * parameters[9]) {
                            parameters[11] = time;
                        }
                        if (time - parameters[11] >= parameters[9]) {
                            parameters[0] = parameters[3];
                            stepBack = true;
                        }
                    }
                    if (parameters[8] == 1 && voltage > stoergroesse * uf - acceptanceThreshold
                            && rD >= RD_OFF_THRESHOLD) {
                        parameters[0] = parameters[2];
                        stepBack = true;
                    }
                    break;
                }

                case LK_IGBT: {
                    double rD = parameters[0];
                    double uf = parameters[1];
                    double voltage = p[nodeX] - p[nodeY];
                    writeCurrentAndVoltage(parameters, 4, (voltage - uf) / rD, voltage);
                    boolean conducting = rD < RD_OFF_THRESHOLD;
                    if (conducting && parameters[8] == 1
                            && voltage < stoergroesse * uf + acceptanceThreshold) {
                        parameters[0] = parameters[3];
                        stepBack = true;
                    }
                    if (parameters[8] == 1 && !conducting
                            && voltage > stoergroesse * uf - acceptanceThreshold) {
                        parameters[0] = parameters[2];
                        stepBack = true;
                    }
                    if (parameters[8] == 0 && parameters[0] == parameters[2]) {
                        parameters[0] = parameters[3];
                        stepBack = true;
                    }
                    break;
                }

                case LK_LKOP2: {
                    int voltageSourceNumber = netlist.getVoltageSourceNumber(elementIdx);
                    int voltageSourceIdx = netlist.getNodeMax() + voltageSourceNumber;
                    parameters[1] = p[voltageSourceIdx];
                    break;
                }

                case LK_TERMINAL:
                case TH_TERMINAL:
                case REL_TERMINAL:
                case LK_GLOBAL_TERMINAL:
                case TH_GLOBAL_TERMINAL:
                case REL_GLOBAL_TERMINAL:
                case LK_M:
                    break;

                default:
                    break;
            }

            // Legacy stores the element current in a type-specific parameter slot;
            // keep the solver's current-state array in sync so the next history
            // shift promotes it to iALT (inductor/cap history terms depend on it).
            if (parameters.length > 1) {
                int currentSlot = switch (componentType) {
                    case LK_S, LK_MOSFET, LK_D, LK_THYR, LK_IGBT -> 4;
                    default -> 1;
                };
                iCurrent[elementIdx] = parameters[Math.min(currentSlot, parameters.length - 1)];
            }
        }

        return stepBack;
    }

    /** Writes current/voltage into legacy storage slots, tolerating short arrays. */
    private static void writeCurrentAndVoltage(double[] parameters, int slot,
                                               double current, double voltage) {
        if (parameters.length > slot) {
            parameters[slot] = current;
        }
        if (parameters.length > slot + 1) {
            parameters[slot + 1] = voltage;
        }
    }
}
