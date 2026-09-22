package com.travelplatform.dto.selection;

import java.math.BigDecimal;

public class SeatDto {
    private Long id;
    private String seatNumber;
    private int row;
    private String column;
    private boolean window;
    private boolean aisle;
    private boolean middle;
    private boolean extraLegroom;
    private boolean emergencyExit;
    private String seatType;
    private BigDecimal price;
    private BigDecimal premiumSurcharge;
    private BigDecimal totalPrice;
    private String currency;
    private String status; // AVAILABLE, HELD, BOOKED, SELECTED
    private boolean preferenceMatch;
    private String matchReason;

    public SeatDto() {}

    public SeatDto(Long id, String seatNumber, int row, String column,
                   boolean window, boolean aisle, boolean middle,
                   boolean extraLegroom, boolean emergencyExit,
                   String seatType, BigDecimal price, BigDecimal premiumSurcharge,
                   String currency, String status, boolean preferenceMatch, String matchReason) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.row = row;
        this.column = column;
        this.window = window;
        this.aisle = aisle;
        this.middle = middle;
        this.extraLegroom = extraLegroom;
        this.emergencyExit = emergencyExit;
        this.seatType = seatType;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.premiumSurcharge = premiumSurcharge != null ? premiumSurcharge : BigDecimal.ZERO;
        this.totalPrice = this.price.add(this.premiumSurcharge);
        this.currency = currency != null ? currency : "INR";
        this.status = status;
        this.preferenceMatch = preferenceMatch;
        this.matchReason = matchReason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public String getColumn() { return column; }
    public void setColumn(String column) { this.column = column; }
    public boolean isWindow() { return window; }
    public void setWindow(boolean window) { this.window = window; }
    public boolean isAisle() { return aisle; }
    public void setAisle(boolean aisle) { this.aisle = aisle; }
    public boolean isMiddle() { return middle; }
    public void setMiddle(boolean middle) { this.middle = middle; }
    public boolean isExtraLegroom() { return extraLegroom; }
    public void setExtraLegroom(boolean extraLegroom) { this.extraLegroom = extraLegroom; }
    public boolean isEmergencyExit() { return emergencyExit; }
    public void setEmergencyExit(boolean emergencyExit) { this.emergencyExit = emergencyExit; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getPremiumSurcharge() { return premiumSurcharge; }
    public void setPremiumSurcharge(BigDecimal premiumSurcharge) { this.premiumSurcharge = premiumSurcharge; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isPreferenceMatch() { return preferenceMatch; }
    public void setPreferenceMatch(boolean preferenceMatch) { this.preferenceMatch = preferenceMatch; }
    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
    public int getRowNumber() { return row; }
    public void setRowNumber(int rowNumber) { this.row = rowNumber; }
    public String getColumnLetter() { return column; }
    public void setColumnLetter(String columnLetter) { this.column = columnLetter; }
    public boolean isAvailable() { return "AVAILABLE".equalsIgnoreCase(status) || "SELECTED".equalsIgnoreCase(status); }
}
