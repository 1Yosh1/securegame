package com.unime.securegame.controller;

import com.unime.securegame.service.ScenarioGeneratorService;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for on-demand scenario content generation.
 * Delegates to ScenarioGeneratorService — pure Java, no external APIs.
 */
@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioGeneratorService scenarioGenerator;

    public ScenarioController(ScenarioGeneratorService scenarioGenerator) {
        this.scenarioGenerator = scenarioGenerator;
    }

    @GetMapping("/generate")
    public String generateScenario(@RequestParam String topic, @RequestParam String type) {
        return scenarioGenerator.generateScenario(topic, type);
    }
}
