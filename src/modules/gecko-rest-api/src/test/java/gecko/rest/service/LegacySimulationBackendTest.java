package gecko.rest.service;

import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacySimulationBackendTest {

    private static CircuitModel modelWithControl(CircuitModel.ComponentData... comps) {
        CircuitModel model = new CircuitModel();
        for (CircuitModel.ComponentData comp : comps) {
            model.getControlComponents().add(comp);
        }
        return model;
    }

    private static CircuitModel.ComponentData block(String name, String outputLabel) {
        CircuitModel.ComponentData comp = new CircuitModel.ComponentData(1, name, 0, 0, 0);
        comp.setParameterStrings(new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX", "0"});
        comp.setTerminalYLabels(outputLabel == null ? new String[0] : new String[]{outputLabel});
        return comp;
    }

    @Test
    void exportNames_preferRequestedSignals() {
        CircuitModel model = modelWithControl(block("VOLT.1", "u1"));
        List<String> names = LegacySimulationBackend.exportSignalNames(model, List.of("u2"));
        assertEquals(List.of("u2"), names);
    }

    @Test
    void exportNames_fallBackToStoredContainerSignals() {
        CircuitModel model = modelWithControl(block("VOLT.1", "u1"));
        model.setDataContainerSignals(new String[]{"u_out"});
        List<String> names = LegacySimulationBackend.exportSignalNames(model, List.of());
        assertEquals(List.of("u_out"), names);
    }

    @Test
    void exportNames_fallBackToMeasurementOutputLabels() {
        CircuitModel model = modelWithControl(
                block("VOLT.1", "uA"), block("AMP.1", "iL"), block("SCOPE.1", null));
        List<String> names = LegacySimulationBackend.exportSignalNames(model, List.of());
        assertEquals(List.of("uA", "iL"), names);
    }

    @Test
    void exportNames_fallBackToBlockNamesWhenUnlabeled() {
        CircuitModel model = modelWithControl(block("VOLT.1", null), block("AMP.1", null));
        List<String> names = LegacySimulationBackend.exportSignalNames(model, null);
        assertEquals(List.of("VOLT.1", "AMP.1"), names);
    }
}
