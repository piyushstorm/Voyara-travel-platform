package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_invitations", indexes = {
    @Index(name = "idx_invite_token", columnList = "token", unique = true),
    @Index(name = "idx_invite_email", columnList = "invitee_email"),
    @Index(name = "idx_invite_trip", columnList = "group_trip_id")
})
public class TripInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_user_id", nullable = false)
    private User inviterUser;

    @Column(nullable = false, length = 150)
    private String inviteeEmail;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_user_id")
    private User inviteeUser;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime sentAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime declinedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public TripInvitation() {}

    public Long getId() { return id; }
    public GroupTrip getGroupTrip() { return groupTrip; }
    public void setGroupTrip(GroupTrip groupTrip) { this.groupTrip = groupTrip; }
    public User getInviterUser() { return inviterUser; }
    public void setInviterUser(User user) { this.inviterUser = user; }
    public String getInviteeEmail() { return inviteeEmail; }
    public void setInviteeEmail(String email) { this.inviteeEmail = email; }
    public User getInviteeUser() { return inviteeUser; }
    public void setInviteeUser(User user) { this.inviteeUser = user; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime t) { this.acceptedAt = t; }
    public LocalDateTime getDeclinedAt() { return declinedAt; }
    public void setDeclinedAt(LocalDateTime t) { this.declinedAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
