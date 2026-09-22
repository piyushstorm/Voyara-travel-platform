package com.travelplatform.dto.selection;

import java.util.List;

public class UserTravelPreferenceDto {
    private String preferredSeatPosition; // WINDOW, AISLE, MIDDLE, ANY
    private String preferredSeatType; // STANDARD, PREMIUM, EXTRA_LEGROOM
    private String preferredRoomType; // STANDARD, DELUXE, SUITE, PRESIDENTIAL
    private String preferredBedType; // KING, QUEEN, TWIN, SINGLE
    private List<String> preferredRoomFeatures; // CITY_VIEW, HIGH_FLOOR, BALCONY, SEA_VIEW
    private String preferredDestinations; // BEACH, MOUNTAIN, HERITAGE, LUXURY
    private String preferredCabinClass; // ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST
    private String budgetLevel; // BUDGET, MID_RANGE, LUXURY

    public UserTravelPreferenceDto() {}

    public UserTravelPreferenceDto(String preferredSeatPosition, String preferredSeatType,
                                   String preferredRoomType, String preferredBedType,
                                   List<String> preferredRoomFeatures) {
        this.preferredSeatPosition = preferredSeatPosition;
        this.preferredSeatType = preferredSeatType;
        this.preferredRoomType = preferredRoomType;
        this.preferredBedType = preferredBedType;
        this.preferredRoomFeatures = preferredRoomFeatures;
    }

    public UserTravelPreferenceDto(String preferredSeatPosition, String preferredSeatType,
                                   String preferredRoomType, String preferredBedType,
                                   List<String> preferredRoomFeatures, String preferredDestinations,
                                   String preferredCabinClass, String budgetLevel) {
        this.preferredSeatPosition = preferredSeatPosition;
        this.preferredSeatType = preferredSeatType;
        this.preferredRoomType = preferredRoomType;
        this.preferredBedType = preferredBedType;
        this.preferredRoomFeatures = preferredRoomFeatures;
        this.preferredDestinations = preferredDestinations;
        this.preferredCabinClass = preferredCabinClass;
        this.budgetLevel = budgetLevel;
    }

    public String getPreferredSeatPosition() { return preferredSeatPosition; }
    public void setPreferredSeatPosition(String preferredSeatPosition) { this.preferredSeatPosition = preferredSeatPosition; }
    public String getPreferredSeatType() { return preferredSeatType; }
    public void setPreferredSeatType(String preferredSeatType) { this.preferredSeatType = preferredSeatType; }
    public String getPreferredRoomType() { return preferredRoomType; }
    public void setPreferredRoomType(String preferredRoomType) { this.preferredRoomType = preferredRoomType; }
    public String getPreferredBedType() { return preferredBedType; }
    public void setPreferredBedType(String preferredBedType) { this.preferredBedType = preferredBedType; }
    public List<String> getPreferredRoomFeatures() { return preferredRoomFeatures; }
    public void setPreferredRoomFeatures(List<String> preferredRoomFeatures) { this.preferredRoomFeatures = preferredRoomFeatures; }
    public String getPreferredDestinations() { return preferredDestinations; }
    public void setPreferredDestinations(String preferredDestinations) { this.preferredDestinations = preferredDestinations; }
    public String getPreferredCabinClass() { return preferredCabinClass; }
    public void setPreferredCabinClass(String preferredCabinClass) { this.preferredCabinClass = preferredCabinClass; }
    public String getBudgetLevel() { return budgetLevel; }
    public void setBudgetLevel(String budgetLevel) { this.budgetLevel = budgetLevel; }
}
