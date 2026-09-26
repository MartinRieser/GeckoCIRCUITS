/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control;

import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.ConstantCalculator;
import gecko.core.control.calculators.ScriptBlockCalculator;
import gecko.core.control.calculators.ScriptDebugContext;
import gecko.core.control.calculators.ScriptDebugHook;
import gecko.core.control.calculators.ScriptDebugSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the script debug support: statement line numbers refer to the
 * original source (normalization preserves the line structure), the debug
 * hook sees the live variables, and snapshots expose the watch state.
 */
class ScriptBlockDebugTest {

    /** Records the hook calls of one execution. */
    private static final class RecordingHook implements ScriptDebugHook {
        final List<Integer> lines = new ArrayList<>();
        final List<Map<String, Double>> variableSnapshots = new ArrayList<>();
        final List<double[]> inputSnapshots = new ArrayList<>();
        final List<Double> times = new ArrayList<>();

        @Override
        public void beforeStatement(ScriptDebugContext context, int line) {
            lines.add(line);
            variableSnapshots.add(new HashMap<>(context.userVariables()));
            inputSnapshots.add(context.inputSignals().clone());
            times.add(context.timeSeconds());
        }
    }

    @Test
    void testStatementLinesReferToOriginalSource() throws Exception {
        String code = """
                // leading comment
                yOUT[0] = u1;

                /* multi
                   line comment */
                acc = acc + 1;
                if (u1 > 0) {
                    yOUT[1] = acc;
                }
                """;
        ScriptBlockCalculator calc = new ScriptBlockCalculator(1, 2, code);
        calc.setBlockName("CTRL_SCRIPT.1");
        RecordingHook hook = new RecordingHook();
        calc.setDebugHook(hook);
        calc.setInputSignal(0, new ConstantCalculator(5.0), 0);

        calc.calculateYOUT(1e-6);

        // Statements live on source lines 2, 6, 7 (if) and 8 (then branch);
        // comment/blank/brace lines never report
        assertEquals(List.of(2, 6, 7, 8), hook.lines);
        assertTrue(calc.isCompiled(), "script should compile");
    }

    @Test
    void testHookSeesLiveVariablesAndTime() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(1, 1, """
                k = u1 * 2;
                yOUT[0] = k;
                """);
        RecordingHook hook = new RecordingHook();
        calc.setDebugHook(hook);
        calc.setInputSignal(0, new ConstantCalculator(10.0), 0);
        AbstractControlCalculatable.setTime(0.003);

        calc.calculateYOUT(1e-6);

        // Before the first statement, k does not exist yet (reads default to 0);
        // before the second statement it carries the first assignment's value
        assertEquals(2, hook.variableSnapshots.size());
        assertNull(hook.variableSnapshots.get(0).get("k"));
        assertEquals(20.0, hook.variableSnapshots.get(1).get("k"), 1e-12);
        assertEquals(10.0, hook.inputSnapshots.get(0)[0], 1e-12);
        assertEquals(0.003, hook.times.get(0), 1e-12);
    }

    @Test
    void testSnapshotExposesStateVariablesForWatch() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(1, 1, """
                counter = counter + u1;
                yOUT[0] = counter;
                """);
        calc.setBlockName("CTRL_SCRIPT.1");
        calc.setInputSignal(0, new ConstantCalculator(2.0), 0);

        calc.calculateYOUT(1e-6);
        calc.calculateYOUT(1e-6);

        ScriptDebugSnapshot snapshot = calc.snapshot();
        assertEquals("CTRL_SCRIPT.1", snapshot.blockName());
        assertEquals(-1, snapshot.line());
        assertEquals(4.0, snapshot.variables().get("counter"), 1e-12);
        assertEquals(4.0, snapshot.outputs()[0], 1e-12);
        assertEquals(1e-6, snapshot.dt(), 1e-15);
    }

    @Test
    void testNormalizeCodePreservesLineStructure() {
        String code = "/* a\nb\nc */ y = 1;\n// tail\nx = y + 1;";
        String normalized = ScriptBlockCalculator.normalizeCode(code);

        assertEquals(code.lines().count(), normalized.lines().count(),
                "normalization must keep the line count for breakpoint anchors");
        assertTrue(normalized.contains("y = 1;"));
        assertTrue(normalized.contains("x = y + 1;"));
    }

    @Test
    void testCompoundAssignmentDesugarKeepsLine() {
        String code = "acc +=\n1;\nyOUT[0] = acc;";
        String normalized = ScriptBlockCalculator.normalizeCode(code);

        assertEquals(3, normalized.lines().count());
    }

    @Test
    void testDebugHookWithoutHookRunsNormally() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(1, 1, "yOUT[0] = u1 + 1;");
        calc.setInputSignal(0, new ConstantCalculator(1.0), 0);

        calc.calculateYOUT(1e-6);
        assertEquals(2.0, calc._outputSignal[0][0], 1e-12);
    }
}
