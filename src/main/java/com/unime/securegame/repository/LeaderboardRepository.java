package com.unime.securegame.repository;

import com.unime.securegame.model.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
    List<LeaderboardEntry> findByScenarioIdOrderByScoreDesc(Long scenarioId);
    List<LeaderboardEntry> findTop10ByScenarioIdOrderByScoreDesc(Long scenarioId);
    Optional<LeaderboardEntry> findByPlayerUsernameAndScenarioId(String playerUsername, Long scenarioId);
    List<LeaderboardEntry> findByPlayerUsernameOrderByScoreDesc(String playerUsername);
}
