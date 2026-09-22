package com.travelplatform.dto.flight;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for multi-city flight search.
 * Each leg represents one segment of the multi-city journey.
 */
public class MultiCitySearchRequest {

    private List<CityLeg> legs;
    private int passengers = 1;
    private String cabinClass = "ECONOMY";

    // Filters
    private String airlineCode;
    private Integer maxStops;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;

    // Sort & pagination
    private String sortBy = "departureTime";
    private String sortOrder = "asc";
    private int page = 0;
    private int size = 10;

    public static class CityLeg {
        private String origin;
        private String destination;
        private LocalDate date;

        public CityLeg() {}
        public CityLeg(String origin, String destination, LocalDate date) {
            this.origin = origin;
            this.destination = destination;
            this.date = date;
        }

        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    public List<CityLeg> getLegs() { return legs; }
    public void setLegs(List<CityLeg> legs) { this.legs = legs; }
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
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
