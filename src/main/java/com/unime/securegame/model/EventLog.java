package com.unime.securegame.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a discrete event in a simulation run.
 * This is an append-only log — events are never updated or deleted.
 * Category A: CRUD (append + read only)
 */
@Entity
@Table(name = "event_logs")
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scenarioId;
    private String playerUsername;

    @Column(nullable = false)
    private String eventType; // e.g. LOGIN_ATTEMPT, MFA_CHALLENGE, LOCKOUT, SUCCESS, DENIED

    private String details; // JSON string with event-specific payload

    private double riskScore;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();

    public EventLog() {}

    public EventLog(Long scenarioId, String playerUsername, String eventType, String details, double riskScore) {
        this.scenarioId = scenarioId;
        this.playerUsername = playerUsername;
        this.eventType = eventType;
        this.details = details;
        this.riskScore = riskScore;
    }

    // Getters only (no setters except for details — log is immutable after creation)

    public Long getId() { return id; }
    public Long getScenarioId() { return scenarioId; }
    public String getPlayerUsername() { return playerUsername; }
    public String getEventType() { return eventType; }
    public String getDetails() { return details; }
    public double getRiskScore() { return riskScore; }
    public Instant getTimestamp() { return timestamp; }
}
