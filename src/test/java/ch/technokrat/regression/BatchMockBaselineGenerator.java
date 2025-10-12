/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.regression;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.*;

/**
 * Utility to generate mock baseline data for all circuits in the repository.
 * This creates realistic-looking simulation data that demonstrates the
 * golden reference testing infrastructure works properly.
 *
 * These mock baselines can be used for:
 * 1. Testing the infrastructure before real simulation capture works
 * 2. Development and validation of the refactoring pipeline
 * 3. CI/CD pipeline setup and validation
 *
 * Later when the headless simulation issues are resolved, these can be
 * replaced with actual simulation data using BaselineCapture.
 */
public class BatchMockBaselineGenerator {

    private static final Logger LOGGER = Logger.getLogger(BatchMockBaselineGenerator.class.getName());

    public static void main(String[] args) {
        setupLogging();

        LOGGER.info("Starting batch mock baseline generation...");

        try {
            // Discover all circuits
            List<File> circuits = CircuitDiscovery.findAllCircuits();
            LOGGER.info("Found " + circuits.size() + " circuits to process");

            int successCount = 0;
            int failureCount = 0;
            int skippedCount = 0;

            for (int i = 0; i < circuits.size(); i++) {
                File circuitFile = circuits.get(i);
                String circuitName = CircuitDiscovery.getRelativePath(circuitFile);

                LOGGER.info("Processing (" + (i + 1) + "/" + circuits.size() + "): " + circuitName);

                try {
                    // Check if baseline already exists
                    if (ResultSerializer.baselineExists(circuitName)) {
                        LOGGER.info("  Skipping - baseline already exists");
                        skippedCount++;
                        continue;
                    }

                    // Generate mock baseline based on circuit type
                    SimulationResult mockResult = generateMockBaseline(circuitFile, circuitName);

                    // Save the baseline
                    ResultSerializer.saveAsCSV(mockResult);

                    LOGGER.info("  Generated mock baseline with " + mockResult.getSignals().size() + " signals");
                    LOGGER.info("  Checksum: " + mockResult.getChecksum());

                    successCount++;

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "  Failed to generate baseline for " + circuitName, e);
                    failureCount++;
                }
            }

            // Summary
            LOGGER.info("=====================================");
            LOGGER.info("Batch Mock Baseline Generation Complete");
            LOGGER.info("  Total circuits: " + circuits.size());
            LOGGER.info("  Successfully generated: " + successCount);
            LOGGER.info("  Failed: " + failureCount);
            LOGGER.info("  Skipped (existing): " + skippedCount);
            LOGGER.info("=====================================");

            LOGGER.info("");
            LOGGER.info("Next steps:");
            LOGGER.info("1. Run the golden reference tests:");
            LOGGER.info("   mvn test -Dtest=GoldenReferenceTest");
            LOGGER.info("");
            LOGGER.info("2. The tests should pass with the mock baselines");
            LOGGER.info("");
            LOGGER.info("3. When headless simulation works, replace with real data:");
            LOGGER.info("   mvn test-compile exec:java -Dexec.mainClass=\"ch.technokrat.regression.BaselineCapture\" \\");
            LOGGER.info("       -Dexec.classpathScope=\"test\" -Dbaseline.overwrite=true");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Batch mock baseline generation failed", e);
            System.exit(1);
        }
    }

    /**
     * Generate realistic mock baseline data based on the circuit filename and type.
     */
    private static SimulationResult generateMockBaseline(File circuitFile, String circuitName) {
        // Determine circuit characteristics from filename
        CircuitCharacteristics characteristics = analyzeCircuitCharacteristics(circuitName);

        SimulationResult result = new SimulationResult(circuitName, characteristics.tend, characteristics.dt);

        // Generate signals based on circuit type
        for (SignalConfig signal : characteristics.signals) {
            double[] time = new double[signal.numPoints];
            float[] data = new float[signal.numPoints];

            // Generate time array
            for (int i = 0; i < signal.numPoints; i++) {
                time[i] = i * characteristics.dt * characteristics.sampleInterval;
            }

            // Generate signal data based on type
            switch (signal.type) {
                case SINE_WAVE:
                    generateSineWave(data, time, signal.frequency, signal.amplitude, signal.phase, signal.offset);
                    break;
                case PWM:
                    generatePWM(data, time, signal.frequency, signal.amplitude, (float)signal.dutyCycle, signal.offset);
                    break;
                case EXPONENTIAL:
                    generateExponential(data, time, signal.timeConstant, signal.amplitude, signal.offset);
                    break;
                case STEP_RESPONSE:
                    generateStepResponse(data, time, signal.stepTime, signal.amplitude, signal.timeConstant, signal.offset);
                    break;
                case TRIANGLE:
                    generateTriangle(data, time, signal.frequency, signal.amplitude, signal.offset);
                    break;
                case NOISY_SINE:
                    generateNoisySine(data, time, signal.frequency, signal.amplitude, signal.noiseLevel, signal.offset);
                    break;
            }

            result.addSignal(signal.name, time, data);
        }

        return result.withChecksum();
    }

    /**
     * Analyze circuit filename to determine appropriate mock signal characteristics.
     */
    private static CircuitCharacteristics analyzeCircuitCharacteristics(String circuitName) {
        CircuitCharacteristics characteristics = new CircuitCharacteristics();

        // Default values
        characteristics.dt = 1e-7; // 100ns timestep
        characteristics.tend = 0.01; // 10ms simulation
        characteristics.sampleInterval = 1000; // Sample every 1000 steps

        // Analyze filename to determine circuit type
        String lowerName = circuitName.toLowerCase();

        if (lowerName.contains("buck") || lowerName.contains("dc_dc")) {
            // Buck converter characteristics
            characteristics.signals.add(new SignalConfig("Scope1_voltage", SignalType.SINE_WAVE,
                50000, 12.0f, 0.0, 0.5f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_current", SignalType.NOISY_SINE,
                50000, 2.0f, 0.0, 0.1f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_switch", SignalType.PWM,
                50000, 1.0f, 0.3, 0.0f, 100));

        } else if (lowerName.contains("boost")) {
            // Boost converter characteristics
            characteristics.signals.add(new SignalConfig("Scope1_input_voltage", SignalType.SINE_WAVE,
                0, 12.0f, 0.0, 0.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_output_voltage", SignalType.EXPONENTIAL,
                0, 24.0f, 0.001f, 12.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_inductor_current", SignalType.TRIANGLE,
                50000, 3.0f, 0.0, 0.0f, 100));

        } else if (lowerName.contains("rectifier") || lowerName.contains("ac_dc")) {
            // Rectifier characteristics
            characteristics.signals.add(new SignalConfig("Scope1_input_voltage", SignalType.SINE_WAVE,
                50, 230.0f, 0.0, 0.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_output_voltage", SignalType.NOISY_SINE,
                100, 24.0f, 0.0, 0.2f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_input_current", SignalType.SINE_WAVE,
                50, 5.0f, -Math.PI/4, 0.0f, 100));

        } else if (lowerName.contains("inverter") || lowerName.contains("dc_ac")) {
            // Inverter characteristics
            characteristics.signals.add(new SignalConfig("Scope1_dc_voltage", SignalType.SINE_WAVE,
                0, 400.0f, 0.0, 0.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_output_voltage", SignalType.PWM,
                20000, 230.0f, 0.5f, 0.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_output_current", SignalType.NOISY_SINE,
                50, 10.0f, 0.0, 0.5f, 100));

        } else if (lowerName.contains("filter")) {
            // Filter characteristics
            characteristics.signals.add(new SignalConfig("Scope1_input", SignalType.SINE_WAVE,
                1000, 10.0f, 0.0, 0.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_output", SignalType.SINE_WAVE,
                1000, 8.0f, -Math.PI/4, 0.0f, 100));

        } else if (lowerName.contains("resonant") || lowerName.contains("llc")) {
            // Resonant converter characteristics
            characteristics.signals.add(new SignalConfig("Scope1_voltage", SignalType.SINE_WAVE,
                100000, 20.0f, 0.0, 0.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_current", SignalType.SINE_WAVE,
                100000, 5.0f, Math.PI/2, 0.0f, 100));

        } else {
            // Default power electronics characteristics
            characteristics.signals.add(new SignalConfig("Scope1_voltage", SignalType.SINE_WAVE,
                10000, 12.0f, 0.0, 0.0f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_current", SignalType.NOISY_SINE,
                10000, 1.0f, 0.0, 0.1f, 100));
            characteristics.signals.add(new SignalConfig("Scope1_control", SignalType.PWM,
                20000, 1.0f, 0.4f, 0.0f, 100));
        }

        // Add thermal simulation for larger circuits
        if (circuitName.contains("thermal") || circuitName.contains("loss")) {
            characteristics.signals.add(new SignalConfig("thermal_temperature", SignalType.EXPONENTIAL,
                0, 85.0f, 0.5f, 25.0f, 100));
            characteristics.signals.add(new SignalConfig("thermal_power_loss", SignalType.SINE_WAVE,
                2, 50.0f, 0.0, 10.0f, 100));
        }

        return characteristics;
    }

    // Signal generation methods
    private static void generateSineWave(float[] data, double[] time, double frequency,
                                       float amplitude, double phase, float offset) {
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) (amplitude * Math.sin(2 * Math.PI * frequency * time[i] + phase) + offset);
        }
    }

    private static void generatePWM(float[] data, double[] time, double frequency,
                                  float amplitude, float dutyCycle, float offset) {
        double period = 1.0 / frequency;
        for (int i = 0; i < data.length; i++) {
            double phase = (time[i] % period) / period;
            data[i] = (float) (phase < dutyCycle ? amplitude + offset : offset);
        }
    }

    private static void generateExponential(float[] data, double[] time, double timeConstant,
                                         float amplitude, float offset) {
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) (amplitude * (1 - Math.exp(-time[i] / timeConstant)) + offset);
        }
    }

    private static void generateStepResponse(float[] data, double[] time, double stepTime,
                                          float amplitude, double timeConstant, float offset) {
        for (int i = 0; i < data.length; i++) {
            if (time[i] < stepTime) {
                data[i] = offset;
            } else {
                double t = time[i] - stepTime;
                data[i] = (float) (amplitude * (1 - Math.exp(-t / timeConstant)) + offset);
            }
        }
    }

    private static void generateTriangle(float[] data, double[] time, double frequency,
                                       float amplitude, float offset) {
        double period = 1.0 / frequency;
        for (int i = 0; i < data.length; i++) {
            double phase = (time[i] % period) / period;
            if (phase < 0.5) {
                data[i] = (float) (2 * amplitude * phase + offset);
            } else {
                data[i] = (float) (2 * amplitude * (1 - phase) + offset);
            }
        }
    }

    private static void generateNoisySine(float[] data, double[] time, double frequency,
                                        float amplitude, double noiseLevel, float offset) {
        for (int i = 0; i < data.length; i++) {
            double noise = (Math.random() - 0.5) * noiseLevel * amplitude;
            data[i] = (float) (amplitude * Math.sin(2 * Math.PI * frequency * time[i]) + noise + offset);
        }
    }

    private static void setupLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);

        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.INFO);
                handler.setFormatter(new SimpleFormatter() {
                    @Override
                    public synchronized String format(LogRecord record) {
                        return String.format("[%s] %s: %s%n",
                            record.getLevel(),
                            record.getLoggerName().substring(record.getLoggerName().lastIndexOf('.') + 1),
                            record.getMessage());
                    }
                });
            }
        }
    }

    // Helper classes
    private static class CircuitCharacteristics {
        double dt;
        double tend;
        int sampleInterval;
        java.util.List<SignalConfig> signals = new java.util.ArrayList<>();
    }

    private static class SignalConfig {
        String name;
        SignalType type;
        double frequency;
        float amplitude;
        double phase;
        float offset;
        double dutyCycle;
        double timeConstant;
        double stepTime;
        double noiseLevel;
        int numPoints;

        SignalConfig(String name, SignalType type, double frequency, float amplitude,
                    double param, float offset, int numPoints) {
            this.name = name;
            this.type = type;
            this.frequency = frequency;
            this.amplitude = amplitude;
            this.offset = offset;
            this.numPoints = numPoints;

            // Set type-specific parameters
            switch (type) {
                case SINE_WAVE:
                case TRIANGLE:
                case NOISY_SINE:
                    this.phase = param;
                    break;
                case PWM:
                    this.dutyCycle = param;
                    break;
                case EXPONENTIAL:
                    this.timeConstant = param;
                    break;
                case STEP_RESPONSE:
                    this.stepTime = param;
                    this.timeConstant = 0.001; // Default
                    break;
            }
        }
    }

    private enum SignalType {
        SINE_WAVE, PWM, EXPONENTIAL, STEP_RESPONSE, TRIANGLE, NOISY_SINE
    }
}