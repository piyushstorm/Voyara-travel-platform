package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.dto.pricing.PricingBreakdownResponse;
import com.travelplatform.entity.PriceFreeze;
import com.travelplatform.entity.PriceHistory;
import com.travelplatform.entity.User;
import com.travelplatform.exception.BadRequestException;
import com.travelplatform.repository.PriceHistoryRepository;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.DynamicPricingService;
import com.travelplatform.service.PriceFreezeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private final PriceHistoryRepository priceHistoryRepo;
    private final PriceFreezeService freezeService;
    private final DynamicPricingService dynamicPricingService;
    private final UserRepository userRepository;

    public PriceController(PriceHistoryRepository priceHistoryRepo,
                           PriceFreezeService freezeService,
                           DynamicPricingService dynamicPricingService,
                           UserRepository userRepository) {
        this.priceHistoryRepo = priceHistoryRepo;
        this.freezeService = freezeService;
        this.dynamicPricingService = dynamicPricingService;
        this.userRepository = userRepository;
    }

    private User getUserOrThrow(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("Authentication required");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /** Get price history for a flight or hotel (for the price chart) */
    @GetMapping("/history")
    public ResponseEntity<List<PriceHistory>> getPriceHistory(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam(required = false) String cabinClass) {
        List<PriceHistory> history = priceHistoryRepo
                .findTop50ByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
        return ResponseEntity.ok(history);
    }

    /** Get detailed transparent price breakdown */
    @GetMapping("/breakdown")
    public ResponseEntity<PricingBreakdownResponse> getPriceBreakdown(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam(required = false) String cabinClass) {
        PricingBreakdownResponse breakdown = dynamicPricingService.getDetailedPricingBreakdown(entityType, entityId, cabinClass);
        if (breakdown == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(breakdown);
    }

    /** Create a price freeze for a flight */
    @PostMapping("/freeze")
    public ResponseEntity<ApiResponse<PriceFreeze>> createPriceFreeze(
            @RequestParam Long flightId,
            @RequestParam String cabinClass,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        PriceFreeze freeze = freezeService.createFlightFreeze(flightId, cabinClass, userId);
        return ResponseEntity.ok(ApiResponse.success("Price frozen", freeze));
    }

    /** Get active freeze for current user */
    @GetMapping("/freeze/active")
    public ResponseEntity<?> getActiveFreeze(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        Long userId = getUserOrThrow(userDetails).getId();
        PriceFreeze freeze = freezeService.getActiveFreeze(entityType, entityId, userId);
        if (freeze == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        return ResponseEntity.ok(Map.of("active", true, "freeze", freeze));
    }

    /** Get all price freezes for current authenticated user */
    @GetMapping("/freezes")
    public ResponseEntity<List<PriceFreeze>> getUserFreezes(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        return ResponseEntity.ok(freezeService.getUserFreezes(userId));
    }

    /** Get a specific freeze by ID */
    @GetMapping("/freezes/{freezeId}")
    public ResponseEntity<PriceFreeze> getFreezeById(
            @PathVariable Long freezeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        PriceFreeze freeze = freezeService.getFreezeById(freezeId);
        if (!freeze.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied to this price freeze");
        }
        return ResponseEntity.ok(freeze);
    }
}

