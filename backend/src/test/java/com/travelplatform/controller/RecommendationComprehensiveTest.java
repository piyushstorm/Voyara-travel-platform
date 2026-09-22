package com.travelplatform.controller;

import com.travelplatform.entity.*;
import com.travelplatform.repository.*;
import com.travelplatform.security.JwtTokenProvider;
import com.travelplatform.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecommendationComprehensiveTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private DestinationRepository destinationRepo;
    @Autowired private HotelRepository hotelRepo;
    @Autowired private FlightRepository flightRepo;
    @Autowired private AirlineRepository airlineRepo;
    @Autowired private AirportRepository airportRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private RecommendationRepository recRepo;
    @Autowired private RecommendationFeedbackRepository feedbackRepo;
    @Autowired private UserTravelPreferenceRepository preferenceRepo;
    @Autowired private UserInteractionRepository interactionRepo;
    @Autowired private RecommendationService recommendationService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private User beachUser;
    private User cityUser;
    private User peerTraveler;
    private String beachUserToken;
    private String cityUserToken;

    private Destination goa;
    private Destination bali;
    private Destination paris;
    private Hotel beachResort;
    private Hotel cityHotel;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        beachUser = new User("Beach Explorer", "beach-" + uid + "@test.com", passwordEncoder.encode("pass123"));
        beachUser.setRole(Role.USER);
        beachUser = userRepo.save(beachUser);
        beachUserToken = tokenProvider.generateAccessToken(beachUser.getEmail());

        cityUser = new User("City Explorer", "city-" + uid + "@test.com", passwordEncoder.encode("pass123"));
        cityUser.setRole(Role.USER);
        cityUser = userRepo.save(cityUser);
        cityUserToken = tokenProvider.generateAccessToken(cityUser.getEmail());

        peerTraveler = new User("Peer Traveler", "peer-" + uid + "@test.com", passwordEncoder.encode("pass123"));
        peerTraveler.setRole(Role.USER);
        peerTraveler = userRepo.save(peerTraveler);

        // 1. Seed Destinations
        goa = destinationRepo.findByNameIgnoreCase("Goa").orElseGet(() -> {
            Destination d = new Destination("Goa", "Goa", "India", "BEACH", "BEACH,NIGHTLIFE,RELAXATION,WATERSPORTS",
                    "Tropical", "Nov-Feb", BigDecimal.valueOf(3500), 0.98, "https://unsplash.com/goa", "Beach paradise");
            return destinationRepo.save(d);
        });

        bali = destinationRepo.findByNameIgnoreCase("Bali").orElseGet(() -> {
            Destination d = new Destination("Bali", "Denpasar", "Indonesia", "BEACH", "BEACH,ISLAND,RELAXATION,NATURE",
                    "Tropical", "Apr-Oct", BigDecimal.valueOf(5500), 0.97, "https://unsplash.com/bali", "Island of gods");
            return destinationRepo.save(d);
        });

        paris = destinationRepo.findByNameIgnoreCase("Paris").orElseGet(() -> {
            Destination d = new Destination("Paris", "Paris", "France", "HERITAGE", "CULTURE,ROMANTIC,CITY,LUXURY",
                    "Oceanic", "Jun-Aug", BigDecimal.valueOf(16000), 0.97, "https://unsplash.com/paris", "City of light");
            return destinationRepo.save(d);
        });

        // 2. Seed Hotels
        beachResort = new Hotel();
        beachResort.setName("W Goa Vagator");
        beachResort.setCity("Goa");
        beachResort.setCountry("India");
        beachResort.setStarRating(5);
        beachResort.setGuestRating(4.8);
        beachResort.setStartingPrice(BigDecimal.valueOf(12000));
        beachResort.setImageUrl("https://unsplash.com/w-goa");
        beachResort.getAmenities().addAll(List.of("POOL", "SPA", "BEACHFRONT", "RESTAURANT"));
        beachResort = hotelRepo.save(beachResort);

        cityHotel = new Hotel();
        cityHotel.setName("The Taj Mahal Palace");
        cityHotel.setCity("Mumbai");
        cityHotel.setCountry("India");
        cityHotel.setStarRating(5);
        cityHotel.setGuestRating(4.9);
        cityHotel.setStartingPrice(BigDecimal.valueOf(15000));
        cityHotel.setImageUrl("https://unsplash.com/taj-mumbai");
        cityHotel.getAmenities().addAll(List.of("POOL", "SPA", "BUSINESS", "CONFERENCE_ROOMS"));
        cityHotel = hotelRepo.save(cityHotel);

        // 3. User Preferences
        UserTravelPreference pref = new UserTravelPreference(beachUser);
        pref.setPreferredDestinations("BEACH,RELAXATION");
        pref.setBudgetLevel("LUXURY");
        preferenceRepo.save(pref);

        // 4. Beach user booking history in Goa
        Booking b1 = new Booking();
        b1.setBookingReference("VOY-TEST-BCH-" + uid);
        b1.setUser(beachUser);
        b1.setBookingType("HOTEL");
        b1.setStatus("COMPLETED");
        b1.setHotel(beachResort);
        b1.setTotalAmount(BigDecimal.valueOf(24000));
        b1.setTravelDate(LocalDateTime.now().minusDays(10));
        bookingRepo.save(b1);

        // 5. Peer traveler overlapping booking in Goa AND also booked Bali!
        Booking bPeer1 = new Booking();
        bPeer1.setBookingReference("VOY-TEST-PEER1-" + uid);
        bPeer1.setUser(peerTraveler);
        bPeer1.setBookingType("HOTEL");
        bPeer1.setStatus("COMPLETED");
        bPeer1.setHotel(beachResort);
        bPeer1.setTotalAmount(BigDecimal.valueOf(24000));
        bPeer1.setTravelDate(LocalDateTime.now().minusDays(15));
        bookingRepo.save(bPeer1);

        UserInteraction peerBaliInteraction = new UserInteraction(peerTraveler, "DESTINATION", bali.getId(), "BOOKED");
        peerBaliInteraction.setRating(5);
        peerBaliInteraction.setTags("beach,island,tropical");
        interactionRepo.save(peerBaliInteraction);
    }

    @Test
    @DisplayName("Content-based destination recommendation: Beach traveler receives Bali with 'You liked beaches!' explanation")
    void testPersonalizedDestinationRecommendations() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + beachUserToken)
                        .param("entityType", "DESTINATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].entityType").value("DESTINATION"))
                .andExpect(jsonPath("$[0].itemDetails.name").isNotEmpty())
                .andExpect(jsonPath("$[0].why.title").value(containsString("beach")));
    }

    @Test
    @DisplayName("Structured explanation: Recommendations include why object, headline, and multi-signal reasons")
    void testStructuredExplanationBreakdown() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + beachUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].why").exists())
                .andExpect(jsonPath("$[0].why.title").isNotEmpty())
                .andExpect(jsonPath("$[0].why.reasons").isArray())
                .andExpect(jsonPath("$[0].why.reasons[0].label").isNotEmpty())
                .andExpect(jsonPath("$[0].why.reasons[0].type").isNotEmpty());
    }

    @Test
    @DisplayName("Collaborative Filtering: Peer travelers overlapping in Goa recommend unexplored Bali destination")
    void testCollaborativeFilteringRecommendation() throws Exception {
        recommendationService.computeAndPersistForUser(beachUser.getId(), "test_collab");

        List<Recommendation> recs = recRepo.findByUserIdAndEntityTypeOrderByScoreDesc(beachUser.getId(), "DESTINATION");
        assertThat(recs).isNotEmpty();

        boolean containsBali = recs.stream().anyMatch(r -> r.getEntityId().equals(bali.getId()));
        assertThat(containsBali).isTrue();
    }

    @Test
    @DisplayName("Feedback loop: Marking recommendation IRRELEVANT excludes it from future recommendations")
    void testFeedbackLoopIrrelevantDemotion() throws Exception {
        // 1. Initial recommendation contains Bali
        var initialRecs = recommendationService.getRecommendations(beachUser.getId(), "DESTINATION", 10);
        Long baliRecId = initialRecs.stream()
                .filter(r -> bali.getId().equals(r.getEntityId()))
                .map(com.travelplatform.dto.recommendation.RecommendationResponseDto::getId)
                .findFirst().orElseThrow();

        // 2. User marks Bali as IRRELEVANT
        mockMvc.perform(post("/api/recommendations/" + baliRecId + "/feedback")
                        .header("Authorization", "Bearer " + beachUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"IRRELEVANT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Permanent feedback record is created
        assertThat(feedbackRepo.findIrrelevantEntityIds(beachUser.getId(), "DESTINATION")).contains(bali.getId());

        // 4. Future recommendations DO NOT contain Bali
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + beachUserToken)
                        .param("entityType", "DESTINATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.entityId == " + bali.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("Feedback loop: Marking recommendation HELPFUL boosts category affinity and reflects in response")
    void testFeedbackLoopHelpfulBoost() throws Exception {
        var initialRecs = recommendationService.getRecommendations(beachUser.getId(), "DESTINATION", 10);
        Long goaRecId = initialRecs.stream()
                .filter(r -> goa.getId().equals(r.getEntityId()))
                .map(com.travelplatform.dto.recommendation.RecommendationResponseDto::getId)
                .findFirst().orElse(initialRecs.get(0).getId());

        mockMvc.perform(post("/api/recommendations/" + goaRecId + "/feedback")
                        .header("Authorization", "Bearer " + beachUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"HELPFUL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(feedbackRepo.findHelpfulEntityIds(beachUser.getId(), "DESTINATION")).contains(goa.getId());
    }

    @Test
    @DisplayName("Cold-Start: Unauthenticated user receives curated trending destinations labeled POPULAR_COLD_START")
    void testColdStartAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                        .param("entityType", "DESTINATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].algorithm").value("POPULAR_COLD_START"))
                .andExpect(jsonPath("$[0].itemDetails.name").isNotEmpty());
    }

    @Test
    @DisplayName("Duplicate & active inventory prevention: Currently booked hotel is excluded from recommendations")
    void testDuplicateAndConsumedExclusion() throws Exception {
        recommendationService.computeAndPersistForUser(beachUser.getId(), "test_duplicate");

        List<Recommendation> hotelRecs = recRepo.findByUserIdAndEntityTypeOrderByScoreDesc(beachUser.getId(), "HOTEL");
        boolean containsBookedHotel = hotelRecs.stream().anyMatch(r -> r.getEntityId().equals(beachResort.getId()));
        assertThat(containsBookedHotel).isFalse();
    }

    @Test
    @DisplayName("Security & IDOR: Unauthenticated feedback returns 401; users cannot submit feedback for other users")
    void testSecurityAndIdorProtection() throws Exception {
        var recs = recommendationService.getRecommendations(beachUser.getId(), "DESTINATION", 5);
        Long recId = recs.get(0).getId();

        // Unauthenticated -> 401 or 403 Forbidden
        mockMvc.perform(post("/api/recommendations/" + recId + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"HELPFUL\"}"))
                .andExpect(status().is(org.hamcrest.Matchers.in(java.util.List.of(401, 403))));

        // IDOR: cityUser attempting to feedback on beachUser's recommendation -> 400 Bad Request
        mockMvc.perform(post("/api/recommendations/" + recId + "/feedback")
                        .header("Authorization", "Bearer " + cityUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"HELPFUL\"}"))
                .andExpect(status().isBadRequest());
    }
}
