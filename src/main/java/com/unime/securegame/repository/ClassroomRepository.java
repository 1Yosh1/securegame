package com.unime.securegame.repository;

import com.unime.securegame.model.ClassroomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<ClassroomEntity, Long> {
    Optional<ClassroomEntity> findByCode(String code);
    List<ClassroomEntity> findByTeacherEmail(String teacherEmail);
}
