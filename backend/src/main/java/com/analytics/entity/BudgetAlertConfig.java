package com.analytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_alert_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bac_campaign_id", columnNames = "campaign_id")
})
public class BudgetAlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "budget_limit", precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetLimit;

    @Column(name = "alert_threshold_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal alertThresholdPercent;

    @Column(name = "alert_channels", length = 512, nullable = false)
    private String alertChannels;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public BigDecimal getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(BigDecimal budgetLimit) { this.budgetLimit = budgetLimit; }
    public BigDecimal getAlertThresholdPercent() { return alertThresholdPercent; }
    public void setAlertThresholdPercent(BigDecimal alertThresholdPercent) {
        this.alertThresholdPercent = alertThresholdPercent;
    }
    public String getAlertChannels() { return alertChannels; }
    public void setAlertChannels(String alertChannels) { this.alertChannels = alertChannels; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
