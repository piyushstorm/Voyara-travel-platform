package com.travelplatform.service;

import com.travelplatform.dto.selection.UserTravelPreferenceDto;
import com.travelplatform.entity.User;
import com.travelplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserTravelPreferenceTest {

    @Autowired private UserTravelPreferenceService preferenceService;
    @Autowired private UserRepository userRepository;
    @Autowired private com.travelplatform.repository.UserTravelPreferenceRepository preferenceRepo;

    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        preferenceRepo.deleteAll();
        user1 = userRepository.findByEmail("pref_user1@voyara.com").orElseGet(() ->
                userRepository.save(new User("Pref User 1", "pref_user1@voyara.com", "Password123!")));

        user2 = userRepository.findByEmail("pref_user2@voyara.com").orElseGet(() ->
                userRepository.save(new User("Pref User 2", "pref_user2@voyara.com", "Password123!")));
    }

    @Test
    @DisplayName("Default travel preferences are returned when no saved preference exists")
    void testDefaultPreferences() {
        UserTravelPreferenceDto dto = preferenceService.getPreferences(user1.getId());
        assertNotNull(dto);
        assertEquals("WINDOW", dto.getPreferredSeatPosition());
        assertEquals("DELUXE", dto.getPreferredRoomType());
    }

    @Test
    @DisplayName("Successfully save and retrieve user travel preferences")
    void testSaveAndRetrievePreferences() {
        UserTravelPreferenceDto update = new UserTravelPreferenceDto(
                "AISLE",
                "EXTRA_LEGROOM",
                "SUITE",
                "KING",
                List.of("CITY_VIEW", "BALCONY", "HIGH_FLOOR")
        );

        UserTravelPreferenceDto saved = preferenceService.savePreferences(user1.getId(), update);
        assertEquals("AISLE", saved.getPreferredSeatPosition());
        assertEquals("EXTRA_LEGROOM", saved.getPreferredSeatType());
        assertEquals("SUITE", saved.getPreferredRoomType());
        assertEquals("KING", saved.getPreferredBedType());
        assertTrue(saved.getPreferredRoomFeatures().contains("BALCONY"));

        // Retrieve again to verify persistence
        UserTravelPreferenceDto fetched = preferenceService.getPreferences(user1.getId());
        assertEquals("AISLE", fetched.getPreferredSeatPosition());
        assertEquals("SUITE", fetched.getPreferredRoomType());
    }

    @Test
    @DisplayName("Preferences are isolated per user (no leakage/IDOR)")
    void testUserIsolation() {
        UserTravelPreferenceDto user1Update = new UserTravelPreferenceDto("WINDOW", "PREMIUM", "DELUXE", "QUEEN", List.of("SEA_VIEW"));
        preferenceService.savePreferences(user1.getId(), user1Update);

        UserTravelPreferenceDto user2Update = new UserTravelPreferenceDto("AISLE", "STANDARD", "STANDARD", "TWIN", List.of("QUIET_ROOM"));
        preferenceService.savePreferences(user2.getId(), user2Update);

        UserTravelPreferenceDto user1Prefs = preferenceService.getPreferences(user1.getId());
        UserTravelPreferenceDto user2Prefs = preferenceService.getPreferences(user2.getId());

        assertEquals("WINDOW", user1Prefs.getPreferredSeatPosition());
        assertEquals("AISLE", user2Prefs.getPreferredSeatPosition());
        assertNotEquals(user1Prefs.getPreferredRoomType(), user2Prefs.getPreferredRoomType());
    }
}
