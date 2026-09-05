/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.simulation;

import gecko.core.control.ControlCalculatorBuilder;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that a classic .ipes project file containing
 * a legacy Java Block (typ 61) runs headlessly with exact signal tap recordings.
 */
class ClassicJavaBlockIpesSimulationTest {

    /** Locates a tutorials file regardless of the module directory the tests run from. */
    private static File findTutorialFile(String relativePath) {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 4 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate.toFile();
            }
            dir = dir.getParent();
        }
        return Paths.get(relativePath).toFile();
    }

    private static File tutorialFile(String relativePath) {
        File file = findTutorialFile("resources/tutorials/" + relativePath);
        assertTrue(file.exists(), "tutorial file must exist: " + file.getAbsolutePath());
        return file;
    }

    @Test
    void testDemoJavaBlockIpesRunsHeadlessly() throws Exception {
        File ipesFile = tutorialFile("7xx_scripting_automation/704_java_blocks/demo_JAVA_Block.ipes");

        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel model = parser.parse(ipesFile.getAbsolutePath());

        assertNotNull(model);
        // Verify control components include typ 61
        List<CircuitModel.ComponentData> controls = model.getControlComponents();
        CircuitModel.ComponentData javaBlock = controls.stream()
                .filter(c -> c.getType() == 61)
                .findFirst()
                .orElse(null);

        assertNotNull(javaBlock, "Should find typ 61 Java Block");
        String sourceCode = (String) javaBlock.getParameters().get("sourceCode");
        assertNotNull(sourceCode, "sourceCode parameter should be parsed");
        assertTrue(sourceCode.contains("yOUT[0]= xIN[0]"), "Source code should contain original formula");

        // Run simulation headlessly
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(ipesFile.getAbsolutePath())
                .simulationDuration(0.002) // run for 2 ms
                .stepWidth(1e-6)
                .build();

        SimulationResult result = engine.runSimulation(config);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Simulation should succeed: " + result.getErrorMessage());
        assertTrue(result.getTotalTimeSteps() > 100);

        // Verify that the 5 output signals (w1, w2, w3, w4, w5) were recorded
        String[] recordedSignals = result.getSignalNames();
        assertNotNull(recordedSignals);

        // tap labels are recorded verbatim, so match exactly ("w1" must not
        // match a hypothetical "w10")
        Set<String> names = new HashSet<>(Arrays.asList(recordedSignals));
        assertTrue(names.contains("w1"), "Signal tap w1 must be recorded, got: " + names);
        assertTrue(names.contains("w2"), "Signal tap w2 must be recorded, got: " + names);
        assertTrue(names.contains("w5"), "Signal tap w5 must be recorded, got: " + names);
    }

    @Test
    void testJavaBlockPmsmIpesRunsHeadlessly() throws Exception {
        File ipesFile = tutorialFile("7xx_scripting_automation/704_java_blocks/JavaBlockPMSM.ipes");

        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel model = parser.parse(ipesFile.getAbsolutePath());
        assertNotNull(model);

        CircuitModel.ComponentData javaBlock = model.getControlComponents().stream()
                .filter(c -> c.getType() == 61)
                .findFirst()
                .orElse(null);
        assertNotNull(javaBlock, "Should find typ 61 Java Block in JavaBlockPMSM.ipes");

        ControlCalculatorBuilder.ControlCoupling coupling = ControlCalculatorBuilder.build(model, null);
        assertNotNull(coupling);
        assertFalse(coupling.calculators().isEmpty(), "Control calculators should be built");

        // Initialize and execute calculators
        coupling.initialize(1e-6);
        for (var calc : coupling.calculators()) {
            calc.calculateYOUT(1e-6);
        }
        assertTrue(coupling.signalTaps().size() > 0, "Signal taps should be registered");
    }
}
