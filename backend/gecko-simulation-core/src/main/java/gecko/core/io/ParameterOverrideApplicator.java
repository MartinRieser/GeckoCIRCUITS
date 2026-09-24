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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies parameter overrides to a {@link CircuitModel} before simulation.
 *
 * <p>Parameters are specified using dot-notation paths of the form
 * {@code "ComponentName.parameterKey"} where {@code ComponentName} matches the
 * component's name (case-sensitive) and {@code parameterKey} is the parameter key
 * stored in {@link CircuitModel.ComponentData}.</p>
 *
 * <p>All circuit, control, and thermal component domains are searched.
 * Unmatched paths are recorded in the result but do not cause exceptions.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * Map<String, Double> overrides = Map.of(
 *     "R1.resistance", 100.0,
 *     "C1.capacitance", 1e-6
 * );
 * OverrideResult result = ParameterOverrideApplicator.applyOverrides(model, overrides);
 * System.out.println("Applied: " + result.appliedCount());
 * }</pre>
 */
public final class ParameterOverrideApplicator {

    private ParameterOverrideApplicator() {
        // utility class
    }

    /**
     * Result of an {@link #applyOverrides} call, capturing counts and path details.
     *
     * @param appliedCount   number of overrides successfully applied
     * @param failedCount    number of overrides that could not be applied
     * @param appliedPaths   dot-notation paths that were successfully applied
     * @param unmatchedPaths dot-notation paths that did not match any component/parameter
     */
    public record OverrideResult(
            int appliedCount,
            int failedCount,
            List<String> appliedPaths,
            List<String> unmatchedPaths
    ) {
        /** Returns {@code true} if every override was applied successfully. */
        public boolean allApplied() {
            return failedCount == 0;
        }
    }

    /**
     * Applies the given parameter overrides to the circuit model.
     *
     * <p>Each entry in {@code overrides} must follow the format
     * {@code "ComponentName.parameterKey"}. Entries with missing dots or
     * whose component/parameter cannot be found are silently skipped and
     * reported in {@link OverrideResult#unmatchedPaths()}.</p>
     *
     * @param model     the circuit model to modify (must not be {@code null})
     * @param overrides map of dot-notation paths to replacement values
     * @return an {@link OverrideResult} describing what was applied
     * @throws IllegalArgumentException if {@code model} is {@code null}
     */
    public static OverrideResult applyOverrides(CircuitModel model, Map<String, Double> overrides) {
        if (model == null) {
            throw new IllegalArgumentException("CircuitModel must not be null");
        }

        List<String> appliedPaths = new ArrayList<>();
        List<String> unmatchedPaths = new ArrayList<>();

        if (overrides == null || overrides.isEmpty()) {
            return new OverrideResult(0, 0, appliedPaths, unmatchedPaths);
        }

        for (Map.Entry<String, Double> entry : overrides.entrySet()) {
            String path = entry.getKey();
            Double value = entry.getValue();

            ComponentParam cp = resolveComponentParam(model, path);
            if (cp == null) {
                unmatchedPaths.add(path != null ? path : "<null>");
                continue;
            }

            // Override if the parameter key already exists or is a recognized semantic alias
            if (cp.component().getParameters().containsKey(cp.parameterKey())
                    || isKnownSemanticAlias(cp.component(), cp.parameterKey())) {
                cp.component().setParameter(cp.parameterKey(), value);
                appliedPaths.add(path);
            } else {
                unmatchedPaths.add(path);
            }
        }

        return new OverrideResult(appliedPaths.size(), unmatchedPaths.size(), appliedPaths, unmatchedPaths);
    }

    /**
     * Applies parameter overrides and also allows creating new parameter keys
     * (i.e. does not require the key to pre-exist).
     *
     * @param model     the circuit model to modify (must not be {@code null})
     * @param overrides map of dot-notation paths to replacement values
     * @return an {@link OverrideResult} describing what was applied
     * @throws IllegalArgumentException if {@code model} is {@code null}
     */
    public static OverrideResult applyOverridesForce(CircuitModel model, Map<String, Double> overrides) {
        if (model == null) {
            throw new IllegalArgumentException("CircuitModel must not be null");
        }

        List<String> appliedPaths = new ArrayList<>();
        List<String> unmatchedPaths = new ArrayList<>();

        if (overrides == null || overrides.isEmpty()) {
            return new OverrideResult(0, 0, appliedPaths, unmatchedPaths);
        }

        for (Map.Entry<String, Double> entry : overrides.entrySet()) {
            String path = entry.getKey();
            Double value = entry.getValue();

            ComponentParam cp = resolveComponentParam(model, path);
            if (cp == null) {
                unmatchedPaths.add(path != null ? path : "<null>");
                continue;
            }

            cp.component().setParameter(cp.parameterKey(), value);
            appliedPaths.add(path);
        }

        return new OverrideResult(appliedPaths.size(), unmatchedPaths.size(), appliedPaths, unmatchedPaths);
    }

    private record ComponentParam(CircuitModel.ComponentData component, String parameterKey) {}

    private static ComponentParam resolveComponentParam(CircuitModel model, String path) {
        if (path == null || !path.contains(".")) {
            return null;
        }
        // Try lastDot first (handles classic Gecko names like C.1, L.1, SIGNAL.1)
        int lastDot = path.lastIndexOf('.');
        String compName = path.substring(0, lastDot);
        String paramKey = path.substring(lastDot + 1);
        CircuitModel.ComponentData found = findComponent(model, compName);
        if (found != null && !paramKey.isEmpty()) {
            return new ComponentParam(found, paramKey);
        }
        // Fallback to firstDot if lastDot didn't match (for single-dot R1.resistance)
        int firstDot = path.indexOf('.');
        if (firstDot != lastDot) {
            compName = path.substring(0, firstDot);
            paramKey = path.substring(firstDot + 1);
            found = findComponent(model, compName);
            if (found != null && !paramKey.isEmpty()) {
                return new ComponentParam(found, paramKey);
            }
        }
        return null;
    }

    private static boolean isKnownSemanticAlias(CircuitModel.ComponentData comp, String key) {
        if (key == null || comp == null) return false;
        int typ = comp.getType();
        String family = comp.getFamily();
        if ("CONTROL".equals(family)) {
            if (typ == 4) { // TYP_SIGNAL_SOURCE
                return switch (key) {
                    case "duty", "tastverhaeltnis", "frequency", "frequenz", "amplitude", "offset" -> true;
                    default -> false;
                };
            }
        } else {
            return switch (key) {
                case "initialVoltage", "uC0" -> typ == 3;
                case "initialCurrent", "iL0" -> typ == 2;
                case "resistance" -> typ == 1 || typ == 7;
                case "inductance" -> typ == 2;
                case "capacitance" -> typ == 3;
                case "voltage" -> typ == 4;
                default -> false;
            };
        }
        return false;
    }

    /**
     * Searches all component domains (circuit, control, thermal) for a component
     * with the given name. Returns the first match or {@code null} if not found.
     */
    private static CircuitModel.ComponentData findComponent(CircuitModel model, String name) {
        for (CircuitModel.ComponentData c : model.getCircuitComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        for (CircuitModel.ComponentData c : model.getControlComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        for (CircuitModel.ComponentData c : model.getThermalComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }
}
