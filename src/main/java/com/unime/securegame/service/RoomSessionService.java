package com.unime.securegame.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the ephemeral state and access rules for live classroom rooms. */
@Service
public class RoomSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ROOM_CODE_SPACE = 10_000;

    private final ConcurrentHashMap<String, RoomSession> rooms = new ConcurrentHashMap<>();

    public RoomView createRoom(String teacher) {
        requireIdentity(teacher, "teacher");
        for (int attempt = 0; attempt < ROOM_CODE_SPACE; attempt++) {
            String code = String.format(java.util.Locale.ROOT, "%04d", RANDOM.nextInt(ROOM_CODE_SPACE));
            RoomSession room = new RoomSession(code, teacher);
            if (rooms.putIfAbsent(code, room) == null) return view(room);
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No room codes are available");
    }

    public RoomView joinRoom(String code, String student) {
        requireIdentity(student, "student");
        RoomSession room = findRoom(code);
        room.students.add(student);
        return view(room);
    }

    public RoomView getStatus(String code, String requester) {
        RoomSession room = findRoom(code);
        requireMember(room, requester);
        return view(room);
    }

    public RoomView setTopic(String code, String topic, String requester) {
        RoomSession room = findRoom(code);
        if (!room.teacher.equals(requester)) {
            throw new org.springframework.security.access.AccessDeniedException("Only this room's teacher may change its topic");
        }
        if (topic == null || topic.isBlank() || topic.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic must contain 1 to 80 characters");
        }
        room.topic = topic.trim();
        return view(room);
    }

    /** Used by the STOMP inbound-channel guard before allowing a room subscription. */
    public boolean isMember(String code, String identity) {
        if (identity == null || identity.isBlank() || code == null || !code.matches("\\d{4}")) return false;
        RoomSession room = rooms.get(code);
        return room != null && (room.teacher.equals(identity) || room.students.contains(identity));
    }

    public Set<String> getStudentsForTeacher(String code, String requester) {
        RoomSession room = findRoom(code);
        if (!room.teacher.equals(requester)) {
            throw new org.springframework.security.access.AccessDeniedException("Only this room's teacher may publish a topic");
        }
        return Set.copyOf(room.students);
    }

    private RoomSession findRoom(String code) {
        if (code == null || !code.matches("\\d{4}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room code must contain four digits");
        }
        RoomSession room = rooms.get(code);
        if (room == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        return room;
    }

    private void requireMember(RoomSession room, String requester) {
        if (requester == null || !(room.teacher.equals(requester) || room.students.contains(requester))) {
            throw new org.springframework.security.access.AccessDeniedException("Not a member of this room");
        }
    }

    private void requireIdentity(String identity, String label) {
        if (identity == null || identity.isBlank() || identity.length() > 254) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + label + " identity");
        }
    }

    private RoomView view(RoomSession room) {
        return new RoomView(room.code, room.topic, room.students.size());
    }

    private static final class RoomSession {
        private final String code;
        private final String teacher;
        private final Set<String> students = ConcurrentHashMap.newKeySet();
        private volatile String topic = "WAITING";

        private RoomSession(String code, String teacher) {
            this.code = code;
            this.teacher = teacher;
        }
    }

    /** Public room state intentionally excludes teacher and student identities. */
    public record RoomView(String code, String topic, int studentCount) {}
}
