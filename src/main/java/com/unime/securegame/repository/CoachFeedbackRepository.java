package com.unime.securegame.repository;

import com.unime.securegame.model.CoachFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoachFeedbackRepository extends JpaRepository<CoachFeedback, Long> {
    List<CoachFeedback> findByRiskFactorAndSeverityBand(String riskFactor, String severityBand);
    List<CoachFeedback> findByRiskFactor(String riskFactor);
}
