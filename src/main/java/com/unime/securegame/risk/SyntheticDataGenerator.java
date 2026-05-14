package com.unime.securegame.risk;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Category C — Generates a synthetic labelled dataset for training the LogisticRegressionModel.
 *
 * Uses a seeded Random to ensure reproducibility (same seed → same dataset).
 * The generation rules reflect domain knowledge about what constitutes an anomalous session:
 *  - Low entropy + no MFA + high fail streak → anomalous
 *  - High entropy + TOTP/WebAuthn + familiar geo/time → legitimate
 *
 * Output can be serialized to CSV for offline inspection and archiving.
 */
@Component
public class SyntheticDataGenerator {

    /** CSV header matching RiskFeature fields */
    public static final String CSV_HEADER =
            "passwordEntropy,geoDistanceKm,timeDeviationHours,uaSimilarity,failStreak,mfaType,label";

    /**
     * Generate a list of labelled RiskFeature samples.
     *
     * @param count Number of samples to generate
     * @param seed  Random seed for reproducibility
     * @return List of RiskFeature with ground-truth labels
     */
    public List<RiskFeature> generate(int count, long seed) {
        Random rng = new Random(seed);
        List<RiskFeature> samples = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            // Determine label first (50/50 split with slight bias toward legitimate)
            boolean isAnomaly = rng.nextDouble() < 0.45;

            samples.add(isAnomaly
                    ? generateAnomalousSample(rng)
                    : generateLegitSample(rng));
        }
        return samples;
    }

    /** Generate features typical of a LEGITIMATE session */
    private RiskFeature generateLegitSample(Random rng) {
        int entropy = 55 + rng.nextInt(46);               // 55–100 (strong password)
        double geo = rng.nextDouble() * 50;               // 0–50 km (nearby)
        double timeDeviation = rng.nextDouble() * 2.0;    // 0–2 hours (usual time)
        double uaSim = 0.85 + rng.nextDouble() * 0.15;    // 0.85–1.0 (familiar UA)
        int failStreak = rng.nextInt(2);                  // 0–1 failures
        int mfaType = 1 + rng.nextInt(2);                 // TOTP or WebAuthn
        return new RiskFeature(entropy, geo, timeDeviation, uaSim, failStreak, mfaType, 0);
    }

    /** Generate features typical of an ANOMALOUS session */
    private RiskFeature generateAnomalousSample(Random rng) {
        int entropy = rng.nextInt(45);                    // 0–44 (weak password)
        double geo = 200 + rng.nextDouble() * 4800;       // 200–5000 km (different country)
        double timeDeviation = 4.0 + rng.nextDouble() * 8.0; // 4–12 hours (unusual time)
        double uaSim = rng.nextDouble() * 0.5;            // 0.0–0.5 (unfamiliar UA)
        int failStreak = 2 + rng.nextInt(9);              // 2–10 failures
        int mfaType = rng.nextInt(2);                     // None or TOTP only
        return new RiskFeature(entropy, geo, timeDeviation, uaSim, failStreak, mfaType, 1);
    }

    /**
     * Serialize a list of RiskFeature samples to CSV string.
     */
    public String toCsv(List<RiskFeature> samples) {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append("\n");
        for (RiskFeature f : samples) {
            sb.append(f.passwordEntropy()).append(',')
              .append(String.format("%.2f", f.geoDistanceKm())).append(',')
              .append(String.format("%.2f", f.timeDeviationHours())).append(',')
              .append(String.format("%.3f", f.uaSimilarity())).append(',')
              .append(f.failStreak()).append(',')
              .append(f.mfaType()).append(',')
              .append(f.label()).append('\n');
        }
        return sb.toString();
    }
}
