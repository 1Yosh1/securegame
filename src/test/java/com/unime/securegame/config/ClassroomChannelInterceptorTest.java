package com.unime.securegame.config;

import com.unime.securegame.service.RoomSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ClassroomChannelInterceptorTest {
    private final RoomSessionService rooms = new RoomSessionService();
    private final ClassroomChannelInterceptor interceptor = new ClassroomChannelInterceptor(rooms);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void onlyAuthenticatedRoomMemberMaySubscribeToPrivateRoomQueue() {
        var room = rooms.createRoom("teacher@example.com");
        rooms.joinRoom(room.code(), "student@example.com");
        var member = authenticated("student@example.com");

        assertDoesNotThrow(() -> interceptor.preSend(subscribe(member, "/user/queue/classroom/" + room.code()), channel));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subscribe(member, "/topic/classroom/" + room.code()), channel));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subscribe(authenticated("outsider@example.com"),
                        "/user/queue/classroom/" + room.code()), channel));
    }

    @Test
    void onlyAuthenticatedClientMaySendToSupportedClassroomHandler() {
        var teacher = authenticated("teacher@example.com", "ROLE_TEACHER");

        assertDoesNotThrow(() -> interceptor.preSend(send(teacher, "/app/classroom/setTopic"), channel));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(send(authenticated("teacher@example.com"), "/topic/classroom/0001"), channel));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(send(null, "/app/classroom/setTopic"), channel));
    }

    private Message<byte[]> subscribe(UsernamePasswordAuthenticationToken user, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(user);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> send(UsernamePasswordAuthenticationToken user, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        if (user != null) accessor.setUser(user);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UsernamePasswordAuthenticationToken authenticated(String name) {
        return authenticated(name, "ROLE_STUDENT");
    }

    private UsernamePasswordAuthenticationToken authenticated(String name, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(name, null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
