package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Permanent feedback storage for the recommendation engine.
 * Records user ratings (HELPFUL or IRRELEVANT) for specific items.
 * Survives across recommendation recomputations and informs the learning pipeline.
 */
@Entity
@Table(name = "recommendation_feedback", indexes = {
    @Index(name = "idx_rec_feedback_user", columnList = "user_id"),
    @Index(name = "idx_rec_feedback_type", columnList = "feedback_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_entity_feedback", columnNames = {"user_id", "entity_type", "entity_id"})
})
public class RecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recommendation_id")
    private Long recommendationId;

    /** HOTEL, FLIGHT, DESTINATION */
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** HELPFUL or IRRELEVANT */
    @Column(name = "feedback_type", nullable = false, length = 30)
    private String feedbackType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RecommendationFeedback() {}

    public RecommendationFeedback(User user, Long recommendationId, String entityType, Long entityId, String feedbackType) {
        this.user = user;
        this.recommendationId = recommendationId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.feedbackType = feedbackType;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getRecommendationId() { return recommendationId; }
    public void setRecommendationId(Long recommendationId) { this.recommendationId = recommendationId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
