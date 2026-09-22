package com.travelplatform.dto.flight;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class FlightSearchRequest {

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    private LocalDate departureDate;
    private LocalDate returnDate;
    private String tripType = "ONE_WAY";  // ONE_WAY, ROUND_TRIP
    private int passengers = 1;
    private String cabinClass = "ECONOMY";  // ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST

    // Filters
    private String airlineCode;
    private Integer maxStops;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
    private String departureTimeRange;  // MORNING, AFTERNOON, EVENING, NIGHT

    // Sort & pagination
    private String sortBy = "departureTime";  // price, duration, departureTime
    private String sortOrder = "asc";
    private int page = 0;
    private int size = 10;

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }
    public int getPassengers() { return passengers; }
    public void setPassengers(int passengers) { this.passengers = passengers; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }
    public Integer getMaxStops() { return maxStops; }
    public void setMaxStops(Integer maxStops) { this.maxStops = maxStops; }
    public java.math.BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(java.math.BigDecimal minPrice) { this.minPrice = minPrice; }
    public java.math.BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(java.math.BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public String getDepartureTimeRange() { return departureTimeRange; }
    public void setDepartureTimeRange(String departureTimeRange) { this.departureTimeRange = departureTimeRange; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
