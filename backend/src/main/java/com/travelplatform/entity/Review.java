package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_user", columnList = "user_id"),
    @Index(name = "idx_review_flight", columnList = "flight_id"),
    @Index(name = "idx_review_hotel", columnList = "hotel_id"),
    @Index(name = "idx_review_rating", columnList = "rating"),
    @Index(name = "idx_review_status", columnList = "status"),
    @Index(name = "idx_review_booking", columnList = "booking_id")
})
public class Review {

    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_FLAGGED = "FLAGGED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REMOVED = "REMOVED";
    public static final String STATUS_HIDDEN = "HIDDEN";
    public static final String STATUS_PENDING_MODERATION = "PENDING_MODERATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(length = 255)
    private String title;

    @Column(nullable = false)
    private int rating;  // 1-5

    @Column(length = 2000)
    private String text;

    @Column(nullable = false)
    private int helpfulCount = 0;

    @Column(nullable = false)
    private int notHelpfulCount = 0;

    /** PUBLISHED, FLAGGED, UNDER_REVIEW, APPROVED, REMOVED */
    @Column(nullable = false)
    private String status = STATUS_PUBLISHED;

    /** Number of active reports flagged by users */
    @Column(nullable = false)
    private int reportCount = 0;

    /** Reason flagged by users (legacy/primary summary) */
    private String flagReason;

    private boolean verifiedBooking = false;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewReply> replies = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Review() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int c) { this.helpfulCount = c; }
    public int getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(int c) { this.notHelpfulCount = c; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }
    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String reason) { this.flagReason = reason; }
    public boolean isVerifiedBooking() { return verifiedBooking; }
    public void setVerifiedBooking(boolean v) { this.verifiedBooking = v; }
    public List<ReviewPhoto> getPhotos() { return photos; }
    public void setPhotos(List<ReviewPhoto> photos) { this.photos = photos; }
    public List<ReviewReply> getReplies() { return replies; }
    public void setReplies(List<ReviewReply> replies) { this.replies = replies; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** Helper checking if publicly visible */
    public boolean isPubliclyVisible() {
        return STATUS_PUBLISHED.equals(status) || STATUS_APPROVED.equals(status) || STATUS_FLAGGED.equals(status);
    }
}
