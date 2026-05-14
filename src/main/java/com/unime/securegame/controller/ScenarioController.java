package com.unime.securegame.controller;

import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.ScenarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioRepository scenarioRepository;

    public ScenarioController(ScenarioRepository scenarioRepository) {
        this.scenarioRepository = scenarioRepository;
    }

    @GetMapping
    public List<Scenario> getAllScenarios() {
        return scenarioRepository.findAll().stream()
                .filter(s -> !s.isDeleted())
                .toList();
    }

    @PostMapping
    public Scenario createScenario(@RequestBody Scenario scenario) {
        return scenarioRepository.save(scenario);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Scenario> getScenario(@PathVariable Long id) {
        return scenarioRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Scenario> updateScenario(@PathVariable Long id, @RequestBody Scenario details) {
        return scenarioRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .map(scenario -> {
                    scenario.setName(details.getName());
                    scenario.setLockoutThreshold(details.getLockoutThreshold());
                    scenario.setMinPasswordEntropy(details.getMinPasswordEntropy());
                    scenario.setMfaRequired(details.isMfaRequired());
                    scenario.setGeoCheckEnabled(details.isGeoCheckEnabled());
                    return ResponseEntity.ok(scenarioRepository.save(scenario));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable Long id) {
        var opt = scenarioRepository.findById(id).filter(s -> !s.isDeleted());
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Scenario scenario = opt.get();
        scenario.setDeleted(true);
        scenarioRepository.save(scenario);
        return ResponseEntity.<Void>noContent().build();
    }
}
