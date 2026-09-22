package com.travelplatform.dto.selection;

import java.util.List;
import java.util.Map;

public class RoomSelectionResponseDto {
    private Long hotelId;
    private String hotelName;
    private String city;
    private int starRating;
    private List<RoomDto> rooms;
    private List<Map<String, Object>> upgradeOptions;
    private String userPreferenceSummary;
    private Long myActiveHoldRoomId;

    public RoomSelectionResponseDto() {}

    public RoomSelectionResponseDto(Long hotelId, String hotelName, String city, int starRating,
                                    List<RoomDto> rooms, List<Map<String, Object>> upgradeOptions,
                                    String userPreferenceSummary, Long myActiveHoldRoomId) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.city = city;
        this.starRating = starRating;
        this.rooms = rooms;
        this.upgradeOptions = upgradeOptions;
        this.userPreferenceSummary = userPreferenceSummary;
        this.myActiveHoldRoomId = myActiveHoldRoomId;
    }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public int getStarRating() { return starRating; }
    public void setStarRating(int starRating) { this.starRating = starRating; }
    public List<RoomDto> getRooms() { return rooms; }
    public void setRooms(List<RoomDto> rooms) { this.rooms = rooms; }
    public List<Map<String, Object>> getUpgradeOptions() { return upgradeOptions; }
    public void setUpgradeOptions(List<Map<String, Object>> upgradeOptions) { this.upgradeOptions = upgradeOptions; }
    public String getUserPreferenceSummary() { return userPreferenceSummary; }
    public void setUserPreferenceSummary(String userPreferenceSummary) { this.userPreferenceSummary = userPreferenceSummary; }
    public Long getMyActiveHoldRoomId() { return myActiveHoldRoomId; }
    public void setMyActiveHoldRoomId(Long myActiveHoldRoomId) { this.myActiveHoldRoomId = myActiveHoldRoomId; }
}
