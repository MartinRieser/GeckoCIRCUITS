/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.systemtests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gecko.GeckoExternal;
import gecko.GeckoSim;

/**
 * Integration tests for real circuit models.
 * Tests verify that example circuit files can be loaded and simulated successfully.
 *
 * <p>Circuit files are loaded from the classpath ({@code /ipes/...}) so the test
 * does not depend on the working directory it is launched from.
 */
public final class ModelResultsTest{
  private static final String IPES_RESOURCE_DIR = "/ipes/";

  private Path tempDir;

  @BeforeClass
  public static void setUpClass(){
    GeckoSim._isTestingMode = true;
    GeckoSim.main(new String[]{});
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  @Before
  public void setUp(){
    GeckoSim._testSuccessful = false;
    try {
      tempDir = Files.createTempDirectory("gecko-model-");
    } catch (IOException ex) {
      fail("Could not create temp dir: " + ex.getMessage());
    }
  }

  @After
  public final void tearDown(){
    if (tempDir != null) {
      try {
        Files.walk(tempDir)
             .sorted((a, b) -> b.compareTo(a))
             .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
      } catch (IOException ignored) { }
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  public void threePhaseVSRTest(){
    openRunAssert("ThreePhase-VSR_10kW_thermal.ipes");
  }

  @Test
  public void buckBoostThermal(){
    openRunAssert("BuckBoost_thermal.ipes");
  }

  @Test
  public void thyristorControlAndParameters(){
    openRunAssert("ThyristorControlBlock.ipes");
  }

  @Test
  public void opAmp(){
    openRunAssert("OpAmp.ipes");
  }

  @Test
  public void thyristorCoupling(){
    openRunAssert("ThyristorCoupling.ipes");
  }

  /**
   * Extracts the named .ipes resource to a temp file, opens it via
   * GeckoExternal, runs the simulation, and asserts basic success criteria.
   */
  public void openRunAssert(String fileName){
    try{
      Thread.sleep(10);
      Path target = tempDir.resolve(fileName);
      try (InputStream is = getClass().getResourceAsStream(IPES_RESOURCE_DIR + fileName)) {
        assertNotNull("Resource not found on classpath: " + IPES_RESOURCE_DIR + fileName, is);
        Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
      }

      GeckoExternal.openFile(target.toAbsolutePath().toString());
      GeckoExternal.runSimulation();

      double simTime = GeckoExternal.getSimulationTime();
      assertTrue("Simulation time should be positive for " + fileName, simTime > 0);

      String[] circuitElements = GeckoExternal.getCircuitElements();
      assertNotNull("Circuit elements should not be null for " + fileName, circuitElements);
      assertTrue("Circuit should have elements for " + fileName,
                 circuitElements.length > 0);

      System.out.println("Successfully simulated: " + fileName);
    }catch(InterruptedException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("Test interrupted: " + ex.getMessage());
    }catch(RemoteException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("Remote exception: " + ex.getMessage());
    }catch(IOException ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("IO exception: " + ex.getMessage());
    }catch(Exception ex){
      Logger.getLogger(ModelResultsTest.class.getName()).log(Level.SEVERE, null, ex);
      fail("Unexpected exception: " + ex.getMessage());
    }
  }
}
