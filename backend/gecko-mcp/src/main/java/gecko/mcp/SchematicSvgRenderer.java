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
    private static final double PADDING = 72.0;

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
                minGx = Math.min(minGx, pos[0] - 4);
                minGy = Math.min(minGy, pos[1] - 4);
                maxGx = Math.max(maxGx, pos[0] + 6);
                maxGy = Math.max(maxGy, pos[1] + 5);
            }
        }

        List<ConnectionData> conns = model.getConnections();
        for (ConnectionData conn : conns) {
            int[][] pts = conn.getPoints();
            if (pts != null) {
                for (int[] pt : pts) {
                    if (pt != null && pt.length >= 2) {
                        minGx = Math.min(minGx, pt[0] - 2);
                        minGy = Math.min(minGy, pt[1] - 2);
                        maxGx = Math.max(maxGx, pt[0] + 2);
                        maxGy = Math.max(maxGy, pt[1] + 2);
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

        double pad = PADDING * s;
        double width = (maxGx - minGx) * grid + 2 * pad;
        double height = (maxGy - minGy) * grid + 2 * pad;

        if (width < 460 * s) width = 460 * s;
        if (height < 320 * s) height = 320 * s;

        StringBuilder sb = new StringBuilder(20480);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %.1f %.1f\" width=\"%.1f\" height=\"%.1f\" class=\"gecko-schematic %s\">\n",
                width, height, width, height, dark ? "theme-dark" : "theme-light"));

        appendStyles(sb, dark);

        // Background & subtle grid pattern
        sb.append(String.format(Locale.ROOT, "  <rect width=\"%.1f\" height=\"%.1f\" fill=\"var(--bg)\" />\n", width, height));
        appendGrid(sb, width, height, grid, dark);

        // Header watermark
        long distinctNets = conns.stream()
                .map(ConnectionData::getLabel)
                .filter(l -> l != null && !l.isBlank() && !l.equalsIgnoreCase("NIX_NIX_NIX"))
                .map(l -> l.startsWith("/") ? l.substring(1) : l)
                .distinct()
                .count();
        appendHeader(sb, title, allComps.size(), distinctNets > 0 ? (int) distinctNets : conns.size(), width, pad);

        // Domain separator if both Power/Thermal and Control stages exist
        appendDomainSeparator(sb, model, minGy, grid, pad, width, s);

        // 2. Render Connections / Wires (Orthogonalized + True T-Junction detection)
        Map<String, Integer> dirMaskByPoint = new HashMap<>();
        recordComponentPinDirections(allComps, dirMaskByPoint);
        sb.append("  <g class=\"connections\">\n");

        Set<String> renderedBadges = new HashSet<>();
        for (ConnectionData conn : conns) {
            String domain = conn.getType() != null ? conn.getType().toUpperCase(Locale.ROOT) : "LK";
            String strokeClass = "wire-lk";
            if ("CONTROL".equals(domain)) strokeClass = "wire-control";
            else if ("THERMAL".equals(domain)) strokeClass = "wire-thermal";

            List<int[]> orthoPts = orthogonalizePoints(conn.getPoints());
            if (orthoPts.size() > 1) {
                sb.append("    <polyline class=\"").append(strokeClass).append("\" points=\"");
                for (int i = 0; i < orthoPts.size(); i++) {
                    int[] pt = orthoPts.get(i);
                    double cx = pad + (pt[0] - minGx) * grid;
                    double cy = pad + (pt[1] - minGy) * grid;
                    if (i > 0) sb.append(" ");
                    sb.append(String.format(Locale.ROOT, "%.1f,%.1f", cx, cy));
                }
                sb.append("\" />\n");

                // Accumulate unit step direction bitmask for true T-junction detection
                recordSegmentDirections(orthoPts, dirMaskByPoint);

                // Render Net Label Badge on longest horizontal segment
                String label = conn.getLabel();
                if (label != null && !label.isBlank()) {
                    String clean = label.startsWith("/") ? label.substring(1) : label;
                    if (!clean.isBlank() && !clean.equalsIgnoreCase("NIX_NIX_NIX")
                            && renderedBadges.add(domain + ":" + clean)) {
                        double[] anchor = findBestBadgeAnchor(orthoPts, minGx, minGy, grid, pad, s);
                        renderNetBadge(sb, clean, anchor[0], anchor[1], dark);
                    }
                }
            }
        }

        // Render junction dots where >= 3 orthogonal wire directions meet
        for (Map.Entry<String, Integer> e : dirMaskByPoint.entrySet()) {
            if (Integer.bitCount(e.getValue()) >= 3) {
                String[] parts = e.getKey().split(",");
                int gx = Integer.parseInt(parts[0]);
                int gy = Integer.parseInt(parts[1]);
                double cx = pad + (gx - minGx) * grid;
                double cy = pad + (gy - minGy) * grid;
                sb.append(String.format(Locale.ROOT,
                        "    <circle class=\"junction-dot\" cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" />\n",
                        cx, cy, 3.6 * s));
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

    /**
     * Converts raw polyline points into strictly orthogonal corner points so diagonal slashes
     * can never appear even on legacy or hand-edited files.
     */
    private static List<int[]> orthogonalizePoints(int[][] raw) {
        List<int[]> out = new ArrayList<>();
        if (raw == null || raw.length == 0) return out;
        for (int[] pt : raw) {
            if (pt == null || pt.length < 2) continue;
            if (out.isEmpty()) {
                out.add(new int[]{pt[0], pt[1]});
                continue;
            }
            int[] prev = out.get(out.size() - 1);
            if (prev[0] == pt[0] && prev[1] == pt[1]) {
                continue;
            }
            if (prev[0] != pt[0] && prev[1] != pt[1]) {
                // Insert orthogonal L-elbow
                out.add(new int[]{pt[0], prev[1]});
            }
            out.add(new int[]{pt[0], pt[1]});
        }
        return simplifyCollinear(out);
    }

    /**
     * Collapses intermediate collinear unit-step points into clean segment endpoints.
     */
    private static List<int[]> simplifyCollinear(List<int[]> pts) {
        if (pts.size() <= 2) return pts;
        List<int[]> res = new ArrayList<>();
        res.add(pts.get(0));
        for (int i = 1; i < pts.size() - 1; i++) {
            int[] a = res.get(res.size() - 1);
            int[] b = pts.get(i);
            int[] c = pts.get(i + 1);
            int dx1 = Integer.compare(b[0], a[0]);
            int dy1 = Integer.compare(b[1], a[1]);
            int dx2 = Integer.compare(c[0], b[0]);
            int dy2 = Integer.compare(c[1], b[1]);
            if (dx1 != dx2 || dy1 != dy2) {
                res.add(b);
            }
        }
        res.add(pts.get(pts.size() - 1));
        return res;
    }

    /**
     * Records directional bitmask (1=E, 2=W, 4=S, 8=N) along every unit grid step of an orthogonal polyline.
     */
    private static void recordSegmentDirections(List<int[]> pts, Map<String, Integer> dirMaskByPoint) {
        for (int i = 0; i < pts.size() - 1; i++) {
            int x1 = pts.get(i)[0];
            int y1 = pts.get(i)[1];
            int x2 = pts.get(i + 1)[0];
            int y2 = pts.get(i + 1)[1];
            int dx = Integer.compare(x2, x1);
            int dy = Integer.compare(y2, y1);
            if (dx == 0 && dy == 0) continue;
            int fwdBit = dx > 0 ? 1 : (dx < 0 ? 2 : (dy > 0 ? 4 : 8));
            int revBit = dx > 0 ? 2 : (dx < 0 ? 1 : (dy > 0 ? 8 : 4));
            int cx = x1;
            int cy = y1;
            while (cx != x2 || cy != y2) {
                String k1 = cx + "," + cy;
                dirMaskByPoint.put(k1, dirMaskByPoint.getOrDefault(k1, 0) | fwdBit);
                cx += dx;
                cy += dy;
                String k2 = cx + "," + cy;
                dirMaskByPoint.put(k2, dirMaskByPoint.getOrDefault(k2, 0) | revBit);
            }
        }
    }

    /**
     * Records the inward component-lead direction at each 2-terminal LK/THERM pin
     * so T-junctions where a component lead taps onto a bus or meets two wires
     * receive a standard schematic junction dot.
     */
    private static void recordComponentPinDirections(List<ComponentData> comps, Map<String, Integer> dirMaskByPoint) {
        for (ComponentData c : comps) {
            String fam = c.getFamily() != null ? c.getFamily().toUpperCase(Locale.ROOT) : "LK";
            if (!"LK".equals(fam) && !"THERM".equals(fam)) continue;
            int typ = c.getType();
            if (typ == 9 || typ == 14) continue;
            int[] pos = c.getPosition();
            if (pos == null || pos.length < 2) continue;
            int cx = pos[0];
            int cy = pos[1];
            if (typ == 31 || typ == 32 || typ == 50) {
                // Ground pin has lead going South (4) from (cx, cy)
                String k = cx + "," + cy;
                dirMaskByPoint.put(k, dirMaskByPoint.getOrDefault(k, 0) | 4);
                continue;
            }
            int orient = c.getOrientation();
            if (orient == 502 || orient == 504) {
                // Horizontal: left pin (cx-2, cy) lead goes East (1), right pin (cx+2, cy) lead goes West (2)
                String kL = (cx - 2) + "," + cy;
                String kR = (cx + 2) + "," + cy;
                dirMaskByPoint.put(kL, dirMaskByPoint.getOrDefault(kL, 0) | 1);
                dirMaskByPoint.put(kR, dirMaskByPoint.getOrDefault(kR, 0) | 2);
            } else {
                // Vertical: top pin (cx, cy-2) lead goes South (4), bottom pin (cx, cy+2) lead goes North (8)
                String kT = cx + "," + (cy - 2);
                String kB = cx + "," + (cy + 2);
                dirMaskByPoint.put(kT, dirMaskByPoint.getOrDefault(kT, 0) | 4);
                dirMaskByPoint.put(kB, dirMaskByPoint.getOrDefault(kB, 0) | 8);
            }
        }
    }

    private static double[] findBestBadgeAnchor(
            List<int[]> pts,
            int minGx,
            int minGy,
            double grid,
            double pad,
            double s
    ) {
        int bestIdx = 0;
        double bestScore = -1.0;
        for (int i = 0; i < pts.size() - 1; i++) {
            int[] a = pts.get(i);
            int[] b = pts.get(i + 1);
            double len = Math.abs(b[0] - a[0]) + Math.abs(b[1] - a[1]);
            // Prefer horizontal segments strongly over vertical segments
            double score = (a[1] == b[1]) ? len * 3.0 : len;
            if (score > bestScore) {
                bestScore = score;
                bestIdx = i;
            }
        }
        int[] a = pts.get(bestIdx);
        int[] b = pts.get(bestIdx + 1);
        double mx = pad + ((a[0] + b[0]) * 0.5 - minGx) * grid;
        double my = pad + ((a[1] + b[1]) * 0.5 - minGy) * grid;
        if (a[1] == b[1]) {
            return new double[]{mx, my - 9.0 * s};
        } else {
            return new double[]{mx + 14.0 * s, my};
        }
    }

    private static void appendDomainSeparator(
            StringBuilder sb,
            CircuitModel model,
            int minGy,
            double grid,
            double pad,
            double width,
            double s
    ) {
        if (model.getCircuitComponents().isEmpty() || model.getControlComponents().isEmpty()) {
            return;
        }
        int maxPowerGy = Integer.MIN_VALUE;
        for (ComponentData c : model.getCircuitComponents()) {
            int[] p = c.getPosition();
            if (p != null && p.length >= 2) maxPowerGy = Math.max(maxPowerGy, p[1] + 2);
        }
        for (ComponentData c : model.getThermalComponents()) {
            int[] p = c.getPosition();
            if (p != null && p.length >= 2) maxPowerGy = Math.max(maxPowerGy, p[1] + 2);
        }
        int minCtrlGy = Integer.MAX_VALUE;
        for (ComponentData c : model.getControlComponents()) {
            int[] p = c.getPosition();
            if (p != null && p.length >= 2) minCtrlGy = Math.min(minCtrlGy, p[1] - 2);
        }
        if (maxPowerGy < minCtrlGy && minCtrlGy < Integer.MAX_VALUE) {
            double sepY = pad + ((maxPowerGy + minCtrlGy) * 0.5 - minGy) * grid;
            sb.append("  <g class=\"domain-separator\" opacity=\"0.65\">\n");
            sb.append(String.format(Locale.ROOT,
                    "    <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"var(--badge-stroke)\" stroke-width=\"1\" stroke-dasharray=\"6 4\" />\n",
                    pad * 0.6, sepY, width - pad * 0.6, sepY));
            sb.append(String.format(Locale.ROOT,
                    "    <text x=\"%.1f\" y=\"%.1f\" font-size=\"9\" font-weight=\"700\" fill=\"var(--text-muted)\" letter-spacing=\"0.8\">POWER STAGE</text>\n",
                    pad * 0.6, sepY - 6 * s));
            sb.append(String.format(Locale.ROOT,
                    "    <text x=\"%.1f\" y=\"%.1f\" font-size=\"9\" font-weight=\"700\" fill=\"var(--wire-ctrl)\" letter-spacing=\"0.8\">CONTROL &amp; MODULATION</text>\n",
                    pad * 0.6, sepY + 13 * s));
            sb.append("  </g>\n");
        }
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
            sb.append("      --comp-accent: #1e1b4b;\n");
            sb.append("      --text-main: #f8fafc;\n");
            sb.append("      --text-muted: #94a3b8;\n");
            sb.append("      --badge-bg: #0f172a;\n");
            sb.append("      --badge-stroke: #475569;\n");
            sb.append("      --badge-text: #93c5fd;\n");
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
            sb.append("      --text-muted: #475569;\n");
            sb.append("      --badge-bg: #ffffff;\n");
            sb.append("      --badge-stroke: #94a3b8;\n");
            sb.append("      --badge-text: #1d4ed8;\n");
            sb.append("      --pin-dot: #2563eb;\n");
            sb.append("    }\n");
        }
        sb.append("    text { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n");
        sb.append("    .wire-lk { fill: none; stroke: var(--wire-lk); stroke-width: 2.3; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .wire-control { fill: none; stroke: var(--wire-ctrl); stroke-width: 2.0; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .wire-thermal { fill: none; stroke: var(--wire-therm); stroke-width: 2.0; stroke-dasharray: 4 2; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .junction-dot { fill: var(--wire-lk); stroke: var(--bg); stroke-width: 1.0; }\n");
        sb.append("    .comp-body { fill: var(--comp-fill); stroke: var(--comp-stroke); stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .comp-lead { fill: none; stroke: var(--comp-stroke); stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }\n");
        sb.append("    .comp-name { font-size: 11px; font-weight: 700; fill: var(--text-main); }\n");
        sb.append("    .comp-val { font-size: 9.5px; font-weight: 500; fill: var(--text-muted); }\n");
        sb.append("    .net-badge { fill: var(--badge-bg); stroke: var(--badge-stroke); stroke-width: 1.0; rx: 4px; }\n");
        sb.append("    .net-text { font-size: 9px; font-weight: 600; fill: var(--badge-text); text-anchor: middle; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }\n");
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
        sb.append("  <g class=\"header\" opacity=\"0.9\">\n");
        sb.append(String.format(Locale.ROOT,
                "    <text x=\"%.1f\" y=\"%.1f\" font-size=\"13\" font-weight=\"700\" fill=\"var(--text-main)\">%s</text>\n",
                pad * 0.6, pad * 0.40, escapeXml(t)));
        sb.append(String.format(Locale.ROOT,
                "    <text x=\"%.1f\" y=\"%.1f\" font-size=\"10\" font-weight=\"500\" fill=\"var(--text-muted)\">%d components • %d nets</text>\n",
                pad * 0.6, pad * 0.62, compCount, wireCount));
        sb.append("  </g>\n");
    }

    private static void renderNetBadge(StringBuilder sb, String name, double x, double y, boolean dark) {
        double bw = Math.max(26.0, name.length() * 6.2 + 10.0);
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

        if ("CONTROL".equals(family)) {
            sb.append(String.format(Locale.ROOT,
                    "    <g transform=\"translate(%.1f, %.1f)\">\n", cx, cy));
            renderControlBlock(sb, comp, s, grid, dark);
            sb.append("    </g>\n");
            renderControlLabels(sb, comp, cx, cy, s);
            return;
        }

        sb.append(String.format(Locale.ROOT,
                "    <g transform=\"translate(%.1f, %.1f) rotate(%.1f)\">\n", cx, cy, rotDeg));
        if ("THERM".equals(family)) {
            renderThermalComponent(sb, comp, s, dark);
        } else {
            renderPowerComponent(sb, comp, s, dark);
        }
        sb.append("    </g>\n");

        // Orientation-aware label placement: vertical components place labels to the right
        // of the symbol so text never collides with vertical wire leads.
        boolean isHorizontal = (orient == 502 || orient == 504);
        if (typ == 31 || typ == 32 || typ == 50) {
            sb.append(String.format(Locale.ROOT,
                    "    <text class=\"comp-name\" text-anchor=\"middle\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                    cx, cy + 18.0 * s, escapeXml(name)));
        } else if (isHorizontal) {
            sb.append(String.format(Locale.ROOT,
                    "    <text class=\"comp-name\" text-anchor=\"middle\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                    cx, cy - 16.0 * s, escapeXml(name)));
            if (!paramVal.isBlank()) {
                sb.append(String.format(Locale.ROOT,
                        "    <text class=\"comp-val\" text-anchor=\"middle\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                        cx, cy + 23.0 * s, escapeXml(paramVal)));
            }
        } else {
            double labelX = cx + 16.0 * s;
            if (!paramVal.isBlank()) {
                sb.append(String.format(Locale.ROOT,
                        "    <text class=\"comp-name\" text-anchor=\"start\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                        labelX, cy - 2.0 * s, escapeXml(name)));
                sb.append(String.format(Locale.ROOT,
                        "    <text class=\"comp-val\" text-anchor=\"start\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                        labelX, cy + 11.0 * s, escapeXml(paramVal)));
            } else {
                sb.append(String.format(Locale.ROOT,
                        "    <text class=\"comp-name\" text-anchor=\"start\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                        labelX, cy + 4.0 * s, escapeXml(name)));
            }
        }
    }

    private static void renderControlLabels(StringBuilder sb, ComponentData comp, double cx, double cy, double s) {
        int typ = comp.getType();
        String name = comp.getName();
        String target = extractCoupledTarget(comp);

        if (typ == 1 || typ == 2 || typ == 1001 || typ == 1002 || typ == 1003) {
            // Probe: label to the left or above so output wire to the right is clear
            String title = !target.isBlank() ? (name + " (" + target + ")") : name;
            sb.append(String.format(Locale.ROOT,
                    "    <text class=\"comp-name\" text-anchor=\"end\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                    cx - 16.0 * s, cy + 4.0 * s, escapeXml(title)));
        } else if (typ == 6 || typ == 1000) {
            // Gate driver: label to the right with target switch arrow
            String title = !target.isBlank() ? (name + " → " + target) : name;
            sb.append(String.format(Locale.ROOT,
                    "    <text class=\"comp-name\" text-anchor=\"start\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                    cx + 14.0 * s, cy + 4.0 * s, escapeXml(title)));
        } else {
            sb.append(String.format(Locale.ROOT,
                    "    <text class=\"comp-name\" text-anchor=\"middle\" x=\"%.1f\" y=\"%.1f\">%s</text>\n",
                    cx, cy - 18.0 * s, escapeXml(name)));
        }
    }

    private static String extractCoupledTarget(ComponentData comp) {
        String[] pstrs = comp.getParameterStrings();
        if (pstrs != null && pstrs.length > 0 && pstrs[0] != null) {
            String t = pstrs[0].trim();
            if (t.startsWith("/")) t = t.substring(1);
            if (!t.isBlank() && !t.equalsIgnoreCase("NIX_NIX_NIX") && !t.equals("0")) {
                return t;
            }
        }
        return "";
    }

    private static double orientationToAngle(int orientation) {
        return switch (orientation) {
            case 501 -> 180.0; // SOUTH_NORTH
            case 502 -> 270.0; // WEST_EAST: local -p (top) rotates to -p (left), +p (bottom) to +p (right)
            case 504 -> 90.0;  // EAST_WEST: local -p (top) rotates to +p (right), +p (bottom) to -p (left)
            default -> 0.0;    // NORTH_SOUTH
        };
    }

    private static void renderPowerComponent(StringBuilder sb, ComponentData comp, double s, boolean dark) {
        int typ = comp.getType();
        double p = 2.0 * DEFAULT_GRID * s; // 2 grid units half-pitch (44px from center to each terminal, 88px total)

        switch (typ) {
            case 1 -> { // Resistor (IEC rectangle)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -12.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 12.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"1.5\" />\n",
                        -5.5 * s, -12.0 * s, 11.0 * s, 24.0 * s));
            }
            case 2, 12, 13 -> { // Inductor (4 smooth coil arches)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -14.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 14.0 * s, p));
                sb.append(String.format(Locale.ROOT,
                        "      <path class=\"comp-lead\" d=\"M 0,%.1f A %.1f,%.1f 0 0,1 0,%.1f A %.1f,%.1f 0 0,1 0,%.1f A %.1f,%.1f 0 0,1 0,%.1f A %.1f,%.1f 0 0,1 0,%.1f\" stroke-width=\"2.0\" />\n",
                        -14.0 * s,
                        4.2 * s, 3.5 * s, -7.0 * s,
                        4.2 * s, 3.5 * s, 0.0,
                        4.2 * s, 3.5 * s, 7.0 * s,
                        4.2 * s, 3.5 * s, 14.0 * s));
            }
            case 3 -> { // Capacitor (parallel plates)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -3.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 3.5 * s, p));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.6\" />\n",
                        -11.0 * s, -3.5 * s, 11.0 * s, -3.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.6\" />\n",
                        -11.0 * s, 3.5 * s, 11.0 * s, 3.5 * s));
            }
            case 4 -> { // Voltage Source (AC or DC)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -13.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 13.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"0\" r=\"%.1f\" />\n", 13.0 * s));
                double[] raw = comp.getRawParameters();
                boolean isAc = (raw != null && raw.length > 0 && Math.abs(raw[0] - 402.0) < 0.5)
                        || comp.getName().toUpperCase(Locale.ROOT).contains("AC");
                if (isAc) {
                    sb.append(String.format(Locale.ROOT,
                            "      <path class=\"comp-lead\" d=\"M %.1f,0 Q %.1f,%.1f 0,0 Q %.1f,%.1f %.1f,0\" stroke-width=\"1.8\" />\n",
                            -6.5 * s, -3.2 * s, -6.5 * s, 3.2 * s, 6.5 * s, 6.5 * s));
                } else {
                    // Plus at -y (termA), Minus at +y (termB)
                    sb.append(String.format(Locale.ROOT,
                            "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"1.6\" />\n",
                            -3.5 * s, -5.5 * s, 3.5 * s, -5.5 * s));
                    sb.append(String.format(Locale.ROOT,
                            "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke-width=\"1.6\" />\n",
                            -9.0 * s, -2.0 * s));
                    sb.append(String.format(Locale.ROOT,
                            "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"1.6\" />\n",
                            -3.5 * s, 6.0 * s, 3.5 * s, 6.0 * s));
                }
            }
            case 5 -> { // Current Source
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -13.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 13.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"0\" r=\"%.1f\" />\n", 13.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke-width=\"1.8\" />\n", -7.0 * s, 7.0 * s));
                sb.append(String.format(Locale.ROOT, "      <polygon points=\"0,%.1f %.1f,%.1f %.1f,%.1f\" fill=\"var(--comp-stroke)\" />\n",
                        7.5 * s, -3.5 * s, 2.5 * s, 3.5 * s, 2.5 * s));
            }
            case 6, 8 -> { // Diode (6) / Thyristor (8)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -7.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 7.5 * s, p));
                sb.append(String.format(Locale.ROOT,
                        "      <polygon class=\"comp-body\" points=\"%.1f,%.1f %.1f,%.1f 0,%.1f\" />\n",
                        -8.0 * s, -7.5 * s, 8.0 * s, -7.5 * s, 7.5 * s));
                sb.append(String.format(Locale.ROOT,
                        "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.2\" />\n",
                        -8.5 * s, 7.5 * s, 8.5 * s, 7.5 * s));
                if (typ == 8) { // Thyristor gate lead
                    sb.append(String.format(Locale.ROOT,
                            "      <polyline class=\"comp-lead\" points=\"0,%.1f %.1f,%.1f %.1f,%.1f\" />\n",
                            7.5 * s, -6.0 * s, 11.5 * s, -11.0 * s, 11.5 * s));
                }
            }
            case 10, 11 -> { // IGBT (typ 10)
                // Collector (-p) and Emitter (+p) leads scaled to match resistor/diode proportion
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -11.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 11.0 * s, p));
                // Gate stub & plate (insulated gate)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"0\" x2=\"%.1f\" y2=\"0\" />\n", -14.0 * s, -7.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.0\" />\n",
                        -7.5 * s, -9.0 * s, -7.5 * s, 9.0 * s));
                // Solid channel bar
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.4\" />\n",
                        -4.0 * s, -10.0 * s, -4.0 * s, 10.0 * s));
                // Collector diagonal & Emitter diagonal with arrow
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke-width=\"1.8\" />\n",
                        -4.0 * s, -4.5 * s, -11.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke-width=\"1.8\" />\n",
                        -4.0 * s, 4.5 * s, 11.0 * s));
                sb.append(String.format(Locale.ROOT, "      <polygon points=\"0,%.1f %.1f,%.1f %.1f,%.1f\" fill=\"var(--comp-stroke)\" />\n",
                        11.0 * s, -4.5 * s, 8.2 * s, -0.6 * s, 5.5 * s));
            }
            case 28 -> { // MOSFET (typ 28)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -11.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 11.0 * s, p));
                // Gate plate
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"0\" x2=\"%.1f\" y2=\"0\" />\n", -14.0 * s, -7.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.0\" />\n",
                        -7.5 * s, -9.0 * s, -7.5 * s, 9.0 * s));
                // 3-segment channel bar
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.0\" />\n",
                        -4.0 * s, -10.0 * s, -4.0 * s, -5.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.0\" />\n",
                        -4.0 * s, -2.5 * s, -4.0 * s, 2.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.0\" />\n",
                        -4.0 * s, 5.0 * s, -4.0 * s, 10.0 * s));
                // Drain/Source connections
                sb.append(String.format(Locale.ROOT, "      <polyline class=\"comp-lead\" points=\"%.1f,%.1f 0,%.1f 0,%.1f\" />\n",
                        -4.0 * s, -7.5 * s, -7.5 * s, -11.0 * s));
                sb.append(String.format(Locale.ROOT, "      <polyline class=\"comp-lead\" points=\"%.1f,0 0,0 0,%.1f %.1f,%.1f\" />\n",
                        -4.0 * s, 7.5 * s, -4.0 * s, 7.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 7.5 * s, 11.0 * s));
            }
            case 7 -> { // Ideal Switch (contact blade)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -8.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 8.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"%.1f\" r=\"%.1f\" />\n", -8.0 * s, 2.2 * s));
                sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"%.1f\" r=\"%.1f\" />\n", 8.0 * s, 2.2 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.2\" />\n",
                        8.0 * s, -7.5 * s, -6.5 * s));
            }
            case 31, 32 -> { // Global Terminal / Ground
                // Note: Ground terminal is at (posX, posY) -> local (0, 0)
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"%.1f\" />\n", 5.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"2.4\" />\n",
                        -10.0 * s, 5.0 * s, 10.0 * s, 5.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"1.9\" />\n",
                        -6.5 * s, 8.5 * s, 6.5 * s, 8.5 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\"1.5\" />\n",
                        -3.0 * s, 12.0 * s, 3.0 * s, 12.0 * s));
            }
            default -> { // Generic 2-terminal block
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", -p, -10.0 * s));
                sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" />\n", 10.0 * s, p));
                sb.append(String.format(Locale.ROOT, "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"3\" />\n",
                        -10.0 * s, -10.0 * s, 20.0 * s, 20.0 * s));
            }
        }
    }

    private static void renderControlBlock(StringBuilder sb, ComponentData comp, double s, double grid, boolean dark) {
        int typ = comp.getType();
        double twoGrid = 2.0 * grid;

        if (typ == 1 || typ == 2 || typ == 1001 || typ == 1002 || typ == 1003) { // Probes
            // Output pin is at +2 grid units right
            sb.append(String.format(Locale.ROOT, "      <line class=\"wire-control\" x1=\"%.1f\" y1=\"0\" x2=\"%.1f\" y2=\"0\" />\n", 12.0 * s, twoGrid));
            sb.append(String.format(Locale.ROOT, "      <circle class=\"comp-body\" cx=\"0\" cy=\"0\" r=\"%.1f\" fill=\"var(--comp-accent)\" stroke=\"var(--wire-ctrl)\" />\n", 12.0 * s));
            String badge = (typ == 1 || typ == 1001) ? "V" : ((typ == 2 || typ == 1002) ? "A" : "OSC");
            sb.append(String.format(Locale.ROOT,
                    "      <text font-size=\"10\" font-weight=\"bold\" fill=\"var(--text-main)\" text-anchor=\"middle\" x=\"0\" y=\"3.5\">%s</text>\n", badge));
        } else if (typ == 6 || typ == 1000) { // Gate driver block
            // Input pin is at -2 grid units left
            sb.append(String.format(Locale.ROOT, "      <line class=\"wire-control\" x1=\"%.1f\" y1=\"0\" x2=\"%.1f\" y2=\"0\" />\n", -twoGrid, -10.0 * s));
            sb.append(String.format(Locale.ROOT,
                    "      <polygon class=\"comp-body\" points=\"%.1f,%.1f %.1f,0 %.1f,%.1f\" fill=\"var(--comp-accent)\" stroke=\"var(--wire-ctrl)\" />\n",
                    -10.0 * s, -9.0 * s, 10.0 * s, -10.0 * s, 9.0 * s));
            sb.append("      <text font-size=\"7.5\" font-weight=\"bold\" fill=\"var(--wire-ctrl)\" text-anchor=\"middle\" x=\"-3.5\" y=\"2.5\">G</text>\n");
        } else { // Script / IC / Math block
            String[] xLabels = comp.getTerminalXLabels();
            String[] yLabels = comp.getTerminalYLabels();
            int nIn = countValidLabels(xLabels);
            int nOut = countValidLabels(yLabels);
            Object anzXin = comp.getParameters().get("anzXIN");
            Object anzYout = comp.getParameters().get("anzYOUT");
            if (anzXin instanceof Number n) nIn = Math.max(nIn, n.intValue());
            if (anzYout instanceof Number n) nOut = Math.max(nOut, n.intValue());
            int maxPins = Math.max(1, Math.max(nIn, nOut));

            double topY = -14.0 * s;
            double bottomY = (maxPins - 1) * grid + 14.0 * s;
            double boxH = bottomY - topY;
            double boxW = 48.0 * s;

            // Input leads & pin dots
            for (int i = 0; i < Math.max(1, nIn); i++) {
                double py = i * grid;
                sb.append(String.format(Locale.ROOT, "      <line class=\"wire-control\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" />\n",
                        -twoGrid, py, -boxW / 2.0, py));
                sb.append(String.format(Locale.ROOT, "      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"var(--wire-ctrl)\" />\n",
                        -boxW / 2.0, py, 2.2 * s));
            }
            // Output leads & pin dots
            for (int j = 0; j < Math.max(1, nOut); j++) {
                double py = j * grid;
                sb.append(String.format(Locale.ROOT, "      <line class=\"wire-control\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" />\n",
                        boxW / 2.0, py, twoGrid, py));
                sb.append(String.format(Locale.ROOT, "      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"var(--wire-ctrl)\" />\n",
                        boxW / 2.0, py, 2.2 * s));
            }

            sb.append(String.format(Locale.ROOT,
                    "      <rect class=\"comp-body\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"5\" fill=\"var(--comp-accent)\" stroke=\"var(--wire-ctrl)\" />\n",
                    -boxW / 2.0, topY, boxW, boxH));

            String label = comp.getName().toUpperCase(Locale.ROOT).contains("SPWM") ? "SPWM"
                    : (comp.getName().toUpperCase(Locale.ROOT).contains("PI") ? "PI / PWM" : "MCU");
            double midY = (topY + bottomY) * 0.5 + 3.5 * s;
            sb.append(String.format(Locale.ROOT,
                    "      <text font-size=\"8.5\" font-weight=\"bold\" fill=\"var(--text-main)\" text-anchor=\"middle\" x=\"0\" y=\"%.1f\">%s</text>\n",
                    midY, escapeXml(label)));
        }
    }

    private static int countValidLabels(String[] labels) {
        if (labels == null) return 0;
        int c = 0;
        for (String l : labels) {
            if (l != null && !l.isBlank() && !l.equalsIgnoreCase("NIX_NIX_NIX")) {
                c++;
            }
        }
        return c;
    }

    private static void renderThermalComponent(StringBuilder sb, ComponentData comp, double s, boolean dark) {
        int typ = comp.getType();
        double p = 2.0 * DEFAULT_GRID * s;
        if (typ == 50) { // Thermal Ground
            sb.append("      <line class=\"comp-lead\" x1=\"0\" y1=\"-15\" x2=\"0\" y2=\"0\" stroke=\"var(--wire-therm)\" />\n");
            sb.append(String.format(Locale.ROOT, "      <circle cx=\"0\" cy=\"0\" r=\"%.1f\" fill=\"var(--wire-therm)\" />\n", 4.0 * s));
            sb.append("      <text font-size=\"8\" font-weight=\"bold\" fill=\"var(--wire-therm)\" text-anchor=\"middle\" x=\"0\" y=\"12\">T_amb</text>\n");
        } else {
            sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"var(--wire-therm)\" />\n", -p, -12.0 * s));
            sb.append(String.format(Locale.ROOT, "      <line class=\"comp-lead\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"var(--wire-therm)\" />\n", 12.0 * s, p));
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
            case 2, 12, 13 -> formatEng(raw[0], "H");
            case 3 -> formatEng(raw[0], "F");
            case 4 -> {
                boolean isAc = Math.abs(raw[0] - 402.0) < 0.5;
                if (isAc && raw.length > 2) {
                    yield String.format(Locale.ROOT, "%.0fV %.0fHz", raw[1], raw[2]);
                } else if (raw.length > 1) {
                    yield String.format(Locale.ROOT, "%.0fV", raw[1]);
                }
                yield "";
            }
            case 5 -> raw.length > 1 ? String.format(Locale.ROOT, "%.2fA", raw[1]) : "";
            case 10, 11 -> {
                if (raw.length > 2 && raw[1] > 0 && raw[2] > 0) {
                    yield String.format(Locale.ROOT, "%.1fV / %s", raw[1], formatEng(raw[2], "Ω"));
                }
                yield "";
            }
            case 28 -> raw.length > 2 && raw[2] > 0 ? formatEng(raw[2], "Ω") : "";
            default -> "";
        };
    }

    private static String formatEng(double val, String unit) {
        if (Math.abs(val) < 1e-15) return "0 " + unit;
        double abs = Math.abs(val);
        if (abs >= 1e6) return trimZeros(val / 1e6) + " M" + unit;
        if (abs >= 1e3) return trimZeros(val / 1e3) + " k" + unit;
        if (abs >= 1.0) return trimZeros(val) + " " + unit;
        if (abs >= 1e-3) return trimZeros(val * 1e3) + " m" + unit;
        if (abs >= 1e-6) return trimZeros(val * 1e6) + " µ" + unit;
        if (abs >= 1e-9) return trimZeros(val * 1e9) + " n" + unit;
        return String.format(Locale.ROOT, "%.1e %s", val, unit);
    }

    private static String trimZeros(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
                break;
            }
            s = s.substring(0, s.length() - 1);
        }
        return s;
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
