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
package gecko.mcp;

import gecko.core.allg.SolverType;
import gecko.core.circuit.losscalculation.SemiconductorDeviceLossModel;
import gecko.core.circuit.losscalculation.SemiconductorLossEngine;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.ControlCalculatorBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.magnetic.MagneticNetworkSolver;
import gecko.core.magnetic.MagneticNode;
import gecko.core.magnetic.MagneticWinding;
import gecko.core.magnetic.NonlinearReluctance;
import gecko.core.magnetic.ReluctanceBranch;
import gecko.core.simulation.MatrixSolverKind;
import gecko.core.simulation.SemiconductorModelKind;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import gecko.core.thermal.ThermalCoupling;
import gecko.core.thermal.ThermalRCModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps the JSON run arguments of the simulate tool family
 * ({@code gecko_simulate}, {@code gecko_get_waveforms}, {@code gecko_measure_metrics},
 * {@code gecko_list_signals}) onto the headless engine's {@link SimulationConfig},
 * exposing the full simulation surface: signal selection, parameter overrides,
 * matrix solver selection, adaptive stepping, semiconductor modeling, and the
 * thermal and magnetic multi-domain configuration.
 */
final class RunOptions {

    private RunOptions() {
    }

    /** Default simulation duration in seconds when the caller omits it. */
    private static final double DEFAULT_DURATION = 20e-3;
    /** Default time step in seconds when the caller omits it. */
    private static final double DEFAULT_DT = 1e-6;
    /** Default ambient temperature for thermal-domain runs. */
    private static final double DEFAULT_AMBIENT_TEMPERATURE = 25.0;

    /** Thermal model kinds of the coupling JSON. */
    private static final String MODEL_KIND_FOSTER = "foster";
    private static final String MODEL_KIND_CAUER = "cauer";
    /** Thermal feedback kinds of the coupling JSON. */
    private static final String COUPLING_KIND_RESISTOR = "resistor";
    private static final String COUPLING_KIND_DIODE = "diode";

    /** Result of one headless run with the extracted engine reporting. */
    record RunResult(long totalSteps, List<String> signalNames, long executionTimeMs,
                     Map<String, Object> metadata, List<String> warnings) {
    }

    /**
     * Builds a simulation config from a circuit file path plus the JSON
     * argument map of a simulate-family tool call.
     */
    static SimulationConfig buildConfig(Path circuit, Map<String, Object> args) {
        SimulationConfig.Builder builder = SimulationConfig.builder()
                .circuitFile(circuit.toString())
                .solverType(solver(str(args, "solver", "be")))
                .stepWidth(optionalDouble(args, "dt", DEFAULT_DT))
                .simulationDuration(optionalDouble(args, "duration",
                        optionalDouble(args, "simulation_time", DEFAULT_DURATION)));
        applyOptions(builder, args);
        return builder.build();
    }

    /**
     * Runs the headless engine for the given circuit and argument map and
     * extracts the reporting (signal names, metadata, warnings).
     */
    static RunResult run(Path circuit, Map<String, Object> args) throws IOException {
        SimulationResult result = new HeadlessEngineHolder().run(buildConfig(circuit, args));
        return toRunResult(result);
    }

    /** Runs the engine and returns the raw result (for CSV-style extraction). */
    static SimulationResult runRaw(Path circuit, Map<String, Object> args) {
        return new HeadlessEngineHolder().run(buildConfig(circuit, args));
    }

    /** Runs the engine for a caller-built config (used by list-signals). */
    static SimulationResult runRaw(SimulationConfig config) {
        return new HeadlessEngineHolder().run(config);
    }

    private static RunResult toRunResult(SimulationResult result) {
        if (!result.isSuccess()) {
            throw new IllegalStateException("Simulation failed: " + result.getErrorMessage());
        }
        return new RunResult(result.getTotalTimeSteps(),
                List.of(result.getSignalNames()),
                result.getExecutionTimeMs(),
                new LinkedHashMap<>(result.getMetadata()),
                List.copyOf(result.getWarnings()));
    }

    /** Applies the shared optional arguments onto a config builder. */
    static void applyOptions(SimulationConfig.Builder builder, Map<String, Object> args) {
        if (args.get("signals") instanceof List<?> signalList) {
            builder.signals(signalList.stream().map(String::valueOf).toList());
        }
        if (args.get("data_logging_interval") instanceof Number interval) {
            builder.dataLoggingInterval(interval.intValue());
        }
        applyParameterOverrides(builder, args);
        applyMatrixSolver(builder, args);
        applyAdaptive(builder, args);
        applySemiconductorModel(builder, args);
        applyThermal(builder, args);
        applyMagnetic(builder, args);
    }

