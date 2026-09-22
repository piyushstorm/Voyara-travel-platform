package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fare option tier (Saver / Standard / Flex) for flights.
 * Each flight can have multiple fare options with different inclusions and price multipliers.
 */
@Entity
@Table(name = "fare_options")
public class FareOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "airline", "origin", "destination", "seats", "fareOptions"})
    private Flight flight;

    @Column(nullable = false)
    private String fareType;  // SAVER, STANDARD, FLEX

    @Column(nullable = false)
    private String name;  // e.g., "Saver", "Standard", "Flex"

    private String description;

    // Price multiplier relative to economy base
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal priceMultiplier;

    // Absolute price override (if set, takes precedence over multiplier)
    @Column(precision = 10, scale = 2)
    private BigDecimal overridePrice;

    // Inclusions
    @Column(nullable = false)
    private int checkedBaggageKg;

    @Column(nullable = false)
    private int cabinBaggageKg;

    private String cabinBaggageDimensions;  // e.g., "55×35×25 cm"

    @Column(nullable = false)
    private boolean mealIncluded;

    @Column(nullable = false)
    private boolean seatSelectionIncluded;

    @Column(nullable = false)
    private boolean changeable;

    private String changeFee;  // e.g., "₹500 fee", "Free"

    @Column(nullable = false)
    private boolean refundable;

    private String refundPolicy;  // e.g., "Full refund up to 48h before departure"

    @Column(nullable = false)
    private boolean priorityBoarding;

    @Column(nullable = false)
    private boolean loungeAccess;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public FareOption() {}

    public FareOption(String fareType, String name, String description, BigDecimal priceMultiplier,
                      int checkedBaggageKg, int cabinBaggageKg, boolean mealIncluded,
                      boolean seatSelectionIncluded, boolean changeable, String changeFee,
                      boolean refundable, String refundPolicy) {
        this.fareType = fareType;
        this.name = name;
        this.description = description;
        this.priceMultiplier = priceMultiplier;
        this.checkedBaggageKg = checkedBaggageKg;
        this.cabinBaggageKg = cabinBaggageKg;
        this.mealIncluded = mealIncluded;
        this.seatSelectionIncluded = seatSelectionIncluded;
        this.changeable = changeable;
        this.changeFee = changeFee;
        this.refundable = refundable;
        this.refundPolicy = refundPolicy;
    }

    public Long getId() { return id; }
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public String getFareType() { return fareType; }
    public void setFareType(String fareType) { this.fareType = fareType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPriceMultiplier() { return priceMultiplier; }
    public void setPriceMultiplier(BigDecimal priceMultiplier) { this.priceMultiplier = priceMultiplier; }
    public BigDecimal getOverridePrice() { return overridePrice; }
    public void setOverridePrice(BigDecimal overridePrice) { this.overridePrice = overridePrice; }
    public int getCheckedBaggageKg() { return checkedBaggageKg; }
    public void setCheckedBaggageKg(int checkedBaggageKg) { this.checkedBaggageKg = checkedBaggageKg; }
    public int getCabinBaggageKg() { return cabinBaggageKg; }
    public void setCabinBaggageKg(int cabinBaggageKg) { this.cabinBaggageKg = cabinBaggageKg; }
    public String getCabinBaggageDimensions() { return cabinBaggageDimensions; }
    public void setCabinBaggageDimensions(String d) { this.cabinBaggageDimensions = d; }
    public boolean isMealIncluded() { return mealIncluded; }
    public void setMealIncluded(boolean mealIncluded) { this.mealIncluded = mealIncluded; }
    public boolean isSeatSelectionIncluded() { return seatSelectionIncluded; }
    public void setSeatSelectionIncluded(boolean b) { this.seatSelectionIncluded = b; }
    public boolean isChangeable() { return changeable; }
    public void setChangeable(boolean changeable) { this.changeable = changeable; }
    public String getChangeFee() { return changeFee; }
    public void setChangeFee(String changeFee) { this.changeFee = changeFee; }
    public boolean isRefundable() { return refundable; }
    public void setRefundable(boolean refundable) { this.refundable = refundable; }
    public String getRefundPolicy() { return refundPolicy; }
    public void setRefundPolicy(String refundPolicy) { this.refundPolicy = refundPolicy; }
    public boolean isPriorityBoarding() { return priorityBoarding; }
    public void setPriorityBoarding(boolean priorityBoarding) { this.priorityBoarding = priorityBoarding; }
    public boolean isLoungeAccess() { return loungeAccess; }
    public void setLoungeAccess(boolean loungeAccess) { this.loungeAccess = loungeAccess; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Calculate the fare price for this option given the base economy price */
    public BigDecimal calculatePrice(BigDecimal baseEconomyPrice) {
        if (overridePrice != null) return overridePrice;
        return baseEconomyPrice.multiply(priceMultiplier);
    }
}
