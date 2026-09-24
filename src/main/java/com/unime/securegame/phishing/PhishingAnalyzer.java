package com.unime.securegame.phishing;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Category C — Phish-o-Meter: URL phishing detection engine.
 *
 * Analyzes a URL using multiple hand-crafted heuristics and produces:
 *   1. A phishing probability score [0.0, 1.0]
 *   2. A list of triggered indicators (for UI display)
 *   3. A risk band (SAFE / SUSPICIOUS / DANGEROUS)
 *
 * This is a purely algorithmic classifier — no external APIs or ML models.
 * Implements Category C because it orchestrates multiple detection heuristics
 * and fuses them into a final verdict.
 *
 * Heuristics implemented (Category B primitives used by this orchestrator):
 *   1. Domain length anomaly (long domains = phishing)
 *   2. IP-as-hostname detection
 *   3. Homograph / lookalike character detection (Punycode / xn--)
 *   4. Suspicious TLD list (country TLDs misused in phishing)
 *   5. Brand spoofing keyword detection (e.g. "paypa1", "micros0ft")
 *   6. URL path depth anomaly
 *   7. Excessive hyphens in domain
 *   8. HTTP (non-HTTPS) scheme
 *   9. Subdomain count anomaly
 *  10. Query parameter length anomaly (hidden encoded payloads)
 */
@Service
public class PhishingAnalyzer {

    // ── Suspicious TLD list ──────────────────────────────────────────────────
    private static final Set<String> SUSPICIOUS_TLDS = Set.of(
            ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".work", ".click",
            ".download", ".review", ".stream", ".gdn", ".bid", ".win"
    );

    // ── Brand keywords commonly spoofed ─────────────────────────────────────
    private static final Set<String> BRAND_KEYWORDS = Set.of(
            "paypal", "paypa1", "paypai", "amazon", "amaz0n", "microsoft", "micros0ft",
            "google", "g00gle", "apple", "app1e", "facebook", "faceb00k",
            "netflix", "netfl1x", "instagram", "twitter", "linkedin",
            "unime", "bankofamerica", "wellsfargo", "chase"
    );

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    /**
     * Analyze a URL for phishing indicators.
     *
     * @param rawUrl The URL string to analyze.
     * @return PhishingReport with score, band, and triggered indicators.
     */
    public PhishingReport analyze(String rawUrl) {
        List<String> indicators = new ArrayList<>();
        double score = 0.0;

        String url = rawUrl.trim();
        String host = extractHost(url);
        String path = extractPath(url);
        String query = extractQuery(url);

        score += checkHttp(url, indicators);
        score += checkIpAsHost(host, indicators);
        score += checkDomainLength(host, indicators);
        score += checkPunycode(host, indicators);
        score += checkSuspiciousTld(host, indicators);
        score += checkBrandSpoofing(url, indicators);
        score += checkExcessiveHyphens(host, indicators);
        score += checkPathDepth(path, indicators);
        score += checkSubdomainCount(host, indicators);
        score += checkQueryLength(query, indicators);

        score = Math.min(1.0, score);
        String band = toBand(score);

        return new PhishingReport(rawUrl, score, band, indicators);
    }


    private double checkHttp(String url, List<String> indicators) {
        if (url.startsWith("http://")) {
            indicators.add("Uses unencrypted HTTP (no HTTPS)");
            return 0.10;
        }
        return 0.0;
    }

    private double checkIpAsHost(String host, List<String> indicators) {
        if (host != null && IP_PATTERN.matcher(host).matches()) {
            indicators.add("Uses raw IP address instead of domain name");
            return 0.30;
        }
        return 0.0;
    }

    private double checkDomainLength(String host, List<String> indicators) {
        if (host != null && host.length() > 30) {
            indicators.add("Unusually long domain name (" + host.length() + " chars)");
            return 0.15;
        }
        return 0.0;
    }

    private double checkPunycode(String host, List<String> indicators) {
        if (host != null && host.contains("xn--")) {
            indicators.add("Punycode encoding detected (possible homograph attack)");
            return 0.20;
        }
        return 0.0;
    }

    private double checkSuspiciousTld(String host, List<String> indicators) {
        if (host != null) {
            String lowerHost = host.toLowerCase();
            for (String tld : SUSPICIOUS_TLDS) {
                if (lowerHost.endsWith(tld)) {
                    indicators.add("Suspicious top-level domain: " + tld);
                    return 0.15;
                }
            }
        }
        return 0.0;
    }

    private double checkBrandSpoofing(String url, List<String> indicators) {
        String lowerUrl = url.toLowerCase();
        for (String brand : BRAND_KEYWORDS) {
            if (lowerUrl.contains(brand)) {
                indicators.add("Potential brand spoofing keyword detected: '" + brand + "'");
                return 0.20;
            }
        }
        return 0.0;
    }

    private double checkExcessiveHyphens(String host, List<String> indicators) {
        if (host != null) {
            long hyphens = host.chars().filter(c -> c == '-').count();
            if (hyphens > 2) {
                indicators.add("Excessive hyphens in domain (" + hyphens + ")");
                return 0.10;
            }
        }
        return 0.0;
    }

    private double checkPathDepth(String path, List<String> indicators) {
        if (path != null) {
            long depth = path.chars().filter(c -> c == '/').count();
            if (depth > 5) {
                indicators.add("Deep URL path (" + depth + " segments) may indicate redirect chain");
                return 0.08;
            }
        }
        return 0.0;
    }

    private double checkSubdomainCount(String host, List<String> indicators) {
        if (host != null) {
            long dots = host.chars().filter(c -> c == '.').count();
            if (dots > 3) {
                indicators.add("Excessive subdomains (" + dots + " levels) — common in phishing");
                return 0.12;
            }
        }
        return 0.0;
    }

    private double checkQueryLength(String query, List<String> indicators) {
        if (query != null && query.length() > 200) {
            indicators.add("Very long query string (" + query.length() + " chars) — may hide malicious payload");
            return 0.10;
        }
        return 0.0;
    }

    private String extractHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractPath(String url) {
        try {
            return URI.create(url).getPath();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractQuery(String url) {
        try {
            return URI.create(url).getQuery();
        } catch (Exception e) {
            return null;
        }
    }

    private String toBand(double score) {
        if (score < 0.25) return "SAFE";
        if (score < 0.55) return "SUSPICIOUS";
        return "DANGEROUS";
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record PhishingReport(
            String analyzedUrl,
            double score,
            String band,
            List<String> indicators
    ) {
        public boolean isDangerous() { return "DANGEROUS".equals(band); }
        public boolean isSafe() { return "SAFE".equals(band); }
    }
}
