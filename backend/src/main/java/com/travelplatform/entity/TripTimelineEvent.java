package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_timeline_events", indexes = {
    @Index(name = "idx_timeline_booking", columnList = "booking_id"),
    @Index(name = "idx_timeline_user", columnList = "user_id"),
    @Index(name = "idx_timeline_time", columnList = "booking_id, event_time")
})
public class TripTimelineEvent {

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
    private String eventType;

    @Column(nullable = false, length = 200)
    private String eventTitle;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    private String eventLocation;

    @Column(columnDefinition = "TEXT")
    private String eventDescription;

    private String icon;

    @Column(nullable = false)
    private boolean isAffected = false;

    @Column(columnDefinition = "TEXT")
    private String affectedReason;

    @Column(nullable = false)
    private int displayOrder = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public TripTimelineEvent() {}

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEventType() { return eventType; }
    public void setEventType(String type) { this.eventType = type; }
    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String title) { this.eventTitle = title; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime time) { this.eventTime = time; }
    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String location) { this.eventLocation = location; }
    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String desc) { this.eventDescription = desc; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isAffected() { return isAffected; }
    public void setAffected(boolean affected) { isAffected = affected; }
    public String getAffectedReason() { return affectedReason; }
    public void setAffectedReason(String reason) { this.affectedReason = reason; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int order) { this.displayOrder = order; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
