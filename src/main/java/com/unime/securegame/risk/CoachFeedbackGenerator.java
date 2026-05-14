package com.unime.securegame.risk;

import com.unime.securegame.model.CoachFeedback;
import com.unime.securegame.repository.CoachFeedbackRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Category C — Generates top-k coach feedback tips for a risk assessment result.
 *
 * Given the top-k risk factor names and a severity band, this component reads
 * matching tips from the DB (Category A) and returns them as an ordered list
 * for display in the Vaadin UI "Coach Tips" panel.
 */
@Component
public class CoachFeedbackGenerator {

    private final CoachFeedbackRepository coachRepo;

    public CoachFeedbackGenerator(CoachFeedbackRepository coachRepo) {
        this.coachRepo = coachRepo;
    }

    /**
     * Retrieve coach tips for the top-k risk factors.
     *
     * @param topFactorNames Feature names in order of contribution (most important first)
     * @param severityBand   "LOW", "MEDIUM", or "HIGH"
     * @param k              Number of factors to look up
     * @return List of tip strings (may be empty if DB has no matching entries)
     */
    public List<String> generate(String[] topFactorNames, String severityBand, int k) {
        List<String> tips = new ArrayList<>();
        int limit = Math.min(k, topFactorNames.length);

        for (int i = 0; i < limit; i++) {
            List<CoachFeedback> matches = coachRepo.findByRiskFactorAndSeverityBand(
                    topFactorNames[i], severityBand);

            if (!matches.isEmpty()) {
                tips.add(matches.get(0).getTip());
            } else {
                // Fall back to any severity tip for this factor
                List<CoachFeedback> any = coachRepo.findByRiskFactor(topFactorNames[i]);
                if (!any.isEmpty()) tips.add(any.get(0).getTip());
            }
        }
        return tips;
    }
}
