package com.unime.securegame.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Category A — CRUD entity for the game leaderboard.
 *
 * Records a player's best score for a specific scenario.
 * The score is computed by the SimulationEngine based on:
 *   - Session outcome (SUCCESS > MFA_CHALLENGE > DENIED)
 *   - Password entropy bonus
 *   - Risk score penalty (lower anomaly = higher rank)
 *   - Time-to-completion bonus
 */
@Entity
@Table(name = "leaderboard_entries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"playerUsername", "scenario_id"}))
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerUsername;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int rank;

    @Column(nullable = false)
    private String outcome; // "SUCCESS", "PARTIAL", "FAILED"

    @Column(nullable = false)
    private Instant achievedAt;

    @Column
    private int passwordEntropyBonus;

    @Column
    private int riskPenalty; // deducted points for anomalous behaviour

    public LeaderboardEntry() {}

    public LeaderboardEntry(String playerUsername, Scenario scenario, int score,
                            String outcome, int passwordEntropyBonus, int riskPenalty) {
        this.playerUsername = playerUsername;
        this.scenario = scenario;
        this.score = score;
        this.outcome = outcome;
        this.passwordEntropyBonus = passwordEntropyBonus;
        this.riskPenalty = riskPenalty;
        this.achievedAt = Instant.now();
        this.rank = 0; // computed separately
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getPlayerUsername() { return playerUsername; }
    public void setPlayerUsername(String playerUsername) { this.playerUsername = playerUsername; }
    public Scenario getScenario() { return scenario; }
    public void setScenario(Scenario scenario) { this.scenario = scenario; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Instant getAchievedAt() { return achievedAt; }
    public void setAchievedAt(Instant achievedAt) { this.achievedAt = achievedAt; }
    public int getPasswordEntropyBonus() { return passwordEntropyBonus; }
    public void setPasswordEntropyBonus(int passwordEntropyBonus) { this.passwordEntropyBonus = passwordEntropyBonus; }
    public int getRiskPenalty() { return riskPenalty; }
    public void setRiskPenalty(int riskPenalty) { this.riskPenalty = riskPenalty; }
}
