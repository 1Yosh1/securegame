package com.unime.securegame.controller;

import com.unime.securegame.service.RoomSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.unime.securegame.service.AdminApiKeyService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
class RoomControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomSessionService roomSessionService;

    @MockBean
    private AdminApiKeyService adminApiKeyService;

    @Test
    @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
    void teacherMayCreateOwnRoomButCannotClaimAnotherIdentity() throws Exception {
        mockMvc.perform(post("/api/room/create").with(csrf()).param("teacher", "teacher@example.com"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/room/create").with(csrf()).param("teacher", "victim@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void studentMayJoinOnlyAsSelfAndCannotSetRoomTopic() throws Exception {
        mockMvc.perform(post("/api/room/join").with(csrf())
                        .param("code", "1234").param("student", "student@example.com"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/room/join").with(csrf())
                        .param("code", "1234").param("student", "victim@example.com"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/room/setTopic").with(csrf())
                        .param("code", "1234").param("topic", "phishing"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "outsider@example.com", roles = "STUDENT")
    void outsiderCannotReadRoomStatus() throws Exception {
        org.mockito.Mockito.when(roomSessionService.getStatus("1234", "outsider@example.com"))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Not a member"));

        mockMvc.perform(get("/api/room/status").param("code", "1234"))
                .andExpect(status().isForbidden());
    }
}
