package com.unime.securegame.repository;

import com.unime.securegame.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    java.util.List<UserEntity> findByClassroomCode(String classroomCode);
    java.util.List<UserEntity> findByRole(String role);
}
