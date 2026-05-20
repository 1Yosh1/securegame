package com.unime.securegame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket / STOMP configuration.
 *
 * Architecture pattern: Publish-Subscribe over STOMP.
 *
 * - Teachers publish to /app/classroom/setTopic (goes through @MessageMapping)
 * - The broker fans out to /topic/classroom/{code}
 * - All subscribed students receive the message instantly (no polling)
 *
 * This replaces the previous HTTP-polling design (GET /api/multiplayer/status
 * every 2 seconds) with a genuine real-time event-driven channel.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback for browsers that don't support native WebSocket
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-process broker for /topic/* destinations
        registry.enableSimpleBroker("/topic");
        // Prefix for client → server messages handled by @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
    }
}
