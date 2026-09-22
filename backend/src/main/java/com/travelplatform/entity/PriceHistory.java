package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records every price change for a flight or hotel.
 * Used by the price-history chart on the frontend.
 */
@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FLIGHT or HOTEL */
    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    /** Cabin class for flights, room type for hotels */
    private String cabinClass;

    private BigDecimal previousPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newPrice;

    /** PEAK_MARKUP, DEMAND_MULTIPLIER, TIME_DECAY, MANUAL_ADJUSTMENT */
    @Column(nullable = false)
    private String reason;

    /** JSON or text description of what triggered the change */
    private String details;

    @Column(precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal demandMultiplier;

    @Column(precision = 5, scale = 2)
    private BigDecimal seasonalMultiplier;

    @Column(precision = 5, scale = 2)
    private BigDecimal inventoryMultiplier;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public PriceHistory() {}

    public PriceHistory(String entityType, Long entityId, String cabinClass,
                        BigDecimal previousPrice, BigDecimal newPrice,
                        String reason, String details) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.cabinClass = cabinClass;
        this.previousPrice = previousPrice;
        this.newPrice = newPrice;
        this.reason = reason;
        this.details = details;
    }

    public PriceHistory(String entityType, Long entityId, String cabinClass,
                        BigDecimal previousPrice, BigDecimal newPrice,
                        String reason, String details, BigDecimal basePrice,
                        BigDecimal demandMultiplier, BigDecimal seasonalMultiplier, BigDecimal inventoryMultiplier) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.cabinClass = cabinClass;
        this.previousPrice = previousPrice;
        this.newPrice = newPrice;
        this.reason = reason;
        this.details = details;
        this.basePrice = basePrice;
        this.demandMultiplier = demandMultiplier;
        this.seasonalMultiplier = seasonalMultiplier;
        this.inventoryMultiplier = inventoryMultiplier;
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getCabinClass() { return cabinClass; }
    public BigDecimal getPreviousPrice() { return previousPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public BigDecimal getDemandMultiplier() { return demandMultiplier; }
    public void setDemandMultiplier(BigDecimal demandMultiplier) { this.demandMultiplier = demandMultiplier; }
    public BigDecimal getSeasonalMultiplier() { return seasonalMultiplier; }
    public void setSeasonalMultiplier(BigDecimal seasonalMultiplier) { this.seasonalMultiplier = seasonalMultiplier; }
    public BigDecimal getInventoryMultiplier() { return inventoryMultiplier; }
    public void setInventoryMultiplier(BigDecimal inventoryMultiplier) { this.inventoryMultiplier = inventoryMultiplier; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
