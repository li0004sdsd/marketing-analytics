package com.analytics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attribution_configs")
public class AttributionConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false)
    private AttributionModelType modelType = AttributionModelType.LAST_CLICK;

    @Column(name = "weight_config", columnDefinition = "TEXT")
    private String weightConfig;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AttributionModelType getModelType() { return modelType; }
    public void setModelType(AttributionModelType modelType) { this.modelType = modelType; }
    public String getWeightConfig() { return weightConfig; }
    public void setWeightConfig(String weightConfig) { this.weightConfig = weightConfig; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
