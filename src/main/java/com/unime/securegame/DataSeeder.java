package com.unime.securegame;

import com.unime.securegame.model.CoachFeedback;
import com.unime.securegame.model.ModelWeight;
import com.unime.securegame.model.PhishingTemplate;
import com.unime.securegame.repository.CoachFeedbackRepository;
import com.unime.securegame.repository.ModelWeightRepository;
import com.unime.securegame.repository.PhishingTemplateRepository;
import com.unime.securegame.repository.PlayerProfileRepository;
import com.unime.securegame.risk.LogisticRegressionModel;
import com.unime.securegame.risk.RuleBasedLayer;
import com.unime.securegame.risk.SyntheticDataGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import com.unime.securegame.service.TotpSecretCipher;

import java.util.List;

/**
 * Seeds the database with initial model weights and coach feedback tips on startup.
 * This runs once if the DB is empty (H2 is reset each run; MariaDB persists).
 */
@Configuration
public class DataSeeder {

    @Bean
    @Order(0)
    CommandLineRunner configureTotpSecretEncryption(
            @Value("${securegame.totp.encryption-key:}") String encryptionKey, Environment environment) {
        return args -> {
            boolean production = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
            if (production && (encryptionKey == null || encryptionKey.isBlank())) {
                throw new IllegalStateException("SECUREGAME_TOTP_ENCRYPTION_KEY is required in the prod profile");
            }
            TotpSecretCipher.configure(encryptionKey);
        };
    }

    @Bean
    @Order(1)
    CommandLineRunner seedData(ModelWeightRepository weightRepo,
                               CoachFeedbackRepository coachRepo,
                               SyntheticDataGenerator dataGen,
                               LogisticRegressionModel lrModel,
                               PhishingTemplateRepository phishingTemplateRepo) {
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
                weightRepo.save(new ModelWeight("riskVectorVersion", -2, 2.0));
            } else {
                // Version 1 stored password strength (higher is safer); version 2 stores
                // risk (higher is more anomalous). Transform legacy LR coefficient and bias
                // so persisted models preserve their predictions under xRisk = 1 - xStrength.
                if (weightRepo.findByFeatureName("riskVectorVersion").isEmpty()) {
                    var entropyWeight = weightRepo.findByFeatureName("passwordEntropy");
                    if (entropyWeight.isPresent()) {
                        double oldWeight = entropyWeight.get().getWeight();
                        entropyWeight.get().setWeight(-oldWeight);
                        weightRepo.save(entropyWeight.get());
                        weightRepo.findByFeatureName("bias").ifPresent(bias -> {
                            bias.setWeight(bias.getWeight() + oldWeight);
                            weightRepo.save(bias);
                        });
                    }
                    weightRepo.save(new ModelWeight("riskVectorVersion", -2, 2.0));
                }
            }

            // Move the curated phishing exercise bank into persistent storage on first startup.
            if (phishingTemplateRepo.count() == 0) {
                phishingTemplateRepo.saveAll(List.of(
                    new PhishingTemplate("https://accounts.google.com/signin", true, "Legitimate Google sign-in on the official domain."),
                    new PhishingTemplate("http://g00gle-secure.com/login", false, "Homograph substitution: zeros replace 'o' in google."),
                    new PhishingTemplate("https://paypal.com/pay", true, "Legitimate PayPal domain with HTTPS."),
                    new PhishingTemplate("http://payp4l-account-verify.com", false, "'4' substitutes 'a'; unknown TLD with 'verify' urgency pattern."),
                    new PhishingTemplate("https://github.com/login", true, "Official GitHub domain."),
                    new PhishingTemplate("https://githubb.com/secure-login", false, "Extra 'b' in domain — typosquatting."),
                    new PhishingTemplate("https://microsoft.com/en-us/account", true, "Official Microsoft domain."),
                    new PhishingTemplate("http://microsofft-login.net/verify-account", false, "Extra 'f', wrong TLD (.net), verification urgency."),
                    new PhishingTemplate("https://amazon.com/orders", true, "Official Amazon domain."),
                    new PhishingTemplate("http://amaz0n-prime-suspend.com/reactivate", false, "Zero substitution + suspension urgency = classic phishing.")
                ));
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

    @Bean
    @Order(2)
    CommandLineRunner encryptLegacyTotpSecrets(PlayerProfileRepository playerProfileRepository) {
        return args -> {
            long batchSize = 100;
            for (long offset = 0, total = playerProfileRepository.count(); offset < total; offset += batchSize) {
                List<com.unime.securegame.model.PlayerProfile> batch = playerProfileRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(
                                Math.toIntExact(offset / batchSize), Math.toIntExact(batchSize))).getContent();
                batch.forEach(profile -> {
                    String storedValue = profile.getTotpSecret();
                    if (storedValue != null && !storedValue.isBlank() && !TotpSecretCipher.isEncrypted(storedValue)) {
                        profile.setTotpSecret(TotpSecretCipher.encryptLegacyValue(storedValue));
                        playerProfileRepository.saveAndFlush(profile);
                    }
                });
            }
        };
    }
}
