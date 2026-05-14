package com.unime.securegame.model;

import jakarta.persistence.*;

/**
 * Category A — CRUD entity storing coach feedback tips.
 * Each tip is linked to a specific risk factor and severity band.
 * The RiskEngine reads these at runtime to produce pedagogical feedback.
 */
@Entity
@Table(name = "coach_feedback")
public class CoachFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String riskFactor;   // e.g. "passwordEntropy", "geoDistanceKm"

    @Column(nullable = false)
    private String severityBand; // "LOW", "MEDIUM", "HIGH"

    @Column(nullable = false, length = 1000)
    private String tip;          // Human-readable pedagogical message

    public CoachFeedback() {}

    public CoachFeedback(String riskFactor, String severityBand, String tip) {
        this.riskFactor = riskFactor;
        this.severityBand = severityBand;
        this.tip = tip;
    }

    public Long getId() { return id; }
    public String getRiskFactor() { return riskFactor; }
    public void setRiskFactor(String riskFactor) { this.riskFactor = riskFactor; }
    public String getSeverityBand() { return severityBand; }
    public void setSeverityBand(String severityBand) { this.severityBand = severityBand; }
    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }
}
