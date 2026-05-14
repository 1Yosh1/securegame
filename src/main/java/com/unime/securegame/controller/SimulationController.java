package com.unime.securegame.controller;

import com.unime.securegame.repository.PlayerProfileRepository;
import com.unime.securegame.service.SimulationEngine;
import com.unime.securegame.service.SimulationEngine.LoginResult;
import java.util.HashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationEngine engine;
    private final PlayerProfileRepository playerRepo;

    public SimulationController(SimulationEngine engine, PlayerProfileRepository playerRepo) {
        this.engine = engine;
        this.playerRepo = playerRepo;
    }

    /**
     * Attempt a login for a player in a given scenario.
     * Body: { "password": "...", "failedAttempts": 0 }
     */
    @PostMapping("/scenarios/{scenarioId}/players/{username}/login")
    public ResponseEntity<Map<String, Object>> login(
            @PathVariable Long scenarioId,
            @PathVariable String username,
            @RequestBody Map<String, Object> body) {

        String password = (String) body.getOrDefault("password", "");
        int failedAttempts = (int) body.getOrDefault("failedAttempts", 0);

        LoginResult result = engine.processLoginAttempt(scenarioId, username, password, failedAttempts);

        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("nextState", result.nextState.name());
        resp.put("requiresMfa", result.requiresMfa);
        return ResponseEntity.ok(resp);
    }

    /**
     * Verify a TOTP code for a player.
     * Body: { "code": "123456", "failedAttempts": 0 }
     */
    @PostMapping("/scenarios/{scenarioId}/players/{username}/mfa")
    public ResponseEntity<Map<String, Object>> verifyMfa(
            @PathVariable Long scenarioId,
            @PathVariable String username,
            @RequestBody Map<String, Object> body) {

        String code = (String) body.getOrDefault("code", "");
        int failedAttempts = (int) body.getOrDefault("failedAttempts", 0);

        var opt = playerRepo.findByUsername(username);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        LoginResult result = engine.processTOTPVerification(
                scenarioId, username, opt.get().getTotpSecret(), code, failedAttempts);
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("nextState", result.nextState.name());
        return ResponseEntity.ok(resp);
    }
}
