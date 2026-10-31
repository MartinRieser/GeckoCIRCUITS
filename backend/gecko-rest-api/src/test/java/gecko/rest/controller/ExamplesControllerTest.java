package gecko.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ExamplesController endpoints.
 */
@WebMvcTest(ExamplesController.class)
@Import(gecko.rest.config.TestSecurityConfig.class)
class ExamplesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listExamples_returnsOkAndArray() throws Exception {
        mockMvc.perform(get("/api/v1/examples")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void crudLifecycle_createsUpdatesAndDeletesExample() throws Exception {
        String createJson = """
                {
                    "id": "test_temp_example",
                    "name": "Test Temp Circuit",
                    "category": "Testing",
                    "description": "A circuit created during unit testing",
                    "content": "tDURATION 0.01\\ndt 1e-6\\n"
                }
                """;

        // 1. Create
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test_temp_example"))
                .andExpect(jsonPath("$.name").value("Test Temp Circuit"))
                .andExpect(jsonPath("$.content").value("tDURATION 0.01\ndt 1e-6\n"));

        // 2. Read single
        mockMvc.perform(get("/api/v1/examples/test_temp_example")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Temp Circuit"));

        // 3. Update
        String updateJson = """
                {
                    "name": "Updated Temp Circuit",
                    "category": "Testing",
                    "description": "Updated description",
                    "content": "tDURATION 0.02\\ndt 1e-6\\n"
                }
                """;

        mockMvc.perform(put("/api/v1/examples/test_temp_example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Temp Circuit"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.content").value("tDURATION 0.02\ndt 1e-6\n"));

        // 4. Delete
        mockMvc.perform(delete("/api/v1/examples/test_temp_example"))
                .andExpect(status().isNoContent());

        // 5. Verify deleted
        mockMvc.perform(get("/api/v1/examples/test_temp_example"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createExample_withOverwrite_updatesExistingInsteadOfDuplicating() throws Exception {
        String initialJson = """
                {
                    "id": "overwrite_test_example",
                    "name": "Unique Overwrite Circuit",
                    "category": "Testing",
                    "description": "Original description",
                    "content": "tDURATION 0.01\\n"
                }
                """;

        // Create initial
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("overwrite_test_example"))
                .andExpect(jsonPath("$.name").value("Unique Overwrite Circuit"));

        // Creating with duplicate name without overwrite returns 409 CONFLICT
        String duplicateNameNoOverwrite = """
                {
                    "name": "Unique Overwrite Circuit",
                    "category": "Testing",
                    "description": "Duplicate attempt",
                    "content": "tDURATION 0.05\\n"
                }
                """;
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateNameNoOverwrite))
                .andExpect(status().isConflict());

        // Creating with duplicate name AND overwrite=true replaces existing
        String duplicateNameWithOverwrite = """
                {
                    "name": "Unique Overwrite Circuit",
                    "category": "Testing",
                    "description": "Overwritten description",
                    "content": "tDURATION 0.09\\n",
                    "overwrite": true
                }
                """;
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateNameWithOverwrite))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("overwrite_test_example"))
                .andExpect(jsonPath("$.description").value("Overwritten description"))
                .andExpect(jsonPath("$.content").value("tDURATION 0.09\n"));

        // Clean up
        mockMvc.perform(delete("/api/v1/examples/overwrite_test_example"))
                .andExpect(status().isNoContent());
    }
}
