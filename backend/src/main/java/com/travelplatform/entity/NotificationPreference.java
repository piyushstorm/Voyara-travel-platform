package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = "user_id")
})
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Email preferences
    @Column(nullable = false)
    private boolean emailBookingUpdates = true;

    @Column(nullable = false)
    private boolean emailPaymentUpdates = true;

    @Column(nullable = false)
    private boolean emailCancellationRefund = true;

    @Column(nullable = false)
    private boolean emailFlightUpdates = true;

    @Column(nullable = false)
    private boolean emailPriceAlerts = false;

    @Column(nullable = false)
    private boolean emailMarketing = false;

    // In-app preferences
    @Column(nullable = false)
    private boolean inAppBookingUpdates = true;

    @Column(nullable = false)
    private boolean inAppPaymentUpdates = true;

    @Column(nullable = false)
    private boolean inAppFlightUpdates = true;

    @Column(nullable = false)
    private boolean inAppPriceAlerts = false;

    // Security notifications cannot be disabled (always on)
    @Column(nullable = false)
    private boolean alwaysSendSecurityNotifications = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public NotificationPreference() {}

    public NotificationPreference(User user) {
        this.user = user;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public boolean isEmailBookingUpdates() { return emailBookingUpdates; }
    public void setEmailBookingUpdates(boolean v) { this.emailBookingUpdates = v; }
    public boolean isEmailPaymentUpdates() { return emailPaymentUpdates; }
    public void setEmailPaymentUpdates(boolean v) { this.emailPaymentUpdates = v; }
    public boolean isEmailCancellationRefund() { return emailCancellationRefund; }
    public void setEmailCancellationRefund(boolean v) { this.emailCancellationRefund = v; }
    public boolean isEmailFlightUpdates() { return emailFlightUpdates; }
    public void setEmailFlightUpdates(boolean v) { this.emailFlightUpdates = v; }
    public boolean isEmailPriceAlerts() { return emailPriceAlerts; }
    public void setEmailPriceAlerts(boolean v) { this.emailPriceAlerts = v; }
    public boolean isEmailMarketing() { return emailMarketing; }
    public void setEmailMarketing(boolean v) { this.emailMarketing = v; }
    public boolean isInAppBookingUpdates() { return inAppBookingUpdates; }
    public void setInAppBookingUpdates(boolean v) { this.inAppBookingUpdates = v; }
    public boolean isInAppPaymentUpdates() { return inAppPaymentUpdates; }
    public void setInAppPaymentUpdates(boolean v) { this.inAppPaymentUpdates = v; }
    public boolean isInAppFlightUpdates() { return inAppFlightUpdates; }
    public void setInAppFlightUpdates(boolean v) { this.inAppFlightUpdates = v; }
    public boolean isInAppPriceAlerts() { return inAppPriceAlerts; }
    public void setInAppPriceAlerts(boolean v) { this.inAppPriceAlerts = v; }
    public boolean isAlwaysSendSecurityNotifications() { return alwaysSendSecurityNotifications; }
    public void setAlwaysSendSecurityNotifications(boolean v) { this.alwaysSendSecurityNotifications = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
