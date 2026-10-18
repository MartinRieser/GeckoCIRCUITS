/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package gecko.mcp;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitModel.ComponentData;
import gecko.core.io.CircuitModel.ConnectionData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Headless schematic vector renderer that translates a {@link CircuitModel} into
 * clean, publication-grade SVG diagrams with semantic component graphics, orthogonal
 * multi-domain wire routing, wire junction dots, net labels, and dark/light themes.
 */
public final class SchematicSvgRenderer {

    private static final double DEFAULT_GRID = 22.0;
    private static final double PADDING = 60.0;

    private SchematicSvgRenderer() {
    }

    /**
     * Renders a circuit file to SVG and optionally writes it to disk.
     */
    public static Map<String, Object> exportSvg(
            Path ipesPath,
            Path outputPath,
            String theme,
            double scale
    ) throws IOException, CircuitFileParser.CircuitParseException {
        CircuitModel model = new CircuitFileParser().parse(ipesPath.toString());
        String svg = renderToSvg(model, ipesPath.getFileName().toString(), theme, scale);

        if (outputPath != null) {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            Files.writeString(outputPath, svg, StandardCharsets.UTF_8);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("circuit_file", ipesPath.toString());
        if (outputPath != null) {
            result.put("output_path", outputPath.toString());
        }
        result.put("components_rendered", model.getAllComponents().size());
        result.put("wires_rendered", model.getConnections().size());
        result.put("theme", "dark".equalsIgnoreCase(theme) ? "dark" : "light");
        result.put("svg", svg);
        return result;
    }

    /**
     * Renders a {@link CircuitModel} directly to SVG string.
     */
    public static String renderToSvg(CircuitModel model, String title, String theme, double scale) {
        boolean dark = "dark".equalsIgnoreCase(theme);
        double s = scale > 0.1 ? scale : 1.0;
        double grid = DEFAULT_GRID * s;

        // 1. Calculate bounding box
        int minGx = Integer.MAX_VALUE;
        int minGy = Integer.MAX_VALUE;
        int maxGx = Integer.MIN_VALUE;
        int maxGy = Integer.MIN_VALUE;

        List<ComponentData> allComps = model.getAllComponents();
        for (ComponentData c : allComps) {
            int[] pos = c.getPosition();
            if (pos != null && pos.length >= 2) {
                minGx = Math.min(minGx, pos[0] - 3);
                minGy = Math.min(minGy, pos[1] - 3);
                maxGx = Math.max(maxGx, pos[0] + 3);
                maxGy = Math.max(maxGy, pos[1] + 3);
            }
        }

        List<ConnectionData> conns = model.getConnections();
        for (ConnectionData conn : conns) {
            int[][] pts = conn.getPoints();
            if (pts != null) {
                for (int[] pt : pts) {
                    if (pt != null && pt.length >= 2) {
                        minGx = Math.min(minGx, pt[0]);
                        minGy = Math.min(minGy, pt[1]);
                        maxGx = Math.max(maxGx, pt[0]);
                        maxGy = Math.max(maxGy, pt[1]);
                    }
                }
            }
        }

        if (minGx > maxGx) {
            minGx = 0;
            minGy = 0;
            maxGx = 30;
            maxGy = 20;
        }

        // Bounding dimensions with padding
        double pad = PADDING * s;
        double width = (maxGx - minGx) * grid + 2 * pad;
        double height = (maxGy - minGy) * grid + 2 * pad;

        if (width < 400 * s) width = 400 * s;
        if (height < 300 * s) height = 300 * s;

        StringBuilder sb = new StringBuilder(16384);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %.1f %.1f\" width=\"%.1f\" height=\"%.1f\" class=\"gecko-schematic %s\">\n",
                width, height, width, height, dark ? "theme-dark" : "theme-light"));

        // Embedded styles
        appendStyles(sb, dark);

        // Background & subtle grid pattern
        sb.append(String.format(Locale.ROOT, "  <rect width=\"%.1f\" height=\"%.1f\" fill=\"var(--bg)\" />\n", width, height));
        appendGrid(sb, width, height, grid, dark);

