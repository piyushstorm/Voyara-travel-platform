package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks temporary seat holds with expiry.
 * Used alongside Seat.heldUntil for a dual-layer approach:
 * - Seat.heldUntil is the fast-check flag
 * - SeatHold is the audit trail + prevents hold stacking
 *
 * Concurrency: Seat uses @Version (optimistic) + PESSIMISTIC_WRITE in the service layer.
 */
@Entity
@Table(name = "seat_holds", indexes = {
    @Index(name = "idx_hold_user", columnList = "user_id"),
    @Index(name = "idx_hold_seat", columnList = "seat_id"),
    @Index(name = "idx_hold_expiry", columnList = "expires_at")
})
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** ACTIVE, EXPIRED, CONFIRMED, RELEASED */
    @Column(nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    private LocalDateTime createdAt;

    public SeatHold() {}

    public SeatHold(Seat seat, User user, int holdMinutes) {
        this.seat = seat;
        this.user = user;
        this.expiresAt = LocalDateTime.now().plusMinutes(holdMinutes);
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime t) { this.expiresAt = t; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
