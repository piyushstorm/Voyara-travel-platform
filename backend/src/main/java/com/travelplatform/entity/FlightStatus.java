package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Current live status of a tracked flight.
 * Updated by the simulator and pushed to clients via WebSocket.
 */
@Entity
@Table(name = "flight_statuses")
public class FlightStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false, unique = true)
    private Flight flight;

    /** ON_TIME, DELAYED, BOARDING, DEPARTED, ARRIVED, CANCELLED */
    @Column(nullable = false)
    private String status = "ON_TIME";

    private String delayReason;

    private LocalDateTime scheduledDeparture;

    private LocalDateTime estimatedDeparture;

    private LocalDateTime scheduledArrival;

    private LocalDateTime estimatedArrival;

    /** Gate number, updated during boarding */
    private String gate;

    /** Terminal info */
    private String terminal;

    /** Delay duration in minutes */
    @Column(nullable = false)
    private int delayMinutes = 0;

    /** Actual departure timestamp */
    private LocalDateTime actualDeparture;

    /** Actual arrival timestamp */
    private LocalDateTime actualArrival;

    /** Boarding commencement timestamp */
    private LocalDateTime boardingTime;

    /** Simulation scenario: ON_TIME, DELAYED_WEATHER, DELAYED_OPERATIONAL, etc. */
    private String scenario = "ON_TIME";

    /** Last simulated timestamp */
    private LocalDateTime lastSimulatedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public FlightStatus() {}

    public Long getId() { return id; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String reason) { this.delayReason = reason; }
    public LocalDateTime getScheduledDeparture() { return scheduledDeparture; }
    public void setScheduledDeparture(LocalDateTime t) { this.scheduledDeparture = t; }
    public LocalDateTime getEstimatedDeparture() { return estimatedDeparture; }
    public void setEstimatedDeparture(LocalDateTime t) { this.estimatedDeparture = t; }
    public LocalDateTime getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(LocalDateTime t) { this.scheduledArrival = t; }
    public LocalDateTime getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(LocalDateTime t) { this.estimatedArrival = t; }
    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }
    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public LocalDateTime getActualDeparture() { return actualDeparture; }
    public void setActualDeparture(LocalDateTime actualDeparture) { this.actualDeparture = actualDeparture; }
    public LocalDateTime getActualArrival() { return actualArrival; }
    public void setActualArrival(LocalDateTime actualArrival) { this.actualArrival = actualArrival; }
    public LocalDateTime getBoardingTime() { return boardingTime; }
    public void setBoardingTime(LocalDateTime boardingTime) { this.boardingTime = boardingTime; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public LocalDateTime getLastSimulatedAt() { return lastSimulatedAt; }
    public void setLastSimulatedAt(LocalDateTime lastSimulatedAt) { this.lastSimulatedAt = lastSimulatedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
