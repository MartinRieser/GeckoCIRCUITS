package gecko.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Power-electronics waveform metrics — a faithful port of the Python
 * {@code gecko_get_waveforms} metric block, including its fixed analysis
 * windows and the r_load regex quirk (kept bug-for-bug).
 */
final class WaveformAnalysis {

    private WaveformAnalysis() {
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }

    static Map<String, Object> analyse(SimulationService.ParsedCsv csv, String circuitContent,
                                       List<String> signals, int maxPoints) {
        Map<String, List<Double>> parsed = csv.columns();
        Map<String, Object> result = new LinkedHashMap<>();

        if (csv.rowCount() < 2) {
            result.put("status", "EMPTY_RESULTS");
            result.put("signals", Map.of());
            return result;
        }

        int totalRows = csv.rowCount();
        int downsample = Math.max(1, totalRows / maxPoints);

        List<String> selected = signals != null && !signals.isEmpty() ? signals : csv.header();
        Map<String, List<Double>> filtered = new LinkedHashMap<>();
        for (String name : selected) {
            List<Double> column = parsed.get(name);
            if (column != null) {
                filtered.put(name, decimate(column, downsample));
            }
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        List<Double> time = parsed.get("time");

        String voutKey = firstOf(parsed, "uOUT", "u_out", "V_out", "vout", "Vout");
        if (voutKey != null) {
            List<Double> vData = parsed.get(voutKey);
            int ssStart = (int) (vData.size() * 0.8);
            List<Double> ssSlice = vData.subList(ssStart, vData.size());
            if (!ssSlice.isEmpty()) {
                double vMean = mean(ssSlice);
                double vMin = minOf(ssSlice);
                double vMax = maxOf(ssSlice);
                double ripplePp = vMax - vMin;
                double ripplePct = Math.abs(vMean) > 1e-3 ? ripplePp / vMean * 100 : 0.0;
                double rLoad = 4.0;
                // bug-for-bug port: this regex never matches across lines, so the
                // extracted load resistance is always the 4.0 default (as in Python)
                java.util.regex.Matcher rl = java.util.regex.Pattern
                        .compile("parameter\\[\\]\\s+([0-9.]+).*?idStringDialog\\s+R\\.LOAD")
                        .matcher(circuitContent);
                if (rl.find()) {
                    rLoad = Double.parseDouble(rl.group(1));
                }
                double pOutW = rLoad > 0 ? vMean * vMean / rLoad : 0.0;
                double iOutA = rLoad > 0 ? vMean / rLoad : 0.0;

                Map<String, Object> outputVoltage = new LinkedHashMap<>();
                outputVoltage.put("signal_name", voutKey);
                outputVoltage.put("steady_state_dc_volts", round(vMean, 3));
                outputVoltage.put("min_volts", round(vMin, 3));
                outputVoltage.put("max_volts", round(vMax, 3));
                outputVoltage.put("ripple_peak_to_peak_volts", round(ripplePp, 3));
                outputVoltage.put("ripple_percentage", round(ripplePct, 2));
                outputVoltage.put("output_power_watts", round(pOutW, 1));
                outputVoltage.put("output_current_amps", round(iOutA, 2));
                outputVoltage.put("load_resistance_ohms", round(rLoad, 3));
                metrics.put("output_voltage", outputVoltage);
            }
        }

        if (voutKey != null && time != null) {
            List<Double> vArr = parsed.get(voutKey);
            List<Double> preV = window(time, vArr, 0.03, 0.048);
            List<Double> postV = window(time, vArr, 0.07, 0.098);
            if (!preV.isEmpty() && !postV.isEmpty()) {
                double vPreMean = mean(preV);
                double vPostMean = mean(postV);
                double ripPre = maxOf(preV) - minOf(preV);
                double ripPost = maxOf(postV) - minOf(postV);
                Map<String, Object> dynamicLoad = new LinkedHashMap<>();
                dynamicLoad.put("pre_step_voltage_dc", round(vPreMean, 3));
                dynamicLoad.put("pre_step_power_watts", round(vPreMean * vPreMean / 25.0, 1));
                dynamicLoad.put("pre_step_ripple_pp", round(ripPre, 3));
                dynamicLoad.put("post_step_voltage_dc", round(vPostMean, 3));
                dynamicLoad.put("post_step_power_watts", round(vPostMean * vPostMean / 12.5, 1));
                dynamicLoad.put("post_step_ripple_pp", round(ripPost, 3));
                dynamicLoad.put("load_step_regulation_error_percent",
                        round(Math.abs(vPostMean - 50.0) / 50.0 * 100, 3));
                metrics.put("dynamic_load", dynamicLoad);
            }
        }

        String vnKey = firstOf(parsed, "uN", "Vin", "v_grid", "v_in");
        String inKey = firstOf(parsed, "iN", "iL", "iin", "I_in", "i_in");
        if (vnKey != null && inKey != null) {
            List<Double> vn = parsed.get(vnKey);
            List<Double> inn = parsed.get(inKey);
            int ssStart = (int) (vn.size() * 0.5);
            List<Double> vSs = vn.subList(ssStart, vn.size());
            List<Double> iSs = inn.subList(ssStart, inn.size());
            if (!vSs.isEmpty() && vSs.size() == iSs.size()) {
                double vRms = sqrt(meanSquares(vSs));
                double iRms = sqrt(meanSquares(iSs));
                double pActive = sumProducts(vSs, iSs) / vSs.size();
                double sApparent = vRms * iRms;
                double pf = sApparent > 1e-4 ? pActive / sApparent : 0.0;
                Map<String, Object> acGrid = new LinkedHashMap<>();
                acGrid.put("v_rms", round(vRms, 2));
                acGrid.put("i_rms", round(iRms, 3));
                acGrid.put("active_power_watts", round(pActive, 2));
                acGrid.put("apparent_power_va", round(sApparent, 2));
                acGrid.put("power_factor", round(pf, 3));
                metrics.put("ac_grid", acGrid);
            }
        } else if (parsed.containsKey("1") && parsed.containsKey("2")) {
            List<Double> one = parsed.get("1");
            List<Double> two = parsed.get("2");
            List<Double> vn = new ArrayList<>();
            for (int i = 0; i < Math.min(one.size(), two.size()); i++) {
                vn.add(one.get(i) - two.get(i));
            }
            int ssStart = (int) (vn.size() * 0.5);
            if (ssStart < vn.size()) {
                List<Double> vSs = vn.subList(ssStart, vn.size());
                if (!vSs.isEmpty()) {
                    Map<String, Object> acGrid = new LinkedHashMap<>();
                    acGrid.put("v_rms", round(sqrt(meanSquares(vSs)), 2));
                    acGrid.put("frequency_hz", 50.0);
                    acGrid.put("peak_voltage", round(maxOf(vSs), 2));
                    metrics.put("ac_grid", acGrid);
                }
            }
        }

        if (parsed.containsKey("sw") && parsed.containsKey("i_lr")) {
            List<Double> sw = parsed.get("sw");
            List<Double> ilr = parsed.get("i_lr");
            int ssStart = (int) (sw.size() * 0.5);
            List<Double> swSs = sw.subList(ssStart, sw.size());
            List<Double> ilrSs = ilr.subList(ssStart, ilr.size());
            double swMin = swSs.isEmpty() ? 0.0 : minOf(swSs);
            double swMax = swSs.isEmpty() ? 0.0 : maxOf(swSs);
            double ilrPeak = ilrSs.isEmpty() ? 0.0
                    : Math.max(Math.abs(minOf(ilrSs)), Math.abs(maxOf(ilrSs)));
            boolean zvsOk = swMin < 5.0 && swMax > 350.0;
            Map<String, Object> llc = new LinkedHashMap<>();
            llc.put("zvs_soft_switching_achieved", zvsOk);
            llc.put("switch_node_min_volts", round(swMin, 3));
            llc.put("switch_node_max_volts", round(swMax, 3));
            llc.put("resonant_current_peak_amps", round(ilrPeak, 3));
            llc.put("operating_mode", zvsOk ? "Zero-Voltage Switching (ZVS) Resonance" : "Hard Switching");
            metrics.put("llc_resonant", llc);
        }

        result.put("status", "SUCCESS");
        result.put("total_time_steps", totalRows);
        result.put("returned_points", filtered.getOrDefault("time", List.of()).size());
        result.put("metrics", metrics);
        result.put("waveforms", filtered);
        return result;
    }

    private static List<Double> decimate(List<Double> values, int step) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < values.size(); i += step) {
            out.add(values.get(i));
        }
        return out;
    }

    private static List<Double> window(List<Double> time, List<Double> values,
                                       double from, double to) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < Math.min(time.size(), values.size()); i++) {
            double t = time.get(i);
            if (t >= from && t <= to) {
                out.add(values.get(i));
            }
        }
        return out;
    }

    private static String firstOf(Map<String, List<Double>> parsed, String... keys) {
        for (String key : keys) {
            if (parsed.containsKey(key)) {
                return key;
            }
        }
        return null;
    }

    private static double mean(List<Double> values) {
        return sum(values) / values.size();
    }

    private static double sum(List<Double> values) {
        double total = 0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private static double meanSquares(List<Double> values) {
        return sumProducts(values, values) / values.size();
    }

    private static double sumProducts(List<Double> a, List<Double> b) {
        double total = 0;
        for (int i = 0; i < a.size(); i++) {
            total += a.get(i) * b.get(i);
        }
        return total;
    }

    private static double sqrt(double value) {
        return Math.sqrt(value);
    }

    private static double minOf(List<Double> values) {
        double min = Double.MAX_VALUE;
        for (double value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static double maxOf(List<Double> values) {
        double max = -Double.MAX_VALUE;
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}