        // Header watermark
        appendHeader(sb, title, allComps.size(), conns.size(), width, pad);

        // 2. Render Connections / Wires
        Map<String, Integer> pointDegree = new HashMap<>();
        for (ConnectionData conn : conns) {
            int[][] pts = conn.getPoints();
            if (pts != null && pts.length > 1) {
                for (int[] pt : pts) {
                    String k = pt[0] + "," + pt[1];
                    pointDegree.put(k, pointDegree.getOrDefault(k, 0) + 1);
                }
            }
        }

        sb.append("  <g class=\"connections\">\n");
        for (ConnectionData conn : conns) {
            String domain = conn.getType() != null ? conn.getType().toUpperCase(Locale.ROOT) : "LK";
            String strokeClass = "wire-lk";
            if ("CONTROL".equals(domain)) strokeClass = "wire-control";
            else if ("THERMAL".equals(domain)) strokeClass = "wire-thermal";

            int[][] pts = conn.getPoints();
            if (pts != null && pts.length > 1) {
                sb.append("    <polyline class=\"").append(strokeClass).append("\" points=\"");
                for (int i = 0; i < pts.length; i++) {
                    double cx = pad + (pts[i][0] - minGx) * grid;
                    double cy = pad + (pts[i][1] - minGy) * grid;
                    if (i > 0) sb.append(" ");
                    sb.append(String.format(Locale.ROOT, "%.1f,%.1f", cx, cy));
                }
                sb.append("\" />\n");

                // Render Net Label Badge if named
                String label = conn.getLabel();
                if (label != null && !label.isBlank() && !label.startsWith("/") && pts.length >= 2) {
                    int mid = pts.length / 2;
                    double mx = pad + (pts[mid][0] - minGx) * grid;
                    double my = pad + (pts[mid][1] - minGy) * grid;
                    renderNetBadge(sb, label, mx, my - 8 * s, dark);
                }
            }
        }

        // Render junction dots (points with >= 3 wire branches)
        for (Map.Entry<String, Integer> e : pointDegree.entrySet()) {
            if (e.getValue() >= 3) {
                String[] parts = e.getKey().split(",");
                int gx = Integer.parseInt(parts[0]);
                int gy = Integer.parseInt(parts[1]);
                double cx = pad + (gx - minGx) * grid;
                double cy = pad + (gy - minGy) * grid;
                sb.append(String.format(Locale.ROOT,
                        "    <circle class=\"junction-dot\" cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" />\n",
                        cx, cy, 3.5 * s));
            }
        }
        sb.append("  </g>\n");

        // 3. Render Components
        sb.append("  <g class=\"components\">\n");
        for (ComponentData comp : allComps) {
            renderComponent(sb, comp, minGx, minGy, grid, pad, s, dark);
        }
        sb.append("  </g>\n");

