package com.travelplatform.dto.recommendation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RecommendationFeedbackRequestDto {

    @NotBlank(message = "Feedback type is required")
    @Pattern(regexp = "^(HELPFUL|IRRELEVANT|NOT_HELPFUL)$", message = "Feedback must be HELPFUL or IRRELEVANT")
    private String feedback;

    private String entityType;
    private Long entityId;

    public RecommendationFeedbackRequestDto() {}

    public RecommendationFeedbackRequestDto(String feedback) {
        this.feedback = feedback;
    }

    public RecommendationFeedbackRequestDto(String feedback, String entityType, Long entityId) {
        this.feedback = feedback;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
}
