package com.unime.securegame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket / STOMP configuration.
 *
 * Architecture pattern: Publish-Subscribe over STOMP.
 *
 * - Teachers publish to /app/classroom/setTopic (goes through @MessageMapping)
 * - The broker delivers to authorized members' /user/queue/classroom/{code}
 * - Only authenticated room members receive the message (no polling)
 *
 * This replaces the previous HTTP-polling design (GET /api/multiplayer/status
 * every 2 seconds) with a genuine real-time event-driven channel.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ClassroomChannelInterceptor classroomChannelInterceptor;

    public WebSocketConfig(ClassroomChannelInterceptor classroomChannelInterceptor) {
        this.classroomChannelInterceptor = classroomChannelInterceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(classroomChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback for browsers that don't support native WebSocket
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-process broker for private per-user queues
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for client → server messages handled by @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
    }
}
