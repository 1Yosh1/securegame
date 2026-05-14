package com.unime.securegame.service;

import com.unime.securegame.model.LeaderboardEntry;
import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.LeaderboardRepository;
import com.unime.securegame.repository.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Category C — Leaderboard ranking service.
 *
 * Score computation formula (Category C orchestration):
 *   baseScore = outcome points (SUCCESS=1000, MFA_CHALLENGE=500, FAILED=100)
 *   score = baseScore + entropyBonus - riskPenalty
 *
 * After recording a score, all players in the scenario are re-ranked.
 * Only the personal best score per player per scenario is kept.
 */
@Service
public class LeaderboardService {

    private static final int SUCCESS_BASE  = 1000;
    private static final int PARTIAL_BASE  = 500;
    private static final int FAILED_BASE   = 100;
    private static final int MAX_ENTROPY_BONUS = 200;
    private static final int MAX_RISK_PENALTY  = 300;

    private final LeaderboardRepository leaderboardRepo;
    private final ScenarioRepository scenarioRepo;

    public LeaderboardService(LeaderboardRepository leaderboardRepo,
                              ScenarioRepository scenarioRepo) {
        this.leaderboardRepo = leaderboardRepo;
        this.scenarioRepo = scenarioRepo;
    }

    /**
     * Record a game result and update the leaderboard.
     *
     * @param playerUsername  Player's username
     * @param scenarioId      Scenario played
     * @param outcome         "SUCCESS", "PARTIAL", "FAILED"
     * @param entropyScore    Password entropy score (0–100)
     * @param riskScore       Risk engine score (0.0–1.0)
     * @return The updated (or newly created) leaderboard entry
     */
    @Transactional
    public LeaderboardEntry recordResult(String playerUsername, Long scenarioId,
                                         String outcome, int entropyScore, double riskScore) {
        Scenario scenario = scenarioRepo.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));

        int base = switch (outcome) {
            case "SUCCESS" -> SUCCESS_BASE;
            case "PARTIAL" -> PARTIAL_BASE;
            default -> FAILED_BASE;
        };

        int entropyBonus = (int) (entropyScore * MAX_ENTROPY_BONUS / 100.0);
        int riskPenalty  = (int) (riskScore * MAX_RISK_PENALTY);
        int totalScore   = Math.max(0, base + entropyBonus - riskPenalty);

        // Upsert: keep best score per player per scenario
        LeaderboardEntry entry = leaderboardRepo
                .findByPlayerUsernameAndScenarioId(playerUsername, scenarioId)
                .orElseGet(() -> new LeaderboardEntry(
                        playerUsername, scenario, 0, outcome, entropyBonus, riskPenalty));

        if (totalScore > entry.getScore()) {
            entry.setScore(totalScore);
            entry.setOutcome(outcome);
            entry.setPasswordEntropyBonus(entropyBonus);
            entry.setRiskPenalty(riskPenalty);
        }
        leaderboardRepo.save(entry);

        // Re-rank all players in scenario
        rerank(scenarioId);
        return entry;
    }

    /** Re-rank all entries for a scenario by score descending */
    @Transactional
    public void rerank(Long scenarioId) {
        List<LeaderboardEntry> entries =
                leaderboardRepo.findByScenarioIdOrderByScoreDesc(scenarioId);
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
        leaderboardRepo.saveAll(entries);
    }

    /** Get top 10 for a scenario */
    public List<LeaderboardEntry> getTop10(Long scenarioId) {
        return leaderboardRepo.findTop10ByScenarioIdOrderByScoreDesc(scenarioId);
    }

    /** Get all entries for a player across all scenarios */
    public List<LeaderboardEntry> getPlayerHistory(String playerUsername) {
        return leaderboardRepo.findByPlayerUsernameOrderByScoreDesc(playerUsername);
    }
}
