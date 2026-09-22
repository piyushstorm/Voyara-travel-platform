package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Configurable rewards configuration.
 * Single-row table storing tier thresholds, earning/redemption rates, expiry rules.
 */
@Entity
@Table(name = "reward_config")
public class RewardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Points earned per 100 INR of eligible spend */
    @Column(nullable = false)
    private int pointsPerHundredRupees = 1;

    /** Points redeemable per 1 INR discount */
    @Column(nullable = false)
    private int pointsPerRupeeRedemption = 10;

    /** GOLD tier qualifying points threshold */
    @Column(nullable = false)
    private long goldTierThreshold = 5000;

    /** PLATINUM tier qualifying points threshold */
    @Column(nullable = false)
    private long platinumTierThreshold = 20000;

    /** GOLD multiplier on base earning rate */
    @Column(nullable = false)
    private double goldEarningMultiplier = 1.5;

    /** PLATINUM multiplier on base earning rate */
    @Column(nullable = false)
    private double platinumEarningMultiplier = 2.0;

    /** Points expiry in days (0 = never expire) */
    @Column(nullable = false)
    private int pointsExpiryDays = 365;

    /** Minimum points for a single redemption */
    @Column(nullable = false)
    private int minRedemptionPoints = 100;

    /** Maximum points that can be redeemed per booking (percentage of booking amount) */
    @Column(nullable = false)
    private int maxRedemptionPercent = 50;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public RewardConfig() {}

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public int getPointsPerHundredRupees() { return pointsPerHundredRupees; }
    public void setPointsPerHundredRupees(int v) { this.pointsPerHundredRupees = v; }
    public int getPointsPerRupeeRedemption() { return pointsPerRupeeRedemption; }
    public void setPointsPerRupeeRedemption(int v) { this.pointsPerRupeeRedemption = v; }
    public long getGoldTierThreshold() { return goldTierThreshold; }
    public void setGoldTierThreshold(long v) { this.goldTierThreshold = v; }
    public long getPlatinumTierThreshold() { return platinumTierThreshold; }
    public void setPlatinumTierThreshold(long v) { this.platinumTierThreshold = v; }
    public double getGoldEarningMultiplier() { return goldEarningMultiplier; }
    public void setGoldEarningMultiplier(double v) { this.goldEarningMultiplier = v; }
    public double getPlatinumEarningMultiplier() { return platinumEarningMultiplier; }
    public void setPlatinumEarningMultiplier(double v) { this.platinumEarningMultiplier = v; }
    public int getPointsExpiryDays() { return pointsExpiryDays; }
    public void setPointsExpiryDays(int v) { this.pointsExpiryDays = v; }
    public int getMinRedemptionPoints() { return minRedemptionPoints; }
    public void setMinRedemptionPoints(int v) { this.minRedemptionPoints = v; }
    public int getMaxRedemptionPercent() { return maxRedemptionPercent; }
    public void setMaxRedemptionPercent(int v) { this.maxRedemptionPercent = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
