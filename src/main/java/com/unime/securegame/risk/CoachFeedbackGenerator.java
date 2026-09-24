package com.unime.securegame.risk;

import com.unime.securegame.model.CoachFeedback;
import com.unime.securegame.repository.CoachFeedbackRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        if (limit == 0) {
            return tips;
        }

        // Fetch all potential feedback for the relevant factors in one query to avoid N+1
        List<String> factorsToQuery = Arrays.asList(topFactorNames).subList(0, limit);
        List<CoachFeedback> allFeedbacks = coachRepo.findByRiskFactorIn(factorsToQuery);

        // Group the fetched feedback by risk factor
        Map<String, List<CoachFeedback>> feedbackByFactor = allFeedbacks.stream()
                .collect(Collectors.groupingBy(CoachFeedback::getRiskFactor));

        for (int i = 0; i < limit; i++) {
            String factor = topFactorNames[i];
            List<CoachFeedback> factorFeedbacks = feedbackByFactor.getOrDefault(factor, new ArrayList<>());

            if (!factorFeedbacks.isEmpty()) {
                // Try to find a match for the specific severity band
                List<CoachFeedback> matches = factorFeedbacks.stream()
                        .filter(f -> severityBand.equals(f.getSeverityBand()))
                        .collect(Collectors.toList());

                if (!matches.isEmpty()) {
                    tips.add(matches.get(0).getTip());
                } else {
                    // Fall back to any severity tip for this factor
                    tips.add(factorFeedbacks.get(0).getTip());
                }
            }
        }
        return tips;
    }
}
