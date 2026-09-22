package com.travelplatform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cabs")
public class Cab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vehicleType; // HATCHBACK, SEDAN, SUV, LUXURY, TEMPO

    @Column(nullable = false)
    private String vehicleName;

    private Integer capacity;
    private Integer luggageCapacity;
    private Boolean ac;
    private BigDecimal baseFare;
    private BigDecimal perKmRate;
    private BigDecimal minimumFare;
    private BigDecimal bookingFee;

    private Double rating;
    private Boolean active;

    @Column(columnDefinition = "TEXT")
    private String features; // JSON or comma-separated: GPS, CHILD_SEAT, etc.

    public Cab() { this.active = true; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getLuggageCapacity() { return luggageCapacity; }
    public void setLuggageCapacity(Integer luggageCapacity) { this.luggageCapacity = luggageCapacity; }
    public Boolean getAc() { return ac; }
    public void setAc(Boolean ac) { this.ac = ac; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }
    public BigDecimal getPerKmRate() { return perKmRate; }
    public void setPerKmRate(BigDecimal perKmRate) { this.perKmRate = perKmRate; }
    public BigDecimal getMinimumFare() { return minimumFare; }
    public void setMinimumFare(BigDecimal minimumFare) { this.minimumFare = minimumFare; }
    public BigDecimal getBookingFee() { return bookingFee; }
    public void setBookingFee(BigDecimal bookingFee) { this.bookingFee = bookingFee; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
}
