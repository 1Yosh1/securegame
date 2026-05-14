package com.unime.securegame;

import com.unime.securegame.model.CoachFeedback;
import com.unime.securegame.model.ModelWeight;
import com.unime.securegame.repository.CoachFeedbackRepository;
import com.unime.securegame.repository.ModelWeightRepository;
import com.unime.securegame.risk.LogisticRegressionModel;
import com.unime.securegame.risk.RuleBasedLayer;
import com.unime.securegame.risk.SyntheticDataGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Seeds the database with initial model weights and coach feedback tips on startup.
 * This runs once if the DB is empty (H2 is reset each run; MariaDB persists).
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(ModelWeightRepository weightRepo,
                               CoachFeedbackRepository coachRepo,
                               SyntheticDataGenerator dataGen,
                               LogisticRegressionModel lrModel) {
        return args -> {
            // Seed model weights if DB is empty
            if (weightRepo.count() == 0) {
                String[] names = RuleBasedLayer.FEATURE_NAMES;
                double[] defaults = {0.15, 0.25, 0.15, 0.20, 0.15, 0.10};
                for (int i = 0; i < names.length; i++) {
                    weightRepo.save(new ModelWeight(names[i], i, defaults[i]));
                }
                // Train LR model on synthetic data immediately
                var samples = dataGen.generate(3000, 42L);
                lrModel.train(samples, 300, 0.1);
                System.out.println("[SecureGame] Model trained on 3000 synthetic samples.");
            }

            // Seed coach feedback tips if DB is empty
            if (coachRepo.count() == 0) {
                List<CoachFeedback> tips = List.of(
                    // passwordEntropy
                    new CoachFeedback("passwordEntropy", "HIGH",
                        "🔐 Your password is very weak! Use at least 12 characters with a mix of uppercase, lowercase, numbers, and symbols. Strong entropy is your first line of defence."),
                    new CoachFeedback("passwordEntropy", "MEDIUM",
                        "⚠️ Consider strengthening your password. Aim for 16+ characters and avoid dictionary words."),
                    new CoachFeedback("passwordEntropy", "LOW",
                        "✅ Good password strength. Keep it up!"),
                    // geoDistanceKm
                    new CoachFeedback("geoDistanceKm", "HIGH",
                        "🌍 Login from an unusual location detected! This is thousands of km from your usual location — a classic attacker indicator. Always verify logins from unknown locations."),
                    new CoachFeedback("geoDistanceKm", "MEDIUM",
                        "📍 Login location is somewhat unusual. If this wasn't you, change your password immediately."),
                    // timeDeviationHours
                    new CoachFeedback("timeDeviationHours", "HIGH",
                        "🕛 Login at an unusual hour! Attackers often work in the victim's off-hours (night/early morning). Step-up authentication is triggered."),
                    new CoachFeedback("timeDeviationHours", "MEDIUM",
                        "⏰ Slightly unusual login time. No action needed, but watch for patterns."),
                    // uaSimilarity
                    new CoachFeedback("uaSimilarity", "HIGH",
                        "💻 Unknown device or browser detected! Your User-Agent doesn't match your registered devices. This could indicate session hijacking or a new attacker device."),
                    new CoachFeedback("uaSimilarity", "MEDIUM",
                        "🔎 Slightly different browser/OS signature. Could be a software update — confirm if this is expected."),
                    // failStreak
                    new CoachFeedback("failStreak", "HIGH",
                        "🚨 Multiple consecutive login failures! This pattern matches a credential-stuffing or brute-force attack. Account lockout policy is now active."),
                    new CoachFeedback("failStreak", "MEDIUM",
                        "⚠️ Several failed attempts. If this wasn't you, your credentials may be compromised."),
                    // mfaType
                    new CoachFeedback("mfaType", "HIGH",
                        "🔓 No MFA configured! Multi-Factor Authentication is the single most effective defence against account takeover. Enable TOTP or WebAuthn immediately."),
                    new CoachFeedback("mfaType", "MEDIUM",
                        "📱 You're using SMS/TOTP. Consider upgrading to hardware security keys (WebAuthn/FIDO2) for phishing-resistant authentication.")
                );
                coachRepo.saveAll(tips);
                System.out.println("[SecureGame] Seeded " + tips.size() + " coach feedback tips.");
            }
        };
    }
}
