package com.unime.securegame.controller;

import com.unime.securegame.model.CoachFeedback;
import com.unime.securegame.model.EventLog;
import com.unime.securegame.repository.CoachFeedbackRepository;
import com.unime.securegame.repository.EventLogRepository;
import com.unime.securegame.risk.RiskEngine;
import com.unime.securegame.risk.RiskFeature;
import com.unime.securegame.risk.SyntheticDataGenerator;
import com.unime.securegame.risk.LogisticRegressionModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

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
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> evaluate(@RequestBody RiskFeatureRequest req) {
        if (!valid(req)) return ResponseEntity.badRequest().body("Invalid risk feature values");
        RiskFeature feature = new RiskFeature(
                req.passwordEntropy(), req.geoDistanceKm(), req.timeDeviationHours(),
                req.uaSimilarity(), req.failStreak(), req.mfaType());
        return ResponseEntity.ok(riskEngine.evaluate(feature));
    }

    /** Train the LR model on freshly generated synthetic data */
    @PostMapping("/train")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<String> trainModel(
            @RequestParam(defaultValue = "3000") int samples,
            @RequestParam(defaultValue = "42") long seed,
            @RequestParam(defaultValue = "200") int epochs,
            @RequestParam(defaultValue = "0.1") double learnRate) {

        if (samples < 1 || samples > 5000 || epochs < 1 || epochs > 1000
                || !Double.isFinite(learnRate) || learnRate <= 0 || learnRate > 1
                || (long) samples * epochs > 2_000_000L) {
            return ResponseEntity.badRequest().body("Training limits exceeded");
        }
        List<RiskFeature> data = dataGen.generate(samples, seed);
        lrModel.train(data, epochs, learnRate);
        return ResponseEntity.ok("Model trained on " + samples + " samples, " + epochs + " epochs.");
    }

    /** Export synthetic dataset as CSV */
    @GetMapping("/dataset/csv")
    public ResponseEntity<String> exportDataset(
            @RequestParam(defaultValue = "500") int samples,
            @RequestParam(defaultValue = "42") long seed) {
        if (samples < 1 || samples > 5000) {
            return ResponseEntity.badRequest().body("Samples must be between 1 and 5000");
        }
        List<RiskFeature> data = dataGen.generate(samples, seed);
        String csv = dataGen.toCsv(data);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"mfa_session_dataset.csv\"")
                .body(csv);
    }

    /** Get event logs by scenario */
    @GetMapping("/events/scenario/{scenarioId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> getEventsByScenario(@PathVariable Long scenarioId) {
        if (scenarioId == null || scenarioId < 1) return ResponseEntity.badRequest().body("Invalid scenario id");
        return ResponseEntity.ok(eventLogRepo.findByScenarioIdOrderByTimestampAsc(scenarioId));
    }

    @GetMapping("/events/scenario/{scenarioId}/csv")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> exportScenarioEvents(@PathVariable Long scenarioId) {
        if (scenarioId == null || scenarioId < 1) return ResponseEntity.badRequest().build();
        List<EventLog> events = eventLogRepo.findByScenarioIdOrderByTimestampAsc(scenarioId);
        if (events.size() > 10_000) return ResponseEntity.status(413).body("Event export limit exceeded");
        StringBuilder csv = new StringBuilder("id,scenarioId,playerUsername,eventType,timestamp,riskScore,details\n");
        for (EventLog event : events) {
            csv.append(event.getId()).append(',')
                    .append(event.getScenarioId()).append(',')
                    .append(csvCell(event.getPlayerUsername())).append(',')
                    .append(csvCell(event.getEventType())).append(',')
                    .append(csvCell(event.getTimestamp() == null ? "" : event.getTimestamp().toString())).append(',')
                    .append(String.format(Locale.ROOT, "%.6f", event.getRiskScore())).append(',')
                    .append(csvCell(event.getDetails())).append('\n');
        }
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"scenario-" + scenarioId + "-events.csv\"")
                .body(csv.toString());
    }

    private String csvCell(String value) {
        if (value == null) return "\"\"";
        String sanitized = value.replace("\r", " ").replace("\n", " ");
        if (!sanitized.isEmpty() && "=+-@\\t".indexOf(sanitized.charAt(0)) >= 0) sanitized = "'" + sanitized;
        return "\"" + sanitized.replace("\"", "\"\"") + "\"";
    }

    private boolean valid(RiskFeatureRequest req) {
        return req != null && req.passwordEntropy() >= 0 && req.passwordEntropy() <= 100
                && Double.isFinite(req.geoDistanceKm()) && req.geoDistanceKm() >= 0 && req.geoDistanceKm() <= 100_000
                && Double.isFinite(req.timeDeviationHours()) && req.timeDeviationHours() >= 0 && req.timeDeviationHours() <= 24
                && Double.isFinite(req.uaSimilarity()) && req.uaSimilarity() >= 0 && req.uaSimilarity() <= 1
                && req.failStreak() >= 0 && req.failStreak() <= 1000
                && req.mfaType() >= 0 && req.mfaType() <= 2;
    }

    /** Get event logs by player */
    @GetMapping("/events/player/{username}")
    @PreAuthorize("hasRole('TEACHER') or #username == authentication.name")
    public List<EventLog> getEventsByPlayer(@PathVariable String username) {
        return eventLogRepo.findByPlayerUsernameOrderByTimestampDesc(username);
    }

    // ── Coach Feedback CRUD ──────────────────────────────────────────────────

    @GetMapping("/coach")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<CoachFeedback> getAllCoachFeedback() {
        return coachRepo.findAll();
    }

    @PostMapping("/coach")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public CoachFeedback createCoachFeedback(@RequestBody CoachFeedback feedback) {
        return coachRepo.save(feedback);
    }

    @DeleteMapping("/coach/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
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
