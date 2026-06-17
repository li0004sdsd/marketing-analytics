package com.analytics.dto;

import jakarta.validation.constraints.NotBlank;

public class BehaviorEventRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String eventName;
    private String page;
    private String sessionId;
    private String properties;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }
}
