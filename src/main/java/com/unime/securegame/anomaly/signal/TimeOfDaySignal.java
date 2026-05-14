package com.unime.securegame.anomaly.signal;

import com.unime.securegame.anomaly.AnomalySignal;
import com.unime.securegame.anomaly.SessionContext;
import org.springframework.stereotype.Component;

/**
 * Category C — Time-of-day anomaly signal.
 *
 * Computes a Z-score of the current login hour against the player's historical
 * average and standard deviation. A large Z-score = unusual login time = anomalous.
 *
 * Formula: z = |currentHour - avgLoginHour| / max(stdLoginHour, 1.0)
 * Normalized: score = min(1.0, z / Z_THRESHOLD)
 */
@Component
public class TimeOfDaySignal implements AnomalySignal {

    private static final double Z_THRESHOLD = 3.0; // Z > 3 → fully anomalous
    private static final double WEIGHT = 0.20;

    @Override
    public double score(SessionContext ctx) {
        double std = Math.max(ctx.stdLoginHour(), 1.0); // avoid division by zero
        double z = Math.abs(ctx.currentHour() - ctx.avgLoginHour()) / std;
        return Math.min(1.0, z / Z_THRESHOLD);
    }

    @Override public String name() { return "timeOfDay"; }
    @Override public double weight() { return WEIGHT; }
}
