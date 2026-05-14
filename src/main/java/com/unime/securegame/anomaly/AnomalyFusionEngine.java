package com.unime.securegame.anomaly;

import com.unime.securegame.anomaly.signal.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Category C — Multi-signal anomaly fusion engine.
 *
 * Collects scores from all registered AnomalySignal implementations and
 * combines them using weighted summation. Each signal contributes independently.
 *
 * Final score = Σ (weight_i × score_i) / Σ weight_i
 *
 * Produces an AnomalyReport with:
 *   - fusedScore [0.0, 1.0]
 *   - alertLevel: CLEAR / WATCHLIST / CHALLENGE / BLOCK
 *   - signal breakdown (name → score)
 *   - alert reason message
 */
@Service
public class AnomalyFusionEngine {

    private final List<AnomalySignal> signals;

    public AnomalyFusionEngine(TimeOfDaySignal timeSignal,
                                GeoDistanceSignal geoSignal,
                                UserAgentSignal uaSignal,
                                FailedAttemptSignal failSignal,
                                DeviceTrustSignal deviceSignal) {
        this.signals = List.of(timeSignal, geoSignal, uaSignal, failSignal, deviceSignal);
    }

    /**
     * Evaluate all signals and produce a fused anomaly report.
     */
    public AnomalyReport evaluate(SessionContext ctx) {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (AnomalySignal signal : signals) {
            double score = signal.score(ctx);
            breakdown.put(signal.name(), score);
            weightedSum += signal.weight() * score;
            totalWeight += signal.weight();
        }

        double fusedScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        fusedScore = Math.min(1.0, Math.max(0.0, fusedScore));

        AlertLevel level = toAlertLevel(fusedScore);
        String reason = buildReason(breakdown, level);

        return new AnomalyReport(fusedScore, level, breakdown, reason,
                ctx.playerUsername(), ctx.scenarioId());
    }

    private AlertLevel toAlertLevel(double score) {
        if (score < 0.25) return AlertLevel.CLEAR;
        if (score < 0.50) return AlertLevel.WATCHLIST;
        if (score < 0.75) return AlertLevel.CHALLENGE;
        return AlertLevel.BLOCK;
    }

    private String buildReason(Map<String, Double> breakdown, AlertLevel level) {
        return switch (level) {
            case CLEAR -> "Session appears normal. No unusual patterns detected.";
            case WATCHLIST -> {
                String topSignal = topSignalName(breakdown);
                yield "Slight anomaly: " + topSignal + " shows elevated score. Monitoring.";
            }
            case CHALLENGE -> {
                String topSignal = topSignalName(breakdown);
                yield "Step-up authentication required. Elevated " + topSignal + " anomaly.";
            }
            case BLOCK -> {
                String topSignal = topSignalName(breakdown);
                yield "Session blocked. Multiple high anomaly signals. Top factor: " + topSignal + ".";
            }
        };
    }

    private String topSignalName(Map<String, Double> breakdown) {
        return breakdown.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
    }

    // ── Nested types ─────────────────────────────────────────────────────────

    public enum AlertLevel { CLEAR, WATCHLIST, CHALLENGE, BLOCK }

    public record AnomalyReport(
            double fusedScore,
            AlertLevel alertLevel,
            Map<String, Double> signalBreakdown,
            String reason,
            String playerUsername,
            Long scenarioId
    ) {
        public boolean requiresAction() {
            return alertLevel == AlertLevel.CHALLENGE || alertLevel == AlertLevel.BLOCK;
        }
    }
}
