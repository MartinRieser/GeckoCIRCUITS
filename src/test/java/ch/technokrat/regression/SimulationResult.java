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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Container for simulation results used in golden reference testing.
 * Stores signal data, timing information, and metadata for comparison.
 */
public class SimulationResult {

    private final String circuitName;
    private final Map<String, SignalData> signals;
    private final double simulationTime;
    private final double timestep;
    private final String checksum;

    public SimulationResult(String circuitName, double simulationTime, double timestep) {
        this.circuitName = circuitName;
        this.simulationTime = simulationTime;
        this.timestep = timestep;
        this.signals = new LinkedHashMap<>();
        this.checksum = null; // Will be computed after signals are added
    }

    private SimulationResult(String circuitName, Map<String, SignalData> signals,
                            double simulationTime, double timestep, String checksum) {
        this.circuitName = circuitName;
        this.signals = new LinkedHashMap<>(signals);
        this.simulationTime = simulationTime;
        this.timestep = timestep;
        this.checksum = checksum;
    }

    public void addSignal(String signalName, double[] timeArray, float[] values) {
        signals.put(signalName, new SignalData(signalName, timeArray, values));
    }

    public void addSignal(String signalName, SignalData data) {
        signals.put(signalName, data);
    }

    public String getCircuitName() {
        return circuitName;
    }

    public Map<String, SignalData> getSignals() {
        return new LinkedHashMap<>(signals);
    }

    public SignalData getSignal(String name) {
        return signals.get(name);
    }

    public double getSimulationTime() {
        return simulationTime;
    }

    public double getTimestep() {
        return timestep;
    }

    public String getChecksum() {
        return checksum;
    }

    /**
     * Compute SHA-256 checksum of all signal data for quick comparison.
     */
    public SimulationResult withChecksum() {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");

            for (Map.Entry<String, SignalData> entry : signals.entrySet()) {
                md.update(entry.getKey().getBytes("UTF-8"));
                SignalData data = entry.getValue();

                for (double t : data.getTimeArray()) {
                    md.update(Double.toString(t).getBytes("UTF-8"));
                }
                for (float v : data.getValues()) {
                    md.update(Float.toString(v).getBytes("UTF-8"));
                }
            }

            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return new SimulationResult(circuitName, signals, simulationTime, timestep, hexString.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimulationResult that = (SimulationResult) o;
        return Double.compare(that.simulationTime, simulationTime) == 0 &&
               Double.compare(that.timestep, timestep) == 0 &&
               Objects.equals(circuitName, that.circuitName) &&
               Objects.equals(signals, that.signals) &&
               Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(circuitName, signals, simulationTime, timestep, checksum);
    }

    /**
     * Container for individual signal time-series data.
     */
    public static class SignalData {
        private final String name;
        private final double[] timeArray;
        private final float[] values;

        public SignalData(String name, double[] timeArray, float[] values) {
            if (timeArray.length != values.length) {
                throw new IllegalArgumentException("Time and value arrays must have same length");
            }
            this.name = name;
            this.timeArray = timeArray.clone();
            this.values = values.clone();
        }

        public String getName() {
            return name;
        }

        public double[] getTimeArray() {
            return timeArray.clone();
        }

        public float[] getValues() {
            return values.clone();
        }

        public int getLength() {
            return values.length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SignalData that = (SignalData) o;
            return Objects.equals(name, that.name) &&
                   java.util.Arrays.equals(timeArray, that.timeArray) &&
                   java.util.Arrays.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(name);
            result = 31 * result + java.util.Arrays.hashCode(timeArray);
            result = 31 * result + java.util.Arrays.hashCode(values);
            return result;
        }
    }
}
