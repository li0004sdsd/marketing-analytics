package com.analytics.dto;

import jakarta.validation.constraints.NotBlank;

public class EventTypeRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String category;
    private Boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
