package com.travelplatform.controller;

import com.travelplatform.dto.selection.UserTravelPreferenceDto;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.service.UserTravelPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/preferences/travel")
public class UserPreferenceController {

    private final UserTravelPreferenceService preferenceService;
    private final UserRepository userRepository;

    public UserPreferenceController(UserTravelPreferenceService preferenceService, UserRepository userRepository) {
        this.preferenceService = preferenceService;
        this.userRepository = userRepository;
    }

    private User getUserOrThrow(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Authentication required");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<UserTravelPreferenceDto> getMyPreferences(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        return ResponseEntity.ok(preferenceService.getPreferences(userId));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> savePreferences(
            @RequestBody UserTravelPreferenceDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserOrThrow(userDetails).getId();
        UserTravelPreferenceDto saved = preferenceService.savePreferences(userId, dto);
        return ResponseEntity.ok(Map.of(
                "message", "Travel preferences saved successfully",
                "preferences", saved
        ));
    }
}
