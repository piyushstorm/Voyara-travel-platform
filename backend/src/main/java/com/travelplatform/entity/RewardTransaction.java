package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable ledger entry for all points changes.
 * Never modify historical entries — corrections create compensating transactions.
 */
@Entity
@Table(name = "reward_transactions", indexes = {
    @Index(name = "idx_rt_user", columnList = "user_id"),
    @Index(name = "idx_rt_type", columnList = "transactionType"),
    @Index(name = "idx_rt_booking", columnList = "bookingReference"),
    @Index(name = "idx_rt_unique_earning", columnList = "transactionType, bookingReference", unique = true)
})
public class RewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Points changed: positive for earn, negative for redeem/expiry */
    @Column(nullable = false)
    private long points;

    /** Transaction type: BOOKING_EARN, REDEMPTION, EXPIRY, ADMIN_ADJUSTMENT, REVERSAL, REFERRAL_BONUS */
    @Column(nullable = false)
    private String transactionType;

    /** Balance after this transaction */
    @Column(nullable = false)
    private long balanceAfter;

    /** Booking reference for earning/redemption context */
    private String bookingReference;

    /** For earning: the eligible amount that generated these points */
    @Column(precision = 12, scale = 2)
    private BigDecimal eligibleAmount;

    /** Description/reason */
    private String description;

    /** Admin who performed adjustment (if applicable) */
    private String performedBy;

    /** Idempotency key to prevent duplicate processing */
    @Column(unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public RewardTransaction() {}

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(long balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    public BigDecimal getEligibleAmount() { return eligibleAmount; }
    public void setEligibleAmount(BigDecimal eligibleAmount) { this.eligibleAmount = eligibleAmount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
