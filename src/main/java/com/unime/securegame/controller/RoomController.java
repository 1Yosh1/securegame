package com.unime.securegame.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/multiplayer")
public class RoomController {
    // In-memory data store for live multiplayer classroom sessions
    private Map<String, Map<String, Object>> rooms = new HashMap<>();

    @PostMapping("/create")
    public Map<String, Object> createRoom(@RequestParam String teacher) {
        String code = String.format("%04d", new Random().nextInt(10000));
        Map<String, Object> room = new HashMap<>();
        room.put("code", code);
        room.put("teacher", teacher);
        room.put("topic", "WAITING");
        room.put("students", new ArrayList<String>());
        rooms.put(code, room);
        return room;
    }

    @PostMapping("/join")
    public Map<String, Object> joinRoom(@RequestParam String code, @RequestParam String student) {
        if(rooms.containsKey(code)) {
            List<String> students = (List<String>) rooms.get(code).get("students");
            if(!students.contains(student)) students.add(student);
            return rooms.get(code);
        }
        throw new RuntimeException("Room not found");
    }

    @PostMapping("/setTopic")
    public Map<String, Object> setTopic(@RequestParam String code, @RequestParam String topic) {
         if(rooms.containsKey(code)) {
            rooms.get(code).put("topic", topic);
            return rooms.get(code);
         }
         throw new RuntimeException("Room not found");
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam String code) {
        return rooms.get(code);
    }
}
