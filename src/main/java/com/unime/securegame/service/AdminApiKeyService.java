package com.unime.securegame.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AdminApiKeyService {

    private final String configuredKey;
    private final boolean requireAdminKey;

    public AdminApiKeyService(@Value("${securegame.admin.api-key:}") String configuredKey,
                              @Value("${securegame.admin.require-key:false}") boolean requireAdminKey) {
        this.configuredKey = configuredKey;
        this.requireAdminKey = requireAdminKey;
    }

    public boolean isValid(String submittedKey) {
        if (requireAdminKey && (configuredKey == null || configuredKey.isBlank())) {
            throw new IllegalStateException("Admin API key is required but securegame.admin.api-key is not configured");
        }
        if (configuredKey == null || configuredKey.isBlank() || submittedKey == null) return false;
        return MessageDigest.isEqual(configuredKey.getBytes(StandardCharsets.UTF_8),
                submittedKey.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isRequired() {
        return requireAdminKey;
    }
}
