package com.unime.securegame.anomaly.signal;

import com.unime.securegame.anomaly.AnomalySignal;
import com.unime.securegame.anomaly.SessionContext;
import org.springframework.stereotype.Component;

/**
 * Category C — Geo-distance anomaly signal.
 *
 * Computes the Haversine great-circle distance between the current login location
 * and the player's last known location.
 *
 * Thresholds (Category B uses GeoIP for lat/lon lookup — this class does the math):
 *   0–50 km      → score ≈ 0 (same city / commute)
 *   50–500 km    → score scales linearly
 *   500+ km      → score approaches 1.0 (different country / continent)
 *   Override: > 2000 km → score = 1.0 (hard anomaly)
 */
@Component
public class GeoDistanceSignal implements AnomalySignal {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double OVERRIDE_THRESHOLD_KM = 2000.0;
    private static final double SCALE_KM = 500.0;
    private static final double WEIGHT = 0.30;

    @Override
    public double score(SessionContext ctx) {
        double distKm = haversine(ctx.knownLat(), ctx.knownLon(),
                                  ctx.currentLat(), ctx.currentLon());

        if (distKm > OVERRIDE_THRESHOLD_KM) return 1.0; // hard override
        return Math.min(1.0, Math.max(0.0, (distKm - 50.0) / SCALE_KM));
    }

    /** Haversine formula for great-circle distance between two lat/lon points */
    public double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.asin(Math.sqrt(a));
    }

    @Override public String name() { return "geoDistance"; }
    @Override public double weight() { return WEIGHT; }
}
