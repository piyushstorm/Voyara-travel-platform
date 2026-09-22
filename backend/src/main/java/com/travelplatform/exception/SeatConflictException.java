package com.travelplatform.exception;

/**
 * Exception thrown when a seat hold or selection conflict occurs.
 * Subclasses BadRequestException for backwards compatibility with existing exception handlers and tests.
 */
public class SeatConflictException extends BadRequestException {

    private final Long seatId;
    private final String seatNumber;
    private final String code;

    public SeatConflictException(Long seatId, String seatNumber, String message) {
        super(message);
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.code = "SEAT_NO_LONGER_AVAILABLE";
    }

    public SeatConflictException(Long seatId, String seatNumber, String code, String message) {
        super(message);
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.code = code != null ? code : "SEAT_NO_LONGER_AVAILABLE";
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getCode() {
        return code;
    }
}
