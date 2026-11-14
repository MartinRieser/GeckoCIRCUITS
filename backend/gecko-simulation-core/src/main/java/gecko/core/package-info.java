/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * GUI-free simulation core for GeckoCIRCUITS.
 *
 * <p>Provides headless circuit parsing, netlist construction, Modified Nodal Analysis (MNA),
 * control-block execution, multi-domain magnetic/thermal solvers, and signal analysis without
 * Swing/AWT dependencies.
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link gecko.core.GeckoHeadless} - Headless entry point and facade</li>
 *   <li>{@code gecko.core.allg} - Solver settings, solver types, and user parameters</li>
 *   <li>{@code gecko.core.circuit} - Component definitions, stampers, loss models, and netlists</li>
 *   <li>{@code gecko.core.control} - Control block builders and calculators</li>
 *   <li>{@code gecko.core.datacontainer} - Simulation waveform containers and time series</li>
 *   <li>{@code gecko.core.io} - Headless {@code .ipes} parser, writer, and circuit model</li>
 *   <li>{@code gecko.core.magnetic} - Magnetic reluctance network solver and saturation models</li>
 *   <li>{@code gecko.core.math} - Dense matrix/LU routines and power-electronics transforms</li>
 *   <li>{@code gecko.core.signal} - FFT and waveform characteristic calculators</li>
 *   <li>{@code gecko.core.simulation} - Headless engine, domain coupler, dense and sparse MNA solvers</li>
 *   <li>{@code gecko.core.thermal} - Thermal RC network solver and electro-thermal coupling</li>
 * </ul>
 *
 * @see gecko.core.simulation.HeadlessSimulationEngine
 * @see gecko.core.circuit.matrix.IMatrixStamper
 */
package gecko.core;
