package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seat_flight", columnList = "flight_id"),
    @Index(name = "idx_seat_held", columnList = "held_until")
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String seatNumber;  // e.g., "1A", "12F"

    @Column(nullable = false)
    private String cabinClass;  // ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST

    private int rowNumber;

    private String columnLetter;  // A, B, C, D, E, F

    @Column(name = "is_window")
    private Boolean window = false;

    @Column(name = "is_aisle")
    private Boolean aisle = false;

    @Column(name = "is_middle")
    private Boolean middle = false;

    @Column(name = "is_extra_legroom")
    private Boolean extraLegroom = false;

    @Column(name = "is_emergency_exit")
    private Boolean emergencyExit = false;

    @Column(name = "seat_type")
    private String seatType = "STANDARD";  // STANDARD, PREMIUM, EXTRA_LEGROOM, EXIT_ROW

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available = true;

    /** Optimistic lock version — concurrent updates will trigger OptimisticLockException */
    @Version
    private Long version = 0L;

    private String heldByUserId;

    private LocalDateTime heldUntil;

    /** Premium seats have an additional surcharge */
    @Column(precision = 8, scale = 2)
    private BigDecimal premiumSurcharge;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Seat() {}

    public Long getId() { return id; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public String getColumnLetter() { return columnLetter; }
    public void setColumnLetter(String columnLetter) { this.columnLetter = columnLetter; }
    public boolean isWindow() { return Boolean.TRUE.equals(window); }
    public void setWindow(Boolean window) { this.window = window != null ? window : false; }
    public boolean isAisle() { return Boolean.TRUE.equals(aisle); }
    public void setAisle(Boolean aisle) { this.aisle = aisle != null ? aisle : false; }
    public boolean isMiddle() { return Boolean.TRUE.equals(middle); }
    public void setMiddle(Boolean middle) { this.middle = middle != null ? middle : false; }
    public boolean isExtraLegroom() { return Boolean.TRUE.equals(extraLegroom); }
    public void setExtraLegroom(Boolean extraLegroom) { this.extraLegroom = extraLegroom != null ? extraLegroom : false; }
    public boolean isEmergencyExit() { return Boolean.TRUE.equals(emergencyExit); }
    public void setEmergencyExit(Boolean emergencyExit) { this.emergencyExit = emergencyExit != null ? emergencyExit : false; }
    public String getSeatType() { return seatType != null ? seatType : (premiumSurcharge != null && premiumSurcharge.compareTo(BigDecimal.ZERO) > 0 ? "PREMIUM" : "STANDARD"); }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public String getCurrency() { return currency != null ? currency : "INR"; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getHeldByUserId() { return heldByUserId; }
    public void setHeldByUserId(String heldByUserId) { this.heldByUserId = heldByUserId; }
    public LocalDateTime getHeldUntil() { return heldUntil; }
    public void setHeldUntil(LocalDateTime heldUntil) { this.heldUntil = heldUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isHeld() {
        return heldUntil != null && heldByUserId != null && LocalDateTime.now().isBefore(heldUntil);
    }

    /** Seat is effectively bookable if available, not held (or hold expired), and not booked */
    public boolean isBookable() {
        return available && !isHeld();
    }

    public Long getVersion() { return version; }
    public BigDecimal getPremiumSurcharge() { return premiumSurcharge; }
    public void setPremiumSurcharge(BigDecimal surcharge) { this.premiumSurcharge = surcharge; }
}
