package com.unime.securegame.service;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class TotpSecretCipher implements AttributeConverter<String, String> {

    private static final String PREFIX = "enc:v1:";
    private static final int NONCE_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretKeySpec key;

    static {
        String configuredKey = System.getProperty("securegame.totp.encryption-key");
        if (configuredKey == null || configuredKey.isBlank()) {
            configuredKey = System.getenv("SECUREGAME_TOTP_ENCRYPTION_KEY");
        }
        if (configuredKey != null && !configuredKey.isBlank()) configure(configuredKey);
    }

    public TotpSecretCipher() {
    }

    public static synchronized void configure(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            key = null;
            return;
        }
        byte[] keyBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("TOTP encryption key must contain at least 32 UTF-8 bytes");
        }
        try {
            key = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(keyBytes), "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** Re-encrypts legacy plaintext through the JPA converter on save. */
    public static String encryptLegacyValue(String plaintext) {
        if (plaintext == null || plaintext.isBlank() || isEncrypted(plaintext)) return plaintext;
        return new TotpSecretCipher().convertToDatabaseColumn(plaintext);
    }

    @Override
    public String convertToDatabaseColumn(String secret) {
        if (secret == null || secret.isBlank() || isEncrypted(secret)) return secret;
        SecretKeySpec activeKey = requireKey();
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, activeKey, new GCMParameterSpec(128, nonce));
            byte[] ciphertext = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            byte[] packed = ByteBuffer.allocate(nonce.length + ciphertext.length).put(nonce).put(ciphertext).array();
            return PREFIX + Base64.getEncoder().encodeToString(packed);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt TOTP secret", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        if (storedValue == null || storedValue.isBlank() || !isEncrypted(storedValue)) return storedValue;
        SecretKeySpec activeKey = requireKey();
        try {
            byte[] packed = Base64.getDecoder().decode(storedValue.substring(PREFIX.length()));
            if (packed.length <= NONCE_BYTES) throw new IllegalArgumentException("Invalid encrypted TOTP secret");
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] nonce = new byte[NONCE_BYTES];
            buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, activeKey, new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to decrypt TOTP secret; verify the configured encryption key", e);
        }
    }

    private SecretKeySpec requireKey() {
        SecretKeySpec activeKey = key;
        if (activeKey == null) {
            throw new IllegalStateException("SECUREGAME_TOTP_ENCRYPTION_KEY must be configured before storing TOTP secrets");
        }
        return activeKey;
    }
}
