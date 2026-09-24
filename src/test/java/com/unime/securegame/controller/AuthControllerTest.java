package com.unime.securegame.controller;

import com.unime.securegame.model.UserEntity;
import com.unime.securegame.repository.UserRepository;
import com.unime.securegame.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @MockBean
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setPassword(hashPassword("password123"));
        testUser.setName("Test User");
        testUser.setRole("STUDENT");
        testUser.setClassroomCode("CLASS123");
        testUser.setLevel(1);
        testUser.setXp(0);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    @Test
    void register_Success() throws Exception {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/register")
                        .param("email", "new@test.com")
                        .param("password", "newpass")
                        .param("name", "New User")
                        .param("role", "STUDENT")
                        .param("classroomCode", "CLASS123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertEquals("new@test.com", savedUser.getEmail());
        assertEquals(hashPassword("newpass"), savedUser.getPassword());
        assertEquals("New User", savedUser.getName());
        assertEquals("STUDENT", savedUser.getRole());
        assertEquals("CLASS123", savedUser.getClassroomCode());
    }

    @Test
    void register_EmailAlreadyExists() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/register")
                        .param("email", "test@test.com")
                        .param("password", "newpass")
                        .param("name", "New User"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void login_Success_OTPSent() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/login")
                        .param("email", "test@test.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("otp_sent"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        verify(emailService).sendOtpEmail(eq("test@test.com"), anyString());
    }

    @Test
    void login_InvalidPassword() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/login")
                        .param("email", "test@test.com")
                        .param("password", "wrongpassword"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void login_UserNotFound() throws Exception {
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .param("email", "notfound@test.com")
                        .param("password", "password123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void updateRole_Success() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/update-role")
                        .param("email", "test@test.com")
                        .param("role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.role").value("TEACHER"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        assertEquals("TEACHER", userCaptor.getValue().getRole());
    }

    @Test
    void updateRole_UserNotFound() throws Exception {
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/update-role")
                        .param("email", "notfound@test.com")
                        .param("role", "TEACHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void forgotPassword_Success() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Recovery OTP sent to email"));

        verify(emailService).sendOtpEmail(eq("test@test.com"), anyString());
    }

    @Test
    void forgotPassword_UserNotFound() throws Exception {
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .param("email", "notfound@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("No account registered with this email"));

        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void getClassroomStudents_Success() throws Exception {
        when(userRepository.findByClassroomCode("CLASS123")).thenReturn(Collections.singletonList(testUser));

        mockMvc.perform(get("/api/auth/classroom/CLASS123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@test.com"))
                .andExpect(jsonPath("$[0].name").value("Test User"));
    }
}
