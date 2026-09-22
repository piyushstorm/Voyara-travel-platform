package com.travelplatform.controller;

import com.travelplatform.dto.selection.RoomHoldResponseDto;
import com.travelplatform.dto.selection.RoomSelectionResponseDto;
import com.travelplatform.entity.RoomHold;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.RoomSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomSelectionService roomService;
    private final UserRepository userRepository;

    public RoomController(RoomSelectionService roomService, UserRepository userRepository) {
        this.roomService = roomService;
        this.userRepository = userRepository;
    }

    private User getUserOrNull(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }

    private User getUserOrThrow(UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("Authentication required");
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /** Get room selection data with gallery images, upgrade deltas, and personalized recommendations */
    @GetMapping("/selection/{hotelId}")
    public ResponseEntity<RoomSelectionResponseDto> getRoomSelection(
            @PathVariable Long hotelId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserOrNull(userDetails);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(roomService.getRoomSelectionDto(hotelId, userId));
    }

    /** Get room upgrade options above the current selection */
    @GetMapping("/upgrades/{hotelId}")
    public ResponseEntity<List<Map<String, Object>>> getUpgradeOptions(
            @PathVariable Long hotelId,
            @RequestParam(defaultValue = "STANDARD") String currentRoomType) {
        return ResponseEntity.ok(roomService.getUpgradeOptions(hotelId, currentRoomType));
    }

    /** Hold a room temporarily (10-minute expiry, pessimistic lock) */
    @PostMapping("/{roomId}/hold")
    public ResponseEntity<RoomHoldResponseDto> holdRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        return ResponseEntity.ok(roomService.holdRoomDto(roomId, userId));
    }

    /** Release a room hold */
    @DeleteMapping("/{roomId}/hold")
    public ResponseEntity<?> releaseRoomHold(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        roomService.releaseRoomHold(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Room hold released"));
    }
}
