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
package gecko.core.circuit.matrix;

import gecko.core.allg.SolverType;
import gecko.core.circuit.parameters.InductorParameters;
import gecko.core.simulation.solver.SolverConstants;

/**
 * Matrix stamper implementation for coupled inductors ({@code LK_LKOP2}).
 *
 * <p>A coupled inductor branch is formulated in MNA with a dedicated branch through-current
 * variable {@code nodeZ}, producing KCL contributions at terminals X and Y, and a branch
 * constitutive relation {@code v(X) - v(Y) - (L/dt)*i = history} at equation row Z.</p>
 *
 * @author GeckoCIRCUITS Core Team
 */
public final class CoupledInductorStamper implements IMatrixStamper {

    private final SolverType solverType;

    /**
     * Constructs a coupled inductor stamper for the given solver numerical integration method.
     *
     * @param solverType the numerical integration method (BE, TRZ, GS)
     */
    public CoupledInductorStamper(final SolverType solverType) {
        this.solverType = solverType != null ? solverType : SolverType.SOLVER_BE;
    }

    /**
     * Constructs a coupled inductor stamper defaulting to Backward Euler.
     */
    public CoupledInductorStamper() {
        this(SolverType.SOLVER_BE);
    }

    @Override
    public void stampMatrixA(final MatrixAccumulator a, final int nodeX, final int nodeY, final int nodeZ,
                             final double[] parameter, final double dt) {
        final double inductance = parameter[InductorParameters.INDEX_INDUCTANCE];
        parameter[InductorParameters.INDEX_EFFECTIVE_L] = inductance;

        // KCL contributions (columns) and branch voltage equation (rows)
        a.add(nodeX, nodeZ, 1.0);
        a.add(nodeY, nodeZ, -(1.0));
        a.add(nodeZ, nodeX, 1.0);
        a.add(nodeZ, nodeY, -(1.0));

        final double companion = switch (solverType) {
            case SOLVER_TRZ -> -2.0 * inductance / dt;
            case SOLVER_GS -> -1.5 * inductance / dt;
            default -> -inductance / dt;
        };
        a.add(nodeZ, nodeZ, companion);
    }

    @Override
    public void stampVectorB(final double[] b, final int nodeX, final int nodeY, final int nodeZ,
                             final double[] parameter, final double dt, final double time,
                             final double[] previousValues) {
        // Handled via nodeZ history equation row in MNA
    }

    @Override
    public double calculateCurrent(final double nodeVoltageX, final double nodeVoltageY,
                                   final double[] parameter, final double dt, final double previousCurrent) {
        return previousCurrent;
    }

    @Override
    public double getAdmittanceWeight(final double parameterValue, final double dt) {
        final double safeL = Math.max(parameterValue, SolverConstants.FAST_NULL_L);
        return dt / safeL;
    }
}
