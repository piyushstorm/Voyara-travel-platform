package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks temporary hotel room holds with expiry.
 * Concurrency-safe reservation before checkout/payment.
 */
@Entity
@Table(name = "room_holds", indexes = {
    @Index(name = "idx_room_hold_user", columnList = "user_id"),
    @Index(name = "idx_room_hold_room", columnList = "room_id"),
    @Index(name = "idx_room_hold_status", columnList = "status"),
    @Index(name = "idx_room_hold_expires", columnList = "expires_at")
})
public class RoomHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

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

    public RoomHold() {}

    public RoomHold(Room room, User user, int holdMinutes) {
        this.room = room;
        this.user = user;
        this.expiresAt = LocalDateTime.now().plusMinutes(holdMinutes);
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
