package com.unime.securegame.anomaly;

/**
 * Category C — Common contract for all anomaly detection signals.
 *
 * Each signal independently evaluates one dimension of a session and returns
 * a score in [0.0, 1.0] where 0.0 = completely normal and 1.0 = fully anomalous.
 *
 * The AnomalyFusionEngine combines these signals via weighted majority voting.
 */
public interface AnomalySignal {

    /**
     * Compute the anomaly score for this signal dimension.
     *
     * @param ctx The session context holding all observable session attributes.
     * @return Score in [0.0, 1.0] — 0 = normal, 1 = anomalous.
     */
    double score(SessionContext ctx);

    /**
     * Human-readable name of this signal (used in explanations).
     */
    String name();

    /**
     * Weight of this signal in the fusion (should sum to 1.0 across all signals).
     */
    double weight();
}
