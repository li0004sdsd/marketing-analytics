package com.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttributionReportEntry {
    private Long campaignId;
    private String campaignName;
    private Long totalTouchPoints;
    private Long totalConversions;
    private BigDecimal totalWeight;
    private BigDecimal totalRevenue;
    private BigDecimal avgWeightPerTouch;
    private LocalDateTime firstTouchAt;
    private LocalDateTime lastTouchAt;
    private java.util.Map<String, BigDecimal> channelWeights;
    private java.util.List<TouchPointBreakdown> touchPoints;

    public AttributionReportEntry() {}

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public Long getTotalTouchPoints() { return totalTouchPoints; }
    public void setTotalTouchPoints(Long totalTouchPoints) { this.totalTouchPoints = totalTouchPoints; }
    public Long getTotalConversions() { return totalConversions; }
    public void setTotalConversions(Long totalConversions) { this.totalConversions = totalConversions; }
    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getAvgWeightPerTouch() { return avgWeightPerTouch; }
    public void setAvgWeightPerTouch(BigDecimal avgWeightPerTouch) { this.avgWeightPerTouch = avgWeightPerTouch; }
    public LocalDateTime getFirstTouchAt() { return firstTouchAt; }
    public void setFirstTouchAt(LocalDateTime firstTouchAt) { this.firstTouchAt = firstTouchAt; }
    public LocalDateTime getLastTouchAt() { return lastTouchAt; }
    public void setLastTouchAt(LocalDateTime lastTouchAt) { this.lastTouchAt = lastTouchAt; }
    public java.util.Map<String, BigDecimal> getChannelWeights() { return channelWeights; }
    public void setChannelWeights(java.util.Map<String, BigDecimal> channelWeights) { this.channelWeights = channelWeights; }
    public java.util.List<TouchPointBreakdown> getTouchPoints() { return touchPoints; }
    public void setTouchPoints(java.util.List<TouchPointBreakdown> touchPoints) { this.touchPoints = touchPoints; }

    public static class TouchPointBreakdown {
        private String userId;
        private String channel;
        private BigDecimal weight;
        private Integer order;
        private LocalDateTime touchTime;
        private String conversionId;
        private BigDecimal revenue;

        public TouchPointBreakdown() {}

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public LocalDateTime getTouchTime() { return touchTime; }
        public void setTouchTime(LocalDateTime touchTime) { this.touchTime = touchTime; }
        public String getConversionId() { return conversionId; }
        public void setConversionId(String conversionId) { this.conversionId = conversionId; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }
}
