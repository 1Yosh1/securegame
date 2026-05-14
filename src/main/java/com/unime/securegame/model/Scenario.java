package com.unime.securegame.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int lockoutThreshold;
    private int minPasswordEntropy;
    private boolean mfaRequired;

    public Scenario() {
    }

    public Scenario(String name, int lockoutThreshold, int minPasswordEntropy, boolean mfaRequired) {
        this.name = name;
        this.lockoutThreshold = lockoutThreshold;
        this.minPasswordEntropy = minPasswordEntropy;
        this.mfaRequired = mfaRequired;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLockoutThreshold() {
        return lockoutThreshold;
    }

    public void setLockoutThreshold(int lockoutThreshold) {
        this.lockoutThreshold = lockoutThreshold;
    }

    public int getMinPasswordEntropy() {
        return minPasswordEntropy;
    }

    public void setMinPasswordEntropy(int minPasswordEntropy) {
        this.minPasswordEntropy = minPasswordEntropy;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }
}
