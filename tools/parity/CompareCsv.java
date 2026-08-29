import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * P5 parity harness: compares two simulation result CSVs (first column = time).
 * Rows are aligned by time proximity; each common signal column must satisfy
 * |a - b| &lt;= absTol + relTol * max(|a|, |b|) at every aligned point.
 *
 * Exit code 0 = pass, 1 = mismatch, 2 = usage/IO error.
 *
 * Usage: CompareCsv &lt;reference.csv&gt; &lt;actual.csv&gt; [relTol=1e-6] [absTol=1e-9]
 */
public final class CompareCsv {

    private CompareCsv() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: CompareCsv <reference.csv> <actual.csv> [relTol=1e-6] [absTol=1e-9] [skipFirstRow]");
            System.exit(2);
        }
        double relTol = args.length > 2 ? Double.parseDouble(args[2]) : 1e-6;
        double absTol = args.length > 3 ? Double.parseDouble(args[3]) : 1e-9;
        boolean skipFirstRow = args.length > 4 && Boolean.parseBoolean(args[4]);

        Table reference = read(Path.of(args[0]));
        Table actual = read(Path.of(args[1]));
        if (skipFirstRow) {
            // the classic engine's row 0 comes from its own initialization
            // convention (switches off, no control step yet) and is not
            // comparable with the headless engine's first logged step
            reference.dropFirstRow();
            actual.dropFirstRow();
        }

        List<String> common = new ArrayList<>();
        for (String column : reference.headers.keySet()) {
            if (actual.headers.containsKey(column)) {
                common.add(column);
            }
        }
        if (common.isEmpty()) {
            System.err.println("no common columns: reference=" + reference.headers.keySet()
                    + " actual=" + actual.headers.keySet());
            System.exit(1);
        }

        List<int[]> pairs = alignByTime(reference, actual);
        if (pairs.isEmpty()) {
            System.err.println("no alignable time rows (reference t0=" + reference.time.get(0)
                    + ", actual t0=" + actual.time.get(0) + ")");
            System.exit(1);
        }

        boolean pass = true;
        System.out.printf(Locale.ROOT, "%-16s %10s %10s %12s %12s %s%n",
                "signal", "points", "maxAbs", "maxRel", "tolerance", "result");
        for (String column : common) {
            double[] refValues = reference.columns.get(reference.headers.get(column));
            double[] actValues = actual.columns.get(actual.headers.get(column));
            double maxAbs = 0;
            double maxRel = 0;
            int failures = 0;
            for (int[] pair : pairs) {
                double a = refValues[pair[0]];
                double b = actValues[pair[1]];
                double abs = Math.abs(a - b);
                double rel = abs / Math.max(Math.max(Math.abs(a), Math.abs(b)), 1e-30);
                maxAbs = Math.max(maxAbs, abs);
                maxRel = Math.max(maxRel, rel);
                if (abs > absTol + relTol * Math.max(Math.abs(a), Math.abs(b))) {
                    failures++;
                }
            }
            boolean signalPass = failures == 0;
            pass &= signalPass;
            System.out.printf(Locale.ROOT, "%-16s %10d %10.3e %12.3e %6.1e/%4.1e %s%s%n",
                    column, pairs.size(), maxAbs, maxRel, relTol, absTol,
                    signalPass ? "PASS" : "FAIL",
                    signalPass ? "" : " (" + failures + " points outside tolerance)");
        }
        System.out.println(pass ? "PARITY: PASS" : "PARITY: FAIL");
        System.exit(pass ? 0 : 1);
    }

    private static final class Table {
        Map<String, Integer> headers = new LinkedHashMap<>();
        List<Double> time = new ArrayList<>();
        Map<Integer, double[]> columns = new LinkedHashMap<>();

        void dropFirstRow() {
            if (time.isEmpty()) {
                return;
            }
            time.remove(0);
            for (Map.Entry<Integer, double[]> entry : columns.entrySet()) {
                double[] series = entry.getValue();
                double[] shifted = new double[series.length - 1];
                System.arraycopy(series, 1, shifted, 0, shifted.length);
                entry.setValue(shifted);
            }
        }
    }

    private static Table read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("empty CSV: " + file);
        }
        Table table = new Table();
        String[] header = lines.get(0).split(",");
        for (int i = 1; i < header.length; i++) {
            table.headers.put(header[i].trim(), i - 1);
        }
        int columns = header.length - 1;
        List<double[]> data = new ArrayList<>();
        for (int line = 1; line < lines.size(); line++) {
            if (lines.get(line).isBlank()) {
                continue;
            }
            String[] cells = lines.get(line).split(",");
            table.time.add(Double.parseDouble(cells[0]));
            double[] row = new double[columns];
            for (int i = 0; i < columns; i++) {
                row[i] = cells.length > i + 1 && !cells[i + 1].isBlank()
                        ? Double.parseDouble(cells[i + 1]) : Double.NaN;
            }
            data.add(row);
        }
        for (int c = 0; c < columns; c++) {
            double[] series = new double[data.size()];
            for (int r = 0; r < data.size(); r++) {
                series[r] = data.get(r)[c];
            }
            table.columns.put(c, series);
        }
        return table;
    }

    /** Pairs rows whose time values agree within 1% of the median step. */
    private static List<int[]> alignByTime(Table reference, Table actual) {
        double dt = medianStep(reference.time);
        double tolerance = dt * 0.01 + 1e-15;
        List<int[]> pairs = new ArrayList<>();
        int a = 0;
        int b = 0;
        while (a < reference.time.size() && b < actual.time.size()) {
            double ta = reference.time.get(a);
            double tb = actual.time.get(b);
            if (Math.abs(ta - tb) <= tolerance) {
                pairs.add(new int[] {a, b});
                a++;
                b++;
            } else if (tb < ta) {
                b++;
            } else {
                a++;
            }
        }
        return pairs;
    }

    private static double medianStep(List<Double> time) {
        if (time.size() < 2) {
            return 1.0;
        }
        List<Double> steps = new ArrayList<>();
        for (int i = 1; i < time.size(); i++) {
            steps.add(time.get(i) - time.get(i - 1));
        }
        steps.sort(Double::compare);
        return steps.get(steps.size() / 2);
    }
}
