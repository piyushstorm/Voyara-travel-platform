package com.travelplatform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "airlines")
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;  // e.g., "AI", "6E", "SG"

    @NotBlank
    private String name;

    private String logoUrl;

    private double rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Airline() {}

    public Airline(String code, String name, String logoUrl, double rating) {
        this.code = code;
        this.name = name;
        this.logoUrl = logoUrl;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
