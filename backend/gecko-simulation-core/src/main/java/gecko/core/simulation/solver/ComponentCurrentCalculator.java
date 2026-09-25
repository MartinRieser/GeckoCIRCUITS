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
import gecko.core.circuit.parameters.CapacitorParameters;
import gecko.core.circuit.parameters.DiodeParameters;
import gecko.core.circuit.parameters.InductorParameters;
import gecko.core.circuit.parameters.ResistorParameters;
import gecko.core.circuit.parameters.SourceParameters;
import gecko.core.circuit.parameters.SwitchParameters;

/**
 * Calculates component currents from node potentials after solving Ax=b system.
 *
 * Extracted from legacy LKMatrices.calculateComponentCurrents() method (lines 596-1144).
 * This class implements the current calculation phase of the circuit simulation,
 * computing branch currents based on component types and node voltages.
 */
public class ComponentCurrentCalculator {

    private static final double FAST_NULL_R = SolverConstants.FAST_NULL_R;
    private static final double FAST_NULL_L = SolverConstants.FAST_NULL_L;

    /** Above this resistance a semiconductor branch counts as blocking (legacy rDoffDEFAULT). */
    private static final double RD_OFF_THRESHOLD = SolverConstants.RD_OFF_THRESHOLD;

    /** When true, the Shockley Newton-Raphson controller owns the LK_D slots. */
    private boolean diodesHandledByNewtonRaphson;

    /**
     * Hands the diode parameter slots to the Newton-Raphson controller for
     * the current time step; the piecewise-linear machine skips LK_D then.
     *
     * @param handledByNewton true to skip diodes in the state machine
     */
    public void setDiodesHandledByNewtonRaphson(final boolean handledByNewton) {
        this.diodesHandledByNewtonRaphson = handledByNewton;
    }

