package gecko.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static gecko.mcp.PyFormat.format;

/**
 * Port of the Python generator for the half-bridge LLC resonant converter
 * project, including the resonant-tank analytics (f0, Z0, Q, k).
 */
final class LlcProject {

    private LlcProject() {
    }

    record Params(double vIn, double vOut, double pOut, double fSw, double lR, double cR,
                  double lM, double tDead, Double rLoad, Double cOut, double duration, double dt,
                  double rOn, double cOss) {

        static Params defaults() {
            return new Params(400.0, 200.0, 10000.0, 100000.0, 2.2e-6, 1.15e-6,
                    1.1e-5, 1.8e-7, null, null, 0.0003, 2e-8, 0.002, 1.5e-9);
        }
    }

    static String generateIpesText(Params p) throws IOException {
        double rLoad = p.rLoad() != null ? p.rLoad() : Math.pow(p.vOut(), 2) / Math.max(50.0, p.pOut());
        double cOut = p.cOut() != null ? p.cOut() : (p.pOut() >= 2000.0 ? 2.0e-4 : 2.0e-5);
        double vCrInit = p.vIn() / 2.0;
        double vOutInit = Math.min(p.vOut(), p.vIn() * 0.5);

        String scriptSource = TemplateEngine.substitute(
                TemplateEngine.load("llc-script_source.tpl"),
                format(p.fSw(), ".1f"),
                format(p.tDead(), ".3e"));

        return TemplateEngine.substitute(
                TemplateEngine.load("llc-ipes_content.tpl"),
                format(p.dt(), "e"),
                format(p.duration(), ".6f"),
                format(p.duration(), ".6f"),
                format(p.vIn(), ".2f"),
                PyFormat.pyStr(p.rOn()),
                PyFormat.pyStr(p.rOn()),
                PyFormat.pyStr(p.rOn()),
                PyFormat.pyStr(p.rOn()),
                format(p.cOss(), ".3e"),
                format(p.cR(), ".6e"),
                format(vCrInit, ".2f"),
                format(p.lR(), ".6e"),
                format(p.lM(), ".6e"),
                PyFormat.pyStr(p.rOn()),
                format(cOut, ".6e"),
                format(vOutInit, ".2f"),
                format(rLoad, ".2f"),
                scriptSource);
    }

    static Map<String, Object> setup(String outputPath, Params p) throws IOException {
        Path target = IpesSupport.resolve(outputPath);
        String text = generateIpesText(p);
        IpesSupport.writeIpesText(target, text, true);

        double rLoad = p.rLoad() != null ? p.rLoad() : Math.pow(p.vOut(), 2) / Math.max(50.0, p.pOut());
        double fRes = 1.0 / (2.0 * Math.PI * Math.sqrt(p.lR() * p.cR()));
        double kFactor = p.lM() / p.lR();
        double z0 = Math.sqrt(p.lR() / p.cR());
        double rAc = (8.0 / (Math.PI * Math.PI)) * rLoad;
        double qFactor = rAc > 1e-4 ? z0 / rAc : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("file", target.toString());
        result.put("file_size_bytes", Files.size(target));
        result.put("input_voltage_volts", p.vIn());
        result.put("output_voltage_volts", p.vOut());
        result.put("output_power_watts", p.pOut());
        result.put("switching_frequency_hz", p.fSw());
        result.put("resonant_frequency_hz", round(fRes, 1));
        result.put("inductance_ratio_k", round(kFactor, 2));
        result.put("characteristic_impedance_ohms", round(z0, 3));
        result.put("quality_factor_q", round(qFactor, 3));
        result.put("load_resistance_ohms", round(rLoad, 3));
        result.put("duration_seconds", p.duration());
        result.put("time_step_seconds", p.dt());
        return result;
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}
