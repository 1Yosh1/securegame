package com.unime.securegame.anomaly.signal;

import com.unime.securegame.anomaly.AnomalySignal;
import com.unime.securegame.anomaly.SessionContext;
import org.springframework.stereotype.Component;

/**
 * Category C — Device trust score decay signal.
 *
 * A device starts with trust = 1.0 when registered and decays toward 0 if:
 *   - The device hasn't been seen recently
 *   - The device was flagged in a previous anomalous session
 *
 * The trust score is stored and managed externally (in PlayerProfile or DeviceTrust entity).
 * This signal inverts it: score = 1.0 - deviceTrustScore.
 */
@Component
public class DeviceTrustSignal implements AnomalySignal {

    private static final double WEIGHT = 0.15;

    @Override
    public double score(SessionContext ctx) {
        // Invert: high trust = low anomaly score
        return Math.min(1.0, Math.max(0.0, 1.0 - ctx.deviceTrustScore()));
    }

    @Override public String name() { return "deviceTrust"; }
    @Override public double weight() { return WEIGHT; }
}
