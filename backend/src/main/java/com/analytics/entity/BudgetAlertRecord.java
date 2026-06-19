package com.analytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_alert_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bar_campaign_day_threshold",
                columnNames = {"campaign_id", "alert_date", "alert_threshold_percent"})
}, indexes = {
        @Index(name = "idx_bar_campaign_day", columnList = "campaign_id, alert_date")
})
public class BudgetAlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "alert_date", nullable = false)
    private LocalDate alertDate;

    @Column(name = "alert_threshold_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal alertThresholdPercent;

    @Column(name = "actual_spend", precision = 18, scale = 2, nullable = false)
    private BigDecimal actualSpend;

    @Column(name = "budget_limit", precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetLimit;

    @Column(name = "alert_channels", length = 512, nullable = false)
    private String alertChannels;

    @Column(name = "alert_message", columnDefinition = "TEXT", nullable = false)
    private String alertMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public LocalDate getAlertDate() { return alertDate; }
    public void setAlertDate(LocalDate alertDate) { this.alertDate = alertDate; }
    public BigDecimal getAlertThresholdPercent() { return alertThresholdPercent; }
    public void setAlertThresholdPercent(BigDecimal alertThresholdPercent) {
        this.alertThresholdPercent = alertThresholdPercent;
    }
    public BigDecimal getActualSpend() { return actualSpend; }
    public void setActualSpend(BigDecimal actualSpend) { this.actualSpend = actualSpend; }
    public BigDecimal getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(BigDecimal budgetLimit) { this.budgetLimit = budgetLimit; }
    public String getAlertChannels() { return alertChannels; }
    public void setAlertChannels(String alertChannels) { this.alertChannels = alertChannels; }
    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
