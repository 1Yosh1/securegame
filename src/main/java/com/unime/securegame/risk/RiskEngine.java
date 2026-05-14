package com.unime.securegame.risk;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Category C — Explainable Risk Engine (main orchestrator).
 *
 * Fuses the RuleBasedLayer score with the LogisticRegressionModel prediction
 * using a configurable blend ratio, then generates:
 *  1. A final risk score [0.0 – 1.0]
 *  2. A severity band (LOW / MEDIUM / HIGH)
 *  3. The top-3 contributing risk factors (explainability)
 *  4. Coach tips for the top-3 factors (pedagogical feedback)
 *
 * The fusion formula is:
 *   finalScore = α * ruleScore + (1 - α) * lrScore
 * where α = RULE_WEIGHT (default 0.6).
 *
 * This keeps the model interpretable (rules dominate) while benefiting
 * from the LR model's learned patterns.
 */
@Service
public class RiskEngine {

    /** Weight given to the hand-crafted rule layer vs the LR model */
    private static final double RULE_WEIGHT = 0.6;
    private static final int TOP_K = 3;

    private final RuleBasedLayer ruleLayer;
    private final LogisticRegressionModel lrModel;
    private final CoachFeedbackGenerator coachGen;

    public RiskEngine(RuleBasedLayer ruleLayer, LogisticRegressionModel lrModel,
                      CoachFeedbackGenerator coachGen) {
        this.ruleLayer = ruleLayer;
        this.lrModel = lrModel;
        this.coachGen = coachGen;
    }

    /**
     * Evaluate the risk of a session and produce a full RiskAssessment.
     *
     * @param feature The risk features for the current session.
     * @return A RiskAssessment with score, band, top factors, and coach tips.
     */
    public RiskAssessment evaluate(RiskFeature feature) {
        // 1. Rule-based score
        RuleBasedLayer.RuleScore ruleScore = ruleLayer.score(feature);

        // 2. Logistic regression prediction
        double lrScore = lrModel.predict(feature);

        // 3. Fuse scores
        double finalScore = RULE_WEIGHT * ruleScore.score() + (1.0 - RULE_WEIGHT) * lrScore;
        finalScore = Math.min(1.0, Math.max(0.0, finalScore));

        // 4. Determine severity band
        String band = toBand(finalScore);

        // 5. Extract top-K contributing features (from rule layer for explainability)
        int[] topIndices = ruleScore.topKIndices(TOP_K);
        String[] topFactors = new String[topIndices.length];
        for (int i = 0; i < topIndices.length; i++) {
            topFactors[i] = RuleBasedLayer.FEATURE_NAMES[topIndices[i]];
        }

        // 6. Generate coach tips for top-K factors
        List<String> tips = coachGen.generate(topFactors, band, TOP_K);

        return new RiskAssessment(finalScore, band, topFactors, tips, ruleScore.score(), lrScore);
    }

    private String toBand(double score) {
        if (score < 0.3) return "LOW";
        if (score < 0.6) return "MEDIUM";
        return "HIGH";
    }

    // ── Result record ────────────────────────────────────────────────────────

    public record RiskAssessment(
            double finalScore,
            String band,
            String[] topRiskFactors,
            List<String> coachTips,
            double ruleScore,
            double lrScore
    ) {}
}
