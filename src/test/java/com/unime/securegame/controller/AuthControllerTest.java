package com.unime.securegame.controller;

import com.unime.securegame.model.ClassroomEntity;
import com.unime.securegame.model.UserEntity;
import com.unime.securegame.repository.ClassroomRepository;
import com.unime.securegame.repository.UserRepository;
import com.unime.securegame.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private final EmailService emailService = mock(EmailService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ClassroomRepository classroomRepository = mock(ClassroomRepository.class);
    private final AuthController controller = new AuthController(emailService, userRepository,
            classroomRepository, new org.springframework.security.web.context.HttpSessionSecurityContextRepository());

    @Test
    void registrationHashesPasswordAndIgnoresRequestedRoleAndClassroom() {
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.register(" Learner@Example.com ", "A long passphrase 123!", "Learner",
                "TEACHER", "attacker-controlled-room");

        assertEquals(200, response.getStatusCode().value());
        UserEntity stored = saved.getValue();
        assertEquals("PENDING", stored.getRole());
        assertNull(stored.getClassroomCode());
        assertNotEquals("A long passphrase 123!", stored.getPassword());
        assertTrue(stored.getPassword().startsWith("pbkdf2-sha256$600000$"));
    }

    @Test
    void loginFailsClosedWhenOtpEmailCannotBeDelivered() {
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.empty(), Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var registration = controller.register("learner@example.com", "A long passphrase 123!", "Learner", null, null);
        assertEquals(200, registration.getStatusCode().value());
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(saved.capture());
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(saved.getValue()));
        when(emailService.sendOtpEmail(eq("learner@example.com"), any())).thenReturn(false);

        var response = controller.login("learner@example.com", "A long passphrase 123!");

        assertEquals(503, response.getStatusCode().value());
        verify(emailService).sendOtpEmail(eq("learner@example.com"), any());
    }

    @Test
    void passwordResetRejectsShortPasswordBeforeConsumingRecoveryCode() {
        var response = controller.resetPassword("learner@example.com", "123456", "short",
                new MockHttpServletRequest());
        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(userRepository);
    }

    @Test
    void classroomStudentDataIsVisibleOnlyToItsTeacherOrAnAdmin() {
        SecurityContextHolder.getContext().setAuthentication(authenticated("teacher@example.com", "ROLE_TEACHER"));
        ClassroomEntity classroom = new ClassroomEntity();
        classroom.setCode("ROOM01");
        classroom.setTeacherEmail("owner@example.com");
        when(classroomRepository.findByCode("ROOM01")).thenReturn(Optional.of(classroom));

        assertEquals(403, controller.getClassroomStudents("ROOM01", SecurityContextHolder.getContext().getAuthentication())
                .getStatusCode().value());
        verifyNoInteractions(userRepository);

        SecurityContextHolder.getContext().setAuthentication(authenticated("owner@example.com", "ROLE_TEACHER"));
        assertEquals(200, controller.getClassroomStudents("ROOM01", SecurityContextHolder.getContext().getAuthentication())
                .getStatusCode().value());
        verify(userRepository).findByClassroomCode("ROOM01");
        SecurityContextHolder.clearContext();
    }

    @Test
    void progressRequiresTrustedDeviceTokenEvenForAuthenticatedOwner() {
        SecurityContextHolder.getContext().setAuthentication(authenticated("learner@example.com", "ROLE_STUDENT"));

        var response = controller.updateProgress("learner@example.com", 10, 1, "forged-token");

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(userRepository);
        SecurityContextHolder.clearContext();
    }

    private UsernamePasswordAuthenticationToken authenticated(String username, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(username, null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
