package com.unime.securegame.model;

import jakarta.persistence.*;

@Entity
@Table(name = "phishing_templates")
public class PhishingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2048)
    private String url;

    @Column(nullable = false)
    private boolean safe;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    private boolean enabled = true;

    public PhishingTemplate() {}

    public PhishingTemplate(String url, boolean safe, String reason) {
        this.url = url;
        this.safe = safe;
        this.reason = reason;
        this.enabled = true;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isSafe() { return safe; }
    public void setSafe(boolean safe) { this.safe = safe; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
