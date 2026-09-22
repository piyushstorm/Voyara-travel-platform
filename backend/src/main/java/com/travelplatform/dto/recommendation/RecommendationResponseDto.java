package com.travelplatform.dto.recommendation;

import java.util.Map;

public class RecommendationResponseDto {
    private Long id;
    private String entityType;
    private Long entityId;
    private Double score;
    private String algorithm;
    private String headline;
    private WhyRecommendationDto why;
    private String feedback;
    private Map<String, Object> itemDetails;

    public RecommendationResponseDto() {}

    public RecommendationResponseDto(Long id, String entityType, Long entityId, Double score,
                                   String algorithm, String headline, WhyRecommendationDto why,
                                   String feedback, Map<String, Object> itemDetails) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.score = score;
        this.algorithm = algorithm;
        this.headline = headline;
        this.why = why;
        this.feedback = feedback;
        this.itemDetails = itemDetails;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getReason() { return headline; }
    public void setReason(String reason) { this.headline = reason; }
    public WhyRecommendationDto getWhy() { return why; }
    public void setWhy(WhyRecommendationDto why) { this.why = why; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Map<String, Object> getItemDetails() { return itemDetails; }
    public void setItemDetails(Map<String, Object> itemDetails) { this.itemDetails = itemDetails; }
}