    private static void applyParameterOverrides(SimulationConfig.Builder builder, Map<String, Object> args) {
        if (!(args.get("parameter_overrides") instanceof Map<?, ?> overrides)) {
            return;
        }
        Map<String, Double> parsed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : overrides.entrySet()) {
            if (!(entry.getValue() instanceof Number number)) {
                throw new IllegalArgumentException(
                        "parameter_overrides values must be numbers: " + entry.getKey());
            }
            parsed.put(String.valueOf(entry.getKey()), number.doubleValue());
        }
        if (!parsed.isEmpty()) {
            builder.withParameters(parsed);
        }
    }

    private static void applyMatrixSolver(SimulationConfig.Builder builder, Map<String, Object> args) {
        String kind = str(args, "matrix_solver", "auto");
        switch (kind.toLowerCase(Locale.ROOT)) {
            case "auto" -> builder.matrixSolverKind(MatrixSolverKind.AUTO);
            case "dense" -> builder.matrixSolverKind(MatrixSolverKind.DENSE);
            case "sparse" -> builder.matrixSolverKind(MatrixSolverKind.SPARSE);
            default -> throw new IllegalArgumentException(
                    "matrix_solver must be auto | dense | sparse, got: " + kind);
        }
    }

    private static void applyAdaptive(SimulationConfig.Builder builder, Map<String, Object> args) {
        if (!(args.get("adaptive") instanceof Map<?, ?> adaptive)) {
            return;
        }
        if (Boolean.TRUE.equals(adaptive.get("enabled"))) {
            builder.adaptiveStepSize(true);
        }
        if (adaptive.get("relative_tolerance") instanceof Number tolerance) {
            builder.relativeTolerance(tolerance.doubleValue());
        }
        if (adaptive.get("min_step") instanceof Number minStep) {
            builder.minStepWidth(minStep.doubleValue());
        }
        if (adaptive.get("max_step") instanceof Number maxStep) {
            builder.maxStepWidth(maxStep.doubleValue());
        }
    }

    private static void applySemiconductorModel(SimulationConfig.Builder builder, Map<String, Object> args) {
        String model = str(args, "semiconductor_model", "classic_pwl");
        switch (model.toLowerCase(Locale.ROOT)) {
            case "classic_pwl", "classic" -> builder.semiconductorModel(
                    SemiconductorModelKind.CLASSIC_PIECEWISE_LINEAR);
            case "shockley_nr", "shockley" -> builder.semiconductorModel(
                    SemiconductorModelKind.SHOCKLEY_NEWTON_RAPHSON);
            default -> throw new IllegalArgumentException(
                    "semiconductor_model must be classic_pwl | shockley_nr, got: " + model);
        }
    }

    private static void applyThermal(SimulationConfig.Builder builder, Map<String, Object> args) {
        if (!(args.get("thermal") instanceof Map<?, ?> thermal)) {
            return;
        }
        double ambient = optionalDouble(thermal, "ambient_temperature", DEFAULT_AMBIENT_TEMPERATURE);
        builder.enableThermalDomain(true).ambientTemperature(ambient);
        if (!(thermal.get("couplings") instanceof List<?> couplings)) {
            return;
        }
        for (Object item : couplings) {
            builder.thermalCoupling(parseCoupling(asMap(item, "thermal coupling")));
        }
    }

    private static ThermalCoupling parseCoupling(Map<?, ?> coupling) {
        String device = str(coupling, "device", "");
        if (device.isBlank()) {
            throw new IllegalArgumentException("thermal coupling requires a 'device' component name");
        }
        Map<?, ?> modelMap = asMap(coupling.get("model"), "thermal model");
        ThermalRCModel model = parseThermalModel(modelMap, device);
        String kind = str(coupling, "kind", COUPLING_KIND_RESISTOR).toLowerCase(Locale.ROOT);
        return switch (kind) {
            case COUPLING_KIND_RESISTOR -> ThermalCoupling.forResistor(device, model,
                    optionalDouble(coupling, "temperature_coefficient", 0.0));
            case COUPLING_KIND_DIODE -> ThermalCoupling.forDiode(device, model,
                    optionalDouble(coupling, "forward_voltage_slope", 0.0));
            default -> throw new IllegalArgumentException(
                    "thermal coupling kind must be resistor | diode, got: " + kind);
        };
    }

    private static ThermalRCModel parseThermalModel(Map<?, ?> model, String device) {
        double[] rTh = doubleArray(model.get("r_th"), "r_th");
        String kind = str(model, "kind", MODEL_KIND_FOSTER).toLowerCase(Locale.ROOT);
        return switch (kind) {
            case MODEL_KIND_FOSTER -> ThermalRCModel.foster(rTh,
                    doubleArray(model.get("tau"), "tau"), DEFAULT_AMBIENT_TEMPERATURE);
            case MODEL_KIND_CAUER -> ThermalRCModel.cauer(rTh,
                    doubleArray(model.get("c_th"), "c_th"), DEFAULT_AMBIENT_TEMPERATURE);
            default -> throw new IllegalArgumentException(
                    "thermal model kind must be foster | cauer, got: " + kind + " (device " + device + ")");
        };
    }

    private static void applyMagnetic(SimulationConfig.Builder builder, Map<String, Object> args) {
        if (!(args.get("magnetic") instanceof Map<?, ?> magnetic)) {
            return;
        }
        if (!(magnetic.get("networks") instanceof List<?> networks) || networks.isEmpty()) {
            throw new IllegalArgumentException("magnetic.networks must list exactly one network");
        }
        if (networks.size() > 1) {
            throw new IllegalArgumentException(
                    "the engine supports one magnetic network per run, got " + networks.size());
        }
        Map<?, ?> network = asMap(networks.get(0), "magnetic network");
        MagneticNetworkSolver solver = new MagneticNetworkSolver(
                SolverType.SOLVER_BE);
        if (network.get("windings") instanceof List<?> windings) {
            for (Object item : windings) {
                solver.addWinding(parseWinding(asMap(item, "magnetic winding")));
            }
        }
        if (network.get("branches") instanceof List<?> branches) {
            for (Object item : branches) {
                parseBranch(asMap(item, "magnetic branch"), solver);
            }
        }
        builder.enableMagneticDomain(true).magneticNetwork(solver);
    }

    private static MagneticWinding parseWinding(Map<?, ?> winding) {
        String name = str(winding, "name", "");
        if (name.isBlank()) {
            throw new IllegalArgumentException("magnetic winding requires a 'name' (the bound LK_L inductor name)");
        }
        int turns = (int) optionalDouble(winding, "turns", 1.0);
        return new MagneticWinding(name, turns,
                MagneticNode.of((int) optionalDouble(winding, "node_a", 0.0)),
                MagneticNode.of((int) optionalDouble(winding, "node_b", 0.0)));
    }

    private static void parseBranch(Map<?, ?> branch, MagneticNetworkSolver solver) {
        String name = str(branch, "name", "");
        if (name.isBlank()) {
            throw new IllegalArgumentException("magnetic branch requires a 'name'");
        }
        MagneticNode nodeA = MagneticNode.of((int) optionalDouble(branch, "node_a", 0.0));
        MagneticNode nodeB = MagneticNode.of((int) optionalDouble(branch, "node_b", 0.0));
        if (branch.get("reluctance") instanceof Number reluctance) {
            solver.addReluctance(name, nodeA, nodeB, ReluctanceBranch.ofReluctance(reluctance.doubleValue()));
            return;
        }
        if (branch.get("air_gap") instanceof Map<?, ?> gap) {
            solver.addReluctance(name, nodeA, nodeB, ReluctanceBranch.ofAirGap(
                    optionalDouble(gap, "length_m", 1e-3),
                    optionalDouble(gap, "cross_section_m2", 1e-4)));
            return;
        }
        if (branch.get("core") instanceof Map<?, ?> core) {
            solver.addReluctance(name, nodeA, nodeB, ReluctanceBranch.ofCore(
                    optionalDouble(core, "length_m", 1e-1),
                    optionalDouble(core, "cross_section_m2", 1e-4),
                    optionalDouble(core, "relative_permeability", 1000.0)));
            return;
        }
        if (branch.get("nonlinear") instanceof Map<?, ?> nonlinear) {
            NonlinearReluctance.CurveKind curve = switch (str(nonlinear, "curve", "tanh")
                    .toUpperCase(Locale.ROOT)) {
                case "FROELICH" -> NonlinearReluctance.CurveKind.FROELICH;
                case "ARCTANGENT" -> NonlinearReluctance.CurveKind.ARCTANGENT;
                case "PIECEWISE_LINEAR", "PWL" -> NonlinearReluctance.CurveKind.PIECEWISE_LINEAR;
                default -> NonlinearReluctance.CurveKind.TANH;
            };
            if (nonlinear.get("ratio") instanceof Number ratio) {
                solver.addNonlinearReluctance(name, nodeA, nodeB, NonlinearReluctance.of(
                        curve, optionalDouble(nonlinear, "permeance", 1e-3),
                        optionalDouble(nonlinear, "saturation_flux", 1e-2), ratio.doubleValue()));
            } else {
                solver.addNonlinearReluctance(name, nodeA, nodeB, NonlinearReluctance.of(
                        curve, optionalDouble(nonlinear, "permeance", 1e-3),
                        optionalDouble(nonlinear, "saturation_flux", 1e-2)));
            }
            return;
        }
        throw new IllegalArgumentException(
                "magnetic branch '" + name + "' needs one of: reluctance | air_gap | core | nonlinear");
    }

    /**
     * Enumerates the recordable channels of a circuit without running it:
     * probe outputs, labeled control taps, net labels, semiconductor loss
     * channels and the multi-domain channels implied by the thermal/magnetic
     * configuration.
     */
    static List<Map<String, Object>> listSignals(Path circuit, Map<String, Object> args)
            throws IOException, gecko.core.io.CircuitFileParser.CircuitParseException {
        CircuitModel model = new CircuitFileParser().parse(circuit.toFile().getAbsolutePath());
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        ControlCalculatorBuilder.ControlCoupling coupling =
                ControlCalculatorBuilder.build(model, netlist);

        List<Map<String, Object>> signals = new ArrayList<>();
        for (String name : coupling.probeSignalNames()) {
            signals.add(signal(name, "probe", "voltmeter/ammeter output"));
        }
        for (ControlCalculatorBuilder.SignalTap tap : coupling.signalTaps()) {
            signals.add(signal(tap.name(), "control_tap", "labeled control block output"));
        }
        if (netlist.getLabelResolver() != null) {
            for (String label : netlist.getLabelResolver().getAllLabels()) {
                signals.add(signal(label, "node_label", "power net label voltage"));
            }
        }

        SemiconductorLossEngine lossEngine = new SemiconductorLossEngine();
        lossEngine.initializeFromNetlist(netlist);
        signals.add(signal(SemiconductorLossEngine.SIGNAL_TOTAL_LOSS, "loss", "total power loss [W]"));
        signals.add(signal(SemiconductorLossEngine.SIGNAL_TOTAL_CONDUCTION, "loss", "total conduction loss [W]"));
        signals.add(signal(SemiconductorLossEngine.SIGNAL_TOTAL_SWITCHING, "loss", "total switching loss [W]"));
        signals.add(signal(SemiconductorLossEngine.SIGNAL_TOTAL_ENERGY, "loss", "cumulative loss energy [J]"));
        for (SemiconductorDeviceLossModel device : lossEngine.getDeviceList()) {
            String name = device.getName();
            signals.add(signal(SemiconductorLossEngine.PREFIX_DEVICE_LOSS + name, "loss",
                    "total loss of " + name + " [W]"));
            signals.add(signal(SemiconductorLossEngine.PREFIX_DEVICE_CONDUCTION + name, "loss",
                    "conduction loss of " + name + " [W]"));
            signals.add(signal(SemiconductorLossEngine.PREFIX_DEVICE_SWITCHING + name, "loss",
                    "switching loss of " + name + " [W]"));
        }

        if (args.get("thermal") instanceof Map<?, ?> thermal
                && thermal.get("couplings") instanceof List<?> couplings) {
            for (Object item : couplings) {
                Map<?, ?> c = asMap(item, "thermal coupling");
                signals.add(signal("Tj_" + str(c, "device", ""), "thermal",
                        "junction temperature of the coupled device [degC]"));
            }
        }
        if (args.get("magnetic") instanceof Map<?, ?> magnetic
                && magnetic.get("networks") instanceof List<?> networks) {
            for (Object item : networks) {
                Map<?, ?> network = asMap(item, "magnetic network");
                if (network.get("windings") instanceof List<?> windings) {
                    for (Object w : windings) {
                        signals.add(signal("Phi_" + str(asMap(w, "winding"), "name", ""), "magnetic",
                                "winding flux [Wb]"));
                    }
                }
            }
        }
        return signals;
    }

    /** Solver name mapping shared with the legacy string-based tool argument. */
    static SolverType solver(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "be", "backward-euler", "0" -> SolverType.SOLVER_BE;
            case "trz", "trapezoidal", "1" -> SolverType.SOLVER_TRZ;
            case "gs", "gear-shichman", "2" -> SolverType.SOLVER_GS;
            default -> throw new IllegalArgumentException("Unknown solver: " + name);
        };
    }

    private static Map<String, Object> signal(String name, String kind, String description) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("kind", kind);
        entry.put("description", description);
        return entry;
    }

    private static Map<?, ?> asMap(Object value, String what) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(what + " must be an object");
        }
        return map;
    }

    private static double[] doubleArray(Object value, String what) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(what + " must be an array of numbers");
        }
        return list.stream().mapToDouble(v -> {
            if (v instanceof Number number) {
                return number.doubleValue();
            }
            throw new IllegalArgumentException(what + " must contain only numbers, got: " + v);
        }).toArray();
    }

    private static String str(Map<?, ?> args, String key, String fallback) {
        Object value = args.get(key);
        return value != null ? String.valueOf(value) : fallback;
    }

    private static double optionalDouble(Map<?, ?> args, String key, double fallback) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return fallback;
    }

    /** Thin holder isolating the engine dependency for mocking clarity. */
    private static final class HeadlessEngineHolder {
        SimulationResult run(SimulationConfig config) {
            return new gecko.core.simulation.HeadlessSimulationEngine().runSimulation(config);
        }
    }
}
