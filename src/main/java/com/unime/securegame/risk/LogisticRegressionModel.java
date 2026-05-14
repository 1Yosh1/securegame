package com.unime.securegame.risk;

import com.unime.securegame.model.ModelWeight;
import com.unime.securegame.repository.ModelWeightRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Category C — Pure-Java Logistic Regression model.
 *
 * Weights are stored in the DB (ModelWeight entity, Category A) and can be updated
 * via REST API after retraining on new synthetic data.
 *
 * Prediction: P(anomaly) = sigmoid(bias + Σ w_i * x_i)
 *
 * The model is initialized with sensible defaults and can be retrained
 * using SyntheticDataGenerator output.
 */
@Component
public class LogisticRegressionModel {

    /** Default weights derived from domain knowledge (before training on synthetic data) */
    private static final double[] DEFAULT_WEIGHTS = {0.8, 2.5, 1.0, 1.5, 2.0, 1.2};
    private static final double DEFAULT_BIAS = -2.0;

    private static final String BIAS_NAME = "bias";

    private final ModelWeightRepository weightRepo;

    public LogisticRegressionModel(ModelWeightRepository weightRepo) {
        this.weightRepo = weightRepo;
    }

    /**
     * Predict the probability that a session is anomalous.
     *
     * @param feature The feature set to evaluate.
     * @return Probability in [0.0, 1.0]. > 0.5 is treated as anomalous.
     */
    public double predict(RiskFeature feature) {
        double[] vector = feature.toVector();
        double[] weights = loadWeights();
        double bias = loadBias();

        double logit = bias;
        for (int i = 0; i < vector.length; i++) {
            logit += weights[i] * vector[i];
        }
        return sigmoid(logit);
    }

    /** Sigmoid activation function */
    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private double[] loadWeights() {
        List<ModelWeight> dbWeights = weightRepo.findAllByOrderByFeatureIndexAsc();
        if (dbWeights.isEmpty()) return DEFAULT_WEIGHTS;

        double[] w = new double[DEFAULT_WEIGHTS.length];
        for (ModelWeight mw : dbWeights) {
            int idx = mw.getFeatureIndex();
            if (idx >= 0 && idx < w.length) {
                w[idx] = mw.getWeight();
            }
        }
        return w;
    }

    private double loadBias() {
        return weightRepo.findByFeatureName(BIAS_NAME)
                .map(ModelWeight::getWeight)
                .orElse(DEFAULT_BIAS);
    }

    /**
     * Train the model using gradient descent on a list of labelled RiskFeatures.
     * Updates weight rows in the DB.
     *
     * @param samples   Training samples with ground-truth labels (0=legit, 1=anomalous)
     * @param epochs    Number of training passes
     * @param learnRate Learning rate (e.g. 0.1)
     */
    public void train(List<RiskFeature> samples, int epochs, double learnRate) {
        double[] weights = new double[DEFAULT_WEIGHTS.length];
        System.arraycopy(DEFAULT_WEIGHTS, 0, weights, 0, weights.length);
        double bias = DEFAULT_BIAS;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] gradW = new double[weights.length];
            double gradB = 0.0;

            for (RiskFeature sample : samples) {
                double[] x = sample.toVector();
                double yHat = sigmoid(bias + dot(weights, x));
                double error = yHat - sample.label();

                for (int i = 0; i < weights.length; i++) {
                    gradW[i] += error * x[i];
                }
                gradB += error;
            }

            // Update weights
            int n = samples.size();
            for (int i = 0; i < weights.length; i++) {
                weights[i] -= learnRate * gradW[i] / n;
            }
            bias -= learnRate * gradB / n;
        }

        // Persist updated weights to DB
        persistWeights(weights, bias);
    }

    private void persistWeights(double[] weights, double bias) {
        String[] names = RuleBasedLayer.FEATURE_NAMES;
        for (int i = 0; i < weights.length; i++) {
            final int idx = i;
            ModelWeight mw = weightRepo.findByFeatureName(names[i])
                    .orElseGet(() -> new ModelWeight(names[idx], idx, 0.0));
            mw.setWeight(weights[i]);
            weightRepo.save(mw);
        }
        ModelWeight biasMw = weightRepo.findByFeatureName(BIAS_NAME)
                .orElseGet(() -> new ModelWeight(BIAS_NAME, -1, 0.0));
        biasMw.setWeight(bias);
        weightRepo.save(biasMw);
    }

    private double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }
}
