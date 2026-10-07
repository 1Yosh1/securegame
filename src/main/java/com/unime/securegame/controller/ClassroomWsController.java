package com.unime.securegame.controller;

import com.unime.securegame.service.RoomSessionService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket/STOMP controller for the live classroom feature.
 *
 * Design pattern: Publish-Subscribe
 *
 * Flow:
 *   1. Teacher creates a room and students join through the shared room session service
 *   2. Joined members subscribe to their authenticated /user/queue/classroom/{code}
 *   3. The owning teacher sends a topic via STOMP to /app/classroom/setTopic
 *   4. The server sends the event only to authenticated members of that room
 *   5. Each student's browser receives the push and immediately launches the game
 *
 * No polling. No database reads per tick. Pure event-driven delivery.
 */
@Controller
public class ClassroomWsController {

    private final SimpMessagingTemplate broker;
    private final RoomSessionService roomSessions;

    public ClassroomWsController(SimpMessagingTemplate broker, RoomSessionService roomSessions) {
        this.broker = broker;
        this.roomSessions = roomSessions;
    }

    /** Receives an owning teacher's topic and privately delivers it to current room members. */
    @MessageMapping("/classroom/setTopic")
    public void setTopic(SetTopicMessage msg, Authentication authentication) {
        if (msg == null) return;
        if (authentication == null || authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).noneMatch("ROLE_TEACHER"::equals)) return;
        try {
            RoomSessionService.RoomView updated = roomSessions.setTopic(
                    msg.getCode(), msg.getTopic(), authentication.getName());
            Map<String, String> event = new HashMap<>();
            event.put("type", "TOPIC_CHANGE");
            event.put("topic", updated.topic());
            event.put("code", updated.code());
            roomSessions.getStudentsForTeacher(updated.code(), authentication.getName())
                    .forEach(member -> broker.convertAndSendToUser(
                            member, "/queue/classroom/" + updated.code(), event));
            broker.convertAndSendToUser(authentication.getName(),
                    "/queue/classroom/" + updated.code(), event);
        } catch (org.springframework.security.access.AccessDeniedException denied) {
            throw denied;
        } catch (org.springframework.web.server.ResponseStatusException ignored) {
            // Invalid or expired room/message: do not disclose room existence over STOMP.
        }
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    public static class SetTopicMessage {
        private String code;
        private String topic;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
    }
}
