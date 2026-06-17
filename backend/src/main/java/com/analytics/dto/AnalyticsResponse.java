package com.analytics.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsResponse {
    private Long totalEvents;
    private Long totalUsers;
    private Long totalEventTypes;
    private List<Map<String, Object>> eventDistribution;
    private List<Map<String, Object>> pageDistribution;
    private List<TrendPoint> dailyTrend;

    public Long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(Long totalEvents) { this.totalEvents = totalEvents; }
    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
    public Long getTotalEventTypes() { return totalEventTypes; }
    public void setTotalEventTypes(Long totalEventTypes) { this.totalEventTypes = totalEventTypes; }
    public List<Map<String, Object>> getEventDistribution() { return eventDistribution; }
    public void setEventDistribution(List<Map<String, Object>> eventDistribution) { this.eventDistribution = eventDistribution; }
    public List<Map<String, Object>> getPageDistribution() { return pageDistribution; }
    public void setPageDistribution(List<Map<String, Object>> pageDistribution) { this.pageDistribution = pageDistribution; }
    public List<TrendPoint> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<TrendPoint> dailyTrend) { this.dailyTrend = dailyTrend; }
}
