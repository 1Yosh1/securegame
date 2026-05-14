package com.unime.securegame.risk;

import com.unime.securegame.model.ModelWeight;
import com.unime.securegame.repository.ModelWeightRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Category C — Hand-crafted weighted rule layer.
 *
 * Each feature is evaluated independently against configurable thresholds.
 * The weights are stored in the DB (ModelWeight entity, Category A) and
 * loaded at scoring time.
 *
 * Score formula: score_i = weight_i × normalized_feature_i
 * Final score  = Σ score_i / Σ weight_i (normalized to [0,1])
 *
 * Interpretation:
 *   0.0 – 0.3 → LOW risk
 *   0.3 – 0.6 → MEDIUM risk
 *   0.6 – 1.0 → HIGH risk
 */
@Component
public class RuleBasedLayer {

    /** Feature names in the same order as RiskFeature.toVector() */
    public static final String[] FEATURE_NAMES = {
            "passwordEntropy",   // index 0 (inverted: low entropy = high risk)
            "geoDistanceKm",     // index 1
            "timeDeviationHours",// index 2
            "uaSimilarity",      // index 3 (inverted in toVector: high diff = high risk)
            "failStreak",        // index 4
            "mfaType"            // index 5 (inverted in toVector: no MFA = high risk)
    };

    /** Default weights used when DB has no entries (bootstrap / first run) */
    private static final double[] DEFAULT_WEIGHTS = {0.15, 0.25, 0.15, 0.20, 0.15, 0.10};

    private final ModelWeightRepository weightRepo;

    public RuleBasedLayer(ModelWeightRepository weightRepo) {
        this.weightRepo = weightRepo;
    }

    /**
     * Compute the rule-based risk score for a given feature vector.
     *
     * @param feature The feature set to evaluate.
     * @return A RuleScore containing the total score [0,1] and per-feature contributions.
     */
    public RuleScore score(RiskFeature feature) {
        double[] vector = feature.toVector();
        double[] weights = loadWeights();

        double weightedSum = 0.0;
        double totalWeight = 0.0;
        double[] contributions = new double[vector.length];

        for (int i = 0; i < vector.length; i++) {
            double contribution = weights[i] * vector[i];
            contributions[i] = contribution;
            weightedSum += contribution;
            totalWeight += weights[i];
        }

        double score = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        return new RuleScore(Math.min(1.0, Math.max(0.0, score)), contributions, FEATURE_NAMES);
    }

    /** Load weights from DB, fall back to defaults if not seeded */
    private double[] loadWeights() {
        List<ModelWeight> dbWeights = weightRepo.findAllByOrderByFeatureIndexAsc();
        if (dbWeights.size() < FEATURE_NAMES.length) {
            return DEFAULT_WEIGHTS;
        }
        double[] w = new double[FEATURE_NAMES.length];
        for (ModelWeight mw : dbWeights) {
            int idx = mw.getFeatureIndex();
            if (idx >= 0 && idx < w.length) {
                w[idx] = mw.getWeight();
            }
        }
        return w;
    }

    // ── Inner result record ──────────────────────────────────────────────────

    public record RuleScore(double score, double[] contributions, String[] featureNames) {

        /** Returns the name of the feature with the highest contribution (top risk driver) */
        public String topFeature() {
            int maxIdx = 0;
            for (int i = 1; i < contributions.length; i++) {
                if (contributions[i] > contributions[maxIdx]) maxIdx = i;
            }
            return featureNames[maxIdx];
        }

        /** Returns indices of features sorted by contribution descending */
        public int[] topKIndices(int k) {
            Integer[] indices = new Integer[contributions.length];
            for (int i = 0; i < indices.length; i++) indices[i] = i;
            java.util.Arrays.sort(indices, (a, b) -> Double.compare(contributions[b], contributions[a]));
            int[] result = new int[Math.min(k, indices.length)];
            for (int i = 0; i < result.length; i++) result[i] = indices[i];
            return result;
        }

        public String band() {
            if (score < 0.3) return "LOW";
            if (score < 0.6) return "MEDIUM";
            return "HIGH";
        }
    }
}
