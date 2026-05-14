package com.unime.securegame.service;

import com.unime.securegame.model.EventLog;
import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.EventLogRepository;
import com.unime.securegame.repository.ScenarioRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Category C — Deterministic Discrete-Event Simulation Engine.
 *
 * Implements a session state machine with:
 *  - Priority event queue (sorted by timestamp seed)
 *  - Policy evaluation: entropy, lockout, MFA-type, device trust, geo (stubs for later phases)
 *  - Deterministic execution: same seed → same outcome
 */
@Service
public class SimulationEngine {

    /** Session states for the state machine */
    public enum SessionState {
        INIT,
        AUTH_ATTEMPT,
        MFA_CHALLENGE,
        SUCCESS,
        LOCKED,
        DENIED
    }

    private final ScenarioRepository scenarioRepository;
    private final EventLogRepository eventLogRepository;
    private final TOTPService totpService;

    public SimulationEngine(ScenarioRepository scenarioRepository,
                            EventLogRepository eventLogRepository,
                            TOTPService totpService) {
        this.scenarioRepository = scenarioRepository;
        this.eventLogRepository = eventLogRepository;
        this.totpService = totpService;
    }

    // ─────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────

    /**
     * Process a login attempt within a scenario.
     *
     * @param scenarioId       The active scenario ID
     * @param playerUsername   The player username
     * @param password         The submitted password
     * @param failedAttempts   Current count of consecutive failures in this session
     * @return LoginResult containing the next state and message
     */
    public LoginResult processLoginAttempt(Long scenarioId, String playerUsername,
                                           String password, int failedAttempts) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));

        // TRANSITION: INIT → AUTH_ATTEMPT
        SessionState state = SessionState.AUTH_ATTEMPT;

        // Policy check 1: Lockout
        if (failedAttempts >= scenario.getLockoutThreshold()) {
            state = SessionState.LOCKED;
            log(scenarioId, playerUsername, "LOCKOUT",
                    "{\"failedAttempts\":" + failedAttempts + "}", 0.9);
            return new LoginResult(false, "Account is locked after " + failedAttempts + " failed attempts.", state, false);
        }

        // Policy check 2: Password entropy
        double entropy = calculateShannonEntropy(password);
        int entropyScore = entropyToScore(entropy);
        if (entropyScore < scenario.getMinPasswordEntropy()) {
            log(scenarioId, playerUsername, "WEAK_PASSWORD",
                    "{\"entropyScore\":" + entropyScore + ",\"required\":" + scenario.getMinPasswordEntropy() + "}", 0.7);
            return new LoginResult(false,
                    "Password too weak (entropy score " + entropyScore + "/" + scenario.getMinPasswordEntropy() + ").",
                    SessionState.DENIED, false);
        }

        // Policy check 3: MFA requirement
        if (scenario.isMfaRequired()) {
            state = SessionState.MFA_CHALLENGE;
            log(scenarioId, playerUsername, "MFA_CHALLENGE_ISSUED",
                    "{\"reason\":\"scenario_policy\"}", 0.3);
            return new LoginResult(true, "Password accepted. MFA required.", state, true);
        }

        // Success (no MFA required)
        state = SessionState.SUCCESS;
        log(scenarioId, playerUsername, "LOGIN_SUCCESS", "{}", 0.1);
        return new LoginResult(true, "Login successful.", state, false);
    }

    /**
     * Process a TOTP verification step.
     *
     * @param scenarioId     The active scenario ID
     * @param playerUsername The player username
     * @param totpSecret     The player's TOTP secret
     * @param code           The submitted 6-digit code
     * @param failedAttempts Current count of consecutive MFA failures
     * @return LoginResult with next session state
     */
    public LoginResult processTOTPVerification(Long scenarioId, String playerUsername,
                                               String totpSecret, String code, int failedAttempts) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));

        if (failedAttempts >= scenario.getLockoutThreshold()) {
            log(scenarioId, playerUsername, "MFA_LOCKOUT",
                    "{\"failedMfaAttempts\":" + failedAttempts + "}", 1.0);
            return new LoginResult(false, "Account locked after " + failedAttempts + " failed MFA attempts.",
                    SessionState.LOCKED, false);
        }

        boolean valid = totpService.verifyCode(totpSecret, code);
        if (valid) {
            log(scenarioId, playerUsername, "LOGIN_SUCCESS", "{\"method\":\"TOTP\"}", 0.05);
            return new LoginResult(true, "MFA verified. Login successful.", SessionState.SUCCESS, false);
        } else {
            log(scenarioId, playerUsername, "MFA_FAILURE",
                    "{\"attempt\":" + (failedAttempts + 1) + "}", 0.6);
            return new LoginResult(false, "Invalid TOTP code.", SessionState.MFA_CHALLENGE, true);
        }
    }

    // ─────────────────────────────────────────────────
    // ENTROPY CALCULATION (Shannon)
    // ─────────────────────────────────────────────────

    /**
     * Calculates the Shannon entropy of a string in bits.
     * H = -Σ p(x) * log2(p(x))
     */
    public double calculateShannonEntropy(String input) {
        if (input == null || input.isEmpty()) return 0.0;

        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }

        double entropy = 0.0;
        int len = input.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    /**
     * Maps Shannon entropy (bits) to a 0–100 score.
     * Typical passwords range from 1.5 bits (very weak) to 4+ bits (strong).
     * We map 0–5 bits linearly to 0–100.
     */
    public int entropyToScore(double entropy) {
        return (int) Math.min(100, (entropy / 5.0) * 100);
    }

    // ─────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────

    private void log(Long scenarioId, String player, String eventType, String details, double riskScore) {
        EventLog event = new EventLog(scenarioId, player, eventType, details, riskScore);
        eventLogRepository.save(event);
    }

    // ─────────────────────────────────────────────────
    // RESULT DTO
    // ─────────────────────────────────────────────────

    public static class LoginResult {
        public final boolean success;
        public final String message;
        public final SessionState nextState;
        public final boolean requiresMfa;

        public LoginResult(boolean success, String message, SessionState nextState, boolean requiresMfa) {
            this.success = success;
            this.message = message;
            this.nextState = nextState;
            this.requiresMfa = requiresMfa;
        }
    }
}
