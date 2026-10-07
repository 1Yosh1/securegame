package com.unime.securegame.controller;

import com.unime.securegame.service.RoomSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClassroomWsControllerTest {
    private final RoomSessionService rooms = new RoomSessionService();
    private final SimpMessagingTemplate broker = mock(SimpMessagingTemplate.class);
    private final ClassroomWsController controller = new ClassroomWsController(broker, rooms);

    @Test
    void topicChangeIsDeliveredOnlyToRoomMembers() {
        var room = rooms.createRoom("teacher@example.com");
        rooms.joinRoom(room.code(), "student@example.com");
        var message = new ClassroomWsController.SetTopicMessage();
        message.setCode(room.code());
        message.setTopic("phishing");

        controller.setTopic(message, teacher("teacher@example.com"));

        verify(broker).convertAndSendToUser(eq("student@example.com"),
                eq("/queue/classroom/" + room.code()), argThat(payload ->
                        "phishing".equals(((Map<?, ?>) payload).get("topic"))));
        verify(broker).convertAndSendToUser(eq("teacher@example.com"),
                eq("/queue/classroom/" + room.code()), any());
        verifyNoMoreInteractions(broker);
        assertEquals("phishing", rooms.getStatus(room.code(), "student@example.com").topic());
    }

    @Test
    void differentTeacherCannotChangeRoomTopic() {
        var room = rooms.createRoom("teacher@example.com");
        var message = new ClassroomWsController.SetTopicMessage();
        message.setCode(room.code());
        message.setTopic("phishing");

        assertThrows(AccessDeniedException.class,
                () -> controller.setTopic(message, teacher("other-teacher@example.com")));
        verifyNoInteractions(broker);
        assertEquals("WAITING", rooms.getStatus(room.code(), "teacher@example.com").topic());
    }

    private UsernamePasswordAuthenticationToken teacher(String name) {
        return UsernamePasswordAuthenticationToken.authenticated(name, null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }
}
