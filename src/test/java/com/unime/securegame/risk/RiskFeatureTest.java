package com.unime.securegame.risk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RiskFeature normalization (toVector).
 * These are pure unit tests — no Spring context needed.
 */
class RiskFeatureTest {

    @Test
    void perfectLegitSession_producesLowRiskVector() {
        RiskFeature f = new RiskFeature(100, 0, 0, 1.0, 0, 2);
        double[] v = f.toVector();

        assertEquals(0.0, v[0], 0.001, "strong password contributes no entropy risk");
        assertEquals(0.0, v[1], 0.001, "geo normalized");
        assertEquals(0.0, v[2], 0.001, "time deviation normalized");
        assertEquals(0.0, v[3], 0.001, "UA similarity inverted (0 = identical)");
        assertEquals(0.0, v[4], 0.001, "fail streak normalized");
        assertEquals(0.0, v[5], 0.001, "mfa inverted (0 = WebAuthn = best)");
    }

    @Test
    void worstCaseAnomalousSession_producesHighRiskVector() {
        RiskFeature f = new RiskFeature(0, 5000, 12, 0.0, 10, 0);
        double[] v = f.toVector();

        assertEquals(1.0, v[0], 0.001, "entropy = 0 → full risk contribution");
        assertEquals(1.0, v[1], 0.001, "geo capped at 1.0");
        assertEquals(1.0, v[2], 0.001, "time deviation capped at 1.0");
        assertEquals(1.0, v[3], 0.001, "UA similarity inverted → 1.0");
        assertEquals(1.0, v[4], 0.001, "fail streak capped at 1.0");
        assertEquals(1.0, v[5], 0.001, "mfa = None inverted → 1.0");
    }

    @Test
    void geoDistance_cappedAt5000km() {
        RiskFeature f = new RiskFeature(50, 10000, 0, 1.0, 0, 1);
        double[] v = f.toVector();
        assertEquals(1.0, v[1], 0.001, "geo > 5000 km capped at 1.0");
    }

    @Test
    void strongerPassword_neverIncreasesRiskVector() {
        double weakPasswordRisk = new RiskFeature(20, 0, 0, 1.0, 0, 2).toVector()[0];
        double strongPasswordRisk = new RiskFeature(90, 0, 0, 1.0, 0, 2).toVector()[0];
        assertTrue(weakPasswordRisk > strongPasswordRisk);
    }

    @Test
    void riskVector_clampsOutOfRangeInputs() {
        double[] vector = new RiskFeature(120, -100, -2, 1.5, -1, 3).toVector();
        for (double value : vector) {
            assertTrue(value >= 0.0 && value <= 1.0);
        }
    }

    @Test
    void nonFiniteRiskInputsNeverProduceNanContributions() {
        double[] vector = new RiskFeature(50, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NaN, 2, 1).toVector();
        for (double value : vector) {
            assertTrue(Double.isFinite(value));
            assertTrue(value >= 0.0 && value <= 1.0);
        }
    }

    @Test
    void inferenceConstructor_setsLabelToNegativeOne() {
        RiskFeature f = new RiskFeature(75, 10, 1.0, 0.9, 0, 1);
        assertEquals(-1, f.label());
    }
}
