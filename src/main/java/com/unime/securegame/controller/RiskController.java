package com.unime.securegame.controller;

import com.unime.securegame.model.CoachFeedback;
import com.unime.securegame.model.EventLog;
import com.unime.securegame.repository.CoachFeedbackRepository;
import com.unime.securegame.repository.EventLogRepository;
import com.unime.securegame.risk.RiskEngine;
import com.unime.securegame.risk.RiskFeature;
import com.unime.securegame.risk.SyntheticDataGenerator;
import com.unime.securegame.risk.LogisticRegressionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskEngine riskEngine;
    private final CoachFeedbackRepository coachRepo;
    private final EventLogRepository eventLogRepo;
    private final SyntheticDataGenerator dataGen;
    private final LogisticRegressionModel lrModel;

    public RiskController(RiskEngine riskEngine, CoachFeedbackRepository coachRepo,
                          EventLogRepository eventLogRepo, SyntheticDataGenerator dataGen,
                          LogisticRegressionModel lrModel) {
        this.riskEngine = riskEngine;
        this.coachRepo = coachRepo;
        this.eventLogRepo = eventLogRepo;
        this.dataGen = dataGen;
        this.lrModel = lrModel;
    }

    /**
     * Evaluate risk for a session.
     * Body: { "passwordEntropy":75, "geoDistanceKm":0, "timeDeviationHours":0.5,
     *          "uaSimilarity":1.0, "failStreak":0, "mfaType":1 }
     */
    @PostMapping("/evaluate")
    public ResponseEntity<RiskEngine.RiskAssessment> evaluate(@RequestBody RiskFeatureRequest req) {
        RiskFeature feature = new RiskFeature(
                req.passwordEntropy(), req.geoDistanceKm(), req.timeDeviationHours(),
                req.uaSimilarity(), req.failStreak(), req.mfaType());
        return ResponseEntity.ok(riskEngine.evaluate(feature));
    }

    /** Train the LR model on freshly generated synthetic data */
    @PostMapping("/train")
    public ResponseEntity<String> trainModel(
            @RequestParam(defaultValue = "3000") int samples,
            @RequestParam(defaultValue = "42") long seed,
            @RequestParam(defaultValue = "200") int epochs,
            @RequestParam(defaultValue = "0.1") double learnRate) {

        List<RiskFeature> data = dataGen.generate(samples, seed);
        lrModel.train(data, epochs, learnRate);
        return ResponseEntity.ok("Model trained on " + samples + " samples, " + epochs + " epochs.");
    }

    /** Export synthetic dataset as CSV */
    @GetMapping("/dataset/csv")
    public ResponseEntity<String> exportDataset(
            @RequestParam(defaultValue = "500") int samples,
            @RequestParam(defaultValue = "42") long seed) {
        List<RiskFeature> data = dataGen.generate(samples, seed);
        String csv = dataGen.toCsv(data);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"mfa_session_dataset.csv\"")
                .body(csv);
    }

    /** Get event logs by scenario */
    @GetMapping("/events/scenario/{scenarioId}")
    public List<EventLog> getEventsByScenario(@PathVariable Long scenarioId) {
        return eventLogRepo.findByScenarioIdOrderByTimestampAsc(scenarioId);
    }

    /** Get event logs by player */
    @GetMapping("/events/player/{username}")
    public List<EventLog> getEventsByPlayer(@PathVariable String username) {
        return eventLogRepo.findByPlayerUsernameOrderByTimestampDesc(username);
    }

    // ── Coach Feedback CRUD ──────────────────────────────────────────────────

    @GetMapping("/coach")
    public List<CoachFeedback> getAllCoachFeedback() {
        return coachRepo.findAll();
    }

    @PostMapping("/coach")
    public CoachFeedback createCoachFeedback(@RequestBody CoachFeedback feedback) {
        return coachRepo.save(feedback);
    }

    @DeleteMapping("/coach/{id}")
    public ResponseEntity<Void> deleteCoachFeedback(@PathVariable Long id) {
        if (!coachRepo.existsById(id)) return ResponseEntity.notFound().build();
        coachRepo.deleteById(id);
        return ResponseEntity.<Void>noContent().build();
    }

    /** Request DTO */
    public record RiskFeatureRequest(
            int passwordEntropy,
            double geoDistanceKm,
            double timeDeviationHours,
            double uaSimilarity,
            int failStreak,
            int mfaType
    ) {}
}
