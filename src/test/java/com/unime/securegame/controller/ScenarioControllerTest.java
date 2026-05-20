package com.unime.securegame.controller;

import com.unime.securegame.service.LlmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScenarioController.class)
class ScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmService llmService;

    @Test
    void generateScenario_returnsLlmResponse() throws Exception {
        when(llmService.generateScenario("TestTopic", "crypto")).thenReturn("[{\"mock\":\"response\"}]");

        mockMvc.perform(get("/api/scenario/generate")
                        .param("topic", "TestTopic")
                        .param("type", "crypto"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"mock\":\"response\"}]"));
    }
}
