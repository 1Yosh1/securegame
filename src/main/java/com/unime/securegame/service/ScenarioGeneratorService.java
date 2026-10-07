package com.unime.securegame.service;

import com.unime.securegame.model.PhishingTemplate;
import com.unime.securegame.repository.PhishingTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ScenarioGeneratorService — Pure-Java scenario content engine.
 *
 * Replaces the previous LlmService which called an external Gemini API endpoint.
 * All scenario content is generated entirely in-process from curated question banks.
 *
 * Design: Deterministic content selection with seeded randomisation.
 * No network calls. No external API dependency. No service mashup.
 *
 * Supports three scenario types:
 *   - "crypto"    → multiple-choice Q&A arrays (JSON)
 *   - "phishing"  → URL classification arrays (JSON)
 *   - "matching"  → term-definition pairs (JSON)
 */
@Service
public class ScenarioGeneratorService {

    // ── Question banks ────────────────────────────────────────────────────────

    private static final Map<String, List<String[]>> CRYPTO_BANKS = new LinkedHashMap<>();
    private static final Map<String, List<String[]>> MATCHING_BANKS = new LinkedHashMap<>();

    static {
        // CRYPTO bank: [question, optA, optB, optC, optD, correctIndex(0-based), explanation]
        List<String[]> mfaQ = new ArrayList<>();
        mfaQ.add(new String[]{"What does TOTP stand for?", "Time-based One-Time Password", "Token-Oriented Transfer Protocol", "Two-Operation Token Pass", "Timed OTP Protocol", "0", "TOTP generates a 6-digit code using the current time and a shared secret."});
        mfaQ.add(new String[]{"Which MFA factor is a fingerprint?", "Something you know", "Something you have", "Something you are", "Somewhere you are", "2", "Biometrics (fingerprint, face) are 'something you are' factors."});
        mfaQ.add(new String[]{"A push-bombing attack exploits which behaviour?", "Weak passwords", "Blind MFA approval", "Expired tokens", "SIM swapping", "1", "Push-bombing floods the user with MFA prompts hoping for a blind accept."});
        CRYPTO_BANKS.put("mfa", mfaQ);

        List<String[]> sqlQ = new ArrayList<>();
        sqlQ.add(new String[]{"Which input stops SQL injection?", "Blacklisting quotes", "Parameterised queries", "URL encoding", "Longer passwords", "1", "Parameterised queries prevent injection by treating input as data, never code."});
        sqlQ.add(new String[]{"' OR '1'='1 is an example of?", "XSS", "CSRF", "SQL Injection", "Buffer Overflow", "2", "This classic payload always evaluates to true, bypassing WHERE clauses."});
        sqlQ.add(new String[]{"Which database principle limits SQL injection blast radius?", "Normalisation", "Least privilege", "Indexing", "Sharding", "1", "Least privilege limits what the DB account can do even if exploited."});
        CRYPTO_BANKS.put("sqli", sqlQ);

        List<String[]> cryptoQ = new ArrayList<>();
        cryptoQ.add(new String[]{"AES-256 key size in bits?", "128", "192", "256", "512", "2", "AES-256 uses a 256-bit key, providing 2^256 possible keys."});
        cryptoQ.add(new String[]{"Which is asymmetric encryption?", "AES", "DES", "RSA", "ChaCha20", "2", "RSA uses a public/private key pair; the others are symmetric."});
        cryptoQ.add(new String[]{"What does a hash function guarantee?", "Reversibility", "Determinism", "Encryption", "Compression", "1", "The same input always produces the same digest (determinism)."});
        CRYPTO_BANKS.put("crypto", cryptoQ);
        CRYPTO_BANKS.put("crypto_decode", cryptoQ);

        List<String[]> privQ = new ArrayList<>();
        privQ.add(new String[]{"What is a kernel exploit?", "A web app flaw", "Code executing in OS ring-0", "A phishing lure", "A SQL payload", "1", "Kernel exploits run with maximum privilege inside the OS kernel."});
        privQ.add(new String[]{"SUID bit on a binary allows?", "Network access", "Running as file owner", "Writing to /etc", "Disabling SELinux", "1", "Set-UID causes the binary to run as its owner (often root)."});
        privQ.add(new String[]{"sudo -l reveals what?", "Firewall rules", "Allowed sudo commands", "Open ports", "Cron jobs", "1", "sudo -l lists the commands the current user may run as root."});
        CRYPTO_BANKS.put("privesc", privQ);

        List<String[]> fwQ = new ArrayList<>();
        fwQ.add(new String[]{"What does a stateful firewall track?", "Packet headers only", "Full TCP connection state", "DNS queries", "User sessions", "1", "Stateful firewalls track TCP state (SYN, ESTABLISHED, FIN) per flow."});
        fwQ.add(new String[]{"Port 443 is used for?", "HTTP", "FTP", "HTTPS/TLS", "SSH", "2", "Port 443 is the standard HTTPS (TLS-encrypted HTTP) port."});
        fwQ.add(new String[]{"An IDS differs from a firewall because it?", "Blocks traffic", "Only detects/alerts", "Encrypts packets", "Assigns IP addresses", "1", "IDS is passive (alert only); IPS actively blocks."});
        CRYPTO_BANKS.put("firewall", fwQ);

        List<String[]> ransomQ = new ArrayList<>();
        ransomQ.add(new String[]{"Ransomware's primary goal?", "Data exfiltration", "Encrypting files for ransom", "DDoS attacks", "Privilege escalation", "1", "Ransomware encrypts victim files and demands payment for the key."});
        ransomQ.add(new String[]{"Best defence against ransomware?", "Antivirus only", "Offline backups", "Stronger passwords", "Firewall rules", "1", "Offline backups allow recovery without paying the ransom."});
        CRYPTO_BANKS.put("ransomware", ransomQ);

        // MATCHING bank: [term1, term2, term3, def1, def2, def3]
        List<String[]> secTerms = new ArrayList<>();
        secTerms.add(new String[]{"Phishing", "Ransomware", "TOTP", "Fraudulent emails to steal credentials", "Malware that encrypts files for ransom", "Time-based One-Time Password algorithm"});
        secTerms.add(new String[]{"SQL Injection", "XSS", "CSRF", "Injecting SQL via unsanitised input", "Injecting scripts into web pages", "Forging requests on behalf of authenticated user"});
        secTerms.add(new String[]{"Firewall", "IDS", "VPN", "Network traffic filter", "Intrusion detection system", "Encrypted private tunnel over public network"});
        secTerms.add(new String[]{"Symmetric", "Asymmetric", "Hashing", "Same key for encrypt and decrypt", "Public/private key pair", "One-way digest function"});
        MATCHING_BANKS.put("default", secTerms);
    }

