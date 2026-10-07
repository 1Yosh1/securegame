package com.unime.securegame.controller;

import com.unime.securegame.service.RoomSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class RoomController {
    private final RoomSessionService roomSessionService;

    public RoomController(RoomSessionService roomSessionService) {
        this.roomSessionService = roomSessionService;
    }

    @PostMapping({"/api/room/create", "/api/multiplayer/create"})
    @PreAuthorize("hasRole('TEACHER') and #teacher == authentication.name")
    public RoomSessionService.RoomView createRoom(@RequestParam String teacher, Authentication authentication) {
        requireTeacher(authentication);
        if (!teacher.equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("A teacher may create rooms only for themselves");
        }
        return roomSessionService.createRoom(teacher);
    }

    @PostMapping({"/api/room/join", "/api/multiplayer/join"})
    @PreAuthorize("isAuthenticated() and #student == authentication.name")
    public RoomSessionService.RoomView joinRoom(@RequestParam String code, @RequestParam String student,
                                                Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !student.equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("A user may join only as themselves");
        }
        return roomSessionService.joinRoom(code, student);
    }

    @PostMapping({"/api/room/setTopic", "/api/multiplayer/setTopic"})
    @PreAuthorize("hasRole('TEACHER')")
    public RoomSessionService.RoomView setTopic(@RequestParam String code, @RequestParam String topic,
                                                Authentication authentication) {
        requireTeacher(authentication);
        return roomSessionService.setTopic(code, topic, authentication.getName());
    }

    @GetMapping({"/api/room/status", "/api/multiplayer/status"})
    @PreAuthorize("isAuthenticated()")
    public RoomSessionService.RoomView getStatus(@RequestParam String code, Authentication authentication) {
        return roomSessionService.getStatus(code, authentication.getName());
    }

    private void requireTeacher(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(authority ->
                "ROLE_TEACHER".equals(authority.getAuthority()))) {
            throw new org.springframework.security.access.AccessDeniedException("Teacher role required");
        }
    }
}
