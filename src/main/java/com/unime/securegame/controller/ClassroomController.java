package com.unime.securegame.controller;

import com.unime.securegame.model.ClassroomEntity;
import com.unime.securegame.repository.ClassroomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomRepository classroomRepository;

    public ClassroomController(ClassroomRepository classroomRepository) {
        this.classroomRepository = classroomRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createClassroom(@RequestParam String name, @RequestParam String teacherEmail) {
        ClassroomEntity classroom = new ClassroomEntity();
        classroom.setName(name);
        classroom.setTeacherEmail(teacherEmail);
        // Generate a random 6-character code
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        classroom.setCode(code);
        classroomRepository.save(classroom);
        return ResponseEntity.ok(classroom);
    }

    @GetMapping("/teacher/{email}")
    public ResponseEntity<?> getTeacherClassrooms(@PathVariable String email) {
        List<ClassroomEntity> classrooms = classroomRepository.findByTeacherEmail(email);
        return ResponseEntity.ok(classrooms);
    }
}
