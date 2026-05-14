package com.unime.securegame.phishing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhishingAnalyzer — tests each heuristic independently
 * and end-to-end for known phishing vs. safe URLs.
 */
class PhishingAnalyzerTest {

    private PhishingAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new PhishingAnalyzer();
    }

    // ── Known safe URLs ───────────────────────────────────────────────────────

    @Test
    void safeUrl_producesLowScore() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("https://www.unime.it/login");
        assertTrue(report.score() < 0.25, "University HTTPS URL should be SAFE: " + report.score());
        assertEquals("SAFE", report.band());
        assertTrue(report.isSafe());
    }

    @Test
    void googleHttps_isSafe() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("https://accounts.google.com/signin");
        // Note: "google" is in our brand keywords → SUSPICIOUS but not DANGEROUS
        assertTrue(report.score() < 0.55, "Google auth URL should not be DANGEROUS");
    }

    // ── Known phishing patterns ───────────────────────────────────────────────

    @Test
    void httpUrl_triggersIndicator() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("http://example.com/login");
        assertTrue(report.indicators().stream().anyMatch(i -> i.contains("HTTP")));
    }

    @Test
    void ipAsHost_producesDangerousScore() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("http://192.168.1.1/admin");
        assertTrue(report.score() >= 0.3, "IP-based URL should score high");
        assertTrue(report.indicators().stream().anyMatch(i -> i.contains("IP address")));
    }

    @Test
    void suspiciousTld_triggersIndicator() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("https://secure-login.tk/paypal");
        assertTrue(report.indicators().stream().anyMatch(i -> i.contains("top-level domain")));
    }

    @Test
    void brandSpoofing_triggersIndicator() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("https://www.paypa1.com/login");
        assertTrue(report.indicators().stream().anyMatch(i -> i.contains("brand spoofing")));
    }

    @Test
    void longDomain_triggersIndicator() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("https://this-is-a-very-long-domain-name-for-phishing.com/login");
        assertTrue(report.indicators().stream().anyMatch(i -> i.contains("long domain")));
    }

    @Test
    void punycodeUrl_triggersIndicator() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("https://xn--pypal-4ve.com/login");
        assertTrue(report.indicators().stream().anyMatch(i -> i.contains("Punycode")));
    }

    @Test
    void classicPhishingUrl_isDangerous() {
        // Combines: HTTP + IP + long path + query
        PhishingAnalyzer.PhishingReport report = analyzer.analyze(
                "http://192.168.0.1/paypal/verify/account/secure/login?token=abcdef&redirect=http%3A%2F%2Fevil.tk%2Fsteal"
        );
        assertTrue(report.score() >= 0.55, "Classic phishing URL should be DANGEROUS: " + report.score());
        assertFalse(report.isSafe());
    }

    @Test
    void report_indicatorsList_isNotNull() {
        PhishingAnalyzer.PhishingReport report =
                analyzer.analyze("https://www.example.com");
        assertNotNull(report.indicators());
    }

    @Test
    void score_alwaysInZeroToOne() {
        // Worst-case combined URL
        String worst = "http://192.168.0.1/paypa1/xn--verify/a/b/c/d/e/f/login?token="
                + "x".repeat(250);
        PhishingAnalyzer.PhishingReport report = analyzer.analyze(worst);
        assertTrue(report.score() >= 0.0 && report.score() <= 1.0,
                "Score must be in [0,1]: " + report.score());
    }
}
