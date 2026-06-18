package com.analytics.dto;

public class TouchPointDTO {
    private String channel;
    private int order;
    private double weight;

    public TouchPointDTO() {}

    public TouchPointDTO(String channel, int order, double weight) {
        this.channel = channel;
        this.order = order;
        this.weight = weight;
    }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
