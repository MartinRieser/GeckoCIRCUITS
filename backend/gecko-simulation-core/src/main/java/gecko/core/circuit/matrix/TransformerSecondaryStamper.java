/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit.matrix;

/**
 * Matrix stamper for the ideal-transformer SECONDARY winding.
 *
 * Port of the legacy QUELLE_VOLTAGECONTROLLED_TRANSFORMER source type: the
 * secondary branch carries the z-current through its nodes (KCL column) but
 * gets NO z-row voltage equation — the winding voltage is set by the primary
 * VCVS coupling and the winding current by the ZCurrentMirrorCoupling
 * (i_sec = -ratio * i_prim), both registered on the netlist by the
 * NetlistBuilder's transformer expansion.
 *
 * A stamps (same KCL column as a voltage source):
 * - a[x][z] += 1
 * - a[y][z] -= 1
 *
 * B vector: no contribution (the constraint is homogeneous).
 */
public class TransformerSecondaryStamper implements IMatrixStamper {

    @Override
    public void stampMatrixA(MatrixAccumulator a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        a.add(nodeX, nodeZ, 1.0);
        a.add(nodeY, nodeZ, -(1.0));
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        // Current constraint handled by the z-current mirror coupling; no
        // winding voltage of its own.
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        // Branch current is a z-unknown; the caller reads it from the
        // solution vector. previousCurrent carries that value.
        return previousCurrent;
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        // Extended MNA formulation, no admittance weight
        return 0.0;
    }
}
