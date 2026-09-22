package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "add_ons")
public class AddOn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // e.g., "Extra Baggage - 5kg"

    private String description;

    @Column(nullable = false)
    private String category;  // BAGGAGE, MEAL, SEAT, PROTECTION

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    private String cabinClass;  // null = all cabin classes, or ECONOMY/BUSINESS/etc

    private String applicability;  // ALL, DOMESTIC, INTERNATIONAL

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public AddOn() {}

    public AddOn(String name, String description, String category, BigDecimal price, String cabinClass, String applicability) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.cabinClass = cabinClass;
        this.applicability = applicability;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCabinClass() { return cabinClass; }
    public void setCabinClass(String cabinClass) { this.cabinClass = cabinClass; }
    public String getApplicability() { return applicability; }
    public void setApplicability(String applicability) { this.applicability = applicability; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
