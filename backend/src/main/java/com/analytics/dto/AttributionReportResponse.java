package com.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AttributionReportResponse {
    private String reportId;
    private LocalDateTime generatedAt;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private Long teamId;
    private Integer campaignCount;
    private Long totalTouchPoints;
    private Long totalConversions;
    private BigDecimal grandTotalWeight;
    private BigDecimal grandTotalRevenue;
    private List<AttributionReportEntry> campaigns;

    public AttributionReportResponse() {}

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getRangeStart() { return rangeStart; }
    public void setRangeStart(LocalDateTime rangeStart) { this.rangeStart = rangeStart; }
    public LocalDateTime getRangeEnd() { return rangeEnd; }
    public void setRangeEnd(LocalDateTime rangeEnd) { this.rangeEnd = rangeEnd; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Integer getCampaignCount() { return campaignCount; }
    public void setCampaignCount(Integer campaignCount) { this.campaignCount = campaignCount; }
    public Long getTotalTouchPoints() { return totalTouchPoints; }
    public void setTotalTouchPoints(Long totalTouchPoints) { this.totalTouchPoints = totalTouchPoints; }
    public Long getTotalConversions() { return totalConversions; }
    public void setTotalConversions(Long totalConversions) { this.totalConversions = totalConversions; }
    public BigDecimal getGrandTotalWeight() { return grandTotalWeight; }
    public void setGrandTotalWeight(BigDecimal grandTotalWeight) { this.grandTotalWeight = grandTotalWeight; }
    public BigDecimal getGrandTotalRevenue() { return grandTotalRevenue; }
    public void setGrandTotalRevenue(BigDecimal grandTotalRevenue) { this.grandTotalRevenue = grandTotalRevenue; }
    public List<AttributionReportEntry> getCampaigns() { return campaigns; }
    public void setCampaigns(List<AttributionReportEntry> campaigns) { this.campaigns = campaigns; }
}
