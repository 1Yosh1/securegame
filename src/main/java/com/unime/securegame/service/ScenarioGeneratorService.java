package com.unime.securegame.service;

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
    private static final Map<String, List<String[]>> PHISHING_BANKS = new LinkedHashMap<>();
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

        // PHISHING bank: [url, isSafe(true/false), reason]
        List<String[]> phishUrls = new ArrayList<>();
        phishUrls.add(new String[]{"https://accounts.google.com/signin", "true", "Legitimate Google sign-in on the official domain."});
        phishUrls.add(new String[]{"http://g00gle-secure.com/login", "false", "Homograph substitution: zeros replace 'o' in google."});
        phishUrls.add(new String[]{"https://paypal.com/pay", "true", "Legitimate PayPal domain with HTTPS."});
        phishUrls.add(new String[]{"http://payp4l-account-verify.com", "false", "'4' substitutes 'a'; unknown TLD with 'verify' urgency pattern."});
        phishUrls.add(new String[]{"https://github.com/login", "true", "Official GitHub login page."});
        phishUrls.add(new String[]{"https://githubb.com/secure-login", "false", "Extra 'b' in domain — typosquatting."});
        phishUrls.add(new String[]{"https://microsoft.com/en-us/account", "true", "Official Microsoft domain."});
        phishUrls.add(new String[]{"http://microsofft-login.net/verify-account", "false", "Extra 'f', wrong TLD (.net), verification urgency."});
        phishUrls.add(new String[]{"https://amazon.com/orders", "true", "Official Amazon domain."});
        phishUrls.add(new String[]{"http://amaz0n-prime-suspend.com/reactivate", "false", "Zero substitution + suspension urgency = classic phishing."});
        PHISHING_BANKS.put("default", phishUrls);

        // MATCHING bank: [term1, term2, term3, def1, def2, def3]
        List<String[]> secTerms = new ArrayList<>();
        secTerms.add(new String[]{"Phishing", "Ransomware", "TOTP", "Fraudulent emails to steal credentials", "Malware that encrypts files for ransom", "Time-based One-Time Password algorithm"});
        secTerms.add(new String[]{"SQL Injection", "XSS", "CSRF", "Injecting SQL via unsanitised input", "Injecting scripts into web pages", "Forging requests on behalf of authenticated user"});
        secTerms.add(new String[]{"Firewall", "IDS", "VPN", "Network traffic filter", "Intrusion detection system", "Encrypted private tunnel over public network"});
        secTerms.add(new String[]{"Symmetric", "Asymmetric", "Hashing", "Same key for encrypt and decrypt", "Public/private key pair", "One-way digest function"});
        MATCHING_BANKS.put("default", secTerms);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generate scenario content for a given topic and type.
     * Returns a JSON string compatible with the existing frontend parsers.
     */
    public String generateScenario(String topic, String type) {
        return switch (type) {
            case "crypto"       -> buildCryptoJson(topic);
            case "phishing"     -> buildPhishingJson();
            case "matching"     -> buildMatchingJson();
            default             -> buildCryptoJson(topic);
        };
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private String buildCryptoJson(String topic) {
        List<String[]> bank = CRYPTO_BANKS.getOrDefault(topic.toLowerCase(),
                CRYPTO_BANKS.get("crypto"));
        Collections.shuffle(bank, new Random());
        List<String[]> selected = bank.subList(0, Math.min(3, bank.size()));

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < selected.size(); i++) {
            String[] q = selected.get(i);
            // q = [question, optA, optB, optC, optD, answerIndex, explanation]
            sb.append(String.format(
                "{\"q\":\"%s\",\"opts\":[\"%s\",\"%s\",\"%s\",\"%s\"],\"ans\":%s,\"expl\":\"%s\"}",
                escape(q[0]), escape(q[1]), escape(q[2]), escape(q[3]), escape(q[4]),
                q[5], escape(q[6])
            ));
            if (i < selected.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildPhishingJson() {
        List<String[]> bank = new ArrayList<>(PHISHING_BANKS.get("default"));
        Collections.shuffle(bank, new Random());
        List<String[]> selected = bank.subList(0, Math.min(6, bank.size()));

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < selected.size(); i++) {
            String[] u = selected.get(i);
            // u = [url, isSafe, reason]
            sb.append(String.format(
                "{\"url\":\"%s\",\"safe\":%s,\"reason\":\"%s\"}",
                escape(u[0]), u[1], escape(u[2])
            ));
            if (i < selected.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildMatchingJson() {
        List<String[]> bank = MATCHING_BANKS.get("default");
        String[] row = bank.get(new Random().nextInt(bank.size()));
        // row = [term1, term2, term3, def1, def2, def3]
        return String.format(
            "{\"terms\":[\"%s\",\"%s\",\"%s\"],\"defs\":[\"%s\",\"%s\",\"%s\"]}",
            escape(row[0]), escape(row[1]), escape(row[2]),
            escape(row[3]), escape(row[4]), escape(row[5])
        );
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
