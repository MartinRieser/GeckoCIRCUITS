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
import gecko.core.magnetic.MagneticNetworkSolver;
import gecko.core.thermal.ThermalCoupling;

import java.util.ArrayList;
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

    /** Default ambient temperature of the thermal domain in degrees Celsius. */
    public static final double DEFAULT_AMBIENT_TEMPERATURE = 25.0;

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
    private final boolean thermalDomainEnabled;
    private final double ambientTemperature;
    private final List<ThermalCoupling> thermalCouplings;
    private final boolean magneticDomainEnabled;
    private final List<MagneticNetworkSolver> magneticNetworks;

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
        this.thermalDomainEnabled = builder.thermalDomainEnabled;
        this.ambientTemperature = builder.ambientTemperature;
        this.thermalCouplings = List.copyOf(builder.thermalCouplings);
        this.magneticDomainEnabled = builder.magneticDomainEnabled;
        this.magneticNetworks = List.copyOf(builder.magneticNetworks);
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
     * Checks if the thermal co-simulation domain is enabled.
     *
     * @return true if the configured thermal couplings are simulated and their
     *         temperatures feed back into the electrical domain
     */
    public boolean isThermalDomainEnabled() {
        return thermalDomainEnabled;
    }

    /**
     * Gets the ambient temperature of the thermal domain.
     *
     * @return ambient temperature in degrees Celsius; also the fallback
     *         junction temperature of loss devices without a thermal model
     */
    public double getAmbientTemperature() {
        return ambientTemperature;
    }

    /**
     * Gets the electro-thermal device couplings.
     *
     * @return unmodifiable list of thermal couplings
     */
    public List<ThermalCoupling> getThermalCouplings() {
        return thermalCouplings;
    }

    /**
     * Checks if the magnetic co-simulation domain is enabled.
     *
     * @return true if the configured magnetic networks are stepped and their
     *         induced EMFs drive the coupled electrical windings
     */
    public boolean isMagneticDomainEnabled() {
        return magneticDomainEnabled;
    }

    /**
     * Gets the magnetic networks of the co-simulation. Each winding of a
     * network binds to the electrical component carrying its name (an LK_L
     * inductor whose inductance the network updates to the differential
     * inductance of the solved operating point).
     *
     * @return unmodifiable list of magnetic networks
     */
    public List<MagneticNetworkSolver> getMagneticNetworks() {
        return magneticNetworks;
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
        private boolean thermalDomainEnabled;
        private double ambientTemperature = DEFAULT_AMBIENT_TEMPERATURE;
        private final List<ThermalCoupling> thermalCouplings = new ArrayList<>();
        private boolean magneticDomainEnabled;
        private final List<MagneticNetworkSolver> magneticNetworks = new ArrayList<>();

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
         * Enables the thermal co-simulation domain. With thermal couplings
         * configured, the engine feeds each coupled device's dissipation into
         * its thermal RC network, steps it with the electrical time step and
         * applies the solved temperatures back to the electrical parameters.
         *
         * @param enable true to simulate the thermal domain
         * @return this builder
         */
        public Builder enableThermalDomain(boolean enable) {
            this.thermalDomainEnabled = enable;
            return this;
        }

        /**
         * Sets the ambient temperature of the thermal domain. Also serves as
         * the fallback junction temperature of loss devices without a thermal
         * model.
         *
         * @param ambientTemperatureC ambient temperature in degrees Celsius
         * @return this builder
         *
         * @throws IllegalArgumentException if the temperature is not finite
         */
        public Builder ambientTemperature(double ambientTemperatureC) {
            if (!Double.isFinite(ambientTemperatureC)) {
                throw new IllegalArgumentException("Ambient temperature must be finite, got: "
                        + ambientTemperatureC);
            }
            this.ambientTemperature = ambientTemperatureC;
            return this;
        }

        /**
         * Adds an electro-thermal device coupling.
         *
         * @param coupling coupling between an electrical component and a
         *                 thermal RC model
         * @return this builder
         *
         * @throws IllegalArgumentException if the coupling is null
         */
        public Builder thermalCoupling(ThermalCoupling coupling) {
            if (coupling == null) {
                throw new IllegalArgumentException("Thermal coupling must not be null");
            }
            this.thermalCouplings.add(coupling);
            return this;
        }

        /**
         * Enables the magnetic co-simulation domain. With magnetic networks
         * configured, the engine feeds each winding's electrical current into
         * its network, steps it with the electrical time step and drives the
         * coupled electrical winding source with the induced EMF
         * {@code v = N * dPhi/dt}.
         *
         * @param enable true to simulate the magnetic domain
         * @return this builder
         */
        public Builder enableMagneticDomain(boolean enable) {
            this.magneticDomainEnabled = enable;
            return this;
        }

        /**
         * Adds a magnetic network to the co-simulation. The network's windings
         * bind by name to electrical LK_L inductors; a network instance is
         * consumed by a single simulation run (its flux history carries run
         * state).
         *
         * @param network fully assembled magnetic network (branches and
         *                windings registered before the run)
         * @return this builder
         *
         * @throws IllegalArgumentException if the network is null
         */
        public Builder magneticNetwork(MagneticNetworkSolver network) {
            if (network == null) {
                throw new IllegalArgumentException("Magnetic network must not be null");
            }
            this.magneticNetworks.add(network);
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
            if (thermalDomainEnabled && thermalCouplings.isEmpty()) {
                throw new IllegalArgumentException(
                        "Thermal domain enabled but no thermal couplings configured");
            }
            if (magneticDomainEnabled && magneticNetworks.isEmpty()) {
                throw new IllegalArgumentException(
                        "Magnetic domain enabled but no magnetic networks configured");
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
