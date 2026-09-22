package com.travelplatform.service;

import com.travelplatform.dto.selection.UserTravelPreferenceDto;
import com.travelplatform.entity.User;
import com.travelplatform.entity.UserTravelPreference;
import com.travelplatform.exception.ResourceNotFoundException;
import com.travelplatform.repository.UserRepository;
import com.travelplatform.repository.UserTravelPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class UserTravelPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserTravelPreferenceService.class);

    private final UserTravelPreferenceRepository preferenceRepo;
    private final UserRepository userRepo;

    public UserTravelPreferenceService(UserTravelPreferenceRepository preferenceRepo, UserRepository userRepo) {
        this.preferenceRepo = preferenceRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public UserTravelPreferenceDto getPreferences(Long userId) {
        UserTravelPreference pref = preferenceRepo.findByUserId(userId)
                .orElse(null);

        if (pref == null) {
            return new UserTravelPreferenceDto("WINDOW", "STANDARD", "DELUXE", "KING", List.of("CITY_VIEW", "HIGH_FLOOR"), "BEACH,RELAXATION", "ECONOMY", "MID_RANGE");
        }

        List<String> features = pref.getPreferredRoomFeatures() != null && !pref.getPreferredRoomFeatures().isBlank()
                ? Arrays.stream(pref.getPreferredRoomFeatures().split(",")).map(String::trim).toList()
                : List.of();

        return new UserTravelPreferenceDto(
                pref.getPreferredSeatPosition(),
                pref.getPreferredSeatType(),
                pref.getPreferredRoomType(),
                pref.getPreferredBedType(),
                features,
                pref.getPreferredDestinations(),
                pref.getPreferredCabinClass(),
                pref.getBudgetLevel()
        );
    }

    @Transactional
    public UserTravelPreferenceDto savePreferences(Long userId, UserTravelPreferenceDto dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserTravelPreference pref = preferenceRepo.findByUserId(userId)
                .orElseGet(() -> new UserTravelPreference(user));

        if (dto.getPreferredSeatPosition() != null) {
            pref.setPreferredSeatPosition(dto.getPreferredSeatPosition().toUpperCase());
        }
        if (dto.getPreferredSeatType() != null) {
            pref.setPreferredSeatType(dto.getPreferredSeatType().toUpperCase());
        }
        if (dto.getPreferredRoomType() != null) {
            pref.setPreferredRoomType(dto.getPreferredRoomType().toUpperCase());
        }
        if (dto.getPreferredBedType() != null) {
            pref.setPreferredBedType(dto.getPreferredBedType().toUpperCase());
        }
        if (dto.getPreferredRoomFeatures() != null) {
            pref.setPreferredRoomFeatures(String.join(",", dto.getPreferredRoomFeatures()));
        }
        if (dto.getPreferredDestinations() != null) {
            pref.setPreferredDestinations(dto.getPreferredDestinations());
        }
        if (dto.getPreferredCabinClass() != null) {
            pref.setPreferredCabinClass(dto.getPreferredCabinClass().toUpperCase());
        }
        if (dto.getBudgetLevel() != null) {
            pref.setBudgetLevel(dto.getBudgetLevel().toUpperCase());
        }

        preferenceRepo.save(pref);
        logger.info("Saved travel preferences for user {}: seat={}, room={}, destinations={}, budget={}",
                userId, pref.getPreferredSeatPosition(), pref.getPreferredRoomType(), pref.getPreferredDestinations(), pref.getBudgetLevel());

        return getPreferences(userId);
    }
}
