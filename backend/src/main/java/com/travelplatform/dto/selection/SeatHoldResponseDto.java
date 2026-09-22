package com.travelplatform.dto.selection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SeatHoldResponseDto {
    private Long holdId;
    private Long seatId;
    private String seatNumber;
    private String cabinClass;
    private LocalDateTime expiresAt;
    private LocalDateTime heldUntil;
    private long secondsRemaining;
    private String status;
    private BigDecimal seatPrice;
    private BigDecimal premiumSurcharge;
    private BigDecimal totalSeatPrice;

    public SeatHoldResponseDto() {}

    public SeatHoldResponseDto(Long holdId, Long seatId, String seatNumber, String cabinClass,
                               LocalDateTime expiresAt, long secondsRemaining, String status) {
        this(holdId, seatId, seatNumber, cabinClass, expiresAt, secondsRemaining, status,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public SeatHoldResponseDto(Long holdId, Long seatId, String seatNumber, String cabinClass,
                               LocalDateTime expiresAt, long secondsRemaining, String status,
                               BigDecimal seatPrice, BigDecimal premiumSurcharge, BigDecimal totalSeatPrice) {
        this.holdId = holdId;
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.cabinClass = cabinClass;
        this.expiresAt = expiresAt;
        this.heldUntil = expiresAt;
        this.secondsRemaining = secondsRemaining;
        this.status = status;
        this.seatPrice = seatPrice != null ? seatPrice : BigDecimal.ZERO;
        this.premiumSurcharge = premiumSurcharge != null ? premiumSurcharge : BigDecimal.ZERO;
        this.totalSeatPrice = totalSeatPrice != null ? totalSeatPrice : this.seatPrice.add(this.premiumSurcharge);
    }

    public Long getHoldId() { return holdId; }
    public void setHoldId(Long holdId) { this.holdId = holdId; }
    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        this.heldUntil = expiresAt;
    }
    public LocalDateTime getHeldUntil() { return heldUntil != null ? heldUntil : expiresAt; }
    public void setHeldUntil(LocalDateTime heldUntil) {
        this.heldUntil = heldUntil;
        this.expiresAt = heldUntil;
    }
    public long getSecondsRemaining() { return secondsRemaining; }
    public void setSecondsRemaining(long secondsRemaining) { this.secondsRemaining = secondsRemaining; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getSeatPrice() { return seatPrice; }
    public void setSeatPrice(BigDecimal seatPrice) { this.seatPrice = seatPrice; }
    public BigDecimal getPremiumSurcharge() { return premiumSurcharge; }
    public void setPremiumSurcharge(BigDecimal premiumSurcharge) { this.premiumSurcharge = premiumSurcharge; }
    public BigDecimal getTotalSeatPrice() { return totalSeatPrice; }
    public void setTotalSeatPrice(BigDecimal totalSeatPrice) { this.totalSeatPrice = totalSeatPrice; }
}
