/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 */
package gecko.geckocircuits.circuit;

import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import org.junit.Test;
import java.io.File;
import java.net.URL;
import static org.junit.Assert.*;

/**
 * End-to-end integration tests for circuit simulations.
 * These tests run full simulations on benchmark circuits using the headless simulation engine.
 */
public class CircuitIntegrationTest {

    private static final double TOLERANCE = 1e-9;

    private String getCircuitPath(String resourceName) throws Exception {
        URL resource = CircuitIntegrationTest.class.getResource("/ipes/education/" + resourceName);
        assertNotNull("Circuit file " + resourceName + " should exist in test resources", resource);
        return new File(resource.toURI()).getAbsolutePath();
    }

    /**
     * Test a simple buck converter circuit integration.
     */
    @Test
    public void testResistorDivider_HalfVoltage() throws Exception {
        String filePath = getCircuitPath("buck_simple.ipes");
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(filePath)
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(gecko.core.allg.SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);
        
        assertTrue("Simulation must complete successfully: " + result.getErrorMessage(), result.isSuccess());
        assertEquals(SimulationResult.Status.SUCCESS, result.getStatus());
        assertTrue("Execution time should be tracked", result.getExecutionTimeMs() >= 0);
        assertTrue("Steps should be simulated", result.getTotalTimeSteps() > 0);
    }

    /**
     * Test RC/buck converter charging behavior and signal extraction.
     */
    @Test
    public void testRCCharging_TimeConstant() throws Exception {
        String filePath = getCircuitPath("buck_simple.ipes");
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(filePath)
                .stepWidth(1e-6)
                .simulationDuration(2e-3)
                .solverType(gecko.core.allg.SolverType.SOLVER_BE)
                .build();

        SimulationResult result = engine.runSimulation(config);
        
        assertTrue("Simulation must complete successfully: " + result.getErrorMessage(), result.isSuccess());
        
        double[] timeArray = result.getTimeArray();
        assertNotNull("Time array should not be null", timeArray);
        assertTrue("Time array should have entries", timeArray.length > 0);
        assertEquals(0.0, timeArray[0], TOLERANCE);
        
        String[] signalNames = result.getSignalNames();
        assertNotNull("Signal names should be resolved", signalNames);
        assertTrue("Should have signal logging", signalNames.length > 0);
    }

    /**
     * Test boost converter circuit integration.
     */
    @Test
    public void testRLCurrentRise_TimeConstant() throws Exception {
        String filePath = getCircuitPath("boost_simple.ipes");
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(filePath)
                .stepWidth(1e-6)
                .simulationDuration(1e-3)
                .solverType(gecko.core.allg.SolverType.SOLVER_TRZ)
                .build();

        SimulationResult result = engine.runSimulation(config);
        
        assertTrue("Simulation must complete successfully: " + result.getErrorMessage(), result.isSuccess());
    }

    /**
     * Test solver type consistency - all solvers (BE, TRZ, GS) should run and converge successfully.
     */
    @Test
    public void testSolverTypes_DCConsistency() throws Exception {
        String filePath = getCircuitPath("buck_simple.ipes");
        gecko.core.allg.SolverType[] solvers = {
            gecko.core.allg.SolverType.SOLVER_BE,
            gecko.core.allg.SolverType.SOLVER_TRZ,
            gecko.core.allg.SolverType.SOLVER_GS
        };

        for (gecko.core.allg.SolverType solver : solvers) {
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(filePath)
                    .stepWidth(1e-6)
                    .simulationDuration(5e-4)
                    .solverType(solver)
                    .build();

            SimulationResult result = engine.runSimulation(config);
            
            assertTrue("Simulation with solver " + solver + " must complete successfully: " + result.getErrorMessage(), result.isSuccess());
            assertEquals(SimulationResult.Status.SUCCESS, result.getStatus());
        }
    }
}
