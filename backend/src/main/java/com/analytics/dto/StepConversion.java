package com.analytics.dto;

public class StepConversion {
    private Integer stepOrder;
    private String stepName;
    private Long userCount;
    private Double conversionRate;
    private Double dropOffRate;

    public StepConversion() {}

    public StepConversion(Integer stepOrder, String stepName, Long userCount, Double conversionRate, Double dropOffRate) {
        this.stepOrder = stepOrder;
        this.stepName = stepName;
        this.userCount = userCount;
        this.conversionRate = conversionRate;
        this.dropOffRate = dropOffRate;
    }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
    public Double getDropOffRate() { return dropOffRate; }
    public void setDropOffRate(Double dropOffRate) { this.dropOffRate = dropOffRate; }
}
