package com.unime.securegame.config;

import com.unime.securegame.service.RoomSessionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

/** Enforces authenticated member-only subscriptions to per-room user queues. */
@Component
public class ClassroomChannelInterceptor implements ChannelInterceptor {
    private final RoomSessionService roomSessions;

    public ClassroomChannelInterceptor(RoomSessionService roomSessions) {
        this.roomSessions = roomSessions;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            authenticatedIdentity(accessor);
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            String identity = authenticatedIdentity(accessor);
            String prefix = "/user/queue/classroom/";
            if (destination == null || !destination.startsWith(prefix)) {
                throw new AccessDeniedException("Only private classroom destinations may be subscribed to");
            }
            String code = destination.substring(prefix.length());
            if (!roomSessions.isMember(code, identity)) {
                throw new AccessDeniedException("Room membership required");
            }
        } else if (command == StompCommand.SEND) {
            Principal principal = accessor.getUser();
            if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
                throw new AccessDeniedException("Authenticated WebSocket session required");
            }
            if (accessor.getDestination() == null || !accessor.getDestination().equals("/app/classroom/setTopic")) {
                throw new AccessDeniedException("Unsupported classroom message destination");
            }
        }
        return message;
    }

    private String authenticatedIdentity(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authenticated WebSocket session required");
        }
        return authentication.getName();
    }
}
