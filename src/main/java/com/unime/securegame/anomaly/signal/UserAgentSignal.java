package com.unime.securegame.anomaly.signal;

import com.unime.securegame.anomaly.AnomalySignal;
import com.unime.securegame.anomaly.SessionContext;
import org.springframework.stereotype.Component;

/**
 * Category C — User-Agent mismatch anomaly signal.
 *
 * Computes the Jaro-Winkler string similarity between the current and known
 * user-agent strings. A low similarity score = unfamiliar browser/OS = anomalous.
 *
 * Score = 1.0 - jaroWinkler(current, known)
 * (inverted: 0 = identical UA → no anomaly; 1 = completely different → full anomaly)
 */
@Component
public class UserAgentSignal implements AnomalySignal {

    private static final double WEIGHT = 0.20;

    @Override
    public double score(SessionContext ctx) {
        if (ctx.knownUserAgent() == null || ctx.currentUserAgent() == null) return 0.5;
        if (ctx.knownUserAgent().equals(ctx.currentUserAgent())) return 0.0;
        double similarity = jaroWinkler(ctx.currentUserAgent(), ctx.knownUserAgent());
        return Math.min(1.0, Math.max(0.0, 1.0 - similarity));
    }

    /**
     * Jaro-Winkler string similarity (pure Java, no external library).
     * Returns a value in [0.0, 1.0] where 1.0 = identical strings.
     */
    public double jaroWinkler(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        double jaro = jaro(s1, s2);
        // Count common prefix up to 4 characters
        int prefix = 0;
        int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));
        while (prefix < maxPrefix && s1.charAt(prefix) == s2.charAt(prefix)) prefix++;
        return jaro + prefix * 0.1 * (1.0 - jaro);
    }

    private double jaro(String s1, String s2) {
        int len1 = s1.length(), len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int matchWindow = Math.max(len1, len2) / 2 - 1;
        if (matchWindow < 0) matchWindow = 0;

        boolean[] matched1 = new boolean[len1];
        boolean[] matched2 = new boolean[len2];
        int matches = 0, transpositions = 0;

        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(i + matchWindow + 1, len2);
            for (int j = start; j < end; j++) {
                if (!matched2[j] && s1.charAt(i) == s2.charAt(j)) {
                    matched1[i] = true;
                    matched2[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) return 0.0;

        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (matched1[i]) {
                while (!matched2[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) transpositions++;
                k++;
            }
        }
        return (matches / (double) len1 + matches / (double) len2
                + (matches - transpositions / 2.0) / matches) / 3.0;
    }

    @Override public String name() { return "userAgentMismatch"; }
    @Override public double weight() { return WEIGHT; }
}
