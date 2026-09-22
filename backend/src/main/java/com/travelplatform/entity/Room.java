package com.travelplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_room_hotel", columnList = "hotel_id"),
    @Index(name = "idx_room_type", columnList = "roomType")
})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonIgnoreProperties({"rooms", "amenities", "imageUrls", "hibernateLazyInitializer", "handler"})
    private Hotel hotel;

    @Column(nullable = false)
    private String roomType;  // STANDARD, DELUXE, SUITE, PRESIDENTIAL

    @Column(nullable = false)
    private String name;  // "Deluxe King Room", etc.

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    /** Base price before dynamic pricing (set on seed, persists across updates) */
    @Column(precision = 10, scale = 2)
    private BigDecimal basePrice;

    private int maxGuests;

    private int bedCount;

    private String bedType;  // SINGLE, DOUBLE, KING, QUEEN

    private double sizeSqm;

    private int totalRooms;

    private int availableRooms;

    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "images_json", columnDefinition = "TEXT")
    private String imagesJson;

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    /** Optimistic lock version — concurrent booking updates trigger OptimisticLockException */
    @Version
    private Long version;

    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Room() {}

    public Long getId() { return id; }
    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
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
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }
    public String getCurrency() { return currency != null ? currency : "INR"; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getVersion() { return version; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public java.util.List<String> getAmenitiesList() {
        if (amenities == null || amenities.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(amenities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public java.util.List<String> getImagesList() {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (imageUrl != null && !imageUrl.isBlank()) {
            list.add(imageUrl);
        }
        if (imagesJson != null && !imagesJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                String[] extra = om.readValue(imagesJson, String[].class);
                for (String url : extra) {
                    if (url != null && !url.isBlank() && !list.contains(url)) {
                        list.add(url);
                    }
                }
            } catch (Exception ignored) {}
        }
        return list;
    }
}
