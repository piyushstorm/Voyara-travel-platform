package com.travelplatform.dto.selection;

import java.util.List;
import java.util.Map;

public class SeatMapResponseDto {
    private Long flightId;
    private String flightNumber;
    private String cabinClass;
    private List<SeatDto> seats;
    private Map<String, Integer> summary;
    private String userPreferenceSummary;
    private List<Long> myActiveHoldSeatIds;

    public SeatMapResponseDto() {}

    public SeatMapResponseDto(Long flightId, String flightNumber, String cabinClass,
                              List<SeatDto> seats, Map<String, Integer> summary,
                              String userPreferenceSummary, List<Long> myActiveHoldSeatIds) {
        this.flightId = flightId;
        this.flightNumber = flightNumber;
        this.cabinClass = cabinClass;
        this.seats = seats;
        this.summary = summary;
        this.userPreferenceSummary = userPreferenceSummary;
        this.myActiveHoldSeatIds = myActiveHoldSeatIds;
    }

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public List<SeatDto> getSeats() { return seats; }
    public void setSeats(List<SeatDto> seats) { this.seats = seats; }
    public Map<String, Integer> getSummary() { return summary; }
    public void setSummary(Map<String, Integer> summary) { this.summary = summary; }
    public String getUserPreferenceSummary() { return userPreferenceSummary; }
    public void setUserPreferenceSummary(String userPreferenceSummary) { this.userPreferenceSummary = userPreferenceSummary; }
    public List<Long> getMyActiveHoldSeatIds() { return myActiveHoldSeatIds; }
    public void setMyActiveHoldSeatIds(List<Long> myActiveHoldSeatIds) { this.myActiveHoldSeatIds = myActiveHoldSeatIds; }
}
