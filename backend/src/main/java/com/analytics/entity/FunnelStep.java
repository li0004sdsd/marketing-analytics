package com.analytics.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "funnel_steps")
public class FunnelStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funnel_id", nullable = false)
    private Funnel funnel;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private String stepName;

    @Column(nullable = false)
    private String eventName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Funnel getFunnel() { return funnel; }
    public void setFunnel(Funnel funnel) { this.funnel = funnel; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
}
