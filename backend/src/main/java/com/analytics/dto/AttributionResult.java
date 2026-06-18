package com.analytics.dto;

import java.util.List;
import java.util.Map;

public class AttributionResult {
    private String modelType;
    private List<TouchPointDTO> touchPoints;
    private Map<String, Double> channelWeights;

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public List<TouchPointDTO> getTouchPoints() { return touchPoints; }
    public void setTouchPoints(List<TouchPointDTO> touchPoints) { this.touchPoints = touchPoints; }
    public Map<String, Double> getChannelWeights() { return channelWeights; }
    public void setChannelWeights(Map<String, Double> channelWeights) { this.channelWeights = channelWeights; }
}
