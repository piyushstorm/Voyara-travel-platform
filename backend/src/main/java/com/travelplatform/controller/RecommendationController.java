package com.travelplatform.controller;

import com.travelplatform.dto.recommendation.RecommendationFeedbackRequestDto;
import com.travelplatform.dto.recommendation.RecommendationResponseDto;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recService;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recService, UserRepository userRepository) {
        this.recService = recService;
        this.userRepository = userRepository;
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElse(null);
    }

    /**
     * Get personalized recommendations for user, with cold-start discovery fallback for anonymous users.
     */
    @GetMapping
    public ResponseEntity<List<RecommendationResponseDto>> getRecommendations(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<RecommendationResponseDto> results = recService.getRecommendations(userId, entityType, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Submit feedback on a specific recommendation.
     */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody RecommendationFeedbackRequestDto body,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        Long userId = getUserId(userDetails);
        recService.submitFeedback(id, userId, body.getFeedback());
        return ResponseEntity.ok(Map.of("success", true, "message", "Feedback recorded successfully"));
    }

    /**
     * Submit feedback directly for an entity (DESTINATION, HOTEL, FLIGHT).
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> submitEntityFeedback(
            @Valid @RequestBody RecommendationFeedbackRequestDto body,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        Long userId = getUserId(userDetails);
        recService.submitEntityFeedback(userId, body.getEntityType(), body.getEntityId(), body.getFeedback());
        return ResponseEntity.ok(Map.of("success", true, "message", "Entity feedback recorded successfully"));
    }

    /**
     * Trigger immediate recommendation recomputation for the authenticated user.
     */
    @PostMapping("/recompute")
    public ResponseEntity<?> triggerComputation(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        Long userId = getUserId(userDetails);
        recService.computeAndPersistForUser(userId, "manual_" + System.currentTimeMillis());
        return ResponseEntity.ok(Map.of("success", true, "message", "Recommendations recomputed successfully"));
    }
}
