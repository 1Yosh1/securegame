package com.unime.securegame.controller;

import com.unime.securegame.phishing.PhishingAnalyzer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/phishing")
public class PhishingController {

    private final PhishingAnalyzer analyzer;

    public PhishingController(PhishingAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Analyze a URL for phishing indicators.
     * Body: { "url": "http://paypa1-secure.tk/login?redirect=..." }
     */
    @PostMapping("/analyze")
    public ResponseEntity<PhishingAnalyzer.PhishingReport> analyze(@RequestBody UrlRequest req) {
        if (req.url() == null || req.url().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyzer.analyze(req.url()));
    }

    public record UrlRequest(String url) {}
}
