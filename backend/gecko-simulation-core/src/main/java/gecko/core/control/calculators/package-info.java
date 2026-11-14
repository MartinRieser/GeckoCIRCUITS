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
 * GUI-free control block calculator implementations extending
 * {@link gecko.core.control.calculators.AbstractControlCalculatable}.
 *
 * <p>Includes arithmetic and math blocks ({@code AddCalculator}, {@code MulCalculator},
 * {@code DivCalculator}, {@code SqrtCalculator}, trigonometric and exponential functions),
 * logic and comparison gates ({@code AndTwoPortCalculator}, {@code OrCalculatorTwoInputs},
 * {@code NotCalculator}, {@code XORCalculator}, {@code GreaterThanCalculator}),
 * dynamic/transfer blocks ({@code IntegratorCalculation}, {@code PICalculator},
 * {@code PDCalculator}, {@code PT1Calculator}, {@code PT2Calculator}, {@code DelayCalculator}),
 * signal sources ({@code ConstantCalculator}, {@code SignalCalculatorSinus},
 * {@code SignalCalculatorRectangle}, {@code SignalCalculatorTriangle},
 * {@code SignalCalculatorRandom}), transforms and motor control ({@code ABCDQCalculator},
 * {@code DQABCDCalculator}, {@code PmsmControlCalculator}, {@code PmsmModulatorCalculator},
 * {@code SlidingDFTCalculator}), and script/C-library blocks ({@code ScriptBlockCalculator},
 * {@code CLibraryCalculator}).
 */
package gecko.core.control.calculators;
