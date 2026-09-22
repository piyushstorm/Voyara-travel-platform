package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores explicit user travel preferences across flights and hotels for personalized recommendations.
 */
@Entity
@Table(name = "user_travel_preferences", indexes = {
    @Index(name = "idx_travel_pref_user", columnList = "user_id")
})
public class UserTravelPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** WINDOW, AISLE, MIDDLE, ANY */
    @Column(name = "preferred_seat_position", length = 30)
    private String preferredSeatPosition = "WINDOW";

    /** STANDARD, PREMIUM, EXTRA_LEGROOM */
    @Column(name = "preferred_seat_type", length = 30)
    private String preferredSeatType = "STANDARD";

    /** STANDARD, DELUXE, SUITE, PRESIDENTIAL */
    @Column(name = "preferred_room_type", length = 30)
    private String preferredRoomType = "DELUXE";

    /** KING, QUEEN, TWIN, SINGLE */
    @Column(name = "preferred_bed_type", length = 30)
    private String preferredBedType = "KING";

    /** Comma-separated features: CITY_VIEW, HIGH_FLOOR, BALCONY, SEA_VIEW */
    @Column(name = "preferred_room_features", length = 255)
    private String preferredRoomFeatures = "CITY_VIEW,HIGH_FLOOR";

    /** Comma-separated destination categories or names: BEACH, MOUNTAIN, HERITAGE, Goa, Bali */
    @Column(name = "preferred_destinations", length = 255)
    private String preferredDestinations = "BEACH,RELAXATION";

    /** ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST */
    @Column(name = "preferred_cabin_class", length = 50)
    private String preferredCabinClass = "ECONOMY";

    /** BUDGET, MID_RANGE, LUXURY */
    @Column(name = "budget_level", length = 50)
    private String budgetLevel = "MID_RANGE";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public UserTravelPreference() {}

    public UserTravelPreference(User user) {
        this.user = user;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPreferredSeatPosition() { return preferredSeatPosition; }
    public void setPreferredSeatPosition(String preferredSeatPosition) { this.preferredSeatPosition = preferredSeatPosition; }
    public String getPreferredSeatType() { return preferredSeatType; }
    public void setPreferredSeatType(String preferredSeatType) { this.preferredSeatType = preferredSeatType; }
    public String getPreferredRoomType() { return preferredRoomType; }
    public void setPreferredRoomType(String preferredRoomType) { this.preferredRoomType = preferredRoomType; }
    public String getPreferredBedType() { return preferredBedType; }
    public void setPreferredBedType(String preferredBedType) { this.preferredBedType = preferredBedType; }
    public String getPreferredRoomFeatures() { return preferredRoomFeatures; }
    public void setPreferredRoomFeatures(String preferredRoomFeatures) { this.preferredRoomFeatures = preferredRoomFeatures; }
    public String getPreferredDestinations() { return preferredDestinations; }
    public void setPreferredDestinations(String preferredDestinations) { this.preferredDestinations = preferredDestinations; }
    public String getPreferredCabinClass() { return preferredCabinClass; }
    public void setPreferredCabinClass(String preferredCabinClass) { this.preferredCabinClass = preferredCabinClass; }
    public String getBudgetLevel() { return budgetLevel; }
    public void setBudgetLevel(String budgetLevel) { this.budgetLevel = budgetLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
