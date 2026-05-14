package com.unime.securegame.anomaly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AnomalyFusionEngine.
 */
@SpringBootTest
@Transactional
class AnomalyFusionEngineTest {

    @Autowired
    private AnomalyFusionEngine fusionEngine;

    @Test
    void clearSession_producesAlertLevelClear() {
        SessionContext ctx = clear();
        AnomalyFusionEngine.AnomalyReport report = fusionEngine.evaluate(ctx);

        assertTrue(report.fusedScore() < 0.25, "Clean session should score < 0.25");
        assertEquals(AnomalyFusionEngine.AlertLevel.CLEAR, report.alertLevel());
        assertFalse(report.requiresAction());
    }

    @Test
    void blockedSession_producesAlertLevelBlock() {
        SessionContext ctx = new SessionContext("attacker", 1L,
                55.75, 37.61, 38.19, 15.55, // Moscow → Messina
                3, 9.0, 1.0,                // 3 AM (unusual)
                "curl/7.0", "Mozilla/5.0 Windows NT", // different UA
                7, 0.1, java.time.Instant.now());     // 7 fails, untrusted device

        AnomalyFusionEngine.AnomalyReport report = fusionEngine.evaluate(ctx);
        assertTrue(report.fusedScore() > 0.6, "Attack session should score > 0.6");
        assertTrue(report.requiresAction());
    }

    @Test
    void report_breakdownContainsAllSignals() {
        AnomalyFusionEngine.AnomalyReport report = fusionEngine.evaluate(clear());
        assertTrue(report.signalBreakdown().containsKey("geoDistance"));
        assertTrue(report.signalBreakdown().containsKey("timeOfDay"));
        assertTrue(report.signalBreakdown().containsKey("userAgentMismatch"));
        assertTrue(report.signalBreakdown().containsKey("failedAttempts"));
        assertTrue(report.signalBreakdown().containsKey("deviceTrust"));
    }

    @Test
    void report_fusedScore_alwaysInZeroToOne() {
        AnomalyFusionEngine.AnomalyReport report = fusionEngine.evaluate(clear());
        assertTrue(report.fusedScore() >= 0.0 && report.fusedScore() <= 1.0);
    }

    @Test
    void report_reasonIsNotEmpty() {
        AnomalyFusionEngine.AnomalyReport report = fusionEngine.evaluate(clear());
        assertNotNull(report.reason());
        assertFalse(report.reason().isBlank());
    }

    private SessionContext clear() {
        return new SessionContext("alice", 1L,
                38.19, 15.55, 38.19, 15.55, // same location
                9, 9.0, 0.5,                // usual time
                "Mozilla/5.0 Chrome", "Mozilla/5.0 Chrome", // same UA
                0, 1.0, java.time.Instant.now());
    }
}
