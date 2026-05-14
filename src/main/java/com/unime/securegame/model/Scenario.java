package com.unime.securegame.model;

import jakarta.persistence.*;

@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Max number of failed login attempts before lockout */
    private int lockoutThreshold = 3;

    /** Minimum Shannon-entropy score for a password to be accepted (0–100 scale) */
    private int minPasswordEntropy = 40;

    /** Whether this scenario requires MFA (TOTP or WebAuthn) */
    private boolean mfaRequired = true;

    /** Whether geo-location anomaly detection is active */
    private boolean geoCheckEnabled = false;

    /** Soft-delete support */
    private boolean deleted = false;

    public Scenario() {}

    public Scenario(String name, int lockoutThreshold, int minPasswordEntropy, boolean mfaRequired) {
        this.name = name;
        this.lockoutThreshold = lockoutThreshold;
        this.minPasswordEntropy = minPasswordEntropy;
        this.mfaRequired = mfaRequired;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLockoutThreshold() { return lockoutThreshold; }
    public void setLockoutThreshold(int lockoutThreshold) { this.lockoutThreshold = lockoutThreshold; }

    public int getMinPasswordEntropy() { return minPasswordEntropy; }
    public void setMinPasswordEntropy(int minPasswordEntropy) { this.minPasswordEntropy = minPasswordEntropy; }

    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }

    public boolean isGeoCheckEnabled() { return geoCheckEnabled; }
    public void setGeoCheckEnabled(boolean geoCheckEnabled) { this.geoCheckEnabled = geoCheckEnabled; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
