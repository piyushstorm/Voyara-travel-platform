package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flights", indexes = {
    @Index(name = "idx_flight_route", columnList = "originCode, destinationCode"),
    @Index(name = "idx_flight_departure", columnList = "departureTime"),
    @Index(name = "idx_flight_date", columnList = "departureDate")
})
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String flightNumber;  // e.g., "AI-302"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id", nullable = false)
    private Airport origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Airport destination;

    // Stored as codes for fast search
    @Column(nullable = false, length = 3)
    private String originCode;

    @Column(nullable = false, length = 3)
    private String destinationCode;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false)
    private java.time.LocalDate departureDate;

    private int durationMinutes;

    private int stops;  // 0 = direct, 1 = one stop, 2 = two stops

    private String stopoverCity;  // e.g., "Delhi" for 1-stop

    // Prices per cabin class (these are the dynamic/current prices)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal economyPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumEconomyPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal businessPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal firstClassPrice;

    // Base prices (original prices before dynamic pricing)
    @Column(precision = 10, scale = 2)
    private BigDecimal economyBasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal premiumEconomyBasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal businessBasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal firstClassBasePrice;

    // Capacity
    private int totalSeatsEconomy;
    private int bookedSeatsEconomy;
    private int totalSeatsPremiumEconomy;
    private int bookedSeatsPremiumEconomy;
    private int totalSeatsBusiness;
    private int bookedSeatsBusiness;
    private int totalSeatsFirst;
    private int bookedSeatsFirst;

    private boolean active = true;

    // SAFE: No cascade — seat and fare data must survive flight deactivation
    @OneToMany(mappedBy = "flight", fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    // SAFE: No cascade — fare data must survive flight deactivation
    @OneToMany(mappedBy = "flight", fetch = FetchType.LAZY)
    private List<FareOption> fareOptions = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Flight() {}

    public Long getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public Airline getAirline() { return airline; }
    public void setAirline(Airline airline) { this.airline = airline; }
    public Airport getOrigin() { return origin; }
    public void setOrigin(Airport origin) { this.origin = origin; }
    public Airport getDestination() { return destination; }
    public void setDestination(Airport destination) { this.destination = destination; }
    public String getOriginCode() { return originCode; }
    public void setOriginCode(String originCode) { this.originCode = originCode; }
    public String getDestinationCode() { return destinationCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public java.time.LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(java.time.LocalDate departureDate) { this.departureDate = departureDate; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getStops() { return stops; }
    public void setStops(int stops) { this.stops = stops; }
    public String getStopoverCity() { return stopoverCity; }
    public void setStopoverCity(String stopoverCity) { this.stopoverCity = stopoverCity; }
    public BigDecimal getEconomyPrice() { return economyPrice; }
    public void setEconomyPrice(BigDecimal economyPrice) { this.economyPrice = economyPrice; }
    public BigDecimal getPremiumEconomyPrice() { return premiumEconomyPrice; }
    public void setPremiumEconomyPrice(BigDecimal price) { this.premiumEconomyPrice = price; }
    public BigDecimal getBusinessPrice() { return businessPrice; }
    public void setBusinessPrice(BigDecimal businessPrice) { this.businessPrice = businessPrice; }
    public BigDecimal getFirstClassPrice() { return firstClassPrice; }
    public void setFirstClassPrice(BigDecimal firstClassPrice) { this.firstClassPrice = firstClassPrice; }
    public BigDecimal getEconomyBasePrice() { return economyBasePrice; }
    public void setEconomyBasePrice(BigDecimal p) { this.economyBasePrice = p; }
    public BigDecimal getPremiumEconomyBasePrice() { return premiumEconomyBasePrice; }
    public void setPremiumEconomyBasePrice(BigDecimal p) { this.premiumEconomyBasePrice = p; }
    public BigDecimal getBusinessBasePrice() { return businessBasePrice; }
    public void setBusinessBasePrice(BigDecimal p) { this.businessBasePrice = p; }
    public BigDecimal getFirstClassBasePrice() { return firstClassBasePrice; }
    public void setFirstClassBasePrice(BigDecimal p) { this.firstClassBasePrice = p; }
    public int getTotalSeatsEconomy() { return totalSeatsEconomy; }
    public void setTotalSeatsEconomy(int totalSeatsEconomy) { this.totalSeatsEconomy = totalSeatsEconomy; }
    public int getBookedSeatsEconomy() { return bookedSeatsEconomy; }
    public void setBookedSeatsEconomy(int bookedSeatsEconomy) { this.bookedSeatsEconomy = bookedSeatsEconomy; }
    public int getTotalSeatsPremiumEconomy() { return totalSeatsPremiumEconomy; }
    public void setTotalSeatsPremiumEconomy(int n) { this.totalSeatsPremiumEconomy = n; }
    public int getBookedSeatsPremiumEconomy() { return bookedSeatsPremiumEconomy; }
    public void setBookedSeatsPremiumEconomy(int n) { this.bookedSeatsPremiumEconomy = n; }
    public int getTotalSeatsBusiness() { return totalSeatsBusiness; }
    public void setTotalSeatsBusiness(int n) { this.totalSeatsBusiness = n; }
    public int getBookedSeatsBusiness() { return bookedSeatsBusiness; }
    public void setBookedSeatsBusiness(int n) { this.bookedSeatsBusiness = n; }
    public int getTotalSeatsFirst() { return totalSeatsFirst; }
    public void setTotalSeatsFirst(int n) { this.totalSeatsFirst = n; }
    public int getBookedSeatsFirst() { return bookedSeatsFirst; }
    public void setBookedSeatsFirst(int n) { this.bookedSeatsFirst = n; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
    public List<FareOption> getFareOptions() { return fareOptions; }
    public void setFareOptions(List<FareOption> fareOptions) { this.fareOptions = fareOptions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public BigDecimal getPriceForClass(String cabinClass) {
        return switch (cabinClass.toUpperCase()) {
            case "ECONOMY" -> economyPrice;
            case "PREMIUM_ECONOMY" -> premiumEconomyPrice;
            case "BUSINESS" -> businessPrice;
            case "FIRST" -> firstClassPrice;
            default -> economyPrice;
        };
    }

    public int getAvailableSeatsForClass(String cabinClass) {
        return switch (cabinClass.toUpperCase()) {
            case "ECONOMY" -> totalSeatsEconomy - bookedSeatsEconomy;
            case "PREMIUM_ECONOMY" -> totalSeatsPremiumEconomy - bookedSeatsPremiumEconomy;
            case "BUSINESS" -> totalSeatsBusiness - bookedSeatsBusiness;
            case "FIRST" -> totalSeatsFirst - bookedSeatsFirst;
            default -> 0;
        };
    }
}
