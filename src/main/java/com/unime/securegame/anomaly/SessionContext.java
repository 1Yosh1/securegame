package com.unime.securegame.anomaly;

import java.time.Instant;

/**
 * Value object carrying all observable attributes of an incoming session.
 * Passed to each AnomalySignal for independent scoring.
 */
public record SessionContext(
        String playerUsername,
        Long scenarioId,
        double currentLat,
        double currentLon,
        double knownLat,          // last known location latitude
        double knownLon,          // last known location longitude
        int currentHour,          // 0–23 (local hour of login)
        double avgLoginHour,      // historical average login hour
        double stdLoginHour,      // historical std-dev of login hour
        String currentUserAgent,
        String knownUserAgent,    // last known user-agent string
        int failStreak,           // consecutive failures in this session
        double deviceTrustScore,  // 0.0–1.0; decays if unknown device
        Instant sessionTime
) {
    /** Convenience constructor with default trust score */
    public SessionContext(String playerUsername, Long scenarioId,
                          double currentLat, double currentLon,
                          double knownLat, double knownLon,
                          int currentHour, double avgLoginHour, double stdLoginHour,
                          String currentUserAgent, String knownUserAgent,
                          int failStreak) {
        this(playerUsername, scenarioId, currentLat, currentLon, knownLat, knownLon,
             currentHour, avgLoginHour, stdLoginHour, currentUserAgent, knownUserAgent,
             failStreak, 1.0, Instant.now());
    }
}
