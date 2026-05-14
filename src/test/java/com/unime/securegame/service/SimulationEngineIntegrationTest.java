package com.unime.securegame.service;

import com.unime.securegame.model.EventLog;
import com.unime.securegame.model.Scenario;
import com.unime.securegame.repository.EventLogRepository;
import com.unime.securegame.repository.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SimulationEngine state machine.
 * Uses H2 in-memory DB (see test/resources/application.properties).
 */
@SpringBootTest
@Transactional
class SimulationEngineIntegrationTest {

    @Autowired
    private SimulationEngine engine;

    @Autowired
    private ScenarioRepository scenarioRepo;

    @Autowired
    private EventLogRepository eventLogRepo;

    private Scenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new Scenario("Test Scenario", 3, 40, true);
        scenario = scenarioRepo.save(scenario);
    }

    @Test
    void weakPassword_returnsDenied() {
        // "ab" has entropy score ≈ 0, which is < 40
        SimulationEngine.LoginResult result =
                engine.processLoginAttempt(scenario.getId(), "player1", "ab", 0);

        assertFalse(result.success);
        assertEquals(SimulationEngine.SessionState.DENIED, result.nextState);
        assertTrue(result.message.contains("weak"), result.message);
    }

    @Test
    void lockedAccount_returnsLocked() {
        SimulationEngine.LoginResult result =
                engine.processLoginAttempt(scenario.getId(), "player1", "StrongPass!123", 3);

        assertFalse(result.success);
        assertEquals(SimulationEngine.SessionState.LOCKED, result.nextState);
    }

    @Test
    void strongPassword_withMfaRequired_returnsMfaChallenge() {
        // "StrongPass!123" has good entropy
        SimulationEngine.LoginResult result =
                engine.processLoginAttempt(scenario.getId(), "player1", "StrongPass!123", 0);

        assertTrue(result.success);
        assertEquals(SimulationEngine.SessionState.MFA_CHALLENGE, result.nextState);
        assertTrue(result.requiresMfa);
    }

    @Test
    void strongPassword_noMfaRequired_returnsSuccess() {
        scenario.setMfaRequired(false);
        scenarioRepo.save(scenario);

        SimulationEngine.LoginResult result =
                engine.processLoginAttempt(scenario.getId(), "player1", "StrongPass!123", 0);

        assertTrue(result.success);
        assertEquals(SimulationEngine.SessionState.SUCCESS, result.nextState);
        assertFalse(result.requiresMfa);
    }

    @Test
    void loginAttempt_logsEventToDatabase() {
        engine.processLoginAttempt(scenario.getId(), "player1", "StrongPass!123", 0);

        List<EventLog> logs = eventLogRepo.findByScenarioIdOrderByTimestampAsc(scenario.getId());
        assertFalse(logs.isEmpty(), "An event should have been logged");
        assertEquals("MFA_CHALLENGE_ISSUED", logs.get(0).getEventType());
    }

    @Test
    void lockout_logsLockoutEvent() {
        engine.processLoginAttempt(scenario.getId(), "player1", "anypassword", 3);

        List<EventLog> logs = eventLogRepo.findByScenarioIdOrderByTimestampAsc(scenario.getId());
        assertFalse(logs.isEmpty());
        assertEquals("LOCKOUT", logs.get(0).getEventType());
    }
}
