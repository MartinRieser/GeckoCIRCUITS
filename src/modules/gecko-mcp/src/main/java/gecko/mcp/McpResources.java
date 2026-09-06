package gecko.mcp;

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Self-documenting MCP Resources exposed by GeckoCIRCUITS.
 */
public final class McpResources {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private McpResources() {
    }

    public static List<SyncResourceSpecification> all() {
        return List.of(
                componentCatalogResource(),
                scriptBlockReferenceResource(),
                topologiesReferenceResource(),
                examplesListResource()
        );
    }

    private static SyncResourceSpecification componentCatalogResource() {
        String uri = "gecko://catalog/components";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Component Catalog")
                .description("Complete schema of all GeckoCIRCUITS power and control components with parameter names, units, defaults, and pinouts")
                .mimeType("application/json")
                .build();

        return new SyncResourceSpecification(resource, (exchange, req) -> {
            try {
                String text = JSON.writeValueAsString(ComponentCatalog.toCatalogJson());
                return new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(uri, "application/json", text)
                ));
            } catch (Exception e) {
                return new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(uri, "text/plain", "Error: " + e.getMessage())
                ));
            }
        });
    }

    private static SyncResourceSpecification scriptBlockReferenceResource() {
        String uri = "gecko://reference/script-blocks";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Control Script Block Reference")
                .description("Language specification, built-in variables, math functions, and syntax rules for GeckoCIRCUITS ScriptBlockCalculator (typ 61)")
                .mimeType("text/markdown")
                .build();

        String doc = """
                # GeckoCIRCUITS ScriptBlockCalculator (typ 61) Reference Manual
                
                The `ScriptBlockCalculator` is a high-speed interpreted DSP microcontroller block running synchronously inside the simulation loop at timestep `dt`.
                
                ## Simulation Variables
                - `t`: Current simulation time in seconds (double)
                - `dt`: Simulation timestep in seconds (double, e.g. 1.0e-6)
                - `PI`: Math constant 3.141592653589793
                
                ## Input & Output Arrays
                - `xIN[0 .. anzXIN - 1]`: Array of measured input signals from voltage/current probes
                - `yOUT[0 .. anzYOUT - 1]`: Array of synthesized output signals (gate pulses, telemetry, etc.)
                - The script must conclude with: `return yOUT;`
                
                ## State Variables (<staticVariables>)
                Variables declared in `<staticVariables>` retain their values across simulation timesteps (integrator states, duty cycles, step counters):
                ```java
                <staticVariables>
                int step_pfc = 0;
                double v_int = 0.0;
                <\\staticVariables>
                ```
                
                ## Mathematical Operators & Functions
                - Arithmetic: `+`, `-`, `*`, `/`, `%` (modulo), `^` (power)
                - Relational: `<`, `<=`, `>`, `>=`, `==`, `!=`
                - Logical: `&&`, `||`, `!`
                - Ternary Conditional: `(condition) ? (then_val) : (else_val)`
                - Math functions:
                  - `sin(x)`, `cos(x)`, `tan(x)`
                  - `asin(x)`, `acos(x)`, `atan(x)`, `atan2(y, x)`
                  - `abs(x)`, `sqrt(x)`, `exp(x)`, `log(x)`
                  - `pow(x, y)`, `min(x, y)`, `max(x, y)`
                
                ## Safe Division Best Practices
                Because boolean operators evaluate sub-expressions, always guard denominators using ternary operators:
                ```java
                double v_safe = (v_total > 50.0) ? v_total : 750.0;
                double duty = v_target / v_safe;
                ```
                """;

        return new SyncResourceSpecification(resource, (exchange, req) ->
                new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(uri, "text/markdown", doc)
                ))
        );
    }

    private static SyncResourceSpecification topologiesReferenceResource() {
        String uri = "gecko://reference/topologies";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Power Topologies Guide")
                .description("Design guidelines and equations for standard power converter topologies (Vienna Rectifier, Interleaved PFC, LLC Resonant, Buck)")
                .mimeType("text/markdown")
                .build();

        String doc = """
                # Power Converter Topologies Reference Guide
                
                ## 1. 3-Phase 3-Level Vienna Rectifier (Active PFC)
                - **Input**: 3-phase AC ($230\\text{ V}_{\\text{RMS}}$ phase, $400\\text{ V}_{\\text{RMS}}$ line-to-line)
                - **DC Link**: Split DC bus $V_{dc} = 750\\text{ V}$ ($2 \\times 375\\text{ V}$)
                - **Modulation Index**: $M = \\frac{2 \\hat{U}_{ph}}{V_{dc}} = \\frac{2 \\times 325.27}{750} = 0.8674$
                - **Feedforward Duty Cycle**: $d_k(t) = 1.0 - M |\\sin_k(\\omega t)|$
                - **Current Feedback**: $d_k(t) = 1.0 - M |\\sin_k| + \\text{sgn}(\\sin_k) \\cdot K_p (i_{ref,k} - i_{meas,k})$
                - **Nominal Peak Current**: $I_{pk} = \\frac{P_{out}}{3 V_{ph,rms}} \\sqrt{2}$ (e.g. $22.55\\text{ A}$ for $11\\text{ kW}$)
                
                ## 2. DC-DC Buck Step-Down Converter
                - **Transfer Ratio**: $V_{out} = D \\cdot V_{in}$
                - **Nominal Duty**: $D = \\frac{V_{out}}{V_{in}}$ (e.g. $49.5 / 750 = 0.0660$)
                - **Inductor Ripple**: $\\Delta I_L = \\frac{(V_{in} - V_{out}) D}{f_{sw} L}$
                - **Hard Voltage Limit**: Clamp $D \\le \\frac{V_{max}}{V_{in}}$ to strictly enforce overvoltage safety.
                
                ## 3. Half-Bridge LLC Resonant Converter
                - **Resonant Frequency**: $f_0 = \\frac{1}{2 \\pi \\sqrt{L_r C_r}}$
                - **Characteristic Impedance**: $Z_0 = \\sqrt{L_r / C_r}$
                - **ZVS Operation**: Frequency modulated around $f_0$; zero voltage switching achieved via magnetizing current $I_m$ discharging $C_{oss}$ during dead time.
                """;

        return new SyncResourceSpecification(resource, (exchange, req) ->
                new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(uri, "text/markdown", doc)
                ))
        );
    }

    private static SyncResourceSpecification examplesListResource() {
        String uri = "gecko://examples/list";
        McpSchema.Resource resource = McpSchema.Resource.builder(uri, "Workspace Example Circuits")
                .description("Index of available example .ipes circuit models in the repository")
                .mimeType("application/json")
                .build();

        return new SyncResourceSpecification(resource, (exchange, req) -> {
            try {
                Path root = IpesSupport.workspaceRoot();
                Path examplesDir = root.resolve("resources/examples");
                List<Map<String, Object>> examples = new ArrayList<>();

                if (Files.exists(examplesDir)) {
                    try (Stream<Path> stream = Files.walk(examplesDir)) {
                        stream.filter(p -> p.toString().endsWith(".ipes")).forEach(p -> {
                            Map<String, Object> entry = new LinkedHashMap<>();
                            entry.put("path", root.relativize(p).toString().replace('\\', '/'));
                            entry.put("name", p.getFileName().toString().replace(".ipes", ""));
                            try {
                                entry.put("size_bytes", Files.size(p));
                            } catch (IOException ignored) {}
                            examples.add(entry);
                        });
                    }
                }

                String text = JSON.writeValueAsString(examples);
                return new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(uri, "application/json", text)
                ));
            } catch (Exception e) {
                return new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(uri, "text/plain", "Error listing examples: " + e.getMessage())
                ));
            }
        });
    }
}
