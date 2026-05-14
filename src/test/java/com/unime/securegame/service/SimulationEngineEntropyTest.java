package com.unime.securegame.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulationEngine — no Spring context, pure logic tests.
 */
class SimulationEngineEntropyTest {

    private final SimulationEngine engine = new SimulationEngine(null, null, null);

    @Test
    void emptyPassword_returnsZeroEntropy() {
        assertEquals(0.0, engine.calculateShannonEntropy(""), 0.001);
    }

    @Test
    void singleChar_returnsZeroEntropy() {
        assertEquals(0.0, engine.calculateShannonEntropy("aaaa"), 0.001);
    }

    @Test
    void allUniqueChars_returnsMaxEntropy() {
        // "abcd" → each char appears once, entropy = log2(4) = 2.0
        double entropy = engine.calculateShannonEntropy("abcd");
        assertEquals(2.0, entropy, 0.001);
    }

    @Test
    void complexPassword_hasHighEntropyScore() {
        // A strong password with mixed chars
        double entropy = engine.calculateShannonEntropy("P@ssw0rd!XY");
        int score = engine.entropyToScore(entropy);
        assertTrue(score > 50, "Strong password should have score > 50, got: " + score);
    }

    @Test
    void weakPassword_hasLowEntropyScore() {
        double entropy = engine.calculateShannonEntropy("aa");
        int score = engine.entropyToScore(entropy);
        assertEquals(0, score, "All-same-char password has 0 entropy score");
    }

    @Test
    void entropyScoreCappedAt100() {
        // Extremely diverse string
        String allChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%";
        double entropy = engine.calculateShannonEntropy(allChars);
        int score = engine.entropyToScore(entropy);
        assertTrue(score <= 100, "Score must never exceed 100");
    }
}
