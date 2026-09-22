package com.travelplatform.dto.selection;

import java.math.BigDecimal;
import java.util.List;

public class RoomDto {
    private Long id;
    private String roomType;
    private String name;
    private String description;
    private BigDecimal pricePerNight;
    private BigDecimal basePrice;
    private int maxGuests;
    private int bedCount;
    private String bedType;
    private double sizeSqm;
    private int totalRooms;
    private int availableRooms;
    private int activeHoldsCount;
    private boolean bookable;
    private String imageUrl;
    private List<String> images;
    private List<String> amenities;
    private String currency;
    private boolean preferenceMatch;
    private String matchReason;
    private BigDecimal upgradePriceDiff;

    public RoomDto() {}

    public RoomDto(Long id, String roomType, String name, String description,
                   BigDecimal pricePerNight, BigDecimal basePrice, int maxGuests,
                   int bedCount, String bedType, double sizeSqm,
                   int totalRooms, int availableRooms, int activeHoldsCount,
                   boolean bookable, String imageUrl, List<String> images,
                   List<String> amenities, String currency,
                   boolean preferenceMatch, String matchReason, BigDecimal upgradePriceDiff) {
        this.id = id;
        this.roomType = roomType;
        this.name = name;
        this.description = description;
        this.pricePerNight = pricePerNight;
        this.basePrice = basePrice;
        this.maxGuests = maxGuests;
        this.bedCount = bedCount;
        this.bedType = bedType;
        this.sizeSqm = sizeSqm;
        this.totalRooms = totalRooms;
        this.availableRooms = availableRooms;
        this.activeHoldsCount = activeHoldsCount;
        this.bookable = bookable;
        this.imageUrl = imageUrl;
        this.images = images;
        this.amenities = amenities;
        this.currency = currency != null ? currency : "INR";
        this.preferenceMatch = preferenceMatch;
        this.matchReason = matchReason;
        this.upgradePriceDiff = upgradePriceDiff;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }
    public int getBedCount() { return bedCount; }
    public void setBedCount(int bedCount) { this.bedCount = bedCount; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public double getSizeSqm() { return sizeSqm; }
    public void setSizeSqm(double sizeSqm) { this.sizeSqm = sizeSqm; }
    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }
    public int getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(int availableRooms) { this.availableRooms = availableRooms; }
    public int getActiveHoldsCount() { return activeHoldsCount; }
    public void setActiveHoldsCount(int activeHoldsCount) { this.activeHoldsCount = activeHoldsCount; }
    public boolean isBookable() { return bookable; }
    public void setBookable(boolean bookable) { this.bookable = bookable; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isPreferenceMatch() { return preferenceMatch; }
    public void setPreferenceMatch(boolean preferenceMatch) { this.preferenceMatch = preferenceMatch; }
    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
    public BigDecimal getUpgradePriceDiff() { return upgradePriceDiff; }
    public void setUpgradePriceDiff(BigDecimal upgradePriceDiff) { this.upgradePriceDiff = upgradePriceDiff; }
}
