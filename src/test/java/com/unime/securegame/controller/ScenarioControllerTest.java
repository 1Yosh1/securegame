package com.unime.securegame.controller;

import com.unime.securegame.repository.ScenarioRepository;
import com.unime.securegame.service.AdminApiKeyService;
import com.unime.securegame.service.ScenarioGeneratorService;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScenarioController.class)
class ScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScenarioGeneratorService scenarioGenerator;

    @MockBean
    private ScenarioRepository scenarioRepository;

    @MockBean
    private AdminApiKeyService adminApiKeyService;

    @Test
    @WithMockUser
    void generateScenario_returnsPureJavaResponse() throws Exception {
        when(scenarioGenerator.generateScenario("TestTopic", "crypto", 42L))
                .thenReturn("[{\"mock\":\"response\"}]");

        mockMvc.perform(get("/api/scenarios/generate")
                        .param("topic", "TestTopic")
                        .param("type", "crypto"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"mock\":\"response\"}]"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createScenario_requiresValidPolicyAndPersistsIt() throws Exception {
        when(scenarioRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/scenarios")
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" MFA Basics \",\"lockoutThreshold\":3,\"minPasswordEntropy\":40,\"mfaRequired\":true,\"geoCheckEnabled\":false}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("MFA Basics")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createScenarioRejectsOutOfRangePolicy() throws Exception {
        mockMvc.perform(post("/api/scenarios")
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Broken\",\"lockoutThreshold\":0,\"minPasswordEntropy\":40,\"mfaRequired\":true,\"geoCheckEnabled\":false}"))
                .andExpect(status().isBadRequest());
    }
}
