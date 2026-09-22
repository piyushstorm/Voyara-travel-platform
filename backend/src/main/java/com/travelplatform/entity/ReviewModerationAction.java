package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_moderation_actions", indexes = {
    @Index(name = "idx_mod_action_review", columnList = "review_id"),
    @Index(name = "idx_mod_action_moderator", columnList = "moderator_id"),
    @Index(name = "idx_mod_action_created", columnList = "created_at")
})
public class ReviewModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false, length = 50)
    private String action; // APPROVE, DISMISS_REPORTS, REMOVE, RESTORE, PHOTO_REMOVED, REPLY_REMOVED

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public ReviewModerationAction() {}

    public ReviewModerationAction(User moderator, Review review, String action, String reason) {
        this.moderator = moderator;
        this.review = review;
        this.action = action;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getModerator() { return moderator; }
    public void setModerator(User moderator) { this.moderator = moderator; }
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
