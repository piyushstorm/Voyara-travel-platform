package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "connection_risks")
public class ConnectionRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_flight_id", nullable = false)
    private Flight firstFlight;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "second_flight_id", nullable = false)
    private Flight secondFlight;

    @Column(nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false)
    private int connectionMinutes;

    @Column(nullable = false)
    private int requiredMinutes = 90;

    @Column(columnDefinition = "TEXT")
    private String riskFactors;

    private LocalDateTime lastCalculatedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public ConnectionRisk() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Flight getFirstFlight() { return firstFlight; }
    public void setFirstFlight(Flight f) { this.firstFlight = f; }
    public Flight getSecondFlight() { return secondFlight; }
    public void setSecondFlight(Flight f) { this.secondFlight = f; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String level) { this.riskLevel = level; }
    public int getConnectionMinutes() { return connectionMinutes; }
    public void setConnectionMinutes(int m) { this.connectionMinutes = m; }
    public int getRequiredMinutes() { return requiredMinutes; }
    public void setRequiredMinutes(int m) { this.requiredMinutes = m; }
    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String factors) { this.riskFactors = factors; }
    public LocalDateTime getLastCalculatedAt() { return lastCalculatedAt; }
    public void setLastCalculatedAt(LocalDateTime t) { this.lastCalculatedAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
