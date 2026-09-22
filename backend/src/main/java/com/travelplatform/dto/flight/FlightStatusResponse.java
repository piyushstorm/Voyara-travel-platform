package com.travelplatform.dto.flight;

import com.travelplatform.entity.Flight;
import com.travelplatform.entity.FlightStatus;

import java.time.LocalDateTime;

public class FlightStatusResponse {

    private Long id;
    private Long flightId;
    private String flightNumber;
    private String airlineCode;
    private String airlineName;
    private String originCode;
    private String originCity;
    private String destinationCode;
    private String destinationCity;
    private String status;
    private int delayMinutes;
    private String delayReason;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime estimatedDeparture;
    private LocalDateTime actualDeparture;
    private LocalDateTime scheduledArrival;
    private LocalDateTime estimatedArrival;
    private LocalDateTime actualArrival;
    private LocalDateTime boardingTime;
    private String gate;
    private String terminal;
    private LocalDateTime updatedAt;
    private String scenario;

    public FlightStatusResponse() {}

    public static FlightStatusResponse from(FlightStatus status) {
        if (status == null) return null;
        FlightStatusResponse dto = new FlightStatusResponse();
        dto.setId(status.getId());
        dto.setStatus(status.getStatus());
        dto.setDelayMinutes(status.getDelayMinutes());
        dto.setDelayReason(status.getDelayReason());
        dto.setScheduledDeparture(status.getScheduledDeparture());
        dto.setEstimatedDeparture(status.getEstimatedDeparture());
        dto.setActualDeparture(status.getActualDeparture());
        dto.setScheduledArrival(status.getScheduledArrival());
        dto.setEstimatedArrival(status.getEstimatedArrival());
        dto.setActualArrival(status.getActualArrival());
        dto.setBoardingTime(status.getBoardingTime());
        dto.setGate(status.getGate());
        dto.setTerminal(status.getTerminal());
        dto.setUpdatedAt(status.getUpdatedAt());
        dto.setScenario(status.getScenario());

        Flight flight = status.getFlight();
        if (flight != null) {
            dto.setFlightId(flight.getId());
            dto.setFlightNumber(flight.getFlightNumber());
            dto.setOriginCode(flight.getOriginCode());
            dto.setOriginCity(flight.getOrigin() != null ? flight.getOrigin().getCity() : null);
            dto.setDestinationCode(flight.getDestinationCode());
            dto.setDestinationCity(flight.getDestination() != null ? flight.getDestination().getCity() : null);
            if (flight.getAirline() != null) {
                dto.setAirlineCode(flight.getAirline().getCode());
                dto.setAirlineName(flight.getAirline().getName());
            }
        }
        return dto;
    }

    // Compatibility getters for frontend flexibility
    public String getDepartureAirportCode() {
        return originCode;
    }

    public String getArrivalAirportCode() {
        return destinationCode;
    }

    public LocalDateTime getLastUpdated() {
        return updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }
    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }
    public String getOriginCode() { return originCode; }
    public void setOriginCode(String originCode) { this.originCode = originCode; }
    public String getOriginCity() { return originCity; }
    public void setOriginCity(String originCity) { this.originCity = originCity; }
    public String getDestinationCode() { return destinationCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }
    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String delayReason) { this.delayReason = delayReason; }
    public LocalDateTime getScheduledDeparture() { return scheduledDeparture; }
    public void setScheduledDeparture(LocalDateTime scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }
    public LocalDateTime getEstimatedDeparture() { return estimatedDeparture; }
    public void setEstimatedDeparture(LocalDateTime estimatedDeparture) { this.estimatedDeparture = estimatedDeparture; }
    public LocalDateTime getActualDeparture() { return actualDeparture; }
    public void setActualDeparture(LocalDateTime actualDeparture) { this.actualDeparture = actualDeparture; }
    public LocalDateTime getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(LocalDateTime scheduledArrival) { this.scheduledArrival = scheduledArrival; }
    public LocalDateTime getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(LocalDateTime estimatedArrival) { this.estimatedArrival = estimatedArrival; }
    public LocalDateTime getActualArrival() { return actualArrival; }
    public void setActualArrival(LocalDateTime actualArrival) { this.actualArrival = actualArrival; }
    public LocalDateTime getBoardingTime() { return boardingTime; }
    public void setBoardingTime(LocalDateTime boardingTime) { this.boardingTime = boardingTime; }
    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }
    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
}
