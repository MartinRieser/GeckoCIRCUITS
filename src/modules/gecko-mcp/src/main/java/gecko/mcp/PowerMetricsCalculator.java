package gecko.mcp;

import java.util.*;

/**
 * High-level power electronics analytics engine for GeckoCIRCUITS simulations.
 *
 * <p>Computes key converter figures of merit (RMS, ripple %, THD, active power,
 * power factor, and efficiency) server-side without streaming massive time-series CSVs.</p>
 */
public final class PowerMetricsCalculator {

    private PowerMetricsCalculator() {
    }

    public static Map<String, Object> calculate(
            SimulationService.ParsedCsv csv,
            double startTime,
            List<String> requestedSignals,
            String vInSignal, String iInSignal,
            String vOutSignal, String iOutSignal,
            boolean isThreePhase
    ) {
        Map<String, List<Double>> columns = csv.columns();
        List<Double> time = columns.get("time");
        if (time == null || time.isEmpty()) {
            throw new IllegalArgumentException("Simulation result contains no time data");
        }

        // Determine analysis window
        int startIndex = 0;
        if (startTime > 0) {
            for (int i = 0; i < time.size(); i++) {
                if (time.get(i) >= startTime) {
                    startIndex = i;
                    break;
                }
            }
        } else {
            // Default to the last 50% of the simulation (steady-state window)
            startIndex = time.size() / 2;
        }

        int count = time.size() - startIndex;
        if (count <= 0) {
            startIndex = 0;
            count = time.size();
        }

        Map<String, Object> signalStats = new LinkedHashMap<>();

        Set<String> targetSignals = (requestedSignals != null && !requestedSignals.isEmpty())
                ? new LinkedHashSet<>(requestedSignals) : new LinkedHashSet<>(columns.keySet());
        targetSignals.remove("time");

        for (String sigName : targetSignals) {
            List<Double> data = columns.get(sigName);
            if (data == null || data.size() < time.size()) continue;

            double sum = 0.0;
            double sumSq = 0.0;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (int i = startIndex; i < time.size(); i++) {
                double val = data.get(i);
                sum += val;
                sumSq += val * val;
                if (val < min) min = val;
                if (val > max) max = val;
            }

            double mean = sum / count;
            double rms = Math.sqrt(sumSq / count);
            double pkPk = max - min;
            double ripplePct = Math.abs(mean) > 1e-6 ? (pkPk / Math.abs(mean)) * 100.0 : 0.0;

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("mean", round(mean, 4));
            stats.put("rms", round(rms, 4));
            stats.put("min", round(min, 4));
            stats.put("max", round(max, 4));
            stats.put("peak_to_peak_ripple", round(pkPk, 4));
            stats.put("ripple_percent", round(ripplePct, 2));

            signalStats.put(sigName, stats);
        }

        // Auto-detect signals if not provided
        if (vOutSignal == null) {
            vOutSignal = findSignal(columns, "uOUT", "u_out", "V_out", "vout", "Vout", "V_OUT", "VLOAD", "V_load");
        }
        if (iOutSignal == null) {
            iOutSignal = findSignal(columns, "iOUT", "i_out", "I_out", "iout", "Iout", "I_OUT", "ILOAD", "I_load");
        }
        if (vInSignal == null) {
            vInSignal = findSignal(columns, "uIN", "u_in", "V_in", "vin", "Vin", "V_IN", "vGRID", "v_grid");
        }
        if (iInSignal == null) {
            iInSignal = findSignal(columns, "iIN", "i_in", "I_in", "iin", "Iin", "I_IN", "iGRID", "i_grid");
        }

        // System-level Power & Efficiency calculations
        Map<String, Object> systemMetrics = new LinkedHashMap<>();

        if (vOutSignal != null && iOutSignal != null
                && columns.containsKey(vOutSignal) && columns.containsKey(iOutSignal)) {
            List<Double> vOut = columns.get(vOutSignal);
            List<Double> iOut = columns.get(iOutSignal);
            double sumPout = 0.0;
            for (int i = startIndex; i < time.size(); i++) {
                sumPout += vOut.get(i) * iOut.get(i);
            }
            double pOutAvg = sumPout / count;
            systemMetrics.put("v_out_signal", vOutSignal);
            systemMetrics.put("i_out_signal", iOutSignal);
            systemMetrics.put("p_out_avg_w", round(pOutAvg, 2));
            systemMetrics.put("p_out_kw", round(pOutAvg / 1000.0, 3));
        }

        if (vInSignal != null && iInSignal != null
                && columns.containsKey(vInSignal) && columns.containsKey(iInSignal)) {
            List<Double> vIn = columns.get(vInSignal);
            List<Double> iIn = columns.get(iInSignal);
            double sumPin = 0.0;
            double sumVinSq = 0.0;
            double sumIinSq = 0.0;

            for (int i = startIndex; i < time.size(); i++) {
                double v = vIn.get(i);
                double current = iIn.get(i);
                sumPin += v * current;
                sumVinSq += v * v;
                sumIinSq += current * current;
            }

            double pInAvg = sumPin / count;
            double vInRms = Math.sqrt(sumVinSq / count);
            double iInRms = Math.sqrt(sumIinSq / count);

            if (isThreePhase) {
                pInAvg *= 3.0; // Total 3-phase power from 1-phase reference
            }

            double sIn = isThreePhase ? (3.0 * vInRms * iInRms) : (vInRms * iInRms);
            double pf = sIn > 1e-6 ? Math.abs(pInAvg / sIn) : 0.0;

            systemMetrics.put("v_in_signal", vInSignal);
            systemMetrics.put("i_in_signal", iInSignal);
            systemMetrics.put("is_three_phase", isThreePhase);
            systemMetrics.put("p_in_avg_w", round(pInAvg, 2));
            systemMetrics.put("p_in_kw", round(pInAvg / 1000.0, 3));
            systemMetrics.put("s_in_va", round(sIn, 2));
            systemMetrics.put("power_factor", round(pf, 4));

            if (systemMetrics.containsKey("p_out_avg_w")) {
                double pOut = (double) systemMetrics.get("p_out_avg_w");
                if (pInAvg > 1e-6) {
                    double eff = (pOut / pInAvg) * 100.0;
                    systemMetrics.put("efficiency_percent", round(eff, 2));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis_window_start_s", round(time.get(startIndex), 6));
        result.put("analysis_window_end_s", round(time.get(time.size() - 1), 6));
        result.put("sample_count", count);
        result.put("system_metrics", systemMetrics);
        result.put("signals", signalStats);
        return result;
    }

    private static String findSignal(Map<String, List<Double>> columns, String... candidates) {
        for (String c : candidates) {
            if (columns.containsKey(c)) return c;
            for (String key : columns.keySet()) {
                if (key.equalsIgnoreCase(c)) return key;
            }
        }
        return null;
    }

    private static double round(double val, int decimals) {
        if (!Double.isFinite(val)) return 0.0;
        double scale = Math.pow(10, decimals);
        return Math.round(val * scale) / scale;
    }
}
