package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Computed recommendation for a user.
 * Batch-scheduled — not computed live.
 * Stores the recommendation type, score, and reasoning.
 */
@Entity
@Table(name = "recommendations", indexes = {
    @Index(name = "idx_rec_user", columnList = "user_id"),
    @Index(name = "idx_rec_type", columnList = "entityType")
})
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** FLIGHT or HOTEL */
    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    /** Overall recommendation score (0-100) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    /** CONTENT_BASED, COLLABORATIVE, HYBRID */
    @Column(nullable = false)
    private String algorithm;

    /** Human-readable reason for this recommendation */
    @Column(nullable = false, length = 500)
    private String reason;

    /** Serialized structured reasons JSON (array of type, label, weight) */
    @Column(name = "structured_reasons_json", columnDefinition = "TEXT")
    private String structuredReasonsJson;

    /** User feedback: HELPFUL, IRRELEVANT, or null */
    private String feedback;

    /** Batch computation cycle this was generated in */
    @JsonIgnore
    private String batchCycle;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Recommendation() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String type) { this.entityType = type; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long id) { this.entityId = id; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStructuredReasonsJson() { return structuredReasonsJson; }
    public void setStructuredReasonsJson(String json) { this.structuredReasonsJson = json; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getBatchCycle() { return batchCycle; }
    public void setBatchCycle(String cycle) { this.batchCycle = cycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
