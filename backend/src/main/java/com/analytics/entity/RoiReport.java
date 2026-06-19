package com.analytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "roi_reports")
public class RoiReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportName;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(precision = 18, scale = 2)
    private BigDecimal revenue;

    @Column(precision = 18, scale = 2)
    private BigDecimal cost;

    @Column(precision = 10, scale = 4)
    private BigDecimal roi;

    @Column(nullable = false)
    private LocalDateTime reportDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getRoi() { return roi; }
    public void setRoi(BigDecimal roi) { this.roi = roi; }
    public LocalDateTime getReportDate() { return reportDate; }
    public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
