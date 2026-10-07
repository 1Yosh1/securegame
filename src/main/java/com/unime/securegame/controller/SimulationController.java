package com.unime.securegame.controller;

import com.unime.securegame.repository.PlayerProfileRepository;
import com.unime.securegame.service.SimulationEngine;
import com.unime.securegame.service.SimulationEngine.LoginResult;
import java.util.HashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasRole('TEACHER') or #username == authentication.name")
    public ResponseEntity<Map<String, Object>> login(
            @PathVariable Long scenarioId,
            @PathVariable String username,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (authentication == null || !username.equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        Object passwordValue = body.getOrDefault("password", "");
        Object attemptsValue = body.getOrDefault("failedAttempts", 0);
        if (!(passwordValue instanceof String password) || password.length() > 256
                || !(attemptsValue instanceof Number attempts) || attempts.intValue() < 0 || attempts.intValue() > 1000) {
            return ResponseEntity.badRequest().build();
        }
        int failedAttempts = attempts.intValue();

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
    @PreAuthorize("hasRole('TEACHER') or #username == authentication.name")
    public ResponseEntity<Map<String, Object>> verifyMfa(
            @PathVariable Long scenarioId,
            @PathVariable String username,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (authentication == null || !username.equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        Object codeValue = body.getOrDefault("code", "");
        Object attemptsValue = body.getOrDefault("failedAttempts", 0);
        if (!(codeValue instanceof String code) || !code.matches("\\d{6}")
                || !(attemptsValue instanceof Number attempts) || attempts.intValue() < 0 || attempts.intValue() > 1000) {
            return ResponseEntity.badRequest().build();
        }
        int failedAttempts = attempts.intValue();

        var opt = playerRepo.findByUsername(username);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        if (opt.get().getTotpSecret() == null || opt.get().getTotpSecret().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        LoginResult result = engine.processTOTPVerification(
                scenarioId, username, opt.get().getTotpSecret(), code, failedAttempts);
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("nextState", result.nextState.name());
        return ResponseEntity.ok(resp);
    }
}
