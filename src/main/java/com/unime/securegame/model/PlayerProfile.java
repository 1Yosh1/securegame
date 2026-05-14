package com.unime.securegame.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "player_profiles")
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private int score = 0;
    private int badgeCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** TOTP secret key associated with this player (generated on registration) */
    @Column(name = "totp_secret")
    private String totpSecret;

    public PlayerProfile() {}

    public PlayerProfile(String username) {
        this.username = username;
    }

    // Getters & Setters

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getBadgeCount() { return badgeCount; }
    public void setBadgeCount(int badgeCount) { this.badgeCount = badgeCount; }

    public Instant getCreatedAt() { return createdAt; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
}
