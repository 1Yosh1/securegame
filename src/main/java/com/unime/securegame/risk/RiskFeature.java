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

    /** Convert to a double[] feature vector for the logistic regression model */
    public double[] toVector() {
        return new double[]{
                passwordEntropy / 100.0,
                Math.min(geoDistanceKm / 5000.0, 1.0),    // normalize to [0,1], cap at 5000 km
                Math.min(timeDeviationHours / 12.0, 1.0),  // normalize to [0,1], cap at 12 hours
                1.0 - uaSimilarity,                         // invert: 0 = identical UA (good), 1 = different (bad)
                Math.min(failStreak / 10.0, 1.0),           // normalize to [0,1], cap at 10 fails
                (2.0 - mfaType) / 2.0                       // invert: 0 = WebAuthn (good), 1 = None (bad)
        };
    }
}
