package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_trip_bookings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_trip_id", "booking_id"})
})
public class GroupTripBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "added_by_user_id", nullable = false)
    private Long addedByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public GroupTripBooking() {}

    public Long getId() { return id; }
    public GroupTrip getGroupTrip() { return groupTrip; }
    public void setGroupTrip(GroupTrip groupTrip) { this.groupTrip = groupTrip; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getAddedByUserId() { return addedByUserId; }
    public void setAddedByUserId(Long userId) { this.addedByUserId = userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
