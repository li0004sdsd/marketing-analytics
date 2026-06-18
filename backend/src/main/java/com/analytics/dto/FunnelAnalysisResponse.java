package com.analytics.dto;

import java.util.List;

public class FunnelAnalysisResponse {
    private Long funnelId;
    private String funnelName;
    private List<StepConversion> stepConversions;
    private Long totalUsers;
    private Double overallConversionRate;

    public Long getFunnelId() { return funnelId; }
    public void setFunnelId(Long funnelId) { this.funnelId = funnelId; }
    public String getFunnelName() { return funnelName; }
    public void setFunnelName(String funnelName) { this.funnelName = funnelName; }
    public List<StepConversion> getStepConversions() { return stepConversions; }
    public void setStepConversions(List<StepConversion> stepConversions) { this.stepConversions = stepConversions; }
    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
    public Double getOverallConversionRate() { return overallConversionRate; }
    public void setOverallConversionRate(Double overallConversionRate) { this.overallConversionRate = overallConversionRate; }
}
