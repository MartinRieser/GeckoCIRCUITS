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
package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a headless simulation run.
 * Encapsulates all parameters needed to execute a simulation without GUI.
 */
public final class SimulationConfig {

    /** Default relative LTE tolerance of the adaptive step controller. */
    public static final double DEFAULT_RELATIVE_TOLERANCE = 1e-3;

    private final SolverSettingsCore solverSettings;
    private final String circuitFilePath;
    private final CircuitModel circuitModel;
    private final Map<String, Double> parameterOverrides;
    private final boolean enableDataLogging;
    private final int dataLoggingInterval;
    private final List<String> signals;
    private final MatrixSolverKind matrixSolverKind;
    private final boolean adaptiveStepSize;
    private final double relativeTolerance;
    private final double minStepWidth;
    private final double maxStepWidth;
    private final SemiconductorModelKind semiconductorModel;

    private SimulationConfig(Builder builder) {
        this.solverSettings = builder.solverSettings.copy();
        this.circuitFilePath = builder.circuitFilePath;
        this.circuitModel = builder.circuitModel;
        this.parameterOverrides = Collections.unmodifiableMap(new HashMap<>(builder.parameterOverrides));
        this.enableDataLogging = builder.enableDataLogging;
        this.dataLoggingInterval = builder.dataLoggingInterval;
        this.signals = builder.signals == null ? null : List.copyOf(builder.signals);
        this.matrixSolverKind = builder.matrixSolverKind;
        this.adaptiveStepSize = builder.adaptiveStepSize;
        this.relativeTolerance = builder.relativeTolerance;
        this.minStepWidth = builder.minStepWidth;
        this.maxStepWidth = builder.maxStepWidth;
        this.semiconductorModel = builder.semiconductorModel;
    }

    /**
     * Gets the solver settings for this simulation.
     *
     * @return solver settings
     */
    public SolverSettingsCore getSolverSettings() {
        return solverSettings.copy();
    }

    /**
     * Gets the path to the circuit file (.ipes).
     *
     * @return circuit file path, or null if circuit is provided programmatically
     */
    public String getCircuitFilePath() {
        return circuitFilePath;
    }

    /**
     * Gets the in-memory circuit model, bypassing file parsing.
     *
     * @return circuit model, or null if the circuit must be loaded from {@link #getCircuitFilePath()}
     */
    public CircuitModel getCircuitModel() {
        return circuitModel;
    }

    /**
     * Gets parameter overrides for the simulation.
     * Keys are parameter names, values are the override values.
     *
     * @return unmodifiable map of parameter overrides
     */
    public Map<String, Double> getParameterOverrides() {
        return parameterOverrides;
    }

    /**
     * Checks if data logging is enabled.
     *
     * @return true if data should be logged during simulation
     */
    public boolean isDataLoggingEnabled() {
        return enableDataLogging;
    }

    /**
     * Gets the data logging interval (every Nth time step).
     *
     * @return logging interval
     */
    public int getDataLoggingInterval() {
        return dataLoggingInterval;
    }

    /**
     * Gets the explicit signal selection, overriding the circuit file's
     * dataContainerSignals when non-empty.
     *
     * @return signal names, or null to use the file's signals
     */
    public List<String> getSignals() {
        return signals;
    }

    /**
     * Gets the MNA linear-algebra backend selection.
     *
     * @return matrix solver kind (AUTO resolves by matrix size in the engine)
     */
    public MatrixSolverKind getMatrixSolverKind() {
        return matrixSolverKind;
    }

    /**
     * Checks whether adaptive step-size control is enabled.
     *
     * @return true when the engine adapts dt from the local truncation error
     */
    public boolean isAdaptiveStepSize() {
        return adaptiveStepSize;
    }

    /**
     * Gets the relative LTE tolerance of the adaptive step controller.
     *
     * @return relative tolerance
     */
    public double getRelativeTolerance() {
        return relativeTolerance;
    }

    /**
     * Gets the lower step-size bound of the adaptive controller.
     *
     * @return minimum step width; {@code <= 0} derives stepWidth/100
     */
    public double getMinStepWidth() {
        return minStepWidth;
    }

    /**
     * Gets the upper step-size bound of the adaptive controller.
     *
     * @return maximum step width; {@code <= 0} derives from stepWidth
     */
    public double getMaxStepWidth() {
        return maxStepWidth;
    }

    /**
     * Gets the semiconductor convergence model.
     *
     * @return piecewise-linear state machine or Shockley Newton-Raphson
     */
    public SemiconductorModelKind getSemiconductorModel() {
        return semiconductorModel;
    }

    /**
     * Creates a new builder for SimulationConfig.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for SimulationConfig.
     */
    public static class Builder {
        private SolverSettingsCore solverSettings = new SolverSettingsCore();
        private String circuitFilePath;
        private CircuitModel circuitModel;
        private Map<String, Double> parameterOverrides = new HashMap<>();
        private boolean enableDataLogging = true;
        private int dataLoggingInterval = 1;
        private List<String> signals;
        private MatrixSolverKind matrixSolverKind = MatrixSolverKind.AUTO;
        private boolean adaptiveStepSize = false;
        private double relativeTolerance = DEFAULT_RELATIVE_TOLERANCE;
        private double minStepWidth;
        private double maxStepWidth;
        private SemiconductorModelKind semiconductorModel = SemiconductorModelKind.CLASSIC_PIECEWISE_LINEAR;

        private Builder() {
        }

