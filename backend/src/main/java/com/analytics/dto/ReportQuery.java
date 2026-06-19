package com.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReportQuery {
    private List<Long> campaignIds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long createdBy;

    public List<Long> getCampaignIds() { return campaignIds; }
    public void setCampaignIds(List<Long> campaignIds) { this.campaignIds = campaignIds; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
