package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Price freeze: a user pays a small fee to lock the current price for a window.
 * If they book within the window, they get the frozen price even if live price changed.
 */
@Entity
@Table(name = "price_freezes")
public class PriceFreeze {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** FLIGHT or HOTEL */
    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    private String cabinClass;  // for flights

    /** Price at the moment of freeze */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal frozenPrice;

    /** Fee charged for the freeze (e.g., 5% of price or flat ₹99) */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal freezeFee;

    /** When the freeze expires (e.g., 15 minutes from creation) */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** ACTIVE, EXPIRED, USED, CANCELLED */
    @Column(nullable = false)
    private String status = "ACTIVE";

    /** If used, link to the booking */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public PriceFreeze() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String type) { this.entityType = type; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long id) { this.entityId = id; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String c) { this.cabinClass = c; }
    public BigDecimal getFrozenPrice() { return frozenPrice; }
    public void setFrozenPrice(BigDecimal price) { this.frozenPrice = price; }
    public BigDecimal getFreezeFee() { return freezeFee; }
    public void setFreezeFee(BigDecimal fee) { this.freezeFee = fee; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime t) { this.expiresAt = t; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