        /**
         * Sets the solver settings.
         *
         * @param solverSettings the solver configuration
         * @return this builder
         */
        public Builder solverSettings(SolverSettingsCore solverSettings) {
            if (solverSettings == null) {
                throw new IllegalArgumentException("solverSettings cannot be null");
            }
            this.solverSettings = solverSettings.copy();
            return this;
        }

        /**
         * Sets the solver type.
         *
         * @param solverType the integration method
         * @return this builder
         */
        public Builder solverType(SolverType solverType) {
            this.solverSettings.setSolverType(solverType);
            return this;
        }

        /**
         * Sets the simulation step width (dt).
         *
         * @param dt time step in seconds
         * @return this builder
         */
        public Builder stepWidth(double dt) {
            this.solverSettings.setStepWidth(dt);
            return this;
        }

        /**
         * Sets the total simulation duration.
         *
         * @param duration simulation time in seconds
         * @return this builder
         */
        public Builder simulationDuration(double duration) {
            this.solverSettings.setSimulationDuration(duration);
            return this;
        }

        /**
         * Sets the circuit file path.
         *
         * @param filePath path to .ipes circuit file
         * @return this builder
         */
        public Builder circuitFile(String filePath) {
            this.circuitFilePath = filePath;
            return this;
        }

        /**
         * Sets an in-memory circuit model, bypassing file loading.
         *
         * @param circuitModel parsed circuit model
         * @return this builder
         */
        public Builder circuitModel(CircuitModel circuitModel) {
            this.circuitModel = circuitModel;
            return this;
        }

        /**
         * Adds a parameter override.
         *
         * @param parameterName the parameter name
         * @param value the override value
         * @return this builder
         */
        public Builder withParameter(String parameterName, double value) {
            this.parameterOverrides.put(parameterName, value);
            return this;
        }

        public Builder parameterOverride(String parameterName, double value) {
            return withParameter(parameterName, value);
        }

        /**
         * Sets all parameter overrides.
         *
         * @param parameters map of parameter names to values
         * @return this builder
         */
        public Builder withParameters(Map<String, Double> parameters) {
            this.parameterOverrides.putAll(parameters);
            return this;
        }

        public Builder parameterOverrides(Map<String, Double> parameters) {
            return withParameters(parameters);
        }

        /**
         * Enables or disables data logging.
         *
         * @param enable true to enable data logging
         * @return this builder
         */
        public Builder enableDataLogging(boolean enable) {
            this.enableDataLogging = enable;
            return this;
        }

        /**
         * Sets the data logging interval.
         *
         * @param interval log every Nth time step
         * @return this builder
         */
        public Builder dataLoggingInterval(int interval) {
            this.dataLoggingInterval = Math.max(1, interval);
            return this;
        }

        /**
         * Sets the signals to record, overriding the circuit file's selection.
         *
         * @param signals signal names to record
         * @return this builder
         */
        public Builder signals(List<String> signals) {
            this.signals = signals;
            return this;
        }

        /**
         * Selects the MNA linear-algebra backend.
         *
         * @param kind DENSE, SPARSE, or AUTO (default)
         * @return this builder
         */
        public Builder matrixSolverKind(MatrixSolverKind kind) {
            this.matrixSolverKind = kind != null ? kind : MatrixSolverKind.AUTO;
            return this;
        }

        /**
         * Enables adaptive step-size control based on the local truncation
         * error between the configured solver and the complementary method.
         *
         * @param adaptive true to adapt dt within [minStepWidth, maxStepWidth]
         * @return this builder
         */
        public Builder adaptiveStepSize(boolean adaptive) {
            this.adaptiveStepSize = adaptive;
            return this;
        }

        /**
         * Sets the relative LTE tolerance of the adaptive step controller.
         *
         * @param tolerance relative tolerance (must be positive)
         * @return this builder
         */
        public Builder relativeTolerance(double tolerance) {
            this.relativeTolerance = tolerance;
            return this;
        }

        /**
         * Sets the lower step-size bound of the adaptive controller.
         *
         * @param minStep minimum step width; {@code <= 0} derives stepWidth/100
         * @return this builder
         */
        public Builder minStepWidth(double minStep) {
            this.minStepWidth = minStep;
            return this;
        }

        /**
         * Sets the upper step-size bound of the adaptive controller.
         *
         * @param maxStep maximum step width; {@code <= 0} derives from stepWidth
         * @return this builder
         */
        public Builder maxStepWidth(double maxStep) {
            this.maxStepWidth = maxStep;
            return this;
        }

        /**
         * Selects the semiconductor convergence model.
         *
         * @param model piecewise-linear state machine (default) or Shockley
         *              Newton-Raphson
         * @return this builder
         */
        public Builder semiconductorModel(SemiconductorModelKind model) {
            this.semiconductorModel = model != null ? model : SemiconductorModelKind.CLASSIC_PIECEWISE_LINEAR;
            return this;
        }

        /**
         * Builds the SimulationConfig instance.
         *
         * @return new SimulationConfig
         */
        public SimulationConfig build() {
            if (relativeTolerance <= 0.0) {
                throw new IllegalArgumentException(
                        "Relative tolerance must be positive, got: " + relativeTolerance);
            }
            return new SimulationConfig(this);
        }
    }

    @Override
    public String toString() {
        String circuit = circuitModel != null ? "in-memory model" : circuitFilePath;
        return String.format("SimulationConfig[circuit=%s, solver=%s, dt=%.2e, duration=%.2e]",
                circuit, solverSettings.getSolverType(),
                solverSettings.getStepWidth(), solverSettings.getSimulationDuration());
    }
}
