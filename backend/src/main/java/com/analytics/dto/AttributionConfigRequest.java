package com.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttributionConfigRequest {
    @NotBlank
    private String name;

    @NotNull
    private String modelType;

    private String weightConfig;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getWeightConfig() { return weightConfig; }
    public void setWeightConfig(String weightConfig) { this.weightConfig = weightConfig; }
}
