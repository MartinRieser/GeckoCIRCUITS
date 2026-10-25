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
package gecko.rest.model.circuit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * REST DTO representing an Oscilloscope / Graph View register.
 */
@Schema(description = "Oscilloscope / Graph View Register")
public record GraphRegisterDto(
    @Schema(description = "Stable unique ID within the circuit", example = "osci_1")
    String id,

    @Schema(description = "User-facing title", example = "Osci 1")
    String name,

    @Schema(description = "Number of stacked subplots: 1, 2, or 3", example = "1")
    int numGraphs,

    @Schema(description = "Selected signal names")
    List<String> signals,

    @Schema(description = "Mapping from signal name to graph index (1, 2, or 3)")
    Map<String, Integer> signalGraphs,

    @Schema(description = "Muted or hidden signal names")
    List<String> hiddenSignals,

    @Schema(description = "Y-axis scaling mode: 'auto' or 'fixed'", example = "auto")
    String yScaleMode,

    @Schema(description = "Whether measurement cursors are active")
    boolean cursorsEnabled
) {}
