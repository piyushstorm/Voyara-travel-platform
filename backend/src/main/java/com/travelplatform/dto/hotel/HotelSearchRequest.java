package com.travelplatform.dto.hotel;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class HotelSearchRequest {

    @NotBlank(message = "Destination is required")
    private String city;

    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests = 2;
    private int rooms = 1;

    // Filters
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
    private Integer minStar;
    private Integer maxStar;
    private String amenity;

    // Sort & pagination
    private String sortBy = "startingPrice";  // startingPrice, starRating, guestRating
    private String sortOrder = "asc";
    private int page = 0;
    private int size = 10;

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }
    public int getRooms() { return rooms; }
    public void setRooms(int rooms) { this.rooms = rooms; }
    public java.math.BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(java.math.BigDecimal minPrice) { this.minPrice = minPrice; }
    public java.math.BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(java.math.BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public Integer getMinStar() { return minStar; }
    public void setMinStar(Integer minStar) { this.minStar = minStar; }
    public Integer getMaxStar() { return maxStar; }
    public void setMaxStar(Integer maxStar) { this.maxStar = maxStar; }
    public String getAmenity() { return amenity; }
    public void setAmenity(String amenity) { this.amenity = amenity; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
