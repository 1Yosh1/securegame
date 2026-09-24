package com.unime.securegame.controller;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket/STOMP controller for the live classroom feature.
 *
 * Design pattern: Publish-Subscribe
 *
 * Flow:
 *   1. Teacher creates a room (REST POST /api/room/create) → gets a 4-digit code
 *   2. Students join (REST POST /api/room/join) and subscribe to /topic/classroom/{code}
 *   3. Teacher sends a topic via STOMP message to /app/classroom/setTopic
 *   4. Server broadcasts TopicEvent to ALL subscribers of /topic/classroom/{code}
 *   5. Each student's browser receives the push and immediately launches the game
 *
 * No polling. No database reads per tick. Pure event-driven delivery.
 */
@Controller
public class ClassroomWsController {

    private final SimpMessagingTemplate broker;

    // In-memory room registry: code → {teacher, students[], currentTopic}
    // Intentionally NOT persisted — classroom sessions are ephemeral
    private final Map<String, Map<String, Object>> rooms = new ConcurrentHashMap<>();

    public ClassroomWsController(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    // ── REST: create room ────────────────────────────────────────────────────

    @PostMapping("/api/room/create")
    @ResponseBody
    public Map<String, Object> createRoom(@RequestParam String teacher) {
        String code = String.format("%04d", new Random().nextInt(10000));
        Map<String, Object> room = new HashMap<>();
        room.put("code", code);
        room.put("teacher", teacher);
        room.put("students", new ArrayList<String>());
        room.put("topic", "WAITING");
        rooms.put(code, room);
        return room;
    }

    // ── REST: student joins a room ────────────────────────────────────────────

    @PostMapping("/api/room/join")
    @ResponseBody
    public Map<String, Object> joinRoom(@RequestParam String code, @RequestParam String student) {
        Map<String, Object> room = rooms.get(code);
        if (room == null) throw new RuntimeException("Room not found: " + code);
        @SuppressWarnings("unchecked")
        List<String> students = (List<String>) room.get("students");
        if (!students.contains(student)) students.add(student);
        return room;
    }

    // ── REST: teacher gets student count ──────────────────────────────────────

    @GetMapping("/api/room/status")
    @ResponseBody
    public Map<String, Object> status(@RequestParam String code) {
        Map<String, Object> room = rooms.get(code);
        if (room == null) throw new RuntimeException("Room not found: " + code);
        return room;
    }

    // ── STOMP: teacher pushes a topic → broadcast to all students ─────────────

    /**
     * Receives a SetTopicMessage from the teacher via STOMP.
     * Broadcasts a TopicEvent to /topic/classroom/{code}.
     * Every subscribed student receives it instantly.
     */
    @MessageMapping("/classroom/setTopic")
    public void setTopic(SetTopicMessage msg) {
        Map<String, Object> room = rooms.get(msg.getCode());
        if (room == null) return;
        room.put("topic", msg.getTopic());

        // Publish to all subscribers on this room's channel
        Map<String, String> event = new HashMap<>();
        event.put("type", "TOPIC_CHANGE");
        event.put("topic", msg.getTopic());
        event.put("code", msg.getCode());
        broker.convertAndSend("/topic/classroom/" + msg.getCode(), event);
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
