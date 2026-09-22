package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user_created", columnList = "user_id, createdAt"),
    @Index(name = "idx_notif_user_read", columnList = "user_id, isRead"),
    @Index(name = "idx_notif_idempotency", columnList = "idempotencyKey", unique = true)
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Notification type: BOOKING_CONFIRMED, PAYMENT_SUCCESSFUL, etc. */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Whether the user has read this notification */
    @Column(nullable = false)
    private boolean isRead = false;

    /** Timestamp when user read the notification */
    private LocalDateTime readAt;

    /** Related entity type: BOOKING, FLIGHT, HOTEL, etc. */
    private String relatedEntityType;

    /** Related entity ID */
    private Long relatedEntityId;

    /** Related entity reference (e.g., booking reference) */
    private String relatedEntityRef;

    /** Delivery channel: IN_APP, EMAIL, BOTH */
    @Column(nullable = false)
    private String channel = "IN_APP";

    /** Delivery status: PENDING, SENT, FAILED */
    @Column(nullable = false)
    private String deliveryStatus = "SENT";

    /** Email delivery attempts */
    private int emailAttempts = 0;

    /** Email failure reason */
    private String emailFailureReason;

    /** Idempotency key to prevent duplicate notifications */
    @Column(unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Notification() {}

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public void setRelatedEntityType(String type) { this.relatedEntityType = type; }
    public Long getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(Long id) { this.relatedEntityId = id; }
    public String getRelatedEntityRef() { return relatedEntityRef; }
    public void setRelatedEntityRef(String ref) { this.relatedEntityRef = ref; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String status) { this.deliveryStatus = status; }
    public int getEmailAttempts() { return emailAttempts; }
    public void setEmailAttempts(int attempts) { this.emailAttempts = attempts; }
    public String getEmailFailureReason() { return emailFailureReason; }
    public void setEmailFailureReason(String reason) { this.emailFailureReason = reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String key) { this.idempotencyKey = key; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
