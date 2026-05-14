package com.unime.securegame.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ScenarioController — tests all CRUD endpoints.
 * Category A test gate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Scenario savedScenario;

    @BeforeEach
    void setUp() {
        savedScenario = scenarioRepository.save(
                new Scenario("University Admin", 3, 50, true));
    }

    @Test
    void getAllScenarios_returnsListWithAtLeastOne() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getScenarioById_returnsCorrectScenario() throws Exception {
        mockMvc.perform(get("/api/scenarios/" + savedScenario.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("University Admin"))
                .andExpect(jsonPath("$.lockoutThreshold").value(3))
                .andExpect(jsonPath("$.mfaRequired").value(true));
    }

    @Test
    void getScenario_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/scenarios/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createScenario_returnsCreatedScenario() throws Exception {
        Scenario newScenario = new Scenario("Restaurant Portal", 5, 30, false);

        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newScenario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Restaurant Portal"))
                .andExpect(jsonPath("$.lockoutThreshold").value(5))
                .andExpect(jsonPath("$.mfaRequired").value(false));
    }

    @Test
    void updateScenario_updatesFieldsCorrectly() throws Exception {
        savedScenario.setName("Updated Name");
        savedScenario.setLockoutThreshold(10);

        mockMvc.perform(put("/api/scenarios/" + savedScenario.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedScenario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.lockoutThreshold").value(10));
    }

    @Test
    void deleteScenario_softDeletes() throws Exception {
        mockMvc.perform(delete("/api/scenarios/" + savedScenario.getId()))
                .andExpect(status().isNoContent());

        // Verify it's soft-deleted (not in non-deleted list)
        mockMvc.perform(get("/api/scenarios/" + savedScenario.getId()))
                .andExpect(status().isNotFound());
    }
}
