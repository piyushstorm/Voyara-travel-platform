package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Destination entity with rich metadata for destination recommendations,
 * content-based categorization, and collaborative discovery.
 */
@Entity
@Table(name = "destinations", indexes = {
    @Index(name = "idx_destination_category", columnList = "category"),
    @Index(name = "idx_destination_popularity", columnList = "popularity_score")
})
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    /** BEACH, MOUNTAIN, HERITAGE, CITY, LUXURY, ADVENTURE, NATURE */
    @Column(nullable = false, length = 50)
    private String category;

    /** Comma-separated tags: BEACH,NIGHTLIFE,RELAXATION,WATERSPORTS,ROMANTIC */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String tags;

    @Column(length = 50)
    private String climate;

    @Column(name = "best_season", length = 100)
    private String bestSeason;

    @Column(name = "average_daily_budget", precision = 10, scale = 2)
    private BigDecimal averageDailyBudget;

    @Column(name = "popularity_score")
    private Double popularityScore = 0.8;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Destination() {}

    public Destination(String name, String city, String country, String category, String tags,
                       String climate, String bestSeason, BigDecimal averageDailyBudget,
                       Double popularityScore, String imageUrl, String description) {
        this.name = name;
        this.city = city;
        this.country = country;
        this.category = category;
        this.tags = tags;
        this.climate = climate;
        this.bestSeason = bestSeason;
        this.averageDailyBudget = averageDailyBudget;
        this.popularityScore = popularityScore;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getClimate() { return climate; }
    public void setClimate(String climate) { this.climate = climate; }
    public String getBestSeason() { return bestSeason; }
    public void setBestSeason(String bestSeason) { this.bestSeason = bestSeason; }
    public BigDecimal getAverageDailyBudget() { return averageDailyBudget; }
    public void setAverageDailyBudget(BigDecimal averageDailyBudget) { this.averageDailyBudget = averageDailyBudget; }
    public Double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
