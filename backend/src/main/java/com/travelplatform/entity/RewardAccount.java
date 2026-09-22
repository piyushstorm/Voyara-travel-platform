package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = "user_id")
})
public class RewardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Current spendable points balance */
    @Column(nullable = false)
    private long pointsBalance = 0;

    /** Lifetime points ever earned */
    @Column(nullable = false)
    private long lifetimePointsEarned = 0;

    /** Lifetime points redeemed */
    @Column(nullable = false)
    private long lifetimePointsRedeemed = 0;

    /** Lifetime points expired */
    @Column(nullable = false)
    private long lifetimePointsExpired = 0;

    /** Current tier: SILVER, GOLD, PLATINUM */
    @Column(nullable = false)
    private String tier = "SILVER";

    /** Qualifying points for tier calculation (may differ from balance) */
    @Column(nullable = false)
    private long qualifyingPoints = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public RewardAccount() {}

    public RewardAccount(User user) {
        this.user = user;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public long getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(long pointsBalance) { this.pointsBalance = pointsBalance; }
    public long getLifetimePointsEarned() { return lifetimePointsEarned; }
    public void setLifetimePointsEarned(long v) { this.lifetimePointsEarned = v; }
    public long getLifetimePointsRedeemed() { return lifetimePointsRedeemed; }
    public void setLifetimePointsRedeemed(long v) { this.lifetimePointsRedeemed = v; }
    public long getLifetimePointsExpired() { return lifetimePointsExpired; }
    public void setLifetimePointsExpired(long v) { this.lifetimePointsExpired = v; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public long getQualifyingPoints() { return qualifyingPoints; }
    public void setQualifyingPoints(long qualifyingPoints) { this.qualifyingPoints = qualifyingPoints; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
