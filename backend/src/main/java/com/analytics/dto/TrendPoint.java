package com.analytics.dto;

public class TrendPoint {
    private String date;
    private Long count;

    public TrendPoint(String date, Long count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() { return date; }
    public Long getCount() { return count; }
}
