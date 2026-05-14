package com.unime.securegame.controller;

import com.unime.securegame.anomaly.AnomalyFusionEngine;
import com.unime.securegame.anomaly.SessionContext;
import com.unime.securegame.model.LeaderboardEntry;
import com.unime.securegame.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final AnomalyFusionEngine fusionEngine;
    private final LeaderboardService leaderboardService;

    public GameController(AnomalyFusionEngine fusionEngine,
                          LeaderboardService leaderboardService) {
        this.fusionEngine = fusionEngine;
        this.leaderboardService = leaderboardService;
    }

    /**
     * Evaluate a session for anomalies.
     * Body: { "playerUsername":"alice", "scenarioId":1, "currentLat":37.7, "currentLon":-122.4,
     *          "knownLat":37.7, "knownLon":-122.4, "currentHour":9, "avgLoginHour":9.5,
     *          "stdLoginHour":1.0, "currentUserAgent":"Mozilla/5.0...",
     *          "knownUserAgent":"Mozilla/5.0...", "failStreak":0 }
     */
    @PostMapping("/anomaly/evaluate")
    public ResponseEntity<AnomalyFusionEngine.AnomalyReport> evaluate(
            @RequestBody SessionContextRequest req) {
        SessionContext ctx = new SessionContext(
                req.playerUsername(), req.scenarioId(),
                req.currentLat(), req.currentLon(),
                req.knownLat(), req.knownLon(),
                req.currentHour(), req.avgLoginHour(), req.stdLoginHour(),
                req.currentUserAgent(), req.knownUserAgent(),
                req.failStreak());
        return ResponseEntity.ok(fusionEngine.evaluate(ctx));
    }

    /** Record a game result and update leaderboard */
    @PostMapping("/result")
    public ResponseEntity<LeaderboardEntry> recordResult(@RequestBody GameResultRequest req) {
        LeaderboardEntry entry = leaderboardService.recordResult(
                req.playerUsername(), req.scenarioId(), req.outcome(),
                req.entropyScore(), req.riskScore());
        return ResponseEntity.ok(entry);
    }

    /** Get top 10 leaderboard for a scenario */
    @GetMapping("/leaderboard/{scenarioId}")
    public List<LeaderboardEntry> getLeaderboard(@PathVariable Long scenarioId) {
        return leaderboardService.getTop10(scenarioId);
    }

    /** Get a player's full score history */
    @GetMapping("/leaderboard/player/{username}")
    public List<LeaderboardEntry> getPlayerHistory(@PathVariable String username) {
        return leaderboardService.getPlayerHistory(username);
    }

    // ── Request DTOs ─────────────────────────────────────────────────────────

    public record SessionContextRequest(
            String playerUsername, Long scenarioId,
            double currentLat, double currentLon,
            double knownLat, double knownLon,
            int currentHour, double avgLoginHour, double stdLoginHour,
            String currentUserAgent, String knownUserAgent,
            int failStreak
    ) {}

    public record GameResultRequest(
            String playerUsername, Long scenarioId,
            String outcome, int entropyScore, double riskScore
    ) {}
}
