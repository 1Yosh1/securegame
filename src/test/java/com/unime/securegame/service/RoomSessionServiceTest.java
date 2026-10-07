package com.unime.securegame.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class RoomSessionServiceTest {
    private final RoomSessionService rooms = new RoomSessionService();

    @Test
    void onlyRoomMembersCanReadStatusAndPublicViewOmitsIdentities() {
        var created = rooms.createRoom("teacher@example.com");
        rooms.joinRoom(created.code(), "student@example.com");

        var studentView = rooms.getStatus(created.code(), "student@example.com");
        var teacherView = rooms.getStatus(created.code(), "teacher@example.com");

        assertEquals("WAITING", studentView.topic());
        assertEquals(1, teacherView.studentCount());
        assertEquals(created.code(), studentView.code());
        assertThrows(AccessDeniedException.class,
                () -> rooms.getStatus(created.code(), "outsider@example.com"));
        assertFalse(studentView.toString().contains("teacher@example.com"));
        assertFalse(studentView.toString().contains("student@example.com"));
    }

    @Test
    void onlyOwningTeacherMaySetBoundedTopic() {
        var created = rooms.createRoom("teacher@example.com");

        assertThrows(AccessDeniedException.class,
                () -> rooms.setTopic(created.code(), "phishing", "other-teacher@example.com"));
        assertEquals("phishing", rooms.setTopic(created.code(), " phishing ", "teacher@example.com").topic());
        assertThrows(ResponseStatusException.class,
                () -> rooms.setTopic(created.code(), " ", "teacher@example.com"));
        assertThrows(ResponseStatusException.class,
                () -> rooms.setTopic(created.code(), "x".repeat(81), "teacher@example.com"));
    }

    @Test
    void roomCodeAndIdentityAreValidatedAndJoiningIsIdempotent() {
        assertThrows(ResponseStatusException.class, () -> rooms.createRoom(" "));
        assertThrows(ResponseStatusException.class, () -> rooms.getStatus("../room", "anyone"));
        var created = rooms.createRoom("teacher@example.com");

        rooms.joinRoom(created.code(), "student@example.com");
        var afterRepeatedJoin = rooms.joinRoom(created.code(), "student@example.com");

        assertEquals(1, afterRepeatedJoin.studentCount());
    }
}
