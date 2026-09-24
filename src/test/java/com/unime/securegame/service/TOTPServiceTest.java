package com.unime.securegame.service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TOTPServiceTest {

    private TOTPService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TOTPService();
    }

    @Test
    void generateSecret_returnsValidBase32String() {
        String secret = totpService.generateSecret();

        assertNotNull(secret, "Generated secret should not be null");
        assertFalse(secret.isEmpty(), "Generated secret should not be empty");
        assertEquals(32, secret.length(), "Default generated secret should be 32 characters long");
        assertTrue(secret.matches("^[A-Z2-7]+$"), "Secret should be a valid Base32 string");
    }

    @Test
    void verifyCode_withValidCode_returnsTrue() throws Exception {
        String secret = totpService.generateSecret();

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);

        // Generate the code for the current time
        long currentBucket = Math.floorDiv(timeProvider.getTime(), 30);
        String validCode = codeGenerator.generate(secret, currentBucket);

        assertTrue(totpService.verifyCode(secret, validCode), "Valid code should be verified successfully");
    }

    @Test
    void verifyCode_withInvalidCode_returnsFalse() {
        String secret = totpService.generateSecret();
        String invalidCode = "000000";

        assertFalse(totpService.verifyCode(secret, invalidCode), "Invalid code should fail verification");
    }
}
