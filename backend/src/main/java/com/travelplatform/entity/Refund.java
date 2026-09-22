package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks refund requests and their processing status.
 * Refunds flow: PENDING → PROCESSING → COMPLETED/REJECTED
 */
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Booking booking;

    @Column(nullable = false, unique = true)
    private String refundId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    /** Percentage of original amount refunded (0-100) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    /** Currency code e.g. INR */
    @Column(nullable = false, length = 10)
    private String currency = "INR";

    /** PENDING, PROCESSING, COMPLETED, REJECTED */
    @Column(nullable = false)
    private String status = "PENDING";

    private String cancellationReason;

    @Column(length = 500)
    private String cancellationComment;

    private String cancellationPolicy;

    private String rejectionReason;

    private String failureReason;

    private String externalRefundId;  // Razorpay refund ID
    private String razorpayRefundId;  // Razorpay refund ID (alias)

    private String idempotencyKey;

    private LocalDateTime processedAt;

    /** Expected completion time */
    private LocalDateTime expectedCompletionAt;

    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Refund() {}

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal amount) { this.refundAmount = amount; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal amount) { this.originalAmount = amount; }
    public BigDecimal getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(BigDecimal pct) { this.refundPercentage = pct; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String reason) { this.cancellationReason = reason; }
    public String getCancellationComment() { return cancellationComment; }
    public void setCancellationComment(String comment) { this.cancellationComment = comment; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public void setCancellationPolicy(String policy) { this.cancellationPolicy = policy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String reason) {
        this.rejectionReason = reason;
        this.failureReason = reason;
    }
    public String getFailureReason() { return failureReason != null ? failureReason : rejectionReason; }
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        this.rejectionReason = failureReason;
    }
    public String getExternalRefundId() { return externalRefundId; }
    public void setExternalRefundId(String id) {
        this.externalRefundId = id;
        if (this.razorpayRefundId == null) this.razorpayRefundId = id;
    }
    public String getRazorpayRefundId() { return razorpayRefundId; }
    public void setRazorpayRefundId(String id) {
        this.razorpayRefundId = id;
        if (this.externalRefundId == null) this.externalRefundId = id;
    }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String key) { this.idempotencyKey = key; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime t) { this.processedAt = t; }
    public LocalDateTime getExpectedCompletionAt() { return expectedCompletionAt; }
    public void setExpectedCompletionAt(LocalDateTime t) { this.expectedCompletionAt = t; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime t) { this.completedAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
