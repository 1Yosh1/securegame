package com.unime.securegame.repository;

import com.unime.securegame.model.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    List<Scenario> findAllByDeletedFalseOrderByIdAsc();
    Optional<Scenario> findByIdAndDeletedFalse(Long id);
}
