package gecko.mcp;

import java.util.*;

/**
 * Multi-domain analytics engine for GeckoCIRCUITS simulations.
 *
 * <p>Computes key figures of merit across electrical, thermal, magnetic, and
 * mechanical domains server-side without streaming massive raw time-series over stdio:
 * <ul>
 *   <li><b>Electrical Converter Metrics:</b> RMS, ripple %, active power, apparent power,
 *       power factor, and conversion efficiency.</li>
 *   <li><b>Thermal Metrics:</b> Steady-state junction temperature, peak-to-peak thermal ripple,
 *       temperature rise above ambient, 2% settling time, and equivalent thermal time constant.</li>
 *   <li><b>Magnetic Metrics:</b> Peak flux, flux excursion, core flux density, Steinmetz core
 *       loss density and total loss, peak winding current, saturation safety margin, and stored energy.</li>
 *   <li><b>Mechanical Metrics:</b> Rotor angular velocity, RPM conversion, speed ripple %,
 *       speed tracking error and settling time, mechanical torque, torque ripple %, and shaft power.</li>
 * </ul>
 * </p>
 */
public final class PowerMetricsCalculator {

    private PowerMetricsCalculator() {
    }

    /**
     * Legacy 8-parameter overload maintaining full backward compatibility.
     */
    public static Map<String, Object> calculate(
            SimulationService.ParsedCsv csv,
            double startTime,
            List<String> requestedSignals,
            String vInSignal, String iInSignal,
            String vOutSignal, String iOutSignal,
            boolean isThreePhase
    ) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (startTime >= 0) args.put("start_time", startTime);
        if (requestedSignals != null) args.put("signals", requestedSignals);
        if (vInSignal != null) args.put("v_in_signal", vInSignal);
        if (iInSignal != null) args.put("i_in_signal", iInSignal);
        if (vOutSignal != null) args.put("v_out_signal", vOutSignal);
        if (iOutSignal != null) args.put("i_out_signal", iOutSignal);
        args.put("is_three_phase", isThreePhase);
        return calculate(csv, args);
    }

    /**
     * Primary multi-domain calculation entry point accepting the full MCP argument map.
     */
    public static Map<String, Object> calculate(
            SimulationService.ParsedCsv csv,
            Map<String, Object> args
    ) {
        Map<String, List<Double>> columns = csv.columns();
        List<Double> time = columns.get("time");
        if (time == null || time.isEmpty()) {
            throw new IllegalArgumentException("Simulation result contains no time data");
        }

        double startTime = getDouble(args, "start_time", -1.0);
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

        // 1. General Signal Statistics
        List<String> requestedSignals = getList(args, "signals");
        Set<String> targetSignals = (requestedSignals != null && !requestedSignals.isEmpty())
                ? new LinkedHashSet<>(requestedSignals) : new LinkedHashSet<>(columns.keySet());
        targetSignals.remove("time");

        Map<String, Object> signalStats = new LinkedHashMap<>();
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

        // 2. Electrical Converter System Metrics
        Map<String, Object> systemMetrics = calculateElectricalMetrics(columns, time, startIndex, count, args);

        // 3. Thermal Domain Metrics
        Map<String, Object> thermalMetrics = calculateThermalMetrics(columns, time, startIndex, count, args);

        // 4. Magnetic Domain Metrics
        Map<String, Object> magneticMetrics = calculateMagneticMetrics(columns, time, startIndex, count, args);

        // 5. Mechanical Domain Metrics
        Map<String, Object> mechanicalMetrics = calculateMechanicalMetrics(columns, time, startIndex, count, args);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis_window_start_s", round(time.get(startIndex), 6));
        result.put("analysis_window_end_s", round(time.get(time.size() - 1), 6));
        result.put("sample_count", count);
        result.put("system_metrics", systemMetrics);
        if (!thermalMetrics.isEmpty()) {
            result.put("thermal_metrics", thermalMetrics);
        }
        if (!magneticMetrics.isEmpty()) {
            result.put("magnetic_metrics", magneticMetrics);
        }
        if (!mechanicalMetrics.isEmpty()) {
            result.put("mechanical_metrics", mechanicalMetrics);
        }
        result.put("signals", signalStats);
        return result;
    }

    // ========================================================================
    // Electrical Power & Efficiency Domain
    // ========================================================================

    private static Map<String, Object> calculateElectricalMetrics(
            Map<String, List<Double>> columns,
            List<Double> time,
            int startIndex,
            int count,
            Map<String, Object> args
    ) {
        String vOutSignal = getString(args, "v_out_signal", null);
        String iOutSignal = getString(args, "i_out_signal", null);
        String vInSignal = getString(args, "v_in_signal", null);
        String iInSignal = getString(args, "i_in_signal", null);
        boolean isThreePhase = Boolean.TRUE.equals(args.get("is_three_phase"))
                || "true".equalsIgnoreCase(String.valueOf(args.get("is_three_phase")));

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
        return systemMetrics;
    }

    // ========================================================================
    // Thermal Domain
    // ========================================================================

    private static Map<String, Object> calculateThermalMetrics(
            Map<String, List<Double>> columns,
            List<Double> time,
            int startIndex,
            int count,
            Map<String, Object> args
    ) {
        List<String> thermalSigs = getList(args, "thermal_signals");
        if (thermalSigs == null || thermalSigs.isEmpty()) {
            thermalSigs = new ArrayList<>();
            for (String sig : columns.keySet()) {
                if ("time".equalsIgnoreCase(sig)) continue;
                String lower = sig.toLowerCase(Locale.ROOT);
                if (lower.contains("torque") || lower.startsWith("time")) continue;
                if (lower.contains("tj") || lower.contains("temp") || lower.contains("therm")
                        || lower.contains("junction") || lower.startsWith("t_") || lower.startsWith("th_")) {
                    thermalSigs.add(sig);
                }
            }
        }

        if (thermalSigs.isEmpty()) {
            return Map.of();
        }

        double ambientTemp = getDouble(args, "ambient_temperature", 25.0);
        Map<String, Object> thermalResults = new LinkedHashMap<>();
        thermalResults.put("ambient_temperature_degC", ambientTemp);

        Map<String, Object> deviceMetrics = new LinkedHashMap<>();
        for (String sig : thermalSigs) {
            List<Double> data = columns.get(sig);
            if (data == null || data.size() < time.size()) continue;

            double sum = 0.0;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = startIndex; i < time.size(); i++) {
                double val = data.get(i);
                sum += val;
                if (val < min) min = val;
                if (val > max) max = val;
            }
            double steadyStateTemp = sum / count;
            double ripplePkPk = max - min;
            double tempRise = steadyStateTemp - ambientTemp;

            // 2% band settling time calculation across the full simulation run
            SettlingResult settling = computeSettling(time, data, steadyStateTemp, 0.02);

            // Equivalent thermal time constant (time from t=0 to 63.2% of rise)
            Double tau = computeTimeConstant(time, data, steadyStateTemp);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("steady_state_temp_degC", round(steadyStateTemp, 2));
            m.put("max_temp_degC", round(max, 2));
            m.put("min_temp_degC", round(min, 2));
            m.put("peak_ripple_degC", round(ripplePkPk, 3));
            m.put("temp_rise_degC", round(tempRise, 2));
            m.put("settling_time_s", settling.time != null ? round(settling.time, 6) : null);
            m.put("is_settled", settling.isSettled);
            m.put("time_constant_s", tau != null ? round(tau, 6) : null);

            deviceMetrics.put(sig, m);
        }

        thermalResults.put("devices", deviceMetrics);
        return thermalResults;
    }

    // ========================================================================
    // Magnetic Domain
    // ========================================================================

    private static Map<String, Object> calculateMagneticMetrics(
            Map<String, List<Double>> columns,
            List<Double> time,
            int startIndex,
            int count,
            Map<String, Object> args
    ) {
        // 1. Flux signals
        List<String> fluxSigs = getList(args, "flux_signals");
        if (fluxSigs == null || fluxSigs.isEmpty()) {
            String singleFlux = getString(args, "flux_signal", null);
            if (singleFlux != null) {
                fluxSigs = List.of(singleFlux);
            } else {
                fluxSigs = new ArrayList<>();
                for (String sig : columns.keySet()) {
                    if ("time".equalsIgnoreCase(sig)) continue;
                    String lower = sig.toLowerCase(Locale.ROOT);
                    if (lower.contains("phi") || lower.contains("flux") || lower.contains("psi")
                            || lower.startsWith("b_") || lower.startsWith("bmag")) {
                        fluxSigs.add(sig);
                    }
                }
            }
        }

        // 2. Inductor current signals
        List<String> iIndSigs = getList(args, "i_inductor_signals");
        if (iIndSigs == null || iIndSigs.isEmpty()) {
            String singleI = getString(args, "i_inductor_signal", null);
            if (singleI != null) {
                iIndSigs = List.of(singleI);
            } else {
                iIndSigs = new ArrayList<>();
                for (String sig : columns.keySet()) {
                    if ("time".equalsIgnoreCase(sig)) continue;
                    String lower = sig.toLowerCase(Locale.ROOT);
                    if (lower.contains("load") || lower.contains("grid") || lower.contains("out") || lower.contains("in")) {
                        continue;
                    }
                    if (lower.contains("i_l") || lower.contains("il") || lower.contains("ichoke")
                            || lower.contains("i_ind") || lower.contains("i_pri") || lower.contains("i_sec")
                            || lower.matches("^i_?l[0-9_]*$")) {
                        iIndSigs.add(sig);
                    }
                }
            }
        }

        if (fluxSigs.isEmpty() && iIndSigs.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> magResult = new LinkedHashMap<>();

        // Core parameters
        Double coreArea = getDouble(args, "core_area_m2", null);
        if (coreArea == null && args.containsKey("core_area_mm2")) {
            coreArea = getDouble(args, "core_area_mm2", 0.0) * 1e-6;
        }
        Double coreVolume = getDouble(args, "core_volume_m3", null);
        if (coreVolume == null && args.containsKey("core_volume_cm3")) {
            coreVolume = getDouble(args, "core_volume_cm3", 0.0) * 1e-6;
        }
        Double steinmetzK = getDouble(args, "steinmetz_k", null);
        Double steinmetzAlpha = getDouble(args, "steinmetz_alpha", null);
        Double steinmetzBeta = getDouble(args, "steinmetz_beta", null);
        Double frequency = getDouble(args, "frequency", null);

        if (!fluxSigs.isEmpty()) {
            Map<String, Object> fluxMetrics = new LinkedHashMap<>();
            for (String sig : fluxSigs) {
                List<Double> data = columns.get(sig);
                if (data == null || data.size() < time.size()) continue;

                double sum = 0.0;
                double maxAbs = 0.0;
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int i = startIndex; i < time.size(); i++) {
                    double val = data.get(i);
                    sum += val;
                    if (Math.abs(val) > maxAbs) maxAbs = Math.abs(val);
                    if (val < min) min = val;
                    if (val > max) max = val;
                }
                double excursion = max - min;
                double mean = sum / count;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("peak_flux_wb", round(maxAbs, 6));
                m.put("flux_excursion_wb", round(excursion, 6));
                m.put("mean_flux_wb", round(mean, 6));

                if (coreArea != null && coreArea > 0) {
                    double peakB = maxAbs / coreArea;
                    double deltaB = excursion / coreArea;
                    m.put("core_area_m2", round(coreArea, 8));
                    m.put("peak_flux_density_t", round(peakB, 4));
                    m.put("flux_density_excursion_t", round(deltaB, 4));

                    if (steinmetzK != null && steinmetzAlpha != null && steinmetzBeta != null && frequency != null && frequency > 0) {
                        double pVol = steinmetzK * Math.pow(frequency, steinmetzAlpha) * Math.pow(peakB, steinmetzBeta);
                        m.put("steinmetz_core_loss_density_w_per_m3", round(pVol, 2));
                        if (coreVolume != null && coreVolume > 0) {
                            m.put("steinmetz_core_loss_total_w", round(pVol * coreVolume, 3));
                        }
                    }
                }
                fluxMetrics.put(sig, m);
            }
            magResult.put("flux_analysis", fluxMetrics);
        }

        if (!iIndSigs.isEmpty()) {
            Double iSat = getDouble(args, "i_sat", getDouble(args, "saturation_current", null));
            Double inductance = getDouble(args, "inductance", null);

            Map<String, Object> currentMetrics = new LinkedHashMap<>();
            for (String sig : iIndSigs) {
                List<Double> data = columns.get(sig);
                if (data == null || data.size() < time.size()) continue;

                double sum = 0.0;
                double sumSq = 0.0;
                double maxAbs = 0.0;
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int i = startIndex; i < time.size(); i++) {
                    double val = data.get(i);
                    sum += val;
                    sumSq += val * val;
                    if (Math.abs(val) > maxAbs) maxAbs = Math.abs(val);
                    if (val < min) min = val;
                    if (val > max) max = val;
                }
                double mean = sum / count;
                double rms = Math.sqrt(sumSq / count);
                double ripple = max - min;
                double ripplePct = Math.abs(mean) > 1e-6 ? (ripple / Math.abs(mean)) * 100.0 : 0.0;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("i_peak_a", round(maxAbs, 3));
                m.put("i_rms_a", round(rms, 3));
                m.put("i_mean_a", round(mean, 3));
                m.put("i_ripple_pkpk_a", round(ripple, 3));
                m.put("i_ripple_percent", round(ripplePct, 2));

                if (iSat != null && iSat > 0) {
                    double margin = 1.0 - (maxAbs / iSat);
                    m.put("i_sat_a", round(iSat, 3));
                    m.put("saturation_margin", round(margin, 4));
                    m.put("saturation_margin_percent", round(margin * 100.0, 2));
                    m.put("is_saturated", maxAbs >= iSat);
                }

                if (inductance != null && inductance > 0) {
                    double energy = 0.5 * inductance * maxAbs * maxAbs;
                    m.put("inductance_h", round(inductance, 6));
                    m.put("peak_stored_energy_j", round(energy, 6));
                }
                currentMetrics.put(sig, m);
            }
            magResult.put("inductor_current_analysis", currentMetrics);
        }

        return magResult;
    }

    // ========================================================================
    // Mechanical Domain
    // ========================================================================

    private static Map<String, Object> calculateMechanicalMetrics(
            Map<String, List<Double>> columns,
            List<Double> time,
            int startIndex,
            int count,
            Map<String, Object> args
    ) {
        List<String> speedSigs = getList(args, "speed_signals");
        if (speedSigs == null || speedSigs.isEmpty()) {
            String singleSpeed = getString(args, "speed_signal", getString(args, "omega_signal", null));
            if (singleSpeed != null) {
                speedSigs = List.of(singleSpeed);
            } else {
                speedSigs = new ArrayList<>();
                for (String sig : columns.keySet()) {
                    if ("time".equalsIgnoreCase(sig)) continue;
                    String lower = sig.toLowerCase(Locale.ROOT);
                    if (lower.contains("omega") || lower.contains("speed") || lower.contains("rpm")
                            || lower.startsWith("w1") || lower.startsWith("w2") || lower.startsWith("w_")) {
                        speedSigs.add(sig);
                    }
                }
            }
        }

        List<String> torqueSigs = getList(args, "torque_signals");
        if (torqueSigs == null || torqueSigs.isEmpty()) {
            String singleTorque = getString(args, "torque_signal", null);
            if (singleTorque != null) {
                torqueSigs = List.of(singleTorque);
            } else {
                torqueSigs = new ArrayList<>();
                for (String sig : columns.keySet()) {
                    if ("time".equalsIgnoreCase(sig)) continue;
                    String lower = sig.toLowerCase(Locale.ROOT);
                    if (lower.contains("torque") || lower.contains("t_e") || lower.contains("t_load")
                            || lower.contains("t_mech") || lower.startsWith("tau")) {
                        torqueSigs.add(sig);
                    }
                }
            }
        }

        if (speedSigs.isEmpty() && torqueSigs.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> mechResult = new LinkedHashMap<>();

        if (!speedSigs.isEmpty()) {
            Double targetSpeed = getDouble(args, "target_speed", getDouble(args, "omega_ref", null));
            Map<String, Object> speedMap = new LinkedHashMap<>();
            for (String sig : speedSigs) {
                List<Double> data = columns.get(sig);
                if (data == null || data.size() < time.size()) continue;

                double sum = 0.0;
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int i = startIndex; i < time.size(); i++) {
                    double val = data.get(i);
                    sum += val;
                    if (val < min) min = val;
                    if (val > max) max = val;
                }
                double meanSpeed = sum / count;
                double ripple = max - min;
                double ripplePct = Math.abs(meanSpeed) > 1e-6 ? (ripple / Math.abs(meanSpeed)) * 100.0 : 0.0;
                double rpm = meanSpeed * 60.0 / (2.0 * Math.PI);

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("mean_rad_s", round(meanSpeed, 2));
                m.put("mean_speed_rad_per_s", round(meanSpeed, 2));
                m.put("mean_rpm", round(rpm, 1));
                m.put("mean_speed_rpm", round(rpm, 1));
                m.put("min_rad_s", round(min, 2));
                m.put("max_rad_s", round(max, 2));
                m.put("ripple_rad_s", round(ripple, 3));
                m.put("speed_ripple_pkpk_rad_per_s", round(ripple, 3));
                m.put("ripple_percent", round(ripplePct, 2));

                if (targetSpeed != null && targetSpeed > 0) {
                    double err = Math.abs(meanSpeed - targetSpeed) / targetSpeed * 100.0;
                    SettlingResult settling = computeSettling(time, data, targetSpeed, 0.02);
                    m.put("target_speed_rad_s", round(targetSpeed, 2));
                    m.put("speed_error_percent", round(err, 2));
                    m.put("tracking_error_percent", round(err, 2));
                    m.put("settling_time_s", settling.time != null ? round(settling.time, 6) : null);
                    m.put("is_settled", settling.isSettled);
                }
                speedMap.put(sig, m);
            }
            mechResult.put("speed_analysis", speedMap);
        }

        if (!torqueSigs.isEmpty()) {
            Map<String, Object> torqueMap = new LinkedHashMap<>();
            for (String sig : torqueSigs) {
                List<Double> data = columns.get(sig);
                if (data == null || data.size() < time.size()) continue;

                double sum = 0.0;
                double maxAbs = 0.0;
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int i = startIndex; i < time.size(); i++) {
                    double val = data.get(i);
                    sum += val;
                    if (Math.abs(val) > maxAbs) maxAbs = Math.abs(val);
                    if (val < min) min = val;
                    if (val > max) max = val;
                }
                double meanTorque = sum / count;
                double ripple = max - min;
                double ripplePct = Math.abs(meanTorque) > 1e-6 ? (ripple / Math.abs(meanTorque)) * 100.0 : 0.0;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("mean_nm", round(meanTorque, 2));
                m.put("mean_torque_nm", round(meanTorque, 2));
                m.put("peak_nm", round(maxAbs, 2));
                m.put("min_nm", round(min, 2));
                m.put("max_nm", round(max, 2));
                m.put("ripple_pkpk_nm", round(ripple, 3));
                m.put("torque_ripple_pkpk_nm", round(ripple, 3));
                m.put("ripple_percent", round(ripplePct, 2));
                m.put("torque_ripple_percent", round(ripplePct, 2));
                torqueMap.put(sig, m);
            }
            mechResult.put("torque_analysis", torqueMap);
        }

        // Shaft power calculation if both speed and torque are available
        if (!speedSigs.isEmpty() && !torqueSigs.isEmpty()) {
            String primarySpeed = speedSigs.get(0);
            String primaryTorque = torqueSigs.get(0);
            List<Double> speedData = columns.get(primarySpeed);
            List<Double> torqueData = columns.get(primaryTorque);
            if (speedData != null && torqueData != null) {
                double sumP = 0.0;
                for (int i = startIndex; i < time.size(); i++) {
                    sumP += speedData.get(i) * torqueData.get(i);
                }
                double avgMechPower = sumP / count;
                Map<String, Object> powerMap = new LinkedHashMap<>();
                powerMap.put("speed_signal", primarySpeed);
                powerMap.put("torque_signal", primaryTorque);
                powerMap.put("p_mech_avg_w", round(avgMechPower, 2));
                powerMap.put("shaft_power_avg_w", round(avgMechPower, 2));
                powerMap.put("p_mech_kw", round(avgMechPower / 1000.0, 3));
                mechResult.put("power_analysis", powerMap);
                mechResult.put("shaft_power_avg_w", round(avgMechPower, 2));
            }
        }

        return mechResult;
    }

    // ========================================================================
    // Transient Dynamics Helpers
    // ========================================================================

    private record SettlingResult(Double time, boolean isSettled) {
    }

    /**
     * Finds the earliest time point after which the signal stays permanently
     * inside the tolerance band [target - band, target + band].
     */
    private static SettlingResult computeSettling(
            List<Double> time,
            List<Double> data,
            double target,
            double toleranceFraction
    ) {
        if (time.isEmpty() || data.isEmpty()) {
            return new SettlingResult(null, false);
        }
        double band = Math.abs(target) * toleranceFraction;
        if (band < 1e-4) {
            band = 1e-4;
        }
        double lower = target - band;
        double upper = target + band;

        int lastOutOfBand = -1;
        for (int i = 0; i < data.size(); i++) {
            double val = data.get(i);
            if (val < lower || val > upper) {
                lastOutOfBand = i;
            }
        }

        if (lastOutOfBand == -1) {
            return new SettlingResult(round(time.get(0), 6), true);
        } else if (lastOutOfBand < data.size() - 1) {
            return new SettlingResult(round(time.get(lastOutOfBand + 1), 6), true);
        } else {
            return new SettlingResult(null, false);
        }
    }

    /**
     * Estimates equivalent 1st-order time constant (time to reach 63.2% of total transient rise).
     */
    private static Double computeTimeConstant(
            List<Double> time,
            List<Double> data,
            double steadyState
    ) {
        if (data.isEmpty() || time.isEmpty()) return null;
        double y0 = data.get(0);
        double deltaY = steadyState - y0;
        if (Math.abs(deltaY) < 1e-4) return null;

        double targetThreshold = y0 + 0.63212 * deltaY;
        for (int i = 0; i < data.size(); i++) {
            double val = data.get(i);
            if (deltaY > 0 && val >= targetThreshold) {
                return time.get(i);
            } else if (deltaY < 0 && val <= targetThreshold) {
                return time.get(i);
            }
        }
        return null;
    }

    // ========================================================================
    // Utility Extraction Helpers
    // ========================================================================

    private static String findSignal(Map<String, List<Double>> columns, String... candidates) {
        for (String c : candidates) {
            if (columns.containsKey(c)) return c;
            for (String key : columns.keySet()) {
                if (key.equalsIgnoreCase(c)) return key;
            }
        }
        return null;
    }

    private static String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : fallback;
    }

    private static Double getDouble(Map<String, Object> map, String key, Double fallback) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return null;
    }

    private static double round(double val, int decimals) {
        if (!Double.isFinite(val)) return 0.0;
        double scale = Math.pow(10, decimals);
        return Math.round(val * scale) / scale;
    }
}
