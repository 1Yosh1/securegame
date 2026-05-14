package com.unime.securegame.repository;

import com.unime.securegame.model.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByScenarioIdOrderByTimestampAsc(Long scenarioId);
    List<EventLog> findByPlayerUsernameOrderByTimestampDesc(String playerUsername);
}
