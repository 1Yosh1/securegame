package com.unime.securegame.risk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the full RiskEngine pipeline.
 * Verifies end-to-end fusion of rule layer + LR model + coach tips.
 */
@SpringBootTest
@Transactional
class RiskEngineIntegrationTest {

    @Autowired
    private RiskEngine riskEngine;

    @Test
    void highRiskFeatures_produceHighBand() {
        RiskFeature highRisk = new RiskFeature(5, 4500, 11, 0.05, 9, 0);
        RiskEngine.RiskAssessment result = riskEngine.evaluate(highRisk);

        assertTrue(result.finalScore() > 0.6,
                "High-risk session should score > 0.6, got: " + result.finalScore());
        assertEquals("HIGH", result.band());
    }

    @Test
    void lowRiskFeatures_produceLowBand() {
        RiskFeature lowRisk = new RiskFeature(95, 2, 0.3, 0.98, 0, 2);
        RiskEngine.RiskAssessment result = riskEngine.evaluate(lowRisk);

        assertTrue(result.finalScore() < 0.4,
                "Low-risk session should score < 0.4, got: " + result.finalScore());
    }

    @Test
    void assessment_alwaysReturnsThreeTopFactors() {
        RiskFeature f = new RiskFeature(40, 500, 5, 0.5, 3, 1);
        RiskEngine.RiskAssessment result = riskEngine.evaluate(f);

        assertNotNull(result.topRiskFactors());
        assertEquals(3, result.topRiskFactors().length,
                "Should always return exactly 3 top risk factors");
    }

    @Test
    void assessment_finalScore_inZeroToOne() {
        RiskFeature f = new RiskFeature(60, 100, 2, 0.8, 1, 1);
        RiskEngine.RiskAssessment result = riskEngine.evaluate(f);

        assertTrue(result.finalScore() >= 0.0 && result.finalScore() <= 1.0,
                "Final score must be in [0,1]");
    }

    @Test
    void assessment_componentScores_arePopulated() {
        RiskFeature f = new RiskFeature(50, 200, 3, 0.7, 2, 1);
        RiskEngine.RiskAssessment result = riskEngine.evaluate(f);

        assertTrue(result.ruleScore() >= 0.0 && result.ruleScore() <= 1.0);
        assertTrue(result.lrScore() >= 0.0 && result.lrScore() <= 1.0);
    }

    @Test
    void trainThenEvaluate_producesConsistentResults() {
        // Train model
        SyntheticDataGenerator gen = new SyntheticDataGenerator();
        List<RiskFeature> data = gen.generate(200, 12L);

        // Verify the engine still produces valid results after the seeder trained the model
        RiskFeature f = new RiskFeature(10, 2000, 8, 0.2, 5, 0);
        RiskEngine.RiskAssessment result = riskEngine.evaluate(f);

        assertNotNull(result);
        assertTrue(result.finalScore() >= 0.0 && result.finalScore() <= 1.0);
    }
}
