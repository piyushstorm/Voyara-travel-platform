package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks processed Razorpay webhook events for idempotency.
 * Prevents duplicate processing of the same event.
 */
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_event_id", columnList = "eventId", unique = true)
})
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Razorpay event ID (unique per event delivery) */
    @Column(nullable = false, unique = true)
    private String eventId;

    /** Event type: payment.captured, payment.failed, refund.processed, etc. */
    @Column(nullable = false)
    private String eventType;

    /** Payload summary for logging */
    @Column(length = 500)
    private String payloadSummary;

    /** PROCESSED, DUPLICATE, FAILED */
    @Column(nullable = false)
    private String status = "PROCESSED";

    @CreationTimestamp
    private LocalDateTime createdAt;

    public WebhookEvent() {}

    public WebhookEvent(String eventId, String eventType, String payloadSummary, String status) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payloadSummary = payloadSummary;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayloadSummary() { return payloadSummary; }
    public void setPayloadSummary(String payloadSummary) { this.payloadSummary = payloadSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