    private final PhishingTemplateRepository phishingTemplateRepository;
    private final ObjectMapper objectMapper;

    public ScenarioGeneratorService(PhishingTemplateRepository phishingTemplateRepository, ObjectMapper objectMapper) {
        this.phishingTemplateRepository = phishingTemplateRepository;
        this.objectMapper = objectMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generate scenario content for a given topic and type.
     * Returns a JSON string compatible with the existing frontend parsers.
     */
    public String generateScenario(String topic, String type) {
        return generateScenario(topic, type, 42L);
    }

    public String generateScenario(String topic, String type, long seed) {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return switch (type) {
            case "crypto" -> buildCryptoJson(topic, seed);
            case "phishing" -> buildPhishingJson(seed);
            case "matching" -> buildMatchingJson(seed);
            default -> throw new IllegalArgumentException("Unsupported scenario type: " + type);
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize generated scenario content", e);
        }
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private String buildCryptoJson(String topic, long seed) {
        List<String[]> bank = CRYPTO_BANKS.getOrDefault(topic.toLowerCase(Locale.ROOT),
                CRYPTO_BANKS.get("crypto"));
        List<String[]> shuffled = new ArrayList<>(bank);
        Collections.shuffle(shuffled, new Random(seed));
        List<Map<String, Object>> selected = shuffled.stream().limit(3).map(q -> Map.<String, Object>of(
                "q", q[0], "opts", List.of(q[1], q[2], q[3], q[4]),
                "ans", Integer.parseInt(q[5]), "expl", q[6])).toList();
        return toJson(selected);
    }

    private String buildPhishingJson(long seed) {
        List<PhishingTemplate> templates = new ArrayList<>(phishingTemplateRepository.findAllByEnabledTrueOrderByIdAsc());
        Collections.shuffle(templates, new Random(seed));
        List<Map<String, Object>> selected = templates.stream().limit(6).map(template -> Map.<String, Object>of(
                "url", template.getUrl(), "safe", template.isSafe(), "reason", template.getReason())).toList();
        return toJson(selected);
    }

    private String buildMatchingJson(long seed) {
        List<String[]> bank = MATCHING_BANKS.get("default");
        String[] row = bank.get(new Random(seed).nextInt(bank.size()));
        return toJson(Map.of("terms", List.of(row[0], row[1], row[2]),
                "defs", List.of(row[3], row[4], row[5])));
    }
}
