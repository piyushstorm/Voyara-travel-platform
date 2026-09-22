package com.travelplatform.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Configurable cancellation policy tiers.
 * Each tier defines the refund percentage based on hours before departure/check-in.
 * Tiers are evaluated top-down; first matching tier wins.
 */
@Entity
@Table(name = "cancellation_policies")
public class CancellationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FLIGHT or HOTEL */
    @Column(nullable = false)
    private String entityType;

    /** Human-readable name like "More than 7 days" */
    @Column(nullable = false)
    private String name;

    /** Minimum hours before departure for this tier to apply (inclusive) */
    @Column(nullable = false)
    private int minHoursBefore;

    /** Maximum hours before departure for this tier (exclusive, 0 = no limit) */
    @Column(nullable = false)
    private int maxHoursBefore;

    /** Percentage of total amount that is refunded (0-100) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    /** Cancellation fee charged */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationFee;

    public CancellationPolicy() {}

    public CancellationPolicy(String entityType, String name, int minHoursBefore,
                              int maxHoursBefore, BigDecimal refundPercentage, BigDecimal cancellationFee) {
        this.entityType = entityType;
        this.name = name;
        this.minHoursBefore = minHoursBefore;
        this.maxHoursBefore = maxHoursBefore;
        this.refundPercentage = refundPercentage;
        this.cancellationFee = cancellationFee;
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getName() { return name; }
    public int getMinHoursBefore() { return minHoursBefore; }
    public int getMaxHoursBefore() { return maxHoursBefore; }
    public BigDecimal getRefundPercentage() { return refundPercentage; }
    public BigDecimal getCancellationFee() { return cancellationFee; }
}
