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
package gecko.core.io;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Project-level oscilloscope / graph view register configuration.
 * Persisted in .ipes format under {@code <graphRegisters>}.
 */
public record GraphRegisterData(
        String id,
        String name,
        int numGraphs, // 1, 2, or 3
        List<String> signals,
        Map<String, Integer> signalGraphs, // signalName -> 1, 2, or 3
        List<String> hiddenSignals,
        String yScaleMode, // "auto" | "fixed"
        boolean cursorsEnabled
) {
    public static final String BLOCK_START = "<graphRegisters>";
    public static final String BLOCK_END = "<\\graphRegisters>";
    public static final String ENTRY_START = "<graphRegister>";
    public static final String ENTRY_END = "<\\graphRegister>";

    public GraphRegisterData {
        id = (id == null || id.isBlank()) ? "osci_1" : id.trim();
        name = (name == null || name.isBlank()) ? "Osci 1" : name.trim();
        numGraphs = (numGraphs < 1 || numGraphs > 3) ? 1 : numGraphs;
        signals = (signals == null) ? List.of() : List.copyOf(signals);
        signalGraphs = (signalGraphs == null) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(signalGraphs));
        hiddenSignals = (hiddenSignals == null) ? List.of() : List.copyOf(hiddenSignals);
        yScaleMode = ("fixed".equalsIgnoreCase(yScaleMode)) ? "fixed" : "auto";
    }

    public static GraphRegisterData defaultRegister(String id, String name) {
        return new GraphRegisterData(id, name, 1, List.of(), Map.of(), List.of(), "auto", false);
    }
}
