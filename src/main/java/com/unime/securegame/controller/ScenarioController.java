package com.unime.securegame.controller;

import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.ScenarioRepository;
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
        return scenarioRepository.findAll();
    }

    @PostMapping
    public Scenario createScenario(@RequestBody Scenario scenario) {
        return scenarioRepository.save(scenario);
    }

    @GetMapping("/{id}")
    public Scenario getScenario(@PathVariable Long id) {
        return scenarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Scenario not found"));
    }

    @PutMapping("/{id}")
    public Scenario updateScenario(@PathVariable Long id, @RequestBody Scenario scenarioDetails) {
        Scenario scenario = scenarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Scenario not found"));
        scenario.setName(scenarioDetails.getName());
        scenario.setLockoutThreshold(scenarioDetails.getLockoutThreshold());
        scenario.setMinPasswordEntropy(scenarioDetails.getMinPasswordEntropy());
        scenario.setMfaRequired(scenarioDetails.isMfaRequired());
        return scenarioRepository.save(scenario);
    }

    @DeleteMapping("/{id}")
    public void deleteScenario(@PathVariable Long id) {
        scenarioRepository.deleteById(id);
    }
}
