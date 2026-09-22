package com.travelplatform.dto.selection;

import java.time.LocalDateTime;

public class RoomHoldResponseDto {
    private Long holdId;
    private Long roomId;
    private String roomName;
    private String roomType;
    private LocalDateTime expiresAt;
    private long secondsRemaining;
    private String status;

    public RoomHoldResponseDto() {}

    public RoomHoldResponseDto(Long holdId, Long roomId, String roomName, String roomType,
                               LocalDateTime expiresAt, long secondsRemaining, String status) {
        this.holdId = holdId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.expiresAt = expiresAt;
        this.secondsRemaining = secondsRemaining;
        this.status = status;
    }

    public Long getHoldId() { return holdId; }
    public void setHoldId(Long holdId) { this.holdId = holdId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public long getSecondsRemaining() { return secondsRemaining; }
    public void setSecondsRemaining(long secondsRemaining) { this.secondsRemaining = secondsRemaining; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
