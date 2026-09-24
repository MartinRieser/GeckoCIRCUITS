package gecko.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static gecko.mcp.PyFormat.format;

/**
 * Port of the Python generator for the 2-phase interleaved boost PFC project.
 * The .ipes text comes from the extracted template; hole order follows
 * pfc-*-holes.json. Golden tests compare against the Python original.
 */
final class PfcProject {

    private PfcProject() {
    }

    /** Parameter record mirroring the Python tool arguments. */
    record Params(double targetVoltage, double vRms, double fGrid, double fSw,
                  double inductance, double capacitance, double rLoadBase,
                  double rLoadStep, double tStep, double duration, double dt) {

        static Params defaults() {
            return new Params(50.0, 24.0, 50.0, 20000.0, 0.0008, 0.0068,
                    25.0, 25.0, 0.05, 0.10, 1e-6);
        }
    }

    static String generateIpesText(Params p) throws IOException {
        String scriptSource = TemplateEngine.substitute(
                TemplateEngine.load("pfc-script_source.tpl"),
                format(p.targetVoltage(), ".1f"),
                format(p.tStep(), ".3f"),
                format(p.tStep(), ".4f"),
                format(p.fGrid(), ".1f"),
                format(p.fGrid(), ".1f"),
                format(p.fSw(), ".0f"),
                format(p.fSw(), ".1f"));
        String staticVars = TemplateEngine.load("pfc-static_vars.txt");
        double vPeak = p.vRms() * Math.sqrt(2.0);

        return TemplateEngine.substitute(
                TemplateEngine.load("pfc-ipes_content.tpl"),
                format(p.dt(), "e"),
                format(p.duration(), ".4f"),
                format(p.duration(), ".4f"),
                format(vPeak, ".4f"),
                format(p.fGrid(), ".1f"),
                format(p.inductance(), ".6e"),
                format(p.inductance(), ".6e"),
                format(p.capacitance(), ".6e"),
                format(p.targetVoltage(), ".1f"),
                format(p.rLoadBase(), ".2f"),
                format(p.rLoadStep(), ".2f"),
                PyFormat.strip(scriptSource),
                PyFormat.strip(staticVars));
    }

    static Map<String, Object> setup(String outputPath, Params p) throws IOException {
        Path target = IpesSupport.resolve(outputPath);
        String text = generateIpesText(p);
        IpesSupport.writeIpesText(target, text, true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("file", target.toString());
        result.put("file_size_bytes", Files.size(target));
        result.put("target_voltage_volts", p.targetVoltage());
        result.put("grid_frequency_hz", p.fGrid());
        result.put("switching_frequency_hz", p.fSw());
        result.put("inductance_henries", p.inductance());
        result.put("capacitance_farads", p.capacitance());
        result.put("base_load_ohms", p.rLoadBase());
        result.put("step_load_ohms", p.rLoadStep());
        result.put("step_time_seconds", p.tStep());
        result.put("duration_seconds", p.duration());
        result.put("time_step_seconds", p.dt());
        result.put("topology", "2-Phase Interleaved Boost PFC with Microcontroller Script Block & Dynamic Load Step");
        result.put("power_components_count", 15);
        result.put("control_components_count", 5);
        return result;
    }
}
