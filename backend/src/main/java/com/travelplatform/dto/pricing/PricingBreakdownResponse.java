package com.travelplatform.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PricingBreakdownResponse {

    private BigDecimal basePrice;
    private BigDecimal currentPrice;
    private String currency = "INR";

    private double demandMultiplier = 1.0;
    private BigDecimal demandAdjustment = BigDecimal.ZERO;
    private String demandDescription;

    private double seasonalMultiplier = 1.0;
    private BigDecimal seasonalAdjustment = BigDecimal.ZERO;
    private String seasonalDescription;

    private double inventoryMultiplier = 1.0;
    private BigDecimal inventoryAdjustment = BigDecimal.ZERO;
    private String inventoryDescription;

    private double timeMultiplier = 1.0;
    private BigDecimal timeAdjustment = BigDecimal.ZERO;
    private String timeDescription;

    private double totalMultiplier = 1.0;
    private boolean safetyCapApplied = false;
    private boolean isPeakPeriod = false;
    private Long hoursUntilDeparture;
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public PricingBreakdownResponse() {}

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getDemandMultiplier() { return demandMultiplier; }
    public void setDemandMultiplier(double demandMultiplier) { this.demandMultiplier = demandMultiplier; }

    public BigDecimal getDemandAdjustment() { return demandAdjustment; }
    public void setDemandAdjustment(BigDecimal demandAdjustment) { this.demandAdjustment = demandAdjustment; }

    public String getDemandDescription() { return demandDescription; }
    public void setDemandDescription(String demandDescription) { this.demandDescription = demandDescription; }

    public double getSeasonalMultiplier() { return seasonalMultiplier; }
    public void setSeasonalMultiplier(double seasonalMultiplier) { this.seasonalMultiplier = seasonalMultiplier; }

    public BigDecimal getSeasonalAdjustment() { return seasonalAdjustment; }
    public void setSeasonalAdjustment(BigDecimal seasonalAdjustment) { this.seasonalAdjustment = seasonalAdjustment; }

    public String getSeasonalDescription() { return seasonalDescription; }
    public void setSeasonalDescription(String seasonalDescription) { this.seasonalDescription = seasonalDescription; }

    public double getInventoryMultiplier() { return inventoryMultiplier; }
    public void setInventoryMultiplier(double inventoryMultiplier) { this.inventoryMultiplier = inventoryMultiplier; }

    public BigDecimal getInventoryAdjustment() { return inventoryAdjustment; }
    public void setInventoryAdjustment(BigDecimal inventoryAdjustment) { this.inventoryAdjustment = inventoryAdjustment; }

    public String getInventoryDescription() { return inventoryDescription; }
    public void setInventoryDescription(String inventoryDescription) { this.inventoryDescription = inventoryDescription; }

    public double getTimeMultiplier() { return timeMultiplier; }
    public void setTimeMultiplier(double timeMultiplier) { this.timeMultiplier = timeMultiplier; }

    public BigDecimal getTimeAdjustment() { return timeAdjustment; }
    public void setTimeAdjustment(BigDecimal timeAdjustment) { this.timeAdjustment = timeAdjustment; }

    public String getTimeDescription() { return timeDescription; }
    public void setTimeDescription(String timeDescription) { this.timeDescription = timeDescription; }

    public double getTotalMultiplier() { return totalMultiplier; }
    public void setTotalMultiplier(double totalMultiplier) { this.totalMultiplier = totalMultiplier; }

    public boolean isSafetyCapApplied() { return safetyCapApplied; }
    public void setSafetyCapApplied(boolean safetyCapApplied) { this.safetyCapApplied = safetyCapApplied; }

    public boolean isPeakPeriod() { return isPeakPeriod; }
    public void setPeakPeriod(boolean peakPeriod) { isPeakPeriod = peakPeriod; }

    public Long getHoursUntilDeparture() { return hoursUntilDeparture; }
    public void setHoursUntilDeparture(Long hoursUntilDeparture) { this.hoursUntilDeparture = hoursUntilDeparture; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
