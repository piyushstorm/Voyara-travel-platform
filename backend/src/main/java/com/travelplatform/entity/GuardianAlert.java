package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "guardian_alerts", indexes = {
    @Index(name = "idx_guardian_alert_user", columnList = "user_id"),
    @Index(name = "idx_guardian_alert_booking", columnList = "booking_id"),
    @Index(name = "idx_guardian_alert_unread", columnList = "user_id, is_read")
})
public class GuardianAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String alertType;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String actionLabel;
    private String actionRoute;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_flight_id")
    private Flight relatedFlight;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private boolean isDismissed = false;

    @Column(unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    public GuardianAlert() {}

    // Getters and Setters
    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String label) { this.actionLabel = label; }
    public String getActionRoute() { return actionRoute; }
    public void setActionRoute(String route) { this.actionRoute = route; }
    public Flight getRelatedFlight() { return relatedFlight; }
    public void setRelatedFlight(Flight flight) { this.relatedFlight = flight; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public boolean isDismissed() { return isDismissed; }
    public void setDismissed(boolean dismissed) { isDismissed = dismissed; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String key) { this.idempotencyKey = key; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
}
