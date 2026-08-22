package gecko.rest.controller;

import gecko.rest.model.circuit.CatalogResponse;
import gecko.rest.model.circuit.CircuitChangeMessage;
import gecko.rest.service.CircuitEditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for CircuitEditController (mocked service).
 */
@WebMvcTest(CircuitEditController.class)
@Import(gecko.rest.config.TestSecurityConfig.class)
class CircuitEditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CircuitEditService editService;

    private static final CircuitChangeMessage CHANGE =
            new CircuitChangeMessage("circuit-1", 3, "createComponent",
                    new gecko.rest.model.circuit.ComponentInfo(1, "R1", "LK", new int[]{96, 48}, 0, java.util.Map.of()));

    @Test
    void createComponent_returns201() throws Exception {
        when(editService.createComponent(eq("circuit-1"), any())).thenReturn(CHANGE);

        mockMvc.perform(post("/api/v1/circuits/circuit-1/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"family": "LK", "type": 1, "x": 100, "y": 50}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operation").value("createComponent"))
                .andExpect(jsonPath("$.modelVersion").value(3));
    }

    @Test
    void createComponent_missingFields_400() throws Exception {
        mockMvc.perform(post("/api/v1/circuits/circuit-1/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchComponent_returns200() throws Exception {
        when(editService.patchComponent(eq("circuit-1"), eq("R1"), any())).thenReturn(CHANGE);

        mockMvc.perform(patch("/api/v1/circuits/circuit-1/components/R1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\": 120}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("createComponent"));
    }

    @Test
    void deleteComponent_returns200() throws Exception {
        when(editService.deleteComponent("circuit-1", "R1")).thenReturn(CHANGE);

        mockMvc.perform(delete("/api/v1/circuits/circuit-1/components/R1"))
                .andExpect(status().isOk());
    }

    @Test
    void createConnection_returns201() throws Exception {
        when(editService.createConnection(eq("circuit-1"), any())).thenReturn(CHANGE);

        mockMvc.perform(post("/api/v1/circuits/circuit-1/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "LK", "points": [[16, 16], [48, 16]]}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void patchConnection_returns200() throws Exception {
        when(editService.patchConnection(eq("circuit-1"), eq(2), any())).thenReturn(CHANGE);

        mockMvc.perform(patch("/api/v1/circuits/circuit-1/connections/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\": \"w\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteConnection_returns200() throws Exception {
        when(editService.deleteConnection("circuit-1", 2)).thenReturn(CHANGE);

        mockMvc.perform(delete("/api/v1/circuits/circuit-1/connections/2"))
                .andExpect(status().isOk());
    }

    @Test
    void setNodeLabel_returns200() throws Exception {
        when(editService.setNodeLabel(eq("circuit-1"), eq("R1"), any())).thenReturn(CHANGE);

        mockMvc.perform(put("/api/v1/circuits/circuit-1/nodes/R1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalIndex\": 0, \"side\": \"x\", \"label\": \"dc\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void undo_conflict_409() throws Exception {
        when(editService.undo("circuit-1")).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Nothing to undo"));

        mockMvc.perform(post("/api/v1/circuits/circuit-1/undo"))
                .andExpect(status().isConflict());
    }

    @Test
    void editorModel_returnsSnapshot() throws Exception {
        when(editService.getEditorModel("circuit-1")).thenReturn(
                new gecko.rest.model.circuit.EditorModelResponse(
                        "circuit-1", 7, "test.ipes", 16, "600x600",
                        List.of(), List.of(),
                        new gecko.rest.model.circuit.EditorModelResponse.SimulationDefaults(
                                1e-6, 0.02, "backward-euler", List.of("V_out"))));

        mockMvc.perform(get("/api/v1/circuits/circuit-1/model"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").value(7))
                .andExpect(jsonPath("$.dpix").value(16))
                .andExpect(jsonPath("$.components").isArray())
                .andExpect(jsonPath("$.connections").isArray())
                .andExpect(jsonPath("$.simulationDefaults.timeStep").value(1e-6))
                .andExpect(jsonPath("$.simulationDefaults.signals[0]").value("V_out"));
    }

    @Test
    void catalog_returnsTypes() throws Exception {
        when(editService.getCatalog()).thenReturn(new CatalogResponse(
                List.of(new CatalogResponse.CatalogEntry(1, "LK_R", "LK"))));

        mockMvc.perform(get("/api/v1/circuits/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0].type").value(1))
                .andExpect(jsonPath("$.types[0].name").value("LK_R"))
                .andExpect(jsonPath("$.types[0].family").value("LK"));
    }
}