    /**
     * Calculates component currents after solving the MNA system Ax=b.
     * Legacy-compatible overload with default disturbance and error counter.
     */
    public boolean calculateComponentCurrents(
            MnaSolver matrixSolver,
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
            MnaSolver matrixSolver,
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
        double acceptanceThreshold = errorCounter > SolverConstants.RELAXATION_THRESHOLD_TIER_2
                ? SolverConstants.RELAXATION_VOLTAGE_TIER_2
                : (errorCounter > SolverConstants.RELAXATION_THRESHOLD_TIER_1
                        ? SolverConstants.RELAXATION_VOLTAGE_TIER_1 : 0.0);

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

                case LK_R, REL_RELUCTANCE, TH_RTH, TH_AMBIENT -> {
                    double resistance = parameters[ResistorParameters.INDEX_RESISTANCE];
                    if (resistance < FAST_NULL_R) {
                        resistance = FAST_NULL_R;
                    }
                    parameters[ResistorParameters.INDEX_CURRENT] = (p[nodeX] - p[nodeY]) / resistance;
                }

                case LK_S, LK_MOSFET -> {
                    double resistance = parameters[SwitchParameters.INDEX_CURRENT_RESISTANCE];
                    if (resistance < FAST_NULL_R) {
                        resistance = FAST_NULL_R;
                    }
                    // legacy storage: LK_S -> [3]=i,[4]=u; MOSFET -> [4]=i,[5]=u.
                    // params[1] holds the rOn slot of LK_S and must not be clobbered.
                    writeCurrentAndVoltage(parameters, SwitchParameters.INDEX_CURRENT,
                            (p[nodeX] - p[nodeY]) / resistance, p[nodeX] - p[nodeY]);
                }

                case LK_L, NONLIN_REL -> {
                    double inductance = parameters[InductorParameters.INDEX_INDUCTANCE];
                    double voltage = p[nodeX] - p[nodeY];

                    if (inductance < FAST_NULL_L) {
                        if (solverType == SolverType.SOLVER_BE) {
                            parameters[InductorParameters.INDEX_INITIAL_CURRENT] = iALT[elementIdx] + dt / FAST_NULL_L * voltage;
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            parameters[InductorParameters.INDEX_INITIAL_CURRENT] = iALT[elementIdx] + dt / (2 * FAST_NULL_L) *
                                    (voltage + (pALT[nodeX] - pALT[nodeY]));
                        } else if (solverType == SolverType.SOLVER_GS) {
                            parameters[InductorParameters.INDEX_INITIAL_CURRENT] = SolverConstants.GEAR_SHICHMAN_COEFF_2_3 * dt / FAST_NULL_L * voltage +
                                    SolverConstants.GEAR_SHICHMAN_COEFF_4_3 * iALT[elementIdx] - SolverConstants.GEAR_SHICHMAN_COEFF_1_3 * iALTALT[elementIdx];
                        }
                    } else {
                        if (solverType == SolverType.SOLVER_BE) {
                            parameters[InductorParameters.INDEX_INITIAL_CURRENT] = iALT[elementIdx] + dt / inductance * voltage;
                        } else if (solverType == SolverType.SOLVER_TRZ) {
                            parameters[InductorParameters.INDEX_INITIAL_CURRENT] = iALT[elementIdx] + dt / (2 * inductance) *
                                    (voltage + (pALT[nodeX] - pALT[nodeY]));
                        } else if (solverType == SolverType.SOLVER_GS) {
                            parameters[InductorParameters.INDEX_INITIAL_CURRENT] = SolverConstants.GEAR_SHICHMAN_COEFF_2_3 * dt / inductance * voltage +
                                    SolverConstants.GEAR_SHICHMAN_COEFF_4_3 * iALT[elementIdx] - SolverConstants.GEAR_SHICHMAN_COEFF_1_3 * iALTALT[elementIdx];
                        }
                    }
                }

                case TH_CTH, LK_C -> {
                    if (componentType == CircuitTypCore.TH_CTH) {
                        parameters[CapacitorParameters.INDEX_EFFECTIVE_C] = parameters[CapacitorParameters.INDEX_CAPACITANCE];
                        parameters[CapacitorParameters.INDEX_NONLINEAR_FACTOR] = parameters[CapacitorParameters.INDEX_CAPACITANCE];
                    }
                    double capacitance = parameters[CapacitorParameters.INDEX_EFFECTIVE_C];
                    double nonlinearFactor = parameters[CapacitorParameters.INDEX_NONLINEAR_FACTOR];
                    double fac = 1.0 - nonlinearFactor / capacitance;
                    double nonLinearCorrectionCurrent = -fac * parameters[CapacitorParameters.INDEX_COMPANION_CURRENT];

                    double voltage = p[nodeX] - p[nodeY];
                    double previousVoltage = pALT[nodeX] - pALT[nodeY];

                    if (solverType == SolverType.SOLVER_BE) {
                        parameters[CapacitorParameters.INDEX_INITIAL_VOLTAGE] = capacitance / dt * (voltage - previousVoltage);
                    } else if (solverType == SolverType.SOLVER_TRZ) {
                        parameters[CapacitorParameters.INDEX_INITIAL_VOLTAGE] = 2 * capacitance / dt * (voltage - previousVoltage) - iALT[elementIdx];
                    } else if (solverType == SolverType.SOLVER_GS) {
                        double twoStepsBack = pALTALT[nodeX] - pALTALT[nodeY];
                        parameters[CapacitorParameters.INDEX_INITIAL_VOLTAGE] = capacitance / dt * (1.5 * voltage - 2 * previousVoltage + 0.5 * twoStepsBack);
                    }
                    parameters[CapacitorParameters.INDEX_COMPANION_CURRENT] = parameters[CapacitorParameters.INDEX_INITIAL_VOLTAGE];
                    parameters[CapacitorParameters.INDEX_INITIAL_VOLTAGE] += nonLinearCorrectionCurrent;
                }

                case LK_I, TH_FLOW -> {
                    int sourceType = (int) parameters[SourceParameters.INDEX_SOURCE_TYPE];
                    switch (sourceType) {
                        case SourceType.QUELLE_DC_NEW, SourceType.QUELLE_DC,
                             SourceType.QUELLE_SIGNALGESTEUERT_NEW, SourceType.QUELLE_SIGNALGESTEUERT -> {
                            parameters[SourceParameters.INDEX_VALUE_DC] = parameters[SourceParameters.INDEX_VALUE_DC];
                        }
                        case SourceType.QUELLE_SIN_NEW, SourceType.QUELLE_SIN -> {
                            double amplitude = parameters[SourceParameters.INDEX_AMPLITUDE_SIN];
                            double frequency = parameters[SourceParameters.INDEX_FREQUENCY];
                            double phase = parameters[SourceParameters.INDEX_PHASE_DEG];
                            double offset = parameters[SourceParameters.INDEX_OFFSET];
                            parameters[SourceParameters.INDEX_VALUE_DC] = amplitude * Math.sin(2 * Math.PI * frequency * time -
                                    Math.toRadians(phase)) + offset;
                        }
                        case SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW,
                             SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY -> {
                            parameters[SourceParameters.INDEX_VALUE_DC] = 0.0;
                        }
                        default -> {
                        }
                    }
                }

                case LK_U, REL_MMF, TH_TEMP -> {
                }

                case LK_D -> {
                    if (diodesHandledByNewtonRaphson) {
                        // The Newton-Raphson controller owns the diode slots
                        // (resistance, forward voltage, current, voltage)
                        break;
                    }
                    // Port of the legacy diode model: params[0]=current rD, [1]=uF,
                    // [2]=rOn, [3]=rOff, [4]=i, [5]=u. The piecewise-linear state
                    // flip sets [0] and requests a re-solve of this time step.
                    double rD = parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE];
                    double uf = parameters[DiodeParameters.INDEX_FORWARD_VOLTAGE];
                    double voltage = p[nodeX] - p[nodeY];
                    writeCurrentAndVoltage(parameters, DiodeParameters.INDEX_CURRENT, (voltage - uf) / rD, voltage);
                    boolean conducting = rD < RD_OFF_THRESHOLD;
                    if (conducting && voltage < stoergroesse * uf + acceptanceThreshold) {
                        parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] = parameters[DiodeParameters.INDEX_R_OFF];
                        stepBack = true;
                    } else if (!conducting && rD >= RD_OFF_THRESHOLD
                            && voltage > stoergroesse * uf - acceptanceThreshold) {
                        parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] = parameters[DiodeParameters.INDEX_R_ON];
                        stepBack = true;
                    }
                }
                case LK_THYR -> {
                    double rD = parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE];
                    double uf = parameters[DiodeParameters.INDEX_FORWARD_VOLTAGE];
                    double voltage = p[nodeX] - p[nodeY];
                    writeCurrentAndVoltage(parameters, DiodeParameters.INDEX_CURRENT, (voltage - uf) / rD, voltage);
                    if (voltage < stoergroesse * uf + acceptanceThreshold
                            && rD < 0.5 * parameters[DiodeParameters.INDEX_R_OFF]) {
                        if (time - parameters[DiodeParameters.INDEX_LAST_CROSSING_TIME]
                                > 3 * parameters[DiodeParameters.INDEX_TURN_OFF_DELAY]) {
                            parameters[DiodeParameters.INDEX_LAST_CROSSING_TIME] = time;
                        }
                        if (time - parameters[DiodeParameters.INDEX_LAST_CROSSING_TIME]
                                >= parameters[DiodeParameters.INDEX_TURN_OFF_DELAY]) {
                            parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] = parameters[DiodeParameters.INDEX_R_OFF];
                            stepBack = true;
                        }
                    }
                    if (parameters[DiodeParameters.INDEX_GATE_SIGNAL] == 1.0
                            && voltage > stoergroesse * uf - acceptanceThreshold
                            && rD >= RD_OFF_THRESHOLD) {
                        parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] = parameters[DiodeParameters.INDEX_R_ON];
                        stepBack = true;
                    }
                }

                case LK_IGBT -> {
                    double rD = parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE];
                    double uf = parameters[DiodeParameters.INDEX_FORWARD_VOLTAGE];
                    double voltage = p[nodeX] - p[nodeY];
                    writeCurrentAndVoltage(parameters, DiodeParameters.INDEX_CURRENT, (voltage - uf) / rD, voltage);
                    boolean conducting = rD < RD_OFF_THRESHOLD;
                    if (conducting && parameters[DiodeParameters.INDEX_GATE_SIGNAL] == 1.0
                            && voltage < stoergroesse * uf + acceptanceThreshold) {
                        parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] = parameters[DiodeParameters.INDEX_R_OFF];
                        stepBack = true;
                    }
                    if (parameters[DiodeParameters.INDEX_GATE_SIGNAL] == 1.0 && !conducting
                            && voltage > stoergroesse * uf - acceptanceThreshold) {
                        parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] = parameters[DiodeParameters.INDEX_R_ON];
                        stepBack = true;
                    }
                    if (parameters[DiodeParameters.INDEX_GATE_SIGNAL] == 0.0
                            && parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] == parameters[DiodeParameters.INDEX_R_ON]) {
                        parameters[DiodeParameters.INDEX_CURRENT_RESISTANCE] = parameters[DiodeParameters.INDEX_R_OFF];
                        stepBack = true;
                    }
                }

                case LK_LKOP2 -> {
                    int voltageSourceNumber = netlist.getVoltageSourceNumber(elementIdx);
                    int voltageSourceIdx = netlist.getNodeMax() + voltageSourceNumber;
                    parameters[InductorParameters.INDEX_INITIAL_CURRENT] = p[voltageSourceIdx];
                }

                case LK_TERMINAL, TH_TERMINAL, REL_TERMINAL, LK_GLOBAL_TERMINAL,
                     TH_GLOBAL_TERMINAL, REL_GLOBAL_TERMINAL, LK_M -> {
                }

                default -> {
                }
            }

            // Legacy stores the element current in a type-specific parameter slot;
            // keep the solver's current-state array in sync so the next history
            // shift promotes it to iALT (inductor/cap history terms depend on it).
            if (parameters.length > 1) {
                int currentSlot = switch (componentType) {
                    case LK_S, LK_MOSFET, LK_D, LK_THYR, LK_IGBT -> DiodeParameters.INDEX_CURRENT;
                    default -> ResistorParameters.INDEX_CURRENT;
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
