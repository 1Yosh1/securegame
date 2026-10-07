package com.unime.securegame.anomaly;

import com.unime.securegame.anomaly.signal.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for each individual AnomalySignal.
 * Pure Java — no Spring context.
 */
class AnomalySignalUnitTest {

    private TimeOfDaySignal timeSignal;
    private AnomalyFusionEngine fusionEngine;
    private GeoDistanceSignal geoSignal;
    private UserAgentSignal uaSignal;
    private FailedAttemptSignal failSignal;
    private DeviceTrustSignal trustSignal;

    @BeforeEach
    void setUp() {
        timeSignal  = new TimeOfDaySignal();
        geoSignal   = new GeoDistanceSignal();
        uaSignal    = new UserAgentSignal();
        failSignal  = new FailedAttemptSignal();
        trustSignal = new DeviceTrustSignal();
        fusionEngine = new AnomalyFusionEngine(List.of(timeSignal, geoSignal, uaSignal, failSignal, trustSignal));
    }

    // ── TimeOfDaySignal ───────────────────────────────────────────────────────

    @Test
    void time_usualLoginHour_producesZeroScore() {
        SessionContext ctx = ctx(9, 9.0, 1.0, 0, 1.0, "UA", "UA");
        assertEquals(0.0, timeSignal.score(ctx), 0.001);
    }

    @Test
    void time_extremelyUnusualHour_producesOneScore() {
        // Z = |3 - 9| / 1.0 = 6.0, capped at Z_THRESHOLD=3 → 1.0
        SessionContext ctx = ctx(3, 9.0, 1.0, 0, 1.0, "UA", "UA");
        assertEquals(1.0, timeSignal.score(ctx), 0.001);
    }

    @Test
    void time_slightDeviation_producesMidScore() {
        // Z = |11 - 9| / 1.0 = 2.0, normalized 2/3 ≈ 0.667
        SessionContext ctx = ctx(11, 9.0, 1.0, 0, 1.0, "UA", "UA");
        assertEquals(0.666, timeSignal.score(ctx), 0.01);
    }

    // ── GeoDistanceSignal ────────────────────────────────────────────────────

    @Test
    void geo_sameLocation_producesZeroScore() {
        // Messina, Italy
        SessionContext ctx = geoCtx(38.19, 15.55, 38.19, 15.55);
        assertEquals(0.0, geoSignal.score(ctx), 0.001);
    }

    @Test
    void geo_sameCity_producesZeroScore() {
        // Catania to Messina (≈ 90km)
        SessionContext ctx = geoCtx(37.50, 15.09, 38.19, 15.55);
        assertTrue(geoSignal.score(ctx) < 0.2, "Same-region should be low risk");
    }

    @Test
    void geo_differentCountry_producesHighScore() {
        // Messina to Moscow (≈ 3400 km) → score = 1.0
        SessionContext ctx = geoCtx(38.19, 15.55, 55.75, 37.61);
        assertEquals(1.0, geoSignal.score(ctx), 0.001);
    }

    @Test
    void geo_haversine_distanceIsAccurate() {
        // Messina (38.19, 15.55) to Rome (41.90, 12.49) ≈ 488 km
        double dist = geoSignal.haversine(38.19, 15.55, 41.90, 12.49);
        assertTrue(dist > 450 && dist < 530, "Expected ~488km, got: " + dist);
    }

    // ── UserAgentSignal ──────────────────────────────────────────────────────

    @Test
    void ua_identicalStrings_producesZeroScore() {
        SessionContext ctx = ctx(9, 9.0, 1.0, 0, 1.0, "Mozilla/5.0", "Mozilla/5.0");
        assertEquals(0.0, uaSignal.score(ctx), 0.001);
    }

    @Test
    void ua_completelyDifferentStrings_producesHighScore() {
        SessionContext ctx = ctx(9, 9.0, 1.0, 0, 1.0, "curl/7.0", "Mozilla/5.0 Windows NT");
        assertTrue(uaSignal.score(ctx) > 0.3, "Very different UAs should score > 0.3");
    }

    @Test
    void ua_jaroWinkler_identicalStrings_returnsOne() {
        assertEquals(1.0, uaSignal.jaroWinkler("test", "test"), 0.001);
    }

    // ── FailedAttemptSignal ──────────────────────────────────────────────────

    @Test
    void fail_zeroStreak_producesZero() {
        SessionContext ctx = ctx(9, 9.0, 1.0, 0, 1.0, "UA", "UA");
        assertEquals(0.0, failSignal.score(ctx), 0.001);
    }

    @Test
    void fail_fiveOrMore_producesOne() {
        SessionContext ctx = ctx(9, 9.0, 1.0, 5, 1.0, "UA", "UA");
        assertEquals(1.0, failSignal.score(ctx), 0.001);
    }

    // ── DeviceTrustSignal ────────────────────────────────────────────────────

    @Test
    void trust_fullTrust_producesZero() {
        SessionContext ctx = ctx(9, 9.0, 1.0, 0, 1.0, "UA", "UA");
        assertEquals(0.0, trustSignal.score(ctx), 0.001);
    }

    @Test
    void trust_noTrust_producesOne() {
        SessionContext ctx = ctx(9, 9.0, 1.0, 0, 0.0, "UA", "UA");
        assertEquals(1.0, trustSignal.score(ctx), 0.001);
    }

    @Test
    void fusionEngine_preservesSignalPolymorphismAndReturnsStableBreakdown() {
        SessionContext context = ctx(9, 9.0, 1.0, 0, 1.0, "UA", "UA");

        var first = fusionEngine.evaluate(context);
        var second = fusionEngine.evaluate(context);

        assertEquals(first.signalBreakdown(), second.signalBreakdown());
        assertEquals(5, first.signalBreakdown().size());
        assertEquals(0.0, first.fusedScore(), 0.001);
    }

    @Test
    void fusionEngineRejectsInvalidSignalScoresAtTheBoundary() {
        AnomalySignal invalidSignal = mock(AnomalySignal.class);
        when(invalidSignal.name()).thenReturn("invalid");
        when(invalidSignal.weight()).thenReturn(1.0);
        when(invalidSignal.score(org.mockito.ArgumentMatchers.any())).thenReturn(Double.NaN);
        AnomalyFusionEngine engine = new AnomalyFusionEngine(List.of(invalidSignal));

        assertThrows(IllegalStateException.class, () -> engine.evaluate(ctx(9, 9.0, 1.0, 0, 1.0, "UA", "UA")));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SessionContext ctx(int hour, double avgHour, double stdHour,
                                int failStreak, double trust,
                                String currentUA, String knownUA) {
        return new SessionContext("player", 1L,
                38.19, 15.55, 38.19, 15.55, // same location
                hour, avgHour, stdHour,
                currentUA, knownUA, failStreak, trust, java.time.Instant.now());
    }

    private SessionContext geoCtx(double knownLat, double knownLon,
                                   double curLat, double curLon) {
        return new SessionContext("player", 1L,
                curLat, curLon, knownLat, knownLon,
                9, 9.0, 1.0, "UA", "UA", 0, 1.0, java.time.Instant.now());
    }
}
