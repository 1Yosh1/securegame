package com.unime.securegame.model;

import jakarta.persistence.*;

/**
 * Category A — CRUD entity storing logistic regression model weights.
 * Each row represents one feature's weight in the trained model.
 * The model can be retrained by updating these rows via the REST API.
 */
@Entity
@Table(name = "model_weights")
public class ModelWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String featureName; // e.g. "passwordEntropy", "geoDistanceKm", "bias"

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private int featureIndex; // 0-based index into RiskFeature.toVector(), -1 for bias

    public ModelWeight() {}

    public ModelWeight(String featureName, int featureIndex, double weight) {
        this.featureName = featureName;
        this.featureIndex = featureIndex;
        this.weight = weight;
    }

    public Long getId() { return id; }
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public int getFeatureIndex() { return featureIndex; }
    public void setFeatureIndex(int featureIndex) { this.featureIndex = featureIndex; }
}
