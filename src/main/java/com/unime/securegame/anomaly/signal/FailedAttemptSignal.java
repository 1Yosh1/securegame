package com.unime.securegame.anomaly.signal;

import com.unime.securegame.anomaly.AnomalySignal;
import com.unime.securegame.anomaly.SessionContext;
import org.springframework.stereotype.Component;

/**
 * Category C — Failed attempt trend anomaly signal.
 *
 * Evaluates the consecutive failure streak in the current session.
 * Threshold: 0 fails = 0.0, 5+ fails = 1.0 (linear interpolation).
 * Override: streak >= 5 → score = 1.0
 */
@Component
public class FailedAttemptSignal implements AnomalySignal {

    private static final double WEIGHT = 0.15;
    private static final int OVERRIDE_THRESHOLD = 5;

    @Override
    public double score(SessionContext ctx) {
        if (ctx.failStreak() >= OVERRIDE_THRESHOLD) return 1.0;
        return Math.min(1.0, ctx.failStreak() / (double) OVERRIDE_THRESHOLD);
    }

    @Override public String name() { return "failedAttempts"; }
    @Override public double weight() { return WEIGHT; }
}
