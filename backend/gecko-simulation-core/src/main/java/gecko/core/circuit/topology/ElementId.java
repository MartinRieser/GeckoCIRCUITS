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
package gecko.core.circuit.topology;

/**
 * Strongly typed identifier of a netlist element, wrapping the non-negative
 * element index used by the MNA solver.
 *
 * <p>Element indices and node indices are both plain {@code int} values in the
 * netlist arrays, so passing one where the other is expected compiles without
 * complaint. Wrapping the index in this type makes such mix-ups a compile-time
 * error while the hot solver loops keep operating on the raw primitives.
 *
 * <p>Usage:
 * <pre>
 * ElementId element = ElementId.of(7);
 * CircuitTypCore type = netlist.getType(element);
 * </pre>
 *
 * @param value non-negative element index into the netlist arrays
 *
 * @see NodeId
 * @see TerminalPair
 * @since v2.18.0 Section 5 - Architectural Enhancements
 */
public record ElementId(int value) implements Comparable<ElementId> {

    /**
     * Canonical constructor validating the element index.
     *
     * @param value non-negative element index into the netlist arrays
     *
     * @throws IllegalArgumentException if the element index is negative
     */
    public ElementId {
        if (value < 0) {
            throw new IllegalArgumentException("Element index cannot be negative: " + value);
        }
    }

    /**
     * Creates an element identifier for the given element index.
     *
     * @param value non-negative element index into the netlist arrays
     * @return element identifier wrapping the index
     *
     * @throws IllegalArgumentException if the element index is negative
     */
    public static ElementId of(int value) {
        return new ElementId(value);
    }

    @Override
    public int compareTo(final ElementId other) {
        return Integer.compare(value, other.value);
    }
}
