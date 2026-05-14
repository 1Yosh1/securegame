package com.unime.securegame.risk;

import com.unime.securegame.repository.ModelWeightRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RuleBasedLayer and LogisticRegressionModel.
 * Verifies that risk scoring produces sensible outputs for known inputs.
 */
@SpringBootTest
@Transactional
class RiskScoringTest {

    @Autowired
    private RuleBasedLayer ruleLayer;

    @Autowired
    private LogisticRegressionModel lrModel;

    @Autowired
    private ModelWeightRepository weightRepo;

    // ── RuleBasedLayer ────────────────────────────────────────────────────────

    @Test
    void perfectLegit_ruleScore_isLow() {
        RiskFeature legit = new RiskFeature(100, 0, 0, 1.0, 0, 2);
        RuleBasedLayer.RuleScore score = ruleLayer.score(legit);
        assertTrue(score.score() < 0.3, "Perfect legit session should be LOW: " + score.score());
        assertEquals("LOW", score.band());
    }

    @Test
    void worstAnomaly_ruleScore_isHigh() {
        RiskFeature anomaly = new RiskFeature(0, 5000, 12, 0.0, 10, 0);
        RuleBasedLayer.RuleScore score = ruleLayer.score(anomaly);
        assertTrue(score.score() > 0.6, "Worst-case anomaly should be HIGH: " + score.score());
        assertEquals("HIGH", score.band());
    }

    @Test
    void ruleScore_alwaysInZeroToOne() {
        RiskFeature f = new RiskFeature(50, 200, 3, 0.7, 2, 1);
        RuleBasedLayer.RuleScore score = ruleLayer.score(f);
        assertTrue(score.score() >= 0.0 && score.score() <= 1.0,
                "Score must be in [0,1]: " + score.score());
    }

    @Test
    void topKIndices_returnsCorrectCount() {
        RiskFeature f = new RiskFeature(20, 3000, 8, 0.2, 5, 0);
        RuleBasedLayer.RuleScore score = ruleLayer.score(f);
        int[] top3 = score.topKIndices(3);
        assertEquals(3, top3.length);
    }

    // ── LogisticRegressionModel ───────────────────────────────────────────────

    @Test
    void lrModel_prediction_inZeroToOne() {
        RiskFeature f = new RiskFeature(60, 100, 2, 0.8, 1, 1);
        double pred = lrModel.predict(f);
        assertTrue(pred >= 0.0 && pred <= 1.0, "LR prediction must be in [0,1]: " + pred);
    }

    @Test
    void lrModel_highRiskFeatures_predictHigherThanLowRisk() {
        RiskFeature highRisk = new RiskFeature(5, 4000, 10, 0.1, 8, 0);
        RiskFeature lowRisk = new RiskFeature(90, 5, 0.5, 0.95, 0, 2);
        assertTrue(lrModel.predict(highRisk) > lrModel.predict(lowRisk),
                "High-risk features should produce higher LR score");
    }

    // ── SyntheticDataGenerator ────────────────────────────────────────────────

    @Autowired
    private SyntheticDataGenerator dataGen;

    @Test
    void syntheticData_generatesCorrectCount() {
        var samples = dataGen.generate(500, 42L);
        assertEquals(500, samples.size());
    }

    @Test
    void syntheticData_deterministic_sameSeed() {
        var a = dataGen.generate(100, 99L);
        var b = dataGen.generate(100, 99L);
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).passwordEntropy(), b.get(i).passwordEntropy());
            assertEquals(a.get(i).label(), b.get(i).label());
        }
    }

    @Test
    void syntheticData_csvHasCorrectHeader() {
        var samples = dataGen.generate(10, 1L);
        String csv = dataGen.toCsv(samples);
        assertTrue(csv.startsWith(SyntheticDataGenerator.CSV_HEADER));
    }

    @Test
    void syntheticData_csvRowCount_matchesSampleCount() {
        int n = 150;
        var samples = dataGen.generate(n, 7L);
        String csv = dataGen.toCsv(samples);
        long rows = csv.lines().count() - 1; // subtract header
        assertEquals(n, rows, "CSV should have exactly " + n + " data rows");
    }
}
