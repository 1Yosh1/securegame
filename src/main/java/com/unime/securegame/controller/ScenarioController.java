package com.unime.securegame.controller;

import com.unime.securegame.service.LlmService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scenario")
public class ScenarioController {

    private final LlmService llmService;

    public ScenarioController(LlmService llmService) {
        this.llmService = llmService;
    }

    @GetMapping("/generate")
    public String generate(@RequestParam String topic, @RequestParam String type) {
        return llmService.generateScenario(topic, type);
    }
}
