package gecko.mcp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PowerMetricsCalculatorTest {

    @Test
    void calculatesAccurateMetricsOnSyntheticWaveforms() {
        // Create synthetic sine wave input: 230V RMS, 50Hz, 10A RMS, PF = 1.0 (in-phase)
        // Output DC: 50V DC with 1V pk-pk ripple (2%), 46A DC
        int points = 1000;
        double dt = 1e-4; // 0.1s total
        List<Double> time = new ArrayList<>(points);
        List<Double> vIn = new ArrayList<>(points);
        List<Double> iIn = new ArrayList<>(points);
        List<Double> vOut = new ArrayList<>(points);
        List<Double> iOut = new ArrayList<>(points);

        double vPk = 230.0 * Math.sqrt(2.0);
        double iPk = 10.0 * Math.sqrt(2.0);
        double omega = 2.0 * Math.PI * 50.0;

        for (int i = 0; i < points; i++) {
            double t = i * dt;
            time.add(t);
            vIn.add(vPk * Math.sin(omega * t));
            iIn.add(iPk * Math.sin(omega * t));
            vOut.add(50.0 + 0.5 * Math.sin(omega * 2.0 * t)); // 50V +/- 0.5V -> 1.0V pk-pk
            iOut.add(44.0); // 44 A
        }

        Map<String, List<Double>> columns = new LinkedHashMap<>();
        columns.put("time", time);
        columns.put("uIN", vIn);
        columns.put("iIN", iIn);
        columns.put("uOUT", vOut);
        columns.put("iOUT", iOut);

        SimulationService.ParsedCsv csv = new SimulationService.ParsedCsv(
                List.of("time", "uIN", "iIN", "uOUT", "iOUT"), columns);

        Map<String, Object> result = PowerMetricsCalculator.calculate(
                csv, 0.05, null, "uIN", "iIN", "uOUT", "iOUT", false);

        assertNotNull(result);
        assertEquals(500, result.get("sample_count"));

        @SuppressWarnings("unchecked")
        Map<String, Object> signals = (Map<String, Object>) result.get("signals");
        @SuppressWarnings("unchecked")
        Map<String, Object> vOutStats = (Map<String, Object>) signals.get("uOUT");
        assertEquals(50.0, (Double) vOutStats.get("mean"), 0.1, "Vout DC average should be ~50V");
        assertEquals(1.0, (Double) vOutStats.get("peak_to_peak_ripple"), 0.1, "Vout ripple should be ~1V");
        assertEquals(2.0, (Double) vOutStats.get("ripple_percent"), 0.2, "Vout ripple percent should be ~2%");

        @SuppressWarnings("unchecked")
        Map<String, Object> sys = (Map<String, Object>) result.get("system_metrics");
        assertEquals(2300.0, (Double) sys.get("p_in_avg_w"), 50.0, "Pin should be ~2300W");
        assertEquals(2200.0, (Double) sys.get("p_out_avg_w"), 50.0, "Pout should be ~2200W (50V * 44A)");
        assertEquals(1.0, (Double) sys.get("power_factor"), 0.02, "Power factor should be ~1.0");
        assertTrue((Double) sys.get("efficiency_percent") > 90.0, "Efficiency should be > 90%");
    }

    @Test
    void threePhaseScalingMultipliesPowerByThree() {
        int points = 100;
        List<Double> time = new ArrayList<>(points);
        List<Double> vIn = new ArrayList<>(points);
        List<Double> iIn = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            time.add(i * 1e-4);
            vIn.add(230.0);
            iIn.add(10.0);
        }

        Map<String, List<Double>> columns = new LinkedHashMap<>();
        columns.put("time", time);
        columns.put("uIN", vIn);
        columns.put("iIN", iIn);

        SimulationService.ParsedCsv csv = new SimulationService.ParsedCsv(
                List.of("time", "uIN", "iIN"), columns);

        Map<String, Object> singlePhase = PowerMetricsCalculator.calculate(
                csv, 0.0, null, "uIN", "iIN", null, null, false);
        @SuppressWarnings("unchecked")
        Map<String, Object> sys1 = (Map<String, Object>) singlePhase.get("system_metrics");
        assertEquals(2300.0, (Double) sys1.get("p_in_avg_w"), 1.0);

        Map<String, Object> threePhase = PowerMetricsCalculator.calculate(
                csv, 0.0, null, "uIN", "iIN", null, null, true);
        @SuppressWarnings("unchecked")
        Map<String, Object> sys3 = (Map<String, Object>) threePhase.get("system_metrics");
        assertEquals(6900.0, (Double) sys3.get("p_in_avg_w"), 1.0, "3-phase should be 3x 2300W = 6900W");
    }

    @Test
    void calculatesThermalMetricsWithSettlingAndTimeConstant() {
        int points = 1000;
        double dt = 1e-3; // 1.0s total
        double tauTarget = 0.10; // 100 ms time constant
        double ambient = 25.0;
        double steadyRise = 75.0; // Steady state = 100 degC

        List<Double> time = new ArrayList<>(points);
        List<Double> tj = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i * dt;
            time.add(t);
            // Exponential rise with 2 degC pk-pk (amplitude 1.0) ripple
            double val = ambient + steadyRise * (1.0 - Math.exp(-t / tauTarget)) + 1.0 * Math.sin(2.0 * Math.PI * 100.0 * t);
            tj.add(val);
        }

        Map<String, List<Double>> columns = new LinkedHashMap<>();
        columns.put("time", time);
        columns.put("tj_mosfet", tj);

        SimulationService.ParsedCsv csv = new SimulationService.ParsedCsv(List.of("time", "tj_mosfet"), columns);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("start_time", 0.6); // Steady-state evaluation window
        args.put("ambient_temperature", ambient);
        args.put("thermal_signals", List.of("tj_mosfet"));

        Map<String, Object> result = PowerMetricsCalculator.calculate(csv, args);
        assertNotNull(result);
        assertTrue(result.containsKey("thermal_metrics"));

        @SuppressWarnings("unchecked")
        Map<String, Object> thermal = (Map<String, Object>) result.get("thermal_metrics");
        assertEquals(25.0, (Double) thermal.get("ambient_temperature_degC"), 0.01);

        @SuppressWarnings("unchecked")
        Map<String, Object> devices = (Map<String, Object>) thermal.get("devices");
        @SuppressWarnings("unchecked")
        Map<String, Object> mosfet = (Map<String, Object>) devices.get("tj_mosfet");
        assertNotNull(mosfet);

        assertEquals(100.0, (Double) mosfet.get("steady_state_temp_degC"), 0.5, "Steady state should be ~100 degC");
        assertEquals(75.0, (Double) mosfet.get("temp_rise_degC"), 0.5, "Delta T should be ~75 degC");
        assertEquals(2.0, (Double) mosfet.get("peak_ripple_degC"), 0.1, "Pk-pk ripple should be ~2 degC");
        assertTrue((Boolean) mosfet.get("is_settled"), "Device temperature should be certified settled");

        Double settlingTime = (Double) mosfet.get("settling_time_s");
        assertNotNull(settlingTime);
        assertTrue(settlingTime > 0.35 && settlingTime < 0.45, "Settling time to 2% band should be ~3.9 tau = ~0.39s");

        Double tau = (Double) mosfet.get("time_constant_s");
        assertNotNull(tau);
        assertEquals(tauTarget, tau, 0.02, "Thermal time constant should be ~0.10s");
    }

    @Test
    void calculatesMagneticMetricsWithSteinmetzAndSaturation() {
        int points = 2000;
        double dt = 1e-6; // 2 ms total
        double freq = 10000.0; // 10 kHz
        double omega = 2.0 * Math.PI * freq;

        List<Double> time = new ArrayList<>(points);
        List<Double> flux = new ArrayList<>(points);
        List<Double> iInd = new ArrayList<>(points);

        for (int i = 0; i < points; i++) {
            double t = i * dt;
            time.add(t);
            // Flux peak = 150 uWb (0.00015 Wb)
            flux.add(0.00015 * Math.sin(omega * t));
            // Current: 10A DC + 2A peak ripple (12A peak, 8A min, 4A pk-pk)
            iInd.add(10.0 + 2.0 * Math.sin(omega * t));
        }

        Map<String, List<Double>> columns = new LinkedHashMap<>();
        columns.put("time", time);
        columns.put("Phi_core", flux);
        columns.put("i_L1", iInd);

        SimulationService.ParsedCsv csv = new SimulationService.ParsedCsv(
                List.of("time", "Phi_core", "i_L1"), columns);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("start_time", 0.001);
        args.put("flux_signals", List.of("Phi_core"));
        args.put("i_inductor_signals", List.of("i_L1"));
        args.put("core_area_m2", 0.0005); // 500 mm^2 -> Bpk = 0.00015 / 0.0005 = 0.3 T
        args.put("core_volume_m3", 2.5e-5); // 25 cm^3
        args.put("frequency", freq);
        args.put("steinmetz_k", 1.5);
        args.put("steinmetz_alpha", 1.3);
        args.put("steinmetz_beta", 2.5);
        args.put("i_sat", 15.0); // 15A sat current -> margin = 1 - 12/15 = 20%
        args.put("inductance", 1e-3); // 1 mH -> E = 0.5 * 1e-3 * 12^2 = 0.072 J

        Map<String, Object> result = PowerMetricsCalculator.calculate(csv, args);
        assertNotNull(result);
        assertTrue(result.containsKey("magnetic_metrics"));

        @SuppressWarnings("unchecked")
        Map<String, Object> mag = (Map<String, Object>) result.get("magnetic_metrics");
        @SuppressWarnings("unchecked")
        Map<String, Object> fluxAnalysis = (Map<String, Object>) mag.get("flux_analysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> phiMetrics = (Map<String, Object>) fluxAnalysis.get("Phi_core");
        assertNotNull(phiMetrics);

        assertEquals(0.00015, (Double) phiMetrics.get("peak_flux_wb"), 1e-6);
        assertEquals(0.00030, (Double) phiMetrics.get("flux_excursion_wb"), 1e-6);
        assertEquals(0.30, (Double) phiMetrics.get("peak_flux_density_t"), 0.01);
        assertEquals(0.60, (Double) phiMetrics.get("flux_density_excursion_t"), 0.01);
        assertTrue((Double) phiMetrics.get("steinmetz_core_loss_density_w_per_m3") > 10000.0);
        assertTrue((Double) phiMetrics.get("steinmetz_core_loss_total_w") > 0.2);

        @SuppressWarnings("unchecked")
        Map<String, Object> curAnalysis = (Map<String, Object>) mag.get("inductor_current_analysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> iMetrics = (Map<String, Object>) curAnalysis.get("i_L1");
        assertNotNull(iMetrics);

        assertEquals(12.0, (Double) iMetrics.get("i_peak_a"), 0.05);
        assertEquals(10.0, (Double) iMetrics.get("i_mean_a"), 0.05);
        assertEquals(4.0, (Double) iMetrics.get("i_ripple_pkpk_a"), 0.05);
        assertEquals(40.0, (Double) iMetrics.get("i_ripple_percent"), 0.5);
        assertEquals(0.20, (Double) iMetrics.get("saturation_margin"), 0.01);
        assertEquals(20.0, (Double) iMetrics.get("saturation_margin_percent"), 1.0);
        assertFalse((Boolean) iMetrics.get("is_saturated"));
        assertEquals(0.072, (Double) iMetrics.get("peak_stored_energy_j"), 0.001);
    }

    @Test
    void calculatesMechanicalMetricsWithSpeedSettlingAndTorque() {
        int points = 1000;
        double dt = 1e-3; // 1.0s
        double targetSpeed = 157.08; // ~1500 RPM
        double tau = 0.04;

        List<Double> time = new ArrayList<>(points);
        List<Double> speed = new ArrayList<>(points);
        List<Double> torque = new ArrayList<>(points);

        for (int i = 0; i < points; i++) {
            double t = i * dt;
            time.add(t);
            double w = targetSpeed * (1.0 - Math.exp(-t / tau)) + 0.5 * Math.sin(2.0 * Math.PI * 50.0 * t);
            double trq = 10.0 + 1.0 * Math.sin(2.0 * Math.PI * 50.0 * t);
            speed.add(w);
            torque.add(trq);
        }

        Map<String, List<Double>> columns = new LinkedHashMap<>();
        columns.put("time", time);
        columns.put("omega_m", speed);
        columns.put("T_mech", torque);

        SimulationService.ParsedCsv csv = new SimulationService.ParsedCsv(
                List.of("time", "omega_m", "T_mech"), columns);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("start_time", 0.5);
        args.put("target_speed", targetSpeed);
        args.put("speed_signals", List.of("omega_m"));
        args.put("torque_signals", List.of("T_mech"));

        Map<String, Object> result = PowerMetricsCalculator.calculate(csv, args);
        assertNotNull(result);
        assertTrue(result.containsKey("mechanical_metrics"));

        @SuppressWarnings("unchecked")
        Map<String, Object> mech = (Map<String, Object>) result.get("mechanical_metrics");
        @SuppressWarnings("unchecked")
        Map<String, Object> speedMap = (Map<String, Object>) mech.get("speed_analysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> spdMetrics = (Map<String, Object>) speedMap.get("omega_m");
        assertNotNull(spdMetrics);

        assertEquals(157.08, (Double) spdMetrics.get("mean_speed_rad_per_s"), 0.1);
        assertEquals(1500.0, (Double) spdMetrics.get("mean_speed_rpm"), 1.0);
        assertEquals(1.0, (Double) spdMetrics.get("speed_ripple_pkpk_rad_per_s"), 0.1);
        assertTrue((Boolean) spdMetrics.get("is_settled"));
        Double settling = (Double) spdMetrics.get("settling_time_s");
        assertNotNull(settling);
        assertTrue(settling > 0.12 && settling < 0.22, "Speed should settle within ~0.16s");
        assertEquals(0.0, (Double) spdMetrics.get("tracking_error_percent"), 0.2);

        @SuppressWarnings("unchecked")
        Map<String, Object> trqMap = (Map<String, Object>) mech.get("torque_analysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> trqMetrics = (Map<String, Object>) trqMap.get("T_mech");
        assertNotNull(trqMetrics);

        assertEquals(10.0, (Double) trqMetrics.get("mean_torque_nm"), 0.1);
        assertEquals(2.0, (Double) trqMetrics.get("torque_ripple_pkpk_nm"), 0.1);
        assertEquals(20.0, (Double) trqMetrics.get("torque_ripple_percent"), 1.0);

        Double pMech = (Double) mech.get("shaft_power_avg_w");
        assertNotNull(pMech);
        assertEquals(1570.8, pMech, 20.0, "Shaft mechanical power should be ~1570W (10Nm * 157.08 rad/s)");
    }

    @Test
    void autoDetectsDomainsWhenSignalsNotExplicitlyProvided() {
        int points = 100;
        List<Double> time = new ArrayList<>(points);
        List<Double> tj = new ArrayList<>(points);
        List<Double> phi = new ArrayList<>(points);
        List<Double> iL = new ArrayList<>(points);
        List<Double> w = new ArrayList<>(points);
        List<Double> torque = new ArrayList<>(points);

        for (int i = 0; i < points; i++) {
            time.add(i * 1e-4);
            tj.add(85.0);
            phi.add(0.001);
            iL.add(5.0);
            w.add(100.0);
            torque.add(2.0);
        }

        Map<String, List<Double>> columns = new LinkedHashMap<>();
        columns.put("time", time);
        columns.put("Tj_diode", tj);
        columns.put("phi_mag", phi);
        columns.put("i_L_boost", iL);
        columns.put("omega_rotor", w);
        columns.put("t_load", torque);

        SimulationService.ParsedCsv csv = new SimulationService.ParsedCsv(
                List.of("time", "Tj_diode", "phi_mag", "i_L_boost", "omega_rotor", "t_load"), columns);

        Map<String, Object> result = PowerMetricsCalculator.calculate(csv, Map.of());
        assertNotNull(result);
        assertTrue(result.containsKey("thermal_metrics"), "Should auto-detect Tj_diode");
        assertTrue(result.containsKey("magnetic_metrics"), "Should auto-detect phi_mag and i_L_boost");
        assertTrue(result.containsKey("mechanical_metrics"), "Should auto-detect omega_rotor and t_load");
    }
}
