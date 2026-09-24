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
package gecko.core.io;

import gecko.core.allg.SolverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and round-trip tests for {@link CircuitFileWriter}.
 */
class CircuitFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void write_nullModel_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CircuitFileWriter.write(null));
        assertThrows(IllegalArgumentException.class, () -> CircuitFileWriter.writeToString(null));
        assertThrows(IllegalArgumentException.class, () -> CircuitFileWriter.write(null, tempDir.resolve("test.ipes")));
    }

    @Test
    void write_nullPath_throwsIllegalArgumentException() {
        CircuitModel model = new CircuitModel();
        assertThrows(IllegalArgumentException.class, () -> CircuitFileWriter.write(model, null));
    }

    @Test
    void roundTrip_programmaticModel_preservesAllFields() throws Exception {
        CircuitModel model = new CircuitModel();
        model.setSimulationDuration(0.05);
        model.setTimeStep(2e-6);
        model.setPauseTime(0.01);
        model.setPreSimulationTime(0.002);
        model.setPreSimulationTimeStep(1e-7);
        model.setSolverType(SolverType.SOLVER_TRZ);
        model.setDisplayPixels(24);
        model.setFontSize(14);
        model.setFontType("Helvetica");
        model.setWindowWidth(1024);
        model.setWindowHeight(768);
        model.setWorksheetSize("800x600");
        model.setFileVersion(175);
        model.setUniqueFileId(98765);
        model.setCreationDate("2026-08-15");
        model.setPath("C:/circuits/test.ipes");

        // Add optimizer parameters
        model.setOptimizerParameter("R_load", 15.5);
        model.setOptimizerParameter("C_filter", 1e-5);
        model.setOptimizerNames(List.of("R_load", "C_filter"));
        model.setOptimizerValues(List.of(15.5, 1e-5));

        // Add scripting
        model.setScripterCode("double val = 42.0;");
        model.setScripterImports("import java.util.*;");
        model.setScripterDeclarations("int counter = 0;");
        model.setScripterExtraFiles("hash123");

        // Add signals
        model.setDataContainerSignals(new String[]{"V_out", "I_in", "P_loss"});

        // Add circuit components
        CircuitModel.ComponentData r1 = new CircuitModel.ComponentData(1, "R1", 10, 20, 1);
        r1.setRawParameters(new double[]{100.0, 0.0, 0.0});
        r1.setParameter("resistance", 100.0);
        r1.setTerminalXLabels(new String[]{"nodeA"});
        r1.setTerminalYLabels(new String[]{"nodeB"});
        r1.setUniqueObjectIdentifier(1001L);
        model.addCircuitComponent(r1);

        CircuitModel.ComponentData c1 = new CircuitModel.ComponentData(3, "C1", 30, 40, 0);
        c1.setRawParameters(new double[]{1e-6, 0.0});
        c1.setParameter("capacitance", 1e-6);
        c1.setTerminalXLabels(new String[]{"nodeB"});
        c1.setTerminalYLabels(new String[]{"GND"});
        c1.setUniqueObjectIdentifier(1002L);
        model.addCircuitComponent(c1);

        // Add control component
        CircuitModel.ComponentData ctrl = new CircuitModel.ComponentData(101, "Gain1", 50, 60, 0);
        ctrl.setRawParameters(new double[]{2.5});
        ctrl.setUniqueObjectIdentifier(2001L);
        model.addControlComponent(ctrl);

        // Add connections
        CircuitModel.ConnectionData conn1 = new CircuitModel.ConnectionData("LK", new int[][]{{10, 20}, {30, 20}, {30, 40}});
        conn1.setLabel("wire1");
        conn1.setConnectorType(0);
        conn1.setUniqueObjectIdentifier(3001L);
        model.addConnection(conn1);

        // Serialize to gzipped bytes
        byte[] gzippedBytes = CircuitFileWriter.write(model, true);
        assertNotNull(gzippedBytes);
        assertTrue(gzippedBytes.length > 0);

        // Parse back
        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel parsedModel = parser.parse(new ByteArrayInputStream(gzippedBytes), "programmatic.ipes");

        // Assert semantic equality
        assertCircuitModelsEqual(model, parsedModel);
    }

    @Test
    void roundTrip_writeToFile_producesValidGzipFile() throws Exception {
        CircuitModel model = new CircuitModel();
        model.setSimulationDuration(0.01);
        model.setTimeStep(1e-6);
        model.setSolverType(SolverType.SOLVER_BE);

        CircuitModel.ComponentData r = new CircuitModel.ComponentData(1, "R1", 5, 5, 0);
        r.setRawParameters(new double[]{50.0});
        model.addCircuitComponent(r);

        Path targetFile = tempDir.resolve("subfolder/output.ipes");
        CircuitFileWriter.write(model, targetFile);

        assertTrue(Files.exists(targetFile));
        assertTrue(Files.size(targetFile) > 0);

        CircuitModel parsedModel = new CircuitFileParser().parse(targetFile.toString());
        assertEquals(0.01, parsedModel.getSimulationDuration(), 1e-12);
        assertEquals(1e-6, parsedModel.getTimeStep(), 1e-15);
        assertEquals(1, parsedModel.getCircuitComponents().size());
        assertEquals("R1", parsedModel.getCircuitComponents().get(0).getName());
    }

    @Test
    void roundTrip_uncompressedAscii() throws Exception {
        CircuitModel model = new CircuitModel();
        model.setSimulationDuration(0.02);
        model.setTimeStep(1e-6);

        CircuitModel.ComponentData l = new CircuitModel.ComponentData(2, "L1", 10, 10, 2);
        l.setRawParameters(new double[]{0.01});
        model.addCircuitComponent(l);

        String ascii = CircuitFileWriter.writeToString(model);
        assertTrue(ascii.contains("tDURATION 0.02"));
        assertTrue(ascii.contains("dt 1.0E-6"));
        assertTrue(ascii.contains("idStringDialog L1"));
        assertTrue(ascii.contains("<\\ElementLK>"));

        byte[] uncompressedBytes = CircuitFileWriter.write(model, false);
        CircuitModel parsedModel = new CircuitFileParser().parse(new ByteArrayInputStream(uncompressedBytes), "uncompressed.ipes");

        assertEquals(0.02, parsedModel.getSimulationDuration(), 1e-12);
        assertEquals(1, parsedModel.getCircuitComponents().size());
        assertEquals("L1", parsedModel.getCircuitComponents().get(0).getName());
    }

    @Test
    void roundTrip_fixture_ex1() throws Exception {
        verifyFixtureRoundTrip("/ipes/ex_1.ipes");
    }

    @Test
    void roundTrip_fixture_ex3Pwm() throws Exception {
        verifyFixtureRoundTrip("/ipes/ex_3_pwm.ipes");
    }

    @Test
    void roundTrip_fixture_testCircuit() throws Exception {
        verifyFixtureRoundTrip("/ipes/test-circuit.ipes");
    }

    @Test
    void roundTrip_fixture_opAmp() throws Exception {
        verifyFixtureRoundTrip("/ipes/OpAmp.ipes");
    }

    @Test
    void roundTrip_fixture_buckBoostThermal() throws Exception {
        verifyFixtureRoundTrip("/ipes/BuckBoost_thermal.ipes");
    }

    @Test
    void roundTrip_fixture_buckSimple() throws Exception {
        verifyFixtureRoundTrip("/ipes/buck_simple.ipes");
    }

    @Test
    void roundTrip_parameterMutation_persistsModifiedValues() throws Exception {
        InputStream is = getClass().getResourceAsStream("/ipes/ex_1.ipes");
        assertNotNull(is);
        CircuitModel model = new CircuitFileParser().parse(is, "ex_1.ipes");
        assertFalse(model.getCircuitComponents().isEmpty());

        CircuitModel.ComponentData comp = model.getCircuitComponents().get(0);
        comp.setParameter("param0", 0.05);

        // Serialize and parse back
        byte[] bytes = CircuitFileWriter.write(model);
        CircuitModel roundTripped = new CircuitFileParser().parse(new ByteArrayInputStream(bytes), "ex_1_mutated.ipes");

        CircuitModel.ComponentData roundTrippedComp = roundTripped.getCircuitComponents().get(0);
        assertEquals(0.05, roundTrippedComp.getRawParameters()[0], 1e-9);
    }

    private void verifyFixtureRoundTrip(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        assertNotNull(is, "Fixture resource not found: " + resourcePath);

        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel originalModel = parser.parse(is, resourcePath);

        // Serialize original model with CircuitFileWriter
        byte[] serializedBytes = CircuitFileWriter.write(originalModel);

        // Parse serialized model back
        CircuitModel roundTrippedModel = parser.parse(new ByteArrayInputStream(serializedBytes), resourcePath);

        // Compare models
        assertCircuitModelsEqual(originalModel, roundTrippedModel);
    }

    /**
     * Asserts that two {@link CircuitModel} instances are semantically equal.
     */
    static void assertCircuitModelsEqual(CircuitModel expected, CircuitModel actual) {
        assertEquals(expected.getSimulationDuration(), actual.getSimulationDuration(), 1e-12, "Simulation duration mismatch");
        assertEquals(expected.getTimeStep(), actual.getTimeStep(), 1e-15, "Time step mismatch");
        assertEquals(expected.getPauseTime(), actual.getPauseTime(), 1e-12, "Pause time mismatch");
        assertEquals(expected.getPreSimulationTime(), actual.getPreSimulationTime(), 1e-12, "Pre-simulation time mismatch");
        assertEquals(expected.getPreSimulationTimeStep(), actual.getPreSimulationTimeStep(), 1e-15, "Pre-simulation time step mismatch");
        assertEquals(expected.getSolverType(), actual.getSolverType(), "Solver type mismatch");

        assertEquals(expected.getDisplayPixels(), actual.getDisplayPixels(), "Display pixels mismatch");
        assertEquals(expected.getFontSize(), actual.getFontSize(), "Font size mismatch");
        assertEquals(expected.getFontType(), actual.getFontType(), "Font type mismatch");
        assertEquals(expected.getWindowWidth(), actual.getWindowWidth(), "Window width mismatch");
        assertEquals(expected.getWindowHeight(), actual.getWindowHeight(), "Window height mismatch");

        assertEquals(expected.getFileVersion(), actual.getFileVersion(), "File version mismatch");
        assertEquals(expected.getUniqueFileId(), actual.getUniqueFileId(), "Unique file ID mismatch");
        assertEquals(expected.getCreationDate(), actual.getCreationDate(), "Creation date mismatch");
        assertEquals(expected.getPath(), actual.getPath(), "Path mismatch");

        assertEquals(expected.getScripterCode().trim(), actual.getScripterCode().trim(), "Scripter code mismatch");
        assertEquals(expected.getScripterImports().trim(), actual.getScripterImports().trim(), "Scripter imports mismatch");
        assertEquals(expected.getScripterDeclarations().trim(), actual.getScripterDeclarations().trim(), "Scripter declarations mismatch");

        assertArrayEquals(expected.getDataContainerSignals(), actual.getDataContainerSignals(), "Data container signals mismatch");

        assertEquals(expected.getOptimizerNames(), actual.getOptimizerNames(), "Optimizer names mismatch");
        assertEquals(expected.getOptimizerValues(), actual.getOptimizerValues(), "Optimizer values mismatch");

        // Verify Circuit Components
        assertComponentsListEqual("CircuitComponents", expected.getCircuitComponents(), actual.getCircuitComponents());

        // Verify Control Components
        assertComponentsListEqual("ControlComponents", expected.getControlComponents(), actual.getControlComponents());

        // Verify Thermal Components
        assertComponentsListEqual("ThermalComponents", expected.getThermalComponents(), actual.getThermalComponents());

        // Verify Connections
        assertEquals(expected.getConnections().size(), actual.getConnections().size(), "Connections count mismatch");
        for (int i = 0; i < expected.getConnections().size(); i++) {
            CircuitModel.ConnectionData expConn = expected.getConnections().get(i);
            CircuitModel.ConnectionData actConn = actual.getConnections().get(i);
            assertEquals(expConn.getType(), actConn.getType(), "Connection[" + i + "] type mismatch");
            assertEquals(expConn.getLabel(), actConn.getLabel(), "Connection[" + i + "] label mismatch");
            assertEquals(expConn.getConnectorType(), actConn.getConnectorType(), "Connection[" + i + "] connectorType mismatch");
            assertEquals(expConn.getUniqueObjectIdentifier(), actConn.getUniqueObjectIdentifier(), "Connection[" + i + "] uniqueObjectIdentifier mismatch");
            assertEquals(expConn.getEnabledShorted(), actConn.getEnabledShorted(), "Connection[" + i + "] enabledShorted mismatch");
            assertEquals(expConn.getParentSheetIdentifier(), actConn.getParentSheetIdentifier(), "Connection[" + i + "] parentSheetIdentifier mismatch");
            assertArrayEquals(expConn.getPoints(), actConn.getPoints(), "Connection[" + i + "] points mismatch");
        }
    }

    private static void assertComponentsListEqual(String label,
                                                 List<CircuitModel.ComponentData> expected,
                                                 List<CircuitModel.ComponentData> actual) {
        assertEquals(expected.size(), actual.size(), label + " count mismatch");
        for (int i = 0; i < expected.size(); i++) {
            CircuitModel.ComponentData exp = expected.get(i);
            CircuitModel.ComponentData act = actual.get(i);

            assertEquals(exp.getType(), act.getType(), label + "[" + i + "] type mismatch");
            assertEquals(exp.getName(), act.getName(), label + "[" + i + "] name mismatch");
            assertArrayEquals(exp.getPosition(), act.getPosition(), label + "[" + i + "] position mismatch");
            assertEquals(exp.getOrientation(), act.getOrientation(), label + "[" + i + "] orientation mismatch");
            assertArrayEquals(exp.getTerminalXLabels(), act.getTerminalXLabels(), label + "[" + i + "] terminalXLabels mismatch");
            assertArrayEquals(exp.getTerminalYLabels(), act.getTerminalYLabels(), label + "[" + i + "] terminalYLabels mismatch");
            assertEquals(exp.getUniqueObjectIdentifier(), act.getUniqueObjectIdentifier(), label + "[" + i + "] uniqueObjectIdentifier mismatch");
            assertEquals(exp.getEnabledShorted(), act.getEnabledShorted(), label + "[" + i + "] enabledShorted mismatch");
            assertEquals(exp.getParentSheetIdentifier(), act.getParentSheetIdentifier(), label + "[" + i + "] parentSheetIdentifier mismatch");
            assertArrayEquals(exp.getParameterStrings(), act.getParameterStrings(), label + "[" + i + "] parameterStrings mismatch");
            assertArrayEquals(exp.getNameOpt(), act.getNameOpt(), label + "[" + i + "] nameOpt mismatch");
            assertEquals(exp.getExtraLines(), act.getExtraLines(), label + "[" + i + "] extraLines mismatch");

            double[] expParams = exp.getRawParameters();
            double[] actParams = act.getRawParameters();
            assertEquals(expParams.length, actParams.length, label + "[" + i + "] rawParameters length mismatch");
            for (int p = 0; p < expParams.length; p++) {
                if (Double.isNaN(expParams[p])) {
                    assertTrue(Double.isNaN(actParams[p]), label + "[" + i + "] param[" + p + "] expected NaN");
                } else {
                    assertEquals(expParams[p], actParams[p], 1e-9, label + "[" + i + "] param[" + p + "] mismatch");
                }
            }
        }
    }
}
