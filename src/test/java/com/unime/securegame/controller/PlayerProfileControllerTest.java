package com.unime.securegame.controller;

import com.unime.securegame.model.PlayerProfile;
import com.unime.securegame.repository.PlayerProfileRepository;
import com.unime.securegame.service.AdminApiKeyService;
import com.unime.securegame.service.TOTPService;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerProfileController.class)
class PlayerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerProfileRepository playerProfileRepository;

    @MockBean
    private TOTPService totpService;

    @MockBean
    private AdminApiKeyService adminApiKeyService;

    @AfterEach
    void clearAuthentication() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "teacher-one", roles = "TEACHER")
    void listPlayersRedactsTotpSecret() throws Exception {
        PlayerProfile player = new PlayerProfile("player-one");
        player.setTotpSecret("DO_NOT_LEAK_THIS_SECRET");
        when(playerProfileRepository.findAll()).thenReturn(List.of(player));

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("DO_NOT_LEAK_THIS_SECRET"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("totpSecret"))));
    }

    @Test
    @WithMockUser(username = "player-one")
    void createPlayerOnlyAcceptsUsername() throws Exception {
        when(totpService.generateSecret()).thenReturn("generated-secret");
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(user("player-one").roles("USER"))
                        .content("{\"username\":\"player-one\",\"score\":9999,\"badgeCount\":99,\"totpSecret\":\"attacker-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("player-one")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("generated-secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("attacker-secret"))));
    }

    @Test
    @WithMockUser(username = "private-player")
    void getOwnPlayerReturnsSafeDto() throws Exception {
        PlayerProfile player = new PlayerProfile("private-player");
        player.setTotpSecret("DO_NOT_LEAK_THIS_SECRET");
        when(playerProfileRepository.findById(42L)).thenReturn(Optional.of(player));

        mockMvc.perform(get("/api/players/42"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("DO_NOT_LEAK_THIS_SECRET"))));
    }

    @Test
    @WithMockUser(username = "player-one")
    void playerCannotReadAnotherPlayersProfile() throws Exception {
        when(playerProfileRepository.findById(42L)).thenReturn(Optional.of(new PlayerProfile("private-player")));

        mockMvc.perform(get("/api/players/42"))
                .andExpect(status().isForbidden());
    }
}
