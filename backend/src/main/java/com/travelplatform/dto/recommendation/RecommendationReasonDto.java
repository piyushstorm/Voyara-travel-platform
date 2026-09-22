package com.travelplatform.dto.recommendation;

public class RecommendationReasonDto {
    private String type;
    private String label;
    private Double weight;

    public RecommendationReasonDto() {}

    public RecommendationReasonDto(String type, String label, Double weight) {
        this.type = type;
        this.label = label;
        this.weight = weight;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}
