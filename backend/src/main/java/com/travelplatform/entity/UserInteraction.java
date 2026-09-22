package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks user interactions for the recommendation engine.
 * Used for both content-based (what attributes does the user prefer?)
 * and collaborative filtering (what have similar users liked?).
 */
@Entity
@Table(name = "user_interactions", indexes = {
    @Index(name = "idx_interaction_user", columnList = "user_id"),
    @Index(name = "idx_interaction_entity", columnList = "entityType, entityId")
})
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** FLIGHT or HOTEL */
    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    /** BOOKED, VIEWED, SEARCHED, SAVED, RATED, REVIEWED */
    @Column(nullable = false)
    private String interactionType;

    /** For ratings: 1-5. For others: null */
    private Integer rating;

    /** Optional tags for content-based scoring: beach, mountain, city, budget, luxury, etc. */
    @Column(length = 500)
    private String tags;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public UserInteraction() {}

    public UserInteraction(User user, String entityType, Long entityId, String interactionType) {
        this.user = user;
        this.entityType = entityType;
        this.entityId = entityId;
        this.interactionType = interactionType;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String type) { this.interactionType = type; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
