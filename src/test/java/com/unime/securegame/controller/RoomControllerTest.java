package com.unime.securegame.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoomControllerTest {

    private RoomController roomController;

    @BeforeEach
    void setUp() {
        roomController = new RoomController();
    }

    @Test
    void testCreateRoom() {
        String teacherName = "Mr. Anderson";
        Map<String, Object> room = roomController.createRoom(teacherName);

        assertNotNull(room);
        assertTrue(room.containsKey("code"));
        assertTrue(room.containsKey("teacher"));
        assertTrue(room.containsKey("topic"));
        assertTrue(room.containsKey("students"));

        assertEquals(teacherName, room.get("teacher"));
        assertEquals("WAITING", room.get("topic"));
        assertTrue(((List<String>) room.get("students")).isEmpty());

        String code = (String) room.get("code");
        assertNotNull(code);
        assertEquals(4, code.length());
        assertTrue(code.matches("\\d{4}"));
    }

    @Test
    void testJoinRoom() {
        String teacherName = "Mr. Anderson";
        Map<String, Object> room = roomController.createRoom(teacherName);
        String code = (String) room.get("code");

        String studentName = "Neo";
        Map<String, Object> updatedRoom = roomController.joinRoom(code, studentName);

        assertNotNull(updatedRoom);
        List<String> students = (List<String>) updatedRoom.get("students");
        assertEquals(1, students.size());
        assertTrue(students.contains(studentName));
    }

    @Test
    void testJoinRoomNotFound() {
        String invalidCode = "9999"; // Assuming not created
        assertThrows(RuntimeException.class, () -> {
            roomController.joinRoom(invalidCode, "Student");
        });
    }

    @Test
    void testSetTopic() {
        String teacherName = "Mr. Anderson";
        Map<String, Object> room = roomController.createRoom(teacherName);
        String code = (String) room.get("code");

        String newTopic = "Cybersecurity Basics";
        Map<String, Object> updatedRoom = roomController.setTopic(code, newTopic);

        assertNotNull(updatedRoom);
        assertEquals(newTopic, updatedRoom.get("topic"));
    }

    @Test
    void testSetTopicNotFound() {
        String invalidCode = "9999";
        assertThrows(RuntimeException.class, () -> {
            roomController.setTopic(invalidCode, "Topic");
        });
    }

    @Test
    void testGetStatus() {
        String teacherName = "Mr. Anderson";
        Map<String, Object> room = roomController.createRoom(teacherName);
        String code = (String) room.get("code");

        Map<String, Object> status = roomController.getStatus(code);
        assertNotNull(status);
        assertEquals(teacherName, status.get("teacher"));
    }
}
