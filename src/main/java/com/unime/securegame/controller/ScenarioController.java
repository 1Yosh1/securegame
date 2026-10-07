package com.unime.securegame.controller;

import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.ScenarioRepository;
import com.unime.securegame.service.AdminApiKeyService;
import com.unime.securegame.service.ScenarioGeneratorService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint for on-demand scenario content generation.
 * Delegates to ScenarioGeneratorService — pure Java, no external APIs.
 */
@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioGeneratorService scenarioGenerator;
    private final ScenarioRepository scenarioRepository;
    private final AdminApiKeyService adminApiKeyService;

    public ScenarioController(ScenarioGeneratorService scenarioGenerator, ScenarioRepository scenarioRepository,
                              AdminApiKeyService adminApiKeyService) {
        this.scenarioGenerator = scenarioGenerator;
        this.scenarioRepository = scenarioRepository;
        this.adminApiKeyService = adminApiKeyService;
    }

    @GetMapping
    public List<ScenarioView> getScenarios() {
        return scenarioRepository.findAllByDeletedFalseOrderByIdAsc().stream()
                .map(ScenarioView::from).toList();
    }

    @PostMapping
    public ResponseEntity<?> createScenario(@RequestBody ScenarioRequest request,
                                            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (!authorized(adminKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!isValid(request)) return ResponseEntity.badRequest().body("Invalid scenario policy");
        Scenario scenario = new Scenario(request.name().trim(), request.lockoutThreshold(),
                request.minPasswordEntropy(), request.mfaRequired());
        scenario.setGeoCheckEnabled(request.geoCheckEnabled());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScenarioView.from(scenarioRepository.saveAndFlush(scenario)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioView> getScenario(@PathVariable Long id) {
        return scenarioRepository.findByIdAndDeletedFalse(id)
                .map(ScenarioView::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateScenario(@PathVariable Long id, @RequestBody ScenarioRequest request,
                                            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (!authorized(adminKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!isValid(request) || request.version() == null) {
            return ResponseEntity.badRequest().body("Invalid scenario policy or missing version");
        }
        Scenario scenario = scenarioRepository.findByIdAndDeletedFalse(id).orElse(null);
        if (scenario == null) return ResponseEntity.notFound().build();
        if (request.version() != scenario.getVersion()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Scenario was changed; reload before updating");
        }
        apply(request, scenario);
        try {
            return ResponseEntity.ok(ScenarioView.from(scenarioRepository.saveAndFlush(scenario)));
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Scenario was changed; reload before updating");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable Long id,
                                                @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (!authorized(adminKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Scenario scenario = scenarioRepository.findByIdAndDeletedFalse(id).orElse(null);
        if (scenario == null) return ResponseEntity.notFound().build();
        scenario.setDeleted(true);
        scenarioRepository.saveAndFlush(scenario);
        return ResponseEntity.noContent().build();
    }

    private boolean authorized(String adminKey) {
        return !adminApiKeyService.isRequired() || adminApiKeyService.isValid(adminKey);
    }

    private boolean isValid(ScenarioRequest request) {
        return request != null && request.name() != null && !request.name().isBlank()
                && request.name().trim().length() <= 120
                && request.lockoutThreshold() >= 1 && request.lockoutThreshold() <= 20
                && request.minPasswordEntropy() >= 0 && request.minPasswordEntropy() <= 100;
    }

    private void apply(ScenarioRequest request, Scenario scenario) {
        scenario.setName(request.name().trim());
        scenario.setLockoutThreshold(request.lockoutThreshold());
        scenario.setMinPasswordEntropy(request.minPasswordEntropy());
        scenario.setMfaRequired(request.mfaRequired());
        scenario.setGeoCheckEnabled(request.geoCheckEnabled());
    }

    public record ScenarioRequest(String name, int lockoutThreshold, int minPasswordEntropy,
                                  boolean mfaRequired, boolean geoCheckEnabled, Long version) {}

    public record ScenarioView(Long id, long version, String name, int lockoutThreshold,
                               int minPasswordEntropy, boolean mfaRequired, boolean geoCheckEnabled) {
        static ScenarioView from(Scenario scenario) {
            return new ScenarioView(scenario.getId(), scenario.getVersion(), scenario.getName(),
                    scenario.getLockoutThreshold(), scenario.getMinPasswordEntropy(),
                    scenario.isMfaRequired(), scenario.isGeoCheckEnabled());
        }
    }

    @GetMapping("/generate")
    public ResponseEntity<?> generateScenario(@RequestParam String topic, @RequestParam String type,
                                              @RequestParam(defaultValue = "42") long seed) {
        if (topic == null || topic.isBlank() || topic.length() > 80
                || type == null || !List.of("crypto", "phishing", "matching").contains(type)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid generation topic or type"));
        }
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(scenarioGenerator.generateScenario(topic, type, seed));
    }
}
