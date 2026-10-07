package com.unime.securegame.risk;

/**
 * Category C — Immutable data record carrying all features for risk scoring.
 *
 * Feature descriptions:
 *  - passwordEntropy:    Shannon entropy score (0–100)
 *  - geoDistanceKm:      Distance from last known location in km (0 = same, large = anomalous)
 *  - timeDeviationHours: Absolute deviation from user's typical login hour (0–12)
 *  - uaSimilarity:       String similarity of current vs stored User-Agent (0.0–1.0, 1.0 = identical)
 *  - failStreak:         Number of consecutive failed attempts in this session (0+)
 *  - mfaType:            0 = None, 1 = TOTP, 2 = WebAuthn (higher = more secure)
 *  - label:              Ground truth: 0 = legitimate, 1 = anomalous (used for training/evaluation)
 */
public record RiskFeature(
        int passwordEntropy,
        double geoDistanceKm,
        double timeDeviationHours,
        double uaSimilarity,
        int failStreak,
        int mfaType,
        int label
) {
    /** Convenience constructor without label (for inference, not training) */
    public RiskFeature(int passwordEntropy, double geoDistanceKm, double timeDeviationHours,
                       double uaSimilarity, int failStreak, int mfaType) {
        this(passwordEntropy, geoDistanceKm, timeDeviationHours, uaSimilarity, failStreak, mfaType, -1);
    }

    /** Convert to a double[] risk vector for both the rule layer and classifier.
     * Every component points in the same direction: 0 is lower risk, 1 is higher risk.
     */
    public double[] toVector() {
        return new double[]{
                1.0 - clamp(passwordEntropy / 100.0),
                clamp(geoDistanceKm / 5000.0),
                clamp(timeDeviationHours / 12.0),
                1.0 - clamp(uaSimilarity),
                clamp(failStreak / 10.0),
                clamp((2.0 - mfaType) / 2.0)
        };
    }

    private static double clamp(double value) {
        if (Double.isNaN(value)) return 1.0;
        return Math.max(0.0, Math.min(value, 1.0));
    }
}
