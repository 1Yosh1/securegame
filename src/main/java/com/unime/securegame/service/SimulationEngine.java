package com.unime.securegame.service;

import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.ScenarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SimulationEngine {

    private final ScenarioRepository scenarioRepository;
    private final TOTPService totpService;

    public SimulationEngine(ScenarioRepository scenarioRepository, TOTPService totpService) {
        this.scenarioRepository = scenarioRepository;
        this.totpService = totpService;
    }

    public LoginResult processLoginAttempt(Long scenarioId, String password, int currentFailedAttempts) {
        Optional<Scenario> optScenario = scenarioRepository.findById(scenarioId);
        if (optScenario.isEmpty()) {
            return new LoginResult(false, "Scenario not found", false);
        }

        Scenario scenario = optScenario.get();

        // 1. Check Lockout State
        if (currentFailedAttempts >= scenario.getLockoutThreshold()) {
            return new LoginResult(false, "Account Locked", false);
        }

        // 2. Evaluate Password Entropy (simple mock for now: length * 10)
        int entropy = evaluatePasswordEntropy(password);
        if (entropy < scenario.getMinPasswordEntropy()) {
            return new LoginResult(false, "Weak Password", false);
        }

        // 3. MFA requirement
        if (scenario.isMfaRequired()) {
            return new LoginResult(true, "MFA Required", true);
        }

        return new LoginResult(true, "Login Successful", false);
    }

    public boolean processTOTP(String secret, String code) {
        return totpService.verifyCode(secret, code);
    }

    private int evaluatePasswordEntropy(String password) {
        if (password == null) return 0;
        return password.length() * 10;
    }

    public static class LoginResult {
        public boolean success;
        public String message;
        public boolean requiresMfa;

        public LoginResult(boolean success, String message, boolean requiresMfa) {
            this.success = success;
            this.message = message;
            this.requiresMfa = requiresMfa;
        }
    }
}
