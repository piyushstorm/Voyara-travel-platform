package com.travelplatform.controller;

import com.travelplatform.dto.selection.SeatHoldResponseDto;
import com.travelplatform.dto.selection.SeatMapResponseDto;
import com.travelplatform.entity.SeatHold;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.SeatSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatSelectionService seatService;
    private final UserRepository userRepository;

    public SeatController(SeatSelectionService seatService, UserRepository userRepository) {
        this.seatService = seatService;
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

    /** Get the seat map for a flight's cabin class with personalized recommendations */
    @GetMapping("/map")
    public ResponseEntity<SeatMapResponseDto> getSeatMap(
            @RequestParam Long flightId,
            @RequestParam String cabinClass,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserOrNull(userDetails);
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(seatService.getSeatMapDto(flightId, cabinClass, userId));
    }

    /** Hold a seat temporarily (10-minute expiry, pessimistic lock) */
    @PostMapping("/{seatId}/hold")
    public ResponseEntity<SeatHoldResponseDto> holdSeat(
            @PathVariable Long seatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        return ResponseEntity.ok(seatService.holdSeatDto(seatId, userId));
    }

    /** Release a seat hold */
    @DeleteMapping("/{seatId}/hold")
    public ResponseEntity<?> releaseHold(
            @PathVariable Long seatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        seatService.releaseHold(seatId, userId);
        return ResponseEntity.ok(Map.of("message", "Seat hold released"));
    }

    /** Get user's active holds */
    @GetMapping("/holds")
    public ResponseEntity<List<SeatHoldResponseDto>> getMyHolds(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        List<SeatHold> holds = seatService.getUserActiveHolds(userId);
        List<SeatHoldResponseDto> dtos = holds.stream().map(h -> {
            long remaining = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), h.getExpiresAt()));
            com.travelplatform.entity.Seat seat = h.getSeat();
            java.math.BigDecimal price = seat != null && seat.getPrice() != null ? seat.getPrice() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal surcharge = seat != null && seat.getPremiumSurcharge() != null ? seat.getPremiumSurcharge() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal total = price.add(surcharge);
            return new SeatHoldResponseDto(
                    h.getId(),
                    seat != null ? seat.getId() : null,
                    seat != null ? seat.getSeatNumber() : "",
                    seat != null ? seat.getCabinClass() : "",
                    h.getExpiresAt(),
                    remaining,
                    h.getStatus(),
                    price,
                    surcharge,
                    total
            );
        }).toList();
        return ResponseEntity.ok(dtos);
    }
}
