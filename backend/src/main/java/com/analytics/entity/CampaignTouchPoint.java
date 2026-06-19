package com.analytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_touch_points", indexes = {
        @Index(name = "idx_ctp_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_ctp_user_id", columnList = "user_id")
})
public class CampaignTouchPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String channel;

    @Column(name = "touch_time", nullable = false)
    private LocalDateTime touchTime;

    @Column(name = "touch_order", nullable = false)
    private Integer touchOrder;

    @Column(name = "attribution_weight", precision = 10, scale = 6, nullable = false)
    private BigDecimal attributionWeight;

    @Column(name = "conversion_id")
    private String conversionId;

    @Column(name = "revenue", precision = 18, scale = 2)
    private BigDecimal revenue;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public LocalDateTime getTouchTime() { return touchTime; }
    public void setTouchTime(LocalDateTime touchTime) { this.touchTime = touchTime; }
    public Integer getTouchOrder() { return touchOrder; }
    public void setTouchOrder(Integer touchOrder) { this.touchOrder = touchOrder; }
    public BigDecimal getAttributionWeight() { return attributionWeight; }
    public void setAttributionWeight(BigDecimal attributionWeight) { this.attributionWeight = attributionWeight; }
    public String getConversionId() { return conversionId; }
    public void setConversionId(String conversionId) { this.conversionId = conversionId; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
