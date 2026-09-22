package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit trail of every status change for a flight.
 * Created by the simulator on each transition.
 */
@Entity
@Table(name = "flight_status_history")
public class FlightStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String previousStatus;

    @Column(nullable = false)
    private String newStatus;

    private String delayReason;

    private LocalDateTime previousEstimatedDeparture;

    private LocalDateTime newEstimatedDeparture;

    private String message;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public FlightStatusHistory() {}

    public Long getId() { return id; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String s) { this.previousStatus = s; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String s) { this.newStatus = s; }
    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String r) { this.delayReason = r; }
    public LocalDateTime getPreviousEstimatedDeparture() { return previousEstimatedDeparture; }
    public void setPreviousEstimatedDeparture(LocalDateTime t) { this.previousEstimatedDeparture = t; }
    public LocalDateTime getNewEstimatedDeparture() { return newEstimatedDeparture; }
    public void setNewEstimatedDeparture(LocalDateTime t) { this.newEstimatedDeparture = t; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
