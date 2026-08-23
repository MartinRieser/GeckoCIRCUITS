package gecko.core.simulation;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassicCompatibilityTest {

    private static final String GUI_TEST_IPES_DIR = "../gecko-gui/src/test/resources/ipes";

    private File resolveCircuit(String relativePath) {
        Path path = Paths.get(GUI_TEST_IPES_DIR, relativePath);
        File file = path.toFile();
        if (!file.exists()) {
            // fallback if running from root
            path = Paths.get("src/modules/gecko-gui/src/test/resources/ipes", relativePath);
            file = path.toFile();
        }
        assertTrue(file.exists(), "Circuit file not found: " + path.toAbsolutePath());
        return file;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "BuckBoost_thermal.ipes",
            "OpAmp.ipes",
            "ThreePhase-VSR_10kW_thermal.ipes",
            "ThyristorControlBlock.ipes",
            "ThyristorCoupling.ipes",
            "education/2phaseDiodeBridge_AC-Inductor.ipes",
            "education/2phaseDiodeBridge_DC-Inductor.ipes",
            "education/boostPFC.ipes",
            "education/boostPFC_currentControl.ipes",
            "education/boost_simple.ipes",
            "education/buckBoost_simple.ipes",
            "education/buck_simple.ipes",
            "education/cuk_simple.ipes",
            "education/diode_RL_singlePH_trafo.ipes",
            "education/sepic_simple.ipes",
            "education/singlePhase_PWM_converter.ipes",
            "education/thyristor_Jakopovic.ipes",
            "education/thyristor_RL_2phBridge.ipes",
            "education/thyristor_RL_3phBridge.ipes",
            "education/thyristor_RL_3ph_trafo.ipes",
            "education/thyristor_RL_single.ipes",
            "education/thyristor_RL_singlePh_trafo.ipes",
            "education/thyristor_commutation_3ph_trafo.ipes",
            "education/thyristor_freeWheelingDiode.ipes",
            "education/thyristor_interface_trafo.ipes",
            "education/thyristor_lossOfCommutation.ipes"
    })
    void testClassicCircuitSimulation(String circuitPath) throws Exception {
        File file = resolveCircuit(circuitPath);
        CircuitModel model = new CircuitFileParser().parse(file.getAbsolutePath());

        double dt = model.getTimeStep() > 0 ? model.getTimeStep() : 1e-6;
        double duration = model.getSimulationDuration() > 0 ? Math.min(model.getSimulationDuration(), 0.005) : 0.005;

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(dt)
                .simulationDuration(duration)
                .build();

        SimulationResult result = engine.runSimulation(config);
        if (!result.isSuccess()) {
            System.err.println("FAILED " + circuitPath + ": " + result.getErrorMessage());
        }
        assertTrue(result.isSuccess(), "Simulation failed for " + circuitPath + ": " + result.getErrorMessage());
    }
}
