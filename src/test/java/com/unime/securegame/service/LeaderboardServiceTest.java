package com.unime.securegame.service;

import com.unime.securegame.model.LeaderboardEntry;
import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LeaderboardService score computation and ranking.
 */
@SpringBootTest
@Transactional
class LeaderboardServiceTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private ScenarioRepository scenarioRepo;

    private Scenario scenario;

    @BeforeEach
    void setUp() {
        scenario = scenarioRepo.save(new Scenario("Leaderboard Test", 3, 40, true));
    }

    @Test
    void successResult_producesHighScore() {
        LeaderboardEntry entry = leaderboardService.recordResult(
                "alice", scenario.getId(), "SUCCESS", 80, 0.1);

        // base=1000, entropy=(80*200/100)=160, risk=(0.1*300)=30 → 1130
        assertTrue(entry.getScore() > 1000, "SUCCESS with good entropy should score > 1000");
        assertEquals("SUCCESS", entry.getOutcome());
    }

    @Test
    void failedResult_producesLowScore() {
        LeaderboardEntry entry = leaderboardService.recordResult(
                "bob", scenario.getId(), "FAILED", 20, 0.9);

        assertTrue(entry.getScore() < 200, "FAILED with weak entropy and high risk should score low");
    }

    @Test
    void personalBest_onlyKeepsBestScore() {
        leaderboardService.recordResult("alice", scenario.getId(), "FAILED", 20, 0.9);
        leaderboardService.recordResult("alice", scenario.getId(), "SUCCESS", 90, 0.05);

        List<LeaderboardEntry> history = leaderboardService.getPlayerHistory("alice");
        assertEquals(1, history.stream()
                .filter(e -> e.getScenario().getId().equals(scenario.getId()))
                .count(), "Only one entry per player per scenario");
        assertEquals("SUCCESS", history.get(0).getOutcome(), "Best outcome should be kept");
    }

    @Test
    void rerank_assignsCorrectRanks() {
        leaderboardService.recordResult("alice", scenario.getId(), "SUCCESS", 90, 0.05);
        leaderboardService.recordResult("bob", scenario.getId(), "PARTIAL", 60, 0.3);
        leaderboardService.recordResult("charlie", scenario.getId(), "FAILED", 10, 0.9);

        List<LeaderboardEntry> top10 = leaderboardService.getTop10(scenario.getId());
        assertEquals(3, top10.size());
        assertEquals(1, top10.get(0).getRank()); // alice is rank 1
        assertTrue(top10.get(0).getScore() > top10.get(1).getScore(), "Top ranked has highest score");
    }

    @Test
    void scoreCannotBeNegative() {
        // Worst case: FAILED + no entropy + max risk penalty
        LeaderboardEntry entry = leaderboardService.recordResult(
                "zero", scenario.getId(), "FAILED", 0, 1.0);
        assertTrue(entry.getScore() >= 0, "Score should never be negative");
    }
}