        sb.append("</svg>\n");
        return sb.toString();
    }

    private static void appendStyles(StringBuilder sb, boolean dark) {
        sb.append("  <style>\n");
        if (dark) {
            sb.append("    :root {\n");
            sb.append("      --bg: #090d16;\n");
            sb.append("      --grid-dot: #1e293b;\n");
            sb.append("      --wire-lk: #60a5fa;\n");
            sb.append("      --wire-ctrl: #34d399;\n");
            sb.append("      --wire-therm: #fbbf24;\n");
            sb.append("      --comp-stroke: #f1f5f9;\n");
            sb.append("      --comp-fill: #1e293b;\n");
            sb.append("      --comp-accent: #312e81;\n");
            sb.append("      --text-main: #f8fafc;\n");
            sb.append("      --text-muted: #94a3b8;\n");
            sb.append("      --badge-bg: #1e293b;\n");
            sb.append("      --badge-stroke: #475569;\n");
            sb.append("      --badge-text: #cbd5e1;\n");
            sb.append("      --pin-dot: #60a5fa;\n");
            sb.append("    }\n");
        } else {
            sb.append("    :root {\n");
            sb.append("      --bg: #f8fafc;\n");
            sb.append("      --grid-dot: #cbd5e1;\n");
            sb.append("      --wire-lk: #2563eb;\n");
            sb.append("      --wire-ctrl: #059669;\n");
            sb.append("      --wire-therm: #d97706;\n");
            sb.append("      --comp-stroke: #0f172a;\n");
            sb.append("      --comp-fill: #ffffff;\n");
            sb.append("      --comp-accent: #eff6ff;\n");
            sb.append("      --text-main: #0f172a;\n");
            sb.append("      --text-muted: #64748b;\n");
            sb.append("      --badge-bg: #ffffff;\n");
            sb.append("      --badge-stroke: #cbd5e1;\n");
            sb.append("      --badge-text: #334155;\n");
            sb.append("      --pin-dot: #2563eb;\n");
            sb.append("    }\n");
        }
        sb.append("    text { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n");
        sb.append("    .wire-lk { fill: none; stroke: var(--wire-lk); stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .wire-control { fill: none; stroke: var(--wire-ctrl); stroke-width: 2.0; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .wire-thermal { fill: none; stroke: var(--wire-therm); stroke-width: 2.0; stroke-dasharray: 4 2; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .junction-dot { fill: var(--comp-stroke); }\n");
        sb.append("    .comp-body { fill: var(--comp-fill); stroke: var(--comp-stroke); stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .comp-lead { fill: none; stroke: var(--comp-stroke); stroke-width: 1.8; stroke-linecap: round; }\n");
        sb.append("    .comp-name { font-size: 11px; font-weight: 700; fill: var(--text-main); text-anchor: middle; }\n");
        sb.append("    .comp-val { font-size: 9.5px; font-weight: 500; fill: var(--text-muted); text-anchor: middle; }\n");
        sb.append("    .net-badge { fill: var(--badge-bg); stroke: var(--badge-stroke); stroke-width: 1.0; rx: 4px; }\n");
        sb.append("    .net-text { font-size: 9px; font-weight: 600; fill: var(--badge-text); text-anchor: middle; font-family: monospace; }\n");
        sb.append("  </style>\n");
    }

    private static void appendGrid(StringBuilder sb, double w, double h, double grid, boolean dark) {
        sb.append("  <defs>\n");
        sb.append(String.format(Locale.ROOT,
                "    <pattern id=\"grid\" width=\"%.1f\" height=\"%.1f\" patternUnits=\"userSpaceOnUse\">\n",
                grid, grid));
        sb.append(String.format(Locale.ROOT,
                "      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"0.8\" fill=\"var(--grid-dot)\" />\n",
                grid / 2.0, grid / 2.0));
        sb.append("    </pattern>\n");
        sb.append("  </defs>\n");
        sb.append(String.format(Locale.ROOT,
                "  <rect width=\"%.1f\" height=\"%.1f\" fill=\"url(#grid)\" />\n", w, h));
    }

    private static void appendHeader(StringBuilder sb, String title, int compCount, int wireCount, double w, double pad) {
        String t = (title != null && !title.isBlank()) ? title : "GeckoCIRCUITS Schematic";
        sb.append("  <g class=\"header\" opacity=\"0.85\">\n");
        sb.append(String.format(Locale.ROOT,
                "    <text x=\"%.1f\" y=\"%.1f\" font-size=\"13\" font-weight=\"700\" fill=\"var(--text-main)\">%s</text>\n",
                pad, pad * 0.45, escapeXml(t)));
        sb.append(String.format(Locale.ROOT,
                "    <text x=\"%.1f\" y=\"%.1f\" font-size=\"10\" font-weight=\"500\" fill=\"var(--text-muted)\">%d components • %d nets</text>\n",
                pad, pad * 0.70, compCount, wireCount));
        sb.append("  </g>\n");
    }

    private static void renderNetBadge(StringBuilder sb, String name, double x, double y, boolean dark) {
        double bw = Math.max(30.0, name.length() * 6.5 + 10.0);
        double bh = 14.0;
        sb.append(String.format(Locale.ROOT,
                "    <g transform=\"translate(%.1f, %.1f)\">\n", x - bw / 2.0, y - bh / 2.0));
        sb.append(String.format(Locale.ROOT,
                "      <rect class=\"net-badge\" width=\"%.1f\" height=\"%.1f\" />\n", bw, bh));
        sb.append(String.format(Locale.ROOT,
                "      <text class=\"net-text\" x=\"%.1f\" y=\"10\">%s</text>\n", bw / 2.0, escapeXml(name)));
        sb.append("    </g>\n");
    }

    private static void renderComponent(
            StringBuilder sb,
            ComponentData comp,
            int minGx,
            int minGy,
            double grid,
            double pad,
            double s,
            boolean dark
    ) {
        int[] pos = comp.getPosition();
        if (pos == null || pos.length < 2) return;

        double cx = pad + (pos[0] - minGx) * grid;
        double cy = pad + (pos[1] - minGy) * grid;
        int orient = comp.getOrientation();
        double rotDeg = orientationToAngle(orient);

        String name = comp.getName();
        String paramVal = formatPrimaryParam(comp);
        int typ = comp.getType();
        String family = comp.getFamily() != null ? comp.getFamily() : "LK";

        sb.append(String.format(Locale.ROOT,
                "    <!-- %s (%s typ %d) -->\n", escapeXml(name), family, typ));
        sb.append(String.format(Locale.ROOT,
                "    <g transform=\"translate(%.1f, %.1f) rotate(%.1f)\">\n", cx, cy, rotDeg));

        // Symbol Body
        if ("CONTROL".equals(family)) {
            renderControlBlock(sb, comp, s, dark);
        } else if ("THERM".equals(family)) {
            renderThermalComponent(sb, comp, s, dark);
        } else {
            renderPowerComponent(sb, comp, s, dark);
        }

        sb.append("    </g>\n");

        // Labels rendered without rotation for crisp readability
        double labelOffsetY = -22.0 * s;
        double valOffsetY = 24.0 * s;
        if (typ == 31 || typ == 50) { // Ground
            labelOffsetY = -16.0 * s;
            valOffsetY = 16.0 * s;
        }

        sb.append(String.format(Locale.ROOT,
                "    <text class=\"comp-name\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                cx, cy + labelOffsetY, escapeXml(name)));
        if (!paramVal.isBlank()) {
            sb.append(String.format(Locale.ROOT,
                    "    <text class=\"comp-val\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                    cx, cy + valOffsetY, escapeXml(paramVal)));
        }
    }

    private static double orientationToAngle(int orientation) {
        return switch (orientation) {
            case 501 -> 180.0; // SOUTH_NORTH
            case 502 -> 90.0;  // WEST_EAST
            case 504 -> 270.0; // EAST_WEST
            default -> 0.0;    // NORTH_SOUTH (East/Standard)
        };
    }

    private static void renderPowerComponent(StringBuilder sb, ComponentData comp, double s, boolean dark) {
        int typ = comp.getType();
        double p = 22.0 * s; // Half-pitch

        switch (typ) {
            case 1 -> { // Resistor
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -12.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 12.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"1\" />\n",
                        -6.0 * s, -12.0 * s, 12.0 * s, 24.0 * s));
            }
            case 2 -> { // Inductor
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -12.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 12.0 * s, p));
                // 3 coiled semicircles
                sb.append(String.format(Locale.ROOT,
                        "      <path class=\"comp-lead\" d=\"M 0,%.1f A 4,4 0 0,1 0,%.1f A 4,4 0 0,1 0,%.1f A 4,4 0 0,1 0,%.1f\" stroke-width=\"2\" />\n",
                        -12.0 * s, -4.0 * s, 4.0 * s, 12.0 * s));
            }
            case 3 -> { // Capacitor
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -3.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 3.5 * s, p));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.5\" />\n",
                        -10.0 * s, -3.5 * s, 10.0 * s, -3.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.5\" />\n",
                        -10.0 * s, 3.5 * s, 10.0 * s, 3.5 * s));
            }
            case 4 -> { // Voltage Source (AC or DC)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -13.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 13.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"0\" r=\"%.1f\" />\n", 13.0 * s));
                double[] raw = comp.getRawParameters();
                boolean isAc = (raw != null && raw.length > 0 && Math.abs(raw[0] - 402.0) < 0.5)
                        || comp.getName().toUpperCase(Locale.ROOT).contains("AC");
                if (isAc) {
                    // Sine symbol ~
                    sb.append(String.format(Locale.ROOT,
                            "      <path class=\"comp-lead\" d=\"M -6,0 Q -3,-6 0,0 Q 3,6 6,0\" stroke-width=\"1.8\" />\n"));
                } else {
                    // DC + -
                    sb.append("      <text font-size=\"9\" font-weight=\"bold\" fill=\"var(--text-main)\" text-anchor=\"middle\" x=\"0\" y=\"-3\">+</text>\n");
                    sb.append("      <text font-size=\"9\" font-weight=\"bold\" fill=\"var(--text-main)\" text-anchor=\"middle\" x=\"0\" y=\"8\">-</text>\n");
                }
            }
            case 5 -> { // Current Source
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -13.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 13.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"0\" r=\"%.1f\" />\n", 13.0 * s));
                sb.append("      <line class=\"comp-lead\" x1=\"0\" y1=\"-7\" x2=\"0\" y2=\"7\" stroke-width=\"1.8\" />\n");
                sb.append("      <polygon points=\"0,7 -3,3 3,3\" fill=\"var(--comp-stroke)\" />\n");
            }
            case 6 -> { // Diode
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -8.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 8.0 * s, p));
                // Triangle anode -> cathode
                sb.append(String.format(Locale.ROOT,
                        "      <polygon class=\"comp-body\" points=\"%.1f,%.1f %.1f,%.1f %.1f,%.1f\" />\n",
                        -8.0 * s, -8.0 * s, 8.0 * s, -8.0 * s, 0.0, 8.0 * s));
                // Cathode bar
                sb.append(String.format(Locale.ROOT,
                        "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.0\" />\n",
                        -8.0 * s, 8.0 * s, 8.0 * s, 8.0 * s));
            }
            case 28, 11, 7, 10 -> { // MOSFET / IGBT / Switch
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -9.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 9.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"2\" />\n",
                        -10.0 * s, -9.0 * s, 20.0 * s, 18.0 * s));
                // Gate notch
                sb.append(String.format(Locale.ROOT,
                        "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"0\" x2=\"%.1f\" y2=\"0\" stroke-width=\"1.8\" />\n",
                        -15.0 * s, -10.0 * s));
                String swType = typ == 28 ? "FET" : (typ == 11 ? "IGBT" : "SW");
                sb.append(String.format(Locale.ROOT,
                        "      <text font-size=\"8\" font-weight=\"bold\" fill=\"var(--text-main)\" text-anchor=\"middle\" x=\"0\" y=\"3\">%s</text>\n",
                        swType));
            }
            case 31 -> { // Global Terminal / Ground
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"0\" />\n", -p));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"0\" x2=\"%.1f\" y2=\"0\" stroke-width=\"2.2\" />\n", -10.0 * s, 10.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"1.8\" />\n", -6.0 * s, 3.5 * s, 6.0 * s, 3.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"1.4\" />\n", -2.5 * s, 7.0 * s, 2.5 * s, 7.0 * s));
            }
            default -> { // Generic 2-terminal block
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -10.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 10.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"3\" />\n",
                        -10.0 * s, -10.0 * s, 20.0 * s, 20.0 * s));
            }
        }
    }

    private static void renderControlBlock(StringBuilder sb, ComponentData comp, double s, boolean dark) {
        int typ = comp.getType();
        double p = 22.0 * s;

        if (typ == 1001 || typ == 1002 || typ == 1003) { // Probes
            sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"0\" r=\"%.1f\" fill=\"var(--comp-accent)\" />\n", 12.0 * s));
            String badge = typ == 1001 ? "V" : (typ == 1002 ? "A" : "OSC");
            sb.append(String.format(Locale.ROOT,
                    "      <text font-size=\"10\" font-weight=\"bold\" fill=\"var(--text-main)\" text-anchor=\"middle\" x=\"0\" y=\"3.5\">%s</text>\n", badge));
        } else if (typ == 6 || typ == 1000) { // Gate block
            sb.append(String.format(Locale.ROOT,
                    "      <polygon class=\"comp-body\" points=\"%.1f,%.1f %.1f,%.1f %.1f,%.1f\" fill=\"var(--comp-accent)\" />\n",
                    -10.0 * s, -8.0 * s, 10.0 * s, 0.0, -10.0 * s, 8.0 * s));
        } else { // Generic IC / Script Block / Math block
            sb.append(String.format(Locale.ROOT,
                    "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"4\" fill=\"var(--comp-accent)\" />\n",
                    -16.0 * s, -12.0 * s, 32.0 * s, 24.0 * s));
            String label = comp.getName().contains("SPWM") ? "SPWM" : (comp.getName().contains("PI") ? "PI" : "CTRL");
            sb.append(String.format(Locale.ROOT,
                    "      <text font-size=\"9\" font-weight=\"bold\" fill=\"var(--text-main)\" text-anchor=\"middle\" x=\"0\" y=\"3\">%s</text>\n", label));
        }
    }

    private static void renderThermalComponent(StringBuilder sb, ComponentData comp, double s, boolean dark) {
        int typ = comp.getType();
        if (typ == 50) { // Thermal Ground
            sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"-15\" x2=\"0\" y2=\"0\" stroke=\"var(--wire-therm)\" />\n"));
            sb.append(String.format(Locale.ROOT, "      <circle cx=\"0\" cy=\"0\" r=\"%.1f\" fill=\"var(--wire-therm)\" />\n", 4.0 * s));
            sb.append("      <text font-size=\"8\" font-weight=\"bold\" fill=\"var(--wire-therm)\" text-anchor=\"middle\" x=\"0\" y=\"12\">T_amb</text>\n");
        } else {
            sb.append(String.format(Locale.ROOT,
                    "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"3\" stroke=\"var(--wire-therm)\" />\n",
                    -12.0 * s, -12.0 * s, 24.0 * s, 24.0 * s));
            sb.append("      <text font-size=\"8\" font-weight=\"bold\" fill=\"var(--wire-therm)\" text-anchor=\"middle\" x=\"0\" y=\"3\">TH</text>\n");
        }
    }

    private static String formatPrimaryParam(ComponentData comp) {
        int typ = comp.getType();
        double[] raw = comp.getRawParameters();
        if (raw == null || raw.length == 0) {
            String[] pstrs = comp.getParameterStrings();
            if (pstrs != null && pstrs.length > 0 && !pstrs[0].equals("NIX_NIX_NIX")) {
                return pstrs[0];
            }
            return "";
        }

        return switch (typ) {
            case 1 -> formatEng(raw[0], "Ω");
            case 2 -> formatEng(raw[0], "H");
            case 3 -> formatEng(raw[0], "F");
            case 4 -> {
                boolean isAc = Math.abs(raw[0] - 402.0) < 0.5;
                if (isAc && raw.length > 2) {
                    yield String.format(Locale.ROOT, "%.0fV %.0fHz", raw[1], raw[2]);
                } else if (raw.length > 1) {
                    yield String.format(Locale.ROOT, "%.1fV", raw[1]);
                }
                yield "";
            }
            case 5 -> raw.length > 1 ? String.format(Locale.ROOT, "%.2fA", raw[1]) : "";
            default -> "";
        };
    }

    private static String formatEng(double val, String unit) {
        if (Math.abs(val) < 1e-15) return "0 " + unit;
        double abs = Math.abs(val);
        if (abs >= 1e6) return String.format(Locale.ROOT, "%.2f M%s", val / 1e6, unit);
        if (abs >= 1e3) return String.format(Locale.ROOT, "%.2f k%s", val / 1e3, unit);
        if (abs >= 1.0) return String.format(Locale.ROOT, "%.2f %s", val, unit);
        if (abs >= 1e-3) return String.format(Locale.ROOT, "%.2f m%s", val * 1e3, unit);
        if (abs >= 1e-6) return String.format(Locale.ROOT, "%.2f µ%s", val * 1e6, unit);
        if (abs >= 1e-9) return String.format(Locale.ROOT, "%.2f n%s", val * 1e9, unit);
        return String.format(Locale.ROOT, "%.2e %s", val, unit);
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
