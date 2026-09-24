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
}
